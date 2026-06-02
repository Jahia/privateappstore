package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.jahia.modules.forge.settings.ForgeSettings;
import org.jahia.modules.forge.settings.ForgeSettingsService;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.sites.JahiaSitesService;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import java.util.regex.Pattern;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLName("ForgeSettingsMutations")
@GraphQLDescription("Private App Store forge settings mutations")
public final class ForgeSettingsMutationExtension {

    private static final String PERMISSION = "siteAdminForgeSettings";
    private static final String SITES_PATH = JahiaSitesService.SITES_JCR_PATH + FileSystem.SEPARATOR;
    /** A siteKey is concatenated into a JCR site path; only a simple identifier is allowed. */
    private static final Pattern SAFE_SITE_KEY = Pattern.compile("[A-Za-z0-9._-]+");

    private ForgeSettingsMutationExtension() {
    }

    @GraphQLField
    @GraphQLName("updateForgeSettings")
    @GraphQLDescription("Create or update the forge settings for a site")
    public static GqlForgeSettings updateForgeSettings(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey,
            @GraphQLName("settings") @GraphQLNonNull @GraphQLDescription("Connection + branding fields") final ForgeSettingsInput settings) {
        validateSiteKey(siteKey);
        final String sitePath = SITES_PATH + siteKey;
        try {
            assertCanManage(sitePath);
        } catch (RepositoryException e) {
            // Surface as a structured error (mirroring CategorySettings/ManageRoles) instead of
            // leaking JCR types; the cause is preserved so the GraphQL layer logs it once.
            throw new ForgeSettingsException("Forge settings update failed for site " + siteKey, e);
        }

        final ForgeSettingsService service = service();
        // Read-modify-write: this mutation owns connection + branding only; rootCategoryUuid
        // (written by CategorySettings) is preserved by starting from the current settings.
        // A blank password is preserved by the service; other blank fields clear their value.
        final ForgeSettings merged = service.get(siteKey).toBuilder()
                .url(settings.getUrl())
                .id(settings.getId())
                .user(settings.getUser())
                .password(settings.getPassword())
                .logoPath(settings.getLogo())
                .copyright(settings.getCopyright())
                .privacyUrl(settings.getPrivacyUrl())
                .termsUrl(settings.getTermsUrl())
                .cookiesUrl(settings.getCookiesUrl())
                .facebookUrl(settings.getFacebookUrl())
                .linkedinUrl(settings.getLinkedinUrl())
                .twitterUrl(settings.getTwitterUrl())
                .youtubeUrl(settings.getYoutubeUrl())
                .build();
        service.save(siteKey, merged);

        // The jahia-store-template chrome renders the logo, copyright and footer links via SSR
        // and Jahia caches that HTML. Flush the site's output cache (whole subtree) so the change
        // is visible immediately instead of serving stale chrome until the cache expires.
        CacheHelper.flushOutputCachesForPath(sitePath, true);

        return ForgeSettingsReader.from(service.get(siteKey), siteKey);
    }

    /** The caller must be able to read the site and hold the forge-settings admin permission. */
    private static void assertCanManage(String sitePath) throws RepositoryException {
        final JCRSessionWrapper caller = JCRSessionFactory.getInstance().getCurrentUserSession();
        if (!caller.nodeExists(sitePath) || !caller.getNode(sitePath).hasPermission(PERMISSION)) {
            throw new AccessDeniedException(PERMISSION);
        }
    }

    static ForgeSettingsService service() {
        final ForgeSettingsService service = BundleUtils.getOsgiService(ForgeSettingsService.class, null);
        if (service == null) {
            throw new ForgeSettingsException("ForgeSettingsService is not available", null);
        }
        return service;
    }

    /**
     * Reject a siteKey that is not a simple identifier before it is concatenated into a JCR
     * site path, so the permission gate cannot be sidestepped with a path-traversal value
     * (e.g. "../.." resolving to "/"). Mirrors {@code MavenProxy.isValidSiteName} (SECURITY-571).
     */
    static void validateSiteKey(String siteKey) {
        if (siteKey == null || !SAFE_SITE_KEY.matcher(siteKey).matches()) {
            throw new ForgeSettingsException("Invalid site key: " + siteKey, null);
        }
    }
}
