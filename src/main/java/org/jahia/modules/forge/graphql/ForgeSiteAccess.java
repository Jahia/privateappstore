package org.jahia.modules.forge.graphql;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.sites.JahiaSitesService;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

/**
 * Shared site-scoped execution guard for the forge GraphQL mutation extensions. Validates the
 * siteKey, then runs {@code work} in a system session — but only after confirming the CALLER can
 * read the site node and holds {@code permission} on it (AccessDenied otherwise). Extracted so the
 * Category-settings and Manage-roles mutations share one copy of this authorization guard
 * (SECURITY-571) instead of each carrying an identical private {@code execute()}.
 */
final class ForgeSiteAccess {

    private static final String SITES_PATH = JahiaSitesService.SITES_JCR_PATH + FileSystem.SEPARATOR;

    private ForgeSiteAccess() {
        // Utility class — not instantiable.
    }

    static <T> T executeAsSystemForSite(String siteKey, String permission, String failureMessage,
                                        JCRCallback<T> work) {
        ForgeSettingsMutationExtension.validateSiteKey(siteKey);
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
                final String sitePath = SITES_PATH + siteKey;
                if (!session.nodeExists(sitePath)) {
                    throw new RepositoryException("Site not found: " + siteKey);
                }

                final JCRSessionWrapper callerSession = JCRSessionFactory.getInstance().getCurrentUserSession();
                if (!callerSession.nodeExists(sitePath)
                        || !callerSession.getNode(sitePath).hasPermission(permission)) {
                    throw new AccessDeniedException(permission);
                }

                return work.doInJCR(session);
            });
        } catch (RepositoryException e) {
            throw new ForgeSettingsException(failureMessage + siteKey, e);
        }
    }
}
