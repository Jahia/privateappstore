package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("ForgeSettings")
@GraphQLDescription("Private App Store remote-forge settings for a Jahia site")
public class GqlForgeSettings {

    private final String siteKey;
    private final String url;
    private final String id;
    private final String user;
    private final boolean passwordSet;

    public GqlForgeSettings(String siteKey, String url, String id, String user, boolean passwordSet) {
        this.siteKey = siteKey;
        this.url = url;
        this.id = id;
        this.user = user;
        this.passwordSet = passwordSet;
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
}
