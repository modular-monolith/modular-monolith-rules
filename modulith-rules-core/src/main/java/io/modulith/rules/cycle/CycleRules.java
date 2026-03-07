package io.modulith.rules.cycle;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory for ArchUnit rules that detect dependency cycles between modules.
 *
 * <p>Cycle rules verify that module dependencies form a directed acyclic graph. Cycles
 * between modules make the codebase harder to understand, test, and evolve independently.
 *
 * <p>Rules are created from a {@link ModulithRuleSet} that describes the module layout.
 * Obtain an instance via {@link io.modulith.rules.ModulithRules#cycleRules()}.
 */
public final class CycleRules {

    private final ModulithRuleSet ruleSet;

    /**
     * Creates a new {@code CycleRules} factory for the given rule set.
     *
     * @param ruleSet the module registry describing the architecture under test
     */
    public CycleRules(ModulithRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    /**
     * Returns a rule that verifies no circular dependencies exist between any of the
     * defined modules. The rule accumulates a directed dependency graph from actual
     * class-level dependencies and runs DFS-based cycle detection after all classes
     * have been inspected.
     *
     * <p>Violation message format:
     * <pre>
     * Circular module dependency detected: ordering -> inventory -> ordering
     * </pre>
     *
     * @return an ArchUnit rule detecting inter-module dependency cycles
     */
    public ArchRule noModuleCycles() {
        return ArchRuleDefinition.classes()
                .that(resideInAnyDefinedModule())
                .should(buildCycleCondition("not participate in circular module dependencies", null));
    }

    /**
     * Returns a rule that verifies the named module does not participate in any
     * circular dependency with other defined modules. The rule still inspects all
     * classes in defined modules to build the full dependency graph, but only reports
     * cycles that include the specified module.
     *
     * @param moduleName the module whose cycle participation should be checked
     * @return an ArchUnit rule scoped to cycles involving the named module
     * @throws IllegalArgumentException if no module with the given name exists
     */
    public ArchRule moduleHasNoCycles(String moduleName) {
        ruleSet.module(moduleName);
        return ArchRuleDefinition.classes()
                .that(resideInAnyDefinedModule())
                .should(buildCycleCondition(
                        "not participate in circular dependencies involving module '" + moduleName + "'",
                        moduleName));
    }

    /**
     * Returns a rule that verifies no circular dependencies exist between modules.
     * Delegates to {@link #noModuleCycles()}.
     *
     * @return an ArchUnit rule detecting inter-module dependency cycles
     */
    public ArchRule noCircularDependenciesBetweenModules() {
        return noModuleCycles();
    }

    /**
     * Detects all cycles in the given directed graph using depth-first search.
     * Each returned inner list represents one cycle, with the starting node repeated
     * at the end to make the cycle explicit (e.g., {@code [A, B, C, A]}).
     *
     * @param graph a map from module name to the set of module names it depends on
     * @return a list of cycles, where each cycle is an ordered list of node names
     */
    public static List<List<String>> detectCycles(Map<String, Set<String>> graph) {
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();
        Deque<String> pathStack = new ArrayDeque<>();
        List<List<String>> cycles = new ArrayList<>();

        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                dfs(node, graph, visited, inStack, pathStack, cycles);
            }
        }
        return cycles;
    }

    /**
     * Performs a recursive DFS from the given node, tracking the current path on
     * {@code pathStack} and the recursion stack in {@code inStack}. When a back edge
     * is detected (a neighbor already on the recursion stack), the cycle is extracted
     * from the path stack and added to {@code cycles}.
     *
     * @param node      the current node being explored
     * @param graph     the directed dependency graph
     * @param visited   nodes that have been fully explored
     * @param inStack   nodes currently on the DFS recursion stack
     * @param pathStack ordered path of nodes from the DFS root to the current node
     * @param cycles    accumulator for detected cycles
     */
    private static void dfs(
            String node,
            Map<String, Set<String>> graph,
            Set<String> visited,
            Set<String> inStack,
            Deque<String> pathStack,
            List<List<String>> cycles) {

        visited.add(node);
        inStack.add(node);
        pathStack.push(node);

        Set<String> neighbors = graph.getOrDefault(node, Collections.emptySet());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                dfs(neighbor, graph, visited, inStack, pathStack, cycles);
            } else if (inStack.contains(neighbor)) {
                List<String> pathList = new ArrayList<>(pathStack);
                Collections.reverse(pathList);
                int startIndex = pathList.indexOf(neighbor);
                if (startIndex >= 0) {
                    List<String> cycle = new ArrayList<>(pathList.subList(startIndex, pathList.size()));
                    cycle.add(neighbor);
                    cycles.add(cycle);
                }
            }
        }

        pathStack.pop();
        inStack.remove(node);
    }

    /**
     * Creates a canonical string key for a cycle so that the same cycle reported
     * from different starting nodes is recognized as a duplicate. The key is the
     * lexicographically smallest rotation of the cycle's node names joined by
     * {@code " -> "}.
     *
     * <p>For example, both {@code [A, B, C, A]} and {@code [B, C, A, B]} produce
     * the same key.
     *
     * @param cycle an ordered list of module names ending with the starting node repeated
     * @return a canonical string representing the cycle, independent of starting point
     */
    public String normalizeCycleKey(List<String> cycle) {
        if (cycle == null || cycle.size() < 2) {
            return cycle == null ? "" : String.join(" -> ", cycle);
        }
        List<String> nodes = cycle.subList(0, cycle.size() - 1);
        int n = nodes.size();
        String smallest = null;
        for (int i = 0; i < n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0) {
                    sb.append(",");
                }
                sb.append(nodes.get((i + j) % n));
            }
            String rotation = sb.toString();
            if (smallest == null || rotation.compareTo(smallest) < 0) {
                smallest = rotation;
            }
        }
        return smallest;
    }

    /**
     * Builds an {@link ArchCondition} that accumulates a module-level dependency graph
     * during {@code check()} and runs cycle detection in {@code finish()}. Violations
     * are reported once per unique cycle.
     *
     * @param description    the human-readable description of the condition
     * @param filterModule   if non-null, only report cycles involving this module name
     * @return an ArchCondition that detects module dependency cycles
     */
    private ArchCondition<JavaClass> buildCycleCondition(String description, String filterModule) {
        return new ArchCondition<JavaClass>(description) {

            private final Map<String, Set<String>> graph = new HashMap<>();
            private final Set<String> reportedCycleKeys = new HashSet<>();

            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                Optional<ModuleDefinition> sourceModuleOpt = findModuleOf(javaClass);
                if (!sourceModuleOpt.isPresent()) {
                    return;
                }
                String sourceModuleName = sourceModuleOpt.get().name();
                for (JavaClass dependency : directDependencyTargets(javaClass)) {
                    Optional<ModuleDefinition> targetModuleOpt = findModuleOf(dependency);
                    if (!targetModuleOpt.isPresent()) {
                        continue;
                    }
                    String targetModuleName = targetModuleOpt.get().name();
                    if (!sourceModuleName.equals(targetModuleName)) {
                        graph.computeIfAbsent(sourceModuleName, k -> new HashSet<>())
                                .add(targetModuleName);
                    }
                }
            }

            @Override
            public void finish(ConditionEvents events) {
                List<List<String>> cycles = detectCycles(graph);
                for (List<String> cycle : cycles) {
                    if (filterModule != null && !cycle.contains(filterModule)) {
                        continue;
                    }
                    String key = normalizeCycleKey(cycle);
                    if (reportedCycleKeys.add(key)) {
                        String cycleStr = String.join(" -> ", cycle);
                        String message = "Circular module dependency detected: " + cycleStr;
                        events.add(SimpleConditionEvent.violated(cycleStr, message));
                    }
                }
            }
        };
    }

    /**
     * Returns a predicate matching any class that resides in a package belonging to
     * one of the defined modules, using ArchUnit package wildcard identifiers.
     *
     * @return a described predicate for classes inside any defined module
     */
    private DescribedPredicate<JavaClass> resideInAnyDefinedModule() {
        String[] packages = ruleSet.allModules().stream()
                .map(ModuleDefinition::archUnitPackageIdentifier)
                .toArray(String[]::new);
        return JavaClass.Predicates.resideInAnyPackage(packages);
    }

    /**
     * Finds the {@link ModuleDefinition} that contains the given class, by checking
     * each registered module's {@code containsClass} method.
     *
     * @param javaClass the class to locate
     * @return an {@code Optional} containing the owning module, or empty if none matches
     */
    private Optional<ModuleDefinition> findModuleOf(JavaClass javaClass) {
        return ruleSet.allModules().stream()
                .filter(m -> m.containsClass(javaClass.getName()))
                .findFirst();
    }

    /**
     * Returns the set of classes that the given class directly depends on.
     *
     * @param javaClass the class whose direct dependencies are collected
     * @return set of target classes from all direct dependencies
     */
    private Set<JavaClass> directDependencyTargets(JavaClass javaClass) {
        return javaClass.getDirectDependenciesFromSelf().stream()
                .map(dep -> dep.getTargetClass())
                .collect(Collectors.toSet());
    }
}
