package job;

import java.util.Set;

public class Mac extends DependencyImpl<Mac> implements Dependency.Optional {
    final boolean available;

    public Mac(Project.Id id) {
        super(id, Set.of());
        available = System.getProperty("os.name").toLowerCase().contains("mac");
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
