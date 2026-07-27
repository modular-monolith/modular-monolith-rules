package io.modulith.rules.graph;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.modulith.rules.api.CommunicationType;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ModuleDependencyGraph}.
 *
 * <p>Reuses the compiled test fixtures from the rule tests:
 * <ul>
 *   <li>sender - SenderServiceImpl depends on receiver's ReceiverService
 *   <li>receiver - standalone target module
 *   <li>alpha, beta, gamma - gamma depends on both alpha and beta; alpha and beta
 *       are independent of each other
 *   <li>cycleA, cycleB - mutual dependency
 * </ul>
 *
 * <p>Assertions target individual lines (header, node, edge) rather than the whole
 * string, so unrelated additions to the output do not break every test.
 */
class ModuleDependencyGraphTest {

    private static final String FIXTURES  = "io.modulith.rules.testfixtures";
    private static final String ALPHA_PKG    = FIXTURES + ".alpha";
    private static final String BETA_PKG     = FIXTURES + ".beta";
    private static final String GAMMA_PKG    = FIXTURES + ".gamma";
    private static final String SENDER_PKG   = FIXTURES + ".sender";
    private static final String RECEIVER_PKG = FIXTURES + ".receiver";
    private static final String CYCLE_A_PKG  = FIXTURES + ".cycleA";
    private static final String CYCLE_B_PKG  = FIXTURES + ".cycleB";

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static JavaClasses senderReceiverClasses() {
        return new ClassFileImporter().importPackages(SENDER_PKG, RECEIVER_PKG);
    }

    private static ModulithRuleSet senderReceiverRuleSet(CommunicationType contract) {
        ModuleDefinition.Builder sender = ModuleDefinition.builder("sender")
                .basePackage(SENDER_PKG);
        if (contract != null) {
            sender.communicatesWith("receiver", contract);
        }
        return ModulithRuleSet.forRootPackage(FIXTURES)
                .module(sender.build())
                .module(ModuleDefinition.builder("receiver").basePackage(RECEIVER_PKG).build())
                .build();
    }

    // ---------------------------------------------------------------------------
    // Header and nodes
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("toMermaid starts with the flowchart header line")
    void toMermaid_shouldStartWithFlowchartHeader() {
        String mermaid = new ModuleDependencyGraph(senderReceiverRuleSet(null))
                .toMermaid(senderReceiverClasses());

        assertThat(mermaid).startsWith("flowchart LR\n");
    }

    @Test
    @DisplayName("every registered module appears as a node, even without dependencies")
    void toMermaid_shouldDeclareEveryModuleAsNode() {
        // alpha and beta have no dependency on each other, so no edges exist,
        // but both must still show up in the diagram.
        JavaClasses classes = new ClassFileImporter().importPackages(ALPHA_PKG, BETA_PKG);
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage(FIXTURES)
                .module(ModuleDefinition.builder("alpha").basePackage(ALPHA_PKG).build())
                .module(ModuleDefinition.builder("beta").basePackage(BETA_PKG).build())
                .build();

        String mermaid = new ModuleDependencyGraph(ruleSet).toMermaid(classes);

        assertThat(mermaid)
                .contains("    alpha\n")
                .contains("    beta\n")
                .doesNotContain("-->")
                .doesNotContain("-.->");
    }

    // ---------------------------------------------------------------------------
    // Sync and async edges
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("a cross-module dependency without an async contract renders as a solid arrow")
    void toMermaid_shouldRenderSolidEdge_forSynchronousDependency() {
        String mermaid = new ModuleDependencyGraph(senderReceiverRuleSet(null))
                .toMermaid(senderReceiverClasses());

        assertThat(mermaid).contains("    sender --> receiver\n");
    }

    @Test
    @DisplayName("a dependency under a declared ASYNCHRONOUS contract renders as a dashed arrow")
    void toMermaid_shouldRenderDashedEdge_forAsynchronousContract() {
        String mermaid = new ModuleDependencyGraph(
                senderReceiverRuleSet(CommunicationType.ASYNCHRONOUS))
                .toMermaid(senderReceiverClasses());

        assertThat(mermaid)
                .contains("    sender -.-> receiver\n")
                .doesNotContain("sender --> receiver");
    }

    @Test
    @DisplayName("a SYNCHRONOUS contract keeps the solid arrow")
    void toMermaid_shouldRenderSolidEdge_forSynchronousContract() {
        String mermaid = new ModuleDependencyGraph(
                senderReceiverRuleSet(CommunicationType.SYNCHRONOUS))
                .toMermaid(senderReceiverClasses());

        assertThat(mermaid).contains("    sender --> receiver\n");
    }

    @Test
    @DisplayName("mutually dependent modules produce one edge in each direction")
    void toMermaid_shouldRenderBothDirections_forCyclicModules() {
        JavaClasses classes = new ClassFileImporter().importPackages(CYCLE_A_PKG, CYCLE_B_PKG);
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage(FIXTURES)
                .module(ModuleDefinition.builder("cycleA").basePackage(CYCLE_A_PKG).build())
                .module(ModuleDefinition.builder("cycleB").basePackage(CYCLE_B_PKG).build())
                .build();

        String mermaid = new ModuleDependencyGraph(ruleSet).toMermaid(classes);

        assertThat(mermaid)
                .contains("    cycleA --> cycleB\n")
                .contains("    cycleB --> cycleA\n");
    }

    // ---------------------------------------------------------------------------
    // Deterministic ordering
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("nodes and edges are sorted by name regardless of registration order")
    void toMermaid_shouldSortNodesAndEdges() {
        // gamma depends on alpha (via AlphaServiceImpl) and on beta (via BetaService).
        // Modules are deliberately registered in non-alphabetical order.
        JavaClasses classes = new ClassFileImporter()
                .importPackages(ALPHA_PKG, BETA_PKG, GAMMA_PKG);
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage(FIXTURES)
                .module(ModuleDefinition.builder("gamma").basePackage(GAMMA_PKG).build())
                .module(ModuleDefinition.builder("beta").basePackage(BETA_PKG).build())
                .module(ModuleDefinition.builder("alpha").basePackage(ALPHA_PKG).build())
                .build();

        String mermaid = new ModuleDependencyGraph(ruleSet).toMermaid(classes);

        int alphaNode = mermaid.indexOf("    alpha\n");
        int betaNode = mermaid.indexOf("    beta\n");
        int gammaNode = mermaid.indexOf("    gamma\n");
        assertThat(alphaNode).isNotNegative();
        assertThat(alphaNode).isLessThan(betaNode);
        assertThat(betaNode).isLessThan(gammaNode);

        int edgeToAlpha = mermaid.indexOf("    gamma --> alpha\n");
        int edgeToBeta = mermaid.indexOf("    gamma --> beta\n");
        assertThat(edgeToAlpha).isNotNegative();
        assertThat(edgeToAlpha).isLessThan(edgeToBeta);
    }

    @Test
    @DisplayName("the same input always yields the same string")
    void toMermaid_shouldBeDeterministic_acrossRegistrationOrderAndRepeatedCalls() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages(ALPHA_PKG, BETA_PKG, GAMMA_PKG);

        ModulithRuleSet orderOne = ModulithRuleSet.forRootPackage(FIXTURES)
                .module(ModuleDefinition.builder("alpha").basePackage(ALPHA_PKG).build())
                .module(ModuleDefinition.builder("beta").basePackage(BETA_PKG).build())
                .module(ModuleDefinition.builder("gamma").basePackage(GAMMA_PKG).build())
                .build();
        ModulithRuleSet orderTwo = ModulithRuleSet.forRootPackage(FIXTURES)
                .module(ModuleDefinition.builder("gamma").basePackage(GAMMA_PKG).build())
                .module(ModuleDefinition.builder("alpha").basePackage(ALPHA_PKG).build())
                .module(ModuleDefinition.builder("beta").basePackage(BETA_PKG).build())
                .build();

        ModuleDependencyGraph graph = new ModuleDependencyGraph(orderOne);
        String first = graph.toMermaid(classes);

        assertThat(first)
                .isEqualTo(graph.toMermaid(classes))
                .isEqualTo(new ModuleDependencyGraph(orderTwo).toMermaid(classes));
    }

    // ---------------------------------------------------------------------------
    // Node identifier sanitization
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("module names that are not valid Mermaid identifiers are sanitized and labelled")
    void toMermaid_shouldSanitizeNodeIds_forNamesWithSpecialCharacters() {
        JavaClasses classes = new ClassFileImporter().importPackages(CYCLE_A_PKG, CYCLE_B_PKG);
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage(FIXTURES)
                .module(ModuleDefinition.builder("cycle-a").basePackage(CYCLE_A_PKG).build())
                .module(ModuleDefinition.builder("cycle-b").basePackage(CYCLE_B_PKG).build())
                .build();

        String mermaid = new ModuleDependencyGraph(ruleSet).toMermaid(classes);

        assertThat(mermaid)
                .contains("    cycle_a[\"cycle-a\"]\n")
                .contains("    cycle_b[\"cycle-b\"]\n")
                .contains("    cycle_a --> cycle_b\n")
                .contains("    cycle_b --> cycle_a\n");
    }
}
