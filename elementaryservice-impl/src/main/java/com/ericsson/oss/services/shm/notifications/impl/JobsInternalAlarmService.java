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
package com.ericsson.oss.services.shm.notifications.impl;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.impl.JobFailureAlarmParameterChangeListener;
import com.ericsson.oss.services.shm.internal.alarm.ShmInternalAlarmGenerator;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * This class is used to check for the configuration parameters depending upon
 * jobtype and retrieve the details to raise an internal alarm
 */
public class JobsInternalAlarmService {

    @Inject
    ShmInternalAlarmGenerator shmInternalAlarmGenerator;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private JobFailureAlarmParameterChangeListener jobFailureAlarmParameterChangeListener;

    private final Logger logger = LoggerFactory.getLogger(JobsInternalAlarmService.class);

    private static final String SHM_ERROR = "SHM_ERROR";

    private static final String SHM_JOB_FAILURE = "SHM_JOB_FAILURE";

    private static final String JOB_FAILURE = "_JOB_FAILURE";

    private static final String MAJOR = "MAJOR";

    private static final String CRITICAL = "CRITICAL";

    private static final String ERROR = "ERROR";

    private static final String PROCESSING_ERROR_ALARM = "Processing Error Alarm";

    private static final String SOFTWARE_PROGRAM_ERROR = "Software Program Error";

    private static final String ERROR_MESSAGE = "ERROR_MESSAGE";

    /**
     * @param mainJobId
     *            ,jobType to verify whether alarm is needed for specific job
     *            type and raise an Internal Alarm
     */
    public void checkIfAlarmHasToBeRaised(final long mainJobId, final String jobType) {
        boolean generateAlarm = false;
        final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(jobType);
        switch (jobTypeEnum) {
        case UPGRADE:
            generateAlarm = jobFailureAlarmParameterChangeListener.isAlarmNeededOnUpgradeJobFailure();
            break;
        case DELETE_UPGRADEPACKAGE:
            generateAlarm = jobFailureAlarmParameterChangeListener.isAlarmNeededOnDeleteUpgradepackageJobFailure();
            break;
        case BACKUP:
            generateAlarm = jobFailureAlarmParameterChangeListener.isAlarmNeededOnBackupJobFailure();
            break;
        case RESTORE:
            generateAlarm = jobFailureAlarmParameterChangeListener.isAlarmNeededOnRestoreJobFailure();
            break;
        case LICENSE:
            generateAlarm = jobFailureAlarmParameterChangeListener.isAlarmNeededOnLicenseJobFailure();
            break;
        case DELETEBACKUP:
            generateAlarm = jobFailureAlarmParameterChangeListener.isAlarmNeededOnDeleteBackupJobFailure();
            break;
        case BACKUP_HOUSEKEEPING:
            generateAlarm = jobFailureAlarmParameterChangeListener.isAlarmNeededOnBackupHousekeepingJobFailure();
            break;
        case NODERESTART:
            generateAlarm = jobFailureAlarmParameterChangeListener.isAlarmNeededOnShmJobFailure();
            break;
        case SYSTEM:
            generateAlarm = jobFailureAlarmParameterChangeListener.isAlarmNeededOnShmJobFailure();
            break;
        case ONBOARD:
            generateAlarm = jobFailureAlarmParameterChangeListener.isAlarmNeededOnOboardJobFailure();
            break;
        case DELETE_SOFTWAREPACKAGE:
            generateAlarm = jobFailureAlarmParameterChangeListener.isAlarmNeededOnDeleteSoftwarePackageJobFailure();
            break;
        case NODE_HEALTH_CHECK:
            break;
        case LICENSE_REFRESH:
            generateAlarm=jobFailureAlarmParameterChangeListener.isAlarmNeededOnLicenseRefreshJobFailure();
            break;
        }

        if (generateAlarm) {
            callShmInternalAlarmService(mainJobId, jobTypeEnum);
        }
    }

    /**
     * @param mainJobId
     *            ,jobType to fetch all the details needed to Raise an Alarm and
     *            call the common class to raise an alarm
     */
    private void callShmInternalAlarmService(final long mainJobId, final JobTypeEnum jobTypeEnum) {
        final Map<String, Object> jobDetails = jobConfigurationService.getJobDetailsToRaiseAlarm(mainJobId);
        if (!jobDetails.isEmpty()) {
            String additionalText = "";
            if(jobTypeEnum == JobTypeEnum.ONBOARD || jobTypeEnum == JobTypeEnum.DELETE_SOFTWAREPACKAGE || jobTypeEnum== JobTypeEnum.LICENSE_REFRESH){
                final HashMap<String,String> jobsType = new HashMap<>();
                jobsType.put("ONBOARD", "Onboard");
                jobsType.put("DELETE_SOFTWAREPACKAGE", "Delete SoftwarePackage");
                jobsType.put("LICENSE_REFRESH", "License Refresh");

                jobDetails.put(ShmConstants.EVENT_TYPE, PROCESSING_ERROR_ALARM);
                jobDetails.put(ShmConstants.PROBABLE_CAUSE, SOFTWARE_PROGRAM_ERROR);
                jobDetails.put(ShmConstants.RECORD_TYPE, ERROR_MESSAGE);
                additionalText = String.format(ShmConstants.INTERNAL_ALARM_ADDITIONAL_TEXT_FOR_FAILED_JOB, jobsType.get((String) jobDetails.get(ShmConstants.JOB_TYPE)), 
                        jobDetails.get(ShmConstants.NAME), jobDetails.get(ShmConstants.OWNER));
                jobDetails.put(ShmConstants.MANAGED_OBJECT_INSTANCE, "SHM " + jobsType.get((String) jobDetails.get(ShmConstants.JOB_TYPE))  +" Job");
                }else{
                jobDetails.put(ShmConstants.EVENT_TYPE, SHM_ERROR);
                jobDetails.put(ShmConstants.PROBABLE_CAUSE, SHM_JOB_FAILURE);
                jobDetails.put(ShmConstants.RECORD_TYPE, ERROR);
                additionalText = String.format(ShmConstants.INTERNAL_ALARM_ADDITIONAL_TEXT, jobDetails.get(ShmConstants.JOB_TYPE), jobDetails.get(ShmConstants.NAME),
                        jobDetails.get(ShmConstants.OWNER), jobDetails.get(ShmConstants.NAME), jobDetails.get(ShmConstants.CREATION_TIME).toString(), jobDetails.get(ShmConstants.TOTAL_NES),
                        jobDetails.get(ShmConstants.FAILED_NES), jobDetails.get(ShmConstants.SKIPPED_NES));
                jobDetails.put(ShmConstants.MANAGED_OBJECT_INSTANCE, "ShmJobName : " + (String) jobDetails.get(ShmConstants.NAME));

                }

            jobDetails.put(ShmConstants.ADDITIONAL_TEXT, additionalText);
            jobDetails.put(ShmConstants.SPECIFIC_PROBLEM, jobTypeEnum.getAttribute() + JOB_FAILURE);

            if (jobTypeEnum == JobTypeEnum.BACKUP || jobTypeEnum == JobTypeEnum.DELETEBACKUP || jobTypeEnum == JobTypeEnum.BACKUP_HOUSEKEEPING || jobTypeEnum == JobTypeEnum.DELETE_UPGRADEPACKAGE || jobTypeEnum == JobTypeEnum.LICENSE_REFRESH ) {
                jobDetails.put(ShmConstants.PERCEIVED_SEVERITY, MAJOR);
            } else {
                jobDetails.put(ShmConstants.PERCEIVED_SEVERITY, CRITICAL);
            }

            try {
                shmInternalAlarmGenerator.raiseInternalAlarm(jobDetails);
            } catch (final Exception ex) {
                logger.error("Exception from internal Alarm service {}", ex);
            }
        } else {
            logger.error("Unable to Raise an internal alarm as the job details cannot be fetched for jodId : {}  and jobType : {}", mainJobId, jobTypeEnum.getAttribute());
        }
    }

}
