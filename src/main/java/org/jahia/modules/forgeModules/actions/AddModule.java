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
        String path = targetNode.getPath();
        JCRNodeWrapper newNode = createNode(req, parameters, jcrSessionWrapper.getNode(path), "comnt:module",moduleTitle, false);
        JCRNodeWrapper folderNode = newNode.addNode("files", "jnt:folder");
        JCRNodeWrapper jahiAppLicense = jcrSessionWrapper.getNodeByUUID(jahiAppLicenseUUID);

        final FileUpload fu = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);
        DiskFileItem screenshotFile1 = fu.getFileItems().get("screenshot1");
        DiskFileItem screenshotFile2 = fu.getFileItems().get("screenshot2");
        DiskFileItem screenshotFile3 = fu.getFileItems().get("screenshot3");
        DiskFileItem screenshotFile4 = fu.getFileItems().get("screenshot4");
        DiskFileItem iconFile = fu.getFileItems().get("iconFile");
        DiskFileItem promoImageFile = fu.getFileItems().get("promoImage");
        logger.info("Adding module !!!!!!!!");

        newNode.setProperty("license",jahiAppLicense);
        newNode.setProperty("quickDescription",quickDescription);
        newNode.setProperty("bigDescription",bigDescription);
        newNode.setProperty("authorName",authorName);
        newNode.setProperty("authorURL",authorURL);
        newNode.setProperty("authorEmail",authorEmail);
        newNode.setProperty("codeRepository",codeRepository);

        /*if (!folderNode.hasNode(moduleTitle)) {
            folderNode.checkout();
            folderNode = folderNode.addNode(moduleTitle, "jnt:folder");
        } else {
            folderNode = folderNode.getNode(moduleTitle);
        }            */

        JCRNodeWrapper screenshotNode1 = uploadModuleFile(folderNode,screenshotFile1);
        JCRNodeWrapper screenshotNode2 = uploadModuleFile(folderNode,screenshotFile2);
        JCRNodeWrapper screenshotNode3 = uploadModuleFile(folderNode,screenshotFile3);
        JCRNodeWrapper screenshotNode4 = uploadModuleFile(folderNode,screenshotFile4);
        JCRNodeWrapper iconFileNode = uploadModuleFile(folderNode,iconFile);
        JCRNodeWrapper promoImageFileNode = uploadModuleFile(folderNode,promoImageFile);

        newNode.setProperty("screenshot1",screenshotNode1);
        newNode.setProperty("screenshot1",screenshotNode1);
        newNode.setProperty("screenshot1",screenshotNode1);
        newNode.setProperty("screenshot2",screenshotNode2);
        newNode.setProperty("screenshot3",screenshotNode3);
        newNode.setProperty("screenshot4",screenshotNode4);
        newNode.setProperty("icon",iconFileNode);
        newNode.setProperty("promoImage",promoImageFileNode);
        jcrSessionWrapper.save();



        return new ActionResult(HttpServletResponse.SC_OK, newNode.getPath(), Render.serializeNodeToJSON(newNode));
    }

    private JCRNodeWrapper uploadModuleFile(JCRNodeWrapper targetFolder,DiskFileItem fileToUpload) throws IOException, RepositoryException {
        if (fileToUpload !=null)
            return targetFolder.uploadFile(fileToUpload.getName(), fileToUpload.getInputStream(), fileToUpload.getContentType());
        else
            return null;
    }

}