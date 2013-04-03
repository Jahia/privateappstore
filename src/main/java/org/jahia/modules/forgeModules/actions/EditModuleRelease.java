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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Date: 2013-03-27
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class EditModuleRelease extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(AddModule.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode().getParent();
        JCRNodeWrapper moduleVersion = resource.getNode();

        String moduleTitle = module.getPropertyAsString("jcr:title");
        String version = moduleVersion.getPropertyAsString("version");

        logger.info("Start updating module release " + version + " of " + moduleTitle);

        String statusUUID = getParameter(parameters, "jahiAppStatus");
        String relatedJahiaVersionUUID = getParameter(parameters, "relatedJahiaVersion");
        String releaseType = getParameter(parameters, "releaseType");
        String changeLog = getParameter(parameters, "changeLog");
        String activeVersion = getParameter(parameters, "activeVersion");

        session.checkout(moduleVersion);

        if (changeLog != null)
            moduleVersion.setProperty("changeLog", changeLog);

        if (releaseType != null)
            moduleVersion.setProperty("releaseType", releaseType);

        if (statusUUID != null) {
            JCRNodeWrapper status = session.getNodeByUUID(statusUUID);
            moduleVersion.setProperty("status",status);
        }

        if (activeVersion != null && activeVersion.equals("on"))
            moduleVersion.setProperty("activeVersion", true);
        else
            moduleVersion.setProperty("activeVersion", false);

        if (relatedJahiaVersionUUID != null) {
            JCRNodeWrapper relatedJahiaVersion = session.getNodeByUUID(relatedJahiaVersionUUID);
            moduleVersion.setProperty("relatedJahiaVersion",relatedJahiaVersion);
        }

        final FileUpload fu = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);

        DiskFileItem moduleBinary = fu.getFileItems().get("moduleBinary");

        if (moduleBinary != null)
            moduleVersion.uploadFile(moduleBinary.getName(), moduleBinary.getInputStream(), moduleBinary.getContentType());

        session.save();

        logger.info("Module release " + version + " of " + moduleTitle + " successfully updated");

        // TODO
        return new ActionResult(HttpServletResponse.SC_OK, moduleVersion.getPath(), Render.serializeNodeToJSON(moduleVersion));

    }

}
