package io.modulith.rules.testfixtures.spring.billing.internal;

import org.springframework.context.annotation.Configuration;

/**
 * Internal configuration class of the billing test module. Present so the fixture
 * set covers the @Configuration stereotype; none of the current Spring rules key
 * off it, so it participates only as an ordinary module-internal class.
 */
@Configuration
public class BillingConfiguration {
}
