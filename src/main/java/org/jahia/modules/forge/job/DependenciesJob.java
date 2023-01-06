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
package org.jahia.modules.forge.job;

import org.jahia.api.Constants;
import org.jahia.modules.forge.actions.UpdateReferencesForModule;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.*;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.Locale;
import java.util.Map;

/**
 * Created by rincevent on 2017-05-25.
 */
public class DependenciesJob extends BackgroundJob {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(DependenciesJob.class);

    @Override
    public void executeJahiaJob(JobExecutionContext jobExecutionContext) throws Exception {
        logger.info("Beginning update of dependencies for missing ones.");
        JCRTemplate.getInstance().doExecute(JahiaUserManagerService.getInstance().getRootUserName(), null, Constants.LIVE_WORKSPACE, Locale.ENGLISH, new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                try {
                    Map<String, UpdateReferencesForModule> beansOfType = SpringContextSingleton.getBeansOfType(UpdateReferencesForModule.class);
                    if (!beansOfType.isEmpty()) {
                        UpdateReferencesForModule module = beansOfType.values().iterator().next();
                        JCRNodeIteratorWrapper nodes = session.getWorkspace().getQueryManager().createQuery("select * from [jnt:forgeModuleVersion] where references is null", Query.JCR_SQL2).execute().getNodes();
                        while (nodes.hasNext()) {
                            JCRNodeWrapper jcrNodeWrapper = (JCRNodeWrapper) nodes.nextNode();
                            if (!jcrNodeWrapper.hasProperty("references")) {
                                logger.info("Updating dependencies of " + jcrNodeWrapper.getDisplayableName());
                                module.executeBackgroundAction(jcrNodeWrapper);
                            } else {
                                logger.info("Skipping updating dependencies of " + jcrNodeWrapper.getDisplayableName() + " as they already exist");
                            }
                        }
                    }
                } catch (NoSuchBeanDefinitionException e) {
                    logger.warn("Notification service is not yet available, if this issue persist please restart your DX platform.");
                }
                return null;
            }
        });
        logger.info("Finished update of dependencies for missing ones.");

    }
}
