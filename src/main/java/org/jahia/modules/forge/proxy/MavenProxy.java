/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Enterprise Distribution                  =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group. All rights reserved.
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTION - IMPORTANT LICENSING INFORMATION
 * ===============================================================
 *
 *     This file is part of a Jahia Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance
 *     with the terms contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     See the license for the rights, obligations and limitations governing use
 *     of the contents of the software.
 *
 *     For questions regarding licensing, support, production usage,
 *     please contact our team at sales@jahia.com or go to: http://www.jahia.com/license
 */
package org.jahia.modules.forge.proxy;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
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

public class MavenProxy implements Controller {

//    private HttpClientService httpClientService;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (request.getMethod().equals("GET")) {
            String pathInfo = StringUtils.substringAfter(request.getPathInfo(),"/mavenproxy/");
            String siteName = StringUtils.substringBefore(pathInfo, "/");
            String path = "/" + StringUtils.substringAfter(pathInfo, "/");

            ModuleReleaseInfo releaseInfo = getModuleReleaseInfo(siteName);

            String url = releaseInfo.getRepositoryUrl() + path;
            HttpClient client = new HttpClient();
            GetMethod getMethod = new GetMethod(url);
            getMethod.addRequestHeader("Authorization", "Basic " + Base64.encode((releaseInfo.getUsername() + ":" + releaseInfo.getPassword()).getBytes()));

            int res = client.executeMethod(null, getMethod);
            if (res == 200) {
                for (Header header : getMethod.getResponseHeaders()) {
                    response.setHeader(header.getName(), header.getValue());
                }
                IOUtils.copy(getMethod.getResponseBodyAsStream(), response.getOutputStream());
            } else {
                response.sendError(res);
            }
        }
        return null;
    }

    private ModuleReleaseInfo getModuleReleaseInfo(final String siteName) throws RepositoryException {
        JCRSessionWrapper session =  JCRSessionFactory.getInstance().getCurrentUserSession("live");
        JCRSiteNode siteNode = (JCRSiteNode) session.getNode("/sites/"+siteName);
        String forgeSettingsUrl = siteNode.getProperty("forgeSettingsUrl").getString();
        String user = siteNode.getProperty("forgeSettingsUser").getString();
        String password = new String(Base64.decode(siteNode.getProperty("forgeSettingsPassword").getString()));
        ModuleReleaseInfo info = new ModuleReleaseInfo();
        info.setRepositoryUrl(forgeSettingsUrl);
        info.setUsername(user);
        info.setPassword(password);
        return info;
    }

//    public void setHttpClientService(HttpClientService httpClientService) {
//        this.httpClientService = httpClientService;
//    }

}
