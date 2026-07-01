/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
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
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.jahia.services.render.filter.RenderFilter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;


@Component(service = RenderFilter.class)
public class PublishedModuleFilter extends AbstractFilter {

    @Activate
    public void activate() {
        setPriority(20);
        setApplyOnModes("live");
        // Gate on the shared jmix:forgeElement mixin (carried by jnt:forgeModule AND jnt:forgePackage,
        // and inherited by any future forge element type) rather than a single concrete type. Binding
        // to jnt:forgeModule only left draft jnt:forgePackage detail pages fully readable to anonymous
        // visitors at their (predictable) direct URL (SECURITY-571 #54).
        setApplyOnNodeTypes("jmix:forgeElement");
    }

    @Override
    public String prepare(RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        if (!resource.getNode().getProperty("published").getBoolean() && !resource.getNode().hasPermission("jcr:write")) {
            renderContext.getResponse().sendRedirect(renderContext.getResponse().encodeRedirectURL(renderContext.getRequest().getContextPath() + "/"));
            return null;
        }
        return super.prepare(renderContext, resource, chain);
    }
}
