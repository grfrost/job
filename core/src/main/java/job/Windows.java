package job;

import java.util.Set;

public class Windows extends DependencyImpl<Linux> implements Dependency.Optional {
    final boolean available;

    public Windows(Project.Id id, Set<Dependency> buildDependencies) {
        super(id, buildDependencies);
        available = System.getProperty("os.name").toLowerCase().contains("windows");
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
