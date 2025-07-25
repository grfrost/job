package job;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
            List<String> opts = new ArrayList<>(List.of());
            opts.addAll(List.of(
                    "jextract",
                    "--target-package", id().shortHyphenatedName(),
                    "--output", javaSourcePath().toString()
            ));
            List<String> providerOpts = new ArrayList<>(List.of());
            if (optProvider != null) {
                var optProviderOpts = optProvider.jExtractOpts();
                providerOpts.addAll(optProviderOpts);
            }
            opts.addAll(providerOpts);
            optProvider.writeCompilerFlags(id().project().rootPath());
            boolean success;
            id().project().reporter.command(this, String.join(" ", opts));
            System.out.println(String.join(" ", opts));
            id().project().reporter.progress(this, "extracting");
            try {
                var process = new ProcessBuilder()
                        .command(opts)
                        .redirectErrorStream(true)
                        .start();
                process.waitFor();
                new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
                        .forEach(s -> {
                            id().project().reporter.warning(this, s);
                            System.err.println(s);
                        });
                success = (process.exitValue() == 0);
                if (!success) {
                    id().project().reporter.error(this, "error " + process.exitValue());
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

    static public JExtract extract(Project.Id id, Set<Dependency> dependencies) {
        return new JExtract(id, Set.of(), dependencies);
    }

    static public JExtract extract(Project.Id id, Dependency... dependencies) {
        return new JExtract(id, Set.of(), Set.of(dependencies));
    }
}
