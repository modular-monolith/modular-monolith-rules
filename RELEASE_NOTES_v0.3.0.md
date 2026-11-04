# Modulith Rules v0.3.0

A maintenance release that moves the library onto ArchUnit 1.5.0, JUnit 6.1.3 and
Spring Boot 4.1.1, fixes the project metadata on Maven Central, and adds the
material that makes the first five minutes shorter.

## What changed

- ArchUnit 1.5.0, JUnit 6.1.3 and Spring Boot 4.1.1 (optional, `modulith-rules-spring` only)
- Project URL and Source Control links on Maven Central now resolve. Each published
  module states its own `url` and `scm` rather than inheriting them, which is what
  produced the broken `.../modulith-rules-core` links in 0.2.0
- New `modulith-rules-archetype`: `mvn archetype:generate` produces a two-module
  project with the architecture test already wired in
- `quickstart/` folder with a drop-in `ArchitectureTest.java` and the Maven and
  Gradle snippets
- README section comparing the library with plain ArchUnit and Spring Modulith, and
  a minimal GitHub Actions job for running the rules on pull requests
- `ADOPTERS.md` and an issue template for teams that want to be listed

## Behaviour

No rule behaviour has changed. Existing configurations keep working as they are.

## Requirements

Java 17+, ArchUnit 1.5.x, JUnit Platform. Spring Boot is optional and only needed for
`modulith-rules-spring`.
