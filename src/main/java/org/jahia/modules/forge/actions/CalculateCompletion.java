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

import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.taglibs.jcr.node.JCRTagUtils;
import org.jahia.utils.i18n.Messages;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 2013-05-09
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class CalculateCompletion extends Action {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(CalculateCompletion.class);

    private static final int TEXT = 100;
    private static final int WEAKREFERENCE = 200;
    private static final int NODE = 300;
    private static final int SCREENSHOTS = 400;
    private static final int VERSIONS = 500;
    private static final int TAGS = 600;
    private static final int SKIP = 900;

    private static final String RESOURCES = "resources.privateappstore";

    private boolean canBePublished = true;
    private int completion = 0;
    private int index = 0;
    private final Map<Integer, Map<String, Object>> todoList = new LinkedHashMap<>();
    private List<Object[]> mandatoryProperties;
    private List<Object[]> otherProperties;

    private void initMandatoryProperties() {
        if (mandatoryProperties == null) {
            mandatoryProperties = new ArrayList<>();
        } else {
            mandatoryProperties.clear();
        }

        mandatoryProperties.add(new Object[]{"jcr:title", TEXT, 20});
        mandatoryProperties.add(new Object[]{"description", TEXT, 20});
        mandatoryProperties.add(new Object[]{"j:defaultCategory", WEAKREFERENCE, 10});
        mandatoryProperties.add(new Object[]{"versions", VERSIONS, 10});
    }

    private void initOtherProperties() {
        otherProperties = new ArrayList<>();

        otherProperties.add(new Object[]{"howToInstall", TEXT, 5});
        otherProperties.add(new Object[]{"authorURL", TEXT, 5});
        otherProperties.add(new Object[]{"icon", NODE, 5});
        otherProperties.add(new Object[]{"screenshots", SCREENSHOTS, 5});
        otherProperties.add(new Object[]{"video", NODE, 5});
        otherProperties.add(new Object[]{"FAQ", TEXT, 5});
        otherProperties.add(new Object[]{"license", TEXT, 5});
        otherProperties.add(new Object[]{"j:tagList", TAGS, 5});
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

        initMandatoryProperties();
        if (otherProperties == null) {
            initOtherProperties();
        }

        for (Object[] property : mandatoryProperties) {
            checkProperty((String) property[0], (Integer) property[1], (Integer) property[2], true, session, module, false);
        }
        for (Object[] property : otherProperties) {
            checkProperty((String) property[0], (Integer) property[1], (Integer) property[2], false, session, module, false);
        }

        JSONObject data = new JSONObject();
        data.put("completion", completion);
        data.put("todoList", todoList);
        data.put("canBePublished", canBePublished);

        return new ActionResult(HttpServletResponse.SC_OK, null, data);
    }

    public static boolean isPublishable(JCRNodeWrapper module) throws RepositoryException {
        return new CalculateCompletion().computePublishable(module);
    }

    private boolean computePublishable(JCRNodeWrapper module) throws RepositoryException {
        canBePublished = true;
        JCRSessionWrapper session = module.getSession();
        initMandatoryProperties();

        for (Object[] property : mandatoryProperties) {
            checkProperty((String) property[0], (Integer) property[1], (Integer) property[2], true, session, module, true);
            if (!canBePublished) {
                return false;
            }
        }
        return canBePublished;
    }

    private void checkProperty(String name, int type, int percentage, boolean mandatory,
                               JCRSessionWrapper session, JCRNodeWrapper module, boolean simpleCheck) {
        boolean completed = isPropertyCompleted(name, type, session, module);

        if (completed) {
            if (simpleCheck) {
                return;
            }
            completion += percentage;
            return;
        }

        if (mandatory) {
            canBePublished = false;
            if (simpleCheck) {
                return;
            }
        }

        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("name",
                Messages.get(RESOURCES, "jnt_forgeModule." + name.replace(':', '_'), session.getLocale(), name));
        propertyMap.put("mandatory", mandatory);

        todoList.put(index++, propertyMap);
    }

    private static boolean isPropertyCompleted(String name, int type, JCRSessionWrapper session, JCRNodeWrapper module) {
        switch (type) {
            case TEXT:
                String propertyAsString = module.getPropertyAsString(name);
                return propertyAsString != null && !isEmptyHtmlText(propertyAsString);
            case NODE:
                return checkNode(module, name);
            case WEAKREFERENCE:
                return checkWeakReference(module, name, session);
            case SCREENSHOTS:
                return checkScreenshots(module, name);
            case TAGS:
                return checkTags(module, name);
            case SKIP:
                return true;
            default:
                return true;
        }
    }

    private static boolean checkNode(JCRNodeWrapper module, String name) {
        try {
            module.getNode(name);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        } catch (RepositoryException e) {
            logger.warn(e.getMessage(), e);
            return true;
        }
    }

    private static boolean checkWeakReference(JCRNodeWrapper module, String name, JCRSessionWrapper session) {
        try {
            if (module.getProperty(name).isMultiple()) {
                for (Value uuid : module.getProperty(name).getValues()) {
                    session.getNodeByUUID(uuid.getString());
                }
            } else {
                session.getNodeByUUID(module.getProperty(name).getString());
            }
            return true;
        } catch (ItemNotFoundException | PathNotFoundException e) {
            return false;
        } catch (RepositoryException e) {
            logger.warn(e.getMessage(), e);
            return true;
        }
    }

    private static boolean checkScreenshots(JCRNodeWrapper module, String name) {
        try {
            JCRNodeWrapper screenshotsList = module.getNode(name);
            return JCRTagUtils.hasChildrenOfType(screenshotsList, "jnt:file");
        } catch (PathNotFoundException e) {
            return false;
        } catch (RepositoryException e) {
            logger.warn(e.getMessage(), e);
            return true;
        }
    }

    private static boolean checkTags(JCRNodeWrapper module, String name) {
        try {
            return module.getProperty(name).getValues().length != 0;
        } catch (PathNotFoundException e) {
            return false;
        } catch (RepositoryException e) {
            logger.warn(e.getMessage(), e);
            return true;
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
