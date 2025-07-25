package job;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface Dependency {
    Project.Id id();

    Set<Dependency> dependencies();

    interface WithPath extends Dependency {
    }

    interface Buildable extends Dependency {
        boolean build();

        boolean clean();

        List<Path> generatedPaths();
    }

    interface Executable extends Dependency {
    }

    interface ExecutableJar extends Executable {
        boolean run(String mainClassName, Set<Dependency> depsInOrder, List<String> args);
    }

    interface Runnable extends Executable {
        boolean run();
    }

    interface Optional extends Dependency {
        boolean isAvailable();
    }
}
