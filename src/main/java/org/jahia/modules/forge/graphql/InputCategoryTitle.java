package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLConstructor;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;

/**
 * Per-language title input for the updateForgeCategoryTitles mutation.
 *
 * <p>Notes for future maintainers:
 * <ul>
 *   <li>The {@code @GraphQLName} value must NOT include an "Input" prefix —
 *       graphql-java-annotations adds it automatically when the type is used
 *       as an input. Naming this {@code InputForgeCategoryTitle} produced
 *       {@code InputInputForgeCategoryTitle} in the generated schema and broke
 *       validation, which in turn took down the entire GraphQL endpoint.</li>
 *   <li>Input fields are discovered from {@code @GraphQLField}-annotated
 *       getters. {@code @GraphQLConstructor} alone is not enough — without
 *       the getters being annotated, graphql-java-annotations reports
 *       "must define one or more fields".</li>
 * </ul>
 */
@GraphQLName("ForgeCategoryTitle")
@GraphQLDescription("Per-language title input for updateForgeCategoryTitles")
public class InputCategoryTitle {

    private final String language;
    private final String title;

    @GraphQLConstructor
    public InputCategoryTitle(
            @GraphQLName("language") @GraphQLNonNull String language,
            @GraphQLName("title") String title) {
        this.language = language;
        this.title = title;
    }

    @GraphQLField
    @GraphQLName("language")
    public String getLanguage() {
        return language;
    }

    @GraphQLField
    @GraphQLName("title")
    public String getTitle() {
        return title;
    }
}
