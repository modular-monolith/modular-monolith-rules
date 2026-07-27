package io.modulith.rules;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for the {@link ModulithRules} facade's rule list methods, using the
 * shared test fixtures: alpha and beta are independent clean modules, gamma
 * depends on alpha's internals.
 */
class ModulithRulesTest {

    private static final String FIXTURES = "io.modulith.rules.testfixtures";
    private static final String ALPHA_PKG = FIXTURES + ".alpha";
    private static final String BETA_PKG  = FIXTURES + ".beta";
    private static final String GAMMA_PKG = FIXTURES + ".gamma";

    @Test
    @DisplayName("allRules returns the narrow trio: allowed deps, internals, cycles")
    void allRules_shouldReturnThreeRules() {
        List<ArchRule> rules = ModulithRules.forPackage(FIXTURES, "alpha", "beta").allRules();

        assertThat(rules).hasSize(3);
    }

    @Test
    @DisplayName("allCoreRules adds API-only access and communication contracts")
    void allCoreRules_shouldReturnFiveRules() {
        List<ArchRule> rules = ModulithRules.forPackage(FIXTURES, "alpha", "beta").allCoreRules();

        assertThat(rules).hasSize(5);
    }

    @Test
    @DisplayName("allCoreRules passes for two independent clean modules")
    void allCoreRules_shouldPass_onCleanModules() {
        JavaClasses classes = new ClassFileImporter().importPackages(ALPHA_PKG, BETA_PKG);
        List<ArchRule> rules = ModulithRules.forPackage(FIXTURES, "alpha", "beta").allCoreRules();

        assertDoesNotThrow(() -> rules.forEach(rule -> rule.check(classes)));
    }

    @Test
    @DisplayName("allCoreRules reports gamma reaching into alpha's internals")
    void allCoreRules_shouldFail_whenInternalsAreAccessed() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages(ALPHA_PKG, BETA_PKG, GAMMA_PKG);
        List<ArchRule> rules = ModulithRules
                .forPackage(FIXTURES, "alpha", "beta", "gamma").allCoreRules();

        assertThatThrownBy(() -> rules.forEach(rule -> rule.check(classes)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Fix:");
    }
}
