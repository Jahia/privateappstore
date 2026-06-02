/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2026 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.forge.proxy;

import java.io.IOException;
import java.net.URI;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.data.templates.ModuleReleaseInfo;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.sites.JahiaSitesService;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.jahia.services.notification.HttpClientService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HTTP servlet exposed on /modules/mavenproxy/{siteName}/** that streams artifacts from the
 * site's configured Maven repository, forwarding credentials from the site's forgeSettings.
 */
@Component(service = { HttpServlet.class, Servlet.class }, property = { "alias=/mavenproxy" })
public class MavenProxy extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenProxy.class);
    private static final String SITES_PATH = JahiaSitesService.SITES_JCR_PATH + FileSystem.SEPARATOR;
    private static final String MODULES_REPOSITORY = "/contents/modules-repository";

    private final HttpClientService httpClientService;

    @Activate
    public MavenProxy(@Reference HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession("live");

            String pathInfo = StringUtils.defaultString(request.getPathInfo());
            if (pathInfo.startsWith("/")) {
                pathInfo = pathInfo.substring(1);
            }
            String siteName = StringUtils.substringBefore(pathInfo, "/");
            String path = "/" + StringUtils.substringAfter(pathInfo, "/");

            // Reject path traversal and absolute-URL injection to prevent SSRF: the caller must
            // not be able to escape the configured repository root or point at another host. The
            // siteName segment is validated separately (it is concatenated into a JCR path).
            if (isSuspicious(path) || !isValidSiteName(siteName)) {
                LOGGER.warn("MavenProxy: rejected suspicious request site='{}' path='{}'", siteName, path);
                trySendError(response, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Authorization (SECURITY-571 B1): serve a site's modules only to a caller who can
            // READ that site's module repository in their OWN session. modules-repository is
            // jmix:accessControlled, so this single repository-level gate handles both cases:
            //   - PUBLIC store (world-readable repository): anonymous/guest visitors can download
            //     — the public-app-store use case. The site's STORED Maven credentials are used
            //     SERVER-SIDE only (to fetch from Nexus) and are NEVER returned to the client, so
            //     forwarding them on behalf of an anonymous request is safe.
            //   - PRIVATE store (restrictive inherited ACL): guest/foreign callers are blocked.
            // (A blanket "no anonymous" rule used to sit in front of this; it broke downloads on
            // public stores and was redundant with this gate, so it was removed.) The proxy
            // intentionally serves raw Maven coordinates (not only catalogued modules), so the
            // gate is repository-level, not artifact-level. Residual: a store whose catalogue is
            // PUBLIC but whose artifacts must stay private needs a restrictive ACL on its
            // modules-repository node (operational prerequisite).
            if (!callerCanAccessRepository(session, siteName)) {
                final String caller = (session.getUser() != null) ? session.getUser().getName() : "<none>";
                LOGGER.warn("MavenProxy: caller '{}' is not authorized for site '{}' module repository",
                        caller, siteName);
                trySendError(response, HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            ModuleReleaseInfo releaseInfo = getModuleReleaseInfo(session, siteName);

            String repositoryUrl = releaseInfo.getRepositoryUrl();
            if (StringUtils.isBlank(repositoryUrl)) {
                LOGGER.warn("MavenProxy: site '{}' has no configured repository URL", siteName);
                trySendError(response, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            // Normalize the root to a single trailing slash so the boundary check below cannot be
            // satisfied by a sibling-prefix host (e.g. root ".../foo" must not accept ".../foobar").
            final String repositoryRoot = StringUtils.removeEnd(repositoryUrl, "/") + "/";
            String url = StringUtils.removeEnd(repositoryUrl, "/") + path;
            // Canonicalize and confirm the resolved URL still lives under the repository root.
            final URI resolvedUri = UriComponentsBuilder.fromHttpUrl(url).build(false).toUri().normalize();
            if (!resolvedUri.toString().startsWith(repositoryRoot)) {
                LOGGER.warn("MavenProxy: resolved URL '{}' escapes repository root '{}'", resolvedUri, repositoryRoot);
                trySendError(response, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            streamArtifact(resolvedUri, releaseInfo, url, response);
        } catch (RepositoryException ex) {
            LOGGER.error("MavenProxy: repository access failed", ex);
            trySendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
            trySendError(response, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void streamArtifact(URI resolvedUri, ModuleReleaseInfo releaseInfo, String url, HttpServletResponse response) throws IOException {
        final CloseableHttpClient httpClient = httpClientService.getHttpClient(url);
        final HttpGet httpMethod = new HttpGet(resolvedUri);
        httpMethod.addHeader("Authorization", "Basic " + Base64.encode((releaseInfo.getUsername() + ":" + releaseInfo.getPassword()).getBytes()));
        try (final CloseableHttpResponse httpResponse = httpClient.execute(httpMethod)) {
            if (httpResponse.getCode() != HttpServletResponse.SC_OK) {
                trySendError(response, httpResponse.getCode());
                return;
            }
            response.setContentType(httpResponse.getEntity().getContentType());
            try {
                IOUtils.copy(httpResponse.getEntity().getContent(), response.getOutputStream());
            } catch (UnsupportedOperationException ex) {
                LOGGER.error("MavenProxy: response entity content not readable", ex);
                trySendError(response, HttpServletResponse.SC_BAD_GATEWAY);
            }
        }
    }

    private static void trySendError(HttpServletResponse response, int code) {
        try {
            response.sendError(code);
        } catch (IOException ex) {
            LOGGER.warn("MavenProxy: failed to send error {} to client", code, ex);
        }
    }

    private static boolean isSuspicious(String path) {
        // Block raw AND percent-encoded traversal / scheme-injection sequences. The proxy now
        // serves anonymous callers on public stores, so harden the SSRF guard against encoded
        // variants regardless of how the downstream URI builder normalizes them. Legitimate
        // Maven coordinate paths never contain these.
        final String p = path.toLowerCase();
        return p.contains("..") || p.contains("\\") || p.contains("://")
                || p.contains("\n") || p.contains("\r")
                || p.contains("%2e") || p.contains("%2f") || p.contains("%5c");
    }

    /** A site key is a simple identifier; reject anything that could alter the JCR path. */
    private static boolean isValidSiteName(String siteName) {
        return StringUtils.isNotBlank(siteName) && siteName.matches("[A-Za-z0-9._-]+");
    }

    /**
     * True when the caller's own session can read the site's (access-controlled) module
     * repository node — the prerequisite for using that site's stored Maven credentials.
     * {@code nodeExists} returns false both when the node is absent and when the caller
     * lacks read access, which is exactly the gate we want for a private site.
     */
    private static boolean callerCanAccessRepository(final JCRSessionWrapper session, final String siteName)
            throws RepositoryException {
        return session.nodeExists(SITES_PATH + siteName + MODULES_REPOSITORY);
    }

    private ModuleReleaseInfo getModuleReleaseInfo(final JCRSessionWrapper session, final String siteName) throws RepositoryException {
        JCRSiteNode siteNode = (JCRSiteNode) session.getNode(SITES_PATH + siteName);
        String forgeSettingsUrl = siteNode.getProperty("forgeSettingsUrl").getString();
        String user = siteNode.getProperty("forgeSettingsUser").getString();
        String password = new String(Base64.decode(siteNode.getProperty("forgeSettingsPassword").getString()));
        ModuleReleaseInfo info = new ModuleReleaseInfo();
        info.setRepositoryUrl(forgeSettingsUrl);
        info.setUsername(user);
        info.setPassword(password);
        return info;
    }
}
