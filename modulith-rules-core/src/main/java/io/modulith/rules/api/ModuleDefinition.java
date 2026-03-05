package io.modulith.rules.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable description of a single module within a modular monolith.
 *
 * <p>A {@code ModuleDefinition} captures the name, root package, public API packages,
 * internal packages, allowed upstream dependencies, and communication contracts of a
 * module. It is the primary input for all modulith-rules checks.
 *
 * <p>Instances are created via the fluent {@link Builder}:
 *
 * <pre>{@code
 * ModuleDefinition orders = ModuleDefinition.builder("orders")
 *         .basePackage("com.example.orders")
 *         .apiPackages(".api.")
 *         .internalPackages(".internal.", ".db.")
 *         .allowedDependencies("catalog", "shared")
 *         .communicatesWith("catalog", CommunicationType.SYNCHRONOUS)
 *         .build();
 * }</pre>
 */
public final class ModuleDefinition {

    private final String name;
    private final String basePackage;
    private final Set<String> apiPackageIdentifiers;
    private final Set<String> internalPackageIdentifiers;
    private final Set<String> allowedDependencies;
    private final Map<String, CommunicationType> communicationContracts;

    private ModuleDefinition(Builder builder) {
        this.name = builder.name;
        this.basePackage = builder.basePackage;
        this.apiPackageIdentifiers = Collections.unmodifiableSet(new HashSet<>(builder.apiPackageIdentifiers));
        this.internalPackageIdentifiers = Collections.unmodifiableSet(new HashSet<>(builder.internalPackageIdentifiers));
        this.allowedDependencies = Collections.unmodifiableSet(new HashSet<>(builder.allowedDependencies));
        this.communicationContracts = Collections.unmodifiableMap(new HashMap<>(builder.communicationContracts));
    }

    /**
     * Creates a new {@link Builder} for a module with the given name.
     *
     * @param name the unique module name, must not be {@code null} or blank
     * @return a new builder instance
     */
    public static Builder builder(String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return new Builder(name);
    }

    /**
     * Returns the unique name of this module.
     *
     * @return module name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns the root package of this module, for example {@code "com.example.orders"}.
     *
     * @return base package, never {@code null}
     */
    public String basePackage() {
        return basePackage;
    }

    /**
     * Returns the raw package identifiers that designate public API sub-packages.
     * Patterns may be relative (starting with {@code "."}) or absolute.
     *
     * @return unmodifiable set of API package identifiers
     */
    public Set<String> apiPackageIdentifiers() {
        return apiPackageIdentifiers;
    }

    /**
     * Returns the raw package identifiers that designate internal sub-packages.
     * Patterns may be relative (starting with {@code "."}) or absolute.
     *
     * @return unmodifiable set of internal package identifiers
     */
    public Set<String> internalPackageIdentifiers() {
        return internalPackageIdentifiers;
    }

    /**
     * Returns the names of modules that this module is allowed to depend on.
     *
     * @return unmodifiable set of allowed dependency module names
     */
    public Set<String> allowedDependencies() {
        return allowedDependencies;
    }

    /**
     * Returns the declared communication contracts keyed by the target module name.
     *
     * @return unmodifiable map of module name to {@link CommunicationType}
     */
    public Map<String, CommunicationType> communicationContracts() {
        return communicationContracts;
    }

    /**
     * Returns the ArchUnit package identifier for this module's entire package tree,
     * using the {@code ..} wildcard to match all sub-packages.
     *
     * @return ArchUnit package identifier, for example {@code "com.example.orders.."}
     */
    public String archUnitPackageIdentifier() {
        return basePackage + "..";
    }

    /**
     * Resolves the API package identifiers into absolute ArchUnit package patterns.
     * Relative patterns (starting with {@code "."}) are resolved against
     * {@link #basePackage()}. Absolute patterns are returned unchanged.
     *
     * @return set of absolute ArchUnit package patterns for API packages
     */
    public Set<String> archUnitApiPackageIdentifiers() {
        return resolvePatterns(apiPackageIdentifiers);
    }

    /**
     * Resolves the internal package identifiers into absolute ArchUnit package patterns.
     * Relative patterns (starting with {@code "."}) are resolved against
     * {@link #basePackage()}. Absolute patterns are returned unchanged.
     *
     * @return set of absolute ArchUnit package patterns for internal packages
     */
    public Set<String> archUnitInternalPackageIdentifiers() {
        return resolvePatterns(internalPackageIdentifiers);
    }

    /**
     * Checks whether the given fully-qualified class name belongs to this module,
     * meaning it resides within {@link #basePackage()} or any of its sub-packages.
     *
     * @param fullClassName fully-qualified class name
     * @return {@code true} if the class belongs to this module
     */
    public boolean containsClass(String fullClassName) {
        if (fullClassName == null) {
            return false;
        }
        return fullClassName.startsWith(basePackage + ".") || fullClassName.equals(basePackage);
    }

    /**
     * Checks whether the given fully-qualified class name is part of this module's
     * public API, meaning it resides in one of the resolved API packages.
     *
     * @param fullClassName fully-qualified class name
     * @return {@code true} if the class is in a public API package of this module
     */
    public boolean isPublicApi(String fullClassName) {
        if (fullClassName == null || !containsClass(fullClassName)) {
            return false;
        }
        for (String pattern : archUnitApiPackageIdentifiers()) {
            String prefix = pattern.endsWith("..") ? pattern.substring(0, pattern.length() - 2) : pattern;
            if (fullClassName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> resolvePatterns(Set<String> patterns) {
        Set<String> resolved = new HashSet<>();
        for (String pattern : patterns) {
            if (pattern.startsWith(".")) {
                resolved.add(basePackage + pattern + ".");
            } else {
                resolved.add(pattern);
            }
        }
        return Collections.unmodifiableSet(resolved);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModuleDefinition)) return false;
        ModuleDefinition that = (ModuleDefinition) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "ModuleDefinition{name='" + name + "', basePackage='" + basePackage + "'}";
    }

    /**
     * Builder for {@link ModuleDefinition}.
     */
    public static final class Builder {

        private final String name;
        private String basePackage;
        private final Set<String> apiPackageIdentifiers = new HashSet<>();
        private final Set<String> internalPackageIdentifiers = new HashSet<>();
        private final Set<String> allowedDependencies = new HashSet<>();
        private final Map<String, CommunicationType> communicationContracts = new HashMap<>();

        private Builder(String name) {
            this.name = name;
        }

        /**
         * Sets the root package for this module.
         *
         * @param basePackage the fully-qualified base package
         * @return this builder
         */
        public Builder basePackage(String basePackage) {
            this.basePackage = Objects.requireNonNull(basePackage, "basePackage must not be null");
            return this;
        }

        /**
         * Adds package identifiers that designate public API sub-packages. Patterns
         * starting with {@code "."} are treated as relative to the base package.
         * For example, {@code ".api."} resolves to {@code "com.example.orders.api."}.
         *
         * @param packagePatterns one or more package identifiers
         * @return this builder
         */
        public Builder apiPackages(String... packagePatterns) {
            apiPackageIdentifiers.addAll(Arrays.asList(packagePatterns));
            return this;
        }

        /**
         * Adds package identifiers that designate internal sub-packages. Patterns
         * starting with {@code "."} are treated as relative to the base package.
         *
         * @param packagePatterns one or more package identifiers
         * @return this builder
         */
        public Builder internalPackages(String... packagePatterns) {
            internalPackageIdentifiers.addAll(Arrays.asList(packagePatterns));
            return this;
        }

        /**
         * Declares one or more module names that this module is allowed to depend on.
         *
         * @param moduleNames names of permitted upstream modules
         * @return this builder
         */
        public Builder allowedDependencies(String... moduleNames) {
            allowedDependencies.addAll(Arrays.asList(moduleNames));
            return this;
        }

        /**
         * Declares the permitted communication type with a specific target module.
         *
         * @param targetModuleName the name of the target module
         * @param type the allowed communication pattern
         * @return this builder
         */
        public Builder communicatesWith(String targetModuleName, CommunicationType type) {
            communicationContracts.put(
                    Objects.requireNonNull(targetModuleName, "targetModuleName must not be null"),
                    Objects.requireNonNull(type, "type must not be null"));
            return this;
        }

        /**
         * Builds and returns the {@link ModuleDefinition}.
         *
         * @return a new immutable {@code ModuleDefinition}
         * @throws IllegalStateException if required fields have not been set
         */
        public ModuleDefinition build() {
            if (basePackage == null || basePackage.isBlank()) {
                throw new IllegalStateException("basePackage must be set for module '" + name + "'");
            }
            return new ModuleDefinition(this);
        }
    }
}
