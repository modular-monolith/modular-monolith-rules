package io.modulith.rules.testfixtures.cycleB.internal;

import io.modulith.rules.testfixtures.cycleA.api.CycleAService;
import io.modulith.rules.testfixtures.cycleB.api.CycleBService;

/**
 * Internal implementation of CycleBService. Depends on CycleAService from cycleA, which
 * creates the second half of the A -> B -> A circular dependency used in cycle detection tests.
 */
public class CycleBServiceImpl implements CycleBService {

    /** Dependency on cycleA creates the cycleB -> cycleA back edge, completing the cycle. */
    private CycleAService cycleAService;
}
