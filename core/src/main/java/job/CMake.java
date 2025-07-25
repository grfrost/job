package job;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CMake extends DependencyImpl<CMake> implements Dependency.Buildable, Dependency.WithPath {



    public boolean cmake(Consumer<String> lineConsumer, List<String> tailopts) {
        List<String> opts = new ArrayList<>();
        opts.add("cmake");
        opts.addAll(tailopts);
        boolean success;

        id.project().reporter.command(this, String.join(" ", opts));
        id.project().reporter.progress(this, "cmake " + tailopts.getFirst());
        try {
            var process = new ProcessBuilder()
                    .command(opts)
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
            new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
                    .forEach(line -> {
                        lineConsumer.accept(line);
                        id().project().reporter.info(this, line);
                    });
            success = (process.exitValue() == 0);

            if (!success) {
                id().project().reporter.error(this, String.join(" ", opts));
                throw new RuntimeException("CMake failed");
            }
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
        if (id.path() != null && !Files.exists(id.path())) {
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
