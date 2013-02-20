package org.jahia.modules.forgeModules.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.tools.files.FileUpload;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author faissah
 */
public class AddModuleRelease extends Action {
    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(AddModuleRelease.class);
    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        String moduleReleaseTitle = getParameter(parameters, "title");
        String desc = getParameter(parameters, "desc");
        String statusUUID = getParameter(parameters, "jahiAppStatus");
        String relatedJahiaVersionUUID = getParameter(parameters, "relatedJahiaVersion");
        String versionNum = getParameter(parameters, "version");
        String releaseType = getParameter(parameters, "releaseType");

        JCRSessionWrapper jcrSessionWrapper = resource.getNode().getSession();
        JCRNodeWrapper node = resource.getNode();
        String path = node.getPath();
        JCRNodeWrapper folderNode = node.getNode("files");
        final FileUpload fu = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);
        DiskFileItem binaryFile = fu.getFileItems().get("binaryFile");
        logger.info("Adding module !!!!!!!!");


        if (!folderNode.hasNode(moduleReleaseTitle)) {
            folderNode.checkout();
            folderNode = folderNode.addNode(moduleReleaseTitle, "jnt:folder");
        } else {
            folderNode = folderNode.getNode(moduleReleaseTitle);
        }

        JCRNodeWrapper moduleVersion = node.addNode("version"+versionNum,"comnt:moduleVersion");
        uploadAndSetModuleFile(moduleVersion,folderNode,binaryFile,"moduleBinary");
        if (moduleReleaseTitle!=null)
        moduleVersion.setProperty("jcr:title",moduleReleaseTitle);
        if (versionNum!=null)
            moduleVersion.setProperty("version",versionNum);
        if (releaseType!=null)
            moduleVersion.setProperty("releaseType",releaseType);
        if (desc!=null)
            moduleVersion.setProperty("desc",desc);
        moduleVersion.setProperty("date", moduleVersion.getProperty("jcr:created").getDate());

        if(statusUUID!=null){
            JCRNodeWrapper status = jcrSessionWrapper.getNodeByUUID(statusUUID);
            moduleVersion.setProperty("status",status);
        }

        if(relatedJahiaVersionUUID!=null){
            JCRNodeWrapper relatedJahiaVersion = jcrSessionWrapper.getNodeByUUID(relatedJahiaVersionUUID);
            moduleVersion.setProperty("relatedJahiaVersion",relatedJahiaVersion);
        }

        jcrSessionWrapper.save();

        return new ActionResult(HttpServletResponse.SC_OK, node.getPath(), Render.serializeNodeToJSON(node));
    }

    private void uploadAndSetModuleFile(JCRNodeWrapper targetNode, JCRNodeWrapper targetFolder,DiskFileItem fileToUpload,  String propertieName) throws IOException, RepositoryException {
        if (fileToUpload !=null){
            JCRNodeWrapper fileNode = targetFolder.uploadFile(fileToUpload.getName(), fileToUpload.getInputStream(), fileToUpload.getContentType());
            targetNode.setProperty(propertieName,fileNode);
        }
    }

}