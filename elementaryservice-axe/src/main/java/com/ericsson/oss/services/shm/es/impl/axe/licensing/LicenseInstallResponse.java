/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.axe.licensing;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import com.ericsson.oss.services.shm.es.axe.common.WinFIOLRequestStatus;

/**
 * @author xsanmee
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseInstallResponse {

    @JsonProperty("status")
    private int status;

    @JsonProperty("License")
    private String license;

    public WinFIOLRequestStatus getStatus() {
        return WinFIOLRequestStatus.getEnum(status);
    }

    public void setStatus(final int status) {
        this.status = status;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(final String license) {
        this.license = license;
    }

    @Override
    public String toString() {
        return "LicenseInstallResponse [status=" + status + ", license=" + license + "]";
    }
}
