package org.jahia.modules.forge.settings;

import org.jahia.services.content.DefaultEventListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

/**
 * Removes a site's forge settings (per-site OSGi config) when the site is deleted, so the
 * configuration does not outlive the site and a recreated same-key site does not inherit stale
 * settings. Without this, settings (unlike the old jmix:forgeSettings JCR storage) would survive
 * site deletion since OSGi config is not part of the JCR subtree.
 */
@Component(service = DefaultEventListener.class, immediate = true)
public class ForgeSiteDeletionListener extends DefaultEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeSiteDeletionListener.class);
    private static final String SITES_PREFIX = "/sites/";

    private final ForgeSettingsService settingsService;

    @Activate
    public ForgeSiteDeletionListener(@Reference ForgeSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public int getEventTypes() {
        return Event.NODE_REMOVED;
    }

    @Override
    public String getPath() {
        return "/sites";
    }

    @Override
    public boolean isDeep() {
        return true;
    }

    @Override
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            final Event event = events.nextEvent();
            try {
                final String path = event.getPath();
                if (path == null || !path.startsWith(SITES_PREFIX)) {
                    continue;
                }
                // Act only on the site node itself (/sites/<key>), not its descendants.
                final String rel = path.substring(SITES_PREFIX.length());
                if (!rel.isEmpty() && rel.indexOf('/') < 0) {
                    settingsService.delete(rel);
                    LOGGER.info("Removed forge settings config for deleted site '{}'", rel);
                }
            } catch (RepositoryException e) {
                LOGGER.error("Error handling site-removal event", e);
            }
        }
    }
}
