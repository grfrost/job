package job;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Project {
    public record Id(Project project, String fullHyphenatedName, String projectRelativeHyphenatedName,
                     String shortHyphenatedName, String version,
                     Path path) {
        String str() {
            return project.name() + " " + fullHyphenatedName + " " + projectRelativeHyphenatedName + " " + shortHyphenatedName + " " + version + " " + (path == null ? "null" : path);
        }

        static Id of(Project project, String projectRelativeHyphenatedName, String shortHyphenatedName, String version, Path path) {
            return new Id(project, project.name() + "-" + projectRelativeHyphenatedName + "-" + version, projectRelativeHyphenatedName, shortHyphenatedName, version, path);
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
                    id = Id.of(project, projectRelativeHyphenatedName, shortHyphenatedName, version, expectedPath);
                } else {
                    var midNames = Arrays.copyOfRange(tailNames, 0, tailNames.length);
                    var shortHyphenatedName = String.join("-", midNames);
                    id = Id.of(project, projectRelativeHyphenatedName, shortHyphenatedName, version, expectedPath);
                }
            }
        }
        return id;
    }

    public Id id(String id) {
        return id(this, id);
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
        artifacts.put(dependency.id().shortHyphenatedName, dependency);
        return dependency;
    }

    public Dependency get(String shortHyphenatedName) {
        return artifacts.get(shortHyphenatedName);
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
                    reporter.command(dependency, "rm -rf " + path);
                    mkdir(path);
                    reporter.command(dependency, "mkdir -p " + path);
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

    public Dag clean(Set<Dependency> dependencies) {
        boolean all = false;
        if (dependencies.isEmpty()) {
            all = true;
            dependencies = this.artifacts.values().stream().collect(Collectors.toSet());
        }
        Dag dag = new Dag(dependencies);
        dag.ordered().stream()
                .filter(d -> d instanceof Dependency.Buildable)
                .map(d -> (Dependency.Buildable) d)
                .forEach(Dependency.Buildable::clean);
        if (all) {
            rmdir(buildPath());
        }
        return dag;
    }

    public Dag clean(String... names) {
        return clean(Set.of(names).stream().map(s -> this.artifacts.get(s)).collect(Collectors.toSet()));
    }

    public Dag build(Dag dag) {
        dag.ordered().stream()
                .filter(d -> d instanceof Dependency.Buildable)
                .map(d -> (Dependency.Buildable) d)
                .forEach(Dependency.Buildable::build);
        return dag;
    }

    public Dag build(Set<Dependency> dependencies) {
        if (dependencies.isEmpty()) {
            dependencies = this.artifacts.values().stream().collect(Collectors.toSet());
        }
        Dag dag = new Dag(dependencies);
        build(dag);
        return dag;
    }

    public Dag build(Dependency... dependencies) {
        return build(Set.of(dependencies));
    }

    public Dag all() {
        return new Dag(new HashSet<>(this.artifacts.values()));
    }
}
