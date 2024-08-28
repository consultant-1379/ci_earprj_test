/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.services.shm.es.api.ActivityStepDurationsReportGenerator;

/**
 * This class listens for the configuration parameter value change for Job query.
 * 
 * @author xmanush
 * 
 */
@ApplicationScoped
public class JobParameterChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobParameterChangeListener.class);

    private static final String JOB_QUERY_BATCH_SIZE = "job_query_max_batch_size";
    private static final String INSTRUMENTATION_LOCK = "instrumentation_lock";
    private static final String JOB_NAME_FOR_JOB_REPORT_GENERATION = "job_name_to_generate_report";
    private static final String RUNNING_MAIN_JOBS_BATCH_SIZE = "running_main_jobs_batch_size";
    private static final String ACTIVE_USER_SESSIONS_MAX_LIMIT = "activeUserSessionsMaxLimit";

    @Inject
    @Configured(propertyName = JOB_QUERY_BATCH_SIZE)
    private int jobBatchSize;

    @Inject
    @Configured(propertyName = INSTRUMENTATION_LOCK)
    private int instrumentationRetry;

    @Inject
    @Configured(propertyName = JOB_NAME_FOR_JOB_REPORT_GENERATION)
    private String jobName;

    @Inject
    @Configured(propertyName = RUNNING_MAIN_JOBS_BATCH_SIZE)
    private int runningMainJobsBatchSize;

    @Inject
    @Configured(propertyName = ACTIVE_USER_SESSIONS_MAX_LIMIT)
    private int activeUserSessionsMaxLimit;

    @Inject
    private ActivityStepDurationsReportGenerator activityStepDurationsReportGenerator;

    /**
     * Listener for jobBatch size attribute value
     * 
     * @param jobBatchSize
     */
    void listenForJobBatchAttribute(@Observes @ConfigurationChangeNotification(propertyName = JOB_QUERY_BATCH_SIZE) final int jobBatchSize) {
        this.jobBatchSize = jobBatchSize;
        LOGGER.info("JobBatchSize CM Parameter value : {}", jobBatchSize);
    }

    public int getJobBatchSize() {
        return jobBatchSize;
    }

    /**
     * Listener for instrumentationRetry attribute value
     * 
     * @param instrumentationRetry
     */
    void listenForInstrumentatioRetryAttribute(@Observes @ConfigurationChangeNotification(propertyName = INSTRUMENTATION_LOCK) final int instrumentationRetry) {
        this.instrumentationRetry = instrumentationRetry;
        LOGGER.info("Instrumentation Retry Parameter : {}, value : {}", INSTRUMENTATION_LOCK, instrumentationRetry);
    }

    public int getInstrumentationRetryAttempt() {
        return instrumentationRetry;
    }

    @Inject
    @Configured(propertyName = "SHM_PERFORM_FAILSAFE_BACKUP")
    private boolean performFailsafeBackup;

    /**
     * Listener for PerformActivateDeactivate attribute value
     * 
     * @param jobBatchSize
     */
    void listenForPerformActivateDeactivate(@Observes @ConfigurationChangeNotification(propertyName = "SHM_PERFORM_FAILSAFE_BACKUP") final boolean performFailsafeBackup) {
        this.performFailsafeBackup = performFailsafeBackup;
        LOGGER.info("performFailsafeBackup : {}", performFailsafeBackup);
    }

    public boolean getPerformFailsafeBackup() {
        return performFailsafeBackup;
    }

    /**
     * Listener for job report generation attribute value.
     * 
     * @param jobName
     */
    public void listenForJobNameAttribute(@Observes @ConfigurationChangeNotification(propertyName = JOB_NAME_FOR_JOB_REPORT_GENERATION) final String jobName) {
        this.jobName = jobName;
        LOGGER.info("\"JOB_NAME_FOR_JOB_REPORT_GENERATION\" parameter value : {}", jobName);
        activityStepDurationsReportGenerator.triggerJobReportGenerationThroughPibScript(jobName);
    }

    public String getJobName() {
        return this.jobName;
    }

    /**
     * Listener for runningMainJobsBatchSize attribute value
     * 
     * @param runningMainJobsBatchSize
     */
    void listenForRunningMainJobsBatchSize(@Observes @ConfigurationChangeNotification(propertyName = RUNNING_MAIN_JOBS_BATCH_SIZE) final int runningMainJobsBatchSize) {
        this.runningMainJobsBatchSize = runningMainJobsBatchSize;
        LOGGER.info("runningMainJobsBatchSize Parameter value : {}", runningMainJobsBatchSize);
    }

    public int getRunningMainJobsBatchSize() {
        return runningMainJobsBatchSize;
    }

    void listenActiveSessionsMaxLimit(@Observes @ConfigurationChangeNotification(propertyName = ACTIVE_USER_SESSIONS_MAX_LIMIT) final int activeUserSessionsMaxLimit) {
        LOGGER.info("activeSessionsMaxLimit changed from {} to {}", this.activeUserSessionsMaxLimit, activeUserSessionsMaxLimit);
        this.activeUserSessionsMaxLimit = activeUserSessionsMaxLimit;
    }

    public int getActiveUserSessionsMaxLimit() {
        return activeUserSessionsMaxLimit;
    }

}
