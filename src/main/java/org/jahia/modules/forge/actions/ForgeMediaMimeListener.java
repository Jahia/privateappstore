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
 *     Copyright (C) 2002-2026 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.forge.actions;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.DefaultEventListener;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.io.IOException;
import java.io.InputStream;

/**
 * Server-side re-validation of the content type of uploaded module icons / screenshots
 * (SECURITY-571 #28).
 *
 * <p>The storefront editor validates the picked file's type in the browser and then persists it via
 * a generic JCR GraphQL mutation that writes {@code jcr:mimeType} straight from the browser's
 * {@code File.type}. A module author can bypass the client allow-list by calling that mutation
 * directly and store, e.g., an SVG (which can carry scripts) under {@code jcr:mimeType=image/png}.
 * Nothing on the backend re-checked the bytes.</p>
 *
 * <p>This listener closes that gap: on any {@code jnt:resource} written under a forge element's
 * {@code icon} or {@code screenshots} folder, it sniffs the actual leading bytes
 * ({@link MagicByteImageValidator}) and, running as system:</p>
 * <ul>
 *   <li>removes the offending {@code jnt:file} when the bytes are not an allow-listed raster
 *       (SVG/HTML/anything else) — a spoofed non-image never survives; and</li>
 *   <li>corrects {@code jcr:mimeType} to the detected type when the bytes are a valid raster but the
 *       declared mime was spoofed to something else.</li>
 * </ul>
 *
 * <p>It follows the module's established {@link DefaultEventListener} pattern (see
 * {@code ForgeSiteDeletionListener}). Being asynchronous it is defense-in-depth, not the sole gate;
 * the storefront also refuses inline rendering of user files and the client keeps its allow-list.</p>
 */
@Component(service = DefaultEventListener.class, immediate = true)
public class ForgeMediaMimeListener extends DefaultEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeMediaMimeListener.class);
    private static final String MODULES_REPOSITORY = "/contents/modules-repository/";
    private static final String ICON_SEGMENT = "/icon/";
    private static final String SCREENSHOTS_SEGMENT = "/screenshots/";
    private static final String JCR_CONTENT = "/jcr:content";
    private static final String PROP_MIME_TYPE = "jcr:mimeType";
    private static final String PROP_DATA = "jcr:data";
    private static final String JNT_RESOURCE = "jnt:resource";
    /** Both workspaces are checked: media can be authored in EDIT or directly in LIVE. */
    private static final String[] WORKSPACES = {"default", "live"};

    @Override
    public int getEventTypes() {
        return Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED;
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
    public String[] getNodeTypes() {
        // Restrict to the binary resource node: for property events this matches jcr:mimeType /
        // jcr:data whose parent is the jnt:resource, so noise from unrelated writes is filtered out.
        return new String[]{JNT_RESOURCE};
    }

    @Override
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            final Event event = events.nextEvent();
            try {
                final String resourcePath = forgeMediaResourcePath(event.getPath());
                if (resourcePath != null) {
                    validateResource(resourcePath);
                }
            } catch (RepositoryException e) {
                LOGGER.error("ForgeMediaMimeListener: error handling event", e);
            }
        }
    }

    /**
     * Return the {@code .../jcr:content} node path when {@code eventPath} is (or is a property of) a
     * jnt:resource under a forge element's {@code icon} or {@code screenshots} folder, else null.
     */
    static String forgeMediaResourcePath(String eventPath) {
        if (eventPath == null || !eventPath.contains(MODULES_REPOSITORY)) {
            return null;
        }
        if (!eventPath.contains(ICON_SEGMENT) && !eventPath.contains(SCREENSHOTS_SEGMENT)) {
            return null;
        }
        final int idx = eventPath.indexOf(JCR_CONTENT);
        if (idx < 0) {
            return null;
        }
        // Trim any trailing property segment (".../jcr:content/jcr:mimeType" -> ".../jcr:content").
        return eventPath.substring(0, idx + JCR_CONTENT.length());
    }

    /** Sniff the resource bytes in whichever workspace(s) hold it and reject/correct as needed. */
    private void validateResource(String resourcePath) {
        for (String workspace : WORKSPACES) {
            try {
                JCRTemplate.getInstance().doExecuteWithSystemSession(null, workspace, session -> {
                    validateInSession(session, resourcePath);
                    return null;
                });
            } catch (RepositoryException e) {
                // Node absent in this workspace (not yet published, or authored only in the other) —
                // expected; keep checking the remaining workspace.
                LOGGER.debug("ForgeMediaMimeListener: '{}' not processed in workspace '{}': {}",
                        ActionSecurityUtils.sanitizeForLog(resourcePath), workspace, e.getMessage());
            }
        }
    }

    private static void validateInSession(JCRSessionWrapper session, String resourcePath)
            throws RepositoryException {
        if (!session.nodeExists(resourcePath)) {
            return;
        }
        final JCRNodeWrapper resource = session.getNode(resourcePath);
        if (!resource.isNodeType(JNT_RESOURCE) || !resource.hasProperty(PROP_DATA)) {
            return;
        }
        final byte[] header = readHeader(resource);
        final String detected = MagicByteImageValidator.detectRasterMime(header);
        final String declared = resource.hasProperty(PROP_MIME_TYPE)
                ? resource.getProperty(PROP_MIME_TYPE).getString() : null;

        if (detected == null) {
            // Not an allow-listed raster (SVG/HTML/…): remove the containing file so a spoofed,
            // potentially script-bearing upload cannot be served.
            final JCRNodeWrapper file = resource.getParent();
            LOGGER.warn("ForgeMediaMimeListener: removed non-raster upload '{}' (declared mime '{}')",
                    ActionSecurityUtils.sanitizeForLog(file.getPath()),
                    ActionSecurityUtils.sanitizeForLog(declared));
            file.remove();
            session.save();
        } else if (!detected.equalsIgnoreCase(declared)) {
            // Valid raster but the declared mime was spoofed: pin it to the sniffed real type.
            resource.setProperty(PROP_MIME_TYPE, detected);
            session.save();
            LOGGER.warn("ForgeMediaMimeListener: corrected spoofed mime on '{}' ('{}' -> '{}')",
                    ActionSecurityUtils.sanitizeForLog(resourcePath),
                    ActionSecurityUtils.sanitizeForLog(declared), detected);
        }
    }

    private static byte[] readHeader(JCRNodeWrapper resource) throws RepositoryException {
        try (InputStream in = resource.getProperty(PROP_DATA).getBinary().getStream()) {
            return in.readNBytes(MagicByteImageValidator.SNIFF_LENGTH);
        } catch (IOException e) {
            LOGGER.warn("ForgeMediaMimeListener: could not read '{}' data: {}",
                    ActionSecurityUtils.sanitizeForLog(resource.getPath()), e.getMessage());
            return new byte[0];
        }
    }
}
