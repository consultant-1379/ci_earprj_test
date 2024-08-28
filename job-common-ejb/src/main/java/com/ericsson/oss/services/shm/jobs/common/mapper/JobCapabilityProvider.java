/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobs.common.mapper;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * CapabilityProvider returns the capability corresponds to the job type.
 * 
 * @author xeswpot
 */
public class JobCapabilityProvider {

    /**
     * Returns the capability based on job type. Null will returned if corresponding capability is not exists for job type.
     * 
     * @param jobTypeEnum
     *            {@link JobTypeEnum}
     * @return capability
     */
    public String getCapability(final JobTypeEnum jobTypeEnum) {

        String capability = null;

        if (jobTypeEnum != null) {
            switch (jobTypeEnum) {

            case UPGRADE:
                capability = SHMCapabilities.UPGRADE_JOB_CAPABILITY;
                break;
            case BACKUP:
                capability = SHMCapabilities.BACKUP_JOB_CAPABILITY;
                break;
            case RESTORE:
                capability = SHMCapabilities.RESTORE_JOB_CAPABILITY;
                break;
            case DELETEBACKUP:
                capability = SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY;
                break;
            case BACKUP_HOUSEKEEPING:
                capability = SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY;
                break;
            case LICENSE:
                capability = SHMCapabilities.LICENSE_JOB_CAPABILITY;
                break;
            case NODERESTART:
                capability = SHMCapabilities.NODE_RESTART_JOB_CAPABILITY;
                break;
            case ONBOARD:
                capability = SHMCapabilities.ONBOARD_JOB_CAPABILITY;
                break;
            case DELETE_SOFTWAREPACKAGE:
                capability = SHMCapabilities.DELETE_SOFTWARE_PACKAGE_JOB_CAPABILITY;
                break;
            case DELETE_UPGRADEPACKAGE:
                capability = SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY;
                break;
            case LICENSE_REFRESH:
                capability = SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY;
                break;
            default:
                break;
            }
        }
        return capability;
    }
}
