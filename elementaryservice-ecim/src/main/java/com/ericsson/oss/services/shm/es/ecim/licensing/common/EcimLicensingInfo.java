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
package com.ericsson.oss.services.shm.es.ecim.licensing.common;

import java.io.Serializable;

public class EcimLicensingInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String businessKey;
    private short actionId;
    private boolean isActivateTriggered;
    private String installStatus;
    private String licenseKeyFilePath;

    /**
     * @param licenseKeyFilePath
     *            the licenseKeyFilePath to set
     */
    public void setLicenseKeyFilePath(final String licenseKeyFilePath) {
        this.licenseKeyFilePath = licenseKeyFilePath;
    }

    private final String licenseMoFdn;

    public EcimLicensingInfo(final String licenseMoFdn, final String licenseKeyFilePath) {
        this.licenseMoFdn = licenseMoFdn;
        this.licenseKeyFilePath = licenseKeyFilePath;
    }

    /**
     * @return the licenseKeyFilePath
     */
    public String getLicenseKeyFilePath() {
        return licenseKeyFilePath;
    }

    /**
     * @return the licenseMoFdn
     */
    public String getLicenseMoFdn() {
        return licenseMoFdn;
    }

    /**
     * @return the isActivateTriggered
     */
    public boolean isActivateTriggered() {
        return isActivateTriggered;
    }

    /**
     * @param isActivateTriggered
     *            the isActivateTriggered to set
     */
    public void setActivateTriggered(final boolean isActivateTriggered) {
        this.isActivateTriggered = isActivateTriggered;
    }

    public short getActionId() {
        return actionId;
    }

    public void setActionId(final short actionId) {
        this.actionId = actionId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(final String businessKey) {
        this.businessKey = businessKey;
    }

    /**
     * @return the installStatus
     */
    public String getInstallStatus() {
        return installStatus;
    }

    /**
     * @param installStatus
     *            the installStatus to set
     */
    public void setInstallStatus(final String installStatus) {
        this.installStatus = installStatus;
    }

    @Override
    public String toString() {
        return "JobLogResponse [businessKey=" + businessKey + ", actionId=" + actionId + ", isActivateTriggered=" + isActivateTriggered + ",installStatus=" + installStatus + ",licenseMoFdn="
                + licenseMoFdn + ",licenseKeyFilePath=" + licenseKeyFilePath + "]";
    }

}
