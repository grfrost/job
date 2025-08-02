package job;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OpenCL extends CMakeInfo {
    public OpenCL(Project.Id id, Set<Dependency> buildDependencies) {
        super(id, "OpenCL", "OPENCL_FOUND", Set.of(
                "OPENCL_FOUND",
                "OpenCL_FOUND",
                "OpenCL_INCLUDE_DIRS",
                "OpenCL_LIBRARY",
                "OpenCL_VERSION_STRING"
        ), buildDependencies);
    }

    @Override
    public void jExtractOpts(List<String> opts) {
        if (isAvailable()) {
            if (darwin) {
                opts.addAll(List.of(
                        "--library", ":/System/Library/Frameworks/OpenCL.framework/OpenCL",
                        "--header-class-name", "opencl_h", fwk + "/OpenCL.framework/Headers/opencl.h"
                ));
            } else if (linux) {
                opts.addAll(List.of(
                        "--library", asString("OpenCL_LIBRARY"),
                        "--include-dir","\"/usr/include/linux;/usr/include\"",
                        "--header-class-name", "opencl_h",
                        asString("OpenCL_INCLUDE_DIRS") + "/CL/opencl.h"
                ));
            }
        }
    }
}
