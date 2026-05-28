/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.forge.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.forge.tags.ForgeFunctions;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

import javax.jcr.NodeIterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Date: 2013-05-15
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
@Component(service = Action.class)
public class PublishModule extends Action {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(PublishModule.class);
    private static final String PUBLISHED = "published";

    @Activate
    public void activate() {
        setName("PublishModule");
        setRequireAuthenticatedUser(true);
        setRequiredMethods("POST");
    }

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode();
        session.checkout(module);

        String publish = getParameter(parameters, "publish");

        if (!CalculateCompletion.isPublishable(module) || publish == null || publish.isEmpty())
            return ActionResult.BAD_REQUEST;

        boolean published = publish.equals("true");
        module.setProperty(PUBLISHED, published);
        session.save();

        if (published) {
            ensureLatestVersionPublished(module, session);
        }

        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put(PUBLISHED, published));
    }

    private static void ensureLatestVersionPublished(JCRNodeWrapper module, JCRSessionWrapper session) throws javax.jcr.RepositoryException {
        NodeIterator ni = module.getNodes();
        List<JCRNodeWrapper> sortedVersions = ForgeFunctions.sortModulesByVersion(ni);
        if (sortedVersions.isEmpty()) {
            return;
        }
        for (JCRNodeWrapper version : sortedVersions) {
            if (version.isNodeType("jnt:forgeModuleVersion") && version.getProperty(PUBLISHED).getBoolean()) {
                return;
            }
        }
        sortedVersions.get(0).setProperty(PUBLISHED, true);
        session.save();
    }
}
