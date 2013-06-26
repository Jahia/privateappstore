package org.jahia.modules.forge.actions;

import org.apache.jackrabbit.spi.commons.query.sql.JCRSQLQueryBuilder;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.bin.SystemAction;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.query.QueryResultWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.jcr.JCRUser;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Date: 2013-05-03
 *
 * @author Frédéric PIERRE
 * @version 1.0
 * [jnt:forgeModule] > jnt:content, mix:title, jmix:editorialContent, jmix:tagged, jmix:forge, jmix:reviews, jmix:rating
- jcr:title (string) mandatory
- description (string, richtext) i18n mandatory
- category (weakreference, choicelist[nodes='$currentSite/contents/forge-modules-categories;jnt:text']) facetable
- icon (weakreference, picker[type='image'])
- authorNameDisplayedAs (string, choicelist[resourceBundle]) = 'username' autocreated < 'username', 'fullName', 'organisation'
- authorURL (string)
- authorEmail (string)
- howToInstall (string, richtext) i18n
- FAQ (string, richtext) i18n
- codeRepository (string)
- license (weakreference,category[root='forge-licenses'])
- downloadCount (long) = 0 hidden onconflict=latest autocreated
- supportedByJahia (boolean) = false autocreated
- reviewedByJahia (boolean) = false autocreated
- published (boolean) = false autocreated
- deleted (boolean) = false autocreated hidden
+ screenshots (jnt:forgeModuleScreenshotsList) = jnt:forgeModuleScreenshotsList autocreated hidden
+ video (jnt:videostreaming) = jnt:videostreaming
+ * (jnt:forgeModuleVersion)

[jnt:forgeModuleVersion]> jnt:content, jmix:editorialContent, jmix:forge
- requiredVersion (weakreference, choicelist[nodes='$currentSite/contents/forge-modules-required-versions//*;jnt:text'])
- releaseType (string, choicelist[resourceBundle]) = 'release' < 'release', 'hotfix', 'service-pack', 'upgrade'
- status (weakreference,category[root='forge-status'])
- versionNumber (string)
- fileDsaSignature (string)
- changeLog (string, richtext)
- activeVersion (boolean) = false autocreated
- url (string)
 */
public class CreateModule extends SystemAction {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(CreateModule.class);
    private AddModuleVersion addModuleVersion;


    @Override
    public ActionResult doExecuteAsSystem(HttpServletRequest req, RenderContext renderContext, JCRSessionWrapper session, Resource resource, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        if (session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("error", "guestNotAllowed"));
        }

        List<String> moduleParamKeys = Arrays.asList("jcr:title","description", "category", "icon", "authorNameDisplayedAs", "authorURL", "authorEmail", "FAQ", "codeRepository", "license", "downloadCount", "supportedByJahia", "reviewedByJahia", "published", "deleted", "screenshots", "video");
        List<String> versionParamKeys = Arrays.asList("jcr:title","requiredVersion", "releaseType", "status", "versionNumber", "fileDsaSignature", "changeLog", "activeVersion", "url");
        Map<String, List<String>> moduleParameters = new HashMap<String, List<String>>();
        Map<String, List<String>> versionParameters = new HashMap<String, List<String>>();

        for (String key : parameters.keySet()) {
            if (moduleParamKeys.contains(key)) {
                moduleParameters.put(key,parameters.get(key));
            } else if (versionParamKeys.contains(key)) {
                versionParameters.put(key,parameters.get(key));
            }
        }

        String title = getParameter(moduleParameters, "jcr:title");
        JCRNodeWrapper repository = resource.getNode();

        logger.info("Start creating Forge Module " + title);

        QueryManager queryManager = session.getWorkspace().getQueryManager();

        StringBuilder sb = new StringBuilder("SELECT * FROM [jnt:forgeModule] as module");
        sb.append(" WHERE isdescendantnode(module, ['").append(repository.getPath()).append("'])");
        sb.append(" AND module.[jcr:title] = \"").append(title).append("\"");

        String query = sb.toString();
        Query q = queryManager.createQuery(query, Query.JCR_SQL2);
        QueryResultWrapper queryResult = (QueryResultWrapper) q.execute();
        JCRNodeWrapper module;

        NodeIterator moduleNodes = queryResult.getNodes();
        if (moduleNodes.getSize() == 0) {
            module = createNode(req, moduleParameters, repository, "jnt:forgeModule", title, false);
        }  else {
            ;module = (JCRNodeWrapper) moduleNodes.nextNode();
        }

        if (!session.getUser().getUsername().equals(Constants.GUEST_USERNAME)) {
            List<String> roles = Arrays.asList("owner");
            module.grantRoles("u:" + session.getUser().getUsername(), new HashSet<String>(roles));
        }

        addModuleVersion.doExecuteAsSystem(req,renderContext,session,new Resource(module,resource.getTemplateType(),resource.getTemplate(),resource.getContextConfiguration()),parameters,urlResolver);

        session.save();

        logger.info("Forge Module " + title + " successfully created and added to forge repository " + repository.getPath());

        return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject().put("moduleUrl", module.getUrl()));
    }

    public void setAddModuleVersion(AddModuleVersion addModuleVersion) {
        this.addModuleVersion = addModuleVersion;
    }
}
