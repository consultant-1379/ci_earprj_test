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

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;

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
import com.ericsson.oss.services.shm.common.exception.BackupDataNotFoundException;
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
import com.ericsson.oss.services.shm.es.impl.cpp.common.SetStartableActivityHandler;
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
 * This class facilitates the setting of configuration version as startable CV in CV MO of CPP based node by invoking the ConfigurationVersion MO action.
 *
 * @author tcsrohc
 *
 */
@EServiceQualifier("CPP.BACKUP.setcvasstartable")
@ActivityInfo(activityName = "setcvasstartable", jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SetStartableCvService extends AbstractBackupActivity implements Activity, AsynchronousActivity {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetStartableCvService.class);

    @Inject
    private SetStartableActivityHandler setStartableActivityHandler;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    /**
     * This method validates the status of CV to decide if setcvasstartable activity can be started or not and sends back the activity result to Work Flow Service.
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
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> emptyMap = new HashMap<String, Object>();
        final String nodeName = neJobStaticData.getNodeName();
        try {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.SET_AS_STARTABLE_CV), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
            final Map<String, Object> moAttributesMap = getConfigurationVersionMo(nodeName);
            if (moAttributesMap == null || moAttributesMap.isEmpty()) {
                LOGGER.error("No CV MO found for activity {} on node {}", activityJobId, nodeName);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.MO_NOT_EXIST, ActivityConstants.SET_AS_STARTABLE_CV, BackupActivityConstants.CV_MO_TYPE), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                precheckResponse = new CppBackupPrecheckResponse(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, emptyMap, emptyMap);
                return precheckResponse;
            }
            final String cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
            final String configurationVersionName = getConfigurationVersionName(neJobStaticData, BackupActivityConstants.STARTABLE_CV_NAME);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.SET_AS_STARTABLE_CV), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            final String logMessage = "Proceeding SetStartable with CV " + configurationVersionName;
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);
            LOGGER.info("{} for {} activity having activityJobId {}", logMessage, getActivityType(), activityJobId);
            activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.SET_STARTABLE_BACKUP_PRECHECK, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            final Map<String, Object> cvNameActionArgument = new HashMap<String, Object>();
            cvNameActionArgument.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, configurationVersionName);
            precheckResponse = new CppBackupPrecheckResponse(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, moAttributesMap, cvNameActionArgument);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred in precheck of set first in rollback list for node {} due to: ", nodeName, ex);
            String exceptionMessage = ExceptionParser.getReason(ex);
            if (exceptionMessage == null || exceptionMessage.isEmpty()) {
                exceptionMessage = ex.getMessage();
            }
            final String logMessage = getActivityType() + " Precheck failed." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            precheckResponse = new CppBackupPrecheckResponse(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, emptyMap, emptyMap);
        }
        activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
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
        String cvMoFdn = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        String configurationVersionName = null;
        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        String neJobBusinessKey = "";

        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final String nodeName = neJobStaticData.getNodeName();
            neJobBusinessKey = neJobStaticData.getNeJobBusinessKey();
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.SET_AS_STARTABLE_ACTIVITY_NAME);
            if (!isUserAuthorized && neJobStaticData != null) {
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.SET_AS_STARTABLE_CV);
                return;
            }

            //precheck Validation
            final CppBackupPrecheckResponse setStartableCVPrecheckResponse = getPrecheckResponse(activityJobId, neJobStaticData);
            if (setStartableCVPrecheckResponse.getActivityStepResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
                LOGGER.debug("Executing {} activity for activityJobId {} on node {}", getActivityType(), activityJobId, nodeName);
                final Map<String, Object> moAttributesMap = setStartableCVPrecheckResponse.getCvMoAttributes();
                cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
                configurationVersionName = (String) setStartableCVPrecheckResponse.getActionArguments().get(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME);
                final boolean performActionStatus = setStartableActivityHandler.executeSetStartableMoAction(activityJobId, neJobStaticData, configurationVersionName, cvMoFdn);
                doExecutePostValidation(activityJobId, neJobStaticData, jobLogList, performActionStatus, cvMoFdn);
            } else {
                LOGGER.error("Precheck in execute failed for {} activity having activityJobId {} on node {} with result {}.", getActivityType(), activityJobId, nodeName,
                        setStartableCVPrecheckResponse.getActivityStepResultEnum());
                activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, getActivityType());
            }
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger action for {} activity having activityJobId {}. Reason : {}", getActivityType(), activityJobId, jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, ActivityConstants.SET_AS_STARTABLE_CV, jdnfEx.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, null, getActivityType());
        } catch (final Exception ex) {
            LOGGER.error("Failed to start MO Action : " + BackupActivityConstants.ACTION_SET_STARTABLE_CV + " on CV MO having FDN : " + cvMoFdn + " because " + ex.getMessage());
            String resultMessage = String.format(JobLogConstants.SET_STARTABLE_ACTION_FAILED, configurationVersionName);
            if (!resultMessage.isEmpty()) {
                resultMessage = resultMessage + String.format(JobLogConstants.FAILURE_REASON, ex.getMessage());
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, resultMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobBusinessKey, ActivityConstants.SET_AS_STARTABLE_CV);
        }
    }

    private void doExecutePostValidation(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList, final boolean isPerformActionSuccess,
            final String cvMoFdn) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        if (isPerformActionSuccess) {
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, MOACTION_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
        } else {
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            systemRecorder.recordCommand(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.SET_STARTABLE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_ERROR,
                    neJobStaticData.getNodeName(), cvMoFdn, activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.BACKUP));
        }
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, getActivityType(), null);
    }

    /**
     * This method handles timeout scenario for SetStartableCvService Activity and checks the startableCv in CV MO on node to see if it is set or not.
     *
     * @param activityJobId
     * @return ActivityStepResult
     *
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            activityStepResult = setStartableActivityHandler.handleTimeoutSetStartableAction(activityJobId, neJobStaticData);
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger timeout for {} activity having activityJobId {}. Reason : {}", getActivityType(), activityJobId, jdnfEx);
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        }
        return activityStepResult;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancel(long)
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("Inside SetStartableCvService cancel() with activityJobId:{}", activityJobId);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            return setStartableActivityHandler.cancelSetStartableAction(activityJobId, neJobStaticData);
        } catch (final JobDataNotFoundException jdnfEx) {
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            LOGGER.error("Unable to cancel set cv as startable for {} activity having activityJobId {}. Reason : {}", getActivityType(), activityJobId, jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            return new ActivityStepResult();
        }
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

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        LOGGER.debug("Entered into cancelTimeout of SetStartableCvService with activityJobId {}", activityJobId);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        JobResult jobResult = JobResult.FAILED;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            activityStepResult = setStartableActivityHandler.cancelTimeoutSetStartable(neJobStaticData, finalizeResult);
            if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS) {
                jobResult = JobResult.SUCCESS;
            }
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to complete cancel timeout for {} activity having activityJobId {}. Reason : {}", getActivityType(), activityJobId, jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final BackupDataNotFoundException e) {
            LOGGER.error("Unable to complete cancel timeout for {} activity having activityJobId {}. Reason : {}", getActivityType(), activityJobId, e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.FAILURE_REASON, e.getMessage()), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
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
        LOGGER.debug("Inside CPP SetStartableCvService.asyncHandleTimeout with activityJobId : {}", activityJobId);
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        NEJobStaticData neJobStaticData = null;
        String nodeName = null;
        long neJobId = 0;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            neJobId = neJobStaticData.getNeJobId();
            activityStepResultEnum = setStartableActivityHandler.handleTimeoutSetStartableAction(activityJobId, neJobStaticData).getActivityResultEnum();
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger timeout for {} activity having activityJobId {} for node {}. Reason : {}", getActivityType(), activityJobId, nodeName, jdnfEx);
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        }
        LOGGER.info("Sending back ActivityStepResult to WorkFlow from CPP SetStartableCvService.asyncHandleTimeout with result : {} for node {} with activityJobId {} and neJobId {}",
                activityStepResultEnum, nodeName, activityJobId, neJobId);
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.SET_AS_STARTABLE_CV, activityStepResultEnum);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#timeoutForAsyncHandleTimeout(long)
     */
    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.info("Failing the activity and sending back to WorkFlow from CPP SetStartableCvService.timeoutForAsyncHandleTimeout for activityJobId : {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.SET_AS_STARTABLE_CV);
    }

}
