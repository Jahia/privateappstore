package org.jahia.modules.forge.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.bin.ActionResult;
import org.jahia.bin.SystemAction;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.JahiaService;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.settings.SettingsBean;
import org.jahia.tools.files.FileUpload;
import org.jahia.utils.i18n.Messages;
import org.json.JSONException;
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
        File f = null;
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
                moduleParams.put("published", Arrays.asList("true"));

                // deploy the artifact on nexus
                /**
                 * Use rest API of nexus with arguments :
                 * r - repository
                 hasPom - whether you are supplying the pom or you want one generated. If generated g, a, v, p, and c are not needed
                 e - extension
                 g - group id
                 a - artifact id
                 v - version
                 p - packaging
                 c - classifier (optional, not shown in examples above)
                 file - the file(s) to be uploaded. These need to come last, and if there is a pom file it should come before the artifact

                 */

                String url = StringUtils.substringBeforeLast(forgeSettingsUrl,"/content/repositories/") + "/service/local/artifact/maven/content";
                String user = site.getProperty("forgeSettingsUser").getString();
                String password = new String(Base64.decode(site.getProperty("forgeSettingsPassword").getString()));
                String repo = site.getProperty("forgeSettingsReleaseRepository").getString();

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
                String path = StringUtils.substringBeforeLast(uploadedJar.getStoreLocation().getPath(),"/");
                String pomFilename = StringUtils.substringAfterLast(uploadedJar.getStoreLocation().getPath(),"/") + ".xml";

                f = new File(path,pomFilename);
                out=new FileOutputStream(f);
                byte buf[]=new byte[1024];
                int len;
                while((len=is.read(buf))>0)
                    out.write(buf,0,len);
                is.close();


                Part[] parts = {
                        new StringPart("e", extension),
                        new StringPart("r", repo),
                        new StringPart("hasPom", "true"),
                        new FilePart("pom.xml","pom.xml",f),
                        new FilePart(filename, uploadedJar.getStoreLocation())
                };
                postMethod.setRequestEntity(
                        new MultipartRequestEntity(parts, postMethod.getParams())
                );
                HttpClient client = new HttpClient();
                int status = client.executeMethod(postMethod);
                logger.info("end of upload : " + status);
                try {
                    JSONObject json = new JSONObject(postMethod.getResponseBodyAsString());
                    moduleParams.put("url", Arrays.asList(forgeSettingsUrl + "/" + StringUtils.replace(json.getString("groupId"),".", "/") + "/" + moduleName + "/" + version + "/" + moduleName + "-" + version + ".jar"));
                } catch (JSONException e) {
                    logger.error("error during parsing of json : " + postMethod.getResponseBodyAsString());
                    String error = Messages.get("resources.Jahia_Forge","forge.uploadJar.error.cannot.upload",session.getLocale());
                    return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error", error));
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

            if (f != null && f.exists()) {
                f.delete();
            }

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
