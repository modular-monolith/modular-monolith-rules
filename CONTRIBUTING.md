# Contributing to modulith-rules

Thank you for your interest in contributing to modulith-rules. This document provides guidelines and information for contributors.

## Reporting Issues

When you encounter a bug or have a feature request, please open an issue on GitHub with the following information:

- A clear, descriptive title
- A description of the problem or requested feature
- Steps to reproduce the issue (for bugs)
- Expected vs. actual behavior (for bugs)
- The version of modulith-rules, Java, and build tool you are using
- Minimal code examples that demonstrate the issue

Before opening a new issue, please search existing issues to avoid duplicates.

## Pull Requests

Contributions via pull requests are welcome. Please follow these steps:

1. Fork the repository and create a feature branch from `main`.
2. Implement your changes with clear, focused commits.
3. Add or update tests to cover your changes.
4. Ensure all tests pass locally before submitting.
5. Update documentation and Javadoc where appropriate.
6. Open a pull request against the `main` branch with a clear description of the changes and the motivation behind them.

Pull requests should address a single concern. Large pull requests are harder to review and slower to merge. If you have multiple independent improvements, please submit them as separate pull requests.

## Development Setup

### Prerequisites

- Java 17 or later
- Maven 3.9 or later

### Building the Project

Clone the repository and build with Maven:

```bash
git clone https://github.com/modulith/modulith-rules.git
cd modulith-rules
mvn verify
```

### Running Tests

```bash
mvn test
```

To run tests for a specific module:

```bash
mvn test -pl modulith-rules-core
```

### IDE Setup

Import the project as a Maven project in your IDE of choice. The project uses standard Maven conventions and requires no special IDE plugins beyond standard Java and Maven support.

## Adding New Rules

When adding a new ArchUnit rule, please follow these conventions:

- Place core rules in `modulith-rules-core` under the appropriate package (`boundary`, `cycle`, or `communication`).
- Spring-specific rules belong in `modulith-rules-spring`.
- Each rule class should have clear Javadoc explaining what the rule enforces, why it matters, and example configuration.
- Include a test that verifies the rule catches violations and passes on conforming code.
- Rules should be composable: prefer small, focused rules over large monolithic checks.
- Use `ModulithRuleSet` as the primary input for all rules rather than accepting raw strings or packages.

When introducing a new rule category, discuss it in an issue first so the design can be reviewed before implementation.

## Code Style

The project follows standard Java conventions with a few additional guidelines:

- Use 4 spaces for indentation, not tabs.
- Keep lines under 120 characters where possible.
- Write Javadoc for all public types and methods.
- Avoid abbreviations in names: prefer `moduleDefinition` over `modDef`.
- Use `final` for fields and local variables where appropriate to communicate immutability intent.
- Prefer immutable objects and builders over mutable state.
- Do not use wildcard imports.

All contributions are expected to follow the project's Apache 2.0 license terms.
