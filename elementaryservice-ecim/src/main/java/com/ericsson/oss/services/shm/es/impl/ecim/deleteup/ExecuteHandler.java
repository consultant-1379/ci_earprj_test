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
import java.util.List;
import java.util.Map;

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
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.DefaultActionRetryPolicy;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.ecim.common.CmHeartbeatHandler;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@Traceable
@RequestScoped
@SuppressWarnings("PMD.TooManyFields")
public class ExecuteHandler {

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private CmHeartbeatHandler cmHeartbeatHandler;

    @Inject
    private DeleteUpgradePackageDataCollector deleteUpJobDataCollector;

    @Inject
    private DeleteUpgradePackageJobDataCollectorRetryProxy deleteUpDataCollectRetryProxy;

    @Inject
    @DefaultActionRetryPolicy
    protected ActionRetryPolicy moActionRetryPolicy;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private PollingActivityConfiguration pollingActivityConfiguration;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private PreCheckHandler preCheckHandler;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Inject
    WorkflowInstanceNotifier workflowInstanceHelper;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteHandler.class);

    private String nodeName = null;
    private String neType = null;
    private String platformType = null;
    private String neFdn = null;
    private JobEnvironment jobEnvironment = null;

    private NEJobStaticData neJobStaticData = null;
    private NetworkElementData networkElement = null;

    private final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
    private final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();

    public void execute(final long activityJobId, final JobActivityInfo jobActivityInfo) {
        LOGGER.info("Entered in DeleteUp execute() Activity with activityJobId {}", activityJobId);
        try {
            initializeVariables(activityJobId);

            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final boolean isPrecheckDone = isPrecheckDone(activityJobAttributes);
            if (!isPrecheckDone) {
                final ActivityStepResult activityStepResult = preCheckHandler.performPreCheck(activityJobId);
                if (ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION.equals(activityStepResult.getActivityResultEnum())) {
                    final String businessKey = neJobStaticData.getNeJobBusinessKey();
                    activityUtils.skipActivity(activityJobId, jobEnvironment, jobLogs, businessKey, ActivityConstants.DELETE_UP_DISPLAY_NAME);
                    return;
                } else if (ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION.equals(activityStepResult.getActivityResultEnum())) {
                    final String businessKey = neJobStaticData.getNeJobBusinessKey();
                    activityUtils.failActivity(activityJobId, jobLogs, businessKey, ActivityConstants.DELETE_UP_DISPLAY_NAME);
                    return;
                } else if (ActivityStepResultEnum.REPEAT_EXECUTE.equals(activityStepResult.getActivityResultEnum())) {
                    LOGGER.info("Going to Repeat Execute for node {} with activity ID {}", nodeName, activityJobId);
                    return;
                }
                persistPrecheck(activityJobId, activityJobAttributes);
            }
            // End of precheck
            execute(activityJobId);

        } catch (Exception ex) {
            LOGGER.error("Failed to proceed for deleteupgradepackage activity {}. Exception : ", ex);

            final String jobLogMessage = String.format(JobLogConstants.UNABLE_TO_PROCEED_ACTION, ActivityConstants.DELETE_UP_DISPLAY_NAME,
                    ExceptionParser.getReason(ex).isEmpty() ? ex.getMessage() : ExceptionParser.getReason(ex));
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            builJobLogMessage(activityJobId, jobLogMessage, null, CommandPhase.FINISHED_WITH_ERROR, JobLogLevel.ERROR.toString());
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ShmConstants.DELETEUPGRADEPKG_ACTIVITY, null);
        }
    }

    private void persistPrecheck(final long activityJobId, final Map<String, Object> activityJobAttributes) {
        final boolean isPrecheckAlreadyDone = isPrecheckDone(activityJobAttributes);
        if (!isPrecheckAlreadyDone) {
            LOGGER.debug("Inside ecim delete up service precheck() with activityJobId {}", activityJobId);
            final String jobLogMessage = String.format(JobLogConstants.PRECHECK_SUCCESS, ActivityConstants.DELETE_UP_DISPLAY_NAME);
            activityUtils.recordEvent(SHMEvents.ECIM_DELETE_UPGRADEPACKAGE_EXECUTE, nodeName, "", "SHM:" + activityJobId + ":" + nodeName + ":" + jobLogMessage);
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_PRECHECK_DONE, ActivityConstants.CHECK_TRUE);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
            jobLogs.clear();
        } else {
            LOGGER.debug("ECIM DELETEUP Pre-Validation already done for the activityJobId:{} and node name: {}", activityJobId, nodeName);
        }
    }

    private boolean isPrecheckDone(final Map<String, Object> activityJobAttributes) {
        final String isPrecheckDone = activityUtils.getActivityJobAttributeValue(activityJobAttributes, ActivityConstants.IS_PRECHECK_DONE);
        if (isPrecheckDone != null) {
            return Boolean.parseBoolean(isPrecheckDone);
        }
        return false;
    }

    private void initializeVariables(final long activityJobId) {
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            networkElement = networkElementRetrivalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
            platformType = neJobStaticData.getPlatformType();
            neFdn = networkElement.getNeFdn();
            jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        } catch (final MoNotFoundException moNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, moNotFoundException);
        } catch (JobDataNotFoundException jobDataNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, jobDataNotFoundException);
        }
    }

    public void execute(final long activityJobId) {
        LOGGER.debug("Entered Delete Upgrade Package activity execute with activityJobId : {}", activityJobId);
        String jobLogMessage = null;
        final Map<String, String> deletableUpData = deleteUpJobDataCollector.getUPMODataToBeProcessed(jobEnvironment);
        final String currentBkp = deletableUpData.get(DeleteUpgradePackageConstants.CURRENT_BKPNAME);
        final String currentUpProductData = deletableUpData.get(DeleteUpgradePackageConstants.CURRENT_UP_MO_DATA);

        final String currentUpProductDataArray[] = currentUpProductData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
        final String productNumber = currentUpProductDataArray[0];
        final String productRevision = currentUpProductDataArray[1];

        if (!currentBkp.isEmpty()) {
            // Proceeding current backup deletion
            final String[] backupData = currentBkp.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
            final String currentBkpname = backupData[0];
            final String currentBkpBrmBackupManagerMoFdn = backupData[1];

            LOGGER.debug("Delete Upgrade Package activity execute with activityJobId : {}, for backup: {} and brmBackupManagerMoFdn: {}", activityJobId, currentBkpname,
                    currentBkpBrmBackupManagerMoFdn);
            jobLogMessage = String.format(JobLogConstants.REFERREDBKP_ACTION_TRIGGERING, EcimBackupConstants.DELETE_BACKUP, currentBkpname);

            builJobLogMessage(activityJobId, jobLogMessage, currentBkpname, CommandPhase.STARTED, JobLogLevel.INFO.toString());
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            performBackupMoDeletionOnNode(activityJobId, currentBkpname, currentBkpBrmBackupManagerMoFdn, activityJobAttributes, currentBkp);

        } else {
            final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            final String swmNamespace = activityUtils.getActivityJobAttributeValue(activityJobAttributes, EcimCommonConstants.SWM_NAMESPACE);
            final String swmMoFdn = deleteUpDataCollectRetryProxy.getSwmManagedObjectFdn(nodeName, swmNamespace);

            final String upMoFdn = activityUtils.getActivityJobAttributeValue(activityJobAttributes, currentUpProductData);
            LOGGER.debug("Delete Upgrade Package activity execute with activityJobId : {}, current UPMO fdn : {}", activityJobId, upMoFdn);

            jobLogMessage = String.format(JobLogConstants.DELETEUP_ACTION_TRIGGERING, ActivityConstants.DELETE_UP_DISPLAY_NAME, productNumber, productRevision);

            builJobLogMessage(activityJobId, jobLogMessage, swmMoFdn, CommandPhase.STARTED, JobLogLevel.INFO.toString());

            final double activityProgressPercentage = (double) activityJobAttributes.get(ShmConstants.PROGRESSPERCENTAGE);
            activityUtils.prepareJobPropertyList(jobProperties, EcimCommonConstants.ReportProgress.MO_ACTIVITY_END_PROGRESS, Double.toString(activityProgressPercentage));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, null, null);

            activityUtils.subscribeToMoNotifications(swmMoFdn, activityJobId, getActivityInfo(activityJobId));

            performUpMoDeletionOnNode(activityJobId, swmMoFdn, DeleteUpgradePackageConstants.REMOVE_UP_ACTION_NAME, upMoFdn, productNumber, productRevision, currentUpProductData);
        }
    }

    private void performUpMoDeletionOnNode(final long activityJobId, final String swmMoFdn, final String actionName, final String upMoFdn, final String productNumber, final String productRevision,
            final String currentUpProductData) {
        LOGGER.debug("Entered Delete Upgrade Package activity perform UpMo action with activityJobId : {}", activityJobId);
        String logMessage = null;
        boolean actionResult;
        try {
            final Map<String, Object> actionArguments = new HashMap<String, Object>();
            actionArguments.put(DeleteUpgradePackageConstants.REMOVE_UP_ACTION_ARG, upMoFdn);

            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, platformType, JobTypeEnum.DELETE_UPGRADEPACKAGE.name(),
                    ActivityConstants.DELETE_UP_DISPLAY_NAME);

            logMessage = String.format(JobLogConstants.DELETEUP_ASYNC_ACTION_TRIGGERED, ActivityConstants.DELETE_UP_DISPLAY_NAME, activityTimeout, productNumber, productRevision);
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
            actionResult = deleteUpDataCollectRetryProxy.performAction(swmMoFdn, actionName, actionArguments, neType, networkElement.getOssModelIdentity());

            if (actionResult) {
                final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
                final String existingStepDurations = ((String) activityJobAttributes.get(ShmConstants.STEP_DURATIONS));
                if (existingStepDurations != null && !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {
                    activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
                }
                // Sending MediationTaskRequest to update heartbeat interval time on CmNodeHeartbeatSupervision MO to over come notification delays from mediation.
                sendHeartbeatIntervalChangeRequest(activityJobId);

                builJobLogMessage(activityJobId, logMessage, swmMoFdn, CommandPhase.FINISHED_WITH_SUCCESS, JobLogLevel.INFO.toString());
            } else {
                logMessage = String.format(JobLogConstants.DELETEUP_ACTION_TRIGGER_FAILED, ActivityConstants.DELETE_UP_DISPLAY_NAME, productNumber, productRevision);
                activityUtils.unSubscribeToMoNotifications(swmMoFdn, activityJobId, getActivityInfo(activityJobId));
                LOGGER.error("Unable to trigger verify action with activityJobId {} and nodeName {}", activityJobId, nodeName);
                builJobLogMessage(activityJobId, logMessage, swmMoFdn, CommandPhase.FINISHED_WITH_ERROR, JobLogLevel.ERROR.toString());
            }
        } catch (final MoNotFoundException e) {
            LOGGER.error("MoNotFoundException occured in performUpMoDeletionOnNode Execute with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId,
                    nodeName, e);
            logMessage = String.format("MoNotFoundException occurred while triggering activity : %s ", ActivityConstants.DELETE_UP_DISPLAY_NAME);
            builJobLogMessage(activityJobId, logMessage, swmMoFdn, CommandPhase.FINISHED_WITH_ERROR, JobLogLevel.ERROR.toString());
            hadleExceptionForUpdeletion(activityJobId, swmMoFdn, currentUpProductData);
        } catch (final UnsupportedFragmentException e) {
            LOGGER.error("UnsupportedFragmentException occured in performUpMoDeletionOnNode Execute with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}",
                    activityJobId, nodeName, e);
            logMessage = String.format("UnsupportedFragmentException occurred while triggering activity : %s ", ActivityConstants.DELETE_UP_DISPLAY_NAME);
            builJobLogMessage(activityJobId, logMessage, swmMoFdn, CommandPhase.FINISHED_WITH_ERROR, JobLogLevel.ERROR.toString());
            hadleExceptionForUpdeletion(activityJobId, swmMoFdn, currentUpProductData);
        } catch (final Exception ex) {
            LOGGER.error("An exception occurred in performUpMoDeletionOnNode Execute with activityJobId : {} nodeName : {} and can not be proceed further. Details are {}", activityJobId, nodeName,
                    ex);
            final String errorMessage = activityUtils.prepareErrorMessage(ex);
            logMessage = String.format("An exception occurred while triggering activity : %s ", ActivityConstants.DELETE_UP_DISPLAY_NAME) + String.format(JobLogConstants.FAILURE_REASON, errorMessage);
            builJobLogMessage(activityJobId, logMessage, swmMoFdn, CommandPhase.FINISHED_WITH_ERROR, JobLogLevel.ERROR.toString());
            hadleExceptionForUpdeletion(activityJobId, swmMoFdn, currentUpProductData);
        }
        LOGGER.debug("Exiting from execute deleteupgradepackage with activityJobId: {}", activityJobId);
    }

    private void performBackupMoDeletionOnNode(final long activityJobId, final String currentBkpName, final String currentBkpBrmBackupManagerMoFdn, final Map<String, Object> activityJobAttributes, final String currentBkp) {
        LOGGER.debug("Entered Delete Upgrade Package activity perform backup action with activityJobId : {} and currentBkpName: {}", activityJobId, currentBkpName);
        String logMessage = null;
        try {
            logMessage = String.format(JobLogConstants.EXECUTION_STARTED, EcimBackupConstants.DELETE_BACKUP, currentBkpName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
            jobLogs.clear();
            activityUtils.recordEvent(SHMEvents.DELETE_BACKUP_EXECUTE, nodeName, currentBkpBrmBackupManagerMoFdn, "SHM:" + activityJobId + ":" + nodeName);
            activityUtils.subscribeToMoNotifications(currentBkpBrmBackupManagerMoFdn, activityJobId, getActivityInfo(activityJobId));
            final int actionInvocationResult = brmMoServiceRetryProxy.executeMoAction(nodeName, new EcimBackupInfo("", currentBkpName, ""), currentBkpBrmBackupManagerMoFdn,
                    EcimBackupConstants.DELETE_BACKUP);
            LOGGER.debug("performaction on node : actionInvocationResult : {}", actionInvocationResult);

            if (actionInvocationResult == 0) {
                final String existingStepDurations = ((String) activityJobAttributes.get(ShmConstants.STEP_DURATIONS));
                if (existingStepDurations != null && !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {
                    activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
                }
                final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, platformType, JobTypeEnum.DELETEBACKUP.name(), EcimBackupConstants.DELETE_BACKUP);
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.DELETEBACKUP_ASYNC_ACTION_TRIGGERED, EcimBackupConstants.DELETE_BACKUP, activityTimeout, currentBkpName),
                        new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
                builJobLogMessage(activityJobId, logMessage, currentBkpName, CommandPhase.FINISHED_WITH_SUCCESS, JobLogLevel.ERROR.toString());
            } else {
                logMessage = String.format(JobLogConstants.BACKUP_NAME, JobLogConstants.ACTION_TRIGGER_FAILED, currentBkpName, EcimBackupConstants.DELETE_BACKUP);
                activityUtils.unSubscribeToMoNotifications(currentBkpBrmBackupManagerMoFdn, activityJobId, getActivityInfo(activityJobId));
                builJobLogMessage(activityJobId, logMessage, currentBkpName, CommandPhase.FINISHED_WITH_ERROR, JobLogLevel.ERROR.toString());
            }
        } catch (final MoNotFoundException moNotFoundException) {
            LOGGER.error("BrmBackupManagerMo not found to proceed with delete backup action: {} on BrmBackup {}", moNotFoundException, currentBkpName);
            logMessage = String.format(JobLogConstants.BRMBACKUPMANAGER_NOT_FOUND, nodeName);
            builJobLogMessage(activityJobId, logMessage, currentBkpName, CommandPhase.FINISHED_WITH_ERROR, JobLogLevel.ERROR.toString());
            handleExceptionForBackup(activityJobId, currentBkpBrmBackupManagerMoFdn, currentBkp);
        } catch (final Exception exception) {
            LOGGER.error("Unable to start MO Action for {} activity with activity job id {} on BrmBackup {} due to the exception : {}", EcimBackupConstants.DELETE_BACKUP, activityJobId,
                    currentBkpName, exception);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(exception);
            String errorMessage = "";
            if (!exceptionMessage.isEmpty()) {
                errorMessage = exceptionMessage;
            } else {
                errorMessage = exception.getMessage();
            }
            logMessage = String.format("An exception occurred while triggering activity : %s ", ActivityConstants.DELETE_UP_DISPLAY_NAME) + String.format(JobLogConstants.FAILURE_REASON, errorMessage);
            builJobLogMessage(activityJobId, logMessage, currentBkpName, CommandPhase.FINISHED_WITH_ERROR, JobLogLevel.ERROR.toString());
            handleExceptionForBackup(activityJobId, currentBkpBrmBackupManagerMoFdn, currentBkp);
        }

        LOGGER.debug("Exiting from execute deleteupgradepackage referred backup with activityJobId: {}", activityJobId);
    }

    private void handleExceptionForBackup(final long activityJobId, final String currentBkpBrmBackupManagerMoFdn, final String currentBkpData) {
        activityUtils.unSubscribeToMoNotifications(currentBkpBrmBackupManagerMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, DeleteUpgradePackageService.class));
        final boolean isRepeat = deleteUpJobDataCollector.setBackupDataForNextItration(activityJobId, currentBkpData);

        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, isRepeat);
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, "execute", processVariables);
    }

    private void hadleExceptionForUpdeletion(final long activityJobId, final String swmMoFdn, final String currentUpProductData) {
        final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
        activityUtils.unSubscribeToMoNotifications(swmMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, DeleteUpgradePackageService.class));
        final boolean isRepeat = deleteUpJobDataCollector.setUpAndBackupDataForNextItration(activityJobId, activityJobAttributes, currentUpProductData);
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        if (isRepeat) {
            activityUtils.prepareJobPropertyList(jobProperties, DeleteUpgradePackageConstants.UP_INTERMEDIATE_FAILURE, JobResult.FAILED.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, null, null);
            processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, isRepeat);
        } else {
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.DELETE_UP_DISPLAY_NAME), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
        jobLogs.clear();
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, "execute", processVariables);
    }

    private void sendHeartbeatIntervalChangeRequest(final long activityJobId) {
        if (neFdn != null) {
            final int cmHeartBeatInterval = pollingActivityConfiguration.getShmHeartBeatIntervalForEcim();
            cmHeartbeatHandler.sendHeartbeatIntervalChangeRequest(cmHeartBeatInterval, activityJobId, neFdn);
        } else {
            LOGGER.warn("Node FDN value returned null for the node:{} and activityJobId: {} in DeleteUpgradePackageServic execute ECIM ", nodeName, activityJobId);
        }
    }

    private void builJobLogMessage(final long activityJobId, final String logMessage, final String source, final CommandPhase commandPhase, final String logLevel) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), logLevel);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
        jobLogs.clear();

        systemRecorder.recordCommand(SHMEvents.ECIM_DELETE_UPGRADEPACKAGE_EXECUTE, commandPhase, nodeName, source, String.format(logMessage, getActivityInfo(activityJobId)));
        activityUtils.recordEvent(SHMEvents.ECIM_DELETE_UPGRADEPACKAGE_EXECUTE, nodeName, source, "SHM:" + activityJobId + ":" + nodeName);
    }

    private JobActivityInfo getActivityInfo(final long activityJobId) {
        return activityUtils.getActivityInfo(activityJobId, DeleteUpgradePackageService.class);
    }
}
