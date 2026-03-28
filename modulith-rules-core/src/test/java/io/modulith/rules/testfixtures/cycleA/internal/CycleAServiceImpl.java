package io.modulith.rules.testfixtures.cycleA.internal;

import io.modulith.rules.testfixtures.cycleA.api.CycleAService;
import io.modulith.rules.testfixtures.cycleB.api.CycleBService;

/**
 * Internal implementation of CycleAService. Depends on CycleBService from cycleB, which
 * creates half of the A -> B -> A circular dependency used in cycle detection tests.
 */
public class CycleAServiceImpl implements CycleAService {

    /** Dependency on cycleB creates the cycleA -> cycleB edge in the module graph. */
    private CycleBService cycleBService;
}
