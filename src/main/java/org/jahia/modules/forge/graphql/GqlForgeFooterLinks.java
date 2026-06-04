package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * Footer legal + social links, read counterpart of {@link ForgeFooterLinksInput}.
 * Exposed as a nested {@code footerLinks} object on {@link GqlForgeSettings} so the
 * READ shape mirrors the WRITE input shape — a caller can round-trip the same
 * structure (SECURITY-571 review: read/write symmetry).
 */
@GraphQLName("ForgeFooterLinksRead")
@GraphQLDescription("Footer legal + social links")
public class GqlForgeFooterLinks {

    private final String privacyUrl;
    private final String termsUrl;
    private final String cookiesUrl;
    private final String facebookUrl;
    private final String linkedinUrl;
    private final String twitterUrl;
    private final String youtubeUrl;

    GqlForgeFooterLinks(String privacyUrl, String termsUrl, String cookiesUrl, String facebookUrl,
                        String linkedinUrl, String twitterUrl, String youtubeUrl) {
        this.privacyUrl = privacyUrl;
        this.termsUrl = termsUrl;
        this.cookiesUrl = cookiesUrl;
        this.facebookUrl = facebookUrl;
        this.linkedinUrl = linkedinUrl;
        this.twitterUrl = twitterUrl;
        this.youtubeUrl = youtubeUrl;
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
}
