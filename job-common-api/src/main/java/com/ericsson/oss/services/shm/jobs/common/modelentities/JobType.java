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
package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.HashMap;
import java.util.Map;

public enum JobType {

    UPGRADE("UPGRADE", "Upgrade"),

    BACKUP("BACKUP", "Backup"),

    RESTORE("RESTORE", "RestoreBackup"),

    LICENSE("LICENSE", "InstallLicense"),

    SYSTEM("SYSTEM", "System"),

    DELETEBACKUP("DELETEBACKUP", "DeleteBackup"),

    BACKUP_HOUSEKEEPING("BACKUP_HOUSEKEEPING", "BackupHousekeeping"),

    NODERESTART("NODERESTART", "NodeRestart"),

    ONBOARD("ONBOARD", "Onboard"),

    DELETE_SOFTWAREPACKAGE("DELETE_SOFTWAREPACKAGE", "DeleteSoftwarePackage"),

    DELETE_UPGRADEPACKAGE("DELETE_UPGRADEPACKAGE", "DeleteUpgradePackage"),

    NODE_HEALTH_CHECK("NODE_HEALTH_CHECK", "NodeHealthCheck"),

    LICENSE_REFRESH("LICENSE_REFRESH","LicenseRefresh");

    private String jobTypeName;
    private String cliJobType;

    private static final Map<String, JobType> jobTypeMap;
    private static Map<String, String> cliJobTypeMap;

    static {

        jobTypeMap = new HashMap<String, JobType>();
        for (final JobType j : JobType.values()) {
            jobTypeMap.put(j.getJobTypeName(), j);

        }
    }

    /**
     * 
     */
    JobType(final String jobTypeName, final String cliJobType) {
        this.jobTypeName = jobTypeName;
        this.cliJobType = cliJobType;
    }

    /**
     * @return the jobTypeName
     */
    public String getJobTypeName() {
        return jobTypeName;
    }

    /**
     * @return the cliJobName
     */
    public String getCliJobType() {
        return cliJobType;
    }

    /**
     * @return the jobTypeMap
     */
    public static JobType getJobType(final String jobTypeName) {
        return jobTypeMap.get(jobTypeName);
    }

    /**
     * @return the cliJobNameType
     */
    public static String getCliJobType(final String jobTypeName) {
        cliJobTypeMap = new HashMap<>();
        for (final JobType s : JobType.values()) {
            cliJobTypeMap.put(s.jobTypeName, s.cliJobType);
        }
        return jobTypeName;
    }
}