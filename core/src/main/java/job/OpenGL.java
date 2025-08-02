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

public class OpenGL extends CMakeInfo {

    final Path glLibrary;

    public OpenGL(Project.Id id, Set<Dependency> buildDependencies) {
        super(id, "OpenGL", "OPENGL_FOUND", Set.of(
                "OPENGL_GLU_FOUND",
                "OPENGL_gl_LIBRARY",
                "OPENGL_glu_LIBRARY",
                "OPENGL_INCLUDE_DIR",
                "OPENGL_LIBRARIES",
                "OPENGL_LIBRARY"

        ), buildDependencies);
        glLibrary = asPath("OpenGL_glu_Library");
    }



    @Override
    public void jExtractOpts(List<String> opts) {
        if (isAvailable()) {
            if (darwin) {
                String glutLibName = "GLUT";
                Stream.of(glutLibName, "OpenGL")
                        .forEach(s -> opts.addAll(List.of("--library", ":/System/Library/Frameworks/" + s + ".framework/" + s)));
                opts.addAll(List.of(
                        "--header-class-name", "opengl_h",
                        fwk + "/" + glutLibName + ".framework/Headers/" + glutLibName + ".h"
                ));
            } else if (linux) {
                String[] libs = ((String) properties.get("OPENGL_LIBRARY")).split(";");
                Stream.of(libs).forEach(s -> opts.addAll(List.of("--library", ":" + s)));
                opts.add("--library");
                opts.add(":" + "/usr/lib/x86_64-linux-gnu/libglut.so");
                opts.add("--include-dir");
                opts.add("\"/usr/include/linux;/usr/include\"");
                opts.addAll(List.of("--header-class-name", "opengl_h", "/usr/include/GL/glut.h"));
            }
        }
    }

}
