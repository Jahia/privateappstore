/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Enterprise Distribution                  =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group. All rights reserved.
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
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
 * JAHIA'S ENTERPRISE DISTRIBUTION - IMPORTANT LICENSING INFORMATION
 * ===============================================================
 *
 *     This file is part of a Jahia Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance
 *     with the terms contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     See the license for the rights, obligations and limitations governing use
 *     of the contents of the software.
 *
 *     For questions regarding licensing, support, production usage,
 *     please contact our team at sales@jahia.com or go to: http://www.jahia.com/license
 */
package org.jahia.modules.forge.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.tools.files.FileUpload;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Date: 2013-03-27
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class EditModuleVersion extends PrivateAppStoreAction {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(EditModuleVersion.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode().getParent();
        JCRNodeWrapper moduleVersion = resource.getNode();

        String moduleTitle = module.getPropertyAsString("jcr:title");
        String versionNumber = moduleVersion.getPropertyAsString("versionNumber");

        logger.info("Start updating module release " + versionNumber + " of " + moduleTitle);

        String relatedJahiaVersionUUID = getParameter(parameters, "relatedJahiaVersion");
        String changeLog = getParameter(parameters, "changeLog");

        session.checkout(moduleVersion);

        if (changeLog != null)
            moduleVersion.setProperty("changeLog", changeLog);

        if (relatedJahiaVersionUUID != null) {
            JCRNodeWrapper relatedJahiaVersion = session.getNodeByUUID(relatedJahiaVersionUUID);
            moduleVersion.setProperty("relatedJahiaVersion",relatedJahiaVersion);
        }

        final FileUpload fu = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);

        DiskFileItem moduleVersionBinary = fu.getFileItems().get("moduleVersionBinary");

        if (moduleVersionBinary != null)
            moduleVersion.uploadFile(moduleVersionBinary.getName(), moduleVersionBinary.getInputStream(), moduleVersionBinary.getContentType());

        session.save();

        logger.info("Module release " + versionNumber + " of " + moduleTitle + " successfully updated");

        // TODO
        return new ActionResult(HttpServletResponse.SC_OK, moduleVersion.getPath(), Render.serializeNodeToJSON(moduleVersion));

    }

}
