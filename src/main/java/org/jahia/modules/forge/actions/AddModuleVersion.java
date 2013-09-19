package org.jahia.modules.forge.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.taglibs.jcr.node.JCRTagUtils;
import org.jahia.tools.files.FileUpload;
import org.jahia.utils.i18n.JahiaResourceBundle;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author faissah
 */
public class AddModuleVersion extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(AddModuleVersion.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper module = resource.getNode();
        String moduleTitle = module.getProperty("jcr:title").getString();

        logger.info("Start adding module version of module '" + moduleTitle + "'");

        final FileUpload fu = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);
        DiskFileItem moduleVersionBinary = fu.getFileItems().get("moduleVersionBinary");

        if (StringUtils.contains(moduleVersionBinary.getName(), "-SNAPSHOT.")) {
            String error = new JahiaResourceBundle(session.getLocale(), "Jahia Forge")
                    .get("forge.uploadJar.error.snapshot.not.allowed", "SNAPSHOT versions not allowed");
            return new ActionResult(HttpServletResponse.SC_OK, module.getPath() + ".forge-module-add-version", new JSONObject().put("error", error));
        }

        Manifest manifest = new JarFile(moduleVersionBinary.getStoreLocation()).getManifest();

        String versionNumber = manifest.getMainAttributes().getValue("Implementation-Version");
        boolean hasModuleVersions = JCRTagUtils.hasChildrenOfType(module, "jnt:forgeModuleVersion");
        if (hasModuleVersions && !hasValidVersionNumber(module, versionNumber)) {
            return new ActionResult(HttpServletResponse.SC_OK, module.getPath(), new JSONObject().put("error", "versionNumber"));
        }
        JCRNodeWrapper moduleVersion = createNode(req, parameters, module, "jnt:forgeModuleVersion", module.getName()+"-"+versionNumber, false);
        moduleVersion.setProperty("versionNumber", versionNumber);

        if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            List<String> roles = Arrays.asList("owner");
            moduleVersion.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
        }

        if (!module.hasProperty("groupId")) {
            String groupId = manifest.getMainAttributes().getValue("Jahia-GroupId");
            if (groupId != null) {
                session.checkout(module);
                module.setProperty("groupId", groupId);
            }
        }

        String moduleVersionBinaryName = moduleVersionBinary.getName();
        String moduleVersionBinaryExtension = moduleVersionBinaryName.substring(moduleVersionBinaryName.lastIndexOf('.') + 1).toLowerCase();

        Tika tika = new Tika();
        String mediaType = tika.detect(moduleVersionBinary.getInputStream());

        if ( !( (moduleVersionBinaryExtension.equals("war") || moduleVersionBinaryExtension.equals("jar"))
                && mediaType.equals("application/zip") )) {
            return ActionResult.BAD_REQUEST;
        }

        final JCRNodeWrapper file =
                moduleVersion.uploadFile(moduleVersionBinary.getName(), moduleVersionBinary.getInputStream(), moduleVersionBinary.getContentType());
        String url = file.getAbsoluteWebdavUrl(req);
        moduleVersion.setProperty("url", url);

        session.save();

        logger.info("Module version " + versionNumber + " of '" + moduleTitle + "' successfully added");

        return new ActionResult(HttpServletResponse.SC_OK, module.getPath(), Render.serializeNodeToJSON(moduleVersion));

    }

    private boolean hasValidVersionNumber(JCRNodeWrapper module, String newVersionNumber) throws RepositoryException {
        List<JCRNodeWrapper> moduleVersions = JCRTagUtils.getChildrenOfType(module, "jnt:forgeModuleVersion");
        for (JCRNodeWrapper moduleVersion : moduleVersions) {
            if (moduleVersion.getProperty("versionNumber").getString().equals(newVersionNumber)) {
                return false;
            }
        }
        return true;
    }

}