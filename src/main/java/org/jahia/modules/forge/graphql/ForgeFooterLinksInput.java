package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLConstructor;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * The store footer's legal + social links, grouped so the parent
 * {@link ForgeSettingsInput} constructor stays within a sane parameter count.
 *
 * <p>Same graphql-java-annotations rules as {@link InputCategoryTitle}: the
 * {@code @GraphQLName} omits the "Input" prefix (the library adds it, so this is
 * {@code InputForgeFooterLinks} in the schema), and input fields are discovered
 * from {@code @GraphQLField}-annotated getters.
 */
@GraphQLName("ForgeFooterLinks")
@GraphQLDescription("Store footer legal + social links")
public class ForgeFooterLinksInput {

    private final String privacyUrl;
    private final String termsUrl;
    private final String cookiesUrl;
    private final String facebookUrl;
    private final String linkedinUrl;
    private final String twitterUrl;
    private final String youtubeUrl;

    @GraphQLConstructor
    public ForgeFooterLinksInput(
            @GraphQLName("privacyUrl") String privacyUrl,
            @GraphQLName("termsUrl") String termsUrl,
            @GraphQLName("cookiesUrl") String cookiesUrl,
            @GraphQLName("facebookUrl") String facebookUrl,
            @GraphQLName("linkedinUrl") String linkedinUrl,
            @GraphQLName("twitterUrl") String twitterUrl,
            @GraphQLName("youtubeUrl") String youtubeUrl) {
        this.privacyUrl = privacyUrl;
        this.termsUrl = termsUrl;
        this.cookiesUrl = cookiesUrl;
        this.facebookUrl = facebookUrl;
        this.linkedinUrl = linkedinUrl;
        this.twitterUrl = twitterUrl;
        this.youtubeUrl = youtubeUrl;
    }

    @GraphQLField
    @GraphQLName("privacyUrl")
    public String getPrivacyUrl() {
        return privacyUrl;
    }

    @GraphQLField
    @GraphQLName("termsUrl")
    public String getTermsUrl() {
        return termsUrl;
    }

    @GraphQLField
    @GraphQLName("cookiesUrl")
    public String getCookiesUrl() {
        return cookiesUrl;
    }

    @GraphQLField
    @GraphQLName("facebookUrl")
    public String getFacebookUrl() {
        return facebookUrl;
    }

    @GraphQLField
    @GraphQLName("linkedinUrl")
    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    @GraphQLField
    @GraphQLName("twitterUrl")
    public String getTwitterUrl() {
        return twitterUrl;
    }

    @GraphQLField
    @GraphQLName("youtubeUrl")
    public String getYoutubeUrl() {
        return youtubeUrl;
    }
}
