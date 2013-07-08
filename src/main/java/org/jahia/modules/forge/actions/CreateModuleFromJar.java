package org.jahia.modules.forge.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.SystemAction;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.tools.files.FileUpload;
import org.json.JSONObject;
import org.mozilla.javascript.tools.idswitch.FileBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
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

    private CreateModule createModule;

    @Override
    public ActionResult doExecuteAsSystem(HttpServletRequest request, RenderContext renderContext, JCRSessionWrapper session, Resource resource,  Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        Map<String, List<String>> moduleParams = new HashMap<String, List<String>>();
        final FileUpload fu = (FileUpload) request.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);
        DiskFileItem uploadedJar = fu.getFileItems().get("file");
        String filename = uploadedJar.getName();
        String extension = StringUtils.substringAfterLast(filename,".");
        boolean isSnapshot = StringUtils.contains(filename,"SNAPSHOT");
        String moduleName;
        String version;
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
                moduleParams.put("moduleName", Arrays.asList(moduleName));
                moduleParams.put("jcr:title", Arrays.asList(attributes.getValue("Implementation-Title")));
                moduleParams.put("description", Arrays.asList(attributes.getValue("Bundle-Description")));
                //moduleParams.put("authorNameDisplayedAs", Arrays.asList(attributes.getValue("Built-By")));
                moduleParams.put("authorURL", Arrays.asList(attributes.getValue("Implementation-URL")));
                //moduleParams.put("authorEmail", Arrays.asList(attributes.getValue("")));
                moduleParams.put("codeRepository", Arrays.asList(attributes.getValue("Jahia-Source-Control-Connection")));
                moduleParams.put("releaseType", Arrays.asList("hotfix"));
                moduleParams.put("versionNumber", Arrays.asList(version));
                moduleParams.put("url", Arrays.asList("http://nexus/released/" + moduleName + "-" + version + ".jar"));
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

                JCRSiteNode site = resource.getNode().getResolveSite();

                String groupID = site.getProperty("forgeSettingsGroupID").getString();
                String url = site.getProperty("forgeSettingsUrl").getString() + "/service/local/artifact/maven/content";
                String user = site.getProperty("forgeSettingsUser").getString();
                String password = new String(Base64.decode(site.getProperty("forgeSettingsPassword").getString()));
                String repo = isSnapshot?site.getProperty("forgeSettingsSnapshotRepository").getString():site.getProperty("forgeSettingsReleaseRepository").getString();

                PostMethod postMethod = new PostMethod(url);
                postMethod.addRequestHeader("Authorization", "Basic " + Base64.encode((user + ":" + password).getBytes()));
                Part[] parts = {
                        new StringPart("e", extension),
                        new StringPart("g", groupID),
                        new StringPart("a", moduleName),
                        new StringPart("v", version),
                        new StringPart("p", extension),
                        new StringPart("r", repo),
                        new StringPart("hasPom","false"),
                        new FilePart(filename,uploadedJar.getStoreLocation())
                };
                postMethod.setRequestEntity(
                        new MultipartRequestEntity(parts, postMethod.getParams())
                );
                HttpClient client = new HttpClient();
                int status = client.executeMethod(postMethod);
                System.out.println("end of upload : " + status);
            } else {
                return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error", "cannotReadManifest"));
            }
        } catch (IOException e) {
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error", "notJarFile"));
        } finally {
            if (jar != null) {
                jar.close();
            }
            uploadedJar.delete();

        }




        // create module


        ActionResult result = createModule.doExecuteAsSystem(request, renderContext, session, resource, moduleParams, urlResolver);

        return result;


    }

    public void setCreateModule(CreateModule createModule) {
        this.createModule = createModule;
    }
}
