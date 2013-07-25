package org.jahia.modules.forge.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.model.Model;
import org.apache.xerces.impl.dv.util.Base64;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Jahia;
import org.jahia.bin.SystemAction;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.tools.files.FileUpload;
import org.jahia.utils.PomUtils;
import org.jahia.utils.i18n.Messages;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Action to par a jar and produce an entry in module list
 */
public class CreateModuleFromJar extends SystemAction {

    private transient static Logger logger = LoggerFactory.getLogger(CreateModuleFromJar.class);
    private CreateModule createModule;

    @Override
    public ActionResult doExecuteAsSystem(HttpServletRequest request, RenderContext renderContext, JCRSessionWrapper session, Resource resource, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        Map<String, List<String>> moduleParams = new HashMap<String, List<String>>();
        final FileUpload fu = (FileUpload) request.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);
        DiskFileItem uploadedJar = fu.getFileItems().get("file");
        String filename = uploadedJar.getName();
        String extension = StringUtils.substringAfterLast(filename, ".");
        if (!(StringUtils.equals(extension,"jar") || StringUtils.equals(extension,"war"))) {
            String error = Messages.get("resources.Jahia_Forge","forge.uploadJar.error.wrong.format",session.getLocale());
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error",error));
        }
        String moduleName;
        String version;
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
                moduleParams.put("activeVersion", Arrays.asList("true"));

                String url = StringUtils.substringBeforeLast(forgeSettingsUrl,"/content/repositories/") + "/service/local/artifact/maven/content";
                String user = site.getProperty("forgeSettingsUser").getString();
                String password = new String(Base64.decode(site.getProperty("forgeSettingsPassword").getString()));

                PostMethod postMethod = new PostMethod(url);
                postMethod.addRequestHeader("Authorization", "Basic " + Base64.encode((user + ":" + password).getBytes()));

                Enumeration<JarEntry>  jarEntries = jar.entries();
                JarEntry jarEntry = null;
                while (jarEntries.hasMoreElements()) {
                    jarEntry = jarEntries.nextElement();
                    String name = jarEntry.getName();
                    if (StringUtils.startsWith(name,"META-INF/maven/") && StringUtils.endsWith(name,"/pom.xml")) {
                        break;
                    }
                }
                InputStream is = jar.getInputStream(jarEntry);
                File pomFile = File.createTempFile("pom",".xml");
                FileUtils.copyInputStreamToFile(is,pomFile);

                File settings = File.createTempFile("settings",".xml");
                BufferedWriter w = new BufferedWriter(new FileWriter(settings));
                w.write("<settings><servers><server><id>remote-repository</id><username>");
                w.write(user);
                w.write("</username><password>");
                w.write(password);
                w.write("</password></server></servers></settings>");
                w.close();

                File artifact = File.createTempFile("artifact", "." + extension);
                FileUtils.copyFile(uploadedJar.getStoreLocation(), artifact);

                try {
                    Model pom = PomUtils.read(pomFile);

                    MavenCli cli = new MavenCli(new ClassWorld("plexus.core", Jahia.class.getClassLoader()));

                    String[] archetypeParams = {"deploy:deploy-file",
                            "-Dfile="+artifact.getPath(),
                            "-DrepositoryId=remote-repository",
                            "-Durl=" + forgeSettingsUrl,
                            "-DpomFile=" + pomFile.getPath(),
                            "-DgroupId=" + pom.getGroupId(),
                            "-DartifactId=" + pom.getArtifactId(),
                            "-Dversion=" + pom.getVersion(),
                            "--settings",settings.getPath()};

                    int ret = cli.doMain(archetypeParams, uploadedJar.getStoreLocation().getParent(),
                            System.out, System.err);
                    if (ret > 0) {
                        logger.error("Maven archetype call returned " + ret);
                        String error = Messages.get("resources.Jahia_Forge","forge.uploadJar.error.cannot.upload",session.getLocale());
                        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error", error));
                    }

                    moduleParams.put("url", Arrays.asList(forgeSettingsUrl + "/" + pom.getGroupId().replace(".","/") + "/" + pom.getArtifactId() + "/" + pom.getVersion() + "/" + pom.getArtifactId() + "-" + pom.getVersion() + "." + extension));
                } finally {
                    FileUtils.deleteQuietly(pomFile);
                    FileUtils.deleteQuietly(settings);
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


        // create module

        return createModule.doExecuteAsSystem(request, renderContext, session, resource, moduleParams, urlResolver);
    }

    public void setCreateModule(CreateModule createModule) {
        this.createModule = createModule;
    }
}
