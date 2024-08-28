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
package com.ericsson.oss.services.shm.job.remote.api.license;

import java.io.Serializable;

public class LicenseJobData implements Serializable {

    private static final long serialVersionUID = -1298654683034647L;

    private String nodeName;
    private String fingerPrint;
    private String sequenceNumber;
    private String licenseKeyFilePath;
    private String nodetype;

    public String getNodetype() {
        return nodetype;
    }

    public void setNodetype(final String nodetype) {
        this.nodetype = nodetype;
    }

    public String getLicenseKeyFilePath() {
        return licenseKeyFilePath;
    }

    public void setLicenseKeyFilePath(final String licenseKeyFilePath) {
        this.licenseKeyFilePath = licenseKeyFilePath;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    public String getFingerPrint() {
        return fingerPrint;
    }

    public void setFingerPrint(final String fingerPrint) {
        this.fingerPrint = fingerPrint;
    }

    public String getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(final String sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

}
