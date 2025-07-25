package job;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Dag {
        record DotBuilder(Consumer<String> consumer){
           // https://graphviz.org/doc/info/lang.html
            static DotBuilder of(Consumer<String> stringConsumer, Consumer<DotBuilder> builderConsumer){
                DotBuilder db = new DotBuilder(stringConsumer);
                db.append("strict digraph graphname {").append("\n");
                db.append("   node [shape=record];\n");
                builderConsumer.accept(db);
                db.append("\n}");
                return db;
            }
            DotBuilder append(String s){
                consumer.accept(s);
                return this;
            }
            DotBuilder quoted(String s){
                append("\"").append(s).append("\"");
                return this;
            }
            DotBuilder node(String n, String label){
                return append("\n   ").quoted(n).append("[").append("label").append("=").quoted(label).append("]").append(";");
            }
            DotBuilder edge(String from, String to){
                return append("\n   ").quoted(from).append("->").quoted(to).append(";");
            }
        }
        Map<Job.Dependency, Set<Job.Dependency>> map = new LinkedHashMap<>();
        record Edge(Job.Dependency from, Job.Dependency to) {}
        List<Edge> edges = new ArrayList<>();
         public void recurse( Job.Dependency from) {
            var set = map.computeIfAbsent(from, _ -> new LinkedHashSet<>());
            var deps = from.dependencies();
            deps.forEach(dep -> {
                edges.add(new Edge(from, dep));
                set.add(dep);
                recurse( dep);
            });
        }
        public Dag(Set<Job.Dependency> deps) {
            deps.forEach(this::recurse);
        }
        public Dag(Job.Dependency ...deps) {
             this(Stream.of(deps).collect(Collectors.toSet()));
        }

        public String toDot(){
            StringBuilder sb = new StringBuilder();
            DotBuilder.of(sb::append, db-> {
                map.keySet().forEach(k -> {
                    db.node(k.id().projectRelativeHyphenatedName(), k.id().projectRelativeHyphenatedName());
                });
                edges.forEach(e ->
                        db.edge(e.from.id().projectRelativeHyphenatedName(), e.to.id().projectRelativeHyphenatedName())
                );
            });
            return sb.toString();
        }
        public Set<Job.Dependency> ordered(){
            Set<Job.Dependency> ordered = new LinkedHashSet<>();
            while (!map.isEmpty()) {
                var leaves = map.entrySet().stream()
                        .filter(e -> e.getValue().isEmpty())    // if this entry has zero dependencies
                        .map(Map.Entry::getKey)                 // get the key
                        .collect(Collectors.toSet());
                map.values().forEach(v -> leaves.forEach(v::remove));
                leaves.forEach(leaf -> {
                    map.remove(leaf);
                    ordered.add(leaf);
                });
            }
            return ordered;
        }

    public Dag available(){
        var ordered = this.ordered();
        Set<Job.Dependency> unavailable = ordered.stream().filter(
                d -> {
                    if (d instanceof Job.Dependency.Optional opt) {
                       return !opt.isAvailable();
                    }else{
                        return false;
                    }
                })
                .collect(Collectors.toSet());

        boolean changed = true;
        while (changed) {
            changed = false;
            for(Job.Dependency dep : ordered) {
                if (!changed) {
                    var optionalDependsOnUnavailable = dep.dependencies().stream().filter(d ->
                            unavailable.contains(d) || d instanceof Job.Dependency.Optional o && !o.isAvailable()).findFirst();
                    if (optionalDependsOnUnavailable.isPresent()) {
                        changed = true;
                        unavailable.add(dep);
                        ordered.remove(dep);
                        break;
                    }
                }
            }
        }
        return new Dag(ordered);
    }


}
