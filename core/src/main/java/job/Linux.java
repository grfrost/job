package job;

import java.util.Set;

public class Linux extends DependencyImpl<Linux> implements Dependency.Optional {
    final boolean available;
    public Linux(Project.Id id) {
        super(id, Set.of());
        available = System.getProperty("os.name").toLowerCase().contains("linux");
    }
    @Override
    public boolean isAvailable() {
        return available;
    }
}
