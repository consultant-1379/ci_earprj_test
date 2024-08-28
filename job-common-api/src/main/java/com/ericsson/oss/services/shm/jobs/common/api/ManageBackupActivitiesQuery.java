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
package com.ericsson.oss.services.shm.jobs.common.api;

public class ManageBackupActivitiesQuery {
    private String neType;
    private Boolean multipleBackups;

    /**
     * @return the neType
     */
    public String getNeType() {
        return neType;
    }

    /**
     * @param neType
     *            the neType to set
     */
    public void setNeType(final String neType) {
        this.neType = neType;
    }

    /**
     * @return the multipleBackups
     */
    public Boolean getMultipleBackups() {
        return multipleBackups;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ManageBackupActivitiesQuery [neType=" + neType + ", multipleBackups=" + multipleBackups + "]";
    }

    /**
     * @param multipleBackups
     *            the multipleBackups to set
     */
    public void setMultipleBackups(final Boolean multipleBackups) {
        this.multipleBackups = multipleBackups;
    }

}
