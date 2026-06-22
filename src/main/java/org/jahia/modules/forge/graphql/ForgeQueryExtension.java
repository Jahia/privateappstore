package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

/**
 * Adds the single {@code forge} namespace field to the root Query, under which every Private
 * App Store read operation lives ({@code query { forge { settings(...) } }}).
 *
 * <p>This replaces the earlier flat fields (forgeSettings, forgeCategorySettings,
 * manageRolesSettings, searchForgePrincipals) that each extended the global Query type and
 * polluted its top level. The bundle's {@code DXGraphQLExtensionsProvider} auto-registers any
 * {@code @GraphQLTypeExtension} class, so adding this one and dropping the annotation from the
 * old extensions is all that is required.
 */
@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLDescription("Private App Store query namespace")
public final class ForgeQueryExtension {

    private ForgeQueryExtension() {
    }

    @GraphQLField
    @GraphQLName("forge")
    @GraphQLDescription("Private App Store queries")
    public static ForgeQuery forge() {
        return new ForgeQuery();
    }
}
