/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.polling;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.moaction.cache.MoActionRequestTimer;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

@ApplicationScoped
@SuppressWarnings("PMD.TooManyFields")
public class PollingActivityConfigurationImpl implements PollingActivityConfiguration {

    private final static Logger LOGGER = LoggerFactory.getLogger(PollingActivityConfigurationImpl.class);

    private static final int MILLISECONDS = 1000;

    @Inject
    @Configured(propertyName = "CPP_UPGRADEJOB_UPGRADE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int cppUpgradeJobUpgradeActivityWaitTimeToStartPolling;

    @Inject
    @Configured(propertyName = "IS_POLLING_ENABLED_FOR_JOBS_ON_CPP_NODES")
    private boolean isPollingEnabledForJobsOnCppNodes;

    @Inject
    @Configured(propertyName = "IS_POLLING_ENABLED_FOR_JOBS_ON_ECIM_NODES")
    private boolean isPollingEnabledForJobsOnEcimNodes;

    @Inject
    @Configured(propertyName = "IS_POLLING_ENABLED_FOR_UPRGRADE_JOB_ON_CPP_NODES")
    private boolean isPollingEnabledForUpgradeJobOnCppNodes;

    @Inject
    @Configured(propertyName = "IS_POLLING_ENABLED_FOR_UPGRADE_JOB_ON_ECIM_NODES")
    private boolean isPollingEnabledForUpgradeJobOnEcimNodes;

    @Inject
    @Configured(propertyName = "IS_POLLING_ENABLED_FOR_BACKUP_JOB_ON_CPP_NODES")
    private boolean isPollingEnabledForBackupJobOnCppNodes;

    @Inject
    @Configured(propertyName = "IS_POLLING_ENABLED_FOR_BACKUP_JOB_ON_ECIM_NODES")
    private boolean isPollingEnabledForBackupJobOnEcimNodes;

    @Inject
    @Configured(propertyName = "IS_POLLING_ENABLED_FOR_RESTORE_JOB_ON_CPP_NODES")
    private boolean isPollingEnabledForRestoreJobOnCppNodes;

    @Inject
    @Configured(propertyName = "IS_POLLING_ENABLED_FOR_RESTORE_JOB_ON_ECIM_NODES")
    private boolean isPollingEnabledForRestoreJobOnEcimNodes;

    @Inject
    @Configured(propertyName = "isPollingEnabledForNHCJobOnECIMNodes")
    private boolean isPollingEnabledForNHCJobOnEcimNodes;

    @Inject
    @Configured(propertyName = "INITIAL_DELAY_TO_START_TIMER_AFTER_SERVICE_STARTUP")
    private int initialDelayToStartTimerAfterServiceStartUp;

    @Inject
    @Configured(propertyName = "INTERVAL_TIME_FOR_POLLING_IN_MILLISECONDS")
    private int intervalTimeForPolling;

    @Inject
    @Configured(propertyName = "CPP_RESTOREJOB_RESTORE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES")
    private int cppRestoreJobRestoreActivityWaitTimeToStartPolling;

    @Inject
    @Configured(propertyName = "SHM_HEARTBEAT_INTERVAL_FOR_ECIM")
    private int shmHeartBeatIntervalForEcim;

    @Inject
    @Configured(propertyName = "OPERATION_TIMEOUT_FOR_CPP_BASED_NODES")
    private int operationTimeOutForCppBasedNodes;

    @Inject
    @Configured(propertyName = "OPERATION_TIMEOUT_FOR_ECIM_BASED_NODES")
    private int operationTimeOutForEcimBasedNodes;

    @Inject
    @Configured(propertyName = "INTERVAL_TIME_FOR_MO_ACTION_CACHE_ITERATION_IN_MILLISECONDS")
    private int intervalTimeForMoActionCache;

    @Inject
    private ActivityPollingTimer activityPollingTimer;

    @Inject
    private PollingTimer pollingTimer;

    @Inject
    private MoActionRequestTimer moActionRequestTimer;

    @Inject
    @Configured(propertyName = "IS_POLLING_ENABLED_FOR_JOBS_ON_AXE_NODES")
    private boolean isPollingEnabledForJobsOnAxeNodes;

    @Inject
    @Configured(propertyName = "IS_POLLING_ENABLED_FOR_BACKUP_JOB_ON_AXE_NODES")
    private boolean isPollingEnabledForBackupJobOnAxeNodes;

    void listenForCppUpgradeJobUpgradeActivityWaitTimeToStartPolling(
            @Observes @ConfigurationChangeNotification(propertyName = "CPP_UPGRADEJOB_UPGRADE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int cppUpgradeJobUpgradeActivityWaitTimeToStartPolling) {
        this.cppUpgradeJobUpgradeActivityWaitTimeToStartPolling = cppUpgradeJobUpgradeActivityWaitTimeToStartPolling;
    }

    void listenForIsPollingEnabledForJobsOnCppNodes(
            @Observes @ConfigurationChangeNotification(propertyName = "IS_POLLING_ENABLED_FOR_JOBS_ON_CPP_NODES") final boolean isPollingEnabledForJobsOnCppNodes) {
        this.isPollingEnabledForJobsOnCppNodes = isPollingEnabledForJobsOnCppNodes;
    }

    void listenForIsPollingEnabledForJobsOnEcimNodes(
            @Observes @ConfigurationChangeNotification(propertyName = "IS_POLLING_ENABLED_FOR_JOBS_ON_ECIM_NODES") final boolean isPollingEnabledForJobsOnEcimNodes) {
        this.isPollingEnabledForJobsOnEcimNodes = isPollingEnabledForJobsOnEcimNodes;
    }

    void listenForIsPollingEnabledForUpgradeJobOnCppNodes(
            @Observes @ConfigurationChangeNotification(propertyName = "IS_POLLING_ENABLED_FOR_UPRGRADE_JOB_ON_CPP_NODES") final boolean isPollingEnabledForUpgradeJobOnCppNodes) {
        this.isPollingEnabledForUpgradeJobOnCppNodes = isPollingEnabledForUpgradeJobOnCppNodes;
    }

    void listenForIsPollingEnabledForUpgradeJobOnEcimNodes(
            @Observes @ConfigurationChangeNotification(propertyName = "IS_POLLING_ENABLED_FOR_UPGRADE_JOB_ON_ECIM_NODES") final boolean isPollingEnabledForUpgradeJobOnEcimNodes) {
        this.isPollingEnabledForUpgradeJobOnEcimNodes = isPollingEnabledForUpgradeJobOnEcimNodes;
    }

    void listenForIsPollingEnabledForNHCJobOnEcimNodes(
            @Observes @ConfigurationChangeNotification(propertyName = "isPollingEnabledForNHCJobOnECIMNodes") final boolean isPollingEnabledForNHCJobOnEcimNodes) {
        this.isPollingEnabledForNHCJobOnEcimNodes = isPollingEnabledForNHCJobOnEcimNodes;
    }

    void listenForIsPollingEnabledForBackupJobOnCppNodes(
            @Observes @ConfigurationChangeNotification(propertyName = "IS_POLLING_ENABLED_FOR_BACKUP_JOB_ON_CPP_NODES") final boolean isPollingEnabledForBackupJobOnCppNodes) {
        this.isPollingEnabledForBackupJobOnCppNodes = isPollingEnabledForBackupJobOnCppNodes;
    }

    void listenForIsPollingEnabledForBackupJobOnEcimNodes(
            @Observes @ConfigurationChangeNotification(propertyName = "IS_POLLING_ENABLED_FOR_BACKUP_JOB_ON_ECIM_NODES") final boolean isPollingEnabledForBackupJobOnEcimNodes) {
        this.isPollingEnabledForBackupJobOnEcimNodes = isPollingEnabledForBackupJobOnEcimNodes;
    }

    void listenForIsPollingEnabledForRestoreJobOnCppNodes(
            @Observes @ConfigurationChangeNotification(propertyName = "IS_POLLING_ENABLED_FOR_RESTORE_JOB_ON_CPP_NODES") final boolean isPollingEnabledForRestoreJobOnCppNodes) {
        this.isPollingEnabledForRestoreJobOnCppNodes = isPollingEnabledForRestoreJobOnCppNodes;
    }

    void listenForIsPollingEnabledForRestoreJobOnEcimNodes(
            @Observes @ConfigurationChangeNotification(propertyName = "IS_POLLING_ENABLED_FOR_RESTORE_JOB_ON_ECIM_NODES") final boolean isPollingEnabledForRestoreJobOnEcimNodes) {
        this.isPollingEnabledForRestoreJobOnEcimNodes = isPollingEnabledForRestoreJobOnEcimNodes;
    }

    void listenForInitialDelayToStartTimerAfterServiceStartUp(
            @Observes @ConfigurationChangeNotification(propertyName = "INITIAL_DELAY_TO_START_TIMER_AFTER_SERVICE_STARTUP") final int initialDelayToStartTimerAfterServiceStartUp) {
        this.initialDelayToStartTimerAfterServiceStartUp = initialDelayToStartTimerAfterServiceStartUp;
        activityPollingTimer.restartTimer();
        pollingTimer.restartTimer();
    }

    void listenForIntervalTimeForPolling(@Observes @ConfigurationChangeNotification(propertyName = "INTERVAL_TIME_FOR_POLLING_IN_MILLISECONDS") final int intervalTimeForPolling) {
        this.intervalTimeForPolling = intervalTimeForPolling;
        activityPollingTimer.restartTimer();
        pollingTimer.restartTimer();

    }

    void listenForCppRestoreJobRestoreActivityWaitTimeToStartPolling(
            @Observes @ConfigurationChangeNotification(propertyName = "CPP_RESTOREJOB_RESTORE_ACTIVITY_WAIT_TIME_TO_START_POLLING_IN_MINUTES") final int cppRestoreJobRestoreActivityWaitTimeToStartPolling) {
        this.cppRestoreJobRestoreActivityWaitTimeToStartPolling = cppRestoreJobRestoreActivityWaitTimeToStartPolling;
    }

    void listenForShmHeartBeatIntervalForEcim(
            @Observes @ConfigurationChangeNotification(propertyName = "HEARTBEAT_INTERVAL_ON_CM_NODE_HEARTBEAT_SUPERVISION_MO") final int shmHeartBeatIntervalForEcim) {
        this.shmHeartBeatIntervalForEcim = shmHeartBeatIntervalForEcim;
    }

    void listenForOperationTimeOutForCppBasedNodes(@Observes @ConfigurationChangeNotification(propertyName = "OPERATION_TIMEOUT_FOR_CPP_BASED_NODES") final int operationTimeOutForCppBasedNodes) {
        this.operationTimeOutForCppBasedNodes = operationTimeOutForCppBasedNodes;
    }

    void listenForOperationTimeOutForEcimBasedNodes(@Observes @ConfigurationChangeNotification(propertyName = "OPERATION_TIMEOUT_FOR_ECIM_BASED_NODES") final int operationTimeOutForEcimBasedNodes) {
        this.operationTimeOutForEcimBasedNodes = operationTimeOutForEcimBasedNodes;
    }

    void listenForIntervalTimeForMoActionCache(
            @Observes @ConfigurationChangeNotification(propertyName = "INTERVAL_TIME_FOR_MO_ACTION_CACHE_ITERATION_IN_MILLISECONDS") final int intervalTimeForMoActionCache) {
        this.intervalTimeForMoActionCache = intervalTimeForMoActionCache;
        moActionRequestTimer.restartTimer();

    }

    void listenForIsPollingEnabledForJobsOnAxeNodes(
            @Observes @ConfigurationChangeNotification(propertyName = "IS_POLLING_ENABLED_FOR_JOBS_ON_AXE_NODES") final boolean isPollingEnabledForJobsOnAxeNodes) {
        this.isPollingEnabledForJobsOnAxeNodes = isPollingEnabledForJobsOnAxeNodes;
    }

    void listenForIsPollingEnabledForBackupJobOnAxeNodes(
            @Observes @ConfigurationChangeNotification(propertyName = "IS_POLLING_ENABLED_FOR_BACKUP_JOB_ON_AXE_NODES") final boolean isPollingEnabledForBackupJobOnAxeNodes) {
        this.isPollingEnabledForBackupJobOnAxeNodes = isPollingEnabledForBackupJobOnAxeNodes;
    }

    @Override
    public boolean isPollingEnabledForJobsOnEcimNodes() {
        return isPollingEnabledForJobsOnEcimNodes;
    }

    @Override
    public boolean isPollingEnabledForUpgradeJobOnEcimNodes() {
        return isPollingEnabledForUpgradeJobOnEcimNodes;
    }

    @Override
    public boolean isPollingEnabledForBackupJobOnEcimNodes() {
        return isPollingEnabledForBackupJobOnEcimNodes;
    }

    @Override
    public boolean isPollingEnabledForRestoreJobOnEcimNodes() {
        return isPollingEnabledForRestoreJobOnEcimNodes;
    }

    @Override
    public int getCppUpgradeJobUpgradeActivityWaitTimeToStartPolling() {
        return this.cppUpgradeJobUpgradeActivityWaitTimeToStartPolling;
    }

    @Override
    public boolean isPollingEnabledForJobsOnCppNodes() {
        return isPollingEnabledForJobsOnCppNodes;
    }

    @Override
    public boolean isPollingEnabledForUpgradeJobOnCppNodes() {
        return isPollingEnabledForUpgradeJobOnCppNodes;
    }

    @Override
    public boolean isPollingEnabledForBackupJobOnCppNodes() {
        return isPollingEnabledForBackupJobOnCppNodes;
    }

    @Override
    public boolean isPollingEnabledForRestoreJobOnCppNodes() {
        return isPollingEnabledForRestoreJobOnCppNodes;
    }

    @Override
    public int getInitialDelayToStartTimerAfterServiceStartUp() {
        return this.initialDelayToStartTimerAfterServiceStartUp;
    }

    @Override
    public int getPollingIntervalDelay() {
        return this.intervalTimeForPolling;
    }

    @Override
    public int getCppRestoreJobRestoreActivityWaitTimeToStartPolling() {
        return this.cppRestoreJobRestoreActivityWaitTimeToStartPolling;
    }

    @Override
    public int getShmHeartBeatIntervalForEcim() {
        return shmHeartBeatIntervalForEcim;
    }

    public long getOperationTimeOutForCppBasedNodes() {
        final long operationTimeOutForCppBasedNodesAsLong = operationTimeOutForCppBasedNodes;
        return (operationTimeOutForCppBasedNodesAsLong * MILLISECONDS);
    }

    public long getOperationTimeOutForEcimBasedNodes() {
        final long operationTimeOutForEcimBasedNodesAsLong = operationTimeOutForEcimBasedNodes;
        return operationTimeOutForEcimBasedNodesAsLong * MILLISECONDS;
    }

    @Override
    public int getIntervalTimeForMoActionCache() {
        return this.intervalTimeForMoActionCache;
    }

    @Override
    public boolean isPollingEnabledForJobsOnAxeNodes() {
        return isPollingEnabledForJobsOnAxeNodes;
    }

    @Override
    public boolean isPollingEnabledForBackupJobOnAxeNodes() {
        return isPollingEnabledForBackupJobOnAxeNodes;
    }

    /**
     * @param platformType
     * @param jobTypeEnum
     * @return boolean value for isPollingEnabled
     */
    @Override
    public boolean isPollingEnabled(final PlatformTypeEnum platformType, final JobTypeEnum jobTypeEnum) {
        boolean isActivityPollingEnabled = false;

        final boolean isEnabledAtPlatformLevel = isPollingEnabledAtPlatform(platformType);
        if (!isEnabledAtPlatformLevel) {
            return isEnabledAtPlatformLevel;
        }
        switch (platformType) {
        case CPP:
            switch (jobTypeEnum) {
            case BACKUP:
                isActivityPollingEnabled = isPollingEnabledForBackupJobOnCppNodes();
                break;
            case RESTORE:
                isActivityPollingEnabled = isPollingEnabledForRestoreJobOnCppNodes();
                break;
            case UPGRADE:
                isActivityPollingEnabled = isPollingEnabledForUpgradeJobOnCppNodes();
                break;
            default:
                LOGGER.warn("Currently polling not supported for the jobType :{} and platform {}", jobTypeEnum, platformType);
                break;
            }
            break;
        case ECIM:
            switch (jobTypeEnum) {
            case BACKUP:
                isActivityPollingEnabled = isPollingEnabledForBackupJobOnEcimNodes();
                break;
            case RESTORE:
                isActivityPollingEnabled = isPollingEnabledForRestoreJobOnEcimNodes();
                break;
            case UPGRADE:
                isActivityPollingEnabled = isPollingEnabledForUpgradeJobOnEcimNodes();
                break;
            case NODE_HEALTH_CHECK:
                isActivityPollingEnabled = isPollingEnabledForNHCJobOnEcimNodes;
                break;
            default:
                LOGGER.warn("Currently polling not supported for the jobType :{} and platform {}", jobTypeEnum, platformType);
                break;
            }
            break;
        case AXE:
            switch (jobTypeEnum) {
            case BACKUP:
                isActivityPollingEnabled = isPollingEnabledForBackupJobOnAxeNodes();
                break;
            case LICENSE:
                LOGGER.debug("License job has to implement polling");
                break;
            default:
                LOGGER.warn("Currently polling not supported for the jobType :{} and platform {}", jobTypeEnum, platformType);
                break;
            }
            break;
        default:
            LOGGER.warn("Currently polling not supported for the jobType :{} and platform {}", jobTypeEnum, platformType);
            break;
        }
        return isActivityPollingEnabled;
    }

    private boolean isPollingEnabledAtPlatform(final PlatformTypeEnum platformType) {
        switch (platformType) {
        case CPP:
            return isPollingEnabledForJobsOnCppNodes();
        case ECIM:
            return isPollingEnabledForJobsOnEcimNodes();
        case AXE:
            return isPollingEnabledForJobsOnAxeNodes();
        default:
            return false;
        }
    }

    @Override
    public long getOperationTimeOutBasedOnPlatformType(final String platform) {
        long operationTimeOut = 0;
        if (platform != null && platform.equalsIgnoreCase(PlatformTypeEnum.CPP.toString())) {
            operationTimeOut = getOperationTimeOutForCppBasedNodes();
        } else if (platform != null && platform.equalsIgnoreCase(PlatformTypeEnum.ECIM.toString())) {
            operationTimeOut = getOperationTimeOutForEcimBasedNodes();
        }
        return operationTimeOut;
    }
}