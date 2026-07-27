package io.modulith.rules.communication;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.modulith.rules.api.CommunicationType;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for {@link CommunicationRules}.
 *
 * <p>Uses compiled test fixture classes to verify that declared inter-module
 * communication contracts are enforced by the ArchUnit rules.
 *
 * <p>Fixture layout:
 * <ul>
 *   <li>sender - has SenderServiceImpl that calls ReceiverService.process() directly
 *   <li>receiver - has ReceiverService interface with a process() method
 *   <li>alpha, gamma - used to produce a NONE contract violation (gamma depends on alpha.internal)
 * </ul>
 */
class CommunicationRulesTest {

    private static final String SENDER_PKG   = "io.modulith.rules.testfixtures.sender";
    private static final String RECEIVER_PKG = "io.modulith.rules.testfixtures.receiver";
    private static final String ALPHA_PKG    = "io.modulith.rules.testfixtures.alpha";
    private static final String GAMMA_PKG    = "io.modulith.rules.testfixtures.gamma";

    // ---------------------------------------------------------------------------
    // asyncModulesShouldNotCallDirectly
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("asyncModulesShouldNotCallDirectly fails when SenderServiceImpl calls ReceiverService.process() directly")
    void asyncContract_shouldFail_whenDirectMethodCallMade() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages(SENDER_PKG, RECEIVER_PKG);

        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(ModuleDefinition.builder("sender")
                        .basePackage(SENDER_PKG)
                        .communicatesWith("receiver", CommunicationType.ASYNCHRONOUS)
                        .build())
                .module(ModuleDefinition.builder("receiver")
                        .basePackage(RECEIVER_PKG)
                        .build())
                .build();

        CommunicationRules rules = new CommunicationRules(ruleSet);
        assertThatThrownBy(() -> rules.asyncModulesShouldNotCallDirectly().check(classes))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("asyncModulesShouldNotCallDirectly passes when no direct calls exist from sender to receiver")
    void asyncContract_shouldPass_whenNoDirectCallsMade() {
        // Import only sender.api (the SenderService interface, which makes no method calls)
        // and all receiver classes. SenderServiceImpl is excluded so there are no direct calls.
        JavaClasses classes = new ClassFileImporter()
                .importPackages(SENDER_PKG + ".api", RECEIVER_PKG);

        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(ModuleDefinition.builder("sender")
                        .basePackage(SENDER_PKG)
                        .communicatesWith("receiver", CommunicationType.ASYNCHRONOUS)
                        .build())
                .module(ModuleDefinition.builder("receiver")
                        .basePackage(RECEIVER_PKG)
                        .build())
                .build();

        CommunicationRules rules = new CommunicationRules(ruleSet);
        assertDoesNotThrow(() -> rules.asyncModulesShouldNotCallDirectly().check(classes));
    }

    // ---------------------------------------------------------------------------
    // noCommModulesShouldNotInteract
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("noCommModulesShouldNotInteract fails when SenderServiceImpl has a dependency on ReceiverService")
    void noneContract_shouldFail_whenAnyDependencyExists() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages(SENDER_PKG, RECEIVER_PKG);

        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(ModuleDefinition.builder("sender")
                        .basePackage(SENDER_PKG)
                        .communicatesWith("receiver", CommunicationType.NONE)
                        .build())
                .module(ModuleDefinition.builder("receiver")
                        .basePackage(RECEIVER_PKG)
                        .build())
                .build();

        CommunicationRules rules = new CommunicationRules(ruleSet);
        assertThatThrownBy(() -> rules.noCommModulesShouldNotInteract().check(classes))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("noCommModulesShouldNotInteract passes when alpha has no dependency on beta")
    void noneContract_shouldPass_whenNoDependencyExists() {
        // AlphaServiceImpl only implements AlphaService (same module).
        // BetaServiceImpl only implements BetaService (same module).
        // Neither alpha nor beta classes depend on the other module.
        JavaClasses classes = new ClassFileImporter()
                .importPackages(ALPHA_PKG, "io.modulith.rules.testfixtures.beta");

        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(ModuleDefinition.builder("alpha")
                        .basePackage(ALPHA_PKG)
                        .communicatesWith("beta", CommunicationType.NONE)
                        .build())
                .module(ModuleDefinition.builder("beta")
                        .basePackage("io.modulith.rules.testfixtures.beta")
                        .build())
                .build();

        CommunicationRules rules = new CommunicationRules(ruleSet);
        assertDoesNotThrow(() -> rules.noCommModulesShouldNotInteract().check(classes));
    }

    // ---------------------------------------------------------------------------
    // allCommunicationContractsRespected
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("allCommunicationContractsRespected catches both async and none violations")
    void allContracts_shouldCombineBothChecks() {
        // sender calls receiver directly (ASYNC violation)
        // gamma depends on alpha (NONE violation via GammaServiceImpl -> AlphaServiceImpl)
        JavaClasses classes = new ClassFileImporter()
                .importPackages(SENDER_PKG, RECEIVER_PKG, ALPHA_PKG, GAMMA_PKG);

        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(ModuleDefinition.builder("sender")
                        .basePackage(SENDER_PKG)
                        .communicatesWith("receiver", CommunicationType.ASYNCHRONOUS)
                        .build())
                .module(ModuleDefinition.builder("receiver")
                        .basePackage(RECEIVER_PKG)
                        .build())
                .module(ModuleDefinition.builder("alpha")
                        .basePackage(ALPHA_PKG)
                        .build())
                .module(ModuleDefinition.builder("gamma")
                        .basePackage(GAMMA_PKG)
                        .communicatesWith("alpha", CommunicationType.NONE)
                        .build())
                .build();

        CommunicationRules rules = new CommunicationRules(ruleSet);
        assertThatThrownBy(() -> rules.allCommunicationContractsRespected().check(classes))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("sender")
                .hasMessageContaining("gamma")
                .hasMessageContaining("Fix:");
    }

    // ---------------------------------------------------------------------------
    // synchronous contract - direct calls are permitted
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("asyncModulesShouldNotCallDirectly passes when the contract is SYNCHRONOUS and direct calls are made")
    void synchronousContract_shouldPass_whenDirectCallMade() {
        // The SYNCHRONOUS contract is not checked by CommunicationRules.
        // Direct method calls are fine when the contract type is SYNCHRONOUS.
        JavaClasses classes = new ClassFileImporter()
                .importPackages(SENDER_PKG, RECEIVER_PKG);

        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(ModuleDefinition.builder("sender")
                        .basePackage(SENDER_PKG)
                        .communicatesWith("receiver", CommunicationType.SYNCHRONOUS)
                        .build())
                .module(ModuleDefinition.builder("receiver")
                        .basePackage(RECEIVER_PKG)
                        .build())
                .build();

        CommunicationRules rules = new CommunicationRules(ruleSet);
        assertDoesNotThrow(() -> rules.asyncModulesShouldNotCallDirectly().check(classes));
    }
}
