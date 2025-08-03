package job;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CMake extends DependencyImpl<CMake> implements Dependency.Buildable, Dependency.WithPath {
    public ForkExec.Result cmake(Consumer<String> lineConsumer, List<String> tailopts) {
        ForkExec.Opts opts = ForkExec.Opts.of("cmake");
        tailopts.forEach(opts::add);
        id.project().reporter.command(this, opts.toString());
        id.project().reporter.progress(this, opts.toString());
        var result =  ForkExec.forkExec(this, id.project().rootPath(), opts);
        result.stdErrAndOut().forEach((line) -> {
            lineConsumer.accept(line);
            id().project().reporter.info(this, line);
        });

        if (result.status()!=0){
            id().project().reporter.error(this, opts.toString());
            throw new RuntimeException("CMake failed");
        }
        return result;
    }

    @Override
    public List<Path> generatedPaths() {
        throw new IllegalStateException("who called me");
    }

    ForkExec.Result cmake(Consumer<String> lineConsumer, String... opts) {
        return cmake(lineConsumer, List.of(opts));
    }

    public ForkExec.Result cmakeInit(Consumer<String> lineConsumer) {
        return cmake(lineConsumer, "--fresh", "-DHAT_TARGET=" + id().project().buildPath(), "-B", cmakeBuildDir().toString(), "-S", cmakeSourceDir().toString());
    }

    public ForkExec.Result cmakeBuildTarget(Consumer<String> lineConsumer, String target) {
        return cmake(lineConsumer, "--build", cmakeBuildDir().toString(), "--target", target);
    }

    public ForkExec.Result cmakeBuild(Consumer<String> lineConsumer) {
        return cmake(lineConsumer, "--build", cmakeBuildDir().toString());
    }

    public ForkExec.Result cmakeClean(Consumer<String> lineConsumer) {
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
