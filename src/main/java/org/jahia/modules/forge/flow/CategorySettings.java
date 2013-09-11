package org.jahia.modules.forge.flow;

import java.io.Serializable;

/**
 * Simple bean for category Settings
 * Stores uuid of the selected root category
 */
public class CategorySettings implements Serializable {
    private static final long serialVersionUID = 6780923248646398508L;
    private String rootCategory;

    public CategorySettings() {
    }

    public String getRootCategory() {
        return rootCategory;
    }

    public void setRootCategory(String rootCategory) {
        this.rootCategory = rootCategory;
    }
}
