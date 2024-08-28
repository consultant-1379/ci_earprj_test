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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * Class to define the job execution metrics in DDP for the main job.
 * 
 * @author zkummad
 *
 */
@ApplicationScoped
@InstrumentedBean(displayName = "Currently running MainJobs", description = "Shows number of MainJobs which are in running state")
public class MainJobInstrumentationBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainJobInstrumentationBean.class);

    private static final String MAIN_JOB = "MainJob";

    private static final String TOTAL_MAIN_JOBS = "totalMainJobs";

    private Map<String, AtomicLong> counterMap = new ConcurrentHashMap<String, AtomicLong>();

    /* These methods will be not used any more. Kept for backward compatibility */
    /***** Start *****/
    @Deprecated
    public void actvityStart(final String jobType) {
        LOGGER.debug("Inside MainJobInstrumentationBean.activiryStart() method with jobType: {}", jobType);
        final String counterName = getCounterName(jobType, MAIN_JOB);
        final AtomicLong counter = getClusterCounter(counterName);
        counter.incrementAndGet();
        final AtomicLong totalCounter = getClusterCounter(TOTAL_MAIN_JOBS);
        totalCounter.incrementAndGet();

    }

    @Deprecated
    public void activityEnd(final String jobType) {
        LOGGER.debug("Inside MainJobInstrumentationBean.activiryEnd() method with jobType: {}", jobType);
        final AtomicLong counter = getClusterCounter(getCounterName(jobType, MAIN_JOB));
        if (counter.get() == 0) {
            return;
        }
        counter.decrementAndGet();
        final AtomicLong totalCounter = getClusterCounter(TOTAL_MAIN_JOBS);
        if (totalCounter.get() == 0) {
            return;
        }
        totalCounter.decrementAndGet();

    }

    /***** End *****/

    private AtomicLong getClusterCounter(final String counterName) {
        AtomicLong counter = counterMap.get(counterName);
        if (counter == null) {
            counter = new AtomicLong();
            counterMap.put(counterName, counter);
        }
        return counter;
    }

    public void updateRunningMainJobCount(final int mainJobsCount, final List<String> jobTypes) {
        try {
            counterMap = new HashMap<String, AtomicLong>();
            for (final String jobType : jobTypes) {
                final AtomicLong counter = getClusterCounter(getCounterName(jobType, MAIN_JOB));
                counter.incrementAndGet();
            }
            final AtomicLong totalCounter = getClusterCounter(TOTAL_MAIN_JOBS);
            totalCounter.set(mainJobsCount);
        } catch (final Exception exception) {
            LOGGER.error("Updating running Main job count failed due to {}", exception.getMessage());
        }
    }

    private String getCounterName(final String jobType, final String name) {
        return jobType + name;
    }

    @MonitoredAttribute(displayName = "Upgrade MainJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getUpgradeMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.UPGRADE.toString(), MAIN_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Backup MainJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getBackupMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.BACKUP.toString(), MAIN_JOB)).get();
    }

    @MonitoredAttribute(displayName = "License MainJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getLicenseMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.LICENSE.toString(), MAIN_JOB)).get();
    }

    @MonitoredAttribute(displayName = "Restore MainJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getRestoreMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.RESTORE.toString(), MAIN_JOB)).get();
    }

    @MonitoredAttribute(displayName = "DeleteBackup MainJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getDeleteBackupMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.DELETEBACKUP.toString(), MAIN_JOB)).get();
    }

    @MonitoredAttribute(displayName = "NodeHealthCheck MainJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getNodeHealthCheckMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.NODE_HEALTH_CHECK.toString(), MAIN_JOB)).get();
    }
    
    @MonitoredAttribute(displayName = "MainJobs total count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getTotalMainJobs() {
        return getClusterCounter(TOTAL_MAIN_JOBS).get();
    }

    /**
     * Method to get the Counter for Delete Upgrade Package.
     * 
     * @return long
     */
    @MonitoredAttribute(displayName = JobInstrumentationConstants.DELETE_UPGRADE_PACKAGE_MAIN_JOB_COUNT, visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getDeleteUpgradePackageMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), MAIN_JOB)).get();
    }

    @MonitoredAttribute(displayName = "MainJob types count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getTotalJobTypesInProgress() {

        int count = 0;
        if (getClusterCounter(getCounterName(JobTypeEnum.UPGRADE.toString(), MAIN_JOB)).get() > 0) {
            count++;
        }
        if (getClusterCounter(getCounterName(JobTypeEnum.BACKUP.toString(), MAIN_JOB)).get() > 0) {
            count++;
        }
        if (getClusterCounter(getCounterName(JobTypeEnum.LICENSE.toString(), MAIN_JOB)).get() > 0) {
            count++;
        }
        if (getClusterCounter(getCounterName(JobTypeEnum.RESTORE.toString(), MAIN_JOB)).get() > 0) {
            count++;
        }
        if (getClusterCounter(getCounterName(JobTypeEnum.DELETEBACKUP.toString(), MAIN_JOB)).get() > 0) {
            count++;
        }
        if (getClusterCounter(getCounterName(JobTypeEnum.DELETE_UPGRADEPACKAGE.toString(), MAIN_JOB)).get() > 0) {
            count++;
        }
        if (getClusterCounter(getCounterName(JobTypeEnum.NODE_HEALTH_CHECK.toString(), MAIN_JOB)).get() > 0) {
            count++;
        }
        if (getClusterCounter(getCounterName(JobTypeEnum.LICENSE_REFRESH.toString(), MAIN_JOB)).get() > 0) {
            count++;
        }

        return count;
    }

    @MonitoredAttribute(displayName = "LKF Refresh MainJobs count", visibility = Visibility.ALL, units = Units.NONE, category = Category.THROUGHPUT, interval = 5, collectionType = CollectionType.DYNAMIC)
    public long getLkfRefreshMainJobs() {
        return getClusterCounter(getCounterName(JobTypeEnum.LICENSE_REFRESH.toString(), MAIN_JOB)).get();
    }
}
