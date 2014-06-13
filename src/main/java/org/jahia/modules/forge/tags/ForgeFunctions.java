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
 *     Copyright (C) 2002-2014 Jahia Solutions Group. All rights reserved.
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
