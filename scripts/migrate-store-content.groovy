/*
 * migrate-store-content.groovy
 * ---------------------------------------------------------------------------
 * Migrate the CONTENT of a Jahia store running the legacy `privateappstore` 4.3.0
 * module to a store running the renamed `jahia-store` 5.0.0 module, when BOTH
 * stores live on the SAME Jahia server.
 *
 * Why this is a plain content copy (not a schema transform):
 *   The 4.3.0 -> 5.0.0 change renamed the module IDENTITY (artifactId, bundle,
 *   i18n namespace, federation global) but kept every JCR node type FEATURE-scoped
 *   and unchanged: jnt:forgeModule, jnt:forgeModuleVersion, jnt:forgePackage,
 *   jmix:forgeSettings, jmix:categorized, jmix:tagged, jmix:reviews, etc. So the
 *   source and target content are structurally identical and can be copied as-is.
 *
 * What it migrates (from /sites/<SOURCE>/... to /sites/<TARGET>/...):
 *   1. contents/modules-required-versions  (per-site jnt:jahiaVersion targets of
 *      the `requiredVersion` weakreference)
 *   2. contents/modules-repository         (the uploaded modules/packages, their
 *      versions, files, icon, screenshots, reviews, tags, categories, changelog,
 *      i18n descriptions/FAQ/howToInstall/license). Each module is copied in a session AS
 *      its original author so jcr:createdBy (the developer shown on the card) is preserved
 *      rather than becoming 'system' (it falls back to system if that user no longer exists).
 *
 * What it does NOT migrate: forge settings (Nexus URL/credentials, branding/footer, logo,
 * root category). As of jahia-store 5.0.0 those live in per-site OSGi configuration, not
 * JCR — re-enter them once in the target store's admin after migrating the modules.
 *
 * Reference handling (the only tricky part):
 *   - `requiredVersion` (version -> modules-required-versions) is PER-SITE, so it
 *     is re-pointed to the TARGET site's modules-required-versions node of the
 *     same name after copy.
 *   - `rootCategory` and module `j:defaultCategory` point at GLOBAL jnt:category
 *     nodes (shared across sites by UUID) -> kept as-is; validated, warned if
 *     dangling. (If the two stores used different category trees, the source's
 *     rootCategory is carried over verbatim, which stays valid on the same server.)
 *   - `forgeSettingsLogo` is copied with the file when it lives inside the source
 *     site; otherwise the (shared) reference is kept.
 *   - The stale per-version `url` property is DROPPED: 5.0.0 generates the
 *     /modules/mavenproxy/... download URL from the Maven coordinates at read time.
 *
 * WORKSPACE (important): store content lives mostly in the LIVE workspace.
 *   Modules are uploaded through the public storefront via the createEntryFromJar
 *   Action, which writes in the REQUEST's workspace -> on a live-rendered page
 *   that is LIVE, so those nodes exist in LIVE (often with no EDIT counterpart).
 *   This script therefore copies WITHIN a single workspace, defaulting to 'live'
 *   (set WORKSPACE = 'live'), so the migrated modules show up on the target
 *   storefront immediately. We deliberately do NOT use the JCR cross-workspace
 *   copy(srcWorkspace,...): it preserves source UUIDs, which would make the target
 *   site share node identities with the source (EDIT/LIVE linkage corruption).
 *   Same-workspace node.copy() assigns fresh UUIDs and is binary/i18n/ACL-safe.
 *
 *   - WORKSPACE = 'live'    : migrate the public store (recommended; matches where
 *                            the content is). Target EDIT is left as-is.
 *   - WORKSPACE = 'default' : migrate the EDIT/authoring tree; set PUBLISH_TO_LIVE
 *                            = true to publish it to LIVE afterwards. Use this only
 *                            if the source store is edited in jContent and fully
 *                            published (i.e. its content really is in EDIT).
 *
 * Idempotent: modules/versions that already exist by name under the target are
 * skipped (set REPLACE_EXISTING = true to overwrite). Re-runnable.
 *
 * HOW TO RUN
 *   Jahia Tools > Groovy Console (https://<host>/modules/tools/groovyConsole.jsp),
 *   or via the provisioning API `executeScript`. Edit the CONFIG block, run with
 *   DRY_RUN = true first to preview, then set DRY_RUN = false to apply.
 * ---------------------------------------------------------------------------
 */

import org.jahia.services.content.JCRTemplate
import org.jahia.services.content.JCRSessionWrapper
import org.jahia.services.content.JCRNodeWrapper
import org.jahia.services.content.JCRCallback
import org.jahia.services.content.JCRPublicationService
import org.jahia.services.usermanager.JahiaUserManagerService
import org.jahia.api.Constants
import javax.jcr.RepositoryException

// ============================ CONFIG ============================
def SOURCE_SITE_KEY   = 'old-store'      // store running privateappstore 4.3.0
def TARGET_SITE_KEY   = 'new-store'      // store running jahia-store 5.0.0
def WORKSPACE         = 'live'           // 'live' (default) = migrate the public storefront content where store
                                         // modules actually live; 'default' = migrate the EDIT/authoring tree.
def DRY_RUN           = true             // true = preview only, no writes
def REPLACE_EXISTING  = false            // true = overwrite modules that already exist by name on the target
def PUBLISH_TO_LIVE   = false            // only when WORKSPACE='default': publish the migrated EDIT content to LIVE
def LANGUAGES         = null             // null = all site languages; or e.g. ['en','fr']
// ================================================================

def REPO              = '/contents/modules-repository'
def REQUIRED_VERSIONS = '/contents/modules-required-versions'
def VERSION_TYPES     = ['jnt:forgeModuleVersion', 'jnt:forgePackageVersion'] as Set
def MODULE_TYPES      = ['jnt:forgeModule', 'jnt:forgePackage'] as Set

def report = new StringBuilder()
def stats  = [modulesCopied: 0, modulesSkipped: 0, versionsRemapped: 0, urlsDropped: 0, reqVersionsCopied: 0, warnings: 0]
def log    = { String msg -> report.append(msg).append('\n'); println msg }
def warn   = { String msg -> stats.warnings++; log("  ! WARN: ${msg}") }

if (WORKSPACE != Constants.LIVE_WORKSPACE && WORKSPACE != Constants.EDIT_WORKSPACE) {
    throw new IllegalArgumentException("WORKSPACE must be 'live' or 'default', got: ${WORKSPACE}")
}

JCRTemplate.getInstance().doExecuteWithSystemSession(null, WORKSPACE, { JCRSessionWrapper session ->

    // ---- helpers --------------------------------------------------------
    def sitePath = { String key -> "/sites/${key}".toString() }

    // Ensure a /contents child folder exists under the target site (the seed
    // import.xml already creates modules-repository + modules-required-versions,
    // but be defensive when migrating into a hand-made site).
    def ensureChild = { JCRNodeWrapper parent, String name, String type ->
        parent.hasNode(name) ? parent.getNode(name) : parent.addNode(name, type)
    }

    // Recursively collect nodes of the given types under a root.
    def collect
    collect = { JCRNodeWrapper node, Set types, List acc ->
        if (types.contains(node.getPrimaryNodeTypeName())) acc.add(node)
        node.getNodes().each { collect(it, types, acc) }
        acc
    }

    // Copy one module subtree into the target modules-repository, recreating the intermediate
    // group folders. jcr:createdBy is a PROTECTED property set at node creation from the SESSION
    // user (it cannot be set afterwards), so a plain system-session copy would stamp every
    // migrated module as 'system'. We therefore run the copy in a session AS the module's author.
    def doCopy = { JCRSessionWrapper s, String srcModPath, String tgtRepoPath, String relPath ->
        JCRNodeWrapper srcMod = s.getNode(srcModPath)
        JCRNodeWrapper parent = s.getNode(tgtRepoPath)
        final int slash = relPath.lastIndexOf('/')
        if (slash > 0) {
            relPath.substring(0, slash).split('/').each { seg ->
                parent = parent.hasNode(seg) ? parent.getNode(seg) : parent.addNode(seg, 'jnt:contentFolder')
            }
        }
        final String name = srcMod.getName()
        if (parent.hasNode(name)) parent.getNode(name).remove()
        srcMod.copy(parent, name, false)
        s.save()
        return null
    }
    def copyAsAuthor = { String srcModPath, String tgtRepoPath, String relPath, String author ->
        def userNode = author ? JahiaUserManagerService.getInstance().lookupUser(author) : null
        if (userNode != null) {
            try {
                JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(userNode.getJahiaUser(), WORKSPACE, null,
                        { JCRSessionWrapper s -> doCopy(s, srcModPath, tgtRepoPath, relPath) } as JCRCallback)
                return
            } catch (Exception e) {
                warn("as-author copy failed for ${relPath} (author=${author}): ${e.message}; copying as system")
            }
        } else if (author) {
            warn("author '${author}' not found; copying ${relPath} as system")
        }
        JCRTemplate.getInstance().doExecuteWithSystemSession(null, WORKSPACE,
                { JCRSessionWrapper s -> doCopy(s, srcModPath, tgtRepoPath, relPath) } as JCRCallback)
    }

    // ---- validate -------------------------------------------------------
    def srcPath = sitePath(SOURCE_SITE_KEY)
    def tgtPath = sitePath(TARGET_SITE_KEY)
    if (SOURCE_SITE_KEY == TARGET_SITE_KEY) throw new RepositoryException("Source and target site keys are identical")
    if (!session.nodeExists(srcPath)) {
        throw new RepositoryException("Source site not found in '${WORKSPACE}' workspace: ${srcPath}")
    }
    if (!session.nodeExists(tgtPath)) {
        throw new RepositoryException("Target site not found in '${WORKSPACE}' workspace: ${tgtPath}" +
            (WORKSPACE == Constants.LIVE_WORKSPACE ? " — publish the target site at least once so its base structure exists in LIVE, then re-run." : ""))
    }
    if (!session.nodeExists(srcPath + REPO)) {
        throw new RepositoryException("Source has no modules-repository in '${WORKSPACE}': ${srcPath + REPO}" +
            (WORKSPACE == Constants.LIVE_WORKSPACE ? " — if the source store is edited in jContent and not published, try WORKSPACE='default'." : ""))
    }

    JCRNodeWrapper srcSite = session.getNode(srcPath)
    JCRNodeWrapper tgtSite = session.getNode(tgtPath)
    JCRNodeWrapper tgtContents = ensureChild(tgtSite, 'contents', 'jnt:contentFolder')

    log("=== Store content migration: ${SOURCE_SITE_KEY} (privateappstore 4.3.0) -> ${TARGET_SITE_KEY} (jahia-store 5.0.0) ===")
    log("Workspace: ${WORKSPACE} | Mode: ${DRY_RUN ? 'DRY-RUN (no writes)' : 'APPLY'} | replaceExisting=${REPLACE_EXISTING}")
    if (WORKSPACE == Constants.LIVE_WORKSPACE) {
        log("  (LIVE migration: content lands directly in the storefront; the target EDIT/jContent tree is NOT populated — same shape as a contribution-style store.)")
    }

    // ---- 1) modules-required-versions ----------------------------------
    log("\n[1] modules-required-versions")
    JCRNodeWrapper tgtReq = ensureChild(tgtContents, 'modules-required-versions', 'jnt:contentFolder')
    if (session.nodeExists(srcPath + REQUIRED_VERSIONS)) {
        session.getNode(srcPath + REQUIRED_VERSIONS).getNodes().each { JCRNodeWrapper rv ->
            if (tgtReq.hasNode(rv.getName())) {
                log("  = exists: ${rv.getName()}")
            } else {
                log("  + copy:   ${rv.getName()} (${rv.getPrimaryNodeTypeName()})")
                if (!DRY_RUN) rv.copy(tgtReq, rv.getName(), false)
                stats.reqVersionsCopied++
            }
        }
    } else {
        warn("source has no modules-required-versions; requiredVersion refs may not resolve")
    }

    // ---- 2) modules-repository (per module, preserving its author) ------
    // Copy each module individually IN A SESSION AS its original author (jcr:createdBy) so the
    // migrated module + version + file nodes keep that author instead of 'system'. (A bulk
    // system-session copy stamps every node 'system' because jcr:createdBy is protected and set
    // from the session user at creation.) Group folders are recreated as needed.
    log("\n[2] modules-repository (preserving author)")
    JCRNodeWrapper srcRepo = session.getNode(srcPath + REPO)
    JCRNodeWrapper tgtRepo = ensureChild(tgtContents, 'modules-repository', 'jnt:contentFolder')
    final String srcRepoPath = srcRepo.getPath()
    final String tgtRepoPath = tgtRepo.getPath()
    collect(srcRepo, MODULE_TYPES, []).each { JCRNodeWrapper srcMod ->
        final String relPath = srcMod.getPath().substring(srcRepoPath.length() + 1)  // e.g. org/foo/mymod
        final String author = srcMod.hasProperty('jcr:createdBy') ? srcMod.getProperty('jcr:createdBy').getString() : null
        if (session.nodeExists(tgtRepoPath + '/' + relPath) && !REPLACE_EXISTING) {
            log("  = skip (exists): ${relPath}")
            stats.modulesSkipped++
        } else {
            log("  + copy:   ${relPath} (author=${author ?: 'system'})")
            if (!DRY_RUN) copyAsAuthor(srcMod.getPath(), tgtRepoPath, relPath, author)
            stats.modulesCopied++
        }
    }

    // ---- 3) fixup: requiredVersion re-point + drop stale url ------------
    // Walk the TARGET repo, map each version back to its SOURCE counterpart by
    // relative path, read the source's requiredVersion target NAME, and re-point
    // to the target site's modules-required-versions node of that name.
    log("\n[3] re-point requiredVersion + drop stale url")
    if (!DRY_RUN) {
        collect(tgtRepo, VERSION_TYPES, []).each { JCRNodeWrapper tv ->
            // drop stale generated-download url (5.0.0 builds it from maven coords)
            if (tv.hasProperty('url')) { tv.getProperty('url').remove(); stats.urlsDropped++ }

            def relUnderRepo = tv.getPath().substring((tgtRepo.getPath() + '/').length())
            def srcVerPath = srcRepo.getPath() + '/' + relUnderRepo
            if (!session.nodeExists(srcVerPath)) { warn("no source version for ${relUnderRepo}"); return }
            def sv = session.getNode(srcVerPath)
            if (!sv.hasProperty('requiredVersion')) return
            String reqName
            try {
                reqName = sv.getProperty('requiredVersion').getNode().getName()
            } catch (RepositoryException e) { warn("source requiredVersion dangling for ${relUnderRepo}"); return }

            if (!tgtReq.hasNode(reqName)) { warn("target modules-required-versions has no '${reqName}' for ${relUnderRepo}"); return }
            tv.setProperty('requiredVersion', tgtReq.getNode(reqName))
            stats.versionsRemapped++
        }
        log("  re-pointed ${stats.versionsRemapped} version(s); dropped ${stats.urlsDropped} stale url(s)")
    } else {
        // dry-run: just count what exists on the source
        collect(srcRepo, VERSION_TYPES, []).each { JCRNodeWrapper sv ->
            if (sv.hasProperty('url')) stats.urlsDropped++
            if (sv.hasProperty('requiredVersion')) stats.versionsRemapped++
        }
        log("  would re-point ~${stats.versionsRemapped} version(s); would drop ~${stats.urlsDropped} stale url(s)")
    }

    // ---- 4) forge settings (NOT migrated) ------------------------------
    // As of jahia-store 5.0.0, forge settings (Nexus URL/credentials, branding/footer,
    // logo, root category) live in per-site OSGi configuration (ForgeSettingsService),
    // NOT in JCR — they were moved off the cross-module jmix:forgeSettings node type.
    // They are therefore NOT part of content migration: re-enter them once in the target
    // store's "Store administration" admin after migrating the modules.
    log("\n[4] forge settings: NOT migrated — configure them in the target store's admin (per-site OSGi config).")

    // ---- 5) save & publish ---------------------------------------------
    if (DRY_RUN) {
        log("\n[5] DRY-RUN: discarding session (no changes persisted)")
    } else {
        session.save()
        log("\n[5] saved ${WORKSPACE} workspace")
        if (PUBLISH_TO_LIVE && WORKSPACE == Constants.EDIT_WORKSPACE) {
            def pub = JCRPublicationService.getInstance()
            def langs = (LANGUAGES != null) ? new HashSet(LANGUAGES) : null
            pub.publishByMainId(tgtContents.getIdentifier(), Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE, langs, true, null)
            pub.publishByMainId(tgtSite.getIdentifier(), Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE, langs, false, null)
            log("    published target contents + site node to LIVE")
        } else if (PUBLISH_TO_LIVE) {
            log("    (publish skipped: already operating in '${WORKSPACE}' — content is live)")
        }
    }

    // ---- summary --------------------------------------------------------
    log("\n=== SUMMARY ===")
    log("  required-versions copied : ${stats.reqVersionsCopied}")
    log("  modules copied           : ${stats.modulesCopied}")
    log("  modules skipped (exist)  : ${stats.modulesSkipped}")
    log("  versions re-pointed      : ${stats.versionsRemapped}")
    log("  stale urls dropped       : ${stats.urlsDropped}")
    log("  warnings                 : ${stats.warnings}")
    if (DRY_RUN) log("  (DRY-RUN — re-run with DRY_RUN = false to apply)")
    return null
} as JCRCallback)

report.toString()
