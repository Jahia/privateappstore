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
import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
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
            String pathInfo = StringUtils.substringAfter(request.getPathInfo(), "/mavenproxy/");
            String siteName = StringUtils.substringBefore(pathInfo, "/");
            String path = "/" + StringUtils.substringAfter(pathInfo, "/");

            ModuleReleaseInfo releaseInfo = getModuleReleaseInfo(siteName);

            String url = releaseInfo.getRepositoryUrl() + path;
            // Usage of SpringContextSingleton to prevent bug QA-9515
            final CloseableHttpClient httpClient = ((HttpClientService) SpringContextSingleton.getBean("HttpClientService")).getHttpClient(url);
            final HttpGet httpMethod = new HttpGet(UriComponentsBuilder.fromHttpUrl(url).build(false).toUri());
            httpMethod.addHeader("Authorization", "Basic " + Base64.encode((releaseInfo.getUsername() + ":" + releaseInfo.getPassword()).getBytes()));
            try (final CloseableHttpResponse httpResponse = httpClient.execute(httpMethod)) {
                if (httpResponse.getCode() == HttpServletResponse.SC_OK) {
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

    private ModuleReleaseInfo getModuleReleaseInfo(final String siteName) throws RepositoryException {
        JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession("live");
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
