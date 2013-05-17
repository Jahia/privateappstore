package org.jahia.modules.forge.actions;

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
 * Date: 2013-05-17
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class DeleteModule extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(DeleteModule.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode();

        session.checkout(module);
        module.setProperty("deleted",true);
        session.save();

        return new ActionResult(HttpServletResponse.SC_OK, module.getParent().getPath(), new JSONObject());
    }
}