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
import javax.jcr.RepositoryException;
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
    private static final int TAGS = 600;
    private static final int SKIP = 900;

    private static boolean canBePublished = true;
    private static int completion = 0, index = 0;
    private static Map<Integer, Map<String, Object>> todoList = new LinkedHashMap<Integer, Map<String, Object>>();
    private static List<Object[]> mandatoryProperties, otherProperties;

    private static void initMandatoryProperties(JCRNodeWrapper module) throws RepositoryException {

        if (mandatoryProperties == null)
            mandatoryProperties = new ArrayList<Object[]>();
        else
            mandatoryProperties.clear();

        mandatoryProperties.add(new Object[]{"jcr:title", TEXT, 20});
        mandatoryProperties.add(new Object[]{"description", TEXT, 20});
        mandatoryProperties.add(new Object[]{"category", WEAKREFERENCE, 10});
        mandatoryProperties.add(new Object[]{"versions", VERSIONS, 10});
        // TODO
        //mandatoryProperties.add(new Object[]{"screenshots", SCREENSHOTS, 10});

        int authorEmailType;
        if (module.getPropertyAsString("authorNameDisplayedAs").equals("organisation")
                || module.getSession().getUser().getProperty("j:email").isEmpty())
            authorEmailType = TEXT;
        else
            authorEmailType = SKIP;
        mandatoryProperties.add(new Object[]{"authorEmail", authorEmailType, 5});
    }

    private static void initOtherProperties(JCRNodeWrapper module) {

        otherProperties = new ArrayList<Object[]>();

        otherProperties.add(new Object[]{"howToInstall", TEXT, 10});
        otherProperties.add(new Object[]{"authorURL", TEXT, 5});
        otherProperties.add(new Object[]{"video", NODE, 5});
        otherProperties.add(new Object[]{"FAQ", TEXT, 5});
        otherProperties.add(new Object[]{"j:tags", TAGS, 5});
    }

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {
        canBePublished = true;
        completion = 0;
        index = 0;
        todoList.clear();

        JCRNodeWrapper module = resource.getNode();

        initMandatoryProperties(module);
        if (otherProperties == null)
            initOtherProperties(module);

        for (Object[] property : mandatoryProperties) {
            checkProperty((String) property[0], (Integer) property[1], (Integer) property[2], true, session, module);
        }
        for (Object[] property : otherProperties) {
            checkProperty((String) property[0], (Integer) property[1], (Integer) property[2], false, session, module);
        }

        JSONObject data = new JSONObject();
        data.put("completion", completion);
        data.put("todoList", todoList);
        data.put("canBePublished", canBePublished);

        return new ActionResult(HttpServletResponse.SC_OK, null, data);
    }

    public static boolean isPublishable(JCRNodeWrapper module) throws RepositoryException {

        canBePublished = true;
        JCRSessionWrapper session = module.getSession();
        initMandatoryProperties(module);

        for (Object[] property : mandatoryProperties) {
            checkProperty((String) property[0], (Integer) property[1], (Integer) property[2], true, session, module, true);
            if (!canBePublished)
                return canBePublished;
        }

        return canBePublished;
    }

    private void checkProperty(String name, int type, int percentage, boolean mandatory,
                                      JCRSessionWrapper session, JCRNodeWrapper module) {
        checkProperty(name, type, percentage, mandatory, session, module, false);
    }

    private static void checkProperty(String name, int type, int percentage, boolean mandatory,
                               JCRSessionWrapper session, JCRNodeWrapper module, boolean simpleCheck) {

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

            case TAGS:

                try {
                    if (module.getProperty(name).getValues().length == 0)
                        completed = false;
                } catch (PathNotFoundException e) {
                    completed = false;
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
                break;

            case SKIP:
                break;
        }

        if (completed) {
            if (simpleCheck)
                return;
            completion += percentage;
        }
        else {
            if (mandatory) {
                canBePublished = false;
                if (simpleCheck)
                    return;
            }

            Map<String, Object> propertyMap = new HashMap<String, Object>();
            propertyMap.put("name",
                new JahiaResourceBundle(session.getLocale(), "Jahia Forge").get("jnt_forgeModule."+name.replace(':', '_'), name));
            propertyMap.put("mandatory", mandatory);

            todoList.put(index++, propertyMap);
        }

    }

    private static boolean isEmptyHtmlText(String html) {

        Source source = new Source(html);
        TextExtractor textExtractor = source.getTextExtractor();

        textExtractor.setExcludeNonHTMLElements(true);
        textExtractor.setConvertNonBreakingSpaces(true);
        textExtractor.setIncludeAttributes(false);

        return textExtractor.toString().trim().length() == 0;
    }

}
