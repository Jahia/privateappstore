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
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author faissah
 */
public class    AddModuleRelease extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(AddModuleRelease.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode();

        String moduleTitle = module.getPropertyAsString("jcr:title");
        String version = getParameter(parameters, "version");

        logger.info("Start adding module release " + version + " of " + moduleTitle);

        JCRNodeWrapper filesFolder = module.getNode("files");
        JCRNodeWrapper moduleVersion = createNode(req, parameters, module, "comnt:moduleVersion", moduleTitle+"-"+version, false);

        String statusUUID = getParameter(parameters, "jahiAppStatus");
        String relatedJahiaVersionUUID = getParameter(parameters, "relatedJahiaVersion");
        String releaseType = getParameter(parameters, "releaseType");
        String activeVersion = getParameter(parameters, "activeVersion");

        if (statusUUID != null) {
            JCRNodeWrapper status = session.getNodeByUUID(statusUUID);
            moduleVersion.setProperty("status",status);
        }

        if (relatedJahiaVersionUUID != null) {
            JCRNodeWrapper relatedJahiaVersion = session.getNodeByUUID(relatedJahiaVersionUUID);
            moduleVersion.setProperty("relatedJahiaVersion",relatedJahiaVersion);
        }

        if (activeVersion.equals("true"))
            moduleVersion.setProperty("activeVersion", true);

        if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            List<String> roles = Arrays.asList("owner");
            moduleVersion.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
        }

        final FileUpload fu = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);

        DiskFileItem moduleBinary = fu.getFileItems().get("moduleBinary");

        JCRNodeWrapper fileNode = filesFolder.uploadFile(moduleBinary.getName(), moduleBinary.getInputStream(),
                moduleBinary.getContentType());

        moduleVersion.setProperty("moduleBinary", fileNode);

        /*
        for (Map.Entry<String, DiskFileItem> entry : fu.getFileItems().entrySet()) {

            DiskFileItem fileItem = entry.getValue();
            String propertyName = entry.getKey();

            if (fileItem != null) {

                JCRNodeWrapper targetNode = moduleVersion;
                JCRNodeWrapper fileNode = filesFolder.uploadFile(fileItem.getName(), fileItem.getInputStream(),
                        fileItem.getContentType());

                targetNode.setProperty(propertyName, fileNode);
            }
        }  */

        session.save();

        logger.info("Module release " + version + " of " + moduleTitle + " successfully added");

        // TODO
        return new ActionResult(HttpServletResponse.SC_OK, moduleVersion.getPath(), Render.serializeNodeToJSON(moduleVersion));

    }

}