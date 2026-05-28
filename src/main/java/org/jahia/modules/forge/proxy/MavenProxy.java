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
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.notification.HttpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

public class MavenProxy implements Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenProxy.class);

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (request.getMethod().equals("GET")) {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession("live");
            // Require an authenticated (non-guest) user: the proxy forwards the site's stored
            // Maven credentials, so it must never be reachable anonymously.
            if (session.getUser() == null || Constants.GUEST_USERNAME.equals(session.getUser().getName())) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return null;
            }

            String pathInfo = StringUtils.substringAfter(request.getPathInfo(), "/mavenproxy/");
            String siteName = StringUtils.substringBefore(pathInfo, "/");
            String path = "/" + StringUtils.substringAfter(pathInfo, "/");

            // Reject path traversal and absolute-URL injection to prevent SSRF: the caller must
            // not be able to escape the configured repository root or point at another host.
            if (path.contains("..") || path.contains("\\") || path.contains("://") || path.contains("\n") || path.contains("\r")) {
                LOGGER.warn("MavenProxy: rejected suspicious path '{}'", path);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }

            ModuleReleaseInfo releaseInfo = getModuleReleaseInfo(session, siteName);

            String repositoryUrl = releaseInfo.getRepositoryUrl();
            String url = repositoryUrl + path;
            // Canonicalize and confirm the resolved URL still lives under the repository root.
            final URI resolvedUri = UriComponentsBuilder.fromHttpUrl(url).build(false).toUri().normalize();
            if (repositoryUrl == null || !resolvedUri.toString().startsWith(repositoryUrl)) {
                LOGGER.warn("MavenProxy: resolved URL '{}' escapes repository root '{}'", resolvedUri, repositoryUrl);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }
            // Usage of SpringContextSingleton to prevent bug QA-9515
            final CloseableHttpClient httpClient = ((HttpClientService) SpringContextSingleton.getBean("HttpClientService")).getHttpClient(url);
            final HttpGet httpMethod = new HttpGet(resolvedUri);
            httpMethod.addHeader("Authorization", "Basic " + Base64.encode((releaseInfo.getUsername() + ":" + releaseInfo.getPassword()).getBytes()));
            try (final CloseableHttpResponse httpResponse = httpClient.execute(httpMethod)) {
                if (httpResponse.getCode() == HttpServletResponse.SC_OK) {
                    response.setContentType(httpResponse.getEntity().getContentType());
                    IOUtils.copy(httpResponse.getEntity().getContent(), response.getOutputStream());
                } else {
                    response.sendError(httpResponse.getCode());
                }
            } catch (IOException ex) {
                LOGGER.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
        return null;
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
