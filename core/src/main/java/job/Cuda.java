package job;

import java.util.Set;

public class Cuda extends CMakeInfo {
    public Cuda(Project.Id id, Set<Dependency> buildDependencies) {
        super(id, "CUDAToolkit", "CUDATOOLKIT_FOUND", Set.of(
                "CUDATOOLKIT_FOUND",
                "CUDA_OpenCL_LIBRARY",
                "CUDA_cuFile_LIBRARY",
                "CUDA_cuda_driver_LIBRARY",
                "CUDA_cudart_LIBRARY",
                "CUDAToolkit_BIN_DIR",
                "CUDAToolkit_INCLUDE_DIRS",
                "CUDAToolkit_NVCC_EXECUTABLE",
                "CUDAToolkit_LIBRARY_DIR",
                "CUDAToolkit_Version",
                "CMAKE_HOST_SYSTEM_NAME",
                "CMAKE_HOST_SYSTEM_PROCESSOR",
                "CMAKE_C_IMPLICIT_LINK_FRAMEWORK_DIRECTORIES"

        ), buildDependencies);
    }
}
