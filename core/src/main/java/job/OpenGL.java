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

public class OpenGL extends CMakeInfo implements JExtractOptProvider {

    final Path glLibrary;

    public OpenGL(Project.Id id, Set<Dependency> buildDependencies) {
        super(id, "OpenGL", "OPENGL_FOUND", Set.of(
                "OPENGL_FOUND",
                "OPENGL_GLU_FOUND",
                "OPENGL_gl_LIBRARY",
                "OPENGL_glu_LIBRARY",
                "OPENGL_INCLUDE_DIR",
                "OPENGL_LIBRARIES",
                "OPENGL_LIBRARY",
                "OpenGL_FOUND",
                "CMAKE_HOST_SYSTEM_NAME",
                "CMAKE_HOST_SYSTEM_PROCESSOR",
                "CMAKE_C_IMPLICIT_LINK_FRAMEWORK_DIRECTORIES"
        ), buildDependencies);
        glLibrary = asPath("OpenGL_glu_Library");
    }

    @Override
    public void writeCompilerFlags(Path outputDir) {
        String sysName = (String) properties.get("CMAKE_HOST_SYSTEM_NAME");
        if (sysName.equals("Darwin")) {
            try {
                Path compileFLags = outputDir.resolve("compile_flags.txt");
                //System.out.println("Creating "+compileFLags.toAbsolutePath());
                Files.writeString(compileFLags, "-F" + properties.get("CMAKE_C_IMPLICIT_LINK_FRAMEWORK_DIRECTORIES") + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public List<String> jExtractOpts() {
        if (isAvailable()) {
            String sysName = (String) properties.get("CMAKE_HOST_SYSTEM_NAME");
            if (sysName.equals("Darwin")) {
                String glutLibName = "GLUT";

                List<String> opts = new ArrayList<>(List.of());
                Stream.of(glutLibName, "OpenGL")
                        .forEach(s ->
                                opts.addAll(
                                        List.of("--library", ":/System/Library/Frameworks/" + s + ".framework/" + s)
                                )
                        );
                var fwk = ((String) properties.get("CMAKE_C_IMPLICIT_LINK_FRAMEWORK_DIRECTORIES"));
                //   System.out.println("fwk = '"+fwk+"'");
                opts.addAll(
                        List.of(
                                "--header-class-name", "opengl_h",
                                fwk + "/" + glutLibName + ".framework/Headers/" + glutLibName + ".h"

                        )
                );
                return opts;
            } else if (sysName.equals("Linux")) {
                List<String> opts = new ArrayList<>(List.of());
                String[] libs = ((String) properties.get("OPENGL_LIBRARY")).split(";");
                Stream.of(libs)
                        .forEach(s ->
                                opts.addAll(
                                        List.of("--library", ":" + s)
                                )
                        );
                opts.add("--library");
                opts.add(":" + "/usr/lib/x86_64-linux-gnu/libglut.so");
                opts.add("--include-dir");
                opts.add("\"/usr/include/linux;/usr/include\"");
                var fwk = ((String) properties.get("CMAKE_C_IMPLICIT_LINK_FRAMEWORK_DIRECTORIES"));
                //   System.out.println("fwk = '"+fwk+"'");
                opts.addAll(
                        List.of(
                                "--header-class-name", "opengl_h", "/usr/include/GL/glut.h"
                                //         fwk + "/"+glutLibName + ".framework/Headers/" + glutLibName + ".h"

                        )
                );
                return opts;
            }

        }
        return null;
    }

}
