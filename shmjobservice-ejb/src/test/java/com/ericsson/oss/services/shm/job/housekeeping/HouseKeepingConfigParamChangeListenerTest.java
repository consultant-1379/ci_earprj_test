/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.housekeeping;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ericsson.oss.services.shm.job.activity.JobType;

public class HouseKeepingConfigParamChangeListenerTest {

    @Test
    public void testGetJobCountForHouseKeeping() {
        final HouseKeepingConfigParamChangeListener houseKeepingConfigParamChangeListener = createAndReturnObjectForTest();
        assertEquals(15, houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.BACKUP));
        assertEquals(10, houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.BACKUP_HOUSEKEEPING));
        assertEquals(50, houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.DELETE_SOFTWAREPACKAGE));
        assertEquals(25, houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.DELETE_UPGRADEPACKAGE));
        assertEquals(20, houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.DELETEBACKUP));
        assertEquals(30, houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.LICENSE));
        assertEquals(115, houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.LICENSE_REFRESH));
        assertEquals(35, houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.NODE_HEALTH_CHECK));
        assertEquals(40, houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.NODERESTART));
        assertEquals(50, houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.ONBOARD));
        assertEquals(45, houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.RESTORE));
        assertEquals(50, houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.SYSTEM));
        assertEquals(55, houseKeepingConfigParamChangeListener.getJobCountForHouseKeeping(JobType.UPGRADE));
    }

    @Test
    public void testGetMinJobAgeForHouseKeeping() {
        final HouseKeepingConfigParamChangeListener houseKeepingConfigParamChangeListener = createAndReturnObjectForTest();
        assertEquals(65, houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(JobType.BACKUP));
        assertEquals(60, houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(JobType.BACKUP_HOUSEKEEPING));
        assertEquals(105, houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(JobType.DELETE_SOFTWAREPACKAGE));
        assertEquals(75, houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(JobType.DELETE_UPGRADEPACKAGE));
        assertEquals(70, houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(JobType.DELETEBACKUP));
        assertEquals(80, houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(JobType.LICENSE));
        assertEquals(85, houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(JobType.LICENSE_REFRESH));
        assertEquals(90, houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(JobType.NODE_HEALTH_CHECK));
        assertEquals(95, houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(JobType.NODERESTART));
        assertEquals(105, houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(JobType.ONBOARD));
        assertEquals(100, houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(JobType.RESTORE));
        assertEquals(105, houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(JobType.SYSTEM));
        assertEquals(110, houseKeepingConfigParamChangeListener.getMinJobAgeForHouseKeeping(JobType.UPGRADE));
    }

    /**
     * @return
     */
    private HouseKeepingConfigParamChangeListener createAndReturnObjectForTest() {

        HouseKeepingConfigParamChangeListener houseKeepingConfigParamChangeListener = new HouseKeepingConfigParamChangeListener();

        houseKeepingConfigParamChangeListener.listenCountOfBackupHousekeepingJobForHouseKeepingAttribute(10);
        houseKeepingConfigParamChangeListener.listenCountOfBackupJobForHouseKeepingAttribute(15);
        houseKeepingConfigParamChangeListener.listenCountOfDeleteBackupJobForHouseKeepingAttribute(20);
        houseKeepingConfigParamChangeListener.listenCountOfDeleteUpgradepackageJobForHouseKeepingAttribute(25);
        houseKeepingConfigParamChangeListener.listenCountOfLicenseJobForHouseKeepingAttribute(30);
        houseKeepingConfigParamChangeListener.listenCountOfNodeHealthCheckReportForHouseKeepingAttribute(35);
        houseKeepingConfigParamChangeListener.listenCountOfNodeRestartJobForHouseKeepingAttribute(40);
        houseKeepingConfigParamChangeListener.listenCountOfRestoreJobForHouseKeepingAttribute(45);
        houseKeepingConfigParamChangeListener.listenCountOfShmJobForHouseKeepingAttribute(50);
        houseKeepingConfigParamChangeListener.listenCountOfUpgradeJobForHouseKeepingAttribute(55);
        houseKeepingConfigParamChangeListener.listenMinimumAgeOfBackupHousekeepingJobForHouseKeepingAttribute(60);
        houseKeepingConfigParamChangeListener.listenMinimumAgeOfBackupJobForHouseKeepingAttribute(65);
        houseKeepingConfigParamChangeListener.listenMinimumAgeOfDeleteBackupJobForHouseKeepingAttribute(70);
        houseKeepingConfigParamChangeListener.listenMinimumAgeOfDeleteUpgradepackageJobForHouseKeepingAttribute(75);
        houseKeepingConfigParamChangeListener.listenMinimumAgeOfLicenseJobForHouseKeepingAttribute(80);
        houseKeepingConfigParamChangeListener.listenMinimumAgeOfLicenseRefreshJobForHouseKeepingAttribute(85);
        houseKeepingConfigParamChangeListener.listenMinimumAgeOfNodeHealthCheckReportForHouseKeepingAttribute(90);
        houseKeepingConfigParamChangeListener.listenMinimumAgeOfNoderRestartJobForHouseKeepingAttribute(95);
        houseKeepingConfigParamChangeListener.listenMinimumAgeOfRestoreJobForHouseKeepingAttribute(100);
        houseKeepingConfigParamChangeListener.listenMinimumAgeOfShmJobForHouseKeepingAttribute(105);
        houseKeepingConfigParamChangeListener.listenMinimumAgeOfUpgradeJobForHouseKeepingAttribute(110);
        houseKeepingConfigParamChangeListener.listenCountOfLicenseRefreshJobForHouseKeepingAttribute(115);

        return houseKeepingConfigParamChangeListener;
    }

}
