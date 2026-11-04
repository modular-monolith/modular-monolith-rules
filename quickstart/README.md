# Five-minute quick start

The shortest path from nothing to a failing build when a module reaches into
another module's internals.

## 1. Add the dependency

Maven, in `test` scope:

```xml
<dependency>
    <groupId>io.modularmonolith</groupId>
    <artifactId>modulith-rules-core</artifactId>
    <version>0.3.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.5.0</version>
    <scope>test</scope>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
testImplementation("io.modularmonolith:modulith-rules-core:0.3.0")
testImplementation("com.tngtech.archunit:archunit-junit5:1.5.0")
```

Gradle (Groovy DSL):

```groovy
testImplementation 'io.modularmonolith:modulith-rules-core:0.3.0'
testImplementation 'com.tngtech.archunit:archunit-junit5:1.5.0'
```

If you already use Spring, add `modulith-rules-spring` the same way for the
`@Transactional`, `@Repository` and controller rules.

## 2. Copy the test

Copy [`ArchitectureTest.java`](ArchitectureTest.java) into `src/test/java` under
any package you like, and change two things: the root package and the list of
module names. Module packages are derived as `<root>.<moduleName>`.

## 3. Run it

```bash
mvn test -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false
# or
./gradlew test --tests '*ArchitectureTest'
```

A clean run means your modules have no cycles and nobody reaches into an
`internal` package from outside. A failure names the class, the package it
reached into, and what to do about it.

## 4. Decide what each module may depend on

Once the test runs clean, the next useful step is to declare the allowed
dependencies per module, which turns on the dependency rule and makes the
module graph explicit. The README section on
[declaring modules explicitly](../README.md#declaring-modules-explicitly) shows
the fluent builder and the YAML form.

## Starting from an archetype instead

To generate a small working project with two modules and the test already in
place:

```bash
mvn archetype:generate \
    -DarchetypeGroupId=io.modularmonolith \
    -DarchetypeArtifactId=modulith-rules-archetype \
    -DarchetypeVersion=0.3.0 \
    -DgroupId=com.acme -DartifactId=shop -Dpackage=com.acme.shop
```
