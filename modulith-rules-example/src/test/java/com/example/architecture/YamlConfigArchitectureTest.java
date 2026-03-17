package com.example.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import io.modulith.rules.ModulithRules;
import io.modulith.rules.api.ModulithRuleSet;
import io.modulith.rules.config.ModulithConfigLoader;
import org.junit.jupiter.api.Test;

/**
 * Architecture tests demonstrating YAML-based configuration via {@link ModulithConfigLoader}.
 *
 * <p>The module structure is declared in {@code src/test/resources/modulith-rules.yml},
 * allowing teams to keep their architectural boundaries documented alongside the code
 * without writing boilerplate Java configuration.
 */
class YamlConfigArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.example");

    private final ModulithRuleSet ruleSet = ModulithConfigLoader.loadFromClasspath();

    private final ModulithRules rules = ModulithRules.of(ruleSet);

    @Test
    void allBoundariesRespected() {
        ArchRule rule = rules.boundaryRules().onlyAllowedDependenciesAreUsed();
        rule.check(classes);
    }

    @Test
    void noCycles() {
        ArchRule rule = rules.cycleRules().noCircularDependenciesBetweenModules();
        rule.check(classes);
    }

    @Test
    void internalsProtected() {
        ArchRule rule = rules.boundaryRules().noModuleAccessesInternalsOfOthers();
        rule.check(classes);
    }

    @Test
    void communicationContractsRespected() {
        ArchRule rule = rules.communicationRules().allCommunicationContractsRespected();
        rule.check(classes);
    }
}
