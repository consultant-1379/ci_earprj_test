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

/**
 * BACKUP activities enum to fetch external inputs for selected activities.
 * 
 * @author xmannit
 * 
 */
public enum BackupActivityEnum {

    CREATE_CV("createcv"), SETSTARTABLE("setcvasstartable"), SETFIRSTINROLLBACK("setcvfirstinrollbacklist"), UPLOAD_CV("exportcv"), CREATE_BACKUP("createbackup"), UPLOAD_BACKUP("uploadbackup");

    private String activityName;

    BackupActivityEnum(final String activityName) {
        this.activityName = activityName;
    }

    /**
     * @return the activityName
     */
    public String getActivityName() {
        return activityName;
    }

    /**
     * @param activityName
     *            the activityName to set
     */
    public void setActivityName(final String activityName) {
        this.activityName = activityName;
    }

}
