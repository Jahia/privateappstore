package org.jahia.modules.forgeModules.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.tools.files.FileUpload;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Date: 2013-03-20
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class EditModule extends Action
{
    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {


        String title = getParameter(parameters, "jcr:title");
        String quickDescription = getParameter(parameters, "quickDescription");
        String bigDescription = getParameter(parameters, "bigDescription");
        String categoryUUID = getParameter(parameters, "moduleCategory");
        String videoIdentifier = getParameter(parameters, "videoIdentifier");
        String authorName = getParameter(parameters, "authorName");
        String authorURL = getParameter(parameters, "authorURL");
        String authorEmail = getParameter(parameters, "authorEmail");
        String codeRepository = getParameter(parameters, "codeRepository");
        String jahiAppLicenseUUID = getParameter(parameters, "jahiAppLicense");
        String reviewedByJahia = getParameter(parameters, "reviewedByJahia");
        String supportedByJahia = getParameter(parameters, "supportedByJahia");

        JCRNodeWrapper module = resource.getNode();
        JCRNodeWrapper filesFolder = module.getNode("files");

        session.checkout(module);

        if (title != null)
            module.setProperty("jcr:title", title);
        if (quickDescription != null)
            module.setProperty("quickDescription", quickDescription);
        if (bigDescription != null)
            module.setProperty("bigDescription", bigDescription);
        if (authorName != null)
            module.setProperty("authorName", authorName);
        if (authorURL != null)
            module.setProperty("authorURL", authorURL);
        if (authorEmail != null)
            module.setProperty("authorEmail", authorEmail);
        if (codeRepository != null)
            module.setProperty("codeRepository", codeRepository);
        if (reviewedByJahia != null)
            module.setProperty("reviewedByJahia", reviewedByJahia);
        else
            module.setProperty("reviewedByJahia", false);
        if (supportedByJahia != null)
            module.setProperty("supportedByJahia", supportedByJahia);
        else
            module.setProperty("supportedByJahia", false);

        if (videoIdentifier != null) {

            String videoProvider = getParameter(parameters, "videoProvider");
            String videoWidth = getParameter(parameters, "videoWidth");
            String videoHeight = getParameter(parameters, "videoHeight");
            String videoAllowfullscreen = getParameter(parameters, "videoAllowfullscreen");

            JCRNodeWrapper videoNode = module.addNode("video", "jnt:videostreaming");

            videoNode.setProperty("identifier",videoIdentifier);
            if (videoProvider != null)
                videoNode.setProperty("provider",videoProvider);
            if (videoWidth != null)
                videoNode.setProperty("width",videoWidth);
            if (videoHeight != null)
                videoNode.setProperty("height",videoHeight);
            if (videoAllowfullscreen != null)
                videoNode.setProperty("allowfullscreen",videoAllowfullscreen);
        }

        if (categoryUUID != null) {
            JCRNodeWrapper category = session.getNodeByUUID(categoryUUID);
            module.setProperty("category", category);
        }

        if (jahiAppLicenseUUID != null) {
            JCRNodeWrapper jahiAppLicense = session.getNodeByUUID(jahiAppLicenseUUID);
            module.setProperty("license", jahiAppLicense);
        }


        final FileUpload fu = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);

        DiskFileItem screenshotFile1 = fu.getFileItems().get("screenshot1");
        DiskFileItem screenshotFile2 = fu.getFileItems().get("screenshot2");
        DiskFileItem screenshotFile3 = fu.getFileItems().get("screenshot3");
        DiskFileItem screenshotFile4 = fu.getFileItems().get("screenshot4");
        DiskFileItem iconFile = fu.getFileItems().get("iconFile");
        DiskFileItem promoImageFile = fu.getFileItems().get("promoImage");

        uploadAndSetModuleFile(module, filesFolder, screenshotFile1, "screenshot1");
        uploadAndSetModuleFile(module, filesFolder, screenshotFile2, "screenshot2");
        uploadAndSetModuleFile(module, filesFolder, screenshotFile3, "screenshot3");
        uploadAndSetModuleFile(module, filesFolder, screenshotFile4, "screenshot4");
        uploadAndSetModuleFile(module, filesFolder, iconFile, "icon");
        uploadAndSetModuleFile(module, filesFolder, promoImageFile, "promoImage");

        if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            List<String> roles = Arrays.asList("owner");
            module.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
        }

        session.save();

        return new ActionResult(HttpServletResponse.SC_OK, module.getPath(), Render.serializeNodeToJSON(module));

    }

    private void uploadAndSetModuleFile(JCRNodeWrapper targetNode, JCRNodeWrapper targetFolder,
                                        DiskFileItem fileToUpload,  String propertyName) throws IOException, RepositoryException {

        if (fileToUpload !=null) {
            JCRNodeWrapper fileNode = targetFolder.uploadFile(fileToUpload.getName(), fileToUpload.getInputStream(), fileToUpload.getContentType());
            targetNode.setProperty(propertyName,fileNode);
        }
    }
}
