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
