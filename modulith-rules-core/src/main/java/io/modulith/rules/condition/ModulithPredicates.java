package io.modulith.rules.condition;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;

import java.util.Set;

/**
 * Utility class providing reusable ArchUnit predicates for module-aware architecture rules.
 *
 * <p>All methods are static factory methods that return {@link DescribedPredicate} instances
 * scoped to a given {@link ModulithRuleSet}. These predicates can be combined with ArchUnit's
 * fluent API to build targeted architecture rules.
 *
 * <p>Example usage:
 * <pre>{@code
 * ArchRule rule = classes()
 *         .that(ModulithPredicates.arePublicApiOf(ruleSet, "orders"))
 *         .should().beAnnotatedWith(ApiController.class);
 * }</pre>
 */
public final class ModulithPredicates {

    private ModulithPredicates() {
    }

    /**
     * Returns a predicate that matches classes residing in any module defined in the
     * given rule set, using ArchUnit's package matching with {@code ..} wildcards.
     *
     * @param ruleSet the module registry
     * @return a predicate matching classes in any defined module
     */
    public static DescribedPredicate<JavaClass> resideInAnyModule(ModulithRuleSet ruleSet) {
        String[] packageIdentifiers = ruleSet.allModules().stream()
            .map(ModuleDefinition::archUnitPackageIdentifier)
            .toArray(String[]::new);
        return JavaClass.Predicates.resideInAnyPackage(packageIdentifiers)
            .as("reside in any defined module");
    }

    /**
     * Returns a predicate that matches classes residing in the specified module,
     * using ArchUnit's package matching with the {@code ..} wildcard.
     *
     * @param ruleSet the module registry
     * @param moduleName the name of the module to match
     * @return a predicate matching classes in the given module
     */
    public static DescribedPredicate<JavaClass> resideInModule(ModulithRuleSet ruleSet, String moduleName) {
        ModuleDefinition module = ruleSet.module(moduleName);
        return JavaClass.Predicates.resideInAPackage(module.archUnitPackageIdentifier())
            .as("reside in module '" + moduleName + "'");
    }

    /**
     * Returns a predicate that matches classes that are part of the public API of the
     * specified module, as determined by {@link ModuleDefinition#isPublicApi(String)}.
     *
     * @param ruleSet the module registry
     * @param moduleName the name of the module whose public API to match
     * @return a predicate matching public API classes of the given module
     */
    public static DescribedPredicate<JavaClass> arePublicApiOf(ModulithRuleSet ruleSet, String moduleName) {
        ModuleDefinition module = ruleSet.module(moduleName);
        return new DescribedPredicate<JavaClass>("are public API of module '" + moduleName + "'") {
            @Override
            public boolean test(JavaClass javaClass) {
                return module.isPublicApi(javaClass.getName());
            }
        };
    }

    /**
     * Returns a predicate that matches classes that belong to any module and reside in
     * an internal package segment, but are not part of the module's public API.
     *
     * <p>A class is considered internal if it belongs to a module, its fully-qualified
     * name starts with one of the module's resolved internal package prefixes, and it
     * is not exposed through the module's public API packages.
     *
     * @param ruleSet the module registry
     * @return a predicate matching internal classes across all defined modules
     */
    public static DescribedPredicate<JavaClass> areInternalToAnyModule(ModulithRuleSet ruleSet) {
        return new DescribedPredicate<JavaClass>("are internal to any defined module") {
            @Override
            public boolean test(JavaClass javaClass) {
                for (ModuleDefinition module : ruleSet.allModules()) {
                    if (!module.containsClass(javaClass.getName())) {
                        continue;
                    }
                    Set<String> internalPatterns = module.archUnitInternalPackageIdentifiers();
                    if (internalPatterns.isEmpty()) {
                        continue;
                    }
                    boolean inInternalPackage = false;
                    for (String pattern : internalPatterns) {
                        String prefix = pattern.endsWith("..")
                            ? pattern.substring(0, pattern.length() - 2)
                            : pattern;
                        if (javaClass.getName().startsWith(prefix)) {
                            inInternalPackage = true;
                            break;
                        }
                    }
                    if (inInternalPackage && !module.isPublicApi(javaClass.getName())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Returns a predicate that matches classes annotated with the given annotation,
     * identified by its fully-qualified class name.
     *
     * <p>This is useful for matching classes marked with custom module boundary
     * annotations such as {@code @ModuleApi} or {@code @ModuleInternal}.
     * The predicate description uses the simple name extracted from the fully-qualified
     * annotation name.
     *
     * @param annotationFqn the fully-qualified name of the annotation to match
     * @return a predicate matching classes carrying the specified annotation
     */
    public static DescribedPredicate<JavaClass> annotatedWith(String annotationFqn) {
        String simpleName = annotationFqn.contains(".")
            ? annotationFqn.substring(annotationFqn.lastIndexOf('.') + 1)
            : annotationFqn;
        return new DescribedPredicate<JavaClass>("annotated with @" + simpleName) {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getAnnotations().stream()
                    .anyMatch(annotation -> annotation.getRawType().getName().equals(annotationFqn));
            }
        };
    }
}
