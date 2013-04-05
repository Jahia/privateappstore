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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Date: 2013-04-04
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class AddReview extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(AddModule.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper node = resource.getNode();

        if (!node.isNodeType("jmix:reviews")) {
            session.checkout(node);
            node.addMixin("jmix:reviews");
            session.save();
        }

        String path = node.getPath() + "/reviews";
        List<String> rating = new ArrayList<String>();

        rating.add(getParameter(parameters, "j:lastVote"));
        parameters.put("rating", rating);

        JCRNodeWrapper review = createNode(req, parameters, session.getNode(path), "jnt:review", null, false);

        session.save();

        return new ActionResult(HttpServletResponse.SC_OK, review.getPath(), Render.serializeNodeToJSON(review));

    }
}
