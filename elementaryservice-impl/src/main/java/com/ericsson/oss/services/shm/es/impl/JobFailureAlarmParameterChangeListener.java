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
package com.ericsson.oss.services.shm.es.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

/**
 * Common for DeleteBackup / Upgrade / Backup / License / Restore listeners(observers) for listening the respective Configuration parameter change in the Models
 * 
 */
@ApplicationScoped
public class JobFailureAlarmParameterChangeListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    @Configured(propertyName = "SEND_ALARM_ON_BACKUP_JOB_FAILURE")
    private boolean alarmNeededOnBackupJobFailure;

    @Inject
    @Configured(propertyName = "SEND_ALARM_ON_UPGRADE_JOB_FAILURE")
    private boolean alarmNeededOnUpgradeJobFailure;

    @Inject
    @Configured(propertyName = "SEND_ALARM_ON_DELETE_UPGRADEPACKAGE_JOB_FAILURE")
    private boolean alarmNeededOnDeleteUpgradepackageJobFailure;

    @Inject
    @Configured(propertyName = "SEND_ALARM_ON_LICENSE_JOB_FAILURE")
    private boolean alarmNeededOnLicenseJobFailure;

    @Inject
    @Configured(propertyName = "SEND_ALARM_ON_RESTORE_JOB_FAILURE")
    private boolean alarmNeededOnRestoreJobFailure;

    @Inject
    @Configured(propertyName = "SEND_ALARM_ON_DELETEBACKUP_JOB_FAILURE")
    private boolean alarmNeededOnDeleteBackupJobFailure;

    @Inject
    @Configured(propertyName = "SEND_ALARM_ON_BACKUP_HOUSEKEEPING_JOB_FAILURE")
    private boolean alarmNeededOnBackupHousekeepingJobFailure;

    @Inject
    @Configured(propertyName = "SEND_ALARM_ON_DELETE_SOFTWARE_PACKAGE_JOB_FAILURE")
    private boolean alarmNeededOnDeleteSoftwarePackageJobFailure;

    @Inject
    @Configured(propertyName = "SEND_ALARM_ON_ONBOARD_JOB_FAILURE")
    private boolean alarmNeededOnOnboardJobFailure;

    @Inject
    @Configured(propertyName = "SEND_ALARM_ON_SHM_JOB_FAILURE")
    private boolean isAlarmNeededOnShmJobFailure;

    @Inject
    @Configured(propertyName = "SEND_ALARM_ON_LICENSE_REFRESH_JOB_FAILURE")
    private boolean isAlarmNeededOnLicenseRefreshJobFailure;


    /**
     * Listener for Backup JobFailure Alarm attribute value
     * 
     * @param backupJobFailureAlarm
     */
    void listenForBackupJobFailureAlarmAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SEND_ALARM_ON_BACKUP_JOB_FAILURE") final boolean alarmNeededOnBackupJobFailure) {
        this.alarmNeededOnBackupJobFailure = alarmNeededOnBackupJobFailure;
        logger.info("Backup JobFailure Alarm Parameter value : {}", alarmNeededOnBackupJobFailure);
    }

    public boolean isAlarmNeededOnBackupJobFailure() {
        return alarmNeededOnBackupJobFailure;
    }

    /**
     * Listener for Upgrade JobFailure Alarm attribute value
     * 
     * @param upgradeJobFailureAlarm
     */
    void listenForUpgradeJobFailureAlarmAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SEND_ALARM_ON_UPGRADE_JOB_FAILURE") final boolean alarmNeededOnUpgradeJobFailure) {
        this.alarmNeededOnUpgradeJobFailure = alarmNeededOnUpgradeJobFailure;
        logger.info("Upgrade JobFailure Alarm Parameter value : {}", alarmNeededOnUpgradeJobFailure);
    }

    public boolean isAlarmNeededOnUpgradeJobFailure() {
        return alarmNeededOnUpgradeJobFailure;
    }

    /**
     * Listener for Delete Upgradepackage JobFailure Alarm attribute value
     * 
     * @param DeleteupgradepackageJobFailureAlarm
     */
    void listenForDeleteUpgradepackageJobFailureAlarmAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SEND_ALARM_ON_DELETE_UPGRADEPACKAGE_JOB_FAILURE") final boolean alarmNeededOnDeleteUpgradepackageJobFailure) {
        this.alarmNeededOnDeleteUpgradepackageJobFailure = alarmNeededOnDeleteUpgradepackageJobFailure;
        logger.info("Delete Upgradepackage JobFailure Alarm Parameter value : {}", alarmNeededOnDeleteUpgradepackageJobFailure);
    }

    public boolean isAlarmNeededOnDeleteUpgradepackageJobFailure() {
        return alarmNeededOnDeleteUpgradepackageJobFailure;
    }

    /**
     * Listener for License JobFailure Alarm attribute value
     * 
     * @param licenseJobFailureAlarm
     */
    void listenForLicenseJobFailureAlarmAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SEND_ALARM_ON_LICENSE_JOB_FAILURE") final boolean alarmNeededOnLicenseJobFailure) {
        this.alarmNeededOnLicenseJobFailure = alarmNeededOnLicenseJobFailure;
        logger.info("License JobFailure Alarm Parameter value : {}", alarmNeededOnLicenseJobFailure);
    }

    public boolean isAlarmNeededOnLicenseJobFailure() {
        return alarmNeededOnLicenseJobFailure;
    }

    /**
     * Listener for Restore JobFailure Alarm attribute value
     * 
     * @param restoreJobFailureAlarm
     */
    void listenForRestoreJobFailureAlarmAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SEND_ALARM_ON_RESTORE_JOB_FAILURE") final boolean alarmNeededOnRestoreJobFailure) {
        this.alarmNeededOnRestoreJobFailure = alarmNeededOnRestoreJobFailure;
        logger.info("Restore JobFailure Alarm Parameter value : {}", alarmNeededOnRestoreJobFailure);
    }

    public boolean isAlarmNeededOnRestoreJobFailure() {
        return alarmNeededOnRestoreJobFailure;
    }

    /**
     * Listener for DeleteBackup JobFailure Alarm attribute value
     * 
     * @param deleteBackupJobFailureAlarm
     */
    void listenForDeleteBackupJobFailureAlarmAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SEND_ALARM_ON_DELETEBACKUP_JOB_FAILURE") final boolean alarmNeededOnDeleteBackupJobFailure) {
        this.alarmNeededOnDeleteBackupJobFailure = alarmNeededOnDeleteBackupJobFailure;
        logger.info("DeleteBackup JobFailure Alarm Parameter value : {}", alarmNeededOnDeleteBackupJobFailure);
    }

    public boolean isAlarmNeededOnDeleteBackupJobFailure() {
        return alarmNeededOnDeleteBackupJobFailure;
    }

    /**
     * Listener for Backup Housekeeping JobFailure Alarm attribute value
     *
     * @param BackupHousekeepingJobFailureAlarm
     */
    void listenForBackupHousekeepingJobFailureAlarmAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SEND_ALARM_ON_BACKUP_HOUSEKEEPING_JOB_FAILURE") final boolean alarmNeededOnBackupHousekeepingJobFailure) {
        this.alarmNeededOnBackupHousekeepingJobFailure = alarmNeededOnBackupHousekeepingJobFailure;
        logger.info("Backup Housekeeping JobFailure Alarm Parameter value : {}", alarmNeededOnBackupHousekeepingJobFailure);
    }

    public boolean isAlarmNeededOnBackupHousekeepingJobFailure() {
        return alarmNeededOnBackupHousekeepingJobFailure;
    }

    /**
     * Listens the change in SEND_ALARM_ON_DELETE_SOFTWARE_PACKAGE_JOB_FAILURE parameter value.
     *
     * @param DeleteSoftwarePackageJobFailureAlarm
     */
    void listenForDeleteSoftwarePackageJobFailureAlarmAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SEND_ALARM_ON_DELETE_SOFTWARE_PACKAGE_JOB_FAILURE") final boolean alarmNeededOnDeleteSoftwarePackageJobFailure) {
        this.alarmNeededOnDeleteSoftwarePackageJobFailure = alarmNeededOnDeleteSoftwarePackageJobFailure;
        logger.info("Delete SoftwarePackage JobFailure JobFailure Alarm Parameter value : {}", alarmNeededOnDeleteSoftwarePackageJobFailure);
    }

    public boolean isAlarmNeededOnDeleteSoftwarePackageJobFailure() {
        return alarmNeededOnDeleteSoftwarePackageJobFailure;
    }

    /**
     * Listens the change in SEND_ALARM_ON_ONBOARD_JOB_FAILURE parameter value.
     *
     * @param OnboardJobFailureAlarm
     */
    void listenForOnboardJobFailureAlarmAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SEND_ALARM_ON_ONBOARD_JOB_FAILURE") final boolean alarmNeededOnOnboardJobFailure) {
        this.alarmNeededOnOnboardJobFailure = alarmNeededOnOnboardJobFailure;
        logger.info("Onboard JobFailure Alarm Parameter value : {}", alarmNeededOnOnboardJobFailure);
    }

    public boolean isAlarmNeededOnOboardJobFailure() {
        return alarmNeededOnOnboardJobFailure;
    }

    /**
     * Listener for SEND_ALARM_ON_SHM_JOB_FAILURE config param is applicable if there is no other specific sdk configuration parameter defined for the job type on which alarm is raised
     *
     * @param isAlarmNeededOnShmJobFailure
     */
    void listenForDefaultJobFailureAlarmAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SEND_ALARM_ON_SHM_JOB_FAILURE") final boolean isAlarmNeededOnShmJobFailure) {
        this.isAlarmNeededOnShmJobFailure = isAlarmNeededOnShmJobFailure;
        logger.info("Shm JobFailure Alarm Parameter value : {}", isAlarmNeededOnShmJobFailure);
    }

    public boolean isAlarmNeededOnShmJobFailure() {
        return isAlarmNeededOnShmJobFailure;
    }
    
    /**
     * Listens the change in SEND_ALARM_ON_LICENSE_REFRESH_JOB_FAILURE parameter value.
     *
     * @param isAlarmNeededOnLicenseRefreshJobFailure
     */
    void listenForLicenseRefreshJobFailureAlarmAttribute(
            @Observes @ConfigurationChangeNotification(propertyName = "SEND_ALARM_ON_LICENSE_REFRESH_JOB_FAILURE") final boolean isAlarmNeededOnLicenseRefreshJobFailure) {
        this.isAlarmNeededOnLicenseRefreshJobFailure = isAlarmNeededOnLicenseRefreshJobFailure;
        logger.info("LicenseRefresh JobFailure Alarm Parameter value : {}", isAlarmNeededOnLicenseRefreshJobFailure);
    }

    public boolean isAlarmNeededOnLicenseRefreshJobFailure() {
        return isAlarmNeededOnLicenseRefreshJobFailure;
    }

}
