package io.modulith.rules.cycle;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for {@link CycleRules}.
 *
 * <p>Tests both the ArchUnit rule methods (which scan real class files) and the static
 * {@link CycleRules#detectCycles(Map)} algorithm with hand-built dependency graphs.
 *
 * <p>Fixture layout:
 * <ul>
 *   <li>alpha, beta, gamma - acyclic modules; gamma depends on both alpha and beta
 *   <li>cycleA, cycleB - two modules with a mutual dependency forming a cycle
 * </ul>
 */
class CycleRulesTest {

    private static final String ALPHA_PKG  = "io.modulith.rules.testfixtures.alpha";
    private static final String BETA_PKG   = "io.modulith.rules.testfixtures.beta";
    private static final String GAMMA_PKG  = "io.modulith.rules.testfixtures.gamma";
    private static final String CYCLE_A_PKG = "io.modulith.rules.testfixtures.cycleA";
    private static final String CYCLE_B_PKG = "io.modulith.rules.testfixtures.cycleB";

    // ---------------------------------------------------------------------------
    // noModuleCycles - ArchUnit rule tests
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("noModuleCycles passes when the module graph is acyclic (alpha, beta, gamma)")
    void noCycles_shouldPass_whenNoCyclesExist() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages(ALPHA_PKG, BETA_PKG, GAMMA_PKG);

        // gamma -> alpha and gamma -> beta, but neither alpha nor beta depends on gamma
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(ModuleDefinition.builder("alpha").basePackage(ALPHA_PKG).build())
                .module(ModuleDefinition.builder("beta").basePackage(BETA_PKG).build())
                .module(ModuleDefinition.builder("gamma").basePackage(GAMMA_PKG).build())
                .build();

        CycleRules rules = new CycleRules(ruleSet);
        assertDoesNotThrow(() -> rules.noModuleCycles().check(classes));
    }

    @Test
    @DisplayName("noModuleCycles fails when cycleA and cycleB have a mutual dependency")
    void noCycles_shouldFail_whenCycleExists() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages(CYCLE_A_PKG, CYCLE_B_PKG);

        // cycleA -> cycleB (CycleAServiceImpl depends on CycleBService)
        // cycleB -> cycleA (CycleBServiceImpl depends on CycleAService)
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(ModuleDefinition.builder("cycleA").basePackage(CYCLE_A_PKG).build())
                .module(ModuleDefinition.builder("cycleB").basePackage(CYCLE_B_PKG).build())
                .build();

        CycleRules rules = new CycleRules(ruleSet);
        assertThatThrownBy(() -> rules.noModuleCycles().check(classes))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("noModuleCycles failure message contains both module names in the cycle path")
    void noCycles_failureMessage_shouldContainCyclePath() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages(CYCLE_A_PKG, CYCLE_B_PKG);

        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(ModuleDefinition.builder("cycleA").basePackage(CYCLE_A_PKG).build())
                .module(ModuleDefinition.builder("cycleB").basePackage(CYCLE_B_PKG).build())
                .build();

        CycleRules rules = new CycleRules(ruleSet);
        assertThatThrownBy(() -> rules.noModuleCycles().check(classes))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("cycleA")
                .hasMessageContaining("cycleB");
    }

    @Test
    @DisplayName("moduleHasNoCycles passes for alpha because alpha is not part of any cycle")
    void moduleHasNoCycles_shouldPass_forModuleNotInCycle() {
        // cycleA <-> cycleB have a cycle, but alpha is standalone and not involved
        JavaClasses classes = new ClassFileImporter()
                .importPackages(ALPHA_PKG, CYCLE_A_PKG, CYCLE_B_PKG);

        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(ModuleDefinition.builder("alpha").basePackage(ALPHA_PKG).build())
                .module(ModuleDefinition.builder("cycleA").basePackage(CYCLE_A_PKG).build())
                .module(ModuleDefinition.builder("cycleB").basePackage(CYCLE_B_PKG).build())
                .build();

        CycleRules rules = new CycleRules(ruleSet);
        assertDoesNotThrow(() -> rules.moduleHasNoCycles("alpha").check(classes));
    }

    @Test
    @DisplayName("moduleHasNoCycles fails for cycleA because it participates in a cycle with cycleB")
    void moduleHasNoCycles_shouldFail_forModuleInCycle() {
        JavaClasses classes = new ClassFileImporter()
                .importPackages(CYCLE_A_PKG, CYCLE_B_PKG);

        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("io.modulith.rules.testfixtures")
                .module(ModuleDefinition.builder("cycleA").basePackage(CYCLE_A_PKG).build())
                .module(ModuleDefinition.builder("cycleB").basePackage(CYCLE_B_PKG).build())
                .build();

        CycleRules rules = new CycleRules(ruleSet);
        assertThatThrownBy(() -> rules.moduleHasNoCycles("cycleA").check(classes))
                .isInstanceOf(AssertionError.class);
    }

    // ---------------------------------------------------------------------------
    // detectCycles - unit tests on the static algorithm
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("detectCycles returns a single cycle for a simple A -> B -> A graph")
    void detectCycles_unitTest_shouldFindSimpleCycle() {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("A", new HashSet<>(Set.of("B")));
        graph.put("B", new HashSet<>(Set.of("A")));

        List<List<String>> cycles = CycleRules.detectCycles(graph);

        assertThat(cycles).hasSize(1);
        List<String> cycle = cycles.get(0);
        assertThat(cycle).contains("A", "B");
        // The cycle ends with the start node repeated (e.g., [A, B, A])
        assertThat(cycle.get(0)).isEqualTo(cycle.get(cycle.size() - 1));
    }

    @Test
    @DisplayName("detectCycles returns an empty list when the graph has no cycles")
    void detectCycles_unitTest_shouldReturnEmpty_whenNoCycles() {
        // A -> B -> C, no back edges
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("A", new HashSet<>(Set.of("B")));
        graph.put("B", new HashSet<>(Set.of("C")));
        graph.put("C", new HashSet<>());

        List<List<String>> cycles = CycleRules.detectCycles(graph);

        assertThat(cycles).isEmpty();
    }

    @Test
    @DisplayName("detectCycles finds both cycles when the graph contains two independent cycles")
    void detectCycles_unitTest_shouldFindMultipleCycles() {
        // Cycle 1: A -> B -> A
        // Cycle 2: C -> D -> C
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("A", new HashSet<>(Set.of("B")));
        graph.put("B", new HashSet<>(Set.of("A")));
        graph.put("C", new HashSet<>(Set.of("D")));
        graph.put("D", new HashSet<>(Set.of("C")));

        List<List<String>> cycles = CycleRules.detectCycles(graph);

        assertThat(cycles).hasSize(2);
        // Each cycle should include both nodes from its pair
        List<String> allNodes = cycles.stream()
                .flatMap(List::stream)
                .toList();
        assertThat(allNodes).contains("A", "B", "C", "D");
    }
}
