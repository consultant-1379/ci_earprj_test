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
package com.ericsson.oss.services.shm.job.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeQualifier;
import com.ericsson.oss.services.shm.jobs.common.api.SHMJobActivitiesResponseModifier;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;

@ApplicationScoped
public class JobActivityResponseModificationProviderFactory {

    private final Logger logger = LoggerFactory.getLogger(JobActivityResponseModificationProviderFactory.class);

    @Inject
    @PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.CPP, jobType = com.ericsson.oss.services.shm.job.activity.JobType.UPGRADE)
    private SHMJobActivitiesResponseModifier cppUpgradeJobActivitiesResponseModifier;
    
    @Inject
    @PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.AXE, jobType = com.ericsson.oss.services.shm.job.activity.JobType.UPGRADE)
    private SHMJobActivitiesResponseModifier axeUpgradeJobActivitiesResponseModifier;

    @Inject
    @PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.ECIM, jobType = com.ericsson.oss.services.shm.job.activity.JobType.BACKUP)
    private SHMJobActivitiesResponseModifier ecimBackupJobActivitiesResponseModifier;

    @Inject
    @Any
    private Instance<ManageBackupActivitiesResponseModifier> backupResponseModifiers;

    public SHMJobActivitiesResponseModifier getActivitiesResponseModifier(final PlatformTypeEnum platformType, final JobType jobType) {
        SHMJobActivitiesResponseModifier shmJobActivitiesResponseModifier = null;
        final String qualifier = platformType + JobVariables.VAR_NAME_DELIMITER + jobType;
        switch (qualifier) {

        // CPP UPGRADE JOB
        case "CPP.UPGRADE":
            shmJobActivitiesResponseModifier = cppUpgradeJobActivitiesResponseModifier;
            break;
        // AXE UPGRADE JOB
        case "AXE.UPGRADE":
            shmJobActivitiesResponseModifier = axeUpgradeJobActivitiesResponseModifier;
            break;
        // ECIM BACKUP JOB
        case "ECIM.BACKUP":
            shmJobActivitiesResponseModifier = ecimBackupJobActivitiesResponseModifier;
            break;
        default:
            logger.warn("Unspecified Qualifier : {}", qualifier);
        }
        return shmJobActivitiesResponseModifier;
    }

    public ManageBackupActivitiesResponseModifier getBackupActivitiesResponseModifier(final PlatformTypeEnum platformType, final JobType jobType) {
        final Instance<ManageBackupActivitiesResponseModifier> backupResponseModifier = backupResponseModifiers.select(new PlatformJobTypeQualifier(platformType, jobType));
        if (backupResponseModifier != null && !backupResponseModifier.isUnsatisfied()) {
            return backupResponseModifier.get();
        } else {
            return null;
        }
    }
}
