package job;

import com.sun.source.util.JavacTask;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Job {
    public static class Reporter {
        public final Consumer<String> command = System.out::println;
        public final Consumer<String> progress = System.out::println;
        public final Consumer<String> error = System.out::println;
        public final Consumer<String> info = System.out::println;
        public final Consumer<String> warning = System.out::println;
        public final Consumer<String> note = System.out::println;
        public void command(Dependency dependency, String command) {
            if (dependency != null) {
                this.command.accept("# "+dependency.id().projectRelativeHyphenatedName+" command line ");
            }
            this.command.accept(command);
        }
        public void progress(Dependency dependency, String command) {
            if (dependency != null) {
                progress.accept("# " + dependency.id().projectRelativeHyphenatedName + " " + command);
            }
        }
        public void error(Dependency dependency, String command) {
            if (dependency != null) {
                error.accept("# "+dependency.id().projectRelativeHyphenatedName+" error ");
            }
            error.accept(command);
        }
        public void info(Dependency dependency, String command) {
           // if (dependency != null) {
           //     info.accept("# "+dependency.id().projectRelativeHyphenatedName+" info ");
          //  }
            info.accept(command);
        }
        public void note(Dependency dependency, String command) {
          //  if (dependency != null) {
            //    note.accept("# "+dependency.id().projectRelativeHyphenatedName+" note ");
          //  }
            note.accept(command);
        }
        public void warning(Dependency dependency, String command) {
         //   if (dependency != null) {
          //      warning.accept("# "+dependency.id().projectRelativeHyphenatedName+" warning ");
          //  }
            warning.accept(command);
        }
        static Reporter verbose = new  Reporter();
        public static Reporter commandsAndErrors = new  Reporter(){
            @Override
            public void warning(Dependency dependency, String command) {

            }
            @Override
            public void info(Dependency dependency, String command) {

            }
            @Override
            public void note(Dependency dependency, String command) {

            }
            @Override
            public void progress(Dependency dependency, String command) {

            }

        };

        public static Reporter progressAndErrors = new  Reporter(){
            @Override
            public void warning(Dependency dependency, String command) {

            }
            @Override
            public void info(Dependency dependency, String command) {

            }
            @Override
            public void note(Dependency dependency, String command) {

            }
            @Override
            public void command(Dependency dependency, String command) {

            }
        };
    }

    public interface Dependency {
        Project.Id id();

        Set<Dependency> dependencies();

        interface WithPath extends Dependency {
        }

        interface Buildable extends Dependency {
            boolean build();

            boolean clean();

            List<Path> generatedPaths();
        }

        interface Executable extends Dependency {
        }

        interface ExecutableJar extends Executable {
            boolean run(String mainClassName, Set<Dependency> depsInOrder, List<String> args);
        }

        interface Runnable extends Executable {
            boolean run();
        }

        interface Optional extends Dependency {
            boolean isAvailable();
        }
    }


    public static abstract class DependencyImpl<T extends DependencyImpl<T>> implements Dependency {
        protected final Project.Id id;

        @Override
        public Project.Id id() {
            return id;
        }

        final private Set<Dependency> dependencies = new LinkedHashSet<>();

        @Override
        public Set<Dependency> dependencies() {
            return dependencies;
        }

        DependencyImpl(Project.Id id, Set<Dependency> dependencies) {
            this.id = id;
            this.dependencies.addAll(dependencies);
        }
    }

    public static class Project {
        public record Id(Project project, String fullHyphenatedName, String projectRelativeHyphenatedName, String shortHyphenatedName, String version,
                         Path path) {
            String str() {
                return project.name() + " " + fullHyphenatedName + " "+projectRelativeHyphenatedName+" " + shortHyphenatedName + " " + version + " " + (path == null ? "null" : path);
            }
            static Id of(Project project, String projectRelativeHyphenatedName, String shortHyphenatedName, String version, Path path) {
                return new Id(project, project.name()+"-"+projectRelativeHyphenatedName+"-"+version, projectRelativeHyphenatedName, shortHyphenatedName, version, path);
            }
        }

        static Id id(Project project, String projectRelativeHyphenatedName) {
            var version = "1.0";
            if (projectRelativeHyphenatedName == null || projectRelativeHyphenatedName.isEmpty()) {
                throw new IllegalArgumentException("projectRelativeHyphenatedName cannot be null or empty yet");
            }
            int lastIndex = projectRelativeHyphenatedName.lastIndexOf('-');
            String[] names;
            if (Pattern.matches("\\d+.\\d+", projectRelativeHyphenatedName.substring(lastIndex + 1))) {
                version = projectRelativeHyphenatedName.substring(lastIndex + 1);
                names = projectRelativeHyphenatedName.substring(0, lastIndex).split("-");
            } else {
                names = projectRelativeHyphenatedName.split("-");
            }

            Path realPossiblyPuralizedPath = null;
            if (project.rootPath().resolve(names[0]) instanceof Path path && Files.isDirectory(path)) {
                realPossiblyPuralizedPath = path;
            } else if (project.rootPath.resolve(names[0] + "s") instanceof Path path && Files.isDirectory(path)) {
                realPossiblyPuralizedPath = path;
            }
            Id id = null;
            if (realPossiblyPuralizedPath == null || names.length == 1) {
                    /* not a dir just a shortHyphenatedName or the shortHyphenatedName is a simplename (no hyphens)
                                               hyphenated                 shortHyphernated       path
                        core ->                core                       core                   <root>/core
                        mac  ->                mac                        mac                    null
                     */
                var shortHyphenatedName = projectRelativeHyphenatedName;
                id = Id.of(project, projectRelativeHyphenatedName, shortHyphenatedName, version, realPossiblyPuralizedPath);
            } else {
                    /* we have one or more names
                                               hyphenated                 shortHyphernated       path
                        backends_ffi_opencl -> backend_ffi_opencl             ffi-opencl         <root>/backend(s)_ffi_opencl

                    */
                var tailNames = Arrays.copyOfRange(names, 1, names.length); // [] -> [....]
                var expectedPath = realPossiblyPuralizedPath.resolve(String.join("/", tailNames));
                if (!Files.isDirectory(expectedPath)) {
                    throw new IllegalArgumentException("The base path existed but sub path does not exist: " + expectedPath);
                } else {
                    if (tailNames.length == 1) {
                        var shortHyphenatedName = tailNames[0];
                        id = Id.of(project,projectRelativeHyphenatedName,  shortHyphenatedName, version,expectedPath);
                    } else {
                        var midNames = Arrays.copyOfRange(tailNames, 0, tailNames.length);
                        var shortHyphenatedName = String.join("-", midNames);
                        id = Id.of(project,projectRelativeHyphenatedName,  shortHyphenatedName, version,expectedPath);
                    }
                }
            }
            return id;
        }

        public Id id(String id) {
            return id(this, id);
        }


        public static class Dag {
            static void recurse(Map<Dependency, Set<Dependency>> map, Dependency from) {
                var set = map.computeIfAbsent(from, _ -> new LinkedHashSet<>());
                var deps = from.dependencies();
                deps.forEach(dep -> {
                    set.add(dep);
                    recurse(map, dep);
                });
            }

            static Set<Dependency> processOrder(Set<Dependency> jars) {
                Map<Dependency, Set<Dependency>> map = new LinkedHashMap<>();
                Set<Dependency> ordered = new LinkedHashSet<>();
                jars.forEach(jar -> recurse(map, jar));
                while (!map.isEmpty()) {
                    var leaves = map.entrySet().stream()
                            .filter(e -> e.getValue().isEmpty())    // if this entry has zero dependencies
                            .map(Map.Entry::getKey)                 // get the key
                            .collect(Collectors.toSet());
                    map.forEach((k, v) ->
                            leaves.forEach(v::remove)
                    );
                    leaves.forEach(leaf -> {
                        map.remove(leaf);
                        ordered.add(leaf);
                    });
                }
                return ordered;
            }

            static Set<Dependency> build(Set<Dependency> jars) {
                var ordered = processOrder(jars);
                ordered.stream().filter(d -> d instanceof Dependency.Buildable).map(d -> (Dependency.Buildable) d).forEach(Dependency.Buildable::build);
                return ordered;
            }

            static Set<Dependency> clean(Set<Dependency> jars) {
                var ordered = processOrder(jars);
                ordered.stream().filter(d -> d instanceof Dependency.Buildable).map(d -> (Dependency.Buildable) d).forEach(Dependency.Buildable::clean);
                return ordered;
            }

        }

        private final Path rootPath;
        private final Path buildPath;
        private final Path confPath;

        private final Map<String, Dependency> artifacts = new LinkedHashMap<>();

        public String name() {
            return rootPath().getFileName().toString();
        }

        public Path rootPath() {
            return rootPath;
        }

        public Path buildPath() {
            return buildPath;
        }

        public Path confPath() {
            return confPath;
        }
public final Reporter reporter;
        public Project(Path root, Reporter reporter) {
            this.rootPath = root;
            if (!Files.exists(root)) {
                throw new IllegalArgumentException("Root path for project does not exist: " + root);
            }
            this.buildPath = root.resolve("build");
            this.confPath = root.resolve("conf");
            this.reporter = reporter;

        }





        public Dependency add(Dependency dependency) {
            artifacts.put(dependency.id().fullHyphenatedName, dependency);
            return dependency;
        }

        public Dependency getArtifact(String dependency) {
            return artifacts.get(dependency);
        }

        public void rmdir(Path... paths) {
            for (Path path : paths) {
                if (Files.exists(path)) {
                    try (var files = Files.walk(path)) {
                        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                }
            }
        }

        public void clean(Dependency dependency, Path... paths) {
            for (Path path : paths) {
                if (Files.exists(path)) {
                    try (var files = Files.walk(path)) {
                        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                        reporter.command(dependency, "rm -rf "+path);
                        mkdir(path);
                        reporter.command(dependency,"mkdir -p "+path);
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                }
            }
        }

        public void mkdir(Path... paths) {
            for (Path path : paths) {
                if (!Files.exists(path)) {
                    try {
                        Files.createDirectories(path);
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                }
            }
        }

        public Set<Dependency> clean(Set<Dependency> dependencies) {
            return Dag.clean(dependencies);
        }

        public void clean(List<String> names) {
            if (names.isEmpty()) {
                rmdir(buildPath());
            } else {
                clean(names.stream().map(this::getArtifact).collect(Collectors.toSet()));
            }
        }

        public Set<Dependency> build(Set<Dependency> dependencies) {
            return Dag.build(dependencies);
        }

        public Set<Dependency> build(Dependency... dependencies) {
            return build(Set.of(dependencies));
        }

        public Set<Dependency> build(List<String> names) {
            if (names.isEmpty()) {
                return build(new HashSet<>(artifacts.values()));
            } else {
                return build(names.stream().map(this::getArtifact).collect(Collectors.toSet()));
            }
        }

        public void start(String... argArr) throws IOException, InterruptedException {
            var args = new ArrayList<>(List.of(argArr));

            Map<String, String> opts = Map.of(
                    "bld", "Will Bld",
                    "help", """
                             help: This list
                              bld: ...buildables | all if none
                                   bld
                                   bld ffi-opencl
                              run: [ffi|my|seq]-[opencl|java|cuda|mock|hip] runnable (i.e has name.Main class)
                                   run ffi-opencl mandel
                                   run ffi-openc nbody
                            clean: ...buildables | all if none
                                   clean
                                   clean ffi-opencl
                            """,
                    "clean", "Will clean",
                    "run", "Will run"
            );
            record Action(String name, String help, List<String> args) {
                int size() {
                    return args.size();
                }

                boolean isEmpty() {
                    return args.isEmpty();
                }

                String get() {
                    var got = (size() > 0) ? args.removeFirst() : null;
                    return got;
                }

                String str() {
                    return name + " '" + String.join(" ", args) + "'";
                }

            }

            List<Action> actions = new ArrayList<>();
            while (!args.isEmpty()) {
                String arg = args.removeFirst();
                if (opts.containsKey(arg)) {
                    List<String> subList = new ArrayList<>();
                    while (!args.isEmpty() &&
                            args.getFirst() instanceof String next
                            && !opts.containsKey(next)) {
                        subList.add(args.removeFirst());
                    }
                    actions.add(new Action(arg, opts.get(arg), subList));
                } else {
                    System.err.println("What " + arg + " " + String.join(" ", args));
                }
            }
            if (actions.stream().anyMatch(a -> a.name.equals("help"))) {
                actions.forEach(action ->
                        System.out.println(action.help)
                );
            } else {
                for (var action : actions) {
                    switch (action.name()) {
                        case "clean" -> clean(action.args);
                        case "bld" -> build(action.args);
                        case "run" -> {
                            String backendName =action.get();
                            if ( getArtifact("hat-backend-" + backendName + "-1.0") instanceof Jar backend) {
                                String runnableName = action.get();
                                if ( getArtifact("hat-example-" + runnableName + "-1.0") instanceof Dependency.ExecutableJar runnable) {
                                    runnable.run(runnable.id().shortHyphenatedName() + ".Main", build(runnable, backend), args);
                                } else {
                                    System.out.println("Failed to find runnable ");
                                }
                            } else {
                                System.out.println("Failed to find 'hat-backend-" + backendName + "-1.0'");
                            }
                        }
                        default -> {
                        }
                    }
                }
            }

        }
    }


    public static class Jar extends DependencyImpl<Jar> implements Dependency.Buildable, Dependency.WithPath  {



        final Set<Path> exclude;

        private Jar(Project.Id id, Set<Path> exclude, Set<Dependency> dependencies) {
            super(id, dependencies);
            this.exclude = exclude;
            if (id.path != null && !Files.exists(id.path())) {
                System.err.println("The path does not exist: " + id.path());
            }
            if (!Files.exists(javaSourcePath())) {
                var jsp = javaSourcePath();
                System.out.println("Failed to find java source " + jsp + " path for " + id.shortHyphenatedName());
            }
            id.project.add(this);
        }

        public static Jar of(Project.Id id, Set<Path> exclude, Set<Dependency> dependencies) {
            return new Jar(id, exclude, dependencies);
        }

        public static Jar of(Project.Id id, Set<Dependency> dependencies) {
            return new Jar(id, Set.of(), dependencies);
        }

        public static Jar of(Project.Id id, Set<Path> exclude, Dependency... dependencies) {
            return of(id, exclude, Set.of(dependencies));
        }

        public static Jar of(Project.Id id, Dependency... dependencies) {
            return of(id, Set.of(), Set.of(dependencies));
        }

        public static class JavaSource extends SimpleJavaFileObject {
            Path path;

            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                try {
                    return Files.readString(Path.of(toUri()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            JavaSource(Path path) {
                super(path.toUri(), Kind.SOURCE);
                this.path = path;
            }
        }

        public Path jarFile() {
            return id().project().buildPath().resolve(id().fullHyphenatedName() + ".jar");
        }

        @Override
        public List<Path> generatedPaths() {
            throw new IllegalStateException("who called me");
        }


        @Override
        public boolean clean() {
            id().project().clean(null, classesDir(), jarFile());
            return true;
        }

        @Override
        public boolean build() {
            List<String> opts = new ArrayList<>(
                    List.of(
                            "--source=26",
                            "--enable-preview",
                            "--add-modules=jdk.incubator.code",
                            "--add-exports=jdk.incubator.code/jdk.incubator.code.dialect.java.impl=ALL-UNNAMED",
                            "-g",
                            "-d", classesDirName()
                    ));
            var deps = classPath(Project.Dag.processOrder(dependencies()));
            if (!deps.isEmpty()) {
                opts.addAll(List.of(
                        "--class-path=" + deps
                ));
            }
            opts.addAll(List.of(
                            "--source-path=" + javaSourcePathName()
                    )
            );



            JavaCompiler javac = ToolProvider.getSystemJavaCompiler();

            id().project().clean(this, classesDir());

            if (Files.exists(javaSourcePath())) {
                try (var files = Files.walk(javaSourcePath())) {
                    var listOfSources = files.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java") && !exclude.contains(p)).map(JavaSource::new).toList();
                    id().project().reporter.command(this, "javac " +
                            String.join(" ", opts) + " " + String.join(" ",
                            listOfSources.stream().map(JavaSource::getName).collect(Collectors.toList())));
                    id().project().reporter.progress(this, "java compiling "+listOfSources.size()+" files");

                    var diagnosticListener = new DiagnosticListener<JavaFileObject>() {
                        @Override
                        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                                id().project().reporter.error(Jar.this, diagnostic.toString());
                            } else if (diagnostic.getKind() == Diagnostic.Kind.WARNING) {
                                id().project().reporter.warning(Jar.this, diagnostic.toString());
                            } else if (diagnostic.getKind() == Diagnostic.Kind.MANDATORY_WARNING) {
                                id().project().reporter.warning(Jar.this, "!!" + diagnostic.toString());
                            } else if (diagnostic.getKind() == Diagnostic.Kind.NOTE) {
                                id().project().reporter.note(Jar.this, diagnostic.toString());
                            }else {
                                id().project().reporter.warning(Jar.this, diagnostic.getKind() + ":" + diagnostic.toString());
                            }
                        }
                    };
                    ((JavacTask) javac.getTask(
                            new PrintWriter(System.err),
                            javac.getStandardFileManager(diagnosticListener, null, null),
                            diagnosticListener,
                            opts,
                            null,
                            listOfSources
                    )).generate().forEach(gc ->
                            id.project.reporter.note(this, gc.getName())
                    );

                    List<Path> dirsToJar = new ArrayList<>(List.of(classesDir()));
                    if (Files.exists(javaResourcePath())) {
                        dirsToJar.add(javaResourcePath());
                    }
                    var jarStream = new JarOutputStream(Files.newOutputStream(jarFile()));


                    record RootAndPath(Path root, Path path) {
                    }
                    dirsToJar.forEach(r -> {
                        try {
                            id().project().reporter.command(this, "jar cvf " + jarFile()+ " " +
                                    String.join(dirsToJar.stream().map(Path::toString).collect(Collectors.joining(" "))));
                            id().project().reporter.progress(this, "creating jar " + jarFile());

                            Files.walk(r)
                                    .filter(p -> !Files.isDirectory(p))
                                    .map(p -> new RootAndPath(r, p))
                                    .sorted(Comparator.comparing(RootAndPath::path))
                                    .forEach(
                                            rootAndPath -> {
                                                try {
                                                    var entry = new JarEntry(rootAndPath.root.relativize(rootAndPath.path).toString());
                                                    entry.setTime(Files.getLastModifiedTime(rootAndPath.path()).toMillis());
                                                    jarStream.putNextEntry(entry);
                                                    Files.newInputStream(rootAndPath.path()).transferTo(jarStream);
                                                    jarStream.closeEntry();
                                                } catch (IOException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            });


                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

                    jarStream.finish();
                    jarStream.close();
                    return true;
                } catch (Exception e) {
                    //   println(e.getMessage());
                    throw new RuntimeException(e);
                }
            } else {
                return true;
            }
        }

        protected String classPath(Set<Dependency> dependencies) {
            return String.join(":", dependencies.stream().filter(p ->
                    p instanceof Jar).map(a -> (Jar) a).map(Jar::jarFileName).toList());
        }

        protected String classPathWithThisLast(Set<Dependency> dependencies) {
            Set<Dependency> all = new LinkedHashSet<>(dependencies);
            all.remove(this);
            all.add(this);
            return String.join(":", all.stream().filter(p ->
                    p instanceof Jar).map(a -> (Jar) a).map(Jar::jarFileName).toList());
        }

        private Path classesDir() {
            return id().project().buildPath().resolve(id().fullHyphenatedName() + ".classes");
        }

        private String classesDirName() {
            return classesDir().toString();
        }

        private String jarFileName() {
            return jarFile().toString();
        }

        private Path javaResourcePath() {
            return id().path().resolve("src/main/resources");

        }

        private String javaResourcePathName() {
            return javaResourcePath().toString();
        }

        private String javaSourcePathName() {
            return javaSourcePath().toString();
        }

        protected Path javaSourcePath() {
            return id().path().resolve("src/main/java");
        }
    }

    public static class RunnableJar extends Jar implements Dependency.ExecutableJar {


        private RunnableJar(Project.Id id, Set<Path> exclude, Set<Dependency> dependencies) {
            super(id, exclude, dependencies);
            id.project.add(this);
        }

        static public RunnableJar of(Project.Id id, Set<Path> exclude, Set<Dependency> dependencies) {
            return new RunnableJar(id, exclude, dependencies);
        }

        static public RunnableJar of(Project.Id id, Set<Path> exclude, Dependency... dependencies) {
            return of(id, exclude, Set.of(dependencies));
        }

        static public RunnableJar of(Project.Id id, Set<Dependency> dependencies) {
            return new RunnableJar(id, Set.of(), dependencies);
        }

        static public RunnableJar of(Project.Id id, Dependency... dependencies) {
            return of(id, Set.of(), Set.of(dependencies));
        }

        @Override
        public List<Path> generatedPaths() {
            throw new IllegalStateException("who called me");
        }

        @Override
        public boolean run(String mainClassName, Set<Dependency> depsInOrder, List<String> args) {

            List<String> opts = new ArrayList<>();
            String javaExecutablePath = ProcessHandle.current()
                    .info()
                    .command()
                    .orElseThrow();
            System.out.println("Using Java executable: " + javaExecutablePath);
            opts.addAll(List.of(
                    javaExecutablePath,
                    "--enable-preview",
                    "--enable-native-access=ALL-UNNAMED"));
            if (id().shortHyphenatedName().equals("nbody")) {
                opts.addAll(List.of(
                        "-XstartOnFirstThread"
                ));
            }
            opts.addAll(List.of(
                    "--add-exports=jdk.incubator.code/jdk.incubator.code.dialect.java.impl=ALL-UNNAMED", // for OpRenderer
                    "--class-path", classPathWithThisLast(depsInOrder),
                    "-Djava.library.path=" + id().project().buildPath,
                    mainClassName
            ));
            opts.addAll(args);
            id().project().reporter.command(this, String.join(" ", opts));
            id().project().reporter.progress(this, "running");
            try {
                var process = new ProcessBuilder().directory(id().project().rootPath().toFile()).redirectErrorStream(true).command(opts).start();
                process.waitFor();
                if (process.exitValue()!=0) {
                    System.out.println("Java failed to execute, is a valid java in your path ? " + id().fullHyphenatedName());
                }
                return process.exitValue() == 0;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public static class CMake extends DependencyImpl<CMake> implements Dependency.Buildable,Dependency.WithPath  {


        public static boolean isInPath() {
            try{
                var process =new ProcessBuilder().command("cmake", "--version").start();
                process.getInputStream().transferTo(System.out);
                process.getErrorStream().transferTo(System.err);
                process.waitFor();
                if (process.exitValue() != 0) {
                    System.out.println("CMake in path but exited with : " + process.exitValue());
                    return false;
                } else {
                    System.out.println("CMake in path ");
                    return true;
                }
            }catch (Exception e){
               // e.printStackTrace();
                System.out.println("No Cmake : " );
                return false;
            }
        }


        public boolean cmake(Consumer<String> lineConsumer, List<String> tailopts) {
            List<String> opts = new ArrayList<>();
            opts.add("cmake");
            opts.addAll(tailopts);
            boolean success;

            id.project().reporter.command(this,  String.join(" ", opts) );
            id.project().reporter.progress(this,  String.join(" ", opts) );
            try {
                var process = new ProcessBuilder()
                        .command(opts)
                        .redirectErrorStream(true)
                        // .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .start();
                process.waitFor();
                new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
                        .forEach(line -> {
                            lineConsumer.accept(line);
                            id().project().reporter.info(this,  line);
                        });
                success = (process.exitValue() == 0);

                if (!success) {
                    id().project().reporter.error(this, String.join(" ", opts));
                    throw new RuntimeException("CMake failed");
                }
               // cmakeProgres.cmakeInfo(this, "Done " + String.join(" ", opts));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return success;
        }

        @Override
        public List<Path> generatedPaths() {
            throw new IllegalStateException("who called me");
        }

        boolean cmake(Consumer<String> lineConsumer, String... opts) {
            return cmake(lineConsumer, List.of(opts));
        }

        public boolean cmakeInit(Consumer<String> lineConsumer) {
            return cmake(lineConsumer, "--fresh", "-DHAT_TARGET=" + id().project().buildPath(), "-B", cmakeBuildDir().toString(), "-S", cmakeSourceDir().toString());
        }

        public boolean cmakeBuildTarget(Consumer<String> lineConsumer, String target) {
            return cmake(lineConsumer, "--build", cmakeBuildDir().toString(), "--target", target);
        }

        public boolean cmakeBuild(Consumer<String> lineConsumer) {
            return cmake(lineConsumer, "--build", cmakeBuildDir().toString());
        }

        public boolean cmakeClean(Consumer<String> lineConsumer) {
            return cmakeBuildTarget(lineConsumer, "clean");
        }


        @Override
        public boolean build() {
            cmakeInit(_ -> {
            });
            cmakeBuild(_ -> {
            });
            return false;
        }

        @Override
        public boolean clean() {
            cmakeInit(_ -> {
            });
            cmakeClean(_ -> {
            });
            return false;
        }

        final Path cmakeSourceDir;
        final Path cmakeBuildDir;

        Path cmakeSourceDir() {
            return cmakeSourceDir;
        }

        Path cmakeBuildDir() {
            return cmakeBuildDir;
        }

        final Path CMakeLists_txt;

        protected CMake(Project.Id gsn, Path cmakeSourceDir, Set<Dependency> dependencies) {
            super(gsn, dependencies);
            if (id.path != null && !Files.exists(id.path())) {
                System.err.println("The path does not exist: " + id.path());
            }
            this.cmakeSourceDir = cmakeSourceDir;
            this.cmakeBuildDir = cmakeSourceDir.resolve("build");
            this.CMakeLists_txt = cmakeSourceDir.resolve("CMakeLists.txt");
        }

        protected CMake(Project.Id id, Set<Dependency> dependencies) {
            this(id, id.path(), dependencies);
        }

        public static CMake of(Project.Id id, Set<Dependency> dependencies) {
            return new CMake(id, dependencies);
        }

        public static CMake of(Project.Id id, Dependency... dependencies) {
            return of(id, Set.of(dependencies));
        }

    }


    public static class JExtract extends Jar {
        public static boolean isInPath() {
            try{
                var process =new ProcessBuilder().command("jextract", "--version").start();
                process.getInputStream().transferTo(System.out);
                process.getErrorStream().transferTo(System.err);
                process.waitFor();
                if (process.exitValue() != 0) {
                    System.out.println("No jextract : " + process.exitValue());
                    return false;
                } else {
                    System.out.println("CMake ok  " );
                    return true;
                }
            }catch (Exception e){
               // e.printStackTrace();
                System.out.println("No Jextract  : " );
                return false;
            }
        }

        @Override
        public Path javaSourcePath() {
            return id.path().resolve("src/main/java");
        }




        public interface ExtractSpec {
            Path header();

            List<Path> frameworks();
        }

        public record Mac(Path macSdkSysLibFrameWorks, Path macSysLibFrameWorks, Path header,
                          List<Path> frameworks) implements ExtractSpec {
            public static Mac of(CMakeInfo cMakeInfo, String... frameworks) {
                var value = (String) cMakeInfo.properties.get("CMAKE_C_IMPLICIT_LINK_FRAMEWORK_DIRECTORIES");
                Path macSdkSysLibFrameWorks = Path.of(value);
                Path macSysLibFrameWorks = Path.of("/System/Library/Frameworks");
                var firstName = frameworks[0];
                return new Mac(
                        macSdkSysLibFrameWorks,
                        macSysLibFrameWorks,
                        macSdkSysLibFrameWorks.resolve(firstName.toUpperCase() + ".framework/Headers/" + firstName + ".h"),
                        Stream.of(frameworks).map(s -> macSysLibFrameWorks.resolve(s + ".framework/" + s)).collect(Collectors.toList())
                );
            }

            void writeCompileFlags(Path outputDir) {
                try {
                    Path compileFLags = outputDir.resolve("compile_flags.txt");
                    Files.writeString(compileFLags, "-F" + macSdkSysLibFrameWorks + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        public record Linux(Path macSdkSysLibFrameWorks, Path macSysLibFrameWorks, Path header,
                            List<Path> frameworks) implements ExtractSpec {
            public static Mac of(CMakeInfo cMakeInfo, String... frameworks) {
                var value = (String) cMakeInfo.properties.get("");
                Path macSdkSysLibFrameWorks = Path.of(value);
                Path macSysLibFrameWorks = Path.of("/System/Library/Frameworks");
                var firstName = frameworks[0];
                return new Mac(
                        macSdkSysLibFrameWorks,
                        macSysLibFrameWorks,
                        macSdkSysLibFrameWorks.resolve(firstName.toUpperCase() + ".framework/Headers/" + firstName + ".h"),
                        Stream.of(frameworks).map(s -> macSysLibFrameWorks.resolve(s + ".framework/" + s)).collect(Collectors.toList())
                );
            }

            void writeCompileFlags(Path outputDir) {
                try {
                    Path compileFLags = outputDir.resolve("compile_flags.txt");
                    Files.writeString(compileFLags, "-F" + macSdkSysLibFrameWorks + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        @Override
        public boolean build() {
            try {
                id.project.mkdir(javaSourcePath());

                List<String> opts = new ArrayList<>(List.of());
                opts.addAll(List.of(
                        "jextract",
                      //  "/Users/grfrost/jextract-22/bin/jextract",
                        "--target-package", id().shortHyphenatedName(),
                        "--output", javaSourcePath().toString()
                ));
                spec.frameworks().forEach(library -> opts.addAll(List.of(
                        "--library", ":" + library
                )));
                opts.addAll(List.of(
                        "--header-class-name", id().shortHyphenatedName() + "_h",
                        spec.header().toString()
                ));
                if (spec instanceof Mac mac) {
                    mac.writeCompileFlags(id().project().rootPath);
                }
                boolean success;

                id().project().reporter.command(this, String.join(" ", opts));
                id().project().reporter.progress(this, String.join(" ", opts));
                try {
                    var process = new ProcessBuilder()
                            .command(opts)
                            .redirectErrorStream(true)
                            .start();
                    process.waitFor();
                    new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
                            .forEach(s -> id().project.reporter.warning(this, s));
                    success = (process.exitValue() == 0);
                    if (!success) {
                        id().project.reporter.error(this, "error " + process.exitValue());
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                super.build();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return false;
        }

        @Override
        public boolean clean() {
            // No opp
            return false;
        }

        final ExtractSpec spec;

        private JExtract(Project.Id id, ExtractSpec spec, Set<Path> exclude, Set<Dependency> dependencies) {
            super(id, exclude, dependencies);
            this.spec = spec;
            id.project.add(this);
        }

        static JExtract of(Project.Id id, ExtractSpec spec, Set<Path> exclude, Set<Dependency> dependencies) {
            return new JExtract(id, spec, exclude, dependencies);
        }

        static JExtract of(Project.Id id, ExtractSpec spec, Set<Path> exclude, Dependency... dependencies) {
            return of(id, spec, exclude, Set.of(dependencies));
        }

        static JExtract of(Project.Id id, ExtractSpec spec, Set<Dependency> dependencies) {
            return new JExtract(id, spec, Set.of(), dependencies);
        }

        public static JExtract of(Project.Id id, ExtractSpec spec, Dependency... dependencies) {
            return of(id, spec, Set.of(), Set.of(dependencies));
        }
    }


    public static abstract class CMakeInfo extends Job.CMake implements Job.Dependency.Optional {
       /* interface Mapper{

        }
        interface PathMapper extends Mapper{
            Path map(String value);
            PathMapper impl =  (s)->Path.of(s);
        }
        interface BooleanMapper extends Mapper{
            boolean map(String value);
            BooleanMapper impl =  (s)->Boolean.getBoolean(s);
        }
        interface StringMapper extends Mapper{
            String map(String value);
            StringMapper impl =  (s)->s;
        }*/

        Path asPath(String key) {
            return properties.containsKey(key) ? Path.of((String) properties.get(key)) : null;
        }

        boolean asBoolean(String key) {
            return properties.containsKey(key) && Boolean.parseBoolean((String) properties.get(key));
        }

        String asString(String key) {
            return (properties.containsKey(key) && properties.get(key) instanceof String s) ? s : null;
        }


        final String find;
        final String response;
        final static String template = """
                cmake_minimum_required(VERSION 3.22.1)
                project(extractions)
                find_package(__find__)
                get_cmake_property(_variableNames VARIABLES)
                foreach (_variableName ${_variableNames})
                   message(STATUS "${_variableName}=${${_variableName}}")
                endforeach()
                """;

        final String text;

        final Set<String> vars;
        Properties properties = new Properties();
        final Path propertiesPath;

        final Map<String, String> otherVarMap = new LinkedHashMap<>();
        final boolean available;

        CMakeInfo(Job.Project.Id id, String find, String response, Set<String> vars, Set<Job.Dependency> buildDependencies) {
            super(id, id.project().confPath().resolve("cmake-info").resolve(find), buildDependencies);
            this.find = find;
            this.response = response;
            this.vars = vars;
            this.text = template.replaceAll("__find__", find).replaceAll("__response__", response);
            this.propertiesPath = cmakeSourceDir().resolve("properties");
            if (Files.exists(propertiesPath)) {
                properties = new Properties();
                try {
                    properties.load(Files.newInputStream(propertiesPath));

                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                id.project().mkdir(cmakeBuildDir());
                try {
                    Files.writeString(CMakeLists_txt, this.text, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                    Pattern p = Pattern.compile("-- *([A-Za-z_0-9]+)=(.*)");
                    cmakeInit((line) -> {
                        if (p.matcher(line) instanceof Matcher matcher && matcher.matches()) {
                            //   System.out.println("GOT "+matcher.group(1)+"->"+matcher.group(2));
                            if (vars.contains(matcher.group(1))) {
                                properties.put(matcher.group(1), matcher.group(2));
                            } else {
                                otherVarMap.put(matcher.group(1), matcher.group(2));
                            }
                        } else {
                            // System.out.println("skipped " + line);
                        }
                    });
                    properties.store(Files.newOutputStream(propertiesPath), "A comment");
                } catch (IOException ioException) {
                    throw new IllegalStateException(ioException);
                }
            }
            available = asBoolean(response);
        }

        @Override
        public boolean isAvailable() {
            return available;
        }
    }

    public static class Mac extends DependencyImpl<Mac> implements Job.Dependency.Optional {
        final boolean available;

        public Mac(Job.Project.Id id, Set<Job.Dependency> buildDependencies) {
            super(id, buildDependencies);
            available = System.getProperty("os.name").toLowerCase().contains("mac");
        }

        @Override
        public boolean isAvailable() {
            return available;
        }
    }

    public static class Linux extends DependencyImpl<Linux> implements Job.Dependency.Optional {
        final boolean available;

        public Linux(Job.Project.Id id, Set<Job.Dependency> buildDependencies) {
            super(id, buildDependencies);
            available = System.getProperty("os.name").toLowerCase().contains("linux");
        }

        @Override
        public boolean isAvailable() {
            return available;
        }
    }

    public static class OpenGL extends Job.CMakeInfo {

        final Path glLibrary;

        public OpenGL(Job.Project.Id id, Set<Job.Dependency> buildDependencies) {
            super(id, "OpenGL", "OPENGL_FOUND", Set.of(
                    "OPENGL_FOUND",
                    "OPENGL_GLU_FOUND",
                    "OPENGL_gl_LIBRARY",
                    "OPENGL_glu_LIBRARY",
                    "OPENGL_INCLUDE_DIR",
                    "OPENGL_LIBRARIES",
                    "OPENGL_LIBRARY",
                    "OpenGL_FOUND",
                    "CMAKE_HOST_SYSTEM_NAME",
                    "CMAKE_HOST_SYSTEM_PROCESSOR",
                    "CMAKE_C_IMPLICIT_LINK_FRAMEWORK_DIRECTORIES"
            ), buildDependencies);
            glLibrary = asPath("OpenGL_glu_Library");
        }

    }

    public static class OpenCL extends Job.CMakeInfo {


        public OpenCL(Job.Project.Id id, Set<Job.Dependency> buildDependencies) {
            super(id, "OpenCL", "OPENCL_FOUND", Set.of(
                    "OPENCL_FOUND",
                    "CMAKE_HOST_SYSTEM_NAME",
                    "CMAKE_HOST_SYSTEM_PROCESSOR",
                    "CMAKE_C_IMPLICIT_LINK_FRAMEWORK_DIRECTORIES",
                    "OpenCL_FOUND",
                    "OpenCL_INCLUDE_DIRS",
                    "OpenCL_LIBRARY",
                    "OpenCL_VERSION_STRING"
            ), buildDependencies);

        }
    }

    public static class Cuda extends Job.CMakeInfo {
        public Cuda(Job.Project.Id id, Set<Job.Dependency> buildDependencies) {
            super(id, "CUDAToolkit", "CUDATOOLKIT_FOUND", Set.of(
                    "CUDATOOLKIT_FOUND",
                    "CUDA_OpenCL_LIBRARY",
                    "CUDA_cuFile_LIBRARY",
                    "CUDA_cuda_driver_LIBRARY",
                    "CUDA_cudart_LIBRARY",
                    "CUDAToolkit_BIN_DIR",
                    "CUDAToolkit_INCLUDE_DIRS",
                    "CUDAToolkit_NVCC_EXECUTABLE",
                    "CUDAToolkit_LIBRARY_DIR",
                    "CUDAToolkit_Version",
                    "CMAKE_HOST_SYSTEM_NAME",
                    "CMAKE_HOST_SYSTEM_PROCESSOR",
                    "CMAKE_C_IMPLICIT_LINK_FRAMEWORK_DIRECTORIES"

            ), buildDependencies);
        }
    }
}


