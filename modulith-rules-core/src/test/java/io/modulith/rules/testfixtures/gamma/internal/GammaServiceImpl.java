package io.modulith.rules.testfixtures.gamma.internal;

import io.modulith.rules.testfixtures.alpha.internal.AlphaServiceImpl;
import io.modulith.rules.testfixtures.beta.api.BetaService;
import io.modulith.rules.testfixtures.gamma.api.GammaService;

/**
 * Internal implementation of GammaService.
 *
 * <p>Intentionally holds a reference to AlphaServiceImpl (an internal class from another module)
 * to create a boundary violation for test scenarios. Also holds a reference to BetaService
 * (a public API class) to demonstrate valid cross-module API access.
 */
public class GammaServiceImpl implements GammaService {

    /** Direct reference to alpha internals - this is the violation under test. */
    private AlphaServiceImpl alphaServiceImpl;

    /** Reference to beta public API - this is valid cross-module access. */
    private BetaService betaService;
}
