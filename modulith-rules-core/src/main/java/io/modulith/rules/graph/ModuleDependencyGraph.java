package io.modulith.rules.graph;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import io.modulith.rules.api.CommunicationType;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Renders the module dependency graph of a modular monolith as a
 * <a href="https://mermaid.js.org/syntax/flowchart.html">Mermaid flowchart</a>,
 * ready to paste into Markdown.
 *
 * <p>Every module registered in the {@link ModulithRuleSet} becomes a node, so
 * isolated modules are visible too. A directed edge is drawn from module A to
 * module B when any class in A has a direct dependency on a class in B, using the
 * same class-to-module resolution as the boundary and cycle rules. Classes that
 * do not belong to a registered module are ignored, consistent with how all other
 * rules treat them.
 *
 * <p>Edge style follows the declared communication contract, the same signal the
 * communication rules enforce:
 * <ul>
 *   <li>{@code A -.-> B} (dashed) when A declares
 *       {@code communicatesWith("B", CommunicationType.ASYNCHRONOUS)}
 *   <li>{@code A --> B} (solid) for every other observed dependency, including
 *       modules with no declared contract
 * </ul>
 *
 * <p>The output is deterministic: nodes are sorted by module name and edges by
 * source then target, so the same input always yields the same string.
 *
 * <p>Example output:
 * <pre>
 * flowchart LR
 *     billing
 *     notifications
 *     ordering
 *     billing --&gt; ordering
 *     notifications -.-&gt; ordering
 * </pre>
 *
 * <p>Typical usage:
 * <pre>{@code
 * JavaClasses classes = new ClassFileImporter().importPackages("com.example");
 * String mermaid = ModulithRules.of(ruleSet).dependencyGraph().toMermaid(classes);
 * }</pre>
 */
public final class ModuleDependencyGraph {

    private final ModulithRuleSet ruleSet;

    /**
     * Creates a new graph renderer for the given rule set.
     *
     * @param ruleSet the module registry describing the architecture, must not be {@code null}
     */
    public ModuleDependencyGraph(ModulithRuleSet ruleSet) {
        this.ruleSet = Objects.requireNonNull(ruleSet, "ruleSet must not be null");
    }

    /**
     * Renders the dependency graph of the given classes as a Mermaid flowchart string.
     *
     * <p>The method only builds the string; it never touches the file system. Callers
     * decide where the diagram goes, for example into a Markdown file or a test log.
     *
     * @param classes the imported classes to analyse, must not be {@code null}
     * @return a Mermaid {@code flowchart LR} definition ending with a newline
     */
    public String toMermaid(JavaClasses classes) {
        Objects.requireNonNull(classes, "classes must not be null");
        Map<String, Set<String>> edges = collectEdges(classes);

        StringBuilder mermaid = new StringBuilder("flowchart LR\n");

        Set<String> moduleNames = new TreeSet<>();
        for (ModuleDefinition module : ruleSet.allModules()) {
            moduleNames.add(module.name());
        }
        for (String name : moduleNames) {
            mermaid.append("    ").append(nodeDeclaration(name)).append('\n');
        }
        for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
            String source = entry.getKey();
            for (String target : entry.getValue()) {
                mermaid.append("    ")
                        .append(nodeId(source))
                        .append(isAsynchronous(source, target) ? " -.-> " : " --> ")
                        .append(nodeId(target))
                        .append('\n');
            }
        }
        return mermaid.toString();
    }

    /**
     * Collects the module-level dependency edges from class-level dependencies,
     * mirroring how the cycle rules accumulate their graph. TreeMap and TreeSet
     * keep the edges sorted for deterministic output.
     */
    private Map<String, Set<String>> collectEdges(JavaClasses classes) {
        Map<String, Set<String>> edges = new TreeMap<>();
        for (JavaClass javaClass : classes) {
            Optional<ModuleDefinition> sourceModule = findModuleOf(javaClass);
            if (!sourceModule.isPresent()) {
                continue;
            }
            String sourceName = sourceModule.get().name();
            javaClass.getDirectDependenciesFromSelf().forEach(dependency -> {
                Optional<ModuleDefinition> targetModule = findModuleOf(dependency.getTargetClass());
                if (targetModule.isPresent() && !targetModule.get().name().equals(sourceName)) {
                    edges.computeIfAbsent(sourceName, k -> new TreeSet<>())
                            .add(targetModule.get().name());
                }
            });
        }
        return edges;
    }

    /**
     * An edge is asynchronous when the source module declares an ASYNCHRONOUS
     * communication contract with the target. This is the module-level signal the
     * communication rules already enforce.
     *
     * <p>Extension point: the communication rules can also classify individual
     * calls as asynchronous (calls to messaging infrastructure or to event
     * classes). Refining edge style from observed calls rather than the declared
     * contract would plug in here, without changing the rendering above.
     */
    private boolean isAsynchronous(String sourceName, String targetName) {
        CommunicationType contract =
                ruleSet.module(sourceName).communicationContracts().get(targetName);
        return CommunicationType.ASYNCHRONOUS.equals(contract);
    }

    /**
     * Declares a node, quoting the label when the module name contains characters
     * that are not valid in a bare Mermaid identifier.
     */
    private static String nodeDeclaration(String moduleName) {
        String id = nodeId(moduleName);
        if (id.equals(moduleName)) {
            return moduleName;
        }
        return id + "[\"" + moduleName + "\"]";
    }

    /** Sanitizes a module name into a Mermaid-safe node identifier. */
    private static String nodeId(String moduleName) {
        return moduleName.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private Optional<ModuleDefinition> findModuleOf(JavaClass javaClass) {
        return ruleSet.allModules().stream()
                .filter(m -> m.containsClass(javaClass.getName()))
                .findFirst();
    }
}
