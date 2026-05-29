package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

@GraphQLName("ForgeManageRolesSettings")
@GraphQLDescription("Site-role management settings for a Private App Store site")
public class GqlManageRolesSettings {

    private final String siteKey;
    private final List<GqlRoleGrants> roles;

    public GqlManageRolesSettings(String siteKey, List<GqlRoleGrants> roles) {
        this.siteKey = siteKey;
        this.roles = roles;
    }

    @GraphQLField
    public String getSiteKey() {
        return siteKey;
    }

    @GraphQLField
    @GraphQLDescription("Roles applicable to the Private App Store and their current members")
    public List<GqlRoleGrants> getRoles() {
        return roles;
    }
}
