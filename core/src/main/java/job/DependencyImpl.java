package job;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class DependencyImpl<T extends DependencyImpl<T>> implements Dependency {
    protected final Project.Id id;

    @Override
    public Project.Id id() {
        return id;
    }

    final private Set<Dependency> dependencies = new LinkedHashSet<>();

    @Override
    public Set<Dependency> dependencies() {
        return dependencies;
    }

    DependencyImpl(Project.Id id, Set<Dependency> dependencies) {
        this.id = id;
        this.dependencies.addAll(dependencies);
    }
}
