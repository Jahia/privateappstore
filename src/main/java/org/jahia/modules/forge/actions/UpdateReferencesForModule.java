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
package org.jahia.modules.forge.actions;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.data.templates.ModuleReleaseInfo;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.rules.BackgroundAction;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.servlet.http.HttpServletResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.notification.HttpClientService;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Date: 2013-05-17
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class UpdateReferencesForModule extends Action implements BackgroundAction {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(UpdateReferencesForModule.class);
    private static final String[] EMPTY_REFERENCES = new String[]{"none"};

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode();

        updateDependencies(module);

        return ActionResult.OK_JSON;
    }

    private void updateDependencies(JCRNodeWrapper module) throws RepositoryException, IOException {
        ModuleReleaseInfo releaseInfo = getModuleReleaseInfo(module.getResolveSite());

        String url = module.getProperty("url").getString();
        // Usage of SpringContextSingleton to prevent bug QA-9515
        final CloseableHttpClient httpClient = ((HttpClientService) SpringContextSingleton.getBean("HttpClientService")).getHttpClient(url);
        final HttpGet httpMethod = new HttpGet(UriComponentsBuilder.fromHttpUrl(url).build(false).toUri());
        httpMethod.addHeader("Authorization", "Basic " + Base64.encode((releaseInfo.getUsername() + ":" + releaseInfo.getPassword()).getBytes()));

        try ( CloseableHttpResponse httpResponse = httpClient.execute(httpMethod)) {
            if (httpResponse.getCode() == HttpServletResponse.SC_OK) {
                File tmpJarFile = File.createTempFile("appStore", ".jar");
                try {
                    IOUtils.copy(httpResponse.getEntity().getContent(), new FileOutputStream(tmpJarFile));
                    try ( JarFile jarFile = new JarFile(tmpJarFile)) {
                        Manifest manifest = jarFile.getManifest();
                        Attributes attributes = manifest.getMainAttributes();
                        String value = attributes.getValue("Jahia-Depends");
                        if (value != null) {
                            String[] jahiaDepends = value.split(",");
                            module.setProperty("references", jahiaDepends);
                        } else {
                            module.setProperty("references", EMPTY_REFERENCES);
                        }
                        module.getSession().save();
                        CacheHelper.flushOutputCachesForPath(module.getParent().getPath(), true);
                    }
                } finally {
                    FileUtils.forceDelete(tmpJarFile);
                }
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private ModuleReleaseInfo getModuleReleaseInfo(final JCRSiteNode siteNode) throws RepositoryException {
        String forgeSettingsUrl = siteNode.getProperty("forgeSettingsUrl").getString();
        String user = siteNode.getProperty("forgeSettingsUser").getString();
        String password = new String(Base64.decode(siteNode.getProperty("forgeSettingsPassword").getString()));
        ModuleReleaseInfo info = new ModuleReleaseInfo();
        info.setRepositoryUrl(forgeSettingsUrl);
        info.setUsername(user);
        info.setPassword(password);
        return info;
    }

    @Override
    public void executeBackgroundAction(JCRNodeWrapper jcrNodeWrapper) {
        try {
            updateDependencies(jcrNodeWrapper);
        } catch (RepositoryException | IOException e) {
            logger.error("Error while updating dependencies", e);
        }
    }
}
