package org.jahia.modules.forge.graphql;

import org.jahia.services.content.JCRNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * Builds a {@link GqlForgeSettings} from a site node's {@code jmix:forgeSettings}
 * properties. Shared by the query (read) and the mutation (re-read after save) so
 * the property/field mapping lives in one place. The logo is stored as a
 * weakreference; we expose its resolved JCR path (the jahia-store-template chrome
 * resolves the reference itself to render the image).
 */
final class ForgeSettingsReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeSettingsReader.class);

    private ForgeSettingsReader() {
    }

    static GqlForgeSettings read(JCRNodeWrapper site, String siteKey) throws RepositoryException {
        return GqlForgeSettings.builder(siteKey)
                .url(getStringProp(site, "forgeSettingsUrl"))
                .id(getStringProp(site, "forgeSettingsId"))
                .user(getStringProp(site, "forgeSettingsUser"))
                .passwordSet(site.hasProperty("forgeSettingsPassword"))
                .logoPath(getLogoPath(site))
                .copyright(getStringProp(site, "forgeSettingsCopyright"))
                .privacyUrl(getStringProp(site, "forgeSettingsPrivacyUrl"))
                .termsUrl(getStringProp(site, "forgeSettingsTermsUrl"))
                .cookiesUrl(getStringProp(site, "forgeSettingsCookiesUrl"))
                .facebookUrl(getStringProp(site, "forgeSettingsFacebookUrl"))
                .linkedinUrl(getStringProp(site, "forgeSettingsLinkedinUrl"))
                .twitterUrl(getStringProp(site, "forgeSettingsTwitterUrl"))
                .youtubeUrl(getStringProp(site, "forgeSettingsYoutubeUrl"))
                .build();
    }

    private static String getStringProp(JCRNodeWrapper node, String name) throws RepositoryException {
        return node.hasProperty(name) ? node.getProperty(name).getString() : null;
    }

    /** Resolve the logo weakreference to a JCR path; tolerate a dangling reference. */
    private static String getLogoPath(JCRNodeWrapper site) {
        try {
            if (site.hasProperty("forgeSettingsLogo")) {
                return site.getProperty("forgeSettingsLogo").getNode().getPath();
            }
        } catch (RepositoryException e) {
            LOGGER.debug("Dangling forgeSettingsLogo reference on {}", site, e);
        }
        return null;
    }
}
