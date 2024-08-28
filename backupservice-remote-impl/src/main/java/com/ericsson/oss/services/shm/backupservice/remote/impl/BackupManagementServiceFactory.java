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
package com.ericsson.oss.services.shm.backupservice.remote.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.backupservice.ecim.remote.CommonRemoteBackUpManagementService;
import com.ericsson.oss.services.shm.backupservice.remote.api.BackupManagementServiceException;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.RemoteActivityInfo;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@ApplicationScoped
public class BackupManagementServiceFactory {

    @Inject
    @RemoteActivityInfo(activityName = EcimBackupConstants.CREATE_BACKUP, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
    private CommonRemoteBackUpManagementService createBackUpRemoteServiceImpl;

    @Inject
    @RemoteActivityInfo(activityName = EcimBackupConstants.UPLOAD_BACKUP, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
    private CommonRemoteBackUpManagementService uploadBackUpRemoteServiceImpl;

    @Inject
    @RemoteActivityInfo(activityName = EcimBackupConstants.DELETE_BACKUP, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
    private CommonRemoteBackUpManagementService deleteBackUpRemoteServiceImpl;

    @Inject
    @RemoteActivityInfo(activityName = EcimBackupConstants.RESTORE_BACKUP, jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.ECIM)
    private CommonRemoteBackUpManagementService restoreBackupRemoteServiceImpl;

    public CommonRemoteBackUpManagementService getBackUpManagementService(final String backUpOperationType) throws BackupManagementServiceException {

        switch (backUpOperationType) {

        case EcimBackupConstants.CREATE_BACKUP:
            return createBackUpRemoteServiceImpl;

        case EcimBackupConstants.UPLOAD_BACKUP:
            return uploadBackUpRemoteServiceImpl;

        case EcimBackupConstants.DELETE_BACKUP:
            return deleteBackUpRemoteServiceImpl;

        case EcimBackupConstants.RESTORE_BACKUP:
            return restoreBackupRemoteServiceImpl;

        default:
            throw new BackupManagementServiceException("Invalid Operation Type");
        }
    }
}
