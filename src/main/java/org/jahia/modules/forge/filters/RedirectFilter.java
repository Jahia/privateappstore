/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Enterprise Distribution                  =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group. All rights reserved.
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
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
 * JAHIA'S ENTERPRISE DISTRIBUTION - IMPORTANT LICENSING INFORMATION
 * ===============================================================
 *
 *     This file is part of a Jahia Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance
 *     with the terms contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     See the license for the rights, obligations and limitations governing use
 *     of the contents of the software.
 *
 *     For questions regarding licensing, support, production usage,
 *     please contact our team at sales@jahia.com or go to: http://www.jahia.com/license
 */
package org.jahia.modules.forge.filters;

import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLGenerator;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;


public class RedirectFilter extends AbstractFilter {

    @Override
    public String prepare(RenderContext renderContext, Resource resource, RenderChain chain)
            throws Exception {
        new URLGenerator(renderContext, resource);
        String url = "/" + resource.getLocale() + renderContext.getURLGenerator().getResourcePath();
        if (renderContext.getServletPath().endsWith(renderContext.getMode()+"frame")) {
            if (resource.getNode().isNodeType("jnt:page")) {
                resource.setTemplate("redirect-page");
            } else {
                resource.setTemplate("redirect-content");
            }
            return null;
        } else {
            renderContext.setRedirect(renderContext.getURLGenerator().getContext() + url);
            return "";
        }
    }
}
