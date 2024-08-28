/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.es.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@ApplicationScoped
@Traceable
public class JobUpdateServiceImpl implements JobUpdateService {

    private static final int NUMBER_OF_DEFAULT_DPS_RETRIES = 5;

    private final static Logger LOGGER = LoggerFactory.getLogger(JobUpdateServiceImpl.class);

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    JobStatusUpdateService jobStatusUpdateService;

    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Inject
    NEJobStatusUpdater neJobStatusUpdater;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsPolicies;

    @Inject
    private DpsRetryConfigurationParamProvider dpsConfigurationParamProvider;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    /**
     * //TODO - This should retry incase if there are any exceptions only, not incase of update failed due to some other reasons. Eg: it will fail , if the given job is a not a running job. then also
     * retries.
     */
    @Override
    public boolean updateRunningJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {

        boolean isJobAttributesPersisted = false;
        final int noOfRetries = getNoOfRetries();
        for (int i = 1; i <= noOfRetries; i++) {
            try {
                isJobAttributesPersisted = jobConfigurationService.persistRunningJobAttributes(jobId, jobPropertyList, jobLogList);
                LOGGER.info("Successfully updated job attributes for jobId {} in trial {} with log update - {} and job property update - {}", jobId, i, jobLogList, jobPropertyList);
                break;
            } catch (final Exception ex) {
                if (i == noOfRetries) {
                    LOGGER.error("Retries exhausted for job attributes update for JobId {} , discarding log update - {}, discarding job property update - {} with exception :", jobId, jobLogList,
                            jobPropertyList, ex.getMessage());
                } else {
                    sleep(ex);
                }
            }
        }
        return isJobAttributesPersisted;
    }

    @Override
    public boolean readAndUpdateRunningJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
        boolean isJobAttributesPersisted = false;
        final int noOfRetries = getNoOfRetries();
        int i = 1;
        for (; i <= noOfRetries; i++) {
            try {
                isJobAttributesPersisted = jobConfigurationService.readAndPersistRunningJobAttributes(jobId, jobPropertyList, jobLogList, null);
                LOGGER.info("Successfully updated job attributes for jobId {} in trial {} with log update - {} and job property update - {}", jobId, i, jobLogList, jobPropertyList);
                break;
            } catch (final Exception ex) {
                if (i == noOfRetries) {
                    LOGGER.error("Retries exhausted for job attributes update for JobId {} , discarding log update - {}, discarding job property update - {} with exception :", jobId, jobLogList,
                            jobPropertyList, ex.getMessage());
                } else {
                    sleep(ex);
                }
            }
        }
        return isJobAttributesPersisted;
    }

    /**
     * @param jobId
     * @param attributes
     */
    @Override
    public void updateJobAttributes(final long jobId, final Map<String, Object> attributes) {
        final int noOfRetries = getNoOfRetries();
        for (int i = 1; i <= noOfRetries; i++) {
            try {
                dpsWriter.update(jobId, attributes);
                LOGGER.info("Successfully updated job attributes for jobId {} in trial {} with job attributes {}", jobId, i, attributes);
                break;
            } catch (final Exception ex) {
                if (i == noOfRetries) {
                    LOGGER.error("Retries exhausted for job attributes update for JobId {} , discarding job attributes update - {} with exception:", jobId, attributes, ex.getMessage());
                } else {
                    sleep(ex);
                }
            }
        }
    }

    /**
     * @param jobId
     * @param attributes
     */
    @Override
    public void updateJobAttributes(final long jobId, final Map<String, Object> attributes, final int noOfRetries) {
        for (int index = 1; index <= noOfRetries; index++) {
            try {
                dpsWriter.update(jobId, attributes);
                LOGGER.info("Successfully updated job attributes for jobId {} in trial {} with job attributes {}", jobId, index, attributes);
                break;
            } catch (final Exception ex) {
                if (index == noOfRetries) {
                    LOGGER.error("Retries exhausted for job attributes update for JobId {} , discarding job attributes update - {} with exception:", jobId, attributes, ex.getMessage());
                    throw ex;
                } else {
                    sleep(ex);
                }
            }
        }
    }

    /**
     * This method updates the NE jobs completed count in Main JobProperty. Also it checks whether all NE Jobs are done.
     * 
     * @param jobId
     * @return boolean
     */
    @Override
    @Deprecated
    public void updateNEJobsCompletedCount(final long mainJobId) {
        LOGGER.debug("Updating completed NE Jobs count for {}", mainJobId);
        boolean neJobDone = false;
        final int noOfRetries = getNoOfRetries();
        for (int i = 1; i <= noOfRetries; i++) {
            try {
                neJobDone = jobStatusUpdateService.updateNEJobsCompletedCount(mainJobId);
                LOGGER.info("Successfully update NE jobs completed status for mainJobId {} in trial {}", mainJobId, i);
                break;
            } catch (final Exception ex) {
                if (i == noOfRetries) {
                    LOGGER.error("Retries exhausted for NE Jobs count update for JobId {} has failed because :", mainJobId, ex.getMessage());
                } else {
                    sleep(ex);
                }
            }
        }
        sendAllNeDoneToWFS(mainJobId, neJobDone);
    }

    /**
     * @param mainJobId
     * @param neJobDone
     */
    private void sendAllNeDoneToWFS(final long mainJobId, final boolean neJobDone) {
        final Map<String, Object> mainJobAttributes = getPoAttributes(mainJobId);
        final long templateJobId = (long) mainJobAttributes.get(ShmConstants.JOB_TEMPLATE_ID);
        if (neJobDone && templateJobId != -1) {
            final boolean isMsgCorrelated = workflowInstanceNotifier.sendAllNeDone(Long.toString(templateJobId));
            LOGGER.debug("All NE Job Done message sent to wrokflow status is [isMsgCorrelated == {} ] for : {}", isMsgCorrelated, templateJobId);
        }
    }

    /**
     * Method to wait the control to wait for defined time.
     * 
     * wait interval may vary depending on DPS unavailable , Optimistic Lock issue.
     * 
     */
    private void sleep(final Exception exception) {
        try {
            if (exception instanceof EJBException && dpsAvailabilityInfoProvider.isDatabaseDown()) {
                LOGGER.warn("Database is down, waiting to retry");
                Thread.sleep(dpsConfigurationParamProvider.getdpsWaitIntervalInMS());
            } else if (exception instanceof EJBTransactionRolledbackException) {
                LOGGER.warn("Optimistic Lock issue occurred, waiting to retry");
                Thread.sleep(dpsConfigurationParamProvider.getDpsOptimisticLockWaitIntervalInMS());
            }
        } catch (final InterruptedException ie) {
            LOGGER.error("Job updation failed, because:", ie);
            Thread.currentThread().interrupt();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.JobUpdateService# updateNEJobEndAttributes(long)
     */
    @Override
    public void updateNEJobEndAttributes(final long neJobId) {
        LOGGER.debug("Inside JobUpdateServiceImpl.updateNEJobEndAttributes of JobUpdateServiceImpl with neJobId : {}", neJobId);
        boolean neJobDone = false;
        final int noOfRetries = getNoOfRetries();

        for (int i = 1; i <= noOfRetries; i++) {
            try {
                neJobDone = neJobStatusUpdater.updateNEJobEndAttributes(neJobId);
                LOGGER.info("Successfully update NEJobEndAttributes for neJobId {} in trial {}", neJobId, i);
                break;
            } catch (final Exception ex) {
                if (i == noOfRetries) {
                    LOGGER.error("Retries exhausted for NE job attributes update for neJobId {} has failed because :", neJobId, ex);
                } else {
                    sleep(ex);
                }
            }
        }
        final Map<String, Object> neJobPoAttributes = getPoAttributes(neJobId);
        final long mainJobId = (long) neJobPoAttributes.get(ShmConstants.MAIN_JOB_ID);
        sendAllNeDoneToWFS(mainJobId, neJobDone);

    }

    /**
     * Retrieves the Po Attributes for given ID, with retry mechanism
     * 
     * @param poId
     * @return
     */
    private Map<String, Object> getPoAttributes(final long poId) {
        final Map<String, Object> poAttributes = retryManager.executeCommand(dpsPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
            @Override
            public Map<String, Object> execute() {
                return retrieveJobWithRetry(poId);
            }
        });
        return poAttributes;
    }

    protected int getNoOfRetries() {
        final int configuredRetries = dpsConfigurationParamProvider.getdpsRetryCount();
        return configuredRetries > NUMBER_OF_DEFAULT_DPS_RETRIES ? configuredRetries : NUMBER_OF_DEFAULT_DPS_RETRIES;
    }

    @Override
    public boolean addOrUpdateOrRemoveJobProperties(final long jobId, final Map<String, String> propertyTobeAdded, final List<Map<String, Object>> jobLogList) {
        boolean isPersisted = false;
        try {
            isPersisted = retryManager.executeCommand(dpsPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() {
                    jobConfigurationService.addOrUpdateOrRemoveJobProperties(jobId, propertyTobeAdded, jobLogList);
                    return true;
                }
            });
        } catch (final Exception exception) {
            LOGGER.error("Failed to update the Job Properties and Job Logs due to ", exception);
            return isPersisted;
        }
        return isPersisted;
    }

    @Override
    public Map<String, Object> retrieveJobWithRetry(final long jobId) {

        final int noOfRetries = getNoOfRetries();
        Map<String, Object> poAttribute = new HashMap<String, Object>();
        for (int retryIndex = 1; retryIndex <= noOfRetries; retryIndex++) {
            try {
                LOGGER.debug("Reading job attributes for job: {} with trial: {} ", jobId, retryIndex);
                poAttribute = jobConfigurationService.retrieveJob(jobId);
                if (poAttribute != null && !poAttribute.isEmpty()) {
                    break;
                }
            } catch (final Exception ex) {
                if (retryIndex == noOfRetries) {
                    LOGGER.error("Retries exhausted for job attributes retrieval for the jobId {} with exception:", jobId, ex.getMessage());
                } else {
                    sleep(ex);
                }
            }
        }
        return poAttribute;

    }

    /**
     * //TODO - This should retry incase if there are any exceptions only, not incase of update failed due to some other reasons. Eg: it will fail , if the given job is a not a running job. then also
     * retries.
     */
    @Override
    public boolean readAndUpdateJobAttributesForCancel(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {

        boolean isJobAttributesPersisted = false;
        final int noOfRetries = getNoOfRetries();
        for (int i = 1; i <= noOfRetries; i++) {
            try {
                isJobAttributesPersisted = jobConfigurationService.readAndPersistJobAttributesForCancel(jobId, jobPropertyList, jobLogList);
                LOGGER.info("Successfully updated job attributes for cancel for jobID {} in trial {}", jobId, i);
                break;
            } catch (final Exception ex) {
                if (i == noOfRetries) {
                    LOGGER.error("Retries exhausted for cancel for job attributes update for JobId {}, discarding log update - {}, discarding job property update - {} with exception:", jobId,
                            jobLogList, jobPropertyList, ex.getMessage());
                } else {
                    sleep(ex);
                }
            }
        }
        return isJobAttributesPersisted;
    }

    @Override
    public boolean readAndUpdateRunningJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList,
            final Double activityProgressPercentage) {
        boolean isJobAttributesPersisted = false;
        final int noOfRetries = getNoOfRetries();
        int i = 1;
        for (; i <= noOfRetries; i++) {
            try {
                isJobAttributesPersisted = jobConfigurationService.readAndPersistRunningJobAttributes(jobId, jobPropertyList, jobLogList, activityProgressPercentage);
                LOGGER.info("Successfully updated job attributes for jobId {} in trial with log update {}, job property update{} and activityProgressPercentage {}", jobId, jobLogList,
                        jobPropertyList, activityProgressPercentage);
                break;
            } catch (final Exception ex) {
                if (i == noOfRetries) {
                    LOGGER.error("Retries exhausted for job attributes update for JobId={} , discarding log update - {}, discarding job property update - {} with exception:", jobId, jobLogList,
                            jobPropertyList, ex.getMessage());
                } else {
                    sleep(ex);
                }
            }
        }
        return isJobAttributesPersisted;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.JobUpdateService#readAndUpdateStepDurations(long, java.lang.String, java.lang.String)
     */
    @Override
    public boolean readAndUpdateStepDurations(final long jobId, final String stepNameAndDurationToPersist, final String stepName) {
        boolean isJobAttributesPersisted = false;
        final int noOfRetries = getNoOfRetries();
        int i = 1;
        for (; i <= noOfRetries; i++) {
            try {
                isJobAttributesPersisted = jobConfigurationService.readAndPersistRunningJobStepDuration(jobId, stepNameAndDurationToPersist, stepName);
                LOGGER.info("Sucessfully updated the job having id {} with attempt {} to persist step durations as {} for {}", jobId, i, stepNameAndDurationToPersist, stepName);
                break;
            } catch (final Exception ex) {
                if (i == noOfRetries) {
                    LOGGER.error("Number of retries completed but could not persist step duration data as {} retries exhausted with jobId : {} for {} in {} with exception:", i, jobId,
                            stepNameAndDurationToPersist, stepName, ex.getMessage());
                }
                try {
                    sleep(ex);
                } catch (final Exception sleepEx) {
                    // log and proceed, do not fail the job.
                    LOGGER.error("Exception occurred while trying to sleep in {}.readAndUpdateStepDurations()," + " reason : {}", JobUpdateServiceImpl.class.getName(), sleepEx.getMessage());
                }
            }
        }
        return isJobAttributesPersisted;
    }

    @Override
    public void updateActivityAsSkipped(final long activityJobPoId) {
        LOGGER.debug("Updating activityJob result and state for activityJobId : {}", activityJobPoId);
        final Map<String, Object> activityJobAttributes = new HashMap<>();
        activityJobAttributes.put(ShmConstants.RESULT, JobResult.SKIPPED.getJobResult());
        activityJobAttributes.put(ShmConstants.STATE, JobState.COMPLETED.getJobStateName());
        activityJobAttributes.put(ShmConstants.ENDTIME, new Date());
        updateJobAttributes(activityJobPoId, activityJobAttributes);
        LOGGER.debug("Successfully updated result : {} and state : {} in ActivityJob PO having activityJobId as : {}", JobResult.SKIPPED, JobState.COMPLETED.getJobStateName(), activityJobPoId);
    }

    @Override
    public void updateJobAttributesWihoutRetries(final long jobId, final Map<String, Object> attributes) {
        dpsWriter.update(jobId, attributes);
        LOGGER.debug("Successfully updated job attributes for jobId {} with job attributes {}", jobId, attributes);
    }

}
