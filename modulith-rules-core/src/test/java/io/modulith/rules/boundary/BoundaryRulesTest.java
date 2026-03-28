package io.modulith.rules.boundary;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for {@link BoundaryRules}.
 *
 * <p>Uses compiled test fixture classes under io.modulith.rules.testfixtures to exercise
 * each rule in both passing and failing scenarios.
 *
 * <p>Fixture layout:
 * <ul>
 *   <li>alpha - standalone module with api and internal packages
 *   <li>beta - standalone module with api and internal packages
 *   <li>gamma - module that depends on alpha.internal (violation) and beta.api (valid)
 * </ul>
 */
class BoundaryRulesTest {

    private static final String ALPHA_PKG = "io.modulith.rules.testfixtures.alpha";
    private static final String BETA_PKG  = "io.modulith.rules.testfixtures.beta";
    private static final String GAMMA_PKG = "io.modulith.rules.testfixtures.gamma";

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static JavaClasses allFixtureClasses() {
        return new ClassFileImporter().importPackages(ALPHA_PKG, BETA_PKG, GAMMA_PKG);
    }

    private static ModuleDefinition alphaModule() {
        return ModuleDefinition.builder("alpha")
                .basePackage(ALPHA_PKG)
                .apiPackages(".api.")
                .internalPackages(".internal.")
                .build();
    }

    private static ModuleDefinition betaModule() {
        return ModuleDefinition.builder("beta")
                .basePackage(BETA_PKG)
                .apiPackages(".api.")
                .internalPackages(".internal.")
                .build();
    }

    // ---------------------------------------------------------------------------
    // modulesOnlyDependOnAllowedModules
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("modulesOnlyDependOnAllowedModules passes when gamma lists both alpha and beta as allowed")
    void allowedDependencies_shouldPass_whenModuleOnlyDependsOnAllowedModules() {
        JavaClasses classes = allFixtureClasses();
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(alphaModule())
                .module(betaModule())
                .module(ModuleDefinition.builder("gamma")
                        .basePackage(GAMMA_PKG)
                        .allowedDependencies("alpha", "beta")
                        .build())
                .build();

        BoundaryRules rules = new BoundaryRules(ruleSet);
        assertDoesNotThrow(() -> rules.modulesOnlyDependOnAllowedModules().check(classes));
    }

    @Test
    @DisplayName("modulesOnlyDependOnAllowedModules fails when gamma depends on alpha but only beta is allowed")
    void allowedDependencies_shouldFail_whenModuleDependsOnDisallowedModule() {
        JavaClasses classes = allFixtureClasses();
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(alphaModule())
                .module(betaModule())
                .module(ModuleDefinition.builder("gamma")
                        .basePackage(GAMMA_PKG)
                        .allowedDependencies("beta") // alpha is intentionally not allowed
                        .build())
                .build();

        BoundaryRules rules = new BoundaryRules(ruleSet);
        assertThatThrownBy(() -> rules.modulesOnlyDependOnAllowedModules().check(classes))
                .isInstanceOf(AssertionError.class);
    }

    // ---------------------------------------------------------------------------
    // internalsShouldNotBeAccessedFromOutside
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("internalsShouldNotBeAccessedFromOutside fails when gamma accesses alpha.internal.AlphaServiceImpl")
    void internals_shouldFail_whenAccessingOtherModuleInternals() {
        JavaClasses classes = allFixtureClasses();
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(alphaModule())
                .module(betaModule())
                .module(ModuleDefinition.builder("gamma")
                        .basePackage(GAMMA_PKG)
                        .internalPackages(".internal.")
                        .build())
                .build();

        BoundaryRules rules = new BoundaryRules(ruleSet);
        assertThatThrownBy(() -> rules.internalsShouldNotBeAccessedFromOutside().check(classes))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("internalsShouldNotBeAccessedFromOutside passes when gamma only accesses beta.api.BetaService")
    void internals_shouldPass_whenAccessingOtherModuleApi() {
        // Import all fixtures but only define beta and gamma.
        // GammaServiceImpl's dependency on AlphaServiceImpl is ignored because
        // alpha is not registered in the rule set, so it is not a known module.
        // The only cross-module dependency inspected is gamma -> beta.api.BetaService,
        // which is not an internal class.
        JavaClasses classes = allFixtureClasses();
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(betaModule())
                .module(ModuleDefinition.builder("gamma")
                        .basePackage(GAMMA_PKG)
                        .internalPackages(".internal.")
                        .build())
                .build();

        BoundaryRules rules = new BoundaryRules(ruleSet);
        assertDoesNotThrow(() -> rules.internalsShouldNotBeAccessedFromOutside().check(classes));
    }

    // ---------------------------------------------------------------------------
    // crossModuleAccessOnlyThroughApi
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("crossModuleAccessOnlyThroughApi fails when gamma accesses alpha.internal.AlphaServiceImpl (not API)")
    void apiAccess_shouldFail_whenAccessingNonApiClassCrossModule() {
        JavaClasses classes = allFixtureClasses();
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(alphaModule())
                .module(betaModule())
                .module(ModuleDefinition.builder("gamma")
                        .basePackage(GAMMA_PKG)
                        .apiPackages(".api.")
                        .build())
                .build();

        BoundaryRules rules = new BoundaryRules(ruleSet);
        assertThatThrownBy(() -> rules.crossModuleAccessOnlyThroughApi().check(classes))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("crossModuleAccessOnlyThroughApi passes when gamma only accesses beta.api.BetaService")
    void apiAccess_shouldPass_whenAccessingApiClassCrossModule() {
        // Only beta and gamma are defined. AlphaServiceImpl is not in any known module
        // so the dependency is skipped. BetaService is in beta's api package, which passes.
        JavaClasses classes = allFixtureClasses();
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(betaModule())
                .module(ModuleDefinition.builder("gamma")
                        .basePackage(GAMMA_PKG)
                        .apiPackages(".api.")
                        .build())
                .build();

        BoundaryRules rules = new BoundaryRules(ruleSet);
        assertDoesNotThrow(() -> rules.crossModuleAccessOnlyThroughApi().check(classes));
    }

    // ---------------------------------------------------------------------------
    // moduleShouldHaveNoDependencies
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("moduleShouldHaveNoDependencies fails because gamma depends on alpha and beta")
    void noDependencies_shouldFail_whenModuleHasOutgoingDependencies() {
        JavaClasses classes = allFixtureClasses();
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(alphaModule())
                .module(betaModule())
                .module(ModuleDefinition.builder("gamma")
                        .basePackage(GAMMA_PKG)
                        .build())
                .build();

        BoundaryRules rules = new BoundaryRules(ruleSet);
        assertThatThrownBy(() -> rules.moduleShouldHaveNoDependencies("gamma").check(classes))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("moduleShouldHaveNoDependencies passes for alpha because it has no outgoing module dependencies")
    void noDependencies_shouldPass_whenModuleIsIsolated() {
        // AlphaServiceImpl only depends on AlphaService, which is in the same module.
        // There are no cross-module dependencies from alpha to any other defined module.
        JavaClasses classes = new ClassFileImporter().importPackages(ALPHA_PKG);
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(alphaModule())
                .build();

        BoundaryRules rules = new BoundaryRules(ruleSet);
        assertDoesNotThrow(() -> rules.moduleShouldHaveNoDependencies("alpha").check(classes));
    }
}
