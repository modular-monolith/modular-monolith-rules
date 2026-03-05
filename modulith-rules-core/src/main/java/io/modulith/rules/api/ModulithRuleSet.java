package io.modulith.rules.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable registry of {@link ModuleDefinition} instances that together describe
 * the module structure of a modular monolith.
 *
 * <p>A {@code ModulithRuleSet} is the primary input for all modulith-rules checks. It is
 * created via the factory method {@link #forRootPackage(String)} and a fluent builder:
 *
 * <pre>{@code
 * ModulithRuleSet ruleSet = ModulithRuleSet.forRootPackage("com.example")
 *         .modules("orders", "catalog", "inventory")
 *         .module(ModuleDefinition.builder("payments")
 *                 .basePackage("com.example.payments")
 *                 .apiPackages(".api.")
 *                 .build())
 *         .build();
 * }</pre>
 *
 * <p>Convention-based modules added via {@link Builder#module(String)} or
 * {@link Builder#modules(String...)} derive their base package as
 * {@code rootPackage + "." + moduleName}.
 */
public final class ModulithRuleSet {

    private final String rootPackage;
    private final Map<String, ModuleDefinition> modules;

    private ModulithRuleSet(Builder builder) {
        this.rootPackage = builder.rootPackage;
        this.modules = Collections.unmodifiableMap(new LinkedHashMap<>(builder.modules));
    }

    /**
     * Returns a new {@link Builder} for a rule set rooted at the given package.
     *
     * @param rootPackage the top-level package shared by all modules, must not be {@code null}
     * @return a new builder instance
     */
    public static Builder forRootPackage(String rootPackage) {
        Objects.requireNonNull(rootPackage, "rootPackage must not be null");
        if (rootPackage.isBlank()) {
            throw new IllegalArgumentException("rootPackage must not be blank");
        }
        return new Builder(rootPackage);
    }

    /**
     * Returns the root package shared by all modules in this rule set.
     *
     * @return root package, never {@code null}
     */
    public String rootPackage() {
        return rootPackage;
    }

    /**
     * Returns an unmodifiable map of module name to {@link ModuleDefinition}.
     *
     * @return all registered modules, never {@code null}
     */
    public Map<String, ModuleDefinition> modules() {
        return modules;
    }

    /**
     * Returns the {@link ModuleDefinition} for the given module name.
     *
     * @param name the module name to look up
     * @return the corresponding definition, never {@code null}
     * @throws IllegalArgumentException if no module with the given name exists
     */
    public ModuleDefinition module(String name) {
        ModuleDefinition definition = modules.get(name);
        if (definition == null) {
            throw new IllegalArgumentException(
                    "No module named '" + name + "' found in this rule set. "
                    + "Registered modules: " + modules.keySet());
        }
        return definition;
    }

    /**
     * Returns all {@link ModuleDefinition} instances in this rule set.
     *
     * @return collection of all module definitions, never {@code null}
     */
    public Collection<ModuleDefinition> allModules() {
        return modules.values();
    }

    /**
     * Returns the number of modules registered in this rule set.
     *
     * @return module count, always greater than zero for a valid rule set
     */
    public int moduleCount() {
        return modules.size();
    }

    @Override
    public String toString() {
        return "ModulithRuleSet{rootPackage='" + rootPackage + "', modules=" + modules.keySet() + "}";
    }

    /**
     * Builder for {@link ModulithRuleSet}.
     */
    public static final class Builder {

        private final String rootPackage;
        private final Map<String, ModuleDefinition> modules = new LinkedHashMap<>();

        private Builder(String rootPackage) {
            this.rootPackage = rootPackage;
        }

        /**
         * Adds a convention-based module whose base package is derived as
         * {@code rootPackage + "." + name}.
         *
         * @param name the module name
         * @return this builder
         */
        public Builder module(String name) {
            Objects.requireNonNull(name, "name must not be null");
            String basePackage = rootPackage + "." + name;
            return module(ModuleDefinition.builder(name).basePackage(basePackage).build());
        }

        /**
         * Adds a convention-based module with an explicit base package.
         *
         * @param name the module name
         * @param basePackage the fully-qualified base package for this module
         * @return this builder
         */
        public Builder module(String name, String basePackage) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(basePackage, "basePackage must not be null");
            return module(ModuleDefinition.builder(name).basePackage(basePackage).build());
        }

        /**
         * Adds a fully configured {@link ModuleDefinition}.
         *
         * @param definition the module definition, must not be {@code null}
         * @return this builder
         */
        public Builder module(ModuleDefinition definition) {
            Objects.requireNonNull(definition, "definition must not be null");
            modules.put(definition.name(), definition);
            return this;
        }

        /**
         * Adds multiple convention-based modules whose base packages are derived as
         * {@code rootPackage + "." + name} for each provided name.
         *
         * @param names one or more module names
         * @return this builder
         */
        public Builder modules(String... names) {
            Arrays.stream(names).forEach(this::module);
            return this;
        }

        /**
         * Builds and returns the {@link ModulithRuleSet}.
         *
         * @return a new immutable {@code ModulithRuleSet}
         * @throws IllegalStateException if no modules have been registered
         */
        public ModulithRuleSet build() {
            if (modules.isEmpty()) {
                throw new IllegalStateException(
                        "At least one module must be registered in the rule set. "
                        + "Use module() or modules() to add modules before calling build().");
            }
            return new ModulithRuleSet(this);
        }
    }
}
