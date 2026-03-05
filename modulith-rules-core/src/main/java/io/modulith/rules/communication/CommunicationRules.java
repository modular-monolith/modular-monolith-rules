package io.modulith.rules.communication;

import com.tngtech.archunit.lang.ArchRule;
import io.modulith.rules.api.ModulithRuleSet;

/**
 * Factory for ArchUnit rules that enforce inter-module communication contracts.
 *
 * <p>Communication rules verify that the communication patterns used between modules
 * (synchronous calls, asynchronous messaging, or no direct communication) match the
 * contracts declared in each module's {@link io.modulith.rules.api.ModuleDefinition}.
 *
 * <p>Rules are created from a {@link ModulithRuleSet} that describes the module layout.
 * Obtain an instance via {@link io.modulith.rules.ModulithRules#communicationRules()}.
 */
public final class CommunicationRules {

    private final ModulithRuleSet ruleSet;

    /**
     * Creates a new {@code CommunicationRules} factory for the given rule set.
     *
     * @param ruleSet the module registry describing the architecture under test
     */
    public CommunicationRules(ModulithRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    /**
     * Returns a rule that verifies all inter-module communication matches the declared
     * contracts in each module's {@code communicationContracts} map.
     * Implementation will be provided in a future release.
     *
     * @return an ArchUnit rule enforcing communication type contracts
     */
    public ArchRule communicationContractsAreRespected() {
        // TODO: implement in future commit
        return null;
    }
}
