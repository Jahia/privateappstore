/**
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.forge.tags;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.templates.ModuleVersion;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 8/28/13
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class ForgeFunctions {

    public static List<JCRNodeWrapper> sortModulesByVersion(NodeIterator moduleIterator) {
        // get Version

        LinkedList<ModuleVersion> versions = new LinkedList<ModuleVersion>();
        Map<ModuleVersion,JCRNodeWrapper> modules = new HashMap<ModuleVersion, JCRNodeWrapper>();
        while (moduleIterator.hasNext()) {
            JCRNodeWrapper module = (JCRNodeWrapper) moduleIterator.nextNode();
            try {
                ModuleVersion moduleVersion = new ModuleVersion(module.getProperty("versionNumber").getString());
                versions.add(moduleVersion);
                modules.put(moduleVersion,module);
            } catch (RepositoryException e) {
                // unable to read version, do nothing
            }
        }
        Collections.sort(versions);
        Collections.reverse(versions);
        LinkedList<JCRNodeWrapper> sortedVersions = new LinkedList<JCRNodeWrapper>();
        for (ModuleVersion version : versions) {
            sortedVersions.add(modules.get(version));
        }
        return sortedVersions;
    }

    public static JCRNodeWrapper latestVersion(List<JCRNodeWrapper> modules) {
        if (modules == null) {
            return null;
        }
        for (JCRNodeWrapper module : modules) {
            try {
                if (module.getProperty("published").getBoolean()) {
                    return module;
                }
            } catch (RepositoryException e) {
                // property cannot be read, do nothing
            }
        }
        return  null;
    }

    public static List<JCRNodeWrapper> previousVersions(List<JCRNodeWrapper> modules) {
        JCRNodeWrapper lastVersion = latestVersion(modules);
        if (lastVersion == null || modules == null) {
            return null;
        }
        List<JCRNodeWrapper> previousModules= new ArrayList<JCRNodeWrapper>(modules);
        for (JCRNodeWrapper module: modules) {
            previousModules.remove(module);
            if (StringUtils.equals(lastVersion.getPath(),module.getPath())) {
                break;
            }
        }
        return previousModules;
    }

    public static List<JCRNodeWrapper> nextVersions(List<JCRNodeWrapper> modules) {
        JCRNodeWrapper lastVersion = latestVersion(modules);
        if (lastVersion == null|| modules == null) {
            return modules;
        }
        List<JCRNodeWrapper> nextModules= new ArrayList<JCRNodeWrapper>(modules);
        boolean delete = false;
        for (JCRNodeWrapper module: modules) {
            if (StringUtils.equals(lastVersion.getPath(),module.getPath())) {
                delete = true;
            }
            if (delete) {
                nextModules.remove(module);
            }
        }
        return nextModules;
    }


}
