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
package com.ericsson.oss.services.shm.es.impl.ecim.deleteup;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@Traceable
@RequestScoped
public class TimeoutHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutHandler.class);

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private DeleteUpgradePackageJobDataCollectorRetryProxy deleteUpDataCollectRetryProxy;

    @Inject
    private DeleteUpgradePackageDataCollector deleteUpJobDataCollector;

    private String nodeName = "";

    private JobEnvironment jobEnvironment = null;

    private NEJobStaticData neJobStaticData = null;

    private final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
    private final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();

    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Verifying activity result in handle timeout for the action  : remove upgradepackage and with action Id : {}", activityJobId);
        String currentBackupData = null;
        String currentUpMoData = null;
        ActivityStepResult timeoutResponse = new ActivityStepResult();

        initializeVariables(activityJobId);

        final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);

        currentBackupData = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.CURRENT_BKPNAME);
        currentUpMoData = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.CURRENT_UP_MO_DATA);
        final String upsWithSyscrBksData = activityUtils.getActivityJobAttributeValue(activityJobAttributes, EcimCommonConstants.UPS_WITH_SYSCR_BACKUPS);

        Map<String, Set<String>> upsWithSyscrBkps = deleteUpJobDataCollector.converToMap(upsWithSyscrBksData);
        // On identifying upsWithSYSCR Backups map, it will be known that AP MO's rbsConfigLevel is corrected so that a SYSCR backup gets deleted by Node.
        if (!currentBackupData.isEmpty()){
            final String[] currentBackupDataArray = currentBackupData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
            timeoutResponse = handleTimeoutFordeleteBackupOnNode(activityJobId, currentBackupDataArray[0], currentBackupDataArray[1], currentBackupData);
        } else if (currentBackupData.isEmpty() && upsWithSyscrBkps != null && !upsWithSyscrBkps.isEmpty()) {
            currentBackupData = deleteUpJobDataCollector.checkForDeletedSysCreatedBackupInfo(upsWithSyscrBkps, nodeName);
            if(!currentBackupData.isEmpty()) {
                String[] backupData = currentBackupData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
                systemRecorder.recordCommand(SHMEvents.ECIM_DELETE_UPGRADEPACKAGE_AP_RBSCONFIGLEVEL_CHANGE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, EcimCommonConstants.AutoProvisioningMoConstants.AP_MO,
                        String.format(JobLogConstants.RESET_AP_MO_RBS_CONFIG_VALUE_DELETE_BACKUP, backupData[0]));
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.RESET_AP_MO_RBS_CONFIG_VALUE_DELETE_BACKUP, backupData[0]), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            } else {
                final Map<String, Object> processVariables = new HashMap<>();
                systemRecorder.recordCommand(SHMEvents.ECIM_DELETE_UPGRADEPACKAGE_AP_RBSCONFIGLEVEL_CHANGE, CommandPhase.FINISHED_WITH_ERROR, nodeName, EcimCommonConstants.AutoProvisioningMoConstants.AP_MO,
                        String.format(JobLogConstants.NO_BACKUP_DELETED_ON_AP_MO_CHANGE, ActivityConstants.DELETE_UP_DISPLAY_NAME));
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.NO_BACKUP_DELETED_ON_AP_MO_CHANGE, ActivityConstants.DELETE_UP_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
                processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, ShmConstants.FALSE.toLowerCase());
                activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SKIPPED.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
                jobLogs.clear();
                timeoutResponse.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                deleteUpJobDataCollector.unSubscribeToBrmBackupMosNotifications(upsWithSyscrBkps, activityJobId);
                return timeoutResponse;
            }
            String backupName = ActivityConstants.EMPTY;
            String backupManager = ActivityConstants.EMPTY;
            if(!currentBackupData.isEmpty()) {
                final String[] currentBackupDataArray = currentBackupData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);    
                backupName = currentBackupDataArray[0];
                backupManager = currentBackupDataArray[1];
            }
            deleteUpJobDataCollector.unSubscribeToBrmBackupMosNotifications(upsWithSyscrBkps, activityJobId);
            timeoutResponse = handleTimeoutFordeleteBackupOnNode(activityJobId, backupName, backupManager, currentBackupData);
        } else {
            final String swmNamespace = activityUtils.getActivityJobAttributeValue(activityJobAttributes, EcimCommonConstants.SWM_NAMESPACE);
            final String swM_MO_Fdn = deleteUpDataCollectRetryProxy.getSwmManagedObjectFdn(nodeName, swmNamespace);
            timeoutResponse = handleTimeoutFordeleteUpgardePackageOnNode(activityJobId, currentUpMoData, swM_MO_Fdn);
        }
        return timeoutResponse;
    }

    private void initializeVariables(final long activityJobId) {
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        } catch (JobDataNotFoundException jobDataNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, jobDataNotFoundException);
        }
    }

    private ActivityStepResult handleTimeoutFordeleteUpgardePackageOnNode(final long activityJobId, final String currentUpMoData, final String swM_MO_Fdn) {
        final String currentUpMoDataArray[] = currentUpMoData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
        LOGGER.debug("Entered handleTimeoutFordeleteUpgradePackageOnNode : {} , nodeName : {} , and activityJobId: {}", currentUpMoData, nodeName, activityJobId);
        JobResult jobResult = null;
        String swMNamespace;
        String logMessage = null;
        double totalActivityProgressPercentage = 0.0;
        boolean isRepeat = false;
        String isIntermediateFailureOnUp = null;
        final ActivityStepResult timeoutResult = new ActivityStepResult();
        ActivityStepResultEnum timeoutResponse = null;
        try {
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            isRepeat = deleteUpJobDataCollector.setUpAndBackupDataForNextItration(activityJobId, activityJobAttributes, currentUpMoData);
            if (currentUpMoDataArray.length == 0) {
                if (isRepeat) {
                    timeoutResult.setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
                    return timeoutResult;
                } else {
                    timeoutResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                    return timeoutResult;
                }
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogs,
                    String.format(JobLogConstants.DELETEUP_TIMEOUT, ActivityConstants.DELETE_UP_DISPLAY_NAME, currentUpMoDataArray[0], currentUpMoDataArray[1]), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            activityUtils.unSubscribeToMoNotifications(swM_MO_Fdn, activityJobId, activityUtils.getActivityInfo(activityJobId, DeleteUpgradePackageService.class));

            swMNamespace = deleteUpJobDataCollector.getSWMNameSpace(nodeName, FragmentType.ECIM_SWM_TYPE.getFragmentName());
            final Set<String> nodeUpData = deleteUpJobDataCollector.getUpData(nodeName, swMNamespace);
            final Set<String> inputProductDataSet = new HashSet<String>();
            inputProductDataSet.add(currentUpMoData);
            final Set<String> filteredValidUPMOData = deleteUpJobDataCollector.fetchValidUPDataOverInputData(nodeUpData, inputProductDataSet);
            if (filteredValidUPMOData.isEmpty()) {
                jobResult = JobResult.SUCCESS;
                logMessage = String.format(JobLogConstants.UP_DELETED_SUCCESSFULLY, currentUpMoDataArray[0], currentUpMoDataArray[1]);
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                totalActivityProgressPercentage = deleteUpJobDataCollector.calculateActivityProgressPercentage(jobEnvironment, 100);
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, totalActivityProgressPercentage);
            } else {
                jobResult = JobResult.FAILED;
                logMessage = String.format(JobLogConstants.UP_DELETION_FAILED, currentUpMoDataArray[0], currentUpMoDataArray[1]);
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }

            isIntermediateFailureOnUp = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.UP_INTERMEDIATE_FAILURE);

            if (isRepeat) {
                if (activityUtils.cancelTriggered(activityJobId)) {
                    jobResult = JobResult.FAILED;
                    activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
                } else {
                    timeoutResponse = ActivityStepResultEnum.REPEAT_EXECUTE;
                }
            } else {

                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
                final String isAnyActiveUp = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.IS_ANY_ACTIVE_UP);
                if (isAnyActiveUp != null && Boolean.valueOf(isAnyActiveUp) || JobResult.FAILED.toString().equals(isIntermediateFailureOnUp) || JobResult.FAILED == jobResult) {
                    jobResult = JobResult.FAILED;
                    logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DELETE_UP_DISPLAY_NAME);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());

                } else {
                    jobResult = JobResult.SUCCESS;
                    logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.DELETE_UP_DISPLAY_NAME);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                }
                LOGGER.debug("Delete upgrade job final result in handle time out {}", jobResult);
                activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
                timeoutResponse = (jobResult == JobResult.SUCCESS) ? ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS : ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
            }
        } catch (MoNotFoundException moNotFoundException) {
            logMessage = moNotFoundException.getMessage();
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobResult = JobResult.FAILED;
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred in timeout with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId, nodeName, ex);
            final String errorMessage = activityUtils.prepareErrorMessage(ex);
            logMessage = String.format("An exception occurred for activity : %s ", ActivityConstants.DELETE_UP_DISPLAY_NAME) + String.format(JobLogConstants.FAILURE_REASON, errorMessage);
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
            jobLogs.clear();
        }
        timeoutResult.setActivityResultEnum(timeoutResponse);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, totalActivityProgressPercentage);
        jobLogs.clear();

        return timeoutResult;
    }

    public ActivityStepResult handleTimeoutFordeleteBackupOnNode(final long activityJobId, final String backupname, final String brmBackupManagerMoFdn, final String deletedBackupData) {
        LOGGER.debug("Entered handleTimeoutFordeleteBackupOnNode with backupName : {} , nodeName : {} , and activityJobId: {}", backupname, nodeName, activityJobId);
        final ActivityStepResult timeoutResult = new ActivityStepResult();
        ActivityStepResultEnum timeoutResponse = null;
        String logMessage = String.format("Delete Backup has Timed Out on node %s", nodeName);
        boolean isRepeat = false;
        activityUtils.recordEvent(SHMEvents.TIME_OUT_FOR_DELETE_BACKUP, nodeName, brmBackupManagerMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        final String bkpfile = "Backup File = %s.";
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(bkpfile, backupname) + String.format(JobLogConstants.TIMEOUT, EcimBackupConstants.DELETE_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.WARN.toString());
        activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, DeleteUpgradePackageService.class));
        try {
            if (!backupname.isEmpty() && !brmBackupManagerMoFdn.isEmpty() && brmMoServiceRetryProxy.isBackupDeletionCompleted(backupname, brmBackupManagerMoFdn, nodeName)) {
                logMessage = String.format("Backup File = %s  deleted successfully after timeout on the node = %s.", backupname, nodeName);// Job Logs.
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                LOGGER.debug("Completed deletion of {} successfully on the node {} after timeout.", backupname, nodeName);
                activityUtils.recordEvent(SHMEvents.TIME_OUT_FOR_DELETE_BACKUP, nodeName, brmBackupManagerMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            } else {
                logMessage = String.format("Deletion of backup - %s failed on the node - %s.", backupname, nodeName);// Job
                                                                                                                     // logs.
                LOGGER.error("Backup deletion failed after timeout for backup - {} on the node {}.", backupname, nodeName);
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                activityUtils.recordEvent(SHMEvents.TIME_OUT_FOR_DELETE_BACKUP, nodeName, brmBackupManagerMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage)); // Record
                                                                                                                                                                                             // event.
                activityUtils.prepareJobPropertyList(jobProperties, DeleteUpgradePackageConstants.BACKUP_INTERMEDIATE_FAILURE, JobResult.FAILED.toString());
            }
            isRepeat = deleteUpJobDataCollector.setBackupDataForNextItration(activityJobId, deletedBackupData);
        } catch (MoNotFoundException moNotFoundException) {
            LOGGER.error("BrmBackupManagerMo not found during time out evaluation of activity completion for the node {} with exception : {}", nodeName, moNotFoundException);// Job Logs.
            logMessage = moNotFoundException.getMessage();
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (UnsupportedFragmentException unsupportedFragmentException) {
            LOGGER.error("BrmBackupManagerMo not found during time out evaluation of activity completion for the node {} with exception : {}", nodeName, unsupportedFragmentException);// Job Logs.
            logMessage = unsupportedFragmentException.getMessage();

            jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        if (isRepeat) {
            timeoutResponse = ActivityStepResultEnum.REPEAT_EXECUTE;
        } else {
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
        }

        timeoutResult.setActivityResultEnum(timeoutResponse);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
        jobLogs.clear();
        return timeoutResult;
    }

}
