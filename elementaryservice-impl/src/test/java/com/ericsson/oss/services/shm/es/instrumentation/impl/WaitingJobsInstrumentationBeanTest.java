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
package com.ericsson.oss.services.shm.es.instrumentation.impl;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.cluster.counter.NamedCounter;

@RunWith(MockitoJUnitRunner.class)
public class WaitingJobsInstrumentationBeanTest {

    private static final String JOB_TYPE = "BACKUP";
    private static final String WAITING_MAINJOB = "WaitingMainJob";
    private static final String TOTAL_WAITING_MAIN_JOBS = "totalWaitingMainJobs";
    private static final String WAITING_NEJOB = "WaitingNeJob";
    private static final String TOTAL_WAITING_NE_JOBS = "totalWaitingNeJobs";
    private static final String NE_TYPE = "erbs";

    private final Map<String, NamedCounter> counterMap = new HashMap<String, NamedCounter>();

    @InjectMocks
    WaitingJobsInstrumentationBean waitingJobsInstrumentationBean;

    @Test
    public void testWaitingJobStart() {
        waitingJobsInstrumentationBean.waitingJobStart(JOB_TYPE);
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getBackupWaitingMainJobs());
    }

    @Test
    public void test_WaitingJobStart() {
        waitingJobsInstrumentationBean.waitingJobStart(JOB_TYPE, NE_TYPE);
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getErbsBackupWaitingNeJobs());
    }

    @Test
    public void testWaitingJobEnd() {
        waitingJobsInstrumentationBean.waitingJobStart(JOB_TYPE);
        waitingJobsInstrumentationBean.waitingJobStart(JOB_TYPE);
        waitingJobsInstrumentationBean.waitingJobEnd(JOB_TYPE);
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getBackupWaitingMainJobs());
    }

    @Test
    public void test_WaitingJobEnd() {
        waitingJobsInstrumentationBean.waitingJobStart(JOB_TYPE, NE_TYPE);
        waitingJobsInstrumentationBean.waitingJobStart(JOB_TYPE, NE_TYPE);
        waitingJobsInstrumentationBean.waitingJobEnd(JOB_TYPE, NE_TYPE);
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getErbsBackupWaitingNeJobs());
    }

    @Test
    public void test_getBackupWaitingMainJobs() {
        waitingJobsInstrumentationBean.waitingJobStart(JOB_TYPE);
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getBackupWaitingMainJobs());
    }

    @Test
    public void test_getUpgradeWaitingMainJobs() {

        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getUpgradeWaitingMainJobs());
    }

    @Test
    public void test_getLicenseWaitingMainJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("LICENSE");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getLicenseWaitingMainJobs());
    }

    @Test
    public void test_getRestoreWaitingMainJobs() {

        waitingJobsInstrumentationBean.waitingJobStart("RESTORE");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getRestoreWaitingMainJobs());
    }

    @Test
    public void test_getDeleteBackupWaitingMainJobs() {

        waitingJobsInstrumentationBean.waitingJobStart("DELETEBACKUP");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getDeleteBackupWaitingMainJobs());
    }

    @Test
    public void test_getErbsRestoreWaitingNeJobs() {

        waitingJobsInstrumentationBean.waitingJobStart("RESTORE", NE_TYPE);
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getErbsRestoreWaitingNeJobs());
    }

    @Test
    public void test_getErbsDeleteBackupWaitingNeJobs() {

        waitingJobsInstrumentationBean.waitingJobStart("DELETEBACKUP", NE_TYPE);
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getErbsDeleteBackupWaitingNeJobs());
    }

    @Test
    public void test_getErbsLicenseWaitingNeJobs() {

        waitingJobsInstrumentationBean.waitingJobStart("LICENSE", NE_TYPE);
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getErbsLicenseWaitingNeJobs());
    }

    @Test
    public void test_getErbsUpgradeWaitingNeJobs() {

        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", NE_TYPE);
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getErbsUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getErbsBackupWaitingNeJobs() {

        waitingJobsInstrumentationBean.waitingJobStart("BACKUP", NE_TYPE);
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getErbsBackupWaitingNeJobs());
    }

    @Test
    public void test_getTotalWaitingMainJobs() {

        waitingJobsInstrumentationBean.waitingJobStart("BACKUP");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getTotalWaitingMainJobs());
    }

    /*
     * @Test public void testGetCppWaitingMainJobs() {
     * when(counterManager.getOrCreateCounter("CPPMainJob",
     * 0L)).thenReturn(namedCounter); Assert.assertEquals(0,
     * waitingJobsInstrumentationBean.getCppWaitingMainJobs()); }
     * 
     * @Test public void testGetCppWaitingNeJobActivities() {
     * when(counterManager.getOrCreateCounter("CPPNeJob",
     * 0L)).thenReturn(namedCounter); Assert.assertEquals(0,
     * waitingJobsInstrumentationBean.getCppWaitingNeJobActivities()); }
     */

    @Test
    public void test_getBscUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "BSC");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getBscUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getMscBcIsUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "MSC-BC-IS");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMscBcIsUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getMscDbUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "MSC-DB");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMscDbUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getMscDbBspUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "MSC-DB-BSP");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMscDbBspUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getVipStpUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "vIP-STP");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getVipStpUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getVmscUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "vMSC");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getVmscUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getVmscHcUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "vMSC-HC");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getVmscHcUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getIpStpUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "IP-STP");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getIpStpUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getIpStpBspUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "IP-STP-BSP");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getIpStpBspUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getVhlrFeUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "vHLR-FE");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getVhlrFeUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getDeleteUpgradePackageWaitingMainJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("DELETE_UPGRADEPACKAGE");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getDeleteUpgradePackageWaitingMainJobs());
    }

    @Test
    public void test_getNHCWaitingMainJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("NODE_HEALTH_CHECK");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getNHCWaitingMainJobs());
    }

    @Test
    public void test_getTotalWaitingNEJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("LICENSE_REFRESH", "RadioNode");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getTotalWaitingNEJobs());
    }

    @Test
    public void test_getSgsnRestoreWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("RESTORE", "sgsn-mme");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getSgsnRestoreWaitingNeJobs());
    }

    @Test
    public void test_getDusGen2RestoreWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("RESTORE", "radionode");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getDusGen2RestoreWaitingNeJobs());
    }

    @Test
    public void test_getMiniLinkIndoorcn510r2RestoreWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("RESTORE", "MINI-LINK-CN510R2");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMiniLinkIndoorcn510r2RestoreWaitingNeJobs());
    }

    @Test
    public void test_getMiniLinkIndoorcn210RestoreWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("RESTORE", "MINI-LINK-CN210");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMiniLinkIndoorcn210RestoreWaitingNeJobs());
    }

    @Test
    public void test_getMiniLinkIndoorcn510r1RestoreWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("RESTORE", "MINI-LINK-CN510R1");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMiniLinkIndoorcn510r1RestoreWaitingNeJobs());
    }

    @Test
    public void test_getMiniLinkIndoorcn810r1RestoreWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("RESTORE", "MINI-LINK-CN810R1");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMiniLinkIndoorcn810r1RestoreWaitingNeJobs());
    }

    @Test
    public void test_getMiniLinkIndoorcn810r2RestoreWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("RESTORE", "MINI-LINK-CN810R2");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMiniLinkIndoorcn810r2RestoreWaitingNeJobs());
    }

    @Test
    public void test_getSgsnBackupWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("BACKUP", "sgsn-mme");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getSgsnBackupWaitingNeJobs());
    }

    @Test
    public void test_getDusGen2BackupWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("BACKUP", "radionode");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getDusGen2BackupWaitingNeJobs());
    }

    @Test
    public void test_getDusGen2UpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "radionode");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getDusGen2UpgradeWaitingNeJobs());
    }

    @Test
    public void test_getSgsnUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "sgsn-mme");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getSgsnUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getMiniLinkIndoorUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "MINI-LINK-Indoor");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMiniLinkIndoorUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getMiniLinkIndoorcn510r2UpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "MINI-LINK-CN510R2");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMiniLinkIndoorcn510r2UpgradeWaitingNeJobs());
    }

    @Test
    public void test_getMiniLinkIndoorLicenseWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("LICENSE", "MINI-LINK-Indoor");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMiniLinkIndoorLicenseWaitingNeJobs());
    }

    @Test
    public void test_getDusGen2LicenseWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("LICENSE", "radionode");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getDusGen2LicenseWaitingNeJobs());
    }

    @Test
    public void test_getDusGen2DeleteBackupWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("DELETEBACKUP", "radionode");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getDusGen2DeleteBackupWaitingNeJobs());
    }

    @Test
    public void test_getSgsnDeleteBackupWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("DELETEBACKUP", "sgsn-mme");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getSgsnDeleteBackupWaitingNeJobs());
    }

    @Test
    public void test_getErbsDeleteUpgradePackageWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("DELETE_UPGRADEPACKAGE", "erbs");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getErbsDeleteUpgradePackageWaitingNeJobs());
    }

    @Test
    public void test_getDusGen2DeleteUpgradePackageWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("DELETE_UPGRADEPACKAGE", "radionode");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getDusGen2DeleteUpgradePackageWaitingNeJobs());
    }

    @Test
    public void test_getSgsnDeleteUpgradePackageWaitingNeJob() {
        waitingJobsInstrumentationBean.waitingJobStart("DELETE_UPGRADEPACKAGE", "sgsn-mme");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getSgsnDeleteUpgradePackageWaitingNeJob());
    }

    @Test
    public void test_getMiniLink669xRestoreWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("RESTORE", "MINI-LINK-669x");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMiniLink669xRestoreWaitingNeJobs());
    }

    @Test
    public void test_getMiniLink669xUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "MINI-LINK-669x");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMiniLink669xUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getMiniLink665xRestoreWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("RESTORE", "MINI-LINK-665x");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMiniLink665xRestoreWaitingNeJobs());
    }

    @Test
    public void test_getMiniLink665xUpgradeWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("UPGRADE", "MINI-LINK-665x");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getMiniLink665xUpgradeWaitingNeJobs());
    }

    @Test
    public void test_getLicenseRefreshWaitingMainJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("LICENSE_REFRESH");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getLicenseRefreshWaitingMainJobs());
    }

    @Test
    public void test_getDusGen2LicenseRefreshWaitingNeJobs() {
        waitingJobsInstrumentationBean.waitingJobStart("LICENSE_REFRESH", "radionode");
        Assert.assertEquals(1, waitingJobsInstrumentationBean.getDusGen2LicenseRefreshWaitingNeJobs());
    }

}
