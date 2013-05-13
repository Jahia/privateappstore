package org.jahia.modules.forge.actions;

import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.taglibs.functions.Functions;
import org.jahia.taglibs.jcr.node.JCRTagUtils;
import org.jahia.utils.i18n.JahiaResourceBundle;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Date: 2013-05-09
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class CalculateCompletion  extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(CalculateCompletion.class);

    private static final int TEXT = 100;
    private static final int WEAKREFERENCE = 200;
    private static final int NODE = 300;
    private static final int SCREENSHOTS = 400;
    private static final int VERSIONS = 500;

    private boolean canBePublished = false;
    private int completion = 0, index = 0;
    private Map<Integer, Map<String, Object>> todoList = new LinkedHashMap<Integer, Map<String, Object>>();

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        canBePublished = false;
        completion = 0;
        index = 0;
        todoList.clear();

        JCRNodeWrapper module = resource.getNode();

        checkProperty("jcr:title", TEXT, 20, true, session, module);
        checkProperty("description", TEXT, 20, true, session, module);
        checkProperty("category", WEAKREFERENCE, 10, true, session, module);
        checkProperty("screenshots", SCREENSHOTS, 10, true, session, module);
        checkProperty("versions", VERSIONS, 10, true, session, module);
        if (module.getPropertyAsString("authorNameDisplayedAs").equals("organisation"))
            checkProperty("authorEmail", TEXT, 5, true, session, module);
        else
            completion += 5;
        checkProperty("howToInstall", TEXT, 10, false, session, module);
        checkProperty("video", NODE, 5, false, session, module);
        checkProperty("FAQ", TEXT, 5, false, session, module);
        checkProperty("authorURL", TEXT, 5, false, session, module);

        JSONObject data = new JSONObject();
        data.put("completion", completion);
        data.put("todoList", todoList);
        data.put("canBePublished", canBePublished);

        return new ActionResult(HttpServletResponse.SC_OK, null, data);
    }

    private void checkProperty(String name, int type, int percentage, boolean mandatory,
                               JCRSessionWrapper session, JCRNodeWrapper module) {

        boolean completed = true;

        switch (type) {

            case TEXT:
                String propertyAsString = module.getPropertyAsString(name);
                if (propertyAsString == null || isEmptyHtmlText(propertyAsString))
                    completed = false;
                break;

            case NODE:

                try {
                    module.getNode(name);
                } catch (PathNotFoundException e) {
                    completed = false;
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
                break;

            case WEAKREFERENCE:

                try {
                    session.getNodeByUUID(module.getProperty(name).getString());
                } catch (ItemNotFoundException e) {
                    completed = false;
                } catch (PathNotFoundException e) {
                    completed = false;
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
                break;

            case SCREENSHOTS:

                try {
                    JCRNodeWrapper screenshotsList = module.getNode(name);
                    if (!JCRTagUtils.hasChildrenOfType(screenshotsList, "jnt:forgeModuleScreenshot"))
                        completed = false;
                } catch (PathNotFoundException e) {
                    completed = false;
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
                break;

            case VERSIONS:

                if (!JCRTagUtils.hasChildrenOfType(module, "jnt:forgeModuleVersion"))
                    completed = false;
                break;

        }

        if (completed) {
            completion += percentage;
        }
        else {
            if (mandatory) {
                canBePublished = false;
            }

            Map<String, Object> propertyMap = new HashMap<String, Object>();
            propertyMap.put("name",
                    new JahiaResourceBundle(session.getLocale(), "Jahia Forge").get("jnt_forgeModule."+name, name));
            propertyMap.put("mandatory", mandatory);

            todoList.put(index++, propertyMap);
        }

    }

    private boolean isEmptyHtmlText(String html) {

        Source source = new Source(html);
        TextExtractor textExtractor = source.getTextExtractor();

        textExtractor.setExcludeNonHTMLElements(true);
        textExtractor.setConvertNonBreakingSpaces(true);
        textExtractor.setIncludeAttributes(false);

        return textExtractor.toString().trim().length() == 0;
    }

}
