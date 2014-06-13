/**
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
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
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group. All rights reserved.
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
 *
 *
 *
 *
 */
package org.jahia.modules.forge.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.tags.TaggingService;
import org.slf4j.Logger;

import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 2013-05-23
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class EditTags extends PrivateAppStoreAction {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(EditTags.class);
    private TaggingService taggingService;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode();
        String title = module.getPropertyAsString("jcr:title");

        session.checkout(module);
        String siteKey = renderContext.getSite().getSiteKey();
        logger.info("Start editing tags of Private App Store module " + title + " " + module.getPath());
        if (module.hasProperty("j:tags")) {
            JCRPropertyWrapper tags = module.getProperty("j:tags");
            Value[] values = tags.getValues();

            ArrayList<String> submittedTags = (ArrayList<String>) parameters.get(
                    "tags-" + module.getIdentifier() + "[]");
            if (submittedTags == null || submittedTags.isEmpty()) {
                tags.remove();
            } else {
                ArrayList<String> currentTags = new ArrayList<String>();
                Map<String, Value> valueMap = new HashMap<String, Value>();
                for (Value tag : values) {
                    String name = session.getNodeByIdentifier(tag.getString()).getName();
                    currentTags.add(name);
                    valueMap.put(name, tag);
                }

                ArrayList<String> removedTags = new ArrayList<String>(currentTags);
                removedTags.removeAll(submittedTags);

                for (String tag : removedTags) {
                    tags.removeValue(valueMap.get(tag));
                }
                //tags.removeValue(new WeakReferenceValue( taggingService.getTag(tag, renderContext.getSite().getSiteKey(), session)));

                ArrayList<String> newTags = new ArrayList<String>(submittedTags);
                if (newTags.removeAll(currentTags)) {
                    for (String tag : newTags) {
                        taggingService.tag(module, tag, siteKey, true);
                    }
                }
            }
        } else {
            ArrayList<String> submittedTags = (ArrayList<String>) parameters.get(
                    "tags-" + module.getIdentifier() + "[]");
            for (String submittedTag : submittedTags) {
                taggingService.tag(module, submittedTag, siteKey, true);
            }
        }
        session.save();
        logger.info("Tags of Private App Store module " + title + " successfully edited " + module.getPath());

        return ActionResult.OK_JSON;
    }

    public void setTaggingService(TaggingService taggingService) {
        this.taggingService = taggingService;
    }
}
