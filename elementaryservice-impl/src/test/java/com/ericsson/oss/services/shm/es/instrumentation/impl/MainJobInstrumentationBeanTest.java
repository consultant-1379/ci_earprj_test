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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@RunWith(MockitoJUnitRunner.class)
public class MainJobInstrumentationBeanTest {

    @InjectMocks
    MainJobInstrumentationBean mainJobInstrumentationBean;

    @Test
    public void testUpgradeMainJobs() {
        final long counter = 1;
        final List<String> jobTypes = new ArrayList<String>();
        jobTypes.add("UPGRADE");
        mainJobInstrumentationBean.updateRunningMainJobCount(1, jobTypes);
        assertEquals(counter, mainJobInstrumentationBean.getUpgradeMainJobs());

    }

    @Test
    public void testLicenseMainJobs() {
        final long counter = 1;
        final List<String> jobTypes = new ArrayList<String>();
        jobTypes.add("LICENSE");
        mainJobInstrumentationBean.updateRunningMainJobCount(1, jobTypes);
        assertEquals(counter, mainJobInstrumentationBean.getLicenseMainJobs());

    }

    @Test
    public void testBackupMainJobs() {
        final long counter = 1;
        final List<String> jobTypes = new ArrayList<String>();
        jobTypes.add("BACKUP");
        mainJobInstrumentationBean.updateRunningMainJobCount(1, jobTypes);
        assertEquals(counter, mainJobInstrumentationBean.getBackupMainJobs());

    }

    @Test
    public void testRestoreMainJobs() {
        final long counter = 1;
        final List<String> jobTypes = new ArrayList<String>();
        jobTypes.add("RESTORE");
        mainJobInstrumentationBean.updateRunningMainJobCount(1, jobTypes);
        assertEquals(counter, mainJobInstrumentationBean.getRestoreMainJobs());

    }

    @Test
    public void testDeleteBackupMainJobs() {
        final long counter = 1;
        final List<String> jobTypes = new ArrayList<String>();
        jobTypes.add("DELETEBACKUP");
        mainJobInstrumentationBean.updateRunningMainJobCount(1, jobTypes);
        assertEquals(counter, mainJobInstrumentationBean.getDeleteBackupMainJobs());

    }

    @Test
    public void testTotalMainJobs() {
        final long counter = 5;
        final List<String> jobTypes = new ArrayList<String>();
        jobTypes.add("LICENSE");
        jobTypes.add("BACKUP");
        jobTypes.add("RESTORE");
        jobTypes.add("DELETEBACKUP");
        jobTypes.add("LICENSE_REFRESH");
        mainJobInstrumentationBean.updateRunningMainJobCount(5, jobTypes);
        assertEquals(counter, mainJobInstrumentationBean.getTotalMainJobs());

    }

    @Test
    public void testCppTotalNumberOfJobTypes() {
        final long counter = 4;
        mainJobInstrumentationBean.actvityStart(JobTypeEnum.DELETEBACKUP.toString());
        mainJobInstrumentationBean.actvityStart(JobTypeEnum.UPGRADE.toString());
        mainJobInstrumentationBean.actvityStart(JobTypeEnum.RESTORE.toString());
        mainJobInstrumentationBean.actvityStart(JobTypeEnum.LICENSE.toString());
        assertEquals(counter, mainJobInstrumentationBean.getTotalJobTypesInProgress());

    }

    @Test
    public void testGetNodeHealthCheckMainJobs() {
        final long counter = 1;
        final List<String> jobTypes = new ArrayList<String>();
        jobTypes.add("NODE_HEALTH_CHECK");
        mainJobInstrumentationBean.updateRunningMainJobCount(1, jobTypes);
        assertEquals(counter, mainJobInstrumentationBean.getNodeHealthCheckMainJobs());
    }

    @Test
    public void testGetDeleteUpgradePackageMainJobs() {
        final long counter = 1;
        final List<String> jobTypes = new ArrayList<String>();
        jobTypes.add("DELETE_UPGRADEPACKAGE");
        mainJobInstrumentationBean.updateRunningMainJobCount(1, jobTypes);
        assertEquals(counter, mainJobInstrumentationBean.getDeleteUpgradePackageMainJobs());
    }

    @Test
    public void testGetLkfRefreshMainJobs() {
        final long counter = 1;
        final List<String> jobTypes = new ArrayList<String>();
        jobTypes.add("LICENSE_REFRESH");
        mainJobInstrumentationBean.updateRunningMainJobCount(1, jobTypes);
        assertEquals(counter, mainJobInstrumentationBean.getLkfRefreshMainJobs());
    }

    @Test
    public void testEcimLicenseRefreshTotalNumberOfJobTypes() {
        final long counter = 1;
        mainJobInstrumentationBean.actvityStart(JobTypeEnum.LICENSE_REFRESH.toString());
        assertEquals(counter, mainJobInstrumentationBean.getTotalJobTypesInProgress());
    }

    @Test
    public void testEcimLicenseRefreshJobTypes() {
        mainJobInstrumentationBean.actvityStart(JobTypeEnum.LICENSE_REFRESH.toString());
        mainJobInstrumentationBean.activityEnd(JobTypeEnum.LICENSE_REFRESH.toString());
    }

}
