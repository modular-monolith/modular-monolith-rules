package ${package}.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.modulith.rules.ModulithRules;

import java.util.List;

/**
 * Module boundaries for ${artifactId}.
 *
 * <p>Modules are the packages directly under {@code ${package}}. Add a module
 * by creating its package and adding its name to the list below.
 */
@AnalyzeClasses(packages = "${package}")
class ArchitectureTest {

    @ArchTest
    static final List<ArchRule> modulithRules =
            ModulithRules.forPackage("${package}", "ordering", "payments")
                    .allRules();
}
