package jnt_categorySettings

import org.jahia.taglibs.jcr.node.JCRTagUtils
import org.apache.commons.lang.StringEscapeUtils
if (renderContext.editMode) {
    print("This will render folders as a tree")
} else {
    def base = renderContext.site.getSession().getNode("/sites/systemsite/categories");
    def displayTree;
    def currentCategory = "";
    try {
        currentCategory = renderContext.mainResource.node.getProperty("categoryRoot").getNode();
    } catch (javax.jcr.RepositoryException e) {
        currentCategory = null;
    }
    displayTree = {parent ->
        def childs = JCRTagUtils.getChildrenOfType(parent, "jnt:category");
        if (childs.size() > 0) {
            println ",children: ["
        };
        childs.each {childNode ->
            println "{label: '"+ StringEscapeUtils.escapeXml(childNode.displayableName) + "', id: '"+childNode.identifier+"'";
            displayTree(childNode);
            println "},"
        }
        if (childs.size() > 0) {
            println "]"
        };
    }
//    println "<label>" + (currentCategory!=null?currentCategory.getDisplayableName() :JahiaResourceBundle.getString("DefaultJahiaTemplates", "label.categories", renderContext.UILocale, "Default Jahia Templates")) + "</label>";
    if (JCRTagUtils.hasChildrenOfType(base, "jnt:category")) {
        println "var data = ["
        println "{label: '"+ StringEscapeUtils.escapeXml(base.displayableName) + "', id: '"+base.identifier+"'";
        displayTree(base);
        println "}];";
    }
}
