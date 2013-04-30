package org.jahia.modules.forgeModules.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
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
 * Date: 2013-04-24
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class DeleteReview extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(DeleteReview.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module;
        JCRNodeWrapper node = resource.getNode();
        String nodePath = node.getPath();
        boolean isReview = node.isNodeType("jnt:review");

        logger.info("Starting to delete reported review " + (!isReview ? " reply " : "") + nodePath);

        if (isReview) { // then node is a review

            module = node.getParent().getParent();

            session.checkout(module);

            long reviewRating = node.getProperty("rating").getLong();

            module.setProperty("j:sumOfVotes", module.getProperty("j:sumOfVotes").getLong() - reviewRating);
            module.setProperty("j:nbOfVotes", module.getProperty("j:nbOfVotes").getLong() - 1);

            node.remove();

        }
        else {  // then node is a reply to a review

            module = node.getParent().getParent().getParent();

            JCRNodeWrapper review = node.getParent();

            session.checkout(review);

            node.remove();
        }

        session.save();

        logger.info("Reported review " + (!isReview ? " reply " : "") + nodePath + " successfully deleted");

        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("moduleUrl", module.getUrl()));
    }
}