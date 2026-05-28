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
 *     Copyright (C) 2002-2026 Jahia Solutions Group. All rights reserved.
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

import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.scheduler.SchedulerService;
import org.jahia.settings.SettingsBean;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi DS component that schedules {@link DependenciesJob} as a RAM Quartz job on a
 * daily cron (02:15) - replaces the previous Spring jobSchedulingBean wiring.
 */
@Component(immediate = true, service = DependenciesJobScheduler.class)
public class DependenciesJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DependenciesJobScheduler.class);
    private static final String CRON_EXPRESSION = "0 15 2 * * ?";
    private static final String JOB_NAME = "ForgeDependenciesJob";

    private SchedulerService schedulerService;
    private JobDetail jobDetail;

    @Activate
    public void start() throws Exception {
        jobDetail = BackgroundJob.createJahiaJob(JOB_NAME, DependenciesJob.class);
        if (!SettingsBean.getInstance().isProcessingServer()) {
            return;
        }
        if (schedulerService.getAllJobs(jobDetail.getGroup()).isEmpty()) {
            Trigger trigger = new CronTrigger(JOB_NAME, jobDetail.getGroup(), CRON_EXPRESSION);
            schedulerService.getRAMScheduler().scheduleJob(jobDetail, trigger);
            logger.info("Scheduled {} with cron {}", JOB_NAME, CRON_EXPRESSION);
        }
    }

    @Deactivate
    public void stop() throws Exception {
        if (jobDetail == null || !SettingsBean.getInstance().isProcessingServer()) {
            return;
        }
        if (!schedulerService.getAllJobs(jobDetail.getGroup()).isEmpty()) {
            schedulerService.getRAMScheduler().deleteJob(jobDetail.getName(), jobDetail.getGroup());
            logger.info("Unscheduled {}", JOB_NAME);
        }
    }

    @Reference
    public void setSchedulerService(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }
}
