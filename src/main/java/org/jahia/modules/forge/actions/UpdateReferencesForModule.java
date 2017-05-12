/**
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group. All rights reserved.
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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.bin.ActionResult;
import org.jahia.data.templates.ModuleReleaseInfo;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;


/**
 * Date: 2013-05-17
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class UpdateReferencesForModule extends PrivateAppStoreAction {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(UpdateReferencesForModule.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode();

            ModuleReleaseInfo releaseInfo = getModuleReleaseInfo(renderContext.getSite());

            String url = module.getProperty("url").getString();
            HttpClient client = new HttpClient();
            GetMethod getMethod = new GetMethod(url);
            getMethod.addRequestHeader("Authorization", "Basic " + Base64.encode((releaseInfo.getUsername() + ":" + releaseInfo.getPassword()).getBytes()));

            int res = client.executeMethod(null, getMethod);
            if (res == 200) {
                File tmpJarFile = File.createTempFile("appStore", ".jar");
                try {
                    IOUtils.copy(getMethod.getResponseBodyAsStream(), new FileOutputStream(tmpJarFile));
                    JarFile jarFile = new JarFile(tmpJarFile);
                    Manifest manifest = jarFile.getManifest();
                    Attributes attributes = manifest.getMainAttributes();
                    String value = attributes.getValue("Jahia-Depends");
                    if(value!=null) {
                        String[] jahiaDepends = value.split(",");
                        module.setProperty("references", jahiaDepends);
                    } else {
                        module.setProperty("references", "none");
                    }
                    session.save();
                    CacheHelper.flushOutputCachesForPath(module.getParent().getPath(), true);
                } finally {
                    FileUtils.forceDelete(tmpJarFile);
                }

            }

        return ActionResult.OK_JSON;
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
}