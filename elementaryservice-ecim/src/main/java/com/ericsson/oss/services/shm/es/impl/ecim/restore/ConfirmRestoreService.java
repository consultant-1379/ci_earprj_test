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
package com.ericsson.oss.services.shm.es.impl.ecim.restore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupUtils;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.ecim.common.BackupMOInformationProvider;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

/**
 * This elementary service class confirms the Restore Backup activity on ECIM node.
 * 
 * @author xyerrsr
 * 
 */

@EServiceQualifier("ECIM.RESTORE.confirmbackup")
@ActivityInfo(activityName = "confirmbackup", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.ECIM)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ConfirmRestoreService implements Activity {

    @Inject
    BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Inject
    ActivityUtils activityUtils;

    @Inject
    JobUpdateService jobUpdateService;

    @Inject
    EcimBackupUtils ecimBackupUtils;

    @Inject
    private FragmentVersionCheck fragmentVersionCheck;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private BackupMOInformationProvider backupMOInformationProvider;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmRestoreService.class);
    private static final String CONFIRM_RESTORE = "Confirm Restore";
    private static final String EXCEPTION_DURING_ACTIVITY_EXECUTE = "Unable to proceed with confirm backup activity on node %s because \"%s\". ";
    private static final String PRODUCT_NUMBER = "productNumber";
    private static final String PRODUCT_REVISION = "productRevision";
    private static final String RESTORED_SW_VERSION_JOB_LOG = "Node is restored to identity : %s revision : %s";

    /**
     * This method performs the PreCheck for ECIM Restore ConfirmRestore activity that includes the existence of BrmRollbackAtRestore MO and the verification of the last restored backup on
     * BrmBackupLabelStore MO, if exists.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        String jobLogMessage = null;
        String logMessage = null;
        String errorMessage = null;
        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        String nodeName = "";
        long activityStartTime = 0L;
        LOGGER.debug("Precheck for {} activity started on the node : {}.", CONFIRM_RESTORE, nodeName);
        final String treatAsInfo = activityUtils.isTreatAs(nodeName, FragmentType.ECIM_BRM_TYPE, SHMCapabilities.RESTORE_JOB_CAPABILITY);
        if (treatAsInfo != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            activityStartTime = neJobStaticData.getActivityStartTime();
            nodeName = neJobStaticData.getNodeName();
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, EcimBackupConstants.CONFIRM_RESTORE);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackupInfoForRestore(neJobStaticData, networkElement);
            activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_INITIATED, CONFIRM_RESTORE) + String.format(JobLogConstants.BACKUP_NAME, ecimBackupInfo.getBackupName()),
                    JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.INFO.toString());
            final String inputVersion = brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName);
            logMessage = fragmentVersionCheck.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion);
            if (logMessage != null) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }
        } catch (final MoNotFoundException | UnsupportedFragmentException | JobDataNotFoundException ex) {
            LOGGER.error("Exception occurred in precheck() of Restore ,Reason : {}", ex);
            logMessage = ex.getMessage();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred in precheck() of Restore ,Reason : {}", ex);
            logMessage = ex.getMessage();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        try {
            //Check whether BrmRollbackAtRestore MO exits. If so, proceed for pre-check. Else, skip the activity execution.
            if (brmMoServiceRetryProxy.isConfirmRequired(nodeName)) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_SUCCESS, CONFIRM_RESTORE), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
                logMessage = String.format("Proceeding with Confirm backup activity execution on the node %s with activityJobId %s. ", nodeName, activityJobId);
            } else { // BrmRollbackAtRestore MO does not exist. So, skip execution.
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_SKIP, CONFIRM_RESTORE, JobLogConstants.BRMROLLBACKATRESTORE_NOT_FOUND), new Date(),
                        JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                logMessage = String.format("Precheck for Confirm backup activity is successful, but execution has to be skipped on the node %s with activity job id %s. ", nodeName, activityJobId);
            }
        } catch (final MoNotFoundException moNotFoundException) {
            jobLogMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, CONFIRM_RESTORE, moNotFoundException.getMessage());
            errorMessage = String.format("Unable to proceed with confirm backup activity on node %s because \"%s\". ", nodeName, moNotFoundException.getMessage());
            return handlePrecheckFailure(activityJobId, nodeName, jobLogMessage, errorMessage, jobLogList);
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            errorMessage = String.format("Unable to proceed with confirm backup activity on node %s because \"%s\". ", nodeName, unsupportedFragmentException.getMessage());
            return handlePrecheckFailure(activityJobId, nodeName, jobLogMessage, errorMessage, jobLogList);
        }

        LOGGER.debug(logMessage);
        activityUtils.recordEvent(SHMEvents.CONFIRM_RESTORE_PRECHECK, nodeName, null, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
        LOGGER.debug("Completed {} precheck with activity job id: {}, NodeName: {}, with preCheckStatus = {}", CONFIRM_RESTORE, activityJobId, nodeName, activityStepResult);
        if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION
                || activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION) {
            LOGGER.debug("Skipping persisting step duration as activity is to be skipped.");
        } else {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        }
        return activityStepResult;
    }

    /**
     * This method handles pre-check failure during confirmation activity on a restore job.
     * 
     * @param activityJobId
     * @param nodeName
     * @param jobLogMessage
     * @param errorMessage
     * @param jobLogList
     */
    private ActivityStepResult handlePrecheckFailure(final long activityJobId, final String nodeName, final String jobLogMessage, final String errorMessage, final List<Map<String, Object>> jobLogList) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        LOGGER.error(errorMessage);
        activityUtils.recordEvent(SHMEvents.CONFIRM_RESTORE_PRECHECK, nodeName, null, activityUtils.additionalInfoForEvent(activityJobId, nodeName, errorMessage));
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
        return activityStepResult;
    }

    /**
     * This method performs the MO action and sends the activity result back to Work Flow Service
     * 
     * @param activityJobId
     */
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        String brmRollbackAtRestoreMoFdn = null;
        int actionInvocationResult = -1;
        String jobLogMessage = null;
        String logMessage = null;
        String errorMessage = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        JobResult jobResult = JobResult.FAILED;
        NEJobStaticData neJobStaticData = null;
        long activityStartTime = 0;
        String nodeName = "";
        LOGGER.info("Inside execute method of confirmrestore activity of Restore job on node {}.", nodeName);
        String businessKey = "";
        //Log a message regarding the start of job execution.
        logMessage = String.format(JobLogConstants.EXECUTING, CONFIRM_RESTORE);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
        jobLogList.clear();
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            activityStartTime = neJobStaticData.getActivityStartTime();
            businessKey = neJobStaticData.getNeJobBusinessKey();
            //Get BrmRollbackAtRestore MO FDN.
            brmRollbackAtRestoreMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(ActivityConstants.CONFIRM_BACKUP, nodeName, null);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGERED, CONFIRM_RESTORE), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityUtils.recordEvent(SHMEvents.CONFIRM_RESTORE_EXECUTE, nodeName, brmRollbackAtRestoreMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));

            //Trigger MO Action on BrmRollbackAtRestore.
            actionInvocationResult = brmMoServiceRetryProxy.executeMoAction(nodeName, null, brmRollbackAtRestoreMoFdn, ActivityConstants.CONFIRM_BACKUP);

            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getBackupInfoForRestore(neJobStaticData, networkElement);
            final List<Map<String, String>> swVersions = backupMOInformationProvider.getswVersionsListFromBrmBackupMOsList(networkElement, ecimBackupInfo);
            LOGGER.debug("List of software versions from Backup MO List with required BackupName : {} ", swVersions);
            final String restoredRevision = swVersions.get(0).get(PRODUCT_REVISION);
            final String restoredIdentity = swVersions.get(0).get(PRODUCT_NUMBER);

            if (actionInvocationResult == 0) {
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
                jobResult = JobResult.SUCCESS;
                logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, CONFIRM_RESTORE);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                LOGGER.info("Execution of {} activity completed successfully on the node : {} with activityJobId : {} ", CONFIRM_RESTORE, nodeName, activityJobId);
            } else {
                jobResult = JobResult.FAILED;
                logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, CONFIRM_RESTORE);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                LOGGER.error("Execution of {} activity failed on the node : {} with activityJobId : {} ", CONFIRM_RESTORE, nodeName, activityJobId);
            }

            logMessage = String.format(RESTORED_SW_VERSION_JOB_LOG, restoredIdentity, restoredRevision);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

            //Record event.
            activityUtils.recordEvent(SHMEvents.CONFIRM_RESTORE_EXECUTE, nodeName, brmRollbackAtRestoreMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));

        } catch (final MoNotFoundException moNotFoundException) {
            jobLogMessage = String.format(JobLogConstants.NETWORKELEMENT_NOT_FOUND, nodeName);
            errorMessage = String.format(EXCEPTION_DURING_ACTIVITY_EXECUTE, nodeName, moNotFoundException.getMessage());
            jobResult = handleExecuteFailure(activityJobId, nodeName, jobLogMessage, errorMessage, jobLogList);
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            errorMessage = String.format(EXCEPTION_DURING_ACTIVITY_EXECUTE, nodeName, unsupportedFragmentException.getMessage());
            jobResult = handleExecuteFailure(activityJobId, nodeName, jobLogMessage, errorMessage, jobLogList);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while invoking ImportBackup action. Exception is : ", ex);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            if (!exceptionMessage.isEmpty()) {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, CONFIRM_RESTORE) + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
                errorMessage = String.format(EXCEPTION_DURING_ACTIVITY_EXECUTE, nodeName, exceptionMessage);
            } else {
                jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, CONFIRM_RESTORE);
                errorMessage = String.format(EXCEPTION_DURING_ACTIVITY_EXECUTE, nodeName, ex.getMessage());
            }
            jobResult = handleExecuteFailure(activityJobId, nodeName, jobLogMessage, errorMessage, jobLogList);
        }

        //Notify workflow service.
        finally {
            notifyWorkflowService(activityJobId, jobResult, businessKey, jobLogList);
        }
    }

    /**
     * This method handles failure in execute method during confirmation activity on a restore job.
     * 
     * @param activityJobId
     * @param nodeName
     * @param activityStepResult
     * @param jobLogMessage
     * @param errorMessage
     * @param jobLogList
     */
    private JobResult handleExecuteFailure(final long activityJobId, final String nodeName, final String jobLogMessage, final String errorMessage, final List<Map<String, Object>> jobLogList) {
        final JobResult jobResult = JobResult.FAILED;
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        LOGGER.error(errorMessage);
        activityUtils.recordEvent(SHMEvents.CONFIRM_RESTORE_EXECUTE, nodeName, null, activityUtils.additionalInfoForEvent(activityJobId, nodeName, errorMessage));
        return jobResult;
    }

    /**
     * This method notifies the workflow service with the job result.
     * 
     * @param activityJobId
     * @param jobResult
     * @param businessKey
     * @param jobLogList
     */
    private void notifyWorkflowService(final long activityJobId, final JobResult jobResult, final String businessKey, final List<Map<String, Object>> jobLogList) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
        final Map<String, Object> emptyMap = Collections.emptyMap();
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);

        final boolean wfsActivated = activityUtils.sendActivateToWFS(businessKey, emptyMap);
        if (!wfsActivated) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.WORKFLOW_SERVICE_INVOCATION_FAILED, CONFIRM_RESTORE), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
        }

    }

    /**
     * This method handles time-out occurrence during the confirmation of restoring backup on the node.
     * 
     * @param activityJobId
     * @return activityStepResult
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        JobResult jobResult = null;
        String logMessage = null;
        NEJobStaticData neJobStaticData = null;
        long activityStartTime = 0;
        String nodeName = "";
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            activityStartTime = neJobStaticData.getActivityStartTime();
            LOGGER.info("Time-out occurred during confirm backup activity in Restore job on node {} with activity job id : {}. ", nodeName, activityJobId);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.TIMEOUT, CONFIRM_RESTORE), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final String activityResult = activityUtils.getActivityJobAttributeValue(activityJobAttributes, ActivityConstants.ACTIVITY_RESULT);
            if (JobResult.SUCCESS.toString().equals(activityResult)) {
                LOGGER.info("Confirm Restore activity completed successfully after time out on the node : {}. ", nodeName);
                logMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, CONFIRM_RESTORE);
                jobResult = JobResult.SUCCESS;
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            } else {
                LOGGER.error("Confirm Restore activity failed after time out on the node : {}. ", nodeName);
                logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, CONFIRM_RESTORE);
                jobResult = JobResult.FAILED;
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
        } catch (JobDataNotFoundException e) {
            LOGGER.error("JobDataNotFoundException occured in Confirm Restore activity of HandleTimeout for {} on the node : {}. ", activityJobId, nodeName);
        } catch(final Exception ex) {
            LOGGER.error("Unable to verify timeout for Confirm Restore activity on node {}. Reason : {}", nodeName, ex);
        }
        
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.toString(), propertyList);
        activityUtils.recordEvent(SHMEvents.CONFIRM_RESTORE_TIME_OUT, nodeName, null, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList);
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        LOGGER.info("Handletimeout completed during Confirm Backup activity in Restore job with the activityJobId : {} and the result is {}", activityJobId, activityStepResult.getActivityResultEnum());
        return activityStepResult;
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("Inside InstallLicenseKeyFileService cancel() with activityJobId:{}", activityJobId);

        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        activityUtils.logCancelledByUser(jobLogList, jobEnvironment, CONFIRM_RESTORE);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        // TODO Auto-generated method stub
        return new ActivityStepResult();
    }

}
