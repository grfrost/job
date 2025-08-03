package job;

import java.nio.file.Path;
import java.util.Set;

public class JExtract extends Jar {
    final JExtractOptProvider optProvider;

    private JExtract(Project.Id id, Set<Path> exclude, Set<Dependency> dependencies) {
        super(id, exclude, dependencies);
        // We expect the dependencies to include a JextractOptProvider
        var optionalProvider = dependencies.stream().filter(dep -> dep instanceof JExtractOptProvider).map(dep -> (JExtractOptProvider) dep).findFirst();
        this.optProvider = optionalProvider.orElseThrow();
        id.project().add(this);
    }

    @Override
    public Path javaSourcePath() {
        return id.path().resolve("src/main/java");
    }

    @Override
    public boolean build() {
        try {
            id.project().mkdir(javaSourcePath());
            var opts = ForkExec.Opts.of("jextract").add(
                    "--target-package", id().shortHyphenatedName(),
                    "--output", javaSourcePath().toString()
            );
            optProvider.jExtractOpts(opts);
            optProvider.writeCompilerFlags(id().project().rootPath());
            id().project().reporter.command(this, opts.toString());
            System.out.println(String.join(" ", opts.toString()));
            id().project().reporter.progress(this, "extracting");
            var result= ForkExec.forkExec(this, id.project().rootPath(),opts);
            result.stdErrAndOut().forEach((line)->{
                id().project().reporter.warning(this, line);
            });
            super.build();
            return result.status()==0;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean clean() {
        return false;
    }

    static public JExtract extract(Project.Id id, Set<Dependency> dependencies) {
        return new JExtract(id, Set.of(), dependencies);
    }

    static public JExtract extract(Project.Id id, Dependency... dependencies) {
        return new JExtract(id, Set.of(), Set.of(dependencies));
    }
}
