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
import org.json.JSONException;
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

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(UpdateModuleIcon.class);

    private static final String RESOURCES = "resources.privateappstore";
    private static final String ICON_UPDATE = "iconUpdate";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String ERR_WRONG_FORMAT = "forge.updateIcon.error.wrong.format";
    /** Maximum accepted size for an uploaded icon. */
    private static final long MAX_ICON_SIZE_BYTES = 5L * 1024 * 1024;
    /** Allowed raster image extensions (SVG is intentionally excluded - it can carry script). */
    private static final Set<String> ALLOWED_ICON_EXTENSIONS =
            new HashSet<>(Arrays.asList("png", "jpg", "jpeg", "gif", "webp"));

    JahiaImageService imageService;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        if (session.getNode(resource.getNode().getPath()).hasNode("icon")) {
            session.getNode(resource.getNode().getPath()).getNode("icon").remove();
        }
        JCRNodeWrapper iconFolder = createNode(req, new HashMap<String, List<String>>(), resource.getNode(), "jnt:folder", "icon", true);
        final FileUpload fileUpload = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);
        String redirectURL = extractSafeRedirect(fileUpload);

        if (fileUpload == null || fileUpload.getFileItems() == null || fileUpload.getFileItems().isEmpty()) {
            String error = Messages.get(RESOURCES, "forge.updateIcon.error.noFileFound", session.getLocale());
            return new ActionResult(HttpServletResponse.SC_OK, redirectURL, new JSONObject().put(ICON_UPDATE, false).put(ERROR_MESSAGE, error));
        }
        final Map<String, DiskFileItem> stringDiskFileItemMap = fileUpload.getFileItems();
        DiskFileItem itemEntry = stringDiskFileItemMap.get(stringDiskFileItemMap.keySet().iterator().next());
        return validateAndProcessIcon(session, iconFolder, itemEntry, redirectURL);
    }

    private static String extractSafeRedirect(FileUpload fileUpload) {
        if (fileUpload == null || !fileUpload.getParameterMap().containsKey("redirectURL")) {
            return null;
        }
        String candidate = fileUpload.getParameterMap().get("redirectURL").get(0);
        if (ActionSecurityUtils.isSafeRedirect(candidate)) {
            return candidate;
        }
        if (logger.isWarnEnabled()) {
            logger.warn("UpdateModuleIcon: rejected unsafe redirectURL '{}'",
                    ActionSecurityUtils.sanitizeForLog(candidate));
        }
        return null;
    }

    private ActionResult validateAndProcessIcon(JCRSessionWrapper session, JCRNodeWrapper iconFolder,
                                                DiskFileItem itemEntry, String redirectURL) throws JSONException {
        if (itemEntry.getSize() > MAX_ICON_SIZE_BYTES) {
            if (logger.isWarnEnabled()) {
                logger.warn("UpdateModuleIcon: rejected oversized icon '{}' ({} bytes)",
                        ActionSecurityUtils.sanitizeForLog(itemEntry.getName()), itemEntry.getSize());
            }
            return wrongFormatResult(session, redirectURL);
        }
        String uploadExtension = FilenameUtils.getExtension(itemEntry.getName()).toLowerCase();
        boolean isImageContentType = itemEntry.getContentType() != null
                && "image".equals(StringUtils.substringBefore(itemEntry.getContentType(), "/"))
                && !"image/svg+xml".equalsIgnoreCase(itemEntry.getContentType());
        if (!isImageContentType || !ALLOWED_ICON_EXTENSIONS.contains(uploadExtension)) {
            return wrongFormatResult(session, redirectURL);
        }
        return processValidIcon(session, iconFolder, itemEntry, redirectURL);
    }

    private ActionResult processValidIcon(JCRSessionWrapper session, JCRNodeWrapper iconFolder,
                                          DiskFileItem itemEntry, String redirectURL) throws JSONException {
        File f = null;
        try {
            JCRNodeWrapper icon = iconFolder.uploadFile(itemEntry.getName(), itemEntry.getInputStream(),
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
            return new ActionResult(HttpServletResponse.SC_OK, redirectURL, new JSONObject().put(ICON_UPDATE, true).put("iconUrl", icon.getUrl()));
        } catch (Exception e) {
            logger.warn("UpdateModuleIcon: failed to process uploaded icon '{}'",
                    ActionSecurityUtils.sanitizeForLog(itemEntry.getName()), e);
            return wrongFormatResult(session, redirectURL);
        } finally {
            FileUtils.deleteQuietly(f);
        }
    }

    private static ActionResult wrongFormatResult(JCRSessionWrapper session, String redirectURL) throws JSONException {
        String error = Messages.get(RESOURCES, ERR_WRONG_FORMAT, session.getLocale());
        return new ActionResult(HttpServletResponse.SC_OK, redirectURL, new JSONObject().put(ICON_UPDATE, false).put(ERROR_MESSAGE, error));
    }

    public void setImageService(JahiaImageService imageService) {
        this.imageService = imageService;
    }
}
