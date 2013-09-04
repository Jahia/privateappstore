package org.jahia.modules.forge.actions;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.tools.files.FileUpload;
import org.jahia.utils.i18n.Messages;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 9/4/13
 * Time: 9:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class UpdateModuleIcon extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(UpdateModuleIcon.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        JCRNodeWrapper icon;
        final FileUpload fileUpload = (FileUpload) req.getAttribute(FileUpload.FILEUPLOAD_ATTRIBUTE);
        if (fileUpload != null && fileUpload.getFileItems() != null && fileUpload.getFileItems().size() > 0) {
            final Map<String, DiskFileItem> stringDiskFileItemMap = fileUpload.getFileItems();
            DiskFileItem itemEntry = stringDiskFileItemMap.get(stringDiskFileItemMap.keySet().iterator().next());
            if (StringUtils.endsWith(StringUtils.lowerCase(itemEntry.getName()),".png")) {
            icon = resource.getNode().uploadFile("icon.png", itemEntry.getInputStream(),
                    JCRContentUtils.getMimeType(itemEntry.getName(), itemEntry.getContentType()));
                session.save();
                return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("iconUpdate", true).put("iconUrl",icon.getUrl()));
            } else {
                String error = Messages.get("resources.Jahia_Forge", "forge.updateIcon.error.wrong.format", session.getLocale());

                return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("iconUpdate", false).put("errorMessage",error));
            }
        }
        logger.warn("Unable to update icon on module " + resource.getNode().getPath());
        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("iconUpdate", false));
    }
}
