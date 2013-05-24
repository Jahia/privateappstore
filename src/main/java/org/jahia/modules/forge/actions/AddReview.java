package org.jahia.modules.forge.actions;

import org.jahia.api.Constants;
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
import java.util.*;

/**
 * Date: 2013-04-04
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class AddReview extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(AddReview.class);

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

        if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            List<String> roles = Arrays.asList("owner");
            review.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
        }

        session.save();

        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("moduleUrl", node.getUrl()));

    }
}