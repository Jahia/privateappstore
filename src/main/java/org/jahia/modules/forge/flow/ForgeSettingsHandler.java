package org.jahia.modules.forge.flow;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.utils.i18n.Messages;
import org.jahia.utils.i18n.ResourceBundles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.i18n.LocaleContextHolder;

import javax.jcr.RepositoryException;
import java.io.Serializable;

/**
 * Flow handler for forge settings
 */
public class ForgeSettingsHandler implements Serializable {

    private static final long serialVersionUID = 2762062442866728163L;

    static Logger logger = LoggerFactory.getLogger(ForgeSettingsHandler.class);

    private ForgeSettings forgeSettings;

    public ForgeSettings getForgeSettingsBySite(JCRSiteNode site) {
        try {
            if (site != null && site.isNodeType("jmix:forgeSettings"))
            {
                if (site.hasProperty("forgeSettingsPassword")) {
                    forgeSettings.setOriginPassword(new String (Base64.decode(site.getProperty("forgeSettingsPassword").getString())));
                }
                forgeSettings.setUrl(site.getProperty("forgeSettingsUrl").getString());
                forgeSettings.setUser(site.getProperty("forgeSettingsUser").getString());
                forgeSettings.setGroupId(site.getProperty("forgeSettingsGroupID").getString());
            }
        } catch (RepositoryException e) {
            logger.warn("unable to read forge settings",e);
        }

        return forgeSettings;
    }

    public void init() {
        forgeSettings = new ForgeSettings();
    }

    public void save(MessageContext messages, JCRSiteNode site) {
        if (site != null) {
            try {
                if (!site.isNodeType("jmix:forgeSettings")) {
                    site.addMixin("jmix:forgeSettings");
                }
                if (StringUtils.isNotBlank(forgeSettings.getPassword())) {
                    site.setProperty("forgeSettingsPassword", Base64.encode(forgeSettings.getPassword().getBytes()));
                }
                site.setProperty("forgeSettingsUrl",forgeSettings.getUrl());
                site.setProperty("forgeSettingsUser",forgeSettings.getUser());
                site.setProperty("forgeSettingsGroupID",forgeSettings.getGroupId());
                site.setProperty("forgeSettingsSnapshotRepository",forgeSettings.getSnapshotRepository());
                site.setProperty("forgeSettingsReleaseRepository",forgeSettings.getReleaseRepository());
                site.getSession().save();
                messages.addMessage(new MessageBuilder()
                        .info()
                        .defaultText(
                                Messages.get(ResourceBundles.JAHIA_INTERNAL_RESOURCES, "label.changeSaved",
                                        LocaleContextHolder.getLocale())).build());
            } catch (RepositoryException e) {
                logger.warn("unable to save forge settings",e);
            }
        }


    }

}
