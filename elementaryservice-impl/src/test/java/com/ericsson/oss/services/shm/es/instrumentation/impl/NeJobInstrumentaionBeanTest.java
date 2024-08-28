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

import java.util.Iterator;

import javax.cache.Cache;
import javax.cache.Cache.Entry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NeJobInstrumentaionBeanTest {

    private static final String PLATEFORM_TYPE = "CPP";
    private static final String JOB_TYPE = "UPGRADE";

    @InjectMocks
    private NeJobInstrumentaionBean objectUnderTest;

    @Mock
    private Cache<String, Double> jobMetricsCache;

    @Mock
    Iterator<javax.cache.Cache.Entry<String, Double>> iterator;

    @Mock
    Entry<String, Double> entry;

    @Test
    public void test_actvityStart() {

        objectUnderTest.actvityStart(PLATEFORM_TYPE, JOB_TYPE);
        Assert.assertEquals(1, objectUnderTest.getCppUpgradeNeJobs());
    }

    @Test
    public void test_activityEnd_totalNeJobs() {
        objectUnderTest.actvityStart(PLATEFORM_TYPE, JOB_TYPE);
        objectUnderTest.activityEnd(PLATEFORM_TYPE, JOB_TYPE);
        Assert.assertEquals(0, objectUnderTest.getTotalActiveNeJobs());
    }

    @Test
    public void test_activityEnd_withCountnotZero() {

        objectUnderTest.actvityStart(PLATEFORM_TYPE, JOB_TYPE);
        objectUnderTest.actvityStart(PLATEFORM_TYPE, JOB_TYPE);
        objectUnderTest.activityEnd(PLATEFORM_TYPE, JOB_TYPE);
        Assert.assertEquals(1, objectUnderTest.getCppUpgradeNeJobs());
    }

    @Test
    public void test_getCppUpgradeNeJobs() {

        objectUnderTest.actvityStart(PLATEFORM_TYPE, JOB_TYPE);
        Assert.assertEquals(1, objectUnderTest.getCppUpgradeNeJobs());
    }

    @Test
    public void test_getCppBackupNeJobs() {

        objectUnderTest.getCppBackupNeJobs();
        objectUnderTest.actvityStart(PLATEFORM_TYPE, "BACKUP");
        Assert.assertEquals(1, objectUnderTest.getCppBackupNeJobs());
    }

    @Test
    public void test_getCppLicenseNeJobs() {

        objectUnderTest.getCppBackupNeJobs();
        objectUnderTest.actvityStart(PLATEFORM_TYPE, "LICENSE");
        Assert.assertEquals(1, objectUnderTest.getCppLicenseNeJobs());
    }

    @Test
    public void test_getEcimUpgradeNeJobs() {

        objectUnderTest.getEcimUpgradeNeJobs();
        objectUnderTest.actvityStart("ECIM", "UPGRADE");
        Assert.assertEquals(1, objectUnderTest.getEcimUpgradeNeJobs());
    }

    @Test
    public void test_getCppTotalNeJobs() {

        objectUnderTest.actvityStart("ECIM", "UPGRADE");
        Assert.assertEquals(1, objectUnderTest.getTotalActiveNeJobs());
    }

    @Test
    public void test_getEcimLicenseRefreshNeJobscount() {
        objectUnderTest.actvityStart("ECIM", "LICENSE_REFRESH");
        Assert.assertEquals(1, objectUnderTest.getEcimLicenseRefreshNeJobscount());
    }

    @Test
    public void test_getAxeLicenseNeJobs() {
        objectUnderTest.actvityStart("AXE", "LICENSE");
        Assert.assertEquals(1, objectUnderTest.getAxeLicenseNeJobs());
    }

    @Test
    public void test_getAXEDeleteBackupNeJobs() {
        objectUnderTest.actvityStart("AXE", "DELETEBACKUP");
        Assert.assertEquals(1, objectUnderTest.getAXEDeleteBackupNeJobs());
    }

    @Test
    public void getAXEBackupNeJobs() {
        objectUnderTest.actvityStart("AXE", "BACKUP");
        Assert.assertEquals(1, objectUnderTest.getAXEBackupNeJobs());
    }

    @Test
    public void getAXEUpgradeNeJobs() {
        objectUnderTest.actvityStart("AXE", "UPGRADE");
        Assert.assertEquals(1, objectUnderTest.getAXEUpgradeNeJobs());
    }

    @Test
    public void getCppDeleteUpgradePackageNeJobs() {
        objectUnderTest.actvityStart("CPP", "DELETE_UPGRADEPACKAGE");
        Assert.assertEquals(1, objectUnderTest.getCppDeleteUpgradePackageNeJobs());
    }

    @Test
    public void getEcimDeleteUpgradePackageNeJobs() {
        objectUnderTest.actvityStart("ECIM", "DELETE_UPGRADEPACKAGE");
        Assert.assertEquals(1, objectUnderTest.getEcimDeleteUpgradePackageNeJobs());
    }

    @Test
    public void getEcimLicenseNeJobs() {
        objectUnderTest.actvityStart("ECIM", "LICENSE");
        Assert.assertEquals(1, objectUnderTest.getEcimLicenseNeJobs());
    }

    @Test
    public void getEcimNodeHealthCheckNeJobscount() {
        objectUnderTest.actvityStart("ECIM", "NODE_HEALTH_CHECK");
        Assert.assertEquals(1, objectUnderTest.getEcimNodeHealthCheckNeJobscount());
    }

    @Test
    public void getEcimRestoreNeJobs() {
        objectUnderTest.actvityStart("ECIM", "RESTORE");
        Assert.assertEquals(1, objectUnderTest.getEcimRestoreNeJobs());
    }

    @Test
    public void getEcimDeleteBackupNeJobs() {
        objectUnderTest.actvityStart("ECIM", "DELETEBACKUP");
        Assert.assertEquals(1, objectUnderTest.getEcimDeleteBackupNeJobs());
    }

    @Test
    public void getEcimBackupNeJobs() {
        objectUnderTest.actvityStart("ECIM", "BACKUP");
        Assert.assertEquals(1, objectUnderTest.getEcimBackupNeJobs());
    }

    @Test
    public void test_getCppRestoreNeJobs() {
        objectUnderTest.getCppRestoreNeJobs();
        objectUnderTest.actvityStart(PLATEFORM_TYPE, "RESTORE");
        Assert.assertEquals(1, objectUnderTest.getCppRestoreNeJobs());
    }

    @Test
    public void test_getCppDeleteBackupNeJobs() {
        objectUnderTest.getCppDeleteBackupNeJobs();
        objectUnderTest.actvityStart(PLATEFORM_TYPE, "DELETEBACKUP");
        Assert.assertEquals(1, objectUnderTest.getCppDeleteBackupNeJobs());
    }
}
