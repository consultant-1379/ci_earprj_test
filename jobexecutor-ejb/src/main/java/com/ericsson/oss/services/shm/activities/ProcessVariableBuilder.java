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
package com.ericsson.oss.services.shm.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ActivityTimeoutConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;

/**
 * To build and give the required process variables
 * 
 * @author xrajeke
 * 
 */
@ApplicationScoped
public class ProcessVariableBuilder {
    private final static Logger LOGGER = LoggerFactory.getLogger(ProcessVariableBuilder.class);

    private static final String PREFIX_POST = "post";
    private static final String PREFIX_PRE = "pre";

    @Inject
    private JobActivitiesProvider jobActivitiesProvider;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private PollingActivityConfiguration pollingActivityConfiguration;

    /**
     * To build and give the required process variables
     * 
     * @param networkElement
     * @param activityJobsList
     * @param neJobId
     * @return
     */
    public Map<String, Object> build(final JobTypeEnum jobTypeEnum, final NetworkElement networkElement, final List<Map<String, Object>> activityJobsList, final long neJobId,
            final Map<String, Map<Object, Object>> neTypeSynchronousActivityMap) {
        final PlatformTypeEnum platformTypeEnum = networkElement.getPlatformType();
        final Map<String, Object> procesVarTemp = buildDefaultProcessVariables(jobTypeEnum.name(), platformTypeEnum.name(), networkElement.getNeType());
        final String precheckTimeout = activityTimeoutsService.getPrecheckTimeout();
        final String timeoutForHandleTimeout = activityTimeoutsService.getTimeoutForHandleTimeout();
        final boolean isPollingEnabled = pollingActivityConfiguration.isPollingEnabled(platformTypeEnum, jobTypeEnum);
        final List<Integer> activityOrderList = new ArrayList<>();
        boolean isNextActivitySynchronous = false;
        Map<Object, Object> activityOrderAndSyncStatus = null;
        if (neTypeSynchronousActivityMap != null && !neTypeSynchronousActivityMap.isEmpty() && neTypeSynchronousActivityMap.containsKey(networkElement.getNeType())) {
            activityOrderAndSyncStatus = neTypeSynchronousActivityMap.get(networkElement.getNeType());
        }
        for (final Map<String, Object> activityJob : activityJobsList) {

            if (activityOrderAndSyncStatus != null && !activityOrderAndSyncStatus.isEmpty() && activityOrderAndSyncStatus.containsKey((int) activityJob.get(ShmConstants.ORDER))) {
                isNextActivitySynchronous = (boolean) activityOrderAndSyncStatus.get((int) activityJob.get(ShmConstants.ORDER));
            } else {
                isNextActivitySynchronous = false;
            }
            final String activityName = activityJob.get(ShmConstants.NAME).toString().toLowerCase();
            final String activityTimeout = activityTimeoutsService.getActivityTimeout(networkElement.getNeType(), platformTypeEnum, jobTypeEnum, activityName);
            final String repeatPrecheckWaitTime = activityTimeoutsService.getRepeatPrecheckWaitInterval(networkElement.getNeType(), platformTypeEnum, jobTypeEnum, activityName);
            if (isPollingEnabled) {
                final String bestTimeout = activityTimeoutsService.getBestTimeout(networkElement.getNeType(), platformTypeEnum, jobTypeEnum, activityName);
                procesVarTemp.put(activityName + JobVariables.VAR_NAME_DELIMITER + JobVariables.BEST_TIMEOUT, bestTimeout);
            }
            if (JobTypeEnum.NODE_HEALTH_CHECK.name().equals(jobTypeEnum.name())) {
                final String nodeSyncCheckWaitIntervalInMin = activityTimeoutsService.getNodeSyncCheckWaitIntervalOrTimeOut(platformTypeEnum,jobTypeEnum,activityName,ActivityTimeoutConstants.NODE_SYNC_WAIT_TIME);
                final String nodeSyncCheckTimeout = activityTimeoutsService.getNodeSyncCheckWaitIntervalOrTimeOut(platformTypeEnum,jobTypeEnum,activityName,ActivityTimeoutConstants.NODE_SYNC_TIMEOUT);
                procesVarTemp.put(activityName + JobVariables.VAR_NAME_DELIMITER + JobVariables.NODE_SYNC_CHECK_WAIT_INTERVAL, nodeSyncCheckWaitIntervalInMin);
                procesVarTemp.put(activityName + JobVariables.VAR_NAME_DELIMITER + JobVariables.NODE_SYNC_CHECK_TIMEOUT, nodeSyncCheckTimeout);
            }
            procesVarTemp.put(activityName + JobVariables.VAR_NAME_DELIMITER + JobVariables.ACTIVITY_REQUIRED, true);
            procesVarTemp.put(activityName + JobVariables.VAR_NAME_DELIMITER + JobVariables.ACTIVITY_STARTUP, activityJob.get(ShmConstants.EXECUTION_MODE));
            procesVarTemp.put(activityName + JobVariables.VAR_NAME_DELIMITER + JobVariables.TIMEOUT, activityTimeout);
            procesVarTemp.put(activityName + JobVariables.VAR_NAME_DELIMITER + JobVariables.ACTIVITY_JOBID, activityJob.get(JobVariables.POID));
            procesVarTemp.put(activityName + JobVariables.VAR_NAME_DELIMITER + JobVariables.ACTIVITY_RESTART, false);
            procesVarTemp.put(activityName + JobVariables.VAR_NAME_DELIMITER + JobVariables.REPEAT_PRECHECK_WAIT_TIME, repeatPrecheckWaitTime);

            procesVarTemp.put((int) activityJob.get(ShmConstants.ORDER) + JobVariables.VAR_NAME_DELIMITER + ShmConstants.ACTIVITYNAME, activityName);
            procesVarTemp.put(activityName + JobVariables.VAR_NAME_DELIMITER + JobVariables.IS_NEXT_ACTIVITY_SYNCHRONOUS, isNextActivitySynchronous);

            if (JobVariables.ACTIVITY_STARTUP_SCHEDULED.equals(activityJob.get(ShmConstants.EXECUTION_MODE))) {
                procesVarTemp.put(activityName + JobVariables.VAR_NAME_DELIMITER + JobVariables.SCHEDULE_TIME, activityJob.get(JobVariables.SCHEDULE_TIME).toString());
            }
            activityOrderList.add((int) activityJob.get(ShmConstants.ORDER));

        }
        procesVarTemp.put(JobVariables.ACTIVITIES_COUNT, activityJobsList.size());
        procesVarTemp.put(JobVariables.IS_POLLING_ENABLED, isPollingEnabled);
        procesVarTemp.put(JobVariables.PRECHECK_TIMEOUT, precheckTimeout);
        procesVarTemp.put(JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT, timeoutForHandleTimeout);
        procesVarTemp.put(JobVariables.JOB_TYPE, jobTypeEnum.name());
        procesVarTemp.put(JobVariables.NE_JOB_ID, neJobId);
        procesVarTemp.put(JobVariables.PLATFORM_TYPE, platformTypeEnum.name());
        procesVarTemp.put(JobVariables.NE_TYPE, networkElement.getNeType());
        procesVarTemp.put(JobVariables.ACTIVITY_ORDER_LIST, activityOrderList);

        return procesVarTemp;
    }

    private Map<String, Object> buildDefaultProcessVariables(final String jobType, final String platform, final String neType) {
        final Map<String, Object> procesVarTemp = new HashMap<>();
        final List<ActivityInfo> activityInfo = new ArrayList<>();

        activityInfo.add(0, new ActivityInfo(PREFIX_PRE + jobType.toLowerCase(), null, -1));

        activityInfo.addAll(jobActivitiesProvider.getActivityInfo(platform, neType, jobType));

        activityInfo.add(activityInfo.size(), new ActivityInfo(PREFIX_POST + jobType.toLowerCase(), null, -1));

        LOGGER.info("{} activity elements are available to build the process variables", activityInfo.size());
        procesVarTemp.put(JobVariables.NE_JOB_ID, -1L);
        procesVarTemp.put(JobVariables.JOB_ID, -1L);

        for (final ActivityInfo activity : activityInfo) {
            procesVarTemp.put(activity.getActivityName() + JobVariables.VAR_NAME_DELIMITER + JobVariables.ACTIVITY_REQUIRED, false);
            procesVarTemp.put(activity.getActivityName() + JobVariables.VAR_NAME_DELIMITER + JobVariables.ACTIVITY_STARTUP, JobVariables.ACTIVITY_STARTUP_IMMEDIATE);
            procesVarTemp.put(activity.getActivityName() + JobVariables.VAR_NAME_DELIMITER + JobVariables.ACTIVITY_JOBID, -1L);
            procesVarTemp.put(activity.getActivityName() + JobVariables.VAR_NAME_DELIMITER + JobVariables.ACTIVITY_RESTART, false);

        }

        return procesVarTemp;
    }

}
