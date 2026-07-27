# Modulith Rules v0.2.0

Modulith Rules is now available on Maven Central: pre-built ArchUnit rules
for enforcing module boundaries in modular monolith architectures. It is
published under the `io.modularmonolith` group id as `modulith-rules-core`
and `modulith-rules-spring`, with `modulith-rules-bom` to align the versions.

Everything runs at test time. There is no runtime footprint and no
production classpath impact; add the modules in `test` scope and wire the
rules into your existing JUnit or ArchUnit tests.

## Highlights

- **Core rules under test.** Boundary rules (allowed dependencies, internal
  package protection, API-only access), cycle detection, and communication
  contract rules, each covered by unit tests exercising both passing and
  failing arrangements.
- **Spring rules under test.** Controller isolation, module-internal
  repositories, transaction boundaries, event placement, and internal bean
  injection, with Spring-annotated fixtures covering both branches of every
  rule.
- **Actionable violation messages.** Every failure names the offending class
  and target package, then appends a concrete fix suggestion, for example
  which api package to expose a service in, so the message tells you what to
  do rather than only what broke.
- **Mermaid dependency graph export.** `dependencyGraph().toMermaid(classes)`
  renders the module graph as a Markdown-ready flowchart: solid arrows for
  synchronous dependencies, dashed for declared asynchronous contracts, with
  deterministic output that diffs cleanly.
- **One-line rule sets.** `allCoreRules()` applies the full core rule set at
  once, and `SpringModulithRules.of(ruleSet).allRules()` does the same for
  the Spring rules.

## Configuration

Modules are declared through a fluent Java API or a YAML file, with a
convention-based shorthand for the common layout. See the README for the
full rule catalogue, configuration reference, and behaviour notes.

## Requirements

Java 17+, ArchUnit 1.4.x, JUnit Platform. Spring Boot is optional and only
needed for `modulith-rules-spring`.
