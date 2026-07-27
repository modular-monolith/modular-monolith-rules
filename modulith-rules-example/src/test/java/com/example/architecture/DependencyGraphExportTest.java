package com.example.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.modulith.rules.ModulithRules;
import io.modulith.rules.api.CommunicationType;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Demonstrates the Mermaid dependency graph export against the example application.
 *
 * <p>Prints the diagram so it can be copied into Markdown, and asserts the parts
 * that should stay stable so the test fails if the module graph drifts.
 */
class DependencyGraphExportTest {

    private static final String ROOT = "com.example";

    private static ModulithRuleSet ruleSet() {
        return ModulithRuleSet.forRootPackage(ROOT)
                .modules("ordering", "inventory", "payments")
                .module(ModuleDefinition.builder("notifications")
                        .basePackage(ROOT + ".notifications")
                        .communicatesWith("ordering", CommunicationType.ASYNCHRONOUS)
                        .build())
                .build();
    }

    @Test
    void exportsMermaidGraphOfTheExampleApplication() {
        JavaClasses classes = new ClassFileImporter().importPackages(ROOT);

        String mermaid = ModulithRules.of(ruleSet()).dependencyGraph().toMermaid(classes);

        System.out.println("```mermaid");
        System.out.print(mermaid);
        System.out.println("```");

        assertAll(
                () -> assertTrue(mermaid.startsWith("flowchart LR\n"),
                        "should start with the flowchart header"),
                () -> assertTrue(mermaid.contains("    ordering --> inventory\n"),
                        "ordering should have a solid edge to inventory"),
                () -> assertTrue(mermaid.contains("    notifications -.-> ordering\n"),
                        "notifications should have a dashed edge to ordering"));
    }
}
