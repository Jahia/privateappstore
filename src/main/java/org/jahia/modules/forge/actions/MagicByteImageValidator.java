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

import java.util.Locale;
import java.util.Set;

/**
 * Content-based (magic-byte) detector for the raster image types the store accepts as module
 * icons / screenshots. The upload path stores {@code jcr:mimeType} from the browser-supplied
 * {@code File.type}, which a caller can spoof by hitting the generic JCR GraphQL mutation directly
 * (SECURITY-571 #28). This class sniffs the actual leading bytes so a server-side check can reject
 * or correct a spoofed declaration instead of trusting the client.
 *
 * <p>SVG is deliberately NOT recognised: {@code image/svg+xml} can carry scripts and, when served
 * inline, would execute in the site origin — so anything that is not one of the allow-listed raster
 * signatures is treated as disallowed.</p>
 */
public final class MagicByteImageValidator {

    /** Canonical MIME types for the accepted raster formats. */
    public static final String PNG = "image/png";
    public static final String JPEG = "image/jpeg";
    public static final String GIF = "image/gif";
    public static final String WEBP = "image/webp";

    /** The allow-list, lower-cased for case-insensitive comparison against a declared mime. */
    private static final Set<String> ALLOWED_RASTER_MIMES = Set.of(PNG, JPEG, GIF, WEBP);

    /** Enough leading bytes to identify every signature below (WEBP needs bytes 8-11 = "WEBP"). */
    public static final int SNIFF_LENGTH = 16;

    private MagicByteImageValidator() {
        // utility class - prevent instantiation
    }

    /**
     * Detect the raster image type from the file's leading bytes.
     *
     * @param header the first bytes of the file (at least {@link #SNIFF_LENGTH} recommended)
     * @return the canonical MIME type ({@link #PNG}/{@link #JPEG}/{@link #GIF}/{@link #WEBP}), or
     *         {@code null} when the bytes match no allow-listed raster signature (incl. SVG, HTML,
     *         empty/short input).
     */
    public static String detectRasterMime(byte[] header) {
        if (header == null) {
            return null;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return PNG;
        }
        // JPEG: FF D8 FF
        if (startsWith(header, 0xFF, 0xD8, 0xFF)) {
            return JPEG;
        }
        // GIF: "GIF8" (covers both GIF87a and GIF89a)
        if (startsWith(header, 0x47, 0x49, 0x46, 0x38)) {
            return GIF;
        }
        // WEBP: "RIFF" .... "WEBP" (bytes 0-3 = RIFF, bytes 8-11 = WEBP)
        if (isWebp(header)) {
            return WEBP;
        }
        return null;
    }

    /** True when {@code mime} is one of the accepted raster types (case-insensitive). */
    public static boolean isAllowedRasterMime(String mime) {
        return mime != null && ALLOWED_RASTER_MIMES.contains(mime.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean isWebp(byte[] header) {
        return startsWith(header, 0x52, 0x49, 0x46, 0x46) // "RIFF"
                && header.length >= 12
                && matchesAt(header, 8, 0x57, 0x45, 0x42, 0x50); // "WEBP"
    }

    private static boolean startsWith(byte[] data, int... signature) {
        return matchesAt(data, 0, signature);
    }

    private static boolean matchesAt(byte[] data, int offset, int... signature) {
        if (data.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((data[offset + i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
