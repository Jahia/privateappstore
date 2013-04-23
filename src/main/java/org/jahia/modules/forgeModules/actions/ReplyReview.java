package org.jahia.modules.forgeModules.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Date: 2013-04-22
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class ReplyReview extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(AddModule.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper review = resource.getNode();
        JCRNodeWrapper module = review.getParent().getParent();

        session.checkout(review);

        JCRNodeWrapper respond = createNode(req, parameters, review, "jnt:post", null, false);

        session.save();

        return new ActionResult(HttpServletResponse.SC_OK, module.getPath(), Render.serializeNodeToJSON(respond));
    }
}
