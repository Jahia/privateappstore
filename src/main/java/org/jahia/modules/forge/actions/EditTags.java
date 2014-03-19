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
