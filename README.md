# Modulith Rules

Pre-built ArchUnit rules for enforcing module boundaries in modular monolith architectures.

## Why?

ArchUnit is powerful but general-purpose. Every team building a modular monolith writes the same custom rules: no cycles, respect module boundaries, protect internal packages, enforce communication contracts. Modulith Rules packages these as a reusable, opinionated library with a fluent API that maps directly to modular monolith concepts.

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>io.github.modulith</groupId>
    <artifactId>modulith-rules-core</artifactId>
    <version>0.1.0</version>
    <scope>test</scope>
</dependency>
```

### 2. Write a minimal test

```java
@AnalyzeClasses(packages = "com.example")
class ArchitectureTest {

    @ArchTest
    static final List<ArchRule> rules =
        ModulithRules.forPackage("com.example", "ordering", "payments", "inventory")
                     .allRules();
}
```

### 3. Add boundary enforcement

```java
@AnalyzeClasses(packages = "com.example")
class ArchitectureTest {

    private static final ModulithRuleSet RULE_SET = ModulithRuleSet
        .forRootPackage("com.example")
        .module(ModuleDefinition.builder("ordering")
            .apiPackages(".api.")
            .internalPackages(".internal.", ".infrastructure.")
            .allowedDependencies("inventory", "payments")
            .communicatesWith("notifications", CommunicationType.ASYNCHRONOUS)
            .build())
        .module(ModuleDefinition.builder("payments")
            .apiPackages(".api.")
            .internalPackages(".internal.")
            .build())
        .module(ModuleDefinition.builder("inventory")
            .apiPackages(".api.")
            .internalPackages(".internal.")
            .build())
        .build();

    @ArchTest
    static final List<ArchRule> rules = ModulithRules.of(RULE_SET).allRules();
}
```

## Features

### Boundary Rules

| Rule | What it checks |
|------|----------------|
| `modulesOnlyDependOnAllowedModules()` | Each module only depends on modules declared in `allowedDependencies` |
| `internalsShouldNotBeAccessedFromOutside()` | Internal packages are not accessed from other modules |
| `crossModuleAccessOnlyThroughApi()` | Cross-module access goes through declared API packages only |
| `moduleShouldHaveNoDependencies(name)` | A specific module has zero outgoing dependencies |
| `moduleShouldOnlyBeAccessedBy(name, ...)` | Only the listed modules may access the named module |

### Cycle Rules

| Rule | What it checks |
|------|----------------|
| `noModuleCycles()` | No circular dependencies exist between any modules |
| `moduleHasNoCycles(name)` | A specific module does not participate in any cycle |

### Communication Rules

| Rule | What it checks |
|------|----------------|
| `asyncModulesShouldNotCallDirectly()` | Modules with an ASYNCHRONOUS contract do not make direct synchronous calls |
| `noCommModulesShouldNotInteract()` | Modules with a NONE contract do not interact at all |
| `allCommunicationContractsRespected()` | Combines both async and no-communication checks |

### Spring-Specific Rules

> Requires the `modulith-rules-spring` artifact.

| Rule | What it checks |
|------|----------------|
| `controllersShouldNotCrossModuleBoundaries()` | `@Controller` and `@RestController` beans do not depend on controllers in other modules |
| `repositoriesShouldBeModuleInternal()` | `@Repository` beans are only accessed from within their own module |
| `transactionalMethodsShouldNotSpanModules()` | `@Transactional` methods do not call across module boundaries |
| `eventClassesShouldBeInApiPackages()` | Event classes used by other modules live in API packages |
| `noDirectInjectionOfInternalBeans()` | No `@Autowired` injection of internal beans across module boundaries |

## Configuration

### A. Fluent Java API

```java
ModulithRuleSet ruleSet = ModulithRuleSet
    .forRootPackage("com.example")
    .module(ModuleDefinition.builder("ordering")
        .apiPackages(".api.")
        .internalPackages(".internal.", ".infrastructure.")
        .allowedDependencies("inventory", "payments")
        .communicatesWith("notifications", CommunicationType.ASYNCHRONOUS)
        .build())
    .module(ModuleDefinition.builder("payments")
        .apiPackages(".api.")
        .internalPackages(".internal.")
        .build())
    .build();

ModulithRules rules = ModulithRules.of(ruleSet);
```

### B. YAML Configuration

Create `src/test/resources/modulith-rules.yml`:

```yaml
root-package: com.example
modules:
  ordering:
    api-packages:
      - .api.
    internal-packages:
      - .internal.
      - .infrastructure.
    allowed-dependencies:
      - inventory
      - payments
    communication:
      notifications: ASYNCHRONOUS
  payments:
    api-packages:
      - .api.
    internal-packages:
      - .internal.
  inventory:
    api-packages:
      - .api.
    internal-packages:
      - .internal.
```

Then load it in your test:

```java
@AnalyzeClasses(packages = "com.example")
class ArchitectureTest {

    @ArchTest
    static final List<ArchRule> rules =
        ModulithRules.of(ModulithConfigLoader.loadFromClasspath()).allRules();
}
```

### C. Minimal Convention-Based

```java
ModulithRules.forPackage("com.example", "ordering", "payments", "inventory").allRules();
```

This derives each module's base package as `com.example.<moduleName>` with no explicit API or internal declarations.

## Violation Messages

Violation messages identify exactly what is wrong and which modules are involved:

```
Architecture Violation: Rule 'modules only depend on allowed modules' was violated (1 times):
Module 'ordering' depends on module 'notifications', but only [inventory, payments] are allowed.
```

```
Architecture Violation: Rule 'internals should not be accessed from outside' was violated (1 times):
Class com.example.payments.internal.PaymentProcessor accessed by com.example.ordering.OrderService,
but internal packages of 'payments' must not be accessed from other modules.
```

```
Architecture Violation: Rule 'no module cycles' was violated (1 times):
Circular dependency detected: ordering -> payments -> inventory -> ordering.
Restructure dependencies to eliminate the cycle.
```

## Modules

| Artifact | Purpose |
|----------|---------|
| `modulith-rules-core` | Core boundary, cycle, and communication rules. Fluent API and YAML config loader. No Spring dependency. |
| `modulith-rules-spring` | Spring Boot-specific rules for controllers, repositories, transactions, events, and injection. |
| `modulith-rules-example` | Working example project demonstrating library usage. |

## How It Differs from Spring Modulith

Spring Modulith is a runtime framework that provides module verification, observability, and event publication at runtime. Modulith Rules is test-time only: no runtime footprint, no framework coupling, and no classpath impact in production.

Key differences:

- Works with any Spring Boot project, including projects that do not use the Spring Modulith runtime
- Provides opinionated, ready-to-use rules without requiring custom ArchUnit rule authoring
- Supports non-Spring projects via the core module
- Runs entirely during `mvn test`, with zero production overhead

The two libraries complement each other. Use Spring Modulith for runtime module isolation and event handling, and Modulith Rules for compile-time architecture enforcement.

## Requirements

- Java 17+
- ArchUnit 1.3+
- JUnit 5
- Spring Boot 3.x (optional, required only for the `modulith-rules-spring` module)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on reporting issues, proposing features, and submitting pull requests.

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.
