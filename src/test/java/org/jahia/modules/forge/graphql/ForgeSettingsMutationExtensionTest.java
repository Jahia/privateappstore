package org.jahia.modules.forge.graphql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ForgeSettingsMutationExtension#validateSiteKey(String)} — the guard every
 * forge GraphQL entry point calls before concatenating a siteKey into a JCR site path, so a
 * traversal value can't sidestep the permission gate. Pure logic, no Jahia container required.
 */
class ForgeSettingsMutationExtensionTest {

    @ParameterizedTest
    @ValueSource(strings = {"store", "private-app-store", "my.site_01", "A1"})
    @DisplayName("validateSiteKey accepts a simple identifier")
    void validateSiteKey_acceptsIdentifier(String siteKey) {
        assertThatCode(() -> ForgeSettingsMutationExtension.validateSiteKey(siteKey))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "..", "../..", "foo/bar", "foo bar", "a\\b", "site:evil"})
    @DisplayName("validateSiteKey rejects null, blank and path-altering values")
    void validateSiteKey_rejectsUnsafe(String siteKey) {
        assertThatThrownBy(() -> ForgeSettingsMutationExtension.validateSiteKey(siteKey))
                .isInstanceOf(RuntimeException.class);
    }
}
