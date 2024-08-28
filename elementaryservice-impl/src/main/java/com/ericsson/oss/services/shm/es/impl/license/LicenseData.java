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
package com.ericsson.oss.services.shm.es.impl.license;

public class LicenseData {

    private long poId;
    private String licenseKeyFilePath;
    private String sequenceNumber;
    private String fingerPrint;

    /**
     * @return the licenseKeyFilePath
     */
    public String getLicenseKeyFilePath() {
        return licenseKeyFilePath;
    }

    /**
     * @param licenseKeyFilePath
     *            the licenseKeyFilePath to set
     */
    public void setLicenseKeyFilePath(final String licenseKeyFilePath) {
        this.licenseKeyFilePath = licenseKeyFilePath;
    }

    /**
     * @return the poId
     */
    public long getPoId() {
        return poId;
    }

    /**
     * @param poId
     *            the poId to set
     */
    public void setPoId(final long poId) {
        this.poId = poId;
    }

    /**
     * @return the sequenceNumber
     */
    public String getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * @param sequenceNumber
     *            the sequenceNumber to set
     */
    public void setSequenceNumber(final String sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * @return the fingerPrint
     */
    public String getFingerPrint() {
        return fingerPrint;
    }

    /**
     * @param fingerPrint
     *            the fingerPrint to set
     */
    public void setFingerPrint(final String fingerPrint) {
        this.fingerPrint = fingerPrint;
    }
}
