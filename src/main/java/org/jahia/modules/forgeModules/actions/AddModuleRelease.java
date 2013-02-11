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
        String status = getParameter(parameters, "status");
        String versionNum = getParameter(parameters, "version");

        JCRSessionWrapper jcrSessionWrapper = resource.getNode().getSession();
        JCRNodeWrapper node = resource.getNode();
        JCRNodeWrapper folderNode = jcrSessionWrapper.getNode(renderContext.getSite().getPath() + "/files/modules/"+node.getName());
        final FileUpload fu = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);
        DiskFileItem binaryFile = fu.getFileItems().get("binaryFile");
        logger.info("Adding module !!!!!!!!");
        String path = node.getPath();

        if (!folderNode.hasNode(moduleReleaseTitle)) {
            folderNode.checkout();
            folderNode = folderNode.addNode(moduleReleaseTitle, "jnt:folder");
        } else {
            folderNode = folderNode.getNode(moduleReleaseTitle);
        }
        JCRNodeWrapper binaryFileNode = uploadModuleFile(folderNode,binaryFile);

        JCRNodeWrapper moduleVersion = node.addNode("version"+versionNum,"comnt:moduleVersion");
        moduleVersion.setProperty("title",moduleReleaseTitle);
        moduleVersion.setProperty("version",versionNum);
        moduleVersion.setProperty("status",status);
        moduleVersion.setProperty("desc",desc);
        moduleVersion.setProperty("moduleBinary",binaryFileNode);
        jcrSessionWrapper.save();



        return new ActionResult(HttpServletResponse.SC_OK, node.getPath(), Render.serializeNodeToJSON(node));
    }

    private JCRNodeWrapper uploadModuleFile(JCRNodeWrapper targetFolder,DiskFileItem fileToUpload) throws IOException, RepositoryException {
        if (fileToUpload !=null)
            return targetFolder.uploadFile(fileToUpload.getName(), fileToUpload.getInputStream(), fileToUpload.getContentType());
        else
            return null;
    }

}