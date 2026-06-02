package org.jahia.modules.forge.graphql;

import org.jahia.modules.forge.settings.ForgeSettings;

/**
 * Maps a {@link ForgeSettings} value object (loaded from per-site OSGi config) to the
 * {@link GqlForgeSettings} GraphQL type. Shared by the query (read) and the mutation
 * (re-read after save) so the field mapping lives in one place. The logo is stored as a
 * plain JCR path string; the {@code jahia-store-template} chrome resolves it to a
 * {@code /files/...} URL itself. The password is never exposed — only {@code passwordSet}.
 */
final class ForgeSettingsReader {

    private ForgeSettingsReader() {
    }

    static GqlForgeSettings from(ForgeSettings s, String siteKey) {
        return GqlForgeSettings.builder(siteKey)
                .url(s.getUrl())
                .id(s.getId())
                .user(s.getUser())
                .passwordSet(s.isPasswordSet())
                .logoPath(s.getLogoPath())
                .copyright(s.getCopyright())
                .privacyUrl(s.getPrivacyUrl())
                .termsUrl(s.getTermsUrl())
                .cookiesUrl(s.getCookiesUrl())
                .facebookUrl(s.getFacebookUrl())
                .linkedinUrl(s.getLinkedinUrl())
                .twitterUrl(s.getTwitterUrl())
                .youtubeUrl(s.getYoutubeUrl())
                .build();
    }
}
