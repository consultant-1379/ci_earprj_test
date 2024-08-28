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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;
import java.util.List;

/**
 * POJO class to fetch backup job configuration data from external services and use it for backup job creation
 * 
 * @author tcsmaup
 * 
 */
public class ShmBackupJobData extends ShmRemoteJobData implements Serializable {

    private static final long serialVersionUID = 2186316806841577519L;

    private String backupName;

    private List<BackupActivityEnum> activities;

    private String backupType;

    private String domainBackupType;

    private String backupComment;

    private boolean autoGenerateBackupName;

    /**
     * @return the isAutoGenerate
     */
    public boolean isAutoGenerateBackupName() {
        return autoGenerateBackupName;
    }

    /**
     * @param isAutoGenerate
     *            the isAutoGenerate to set
     */
    public void setAutoGenerateBackupName(final boolean autoGenerateBackupName) {
        this.autoGenerateBackupName = autoGenerateBackupName;
    }

    /**
     * @return the backupName
     */
    public String getBackupName() {
        return backupName;
    }

    /**
     * @param backupName
     *            the backupName to set
     */
    public void setBackupName(final String backupName) {
        this.backupName = backupName;
    }

    /**
     * @return the activities
     */
    public List<BackupActivityEnum> getActivities() {
        return activities;
    }

    /**
     * @param activities
     *            the activities to set
     */
    public void setActivities(final List<BackupActivityEnum> activities) {
        this.activities = activities;
    }

    /**
     * @return the backupType
     */
    public String getBackupType() {
        return backupType;
    }

    /**
     * @param backupType
     *            the backupType to set
     */
    public void setBackupType(final String backupType) {
        this.backupType = backupType;
    }

    /**
     * @return the domainBackupType
     */
    public String getDomainBackupType() {
        return domainBackupType;
    }

    /**
     * @param domainBackupType
     *            the domainBackupType to set
     */
    public void setDomainBackupType(final String domainBackupType) {
        this.domainBackupType = domainBackupType;
    }

    /**
     * @return the backupComment
     */
    public String getBackupComment() {
        return backupComment;
    }

    /**
     * @param backupComment
     *            the backupComment to set
     */
    public void setBackupComment(final String backupComment) {
        this.backupComment = backupComment;
    }

    @Override
    public String toString() {
        return "JobName:" + this.getJobName() + " BackupName:" + this.getBackupName() + " BackupType: " + this.getBackupType() + "Backup Comment:" + this.getBackupComment()
                + " Selected Backup Activities:" + this.getActivities() + " DomainBackupType: " + this.getDomainBackupType() + " NetworkElementSearchScope:" + this.getNetworkElementSearchScopes()
                + " NeNames:" + this.getNeNames() + " Collection:" + this.getCollection() + " Saved Search ID:" + this.getSavedSearchId();
    }
}
