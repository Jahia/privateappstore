package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLName("CategorySettingsQueries")
@GraphQLDescription("Private App Store category settings queries")
public final class CategorySettingsQueryExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategorySettingsQueryExtension.class);
    private static final String PERMISSION = "siteAdminForgeSettings";
    private static final String ROOT_CATEGORY = "rootCategory";
    private static final String JCR_TITLE = "jcr:title";
    private static final String JNT_CATEGORY = "jnt:category";
    private static final String WORKSPACE_DEFAULT = "default";
    private static final String WORKSPACE_LIVE = "live";

    private CategorySettingsQueryExtension() {
    }

    @GraphQLField
    @GraphQLName("forgeCategorySettings")
    @GraphQLDescription("Read the forge category settings for a site")
    public static GqlForgeCategorySettings getForgeCategorySettings(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey) {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<GqlForgeCategorySettings>() {
                @Override
                public GqlForgeCategorySettings doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    final String sitePath = "/sites/" + siteKey;
                    if (!session.nodeExists(sitePath)) {
                        return null;
                    }

                    final JCRSessionWrapper callerSession = JCRSessionFactory.getInstance().getCurrentUserSession();
                    if (!callerSession.nodeExists(sitePath)
                            || !callerSession.getNode(sitePath).hasPermission(PERMISSION)) {
                        throw new AccessDeniedException(PERMISSION);
                    }

                    final JCRSiteNode site = (JCRSiteNode) session.getNode(sitePath);
                    final List<String> siteLanguages = new ArrayList<>();
                    for (Locale locale : site.getLanguagesAsLocales()) {
                        siteLanguages.add(locale.getLanguage());
                    }

                    if (!site.hasProperty(ROOT_CATEGORY)) {
                        return new GqlForgeCategorySettings(siteKey, null, null, null,
                                siteLanguages, Collections.emptyList());
                    }

                    final JCRNodeWrapper root;
                    try {
                        root = (JCRNodeWrapper) site.getProperty(ROOT_CATEGORY).getNode();
                    } catch (ItemNotFoundException e) {
                        LOGGER.warn("rootCategory property points at a missing node for site {}", siteKey);
                        return new GqlForgeCategorySettings(siteKey, null, null, null,
                                siteLanguages, Collections.emptyList());
                    }

                    return new GqlForgeCategorySettings(
                            siteKey,
                            root.getIdentifier(),
                            root.getPath(),
                            root.getDisplayableName(),
                            siteLanguages,
                            readCategories(root, site));
                }
            });
        } catch (RepositoryException e) {
            LOGGER.error("Error reading category settings for site {}", siteKey, e);
            return null;
        }
    }

    private static List<GqlForgeCategory> readCategories(JCRNodeWrapper root, JCRSiteNode site)
            throws RepositoryException {
        final List<GqlForgeCategory> categories = new ArrayList<>();
        final JCRNodeIteratorWrapper children = root.getNodes();
        while (children.hasNext()) {
            final JCRNodeWrapper child = (JCRNodeWrapper) children.nextNode();
            if (!child.isNodeType(JNT_CATEGORY)) {
                continue;
            }

            categories.add(new GqlForgeCategory(
                    child.getIdentifier(),
                    child.getName(),
                    child.getDisplayableName(),
                    readTitles(child.getIdentifier(), site),
                    readUsages(child.getIdentifier())));
        }

        return categories;
    }

    private static List<GqlCategoryTitle> readTitles(String uuid, JCRSiteNode site)
            throws RepositoryException {
        final List<GqlCategoryTitle> titles = new ArrayList<>();
        for (Locale locale : site.getLanguagesAsLocales()) {
            final JCRSessionWrapper localized = JCRSessionFactory.getInstance()
                    .getCurrentUserSession(WORKSPACE_DEFAULT, locale);
            try {
                final JCRNodeWrapper node = localized.getNodeByIdentifier(uuid);
                String title = null;
                if (node.hasProperty(JCR_TITLE)) {
                    final String raw = node.getProperty(JCR_TITLE).getString();
                    if (raw != null && !raw.isEmpty()) {
                        title = raw;
                    }
                }

                titles.add(new GqlCategoryTitle(locale.getLanguage(), title));
            } catch (ItemNotFoundException e) {
                // Localized translation node not present yet
                titles.add(new GqlCategoryTitle(locale.getLanguage(), null));
            }
        }

        return titles;
    }

    private static List<String> readUsages(String uuid) throws RepositoryException {
        final Set<String> usages = new LinkedHashSet<>();
        collectUsages(uuid, WORKSPACE_DEFAULT, usages);
        collectUsages(uuid, WORKSPACE_LIVE, usages);
        return new ArrayList<>(usages);
    }

    private static void collectUsages(String uuid, String workspace, Set<String> usages)
            throws RepositoryException {
        final JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
        try {
            final JCRNodeWrapper node = session.getNodeByIdentifier(uuid);
            final PropertyIterator refs = node.getWeakReferences();
            while (refs.hasNext()) {
                final Property prop = refs.nextProperty();
                usages.add(((JCRNodeWrapper) prop.getParent()).getDisplayableName());
            }
        } catch (ItemNotFoundException e) {
            // Category not visible in this workspace
        }
    }
}
