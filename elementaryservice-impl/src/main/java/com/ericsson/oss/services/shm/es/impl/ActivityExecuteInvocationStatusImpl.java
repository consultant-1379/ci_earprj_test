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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.ActivityExecuteInvocationStatus;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ActivityExecuteInvocationStatusImpl implements ActivityExecuteInvocationStatus {

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private ActivityUtils activityUtils;

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private static final String ACTIVATE_ACTIVITY_NAME = "activate";
    private static final String PREPARE_ACTIVITY_NAME = "prepare";

    /**
     * Checks whether the moaction is already triggered on node or not.
     * 
     * @return true if moaction is already triggered once , false otherwise
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean isActivityExecuted(final long activityJobId) {

        LOGGER.debug("Checking activityJob({}) execution status...", activityJobId);
        final Map<String, Object> jobPoAttributes = jobUpdateService.retrieveJobWithRetry(activityJobId);
        final String activityName = (String) jobPoAttributes.get(ActivityConstants.ACTIVITY_NAME);
        final List<Map<String, Object>> activityJobPropertyList = (List<Map<String, Object>>) jobPoAttributes.get(ActivityConstants.JOB_PROPERTIES);
        boolean isExecuted = false;

        LOGGER.debug("activityjob id : {}, activityJobPropertyList in ActivityExecuteInvocationStatusImpl : {}", activityJobId, activityJobPropertyList);
        if (activityJobPropertyList == null || activityJobPropertyList.isEmpty()) {
            return isExecuted;
        }

        for (final Map<String, Object> jobproperty : activityJobPropertyList) {
            if (jobproperty.get(ShmConstants.KEY).equals(ShmConstants.IS_EXECUTED)) {
                isExecuted = Boolean.parseBoolean((String) jobproperty.get(ShmConstants.VALUE));
                break;
            }
        }

        if (isExecuted) {
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            activityUtils.addJobLog(String.format(JobLogConstants.EXECUTION_TRIGGERED_EVALUATE_RESULT, activityName), JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.INFO.toString());

            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
        }
        return isExecuted;
    }

    /**
     * Update the job property "isExecuted" on successful exection of moaction
     * 
     * @return true if successfully updated the parameter "isExecuted", false otherwise
     */
    @Override
    public void updateActivityExecuteStatus(final long activityJobId) {

        LOGGER.debug("Skip IS_EXECUTED update for deletebackup and restore install activities."); //Cluster reboot functionality yet to be fixed.
        final Map<String, Object> jobPoAttributes = jobUpdateService.retrieveJobWithRetry(activityJobId);
        final String activityName = (String) jobPoAttributes.get(ActivityConstants.ACTIVITY_NAME);
        LOGGER.debug("Activity Job Id - {}. Activity Name is : {}", activityJobId, activityName);

        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
        final long templateJobId = (long) mainJobAttributes.get("templateJobId");
        LOGGER.debug("templateJobId : {} & mainJobAttributes : {}", templateJobId, mainJobAttributes);

        final Map<String, Object> templateJobAttributes = activityUtils.getPoAttributes(templateJobId);
        final String jobTypeString = (String) templateJobAttributes.get(ShmConstants.JOB_TYPE);
        LOGGER.debug("jobTypeString : {}", jobTypeString);
        final JobTypeEnum jobType = JobTypeEnum.getJobType(jobTypeString);
        LOGGER.debug("templateJobAttributes : {} and job type from job template : {}", templateJobAttributes, jobType);

        final boolean skipIsExecutedProperty = JobTypeEnum.DELETEBACKUP.equals(jobType) //activity names are hard-coded since this is a temporary fix only.
                || (JobTypeEnum.BACKUP_HOUSEKEEPING.equals(jobType) && "deletebackup".equals(activityName)) || (JobTypeEnum.RESTORE.equals(jobType) && ("install".equals(activityName)))
                || (JobTypeEnum.BACKUP.equals(jobType) && "exportcv".equals(activityName)) || (JobTypeEnum.UPGRADE.equals(jobType) && PREPARE_ACTIVITY_NAME.equals(activityName))
                || (JobTypeEnum.UPGRADE.equals(jobType) && ACTIVATE_ACTIVITY_NAME.equals(activityName) || (JobTypeEnum.LICENSE.equals(jobType) && ("install".equals(activityName)))
                        || (JobTypeEnum.BACKUP.equals(jobType) && "uploadbackup".equals(activityName)) || (JobTypeEnum.ONBOARD.equals(jobType) && "onboard".equals(activityName))
                        || (JobTypeEnum.DELETE_SOFTWAREPACKAGE.equals(jobType) && "delete_softwarepackage".equals(activityName))
                        || (JobTypeEnum.DELETE_UPGRADEPACKAGE.equals(jobType) && ShmConstants.DELETEUPGRADEPKG_ACTIVITY.equals(activityName))
                        || (JobTypeEnum.NODE_HEALTH_CHECK.equals(jobType) && "enmhealthcheck".equals(activityName))
                        || (JobTypeEnum.NODE_HEALTH_CHECK.equals(jobType) && "nodehealthcheck".equals(activityName))
                        || (JobTypeEnum.LICENSE_REFRESH.equals(jobType) && "install".equals(activityName)));
        LOGGER.debug("skipIsExecutedProperty : {}", skipIsExecutedProperty);

        if (!skipIsExecutedProperty) {
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            activityUtils.addJobProperty(ShmConstants.IS_EXECUTED, Boolean.TRUE.toString(), jobPropertyList);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null);
        }
        LOGGER.debug("Job property \"{}\" is updated for ActivityJob with PO Id {}", ShmConstants.IS_EXECUTED, activityJobId);
    }
}
