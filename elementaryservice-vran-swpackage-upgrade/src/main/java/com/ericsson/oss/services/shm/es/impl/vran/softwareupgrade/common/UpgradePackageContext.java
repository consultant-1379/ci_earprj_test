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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common;

import com.ericsson.oss.services.shm.es.impl.JobEnvironment;

public class UpgradePackageContext {

    private String nodeFdn;
    private String nodeName;
    private String actionTriggered;
    private String businessKey;
    private JobEnvironment jobEnvironment;
    private String packageLocation;
    private String softwarePackageName;
    private String softwarePackageId;
    private int vnfJobId;
    private String vnfId;
    private String vnfPackageId;
    private String vnfDescription;
    private String vnfmFdn;
    private String vnfmName;

    public String getNodeFdn() {
        return nodeFdn;
    }

    public void setNodeFdn(final String nodeFdn) {
        this.nodeFdn = nodeFdn;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    public String getActionTriggered() {
        return actionTriggered;
    }

    public void setActionTriggered(final String actionTriggered) {
        this.actionTriggered = actionTriggered;
    }

    public JobEnvironment getJobEnvironment() {
        return jobEnvironment;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(final String businessKey) {
        this.businessKey = businessKey;
    }

    public JobEnvironment getJobContext() {
        return jobEnvironment;
    }

    public void setJobEnvironment(final JobEnvironment jobEnvironment) {
        this.jobEnvironment = jobEnvironment;
    }

    public String getPackageLocation() {
        return packageLocation;
    }

    public void setPackageLocation(final String packageLocation) {
        this.packageLocation = packageLocation;
    }

    public String getSoftwarePackageName() {
        return softwarePackageName;
    }

    public void setSoftwarePackageName(final String softwarePackageName) {
        this.softwarePackageName = softwarePackageName;
    }

    public String getSoftwarePackageId() {
        return softwarePackageId;
    }

    public void setSoftwarePackageId(final String softwarePackageId) {
        this.softwarePackageId = softwarePackageId;
    }

    public String getVnfId() {
        return vnfId;
    }

    public int getVnfJobId() {
        return vnfJobId;
    }

    public void setVnfJobId(final int vnfJobId) {
        this.vnfJobId = vnfJobId;
    }

    public void setVnfId(final String vnfId) {
        this.vnfId = vnfId;
    }

    public String getVnfPackageId() {
        return vnfPackageId;
    }

    public void setVnfPackageId(final String vnfPackageId) {
        this.vnfPackageId = vnfPackageId;
    }

    public String getVnfDescription() {
        return vnfDescription;
    }

    public void setVnfDescription(final String vnfDescription) {
        this.vnfDescription = vnfDescription;
    }

    public String getVnfmFdn() {
        return vnfmFdn;
    }

    public void setVnfmFdn(final String vnfmFdn) {
        this.vnfmFdn = vnfmFdn;
    }

    public String getVnfmName() {
        return vnfmName;
    }

    public void setVnfmName(final String vnfmName) {
        this.vnfmName = vnfmName;
    }

    @Override
    public String toString() {
        return "UpgradePackageContext [nodeFdn=" + nodeFdn + ", nodeName=" + nodeName + ", actionTriggered=" + actionTriggered + ", businessKey=" + businessKey + ", jobEnvironment=" + jobEnvironment
                + ", packageLocation=" + packageLocation + ", softwarePackageName=" + softwarePackageName + ", softwarePackageId=" + softwarePackageId + ", vnfJobId=" + vnfJobId + ", vnfId=" + vnfId
                + ", vnfPackageId=" + vnfPackageId + ", vnfDescription=" + vnfDescription + ", vnfmFdn=" + vnfmFdn + ", vnfmName=" + vnfmName + "]";
    }

}
