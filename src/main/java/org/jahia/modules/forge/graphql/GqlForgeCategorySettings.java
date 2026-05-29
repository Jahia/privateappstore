package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

@GraphQLName("ForgeCategorySettings")
@GraphQLDescription("Forge category settings for a Jahia site")
public class GqlForgeCategorySettings {

    private final String siteKey;
    private final String rootCategoryUuid;
    private final String rootCategoryPath;
    private final String rootCategoryDisplayName;
    private final List<String> siteLanguages;
    private final List<GqlForgeCategory> categories;

    public GqlForgeCategorySettings(String siteKey, String rootCategoryUuid,
                                    String rootCategoryPath, String rootCategoryDisplayName,
                                    List<String> siteLanguages, List<GqlForgeCategory> categories) {
        this.siteKey = siteKey;
        this.rootCategoryUuid = rootCategoryUuid;
        this.rootCategoryPath = rootCategoryPath;
        this.rootCategoryDisplayName = rootCategoryDisplayName;
        this.siteLanguages = siteLanguages;
        this.categories = categories;
    }

    @GraphQLField
    public String getSiteKey() {
        return siteKey;
    }

    @GraphQLField
    @GraphQLDescription("UUID of the JCR node currently configured as the site's root category")
    public String getRootCategoryUuid() {
        return rootCategoryUuid;
    }

    @GraphQLField
    public String getRootCategoryPath() {
        return rootCategoryPath;
    }

    @GraphQLField
    public String getRootCategoryDisplayName() {
        return rootCategoryDisplayName;
    }

    @GraphQLField
    @GraphQLDescription("Languages activated on the site, used to drive the per-language title editor")
    public List<String> getSiteLanguages() {
        return siteLanguages;
    }

    @GraphQLField
    @GraphQLDescription("Child jnt:category nodes of the root category; empty when no root is configured")
    public List<GqlForgeCategory> getCategories() {
        return categories;
    }
}
