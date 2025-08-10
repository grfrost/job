package job;

import java.util.List;
import java.util.Set;

public class OpenGL extends CMakeInfo {

   // final Path glLibrary;

    public OpenGL(Project.Id id, Set<Dependency> buildDependencies) {
        super(id, "OpenGL", "OPENGL_FOUND", Set.of(
                "OPENGL_GLU_FOUND",
                "OPENGL_gl_LIBRARY",
                "OPENGL_glu_LIBRARY",
                "OPENGL_INCLUDE_DIR",
                "OPENGL_LIBRARIES",
                "OPENGL_LIBRARY"

        ), buildDependencies);
    }
    public OpenGL(Project.Id id, Dependency ...dependencies) {
        this(id, Set.of(dependencies));
    }

    @Override
    public void jExtractOpts(ForkExec.Opts opts) {
        if (isAvailable()) {
            if (darwin) {
                List.of("GLUT", "OpenGL").forEach(s -> opts.add("--library", ":/System/Library/Frameworks/" + s + ".framework/" + s));
                opts.add("--header-class-name", "opengl_h", fwk + "/GLUT.framework/Headers/GLUT.h");
            } else if (linux) {
                asSemiSeparatedStringList("OPENGL_LIBRARY").forEach(lib -> opts.add("--library", ":" + lib));
                opts.add(
                        "--library",":/usr/lib/x86_64-linux-gnu/libglut.so",
                        "--include-dir","\"/usr/include/linux;/usr/include\"",
                        "--header-class-name", "opengl_h", "/usr/include/GL/glut.h"
                );
            }
        }
    }

}
