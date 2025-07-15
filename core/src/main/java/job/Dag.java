package job;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Dag {
    static class Graph {
        Map<Job.Dependency, Set<Job.Dependency>> map = new LinkedHashMap<>();
        record Edge(Job.Dependency from, Job.Dependency to) {

        }
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
        Graph(Set<Job.Dependency> deps) {
            deps.forEach(this::recurse);
        }
        @Override public String toString(){
             StringBuilder sb = new StringBuilder();
            sb.append("strict digraph graphname {");
            edges.forEach(e-> sb
                    .append("   ")
                    .append('"')
                    .append(e.from.id().projectRelativeHyphenatedName())
                    .append("\" -> \n")
                    .append(e.to.id().projectRelativeHyphenatedName())
                    .append('"')
            );
            sb.append("}");
            return sb.toString();
        }
        Set<Job.Dependency> ordered(){
            Set<Job.Dependency> ordered = new LinkedHashSet<>();
            while (!map.isEmpty()) {
                var leaves = map.entrySet().stream()
                        .filter(e -> e.getValue().isEmpty())    // if this entry has zero dependencies
                        .map(Map.Entry::getKey)                 // get the key
                        .collect(Collectors.toSet());
                map.forEach((k, v) ->
                        leaves.forEach(v::remove)
                );
                leaves.forEach(leaf -> {
                    map.remove(leaf);
                    ordered.add(leaf);
                });
            }
            return ordered;
        }
    }
    public static Set<Job.Dependency> processOrder(Set<Job.Dependency> jars) {
        Graph graph = new Graph(jars);

        return graph.ordered();
    }

    public static Set<Job.Dependency> processOrder(Job.Dependency... dependencies) {
        return processOrder(Set.of(dependencies));
    }

    static Set<Job.Dependency> build(Set<Job.Dependency> jars) {
        var ordered = processOrder(jars);
        //   var unavailable =  ordered.stream().filter(d -> d instanceof Dependency.Optional optional && !optional.isAvailable()).findFirst();
        // if  (unavailable.isPresent()){
        //   System.out.println("dependencies contain optional and unavailable "+ unavailable.get().id().shortHyphenatedName);
        // return Set.of();
        // }else {
        //   System.out.println("No unavailable  dependencies ");
        ordered.stream().filter(d -> d instanceof Job.Dependency.Buildable).map(d -> (Job.Dependency.Buildable) d).forEach(
                Job.Dependency.Buildable::build
        );
        // }
        return ordered;
    }

    static Set<Job.Dependency> clean(Set<Job.Dependency> jars) {
        var ordered = processOrder(jars);
        ordered.stream().filter(d -> d instanceof Job.Dependency.Buildable).map(d -> (Job.Dependency.Buildable) d).forEach(Job.Dependency.Buildable::clean);
        return ordered;
    }

}
