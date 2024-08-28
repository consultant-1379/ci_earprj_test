/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.common;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.HANDLE_TIMEOUT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_START_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.moaction.retry.cpp.backup.BackupRetryPolicy;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * Common class for perform setStartable related operation. It will be called from SetStartableCvService and RestoreService(CPP).
 * 
 * @author tcsgusw
 * 
 */

public class SetStartableActivityHandler extends AbstractBackupActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetStartableActivityHandler.class);

    @Inject
    private BackupRetryPolicy backupRetryPolicy;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    /**
     * @deprecated, use {@link SetStartableActivityHandler.executeSetStartableMoAction(long, NEJobStaticData, String, String)} instead.
     * 
     * @param jobEnvironment
     * @param configurationVersionName
     * @param cvMoFdn
     * @return
     */
    @Deprecated
    public boolean executeSetStartableMoAction(final JobEnvironment jobEnvironment, final String configurationVersionName, final String cvMoFdn) {
        LOGGER.debug("Entering into executeSetStartableMoAction to trigger setStartable action on cvMoFdn: {} for the node : {}", cvMoFdn, jobEnvironment.getNodeName());
        boolean performActionStatus = false;
        List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final String nodeName = jobEnvironment.getNodeName();
        final long mainJobId = jobEnvironment.getMainJobId();
        final long activityJobId = jobEnvironment.getActivityJobId();
        String logMessage = String.format(JobLogConstants.SET_STARTABLE_EXECUTE_ACTION, configurationVersionName);
        String resultMessage;
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        try {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            activityUtils.recordEvent(getNotificationEventType(), nodeName, cvMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Started
            systemRecorder.recordCommand(getNotificationEventType(), CommandPhase.STARTED, nodeName, cvMoFdn, activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));

            final Map<String, Object> actionArguments = getSetStartableActionArguments(configurationVersionName);
            jobLogList = new ArrayList<Map<String, Object>>();
            final int actionId = commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_SET_STARTABLE_CV, cvMoFdn, actionArguments, backupRetryPolicy.getDpsMoActionRetryPolicy());
            final long actionTriggeredTime = System.currentTimeMillis();
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_TRIGGERED_TIME, String.valueOf(actionTriggeredTime));
            performActionStatus = true;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.SET_STARTABLE_ACTION_SUCCESS, configurationVersionName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            logMessage = BackupActivityConstants.ACTION_SET_STARTABLE_CV + " MO Action Initiated with action Id : " + actionId + " on CV MO having FDN: " + cvMoFdn;
            LOGGER.debug("{} for activity {}", logMessage, activityJobId);

            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Success
            systemRecorder.recordCommand(getNotificationEventType(), CommandPhase.FINISHED_WITH_SUCCESS, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));

            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_TRIGGERED, BackupActivityConstants.ACTION_SET_STARTABLE_CV);
            resultMessage = "CV " + configurationVersionName + " has been set as Startable CV.";
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ShmConstants.TRUE.toLowerCase());
        } catch (final Exception ex) {
            resultMessage = String.format(JobLogConstants.SET_STARTABLE_ACTION_FAILED, configurationVersionName);
            if (!ExceptionParser.getReason(ex).isEmpty()) {
                resultMessage = String.format(JobLogConstants.SET_STARTABLE_ACTION_FAILED, configurationVersionName) + String.format(JobLogConstants.FAILURE_REASON, ExceptionParser.getReason(ex));
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, resultMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            logMessage = "Unable to start MO Action with action: " + BackupActivityConstants.ACTION_SET_STARTABLE_CV + " on CV MO having FDN : " + cvMoFdn + " because " + ex.getMessage();
            LOGGER.error(logMessage);
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Failure
            systemRecorder.recordCommand(getNotificationEventType(), CommandPhase.FINISHED_WITH_ERROR, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
        }
        activityUtils.recordEvent(getNotificationEventType(), nodeName, cvMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        LOGGER.debug("{} for the activity Job Id : {} ", resultMessage, activityJobId);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        return performActionStatus;
    }

    public boolean executeSetStartableMoAction(final long activityJobId, final NEJobStaticData neJobStaticData, final String configurationVersionName, final String cvMoFdn) {
        LOGGER.debug("Entering into executeSetStartableMoAction to trigger setStartable action on cvMoFdn: {} for the node : {}", cvMoFdn, neJobStaticData.getNodeName());
        boolean performActionStatus = false;
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final String nodeName = neJobStaticData.getNodeName();
        final long mainJobId = neJobStaticData.getMainJobId();
        String logMessage = String.format(JobLogConstants.SET_STARTABLE_EXECUTE_ACTION, configurationVersionName);
        String resultMessage;
        try {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
            jobLogList.clear();
            activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), getNotificationEventType(), nodeName, cvMoFdn,
                    activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Started
            systemRecorder.recordCommand(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), getNotificationEventType(), CommandPhase.STARTED, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));

            final Map<String, Object> actionArguments = getSetStartableActionArguments(configurationVersionName);
            final int actionId = commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_SET_STARTABLE_CV, cvMoFdn, actionArguments, backupRetryPolicy.getDpsMoActionRetryPolicy());
            final long actionTriggeredTime = System.currentTimeMillis();
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_TRIGGERED_TIME, String.valueOf(actionTriggeredTime));
            performActionStatus = true;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.SET_STARTABLE_ACTION_SUCCESS, configurationVersionName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            logMessage = BackupActivityConstants.ACTION_SET_STARTABLE_CV + " MO Action Initiated with action Id : " + actionId + " on CV MO having FDN: " + cvMoFdn;
            LOGGER.debug("{} for activity {}", logMessage, activityJobId);
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Success
            systemRecorder.recordCommand(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), getNotificationEventType(), CommandPhase.FINISHED_WITH_SUCCESS, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_TRIGGERED, BackupActivityConstants.ACTION_SET_STARTABLE_CV);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ShmConstants.TRUE.toLowerCase());
            resultMessage = "CV " + configurationVersionName + " has been set as Startable CV.";
        } catch (final Exception ex) {
            resultMessage = String.format(JobLogConstants.SET_STARTABLE_ACTION_FAILED, configurationVersionName);
            if (!ExceptionParser.getReason(ex).isEmpty()) {
                resultMessage = String.format(JobLogConstants.SET_STARTABLE_ACTION_FAILED, configurationVersionName) + String.format(JobLogConstants.FAILURE_REASON, ExceptionParser.getReason(ex));
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, resultMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            logMessage = "Unable to start MO Action with action: " + BackupActivityConstants.ACTION_SET_STARTABLE_CV + " on CV MO having FDN : " + cvMoFdn + " because " + ex.getMessage();
            LOGGER.error(logMessage);
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Failure
            systemRecorder.recordCommand(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), getNotificationEventType(), CommandPhase.FINISHED_WITH_ERROR, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
        }
        activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), getNotificationEventType(), nodeName, cvMoFdn,
                activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        LOGGER.debug("{} for the activity Job Id : {} ", resultMessage, activityJobId);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        return performActionStatus;
    }

    /**
     * @deprecated, use {@link SetStartableActivityHandler.cancelSetStartableAction(long, NEJobStaticData)} instead.
     * 
     * @param jobEnvironment
     * @return
     */
    @Deprecated
    public ActivityStepResult cancelSetStartableAction(final JobEnvironment jobEnvironment) {
        LOGGER.debug("Inside SetStartableActivityHandler.cancelSetStartableAction() with activityJobId:{}", jobEnvironment.getActivityJobId());

        final long activityJobId = jobEnvironment.getActivityJobId();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        activityUtils.logCancelledByUser(jobLogList, jobEnvironment, ActivityConstants.SET_AS_STARTABLE_CV);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, ActivityConstants.SET_AS_STARTABLE_CV), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.WARN.toString());
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, ShmConstants.TRUE.toLowerCase());
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        LOGGER.debug("Exiting from SetStartableActivityHandler.cancelSetStartableAction() with activityJobId:{}", activityJobId);
        // Return type will be changed to void after making cancel() asynchronous.
        return new ActivityStepResult();
    }

    public ActivityStepResult cancelSetStartableAction(final long activityJobId, final NEJobStaticData neJobStaticData) {
        LOGGER.debug("Inside SetStartableActivityHandler.cancelSetStartableAction() with activityJobId:{}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        activityUtils.logCancelledByUser(jobLogList, neJobStaticData, ActivityConstants.SET_AS_STARTABLE_CV);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, ActivityConstants.SET_AS_STARTABLE_CV), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.WARN.toString());
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, ShmConstants.TRUE.toLowerCase());
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        LOGGER.debug("Exiting from SetStartableActivityHandler.cancelSetStartableAction() with activityJobId:{}", activityJobId);
        // Return type will be changed to void after making cancel() asynchronous.
        return new ActivityStepResult();
    }

    /**
     * Will perform handle timeout for setStartable activity.
     * 
     * @deprecated, use {@link SetStartableActivityHandler.handleTimeoutSetStartableAction(long, NEJobStaticData)} instead.
     * 
     * @param activityJobId
     * @param jobEnvironment
     * @return
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public ActivityStepResult handleTimeoutSetStartableAction(final JobEnvironment jobEnvironment) {
        LOGGER.debug("Entering into SetStartableActivityHandler.handleTimeoutSetStartableAction with activity job Id : {}", jobEnvironment.getActivityJobId());
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        String cvMoFdn = null;
        Map<String, Object> cvMoAttr = null;
        String nodeName = null;
        String logMessage = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        JobResult jobResult = JobResult.FAILED;
        String configurationVersionName = null;
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        final long activityJobId = jobEnvironment.getActivityJobId();
        try {
            nodeName = jobEnvironment.getNodeName();
            final Map<String, Object> moAttributesMap = getConfigurationVersionMo(nodeName);
            configurationVersionName = getConfigurationVersionName(jobEnvironment, BackupActivityConstants.STARTABLE_CV_NAME);
            if (moAttributesMap != null) {
                cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
                cvMoAttr = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
            }
            logMessage = String.format(JobLogConstants.TIMEOUT, BackupActivityConstants.ACTION_SET_STARTABLE_CV);
            activityUtils.recordEvent(SHMEvents.SET_STARTABLE_BACKUP_TIME_OUT, nodeName, cvMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            final boolean isCvSetAsStartable = verifyAction(cvMoAttr, configurationVersionName, ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION);

            if (isCvSetAsStartable) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.SET_STARTABLE_ACTION_SUCCESS, configurationVersionName), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
                LOGGER.debug("Verified by timeout that CV {} has been set as Startable CV.", configurationVersionName);
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
                jobResult = JobResult.SUCCESS;
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.SET_STARTABLE_ACTION_FAILED, configurationVersionName), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
                LOGGER.debug("Verified by timeout that unable to set CV {} as Startable CV.", configurationVersionName);
            }
            final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        } catch (final Exception ex) {
            LOGGER.error("Failed to handle timeout scenario for the setStartable action for the node : {} . Failur reason : {} ", nodeName, ExceptionParser.getReason(ex));
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, BackupActivityConstants.ACTION_SET_STARTABLE_CV, ExceptionParser.getReason(ex));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        LOGGER.debug("Exiting from SetStartableExecuteHandler.handleTimeoutSetStartableAction with activity job Id : {}", activityJobId);
        return activityStepResult;
    }

    /**
     * Will perform handle timeout for setStartable activity.
     * 
     * @param activityJobId
     * @param neJobStaticData
     * @return
     */
    @SuppressWarnings("unchecked")
    public ActivityStepResult handleTimeoutSetStartableAction(final long activityJobId, final NEJobStaticData neJobStaticData) {
        LOGGER.debug("Entering into SetStartableActivityHandler.handleTimeoutSetStartableAction with activity job Id : {}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        String nodeName = null;
        String logMessage = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        JobResult jobResult = JobResult.FAILED;
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        try {
            nodeName = neJobStaticData.getNodeName();
            final Map<String, Object> moAttributesMap = getConfigurationVersionMo(nodeName);
            final String configurationVersionName = getConfigurationVersionName(neJobStaticData, BackupActivityConstants.STARTABLE_CV_NAME);
            String cvMoFdn = null;
            Map<String, Object> cvMoAttr = null;
            if (moAttributesMap != null) {
                cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
                cvMoAttr = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
            }
            logMessage = String.format(JobLogConstants.TIMEOUT, BackupActivityConstants.ACTION_SET_STARTABLE_CV);
            activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.SET_STARTABLE_BACKUP_TIME_OUT, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            final boolean isCvSetAsStartable = SetStartableActivityHandler.verifyAction(cvMoAttr, configurationVersionName, ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION);
            if (isCvSetAsStartable) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.SET_STARTABLE_ACTION_SUCCESS, configurationVersionName), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
                LOGGER.debug("Verified by timeout that CV {} has been set as Startable CV.", configurationVersionName);
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                jobResult = JobResult.SUCCESS;
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.SET_STARTABLE_ACTION_FAILED, configurationVersionName), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
                LOGGER.debug("Verified by timeout that unable to set CV {} as Startable CV.", configurationVersionName);
            }
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
        } catch (final Exception ex) {
            String exceptionMessage = ExceptionParser.getReason(ex);
            if (exceptionMessage == null || exceptionMessage.isEmpty()) {
                exceptionMessage = ex.getMessage();
            }
            LOGGER.error("Failed to handle timeout scenario for the setStartable action for the node : {} . Failure reason : {} ", nodeName, exceptionMessage);
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, BackupActivityConstants.ACTION_SET_STARTABLE_CV, exceptionMessage);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        LOGGER.debug("Exiting from SetStartableExecuteHandler.handleTimeoutSetStartableAction with activity job Id : {}", activityJobId);
        return activityStepResult;
    }

    /**
     * @deprecated, use {@link SetStartableActivityHandler.cancelTimeoutSetStartable(NEJobStaticData, boolean)} instead.
     * 
     * @param finalizeResult
     * @param jobEnvironment
     * @return
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public ActivityStepResult cancelTimeoutSetStartable(final boolean finalizeResult, final JobEnvironment jobEnvironment) {
        LOGGER.debug("Entered into cancelTimeoutSetStartable of SetStartableActivityHandler with activityJobId {}", jobEnvironment.getActivityJobId());
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final String nodeName = jobEnvironment.getNodeName();
        final Map<String, Object> configurationVersionMo = getConfigurationVersionMo(nodeName);
        final Map<String, Object> cvMoAttr = (Map<String, Object>) configurationVersionMo.get(ShmConstants.MO_ATTRIBUTES);
        final String configurationVersionName = getConfigurationVersionName(jobEnvironment, BackupActivityConstants.CV_NAME);
        final boolean isCvSetAsStartable = verifyAction(cvMoAttr, configurationVersionName, ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION);
        if (isCvSetAsStartable) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
        return activityStepResult;
    }

    @SuppressWarnings("unchecked")
    public ActivityStepResult cancelTimeoutSetStartable(final NEJobStaticData neJobStaticData, final boolean finalizeResult) throws JobDataNotFoundException {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        final String nodeName = neJobStaticData.getNodeName();
        final Map<String, Object> configurationVersionMo = getConfigurationVersionMo(nodeName);
        final Map<String, Object> cvMoAttr = (Map<String, Object>) configurationVersionMo.get(ShmConstants.MO_ATTRIBUTES);
        final String configurationVersionName = getConfigurationVersionName(neJobStaticData, BackupActivityConstants.STARTABLE_CV_NAME);
        final boolean isCvSetAsStartable = verifyAction(cvMoAttr, configurationVersionName, ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION);
        if (isCvSetAsStartable) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        }
        return activityStepResult;
    }

    /**
     * This method verifies that whether action triggered is successfully executed on the node.
     * 
     * @param activityJobId
     * @param configurationVersionName
     * @param attributeToBeCheckedKey
     * @return
     */
    private static boolean verifyAction(final Map<String, Object> cvMoAttributes, final String configurationVersionName, final String attributeToBeCheckedKey) {
        final String startableConfigurationVersion = (String) cvMoAttributes.get(attributeToBeCheckedKey);
        LOGGER.debug("StartableConfiguratioVersion of configurationVersionName {} ", configurationVersionName);
        boolean verified;
        if (configurationVersionName.equals(startableConfigurationVersion)) {
            verified = ActivityConstants.TRUE;
        } else {
            verified = ActivityConstants.FALSE;
        }
        return verified;
    }

    private static Map<String, Object> getSetStartableActionArguments(final String configurationVersionName) {
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, configurationVersionName);
        return actionArguments;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity#getActivityType()
     */
    @Override
    public String getActivityType() {
        return ActivityConstants.SET_AS_STARTABLE_CV;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity#getNotificationEventType()
     */
    @Override
    public String getNotificationEventType() {
        return SHMEvents.SET_STARTABLE_BACKUP_EXECUTE;
    }

}
