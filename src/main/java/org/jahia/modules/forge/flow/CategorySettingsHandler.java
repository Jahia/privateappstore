package org.jahia.modules.forge.flow;

import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;
import java.io.Serializable;

/**
 * Flow handler for catergory settings
 */
public class CategorySettingsHandler implements Serializable{

    private static final long serialVersionUID = 871757139749838806L;
    private CategorySettings categorySettings;

    public void init() {
        categorySettings = new CategorySettings();
    }

    public void save(JCRNodeWrapper site) {

        try {
            site.setProperty("rootCategory", categorySettings.getRootCategory());
            site.getSession().save();
        } catch (RepositoryException e) {
            // save failed
            e.printStackTrace();
        }


    }

    public CategorySettings getCategorySettings() {
        return categorySettings;
    }
}
