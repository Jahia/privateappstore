package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("ForgePrincipal")
@GraphQLDescription("A user or group principal that can be granted a site role")
public class GqlPrincipal {

    private final String name;
    private final PrincipalType type;
    private final String displayName;

    public GqlPrincipal(String name, PrincipalType type, String displayName) {
        this.name = name;
        this.type = type;
        this.displayName = displayName;
    }

    @GraphQLField
    public String getName() {
        return name;
    }

    @GraphQLField
    public PrincipalType getType() {
        return type;
    }

    @GraphQLField
    public String getDisplayName() {
        return displayName;
    }
}
