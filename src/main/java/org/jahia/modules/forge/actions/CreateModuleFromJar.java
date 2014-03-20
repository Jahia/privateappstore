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
package org.jahia.modules.forge.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.commons.Version;
import org.jahia.data.templates.ModuleReleaseInfo;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.taglibs.jcr.node.JCRTagUtils;
import org.jahia.tools.files.FileUpload;
import org.jahia.utils.i18n.Messages;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Action to par a jar and produce an entry in module list
 */
public class CreateModuleFromJar extends PrivateAppStoreAction {

    private transient static Logger logger = LoggerFactory.getLogger(CreateModuleFromJar.class);
    private JahiaTemplateManagerService templateManagerService;

    @Override
    public ActionResult doExecute(HttpServletRequest request, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        Map<String, List<String>> moduleParams = new HashMap<String, List<String>>();
        final FileUpload fu = (FileUpload) request.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);
        DiskFileItem uploadedJar = fu.getFileItems().get("file");
        String filename = uploadedJar.getName();
        if (!StringUtils.contains(filename,"-SNAPSHOT.")) {
            String extension = StringUtils.substringAfterLast(filename, ".");
            if (!(StringUtils.equals(extension,"jar") || StringUtils.equals(extension,"war"))) {
                String error = Messages.get("resources.private-app-store","forge.uploadJar.error.wrong.format",session.getLocale());
                return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error",error));
            }
            String moduleName;
            String version;
            String groupId;
            OutputStream out = null;
            JarFile jar = null;
            try {
                jar = new JarFile(uploadedJar.getStoreLocation());
                Manifest manifest = jar.getManifest();
                if (manifest != null) {
                    Attributes attributes = manifest.getMainAttributes();
                    version = attributes.getValue("Implementation-Version");
                    moduleName = attributes.getValue("Bundle-SymbolicName");
                    if (uploadedJar.getName().endsWith(".war")) {
                        moduleName = attributes.getValue("root-folder");
                    }
                    groupId = attributes.getValue("Jahia-GroupId");
                    JCRSiteNode site = resource.getNode().getResolveSite();
                    String forgeSettingsUrl = site.getProperty("forgeSettingsUrl").getString();

                    moduleParams.put("moduleName", Arrays.asList(moduleName));
                    moduleParams.put("groupId", Arrays.asList(groupId));
                    moduleParams.put("jcr:title", Arrays.asList(attributes.getValue("Implementation-Title")));
                    moduleParams.put("description", Arrays.asList(attributes.getValue("Bundle-Description")));
                    //moduleParams.put("authorNameDisplayedAs", Arrays.asList(attributes.getValue("Built-By")));
                    moduleParams.put("authorURL", Arrays.asList(attributes.getValue("Implementation-URL")));
                    //moduleParams.put("authorEmail", Arrays.asList(attributes.getValue("")));
                    moduleParams.put("codeRepository", Arrays.asList(attributes.getValue("Jahia-Source-Control-Connection")));
                    moduleParams.put("versionNumber", Arrays.asList(version));
                    String forgeUrl = StringUtils.substringBefore(request.getRequestURL().toString(), "/render");
                    String reqVersionAttribute = attributes.getValue("Jahia-Required-Version");
                    final String requiredVersion = "version-" + reqVersionAttribute;
                    if (moduleName == null || groupId == null || requiredVersion == null) {
                        String error = Messages.get("resources.private-app-store","forge.uploadJar.error.missing.manifest.attribute",session.getLocale());
                        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error",error));
                    }
                    String moduleRelPath = groupId.replace(".", "/") + "/" + moduleName;
                    moduleParams.put("url", Arrays.asList(forgeUrl + "/mavenproxy/" + site.getName() + "/" + moduleRelPath + "/" + version + "/" + moduleName + "-" + version + "." + extension));
                    JCRNodeWrapper versions = session.getNode(resource.getNode().getResolveSite().getPath() + "/contents/modules-required-versions");

                    if (!versions.hasNode(requiredVersion)) {
                        Version v = new Version(requiredVersion);
                        JCRNodeWrapper n = versions.addNode(requiredVersion, "jnt:jahiaVersion");
                        n.setProperty("major",v.getMajorVersion());
                        n.setProperty("minor",v.getMinorVersion());
                        n.setProperty("servicePack",v.getServicePackVersion());
                        n.setProperty("patch",v.getPatchVersion());
                        n.setProperty("releaseCandidate",v.getReleaseCandidateNumber());
                        n.setProperty("beta",v.getBetaNumber());
                        n.setProperty("qualifier",v.getQualifiers().toArray(new String[v.getQualifiers().size()]));
                    }
                    moduleParams.put("requiredVersion", Arrays.asList(versions.getNode(requiredVersion).getIdentifier()));

                    String user = site.getProperty("forgeSettingsUser").getString();
                    String password = new String(Base64.decode(site.getProperty("forgeSettingsPassword").getString()));

                    File artifact = null;
                    try {
                        artifact = File.createTempFile("artifact", "." + extension);
                        FileUtils.copyFile(uploadedJar.getStoreLocation(), artifact);

                        ModuleReleaseInfo moduleReleaseInfo = new ModuleReleaseInfo();
                        moduleReleaseInfo.setRepositoryId("remote-repository");
                        moduleReleaseInfo.setRepositoryUrl(forgeSettingsUrl);
                        moduleReleaseInfo.setUsername(user);
                        moduleReleaseInfo.setPassword(password);
                        templateManagerService.deployToMaven(groupId,moduleName,moduleReleaseInfo, artifact);
                    } catch (IOException e) {
                        String error = Messages.get("resources.private-app-store","forge.uploadJar.error.cannot.upload",session.getLocale());
                        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error", error));
                    } finally {
                        FileUtils.deleteQuietly(artifact);
                    }

                    // Create module

                    List<String> moduleParamKeys = Arrays.asList("description", "category", "icon", "authorNameDisplayedAs", "authorURL", "authorEmail", "FAQ", "codeRepository", "downloadCount", "supportedByJahia", "reviewedByJahia", "published", "deleted", "screenshots", "video","groupId");
                    List<String> versionParamKeys = Arrays.asList("requiredVersion", "versionNumber", "fileDsaSignature", "changeLog", "url");
                    Map<String, List<String>> moduleParameters = new HashMap<String, List<String>>();
                    Map<String, List<String>> versionParameters = new HashMap<String, List<String>>();

                    String title = getParameter(moduleParams, "jcr:title");
                    if (StringUtils.isEmpty(title)) {
                        title = moduleName;
                    }
                    // manually add jcr:title
                    moduleParameters.put("jcr:title", Arrays.asList(title));
                    versionParameters.put("jcr:title", Arrays.asList(title));

                    for (String key : moduleParams.keySet()) {
                        if (moduleParamKeys.contains(key) && moduleParams.get(key).get(0) != null) {
                            moduleParameters.put(key, moduleParams.get(key));
                        } else if (versionParamKeys.contains(key) && moduleParams.get(key).get(0) != null) {
                            versionParameters.put(key, moduleParams.get(key));
                        }
                    }

                    JCRNodeWrapper repository = resource.getNode();

                    logger.info("Start creating Private App Store Module {}", moduleName);

                    JCRNodeWrapper module;

                    if (!repository.hasNode(moduleRelPath)) {
                        for (String segment : groupId.split("\\.")) {
                             if (repository.hasNode(segment)) {
                                 repository = repository.getNode(segment);
                             } else {
                                 repository = repository.addNode(segment, "jnt:contentFolder");
                             }
                        }
                        module = createNode(request, moduleParameters, repository, "jnt:forgeModule", moduleName, false);
                    } else {
                        module = repository.getNode(moduleRelPath);
                        setProperties(module, moduleParameters);
                    }

                    if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
                        List<String> roles = Arrays.asList("owner");
                        module.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
                    }

                    boolean hasModuleVersions = JCRTagUtils.hasChildrenOfType(module, "jnt:forgeModuleVersion");

                    // create module version

                    logger.info("Start adding module version {} of {}", version, title);

                    if (hasModuleVersions && !hasValidVersionNumber(module, version)) {
                        String error = Messages.getWithArgs("resources.private-app-store","forge.uploadJar.error.versionNumber",session.getLocale(),moduleName,version);
                        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error", error));
                    }

                    JCRNodeWrapper moduleVersion = createNode(request, versionParameters, module, "jnt:forgeModuleVersion", module.getName()+"-"+version, false);

                    if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
                        List<String> roles = Arrays.asList("owner");
                        moduleVersion.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
                    }


                    logger.info("Module version {} of {} successfully added", version, title);

                    logger.info("Private App Store Module {} successfully created and added to repository {}", moduleName,
                            repository.getPath());

                    String moduleUrl = renderContext.getResponse().encodeURL(module.getUrl());
                    String moduleAbsoluteUrl = module.getProvider().getAbsoluteContextPath(request) + moduleUrl;
                    session.save();

                    return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("moduleUrl", moduleUrl).put(
                            "moduleAbsoluteUrl", moduleAbsoluteUrl));


                } else {
                    String error = Messages.get("resources.private-app-store","forge.uploadJar.error.unable.read.manifest",session.getLocale());
                    return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error",error));
                }
            } catch (IOException e) {
                String error = Messages.get("resources.private-app-store","forge.uploadJar.error.unable.read.file",session.getLocale());
                return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error", error));
            } finally {
                if (jar != null) {
                    jar.close();
                }

                uploadedJar.delete();

                if (out != null) {
                    out.close();
                }

            }
        } else {
            String error = Messages.get("resources.private-app-store","forge.uploadJar.error.snapshot.not.allowed",session.getLocale());
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error", error));
        }
        // create module
    }

    public void setTemplateManagerService(JahiaTemplateManagerService templateManagerService) {
        this.templateManagerService = templateManagerService;
    }

    private boolean hasValidVersionNumber(JCRNodeWrapper module, String versionNumber) throws RepositoryException {

        if (versionNumber == null ) {
            return false;
        }

        List<JCRNodeWrapper> moduleVersions = JCRTagUtils.getChildrenOfType(module, "jnt:forgeModuleVersion");

        for (JCRNodeWrapper moduleVersion : moduleVersions) {
            if (moduleVersion.hasProperty("versionNumber") &&moduleVersion.getProperty("versionNumber").getString().equals(versionNumber))
                return false;
        }

        return true;
    }
}
