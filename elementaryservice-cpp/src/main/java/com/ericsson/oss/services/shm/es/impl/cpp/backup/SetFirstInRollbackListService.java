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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * This class facilitates the setting of configuration version as first in rollback list of CV MO of CPP based node by invoking the ConfigurationVersion MO action.
 * 
 * @author tcsrohc
 * 
 */
@EServiceQualifier("CPP.BACKUP.setcvfirstinrollbacklist")
@ActivityInfo(activityName = "setcvfirstinrollbacklist", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SetFirstInRollbackListService extends AbstractBackupActivity implements Activity, AsynchronousActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetFirstInRollbackListService.class);

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    /**
     * This method validates the status of CV to decide if setcvfirstinrollbacklist activity can be started or not and sends back the activity result to Work Flow Service.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            activityStepResult.setActivityResultEnum(getPrecheckResponse(activityJobId, neJobStaticData).getActivityStepResultEnum());
        } catch (final JobDataNotFoundException jdnfEx) {
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            LOGGER.error(String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.SET_AS_STARTABLE_CV, jdnfEx.getMessage()));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.SET_AS_STARTABLE_CV, jdnfEx.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        return activityStepResult;
    }

    private CppBackupPrecheckResponse getPrecheckResponse(final long activityJobId, final NEJobStaticData neJobStaticData) {
        CppBackupPrecheckResponse precheckResponse = null;
        final String nodeName = neJobStaticData.getNodeName();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> emptyMap = new HashMap<String, Object>();
        try {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
            final Map<String, Object> moAttributesMap = getConfigurationVersionMo(nodeName);
            final String cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
            if (moAttributesMap.isEmpty()) {
                LOGGER.error("No CV MO found for {} activity with Id {} for node {}", getActivityType(), activityJobId, nodeName);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.MO_NOT_EXIST, ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST, BackupActivityConstants.CV_MO_TYPE),
                        new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                precheckResponse = new CppBackupPrecheckResponse(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, emptyMap, emptyMap);
                return precheckResponse;
            }
            final String configurationVersionName = getConfigurationVersionName(neJobStaticData, BackupActivityConstants.ROLLBACK_CV_NAME);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            final String logMessage = "Proceeding SetFirstInRollBackList with CV " + configurationVersionName;
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);
            LOGGER.debug("{} for {} activity with activityJobId {} ", logMessage, getActivityType(), activityJobId);
            activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.SET_BACKUP_FIRST_IN_ROLLBACK_LIST_PRECHECK, nodeName, cvMoFdn,
                    "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
            final Map<String, Object> cvNameActionArgument = new HashMap<String, Object>();
            cvNameActionArgument.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, configurationVersionName);
            precheckResponse = new CppBackupPrecheckResponse(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, moAttributesMap, cvNameActionArgument);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred in precheck of set first in rollback list for node {} due to : {}", nodeName, ex);
            String exceptionMessage = ExceptionParser.getReason(ex);
            if (exceptionMessage == null || exceptionMessage.isEmpty()) {
                exceptionMessage = ex.getMessage();
            }
            final String logMessage = getActivityType() + " Precheck failed." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            precheckResponse = new CppBackupPrecheckResponse(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, emptyMap, emptyMap);
        }
        activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
        return precheckResponse;
    }

    /**
     * This method is the activity execute step called when pre-check stage is passed. This performs the MO action and sends back activity result to Work Flow Service
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        String nodeName = null;
        long mainJobId = 0;
        String cvMoFdn = null;
        JobResult jobResult = JobResult.FAILED;
        String logMessage = null;
        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        String neJobBusinessKey = "";
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            neJobBusinessKey = neJobStaticData.getNeJobBusinessKey();
            mainJobId = neJobStaticData.getMainJobId();
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.SET_FIRST_IN_ROLLBACK_ACTIVITY_NAME);
            if (!isUserAuthorized && neJobStaticData != null) {
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST);
                return;
            }

            final CppBackupPrecheckResponse setFirstInRollBackListPrecheckResponse = getPrecheckResponse(activityJobId, neJobStaticData);
            if (setFirstInRollBackListPrecheckResponse.getActivityStepResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
                LOGGER.debug("Executing {} activity for activityJobId {} on node {}", getActivityType(), activityJobId, nodeName);
                cvMoFdn = (String) setFirstInRollBackListPrecheckResponse.getCvMoAttributes().get(ShmConstants.FDN);
                activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.SET_BACKUP_FIRST_IN_ROLLBACK_LIST_EXECUTE, nodeName, cvMoFdn,
                        "SHM:" + activityJobId + ":" + nodeName);
                // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Started
                systemRecorder.recordCommand(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.SET_BACKUP_FIRST_IN_ROLLBACK_LIST_SERVICE, CommandPhase.STARTED, nodeName,
                        cvMoFdn, activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
                final Map<String, Object> actionArguments = setFirstInRollBackListPrecheckResponse.getActionArguments();
                final String configurationVersionName = (String) actionArguments.get(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Setting Configuration Version " + configurationVersionName + " as First in Rollback List", new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                final int actionId = commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV, cvMoFdn, actionArguments,
                        backupMoActionRetryPolicy.getDpsMoActionRetryPolicy());
                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
                final long actionTriggeredTime = System.currentTimeMillis();
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_TRIGGERED_TIME, String.valueOf(actionTriggeredTime));
                LOGGER.debug("ActionId for activity {} : {}", activityJobId, actionId);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Configuration Version " + configurationVersionName + " has been set first in rollback list.", new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                logMessage = "MO Action Initiated with action " + BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV + " on CV MO having FDN: " + cvMoFdn;
                LOGGER.debug("{} for activity with Id {} for node {}", logMessage, activityJobId, nodeName);
                // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Success
                systemRecorder.recordCommand(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.SET_BACKUP_FIRST_IN_ROLLBACK_LIST_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS,
                        nodeName, cvMoFdn, activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
                jobResult = JobResult.SUCCESS;
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
            } else {
                LOGGER.error("Precheck in execute failed for {} activity having activityJobId {} on node {} with result {}.", getActivityType(), activityJobId, nodeName,
                        setFirstInRollBackListPrecheckResponse.getActivityStepResultEnum());
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST);
            }
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger action for {} activity {} with activityJobId {} on node {}. Reason : {}", getActivityType(), activityJobId, nodeName, jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST, jdnfEx.getMessage()),
                    new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, null, ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST);
            return;
        } catch (final Exception e) {
            LOGGER.error("MO Action Initiation failed on executeActionOnMo for {} activity with activityJobId {} on node {}. Reason : {}", getActivityType(), activityJobId, nodeName, e);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            if (!exceptionMessage.isEmpty()) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Set First in the Rollback list activity has Failed." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Set First in the Rollback list activity has Failed", new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
            logMessage = "Unable to start MO Action with action: " + BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV + " on CV MO having FDN: " + cvMoFdn + " because " + e.getMessage();
            LOGGER.debug("{} for activity with Id {} on node {}", logMessage, activityJobId, nodeName);
            // COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Failure
            systemRecorder.recordCommand(SHMEvents.SET_BACKUP_FIRST_IN_ROLLBACK_LIST_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST);
            return;
        }
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ShmConstants.WFS_ACTIVATE_EXECUTE, null);
    }

    /**
     * This method handles timeout scenario for SetFirstInRollbackList Activity and checks the rollback list in CV MO on node to see if it is set or not.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Inside CPP SetFirstInRollbackListService.handleTimeout with activityJobId : {}", activityJobId);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            activityStepResult = processTimeout(activityJobId, neJobStaticData);
        } catch (final JobDataNotFoundException jdnfEx) {
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            LOGGER.error("Unable to trigger timeout. Reason : {}", jdnfEx);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        }
        return activityStepResult;
    }

    @SuppressWarnings("unchecked")
    private ActivityStepResult processTimeout(final long activityJobId, final NEJobStaticData neJobStaticData) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final String nodeName = neJobStaticData.getNodeName();
        JobResult jobResult = JobResult.FAILED;
        try {
            final Map<String, Object> configurationVersionMo = getConfigurationVersionMo(nodeName);
            final Map<String, Object> configurationVersionAttributes = (Map<String, Object>) configurationVersionMo.get(ShmConstants.MO_ATTRIBUTES);
            final String configurationVersionName = getConfigurationVersionName(neJobStaticData, BackupActivityConstants.ROLLBACK_CV_NAME);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Setting CV " + configurationVersionName + " as first in Rollback List has timed out.", new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.WARN.toString());
            final boolean verified = SetFirstInRollbackListService.verifyAction(configurationVersionAttributes, configurationVersionName);
            if (verified) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Configuration Version " + configurationVersionName + " has been set as first CV in Rollback List.", new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                LOGGER.info("Verified by timeout that CV {} has been set first in rollback list.", configurationVersionName);
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                jobResult = JobResult.SUCCESS;
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Set First in the Rollback list activity is Failed", new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                LOGGER.error("Verified by timeout that unable to set CV {} as first in rollback list.", configurationVersionName);
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            }
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred in processTimeout of set first in rollback list for node {} due to : {}", nodeName, ex);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        }
        activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        return activityStepResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancel(long)
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("Inside SetFirstInRollbackList cancel() with activityJobId:{}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        String nodeName = null;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            activityUtils.logCancelledByUser(jobLogList, neJobStaticData, ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to cancel set first in rollback list for {} activity having activityJobId {} on node {}. Reason : {}", getActivityType(), activityJobId, nodeName, jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while cancelling the activity {} having activityJobId {} for node {}. Reason is : {}", getActivityType(), activityJobId, nodeName, ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FAILURE_REASON, ex.getMessage()), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        // Return type will be changed to void after making cancel() asynchronous.
        return new ActivityStepResult();
    }

    /**
     * This method verifies that whether action triggered is successfully executed on the node.
     * 
     * @param configurationVersionAttributes
     * @param configurationVersionName
     * @return
     */
    @SuppressWarnings("unchecked")
    private static boolean verifyAction(final Map<String, Object> configurationVersionAttributes, final String configurationVersionName) {
        LOGGER.debug("Verify Action with CV Name {} ", configurationVersionName);
        final List<String> rollbackList = (List<String>) configurationVersionAttributes.get(ConfigurationVersionMoConstants.ROLLBACK_LIST);
        final int FIRST_ITEM_FROM_LIST = 0;
        boolean verified;
        if (!rollbackList.isEmpty() && rollbackList.get(FIRST_ITEM_FROM_LIST).equals(configurationVersionName)) {
            verified = ActivityConstants.TRUE;
        } else {
            verified = ActivityConstants.FALSE;
        }
        return verified;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity#getActivityType()
     */
    @Override
    public String getActivityType() {
        return ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity#getNotificationEventType()
     */
    @Override
    public String getNotificationEventType() {
        return SHMEvents.SET_BACKUP_FIRST_IN_ROLLBACK_LIST_EXECUTE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @SuppressWarnings("unchecked")
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        LOGGER.info("Entered into cancelTimeout of SetFirstInRollbackListService with activityJobId {}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        String nodeName = null;
        JobResult jobResult = JobResult.FAILED;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final Map<String, Object> configurationVersionMo = getConfigurationVersionMo(nodeName);
            final Map<String, Object> configurationVersionAttributes = (Map<String, Object>) configurationVersionMo.get(ShmConstants.MO_ATTRIBUTES);
            final String configurationVersionName = getConfigurationVersionName(neJobStaticData, BackupActivityConstants.ROLLBACK_CV_NAME);
            final boolean verified = verifyAction(configurationVersionAttributes, configurationVersionName);
            if (verified) {
                jobResult = JobResult.SUCCESS;
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
            }
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to cancel set first in rollback list for activity {} having activityJobId {} on node {}. Reason : {}", getActivityType(), activityJobId, nodeName, jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred in cancelTimeout for activity {} on node {}. Reason is : {}", ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST, nodeName, ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FAILURE_REASON, ex.getMessage()), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        return activityStepResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncPrecheck(long)
     */
    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        // Not needed if there is no node read calls in precheck or async threads are still not increased.
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#precheckHandleTimeout(long)
     */
    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        // Implement this when (if) precheck is made async.
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncHandleTimeout(long)
     */
    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        LOGGER.debug("Inside CPP SetFirstInRollbackListService.asyncHandleTimeout with activityJobId : {}", activityJobId);
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        NEJobStaticData neJobStaticData = null;
        String nodeName = null;
        long neJobId = 0;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            neJobId = neJobStaticData.getNeJobId();
            activityStepResultEnum = processTimeout(activityJobId, neJobStaticData).getActivityResultEnum();
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger timeout for {} activity having activityJobId {} on node {}. Reason : {}", getActivityType(), activityJobId, nodeName, jdnfEx);
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        }
        LOGGER.info("Sending back ActivityStepResult to WorkFlow from CPP SetFirstInRollbackListService.asyncHandleTimeout with result : {} for node {} with activityJobId {} and neJobId {}",
                activityStepResultEnum, nodeName, activityJobId, neJobId);
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST, activityStepResultEnum);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#timeoutForAsyncHandleTimeout(long)
     */
    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.info("Failing the activity and sending back to WorkFlow from CPP SetFirstInRollbackListService.timeoutForAsyncHandleTimeout for activityJobId : {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.SET_FIRST_IN_ROLLBACK_LIST);
    }

}
