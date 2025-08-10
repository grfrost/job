package job;

import java.util.Set;

public class Opt extends DependencyImpl<Opt> implements Dependency.Optional {
    final boolean available;

    public Opt(Project.Id id, boolean available, Set<Dependency> buildDependencies) {
        super(id, buildDependencies);
        this.available = available;
    }
    public Opt(Project.Id id, boolean available, Dependency ... dependencies) {
        super(id, Set.of(dependencies));
        this.available = available;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
