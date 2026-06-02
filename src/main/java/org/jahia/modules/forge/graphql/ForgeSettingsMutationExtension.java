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
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.nio.charset.StandardCharsets;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLName("ForgeSettingsMutations")
@GraphQLDescription("Private App Store forge settings mutations")
public final class ForgeSettingsMutationExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeSettingsMutationExtension.class);
    private static final String JMIX_FORGE_SETTINGS = "jmix:forgeSettings";
    private static final String PERMISSION = "siteAdminForgeSettings";
    private static final String FORGE_SETTINGS_LOGO = "forgeSettingsLogo";

    private ForgeSettingsMutationExtension() {
    }

    @GraphQLField
    @GraphQLName("updateForgeSettings")
    @GraphQLDescription("Create or update the forge settings for a site")
    public static GqlForgeSettings updateForgeSettings(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey,
            @GraphQLName("settings") @GraphQLNonNull @GraphQLDescription("Connection + branding fields") final ForgeSettingsInput settings) {
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

                    setStringProp(site, "forgeSettingsUrl", settings.getUrl());
                    setStringProp(site, "forgeSettingsId", settings.getId());
                    setStringProp(site, "forgeSettingsUser", settings.getUser());

                    // Password handling preserves the previous Spring Web Flow behavior:
                    // store as base64 (obfuscation only — NOT encryption), blank input
                    // leaves the existing value alone so the UI can omit the field for
                    // already-configured sites without wiping it.
                    // SECURITY (SECURITY-571 review, accepted risk): the Nexus credential is
                    // stored reversibly. It is never returned to clients (GqlForgeSettings only
                    // exposes isPasswordSet) and never logged, and MavenProxy now restricts its
                    // use to callers who can read the site's module repository. A follow-up should
                    // move it to the platform credential store / a secret manager and exclude the
                    // forgeSettingsPassword property from cross-environment JCR exports.
                    final String password = settings.getPassword();
                    if (StringUtils.isNotBlank(password)) {
                        site.setProperty(
                                "forgeSettingsPassword",
                                Base64.encode(password.getBytes(StandardCharsets.UTF_8)));
                    }

                    // Branding / footer. Blank clears these (they are plain editable text,
                    // unlike the write-only password). The logo is a weakreference.
                    setLogoRef(session, site, settings.getLogo());
                    setStringProp(site, "forgeSettingsCopyright", settings.getCopyright());
                    setStringProp(site, "forgeSettingsPrivacyUrl", settings.getPrivacyUrl());
                    setStringProp(site, "forgeSettingsTermsUrl", settings.getTermsUrl());
                    setStringProp(site, "forgeSettingsCookiesUrl", settings.getCookiesUrl());
                    setStringProp(site, "forgeSettingsFacebookUrl", settings.getFacebookUrl());
                    setStringProp(site, "forgeSettingsLinkedinUrl", settings.getLinkedinUrl());
                    setStringProp(site, "forgeSettingsTwitterUrl", settings.getTwitterUrl());
                    setStringProp(site, "forgeSettingsYoutubeUrl", settings.getYoutubeUrl());

                    session.save();

                    return ForgeSettingsReader.read(site, siteKey);
                }
            });
        } catch (RepositoryException e) {
            // Surface as a structured error (mirroring CategorySettings/ManageRoles) instead of
            // swallowing to null — a null return is indistinguishable from "succeeded but empty"
            // and hides access-denied / site-not-found from the caller.
            LOGGER.error("Error updating forge settings for site {}", siteKey, e);
            throw new ForgeSettingsException("Forge settings update failed for site " + siteKey, e);
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
            if (site.hasProperty(FORGE_SETTINGS_LOGO)) {
                site.getProperty(FORGE_SETTINGS_LOGO).remove();
            }
            return;
        }
        final JCRNodeWrapper target;
        try {
            target = logo.startsWith("/")
                    ? session.getNode(logo)
                    : (JCRNodeWrapper) session.getNodeByIdentifier(logo);
        } catch (PathNotFoundException | ItemNotFoundException e) {
            // Unresolvable reference: leave any previously configured logo untouched rather than
            // aborting the whole mutation, matching this method's documented contract.
            LOGGER.warn("Ignoring unresolvable forge settings logo reference '{}'", logo);
            return;
        }
        site.setProperty(FORGE_SETTINGS_LOGO, target);
    }
}
