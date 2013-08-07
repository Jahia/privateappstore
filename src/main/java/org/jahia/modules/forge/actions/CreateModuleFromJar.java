package org.jahia.modules.forge.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.bin.ActionResult;
import org.jahia.bin.SystemAction;
import org.jahia.data.templates.ModuleReleaseInfo;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.tools.files.FileUpload;
import org.jahia.utils.PomUtils;
import org.jahia.utils.i18n.Messages;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Action to par a jar and produce an entry in module list
 */
public class CreateModuleFromJar extends SystemAction {

    private transient static Logger logger = LoggerFactory.getLogger(CreateModuleFromJar.class);
    private CreateModule createModule;
    private JahiaTemplateManagerService templateManagerService;

    @Override
    public ActionResult doExecuteAsSystem(HttpServletRequest request, RenderContext renderContext, JCRSessionWrapper session, Resource resource, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        Map<String, List<String>> moduleParams = new HashMap<String, List<String>>();
        final FileUpload fu = (FileUpload) request.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);
        DiskFileItem uploadedJar = fu.getFileItems().get("file");
        String filename = uploadedJar.getName();
        if (!StringUtils.contains(filename,"-SNAPSHOT.")) {
            String extension = StringUtils.substringAfterLast(filename, ".");
            if (!(StringUtils.equals(extension,"jar") || StringUtils.equals(extension,"war"))) {
                String error = Messages.get("resources.Jahia_Forge","forge.uploadJar.error.wrong.format",session.getLocale());
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
                    moduleName = attributes.getValue("Jahia-Root-Folder");
                    if (uploadedJar.getName().endsWith(".war")) {
                        moduleName = attributes.getValue("root-folder");
                    }
                    groupId = attributes.getValue("Jahia-GroupId");
                    File pomFile = templateManagerService.extractPomFromJar(jar,groupId);
                    Model pom = PomUtils.read(pomFile);
                    FileUtils.deleteQuietly(pomFile);

                    JCRSiteNode site = resource.getNode().getResolveSite();
                    String forgeSettingsUrl = site.getProperty("forgeSettingsUrl").getString();

                    moduleParams.put("moduleName", Arrays.asList(moduleName));
                    moduleParams.put("jcr:title", Arrays.asList(attributes.getValue("Implementation-Title")));
                    moduleParams.put("description", Arrays.asList(attributes.getValue("Bundle-Description")));
                    //moduleParams.put("authorNameDisplayedAs", Arrays.asList(attributes.getValue("Built-By")));
                    moduleParams.put("authorURL", Arrays.asList(attributes.getValue("Implementation-URL")));
                    //moduleParams.put("authorEmail", Arrays.asList(attributes.getValue("")));
                    moduleParams.put("codeRepository", Arrays.asList(attributes.getValue("Jahia-Source-Control-Connection")));
                    moduleParams.put("releaseType", Arrays.asList("hotfix"));
                    moduleParams.put("versionNumber", Arrays.asList(version));
                    String forgeUrl = StringUtils.substringBefore(request.getRequestURL().toString(), "/render");
                    moduleParams.put("url", Arrays.asList(forgeUrl + "/mavenproxy/" + site.getName() + "/" + pom.getGroupId().replace(".","/") + "/" + pom.getArtifactId() + "/" + pom.getVersion() + "/" + pom.getArtifactId() + "-" + pom.getVersion() + "." + extension));

                    final String requiredVersion = pom.getParent().getVersion();
                    JCRNodeWrapper versions = session.getNode(resource.getNode().getResolveSite().getPath() + "/contents/forge-modules-required-versions");
                    if (!versions.hasNode(requiredVersion)) {
                        versions.addNode(requiredVersion, "jnt:text").setProperty("text",requiredVersion);
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
                        moduleReleaseInfo.setCatalogUsername(user);
                        moduleReleaseInfo.setCatalogPassword(password);
                        templateManagerService.deployToMaven(moduleReleaseInfo, artifact);
                    } catch (IOException e) {
                        String error = Messages.get("resources.Jahia_Forge","forge.uploadJar.error.cannot.upload",session.getLocale());
                        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error", error));
                    } finally {
                        FileUtils.deleteQuietly(artifact);
                    }
                } else {
                    String error = Messages.get("resources.Jahia_Forge","forge.uploadJar.error.unable.read.manifest",session.getLocale());
                    return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error",error));
                }
            } catch (IOException e) {
                String error = Messages.get("resources.Jahia_Forge","forge.uploadJar.error.unable.read.file",session.getLocale());
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
            String error = Messages.get("resources.Jahia_Forge","forge.uploadJar.error.snapshot.not.allowed",session.getLocale());
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error", error));
        }


        // create module

        return createModule.doExecuteAsSystem(request, renderContext, session, resource, moduleParams, urlResolver);
    }

    public void setCreateModule(CreateModule createModule) {
        this.createModule = createModule;
    }

    public void setTemplateManagerService(JahiaTemplateManagerService templateManagerService) {
        this.templateManagerService = templateManagerService;
    }
}
