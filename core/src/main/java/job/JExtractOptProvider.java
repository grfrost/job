package job;

import java.nio.file.Path;
import java.util.List;

interface JExtractOptProvider {
    void jExtractOpts(List<String> opts);
     void writeCompilerFlags(Path outputDir);
}
