package org.jahia.modules.forgeModules.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.mail.MailServiceImpl;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaUser;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Date: 2013-04-23
 *
 * @author Frédéric PIERRE
 * @version 1.0
 */
public class ReportReview extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(AddModule.class);
    private MailServiceImpl mailService;
    private String templatePath;

    public void setMailService(MailServiceImpl mailService) {
        this.mailService = mailService;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JCRNodeWrapper node = resource.getNode();
        JahiaUser user = session.getUser();

        session.checkout(node);

        node.addMixin("jmix:reportedReview");

        mailService.getSettings().getTo();

        try {
            sendNotificationEmail(node, user, session.getLocale());
        } catch (Exception e) {
            logger.error("Cannot send email notification of new review reported as spam", e);
        }

        session.save();

        // TODO
        return null;
    }

    private void sendNotificationEmail(JCRNodeWrapper node, JahiaUser user, Locale locale) throws RepositoryException, ScriptException {

        if (mailService.isEnabled()) {

            // Prepare email to be sent
            String to = mailService.defaultRecipient();
            String from = mailService.defaultSender();
            String cc = null;
            String bcc = null;

            Map<String,Object> bindings = new HashMap<String,Object>();
            bindings.put("reportedNodePath", node.getPath());
            bindings.put("isReview", new Boolean(node.isNodeType("jnt:review")));
            bindings.put("username", user.getUsername());

            mailService.sendMessageWithTemplate(templatePath, bindings, to, from, cc, bcc, locale, "Forge Modules");
        }
    }
}
