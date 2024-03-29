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
 * Date: 2013-05-08
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class EditVideo extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(EditVideo.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode();

        logger.info("Start editing video node of Private App Store module " + module.getPath());

        session.checkout(module);

        JCRNodeWrapper videoNode = module.getNode("video");

        String provider = getParameter(parameters, "provider");
        String identifier = getParameter(parameters, "identifier");
        String width = getParameter(parameters, "width");
        String height = getParameter(parameters, "height");
        String allowfullscreen = getParameter(parameters, "allowfullscreen");

        if (provider != null)
            videoNode.setProperty("provider", provider);
        if (identifier != null)
            videoNode.setProperty("identifier", identifier);
        if (width != null)
            videoNode.setProperty("width", width);
        if (height != null)
            videoNode.setProperty("height", height);

        if (allowfullscreen != null && allowfullscreen.equals("on"))
            videoNode.setProperty("allowfullscreen", true);
        else
            videoNode.setProperty("allowfullscreen", false);

        session.save();

        logger.info("Video node successfully edited in Private App Store module " + module.getPath());

        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject());
    }
}
