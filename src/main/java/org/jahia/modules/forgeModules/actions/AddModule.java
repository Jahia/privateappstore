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
public class AddModule extends Action {
    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(AddModule.class);
    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        String moduleTitle = getParameter(parameters, "title");
        String quickDescription = getParameter(parameters, "quickDescription");
        String bigDescription = getParameter(parameters, "bigDescription");
        String authorName = getParameter(parameters, "authorName");
        String authorURL = getParameter(parameters, "authorURL");
        String authorEmail = getParameter(parameters, "authorEmail");
        String codeRepository = getParameter(parameters, "codeRepository");
        String jahiAppLicenseUUID = getParameter(parameters, "jahiAppLicense");
        JCRSessionWrapper jcrSessionWrapper = resource.getNode().getSession();
        JCRNodeWrapper targetNode = resource.getNode();
        JCRNodeWrapper newNode = null;
        JCRNodeWrapper folderNode = null;

        if(targetNode.isNodeType("comnt:module")){
            newNode = targetNode;
            folderNode = newNode.getNode("files");
        }else{
            String path = targetNode.getPath();
            newNode = createNode(req, parameters, jcrSessionWrapper.getNode(path), "comnt:module",moduleTitle, false);
            folderNode = newNode.addNode("files", "jnt:folder");
        }

        if (jahiAppLicenseUUID!=null){
            JCRNodeWrapper jahiAppLicense = jcrSessionWrapper.getNodeByUUID(jahiAppLicenseUUID);
            newNode.setProperty("license",jahiAppLicense);
        }

        final FileUpload fu = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);

        logger.info("Adding module !!!!!!!!");

        if (moduleTitle!=null)
            newNode.setProperty("jcr:title",moduleTitle);
        if (quickDescription!=null)
            newNode.setProperty("quickDescription",quickDescription);
        if (bigDescription!=null)
            newNode.setProperty("bigDescription",bigDescription);
        if (authorName!=null)
            newNode.setProperty("authorName",authorName);
        if (authorURL!=null)
            newNode.setProperty("authorURL",authorURL);
        if (authorEmail!=null)
            newNode.setProperty("authorEmail",authorEmail);
        if (codeRepository!=null)
            newNode.setProperty("codeRepository",codeRepository);

        /*if (!folderNode.hasNode(moduleTitle)) {
            folderNode.checkout();
            folderNode = folderNode.addNode(moduleTitle, "jnt:folder");
        } else {
            folderNode = folderNode.getNode(moduleTitle);
        }            */
        DiskFileItem screenshotFile1 = fu.getFileItems().get("screenshot1");
        DiskFileItem screenshotFile2 = fu.getFileItems().get("screenshot2");
        DiskFileItem screenshotFile3 = fu.getFileItems().get("screenshot3");
        DiskFileItem screenshotFile4 = fu.getFileItems().get("screenshot4");
        DiskFileItem iconFile = fu.getFileItems().get("iconFile");
        DiskFileItem promoImageFile = fu.getFileItems().get("promoImage");

        uploadAndSetModuleFile(newNode,folderNode,screenshotFile1,"screenshot1");
        uploadAndSetModuleFile(newNode,folderNode,screenshotFile2,"screenshot2");
        uploadAndSetModuleFile(newNode,folderNode,screenshotFile3,"screenshot3");
        uploadAndSetModuleFile(newNode,folderNode,screenshotFile4,"screenshot4");
        uploadAndSetModuleFile(newNode,folderNode,iconFile,"icon");
        uploadAndSetModuleFile(newNode,folderNode,promoImageFile,"promoImage");
        jcrSessionWrapper.save();



        return new ActionResult(HttpServletResponse.SC_OK, newNode.getPath(), Render.serializeNodeToJSON(newNode));
    }

    private void uploadAndSetModuleFile(JCRNodeWrapper targetNode, JCRNodeWrapper targetFolder,DiskFileItem fileToUpload,  String propertieName) throws IOException, RepositoryException {
        if (fileToUpload !=null){
            JCRNodeWrapper fileNode = targetFolder.uploadFile(fileToUpload.getName(), fileToUpload.getInputStream(), fileToUpload.getContentType());
            targetNode.setProperty(propertieName,fileNode);
        }
    }

}