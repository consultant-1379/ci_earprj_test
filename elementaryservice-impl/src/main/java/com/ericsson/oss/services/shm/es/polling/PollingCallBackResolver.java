/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.polling;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.PollingCallBack;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * This class is used to fetch the corresponding elementary service to which polling response is to be sent for processing.
 * 
 * @author xsrabop
 * 
 */
@ApplicationScoped
public class PollingCallBackResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollingCallBackResolver.class);

    private static final String ECIM_UPGRADE_ACTIVATE = "ECIM.UPGRADE.activate";
    private static final String ECIM_BACKUP_CREATE = "ECIM.BACKUP.createbackup";
    private static final String ECIM_BACKUP_UPLOAD = "ECIM.BACKUP.uploadbackup";
    private static final String CPP_UPGRADE_UPGRADE = "CPP.UPGRADE.upgrade";
    private static final String CPP_BACKUP_UPLOAD = "CPP.BACKUP.exportcv";
    private static final String AXE_BACKUP_CREATE = "AXE.BACKUP.createbackup";
    private static final String AXE_BACKUP_UPLOAD = "AXE.BACKUP.uploadbackup";

    @Inject
    @ActivityInfo(activityName = "activate", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.ECIM)
    private PollingCallBack activateService;

    @Inject
    @ActivityInfo(activityName = ShmConstants.CREATE_BACKUP, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
    private PollingCallBack createBackupService;

    @Inject
    @ActivityInfo(activityName = ShmConstants.UPLOAD_BACKUP, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
    private PollingCallBack uploadBackupService;

    @Inject
    @ActivityInfo(activityName = ShmConstants.UPGRADE_ACTIVITY, jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.CPP)
    private PollingCallBack upgradeService;

    @Inject
    @ActivityInfo(activityName = ShmConstants.UPLOAD_CV__ACTIVITY, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.CPP)
    private PollingCallBack uploadCvService;

    @Inject
    @ActivityInfo(activityName = ShmConstants.CREATE_BACKUP, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.AXE)
    private PollingCallBack createService;

    @Inject
    @ActivityInfo(activityName = ShmConstants.UPLOAD_BACKUP, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.AXE)
    private PollingCallBack uploadService;

    public PollingCallBack getPollingCallBackService(final PlatformTypeEnum platform, final JobTypeEnum jobType, final String activityName) {
        PollingCallBack pollingCallBackResolver = null;
        final String qualifier = platform + JobVariables.VAR_NAME_DELIMITER + jobType + JobVariables.VAR_NAME_DELIMITER + activityName;
        switch (qualifier) {
        case ECIM_UPGRADE_ACTIVATE:
            pollingCallBackResolver = activateService;
            break;
        case ECIM_BACKUP_CREATE:
            pollingCallBackResolver = createBackupService;
            break;
        case ECIM_BACKUP_UPLOAD:
            pollingCallBackResolver = uploadBackupService;
            break;
        case CPP_UPGRADE_UPGRADE:
            pollingCallBackResolver = upgradeService;
            break;
        case CPP_BACKUP_UPLOAD:
            pollingCallBackResolver = uploadCvService;
            break;
        case AXE_BACKUP_CREATE:
            pollingCallBackResolver = createService;
            break;
        case AXE_BACKUP_UPLOAD:
            pollingCallBackResolver = uploadService;
            break;
        default:
            LOGGER.warn("Unable to resolve polling call back service with qualifier : {}", qualifier);
            break;
        }
        return pollingCallBackResolver;
    }

}
