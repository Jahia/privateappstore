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
package org.jahia.modules.forge.flow;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.utils.i18n.Messages;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.webflow.core.collection.LocalParameterMap;

import javax.jcr.*;
import java.io.Serializable;
import java.util.*;

/**
 * Flow handler for category settings
 */
public class CategorySettingsHandler implements Serializable {

    private static final long serialVersionUID = 871757139749838806L;
    private static final String RESOURCES = "resources.privateappstore";
    private static final String ROOT_CATEGORY = "rootCategory";
    private static final String WORKSPACE_DEFAULT = "default";
    private static final String JCR_TITLE = "jcr:title";
    private String rootCategoryIdentifier;
    private String currentCategory;
    private Map<Locale, String> categoryI18NTitles;
    private List<String> availableLanguages;
    private Set<String> categoryUsages;

    public void init(JCRSiteNode site) {
        try {
            if (site.hasProperty(ROOT_CATEGORY)) {
                rootCategoryIdentifier = site.getProperty(ROOT_CATEGORY).getNode().getIdentifier();
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    public void save(MessageContext messages, JCRNodeWrapper site, String rootCategoryIdentifier) {

        try {
            Node category = site.getSession().getNodeByIdentifier(rootCategoryIdentifier);
            site.setProperty(ROOT_CATEGORY, category);
            site.getSession().save();
            this.rootCategoryIdentifier = rootCategoryIdentifier;
        } catch (RepositoryException e) {
            messages.addMessage(new MessageBuilder()
                    .error()
                    .source(ROOT_CATEGORY)
                    .defaultText(
                            Messages.getWithArgs(RESOURCES,
                                    "jahiaForge.settings.rootCategory.error", LocaleContextHolder.getLocale(), e.getMessage())).build());
            e.printStackTrace();
        }


    }

    public void saveCategory(MessageContext messages, LocalParameterMap params) {
        for (String key : (Set<String>) params.asMap().keySet()) {
            if (StringUtils.startsWith(key, "lang_")) {
                String language = StringUtils.substringAfter(key, "lang_");
                try {
                    Session localizedSession = JCRSessionFactory.getInstance().getCurrentUserSession(WORKSPACE_DEFAULT, new Locale(language));
                    if (StringUtils.isEmpty(params.get(key))) {
                        localizedSession.getNodeByIdentifier(currentCategory).getProperty(JCR_TITLE).remove();
                    } else {
                        localizedSession.getNodeByIdentifier(currentCategory).setProperty(JCR_TITLE, params.get(key));
                    }
                    localizedSession.save();
                } catch (RepositoryException e) {
                    messages.addMessage(new MessageBuilder()
                            .error()
                            .source("editCategory")
                            .defaultText(
                                    Messages.getWithArgs(RESOURCES,
                                            "jahiaForge.settings.editCategory.error", LocaleContextHolder.getLocale(), e.getMessage())).build());
                    e.printStackTrace();
                }

            }

        }
    }

    public void setCurrentCategory(JCRSiteNode site, String currentCategoryUUID) {
        try {
            this.currentCategory = currentCategoryUUID;
            categoryI18NTitles = new HashMap<Locale, String>();
            availableLanguages = new LinkedList<String>();
            categoryUsages = new HashSet<String>();
            for (Locale locale : site.getLanguagesAsLocales()) {
                JCRNodeWrapper localizedCategory = JCRSessionFactory.getInstance().getCurrentUserSession(WORKSPACE_DEFAULT, locale).getNodeByIdentifier(currentCategoryUUID);
                if (localizedCategory.hasProperty(JCR_TITLE) && StringUtils.isNotEmpty(localizedCategory.getProperty(JCR_TITLE).getString())) {
                    categoryI18NTitles.put(locale, localizedCategory.getProperty(JCR_TITLE).getString());
                } else {
                    availableLanguages.add(locale.getLanguage());
                }
            }
            Session liveSession = JCRSessionFactory.getInstance().getCurrentUserSession("live");
            PropertyIterator references = liveSession.getNodeByIdentifier(currentCategory).getWeakReferences();
            while (references.hasNext()) {
                Property prop = references.nextProperty();
                categoryUsages.add(((JCRNodeWrapper) prop.getParent()).getDisplayableName());
            }
            Session defaultSession = JCRSessionFactory.getInstance().getCurrentUserSession(WORKSPACE_DEFAULT);
            references = defaultSession.getNodeByIdentifier(currentCategory).getWeakReferences();
            while (references.hasNext()) {
                Property prop = references.nextProperty();
                categoryUsages.add(((JCRNodeWrapper) prop.getParent()).getDisplayableName());
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    public boolean addCategory(MessageContext messages, JCRSiteNode site, String category) {
        try {
            if (StringUtils.isNotEmpty(category)) {
                Session session = JCRSessionFactory.getInstance().getCurrentUserSession(WORKSPACE_DEFAULT);
                Node rootCategory = session.getNodeByIdentifier(rootCategoryIdentifier);
                category = JCRContentUtils.findAvailableNodeName(rootCategory, category);
                String uuid = session.getNodeByIdentifier(rootCategoryIdentifier).addNode(category, "jnt:category").getIdentifier();
                session.save();
                setCurrentCategory(site, uuid);
                availableLanguages = new LinkedList<String>();
                for (Locale locale : site.getLanguagesAsLocales()) {
                    availableLanguages.add(locale.getLanguage());
                }
                return true;
            } else {
                return false;
            }
        } catch (RepositoryException e) {
            messages.addMessage(new MessageBuilder()
                    .error()
                    .source("addCategory")
                    .defaultText(
                            Messages.getWithArgs(RESOURCES,
                                    "jahiaForge.settings.addCategory.error", LocaleContextHolder.getLocale(), e.getMessage())).build());
            e.printStackTrace();
            return false;
        }

    }

    public void deleteCategory(MessageContext messages) {
        try {
            Session session = JCRSessionFactory.getInstance().getCurrentUserSession(WORKSPACE_DEFAULT);
            session.getNodeByIdentifier(currentCategory).remove();
            availableLanguages.clear();
            categoryI18NTitles.clear();
            categoryUsages.clear();
            currentCategory = null;
            session.save();
        } catch (RepositoryException e) {
            messages.addMessage(new MessageBuilder()
                    .error()
                    .source("deleteCategory")
                    .defaultText(
                            Messages.getWithArgs(RESOURCES,
                                    "jahiaForge.settings.deleteCategory.error", LocaleContextHolder.getLocale(), e.getMessage())).build());
            e.printStackTrace();
        }
    }

    public String getCurrentCategory() {
        return currentCategory;
    }

    public Map<Locale, String> getCategoryI18NTitles() {
        return categoryI18NTitles;
    }

    public List<String> getAvailableLanguages() {
        return availableLanguages;
    }

    public Set<String> getCategoryUsages() {
        return categoryUsages;
    }
}
