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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architecture tests demonstrating four ways of using modulith-rules.
 *
 * <ul>
 *   <li>{@link QuickStartTests} - minimal setup using convention-based module names</li>
 *   <li>{@link FullConfigTests} - explicit module configuration with all options</li>
 *   <li>{@link ArchUnitNativeTests} - static {@code @ArchTest} fields for native ArchUnit integration</li>
 *   <li>{@link DependencyGraphTests} - Mermaid dependency graph export</li>
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
            rules.communicationRules().allCommunicationContractsRespected().check(classes);
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

    /**
     * Approach 4: Mermaid dependency graph export.
     *
     * <p>Unlike the other approaches this produces a diagram rather than enforcing a
     * rule, so it is a plain {@code @Test} rather than an {@code @ArchTest}. Running
     * it prints a ready-to-paste Mermaid block to the console:
     *
     * <pre>
     * mvn -pl modulith-rules-example -am test -Dtest='ModularMonolithArchitectureTest$DependencyGraphTests'
     * </pre>
     *
     * <p>The assertions keep the diagram honest. They fail if the module graph drifts,
     * for example if the ordering to inventory dependency disappears or a declared
     * asynchronous contract stops being rendered as a dashed arrow.
     */
    @Nested
    class DependencyGraphTests {

        private final JavaClasses classes = new ClassFileImporter()
                .importPackages("com.example");

        /**
         * The same configuration as {@link FullConfigTests}, because edge style follows
         * the declared communication contracts. Convention-based setup declares no
         * contracts, so every edge would render solid.
         */
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

        @Test
        void printsMermaidDependencyGraph() {
            String mermaid = ModulithRules.of(ruleSet).dependencyGraph().toMermaid(classes);

            // Printed with the fence so the whole block can be pasted into Markdown.
            System.out.println("```mermaid");
            System.out.print(mermaid);
            System.out.println("```");

            assertAll(
                    () -> assertTrue(mermaid.startsWith("flowchart LR\n"),
                            "should start with the Mermaid flowchart header"),
                    () -> assertTrue(mermaid.contains("    payments\n"),
                            "every module should appear as a node, including leaf modules"),
                    () -> assertTrue(mermaid.contains("    ordering --> inventory\n"),
                            "a plain cross-module dependency should be a solid arrow"),
                    () -> assertTrue(mermaid.contains("    notifications -.-> ordering\n"),
                            "a declared asynchronous contract should be a dashed arrow"));
        }

        @Test
        void graphIsDeterministic() {
            ModulithRules rules = ModulithRules.of(ruleSet);

            assertEquals(rules.dependencyGraph().toMermaid(classes),
                    rules.dependencyGraph().toMermaid(classes),
                    "the same input should always produce the same diagram");
        }
    }
}
