package org.jahia.modules.forge.settings;

import org.apache.commons.lang.StringUtils;

/**
 * Immutable value object holding a site's forge settings (connection + branding +
 * the category root reference). Persisted by {@link ForgeSettingsService} in per-site
 * OSGi configuration rather than in JCR, so it is decoupled from the cross-module
 * {@code jmix:forgeSettings} node-type definition (two coexisting forge modules declare
 * that global type with different content, which broke JCR-backed storage).
 *
 * <p>The {@code password} field carries the CLEARTEXT password in memory (decoded on read,
 * supplied raw on write). The service stores it base64-obfuscated. It is never exposed to
 * GraphQL clients (only {@link #isPasswordSet()} is) and never logged.
 *
 * <p>{@code logoPath} is a plain JCR path string (not a weakreference) — the chrome resolves
 * it to a {@code /files/...} URL itself.
 *
 * <p>Built via {@link #builder()} (many optional fields; avoids a high-arity constructor).
 */
public final class ForgeSettings {

    private final String url;
    private final String id;
    private final String user;
    private final String password;
    private final String logoPath;
    private final String copyright;
    private final String privacyUrl;
    private final String termsUrl;
    private final String cookiesUrl;
    private final String facebookUrl;
    private final String linkedinUrl;
    private final String twitterUrl;
    private final String youtubeUrl;
    private final String rootCategoryUuid;

    private ForgeSettings(Builder b) {
        this.url = b.url;
        this.id = b.id;
        this.user = b.user;
        this.password = b.password;
        this.logoPath = b.logoPath;
        this.copyright = b.copyright;
        this.privacyUrl = b.privacyUrl;
        this.termsUrl = b.termsUrl;
        this.cookiesUrl = b.cookiesUrl;
        this.facebookUrl = b.facebookUrl;
        this.linkedinUrl = b.linkedinUrl;
        this.twitterUrl = b.twitterUrl;
        this.youtubeUrl = b.youtubeUrl;
        this.rootCategoryUuid = b.rootCategoryUuid;
    }

    /** An all-empty settings object (returned when a site has no configuration yet). */
    public static ForgeSettings empty() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder pre-populated with this object's values, so a caller can overlay only the
     * fields it owns and {@code save} the whole object — connection/branding and the category
     * root are written by different mutations into the same per-site config, and each must
     * preserve the other's fields. (The password it carries is the cleartext value;
     * {@code save} re-obfuscates it, so a no-op round-trip is harmless.)
     */
    public Builder toBuilder() {
        return new Builder()
                .url(url).id(id).user(user).password(password).logoPath(logoPath)
                .copyright(copyright).privacyUrl(privacyUrl).termsUrl(termsUrl)
                .cookiesUrl(cookiesUrl).facebookUrl(facebookUrl).linkedinUrl(linkedinUrl)
                .twitterUrl(twitterUrl).youtubeUrl(youtubeUrl).rootCategoryUuid(rootCategoryUuid);
    }

    public String getUrl() {
        return url;
    }

    public String getId() {
        return id;
    }

    public String getUser() {
        return user;
    }

    /** Cleartext password (decoded on read / raw on write); blank means "unset" or "unchanged". */
    public String getPassword() {
        return password;
    }

    /** True when a password is currently stored (read path); never returns the value itself. */
    public boolean isPasswordSet() {
        return StringUtils.isNotBlank(password);
    }

    public String getLogoPath() {
        return logoPath;
    }

    public String getCopyright() {
        return copyright;
    }

    public String getPrivacyUrl() {
        return privacyUrl;
    }

    public String getTermsUrl() {
        return termsUrl;
    }

    public String getCookiesUrl() {
        return cookiesUrl;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public String getTwitterUrl() {
        return twitterUrl;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public String getRootCategoryUuid() {
        return rootCategoryUuid;
    }

    public static final class Builder {
        private String url;
        private String id;
        private String user;
        private String password;
        private String logoPath;
        private String copyright;
        private String privacyUrl;
        private String termsUrl;
        private String cookiesUrl;
        private String facebookUrl;
        private String linkedinUrl;
        private String twitterUrl;
        private String youtubeUrl;
        private String rootCategoryUuid;

        private Builder() {
        }

        public Builder url(String v) {
            this.url = v;
            return this;
        }

        public Builder id(String v) {
            this.id = v;
            return this;
        }

        public Builder user(String v) {
            this.user = v;
            return this;
        }

        public Builder password(String v) {
            this.password = v;
            return this;
        }

        public Builder logoPath(String v) {
            this.logoPath = v;
            return this;
        }

        public Builder copyright(String v) {
            this.copyright = v;
            return this;
        }

        public Builder privacyUrl(String v) {
            this.privacyUrl = v;
            return this;
        }

        public Builder termsUrl(String v) {
            this.termsUrl = v;
            return this;
        }

        public Builder cookiesUrl(String v) {
            this.cookiesUrl = v;
            return this;
        }

        public Builder facebookUrl(String v) {
            this.facebookUrl = v;
            return this;
        }

        public Builder linkedinUrl(String v) {
            this.linkedinUrl = v;
            return this;
        }

        public Builder twitterUrl(String v) {
            this.twitterUrl = v;
            return this;
        }

        public Builder youtubeUrl(String v) {
            this.youtubeUrl = v;
            return this;
        }

        public Builder rootCategoryUuid(String v) {
            this.rootCategoryUuid = v;
            return this;
        }

        public ForgeSettings build() {
            return new ForgeSettings(this);
        }
    }
}
