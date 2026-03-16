package io.modulith.rules.boundary;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory for ArchUnit rules that enforce module boundary constraints.
 *
 * <p>Boundary rules verify that code in one module does not access internal packages
 * of another module, and that inter-module dependencies only flow through declared
 * public API packages.
 *
 * <p>Rules are created from a {@link ModulithRuleSet} that describes the module layout.
 * Obtain an instance via {@link io.modulith.rules.ModulithRules#boundaryRules()}.
 */
public final class BoundaryRules {

    private final ModulithRuleSet ruleSet;

    /**
     * Creates a new {@code BoundaryRules} factory for the given rule set.
     *
     * @param ruleSet the module registry describing the architecture under test
     */
    public BoundaryRules(ModulithRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    /**
     * Returns a rule that verifies each module only depends on modules explicitly
     * declared in its {@code allowedDependencies}. Modules with an empty
     * {@code allowedDependencies} set are treated as unrestricted and are skipped.
     *
     * <p>Violation message format:
     * <pre>
     * Module 'ordering': class com.example.ordering.internal.OrderServiceImpl depends on
     * com.example.notifications.internal.EmailSender in module 'notifications', but
     * 'notifications' is not in the allowed dependencies [inventory, payments]
     * </pre>
     *
     * @return an ArchUnit rule enforcing allowed dependency declarations
     */
    public ArchRule modulesOnlyDependOnAllowedModules() {
        return ArchRuleDefinition.classes()
                .that(resideInAnyDefinedModule())
                .should(new ArchCondition<JavaClass>("only depend on modules declared in allowedDependencies") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        Optional<ModuleDefinition> sourceModuleOpt = findModuleOf(javaClass);
                        if (!sourceModuleOpt.isPresent()) {
                            return;
                        }
                        ModuleDefinition sourceModule = sourceModuleOpt.get();
                        if (sourceModule.allowedDependencies().isEmpty()) {
                            return;
                        }
                        for (JavaClass dependency : directDependencyTargets(javaClass)) {
                            Optional<ModuleDefinition> targetModuleOpt = findModuleOf(dependency);
                            if (!targetModuleOpt.isPresent()) {
                                continue;
                            }
                            ModuleDefinition targetModule = targetModuleOpt.get();
                            if (targetModule.name().equals(sourceModule.name())) {
                                continue;
                            }
                            if (!sourceModule.allowedDependencies().contains(targetModule.name())) {
                                List<String> sortedAllowed = new ArrayList<>(sourceModule.allowedDependencies());
                                Collections.sort(sortedAllowed);
                                String message = String.format(
                                        "Module '%s': class %s depends on %s in module '%s',"
                                        + " but '%s' is not in the allowed dependencies %s",
                                        sourceModule.name(),
                                        javaClass.getName(),
                                        dependency.getName(),
                                        targetModule.name(),
                                        targetModule.name(),
                                        sortedAllowed
                                );
                                events.add(SimpleConditionEvent.violated(javaClass, message));
                            }
                        }
                    }
                });
    }

    /**
     * Returns a rule that verifies no class in any defined module accesses the internal
     * packages of another module. Internal packages are identified by the target module's
     * configured {@code internalPackageIdentifiers}, or by conventional patterns such as
     * {@code .internal.} and {@code .infrastructure.} when none are configured.
     *
     * <p>Violation messages instruct consumers to use the public API instead.
     *
     * @return an ArchUnit rule enforcing internal package protection
     */
    public ArchRule internalsShouldNotBeAccessedFromOutside() {
        return ArchRuleDefinition.classes()
                .that(resideInAnyDefinedModule())
                .should(new ArchCondition<JavaClass>("not access internal packages of other modules") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        Optional<ModuleDefinition> sourceModuleOpt = findModuleOf(javaClass);
                        if (!sourceModuleOpt.isPresent()) {
                            return;
                        }
                        ModuleDefinition sourceModule = sourceModuleOpt.get();
                        for (JavaClass dependency : directDependencyTargets(javaClass)) {
                            Optional<ModuleDefinition> targetModuleOpt = findModuleOf(dependency);
                            if (!targetModuleOpt.isPresent()) {
                                continue;
                            }
                            ModuleDefinition targetModule = targetModuleOpt.get();
                            if (targetModule.name().equals(sourceModule.name())) {
                                continue;
                            }
                            if (isInInternalPackage(dependency, targetModule)) {
                                String message = String.format(
                                        "Class %s in module '%s' accesses %s which is in the"
                                        + " internal package of module '%s'. Use the public API instead.",
                                        javaClass.getName(),
                                        sourceModule.name(),
                                        dependency.getName(),
                                        targetModule.name()
                                );
                                events.add(SimpleConditionEvent.violated(javaClass, message));
                            }
                        }
                    }
                });
    }

    /**
     * Returns a rule that verifies all cross-module access goes through the target
     * module's declared API packages. Modules without any configured API packages are
     * skipped by this rule.
     *
     * <p>Violation messages list the expected API packages to guide consumers toward
     * the correct entry points.
     *
     * @return an ArchUnit rule enforcing API-only cross-module access
     */
    public ArchRule crossModuleAccessOnlyThroughApi() {
        return ArchRuleDefinition.classes()
                .that(resideInAnyDefinedModule())
                .should(new ArchCondition<JavaClass>("only access other modules through their public API") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        Optional<ModuleDefinition> sourceModuleOpt = findModuleOf(javaClass);
                        if (!sourceModuleOpt.isPresent()) {
                            return;
                        }
                        ModuleDefinition sourceModule = sourceModuleOpt.get();
                        for (JavaClass dependency : directDependencyTargets(javaClass)) {
                            Optional<ModuleDefinition> targetModuleOpt = findModuleOf(dependency);
                            if (!targetModuleOpt.isPresent()) {
                                continue;
                            }
                            ModuleDefinition targetModule = targetModuleOpt.get();
                            if (targetModule.name().equals(sourceModule.name())) {
                                continue;
                            }
                            if (targetModule.apiPackageIdentifiers().isEmpty()) {
                                continue;
                            }
                            if (!targetModule.isPublicApi(dependency.getName())) {
                                String message = String.format(
                                        "Class %s in module '%s' accesses %s in module '%s'"
                                        + " outside the public API. Expected API packages: %s",
                                        javaClass.getName(),
                                        sourceModule.name(),
                                        dependency.getName(),
                                        targetModule.name(),
                                        targetModule.archUnitApiPackageIdentifiers()
                                );
                                events.add(SimpleConditionEvent.violated(javaClass, message));
                            }
                        }
                    }
                });
    }

    /**
     * Returns a rule that verifies the named module has zero outgoing dependencies
     * to any other defined module.
     *
     * @param moduleName the name of the module that must remain dependency-free
     * @return an ArchUnit rule asserting the module has no outgoing module dependencies
     * @throws IllegalArgumentException if no module with the given name exists in the rule set
     */
    public ArchRule moduleShouldHaveNoDependencies(String moduleName) {
        ModuleDefinition module = ruleSet.module(moduleName);
        return ArchRuleDefinition.classes()
                .that(JavaClass.Predicates.resideInAPackage(module.archUnitPackageIdentifier()))
                .should(new ArchCondition<JavaClass>(
                        "have no outgoing dependencies to other defined modules") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        for (JavaClass dependency : directDependencyTargets(javaClass)) {
                            Optional<ModuleDefinition> targetModuleOpt = findModuleOf(dependency);
                            if (!targetModuleOpt.isPresent()) {
                                continue;
                            }
                            if (!targetModuleOpt.get().name().equals(moduleName)) {
                                String message = String.format(
                                        "Module '%s' should have no dependencies, but class %s"
                                        + " depends on %s in module '%s'",
                                        moduleName,
                                        javaClass.getName(),
                                        dependency.getName(),
                                        targetModuleOpt.get().name()
                                );
                                events.add(SimpleConditionEvent.violated(javaClass, message));
                            }
                        }
                    }
                });
    }

    /**
     * Returns a rule that verifies the named module is only accessed by the listed
     * accessor modules. Any other defined module that holds a dependency on the target
     * module will cause a violation.
     *
     * @param moduleName          the name of the module whose access should be restricted
     * @param accessorModuleNames the names of modules that are permitted to access it
     * @return an ArchUnit rule restricting which modules may access the target module
     * @throws IllegalArgumentException if no module with the given name exists in the rule set
     */
    public ArchRule moduleShouldOnlyBeAccessedBy(String moduleName, String... accessorModuleNames) {
        ModuleDefinition targetModule = ruleSet.module(moduleName);
        Set<String> allowedAccessors = new HashSet<>(Arrays.asList(accessorModuleNames));
        return ArchRuleDefinition.classes()
                .that(resideInAnyDefinedModule())
                .should(new ArchCondition<JavaClass>(
                        "only be accessed by " + Arrays.asList(accessorModuleNames)) {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        Optional<ModuleDefinition> sourceModuleOpt = findModuleOf(javaClass);
                        if (!sourceModuleOpt.isPresent()) {
                            return;
                        }
                        ModuleDefinition sourceModule = sourceModuleOpt.get();
                        if (sourceModule.name().equals(moduleName)) {
                            return;
                        }
                        for (JavaClass dependency : directDependencyTargets(javaClass)) {
                            if (targetModule.containsClass(dependency.getName())
                                    && !allowedAccessors.contains(sourceModule.name())) {
                                String message = String.format(
                                        "Module '%s' should only be accessed by %s,"
                                        + " but class %s in module '%s' accesses %s",
                                        moduleName,
                                        Arrays.asList(accessorModuleNames),
                                        javaClass.getName(),
                                        sourceModule.name(),
                                        dependency.getName()
                                );
                                events.add(SimpleConditionEvent.violated(javaClass, message));
                            }
                        }
                    }
                });
    }

    /**
     * Returns a rule that verifies no module accesses the internal packages of another module.
     * Delegates to {@link #internalsShouldNotBeAccessedFromOutside()}.
     *
     * @return an ArchUnit rule enforcing internal package protection
     */
    public ArchRule noModuleAccessesInternalsOfOthers() {
        return internalsShouldNotBeAccessedFromOutside();
    }

    /**
     * Returns a rule that verifies each module only depends on modules declared in its
     * {@code allowedDependencies}. Delegates to {@link #modulesOnlyDependOnAllowedModules()}.
     *
     * @return an ArchUnit rule enforcing allowed dependency declarations
     */
    public ArchRule onlyAllowedDependenciesAreUsed() {
        return modulesOnlyDependOnAllowedModules();
    }

    /**
     * Returns a predicate matching any class that resides in a package belonging to
     * one of the defined modules, using ArchUnit package wildcard identifiers.
     *
     * @return a described predicate for classes inside any defined module
     */
    private DescribedPredicate<JavaClass> resideInAnyDefinedModule() {
        String[] packages = ruleSet.allModules().stream()
                .map(ModuleDefinition::archUnitPackageIdentifier)
                .toArray(String[]::new);
        return JavaClass.Predicates.resideInAnyPackage(packages);
    }

    /**
     * Finds the {@link ModuleDefinition} that contains the given class, by checking
     * each registered module's {@code containsClass} method.
     *
     * @param javaClass the class to locate
     * @return an {@code Optional} containing the owning module, or empty if none matches
     */
    private Optional<ModuleDefinition> findModuleOf(JavaClass javaClass) {
        return ruleSet.allModules().stream()
                .filter(m -> m.containsClass(javaClass.getName()))
                .findFirst();
    }

    /**
     * Determines whether the given class resides in an internal package of its module.
     * If the module has configured {@code internalPackageIdentifiers}, those are used.
     * Otherwise, conventional sub-package segments {@code .internal.} and
     * {@code .infrastructure.} are checked against the class name relative to the
     * module's base package.
     *
     * @param javaClass the class to inspect
     * @param module    the module that owns the class
     * @return {@code true} if the class is considered internal to the module
     */
    private boolean isInInternalPackage(JavaClass javaClass, ModuleDefinition module) {
        String className = javaClass.getName();
        Set<String> identifiers = module.internalPackageIdentifiers();
        if (!identifiers.isEmpty()) {
            for (String identifier : identifiers) {
                if (className.contains(identifier)) {
                    return true;
                }
            }
            return false;
        }
        String relativePath = className.startsWith(module.basePackage())
                ? className.substring(module.basePackage().length())
                : className;
        return relativePath.contains(".internal.") || relativePath.contains(".infrastructure.");
    }

    /**
     * Returns the set of classes that the given class directly depends on.
     *
     * @param javaClass the class whose direct dependencies are collected
     * @return set of target classes from all direct dependencies
     */
    private Set<JavaClass> directDependencyTargets(JavaClass javaClass) {
        return javaClass.getDirectDependenciesFromSelf().stream()
                .map(dep -> dep.getTargetClass())
                .collect(Collectors.toSet());
    }

}
