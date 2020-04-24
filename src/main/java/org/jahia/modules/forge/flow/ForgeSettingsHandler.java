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
package org.jahia.modules.forge.flow;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.jahia.modules.forge.actions.PrivateAppStoreAction;
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
 * Flow handler for Private App Store settings
 */
public class ForgeSettingsHandler implements Serializable {

    private static final long serialVersionUID = 2762062442866728163L;

    static Logger logger = LoggerFactory.getLogger(ForgeSettingsHandler.class);

    private ForgeSettings forgeSettings;
    
    public ForgeSettingsHandler() {
        super();
        PrivateAppStoreAction.ensureLicense();
    }

    public ForgeSettings getForgeSettingsBySite(JCRSiteNode site) {
        try {
            if (site != null && site.isNodeType("jmix:forgeSettings"))
            {
                forgeSettings.setPassword(new String (Base64.decode(site.getProperty("forgeSettingsPassword").getString())));
                forgeSettings.setUrl(site.getProperty("forgeSettingsUrl").getString());
                forgeSettings.setId(site.getProperty("forgeSettingsId").getString());
                forgeSettings.setUser(site.getProperty("forgeSettingsUser").getString());
            }
        } catch (RepositoryException e) {
            logger.warn("unable to read Private App Store settings",e);
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
                site.setProperty("forgeSettingsId",forgeSettings.getId());
                site.setProperty("forgeSettingsUser",forgeSettings.getUser());
                site.getSession().save();
                messages.addMessage(new MessageBuilder()
                        .info()
                        .defaultText(
                                Messages.get(ResourceBundles.JAHIA_INTERNAL_RESOURCES, "label.changeSaved",
                                        LocaleContextHolder.getLocale())).build());
            } catch (RepositoryException e) {
                logger.warn("unable to save Private App Store settings",e);
            }
        }


    }

}
