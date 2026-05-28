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
package org.jahia.modules.forge.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.image.JahiaImageService;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.tools.files.FileUpload;
import org.jahia.utils.i18n.Messages;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Action called to update a module icon
 */
public class UpdateModuleIcon extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(UpdateModuleIcon.class);

    private static final String RESOURCES = "resources.privateappstore";
    /** Maximum accepted size for an uploaded icon. */
    private static final long MAX_ICON_SIZE_BYTES = 5L * 1024 * 1024;
    /** Allowed raster image extensions (SVG is intentionally excluded - it can carry script). */
    private static final Set<String> ALLOWED_ICON_EXTENSIONS =
            new HashSet<>(Arrays.asList("png", "jpg", "jpeg", "gif", "webp"));

    JahiaImageService imageService;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        // cleanup folder icon
        if (session.getNode(resource.getNode().getPath()).hasNode("icon")) {
            session.getNode(resource.getNode().getPath()).getNode("icon").remove();
        }
        JCRNodeWrapper iconFolder = createNode(req, new HashMap<String, List<String>>(), resource.getNode(), "jnt:folder", "icon", true);
        JCRNodeWrapper icon;
        final FileUpload fileUpload = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);
        String redirectURL = null;
        ActionResult result;
        if (fileUpload != null && fileUpload.getParameterMap().containsKey("redirectURL")) {
            // Only accept a site-relative redirect target to prevent open redirect.
            String candidate = fileUpload.getParameterMap().get("redirectURL").get(0);
            if (isSafeRedirect(candidate)) {
                redirectURL = candidate;
            } else {
                logger.warn("UpdateModuleIcon: rejected unsafe redirectURL '{}'", candidate);
            }
        }
        if (fileUpload != null && fileUpload.getFileItems() != null && fileUpload.getFileItems().size() > 0) {
            final Map<String, DiskFileItem> stringDiskFileItemMap = fileUpload.getFileItems();
            DiskFileItem itemEntry = stringDiskFileItemMap.get(stringDiskFileItemMap.keySet().iterator().next());
            String uploadExtension = FilenameUtils.getExtension(itemEntry.getName()).toLowerCase();
            boolean isImageContentType = itemEntry.getContentType() != null
                    && "image".equals(StringUtils.substringBefore(itemEntry.getContentType(), "/"))
                    && !"image/svg+xml".equalsIgnoreCase(itemEntry.getContentType());
            if (itemEntry.getSize() > MAX_ICON_SIZE_BYTES) {
                logger.warn("UpdateModuleIcon: rejected oversized icon '{}' ({} bytes)", itemEntry.getName(), itemEntry.getSize());
                String error = Messages.get(RESOURCES, "forge.updateIcon.error.wrong.format", session.getLocale());
                result = new ActionResult(HttpServletResponse.SC_OK, redirectURL, new JSONObject().put("iconUpdate", false).put("errorMessage", error));
            } else if (isImageContentType && ALLOWED_ICON_EXTENSIONS.contains(uploadExtension)) {
                File f = null;
                try {
                    icon = iconFolder.uploadFile(itemEntry.getName(), itemEntry.getInputStream(),
                            JCRContentUtils.getMimeType(itemEntry.getName(), itemEntry.getContentType()));
                    String fileExtension = FilenameUtils.getExtension(icon.getName());

                    f = File.createTempFile("thumb", "." + fileExtension);
                    // createThumb/getImage decode the bytes: a non-image masquerading as an image
                    // (declared content-type) fails here and is rejected below.
                    imageService.createThumb(imageService.getImage(icon), f, 125, false);
                    try (InputStream is = new FileInputStream(f)) {
                        icon = iconFolder.uploadFile(itemEntry.getName(), is, JCRContentUtils.getMimeType(itemEntry.getName(), itemEntry.getContentType()));
                    }
                    session.save();
                    result = new ActionResult(HttpServletResponse.SC_OK, redirectURL, new JSONObject().put("iconUpdate", true).put("iconUrl", icon.getUrl()));
                } catch (Exception e) {
                    logger.warn("UpdateModuleIcon: failed to process uploaded icon '{}'", itemEntry.getName(), e);
                    String error = Messages.get(RESOURCES, "forge.updateIcon.error.wrong.format", session.getLocale());
                    result = new ActionResult(HttpServletResponse.SC_OK, redirectURL, new JSONObject().put("iconUpdate", false).put("errorMessage", error));
                } finally {
                    FileUtils.deleteQuietly(f);
                }
            } else {
                String error = Messages.get(RESOURCES, "forge.updateIcon.error.wrong.format", session.getLocale());
                result = new ActionResult(HttpServletResponse.SC_OK, redirectURL, new JSONObject().put("iconUpdate", false).put("errorMessage", error));
            }
        } else {
            String error = Messages.get(RESOURCES, "forge.updateIcon.error.noFileFound", session.getLocale());
            result = new ActionResult(HttpServletResponse.SC_OK, redirectURL, new JSONObject().put("iconUpdate", false).put("errorMessage", error));
        }
        return result;
    }

    /**
     * Only allow site-relative redirect targets. Rejects absolute URLs, protocol-relative URLs
     * and pseudo-schemes (javascript:, data:, ...) to prevent open redirect / XSS.
     */
    private static boolean isSafeRedirect(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String lower = url.trim().toLowerCase();
        return lower.startsWith("/")
                && !lower.startsWith("//")
                && !lower.startsWith("/\\")
                && !lower.contains("://");
    }

    public void setImageService(JahiaImageService imageService) {
        this.imageService = imageService;
    }
}
