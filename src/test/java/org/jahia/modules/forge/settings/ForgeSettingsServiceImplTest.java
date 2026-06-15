package org.jahia.modules.forge.settings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ForgeSettingsServiceImpl#escape(String)} — the LDAP/OSGi filter-assertion
 * escaper that keeps an attacker-influenced siteKey from injecting into the configuration filter
 * {@code (&(service.factoryPid=...)(siteKey=<value>))}. Pure logic, no OSGi runtime required.
 */
class ForgeSettingsServiceImplTest {

    @Test
    @DisplayName("escape leaves a plain value untouched")
    void escape_plain() {
        assertThat(ForgeSettingsServiceImpl.escape("my-store")).isEqualTo("my-store");
    }

    @Test
    @DisplayName("escape encodes each RFC 4515 special character")
    void escape_specialChars() {
        assertThat(ForgeSettingsServiceImpl.escape("*")).isEqualTo("\\2a");
        assertThat(ForgeSettingsServiceImpl.escape("(")).isEqualTo("\\28");
        assertThat(ForgeSettingsServiceImpl.escape(")")).isEqualTo("\\29");
        assertThat(ForgeSettingsServiceImpl.escape("\\")).isEqualTo("\\5c");
    }

    @Test
    @DisplayName("escape neutralises a filter-injection attempt and escapes backslash FIRST")
    void escape_injectionAttempt() {
        // A crafted wildcard/paren payload must not survive as filter syntax.
        assertThat(ForgeSettingsServiceImpl.escape("*)(uid=*"))
                .isEqualTo("\\2a\\29\\28uid=\\2a");
        // Backslash is escaped before the others so its own escape is not re-escaped.
        assertThat(ForgeSettingsServiceImpl.escape("\\*")).isEqualTo("\\5c\\2a");
    }

    @Test
    @DisplayName("decodePassword returns null for a blank value")
    void decodePassword_blank() {
        assertThat(ForgeSettingsServiceImpl.decodePassword(null)).isNull();
        assertThat(ForgeSettingsServiceImpl.decodePassword("")).isNull();
        assertThat(ForgeSettingsServiceImpl.decodePassword("   ")).isNull();
    }

    @Test
    @DisplayName("decodePassword decodes a base64-encoded value (how save()/the UI store it)")
    void decodePassword_base64() {
        final String encoded = Base64.getEncoder().encodeToString("s3cr3t!".getBytes(StandardCharsets.UTF_8));
        assertThat(ForgeSettingsServiceImpl.decodePassword(encoded)).isEqualTo("s3cr3t!");
    }

    @Test
    @DisplayName("decodePassword falls back to the raw value when it is not valid base64 (hand-edited .cfg)")
    void decodePassword_plaintextFallback() {
        // A password typed straight into karaf/etc — the space and '!' make it invalid base64, so
        // it must be used as-is rather than throwing (which would break every get() for the site).
        assertThat(ForgeSettingsServiceImpl.decodePassword("my plaintext pwd!"))
                .isEqualTo("my plaintext pwd!");
    }
}
