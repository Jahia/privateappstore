package org.jahia.modules.forge.cache;

import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.cache.CacheKeyPartGenerator;
import org.osgi.service.component.annotations.Component;

/**
 * Adds the network the request arrived from to the fragment cache key, so a view may
 * safely render differently for a visitor coming through the public CDN than for one
 * coming through Jahia's VPN.
 *
 * <p>Background (SUPPORT-687): store.jahia.com is locked down at HAProxy so the public
 * internet gets anonymous, read-only use while every privileged action stays on the VPN.
 * The storefront therefore wants to hide the header sign-in trigger for public visitors,
 * because {@code /cms/login} is blocked for them and the form would 404 on submit.
 *
 * <p>That cannot be done with a plain server-side conditional. {@code templates/Page}
 * declares {@code cache.perUser}, whose key for an anonymous visitor is {@code guest} —
 * the same value for a public visitor and a VPN operator alike. The first render of a
 * page would be cached and then served to the other audience, in either direction: a
 * public visitor handed the sign-in button, or worse, an operator who loses it. That is
 * a cache key that models permission rather than what the output varies on.
 *
 * <p>Registering this part generator makes the network part of the key itself, so the two
 * audiences simply get two cache entries and the conditional becomes safe to write.
 *
 * <p><b>Trust:</b> the value comes from a request header that HAProxy unconditionally
 * deletes and re-sets on every request, so a client cannot supply it. If the header is
 * absent — no reverse proxy in front, i.e. local development, CI and the Cypress harness
 * — the request is treated as privileged, which keeps those environments behaving exactly
 * as they did before. That default fails open on presentation only: what actually stops a
 * public login is HAProxy's deny on {@code /cms/login}, never this class.
 *
 * <p><b>Cost:</b> part generators are global, so this adds a component to every fragment
 * cache key on the platform, not only to this module's views. In practice the public
 * variant is the hot one and the VPN variant stays small, but it is a platform-wide
 * effect and not a module-local one.
 *
 * <p><b>Registration:</b> a plain OSGi service. Jahia's OSGIRegistry bundle
 * ({@code org.jahia.bundles.extends.osgi.registry}) declares a {@code 0..n} dynamic
 * reference on this exact interface and forwards each bound service to
 * {@code DefaultCacheKeyGenerator.registerPartGenerator}, so publishing the service is
 * all that is required. No Spring context is involved; Spring is deprecated for Jahia
 * modules.
 *
 * <p>Because this sits in a NEW package, {@code org.jahia.modules.forge.cache.*} must be
 * listed in {@code _dsannotations} in the pom. A package missing from that list has its
 * {@code @Component} silently skipped, and the service is never published at all.
 */
@Component(service = CacheKeyPartGenerator.class)
public class EdgeOriginCacheKeyPartGenerator implements CacheKeyPartGenerator {

    /** Cache-key component name. Must be stable: it is embedded in every cached fragment key. */
    private static final String KEY = "edgeOrigin";

    /** Set by HAProxy on every request; any inbound value is stripped first. */
    public static final String EDGE_HEADER = "X-Jahia-Edge";

    /** Value for traffic that arrived through the public CDN. */
    public static final String PUBLIC_EDGE = "public";

    /** Value for traffic that arrived from the VPN, and the default when no proxy is in front. */
    public static final String PRIVILEGED_EDGE = "vpn";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getValue(Resource resource, RenderContext renderContext, Properties properties) {
        return isPublicEdge(renderContext) ? PUBLIC_EDGE : PRIVILEGED_EDGE;
    }

    /**
     * Whether this request reached Jahia through the public CDN rather than the VPN.
     * Exposed so views and tests can ask the same question the cache key is built on,
     * instead of re-deriving it and drifting from it.
     */
    public static boolean isPublicEdge(RenderContext renderContext) {
        if (renderContext == null) {
            return false;
        }
        HttpServletRequest request = renderContext.getRequest();
        if (request == null) {
            return false;
        }
        return PUBLIC_EDGE.equalsIgnoreCase(request.getHeader(EDGE_HEADER));
    }

    @Override
    public String replacePlaceholders(RenderContext renderContext, String keyPart) {
        return keyPart;
    }
}
