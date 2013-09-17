package org.jahia.modules.forge.flow;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.categories.jcr.JCRCategory;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.springframework.webflow.context.servlet.HttpServletRequestParameterMap;
import org.springframework.webflow.core.collection.LocalParameterMap;
import org.springframework.webflow.mvc.servlet.AbstractFlowHandler;

import javax.jcr.*;
import java.io.Serializable;
import java.util.*;

/**
 * Flow handler for catergory settings
 */
public class CategorySettingsHandler implements Serializable{

    private static final long serialVersionUID = 871757139749838806L;
    private String rootCategoryIdentifier;
    private String currentCategory;
    private Map<Locale,String> categoryI18NTitles;
    private List<String> availableLanguages;
    private Set<String> categoryUsages;
    public void init(JCRSiteNode site) {
        try {
            rootCategoryIdentifier = site.getProperty("rootCategory").getNode().getIdentifier();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    public void save(JCRNodeWrapper site, String rootCategoryIdentifier) {

        try {
            Node category = site.getSession().getNodeByIdentifier(rootCategoryIdentifier);
            site.setProperty("rootCategory", category);
            site.getSession().save();
            this.rootCategoryIdentifier = rootCategoryIdentifier;
        } catch (RepositoryException e) {
            // save failed
            e.printStackTrace();
        }


    }

    public void saveCategory(LocalParameterMap params) {
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

    public void addCategory(JCRSiteNode site, String category) {
        try {
            Session session =JCRSessionFactory.getInstance().getCurrentUserSession("default");
            Node rootCategory = session.getNodeByIdentifier(rootCategoryIdentifier);
            category = JCRContentUtils.findAvailableNodeName(rootCategory,category) ;
            this.currentCategory = session.getNodeByIdentifier(rootCategoryIdentifier).addNode(category,"jnt:category").getIdentifier();
            session.save();
            availableLanguages = new LinkedList<String>();
            for (Locale locale : site.getLanguagesAsLocales()) {
                availableLanguages.add(locale.getLanguage());
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }

    }

    public void deleteCategory() {
        try {
            Session session =JCRSessionFactory.getInstance().getCurrentUserSession("default");
            session.getNodeByIdentifier(currentCategory).remove();
            availableLanguages.clear();
            categoryI18NTitles.clear();
            categoryUsages.clear();
            currentCategory = null;
            session.save();
        } catch (RepositoryException e) {
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
