package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("ForgeCategoryTitle")
@GraphQLDescription("Per-language title for a forge category")
public class GqlCategoryTitle {

    private final String language;
    private final String title;

    public GqlCategoryTitle(String language, String title) {
        this.language = language;
        this.title = title;
    }

    @GraphQLField
    public String getLanguage() {
        return language;
    }

    @GraphQLField
    @GraphQLDescription("Localized title, null when not translated in this language")
    public String getTitle() {
        return title;
    }
}
