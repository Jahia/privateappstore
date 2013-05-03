package org.jahia.modules.forge.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Date: 2013-04-04
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class DownloadCount  extends Action {

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode();

        session.checkout(module);

        module.setProperty("downloadCount", module.getProperty("downloadCount").getLong() + 1);

        session.save();

        return ActionResult.OK_JSON;

    }
}
