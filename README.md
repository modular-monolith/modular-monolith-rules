#Modulith Rules 
Pre-built ArchUnit rules for enforcing module boundaries in modular monolith architectures.

What is this?
Every team building a modular monolith ends up writing the same custom ArchUnit rules: no cycles, respect module boundaries, protect internal packages, enforce communication contracts.
Modulith Rules packages these as a reusable, opinionated library with a fluent API that maps directly to modular monolith concepts.

Status
This project is under active development. The first release (v0.1.0) is in progress.

Planned Features
- Module boundary enforcement (allowed dependencies, internal package protection, API-only access)
- Circular dependency detection between modules
- Communication pattern contracts (synchronous, asynchronous, none)
- Spring Boot-specific rules (transaction boundaries, repository isolation, controller isolation)
- YAML and fluent Java API configuration
- Actionable violation messages that explain what is wrong and how to fix it

Modules
ArtifactPurposemodulith-rules-coreCore rules, config loader, fluent API. No Spring dependency.modulith-rules-springSpring Boot-specific rules.modulith-rules-exampleWorking example project.
Requirements

Java 17+
ArchUnit 1.3+
JUnit 5
Spring Boot 3.x (for spring module only, optional)

License
Apache License 2.0
