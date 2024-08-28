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
package com.ericsson.oss.services.shm.jobs.common.enums;

import java.io.Serializable;

public enum JobTypeEnum implements Serializable{

    UPGRADE("UPGRADE", 1),

    BACKUP("BACKUP", 2),

    RESTORE("RESTORE", 3),

    LICENSE("LICENSE", 4),

    SYSTEM("SYSTEM", 5),

    DELETEBACKUP("DELETEBACKUP", 6),

    BACKUP_HOUSEKEEPING("BACKUP_HOUSEKEEPING", 7),

    NODERESTART("NODERESTART", 8),

    ONBOARD("ONBOARD", 9),

    DELETE_SOFTWAREPACKAGE("DELETE_SOFTWAREPACKAGE", 10),

    DELETE_UPGRADEPACKAGE("DELETE_UPGRADEPACKAGE", 11),

    NODE_HEALTH_CHECK("NODE_HEALTH_CHECK", 12),

    LICENSE_REFRESH("LICENSE_REFRESH",13);

    private final String attribute;
    private final int value;

    private JobTypeEnum(final String attribute, final int value) {
        this.attribute = attribute;
        this.value = value;
    }

    public String getAttribute() {
        return attribute;
    }

    public int isDefault() {
        return value;
    }

    public static JobTypeEnum getJobType(final String jobType) {

        for (final JobTypeEnum s : JobTypeEnum.values()) {
            if (s.name().equalsIgnoreCase(jobType)) {
                return s;
            }
        }
        return null;
    }

}
