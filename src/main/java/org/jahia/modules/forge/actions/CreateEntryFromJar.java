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
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.xerces.impl.dv.util.Base64;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;

/**
 * Action to par a jar and produce an entry in module list
 */
@Component(service = Action.class)
public class CreateEntryFromJar extends Action {

    private static final Logger logger = LoggerFactory.getLogger(CreateEntryFromJar.class);
    private static final String DEFAULT_MAVEN_EXECUTABLE = "mvn";
    private static final String[] EMPTY_REFERENCES = new String[]{"none"};
    private static final String JNT_FORGEMODULEVERSION = "jnt:forgeModuleVersion";
    private static final String JNT_FORGEPACKAGEVERSION = "jnt:forgePackageVersion";
    private static final String JNT_CONTENT_FOLDER = "jnt:contentFolder";
    private static final String REDIRECT_URL = "redirectURL";
    private static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";
    private static final String REQUIRED_VERSION = "requiredVersion";
    private static final String VERSION_NUMBER = "versionNumber";
    private static final String VERSION_PREFIX = "version-";
    /**
     * Path of the {@link org.jahia.modules.forge.proxy.MavenProxy} servlet, relative to the Jahia
     * context root. The proxy is an OSGi HttpServlet (alias=/mavenproxy) and is therefore served
     * under /modules — NOT under /cms (the render servlet). Download URLs must use this prefix.
     */
    private static final String MAVENPROXY_PATH = "/modules/mavenproxy/";
    private static final String DESCRIPTION = "description";
    private static final String AUTHOR_URL = "authorURL";
    private static final String ERROR = "error";
    private static final String PACKAGES = "packages";
    private static final String OWNER = "owner";
    private static final String MODULES_LIST = "modulesList";
    private static final String RESOURCES_PRIVATEAPPSTORE = "resources.privateappstore";
    private static final String GROUP_ID = "groupId";
    private static final String MODULE_NAME = "moduleName";
    private static final String CATEGORY = "category";
    private static final String AUTHOR_NAME_DISPLAYED_AS = "authorNameDisplayedAs";
    private static final String AUTHOR_EMAIL = "authorEmail";
    private static final String CODE_REPOSITORY = "codeRepository";
    private static final String DOWNLOAD_COUNT = "downloadCount";
    private static final String SUPPORTED_BY_JAHIA = "supportedByJahia";
    private static final String REVIEWED_BY_JAHIA = "reviewedByJahia";
    private static final String PUBLISHED = "published";
    private static final String DELETED = "deleted";
    private static final String SCREENSHOTS = "screenshots";
    private static final String VIDEO = "video";
    private static final String CHANGE_LOG = "changeLog";
    private static final String FILE_DSA_SIGNATURE = "fileDsaSignature";
    private static final String REFERENCES = "references";
    private static final String SUCCESS_REDIRECT_URL = "successRedirectUrl";
    private static final String SUCCESS_REDIRECT_ABSOLUTE_URL = "successRedirectAbsoluteUrl";
    private static final String ERR_UNABLE_READ_FILE = "forge.uploadJar.error.unable.read.file";
    private static final String ERR_MISSING_MANIFEST_ATTRIBUTE = "forge.uploadJar.error.missing.manifest.attribute";
    private static final String ERR_VERSION_NUMBER = "forge.uploadJar.error.versionNumber";
    private static final String LOG_UNSAFE_REDIRECT = "CreateEntryFromJar: rejected unsafe redirectURL '{}'";
    /** Maximum accepted size for an uploaded artifact (guards against resource-exhaustion uploads). */
    private static final long MAX_UPLOAD_SIZE_BYTES = 200L * 1024 * 1024;
    /** Maximum size of a single tar entry we read into memory (guards against decompression bombs). */
    private static final long MAX_TAR_ENTRY_SIZE_BYTES = 10L * 1024 * 1024;

    String mavenExecutable;

    @Activate
    public void activate() {
        setName("createEntryFromJar");
        setRequireAuthenticatedUser(true);
        setRequiredPermission("jahiaForgeUploadModule");
        setRequiredMethods("POST");
        if (mavenExecutable == null) {
            String configured = System.getProperty("mvnPath", DEFAULT_MAVEN_EXECUTABLE);
            setMavenExecutable(configured);
        }
    }

    @Override
    public ActionResult doExecute(HttpServletRequest request, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        final FileUpload fu = (FileUpload) request.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);

        DiskFileItem uploadedFile = fu.getFileItems().get("file");
        if (uploadedFile == null) {
            return errorResult(session, ERR_UNABLE_READ_FILE);
        }
        if (uploadedFile.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            if (logger.isWarnEnabled()) {
                logger.warn("CreateEntryFromJar: rejected oversized upload '{}' ({} bytes)",
                        ActionSecurityUtils.sanitizeForLog(uploadedFile.getName()), uploadedFile.getSize());
            }
            uploadedFile.delete();
            return errorResult(session, ERR_UNABLE_READ_FILE);
        }
        String filename = uploadedFile.getName();
        if (StringUtils.contains(filename, "SNAPSHOT.")) {
            return errorResult(session, "forge.uploadJar.error.snapshot.not.allowed");
        }
        String extension = StringUtils.substringAfterLast(filename, ".");
        if (!(StringUtils.equals(extension, "jar") || StringUtils.equals(extension, "war") || StringUtils.equals(extension, "tgz"))) {
            return errorResult(session, "forge.uploadJar.error.wrong.format");
        }
        Map<String, List<String>> formParameters = fu.getParameterMap();
        if (extension.endsWith("tgz")) {
            return handleTgzUpload(uploadedFile, extension, request, renderContext, resource, session, formParameters);
        }
        return handleJarUpload(uploadedFile, extension, request, renderContext, resource, session, formParameters);
    }

    private ActionResult handleTgzUpload(DiskFileItem uploadedFile, String extension, HttpServletRequest request,
                                         RenderContext renderContext, Resource resource, JCRSessionWrapper session,
                                         Map<String, List<String>> formParameters) throws Exception {
        try (InputStream fileInputStream = new FileInputStream(uploadedFile.getStoreLocation());
             InputStream gzipInputStream = new GZIPInputStream(fileInputStream);
             TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream)) {

            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextEntry()) != null) {
                if (!entry.isFile() || !"package/package.json".equals(entry.getName())) {
                    continue;
                }
                if (entry.getSize() > MAX_TAR_ENTRY_SIZE_BYTES) {
                    logger.warn("CreateEntryFromJar: package.json entry too large ({} bytes) - rejected", entry.getSize());
                    return errorResult(session, ERR_UNABLE_READ_FILE);
                }
                final JSONObject jsonObject = new JSONObject(IOUtils.toString(tarInputStream, "UTF-8"));
                return createJavascriptModule(uploadedFile, jsonObject, request, renderContext, resource, session, extension, formParameters);
            }
            return errorResult(session, "forge.uploadJar.error.unable.read.manifest");
        } catch (IOException ex) {
            logger.error("Impossible to parse archive", ex);
            return errorResult(session, ERR_UNABLE_READ_FILE);
        } finally {
            uploadedFile.delete();
        }
    }

    private ActionResult handleJarUpload(DiskFileItem uploadedFile, String extension, HttpServletRequest request,
                                         RenderContext renderContext, Resource resource, JCRSessionWrapper session,
                                         Map<String, List<String>> formParameters) throws Exception {
        try (JarFile jar = new JarFile(uploadedFile.getStoreLocation())) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                return errorResult(session, "forge.uploadJar.error.unable.read.manifest");
            }
            Attributes attributes = manifest.getMainAttributes();
            if (attributes.getValue("Jahia-Package-Name") != null) {
                return createPackage(uploadedFile, jar, attributes, request, renderContext, resource, session, formParameters);
            }
            return createModule(uploadedFile, attributes, request, renderContext, resource, session, extension, formParameters);
        } catch (IOException ex) {
            logger.error("Impossible to parse archive", ex);
            return errorResult(session, ERR_UNABLE_READ_FILE);
        } finally {
            uploadedFile.delete();
        }
    }
    
    private ActionResult createJavascriptModule(DiskFileItem uploadedFile, JSONObject jsonObject, HttpServletRequest request, RenderContext renderContext, Resource resource, JCRSessionWrapper session, String extension, Map<String, List<String>> formParams) throws Exception {
        JCRNodeWrapper repository = resource.getNode();
        final String version = jsonObject.getString("version");
        final String moduleName = jsonObject.getString("name");
        final JSONObject jahiaJsonObject = jsonObject.getJSONObject("jahia");
        final JSONObject mavenJsonObject = jahiaJsonObject.getJSONObject("maven");
        final String groupId = mavenJsonObject.getString(GROUP_ID);
        final JCRSiteNode site = resource.getNode().getResolveSite();
        final Map<String, List<String>> moduleParams = new HashMap<>();
        moduleParams.put(MODULE_NAME, Arrays.asList(moduleName));
        moduleParams.put(GROUP_ID, Arrays.asList(groupId));
        moduleParams.put(Constants.JCR_TITLE, Arrays.asList(moduleName));
        moduleParams.put(VERSION_NUMBER, Arrays.asList(version));

        final String reqVersionAttribute = jahiaJsonObject.getString("required-version");
        final String requiredVersion = VERSION_PREFIX + reqVersionAttribute;
        if (StringUtils.isEmpty(moduleName) || StringUtils.isEmpty(groupId) || StringUtils.isEmpty(reqVersionAttribute)) {
            return errorResult(session, ERR_MISSING_MANIFEST_ATTRIBUTE);
        }
        final String moduleRelPath = groupId.replace(".", "/") + "/" + moduleName;
        moduleParams.put("url", Arrays.asList(buildMavenProxyUrl(request, site.getName(), moduleRelPath, version, moduleName, extension)));

        final JCRNodeWrapper versions = getJahiaVersion(requiredVersion, resource, session);
        moduleParams.put(REQUIRED_VERSION, Arrays.asList(versions.getNode(requiredVersion).getIdentifier()));

        final List<String> moduleParamKeys = Arrays.asList(DESCRIPTION, CATEGORY, "icon", AUTHOR_NAME_DISPLAYED_AS, AUTHOR_URL, AUTHOR_EMAIL, "FAQ", CODE_REPOSITORY, DOWNLOAD_COUNT, SUPPORTED_BY_JAHIA, REVIEWED_BY_JAHIA, PUBLISHED, DELETED, SCREENSHOTS, VIDEO, GROUP_ID);
        final List<String> versionParamKeys = Arrays.asList(REQUIRED_VERSION, VERSION_NUMBER, FILE_DSA_SIGNATURE, CHANGE_LOG, "url");
        final Map<String, List<String>> moduleParameters = new HashMap<>();
        final Map<String, List<String>> versionParameters = new HashMap<>();

        String title = getParameter(moduleParams, Constants.JCR_TITLE);
        if (StringUtils.isEmpty(title)) {
            title = moduleName;
        }
        moduleParameters.put(Constants.JCR_TITLE, Arrays.asList(title));
        versionParameters.put(Constants.JCR_TITLE, Arrays.asList(title));
        partitionParams(moduleParams, moduleParamKeys, versionParamKeys, moduleParameters, versionParameters);

        logger.info("Start creating Private App Store Javascript Module {}", moduleName);

        JCRNodeWrapper module = upsertModuleNode(request, repository, moduleRelPath, groupId, moduleName, moduleParameters);
        grantOwnerRole(session, module);

        boolean hasModuleVersions = JCRTagUtils.hasChildrenOfType(module, JNT_FORGEMODULEVERSION);
        logger.info("Start adding module version {} of {}", version, title);

        if (hasModuleVersions && !hasValidVersionNumber(module, version)) {
            String error = Messages.getWithArgs(RESOURCES_PRIVATEAPPSTORE, ERR_VERSION_NUMBER, session.getLocale(), moduleName, version);
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(ERROR, error));
        }

        JCRNodeWrapper moduleVersion = createNode(request, versionParameters, module, JNT_FORGEMODULEVERSION, module.getName() + "-" + version, false);

        String value = jahiaJsonObject.getString("module-dependencies");
        if (value != null) {
            moduleVersion.setProperty(REFERENCES, value.split(","));
        } else {
            moduleVersion.setProperty(REFERENCES, EMPTY_REFERENCES);
        }
        grantOwnerRole(session, moduleVersion);

        logger.info("Javascript Module version {} of {} successfully added", version, title);
        logger.info("Private App Store Javascript Module {} successfully created and added to repository {}", moduleName, repository.getPath());

        final String moduleUrl = renderContext.getResponse().encodeURL(module.getUrl());
        final String moduleAbsoluteUrl = module.getProvider().getAbsoluteContextPath(request) + moduleUrl;
        session.save();
        moduleVersion.uploadFile(uploadedFile.getName(), uploadedFile.getInputStream(), uploadedFile.getContentType());
        session.save();

        final ActionResult uploadResult = new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(SUCCESS_REDIRECT_URL, moduleUrl).put(
                SUCCESS_REDIRECT_ABSOLUTE_URL, moduleAbsoluteUrl));
        applySafeRedirect(uploadResult, formParams);

        return uploadResult;
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
        final String requiredVersion = VERSION_PREFIX + reqVersionAttribute;
        if (StringUtils.isEmpty(packageName) || StringUtils.isEmpty(reqVersionAttribute) || StringUtils.isEmpty(version)) {
            return errorResult(session, ERR_MISSING_MANIFEST_ATTRIBUTE);
        }
        JCRNodeWrapper versions = getJahiaVersion(requiredVersion, resource, session);
        packageParams.put(REQUIRED_VERSION, Arrays.asList(versions.getNode(requiredVersion).getIdentifier()));

        List<String> packageParamKeys = Arrays.asList(DESCRIPTION, CATEGORY, "icon", AUTHOR_NAME_DISPLAYED_AS, AUTHOR_URL, AUTHOR_EMAIL, "FAQ", DOWNLOAD_COUNT, SUPPORTED_BY_JAHIA, REVIEWED_BY_JAHIA, PUBLISHED, DELETED, SCREENSHOTS, VIDEO);
        List<String> versionParamKeys = Arrays.asList(REQUIRED_VERSION, VERSION_NUMBER, FILE_DSA_SIGNATURE, CHANGE_LOG);
        Map<String, List<String>> packageParameters = new HashMap<>();
        Map<String, List<String>> versionParameters = new HashMap<>();

        String title = getParameter(packageParams, Constants.JCR_TITLE);
        if (StringUtils.isEmpty(title)) {
            title = packageName;
        }
        packageParameters.put(Constants.JCR_TITLE, Arrays.asList(title));
        versionParameters.put(Constants.JCR_TITLE, Arrays.asList(title));
        partitionParams(packageParams, packageParamKeys, versionParamKeys, packageParameters, versionParameters);

        logger.info("Start creating Private App Store Package {}", packageName);

        modulesPackage = upsertPackageNode(request, repository, packageRelPath, packageName, packageParameters);
        grantOwnerRole(session, modulesPackage);

        boolean hasPackageVersions = JCRTagUtils.hasChildrenOfType(modulesPackage, JNT_FORGEPACKAGEVERSION);
        logger.info("Start adding package version {} of {}", version, title);

        if (hasPackageVersions && !hasValidVersionNumber(modulesPackage, version)) {
            String error = Messages.getWithArgs(RESOURCES_PRIVATEAPPSTORE, ERR_VERSION_NUMBER, session.getLocale(), packageName, version);
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(ERROR, error));
        }

        JCRNodeWrapper packageVersion = createNode(request, versionParameters, modulesPackage, JNT_FORGEPACKAGEVERSION, modulesPackage.getName() + "-" + version, false);
        grantOwnerRole(session, packageVersion);
        packageVersion.uploadFile(uploadedFile.getName(), uploadedFile.getInputStream(), uploadedFile.getContentType());

        logger.info("Package version {} of {} successfully added", version, title);

        populatePackageModulesList(packageVersion, jar);

        logger.info("Private App Store Package {} successfully created and added to repository {}", packageName, modulesPackage.getParent().getPath());

        String packageUrl = renderContext.getResponse().encodeURL(modulesPackage.getUrl());
        String packageAbsoluteUrl = modulesPackage.getProvider().getAbsoluteContextPath(request) + packageUrl;
        session.save();

        ActionResult uploadResult = new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(SUCCESS_REDIRECT_URL, packageUrl).put(
                SUCCESS_REDIRECT_ABSOLUTE_URL, packageAbsoluteUrl));
        applySafeRedirect(uploadResult, formParams);
        return uploadResult;
    }

    private JCRNodeWrapper upsertPackageNode(HttpServletRequest request, JCRNodeWrapper repositoryStart,
                                             String packageRelPath, String packageName,
                                             Map<String, List<String>> packageParameters) throws RepositoryException {
        if (!repositoryStart.hasNode(packageRelPath)) {
            JCRNodeWrapper packagesFolder = repositoryStart.hasNode(PACKAGES)
                    ? repositoryStart.getNode(PACKAGES)
                    : repositoryStart.addNode(PACKAGES, JNT_CONTENT_FOLDER);
            return createNode(request, packageParameters, packagesFolder, "jnt:forgePackage", packageName, false);
        }
        JCRNodeWrapper modulesPackage = repositoryStart.getNode(packageRelPath);
        packageParameters.remove(DESCRIPTION);
        packageParameters.remove(Constants.JCR_TITLE);
        setProperties(modulesPackage, packageParameters);
        return modulesPackage;
    }

    private static void populatePackageModulesList(JCRNodeWrapper packageVersion, JarFile jar) throws RepositoryException, IOException {
        JCRNodeWrapper modulesList = packageVersion.hasNode(MODULES_LIST)
                ? packageVersion.getNode(MODULES_LIST)
                : packageVersion.addNode(MODULES_LIST, "jnt:forgePackageModulesList");

        Map<String, ModulesPackage.PackagedModule> packagedModuleMap = ModulesPackage.create(jar).getModules();
        for (ModulesPackage.PackagedModule packagedModule : packagedModuleMap.values()) {
            String symbolicName = packagedModule.getManifestAttributes().getValue(BUNDLE_SYMBOLIC_NAME);
            JCRNodeWrapper module = modulesList.hasNode(symbolicName)
                    ? modulesList.getNode(symbolicName)
                    : modulesList.addNode(symbolicName, "jnt:forgePackageModule");
            module.setProperty(MODULE_NAME, packagedModule.getManifestAttributes().getValue("Implementation-Title"));
            module.setProperty("moduleVersion", packagedModule.getManifestAttributes().getValue("Implementation-Version"));
        }
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

        moduleParams.put(MODULE_NAME, Arrays.asList(moduleName));
        moduleParams.put(GROUP_ID, Arrays.asList(groupId));
        moduleParams.put(Constants.JCR_TITLE, Arrays.asList(attributes.getValue("Implementation-Title")));
        moduleParams.put(DESCRIPTION, Arrays.asList(attributes.getValue("Bundle-Description")));
        moduleParams.put(AUTHOR_URL, Arrays.asList(attributes.getValue("Implementation-URL")));
        moduleParams.put(CODE_REPOSITORY, Arrays.asList(attributes.getValue("Jahia-Source-Control-Connection")));
        moduleParams.put(VERSION_NUMBER, Arrays.asList(version));


        String reqVersionAttribute = attributes.getValue("Jahia-Required-Version");
        final String requiredVersion = VERSION_PREFIX + reqVersionAttribute;
        if (StringUtils.isEmpty(moduleName) || StringUtils.isEmpty(groupId) || StringUtils.isEmpty(reqVersionAttribute)) {
            return errorResult(session, ERR_MISSING_MANIFEST_ATTRIBUTE);
        }
        String moduleRelPath = groupId.replace(".", "/") + "/" + moduleName;
        moduleParams.put("url", Arrays.asList(buildMavenProxyUrl(request, site.getName(), moduleRelPath, version, moduleName, extension)));

        JCRNodeWrapper versions = getJahiaVersion(requiredVersion, resource, session);
        moduleParams.put(REQUIRED_VERSION, Arrays.asList(versions.getNode(requiredVersion).getIdentifier()));

        ActionResult deployFailure = deployArtifact(uploadedFile, site, extension, groupId, moduleName, forgeSettingsUrl, session);
        if (deployFailure != null) {
            return deployFailure;
        }

        List<String> moduleParamKeys = Arrays.asList(DESCRIPTION, CATEGORY, "icon", AUTHOR_NAME_DISPLAYED_AS, AUTHOR_URL, AUTHOR_EMAIL, "FAQ", CODE_REPOSITORY, DOWNLOAD_COUNT, SUPPORTED_BY_JAHIA, REVIEWED_BY_JAHIA, PUBLISHED, DELETED, SCREENSHOTS, VIDEO, GROUP_ID);
        List<String> versionParamKeys = Arrays.asList(REQUIRED_VERSION, VERSION_NUMBER, FILE_DSA_SIGNATURE, CHANGE_LOG, "url");
        Map<String, List<String>> moduleParameters = new HashMap<>();
        Map<String, List<String>> versionParameters = new HashMap<>();

        String title = getParameter(moduleParams, Constants.JCR_TITLE);
        if (StringUtils.isEmpty(title)) {
            title = moduleName;
        }
        moduleParameters.put(Constants.JCR_TITLE, Arrays.asList(title));
        versionParameters.put(Constants.JCR_TITLE, Arrays.asList(title));
        partitionParams(moduleParams, moduleParamKeys, versionParamKeys, moduleParameters, versionParameters);

        logger.info("Start creating Private App Store Module {}", moduleName);

        JCRNodeWrapper module = upsertModuleNode(request, repository, moduleRelPath, groupId, moduleName, moduleParameters);
        grantOwnerRole(session, module);

        boolean hasModuleVersions = JCRTagUtils.hasChildrenOfType(module, JNT_FORGEMODULEVERSION);
        logger.info("Start adding module version {} of {}", version, title);

        if (hasModuleVersions && !hasValidVersionNumber(module, version)) {
            String error = Messages.getWithArgs(RESOURCES_PRIVATEAPPSTORE, ERR_VERSION_NUMBER, session.getLocale(), moduleName, version);
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(ERROR, error));
        }

        JCRNodeWrapper moduleVersion = createNode(request, versionParameters, module, JNT_FORGEMODULEVERSION, module.getName() + "-" + version, false);

        String value = attributes.getValue("Jahia-Depends");
        if (value != null) {
            moduleVersion.setProperty(REFERENCES, value.split(","));
        } else {
            moduleVersion.setProperty(REFERENCES, EMPTY_REFERENCES);
        }
        grantOwnerRole(session, moduleVersion);

        logger.info("Module version {} of {} successfully added", version, title);
        logger.info("Private App Store Module {} successfully created and added to repository {}", moduleName, module.getParent().getPath());

        String moduleUrl = renderContext.getResponse().encodeURL(module.getUrl());
        String moduleAbsoluteUrl = module.getProvider().getAbsoluteContextPath(request) + moduleUrl;
        session.save();
        ActionResult uploadResult = new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(SUCCESS_REDIRECT_URL, moduleUrl).put(
                SUCCESS_REDIRECT_ABSOLUTE_URL, moduleAbsoluteUrl));
        applySafeRedirect(uploadResult, formParams);
        return uploadResult;
    }

    private ActionResult deployArtifact(DiskFileItem uploadedFile, JCRSiteNode site, String extension,
                                        String groupId, String moduleName, String forgeSettingsUrl,
                                        JCRSessionWrapper session) throws RepositoryException, JSONException {
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
            return null;
        } catch (IOException e) {
            return errorResult(session, "forge.uploadJar.error.cannot.upload");
        } finally {
            FileUtils.deleteQuietly(artifact);
        }
    }

    private static ActionResult errorResult(JCRSessionWrapper session, String messageKey) throws JSONException {
        String error = Messages.get(RESOURCES_PRIVATEAPPSTORE, messageKey, session.getLocale());
        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(ERROR, error));
    }

    private static void applySafeRedirect(ActionResult result, Map<String, List<String>> formParams) {
        if (!formParams.containsKey(REDIRECT_URL)) {
            return;
        }
        String redirectUrl = formParams.get(REDIRECT_URL).get(0);
        if (ActionSecurityUtils.isSafeRedirect(redirectUrl)) {
            result.setUrl(redirectUrl);
        } else if (logger.isWarnEnabled()) {
            logger.warn(LOG_UNSAFE_REDIRECT, ActionSecurityUtils.sanitizeForLog(redirectUrl));
        }
    }

    private static void grantOwnerRole(JCRSessionWrapper session, JCRNodeWrapper node) throws RepositoryException {
        if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            node.grantRoles("u:" + session.getUser().getUsername(), new HashSet<>(Arrays.asList(OWNER)));
        }
    }

    private static void partitionParams(Map<String, List<String>> source,
                                        List<String> moduleKeys, List<String> versionKeys,
                                        Map<String, List<String>> moduleTarget,
                                        Map<String, List<String>> versionTarget) {
        for (Map.Entry<String, List<String>> e : source.entrySet()) {
            String key = e.getKey();
            List<String> value = e.getValue();
            if (value.get(0) == null) {
                continue;
            }
            if (moduleKeys.contains(key)) {
                moduleTarget.put(key, value);
            } else if (versionKeys.contains(key)) {
                versionTarget.put(key, value);
            }
        }
    }

    /**
     * Build the artifact download URL served by the {@link org.jahia.modules.forge.proxy.MavenProxy}
     * servlet. The proxy lives under the Jahia context root at {@link #MAVENPROXY_PATH} (the OSGi
     * /modules context), so the URL is derived by stripping the render servlet path (/cms/...) off
     * the current request and appending the proxy path. Using /cms here (as the legacy code did)
     * would route the download to the render servlet and 404.
     */
    private static String buildMavenProxyUrl(HttpServletRequest request, String siteName, String moduleRelPath,
                                             String version, String moduleName, String extension) {
        String contextRoot = StringUtils.substringBefore(request.getRequestURL().toString(), "/cms");
        return contextRoot + MAVENPROXY_PATH + siteName + "/" + moduleRelPath + "/" + version + "/"
                + moduleName + "-" + version + "." + extension;
    }

    private JCRNodeWrapper upsertModuleNode(HttpServletRequest request, JCRNodeWrapper repositoryStart,
                                            String moduleRelPath, String groupId, String moduleName,
                                            Map<String, List<String>> moduleParameters) throws RepositoryException {
        if (!repositoryStart.hasNode(moduleRelPath)) {
            JCRNodeWrapper cursor = repositoryStart;
            for (String segment : groupId.split("\\.")) {
                if (cursor.hasNode(segment)) {
                    cursor = cursor.getNode(segment);
                } else {
                    cursor = cursor.addNode(segment, JNT_CONTENT_FOLDER);
                }
            }
            return createNode(request, moduleParameters, cursor, "jnt:forgeModule", moduleName, false);
        }
        JCRNodeWrapper module = repositoryStart.getNode(moduleRelPath);
        moduleParameters.remove(DESCRIPTION);
        moduleParameters.remove(Constants.JCR_TITLE);
        setProperties(module, moduleParameters);
        return module;
    }

    public void setMavenExecutable(String mavenExecutable) {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows") && !mavenExecutable.endsWith(".bat")) {
            mavenExecutable = mavenExecutable + ".bat";
        }
        this.mavenExecutable = mavenExecutable;
    }

    private JCRNodeWrapper getJahiaVersion(String requiredVersion, Resource resource, JCRSessionWrapper session) throws RepositoryException {
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
                    // XML-escape every injected value to prevent settings.xml injection
                    // (e.g. an attacker-controlled username/password breaking out into <mirror> elements).
                    w.write("<settings><servers><server><id>" + StringEscapeUtils.escapeXml(releaseInfo.getRepositoryId()) + "</id><username>");
                    w.write(StringEscapeUtils.escapeXml(releaseInfo.getUsername()));
                    w.write("</username><password>");
                    w.write(StringEscapeUtils.escapeXml(releaseInfo.getPassword()));
                    w.write("</password></server></servers></settings>");
                }
            }
            try (JarFile jar = new JarFile(generatedJar)) {
                pomFile = PomUtils.extractPomFromJar(jar, groupId, artifactId);
            }

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
