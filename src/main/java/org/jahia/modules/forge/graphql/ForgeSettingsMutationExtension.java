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
            @GraphQLName("password") final String password,
            @GraphQLName("logo") @GraphQLDescription("UUID or path of the logo image node; blank clears it") final String logo,
            @GraphQLName("copyright") final String copyright,
            @GraphQLName("privacyUrl") final String privacyUrl,
            @GraphQLName("termsUrl") final String termsUrl,
            @GraphQLName("cookiesUrl") final String cookiesUrl,
            @GraphQLName("facebookUrl") final String facebookUrl,
            @GraphQLName("linkedinUrl") final String linkedinUrl,
            @GraphQLName("twitterUrl") final String twitterUrl,
            @GraphQLName("youtubeUrl") final String youtubeUrl) {
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

                    // Branding / footer. Blank clears these (they are plain editable text,
                    // unlike the write-only password). The logo is a weakreference.
                    setLogoRef(session, site, logo);
                    setStringProp(site, "forgeSettingsCopyright", copyright);
                    setStringProp(site, "forgeSettingsPrivacyUrl", privacyUrl);
                    setStringProp(site, "forgeSettingsTermsUrl", termsUrl);
                    setStringProp(site, "forgeSettingsCookiesUrl", cookiesUrl);
                    setStringProp(site, "forgeSettingsFacebookUrl", facebookUrl);
                    setStringProp(site, "forgeSettingsLinkedinUrl", linkedinUrl);
                    setStringProp(site, "forgeSettingsTwitterUrl", twitterUrl);
                    setStringProp(site, "forgeSettingsYoutubeUrl", youtubeUrl);

                    session.save();

                    return ForgeSettingsReader.read(site, siteKey);
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

    /**
     * Set (or clear, when blank) the logo weakreference. The value is the picked
     * media node's UUID or absolute path; an unresolvable value is ignored so a
     * bad reference can never wipe a previously configured logo silently — the
     * mutation simply leaves it untouched.
     */
    private static void setLogoRef(JCRSessionWrapper session, JCRNodeWrapper site, String logo)
            throws RepositoryException {
        if (StringUtils.isBlank(logo)) {
            if (site.hasProperty("forgeSettingsLogo")) {
                site.getProperty("forgeSettingsLogo").remove();
            }
            return;
        }
        final JCRNodeWrapper target = logo.startsWith("/")
                ? session.getNode(logo)
                : (JCRNodeWrapper) session.getNodeByIdentifier(logo);
        site.setProperty("forgeSettingsLogo", target);
    }
}
