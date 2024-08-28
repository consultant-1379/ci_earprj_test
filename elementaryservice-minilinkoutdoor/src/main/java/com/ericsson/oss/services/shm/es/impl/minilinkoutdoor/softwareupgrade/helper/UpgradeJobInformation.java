/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.helper;

/**
 * This class holds the information for all MINI-LINK Outdoor upgrade job
 * 
 * @author NightsWatch
 */
public class UpgradeJobInformation {

    private String nodeFdn;
    private String nodeName;
    private String fileName;
    private String productRevision;
    private String productNumber;
    private long neJobId;

    public UpgradeJobInformation(final String nodeFdn, final String nodeName) {
        this.nodeFdn = nodeFdn;
        this.nodeName = nodeName;
    }

    /**
     * @return the filePath
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param filePath
     * 
     */
    public void setFileName(final String filePath) {
        this.fileName = filePath;
    }

    public String getProductRevision() {
        return productRevision;
    }

    /**
     * @param filename
     * 
     */
    public void setProductRevision(final String productRevision) {
        this.productRevision = productRevision;
    }

    /**
     * @return the softwarePackageId
     */
    public String getProductNumber() {
        return productNumber;
    }

    /**
     * @param softwarePackageId
     * 
     */
    public void setProductNumber(final String productNumber) {
        this.productNumber = productNumber;
    }

    /**
     * @return the nodeFdn
     */
    public String getNodeFdn() {
        return nodeFdn;
    }

    /**
     * @param nodeFdn
     * 
     */
    public void setNodeFdn(final String nodeFdn) {
        this.nodeFdn = nodeFdn;
    }

    /**
     * @return the neName
     */
    public String getNeName() {
        return nodeName;
    }

    /**
     * @param neName
     * 
     */
    public void setNeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    /**
     * @return the neJobId
     */
    public long getNeJobId() {
        return neJobId;
    }

    /**
     * @param neJobId
     *            the neJobId to set
     */
    public void setNeJobId(final long neJobId) {
        this.neJobId = neJobId;
    }

    @Override
    public String toString() {
        return "UpgradeJobInformation [nodeFdn=" + nodeFdn + ", nodeName=" + nodeName + ", fileName=" + fileName + ", productRevision=" + productRevision
                + ", productNumber=" + productNumber + ", neJobId= " + neJobId + "]";
    }

}
