package job;

import java.nio.file.Path;
import java.util.List;

interface JExtractOptProvider {
    List<String> jExtractOpts();

    default void writeCompilerFlags(Path outputDir) {
    } // hack for mac
}
