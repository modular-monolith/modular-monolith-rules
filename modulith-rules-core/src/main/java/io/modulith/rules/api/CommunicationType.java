package io.modulith.rules.api;

/**
 * Describes the permitted communication pattern between two modules.
 *
 * <p>Used in {@link ModuleDefinition#communicationContracts()} to declare how a module
 * is allowed to interact with each of its dependencies. Rules can enforce that only the
 * declared pattern is used in the actual code.
 */
public enum CommunicationType {

    /**
     * The module communicates with its dependency synchronously, for example via direct
     * method calls, REST clients, or gRPC stubs. The caller blocks until the response
     * is received.
     */
    SYNCHRONOUS,

    /**
     * The module communicates with its dependency asynchronously, for example via a
     * message broker, an event bus, or a reactive stream. The caller does not block
     * waiting for a response.
     */
    ASYNCHRONOUS,

    /**
     * No runtime communication is permitted between the two modules. They may share
     * compile-time types (such as API value objects), but no call or message should
     * flow from one to the other at runtime.
     */
    NONE
}
