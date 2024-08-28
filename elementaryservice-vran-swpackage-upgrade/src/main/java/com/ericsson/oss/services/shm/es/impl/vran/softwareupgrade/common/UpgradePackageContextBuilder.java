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

public class UpgradePackageContextBuilder {

    private final UpgradePackageContext upgradePackageContext = new UpgradePackageContext();

    public UpgradePackageContextBuilder setNodeFdn(final String nodeFdn) {
        upgradePackageContext.setNodeFdn(nodeFdn);
        return this;
    }

    public UpgradePackageContextBuilder setNodeName(final String nodeName) {
        upgradePackageContext.setNodeName(nodeName);
        return this;
    }

    public UpgradePackageContextBuilder setActionTriggered(final String actionTriggered) {
        upgradePackageContext.setActionTriggered(actionTriggered);
        return this;
    }

    public UpgradePackageContextBuilder setBusinessKey(final String businessKey) {
        upgradePackageContext.setBusinessKey(businessKey);
        return this;
    }

    public UpgradePackageContextBuilder setJobEnvironment(final JobEnvironment jobEnvironment) {
        upgradePackageContext.setJobEnvironment(jobEnvironment);
        return this;
    }

    public UpgradePackageContextBuilder setPackageLocation(final String packageLocation) {
        upgradePackageContext.setPackageLocation(packageLocation);
        return this;
    }

    public UpgradePackageContextBuilder setSoftwarePackageName(final String softwarePackageName) {
        upgradePackageContext.setSoftwarePackageName(softwarePackageName);
        return this;
    }

    public UpgradePackageContextBuilder setSoftwarePackageId(final String softwarePackageId) {
        upgradePackageContext.setSoftwarePackageId(softwarePackageId);
        return this;
    }

    public UpgradePackageContextBuilder setVnfJobId(final int vnfJobId) {
        upgradePackageContext.setVnfJobId(vnfJobId);
        return this;
    }

    public UpgradePackageContextBuilder setVnfId(final String vnfId) {
        upgradePackageContext.setVnfId(vnfId);
        return this;
    }

    public UpgradePackageContextBuilder setVnfPackageId(final String vnfPackageId) {
        upgradePackageContext.setVnfPackageId(vnfPackageId);
        return this;
    }

    public UpgradePackageContextBuilder setVnfDescription(final String vnfDescription) {
        upgradePackageContext.setVnfDescription(vnfDescription);
        return this;
    }

    public UpgradePackageContextBuilder setVnfmFdn(final String vnfmFdn) {
        upgradePackageContext.setVnfmFdn(vnfmFdn);
        return this;
    }

    public UpgradePackageContextBuilder setVnfmName(final String vnfmName) {
        upgradePackageContext.setVnfmName(vnfmName);
        return this;
    }

    public UpgradePackageContext buildUpgradePackageContext() {
        return upgradePackageContext;
    }
}
