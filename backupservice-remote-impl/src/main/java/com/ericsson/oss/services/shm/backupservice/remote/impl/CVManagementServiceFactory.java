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

import javax.ejb.*;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.backupservice.cpp.remote.*;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.RemoteActivityInfo;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonRemoteCvManagementService;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CVManagementServiceFactory {

    @Inject
    private CreateCVRemoteServiceImpl cppCreateCVServiceImpl;

    @Inject
    @RemoteActivityInfo(activityName = BackupActivityConstants.ACTION_UPLOAD_CV, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.CPP)
    private CommonRemoteCvManagementService cppUploadCVServiceImpl;

    @Inject
    private SetStartableCVRemoteServiceImpl cppSetStartableCVServiceImpl;

    @Inject
    private SetFirstRollBackListRemoteCVServiceImpl cppSetFirsRollBackListCVServiceImpl;

    /**
     * @param string
     * @return
     */
    public CommonRemoteCvManagementService getCvManagementService(final String nodeName, final String cvOperationType) {

        switch (cvOperationType) {
            case BackupActivityConstants.ACTION_CREATE_CV:
                return cppCreateCVServiceImpl;

            case BackupActivityConstants.ACTION_UPLOAD_CV:
                return cppUploadCVServiceImpl;

            case BackupActivityConstants.ACTION_SET_STARTABLE_CV:
                return cppSetStartableCVServiceImpl;

            case BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV:
                return cppSetFirsRollBackListCVServiceImpl;

            default:
                return null;
        }
    }

}
