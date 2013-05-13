package org.jahia.modules.forge.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Date: 2013-05-07
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class AddVideo extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(AddVideo.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode();

        logger.info("Start adding video node to Forge module " + module.getPath());

        session.checkout(module);

        JCRNodeWrapper videoNode = createNode(req, parameters, module, "jnt:videostreaming", "video", false);
        String allowfullscreen = getParameter(parameters, "allowfullscreen");

        if (allowfullscreen != null)
            videoNode.setProperty("allowfullscreen", true);
        else
            videoNode.setProperty("allowfullscreen", false);

        session.save();

        logger.info("Video node successfully added to Forge module " + module.getPath());

        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject());
    }
}
