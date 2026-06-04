package org.jahia.modules.forge.settings;

/**
 * Reads and writes a site's forge settings (Nexus connection + storefront branding +
 * category root) in per-site OSGi configuration.
 *
 * <p>Storage was moved out of JCR ({@code jmix:forgeSettings} on the site node) because two
 * coexisting forge modules (e.g. {@code privateappstore} 4.3.0 and {@code jahia-store} 5.0.0)
 * declare that GLOBAL node type with different content, so the active definition is
 * non-deterministic and writing {@code forgeSettingsLogo} can throw
 * {@code ConstraintViolationException: Couldn't find definition for property}. OSGi config is
 * keyed by site and independent of node types, workspaces and publication.
 *
 * <p>Fetch from static / non-DI code (e.g. GraphQL type-extensions) with
 * {@code BundleUtils.getOsgiService(ForgeSettingsService.class, null)}; inject with
 * {@code @Reference} in OSGi components.
 */
public interface ForgeSettingsService {

    /** Current settings for the site; never null — {@link ForgeSettings#empty()} when unset. */
    ForgeSettings get(String siteKey);

    /**
     * Create or update the settings for a site. A blank {@link ForgeSettings#getPassword()}
     * leaves any previously stored password unchanged (so the UI can omit it); a non-blank
     * value replaces it. All other blank fields clear their stored value.
     */
    void save(String siteKey, ForgeSettings settings);

    /**
     * Remove a site's settings configuration. Called when a site is deleted so its config does
     * not outlive it (and a recreated same-key site does not inherit stale settings). No-op when
     * the site has no configuration.
     */
    void delete(String siteKey);
}
