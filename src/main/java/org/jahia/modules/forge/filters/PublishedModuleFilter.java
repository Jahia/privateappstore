package org.jahia.modules.forge.filters;

import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;

import javax.jcr.PathNotFoundException;


public class PublishedModuleFilter extends AbstractFilter {
    @Override
    public String prepare(RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        if (!resource.getNode().getProperty("published").getBoolean() && !resource.getNode().hasPermission("jcr:write")) {
            renderContext.getResponse().sendRedirect(renderContext.getResponse().encodeRedirectURL(renderContext.getRequest().getContextPath() + "/"));
            return null;
        }
        return super.prepare(renderContext, resource, chain);
    }
}