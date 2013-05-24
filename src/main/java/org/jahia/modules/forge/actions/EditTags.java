package org.jahia.modules.forge.actions;

import org.apache.jackrabbit.value.ReferenceValue;
import org.apache.jackrabbit.value.WeakReferenceValue;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.nodetypes.ValueImpl;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.search.jcr.JahiaJCRSearchProvider;
import org.jahia.services.tags.TaggingService;
import org.slf4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Date: 2013-05-23
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class EditTags extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(EditTags.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode();
        String title = module.getPropertyAsString("jcr:title");

        session.checkout(module);

        logger.info("Start editing tags of Forge module " + title + " " + module.getPath());

        JCRPropertyWrapper tags = module.getProperty("j:tags");
        Value[] values = tags.getValues();

        ArrayList<String> submittedTags = (ArrayList<String>) parameters.get("tags-" + module.getIdentifier() + "[]");

        ListIterator<String> listIterator = submittedTags.listIterator();
        while(listIterator.hasNext())
            listIterator.set(listIterator.next().toLowerCase());

        ArrayList<String> currentTags = new ArrayList<String>();
        for (Value tag : values)
            currentTags.add(session.getNodeByIdentifier(tag.getString()).getName());

        TaggingService taggingService = new TaggingService();

        ArrayList<String> removedTags = new ArrayList<String>(currentTags);
        removedTags.removeAll(submittedTags);
        for (String tag : removedTags)
            tags.removeValue(new ValueImpl(taggingService.getTag(tag, renderContext.getSite().getSiteKey(), session).getIdentifier(), PropertyType.REFERENCE));
            //tags.removeValue(new WeakReferenceValue( taggingService.getTag(tag, renderContext.getSite().getSiteKey(), session)));

        ArrayList<String> newTags = new ArrayList<String>(submittedTags);
        if(newTags.removeAll(currentTags))
            for (String tag : newTags)
                taggingService.tag(module, tag, renderContext.getSite().getSiteKey(), true);

        session.save();

        logger.info("Tags of Forge module " + title + " successfully edited " + module.getPath());

        return ActionResult.OK_JSON;
    }
}
