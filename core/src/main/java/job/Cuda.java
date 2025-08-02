package job;

import java.util.List;
import java.util.Set;

public class Cuda extends CMakeInfo {
    public Cuda(Project.Id id, Set<Dependency> buildDependencies) {
        super(id, "CUDAToolkit", "CUDATOOLKIT_FOUND", Set.of(
                "CUDA_OpenCL_LIBRARY",
                "CUDA_cuFile_LIBRARY",
                "CUDA_cuda_driver_LIBRARY",
                "CUDA_cudart_LIBRARY",
                "CUDAToolkit_BIN_DIR",
                "CUDAToolkit_INCLUDE_DIRS",
                "CUDAToolkit_NVCC_EXECUTABLE",
                "CUDAToolkit_LIBRARY_DIR",
                "CUDAToolkit_Version"
        ), buildDependencies);
    }

    @Override
    public void jExtractOpts(List<String> opts) {

    }
}
