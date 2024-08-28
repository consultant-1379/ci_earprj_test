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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;

public class ShmDeleteUpgradePkgJobData extends ShmRemoteJobData implements Serializable {

    private static final long serialVersionUID = -2182720245798041750L;

    private String productNumber;
    private String productRevision;
    private String activity;
    private boolean deleteFromRollbackList;
    private boolean deleteReferredUPs;
    private boolean deleteReferredBackups;

    /**
     * @return the productNumber
     */
    public String getProductNumber() {
        return productNumber;
    }

    /**
     * @param productNumber
     *            the productNumber to set
     */
    public void setProductNumber(final String productNumber) {
        this.productNumber = productNumber;
    }

    /**
     * @return the productRevision
     */
    public String getProductRevision() {
        return productRevision;
    }

    /**
     * @param productRevision
     *            the productRevision to set
     */
    public void setProductRevision(final String productRevision) {
        this.productRevision = productRevision;
    }

    /**
     * @return the activity
     */
    public String getActivity() {
        return activity;
    }

    /**
     * @param activity
     *            the activity to set
     */
    public void setActivity(final String activity) {
        this.activity = activity;
    }

    /**
     * @return the deleteFromRollbackList
     */
    public boolean getDeleteFromRollbackList() {
        return deleteFromRollbackList;
    }

    /**
     * @param deleteFromRollbackList
     *            the deleteFromRollbackList to set
     */
    public void setDeleteFromRollbackList(final boolean deleteFromRollbackList) {
        this.deleteFromRollbackList = deleteFromRollbackList;
    }

    /**
     * @return the deleteReferredUPs
     */
    public boolean getDeleteReferredUPs() {
        return deleteReferredUPs;
    }

    /**
     * @param deleteReferredUPs
     *            the deleteReferredUPs to set
     */
    public void setDeleteReferredUPs(final boolean deleteReferredUPs) {
        this.deleteReferredUPs = deleteReferredUPs;
    }

    @Override
    public String toString() {
        return "ShmDeleteUpgradePkgJobData [productNumber=" + productNumber + ", productRevision=" + productRevision + ", activity=" + activity + ", deleteReferredBackups=" + deleteReferredBackups
                + ", deleteReferredUPs=" + deleteReferredUPs + ", deleteFromRollbackList=" + deleteFromRollbackList + "]";
    }

    /**
     * @return the deleteReferredBackups
     */
    public boolean getDeleteReferredBackups() {
        return deleteReferredBackups;
    }

    /**
     * @param deleteReferredBackups
     *            the deleteReferredBackups to set
     */
    public void setDeleteReferredBackups(final boolean deleteReferredBackups) {
        this.deleteReferredBackups = deleteReferredBackups;
    }
}
