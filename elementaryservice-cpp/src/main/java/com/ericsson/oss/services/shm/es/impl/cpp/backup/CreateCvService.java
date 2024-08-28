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
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
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
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * This class facilitates the creation of configuration version of CPP based node by invoking the ConfigurationVersion MO action that initializes the create activity.
 *
 * @author tcsrohc
 *
 */
@EServiceQualifier("CPP.BACKUP.createcv")
@ActivityInfo(activityName = "createcv", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CreateCvService extends AbstractBackupActivity implements Activity, AsynchronousActivity {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCvService.class);

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    protected NetworkElementRetrievalBean networkElementRetrievalBean;

    /**
     * This method validates the node is in sync with OSS to decide if create activity can be started or not and sends back the activity result to Work Flow Service.
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
            LOGGER.error(String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.CREATE_CV, jdnfEx.getMessage()));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.CREATE_CV, jdnfEx.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
        return activityStepResult;
    }

    private static boolean isCvMOExist(final String configVersionMOFdn) {
        if (configVersionMOFdn != null) {
            return true;
        }
        return false;
    }

    /**
     * This method updates the NE Job properties for given NE Job.
     *
     * @param neJobId
     * @param neJobAttributes
     * @param cvName
     */
    private void updateNeJobProperty(final long neJobId, final Map<String, Object> neJobAttributes, final String cvName) {
        LOGGER.debug("Updating NE Job property for : {} with attributes {}", neJobId, neJobAttributes);
        final List<Map<String, Object>> neJobPropertiesList = new ArrayList<Map<String, Object>>();
        activityUtils.prepareJobPropertyList(neJobPropertiesList, BackupActivityConstants.CV_NAME, cvName);
        activityUtils.prepareJobPropertyList(neJobPropertiesList, BackupActivityConstants.STARTABLE_CV_NAME, cvName);
        activityUtils.prepareJobPropertyList(neJobPropertiesList, BackupActivityConstants.ROLLBACK_CV_NAME, cvName);
        activityUtils.prepareJobPropertyList(neJobPropertiesList, BackupActivityConstants.UPLOAD_CV_NAME, cvName);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobPropertiesList);
        LOGGER.debug("Updating NE Job property {} exit", neJobPropertiesList);
        jobUpdateService.readAndUpdateRunningJobAttributes(neJobId, neJobPropertiesList, null, null);
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
        NEJobStaticData neJobStaticData = null;
        boolean actionPerformed = false;
        String nodeName = "";
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        JobStaticData jobStaticData = null;
        String neJobBusinessKey = "";
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            neJobBusinessKey = neJobStaticData.getNeJobBusinessKey();
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.CREATE_CV_ACTIVITY_NAME);
            if (!isUserAuthorized) {
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.CREATE_CV);
                return;
            }

            //precheck Validation
            final CppBackupPrecheckResponse createCVPrecheckResponse = getPrecheckResponse(activityJobId, neJobStaticData);
            if (createCVPrecheckResponse.getActivityStepResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
                final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
                LOGGER.debug("Executing CreateCV for activityJobId : {} on node : {}", activityJobId, nodeName);
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, MOACTION_START_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                actionPerformed = performAction(activityJobId, neJobStaticData, createCVPrecheckResponse, jobLogList);
                doExecutePostValidation(actionPerformed, activityJobId, neJobStaticData, createCVPrecheckResponse, jobLogList, jobPropertyList);
                activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ShmConstants.WFS_ACTIVATE_EXECUTE, null);
            } else {
                LOGGER.error("Precheck in execute failed for {} activity having activityJobId {} on node {} with result {}.", getActivityType(), activityJobId, nodeName,
                        createCVPrecheckResponse.getActivityStepResultEnum());
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, getActivityType());
            }

        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger action. Reason : {}", jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, ActivityConstants.CREATE_CV, jdnfEx.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.CREATE_CV);
        } catch (final Exception exception) {
            final String exceptionMessage = ExceptionParser.getReason(exception);
            if (!actionPerformed) {
                LOGGER.error("Exception occurred in execute of Create CV on node {} due to : {}", nodeName, exception);
                final String logMessage = getActivityType() + " execute failed." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.CREATE_CV);
            }
        }
    }

    private CppBackupPrecheckResponse getPrecheckResponse(final long activityJobId, final NEJobStaticData neJobStaticData) {
        ActivityStepResultEnum precheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        CppBackupPrecheckResponse precheckResponse = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> emptyMap = new HashMap<String, Object>();
        final String nodeName = neJobStaticData.getNodeName();
        final long neJobId = neJobStaticData.getNeJobId();
        try {
            initiateActivity(activityJobId, neJobId, nodeName, jobLogList);

            final String configVersionMOFdn = getCVMoFdn(nodeName);
            if (!CreateCvService.isCvMOExist(configVersionMOFdn)) {
                final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
                LOGGER.error("No CV MO found for FDN {} with activity with Id : {}", configVersionMOFdn, activityJobId);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.MO_NOT_EXIST, ActivityConstants.CREATE_CV, BackupActivityConstants.CV_MO_TYPE), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
                precheckResponse = new CppBackupPrecheckResponse(precheckStatus, emptyMap, emptyMap);
                return precheckResponse;
            }
            final Map<String, Object> neJobAttributes = jobConfigServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId());
            final Map<String, Object> actionArguments = prepareActionArguments(neJobStaticData);
            String cvName = (String) actionArguments.get(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME);
            final String cvType = (String) actionArguments.get(ConfigurationVersionMoConstants.ACTION_ARG_TYPE);
            final String operatorName = (String) actionArguments.get(ConfigurationVersionMoConstants.ACTION_ARG_OPERATOR_NAME);
            LOGGER.debug("cvName : {}, cvType : {}, operatorName : {}", cvName, cvType, operatorName);
            if (cvName == null || ActivityConstants.EMPTY.equals(cvName) || cvType == null || ActivityConstants.EMPTY.equals(cvType) || operatorName == null
                    || ActivityConstants.EMPTY.equals(operatorName)) {
                final String logMessage = String.format(JobLogConstants.INSUFFICIENT_INPUTS, ActivityConstants.CREATE_CV);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                LOGGER.debug("{} for activity with Id{}", logMessage, activityJobId);
                //Persist Result as Failed in case of unable to trigger action.
                final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
                precheckResponse = new CppBackupPrecheckResponse(precheckStatus, emptyMap, emptyMap);
                return precheckResponse;
            }
            updateNeJobProperty(neJobId, neJobAttributes, cvName);
            activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.CREATE_BACKUP_PRECHECK, nodeName, configVersionMOFdn,
                    "SHM:" + activityJobId + ":" + nodeName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.CREATE_CV), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);
            precheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
            final Map<String, Object> cvMoFdnMap = new HashMap<>();
            cvMoFdnMap.put(ShmConstants.FDN, configVersionMOFdn);
            precheckResponse = new CppBackupPrecheckResponse(precheckStatus, cvMoFdnMap, actionArguments);
        } catch (final Exception exception) {
            LOGGER.error("Exception occurred in precheck of Create Backup for node {} due to : {}", nodeName, exception);
            String exceptionMessage = ExceptionParser.getReason(exception);
            if (exceptionMessage == null || exceptionMessage.isEmpty()) {
                exceptionMessage = exception.getMessage();
            }
            final String logMessage = getActivityType() + " Precheck failed." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            precheckResponse = new CppBackupPrecheckResponse(precheckStatus, emptyMap, emptyMap);
        }
        activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        return precheckResponse;
    }

    private void initiateActivity(final long activityJobId, final long neJobId, final String nodeName, final List<Map<String, Object>> jobLogList) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.CREATE_CV), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        final String treatAsInfo = activityUtils.isTreatAs(nodeName);
        if (treatAsInfo != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
    }

    private void doExecutePostValidation(final boolean isActionPerformed, final long activityJobId, final NEJobStaticData neJobStaticData, final CppBackupPrecheckResponse precheckResponse,
            final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList) {
        JobResult jobResult = null;
        final String nodeName = neJobStaticData.getNodeName();
        final Map<String, Object> moAttributesMap = precheckResponse.getCvMoAttributes();
        final String cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
        if (isActionPerformed) {
            jobResult = JobResult.SUCCESS;
            final long mainJobId = neJobStaticData.getMainJobId();
            systemRecorder.recordCommand(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
            final String configurationVersionName = (String) precheckResponse.getActionArguments().get(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CV_CREATED_SUCCESSFULLY, configurationVersionName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
        } else {
            jobResult = JobResult.FAILED;
            systemRecorder.recordCommand(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR,
                    neJobStaticData.getNodeName(), cvMoFdn, activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
        }
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        jobLogList.clear();
    }

    /**
     * This method is responsible for performing the action on the node.
     *
     * @param activityJobId
     * @param neJobStaticData
     * @param precheckResponse
     * @param jobLogList
     * @return
     */
    private boolean performAction(final long activityJobId, final NEJobStaticData neJobStaticData, final CppBackupPrecheckResponse precheckResponse, final List<Map<String, Object>> jobLogList) {
        boolean isActionPerformed = false;
        int actionId = -1;
        final String nodeName = neJobStaticData.getNodeName();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> moAttributesMap = precheckResponse.getCvMoAttributes();
        final String cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
        String jobLogMessage = "Unable to start MO Action with action \"" + BackupActivityConstants.ACTION_CREATE_CV + "\" on CV MO having FDN: " + cvMoFdn;
        final Map<String, Object> actionArguments = precheckResponse.getActionArguments();
        final String configurationVersionName = (String) actionArguments.get(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME);
        try {
            activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.CREATE_BACKUP_EXECUTE, nodeName, cvMoFdn, "SHM:" + activityJobId + ":" + nodeName);
            //COM_ERICSSON_OSS_ITPF_COMMAND_LOGGER-Started
            systemRecorder.recordCommand(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.CREATE_BACKUP_SERVICE, CommandPhase.STARTED, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
            actionId = commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_CREATE_CV, cvMoFdn, actionArguments, backupMoActionRetryPolicy.getDpsMoActionRetryPolicy());
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
            LOGGER.debug("ActionId for activity {} is {}", activityJobId, actionId);
            final long actionTriggeredTime = System.currentTimeMillis();
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTION_TRIGGERED_TIME, String.valueOf(actionTriggeredTime));
            isActionPerformed = true;
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
            jobLogMessage = "MO Action Initiated with action \"" + BackupActivityConstants.ACTION_CREATE_CV + "\" on CV MO having FDN: " + cvMoFdn;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Triggered creation of Configuration Version having name " + configurationVersionName, new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
        } catch (final RetriableCommandException exception) {
            final String exceptionMessage = exception.getCause().getMessage();
            LOGGER.error("RetriableException occurred : {}. Retrying... ", exception.getMessage());
            if (!exceptionMessage.isEmpty()) {
                jobLogMessage = String.format(JobLogConstants.UNABLE_TO_CREATE_CV, configurationVersionName) + " " + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            } else {
                jobLogMessage = String.format(JobLogConstants.UNABLE_TO_CREATE_CV, configurationVersionName) + " " + String.format(JobLogConstants.FAILURE_REASON, exception.getMessage());
            }
        }
        LOGGER.debug("isActionPerformed is {} for {} activity with activityJobId {} for node {} ", isActionPerformed, getActivityType(), activityJobId, nodeName);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.CREATE_BACKUP_EXECUTE, nodeName, cvMoFdn,
                activityUtils.additionalInfoForEvent(activityJobId, nodeName, jobLogMessage));
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        jobLogList.clear();
        return isActionPerformed;
    }

    /**
     * This method handles timeout scenario for Create Cv Activity and checks the stored configuration version in CV MO on node to see if CV is created or not.
     *
     * @param activityJobId
     * @return ActivityStepResult
     *
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Inside CPP CreateCvService.handleTimeout with activityJobId : {}", activityJobId);
        return processTimeout(activityJobId, null);
    }

    @SuppressWarnings("unchecked")
    private ActivityStepResult processTimeout(final long activityJobId, NEJobStaticData neJobStaticData) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        JobResult jobResult;
        boolean isCvCreated = false;
        String nodeName = null;
        try {
            if (neJobStaticData == null) {
                neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            }
            nodeName = neJobStaticData.getNodeName();
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
            final Map<String, Object> configurationVersionMo = getConfigurationVersionMo(nodeName);
            final String cvMoFdn = (String) configurationVersionMo.get(ShmConstants.FDN);
            final Map<String, Object> configurationVersionMoAttributes = (Map<String, Object>) configurationVersionMo.get(ShmConstants.MO_ATTRIBUTES);
            final String logMessage = "Creation of ConfigurationVersion Timed out.";
            final String configurationVersionName = getConfigurationVersionName(neJobStaticData, BackupActivityConstants.CV_NAME);
            isCvCreated = CreateCvService.verifyAction(configurationVersionMoAttributes, configurationVersionName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.CREATE_BACKUP_TIME_OUT, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            if (isCvCreated) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CV_CREATED_SUCCESSFULLY, configurationVersionName), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
                LOGGER.info("Verified by timeout that CV {} has been created on node {}.", configurationVersionName, nodeName);
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
                jobResult = JobResult.SUCCESS;
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.UNABLE_TO_CREATE_CV, configurationVersionName), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
                LOGGER.info("Verified by timeout that CV Creation has failed. CV {} has not been created on node {}.", configurationVersionName, nodeName);
                jobResult = JobResult.FAILED;
            }
            LOGGER.debug("{} for activity with Id {}", logMessage, activityJobId);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger timeout. Reason : {}", jdnfEx);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            return activityStepResult;
        } catch (final Exception exception) {
            String exceptionMessage = ExceptionParser.getReason(exception);
            if (exceptionMessage == null || exceptionMessage.isEmpty()) {
                exceptionMessage = exception.getMessage();
            }
            final String logMessage = getActivityType() + " handleTimeout failed." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            if (!isCvCreated) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
                LOGGER.error("Exception occurred in handleTimeout of Create Backup for node {} due to : ", nodeName, exception);
            } else {
                LOGGER.warn("Cv created Successfully but exception occurred for node {} due to : {}", nodeName, exception);
            }
        }
        if (neJobStaticData != null) {
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
        }
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
        LOGGER.debug("Inside Create_CV Service cancel() with activityJobId : {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        String nodeName = null;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            activityUtils.logCancelledByUser(jobLogList, neJobStaticData, ActivityConstants.CREATE_CV);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, ActivityConstants.CREATE_CV), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.WARN.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to cancel create cv job. Reason : {}", jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred in CreateCvService.cancel for node {}. Reason is : {}", nodeName, ex);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FAILURE_REASON, ex.getMessage()), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        return new ActivityStepResult();
    }

    /**
     * This method prepares all the arguments needed for trigger the create cv action on the node.
     *
     * @param activityJobId
     * @param cvMoAttr
     * @return
     * @throws JobDataNotFoundException
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> prepareActionArguments(final NEJobStaticData neJobStaticData) throws JobDataNotFoundException {
        final Map<String, Object> mainJobAttributes = jobConfigServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
        final String nodeName = neJobStaticData.getNodeName();
        final long neJobId = neJobStaticData.getNeJobId();
        final String platformType = neJobStaticData.getPlatformType();
        final long templateJobId = (long) mainJobAttributes.get(ShmConstants.JOBTEMPLATEID);
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final Map<String, Object> templateJobAttr = jobUpdateService.retrieveJobWithRetry(templateJobId);
        LOGGER.debug("TemplateJobAttr for activity {} is {}", neJobId, templateJobAttr);
        String operatorName = (String) templateJobAttr.get(ShmConstants.OWNER);

        //Getting Configuration Version name from NE Job properties
        final String configurationVersionName = getConfigurationVersionName(neJobStaticData, BackupActivityConstants.CV_NAME);
        String neType = null;
        try {
            neType = networkElementRetrievalBean.getNeType(nodeName);
        } catch (final MoNotFoundException moNotFoundEx) {
            LOGGER.error("Exception while fetching neType of node :  {}. Reason : {}", nodeName, moNotFoundEx.getMessage());
        }
        LOGGER.debug("PrepareActionArguments NeType {}, platform {} ", neType, platformType);
        final List<String> keyList = new ArrayList<String>();
        keyList.add(BackupActivityConstants.CV_IDENTITY);
        keyList.add(BackupActivityConstants.CV_TYPE);
        keyList.add(BackupActivityConstants.CV_COMMENT);
        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, nodeName, neType, platformType);
        String identity = keyValueMap.get(BackupActivityConstants.CV_IDENTITY);
        identity = identity != null ? CreateCvService.truncateString(identity) : "";
        String cvComment = keyValueMap.get(BackupActivityConstants.CV_COMMENT);
        if (cvComment == null) {
            cvComment = "";
        }
        cvComment = CreateCvService.truncateString(cvComment);
        final String cvTypeValue = keyValueMap.get(BackupActivityConstants.CV_TYPE);
        LOGGER.debug("ConfigurationVersionName={}, Identity={}, CvTypeValue={} for activity={}", configurationVersionName, identity, cvTypeValue, neJobId);
        String cvType = null;
        if (ConfigurationVersionType.getCvType(cvTypeValue) != null) {
            cvType = ConfigurationVersionType.getCvType(cvTypeValue).toString();
        }
        LOGGER.debug("CvType for activity {} is {}", neJobId, cvType);
        LOGGER.debug("Validating operator Name for acitivity {}", neJobId);
        operatorName = CreateCvService.truncateString(operatorName);
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, configurationVersionName);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_IDENTITY, identity);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_TYPE, cvType);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_OPERATOR_NAME, operatorName);
        actionArguments.put(ConfigurationVersionMoConstants.ACTION_ARG_COMMENT, cvComment);
        return actionArguments;
    }

    /**
     * This method verifies that whether action triggered is successfully executed on the node.
     *
     * @param cvMoAttributes
     * @param configurationVersionName
     * @return
     */
    @SuppressWarnings("unchecked")
    private static boolean verifyAction(final Map<String, Object> cvMoAttributes, final String configurationVersionName) {
        final List<Map<String, String>> storedConfigurationVersionList = (List<Map<String, String>>) cvMoAttributes.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION);
        LOGGER.debug("StoredConfigurationVersionList is {}", storedConfigurationVersionList);
        boolean verified = ActivityConstants.FALSE;
        String cvNameStoredConfigurationVersion;
        for (final Map<String, String> storedConfigurationVersion : storedConfigurationVersionList) {
            cvNameStoredConfigurationVersion = storedConfigurationVersion.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME);
            if (cvNameStoredConfigurationVersion.equals(configurationVersionName)) {
                verified = ActivityConstants.TRUE;
            }
        }
        LOGGER.info("Create CV verified: {}", verified);
        return verified;
    }

    /**
     * This method truncates the item size to {@code CV_NAME_MAX_LIMIT}.
     *
     * @param actionArgument
     * @return
     */
    private static String truncateString(final String actionArgument) {
        String truncatedActionArgument = actionArgument;
        LOGGER.debug("Before truncation : {} ", actionArgument);
        if (actionArgument != null && actionArgument.length() > BackupActivityConstants.CV_NAME_MAX_CHAR_LIMIT) {
            truncatedActionArgument = actionArgument.substring(0, BackupActivityConstants.CV_NAME_MAX_CHAR_LIMIT);
        }
        LOGGER.debug("After truncation : {}", truncatedActionArgument);
        return truncatedActionArgument;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity#getActivityType()
     */
    @Override
    public String getActivityType() {
        return ActivityConstants.CREATE_CV;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity#getNotificationEventType()
     */
    @Override
    public String getNotificationEventType() {
        return SHMEvents.CREATE_BACKUP_EXECUTE;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @SuppressWarnings("unchecked")
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        LOGGER.info("Entered into cancelTimeout of CreateCvService with activityJobId {}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        String nodeName = null;
        JobResult jobResult = JobResult.FAILED;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            final Map<String, Object> configurationVersionMo = getConfigurationVersionMo(nodeName);
            final Map<String, Object> configurationVersionMoAttributes = (Map<String, Object>) configurationVersionMo.get(ShmConstants.MO_ATTRIBUTES);
            final String configurationVersionName = getConfigurationVersionName(neJobStaticData, BackupActivityConstants.CV_NAME);
            final boolean isCvCreated = CreateCvService.verifyAction(configurationVersionMoAttributes, configurationVersionName);
            if (isCvCreated) {
                jobResult = JobResult.SUCCESS;
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
            }
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to complete cancel timeout. Reason : {}", jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred in CreateCvService.cancelTimeout for node {}. Reason is : {}", nodeName, ex);
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
        LOGGER.debug("Inside CPP CreateCvService.asyncHandleTimeout with activityJobId : {}", activityJobId);
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
            LOGGER.error("Unable to trigger timeout. Reason : {}", jdnfEx);
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
        LOGGER.info("Sending back ActivityStepResult to WorkFlow from CPP CreateCvService.asyncHandleTimeout with result : {} for node {} with activityJobId {} and neJobId {}", activityStepResultEnum,
                nodeName, activityJobId, neJobId);
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.CREATE_CV, activityStepResultEnum);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#timeoutForAsyncHandleTimeout(long)
     */
    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.info("Failing Create CV activity and sending back to WorkFlow from CPP CreateCvService.timeoutForAsyncHandleTimeout for activityJobId : {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.CREATE_CV);
    }

}
