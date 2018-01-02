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
 *     Copyright (C) 2002-2018 Jahia Solutions Group. All rights reserved.
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
