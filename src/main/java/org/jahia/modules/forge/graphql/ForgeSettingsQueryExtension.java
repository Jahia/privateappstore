package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLName("ForgeSettingsQueries")
@GraphQLDescription("Private App Store forge settings queries")
public final class ForgeSettingsQueryExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeSettingsQueryExtension.class);
    private static final String JMIX_FORGE_SETTINGS = "jmix:forgeSettings";
    private static final String PERMISSION = "siteAdminForgeSettings";

    private ForgeSettingsQueryExtension() {
    }

    @GraphQLField
    @GraphQLName("forgeSettings")
    @GraphQLDescription("Read the forge settings for a site")
    public static GqlForgeSettings getForgeSettings(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey) {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<GqlForgeSettings>() {
                @Override
                public GqlForgeSettings doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    final String sitePath = "/sites/" + siteKey;
                    if (!session.nodeExists(sitePath)) {
                        return null;
                    }

                    final JCRSessionWrapper callerSession = JCRSessionFactory.getInstance().getCurrentUserSession();
                    if (!callerSession.nodeExists(sitePath)
                            || !callerSession.getNode(sitePath).hasPermission(PERMISSION)) {
                        throw new AccessDeniedException(PERMISSION);
                    }

                    final JCRNodeWrapper site = session.getNode(sitePath);
                    if (!site.isNodeType(JMIX_FORGE_SETTINGS)) {
                        return new GqlForgeSettings(siteKey, null, null, null, false);
                    }

                    return new GqlForgeSettings(
                            siteKey,
                            getStringProp(site, "forgeSettingsUrl"),
                            getStringProp(site, "forgeSettingsId"),
                            getStringProp(site, "forgeSettingsUser"),
                            site.hasProperty("forgeSettingsPassword"));
                }
            });
        } catch (RepositoryException e) {
            LOGGER.error("Error reading forge settings for site {}", siteKey, e);
            return null;
        }
    }

    private static String getStringProp(JCRNodeWrapper node, String name) throws RepositoryException {
        return node.hasProperty(name) ? node.getProperty(name).getString() : null;
    }
}
