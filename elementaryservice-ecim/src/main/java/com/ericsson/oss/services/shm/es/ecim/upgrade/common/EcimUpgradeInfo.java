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
package com.ericsson.oss.services.shm.es.ecim.upgrade.common;

import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;

/**
 * This class contains the getter and setter methods for preparing ECIM Upgrade Job Environment.
 * 
 * @author xvishsr
 * 
 */
public class EcimUpgradeInfo {

    private String actionTriggered;
    private boolean ignoreBreakPoints;
    private NEJobStaticData neJobStaticData;
    private JobEnvironment jobEnvironment;
    private String upgradePackageFilePath;
    private long activityJobId;
    private String upgradeType;

    public String getActionTriggered() {
        return actionTriggered;
    }

    public void setActionTriggered(final String actionTriggered) {
        this.actionTriggered = actionTriggered;
    }

    public boolean isIgnoreBreakPoints() {
        return ignoreBreakPoints;
    }

    public void setIgnoreBreakPoints(final boolean ignoreBreakPoints) {
        this.ignoreBreakPoints = ignoreBreakPoints;
    }

    public NEJobStaticData getNeJobStaticData() {
        return neJobStaticData;
    }

    public void setNeJobStaticData(final NEJobStaticData neJobStaticData) {
        this.neJobStaticData = neJobStaticData;
    }

    public JobEnvironment getJobEnvironment() {
        return jobEnvironment;
    }

    public void setJobEnvironment(final JobEnvironment jobEnvironment) {
        this.jobEnvironment = jobEnvironment;
    }

    public String getUpgradePackageFilePath() {
        return upgradePackageFilePath;
    }

    public void setUpgradePackageFilePath(final String upgradePackageFilePath) {
        this.upgradePackageFilePath = upgradePackageFilePath;
    }

    public long getActivityJobId() {
        return activityJobId;
    }

    public void setActivityJobId(final long activityJobId) {
        this.activityJobId = activityJobId;
    }

    public String getUpgradeType() {
        return upgradeType;
    }

    public void setUpgradeType(final String upgradeType) {
        this.upgradeType = upgradeType;
    }

}
