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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.InstrumentedBean;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Category;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.CollectionType;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Units;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Visibility;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.loadcontrol.local.api.SHMLoadControllerLocalService;

/**
 * Class which Captures the job activities in DDP.
 * 
 * @author zkummad
 * 
 */
@ApplicationScoped
@InstrumentedBean(displayName = "Currently running activities", description = "Shows number of activities which are in running state")
public class ActivityInstrumentationBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityInstrumentationBean.class);

    @Inject
    private SHMLoadControllerLocalService loadControllerLocalService;

    private final Map<String, AtomicLong> counterMap = new ConcurrentHashMap<String, AtomicLong>();

    public void actvityStart(final String platformType, final String jobType, final String name) {
        LOGGER.debug("Inside ActivityInstrumentationBean.activityStart() method with jobType: {}", jobType);
        final String counterName = getCounterName(platformType, jobType, name);
        final AtomicLong counter = getClusterCounter(counterName);
        counter.incrementAndGet();
        final AtomicLong totalCounter = getClusterCounter("totalActivities");
        totalCounter.incrementAndGet();

    }

    public void activityEnd(final String platformType, final String jobType, final String name) {
        LOGGER.debug("Inside ActivityInstrumentationBean.activityEnd() method  with jobType: {}", jobType);
        final AtomicLong counter = getClusterCounter(getCounterName(platformType, jobType, name));
        if (counter.get() == 0) {
            return;
        }
        counter.decrementAndGet();
        final AtomicLong totalCounter = getClusterCounter("totalActivities");
        if (totalCounter.get() == 0) {
            return;
        }
        totalCounter.decrementAndGet();

    }

    private AtomicLong getClusterCounter(final String counterName) {
        AtomicLong counter = counterMap.get(counterName);
        if (counter == null) {
            counter = new AtomicLong();
            counterMap.put(counterName, counter);
        }
        return counter;
    }

    private String getCounterName(final String platformType, final String jobType, final String name) {
        return platformType + jobType + name;
    }

    @MonitoredAttribute(displayName = "Upgrade install activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppUpgradeInstalls() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "UPGRADE", "install").get();
    }

    @MonitoredAttribute(displayName = "Upgrade verify activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppUpgradeVerifies() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "UPGRADE", "verify").get();
    }

    @MonitoredAttribute(displayName = "Upgrade upgrade activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppUpgradeUpgrades() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "UPGRADE", "upgrade").get();
    }

    @MonitoredAttribute(displayName = "Upgrade confirm activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppUpgradeConfirms() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "UPGRADE", "confirm").get();
    }

    @MonitoredAttribute(displayName = "Backup setcvasstartable activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppBackupSetCVAsStartables() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "BACKUP", "setcvasstartable").get();
    }

    @MonitoredAttribute(displayName = "Backup setcvfirstinrollbacklist activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppBackupSetCVFirstInRollbackLists() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "BACKUP", "setcvfirstinrollbacklist").get();
    }

    @MonitoredAttribute(displayName = "Backup exportcv activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppBackupExportCVs() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "BACKUP", "exportcv").get();
    }

    @MonitoredAttribute(displayName = "Backup createcv activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppBackupCreateCVs() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "BACKUP", "createcv").get();
    }

    @MonitoredAttribute(displayName = "License install activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppLicenseInstalls() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "LICENSE", "install").get();
    }

    @MonitoredAttribute(displayName = "DeleteBackup deletecv activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppDeleteBackup() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "DELETEBACKUP", "deletecv").get();
    }

    @MonitoredAttribute(displayName = "Restore download activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppRestoreDownloadCVs() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "RESTORE", "download").get();
    }

    @MonitoredAttribute(displayName = "Restore verify activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppRestoreVerifyCVs() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "RESTORE", "verify").get();
    }

    @MonitoredAttribute(displayName = "Restore install activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppRestoreInstallCVs() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "RESTORE", "install").get();
    }

    @MonitoredAttribute(displayName = "Restore restore activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppRestoreRestores() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "RESTORE", "restore").get();
    }

    @MonitoredAttribute(displayName = "Restore confirm activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppRestoreConfirmCVs() {
        return loadControllerLocalService.getCurrentLoadControllerValue("CPP", "RESTORE", "confirm").get();
    }

    @MonitoredAttribute(displayName = "ECIM Prepare activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimUpgradePrepares() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.UPGRADE.toString(), "prepare").get();
    }

    @MonitoredAttribute(displayName = "ECIM Verify activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimUpgradeVerifys() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.UPGRADE.toString(), "verify").get();
    }

    @MonitoredAttribute(displayName = "ECIM Activate activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimUpgradeActivates() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.UPGRADE.toString(), "activate").get();
    }

    @MonitoredAttribute(displayName = "ECIM Confirm activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimUpgradeConfirms() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.UPGRADE.toString(), "confirm").get();
    }

    @MonitoredAttribute(displayName = "ECIM create Backup activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimBackupCreateBackups() {
        return loadControllerLocalService.getCurrentLoadControllerValue("ECIM", "BACKUP", "createbackup").get();
    }

    @MonitoredAttribute(displayName = "ECIM Upload Backup activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimBackupUploads() {
        return loadControllerLocalService.getCurrentLoadControllerValue("ECIM", "BACKUP", "uploadbackup").get();
    }

    @MonitoredAttribute(displayName = "ECIM Delete Backup activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimDeleteBackups() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.DELETEBACKUP.toString(), "deletebackup").get();
    }

    @MonitoredAttribute(displayName = "ECIM Restore Download Backup activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimRestoreDownloadBackups() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.RESTORE.toString(), "downloadbackup").get();
    }

    @MonitoredAttribute(displayName = "ECIM Restore Restore Backup activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimRestoreRestoreBackups() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.RESTORE.toString(), "restorebackup").get();
    }

    @MonitoredAttribute(displayName = "ECIM Restore Confirm Backup activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimRestoreConfirmBackups() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.RESTORE.toString(), "confirmbackup").get();
    }

    @MonitoredAttribute(displayName = "ECIM NodeHealthCheck activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimNodeHealthCHecks() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.NODE_HEALTH_CHECK.toString(), "nodehealthcheck").get();
    }

    @MonitoredAttribute(displayName = "ECIM install license activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimLicenseInstalls() {
        return loadControllerLocalService.getCurrentLoadControllerValue("ECIM", "LICENSE", "install").get();
    }

    @MonitoredAttribute(displayName = "Running activities total count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppTotalActivities() {
        return getClusterCounter("totalActivities").get();
    }


    /**
     * This method returns the Cluster Counter for Delete Upgrade Package For ECIM Platform
     * 
     */
    @MonitoredAttribute(displayName = JobInstrumentationConstants.DELETE_UPGRADE_PACKAGE_ECIM_ACTIVITIES_COUNT, visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimDeleteUpgradePackages() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(),
                JobInstrumentationConstants.DELETE_UPGRADE_ACTIVITY_NAME).get();
    }

    /**
     * This method returns the Cluster Counter for Delete Upgrade Package For CPP Platform
     */
    @MonitoredAttribute(displayName = JobInstrumentationConstants.DELETE_UPGRADE_PACKAGE_CPP_ACTIVITIES_COUNT, visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppDeleteUpgradePackages() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.CPP.toString(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(),
                JobInstrumentationConstants.DELETE_UPGRADE_ACTIVITY_NAME).get();
    }

    @MonitoredAttribute(displayName = "AXE create Backup activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getAxeBackupCreateBackups() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.AXE.toString(), JobTypeEnum.BACKUP.toString(), "createbackup").get();
    }

    @MonitoredAttribute(displayName = "AXE Upload Backup activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getAxeBackupUploads() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.AXE.toString(), JobTypeEnum.BACKUP.toString(), "uploadbackup").get();
    }

    @MonitoredAttribute(displayName = "AXE Delete Backup activities count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getAxeDeleteBackups() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.AXE.toString(), JobTypeEnum.DELETEBACKUP.toString(), "deletebackup").get();
    }

    @MonitoredAttribute(displayName = JobInstrumentationConstants.AXE_LICENSE_INSTALL_ACTIVITIES_COUNT, visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getAxeLicenseInstalls() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.AXE.toString(), JobTypeEnum.LICENSE.toString(), JobInstrumentationConstants.AXE_LICENSE_ACTIVITY_NAME).get();
    }
    
    @MonitoredAttribute(displayName = "AXE Upgrade running activities total count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getAxeUpgradeTotalActivities() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.AXE.toString(), JobTypeEnum.UPGRADE.toString(), "axeActivity").get();
    }
    
    /* License Refresh - ECIM - Refresh, Request, Install*/
    @MonitoredAttribute(displayName = "ECIM License Refresh Job Refresh activity count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimLicenseRefreshJobRefreshActivities() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.LICENSE_REFRESH.toString(), "refresh").get();
    }

    @MonitoredAttribute(displayName = "ECIM License Refresh Job Request activity count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimLicenseRefreshJobRequestActivities() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.LICENSE_REFRESH.toString(), "request").get();
    }

    @MonitoredAttribute(displayName = "ECIM License Refresh Job Install activity count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimLicenseRefreshJobInstallActivities() {
        return loadControllerLocalService.getCurrentLoadControllerValue(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.LICENSE_REFRESH.toString(), "install").get();
    }

}
