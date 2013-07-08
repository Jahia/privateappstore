package org.jahia.modules.forge.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.SystemAction;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.tools.files.FileUpload;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        JarFile jar = null;
        try {
            jar = new JarFile(uploadedJar.getStoreLocation());
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                Attributes attributes = manifest.getMainAttributes();
                String version = attributes.getValue("Implementation-Version");
                String moduleName = attributes.getValue("Jahia-Root-Folder");
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

        // deploy the artifact on nexus

        return result;


    }

    public void setCreateModule(CreateModule createModule) {
        this.createModule = createModule;
    }
}
