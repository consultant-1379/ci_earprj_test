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

import com.ericsson.oss.itpf.sdk.instrument.annotation.InstrumentedBean;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Category;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.CollectionType;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Units;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Visibility;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * Class to define the job execution metrics in DDP for the jobs that are waiting.
 * 
 * @author zkummad
 * 
 */
@SuppressWarnings("PMD")
@ApplicationScoped
@InstrumentedBean(displayName = "Currently Waiting Jobs", description = "Shows number of Jobs which are in waiting state")
public class WaitingJobsInstrumentationBean {

    private final Map<String, AtomicLong> counterMap = new ConcurrentHashMap<String, AtomicLong>();

    private static final String TOTAL_WAITING_MAIN_JOBS = "totalWaitingMainJobs";
    private static final String WAITING_MAINJOB = "WaitingMainJob";
    private static final String WAITING_NEJOB = "WaitingNeJob";
    private static final String TOTAL_WAITING_NE_JOBS = "totalWaitingNeJobs";

    public void waitingJobStart(final String jobType) {
        final String counterName = getCounterName(jobType, WAITING_MAINJOB);
        final AtomicLong counter = getClusterCounter(counterName);
        counter.incrementAndGet();
        final AtomicLong totalCounter = getClusterCounter(TOTAL_WAITING_MAIN_JOBS);
        totalCounter.incrementAndGet();
    }

    public void waitingJobStart(final String jobType, final String neType) {
        final String counterName = getCounterName(neType, jobType, WAITING_NEJOB);
        final AtomicLong counter = getClusterCounter(counterName);
        counter.incrementAndGet();
        final AtomicLong totalCounter = getClusterCounter(TOTAL_WAITING_NE_JOBS);
        totalCounter.incrementAndGet();
    }

    public void waitingJobEnd(final String jobType, final String neType) {
        final AtomicLong counter = getClusterCounter(getCounterName(neType, jobType, WAITING_NEJOB));
        if (counter.get() == 0) {
            return;
        }
        counter.decrementAndGet();
        final AtomicLong totalCounter = getClusterCounter(TOTAL_WAITING_NE_JOBS);
        if (totalCounter.get() == 0) {
            return;
        }
        totalCounter.decrementAndGet();

    }

    public void waitingJobEnd(final String jobType) {
        final AtomicLong counter = getClusterCounter(getCounterName(jobType, WAITING_MAINJOB));
        if (counter.get() == 0) {
            return;
        }
        counter.decrementAndGet();
        final AtomicLong totalCounter = getClusterCounter(TOTAL_WAITING_MAIN_JOBS);
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

    private String getCounterName(final String jobType, final String jobLevel) {
        return jobType + jobLevel;
    }

    private String getCounterName(final String neType, final String jobType, final String jobLevel) {
        return neType + jobType + jobLevel;
    }

    @MonitoredAttribute(displayName = "Waiting BackUp Mainjobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getBackupWaitingMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.BACKUP.toString(), WAITING_MAINJOB)).get();
    }

    @MonitoredAttribute(displayName = "Upgrade Waiting MainJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getUpgradeWaitingMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.UPGRADE.toString(), WAITING_MAINJOB)).get();
    }

    @MonitoredAttribute(displayName = "BSC Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getBscUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.BSC, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "MSC-BC-BSP Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMscBcBspUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MSC_BC_BSP, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "MSC-BC-IS Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMscBcIsUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MSC_BC_IS, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "MSC_DB Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMscDbUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MSC_DB, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "MSC-DB-BSP Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMscDbBspUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MSC_DB_BSP, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "vIP_STP Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getVipStpUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.VIP_STP, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "VMSC Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getVmscUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.VMSC, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "VMSC-HC Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getVmscHcUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.VMSC_HC, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "IP-STP Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getIpStpUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.IP_STP, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "IP-STP-BSP Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getIpStpBspUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.IP_STP_BSP, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "HLR-FE Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getHlrFeUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.HLR_FE, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "HLR-FE-BSP Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getHlrFeBspUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.HLR_FE_BSP, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "HLR-FE-IS Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getHlrFeIsUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.HLR_FE_IS, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "vHLR-FE Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getVhlrFeUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.VHLR_FE, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "License Waiting MainJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getLicenseWaitingMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.LICENSE.toString(), WAITING_MAINJOB)).get();
    }

    @MonitoredAttribute(displayName = "Restore Waiting MainJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getRestoreWaitingMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.RESTORE.toString(), WAITING_MAINJOB)).get();
    }

    @MonitoredAttribute(displayName = "DeleteBackUp Waiting MainJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getDeleteBackupWaitingMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.DELETEBACKUP.toString(), WAITING_MAINJOB)).get();
    }

    @MonitoredAttribute(displayName = "DeleteBackUp Waiting MainJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getDeleteUpgradePackageWaitingMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), WAITING_MAINJOB)).get();
    }

    @MonitoredAttribute(displayName = "Waiting NHC MainJobs total count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getNHCWaitingMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.NODE_HEALTH_CHECK.toString(), WAITING_MAINJOB)).get();
    }

    @MonitoredAttribute(displayName = "Waiting MainJobs total count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getTotalWaitingMainJobs() {
        return getClusterCounter(TOTAL_WAITING_MAIN_JOBS).get();
    }

    @MonitoredAttribute(displayName = "Waiting NEJobs total count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getTotalWaitingNEJobs() {
        return getClusterCounter(TOTAL_WAITING_NE_JOBS).get();
    }

    @MonitoredAttribute(displayName = "Waiting NHC NEJobs total count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getNHCWaitingNEJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.NODE_HEALTH_CHECK.toString(), WAITING_MAINJOB)).get();
    }

    @MonitoredAttribute(displayName = "ERBS Restore Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getErbsRestoreWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.ERBS, JobTypeEnum.RESTORE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "SGSN Restore Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getSgsnRestoreWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.SGSN, JobTypeEnum.RESTORE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "DUSGEN2 Restore Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getDusGen2RestoreWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.DUSG2, JobTypeEnum.RESTORE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "MINI-LINK-Indoor Restore Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMiniLinkIndoorRestoreWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MINILINKINDOOR, JobTypeEnum.RESTORE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "MINI-LINK-CN510R2 Restore Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMiniLinkIndoorcn510r2RestoreWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MINILINKINDOOR_CN510R2, JobTypeEnum.RESTORE.toString(), WAITING_NEJOB)).get();
    }

	@MonitoredAttribute(displayName = "MINI-LINK-CN210 Restore Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMiniLinkIndoorcn210RestoreWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MINILINKINDOOR_CN210, JobTypeEnum.RESTORE.toString(), WAITING_NEJOB)).get();
    }

	@MonitoredAttribute(displayName = "MINI-LINK-CN510R1 Restore Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMiniLinkIndoorcn510r1RestoreWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MINILINKINDOOR_CN510R1, JobTypeEnum.RESTORE.toString(), WAITING_NEJOB)).get();
    }

	@MonitoredAttribute(displayName = "MINI-LINK-CN810R1 Restore Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMiniLinkIndoorcn810r1RestoreWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MINILINKINDOOR_CN810R1, JobTypeEnum.RESTORE.toString(), WAITING_NEJOB)).get();
    }

	@MonitoredAttribute(displayName = "MINI-LINK-CN810R2 Restore Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMiniLinkIndoorcn810r2RestoreWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MINILINKINDOOR_CN810R2, JobTypeEnum.RESTORE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "ERBS Backup Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getErbsBackupWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.ERBS, JobTypeEnum.BACKUP.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "SGSN BackUp Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getSgsnBackupWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.SGSN, JobTypeEnum.BACKUP.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "DUSGEN2 Backup Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getDusGen2BackupWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.DUSG2, JobTypeEnum.BACKUP.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "DUSGEN2 Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getDusGen2UpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.DUSG2, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "SGSN Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getSgsnUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.SGSN, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "ERBS Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getErbsUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.ERBS, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "MINI-LINK-Indoor Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMiniLinkIndoorUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MINILINKINDOOR, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "MINI-LINK-CN510R2 Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMiniLinkIndoorcn510r2UpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MINILINKINDOOR_CN510R2, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "ERBS License Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getErbsLicenseWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.ERBS, JobTypeEnum.LICENSE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "MINI-LINK-Indoor License Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMiniLinkIndoorLicenseWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MINILINKINDOOR, JobTypeEnum.LICENSE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "DUSGEN2 License Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getDusGen2LicenseWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.DUSG2, JobTypeEnum.LICENSE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "DUSGEN2 DeleteBackup Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getDusGen2DeleteBackupWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.DUSG2, JobTypeEnum.DELETEBACKUP.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "ERBS DeleteBackup Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getErbsDeleteBackupWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.ERBS, JobTypeEnum.DELETEBACKUP.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "SGSN DeleteBackup Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getSgsnDeleteBackupWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.SGSN, JobTypeEnum.DELETEBACKUP.toString(), WAITING_NEJOB)).get();
    }

    /**
     * Method to get the ERBS Delete Upgrade Package NeJobs count which are waiting.
     * 
     * @return long
     */
    @MonitoredAttribute(displayName = JobInstrumentationConstants.DELETE_UPGRADE_PACKAGE_ERBS_JOB_COUNT, visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getErbsDeleteUpgradePackageWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.ERBS, JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), WAITING_NEJOB)).get();
    }

    /**
     * Method to get the RadioNode Delete Upgrade Package NeJobs count which are waiting.
     * 
     * @return long
     */
    @MonitoredAttribute(displayName = JobInstrumentationConstants.DELETE_UPGRADE_PACKAGE_DUSGEN2_JOB_COUNT, visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getDusGen2DeleteUpgradePackageWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.DUSG2, JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), WAITING_NEJOB)).get();
    }

    /**
     * Method to get the SGSN Delete Upgrade Package NeJobs count which are waiting.
     * 
     * @return long
     */
    @MonitoredAttribute(displayName = JobInstrumentationConstants.DELETE_UPGRADE_PACKAGE_SGSN_JOB_COUNT, visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getSgsnDeleteUpgradePackageWaitingNeJob() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.SGSN, JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "MINI-LINK-669x Restore Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMiniLink669xRestoreWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MINILINK669X, JobTypeEnum.RESTORE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "MINI-LINK-669x Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMiniLink669xUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MINILINK669X, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "MINI-LINK-665x Restore Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMiniLink665xRestoreWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MINILINK665X, JobTypeEnum.RESTORE.toString(), WAITING_NEJOB)).get();
    }

    @MonitoredAttribute(displayName = "MINI-LINK-665x Upgrade Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getMiniLink665xUpgradeWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.MINILINK665X, JobTypeEnum.UPGRADE.toString(), WAITING_NEJOB)).get();
    }

    /* License Refresh Job - Main Job */
    @MonitoredAttribute(displayName = "Waiting License Refresh MainJobs total count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getLicenseRefreshWaitingMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.LICENSE_REFRESH.toString(), WAITING_MAINJOB)).get();
    }

    /* License Refresh Job - NE jobs */
    @MonitoredAttribute(displayName = "Waiting License Refresh NEJobs total count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getLicenseRefreshWaitingNEJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.LICENSE_REFRESH.toString(), WAITING_NEJOB)).get();
    }

    /* License Refresh Job for RadioNode - NE jobs */
    @MonitoredAttribute(displayName = "DUSGEN2 License Refresh Waiting NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getDusGen2LicenseRefreshWaitingNeJobs() {
        return getClusterCounter(getCounterName(JobInstrumentationConstants.DUSG2, JobTypeEnum.LICENSE_REFRESH.toString(), WAITING_NEJOB)).get();
    }

}
