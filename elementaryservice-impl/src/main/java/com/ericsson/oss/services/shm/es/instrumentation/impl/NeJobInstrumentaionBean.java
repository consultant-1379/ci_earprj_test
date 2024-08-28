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
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;

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

/**
 * Class to define the job execution metrics in DDP for Network Element.
 * 
 * @author zkummad
 * 
 */
@ApplicationScoped
@InstrumentedBean(displayName = "Currently running NEJobs", description = "Shows number of NEJobs which are in running state")
public class NeJobInstrumentaionBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeJobInstrumentaionBean.class);

    private static final String NE_JOB = "NeJob";

    private static final String TOTAL_NE_JOBS = "totalNeJobs";

    private final Map<String, AtomicLong> counterMap = new HashMap<String, AtomicLong>();

    public void actvityStart(final String platformType, final String jobType) {

        LOGGER.debug("Inside NeJobInstrumentationBean.activiryStart() method with jobType: {}", jobType);
        final String counterName = getCounterName(platformType, jobType, NE_JOB);
        final AtomicLong counter = getClusterCounter(counterName);
        counter.incrementAndGet();
        final AtomicLong totalCounter = getClusterCounter(TOTAL_NE_JOBS);
        totalCounter.incrementAndGet();
    }

    public void activityEnd(final String platformType, final String jobType) {
        LOGGER.debug("Inside NeJobInstrumentationBean.activiryEnd() method with jobType: {}", jobType);
        final AtomicLong counter = getClusterCounter(getCounterName(platformType, jobType, NE_JOB));
        if (counter.get() == 0) {
            return;
        }
        counter.decrementAndGet();

        final AtomicLong totalCounter = getClusterCounter(TOTAL_NE_JOBS);
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

    @MonitoredAttribute(displayName = "Running Upgrade NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppUpgradeNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.CPP.toString(), JobTypeEnum.UPGRADE.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Running AXE Upgrade NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getAXEUpgradeNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.AXE.toString(), JobTypeEnum.UPGRADE.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Running Backup NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppBackupNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.CPP.toString(), JobTypeEnum.BACKUP.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Running License NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppLicenseNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.CPP.toString(), JobTypeEnum.LICENSE.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Running Restore NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppRestoreNeJobs() {

        return getClusterCounter(getCounterName(PlatformTypeEnum.CPP.toString(), JobTypeEnum.RESTORE.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Running DeleteBackup NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppDeleteBackupNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.CPP.toString(), JobTypeEnum.DELETEBACKUP.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Running ECIM Backup NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimBackupNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.BACKUP.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Running ECIM Upgrade NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimUpgradeNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.UPGRADE.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Running ECIM Delete Backup NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimDeleteBackupNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.DELETEBACKUP.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Running ECIM Restore NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimRestoreNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.RESTORE.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Running ECIM NodeHealthCheck NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimNodeHealthCheckNeJobscount() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.NODE_HEALTH_CHECK.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Active NE Jobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getTotalActiveNeJobs() {
        return getClusterCounter(TOTAL_NE_JOBS).get();
    }

    @MonitoredAttribute(displayName = "Running License NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimLicenseNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.LICENSE.toString(), NE_JOB)).get();
    }

    /**
     * Method to get the Running DeleteUpgrade Package Job Count where the node type is of ECIM.
     * 
     * @return long
     */
    @MonitoredAttribute(displayName = JobInstrumentationConstants.DELETE_UPGRADE_PACKAGE_NE_JOB_COUNT, visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimDeleteUpgradePackageNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), NE_JOB)).get();
    }

    /**
     * Method to get the Running DeleteUpgrade Package Job Count where the node type is of CPP.
     * 
     * @return long
     */
    @MonitoredAttribute(displayName = JobInstrumentationConstants.DELETE_UPGRADE_PACKAGE_NE_JOB_COUNT, visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getCppDeleteUpgradePackageNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.CPP.toString(), JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Running AXE Backup NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getAXEBackupNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.AXE.toString(), JobTypeEnum.BACKUP.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Running AXE DeleteBackup NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getAXEDeleteBackupNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.AXE.toString(), JobTypeEnum.DELETEBACKUP.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = JobInstrumentationConstants.AXE_LICENSE_INSTALL_NEJOB_COUNT, visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getAxeLicenseNeJobs() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.AXE.toString(), JobTypeEnum.LICENSE.toString(), NE_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Running ECIM LicenseRefresh NeJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getEcimLicenseRefreshNeJobscount() {
        return getClusterCounter(getCounterName(PlatformTypeEnum.ECIM.toString(), JobTypeEnum.LICENSE_REFRESH.toString(), NE_JOB)).get();
    }

}
