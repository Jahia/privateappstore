package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("ForgeSettings")
@GraphQLDescription("Private App Store remote-forge settings and site branding for a Jahia site")
public class GqlForgeSettings {

    private final String siteKey;
    private final String url;
    private final String id;
    private final String user;
    private final boolean passwordSet;
    // Branding / footer (rendered by the store-template chrome).
    private final String logoPath;
    private final String copyright;
    private final String privacyUrl;
    private final String termsUrl;
    private final String cookiesUrl;
    private final String facebookUrl;
    private final String linkedinUrl;
    private final String twitterUrl;
    private final String youtubeUrl;

    private GqlForgeSettings(Builder b) {
        this.siteKey = b.siteKey;
        this.url = b.url;
        this.id = b.id;
        this.user = b.user;
        this.passwordSet = b.passwordSet;
        this.logoPath = b.logoPath;
        this.copyright = b.copyright;
        this.privacyUrl = b.privacyUrl;
        this.termsUrl = b.termsUrl;
        this.cookiesUrl = b.cookiesUrl;
        this.facebookUrl = b.facebookUrl;
        this.linkedinUrl = b.linkedinUrl;
        this.twitterUrl = b.twitterUrl;
        this.youtubeUrl = b.youtubeUrl;
    }

    public static Builder builder(String siteKey) {
        return new Builder(siteKey);
    }

    @GraphQLField
    @GraphQLDescription("The site key")
    public String getSiteKey() {
        return siteKey;
    }

    @GraphQLField
    @GraphQLDescription("Remote forge endpoint URL")
    public String getUrl() {
        return url;
    }

    @GraphQLField
    @GraphQLDescription("Remote forge identifier")
    public String getId() {
        return id;
    }

    @GraphQLField
    @GraphQLDescription("Remote forge username")
    public String getUser() {
        return user;
    }

    // Never expose the stored password to the client. The UI only needs to know
    // whether one is currently set so it can render an unchanged-vs-replace UX.
    @GraphQLField
    @GraphQLDescription("True if a password is currently stored")
    public boolean isPasswordSet() {
        return passwordSet;
    }

    @GraphQLField
    @GraphQLDescription("JCR path of the configured logo image, or null")
    public String getLogoPath() {
        return logoPath;
    }

    @GraphQLField
    @GraphQLDescription("Footer copyright text")
    public String getCopyright() {
        return copyright;
    }

    @GraphQLField
    @GraphQLDescription("Footer privacy-policy link")
    public String getPrivacyUrl() {
        return privacyUrl;
    }

    @GraphQLField
    @GraphQLDescription("Footer terms-of-use link")
    public String getTermsUrl() {
        return termsUrl;
    }

    @GraphQLField
    @GraphQLDescription("Footer cookie-policy link")
    public String getCookiesUrl() {
        return cookiesUrl;
    }

    @GraphQLField
    @GraphQLDescription("Footer Facebook link")
    public String getFacebookUrl() {
        return facebookUrl;
    }

    @GraphQLField
    @GraphQLDescription("Footer LinkedIn link")
    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    @GraphQLField
    @GraphQLDescription("Footer Twitter/X link")
    public String getTwitterUrl() {
        return twitterUrl;
    }

    @GraphQLField
    @GraphQLDescription("Footer YouTube link")
    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    /** Many-optional-field builder (keeps the read/write call sites safe and readable). */
    public static final class Builder {
        private final String siteKey;
        private String url;
        private String id;
        private String user;
        private boolean passwordSet;
        private String logoPath;
        private String copyright;
        private String privacyUrl;
        private String termsUrl;
        private String cookiesUrl;
        private String facebookUrl;
        private String linkedinUrl;
        private String twitterUrl;
        private String youtubeUrl;

        private Builder(String siteKey) {
            this.siteKey = siteKey;
        }

        public Builder url(String v) { this.url = v; return this; }
        public Builder id(String v) { this.id = v; return this; }
        public Builder user(String v) { this.user = v; return this; }
        public Builder passwordSet(boolean v) { this.passwordSet = v; return this; }
        public Builder logoPath(String v) { this.logoPath = v; return this; }
        public Builder copyright(String v) { this.copyright = v; return this; }
        public Builder privacyUrl(String v) { this.privacyUrl = v; return this; }
        public Builder termsUrl(String v) { this.termsUrl = v; return this; }
        public Builder cookiesUrl(String v) { this.cookiesUrl = v; return this; }
        public Builder facebookUrl(String v) { this.facebookUrl = v; return this; }
        public Builder linkedinUrl(String v) { this.linkedinUrl = v; return this; }
        public Builder twitterUrl(String v) { this.twitterUrl = v; return this; }
        public Builder youtubeUrl(String v) { this.youtubeUrl = v; return this; }

        public GqlForgeSettings build() {
            return new GqlForgeSettings(this);
        }
    }
}
