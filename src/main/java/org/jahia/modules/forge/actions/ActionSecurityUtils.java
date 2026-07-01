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

import org.apache.commons.lang.StringUtils;

/**
 * Small security helpers shared by upload/redirect actions: site-relative redirect validation
 * and log-input sanitization (CR/LF stripping) to prevent log forging. Public so the single
 * {@link #sanitizeForLog} implementation can be reused across the bundle (e.g. by the MavenProxy
 * servlet in the sibling {@code proxy} package) instead of being duplicated (SECURITY-571 #58).
 */
public final class ActionSecurityUtils {

    private ActionSecurityUtils() {
        // utility class - prevent instantiation
    }

    /**
     * Only allow site-relative redirect targets. Rejects absolute URLs, protocol-relative URLs
     * and pseudo-schemes (javascript:, data:, ...) to prevent open redirect / XSS.
     */
    static boolean isSafeRedirect(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String lower = url.trim().toLowerCase();
        return lower.startsWith("/")
                && !lower.startsWith("//")
                && !lower.startsWith("/\\")
                && !lower.contains("://");
    }

    /**
     * Sanitize a string before logging by stripping CR/LF and other control characters that could
     * be used to forge a log line. Returns "null" for null input. Truncated to 200 chars to keep
     * log lines bounded.
     */
    public static String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }
        String stripped = input.replaceAll("[\\u0000-\\u001F\\u007F]", "_");
        if (stripped.length() > 200) {
            stripped = stripped.substring(0, 200) + "...";
        }
        return stripped;
    }
}
