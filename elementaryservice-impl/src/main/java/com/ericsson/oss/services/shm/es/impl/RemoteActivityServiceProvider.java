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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.RemoteActivityCallBack;
import com.ericsson.oss.services.shm.es.api.RemoteActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * This class selects remoteCallBack Implementation based on platform,jobType and activityName to process notification which are received on Topic. 
 * There is a memory leakage issue in using InstanceProvider. So, we changed the implementation to Factory Design as a quick fix. 
 * However, using Factory Design leads to poor code maintainability, so we need to analyze and find out a better solution which improves code maintainability and doesn't leak any memory. 
 * JIRA : https://jira-nam.lmera.ericsson.se/browse/TORF-180324
 * @author tcssagu
 * 
 */
public class RemoteActivityServiceProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    //CPP Backup Job - UploadCvService
    @Inject
    @RemoteActivityInfo(activityName = "putToFtpServer", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.CPP)
    RemoteActivityCallBack cppUploadCvService;

    //ECIM Backup Job - CreateBackupService
    @Inject
    @RemoteActivityInfo(activityName = "Createbackup", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
    RemoteActivityCallBack ecimCreateBackupService;

    //ECIM Backup Job - UploadBackupService
    @Inject
    @RemoteActivityInfo(activityName = "Uploadbackup", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
    RemoteActivityCallBack ecimUploadBackupService;

    //ECIM DeleteBackup Job - DeleteBackupService
    @Inject
    @RemoteActivityInfo(activityName = "deletebackup", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
    RemoteActivityCallBack ecimDeleteBackupService;

    //ECIM Restore Job - RestoreService
    @Inject
    @RemoteActivityInfo(activityName = "RestoreBackup", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.ECIM)
    RemoteActivityCallBack ecimRestoreBackupService;

    public RemoteActivityCallBack getActivityNotificationHandler(final PlatformTypeEnum platform, final JobTypeEnum jobType, final String activityName) {
        RemoteActivityCallBack remoteActivityCallBackInstance = null;
        final String qualifier = platform + JobVariables.VAR_NAME_DELIMITER + jobType + JobVariables.VAR_NAME_DELIMITER + activityName;
        switch (qualifier) {

        //CPP Backup Job
        case "CPP.BACKUP.putToFtpServer":
            remoteActivityCallBackInstance = cppUploadCvService;
            break;

        //ECIM Backup Job
        case "ECIM.BACKUP.Createbackup":
            remoteActivityCallBackInstance = ecimCreateBackupService;
            break;
        case "ECIM.BACKUP.Uploadbackup":
            remoteActivityCallBackInstance = ecimUploadBackupService;
            break;

        //ECIM DeleteBackup Job
        case "ECIM.DELETEBACKUP.deletebackup":
            remoteActivityCallBackInstance = ecimDeleteBackupService;
            break;

        //ECIM Restore Job
        case "ECIM.RESTORE.RestoreBackup":
            remoteActivityCallBackInstance = ecimRestoreBackupService;
            break;

        default:
            logger.warn("Unspecified Qualifier : {}", qualifier);
        }
        return remoteActivityCallBackInstance;
    }

}
