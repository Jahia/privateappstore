/**
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group. All rights reserved.
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
 *
 *
 *
 *
 */
package org.jahia.modules.forge.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.JahiaService;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.image.BufferImage;
import org.jahia.services.image.JahiaImageService;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.tools.files.FileUpload;
import org.jahia.utils.i18n.Messages;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action called to update a module icon
 */
public class UpdateModuleIcon extends PrivateAppStoreAction {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(UpdateModuleIcon.class);

    JahiaImageService imageService;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        // cleanup folder icon
        if (  session.getNode(resource.getNode().getPath()).hasNode("icon")) {
            session.getNode(resource.getNode().getPath()).getNode("icon").remove();
        }
        JCRNodeWrapper iconFolder =  createNode(req, new HashMap<String, List<String>>(),resource.getNode(),"jnt:folder","icon",true);
        JCRNodeWrapper icon;
        final FileUpload fileUpload = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);
        if (fileUpload != null && fileUpload.getFileItems() != null && fileUpload.getFileItems().size() > 0) {
            final Map<String, DiskFileItem> stringDiskFileItemMap = fileUpload.getFileItems();
            DiskFileItem itemEntry = stringDiskFileItemMap.get(stringDiskFileItemMap.keySet().iterator().next());
            if (StringUtils.substringBefore(itemEntry.getContentType(), "/").equals("image")) {
                icon = iconFolder.uploadFile(itemEntry.getName(), itemEntry.getInputStream(),
                        JCRContentUtils.getMimeType(itemEntry.getName(), itemEntry.getContentType()));
                String fileExtension = FilenameUtils.getExtension(icon.getName());

                final File f = File.createTempFile("thumb", "." + fileExtension);
                imageService.createThumb(imageService.getImage(icon), f, 125, false);
                InputStream is = new FileInputStream(f);
                icon = iconFolder.uploadFile(itemEntry.getName(),is,JCRContentUtils.getMimeType(itemEntry.getName(),itemEntry.getContentType()));
                is.close();
                session.save();
                return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("iconUpdate", true).put("iconUrl",icon.getUrl()));
            } else {
                String error = Messages.get("resources.privateappstore", "forge.updateIcon.error.wrong.format", session.getLocale());

                return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("iconUpdate", false).put("errorMessage",error));
            }
        } else {
            String error = Messages.get("resources.privateappstore", "forge.updateIcon.error.noFileFound", session.getLocale());

            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("iconUpdate", false).put("errorMessage",error));
        }
    }

    public void setImageService(JahiaImageService imageService) {
        this.imageService = imageService;
    }
}
