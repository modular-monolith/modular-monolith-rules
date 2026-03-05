package io.modulith.rules.cycle;

import com.tngtech.archunit.lang.ArchRule;
import io.modulith.rules.api.ModulithRuleSet;

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
     * Returns a rule that verifies no circular dependencies exist between modules.
     * Implementation will be provided in a future release.
     *
     * @return an ArchUnit rule detecting inter-module dependency cycles
     */
    public ArchRule noCircularDependenciesBetweenModules() {
        // TODO: implement in future commit
        return null;
    }
}
