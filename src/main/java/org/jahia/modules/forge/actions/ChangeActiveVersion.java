package org.jahia.modules.forge.actions;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.rules.BackgroundAction;
import org.jahia.taglibs.jcr.node.JCRTagUtils;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.util.Iterator;
import java.util.List;

/**
 * Date: 2013-05-13
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class ChangeActiveVersion  implements BackgroundAction {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(ChangeActiveVersion.class);

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void executeBackgroundAction(JCRNodeWrapper node) {

        try {
            JCRNodeWrapper module = node.getParent();
            List<JCRNodeWrapper> moduleVersions = JCRTagUtils.getChildrenOfType(module, "jnt:forgeModuleVersion");

            for (JCRNodeWrapper moduleVersion : moduleVersions) {

                if (moduleVersion.getProperty("activeVersion").getBoolean()
                        && !moduleVersion.getIdentifier().equals(node.getIdentifier()))
                    moduleVersion.setProperty("activeVersion", false);
            }

        } catch (RepositoryException e) {
            logger.warn(e.getMessage(), e);
        }
    }
}