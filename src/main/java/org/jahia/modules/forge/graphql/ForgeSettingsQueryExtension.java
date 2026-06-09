package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.sites.JahiaSitesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLName("ForgeSettingsQueries")
@GraphQLDescription("Private App Store forge settings queries")
public final class ForgeSettingsQueryExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeSettingsQueryExtension.class);
    private static final String PERMISSION = "siteAdminForgeSettings";
    private static final String SITES_PATH = JahiaSitesService.SITES_JCR_PATH + FileSystem.SEPARATOR;

    private ForgeSettingsQueryExtension() {
    }

    @GraphQLField
    @GraphQLName("forgeSettings")
    @GraphQLDescription("Read the forge settings for a site")
    public static GqlForgeSettings getForgeSettings(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey) {
        ForgeSettingsMutationExtension.validateSiteKey(siteKey);
        final String sitePath = SITES_PATH + siteKey;
        try {
            final JCRSessionWrapper caller = JCRSessionFactory.getInstance().getCurrentUserSession();
            if (!caller.nodeExists(sitePath)) {
                return null;
            }
            if (!caller.getNode(sitePath).hasPermission(PERMISSION)) {
                throw new AccessDeniedException(PERMISSION);
            }
        } catch (AccessDeniedException e) {
            // Surface authorization failures as an error. AccessDeniedException is itself a
            // RepositoryException, so without this more-specific catch the broad one below would
            // swallow it and return null — rendering in the admin UI as an empty settings panel
            // indistinguishable from a server fault (SECURITY-571 ergonomy).
            throw new ForgeSettingsException("Not allowed to read forge settings for site " + siteKey, e);
        } catch (RepositoryException e) {
            LOGGER.error("Error reading forge settings for site {}", siteKey, e);
            return null;
        }
        return ForgeSettingsReader.from(ForgeSettingsMutationExtension.service().get(siteKey), siteKey);
    }
}
