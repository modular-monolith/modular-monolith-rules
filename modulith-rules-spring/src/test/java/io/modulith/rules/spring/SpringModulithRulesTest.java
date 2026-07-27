package io.modulith.rules.spring;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for {@link SpringModulithRules}.
 *
 * <p>Uses compiled Spring-annotated test fixture classes under
 * io.modulith.rules.testfixtures.spring to exercise each rule in both passing and
 * failing scenarios. Like the core rule tests, these use a plain
 * {@link ClassFileImporter} with per-test module definitions rather than the
 * {@code @AnalyzeClasses}/{@code @ArchTest} style, because each test needs its own
 * combination of imported packages and rule set.
 *
 * <p>Fixture layout (each module mirrors the Day 1 api/internal split, plus a web
 * sub-package for controllers):
 * <ul>
 *   <li>payments - target module: PaymentService + PaymentConfirmedEvent in api,
 *       PaymentLedgerRepository (@Repository), PaymentServiceImpl (@Service) and
 *       PaymentSettledEvent in internal, PaymentController (@Controller) in web
 *   <li>orders - violating module: OrderController (@RestController) depends on
 *       PaymentController; OrderServiceImpl (@Service) injects the internal
 *       PaymentLedgerRepository, calls PaymentService from a @Transactional method,
 *       and listens to the internal PaymentSettledEvent
 *   <li>billing - clean module: BillingController (@RestController) and
 *       BillingServiceImpl (@Service) only touch payments through its api package,
 *       keep transactions module-local, and listen to the api-level
 *       PaymentConfirmedEvent
 * </ul>
 *
 * <p>Note: the current rules key off @Controller/@RestController, @Repository,
 * @Transactional and @Autowired plus constructor injection. @Service, @Component,
 * @Configuration, @Async, @Scheduled and @EventListener are not inspected by any
 * rule; the fixtures still carry @Service/@Configuration/@EventListener so the
 * arrangements look like real Spring code.
 */
class SpringModulithRulesTest {

    private static final String FIXTURE_ROOT = "io.modulith.rules.testfixtures.spring";
    private static final String ORDERS_PKG   = FIXTURE_ROOT + ".orders";
    private static final String PAYMENTS_PKG = FIXTURE_ROOT + ".payments";
    private static final String BILLING_PKG  = FIXTURE_ROOT + ".billing";

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /** Classes containing the violating arrangements: orders crossing into payments. */
    private static JavaClasses violatingClasses() {
        return new ClassFileImporter().importPackages(ORDERS_PKG, PAYMENTS_PKG);
    }

    /** Classes containing the clean arrangements: billing using payments only via its API. */
    private static JavaClasses cleanClasses() {
        return new ClassFileImporter().importPackages(BILLING_PKG, PAYMENTS_PKG);
    }

    private static ModuleDefinition ordersModule() {
        return ModuleDefinition.builder("orders")
                .basePackage(ORDERS_PKG)
                .apiPackages(".api.")
                .internalPackages(".internal.")
                .build();
    }

    private static ModuleDefinition paymentsModule() {
        return ModuleDefinition.builder("payments")
                .basePackage(PAYMENTS_PKG)
                .apiPackages(".api.")
                .internalPackages(".internal.")
                .build();
    }

    private static ModuleDefinition billingModule() {
        return ModuleDefinition.builder("billing")
                .basePackage(BILLING_PKG)
                .apiPackages(".api.")
                .internalPackages(".internal.")
                .build();
    }

    private static SpringModulithRules violatingRules() {
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage(FIXTURE_ROOT)
                .module(ordersModule())
                .module(paymentsModule())
                .build();
        return new SpringModulithRules(ruleSet);
    }

    private static SpringModulithRules cleanRules() {
        ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage(FIXTURE_ROOT)
                .module(billingModule())
                .module(paymentsModule())
                .build();
        return new SpringModulithRules(ruleSet);
    }

    // ---------------------------------------------------------------------------
    // controllersShouldNotCrossModuleBoundaries
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("controllersShouldNotCrossModuleBoundaries passes when BillingController only depends on services")
    void controllers_shouldPass_whenControllerOnlyDependsOnServicesAndOwnModule() {
        assertDoesNotThrow(() ->
                cleanRules().controllersShouldNotCrossModuleBoundaries().check(cleanClasses()));
    }

    @Test
    @DisplayName("controllersShouldNotCrossModuleBoundaries fails when OrderController depends on PaymentController")
    void controllers_shouldFail_whenControllerDependsOnControllerInOtherModule() {
        assertThatThrownBy(() ->
                violatingRules().controllersShouldNotCrossModuleBoundaries().check(violatingClasses()))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Fix:")
                .hasMessageContaining("OrderController")
                .hasMessageContaining("PaymentController");
    }

    // ---------------------------------------------------------------------------
    // repositoriesShouldBeModuleInternal
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("repositoriesShouldBeModuleInternal passes when repositories are only used inside their module")
    void repositories_shouldPass_whenRepositoryOnlyAccessedFromOwnModule() {
        assertDoesNotThrow(() ->
                cleanRules().repositoriesShouldBeModuleInternal().check(cleanClasses()));
    }

    @Test
    @DisplayName("repositoriesShouldBeModuleInternal fails when OrderServiceImpl uses PaymentLedgerRepository")
    void repositories_shouldFail_whenRepositoryAccessedFromOtherModule() {
        assertThatThrownBy(() ->
                violatingRules().repositoriesShouldBeModuleInternal().check(violatingClasses()))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Fix:")
                .hasMessageContaining("OrderServiceImpl")
                .hasMessageContaining("PaymentLedgerRepository");
    }

    // ---------------------------------------------------------------------------
    // transactionalMethodsShouldNotSpanModules
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("transactionalMethodsShouldNotSpanModules passes when @Transactional methods stay module-local")
    void transactional_shouldPass_whenTransactionalMethodsStayWithinModule() {
        assertDoesNotThrow(() ->
                cleanRules().transactionalMethodsShouldNotSpanModules().check(cleanClasses()));
    }

    @Test
    @DisplayName("transactionalMethodsShouldNotSpanModules fails when placeOrder() calls the payments module")
    void transactional_shouldFail_whenTransactionalMethodCallsOtherModule() {
        assertThatThrownBy(() ->
                violatingRules().transactionalMethodsShouldNotSpanModules().check(violatingClasses()))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Fix:")
                .hasMessageContaining("placeOrder")
                .hasMessageContaining("payments");
    }

    // ---------------------------------------------------------------------------
    // eventClassesShouldBeInApiPackages
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("eventClassesShouldBeInApiPackages passes when PaymentConfirmedEvent in payments.api is shared")
    void events_shouldPass_whenSharedEventResidesInApiPackage() {
        assertDoesNotThrow(() ->
                cleanRules().eventClassesShouldBeInApiPackages().check(cleanClasses()));
    }

    @Test
    @DisplayName("eventClassesShouldBeInApiPackages fails when orders listens to PaymentSettledEvent in payments.internal")
    void events_shouldFail_whenSharedEventResidesInInternalPackage() {
        assertThatThrownBy(() ->
                violatingRules().eventClassesShouldBeInApiPackages().check(violatingClasses()))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Fix:")
                .hasMessageContaining("PaymentSettledEvent")
                .hasMessageContaining("orders");
    }

    // ---------------------------------------------------------------------------
    // noDirectInjectionOfInternalBeans
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("noDirectInjectionOfInternalBeans passes when only public API beans are injected across modules")
    void injection_shouldPass_whenOnlyPublicApiBeansInjectedAcrossModules() {
        assertDoesNotThrow(() ->
                cleanRules().noDirectInjectionOfInternalBeans().check(cleanClasses()));
    }

    @Test
    @DisplayName("noDirectInjectionOfInternalBeans fails when OrderServiceImpl injects PaymentLedgerRepository")
    void injection_shouldFail_whenInternalBeanInjectedFromOtherModule() {
        assertThatThrownBy(() ->
                violatingRules().noDirectInjectionOfInternalBeans().check(violatingClasses()))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Fix:")
                .hasMessageContaining("OrderServiceImpl")
                .hasMessageContaining("PaymentLedgerRepository");
    }
}
