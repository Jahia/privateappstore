package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

@GraphQLName("ForgeCategory")
@GraphQLDescription("A category node under the site's root category")
public class GqlForgeCategory {

    private final String uuid;
    private final String name;
    private final String displayName;
    private final List<GqlCategoryTitle> titles;
    private final List<String> usages;

    public GqlForgeCategory(String uuid, String name, String displayName,
                            List<GqlCategoryTitle> titles, List<String> usages) {
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
        this.titles = titles;
        this.usages = usages;
    }

    @GraphQLField
    public String getUuid() {
        return uuid;
    }

    @GraphQLField
    public String getName() {
        return name;
    }

    @GraphQLField
    public String getDisplayName() {
        return displayName;
    }

    @GraphQLField
    @GraphQLDescription("Localized titles, one entry per site language (title may be null)")
    public List<GqlCategoryTitle> getTitles() {
        return titles;
    }

    @GraphQLField
    @GraphQLDescription("Displayable names of nodes that weakly reference this category")
    public List<String> getUsages() {
        return usages;
    }
}
