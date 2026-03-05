package io.modulith.rules;

import com.tngtech.archunit.lang.ArchRule;
import io.modulith.rules.api.ModulithRuleSet;
import io.modulith.rules.boundary.BoundaryRules;
import io.modulith.rules.communication.CommunicationRules;
import io.modulith.rules.cycle.CycleRules;

import java.util.List;
import java.util.Objects;

/**
 * Main entry point for modulith-rules.
 *
 * <p>{@code ModulithRules} provides a fluent API for obtaining groups of ArchUnit rules
 * that enforce module boundary constraints, detect dependency cycles, and verify
 * inter-module communication contracts.
 *
 * <p>Example usage with an explicit {@link ModulithRuleSet}:
 *
 * <pre>{@code
 * ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("com.example")
 *         .modules("orders", "catalog", "inventory", "payments")
 *         .build();
 *
 * ModulithRules rules = ModulithRules.of(ruleSet);
 *
 * // Use individual rule groups in ArchUnit tests:
 * rules.boundaryRules().noModuleAccessesInternalsOfOthers().check(classes);
 * rules.cycleRules().noCircularDependenciesBetweenModules().check(classes);
 * }</pre>
 *
 * <p>Example usage with convention-based module names:
 *
 * <pre>{@code
 * ModulithRules rules = ModulithRules.forPackage(
 *         "com.example", "orders", "catalog", "inventory");
 *
 * List<ArchRule> all = rules.allRules();
 * }</pre>
 */
public final class ModulithRules {

    private final ModulithRuleSet ruleSet;

    private ModulithRules(ModulithRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    /**
     * Creates a {@code ModulithRules} instance for the given rule set.
     *
     * @param ruleSet the module registry, must not be {@code null}
     * @return a new {@code ModulithRules} instance
     */
    public static ModulithRules of(ModulithRuleSet ruleSet) {
        Objects.requireNonNull(ruleSet, "ruleSet must not be null");
        return new ModulithRules(ruleSet);
    }

    /**
     * Creates a {@code ModulithRules} instance using convention-based module names.
     * Each module's base package is derived as {@code rootPackage + "." + moduleName}.
     *
     * @param rootPackage the top-level package shared by all modules
     * @param moduleNames the names of the modules to register
     * @return a new {@code ModulithRules} instance
     */
    public static ModulithRules forPackage(String rootPackage, String... moduleNames) {
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage(rootPackage)
                .modules(moduleNames)
                .build();
        return new ModulithRules(ruleSet);
    }

    /**
     * Returns the underlying {@link ModulithRuleSet} used by this instance.
     *
     * @return the rule set, never {@code null}
     */
    public ModulithRuleSet ruleSet() {
        return ruleSet;
    }

    /**
     * Returns a {@link BoundaryRules} factory for rules that enforce module boundary
     * constraints, such as preventing access to internal packages across module lines.
     *
     * @return a boundary rules factory bound to this rule set
     */
    public BoundaryRules boundaryRules() {
        return new BoundaryRules(ruleSet);
    }

    /**
     * Returns a {@link CycleRules} factory for rules that detect dependency cycles
     * between modules.
     *
     * @return a cycle rules factory bound to this rule set
     */
    public CycleRules cycleRules() {
        return new CycleRules(ruleSet);
    }

    /**
     * Returns a {@link CommunicationRules} factory for rules that verify inter-module
     * communication contracts match their declarations.
     *
     * @return a communication rules factory bound to this rule set
     */
    public CommunicationRules communicationRules() {
        return new CommunicationRules(ruleSet);
    }

    /**
     * Returns a combined list of core ArchUnit rules covering boundary enforcement,
     * internal package protection, and cycle detection. This is a convenience method
     * for applying all standard rules in a single ArchUnit test.
     *
     * <p>The returned list includes:
     * <ul>
     *   <li>a rule verifying modules only depend on declared allowed dependencies</li>
     *   <li>a rule verifying no module accesses internal packages of another</li>
     *   <li>a rule verifying no circular dependencies exist between modules</li>
     * </ul>
     *
     * @return an unmodifiable list of ArchUnit rules
     */
    public List<ArchRule> allRules() {
        return List.of(
                boundaryRules().onlyAllowedDependenciesAreUsed(),
                boundaryRules().noModuleAccessesInternalsOfOthers(),
                cycleRules().noCircularDependenciesBetweenModules()
        );
    }
}
