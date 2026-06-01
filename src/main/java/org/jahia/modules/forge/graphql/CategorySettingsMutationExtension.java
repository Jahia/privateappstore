package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.sites.JahiaSitesService;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Locale;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLName("CategorySettingsMutations")
@GraphQLDescription("Private App Store category settings mutations")
public final class CategorySettingsMutationExtension {

    private static final String PERMISSION = "siteAdminForgeSettings";
    private static final String SITES_PATH = JahiaSitesService.SITES_JCR_PATH + FileSystem.SEPARATOR;
    private static final String ROOT_CATEGORY = "rootCategory";
    private static final String JCR_TITLE = "jcr:title";
    private static final String JNT_CATEGORY = "jnt:category";
    private static final String JMIX_FORGE_SETTINGS = "jmix:forgeSettings";
    private static final String WORKSPACE_DEFAULT = "default";

    private CategorySettingsMutationExtension() {
    }

    @GraphQLField
    @GraphQLName("setRootCategory")
    @GraphQLDescription("Set the JCR node configured as the site's root category")
    public static Boolean setRootCategory(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey,
            @GraphQLName("rootCategoryUuid") @GraphQLNonNull final String rootCategoryUuid) {
        return execute(siteKey, session -> {
            final JCRNodeWrapper site = session.getNode(SITES_PATH + siteKey);
            // rootCategory is declared on jmix:forgeSettings — the site must
            // carry that mixin before the setProperty call will persist.
            if (!site.isNodeType(JMIX_FORGE_SETTINGS)) {
                site.addMixin(JMIX_FORGE_SETTINGS);
            }

            // Validate the target is an actual category: the work runs in a system
            // session that bypasses ACLs, so an unvalidated UUID could point the
            // site's root at any node in the repository.
            final JCRNodeWrapper category = resolveCategory(session, rootCategoryUuid);
            site.setProperty(ROOT_CATEGORY, category);
            session.save();
            return Boolean.TRUE;
        });
    }

    @GraphQLField
    @GraphQLName("addForgeCategory")
    @GraphQLDescription("Create a jnt:category node under the site's root category, returning its UUID")
    public static String addForgeCategory(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey,
            @GraphQLName("name") @GraphQLNonNull final String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Category name must not be blank");
        }

        return execute(siteKey, session -> {
            final JCRNodeWrapper site = session.getNode(SITES_PATH + siteKey);
            if (!site.hasProperty(ROOT_CATEGORY)) {
                throw new RepositoryException("No root category configured for site " + siteKey);
            }

            final JCRNodeWrapper root = (JCRNodeWrapper) site.getProperty(ROOT_CATEGORY).getNode();
            final String safeName = JCRContentUtils.findAvailableNodeName(root, name);
            final JCRNodeWrapper category = root.addNode(safeName, JNT_CATEGORY);
            session.save();
            return category.getIdentifier();
        });
    }

    @GraphQLField
    @GraphQLName("updateForgeCategoryTitles")
    @GraphQLDescription("Set per-language jcr:title on a category. Blank/null title removes the translation.")
    public static Boolean updateForgeCategoryTitles(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey,
            @GraphQLName("uuid") @GraphQLNonNull final String uuid,
            @GraphQLName("titles") @GraphQLNonNull final List<InputCategoryTitle> titles) {
        return execute(siteKey, session -> {
            // Confine titling to a category within THIS site's root-category subtree
            // (the gate is site-scoped; validate the caller-supplied UUID).
            assertManagedBySite(session.getNode(SITES_PATH + siteKey), resolveCategory(session, uuid));
            for (InputCategoryTitle title : titles) {
                applyTitle(uuid, title);
            }

            return Boolean.TRUE;
        });
    }

    private static void applyTitle(String uuid, InputCategoryTitle title) throws RepositoryException {
        if (title == null || StringUtils.isBlank(title.getLanguage())) {
            return;
        }

        // Locale.forLanguageTag avoids the now-deprecated Locale(String) constructor
        // while preserving the simple "en"/"fr" tag semantics the JCR session expects.
        final JCRSessionWrapper localized = JCRSessionFactory.getInstance()
                .getCurrentUserSession(WORKSPACE_DEFAULT, Locale.forLanguageTag(title.getLanguage()));
        final JCRNodeWrapper node = localized.getNodeByIdentifier(uuid);
        if (StringUtils.isBlank(title.getTitle())) {
            if (node.hasProperty(JCR_TITLE)) {
                node.getProperty(JCR_TITLE).remove();
            }
        } else {
            node.setProperty(JCR_TITLE, title.getTitle());
        }

        localized.save();
    }

    @GraphQLField
    @GraphQLName("deleteForgeCategory")
    @GraphQLDescription("Delete a category node by UUID")
    public static Boolean deleteForgeCategory(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey,
            @GraphQLName("uuid") @GraphQLNonNull final String uuid) {
        return execute(siteKey, session -> {
            // Confine deletion to a category within THIS site's root-category subtree.
            // The gate authorizes against the site, but the system session bypasses
            // ACLs — without this a site admin could delete any node by UUID.
            final JCRNodeWrapper node = resolveCategory(session, uuid);
            assertManagedBySite(session.getNode(SITES_PATH + siteKey), node);
            node.remove();
            session.save();
            return Boolean.TRUE;
        });
    }

    /**
     * Resolve a caller-supplied category UUID, rejecting anything that is not an
     * existing jnt:category. The mutation gate authorizes against the SITE, but the
     * work then runs in a system session that bypasses JCR ACLs — so a UUID argument
     * must be validated or a site administrator could target arbitrary repository
     * nodes by id (SECURITY-571).
     */
    private static JCRNodeWrapper resolveCategory(JCRSessionWrapper session, String uuid) throws RepositoryException {
        final JCRNodeWrapper node;
        try {
            node = session.getNodeByIdentifier(uuid);
        } catch (ItemNotFoundException e) {
            throw new AccessDeniedException("No such category: " + uuid);
        }
        if (!node.isNodeType(JNT_CATEGORY)) {
            throw new AccessDeniedException("Not a category: " + uuid);
        }
        return node;
    }

    /**
     * Enforce that a category lives within the site's configured root-category
     * subtree — i.e. one this site administrator actually manages — never the root
     * itself nor a node outside it. Without this a site admin could edit or delete
     * another site's categories (or arbitrary nodes) by passing their own siteKey
     * plus a foreign UUID.
     */
    private static void assertManagedBySite(JCRNodeWrapper site, JCRNodeWrapper category) throws RepositoryException {
        if (!site.hasProperty(ROOT_CATEGORY)) {
            throw new AccessDeniedException("No root category configured for site " + site.getName());
        }
        final JCRNodeWrapper root = (JCRNodeWrapper) site.getProperty(ROOT_CATEGORY).getNode();
        if (!category.getPath().startsWith(root.getPath() + "/")) {
            throw new AccessDeniedException(
                    "Category " + category.getPath() + " is not within the site's root category");
        }
    }

    private static <T> T execute(String siteKey, JCRCallback<T> work) {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
                final String sitePath = SITES_PATH + siteKey;
                if (!session.nodeExists(sitePath)) {
                    throw new RepositoryException("Site not found: " + siteKey);
                }

                final JCRSessionWrapper callerSession = JCRSessionFactory.getInstance().getCurrentUserSession();
                if (!callerSession.nodeExists(sitePath)
                        || !callerSession.getNode(sitePath).hasPermission(PERMISSION)) {
                    throw new AccessDeniedException(PERMISSION);
                }

                return work.doInJCR(session);
            });
        } catch (RepositoryException e) {
            throw new ForgeSettingsException(
                    "Category settings mutation failed for site " + siteKey, e);
        }
    }
}
