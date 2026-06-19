package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

/**
 * Adds the single {@code forge} namespace field to the root Mutation, under which every Private
 * App Store write operation lives ({@code mutation { forge { updateSettings(...) } }}).
 *
 * <p>This replaces the earlier flat fields (updateForgeSettings, setRootCategory,
 * addForgeCategory, ...) that each extended the global Mutation type and polluted its top level.
 * The bundle's {@code DXGraphQLExtensionsProvider} auto-registers any {@code @GraphQLTypeExtension}
 * class, so adding this one and dropping the annotation from the old extensions is all that is
 * required.
 */
@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLDescription("Private App Store mutation namespace")
public final class ForgeMutationExtension {

    private ForgeMutationExtension() {
    }

    @GraphQLField
    @GraphQLName("forge")
    @GraphQLDescription("Private App Store mutations")
    public static ForgeMutation forge() {
        return new ForgeMutation();
    }
}
