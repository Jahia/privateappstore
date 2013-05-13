package org.jahia.modules.forge.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.jcr.JCRUser;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Date: 2013-05-03
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class CreateModule extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(CreateModule.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        String title = getParameter(parameters, "jcr:title");
        JCRNodeWrapper repository = resource.getNode();

        logger.info("Start creating Forge Module " + title);

        JCRNodeWrapper module = createNode(req, parameters, repository, "jnt:forgeModule", title, false);
        session.save();

        logger.info("Forge Module " + title + " successfully created and added to forge repository " + repository.getPath());

        return new ActionResult(HttpServletResponse.SC_OK, module.getPath(), Render.serializeNodeToJSON(module));
    }
}
