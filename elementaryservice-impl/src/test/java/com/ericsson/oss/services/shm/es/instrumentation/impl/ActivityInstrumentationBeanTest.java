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
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.loadcontrol.local.api.SHMLoadControllerLocalService;

@RunWith(MockitoJUnitRunner.class)
public class ActivityInstrumentationBeanTest {

    @InjectMocks
    ActivityInstrumentationBean activityInstrumentationBean;

    @Mock
    private SHMLoadControllerLocalService loadControllerLocalService;

    private final long counter = 4;

    private final AtomicLong loadControllerValue = new AtomicLong(4);

    @Test
    public void testCppUpgradeInstalls() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "UPGRADE", "install")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppUpgradeInstalls());
    }

    @Test
    public void testCppUpgradeVerifies() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "UPGRADE", "verify")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppUpgradeVerifies());
    }

    @Test
    public void testCppUpgradeUpgrades() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "UPGRADE", "upgrade")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppUpgradeUpgrades());
    }

    @Test
    public void testCppUpgradeConfirms() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "UPGRADE", "confirm")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppUpgradeConfirms());

    }

    @Test
    public void testCppBackupSetCVAsStartables() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "BACKUP", "setcvasstartable")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppBackupSetCVAsStartables());

    }

    @Test
    public void testCppBackupSetCVFirstInRollbackLists() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "BACKUP", "setcvfirstinrollbacklist")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppBackupSetCVFirstInRollbackLists());

    }

    @Test
    public void testCppBackupExportCVs() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "BACKUP", "exportcv")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppBackupExportCVs());

    }

    @Test
    public void testCppBackupCreateCVs() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "BACKUP", "createcv")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppBackupCreateCVs());

    }

    @Test
    public void testCppLicenseInstalls() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "LICENSE", "install")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppLicenseInstalls());

    }

    @Test
    public void testCppRestoreDownloadCVs() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "RESTORE", "download")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppRestoreDownloadCVs());

    }

    @Test
    public void testCppRestoreVerifyCVs() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "RESTORE", "verify")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppRestoreVerifyCVs());

    }

    @Test
    public void testCppRestoreInstallCVs() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "RESTORE", "install")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppRestoreInstallCVs());

    }

    @Test
    public void testCppRestoreRestores() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "RESTORE", "restore")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppRestoreRestores());

    }

    @Test
    public void testCppRestoreConfirmCVs() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "RESTORE", "confirm")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppRestoreConfirmCVs());

    }

    @Test
    public void testCppDeleteBackup() {
        when(loadControllerLocalService.getCurrentLoadControllerValue("CPP", "DELETEBACKUP", "deletecv")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getCppDeleteBackup());

    }

    @Test
    public void testEcimUpgradePrepares() {
        when(loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.UPGRADE.toString(), "prepare")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getEcimUpgradePrepares());

    }

    @Test
    public void testEcimUpgradeVerifys() {
        when(loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.UPGRADE.toString(), "verify")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getEcimUpgradeVerifys());

    }

    @Test
    public void testEcimUpgradeActivates() {
        when(loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.UPGRADE.toString(), "activate")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getEcimUpgradeActivates());

    }

    @Test
    public void testEcimUpgradeConfirms() {
        when(loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.UPGRADE.toString(), "confirm")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getEcimUpgradeConfirms());

    }

    @Test
    public void testCppTotalActivities() {
        for (int i = 0; i < counter; i++) {
            activityInstrumentationBean.actvityStart(PlatformTypeEnum.CPP.toString(), JobTypeEnum.UPGRADE.toString(), "verify");
        }
        assertEquals(counter, activityInstrumentationBean.getCppTotalActivities());

    }

    @Test
    public void testGetEcimLicenseRefreshJobRefreshActivities() {
        when(loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.LICENSE_REFRESH.toString(), "refresh")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getEcimLicenseRefreshJobRefreshActivities());
    }

    @Test
    public void testGetEcimLicenseRefreshJobRequestActivities() {
        when(loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.LICENSE_REFRESH.toString(), "request")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getEcimLicenseRefreshJobRequestActivities());
    }

    @Test
    public void testGetEcimLicenseRefreshJobInstallActivities() {
        when(loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.LICENSE_REFRESH.toString(), "install")).thenReturn(loadControllerValue);
        assertEquals(counter, activityInstrumentationBean.getEcimLicenseRefreshJobInstallActivities());
    }

}
