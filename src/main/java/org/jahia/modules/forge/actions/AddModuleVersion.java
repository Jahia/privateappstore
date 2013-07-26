package org.jahia.modules.forge.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.tika.Tika;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.bin.SystemAction;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.taglibs.jcr.node.JCRTagUtils;
import org.jahia.tools.files.FileUpload;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author faissah
 */
public class AddModuleVersion extends SystemAction {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(AddModuleVersion.class);

    @Override
    public ActionResult doExecuteAsSystem(HttpServletRequest req, RenderContext renderContext, JCRSessionWrapper session, Resource resource, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode();
        String moduleTitle = module.getPropertyAsString("jcr:title");
        String versionNumber = getParameter(parameters, "versionNumber");
        boolean hasModuleVersions = JCRTagUtils.hasChildrenOfType(module, "jnt:forgeModuleVersion");

        logger.info("Start adding module version {} of {}", versionNumber, moduleTitle);

        if (hasModuleVersions && !hasValidVersionNumber(module, versionNumber)) {
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error", "versionNumber"));
        }

        JCRNodeWrapper moduleVersion = createNode(req, parameters, module, "jnt:forgeModuleVersion", module.getName()+"-"+versionNumber, false);

        String activeVersion = getParameter(parameters, "activeVersion");

        if (!hasModuleVersions || (activeVersion != null && activeVersion.equals("on"))) {
            moduleVersion.setProperty("activeVersion", true);
            moduleVersion.setProperty("published", true);
        }

        if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            List<String> roles = Arrays.asList("owner");
            moduleVersion.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
        }

        session.save();

        logger.info("Module version {} of {} successfully added", versionNumber, moduleTitle);

        return new ActionResult(HttpServletResponse.SC_OK, module.getPath(), Render.serializeNodeToJSON(moduleVersion));

    }

    private boolean hasValidVersionNumber(JCRNodeWrapper module, String versionNumber) throws RepositoryException {

        if (versionNumber == null ) {
            return false;
        }

        List<JCRNodeWrapper> moduleVersions = JCRTagUtils.getChildrenOfType(module, "jnt:forgeModuleVersion");

        for (JCRNodeWrapper moduleVersion : moduleVersions) {
            if (moduleVersion.hasProperty("versionNumber") &&moduleVersion.getProperty("versionNumber").getString().equals(versionNumber))
                return false;
        }

        return true;
    }

}