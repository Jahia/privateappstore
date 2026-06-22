package org.jahia.modules.forge.settings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.osgi.service.cm.ConfigurationException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Dictionary;
import java.util.Hashtable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ForgeSettingsServiceImpl}. The {@link org.osgi.service.cm.ManagedServiceFactory}
 * dispatch ({@code updated}/{@code get}/{@code deleted}) is exercised directly with a configuration
 * {@link Dictionary} — no OSGi runtime needed. The pure helpers (site-key safety, file name and the
 * password decoder) are covered too; the file write/read round-trip against a live Jahia is covered
 * by the Cypress E2E.
 */
class ForgeSettingsServiceImplTest {

    private static Dictionary<String, Object> config(String siteKey) {
        final Dictionary<String, Object> d = new Hashtable<>();
        if (siteKey != null) {
            d.put("siteKey", siteKey);
        }
        return d;
    }

    @Test
    @DisplayName("updated then get returns the configuration keyed by siteKey; unknown site is empty")
    void updated_get_bySiteKey() throws ConfigurationException {
        final ForgeSettingsServiceImpl service = new ForgeSettingsServiceImpl();
        final Dictionary<String, Object> d = config("storeA");
        d.put("url", "https://nexus.example/releases");
        d.put("copyright", "ACME");

        service.updated("pid-a", d);

        assertThat(service.get("storeA").getUrl()).isEqualTo("https://nexus.example/releases");
        assertThat(service.get("storeA").getCopyright()).isEqualTo("ACME");
        // A site with no configuration instance reads as empty, never null.
        assertThat(service.get("unknown").getUrl()).isNull();
    }

    @Test
    @DisplayName("updated decodes the base64 password")
    void updated_decodesPassword() throws ConfigurationException {
        final ForgeSettingsServiceImpl service = new ForgeSettingsServiceImpl();
        final Dictionary<String, Object> d = config("storeA");
        d.put("password", Base64.getEncoder().encodeToString("s3cr3t!".getBytes(StandardCharsets.UTF_8)));

        service.updated("pid-a", d);

        assertThat(service.get("storeA").getPassword()).isEqualTo("s3cr3t!");
    }

    @Test
    @DisplayName("a configuration with no siteKey (the shipped template) is ignored")
    void updated_blankSiteKeyIgnored() throws ConfigurationException {
        final ForgeSettingsServiceImpl service = new ForgeSettingsServiceImpl();

        service.updated("pid-template", config(null));
        service.updated("pid-empty", config(""));

        // Nothing was registered, so any lookup is empty (and no NPE on the missing key).
        assertThat(service.get("").getUrl()).isNull();
    }

    @Test
    @DisplayName("deleted evicts the site that the configuration PID carried")
    void deleted_evictsBySite() throws ConfigurationException {
        final ForgeSettingsServiceImpl service = new ForgeSettingsServiceImpl();
        final Dictionary<String, Object> d = config("storeA");
        d.put("url", "https://nexus.example/releases");
        service.updated("pid-a", d);

        service.deleted("pid-a");

        assertThat(service.get("storeA").getUrl()).isNull();
    }

    @Test
    @DisplayName("re-keying a configuration to a new siteKey drops the old site")
    void updated_rekeyDropsOldSite() throws ConfigurationException {
        final ForgeSettingsServiceImpl service = new ForgeSettingsServiceImpl();
        final Dictionary<String, Object> first = config("storeA");
        first.put("url", "https://a.example");
        service.updated("pid-x", first);

        final Dictionary<String, Object> second = config("storeB");
        second.put("url", "https://b.example");
        service.updated("pid-x", second);

        assertThat(service.get("storeA").getUrl()).isNull();
        assertThat(service.get("storeB").getUrl()).isEqualTo("https://b.example");
    }

    @Test
    @DisplayName("isSafeSiteKey accepts a plain site key")
    void isSafeSiteKey_plain() {
        assertThat(ForgeSettingsServiceImpl.isSafeSiteKey("my-store")).isTrue();
        assertThat(ForgeSettingsServiceImpl.isSafeSiteKey("store1")).isTrue();
        assertThat(ForgeSettingsServiceImpl.isSafeSiteKey("a.b_c-d")).isTrue();
    }

    @Test
    @DisplayName("isSafeSiteKey rejects blank, null, separators and traversal")
    void isSafeSiteKey_unsafe() {
        assertThat(ForgeSettingsServiceImpl.isSafeSiteKey(null)).isFalse();
        assertThat(ForgeSettingsServiceImpl.isSafeSiteKey("")).isFalse();
        assertThat(ForgeSettingsServiceImpl.isSafeSiteKey("   ")).isFalse();
        // Anything that could escape the etc directory must be refused before reaching the file name.
        assertThat(ForgeSettingsServiceImpl.isSafeSiteKey("..")).isFalse();
        assertThat(ForgeSettingsServiceImpl.isSafeSiteKey("../evil")).isFalse();
        assertThat(ForgeSettingsServiceImpl.isSafeSiteKey("a/b")).isFalse();
        assertThat(ForgeSettingsServiceImpl.isSafeSiteKey("a\\b")).isFalse();
        assertThat(ForgeSettingsServiceImpl.isSafeSiteKey(".hidden")).isFalse();
        assertThat(ForgeSettingsServiceImpl.isSafeSiteKey("with space")).isFalse();
    }

    @Test
    @DisplayName("configFileName puts the site key in the factory-PID file name")
    void configFileName_format() {
        assertThat(ForgeSettingsServiceImpl.configFileName("mystore"))
                .isEqualTo("org.jahia.modules.forge.forgeSettings-mystore.cfg");
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
