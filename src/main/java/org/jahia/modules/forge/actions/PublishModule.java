/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Enterprise Distribution                  =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group. All rights reserved.
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTION - IMPORTANT LICENSING INFORMATION
 * ===============================================================
 *
 *     This file is part of a Jahia Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance
 *     with the terms contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     See the license for the rights, obligations and limitations governing use
 *     of the contents of the software.
 *
 *     For questions regarding licensing, support, production usage,
 *     please contact our team at sales@jahia.com or go to: http://www.jahia.com/license
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
public class PublishModule  extends PrivateAppStoreAction {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(PublishModule.class);

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
        module.setProperty("published", published);
        session.save();

        // if no versions is published, publish the last one
        if (published) {
            NodeIterator ni = module.getNodes();
            List<JCRNodeWrapper> sortedVersions = ForgeFunctions.sortModulesByVersion(ni);
            if (!sortedVersions.isEmpty()) {
                boolean hasPublishedVersion = false;
                for (JCRNodeWrapper version : sortedVersions) {
                    if (version.isNodeType("jnt:forgeModuleVersion") && version.getProperty("published").getBoolean()) {
                        hasPublishedVersion = true;
                        break;
                    }
                }
                if (!hasPublishedVersion) {
                    sortedVersions.get(0).setProperty("published",true);
                    session.save();
                }
            }
        }

        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("published", published));
    }
}
