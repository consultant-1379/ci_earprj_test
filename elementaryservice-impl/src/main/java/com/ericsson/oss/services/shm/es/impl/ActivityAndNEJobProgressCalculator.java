/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl;

import static com.ericsson.oss.services.shm.shared.constants.ActivityConstants.TOTAL_ACTIVITIES;
import static com.ericsson.oss.services.shm.shared.constants.ActivityConstants.TOTAL_ACTIVITIES_PROGRESS_PERCENTAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * This class facilitates the calculation and updation of Activity job and NE job Progress percentages of CPP Nodes for every Activity of Job.
 *
 * @author tcsnean
 *
 */
public class ActivityAndNEJobProgressCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityAndNEJobProgressCalculator.class);

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private JobConfigurationService jobConfigurationService;

    /**
     * This method updates the ActivityJobProgressPercentage for every Activity.
     *
     * @param activityJobId
     * @param jobPropertyList
     * @param jobLogList
     * @param activityProgressPercentage
     *
     */
    public boolean updateActivityJobProgressPercentage(final long activityJobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList,
            final double activityProgressPercentage) {
        LOGGER.trace("For activityJobId: {} Updating the Activity Job progress percentage :{}", activityJobId, activityProgressPercentage);
        return jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, activityProgressPercentage);
    }

    /**
     * This method updates the NEJobProgressPercentag for every Activity.
     *
     * @param neJobId
     *
     */
    public void updateNEJobProgressPercentage(final long neJobId) {
        final double progressPercentage = computeNEJobProgressPercentage(neJobId);
        LOGGER.debug("For neJobId: {} Updating the NE progress percentage to :{}", neJobId, progressPercentage);
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, progressPercentage);
        jobUpdateService.updateJobAttributes(neJobId, neJobAttributes);
    }

    /**
     * This method updates the NEJobProgressPercentag for every Activity.
     *
     * @param neJobId
     * @param noOfRetries
     *
     */
    public void updateNEJobProgressWithRetry(final long neJobId, final int noOfRetries) {
        final double progressPercentage = computeNEJobProgressPercentage(neJobId);
        LOGGER.debug("For neJobId: {} Updating the NE progress percentage to :{}", neJobId, progressPercentage);
        final Map<String, Object> neJobAttributes = new HashMap<String, Object>();
        neJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, progressPercentage);
        jobUpdateService.updateJobAttributes(neJobId, neJobAttributes, noOfRetries);
    }

    private double computeNEJobProgressPercentage(final long neJobId) {
        double progressPercentage = 0;
        final Map<String, Object> activitiesCount = getActivitiesCountAndPercentage(neJobId);
        final double totalActivities = (int) activitiesCount.get(TOTAL_ACTIVITIES);
        final double totalActivitiesPercentage = (double) activitiesCount.get(TOTAL_ACTIVITIES_PROGRESS_PERCENTAGE);
        LOGGER.debug("For neJobId: {} count of Total activities: {} count of total activities percentage:{}", neJobId, totalActivities, totalActivitiesPercentage);
        if (totalActivities > 0) {
            progressPercentage = (totalActivitiesPercentage) / (totalActivities);
            progressPercentage = Math.round(progressPercentage * 100);
            progressPercentage = progressPercentage / 100;
            LOGGER.trace("For neJobId: {} present NE Progress percentage: {}", neJobId, progressPercentage);
        }
        return progressPercentage;
    }

    private Map<String, Object> getActivitiesCountAndPercentage(final long poId) {
        final Map<String, Object> poAttributes = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
            @Override
            public Map<String, Object> execute() {
                return jobConfigurationService.getActivitiesCountAndPercentage(poId);
            }
        });
        return poAttributes;
    }

    /**
     * This method calculates the ActivityJobProgressPercentage For MultipleBackups for every Activity.
     *
     * This method has been Deprecated, use {@link ActivityAndNEJobProgressCalculator.calculateActivityProgressPercentage(long, int, double)}.
     *
     * @param activityJobId
     * @param jobPropertyList
     * @param jobLogList
     * @param activityProgressPercentage
     *
     */
    @Deprecated
    public double calculateActivityProgressPercentage(final JobEnvironment jobEnvironment, final int repeatActivityCount, final double progressPercentage) {
        final Map<String, Object> activityJobAttributes = jobEnvironment.getActivityJobAttributes();
        double activityProgressPercentage = (double) activityJobAttributes.get(ShmConstants.PROGRESSPERCENTAGE);
        if (repeatActivityCount != 0) {
            activityProgressPercentage += (progressPercentage / repeatActivityCount);
        }
        activityProgressPercentage = Math.round(activityProgressPercentage * 100);
        activityProgressPercentage = activityProgressPercentage / 100;
        return activityProgressPercentage;
    }

    /**
     * This method calculates the ActivityJobProgressPercentage For MultipleBackups for every Activity.
     *
     * @param activityJobId
     * @param repeatActivityCount
     * @param activityJobAttributes
     * @param progressPercentage
     * @return
     */
    public double calculateActivityProgressPercentage(final long activityJobId, final int repeatActivityCount, final Map<String, Object> activityJobAttributes, final double progressPercentage) {
        double activityProgressPercentage = (double) activityJobAttributes.get(ShmConstants.PROGRESSPERCENTAGE);
        if (repeatActivityCount != 0) {
            activityProgressPercentage += (progressPercentage / repeatActivityCount);
        }
        activityProgressPercentage = Math.round(activityProgressPercentage * 100);
        activityProgressPercentage = activityProgressPercentage / 100;
        return activityProgressPercentage;
    }

}
