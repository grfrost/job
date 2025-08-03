package job;

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
    public void jExtractOpts(ForkExec.Opts opts) {
        if (isAvailable()) {
            if (darwin) {
                opts.add(
                        "--library", ":/System/Library/Frameworks/OpenCL.framework/OpenCL",
                        "--header-class-name", "opencl_h",
                        fwk + "/OpenCL.framework/Headers/opencl.h"
                );
            } else if (linux) {
                opts.add(
                        "--library", asString("OpenCL_LIBRARY"),
                        "--include-dir","\"/usr/include/linux;/usr/include\"",
                        "--header-class-name", "opencl_h",
                        asString("OpenCL_INCLUDE_DIRS") + "/CL/opencl.h"
                );
            }
        }
    }
}
