package io.modulith.rules.config;

import io.modulith.rules.api.CommunicationType;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ModulithConfigLoader}.
 *
 * <p>Most tests use {@link ModulithConfigLoader#loadFromString(String)} to avoid filesystem
 * dependencies. One test exercises {@link ModulithConfigLoader#loadFromClasspath()} using
 * the modulith-rules.yml placed in the core test resources directory.
 */
class ModulithConfigLoaderTest {

    // ---------------------------------------------------------------------------
    // Successful parsing
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("loadFromString parses a minimal config with root-package and a single named module")
    void shouldParseMinimalConfig() {
        String yaml =
                "root-package: com.example\n" +
                "modules:\n" +
                "  foo:\n";

        ModulithRuleSet ruleSet = ModulithConfigLoader.loadFromString(yaml);

        assertThat(ruleSet.rootPackage()).isEqualTo("com.example");
        assertThat(ruleSet.moduleCount()).isEqualTo(1);
        ModuleDefinition foo = ruleSet.module("foo");
        assertThat(foo.name()).isEqualTo("foo");
        assertThat(foo.basePackage()).isEqualTo("com.example.foo");
    }

    @Test
    @DisplayName("loadFromString parses a full config with all supported module fields")
    void shouldParseFullConfig() {
        String yaml =
                "root-package: com.example\n" +
                "modules:\n" +
                "  orders:\n" +
                "    base-package: com.example.orders\n" +
                "    api-packages:\n" +
                "      - .api.\n" +
                "    internal-packages:\n" +
                "      - .internal.\n" +
                "    allowed-dependencies:\n" +
                "      - catalog\n" +
                "    communication:\n" +
                "      catalog: SYNCHRONOUS\n";

        ModulithRuleSet ruleSet = ModulithConfigLoader.loadFromString(yaml);

        assertThat(ruleSet.rootPackage()).isEqualTo("com.example");
        ModuleDefinition orders = ruleSet.module("orders");
        assertThat(orders.basePackage()).isEqualTo("com.example.orders");
        assertThat(orders.apiPackageIdentifiers()).containsExactly(".api.");
        assertThat(orders.internalPackageIdentifiers()).containsExactly(".internal.");
        assertThat(orders.allowedDependencies()).containsExactly("catalog");
        assertThat(orders.communicationContracts())
                .containsEntry("catalog", CommunicationType.SYNCHRONOUS);
    }

    @Test
    @DisplayName("loadFromString parses a config with three modules and exposes all by name")
    void shouldParseMultipleModules() {
        String yaml =
                "root-package: com.example\n" +
                "modules:\n" +
                "  one:\n" +
                "  two:\n" +
                "  three:\n";

        ModulithRuleSet ruleSet = ModulithConfigLoader.loadFromString(yaml);

        assertThat(ruleSet.moduleCount()).isEqualTo(3);
        assertThat(ruleSet.module("one")).isNotNull();
        assertThat(ruleSet.module("two")).isNotNull();
        assertThat(ruleSet.module("three")).isNotNull();
    }

    @Test
    @DisplayName("loadFromString derives basePackage as rootPackage + '.' + moduleName when base-package is absent")
    void shouldDeriveBasePackageFromRootAndModuleName() {
        String yaml =
                "root-package: com.example\n" +
                "modules:\n" +
                "  ordering:\n";

        ModulithRuleSet ruleSet = ModulithConfigLoader.loadFromString(yaml);

        assertThat(ruleSet.module("ordering").basePackage()).isEqualTo("com.example.ordering");
    }

    @Test
    @DisplayName("loadFromString parses ASYNCHRONOUS and NONE communication contracts")
    void shouldParseCommunicationContracts() {
        String yaml =
                "root-package: com.example\n" +
                "modules:\n" +
                "  sender:\n" +
                "    communication:\n" +
                "      receiver: ASYNCHRONOUS\n" +
                "      archiver: NONE\n";

        ModulithRuleSet ruleSet = ModulithConfigLoader.loadFromString(yaml);

        ModuleDefinition sender = ruleSet.module("sender");
        assertThat(sender.communicationContracts())
                .containsEntry("receiver", CommunicationType.ASYNCHRONOUS)
                .containsEntry("archiver", CommunicationType.NONE);
    }

    @Test
    @DisplayName("loadFromString skips comment lines starting with '#' and parses the rest successfully")
    void shouldIgnoreComments() {
        String yaml =
                "# Top-level comment\n" +
                "root-package: com.example\n" +
                "# Another comment before modules\n" +
                "modules:\n" +
                "  foo:\n" +
                "    # Inline comment inside module block\n" +
                "    api-packages:\n" +
                "      - .api.\n";

        ModulithRuleSet ruleSet = ModulithConfigLoader.loadFromString(yaml);

        assertThat(ruleSet.rootPackage()).isEqualTo("com.example");
        assertThat(ruleSet.moduleCount()).isEqualTo(1);
        assertThat(ruleSet.module("foo").apiPackageIdentifiers()).containsExactly(".api.");
    }

    // ---------------------------------------------------------------------------
    // Classpath loading
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("loadFromClasspath loads modulith-rules.yml from the test resources directory")
    void shouldLoadFromClasspath() {
        // modulith-rules.yml is placed in modulith-rules-core/src/test/resources
        ModulithRuleSet ruleSet = ModulithConfigLoader.loadFromClasspath();

        assertThat(ruleSet.rootPackage()).isEqualTo("io.modulith.rules.testfixtures");
        assertThat(ruleSet.moduleCount()).isGreaterThanOrEqualTo(1);
        assertThat(ruleSet.module("alpha")).isNotNull();
        assertThat(ruleSet.module("beta")).isNotNull();
    }

    // ---------------------------------------------------------------------------
    // Error handling
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("loadFromString throws IllegalStateException when root-package is missing")
    void shouldThrowWhenRootPackageMissing() {
        String yaml =
                "modules:\n" +
                "  foo:\n";

        assertThatThrownBy(() -> ModulithConfigLoader.loadFromString(yaml))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("loadFromClasspath throws IllegalArgumentException when the file is not on the classpath")
    void shouldThrowWhenConfigFileNotFound() {
        assertThatThrownBy(() -> ModulithConfigLoader.loadFromClasspath("nonexistent.yml"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
