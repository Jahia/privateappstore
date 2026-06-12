package org.jahia.modules.forge.settings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
