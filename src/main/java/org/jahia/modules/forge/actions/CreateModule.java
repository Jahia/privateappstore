package org.jahia.modules.forge.actions;

import org.apache.jackrabbit.spi.commons.query.sql.JCRSQLQueryBuilder;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.query.QueryResultWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.jcr.JCRUser;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
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

        QueryManager queryManager = session.getWorkspace().getQueryManager();

        StringBuilder sb = new StringBuilder("SELECT * FROM [jnt:forgeModule] as module");
        sb.append(" WHERE isdescendantnode(module, ['").append(repository.getPath()).append("'])");
        sb.append(" AND module.[jcr:title] = \"").append(title).append("\"");

        String query = sb.toString();
        Query q = queryManager.createQuery(query, Query.JCR_SQL2);
        QueryResultWrapper queryResult = (QueryResultWrapper) q.execute();

        boolean isValidTitle = queryResult.getNodes().getSize() == 0;

        if (!isValidTitle)
            return new ActionResult(HttpServletResponse.SC_OK, renderContext.getMainResource().getNode().getPath(), new JSONObject().put("error", "titleAlreadyUsed"));

        JCRNodeWrapper module = createNode(req, parameters, repository, "jnt:forgeModule", title, false);

        if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            List<String> roles = Arrays.asList("owner");
            module.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
        }

        session.save();

        logger.info("Forge Module " + title + " successfully created and added to forge repository " + repository.getPath());

        return new ActionResult(HttpServletResponse.SC_OK, module.getPath(), new JSONObject().put("moduleUrl", module.getUrl()));
    }
}
