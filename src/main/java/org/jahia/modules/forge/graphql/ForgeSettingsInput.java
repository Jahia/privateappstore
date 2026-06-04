package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLConstructor;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * Connection + branding fields for {@code updateForgeSettings}, grouped into a
 * single input object so the mutation stays within a reasonable parameter count.
 * The footer's legal/social links are nested in {@link ForgeFooterLinksInput} so
 * this constructor itself stays within the parameter limit too.
 *
 * <p>Notes for future maintainers (see {@link InputCategoryTitle} for the same
 * pattern and pitfalls):
 * <ul>
 *   <li>The {@code @GraphQLName} value must NOT include an "Input" prefix —
 *       graphql-java-annotations prepends it automatically when the type is used
 *       as an input, so this surfaces in the schema as
 *       {@code InputForgeSettingsParams}.</li>
 *   <li>Input fields are discovered from {@code @GraphQLField}-annotated getters;
 *       {@code @GraphQLConstructor} alone is not enough.</li>
 *   <li>The {@code getPrivacyUrl()}…{@code getYoutubeUrl()} getters are plain
 *       (NOT {@code @GraphQLField}) convenience accessors that flatten the nested
 *       links for the mutation — they are null-safe so an absent {@code footerLinks}
 *       clears those properties, matching the previous flat-argument behavior.</li>
 * </ul>
 */
@GraphQLName("ForgeSettingsParams")
@GraphQLDescription("Connection + branding fields for updateForgeSettings")
public class ForgeSettingsInput {

    private final String url;
    private final String id;
    private final String user;
    private final String password;
    private final String logo;
    private final String copyright;
    private final ForgeFooterLinksInput footerLinks;

    @GraphQLConstructor
    public ForgeSettingsInput(
            @GraphQLName("url") String url,
            @GraphQLName("id") String id,
            @GraphQLName("user") String user,
            @GraphQLName("password") String password,
            @GraphQLName("logo") String logo,
            @GraphQLName("copyright") String copyright,
            @GraphQLName("footerLinks") ForgeFooterLinksInput footerLinks) {
        this.url = url;
        this.id = id;
        this.user = user;
        this.password = password;
        this.logo = logo;
        this.copyright = copyright;
        this.footerLinks = footerLinks;
    }

    @GraphQLField
    @GraphQLName("url")
    public String getUrl() {
        return url;
    }

    @GraphQLField
    @GraphQLName("id")
    public String getId() {
        return id;
    }

    @GraphQLField
    @GraphQLName("user")
    public String getUser() {
        return user;
    }

    @GraphQLField
    @GraphQLName("password")
    public String getPassword() {
        return password;
    }

    @GraphQLField
    @GraphQLName("logo")
    @GraphQLDescription("UUID or path of the logo image node; blank clears it")
    public String getLogo() {
        return logo;
    }

    @GraphQLField
    @GraphQLName("copyright")
    public String getCopyright() {
        return copyright;
    }

    @GraphQLField
    @GraphQLName("footerLinks")
    @GraphQLDescription("Footer legal + social links")
    public ForgeFooterLinksInput getFooterLinks() {
        return footerLinks;
    }

    // Plain (non-GraphQL) null-safe accessors that flatten the nested links for
    // the mutation body. Absent footerLinks => null => the property is cleared.
    public String getPrivacyUrl() {
        return footerLinks == null ? null : footerLinks.getPrivacyUrl();
    }

    public String getTermsUrl() {
        return footerLinks == null ? null : footerLinks.getTermsUrl();
    }

    public String getCookiesUrl() {
        return footerLinks == null ? null : footerLinks.getCookiesUrl();
    }

    public String getFacebookUrl() {
        return footerLinks == null ? null : footerLinks.getFacebookUrl();
    }

    public String getLinkedinUrl() {
        return footerLinks == null ? null : footerLinks.getLinkedinUrl();
    }

    public String getTwitterUrl() {
        return footerLinks == null ? null : footerLinks.getTwitterUrl();
    }

    public String getYoutubeUrl() {
        return footerLinks == null ? null : footerLinks.getYoutubeUrl();
    }
}
