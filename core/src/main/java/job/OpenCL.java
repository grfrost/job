package job;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class OpenCL extends CMakeInfo implements JExtractOptProvider {
    public OpenCL(Project.Id id, Set<Dependency> buildDependencies) {
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

    @Override
    public void writeCompilerFlags(Path outputDir) {
        String sysName = (String) properties.get("CMAKE_HOST_SYSTEM_NAME");
        if (sysName.equals("Darwin")) {
            try {
                Path compileFLags = outputDir.resolve("compile_flags.txt");
                //System.out.println("Creating " + compileFLags.toAbsolutePath());
                Files.writeString(compileFLags, "-F" + properties.get("CMAKE_C_IMPLICIT_LINK_FRAMEWORK_DIRECTORIES") + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public List<String> jExtractOpts() {
        if (isAvailable()) {
            String libName = "OpenCL";
            String sysName = (String) properties.get("CMAKE_HOST_SYSTEM_NAME");
            if (sysName.equals("Darwin")) {
                List<String> opts = new ArrayList<>(List.of());
                Stream.of(libName)
                        .forEach(s ->
                                opts.addAll(
                                        List.of("--library", ":/System/Library/Frameworks/" + s + ".framework/" + s)
                                )
                        );
                var fwk = ((String) properties.get("CMAKE_C_IMPLICIT_LINK_FRAMEWORK_DIRECTORIES"));
                // System.out.println("fwk = '"+fwk+"'");
                opts.addAll(
                        List.of(
                                "--header-class-name", "opencl_h",
                                fwk + "/" + libName + ".framework/Headers/opencl.h"
                        )
                );
                return opts;
            } else if (sysName.equals("Linux")) {
                List<String> opts = new ArrayList<>(List.of());
                Stream.of(libName)
                        .forEach(s ->
                                opts.addAll(
                                        List.of("--library", (String) properties.get("OpenCL_LIBRARY"))
                                )
                        );
                opts.add("--include-dir");
                opts.add("\"/usr/include/linux;/usr/include\"");
                var fwk = ((String) properties.get("CMAKE_C_IMPLICIT_LINK_FRAMEWORK_DIRECTORIES"));
                // System.out.println("fwk = '"+fwk+"'");
                opts.addAll(
                        List.of(
                                "--header-class-name", "opencl_h", (String) properties.get("OpenCL_INCLUDE_DIRS") + "/CL/opencl.h"
                        )
                );
                return opts;
            }

        }
        return null;
    }
}
