package com.ericsson.oss.services.shm.es.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * There is a memory leakage issue in using InstanceProvider. So, we changed the implementation to Factory Design as a quick fix. However, using Factory Design leads to poor code maintainability, so
 * we need to analyze and find out a better solution which improves code maintainability and doesn't leak any memory. JIRA : https://jira-nam.lmera.ericsson.se/browse/TORF-180324
 */
@ApplicationScoped
public class ActivityCompleteCallBackProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    //CPP Backup Job
    @Inject
    @ActivityInfo(activityName = "exportcv", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.CPP)
    ActivityCompleteCallBack cppUploadCvService;

    //CPP Restore Job
    @Inject
    @ActivityInfo(activityName = "download", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
    ActivityCompleteCallBack cppDownloadCvService;

    @Inject
    @ActivityInfo(activityName = "verify", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
    ActivityCompleteCallBack cppVerifyRestoreService;

    @Inject
    @ActivityInfo(activityName = "restore", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
    ActivityCompleteCallBack cppRestoreService;

    @Inject
    @ActivityInfo(activityName = "confirm", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
    ActivityCompleteCallBack cppConfirmRestoreService;

    //CPP Upgrade Job
    @Inject
    @ActivityInfo(activityName = "verify", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.CPP)
    ActivityCompleteCallBack cppVerifyService;

    @Inject
    @ActivityInfo(activityName = "upgrade", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.CPP)
    ActivityCompleteCallBack cppUpgradeService;

    @Inject
    @ActivityInfo(activityName = "manualrestart", jobType = JobTypeEnum.NODERESTART, platform = PlatformTypeEnum.CPP)
    ActivityCompleteCallBack cppNodeRestartService;

    public ActivityCompleteCallBack onActionCompleteHandler(final PlatformTypeEnum platform, final JobTypeEnum jobType, final String activityName) {
        ActivityCompleteCallBack activityCompleteCallBackInstance = null;
        final String qualifier = platform + JobVariables.VAR_NAME_DELIMITER + jobType + JobVariables.VAR_NAME_DELIMITER + activityName;
        switch (qualifier) {

        //CPP Backup Job
        case "CPP.BACKUP.exportcv":
            activityCompleteCallBackInstance = cppUploadCvService;
            break;

        //CPP Restore Job
        case "CPP.RESTORE.download":
            activityCompleteCallBackInstance = cppDownloadCvService;
            break;
        case "CPP.RESTORE.verify":
            activityCompleteCallBackInstance = cppVerifyRestoreService;
            break;
        case "CPP.RESTORE.restore":
            activityCompleteCallBackInstance = cppRestoreService;
            break;
        case "CPP.RESTORE.confirm":
            activityCompleteCallBackInstance = cppConfirmRestoreService;
            break;

        //CPP Upgrade Job
        case "CPP.UPGRADE.verify":
            activityCompleteCallBackInstance = cppVerifyService;
            break;
        case "CPP.UPGRADE.upgrade":
            activityCompleteCallBackInstance = cppUpgradeService;
            break;

        //CPP Node Restart Job
        case "CPP.NODERESTART.manualrestart":
            activityCompleteCallBackInstance = cppNodeRestartService;
            break;

        default:
            logger.warn("Unspecified Qualifier : {}", qualifier);
        }
        return activityCompleteCallBackInstance;

    }

}
