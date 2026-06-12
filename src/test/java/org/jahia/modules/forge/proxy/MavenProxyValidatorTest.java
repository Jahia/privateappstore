package org.jahia.modules.forge.proxy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link MavenProxy} SSRF / path-traversal guards. The proxy now serves
 * anonymous callers on public stores, so {@code isSuspicious} must block raw AND percent-encoded
 * traversal/scheme-injection, and {@code isValidSiteName} must reject anything that could alter the
 * JCR path. Pure logic, no Jahia container required.
 */
class MavenProxyValidatorTest {

    @Test
    @DisplayName("isSuspicious passes a legitimate Maven coordinate path")
    void isSuspicious_passesLegitPath() {
        assertThat(MavenProxy.isSuspicious("org/jahia/modules/my-module/1.0.0/my-module-1.0.0.jar")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../../etc/passwd",
            "a\\b",
            "http://169.254.169.254/latest/meta-data",
            "file:///etc/passwd",
            "a\nb",
            "a\rb",
            "%2e%2e/x",
            "%2E%2E/x",          // uppercase: lowercased before the check
            "x%2fy",
            "x%5cy"
    })
    @DisplayName("isSuspicious blocks raw and percent-encoded traversal / scheme injection (case-insensitive)")
    void isSuspicious_blocksTraversalAndSchemes(String path) {
        assertThat(MavenProxy.isSuspicious(path)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"store", "my-site_01", "private.app.store", "A1"})
    @DisplayName("isValidSiteName accepts simple identifiers")
    void isValidSiteName_acceptsIdentifiers(String name) {
        assertThat(MavenProxy.isValidSiteName(name)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "..", "x..y", "../etc", "foo/bar", "foo bar", "a\\b", "site:evil"})
    @DisplayName("isValidSiteName rejects blanks and anything that could alter the JCR path")
    void isValidSiteName_rejectsUnsafe(String name) {
        assertThat(MavenProxy.isValidSiteName(name)).isFalse();
    }
}
