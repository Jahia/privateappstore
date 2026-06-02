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
 *      i18n descriptions/FAQ/howToInstall/license)
 *   3. jmix:forgeSettings on the site node  (Nexus URL/id/user/password, copyright,
 *      footer URLs, rootCategory, logo)
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
import org.jahia.api.Constants
import javax.jcr.RepositoryException

// ============================ CONFIG ============================
def SOURCE_SITE_KEY   = 'old-store'      // store running privateappstore 4.3.0
def TARGET_SITE_KEY   = 'new-store'      // store running jahia-store 5.0.0
def DRY_RUN           = true             // true = preview only, no writes
def REPLACE_EXISTING  = false            // true = overwrite modules that already exist by name on the target
def MIGRATE_SETTINGS  = true             // copy jmix:forgeSettings (logo/footer/Nexus) onto the target site
def PUBLISH_TO_LIVE    = false           // publish the target contents subtree to LIVE after migration
def LANGUAGES         = null             // null = all site languages; or e.g. ['en','fr']
// ================================================================

def REPO              = '/contents/modules-repository'
def REQUIRED_VERSIONS = '/contents/modules-required-versions'
def VERSION_TYPES     = ['jnt:forgeModuleVersion', 'jnt:forgePackageVersion'] as Set

def report = new StringBuilder()
def stats  = [modulesCopied: 0, modulesSkipped: 0, versionsRemapped: 0, urlsDropped: 0, reqVersionsCopied: 0, warnings: 0]
def log    = { String msg -> report.append(msg).append('\n'); println msg }
def warn   = { String msg -> stats.warnings++; log("  ! WARN: ${msg}") }

JCRTemplate.getInstance().doExecuteWithSystemSession(null, Constants.EDIT_WORKSPACE, { JCRSessionWrapper session ->

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

    // ---- validate -------------------------------------------------------
    def srcPath = sitePath(SOURCE_SITE_KEY)
    def tgtPath = sitePath(TARGET_SITE_KEY)
    if (!session.nodeExists(srcPath)) throw new RepositoryException("Source site not found: ${srcPath}")
    if (!session.nodeExists(tgtPath)) throw new RepositoryException("Target site not found: ${tgtPath}")
    if (SOURCE_SITE_KEY == TARGET_SITE_KEY) throw new RepositoryException("Source and target site keys are identical")
    if (!session.nodeExists(srcPath + REPO)) throw new RepositoryException("Source has no modules-repository: ${srcPath + REPO}")

    JCRNodeWrapper srcSite = session.getNode(srcPath)
    JCRNodeWrapper tgtSite = session.getNode(tgtPath)
    JCRNodeWrapper tgtContents = ensureChild(tgtSite, 'contents', 'jnt:contentFolder')

    log("=== Store content migration: ${SOURCE_SITE_KEY} (privateappstore 4.3.0) -> ${TARGET_SITE_KEY} (jahia-store 5.0.0) ===")
    log("Mode: ${DRY_RUN ? 'DRY-RUN (no writes)' : 'APPLY'} | replaceExisting=${REPLACE_EXISTING} | migrateSettings=${MIGRATE_SETTINGS} | publish=${PUBLISH_TO_LIVE}")

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

    // ---- 2) modules-repository -----------------------------------------
    log("\n[2] modules-repository")
    JCRNodeWrapper srcRepo = session.getNode(srcPath + REPO)
    JCRNodeWrapper tgtRepo = ensureChild(tgtContents, 'modules-repository', 'jnt:contentFolder')
    srcRepo.getNodes().each { JCRNodeWrapper child ->
        def name = child.getName()
        if (tgtRepo.hasNode(name)) {
            if (REPLACE_EXISTING) {
                log("  ~ replace: ${name} (${child.getPrimaryNodeTypeName()})")
                if (!DRY_RUN) { tgtRepo.getNode(name).remove(); child.copy(tgtRepo, name, false) }
                stats.modulesCopied++
            } else {
                log("  = skip (exists): ${name}")
                stats.modulesSkipped++
            }
        } else {
            log("  + copy:   ${name} (${child.getPrimaryNodeTypeName()})")
            if (!DRY_RUN) child.copy(tgtRepo, name, false)
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

    // ---- 4) jmix:forgeSettings on the site node ------------------------
    if (MIGRATE_SETTINGS) {
        log("\n[4] forgeSettings (site mixin)")
        if (!srcSite.isNodeType('jmix:forgeSettings')) {
            log("  source site has no forgeSettings; nothing to copy")
        } else {
            if (!DRY_RUN && !tgtSite.isNodeType('jmix:forgeSettings')) tgtSite.addMixin('jmix:forgeSettings')

            def stringProps = ['forgeSettingsUrl','forgeSettingsId','forgeSettingsUser','forgeSettingsPassword',
                               'forgeSettingsCopyright','forgeSettingsPrivacyUrl','forgeSettingsTermsUrl',
                               'forgeSettingsCookiesUrl','forgeSettingsFacebookUrl','forgeSettingsLinkedinUrl',
                               'forgeSettingsTwitterUrl','forgeSettingsYoutubeUrl']
            stringProps.each { p ->
                if (srcSite.hasProperty(p)) {
                    def v = srcSite.getProperty(p).getString()
                    log("  - ${p} = ${(p == 'forgeSettingsPassword') ? '********' : v}")
                    if (!DRY_RUN) tgtSite.setProperty(p, v)
                }
            }

            // rootCategory: global category UUID -> valid on the same server. Carry over.
            if (srcSite.hasProperty('rootCategory')) {
                try {
                    def cat = srcSite.getProperty('rootCategory').getNode()
                    log("  - rootCategory -> ${cat.getPath()}")
                    if (!DRY_RUN) tgtSite.setProperty('rootCategory', cat)
                } catch (RepositoryException e) { warn("source rootCategory is dangling; skipped") }
            }

            // forgeSettingsLogo: copy the file when it lives inside the source site,
            // else keep the (shared) reference.
            if (srcSite.hasProperty('forgeSettingsLogo')) {
                try {
                    def logo = srcSite.getProperty('forgeSettingsLogo').getNode()
                    if (logo.getPath().startsWith(srcSite.getPath() + '/')) {
                        def relLogo = logo.getPath().substring((srcSite.getPath() + '/').length())   // e.g. files/store/logo.png
                        def tgtLogoPath = tgtSite.getPath() + '/' + relLogo
                        log("  - logo (site-local): ${logo.getPath()} -> ${tgtLogoPath}")
                        if (!DRY_RUN) {
                            JCRNodeWrapper parent = tgtSite
                            def segs = relLogo.split('/')
                            for (int i = 0; i < segs.length - 1; i++) {
                                parent = parent.hasNode(segs[i]) ? parent.getNode(segs[i]) : parent.addNode(segs[i], 'jnt:folder')
                            }
                            if (!session.nodeExists(tgtLogoPath)) logo.copy(parent, segs[-1], false)
                            tgtSite.setProperty('forgeSettingsLogo', session.getNode(tgtLogoPath))
                        }
                    } else {
                        log("  - logo (shared): ${logo.getPath()} -> reference kept")
                        if (!DRY_RUN) tgtSite.setProperty('forgeSettingsLogo', logo)
                    }
                } catch (RepositoryException e) { warn("source forgeSettingsLogo is dangling; skipped") }
            }
        }
    }

    // ---- 5) save & publish ---------------------------------------------
    if (DRY_RUN) {
        log("\n[5] DRY-RUN: discarding session (no changes persisted)")
    } else {
        session.save()
        log("\n[5] saved EDIT workspace")
        if (PUBLISH_TO_LIVE) {
            def pub = JCRPublicationService.getInstance()
            def langs = (LANGUAGES != null) ? new HashSet(LANGUAGES) : null
            pub.publishByMainId(tgtContents.getIdentifier(), Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE, langs, true, null)
            pub.publishByMainId(tgtSite.getIdentifier(), Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE, langs, false, null)
            log("    published target contents + site node to LIVE")
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
