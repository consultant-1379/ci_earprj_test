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
package com.ericsson.oss.services.shm.es.impl.ecim.deleteup;

import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackupCreationType;

public class UpgradePackageBean extends Object {

    private String moFdn;
    private String productNumber;
    private String productRevision;
    private String backupName;
    private String brmBackupManagerMoFdn;
    private BrmBackupCreationType creationType;

    public UpgradePackageBean(final String productNumber, final String productRevision, final String moFdn) {
        super();
        this.productNumber = productNumber;
        this.productRevision = productRevision;
        this.moFdn = moFdn;
    }

    public UpgradePackageBean(final String productNumber, final String productRevision) {
        super();
        this.productNumber = productNumber;
        this.productRevision = productRevision;
    }

    public UpgradePackageBean() {
        super();
    }

    @Override
    public String toString() {
        return "UpgradePackageBean [moFdn=" + moFdn + ", productNumber=" + productNumber + ", productRevision=" + productRevision + ", backupName=" + backupName + "]";
    }

    /**
     * @return the moFdn
     */
    public String getMoFdn() {
        return moFdn;
    }

    /**
     * @param moFdn
     *            the moFdn to set
     */
    public void setMoFdn(final String moFdn) {
        this.moFdn = moFdn;
    }

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
     * @return the brmBackupManagerMoFdn
     */
    public String getBrmBackupManagerMoFdn() {
        return brmBackupManagerMoFdn;
    }

    /**
     * @param brmBackupManagerMoFdn
     *            the brmBackupManagerMoFdn to set
     */
    public void setBrmBackupManagerMoFdn(final String brmBackupManagerMoFdn) {
        this.brmBackupManagerMoFdn = brmBackupManagerMoFdn;
    }

    /**
     * @return the creationType
     */
    public BrmBackupCreationType getCreationType() {
        return creationType;
    }

    /**
     * @param creationType
     *            the creationType to set
     */
    public void setCreationType(final BrmBackupCreationType creationType) {
        this.creationType = creationType;
    }

}
