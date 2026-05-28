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
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.*;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.Locale;

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
                UpdateReferencesForModule module = BundleUtils.getOsgiService(UpdateReferencesForModule.class, null);
                if (module == null) {
                    logger.warn("UpdateReferencesForModule action is not yet available, if this issue persists please restart your DX platform.");
                    return null;
                }
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
                return null;
            }
        });
        logger.info("Finished update of dependencies for missing ones.");

    }
}
