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

package com.ericsson.oss.services.shm.es.impl.minilink.common;

import com.ericsson.oss.services.shm.es.impl.JobEnvironment;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.*;

/**
 * This class serves as a DTO between the BackupService and the other beans. It gives an API for retrieving the properties and arguments of the job.
 * It should be instantiated with the getBackupActivityProperties methods in the BackupUtil class as this requires the usage of the ActivityUtils bean.
 */
public class BackupActivityProperties {

    private final long activityJobId;
    private final String activityName;
    private final JobEnvironment jobEnvironment;
    private String backupName;

    private final Class<?> activityServiceClass;

    public BackupActivityProperties(final long activityJobId, final JobEnvironment jobEnvironment, final String backupName, final String activityName, final Class<?> activityServiceClass) {
        this.activityJobId = activityJobId;
        this.jobEnvironment = jobEnvironment;
        this.backupName = backupName;
        this.activityName = activityName;
        this.activityServiceClass = activityServiceClass;
    }

    /**
     * Returns the activity's job ID.
     * @return
     */
    public long getActivityJobId() {
        return activityJobId;
    }

    /**
     * Returns the corresponding JobEnvironment
     * @return
     */
    public JobEnvironment getJobEnvironment() {
        return jobEnvironment;
    }

    /**
     * Returns the node's name to which this activity corresponds to.l
     * @return
     */
    public String getNodeName() {
        return jobEnvironment.getNodeName();
    }

    /**
     * Return the backup name that was set in the UI when the job was created.
     * @return
     */
    public String getBackupName() {
        return backupName;
    }

    /**
     * Returns the backup file name relative to the MINI-LINK backup smrs root.
     * @return
     */
    public String getBackupFileWithPath() {
        return getNodeName() + SLASH + getBackupFileName();
    }

    /***
     * Returns the backup file's name (with the .cfg extension)
     * @return
     */
    public String getBackupFileName() {
        return getBackupName() + DOT + CONFIG_FILE_EXTENSION;
    }

    public String getActivityName() {
        return activityName;
    }

    public Class<?> getActivityServiceClass() {
        return activityServiceClass;
    }

    /**
     * @param backupName
     */
    public void setBackupName(final String backupName) {
        this.backupName = backupName;
    }

}
