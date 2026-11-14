package io.modulith.rules.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Package matching for API and internal package declarations. The match has to stop at
 * a package boundary, otherwise an API package of {@code .c.} also claims
 * {@code com.example.b.cache}.
 */
class ModuleDefinitionTest {

    private static final ModuleDefinition B = ModuleDefinition.builder("b")
            .basePackage("com.example.b")
            .apiPackages(".c.")
            .internalPackages(".internal.")
            .build();

    @Test
    @DisplayName("a class in the declared API package is public API")
    void classInApiPackage_isPublicApi() {
        assertThat(B.isPublicApi("com.example.b.c.Foo")).isTrue();
    }

    @Test
    @DisplayName("a class in a sub-package of the API package is public API")
    void classInApiSubPackage_isPublicApi() {
        assertThat(B.isPublicApi("com.example.b.c.events.OrderPlaced")).isTrue();
    }

    @Test
    @DisplayName("a sibling package sharing the API name as a prefix is not public API")
    void siblingPackageWithSamePrefix_isNotPublicApi() {
        assertThat(B.isPublicApi("com.example.b.cache.Foo")).isFalse();
    }

    @Test
    @DisplayName("an internal class is not public API")
    void internalClass_isNotPublicApi() {
        assertThat(B.isPublicApi("com.example.b.internal.Foo")).isFalse();
    }

    @Test
    @DisplayName("absolute patterns match with or without the trailing dots")
    void absolutePatterns_matchWithOrWithoutTrailingDots() {
        assertThat(ModuleDefinition.classMatchesPackagePattern("com.example.b.api.Foo", "com.example.b.api..")).isTrue();
        assertThat(ModuleDefinition.classMatchesPackagePattern("com.example.b.api.Foo", "com.example.b.api")).isTrue();
        assertThat(ModuleDefinition.classMatchesPackagePattern("com.example.b.apiinternal.Foo", "com.example.b.api..")).isFalse();
    }

    @Test
    @DisplayName("nested classes follow their enclosing class's package")
    void nestedClass_followsEnclosingPackage() {
        assertThat(B.isPublicApi("com.example.b.c.Foo$Bar")).isTrue();
        assertThat(B.isPublicApi("com.example.b.cache.Foo$Bar")).isFalse();
    }
}
