/**
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group. All rights reserved.
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

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.tags.TaggingService;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Created by kevan on 09/09/14.
 */
public class CustomMatchingTags extends Action{
    private TaggingService taggingService;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        String prefix = parameters.get("q") != null && parameters.get("q").size() > 0 ? parameters.get("q").get(0) : "";
        String path = parameters.get("path") != null && parameters.get("path").size() > 0 ? parameters.get("path").get(0) : renderContext.getSite().getPath();
        Long limit = parameters.get("limit") != null && parameters.get("limit").size() > 0 ? Long.valueOf(parameters.get("limit").get(0)) : 10l;
        Map<String, Long> tags = taggingService.getTagsSuggester().suggest(prefix, path, 1l, limit, 0l, true, session);
        JSONObject result = new JSONObject();
        JSONArray tagsJSON = new JSONArray();
        for(String tag : tags.keySet()){
            JSONObject tagJSON = new JSONObject();
            tagJSON.put("name", tag);
            tagJSON.put("count", tags.get(tag));
            tagsJSON.put(tagJSON);
        }

        String transformedPrefix = taggingService.getTagHandler().execute(prefix);
        if(StringUtils.isNotEmpty(transformedPrefix)){
            JSONObject tagJSON = new JSONObject();
            tagJSON.put("name", transformedPrefix);
            tagJSON.put("count", 0);
            tagsJSON.put(tagJSON);
        }

        result.put("tags", tagsJSON);
        return new ActionResult(HttpServletResponse.SC_OK, null, result);
    }

    public void setTaggingService(TaggingService taggingService) {
        this.taggingService = taggingService;
    }
}
