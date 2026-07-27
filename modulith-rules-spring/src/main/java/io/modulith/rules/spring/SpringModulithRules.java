package io.modulith.rules.spring;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;

import java.util.List;
import java.util.Optional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Factory for ArchUnit rules specific to Spring Boot modular monolith applications.
 *
 * <p>These rules enforce Spring-aware boundaries such as preventing controllers from
 * crossing module lines, keeping repositories module-internal, and ensuring that
 * transactional methods do not span module boundaries.
 *
 * <p>Obtain instances of the individual rules by constructing this class with a
 * {@link ModulithRuleSet}:
 *
 * <pre>{@code
 * SpringModulithRules springRules = new SpringModulithRules(ruleSet);
 * springRules.controllersShouldNotCrossModuleBoundaries().check(importedClasses);
 * }</pre>
 */
public final class SpringModulithRules {

    private static final String CONTROLLER =
            "org.springframework.stereotype.Controller";
    private static final String REST_CONTROLLER =
            "org.springframework.web.bind.annotation.RestController";
    private static final String SERVICE =
            "org.springframework.stereotype.Service";
    private static final String REPOSITORY =
            "org.springframework.stereotype.Repository";
    private static final String COMPONENT =
            "org.springframework.stereotype.Component";
    private static final String TRANSACTIONAL =
            "org.springframework.transaction.annotation.Transactional";
    private static final String EVENT_LISTENER =
            "org.springframework.context.event.EventListener";
    private static final String ASYNC =
            "org.springframework.scheduling.annotation.Async";
    private static final String AUTOWIRED =
            "org.springframework.beans.factory.annotation.Autowired";
    private static final String APPLICATION_EVENT_PUBLISHER =
            "org.springframework.context.ApplicationEventPublisher";

    private final ModulithRuleSet ruleSet;

    /**
     * Creates a new {@code SpringModulithRules} factory for the given rule set.
     *
     * @param ruleSet the module registry describing the architecture under test
     */
    public SpringModulithRules(ModulithRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    /**
     * Creates a {@code SpringModulithRules} factory for the given rule set,
     * mirroring {@code ModulithRules.of(ruleSet)} so both entry points read the same.
     *
     * @param ruleSet the module registry describing the architecture under test
     * @return a new {@code SpringModulithRules} factory
     */
    public static SpringModulithRules of(ModulithRuleSet ruleSet) {
        return new SpringModulithRules(ruleSet);
    }

    /**
     * Returns every Spring-specific rule as a single list, convenient for an
     * {@code @ArchTest} field or a loop over {@code rule.check(classes)}.
     *
     * @return an unmodifiable list of all Spring ArchUnit rules
     */
    public List<ArchRule> allRules() {
        return List.of(
                controllersShouldNotCrossModuleBoundaries(),
                repositoriesShouldBeModuleInternal(),
                transactionalMethodsShouldNotSpanModules(),
                eventClassesShouldBeInApiPackages(),
                noDirectInjectionOfInternalBeans()
        );
    }

    /**
     * Returns a rule that verifies classes annotated with {@code @RestController} or
     * {@code @Controller} do not depend on controllers in other modules.
     *
     * <p>The condition inspects direct dependencies from each controller class. If a
     * dependency target is also annotated with a controller annotation and belongs to a
     * different module, a violation is reported.
     *
     * @return an ArchRule enforcing controller module isolation
     */
    public ArchRule controllersShouldNotCrossModuleBoundaries() {
        DescribedPredicate<JavaClass> isController =
                new DescribedPredicate<JavaClass>("annotated with @Controller or @RestController") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        return javaClass.isAnnotatedWith(CONTROLLER)
                                || javaClass.isAnnotatedWith(REST_CONTROLLER);
                    }
                };

        return classes()
                .that(isController)
                .should(new ArchCondition<JavaClass>("not depend on controllers in other modules") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        Optional<ModuleDefinition> sourceModule = findModuleOf(javaClass);
                        for (Dependency dep : javaClass.getDirectDependenciesFromSelf()) {
                            JavaClass target = dep.getTargetClass();
                            if (target.isAnnotatedWith(CONTROLLER)
                                    || target.isAnnotatedWith(REST_CONTROLLER)) {
                                if (sourceModule.isPresent()) {
                                    Optional<ModuleDefinition> targetModule = findModuleOf(target);
                                    if (targetModule.isPresent()
                                            && !targetModule.get().name().equals(sourceModule.get().name())) {
                                        events.add(SimpleConditionEvent.violated(javaClass,
                                                javaClass.getName() + " depends on controller "
                                                + target.getName() + " in a different module '"
                                                + targetModule.get().name() + "'."
                                                + " Fix: depend on a service interface in "
                                                + targetModule.get().basePackage() + ".api"
                                                + " instead of the controller, or move "
                                                + javaClass.getName() + " into module '"
                                                + targetModule.get().name() + "' if it belongs there"));
                                    }
                                }
                            }
                        }
                    }
                });
    }

    /**
     * Returns a rule that verifies classes annotated with {@code @Repository} are only
     * accessed from within their own module.
     *
     * <p>The condition inspects dependencies to the repository class. If the originating
     * class belongs to a different module, a violation is reported suggesting callers
     * access data through the module's public API instead.
     *
     * @return an ArchRule enforcing repository module-internal access
     */
    public ArchRule repositoriesShouldBeModuleInternal() {
        return classes()
                .that().areAnnotatedWith(REPOSITORY)
                .should(new ArchCondition<JavaClass>("only be accessed from within their own module") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        Optional<ModuleDefinition> repoModule = findModuleOf(javaClass);
                        for (Dependency dep : javaClass.getDirectDependenciesToSelf()) {
                            JavaClass origin = dep.getOriginClass();
                            Optional<ModuleDefinition> originModule = findModuleOf(origin);
                            if (repoModule.isPresent() && originModule.isPresent()
                                    && !originModule.get().name().equals(repoModule.get().name())) {
                                events.add(SimpleConditionEvent.violated(javaClass,
                                        origin.getName() + " accesses repository "
                                        + javaClass.getName() + " from a different module."
                                        + " Fix: expose the data through a service in "
                                        + repoModule.get().basePackage() + ".api and call"
                                        + " that from " + origin.getName() + " instead"));
                            }
                        }
                    }
                });
    }

    /**
     * Returns a rule that verifies classes in defined modules do not have
     * {@code @Transactional} methods that call into a different module.
     *
     * <p>The condition checks each method. If the class or the method is annotated
     * with {@code @Transactional}, each method call made from that method is inspected.
     * A violation is reported when the call target resides in a different module.
     *
     * @return an ArchRule enforcing that transactions do not span module boundaries
     */
    public ArchRule transactionalMethodsShouldNotSpanModules() {
        return classes()
                .that(resideInAnyDefinedModule())
                .should(new ArchCondition<JavaClass>(
                        "not have @Transactional methods calling across module boundaries") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        Optional<ModuleDefinition> sourceModule = findModuleOf(javaClass);
                        if (!sourceModule.isPresent()) {
                            return;
                        }
                        boolean classIsTransactional = javaClass.isAnnotatedWith(TRANSACTIONAL);
                        for (JavaMethod method : javaClass.getMethods()) {
                            if (!classIsTransactional && !method.isAnnotatedWith(TRANSACTIONAL)) {
                                continue;
                            }
                            for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
                                JavaClass targetOwner = call.getTargetOwner();
                                Optional<ModuleDefinition> targetModule = findModuleOf(targetOwner);
                                if (targetModule.isPresent()
                                        && !targetModule.get().name().equals(sourceModule.get().name())) {
                                    events.add(SimpleConditionEvent.violated(javaClass,
                                            javaClass.getName() + "." + method.getName()
                                            + " is @Transactional and calls "
                                            + targetOwner.getName() + " in module '"
                                            + targetModule.get().name()
                                            + "', so the transaction spans both modules."
                                            + " Fix: move the call to " + targetOwner.getName()
                                            + " out of the transactional method, or publish an"
                                            + " event that module '" + targetModule.get().name()
                                            + "' handles after the transaction commits"));
                                }
                            }
                        }
                    }
                });
    }

    /**
     * Returns a rule that verifies event classes (simple name ending with "Event") that
     * are used by other modules reside in the API package of their own module.
     *
     * <p>The condition inspects dependencies to the event class. If any dependency
     * originates from a different module and the event class is not in its module's
     * public API, a violation is reported suggesting to move the event class to the
     * API package.
     *
     * @return an ArchRule enforcing that shared events are declared in API packages
     */
    public ArchRule eventClassesShouldBeInApiPackages() {
        return classes()
                .that(resideInAnyDefinedModule())
                .and().haveSimpleNameEndingWith("Event")
                .should(new ArchCondition<JavaClass>(
                        "be in the API package of their module if used by other modules") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        Optional<ModuleDefinition> eventModule = findModuleOf(javaClass);
                        if (!eventModule.isPresent()) {
                            return;
                        }
                        for (Dependency dep : javaClass.getDirectDependenciesToSelf()) {
                            JavaClass origin = dep.getOriginClass();
                            Optional<ModuleDefinition> originModule = findModuleOf(origin);
                            if (originModule.isPresent()
                                    && !originModule.get().name().equals(eventModule.get().name())) {
                                if (!eventModule.get().isPublicApi(javaClass.getName())) {
                                    events.add(SimpleConditionEvent.violated(javaClass,
                                            javaClass.getName() + " is used by module '"
                                            + originModule.get().name()
                                            + "' but is not in the API package of module '"
                                            + eventModule.get().name() + "'."
                                            + " Fix: move " + javaClass.getSimpleName()
                                            + " to " + eventModule.get().basePackage()
                                            + ".api so other modules can depend on it"));
                                }
                            }
                        }
                    }
                });
    }

    /**
     * Returns a rule that verifies classes in defined modules do not inject internal
     * beans from other modules.
     *
     * <p>The condition checks constructor parameter types and fields annotated with
     * {@code @Autowired}. If a parameter or field type belongs to a different module
     * that has API packages defined and the type is not in that module's public API,
     * a violation is reported.
     *
     * @return an ArchRule enforcing that only public API beans are injected across modules
     */
    public ArchRule noDirectInjectionOfInternalBeans() {
        return classes()
                .that(resideInAnyDefinedModule())
                .should(new ArchCondition<JavaClass>("not inject internal beans from other modules") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        Optional<ModuleDefinition> sourceModule = findModuleOf(javaClass);
                        if (!sourceModule.isPresent()) {
                            return;
                        }
                        javaClass.getConstructors().forEach(constructor ->
                                constructor.getRawParameterTypes().forEach(paramType ->
                                        checkCrossModuleInternalAccess(javaClass, paramType,
                                                sourceModule.get(), "constructor parameter", events)));
                        javaClass.getFields().forEach(field -> {
                            if (field.isAnnotatedWith(AUTOWIRED)) {
                                checkCrossModuleInternalAccess(javaClass, field.getRawType(),
                                        sourceModule.get(), "autowired field", events);
                            }
                        });
                    }
                });
    }

    private void checkCrossModuleInternalAccess(JavaClass sourceClass, JavaClass targetType,
            ModuleDefinition sourceModule, String accessType, ConditionEvents events) {
        Optional<ModuleDefinition> targetModule = findModuleOf(targetType);
        if (targetModule.isPresent()
                && !targetModule.get().name().equals(sourceModule.name())) {
            if (!targetModule.get().apiPackageIdentifiers().isEmpty()
                    && !targetModule.get().isPublicApi(targetType.getName())) {
                events.add(SimpleConditionEvent.violated(sourceClass,
                        sourceClass.getName() + " injects " + targetType.getName()
                        + " via " + accessType + " from module '"
                        + targetModule.get().name()
                        + "' but it is not part of that module's public API."
                        + " Fix: inject an interface from "
                        + targetModule.get().basePackage() + ".api instead, or add "
                        + targetType.getName() + " to the api packages of module '"
                        + targetModule.get().name() + "' if it is meant to be shared"));
            }
        }
    }

    private DescribedPredicate<JavaClass> resideInAnyDefinedModule() {
        return new DescribedPredicate<JavaClass>("reside in any defined module") {
            @Override
            public boolean test(JavaClass javaClass) {
                return findModuleOf(javaClass).isPresent();
            }
        };
    }

    private Optional<ModuleDefinition> findModuleOf(JavaClass javaClass) {
        String className = javaClass.getName();
        for (ModuleDefinition module : ruleSet.allModules()) {
            if (module.containsClass(className)) {
                return Optional.of(module);
            }
        }
        return Optional.empty();
    }
}
