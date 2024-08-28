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

package com.ericsson.oss.services.shm.job.housekeeping;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.services.shm.job.activity.JobType;

/**
 * This class is used to listen and update the count of jobs after which housekeeping should be triggered for that job type
 * 
 * @author xsrakon
 * 
 */
@ApplicationScoped
public class HouseKeepingConfigParamChangeListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    @Configured(propertyName = "BACKUP_JOB_COUNT_FOR_JOB_HOUSEKEEPING")
    private int backupJobCountForHouseKeeping;

    @Inject
    @Configured(propertyName = "BACKUP_JOB_AGE_FOR_JOB_HOUSEKEEPING")
    private int backupJobMinAgeForHouseKeeping;

    @Inject
    @Configured(propertyName = "UPGRADE_JOB_COUNT_FOR_JOB_HOUSEKEEPING")
    private int upgradeJobCountForHouseKeeping;

    @Inject
    @Configured(propertyName = "UPGRADE_JOB_AGE_FOR_JOB_HOUSEKEEPING")
    private int upgradeJobMinAgeForHouseKeeping;

    @Inject
    @Configured(propertyName = "DELETE_UPGRADEPACKAGE_JOB_COUNT_FOR_JOB_HOUSEKEEPING")
    private int deleteUpgradepackageJobCountForHouseKeeping;

    @Inject
    @Configured(propertyName = "DELETE_UPGRADEPACKAGE_JOB_AGE_FOR_JOB_HOUSEKEEPING")
    private int delteUpgradepackageJobMinAgeForHouseKeeping;

    @Inject
    @Configured(propertyName = "LICENSE_JOB_COUNT_FOR_JOB_HOUSEKEEPING")
    private int licenseJobCountForHouseKeeping;

    @Inject
    @Configured(propertyName = "LICENSE_JOB_AGE_FOR_JOB_HOUSEKEEPING")
    private int licenseJobMinAgeForHouseKeeping;

    @Inject
    @Configured(propertyName = "RESTORE_JOB_COUNT_FOR_JOB_HOUSEKEEPING")
    private int restoreJobCountForHouseKeeping;

    @Inject
    @Configured(propertyName = "RESTORE_JOB_AGE_FOR_JOB_HOUSEKEEPING")
    private int restoreJobMinAgeForHouseKeeping;

    @Inject
    @Configured(propertyName = "DELETE_BACKUP_JOB_COUNT_FOR_JOB_HOUSEKEEPING")
    private int deleteBackupJobCountForHouseKeeping;

    @Inject
    @Configured(propertyName = "DELETE_BACKUP_JOB_AGE_FOR_JOB_HOUSEKEEPING")
    private int deleteBackupJobMinAgeForHouseKeeping;

    @Inject
    @Configured(propertyName = "BACKUP_HOUSEKEEPING_JOB_COUNT_FOR_JOB_HOUSEKEEPING")
    private int backupHousekeepingJobCountForHouseKeeping;

    @Inject
    @Configured(propertyName = "BACKUP_HOUSEKEEPING_JOB_AGE_FOR_JOB_HOUSEKEEPING")
    private int backupHousekeepingJobMinAgeForHouseKeeping;

    @Inject
    @Configured(propertyName = "nodeHealthCheckReportCountForHousekeeping")
    private int nodeHealthCheckJobCountForHouseKeeping;

    @Inject
    @Configured(propertyName = "nodeHealthCheckReportAgeForHousekeeping")
    private int nodeHealthCheckJobMinAgeForHouseKeeping;

    @Inject
    @Configured(propertyName = "DEFAULT_JOB_COUNT_FOR_SHM_JOB_HOUSEKEEPING")
    private int defaultCountForShmJobHouseKeeping;

    @Inject
    @Configured(propertyName = "DEFAULT_JOB_AGE_FOR_SHM_JOB_HOUSEKEEPING")
    private int defaultMinAgeForShmJobHouseKeeping;

    @Inject
    @Configured(propertyName = "NODERESTART_JOB_COUNT_FOR_JOB_HOUSEKEEPING")
    private int nodeRestartJobCountForHouseKeeping;

    @Inject
    @Configured(propertyName = "NODERESTART_JOB_AGE_FOR_JOB_HOUSEKEEPING")
    private int nodeRestartJobMinAgeForHouseKeeping;

    @Inject
    @Configured(propertyName = "LICENSE_REFRESH_JOB_AGE_FOR_JOB_HOUSEKEEPING")
    private int licenseRefreshJobMinAgeForHouseKeeping;

    @Inject
    @Configured(propertyName = "LICENSE_REFRESH_JOB_COUNT_FOR_JOB_HOUSEKEEPING")
    private int licenseRefreshJobCountForHouseKeeping;

    /**
     * Listener for BACKUP_JOB_COUNT_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param backupJobCountForHouseKeeping
     */
    void listenCountOfBackupJobForHouseKeepingAttribute(@Observes @ConfigurationChangeNotification(propertyName = "BACKUP_JOB_COUNT_FOR_JOB_HOUSEKEEPING") final int backupJobCountForHouseKeeping) {
        this.backupJobCountForHouseKeeping = backupJobCountForHouseKeeping;
        logger.info("Backup Job Count For HouseKeeping {}", backupJobCountForHouseKeeping);
    }

    public int getBackupJobCountForHouseKeeping() {
        return backupJobCountForHouseKeeping;
    }

    /**
     * Listener for BACKUP_JOB_AGE_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param backupJobMinAgeForHouseKeeping
     */
    void listenMinimumAgeOfBackupJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "BACKUP_JOB_AGE_FOR_JOB_HOUSEKEEPING") final int backupJobMinAgeForHouseKeeping) {
        this.backupJobMinAgeForHouseKeeping = backupJobMinAgeForHouseKeeping;
        logger.info("Backup Job Minimum Age For HouseKeeping {}", backupJobMinAgeForHouseKeeping);
    }

    public int getBackupJobMinimumAgeForHouseKeeping() {
        return backupJobMinAgeForHouseKeeping;
    }

    /**
     * Listener for UPGRADE_JOB_COUNT_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param upgradeJobCountForHouseKeeping
     */
    void listenCountOfUpgradeJobForHouseKeepingAttribute(@Observes @ConfigurationChangeNotification(propertyName = "UPGRADE_JOB_COUNT_FOR_JOB_HOUSEKEEPING") final int upgradeJobCountForHouseKeeping) {
        this.upgradeJobCountForHouseKeeping = upgradeJobCountForHouseKeeping;
        logger.info("Upgrade Job Count For HouseKeeping {}", upgradeJobCountForHouseKeeping);
    }

    public int getUpgradeJobCountForHouseKeeping() {
        return upgradeJobCountForHouseKeeping;
    }

    /**
     * Listener for UPGRADE_JOB_AGE_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param upgradeJobMinAgeForHouseKeeping
     */
    void listenMinimumAgeOfUpgradeJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "UPGRADE_JOB_AGE_FOR_JOB_HOUSEKEEPING") final int upgradeJobMinAgeForHouseKeeping) {
        this.upgradeJobMinAgeForHouseKeeping = upgradeJobMinAgeForHouseKeeping;
        logger.info("Upgrade Job Minimum Age For HouseKeeping {}", upgradeJobMinAgeForHouseKeeping);
    }

    public int getUpgradeJobMinimumAgeForHouseKeeping() {
        return upgradeJobMinAgeForHouseKeeping;
    }

    /**
     * Listener for DELETE_UPGRADEPACKAGE_JOB_COUNT_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param deleteUpgradepackageJobCountForHouseKeeping
     */
    void listenCountOfDeleteUpgradepackageJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "DELETE_UPGRADEPACKAGE_JOB_COUNT_FOR_JOB_HOUSEKEEPING") final int deleteUpgradepackageJobCountForHouseKeeping) {
        this.deleteUpgradepackageJobCountForHouseKeeping = deleteUpgradepackageJobCountForHouseKeeping;
        logger.info("Delete Upgradepackage Job Count For HouseKeeping {}", deleteUpgradepackageJobCountForHouseKeeping);
    }

    public int getDeleteUpgradepackageJobCountForHouseKeeping() {
        return deleteUpgradepackageJobCountForHouseKeeping;
    }

    /**
     * Listener for DELETE_UPGRADEPACKAGE_JOB_AGE_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param deleteUpgradepackageJobMinAgeForHouseKeeping
     */
    void listenMinimumAgeOfDeleteUpgradepackageJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "DELETE_UPGRADEPACKAGE_JOB_AGE_FOR_JOB_HOUSEKEEPING") final int deleteUpgradepackageJobMinAgeForHouseKeeping) {
        this.delteUpgradepackageJobMinAgeForHouseKeeping = deleteUpgradepackageJobMinAgeForHouseKeeping;
        logger.info("Delete Upgradepackage Job Minimum Age For HouseKeeping {}", deleteUpgradepackageJobMinAgeForHouseKeeping);
    }

    public int getDeleteUpgradepackageJobMinimumAgeForHouseKeeping() {
        return delteUpgradepackageJobMinAgeForHouseKeeping;
    }

    /**
     * Listener for RESTORE_JOB_COUNT_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param restoreJobCountForHouseKeeping
     */
    void listenCountOfRestoreJobForHouseKeepingAttribute(@Observes @ConfigurationChangeNotification(propertyName = "RESTORE_JOB_COUNT_FOR_JOB_HOUSEKEEPING") final int restoreJobCountForHouseKeeping) {
        this.restoreJobCountForHouseKeeping = restoreJobCountForHouseKeeping;
        logger.info("Restore Job Count For HouseKeeping {}", restoreJobCountForHouseKeeping);
    }

    public int getRestoreJobCountForHouseKeeping() {
        return restoreJobCountForHouseKeeping;
    }

    /**
     * Listener for RESTORE_JOB_AGE_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param restoreJobMinAgeForHouseKeeping
     */
    void listenMinimumAgeOfRestoreJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "RESTORE_JOB_AGE_FOR_JOB_HOUSEKEEPING") final int restoreJobMinAgeForHouseKeeping) {
        this.restoreJobMinAgeForHouseKeeping = restoreJobMinAgeForHouseKeeping;
        logger.info("Restore Job Minimum Age For HouseKeeping {}", restoreJobMinAgeForHouseKeeping);
    }

    public int getRestoreJobMinimumAgeForHouseKeeping() {
        return restoreJobMinAgeForHouseKeeping;
    }

    /**
     * Listener for DELETE_BACKUP_JOB_COUNT_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param deleteBackupJobCountForHouseKeeping
     */
    void listenCountOfDeleteBackupJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "DELETE_BACKUP_JOB_COUNT_FOR_JOB_HOUSEKEEPING") final int deleteBackupJobCountForHouseKeeping) {
        this.deleteBackupJobCountForHouseKeeping = deleteBackupJobCountForHouseKeeping;
        logger.info("Delete Backup Job Count For HouseKeeping {}", deleteBackupJobCountForHouseKeeping);
    }

    public int getDeleteBackupJobCountForHouseKeeping() {
        return deleteBackupJobCountForHouseKeeping;
    }

    /**
     * Listener for DELETE_BACKUP_JOB_AGE_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param deleteBackupJobMinAgeForHouseKeeping
     */
    void listenMinimumAgeOfDeleteBackupJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "DELETE_BACKUP_JOB_AGE_FOR_JOB_HOUSEKEEPING") final int deleteBackupJobMinAgeForHouseKeeping) {
        this.deleteBackupJobMinAgeForHouseKeeping = deleteBackupJobMinAgeForHouseKeeping;
        logger.info("Delete Backup Job Age For HouseKeeping {}", deleteBackupJobMinAgeForHouseKeeping);
    }

    public int getDeleteBackupJobMinimumAgeForHouseKeeping() {
        return deleteBackupJobMinAgeForHouseKeeping;
    }

    /**
     * Listener for LICENSE_JOB_COUNT_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param licenseJobCountForHouseKeeping
     */
    void listenCountOfLicenseJobForHouseKeepingAttribute(@Observes @ConfigurationChangeNotification(propertyName = "LICENSE_JOB_COUNT_FOR_JOB_HOUSEKEEPING") final int licenseJobCountForHouseKeeping) {
        this.licenseJobCountForHouseKeeping = licenseJobCountForHouseKeeping;
        logger.info("License Job Count For HouseKeeping {}", licenseJobCountForHouseKeeping);
    }

    public int getLicenseJobCountForHouseKeeping() {
        return licenseJobCountForHouseKeeping;
    }

    /**
     * Listener for LICENSE_JOB_AGE_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param licenseJobMinAgeForHouseKeeping
     */
    void listenMinimumAgeOfLicenseJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "LICENSE_JOB_AGE_FOR_JOB_HOUSEKEEPING") final int licenseJobMinAgeForHouseKeeping) {
        this.licenseJobMinAgeForHouseKeeping = licenseJobMinAgeForHouseKeeping;
        logger.info("License Job Minimum Age For HouseKeeping {}", licenseJobMinAgeForHouseKeeping);
    }

    public int getLicenseJobMinimumAgeForHouseKeeping() {
        return licenseJobMinAgeForHouseKeeping;
    }

    /**
     * Listener for BACKUP_HOUSEKEEPING_JOB_COUNT_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param backupHousekeepingJobCountForHouseKeeping
     */
    void listenCountOfBackupHousekeepingJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "BACKUP_HOUSEKEEPING_JOB_COUNT_FOR_JOB_HOUSEKEEPING") final int backupHousekeepingJobCountForHouseKeeping) {
        this.backupHousekeepingJobCountForHouseKeeping = backupHousekeepingJobCountForHouseKeeping;
        logger.info("Backup Housekeeping Job Count For HouseKeeping {}", backupHousekeepingJobCountForHouseKeeping);
    }

    public int getBackupHousekeepingJobCountForHouseKeeping() {
        return backupHousekeepingJobCountForHouseKeeping;
    }

    /**
     * Listener for BACKUP_HOUSEKEEPING_JOB_AGE_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param backupHousekeepingJobMinAgeForHouseKeeping
     */
    void listenMinimumAgeOfBackupHousekeepingJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "BACKUP_HOUSEKEEPING_JOB_AGE_FOR_JOB_HOUSEKEEPING") final int backupHousekeepingJobMinAgeForHouseKeeping) {
        this.backupHousekeepingJobMinAgeForHouseKeeping = backupHousekeepingJobMinAgeForHouseKeeping;
        logger.info("Backup Housekeeping  Job Minimum Age For HouseKeeping {}", backupHousekeepingJobMinAgeForHouseKeeping);
    }

    public int getBackupHousekeepingJobMinimumAgeForHouseKeeping() {
        return backupHousekeepingJobMinAgeForHouseKeeping;
    }

    /**
     * Listener for nodeHealthCheckReportCountForHousekeeping attribute value
     * 
     * @param nodeHealthCheckJobCountForHouseKeeping
     */
    void listenCountOfNodeHealthCheckReportForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "nodeHealthCheckReportCountForHousekeeping") final int nodeHealthCheckJobCountForHouseKeeping) {
        this.nodeHealthCheckJobCountForHouseKeeping = nodeHealthCheckJobCountForHouseKeeping;
        logger.info("Node Health Check Report Count For HouseKeeping {}", nodeHealthCheckJobCountForHouseKeeping);
    }

    public int getNodeHealthCheckReportCountForHouseKeeping() {
        return nodeHealthCheckJobCountForHouseKeeping;
    }

    /**
     * Listener for nodeHealthCheckReportAgeForHousekeeping attribute value
     * 
     * @param nodeHealthCheckJobMinAgeForHouseKeeping
     */
    void listenMinimumAgeOfNodeHealthCheckReportForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "nodeHealthCheckReportAgeForHousekeeping") final int nodeHealthCheckJobMinAgeForHouseKeeping) {
        this.nodeHealthCheckJobMinAgeForHouseKeeping = nodeHealthCheckJobMinAgeForHouseKeeping;
        logger.info("Node Health Check Report Minimum Age For HouseKeeping {}", nodeHealthCheckJobMinAgeForHouseKeeping);
    }

    public int getNodeHealthCheckReportMinimumAgeForHouseKeeping() {
        return nodeHealthCheckJobMinAgeForHouseKeeping;
    }

    /**
     * Listener for DEFAULT_JOB_COUNT_FOR_SHM_JOB_HOUSEKEEPING config param is applicable if no other specific sdk configuration parameter is defined for the count criteria for the job type on which
     * jobs housekeeping is invoked
     * 
     * @param defaultCountForShmJobHouseKeeping
     */
    void listenCountOfShmJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "DEFAULT_JOB_COUNT_FOR_SHM_JOB_HOUSEKEEPING") final int defaultCountForShmJobHouseKeeping) {
        this.defaultCountForShmJobHouseKeeping = defaultCountForShmJobHouseKeeping;
        logger.info("Default Shm Job Count For HouseKeeping {}", defaultCountForShmJobHouseKeeping);
    }

    public int getDefaultCountForShmJobHouseKeeping() {
        return defaultCountForShmJobHouseKeeping;
    }

    /**
     * Listener for DEFAULT_JOB_AGE_FOR_SHM_JOB_HOUSEKEEPING config param is applicable if no other specific sdk configuration parameter is defined for the age(in days) criteria for the job type on
     * which jobs housekeeping is invoked
     * 
     * @param defaultMinAgeForShmJobHouseKeeping
     */
    void listenMinimumAgeOfShmJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "DEFAULT_JOB_AGE_FOR_SHM_JOB_HOUSEKEEPING") final int defaultMinAgeForShmJobHouseKeeping) {
        this.defaultMinAgeForShmJobHouseKeeping = defaultMinAgeForShmJobHouseKeeping;
        logger.info("Default Shm Job Age For HouseKeeping {}", defaultMinAgeForShmJobHouseKeeping);
    }

    public int getDefaultMinimumAgeForShmJobHouseKeeping() {
        return defaultMinAgeForShmJobHouseKeeping;
    }

    /**
     * Listener for NODERESTART_JOB_COUNT_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param nodeRestartJobCountForHouseKeeping
     */
    void listenCountOfNodeRestartJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "NODERESTART_JOB_COUNT_FOR_JOB_HOUSEKEEPING") final int nodeRestartJobCountForHouseKeeping) {
        this.nodeRestartJobCountForHouseKeeping = nodeRestartJobCountForHouseKeeping;
        logger.info("NodeRestart Job Count For HouseKeeping {}", nodeRestartJobCountForHouseKeeping);
    }

    public int getNodeRestartJobCountForHouseKeeping() {
        return nodeRestartJobCountForHouseKeeping;
    }

    /**
     * Listener for NODERESTART_JOB_AGE_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param nodeRestartJobMinAgeForHouseKeeping
     */
    void listenMinimumAgeOfNoderRestartJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "NODERESTART_JOB_AGE_FOR_JOB_HOUSEKEEPING") final int nodeRestartJobMinAgeForHouseKeeping) {
        this.nodeRestartJobMinAgeForHouseKeeping = nodeRestartJobMinAgeForHouseKeeping;
        logger.info("NodeRestart Job Minimum Age For HouseKeeping {}", nodeRestartJobMinAgeForHouseKeeping);
    }

    public int getNodeRestartJobMinimumAgeForHouseKeeping() {
        return nodeRestartJobMinAgeForHouseKeeping;
    }

    /**
     * Listener for LICENSE_REFRESH_JOB_COUNT_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param licenseRefreshJobCountForHouseKeeping
     */
    void listenCountOfLicenseRefreshJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "LICENSE_REFRESH_JOB_COUNT_FOR_JOB_HOUSEKEEPING") final int licenseRefreshJobCountForHouseKeeping) {
        this.licenseRefreshJobCountForHouseKeeping = licenseRefreshJobCountForHouseKeeping;
        logger.info("License Refresh Job Count For HouseKeeping {}", licenseRefreshJobCountForHouseKeeping);
    }

    public int getLicenseRefreshJobCountForHouseKeeping() {
        return licenseRefreshJobCountForHouseKeeping;
    }

    /**
     * Listener for LICENSE_REFRESH_JOB_AGE_FOR_JOB_HOUSEKEEPING attribute value
     * 
     * @param licenseRefreshJobMinAgeForHouseKeeping
     */
    void listenMinimumAgeOfLicenseRefreshJobForHouseKeepingAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "LICENSE_REFRESH_JOB_AGE_FOR_JOB_HOUSEKEEPING") final int licenseRefreshJobMinAgeForHouseKeeping) {
        this.licenseRefreshJobMinAgeForHouseKeeping = licenseRefreshJobMinAgeForHouseKeeping;
        logger.info("License Refresh Job Minimum Age For HouseKeeping {}", licenseRefreshJobMinAgeForHouseKeeping);
    }

    public int getLicenseRefreshJobMinAgeForHouseKeeping() {
        return licenseRefreshJobMinAgeForHouseKeeping;
    }

    public int getJobCountForHouseKeeping(final JobType jobType) {
        int jobCount = 0;
        if (JobType.BACKUP.toString().equals(jobType.toString())) {
            jobCount = getBackupJobCountForHouseKeeping();
        } else if (JobType.UPGRADE.toString().equals(jobType.toString())) {
            jobCount = getUpgradeJobCountForHouseKeeping();
        } else if (JobType.DELETE_UPGRADEPACKAGE.toString().equals(jobType.toString())) {
            jobCount = getDeleteUpgradepackageJobCountForHouseKeeping();
        } else if (JobType.RESTORE.toString().equals(jobType.toString())) {
            jobCount = getRestoreJobCountForHouseKeeping();
        } else if (JobType.DELETEBACKUP.toString().equals(jobType.toString())) {
            jobCount = getDeleteBackupJobCountForHouseKeeping();
        } else if (JobType.LICENSE.toString().equals(jobType.toString())) {
            jobCount = getLicenseJobCountForHouseKeeping();
        } else if (JobType.BACKUP_HOUSEKEEPING.toString().equals(jobType.toString())) {
            jobCount = getBackupHousekeepingJobCountForHouseKeeping();
        } else if (JobType.NODE_HEALTH_CHECK.toString().equals(jobType.toString())) {
            jobCount = getNodeHealthCheckReportCountForHouseKeeping();
        } else if (JobType.NODERESTART.toString().equals(jobType.toString())) {
            jobCount = getNodeRestartJobCountForHouseKeeping();
        } else if (JobType.LICENSE_REFRESH.toString().equals(jobType.toString())) {
            jobCount = getLicenseRefreshJobCountForHouseKeeping();
        } else {
            jobCount = getDefaultCountForShmJobHouseKeeping();
        }
        return jobCount;
    }

    public int getMinJobAgeForHouseKeeping(final JobType jobType) {
        int minJobAge = 0;
        if (JobType.BACKUP.toString().equals(jobType.toString())) {
            minJobAge = getBackupJobMinimumAgeForHouseKeeping();
        } else if (JobType.UPGRADE.toString().equals(jobType.toString())) {
            minJobAge = getUpgradeJobMinimumAgeForHouseKeeping();
        } else if (JobType.DELETE_UPGRADEPACKAGE.toString().equals(jobType.toString())) {
            minJobAge = getDeleteUpgradepackageJobMinimumAgeForHouseKeeping();
        } else if (JobType.RESTORE.toString().equals(jobType.toString())) {
            minJobAge = getRestoreJobMinimumAgeForHouseKeeping();
        } else if (JobType.DELETEBACKUP.toString().equals(jobType.toString())) {
            minJobAge = getDeleteBackupJobMinimumAgeForHouseKeeping();
        } else if (JobType.LICENSE.toString().equals(jobType.toString())) {
            minJobAge = getLicenseJobMinimumAgeForHouseKeeping();
        } else if (JobType.BACKUP_HOUSEKEEPING.toString().equals(jobType.toString())) {
            minJobAge = getBackupHousekeepingJobMinimumAgeForHouseKeeping();
        } else if (JobType.NODE_HEALTH_CHECK.toString().equals(jobType.toString())) {
            minJobAge = getNodeHealthCheckReportMinimumAgeForHouseKeeping();
        } else if (JobType.NODERESTART.toString().equals(jobType.toString())) {
            minJobAge = getNodeRestartJobMinimumAgeForHouseKeeping();
        } else if (JobType.LICENSE_REFRESH.toString().equals(jobType.toString())) {
            minJobAge = getLicenseRefreshJobMinAgeForHouseKeeping();
        } else {
            minJobAge = getDefaultMinimumAgeForShmJobHouseKeeping();
        }
        return minJobAge;
    }

}
