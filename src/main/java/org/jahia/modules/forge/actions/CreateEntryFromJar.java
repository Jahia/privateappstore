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

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.xerces.impl.dv.util.Base64;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jahia.api.Constants;
import org.jahia.bin.ActionResult;
import org.jahia.commons.Version;
import org.jahia.data.templates.ModuleReleaseInfo;
import org.jahia.data.templates.ModulesPackage;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.taglibs.jcr.node.JCRTagUtils;
import org.jahia.tools.files.FileUpload;
import org.jahia.utils.PomUtils;
import org.jahia.utils.ProcessHelper;
import org.jahia.utils.i18n.Messages;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Action to par a jar and produce an entry in module list
 */
public class CreateEntryFromJar extends PrivateAppStoreAction {

    private static final Logger logger = LoggerFactory.getLogger(CreateEntryFromJar.class);
    private static final String[] EMPTY_REFERENCES = new String[]{"none"};
    private static final String JNT_FORGEMODULEVERSION = "jnt:forgeModuleVersion";
    private static final String JNT_FORGEPACKAGEVERSION = "jnt:forgePackageVersion";
    private static final String REDIRECT_URL = "redirectURL";
    private static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";
    private static final String REQUIRED_VERSION = "requiredVersion";
    private static final String VERSION_NUMBER = "versionNumber";
    private static final String DESCRIPTION = "description";
    private static final String AUTHOR_URL = "authorURL";
    private static final String ERROR = "error";
    private static final String PACKAGES = "packages";
    private static final String OWNER = "owner";
    private static final String MODULES_LIST = "modulesList";
    private static final String RESOURCES_PRIVATEAPPSTORE = "resources.privateappstore";

    String mavenExecutable;

    @Override
    public ActionResult doExecute(HttpServletRequest request, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        final FileUpload fu = (FileUpload) request.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);

        DiskFileItem uploadedFile = fu.getFileItems().get("file");
        String filename = uploadedFile.getName();
        Map<String, List<String>> formParameters = fu.getParameterMap();
        if (!StringUtils.contains(filename, "SNAPSHOT.")) {
            String extension = StringUtils.substringAfterLast(filename, ".");
            if (!(StringUtils.equals(extension, "jar") || StringUtils.equals(extension, "war"))) {
                String error = Messages.get(RESOURCES_PRIVATEAPPSTORE, "forge.uploadJar.error.wrong.format", session.getLocale());
                return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(ERROR, error));
            }
            try (JarFile jar = new JarFile(uploadedFile.getStoreLocation());) {
                Manifest manifest = jar.getManifest();
                if (manifest != null) {
                    Attributes attributes = manifest.getMainAttributes();
                    if (attributes.getValue("Jahia-Package-Name") != null) {
                        return createPackage(uploadedFile, jar, attributes, request, renderContext, resource, session, formParameters);
                    } else {
                        return createModule(uploadedFile, attributes, request, renderContext, resource, session, extension, formParameters);
                    }
                } else {
                    String error = Messages.get(RESOURCES_PRIVATEAPPSTORE, "forge.uploadJar.error.unable.read.manifest", session.getLocale());
                    return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(ERROR, error));
                }
            } catch (IOException e) {
                String error = Messages.get(RESOURCES_PRIVATEAPPSTORE, "forge.uploadJar.error.unable.read.file", session.getLocale());
                return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(ERROR, error));
            } finally {
                uploadedFile.delete();
            }
        } else {
            String error = Messages.get(RESOURCES_PRIVATEAPPSTORE, "forge.uploadJar.error.snapshot.not.allowed", session.getLocale());
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(ERROR, error));
        }
    }

    private ActionResult createPackage(DiskFileItem uploadedFile, JarFile jar, Attributes attributes, HttpServletRequest request, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> formParams) throws RepositoryException, JSONException, IOException {
        JCRNodeWrapper repository = resource.getNode();

        JCRNodeWrapper modulesPackage;

        String packageName = attributes.getValue("Jahia-Package-ID");
        String packageRelPath = "packages/" + packageName;
        String version = attributes.getValue("Jahia-Package-Version");

        Map<String, List<String>> packageParams = new HashMap<>();

        packageParams.put("packageName", Arrays.asList(packageName));
        packageParams.put(Constants.JCR_TITLE, Arrays.asList(attributes.getValue("Jahia-Package-Name")));
        packageParams.put(DESCRIPTION, Arrays.asList(attributes.getValue("Jahia-Package-Description")));
        packageParams.put(VERSION_NUMBER, Arrays.asList(version));

        String reqVersionAttribute = attributes.getValue("Jahia-Required-Version");
        final String requiredVersion = "version-" + reqVersionAttribute;
        if (StringUtils.isEmpty(packageName) || StringUtils.isEmpty(reqVersionAttribute) || StringUtils.isEmpty(version)) {
            String error = Messages.get(RESOURCES_PRIVATEAPPSTORE, "forge.uploadJar.error.missing.manifest.attribute", session.getLocale());
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(ERROR, error));
        }
        JCRNodeWrapper versions = getJahiaVersion(requiredVersion, resource, session);

        packageParams.put(REQUIRED_VERSION, Arrays.asList(versions.getNode(requiredVersion).getIdentifier()));

        // Create package
        List<String> packageParamKeys = Arrays.asList(DESCRIPTION, "category", "icon", "authorNameDisplayedAs", AUTHOR_URL, "authorEmail", "FAQ", "downloadCount", "supportedByJahia", "reviewedByJahia", "published", "deleted", "screenshots", "video");
        List<String> versionParamKeys = Arrays.asList(REQUIRED_VERSION, VERSION_NUMBER, "fileDsaSignature", "changeLog");
        Map<String, List<String>> packageParameters = new HashMap<>();
        Map<String, List<String>> versionParameters = new HashMap<>();

        String title = getParameter(packageParams, Constants.JCR_TITLE);
        if (StringUtils.isEmpty(title)) {
            title = packageName;
        }

        // manually add jcr:title
        packageParameters.put(Constants.JCR_TITLE, Arrays.asList(title));
        versionParameters.put(Constants.JCR_TITLE, Arrays.asList(title));

        for (Map.Entry<String, List<String>> packageParam : packageParams.entrySet()) {
            String packageParamKey = packageParam.getKey();
            List<String> packageParamValue = packageParam.getValue();
            if (packageParamKeys.contains(packageParamKey) && packageParamValue.get(0) != null) {
                packageParameters.put(packageParamKey, packageParamValue);
            } else if (versionParamKeys.contains(packageParamKey) && packageParamValue.get(0) != null) {
                versionParameters.put(packageParamKey, packageParamValue);
            }
        }

        logger.info("Start creating Private App Store Package {}", packageName);

        if (!repository.hasNode(packageRelPath)) {
            if (repository.hasNode(PACKAGES)) {
                repository = repository.getNode(PACKAGES);
            } else {
                repository = repository.addNode(PACKAGES, "jnt:contentFolder");
            }
            modulesPackage = createNode(request, packageParameters, repository, "jnt:forgePackage", packageName, false);
        } else {
            modulesPackage = repository.getNode(packageRelPath);
            packageParameters.remove(DESCRIPTION);
            packageParameters.remove(Constants.JCR_TITLE);
            setProperties(modulesPackage, packageParameters);
        }

        if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            List<String> roles = Arrays.asList(OWNER);
            modulesPackage.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
        }

        boolean hasPackageVersions = JCRTagUtils.hasChildrenOfType(modulesPackage, JNT_FORGEPACKAGEVERSION);

        // create package version
        logger.info("Start adding package version {} of {}", version, title);

        if (hasPackageVersions && !hasValidVersionNumber(modulesPackage, version)) {
            String error = Messages.getWithArgs(RESOURCES_PRIVATEAPPSTORE, "forge.uploadJar.error.versionNumber", session.getLocale(), packageName, version);
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(ERROR, error));
        }

        JCRNodeWrapper packageVersion = createNode(request, versionParameters, modulesPackage, JNT_FORGEPACKAGEVERSION, modulesPackage.getName() + "-" + version, false);

        if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            List<String> roles = Arrays.asList(OWNER);
            packageVersion.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
        }

        packageVersion.uploadFile(uploadedFile.getName(), uploadedFile.getInputStream(), uploadedFile.getContentType());

        logger.info("Package version {} of {} successfully added", version, title);

        // create modules list
        JCRNodeWrapper modulesList;

        if (packageVersion.hasNode(MODULES_LIST)) {
            modulesList = packageVersion.getNode(MODULES_LIST);
        } else {
            modulesList = packageVersion.addNode(MODULES_LIST, "jnt:forgePackageModulesList");
        }

        // Get list of modules from package
        Map<String, ModulesPackage.PackagedModule> packagedModuleMap = ModulesPackage.create(jar).getModules();

        for (ModulesPackage.PackagedModule packagedModule : packagedModuleMap.values()) {
            JCRNodeWrapper module;

            if (modulesList.hasNode(packagedModule.getManifestAttributes().getValue(BUNDLE_SYMBOLIC_NAME))) {
                module = modulesList.getNode(packagedModule.getManifestAttributes().getValue(BUNDLE_SYMBOLIC_NAME));
            } else {
                module = modulesList.addNode(packagedModule.getManifestAttributes().getValue(BUNDLE_SYMBOLIC_NAME), "jnt:forgePackageModule");
            }

            module.setProperty("moduleName", packagedModule.getManifestAttributes().getValue("Implementation-Title"));
            module.setProperty("moduleVersion", packagedModule.getManifestAttributes().getValue("Implementation-Version"));
        }

        logger.info("Private App Store Package {} successfully created and added to repository {}", packageName,
                repository.getPath());

        String packageUrl = renderContext.getResponse().encodeURL(modulesPackage.getUrl());
        String packageAbsoluteUrl = modulesPackage.getProvider().getAbsoluteContextPath(request) + packageUrl;

        session.save();

        ActionResult uploadResult =  new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("successRedirectUrl", packageUrl).put(
                "successRedirectAbsoluteUrl", packageAbsoluteUrl));
        if(formParams.containsKey(REDIRECT_URL)){
            uploadResult.setUrl(formParams.get(REDIRECT_URL).get(0));
        }

        return uploadResult;
    }

    private ActionResult createModule(DiskFileItem uploadedFile, Attributes attributes, HttpServletRequest request, RenderContext renderContext, Resource resource, JCRSessionWrapper session, String extension, Map<String, List<String>> formParams) throws Exception {
        JCRNodeWrapper repository = resource.getNode();

        Map<String, List<String>> moduleParams = new HashMap<>();
        String groupId;
        String version = attributes.getValue("Implementation-Version");
        String moduleName = attributes.getValue(BUNDLE_SYMBOLIC_NAME);
        if (uploadedFile.getName().endsWith(".war")) {
            moduleName = attributes.getValue("root-folder");
        }
        groupId = attributes.getValue("Jahia-GroupId");
        JCRSiteNode site = resource.getNode().getResolveSite();
        String forgeSettingsUrl = site.getProperty("forgeSettingsUrl").getString();

        moduleParams.put("moduleName", Arrays.asList(moduleName));
        moduleParams.put("groupId", Arrays.asList(groupId));
        moduleParams.put(Constants.JCR_TITLE, Arrays.asList(attributes.getValue("Implementation-Title")));
        moduleParams.put(DESCRIPTION, Arrays.asList(attributes.getValue("Bundle-Description")));
        moduleParams.put(AUTHOR_URL, Arrays.asList(attributes.getValue("Implementation-URL")));
        moduleParams.put("codeRepository", Arrays.asList(attributes.getValue("Jahia-Source-Control-Connection")));
        moduleParams.put(VERSION_NUMBER, Arrays.asList(version));


        String forgeUrl = StringUtils.substringBefore(request.getRequestURL().toString(), "/render");
        String reqVersionAttribute = attributes.getValue("Jahia-Required-Version");
        final String requiredVersion = "version-" + reqVersionAttribute;
        if (StringUtils.isEmpty(moduleName) || StringUtils.isEmpty(groupId) || StringUtils.isEmpty(reqVersionAttribute)) {
            String error = Messages.get(RESOURCES_PRIVATEAPPSTORE, "forge.uploadJar.error.missing.manifest.attribute", session.getLocale());
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(ERROR, error));
        }
        String moduleRelPath = groupId.replace(".", "/") + "/" + moduleName;
        moduleParams.put("url", Arrays.asList(forgeUrl + "/mavenproxy/" + site.getName() + "/" + moduleRelPath + "/" + version + "/" + moduleName + "-" + version + "." + extension));

        JCRNodeWrapper versions = getJahiaVersion(requiredVersion, resource, session);

        moduleParams.put(REQUIRED_VERSION, Arrays.asList(versions.getNode(requiredVersion).getIdentifier()));

        String user = site.getProperty("forgeSettingsUser").getString();
        String password = new String(Base64.decode(site.getProperty("forgeSettingsPassword").getString()));

        File artifact = null;
        try {
            artifact = File.createTempFile("artifact", "." + extension);
            FileUtils.copyFile(uploadedFile.getStoreLocation(), artifact);

            ModuleReleaseInfo moduleReleaseInfo = new ModuleReleaseInfo();
            moduleReleaseInfo.setRepositoryId("remote-repository");
            moduleReleaseInfo.setRepositoryUrl(forgeSettingsUrl);
            moduleReleaseInfo.setUsername(user);
            moduleReleaseInfo.setPassword(password);
            deployToMaven(groupId, moduleName, moduleReleaseInfo, artifact);
        } catch (IOException e) {
            String error = Messages.get(RESOURCES_PRIVATEAPPSTORE, "forge.uploadJar.error.cannot.upload", session.getLocale());
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(ERROR, error));
        } finally {
            FileUtils.deleteQuietly(artifact);
        }

        // Create module

        List<String> moduleParamKeys = Arrays.asList(DESCRIPTION, "category", "icon", "authorNameDisplayedAs", AUTHOR_URL, "authorEmail", "FAQ", "codeRepository", "downloadCount", "supportedByJahia", "reviewedByJahia", "published", "deleted", "screenshots", "video", "groupId");
        List<String> versionParamKeys = Arrays.asList(REQUIRED_VERSION, VERSION_NUMBER, "fileDsaSignature", "changeLog", "url");
        Map<String, List<String>> moduleParameters = new HashMap<>();
        Map<String, List<String>> versionParameters = new HashMap<>();

        String title = getParameter(moduleParams, Constants.JCR_TITLE);
        if (StringUtils.isEmpty(title)) {
            title = moduleName;
        }
        // manually add jcr:title
        moduleParameters.put(Constants.JCR_TITLE, Arrays.asList(title));
        versionParameters.put(Constants.JCR_TITLE, Arrays.asList(title));

        for (Map.Entry<String, List<String>> moduleParam : moduleParams.entrySet()) {
            String moduleParamKey = moduleParam.getKey();
            List<String> moduleParamValue = moduleParam.getValue();
            if (moduleParamKeys.contains(moduleParamKey) && moduleParamValue.get(0) != null) {
                moduleParameters.put(moduleParamKey, moduleParamValue);
            } else if (versionParamKeys.contains(moduleParamKey) && moduleParamValue.get(0) != null) {
                versionParameters.put(moduleParamKey, moduleParamValue);
            }
        }

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
            moduleParameters.remove(DESCRIPTION);
            moduleParameters.remove(Constants.JCR_TITLE);
            setProperties(module, moduleParameters);
        }

        if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            List<String> roles = Arrays.asList(OWNER);
            module.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
        }

        boolean hasModuleVersions = JCRTagUtils.hasChildrenOfType(module, JNT_FORGEMODULEVERSION);

        // create module version

        logger.info("Start adding module version {} of {}", version, title);

        if (hasModuleVersions && !hasValidVersionNumber(module, version)) {
            String error = Messages.getWithArgs(RESOURCES_PRIVATEAPPSTORE, "forge.uploadJar.error.versionNumber", session.getLocale(), moduleName, version);
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(ERROR, error));
        }

        JCRNodeWrapper moduleVersion = createNode(request, versionParameters, module, JNT_FORGEMODULEVERSION, module.getName() + "-" + version, false);

        String value = attributes.getValue("Jahia-Depends");
        if(value!=null) {
            String[] jahiaDepends = value.split(",");
            moduleVersion.setProperty("references", jahiaDepends);
        } else {
            moduleVersion.setProperty("references", EMPTY_REFERENCES);
        }

        if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            List<String> roles = Arrays.asList(OWNER);
            moduleVersion.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
        }

        logger.info("Module version {} of {} successfully added", version, title);

        logger.info("Private App Store Module {} successfully created and added to repository {}", moduleName,
                repository.getPath());

        String moduleUrl = renderContext.getResponse().encodeURL(module.getUrl());
        String moduleAbsoluteUrl = module.getProvider().getAbsoluteContextPath(request) + moduleUrl;
        session.save();
        ActionResult uploadResult =  new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("successRedirectUrl", moduleUrl).put(
                "successRedirectAbsoluteUrl", moduleAbsoluteUrl));
        if(formParams.containsKey(REDIRECT_URL)){
            uploadResult.setUrl(formParams.get(REDIRECT_URL).get(0));
        }

        return uploadResult;
    }

    public void setMavenExecutable(String mavenExecutable) {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows") && !mavenExecutable.endsWith(".bat")) {
            mavenExecutable = mavenExecutable + ".bat";
        }
        this.mavenExecutable = mavenExecutable;
    }

    private JCRNodeWrapper getJahiaVersion(String requiredVersion, Resource resource, JCRSessionWrapper session) throws RepositoryException{
        JCRNodeWrapper versions = session.getNode(resource.getNode().getResolveSite().getPath() + "/contents/modules-required-versions");

        if (!versions.hasNode(requiredVersion)) {
            Version v = new Version(requiredVersion);
            JCRNodeWrapper n = versions.addNode(requiredVersion, "jnt:jahiaVersion");
            n.setProperty("major", v.getMajorVersion());
            n.setProperty("minor", v.getMinorVersion());
            n.setProperty("servicePack", v.getServicePackVersion());
            n.setProperty("patch", v.getPatchVersion());
            n.setProperty("releaseCandidate", v.getReleaseCandidateNumber());
            n.setProperty("beta", v.getBetaNumber());
            n.setProperty("qualifier", v.getQualifiers().toArray(new String[v.getQualifiers().size()]));
        }

        return versions;
    }

    private boolean hasValidVersionNumber(JCRNodeWrapper node, String versionNumber) throws RepositoryException {

        if (StringUtils.isEmpty(versionNumber)) {
            return false;
        }

        List<JCRNodeWrapper> nodeVersions;

        if (node.hasProperty(JNT_FORGEMODULEVERSION)) {
            nodeVersions = JCRTagUtils.getChildrenOfType(node, JNT_FORGEMODULEVERSION);
        } else {
            nodeVersions = JCRTagUtils.getChildrenOfType(node, JNT_FORGEPACKAGEVERSION);
        }

        for (JCRNodeWrapper nodeVersion : nodeVersions) {
            if (nodeVersion.hasProperty(VERSION_NUMBER) && nodeVersion.getProperty(VERSION_NUMBER).getString().equals(versionNumber))
                return false;
        }

        return true;
    }

    private void deployToMaven(String groupId, String artifactId, ModuleReleaseInfo releaseInfo, File generatedJar) throws IOException {
        File settings = null;
        File pomFile = null;
        try {
            if (!StringUtils.isEmpty(releaseInfo.getUsername()) && !StringUtils.isEmpty(releaseInfo.getPassword())) {
                settings = File.createTempFile("settings", ".xml");
                try (BufferedWriter w = new BufferedWriter(new FileWriter(settings))) {
                    w.write("<settings><servers><server><id>" + releaseInfo.getRepositoryId() + "</id><username>");
                    w.write(releaseInfo.getUsername());
                    w.write("</username><password>");
                    w.write(releaseInfo.getPassword());
                    w.write("</password></server></servers></settings>");
                }
            }
            JarFile jar = new JarFile(generatedJar);
            pomFile = PomUtils.extractPomFromJar(jar, groupId, artifactId);
            jar.close();

            Model pom;
            try {
                pom = PomUtils.read(pomFile);
            } catch (XmlPullParserException e) {
                throw new IOException(e);
            }
            String version = pom.getVersion();
            if (version == null) {
                version = pom.getParent().getVersion();
            }
            if (version == null) {
                throw new IOException("unable to read project version");
            }
            String[] deployParams = {"deploy:deploy-file", "-Dfile=" + generatedJar,
                    "-DrepositoryId=" + releaseInfo.getRepositoryId(), "-Durl=" + releaseInfo.getRepositoryUrl(),
                    "-DpomFile=" + pomFile.getPath(),
                    "-Dpackaging=" + StringUtils.substringAfterLast(generatedJar.getName(), "."),
                    "-DgroupId=" + PomUtils.getGroupId(pom), "-DartifactId=" + pom.getArtifactId(),
                    "-Dversion=" + version};
            if (settings != null) {
                deployParams = (String[]) ArrayUtils.addAll(deployParams,
                        new String[]{"--settings", settings.getPath()});
            }
            StringBuilder out = new StringBuilder();
            int ret = ProcessHelper.execute(mavenExecutable, deployParams, null,
                    generatedJar.getParentFile(), out, out);

            if (ret > 0) {
                String s = getMavenError(out.toString());
                logger.error("Maven archetype call returned {}", ret);
                logger.error("Maven out : {}", out);
                throw new IOException("Maven invocation failed\n" + s);
            }
        } finally {
            FileUtils.deleteQuietly(settings);
            FileUtils.deleteQuietly(pomFile);
        }
    }

    private String getMavenError(String out) {
        Matcher m = Pattern.compile("^\\[ERROR\\](.*)$", Pattern.MULTILINE).matcher(out);
        StringBuilder s = new StringBuilder();
        while (m.find()) {
            s.append(m.group(1)).append("\n");
        }
        return s.toString();
    }
}
