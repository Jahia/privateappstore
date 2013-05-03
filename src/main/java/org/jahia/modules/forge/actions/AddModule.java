package org.jahia.modules.forge.actions;

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
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author faissah
 */
public class AddModule extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(AddModule.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        String title = getParameter(parameters, "jcr:title");
        JCRNodeWrapper repository = resource.getNode();

        logger.info("Start adding module " + title + " into forge repository " + repository.getPath());

        String videoIdentifier = getParameter(parameters, "videoIdentifier");
        String categoryUUID = getParameter(parameters, "moduleCategory");
        String jahiAppLicenseUUID = getParameter(parameters, "jahiAppLicense");

        JCRNodeWrapper module = createNode(req, parameters, repository, "jnt:forgeModule", null, false);
        JCRNodeWrapper filesFolder = module.addNode("files", "jnt:folder");

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

        JCRNodeWrapper screenshots = module.addNode("screenshots", "jnt:forgeModuleScreenshotsList");
        String screenshotKey = "screenshot";

        for (Map.Entry<String, DiskFileItem> entry : fu.getFileItems().entrySet()) {

            DiskFileItem fileItem = entry.getValue();
            String propertyName = entry.getKey();

            if (fileItem != null) {

                JCRNodeWrapper targetNode;
                JCRNodeWrapper fileNode = filesFolder.uploadFile(fileItem.getName(), fileItem.getInputStream(),
                        fileItem.getContentType());

                if (propertyName.length() > screenshotKey.length() &&
                        propertyName.substring(0,screenshotKey.length()).equals(screenshotKey)){

                    targetNode = screenshots.addNode(propertyName, "jnt:forgeModuleScreenshot");
                    propertyName = screenshotKey;
                }
                else
                    targetNode = module;

                targetNode.setProperty(propertyName, fileNode);
            }
        }

        if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            List<String> roles = Arrays.asList("owner");
            module.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
        }

        session.save();

        logger.info("Module " + title + " successfully added into forge repository " + repository.getPath() );

        return new ActionResult(HttpServletResponse.SC_OK, module.getPath(), Render.serializeNodeToJSON(module));

    }

}