package job;

import com.sun.source.util.JavacTask;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public class Jar extends DependencyImpl<Jar> implements Dependency.Buildable, Dependency.WithPath, Dependency.ExecutableJar {
    final Set<Path> exclude;

    protected Jar(Project.Id id, Set<Path> exclude, Set<Dependency> dependencies) {
        super(id, dependencies);
        this.exclude = exclude;
        if (id.path() != null && !Files.exists(id.path())) {
            System.err.println("The path does not exist: " + id.path());
        }
        if (!Files.exists(javaSourcePath())) {
            var jsp = javaSourcePath();
            System.out.println("Failed to find java source " + jsp + " path for " + id.shortHyphenatedName());
        }
        id.project().add(this);
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
        Dag dag = new Dag(dependencies());
        var deps = classPath(dag.ordered());
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
                        } else {
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
                        id.project().reporter.note(this, gc.getName())
                );

                List<Path> dirsToJar = new ArrayList<>(List.of(classesDir()));
                if (Files.exists(javaResourcePath())) {
                    dirsToJar.add(javaResourcePath());
                }
                var jarStream = new JarOutputStream(Files.newOutputStream(jarFile()));


                record RootAndPath(Path root, Path path) {
                }
                id().project().reporter.command(this, "jar cvf " + jarFile() + " " +
                        String.join(dirsToJar.stream().map(Path::toString).collect(Collectors.joining(" "))));
                id().project().reporter.progress(this, "compiled " + listOfSources.size() + " file" + (listOfSources.size() > 1 ? "s" : "") + " to " + jarFile().getFileName());

                dirsToJar.forEach(r -> {
                    try {

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
        // FIX this we need top pass opts to run!
        if (id().shortHyphenatedName().equals("nbody") && System.getProperty("os.name").toLowerCase().contains("mac")) {
            opts.addAll(List.of(
                    "-XstartOnFirstThread"
            ));
        }
        opts.addAll(List.of(
                "--add-exports=jdk.incubator.code/jdk.incubator.code.dialect.java.impl=ALL-UNNAMED", // for OpRenderer
                "--class-path", classPathWithThisLast(depsInOrder),
                "-Djava.library.path=" + id().project().buildPath(),
                mainClassName
        ));
        opts.addAll(args);
        id().project().reporter.command(this, String.join(" ", opts));
        System.out.println(String.join(" ", opts));
        id().project().reporter.progress(this, "running");
        try {
            var process = new ProcessBuilder().directory(id().project().rootPath().toFile()).redirectErrorStream(true).command(opts).start();
            process.waitFor();
            if (process.exitValue() != 0) {
                System.out.println("Java failed to execute, is a valid java in your path ? " + id().fullHyphenatedName());
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
