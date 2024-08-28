/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been .
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup;

import java.util.List;

public class DeleteUpgradePackageActionInfo {

    private String productNumber;
    private String productRevision;
    private boolean deleteFromRollbackList;
    private boolean deleteReferredUPs;
    private List<ProductDataBean> productData;

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
     * @return the isPreventUpDeletable
     */
    public boolean isPreventUpDeletable() {
        return deleteFromRollbackList;
    }

    /**
     * @param isPreventUpDeletable
     *            the isPreventUpDeletable to set
     */
    public void setPreventUpDeletable(final boolean isPreventUpDeletable) {
        this.deleteFromRollbackList = isPreventUpDeletable;
    }

    /**
     * @return the isPreventCvDeletable
     */
    public boolean isPreventCvDeletable() {
        return deleteReferredUPs;
    }

    /**
     * @param isPreventCvDeletable
     *            the isPreventCvDeletable to set
     */
    public void setPreventCvDeletable(final boolean isPreventCvDeletable) {
        this.deleteReferredUPs = isPreventCvDeletable;
    }

    /**
     * @return the productData
     */
    public List<ProductDataBean> getProductData() {
        return productData;
    }

    /**
     * @param productData
     *            the productData to set
     */
    public void setProductData(final List<ProductDataBean> productData) {
        this.productData = productData;
    }

    @Override
    public String toString() {
        return "DeleteUpgradePackageActionInfo [productNumber=" + productNumber + ", productRevision=" + productRevision + ", deleteFromRollbackList=" + deleteFromRollbackList + ", deleteReferredUPs="
                + deleteReferredUPs + ", productData=" + productData + "]";
    }

}
