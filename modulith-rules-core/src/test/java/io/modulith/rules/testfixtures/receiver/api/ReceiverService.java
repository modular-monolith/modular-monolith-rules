package io.modulith.rules.testfixtures.receiver.api;

/**
 * Public API interface for the receiver test module.
 */
public interface ReceiverService {

    /**
     * Processes an incoming request. Direct calls to this method from an async-only
     * sender violate the communication contract.
     */
    void process();
}
