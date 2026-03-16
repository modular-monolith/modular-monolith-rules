package com.example.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.modulith.rules.ModulithRules;
import io.modulith.rules.api.CommunicationType;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Architecture tests demonstrating three approaches for using modulith-rules.
 *
 * <ul>
 *   <li>{@link QuickStartTests} - minimal setup using convention-based module names</li>
 *   <li>{@link FullConfigTests} - explicit module configuration with all options</li>
 *   <li>{@link ArchUnitNativeTests} - static {@code @ArchTest} fields for native ArchUnit integration</li>
 * </ul>
 */
class ModularMonolithArchitectureTest {

    /**
     * Approach 1: Quick-start with convention-based module names.
     *
     * <p>Ideal for teams that want to get started quickly without deep configuration.
     * Module packages are derived by convention as {@code rootPackage + "." + moduleName}.
     */
    @Nested
    class QuickStartTests {

        private final JavaClasses classes = new ClassFileImporter()
                .importPackages("com.example");

        @Test
        void no_circular_dependencies_between_modules() {
            ModulithRules
                    .forPackage("com.example", "ordering", "inventory", "payments", "notifications")
                    .cycleRules()
                    .noModuleCycles()
                    .check(classes);
        }
    }

    /**
     * Approach 2: Full configuration with explicit module definitions.
     *
     * <p>Provides precise control over API packages, internal packages, allowed
     * dependencies, and inter-module communication contracts.
     */
    @Nested
    class FullConfigTests {

        private final JavaClasses classes = new ClassFileImporter()
                .importPackages("com.example");

        private final ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("com.example")
                .module(ModuleDefinition.builder("ordering")
                        .basePackage("com.example.ordering")
                        .apiPackages(".api.")
                        .internalPackages(".internal.", ".infrastructure.")
                        .allowedDependencies("inventory", "payments")
                        .communicatesWith("notifications", CommunicationType.ASYNCHRONOUS)
                        .build())
                .module(ModuleDefinition.builder("inventory")
                        .basePackage("com.example.inventory")
                        .apiPackages(".api.")
                        .internalPackages(".internal.")
                        .build())
                .module(ModuleDefinition.builder("payments")
                        .basePackage("com.example.payments")
                        .apiPackages(".api.")
                        .internalPackages(".internal.")
                        .build())
                .module(ModuleDefinition.builder("notifications")
                        .basePackage("com.example.notifications")
                        .apiPackages(".api.")
                        .internalPackages(".internal.")
                        .allowedDependencies("ordering")
                        .communicatesWith("ordering", CommunicationType.ASYNCHRONOUS)
                        .build())
                .build();

        private final ModulithRules rules = ModulithRules.of(ruleSet);

        @Test
        void modulesRespectDeclaredDependencies() {
            rules.boundaryRules().onlyAllowedDependenciesAreUsed().check(classes);
        }

        @Test
        void internalPackagesProtected() {
            rules.boundaryRules().noModuleAccessesInternalsOfOthers().check(classes);
        }

        @Test
        void crossModuleAccessThroughApi() {
            rules.boundaryRules().crossModuleAccessOnlyThroughApi().check(classes);
        }

        @Test
        void noCycles() {
            rules.cycleRules().noCircularDependenciesBetweenModules().check(classes);
        }

        @Test
        void asyncContractsRespected() {
            rules.communicationRules().communicationContractsAreRespected().check(classes);
        }

        @Test
        void inventoryIsIndependent() {
            rules.boundaryRules().moduleShouldHaveNoDependencies("inventory").check(classes);
        }

        @Test
        void paymentsOnlyAccessedByOrdering() {
            rules.boundaryRules().moduleShouldOnlyBeAccessedBy("payments", "ordering").check(classes);
        }
    }

    /**
     * Approach 3: Native ArchUnit {@code @ArchTest} integration.
     *
     * <p>Uses ArchUnit's built-in test runner via {@code @AnalyzeClasses} and static
     * {@code @ArchTest} fields. Rules are evaluated automatically by ArchUnit without
     * needing to call {@code rule.check(classes)} explicitly.
     */
    @AnalyzeClasses(packages = "com.example")
    static class ArchUnitNativeTests {

        private static final ModulithRuleSet RULE_SET = ModulithRuleSet.forRootPackage("com.example")
                .module(ModuleDefinition.builder("ordering")
                        .basePackage("com.example.ordering")
                        .apiPackages(".api.")
                        .internalPackages(".internal.")
                        .allowedDependencies("inventory", "payments")
                        .build())
                .module(ModuleDefinition.builder("inventory")
                        .basePackage("com.example.inventory")
                        .apiPackages(".api.")
                        .internalPackages(".internal.")
                        .build())
                .module(ModuleDefinition.builder("payments")
                        .basePackage("com.example.payments")
                        .apiPackages(".api.")
                        .internalPackages(".internal.")
                        .build())
                .module(ModuleDefinition.builder("notifications")
                        .basePackage("com.example.notifications")
                        .apiPackages(".api.")
                        .internalPackages(".internal.")
                        .allowedDependencies("ordering")
                        .build())
                .build();

        private static final ModulithRules RULES = ModulithRules.of(RULE_SET);

        @ArchTest
        static final ArchRule no_cycles = RULES.cycleRules().noModuleCycles();

        @ArchTest
        static final ArchRule respect_allowed_dependencies =
                RULES.boundaryRules().onlyAllowedDependenciesAreUsed();

        @ArchTest
        static final ArchRule protect_internals =
                RULES.boundaryRules().noModuleAccessesInternalsOfOthers();

        @ArchTest
        static final ArchRule access_through_api =
                RULES.boundaryRules().crossModuleAccessOnlyThroughApi();
    }
}
