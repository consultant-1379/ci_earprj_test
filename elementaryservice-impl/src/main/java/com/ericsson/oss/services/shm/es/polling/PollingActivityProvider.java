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
package com.ericsson.oss.services.shm.es.polling;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivity;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@ApplicationScoped
public class PollingActivityProvider {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Inject
    @ActivityInfo(activityName = "restore", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
    PollingActivity cppRestoreService;

    public PollingActivity getPollingActivity(final String platform, final String jobType, final String activityName) {
        PollingActivity pollingActivity = null;

        final String qualifier = PlatformTypeEnum.getPlatform(platform) + JobVariables.VAR_NAME_DELIMITER + JobTypeEnum.getJobType(jobType) + JobVariables.VAR_NAME_DELIMITER + activityName;

        switch (qualifier) {

        case "CPP.RESTORE.restore":
            pollingActivity = cppRestoreService;
            break;

        default:
            LOGGER.warn("Unspecified Qualifier : {}", qualifier);
        }

        return pollingActivity;
    }

}
