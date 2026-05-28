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
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.api.Constants;
import org.jahia.data.templates.ModuleReleaseInfo;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;

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

    private HttpClientService httpClientService;

    @Reference
    public void setHttpClientService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession("live");
            // Require an authenticated (non-guest) user: the proxy forwards the site's stored
            // Maven credentials, so it must never be reachable anonymously.
            if (session.getUser() == null || Constants.GUEST_USERNAME.equals(session.getUser().getName())) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String pathInfo = StringUtils.defaultString(request.getPathInfo());
            if (pathInfo.startsWith("/")) {
                pathInfo = pathInfo.substring(1);
            }
            String siteName = StringUtils.substringBefore(pathInfo, "/");
            String path = "/" + StringUtils.substringAfter(pathInfo, "/");

            // Reject path traversal and absolute-URL injection to prevent SSRF: the caller must
            // not be able to escape the configured repository root or point at another host.
            if (path.contains("..") || path.contains("\\") || path.contains("://") || path.contains("\n") || path.contains("\r")) {
                LOGGER.warn("MavenProxy: rejected suspicious path '{}'", path);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            ModuleReleaseInfo releaseInfo = getModuleReleaseInfo(session, siteName);

            String repositoryUrl = releaseInfo.getRepositoryUrl();
            String url = repositoryUrl + path;
            // Canonicalize and confirm the resolved URL still lives under the repository root.
            final URI resolvedUri = UriComponentsBuilder.fromHttpUrl(url).build(false).toUri().normalize();
            if (repositoryUrl == null || !resolvedUri.toString().startsWith(repositoryUrl)) {
                LOGGER.warn("MavenProxy: resolved URL '{}' escapes repository root '{}'", resolvedUri, repositoryUrl);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            final CloseableHttpClient httpClient = httpClientService.getHttpClient(url);
            final HttpGet httpMethod = new HttpGet(resolvedUri);
            httpMethod.addHeader("Authorization", "Basic " + Base64.encode((releaseInfo.getUsername() + ":" + releaseInfo.getPassword()).getBytes()));
            try (final CloseableHttpResponse httpResponse = httpClient.execute(httpMethod)) {
                if (httpResponse.getCode() == HttpServletResponse.SC_OK) {
                    response.setContentType(httpResponse.getEntity().getContentType());
                    IOUtils.copy(httpResponse.getEntity().getContent(), response.getOutputStream());
                } else {
                    response.sendError(httpResponse.getCode());
                }
            }
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (RepositoryException ex) {
            LOGGER.error("MavenProxy: repository access failed", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private ModuleReleaseInfo getModuleReleaseInfo(final JCRSessionWrapper session, final String siteName) throws RepositoryException {
        JCRSiteNode siteNode = (JCRSiteNode) session.getNode("/sites/" + siteName);
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
