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
package com.ericsson.oss.services.shm.es.activity.callback;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.MOActionCallBack;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * This class resolves the input response send to the observer.
 * 
 * @author zdonkri
 */

public class MOActionCallBackReslover {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOActionCallBackReslover.class);

    @Inject
    @ActivityInfo(activityName = ShmConstants.UPLOAD_BACKUP, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
    private MOActionCallBack uploadBackupService;

    public MOActionCallBack getMOActionCallBackService(final PlatformTypeEnum platform, final JobTypeEnum jobType, final String activityName) {

        final String backupUploadActivity = PlatformTypeEnum.ECIM + JobVariables.VAR_NAME_DELIMITER + JobTypeEnum.BACKUP + JobVariables.VAR_NAME_DELIMITER + ShmConstants.UPLOAD_BACKUP;

        MOActionCallBack moActionCallBackResolver = null;
        final String qualifier = platform + JobVariables.VAR_NAME_DELIMITER + jobType + JobVariables.VAR_NAME_DELIMITER + activityName;

        if (backupUploadActivity.equals(qualifier)) {
            moActionCallBackResolver = uploadBackupService;
        } else {
            LOGGER.warn("Unable to resolve MO Action call back service with qualifier : {}", qualifier);
        }
        return moActionCallBackResolver;
    }
}
