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
 *     Copyright (C) 2002-2015 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.forge.flow;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.forge.actions.PrivateAppStoreAction;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.utils.i18n.Messages;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.webflow.core.collection.LocalParameterMap;

/**
 * Flow handler for category settings
 */
public class CategorySettingsHandler implements Serializable {

    private static final long serialVersionUID = 871757139749838806L;
    private String rootCategoryIdentifier;
    private String currentCategory;
    private Map<Locale,String> categoryI18NTitles;
    private List<String> availableLanguages;
    private Set<String> categoryUsages;
    
    /**
     * Initializes an instance of this class.
     */
    public CategorySettingsHandler() {
        super();
        PrivateAppStoreAction.ensureLicense();
    }
    
    public void init(JCRSiteNode site) {
        try {
            if (site.hasProperty("rootCategory")) {
                rootCategoryIdentifier = site.getProperty("rootCategory").getNode().getIdentifier();
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    public void save(MessageContext messages,JCRNodeWrapper site, String rootCategoryIdentifier) {

        try {
            Node category = site.getSession().getNodeByIdentifier(rootCategoryIdentifier);
            site.setProperty("rootCategory", category);
            site.getSession().save();
            this.rootCategoryIdentifier = rootCategoryIdentifier;
        } catch (RepositoryException e) {
            messages.addMessage(new MessageBuilder()
                    .error()
                    .source("rootCategory")
                    .defaultText(
                            Messages.getWithArgs("resources.privateappstore",
                                    "jahiaForge.settings.rootCategory.error", LocaleContextHolder.getLocale(),e.getMessage())).build());
            e.printStackTrace();
        }


    }

    public void saveCategory(MessageContext messages,LocalParameterMap params) {
        for (String key :(Set<String>) params.asMap().keySet()) {
            if (StringUtils.startsWith(key, "lang_")) {
                String language = StringUtils.substringAfter(key,"lang_");
                try {
                    Session localizedSession = JCRSessionFactory.getInstance().getCurrentUserSession("default", new Locale(language));
                    if (StringUtils.isEmpty(params.get(key))) {
                        localizedSession.getNodeByIdentifier(currentCategory).getProperty("jcr:title").remove();
                    } else {
                        localizedSession.getNodeByIdentifier(currentCategory).setProperty("jcr:title",params.get(key));
                    }
                    localizedSession.save();
                } catch (RepositoryException e) {
                    messages.addMessage(new MessageBuilder()
                            .error()
                            .source("editCategory")
                            .defaultText(
                                    Messages.getWithArgs("resources.privateappstore",
                                            "jahiaForge.settings.editCategory.error", LocaleContextHolder.getLocale(),e.getMessage())).build());
                    e.printStackTrace();
                }

            }

        }
    }

    public void setCurrentCategory(JCRSiteNode site,String currentCategoryUUID) {
        try {
            this.currentCategory = currentCategoryUUID;
            categoryI18NTitles = new HashMap<Locale, String>();
            availableLanguages = new LinkedList<String>();
            categoryUsages = new HashSet<String>();
            for (Locale locale : site.getLanguagesAsLocales()) {
                JCRNodeWrapper localizedCategory = JCRSessionFactory.getInstance().getCurrentUserSession("default", locale).getNodeByIdentifier(currentCategoryUUID);
                if (localizedCategory.hasProperty("jcr:title") && StringUtils.isNotEmpty(localizedCategory.getProperty("jcr:title").getString())) {
                    categoryI18NTitles.put(locale,localizedCategory.getProperty("jcr:title").getString());
                } else {
                    availableLanguages.add(locale.getLanguage());
                }
            }
            Session liveSession= JCRSessionFactory.getInstance().getCurrentUserSession("live");
            PropertyIterator references = liveSession.getNodeByIdentifier(currentCategory).getWeakReferences();
            while (references.hasNext()) {
                Property prop = references.nextProperty();
                categoryUsages.add(((JCRNodeWrapper) prop.getParent()).getDisplayableName());
            }
            Session defaultSession= JCRSessionFactory.getInstance().getCurrentUserSession("default");
            references = defaultSession.getNodeByIdentifier(currentCategory).getWeakReferences();
            while (references.hasNext()) {
                Property prop = references.nextProperty();
                categoryUsages.add(((JCRNodeWrapper) prop.getParent()).getDisplayableName());
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    public boolean addCategory(MessageContext messages,JCRSiteNode site, String category) {
        try {
            if (StringUtils.isNotEmpty(category)) {
                Session session =JCRSessionFactory.getInstance().getCurrentUserSession("default");
                Node rootCategory = session.getNodeByIdentifier(rootCategoryIdentifier);
                category = JCRContentUtils.findAvailableNodeName(rootCategory,category) ;
                String uuid = session.getNodeByIdentifier(rootCategoryIdentifier).addNode(category,"jnt:category").getIdentifier();
                session.save();
                setCurrentCategory(site,uuid);
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
                            Messages.getWithArgs("resources.privateappstore",
                                    "jahiaForge.settings.addCategory.error", LocaleContextHolder.getLocale(),e.getMessage())).build());
            e.printStackTrace();
            return false;
        }

    }

    public void deleteCategory(MessageContext messages) {
        try {
            Session session =JCRSessionFactory.getInstance().getCurrentUserSession("default");
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
                            Messages.getWithArgs("resources.privateappstore",
                                    "jahiaForge.settings.deleteCategory.error", LocaleContextHolder.getLocale(),e.getMessage())).build());
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
