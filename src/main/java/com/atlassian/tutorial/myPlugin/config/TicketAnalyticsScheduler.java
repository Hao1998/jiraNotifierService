package com.atlassian.tutorial.myPlugin.config;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.JobRunnerKey;
import com.atlassian.scheduler.config.Schedule;
import com.atlassian.tutorial.myPlugin.services.TicketAnalyticsJob;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class TicketAnalyticsScheduler implements InitializingBean, DisposableBean {

    private static final JobRunnerKey JOB_RUNNER_KEY = JobRunnerKey.of("ticketAnalyticsJobRunner");
    private static final JobId JOB_ID = JobId.of("ticketAnalyticsJob");

    private final SchedulerService schedulerService;
    private final TicketAnalyticsJob ticketAnalyticsJob;

    @Inject
    public TicketAnalyticsScheduler(@ComponentImport SchedulerService schedulerService,
                                    TicketAnalyticsJob ticketAnalyticsJob) {
        this.schedulerService = schedulerService;
        this.ticketAnalyticsJob = ticketAnalyticsJob;
    }

    @Override
    public void destroy() throws Exception {
        schedulerService.unscheduleJob(JOB_ID);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Schedule the job to run daily at midnight
        Schedule schedule = Schedule.forCronExpression("0 0 0 * * ?");

        JobConfig jobConfig = JobConfig.forJobRunnerKey(JOB_RUNNER_KEY)
                .withSchedule(schedule);

        //Register job
        schedulerService.registerJobRunner(JOB_RUNNER_KEY, ticketAnalyticsJob);

        //Schedule job
        schedulerService.scheduleJob(JOB_ID, jobConfig);
    }
}
