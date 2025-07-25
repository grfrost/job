package job;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class CMakeInfo extends CMake implements Dependency.Optional {

    Path asPath(String key) {
        return properties.containsKey(key) ? Path.of((String) properties.get(key)) : null;
    }

    boolean asBoolean(String key) {
        return properties.containsKey(key) && Boolean.parseBoolean((String) properties.get(key));
    }

    String asString(String key) {
        return (properties.containsKey(key) && properties.get(key) instanceof String s) ? s : null;
    }


    final String find;
    final String response;
    final static String template = """
            cmake_minimum_required(VERSION 3.22.1)
            project(extractions)
            find_package(__find__)
            get_cmake_property(_variableNames VARIABLES)
            foreach (_variableName ${_variableNames})
               message(STATUS "${_variableName}=${${_variableName}}")
            endforeach()
            """;

    final String text;

    final Set<String> vars;
    Properties properties = new Properties();
    final Path propertiesPath;

    final Map<String, String> otherVarMap = new LinkedHashMap<>();
    final boolean available;

    CMakeInfo(Project.Id id, String find, String response, Set<String> vars, Set<Dependency> buildDependencies) {
        super(id, id.project().confPath().resolve("cmake-info").resolve(find), buildDependencies);
        this.find = find;
        this.response = response;
        this.vars = vars;
        this.text = template.replaceAll("__find__", find).replaceAll("__response__", response);
        this.propertiesPath = cmakeSourceDir().resolve("properties");
        if (Files.exists(propertiesPath)) {
            properties = new Properties();
            try {
                properties.load(Files.newInputStream(propertiesPath));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            id.project().mkdir(cmakeBuildDir());
            try {
                Files.writeString(CMakeLists_txt, this.text, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                Pattern p = Pattern.compile("-- *([A-Za-z_0-9]+)=(.*)");
                cmakeInit((line) -> {
                    if (p.matcher(line) instanceof Matcher matcher && matcher.matches()) {
                        //   System.out.println("GOT "+matcher.group(1)+"->"+matcher.group(2));
                        if (vars.contains(matcher.group(1))) {
                            properties.put(matcher.group(1), matcher.group(2));
                        } else {
                            otherVarMap.put(matcher.group(1), matcher.group(2));
                        }
                    } else {
                        // System.out.println("skipped " + line);
                    }
                });
                properties.store(Files.newOutputStream(propertiesPath), "A comment");
            } catch (IOException ioException) {
                throw new IllegalStateException(ioException);
            }
        }
        available = asBoolean(response);
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
