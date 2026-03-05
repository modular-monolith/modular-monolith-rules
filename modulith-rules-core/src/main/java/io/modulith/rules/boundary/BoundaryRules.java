package io.modulith.rules.boundary;

import com.tngtech.archunit.lang.ArchRule;
import io.modulith.rules.api.ModulithRuleSet;

/**
 * Factory for ArchUnit rules that enforce module boundary constraints.
 *
 * <p>Boundary rules verify that code in one module does not access internal packages
 * of another module, and that inter-module dependencies only flow through declared
 * public API packages.
 *
 * <p>Rules are created from a {@link ModulithRuleSet} that describes the module layout.
 * Obtain an instance via {@link io.modulith.rules.ModulithRules#boundaryRules()}.
 */
public final class BoundaryRules {

    private final ModulithRuleSet ruleSet;

    /**
     * Creates a new {@code BoundaryRules} factory for the given rule set.
     *
     * @param ruleSet the module registry describing the architecture under test
     */
    public BoundaryRules(ModulithRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    /**
     * Returns a rule that verifies no module accesses the internal packages of another module.
     * Implementation will be provided in a future release.
     *
     * @return an ArchUnit rule enforcing internal package protection
     */
    public ArchRule noModuleAccessesInternalsOfOthers() {
        // TODO: implement in future commit
        return null;
    }

    /**
     * Returns a rule that verifies each module only depends on modules declared in its
     * {@code allowedDependencies}. Implementation will be provided in a future release.
     *
     * @return an ArchUnit rule enforcing allowed dependency declarations
     */
    public ArchRule onlyAllowedDependenciesAreUsed() {
        // TODO: implement in future commit
        return null;
    }
}
