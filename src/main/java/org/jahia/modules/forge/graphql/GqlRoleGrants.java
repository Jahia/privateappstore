package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

@GraphQLName("ForgeRoleGrants")
@GraphQLDescription("A site role and the principals currently granted to it")
public class GqlRoleGrants {

    private final String role;
    private final List<GqlPrincipal> members;

    public GqlRoleGrants(String role, List<GqlPrincipal> members) {
        this.role = role;
        this.members = members;
    }

    @GraphQLField
    public String getRole() {
        return role;
    }

    @GraphQLField
    public List<GqlPrincipal> getMembers() {
        return members;
    }
}
