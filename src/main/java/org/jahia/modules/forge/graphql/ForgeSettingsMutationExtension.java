package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
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
import java.nio.charset.StandardCharsets;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLName("ForgeSettingsMutations")
@GraphQLDescription("Private App Store forge settings mutations")
public final class ForgeSettingsMutationExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeSettingsMutationExtension.class);
    private static final String JMIX_FORGE_SETTINGS = "jmix:forgeSettings";
    private static final String PERMISSION = "siteAdminForgeSettings";

    private ForgeSettingsMutationExtension() {
    }

    @GraphQLField
    @GraphQLName("updateForgeSettings")
    @GraphQLDescription("Create or update the forge settings for a site")
    public static GqlForgeSettings updateForgeSettings(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey,
            @GraphQLName("url") final String url,
            @GraphQLName("id") final String id,
            @GraphQLName("user") final String user,
            @GraphQLName("password") final String password) {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<GqlForgeSettings>() {
                @Override
                public GqlForgeSettings doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    final String sitePath = "/sites/" + siteKey;
                    if (!session.nodeExists(sitePath)) {
                        throw new RepositoryException("Site not found: " + siteKey);
                    }

                    final JCRSessionWrapper callerSession = JCRSessionFactory.getInstance().getCurrentUserSession();
                    if (!callerSession.nodeExists(sitePath)
                            || !callerSession.getNode(sitePath).hasPermission(PERMISSION)) {
                        throw new AccessDeniedException(PERMISSION);
                    }

                    final JCRNodeWrapper site = session.getNode(sitePath);
                    if (!site.isNodeType(JMIX_FORGE_SETTINGS)) {
                        site.addMixin(JMIX_FORGE_SETTINGS);
                    }

                    setStringProp(site, "forgeSettingsUrl", url);
                    setStringProp(site, "forgeSettingsId", id);
                    setStringProp(site, "forgeSettingsUser", user);

                    // Password handling preserves the previous Spring Web Flow behavior:
                    // store as base64 (obfuscation only — not encryption), blank input
                    // leaves the existing value alone so the UI can omit the field for
                    // already-configured sites without wiping it.
                    if (StringUtils.isNotBlank(password)) {
                        site.setProperty(
                                "forgeSettingsPassword",
                                Base64.encode(password.getBytes(StandardCharsets.UTF_8)));
                    }

                    session.save();

                    return new GqlForgeSettings(
                            siteKey,
                            url,
                            id,
                            user,
                            site.hasProperty("forgeSettingsPassword"));
                }
            });
        } catch (RepositoryException e) {
            LOGGER.error("Error updating forge settings for site {}", siteKey, e);
            return null;
        }
    }

    private static void setStringProp(JCRNodeWrapper node, String name, String value)
            throws RepositoryException {
        if (StringUtils.isNotBlank(value)) {
            node.setProperty(name, value);
        } else if (node.hasProperty(name)) {
            node.getProperty(name).remove();
        }
    }
}
