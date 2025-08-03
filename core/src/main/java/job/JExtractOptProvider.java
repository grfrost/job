package job;

import java.nio.file.Path;

interface JExtractOptProvider {
    void jExtractOpts(ForkExec.Opts opts);
     void writeCompilerFlags(Path outputDir);
}
