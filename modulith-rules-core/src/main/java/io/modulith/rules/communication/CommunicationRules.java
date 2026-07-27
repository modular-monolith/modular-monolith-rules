package io.modulith.rules.communication;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.modulith.rules.api.CommunicationType;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Factory for ArchUnit rules that enforce inter-module communication contracts.
 *
 * <p>Communication rules verify that the communication patterns used between modules
 * (synchronous calls, asynchronous messaging, or no direct communication) match the
 * contracts declared in each module's {@link io.modulith.rules.api.ModuleDefinition}.
 *
 * <p>Rules are created from a {@link ModulithRuleSet} that describes the module layout.
 * Obtain an instance via {@link io.modulith.rules.ModulithRules#communicationRules()}.
 */
public final class CommunicationRules {

    private static final String[] ASYNC_INFRASTRUCTURE_CLASSES = {
        "org.springframework.context.ApplicationEventPublisher",
        "org.springframework.context.event.EventListener",
        "org.springframework.scheduling.annotation.Async",
        "org.springframework.kafka.core.KafkaTemplate",
        "org.springframework.amqp.rabbit.core.RabbitTemplate",
        "org.springframework.jms.core.JmsTemplate"
    };

    private final ModulithRuleSet ruleSet;

    /**
     * Creates a new {@code CommunicationRules} factory for the given rule set.
     *
     * @param ruleSet the module registry describing the architecture under test
     */
    public CommunicationRules(ModulithRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    /**
     * Returns a rule that verifies modules with an ASYNCHRONOUS communication contract
     * do not make direct synchronous method calls to classes in the target module.
     *
     * <p>For each class in a defined module, all outgoing method calls are inspected.
     * If a call targets a class in another module and the source module declares an
     * ASYNCHRONOUS contract with that target module, the call must go through async
     * infrastructure (events, messages). Any direct call to a non-infrastructure class
     * is reported as a violation. Calls to event classes (simple name ending with
     * "Event") are permitted, because reading a received event's data is part of the
     * asynchronous flow rather than a synchronous call into the target module.
     *
     * @return an ArchUnit rule enforcing async communication contracts
     */
    public ArchRule asyncModulesShouldNotCallDirectly() {
        return classes()
            .that().resideInAnyPackage(allModulePackages())
            .should(new ArchCondition<JavaClass>(
                "not make direct synchronous calls to modules requiring asynchronous communication") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    ModuleDefinition sourceModule = findModuleForClass(javaClass.getName());
                    if (sourceModule == null) {
                        return;
                    }
                    for (JavaMethodCall call : javaClass.getMethodCallsFromSelf()) {
                        String targetClassName = call.getTargetOwner().getName();
                        ModuleDefinition targetModule = findModuleForClass(targetClassName);
                        if (targetModule == null || targetModule.name().equals(sourceModule.name())) {
                            continue;
                        }
                        CommunicationType contract =
                            sourceModule.communicationContracts().get(targetModule.name());
                        if (CommunicationType.ASYNCHRONOUS.equals(contract)
                                && !isAsyncInfrastructureCall(call)) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                asyncViolationMessage(javaClass, call, sourceModule, targetModule)));
                        }
                    }
                }
            });
    }

    /**
     * Returns a rule that verifies modules with a NONE communication contract do not
     * hold any direct dependency on classes in the target module.
     *
     * <p>For each class in a defined module, all direct dependencies are inspected.
     * If a dependency targets a class in another module and the source module declares
     * a NONE contract with that target module, the dependency is reported as a violation.
     *
     * @return an ArchUnit rule enforcing no-communication contracts
     */
    public ArchRule noCommModulesShouldNotInteract() {
        return classes()
            .that().resideInAnyPackage(allModulePackages())
            .should(new ArchCondition<JavaClass>(
                "not interact with modules where the communication contract is NONE") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    ModuleDefinition sourceModule = findModuleForClass(javaClass.getName());
                    if (sourceModule == null) {
                        return;
                    }
                    for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                        String targetClassName = dependency.getTargetClass().getName();
                        ModuleDefinition targetModule = findModuleForClass(targetClassName);
                        if (targetModule == null || targetModule.name().equals(sourceModule.name())) {
                            continue;
                        }
                        CommunicationType contract =
                            sourceModule.communicationContracts().get(targetModule.name());
                        if (CommunicationType.NONE.equals(contract)) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                noneViolationMessage(javaClass, targetClassName, sourceModule, targetModule)));
                        }
                    }
                }
            });
    }

    /**
     * Returns a rule that combines both the async and no-communication contract checks
     * in a single {@link ArchCondition}.
     *
     * <p>This is a convenience method equivalent to applying
     * {@link #asyncModulesShouldNotCallDirectly()} and
     * {@link #noCommModulesShouldNotInteract()} together.
     *
     * @return an ArchUnit rule enforcing all declared communication contracts
     */
    public ArchRule allCommunicationContractsRespected() {
        return classes()
            .that().resideInAnyPackage(allModulePackages())
            .should(new ArchCondition<JavaClass>("respect all declared inter-module communication contracts") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    ModuleDefinition sourceModule = findModuleForClass(javaClass.getName());
                    if (sourceModule == null) {
                        return;
                    }
                    for (JavaMethodCall call : javaClass.getMethodCallsFromSelf()) {
                        String targetClassName = call.getTargetOwner().getName();
                        ModuleDefinition targetModule = findModuleForClass(targetClassName);
                        if (targetModule == null || targetModule.name().equals(sourceModule.name())) {
                            continue;
                        }
                        CommunicationType contract =
                            sourceModule.communicationContracts().get(targetModule.name());
                        if (CommunicationType.ASYNCHRONOUS.equals(contract)
                                && !isAsyncInfrastructureCall(call)) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                asyncViolationMessage(javaClass, call, sourceModule, targetModule)));
                        }
                    }
                    for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                        String targetClassName = dependency.getTargetClass().getName();
                        ModuleDefinition targetModule = findModuleForClass(targetClassName);
                        if (targetModule == null || targetModule.name().equals(sourceModule.name())) {
                            continue;
                        }
                        CommunicationType contract =
                            sourceModule.communicationContracts().get(targetModule.name());
                        if (CommunicationType.NONE.equals(contract)) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                noneViolationMessage(javaClass, targetClassName, sourceModule, targetModule)));
                        }
                    }
                }
            });
    }

    private static String asyncViolationMessage(JavaClass javaClass, JavaMethodCall call,
            ModuleDefinition sourceModule, ModuleDefinition targetModule) {
        String targetClassName = call.getTargetOwner().getName();
        return "Module '" + sourceModule.name() + "': class "
            + javaClass.getName()
            + " makes a direct synchronous call to "
            + targetClassName + "." + call.getName()
            + "() in module '" + targetModule.name()
            + "', but the communication contract requires asynchronous communication."
            + " Fix: publish an application event from module '" + sourceModule.name()
            + "' and handle it in module '" + targetModule.name()
            + "' with an @EventListener, instead of calling "
            + targetClassName + "." + call.getName() + "() directly";
    }

    private static String noneViolationMessage(JavaClass javaClass, String targetClassName,
            ModuleDefinition sourceModule, ModuleDefinition targetModule) {
        return "Module '" + sourceModule.name() + "': class "
            + javaClass.getName()
            + " has a dependency on " + targetClassName
            + " in module '" + targetModule.name()
            + "', but no communication is allowed between these modules."
            + " Fix: remove the dependency on " + targetClassName + " from "
            + javaClass.getName() + ", or declare a communication contract between '"
            + sourceModule.name() + "' and '" + targetModule.name()
            + "' if they are meant to interact";
    }

    private boolean isAsyncInfrastructureCall(JavaMethodCall call) {
        String targetClassName = call.getTargetOwner().getName();
        for (String asyncClass : ASYNC_INFRASTRUCTURE_CLASSES) {
            if (asyncClass.equals(targetClassName)) {
                return true;
            }
        }
        // Reading data from a received event (e.g. event.orderId() in an event
        // listener) is part of the asynchronous flow, not a synchronous call into
        // the target module.
        return call.getTargetOwner().getSimpleName().endsWith("Event");
    }

    private ModuleDefinition findModuleForClass(String className) {
        for (ModuleDefinition module : ruleSet.allModules()) {
            if (module.containsClass(className)) {
                return module;
            }
        }
        return null;
    }

    private String[] allModulePackages() {
        return ruleSet.allModules().stream()
            .map(ModuleDefinition::archUnitPackageIdentifier)
            .toArray(String[]::new);
    }
}
