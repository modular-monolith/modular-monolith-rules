package com.example.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.modulith.rules.ModulithRules;

import java.util.List;

/**
 * Drop-in architecture test for a modular monolith.
 *
 * <p>Copy this file into {@code src/test/java}, change the root package and the
 * module names on the two marked lines, and run your tests. That is the whole
 * setup. Everything else here is a comment.
 *
 * <p>Three rules run by default: modules only depend on what they have
 * declared, internal packages stay internal, and there are no cycles between
 * modules. With the convention-based setup below, no module declares allowed
 * dependencies, so the first rule has nothing to enforce yet; internal
 * detection falls back to {@code .internal.} and {@code .infrastructure.}
 * packages, and cycle detection is on. When you are ready to say which modules
 * may talk to which, move to {@code ModulithRuleSet} (see the README).
 */
@AnalyzeClasses(packages = "com.example")                              // 1. your root package
class ArchitectureTest {

    @ArchTest
    static final List<ArchRule> modulithRules =
            ModulithRules.forPackage("com.example",                     // 2. the same root package
                            "ordering", "payments", "inventory")        //    and your module names
                    .allRules();
}
