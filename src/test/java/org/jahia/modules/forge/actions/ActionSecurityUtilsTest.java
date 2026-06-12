package org.jahia.modules.forge.actions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ActionSecurityUtils} — the open-redirect guard and the log-forging
 * sanitizer used by the upload action. Pure logic, no Jahia container required.
 */
class ActionSecurityUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"/home.html", "/sites/store/contents/x.html", "/ok", "/a/b/c"})
    @DisplayName("isSafeRedirect accepts site-relative paths")
    void isSafeRedirect_acceptsRelative(String url) {
        assertThat(ActionSecurityUtils.isSafeRedirect(url)).isTrue();
    }

    @Test
    @DisplayName("isSafeRedirect trims leading whitespace before judging")
    void isSafeRedirect_trimsWhitespace() {
        assertThat(ActionSecurityUtils.isSafeRedirect("   /ok")).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "//evil.com",
            "/\\evil.com",
            "http://evil.com",
            "HTTPS://evil.com",
            "javascript:alert(1)",
            "data:text/html,x",
            "/path://escape"
    })
    @DisplayName("isSafeRedirect rejects absolute, protocol-relative and pseudo-scheme URLs")
    void isSafeRedirect_rejectsUnsafe(String url) {
        assertThat(ActionSecurityUtils.isSafeRedirect(url)).isFalse();
    }

    @Test
    @DisplayName("sanitizeForLog returns the literal \"null\" for null input")
    void sanitizeForLog_null() {
        assertThat(ActionSecurityUtils.sanitizeForLog(null)).isEqualTo("null");
    }

    @Test
    @DisplayName("sanitizeForLog strips CR/LF and control characters that could forge a log line")
    void sanitizeForLog_stripsControlChars() {
        assertThat(ActionSecurityUtils.sanitizeForLog("line1\r\nFAKE LOG ENTRY")).isEqualTo("line1__FAKE LOG ENTRY");
        assertThat(ActionSecurityUtils.sanitizeForLog("a\tb")).isEqualTo("a_b");
        assertThat(ActionSecurityUtils.sanitizeForLog("plain text")).isEqualTo("plain text");
    }

    @Test
    @DisplayName("sanitizeForLog truncates to 200 chars + ellipsis")
    void sanitizeForLog_truncates() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 250; i++) {
            sb.append('x');
        }
        String result = ActionSecurityUtils.sanitizeForLog(sb.toString());
        assertThat(result).hasSize(203).endsWith("...");
    }
}
