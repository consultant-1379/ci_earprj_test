/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.deletebackup;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.ACTIVITY_END_PROGRESS_PERCENTAGE;

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

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.DeleteBackupUtility;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupUtils;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.DeleteSmrsBackupUtil;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ecim.backup.CancelBackupService;
import com.ericsson.oss.services.shm.es.impl.ecim.common.EcimCommonUtils;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

/**
 * This elementary service class performs the delete action on BrmBackupManager MOs and deletes the corresponding BrmBackup MOs based on user request.
 */
@SuppressWarnings("PMD.TooManyFields")
@EServiceQualifier("ECIM.DELETEBACKUP.deletebackup")
@ActivityInfo(activityName = "deletebackup", jobType = JobTypeEnum.DELETEBACKUP, platform = PlatformTypeEnum.ECIM)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DeleteBackupService implements Activity, ActivityCallback, AsynchronousActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteBackupService.class);

    private static final String DOMAIN = "DOMAIN";
    private static final String TYPE = "TYPE";
    private static final String LOCATION = "LOCATION";
    private static final String BACKUP_NAME = "BACKUP_NAME";

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private EcimBackupUtils ecimBackupUtils;

    @Inject
    private EcimCommonUtils ecimCommonUtils;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Inject
    private CancelBackupService cancelBackupService;

    @Inject
    private FragmentVersionCheck fragmentVersionCheck;

    @Inject
    private DeleteSmrsBackupUtil deleteSmrsBackupService;

    @Inject
    private DeleteBackupUtility deleteBackupUtility;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private NEJobProgressPercentageCache progressPercentageCache;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxy;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

/**
     * This method performs the PreCheck for ECIM Delete Backup activity that includes the availability of Backup MO.
     * 
     * This method has been Deprecated, use {@link DeleteBackupService.asyncPrecheck(long) method instead.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.debug("Inside ECIM DeleteBackupService.precheck with activityJobId : {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, EcimBackupConstants.DELETE_BACKUP);
            if (!isUserAuthorized) {
                return activityStepResult;
            }
            activityStepResult = getPrecheckResponse(activityJobId, neJobStaticData, jobLogList, jobPropertyList);
        } catch (final JobDataNotFoundException | MoNotFoundException jdnfEx) {
            LOGGER.error(String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.DELETE_BACKUP, jdnfEx.getMessage()));
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, EcimBackupConstants.DELETE_BACKUP, jdnfEx.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        } catch (final Exception ex) {
            LOGGER.error(String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.CREATE_CV, ex.getMessage()));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.CREATE_CV, ex.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        return activityStepResult;
    }

    private ActivityStepResult getPrecheckResponse(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList,
            final List<Map<String, Object>> jobPropertyList) {
        int successCount = 0;
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        final String nodeName = neJobStaticData.getNodeName();
        LOGGER.debug("Performing precheck for delete backups on the node : {}", nodeName);
        final String jobLogMessage = "\"%s\" activity initiated on the node %s.";
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(jobLogMessage, EcimBackupConstants.DELETE_BACKUP, nodeName), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        try {
            List<String> inputBackupDataList = null;
            final Map<String, Object> mainJobAttributes = jobConfigServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
            inputBackupDataList = ecimBackupUtils.getBackupDataToBeDeleted(mainJobAttributes, nodeName);
            LOGGER.debug("inputBackupDataList: {}", inputBackupDataList);
            //Group backupNames (i.e., key = domain|type|location, value = backupName) based on nodeName, domain, type and Location
            final Map<String, List<String>> backupNamesGroupedByDomainAndTypeAndLocation = ecimBackupUtils.groupBackupNamesByDomainTypeAndLoc(inputBackupDataList);
            LOGGER.debug("keyset in DBS : {}", backupNamesGroupedByDomainAndTypeAndLocation.keySet());
            for (final String groupKey : backupNamesGroupedByDomainAndTypeAndLocation.keySet()) {
                final Map<String, String> bkupDomainAndTypeAndLoc = ecimBackupUtils.getBackupDomainTypeAndLocation(groupKey);
                final String backupDomain = bkupDomainAndTypeAndLoc.get(DOMAIN);
                final String backupType = bkupDomainAndTypeAndLoc.get(TYPE);
                final String backupLocation = bkupDomainAndTypeAndLoc.get(LOCATION);
                final Map<String, String> locationMap = new HashMap<String, String>();
                locationMap.put(backupLocation, groupKey);
                //backups related to ENM
                if (backupLocation != null && backupLocation.equalsIgnoreCase(EcimBackupConstants.LOCATION_ENM)) {
                    successCount = preValidateEnmBackups(activityJobId, locationMap, backupNamesGroupedByDomainAndTypeAndLocation, nodeName, jobLogList, successCount);
                }
                //backups related to NODE
                if (backupLocation != null && backupLocation.equalsIgnoreCase(EcimBackupConstants.LOCATION_NODE)) {
                    successCount = preValidateNodeBackups(activityJobId, locationMap, backupNamesGroupedByDomainAndTypeAndLocation, nodeName, backupDomain, backupType, jobLogList, successCount);
                }
            }
            if (successCount > 0) {
                activityStepResult = updateJobLogsAndResultForSuccess(activityJobId, jobLogList, jobPropertyList, activityStepResult);
            } else {
                final String failureLog = String.format("Selected backups does not exist on the node %s. ", nodeName);
                activityStepResult = updateJobLogsAndResultForSkipped(activityJobId, jobLogList, jobPropertyList, activityStepResult, failureLog);
            }
        } catch (final Exception exception) {
            final String failureLog = String.format("Exception occured in precheck of DeleteBackup for nodeName: %s. Exception is : %s. ", nodeName, exception.getMessage());
            LOGGER.error("Exception occured in precheck of DeleteBackup for nodeName {} with activityJobId {}. Failure reason is : {}", nodeName, activityJobId, exception);
            activityStepResult = updateJobLogsAndResultForFailure(activityJobId, jobLogList, jobPropertyList, activityStepResult, failureLog);
        }
        if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);
        } else {
            LOGGER.debug("Skipping persisting step duration as activity is to be skipped or failed.");
        }
        return activityStepResult;
    }

    /**
     * This method performs the PreCheck for ECIM Backup Deletion On Smrs location.It will check whether any backup exist to delete
     * 
     * @param activityJobId
     * @return boolean
     */
    private boolean precheckForBkpsOnEnm(final long activityJobId, final String nodeName, final List<String> backupsToBeDeletedOnSmrs) {
        LOGGER.debug("Inside precheck of precheckForBkpsOnEnm with activityJobId : {}", activityJobId);
        boolean precheckResult = false;
        if (!backupsToBeDeletedOnSmrs.isEmpty()) {
            LOGGER.debug("Precheck - atleast one backup should present on ENM for deletion with activity job id : {}", activityJobId);
            final String logMessage = String.format("Proceeding with deletebackup execute on the Smrs %s with activityJobId %s ", nodeName, activityJobId);
            LOGGER.debug(logMessage);
            activityUtils.recordEvent(SHMEvents.DELETE_BACKUP_PRECHECK, nodeName, null, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            precheckResult = true;
        } else {
            LOGGER.debug("Precheck - atleast one backup is NOT present On ENM for deletion.");
            final String failureLog = String.format("Selected backups does not exist on the smrs %s. ", nodeName);
            LOGGER.error(failureLog);
            final String logMessage = String.format("Exiting deletebackup as backups does not exist on the Smrs %s with activityJobId %s. ", nodeName, activityJobId);
            activityUtils.recordEvent(SHMEvents.DELETE_BACKUP_PRECHECK, nodeName, null, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            precheckResult = false;
        }
        return precheckResult;
    }

    private ActivityStepResult updateJobLogsAndResultForFailure(final long activityJobId, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList,
            final ActivityStepResult activityStepResult, final String failureLog) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.BACKUP_CANNOT_BE_DELETED_ON_NODE, EcimBackupConstants.DELETE_BACKUP, failureLog), new Date(),
                JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);//Persist job logs and properties.
        LOGGER.debug("activityStepResult {}", activityStepResult.getActivityResultEnum());
        return activityStepResult;
    }

    private ActivityStepResult updateJobLogsAndResultForSkipped(final long activityJobId, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList,
            final ActivityStepResult activityStepResult, final String failureLog) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_SUCCESS, EcimBackupConstants.DELETE_BACKUP, failureLog), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);//Persist job logs and properties.
        LOGGER.debug("activityStepResult {}", activityStepResult.getActivityResultEnum());
        return activityStepResult;
    }

    private ActivityStepResult updateJobLogsAndResultForSuccess(final long activityJobId, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList,
            final ActivityStepResult activityStepResult) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_SUCCESS, EcimBackupConstants.DELETE_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);//Persist job logs and properties.
        LOGGER.debug("activityStepResult {}", activityStepResult.getActivityResultEnum());
        return activityStepResult;
    }

    private void handleExceptionForPrecheck(final List<Map<String, Object>> jobLogList, final Exception ex) {
        LOGGER.error("{} occurred ,Reason : {}", ex, ex.getMessage());
        final String logMessage = ex.getMessage();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
    }

    private void handleUnsupportedFragmentException(final long activityJobId, final List<Map<String, Object>> jobLogList, final String nodeName,
            final UnsupportedFragmentException unsupportedFragmentException) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format("Unsupported Fragment Exception encountered on node - %s.", nodeName), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.ERROR.toString());
        final Throwable cause = unsupportedFragmentException.getCause();
        final String message = cause != null ? cause.getMessage() : unsupportedFragmentException.getMessage();
        final String logMessage = String.format("Unable to proceed with delete backup activity on node %s because \"%s\" ", nodeName, message);
        LOGGER.error("Unable to proceed delete backup activity  on node {} because {} for activityjob id {} ", nodeName, unsupportedFragmentException, activityJobId);
        activityUtils.recordEvent(SHMEvents.DELETE_BACKUP_PRECHECK, nodeName, null, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
    }

    private void handleMoNotFoundException(final long activityJobId, final List<Map<String, Object>> jobLogList, final String nodeName, final String backupDomain, final String backupType,
            final MoNotFoundException moNotFoundException) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format("BackupManager MO Not found on node : %s for domain : %s, and type : %s.", nodeName, backupDomain, backupType), new Date(),
                JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        final Throwable cause = moNotFoundException.getCause();
        final String message = cause != null ? cause.getMessage() : moNotFoundException.getMessage();
        final String logMessage = String.format("Unable to proceed with delete backup activity on node %s because \"%s\" ", nodeName, message);
        LOGGER.error("Unable to proceed delete backup activity  on node {} because {} for activityjob id {} ", nodeName, moNotFoundException, activityJobId);
        activityUtils.recordEvent(SHMEvents.DELETE_BACKUP_PRECHECK, nodeName, null, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
    }

    private void populateActivityDetailsAndAbortDeletion(final long activityJobId, final List<Map<String, Object>> jobLogList, final String nodeName) {
        LOGGER.debug("Precheck - atleast one backup is NOT present for deletion.");
        final String failureLog = String.format("Selected backups does not exist on the node %s. ", nodeName);
        LOGGER.error(failureLog);

        final String logMessage = String.format("Exiting deletebackup as backups does not exist on the node %s with activityJobId %s. ", nodeName, activityJobId);
        activityUtils.recordEvent(SHMEvents.DELETE_BACKUP_PRECHECK, nodeName, null, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
    }

    private int populateActivityDetailsAndProceedForDeletion(final long activityJobId, final List<Map<String, Object>> jobLogList, int successCount, final String nodeName) {
        LOGGER.debug("Precheck - atleast one backup is present for deletion with activity job id : {}", activityJobId);
        final String logMessage = String.format("Proceeding with deletebackup execute on the node %s with activityJobId %s ", nodeName, activityJobId);
        LOGGER.debug(logMessage);
        activityUtils.recordEvent(SHMEvents.DELETE_BACKUP_PRECHECK, nodeName, null, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        successCount++;
        return successCount;
    }

    /**
     * This method deletes the BrmBackup on the node (from BrmBackupManager MO) and from ENM.
     * 
     * @param activityJobId
     */
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        LOGGER.debug("Mo action execution starts for ecim delete backup activity with activityJobId: {}", activityJobId);
        String nodeName = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            LOGGER.debug("Deleting Backup for NodeName : {}", nodeName);
            final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
            final double activityProgressPercentage = (double) activityJobAttributes.get(ShmConstants.PROGRESSPERCENTAGE);
            final Map<String, Object> mainJobAttributes = jobConfigServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
            activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.MO_ACTIVITY_END_PROGRESS, Double.toString(activityProgressPercentage));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
            final List<String> inputBackupDataList = ecimBackupUtils.getBackupDataToBeDeleted(mainJobAttributes, nodeName);
            final String inputBackupData = deleteBackupUtility.getBackupNameToBeProcessed(inputBackupDataList, activityJobAttributes, activityJobId);
            LOGGER.debug("inputBackupData : {}", inputBackupData);
            final Map<String, String> backupNameAndDomainAndTypeAndLocation = ecimBackupUtils.groupBackupNamesByDomainTypeAndLoc(inputBackupData, nodeName);
            LOGGER.debug("backupNameAndDomainAndTypeAndLocation :{}", backupNameAndDomainAndTypeAndLocation);
            if (backupNameAndDomainAndTypeAndLocation != null && backupNameAndDomainAndTypeAndLocation.get(LOCATION).equalsIgnoreCase(EcimBackupConstants.LOCATION_ENM)) {
                final String backupName = backupNameAndDomainAndTypeAndLocation.get(BACKUP_NAME);
                deleteBackupFromENM(activityJobId, neJobStaticData, backupName, nodeName);
            } else if (backupNameAndDomainAndTypeAndLocation != null && backupNameAndDomainAndTypeAndLocation.get(LOCATION).equalsIgnoreCase(EcimBackupConstants.LOCATION_NODE)) {
                final String backupName = backupNameAndDomainAndTypeAndLocation.get(BACKUP_NAME);
                final String backupDomain = backupNameAndDomainAndTypeAndLocation.get(DOMAIN);
                final String backupType = backupNameAndDomainAndTypeAndLocation.get(TYPE);
                final String backupNameDomainAndType = backupName + "|" + backupDomain + "|" + backupType;
                deleteBackupUtility.deleteBackupFromNode(activityJobId, neJobStaticData, backupNameDomainAndType, JobTypeEnum.DELETEBACKUP, getActivityInfo(activityJobId));
            }
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger action. Reason :{} ", jdnfEx.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_TRIGGER_FAILED_WITH_REASON, EcimBackupConstants.DELETE_BACKUP, jdnfEx.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, null, EcimBackupConstants.DELETE_BACKUP);
        } catch (final Exception exception) {
            String exceptionMessage = ExceptionParser.getReason(exception);
            if (exceptionMessage == null || exceptionMessage.isEmpty()) {
                exceptionMessage = exception.getMessage();
            }
            LOGGER.error("Exception occurred in execute of DeleteBackup activity due to : {}", exception);
            final String logMessage = "Execution failed for Delete Backup activity. Reason:" + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, null, ActivityConstants.EXECUTE);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
    }

    private int preValidateEnmBackups(final long activityJobId, final Map<String, String> locationMap, final Map<String, List<String>> backupNamesGroupedByDomainAndTypeAndLocation,
            final String nodeName, final List<Map<String, Object>> jobLogList, int successCount) {
        final String enmLocKey = locationMap.get(EcimBackupConstants.LOCATION_ENM);
        final List<String> backupsToBeDeletedOnEnm = backupNamesGroupedByDomainAndTypeAndLocation.get(enmLocKey);
        LOGGER.debug("backupsToBeDeletedOnEnm {}", backupsToBeDeletedOnEnm);
        if (precheckForBkpsOnEnm(activityJobId, nodeName, backupsToBeDeletedOnEnm)) {
            final String logMessage = JobLogConstants.DELETE_BACKUP_ENM_MSG;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            successCount++;
        } else {
            final String logMessage = JobLogConstants.DELETE_BACKUP_ENM_MSG;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.recordEvent(SHMEvents.DELETE_BACKUP_PRECHECK, nodeName, null, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        }
        return successCount;
    }

    private int preValidateNodeBackups(final long activityJobId, final Map<String, String> locationMap, final Map<String, List<String>> backupNamesGroupedByDomainAndTypeAndLocation,
            final String nodeName, final String backupDomain, final String backupType, final List<Map<String, Object>> jobLogList, int successCount) {
        final List<String> backupsToBeDeletedOnNode = new ArrayList<String>();
        try {
            final String treatAsInfo = activityUtils.isTreatAs(nodeName, FragmentType.ECIM_BRM_TYPE, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
            if (treatAsInfo != null) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }
            try {
                final String inputVersion = brmMoServiceRetryProxy.getBrmFragmentVersion(nodeName);
                final String logMessage = fragmentVersionCheck.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion);
                if (logMessage != null) {
                    jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                }
            } catch (final MoNotFoundException ex) {
                handleExceptionForPrecheck(jobLogList, ex);
            } catch (final UnsupportedFragmentException ex) {
                handleExceptionForPrecheck(jobLogList, ex);
            }
            final String nodeLocKey = locationMap.get(EcimBackupConstants.LOCATION_NODE);
            final List<String> backupNameListSpecificToBackupMgr = backupNamesGroupedByDomainAndTypeAndLocation.get(nodeLocKey);
            LOGGER.debug("backupNameListSpecificToBackupMgr in DPS : {}", backupNameListSpecificToBackupMgr);
            //Get the valid backups specific to a backup manager (this is because BrmBackup MOs exist on the BrmBackupManager). Add them to the final list of backups to be deleted.
            final List<String> validBackupList = brmMoServiceRetryProxy.getBackupDetails(backupNameListSpecificToBackupMgr, nodeName, backupDomain, backupType);
            backupsToBeDeletedOnNode.addAll(validBackupList);
        } catch (final MoNotFoundException moNotFoundException) {
            handleMoNotFoundException(activityJobId, jobLogList, nodeName, backupDomain, backupType, moNotFoundException);
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            handleUnsupportedFragmentException(activityJobId, jobLogList, nodeName, unsupportedFragmentException);
        }
        if (!backupsToBeDeletedOnNode.isEmpty()) {
            successCount = populateActivityDetailsAndProceedForDeletion(activityJobId, jobLogList, successCount, nodeName);
        } else {
            populateActivityDetailsAndAbortDeletion(activityJobId, jobLogList, nodeName);
        }
        return successCount;
    }

    private JobActivityInfo getActivityInfo(final long activityJobId) {
        return activityUtils.getActivityInfo(activityJobId, DeleteBackupService.class);
    }

    public void deleteBackupFromENM(final long activityJobId, final NEJobStaticData neJobStaticData, final String backupName, final String nodeName) {
        LOGGER.debug("Deleting backup from ENM with backupName : {} , nodeName : {} , and activityJobId: {}", backupName, nodeName, activityJobId);
        String neType = "";
        JobResult jobResult = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
        final String logMessage = String.format(JobLogConstants.EXECUTION_STARTED, EcimBackupConstants.DELETE_BACKUP, backupName);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (backupName != null && !backupName.isEmpty()) {
            final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
            try {
                final NetworkElement networkElement = deleteSmrsBackupService.getNetworkElement(nodeName);
                neType = networkElement.getNeType();
            } catch (final MoNotFoundException e) {
                LOGGER.error("deleteBackupOnEnm has failed due to {}", e);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.NETWORKELEMENT_NOT_FOUND, nodeName), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            }
            final Map<String, Object> jobResultMap = executeDeleteBackupOnEnm(nodeName, backupName, neType, jobLogList, activityJobId, jobPropertyList);
            jobResult = (JobResult) jobResultMap.get(ShmConstants.RESULT);
            final double totalActivityProgressPercentage = (double) jobResultMap.get(ShmConstants.PROGRESSPERCENTAGE);
            final Map<String, Object> repeatRequiredAndActivityResult = deleteBackupUtility.evaluateRepeatRequiredAndActivityResult(activityJobId, jobResult, jobPropertyList);
            final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
            final Map<String, Object> processVariables = new HashMap<String, Object>();
            if (repeatRequired) {
                processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
            }
            final String existingStepDurations = (String) activityJobAttributes.get(ShmConstants.STEP_DURATIONS);
            if (existingStepDurations != null && !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {
                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.EXECUTE);
            }
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, totalActivityProgressPercentage);
            progressPercentageCache.bufferNEJobs(neJobStaticData.getNeJobId());
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ActivityConstants.EXECUTE, processVariables);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeDeleteBackupOnEnm(final String nodeName, final String backupName, final String neType, final List<Map<String, Object>> jobLogList, final long activityJobId,
            final List<Map<String, Object>> jobPropertyList) {
        JobResult jobResult = null;
        double totalActivityProgressPercentage = 0.0;
        final Map<String, Object> jobResultMap = new HashMap<>();
        final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
        final double eachMO_End_ProgressPercentage = (double) activityJobAttributes.get(ShmConstants.PROGRESSPERCENTAGE);
        if (!deleteSmrsBackupService.isBackupExistsOnSmrs(nodeName, backupName, neType)) {
            LOGGER.debug("Either backup {} doesn't exists or already deleted", backupName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.BACKUP_CANNOT_BE_DELETED_ON_SMRS, backupName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.ONGOING, nodeName, backupName, Long.toString(activityJobId));
            jobResult = JobResult.SKIPPED;
        } else if (executeForBkpsOnEnm(nodeName, backupName, neType)) {
            jobResult = JobResult.SUCCESS;
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, backupName, Long.toString(activityJobId));
            jobLogUtil
                    .prepareJobLogAtrributesList(jobLogList, "Backup On Enm " + backupName + " has been deleted successfully.", new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            final int totalBkps = deleteBackupUtility.getCountOfTotalBackups(activityJobAttributes);
            totalActivityProgressPercentage = eachMO_End_ProgressPercentage + ACTIVITY_END_PROGRESS_PERCENTAGE / totalBkps;
            activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.MO_ACTIVITY_END_PROGRESS, Double.toString(totalActivityProgressPercentage));
        } else {
            jobResult = JobResult.FAILED;
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, backupName, Long.toString(activityJobId));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Backup On Enm:" + backupName + " cannot be deleted .", new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }

        jobResultMap.put(ShmConstants.RESULT, jobResult);
        jobResultMap.put(ShmConstants.PROGRESSPERCENTAGE, totalActivityProgressPercentage);
        return jobResultMap;
    }

    private static int getCountOfProcessedBackups(final List<Map<String, String>> jobPropertyList) {
        int processedBackups = 0;
        if (jobPropertyList != null) {
            for (final Map<String, String> eachJobProperty : jobPropertyList) {
                if (BackupActivityConstants.PROCESSED_BACKUPS.equals(eachJobProperty.get(ShmConstants.KEY))) {
                    processedBackups = Integer.parseInt(eachJobProperty.get(ShmConstants.VALUE));
                    break;
                }
            }
        }
        LOGGER.debug("Processed backups count = {}", processedBackups);
        return processedBackups;
    }

    @SuppressWarnings("unchecked")
    private String getBackup(final long activityJobId) {
        final List<Map<String, String>> activityJobPropertyList = (List<Map<String, String>>) jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId).get(ShmConstants.JOBPROPERTIES);
        String backup = "";
        if (activityJobPropertyList != null) {
            for (final Map<String, String> eachJobProperty : activityJobPropertyList) {
                if (EcimBackupConstants.CURRENT_BACKUP.equals(eachJobProperty.get(ShmConstants.KEY))) {
                    backup = eachJobProperty.get(ShmConstants.VALUE);
                    break;
                }
            }
        }
        LOGGER.debug("Current backup = {}", backup);
        return backup;
    }

    @SuppressWarnings("unchecked")
    protected String getBackupNameToBeProcessed(final List<String> backupDataList, final Map<String, Object> activityJobAttributes, final long activityJobId) {
        final int processedBackups = getCountOfProcessedBackups((List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES));
        final String[] listOfBackups = backupDataList.toArray(new String[0]);
        final String backupToBeProcessed = listOfBackups[processedBackups];
        LOGGER.debug("processedBackups : {} , listOfBackups : {} ,backupToBeProcessed : {}", processedBackups, listOfBackups, backupToBeProcessed);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.PROCESSED_BACKUPS, Integer.toString(processedBackups + 1));
        activityUtils.prepareJobPropertyList(jobPropertyList, EcimBackupConstants.CURRENT_BACKUP, backupToBeProcessed);
        activityUtils.prepareJobPropertyList(jobPropertyList, EcimBackupConstants.TOTAL_BACKUPS, Integer.toString(listOfBackups.length));
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
        return backupToBeProcessed;
    }

    /**
     * This method performs the Execution for ECIM Backup Deletion On SMRS location.Based to Action status it will update the workflow service
     * 
     * @param activityJobId
     * 
     */
    private boolean executeForBkpsOnEnm(final String nodeName, final String backupToBeDeletedOnSmrs, final String neType) {
        boolean result = false;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        LOGGER.debug("Deleting Backup on Smrs");
        try {
            final String logMessage = String.format(JobLogConstants.EXECUTION_STARTED, EcimBackupConstants.DELETE_BACKUP, backupToBeDeletedOnSmrs);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            if (deleteSmrsBackupService.deleteBackupOnSmrs(nodeName, backupToBeDeletedOnSmrs, neType)) {
                result = true;
            }
        } catch (final Exception e) {
            LOGGER.error("deleteBackupOnSmrs has failed due to {}", e);
            result = false;
        }
        return result;
    }

    private boolean isBackupDeletionCompletedOnSmrs(final String nodeName, final String backupToBeDeletedOnSmrs, final String neType) {
        boolean result = true;
        LOGGER.debug("Verying Backup Exists on Smrs");
        if (deleteSmrsBackupService.isBackupExistsOnSmrs(nodeName, backupToBeDeletedOnSmrs, neType)) {
            result = false;
        }
        return result;
    }

    /**
     * If no more backups are present, then set activity result to success or failed based on the existence intermediate failures in the job. Else, set the job property to indicate that this is an
     * intermediate failure. Notify the workflow service with necessary process variables.
     * 
     * @param activityJobId
     * @param neJobStaticData
     * @param jobPropertyList
     * @param activityJobId
     */
    private void handleFailureOrException(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        final Map<String, Object> repeatRequiredAndActivityResult = deleteBackupUtility.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.FAILED, jobPropertyList);
        final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
        if (repeatRequired) {
            processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ActivityConstants.EXECUTE, processVariables);
    }

/**
     * This method handles timeout scenario for Delete backup Job.
     * 
     * This method has been Deprecated, use {@link DeleteBackupService.asyncHandleTimeout(long) method instead.
     * 
     * @param activityJobId
     * @return activityStepResult
     */
    @Deprecated
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        LOGGER.debug("Inside ECIM DeleteBackupService.handleTimeout with activityJobId : {}", activityJobId);
        return (processTimeOut(activityJobId, null));
    }

    private ActivityStepResult processTimeOut(final long activityJobId, NEJobStaticData neJobStaticData) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        JobResult jobResult = null;
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.OPERATION_TIMED_OUT, EcimBackupConstants.DELETE_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        jobLogList.clear();
        if (neJobStaticData == null) {
            try {
                neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
            } catch (final JobDataNotFoundException jdnfEx) {
                LOGGER.error("Unable to trigger timeout. Reason :{} ", jdnfEx.getMessage());
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
                return activityStepResult;
            }
        }
        final String nodeName = neJobStaticData.getNodeName();
        //Retrieve the backup that is currently being processed for deletion. It will be of the form BackupName|Domain|Type.
        final String inputBackupData = getBackup(activityJobId);
        final Map<String, String> backupNameAndDomainAndTypeAndLocation = ecimBackupUtils.groupBackupNamesByDomainTypeAndLoc(inputBackupData, nodeName);
        //handleTimeout for DeleteBackup on ENM
        if (backupNameAndDomainAndTypeAndLocation != null && backupNameAndDomainAndTypeAndLocation.get(LOCATION).equalsIgnoreCase(EcimBackupConstants.LOCATION_ENM)) {
            final String backupName = backupNameAndDomainAndTypeAndLocation.get(BACKUP_NAME);
            jobResult = handleTimeoutFordeleteBackupOnENM(backupName, nodeName, activityJobId, jobLogList);
        } else if (backupNameAndDomainAndTypeAndLocation != null && backupNameAndDomainAndTypeAndLocation.get(LOCATION).equalsIgnoreCase(EcimBackupConstants.LOCATION_NODE)) {
            final String backupName = backupNameAndDomainAndTypeAndLocation.get(BACKUP_NAME);
            final String backupDomain = backupNameAndDomainAndTypeAndLocation.get(DOMAIN);
            final String backupType = backupNameAndDomainAndTypeAndLocation.get(TYPE);
            final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupDomain, backupName, backupType);
            jobResult = handleTimeoutFordeleteBackupOnNode(activityJobId, neJobStaticData, ecimBackupInfo, nodeName, jobLogList);
        }
        final Map<String, Object> repeatRequiredAndActivityResult = deleteBackupUtility.evaluateRepeatRequiredAndActivityResult(activityJobId, jobResult, jobPropertyList);
        final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
        final boolean isActivitySuccess = (boolean) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (repeatRequired) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
        } else if (isActivitySuccess) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
        if (!repeatRequired) {
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
        }
        return activityStepResult;
    }

    private JobResult handleTimeoutFordeleteBackupOnENM(final String backupName, final String nodeName, final long activityJobId, final List<Map<String, Object>> jobLogList) {
        LOGGER.debug("handleTimeoutFordeleteBackupOnENM with backupName : {} , nodeName : {} , and activityJobId: {}", backupName, nodeName, activityJobId);
        String neType = "";
        JobResult jobResult = JobResult.FAILED;
        try {
            final NetworkElement networkElement = deleteSmrsBackupService.getNetworkElement(nodeName);
            neType = networkElement.getNeType();
        } catch (final MoNotFoundException e) {
            LOGGER.error("deleteBackupOnEnm has failed due to {}", e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.NETWORKELEMENT_NOT_FOUND, nodeName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        }
        if (isBackupDeletionCompletedOnSmrs(nodeName, backupName, neType)) {
            jobResult = JobResult.SUCCESS;
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, backupName, Long.toString(activityJobId));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Backup On Enm " + backupName + " has been deleted successfully after timeout.", new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
        } else {
            jobResult = JobResult.FAILED;
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, backupName, Long.toString(activityJobId));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Deletion of Backup " + backupName + " failed on ENM", new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        return jobResult;
    }

    public JobResult handleTimeoutFordeleteBackupOnNode(final long activityJobId, final NEJobStaticData neJobStaticData, final EcimBackupInfo ecimBackupInfo, final String nodeName,
            final List<Map<String, Object>> jobLogList) {
        LOGGER.debug("Entered handleTimeoutFordeleteBackupOnNode with backupName : {} , nodeName : {} , and activityJobId: {}", ecimBackupInfo.getBackupName(), nodeName, activityJobId);
        String brmBackupManagerMoFdn = null;
        String jobLogMessage = null;
        JobResult jobResult = JobResult.FAILED;
        try {
            brmBackupManagerMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfo);
            activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            LOGGER.debug("Unsubscribed to Mo Notifications");

            final String logMessage = String.format("Delete Backup has Timed Out on node %s", nodeName);
            activityUtils.recordEvent(SHMEvents.TIME_OUT_FOR_DELETE_BACKUP, nodeName, brmBackupManagerMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.DELETEBACKUP_TIMEOUT, ecimBackupInfo.getBackupName(), EcimBackupConstants.DELETE_BACKUP), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            if (brmMoServiceRetryProxy.isBackupDeletionCompleted(nodeName, ecimBackupInfo)) { //Timeout success.
                jobLogMessage = String.format("Backup File = %s  deleted successfully after timeout on the node = %s.", ecimBackupInfo.getBackupName(), nodeName);//Job Logs.
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                LOGGER.debug("Completed deletion of {} successfully on the node {} after timeout.", ecimBackupInfo.getBackupName(), nodeName);
                activityUtils.recordEvent(SHMEvents.TIME_OUT_FOR_DELETE_BACKUP, nodeName, brmBackupManagerMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage)); //Record event.
                final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
                final String anyIntermediateFailures = activityUtils.getActivityJobAttributeValue(activityJobAttributes, ActivityConstants.ANY_INTERMEDIATE_FAILURE);
                LOGGER.debug("anyIntermediateFailures in handleTimeout : {}", anyIntermediateFailures);
                if (anyIntermediateFailures != null && Boolean.getBoolean(anyIntermediateFailures)) {
                    jobResult = JobResult.FAILED;
                } else {
                    jobResult = JobResult.SUCCESS;
                }
            } else { //Timeout failure.
                jobLogMessage = String.format("Deletion of backup - %s failed on the node - %s.", ecimBackupInfo.getBackupName(), nodeName);//Job logs.
                LOGGER.error("Backup deletion failed after timeout for backup - {} on the node {}.", ecimBackupInfo.getBackupName(), nodeName);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityUtils.recordEvent(SHMEvents.TIME_OUT_FOR_DELETE_BACKUP, nodeName, brmBackupManagerMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage)); //Record event.
                jobResult = JobResult.FAILED;
            }
        } catch (final MoNotFoundException moNotFoundException) {
            LOGGER.debug("BRMBackup Manager MO Exception occurred in deleteBackupOnNode");
            LOGGER.error("BrmBackupManagerMo not found during time out evaluation of activity completion for the node {} with exception : {}", nodeName, moNotFoundException);//Job Logs.
            jobLogMessage = String.format(JobLogConstants.BRMBACKUPMANAGER_NOT_FOUND, nodeName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            systemRecorder.recordCommand(SHMEvents.TIME_OUT_FOR_DELETE_BACKUP, CommandPhase.FINISHED_WITH_ERROR, nodeName, brmBackupManagerMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.DELETEBACKUP));
            jobResult = JobResult.FAILED;
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            LOGGER.error("Unsupported fragment during time out evaluation of activity completion for the node {} with exception : {}", nodeName, unsupportedFragmentException); //Job Logs.
            jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobResult = JobResult.FAILED;
        }
        return jobResult;
    }

    /**
     * Processes the AVC Notification sent when there has been an update to the attribute progressReport on the node. If the job is finished and is successful we de-register from notifications and
     * save the state of the job as SUCCESS. If the job is finished and is failed we de-register from notifications and save the state of the job as FAILED.
     * 
     * @param notification
     *            avc notification
     * @return void
     */
    @Override
    public void processNotification(final Notification notification) {
        LOGGER.debug("Entered ECIM -delete backup - processNotification with event type : {}", notification.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("ECIM - delete backup - Discarding non-AVC notification.");
            return;
        }
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        String nodeName = null;
        String jobLogMessage = null;
        AsyncActionProgress progressReport = null;

        final NotificationSubject notificationSubject = notification.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
        LOGGER.debug("modifiedAttributes in processNotification for activity {} : {}", activityJobId, modifiedAttributes);

        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger action. Reason :{} ", jdnfEx.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            activityUtils.failActivity(activityJobId, jobLogList, null, EcimBackupConstants.DELETE_BACKUP);
            return;
        }
        nodeName = neJobStaticData.getNodeName();
        final String inputBackupData = getBackup(activityJobId);
        final Map<String, String> backupNameAndDomainAndTypeAndLocation = ecimBackupUtils.groupBackupNamesByDomainTypeAndLoc(inputBackupData, nodeName);
        final String backupToBeDeleted = backupNameAndDomainAndTypeAndLocation.get(BACKUP_NAME);
        final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfo(backupToBeDeleted);
        final String backupName = ecimBackupInfo.getBackupName();
        JobResult jobResult = null;
        double totalActivityProgressPercentage = 0.0;
        try {
            progressReport = brmMoServiceRetryProxy.getValidAsyncActionProgress(nodeName, EcimBackupConstants.DELETE_BACKUP, modifiedAttributes);

            final boolean progressReportflag = cancelBackupService.validateActionProgressReport(progressReport, EcimBackupConstants.BACKUP_DELETE_ACTION_BSP, EcimBackupConstants.BACKUP_DELETE_ACTION);
            if (progressReportflag) {
                LOGGER.warn("Discarding invalid notification,for the activityJobId {} and modifiedAttributes {}", activityJobId, modifiedAttributes);
                return;
            }
            final String brmBackupManagerMoFdn = activityUtils.getMoFdnFromNotificationSubject(notificationSubject);
            final Date notificationTime = activityUtils.getNotificationTimeStamp(notificationSubject);
            final long mainJobId = neJobStaticData.getMainJobId();

            if (EcimBackupConstants.BACKUP_CANCEL_ACTION.equalsIgnoreCase(progressReport.getActionName())) {
                jobResult = ecimCommonUtils.handleCancelProgressReportState(jobLogList, activityJobId, progressReport, notificationTime, EcimBackupConstants.DELETE_BACKUP);
            } else if (EcimBackupConstants.BACKUP_DELETE_ACTION.equalsIgnoreCase(progressReport.getActionName())
                    || EcimBackupConstants.BACKUP_DELETE_ACTION_BSP.equalsIgnoreCase(progressReport.getActionName())) {
                jobResult = ecimCommonUtils.handleMoActionProgressReportState(jobLogList, activityJobId, progressReport, notificationTime, EcimBackupConstants.DELETE_BACKUP, backupToBeDeleted);
                totalActivityProgressPercentage = ecimCommonUtils.calculateActivityProgressPercentage(activityJobId, progressReport);
            }
            if (jobResult != null) {
                if (jobResult == JobResult.SUCCESS || jobResult == JobResult.CANCELLED) {
                    systemRecorder.recordCommand(SHMEvents.UPLOAD_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, brmBackupManagerMoFdn,
                            activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
                } else if (jobResult == JobResult.FAILED) {
                    systemRecorder.recordCommand(SHMEvents.UPLOAD_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, brmBackupManagerMoFdn,
                            activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
                }
                activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, DeleteBackupService.class));
                final Map<String, Object> repeatRequiredAndActivityResult = deleteBackupUtility.evaluateRepeatRequiredAndActivityResult(activityJobId, jobResult, jobPropertyList);
                final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
                final Map<String, Object> processVariables = new HashMap<String, Object>();
                if (repeatRequired) {
                    processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
                } else if (jobResult == JobResult.SUCCESS || jobResult == JobResult.FAILED) {
                    activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PROCESS_NOTIFICATION);
                }
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, totalActivityProgressPercentage);
                activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, ActivityConstants.EXECUTE, processVariables);
            } else {
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, totalActivityProgressPercentage);
                progressPercentageCache.bufferNEJobs(neJobStaticData.getNeJobId());
            }
        } catch (final MoNotFoundException moNotFoundException) {
            //Job Logs.
            LOGGER.error("BrmBackupManagerMo not found for notification on backup {} on node {} with exception {}", backupName, nodeName, moNotFoundException);
            jobLogMessage = String.format(JobLogConstants.BRMBACKUPMANAGER_NOT_FOUND, nodeName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            //Update intermediate failure details and set the activity result. Notify the workflow.
            handleFailureOrException(activityJobId, neJobStaticData, jobPropertyList, jobLogList);
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            LOGGER.error("Unsupported fragment for notification on backup {} on node {} with exception {}", backupName, nodeName, unsupportedFragmentException);
            jobLogMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            //Update intermediate failure details and set the activity result. Notify the workflow.
            handleFailureOrException(activityJobId, neJobStaticData, jobPropertyList, jobLogList);
        } catch (final Exception exception) {
            //Job Logs.
            LOGGER.error("Exception occured during processing of notifications for delete backup action on backup {} on node {} with exception : {}", backupName, nodeName, exception);
            jobLogMessage = String.format(JobLogConstants.DELETEBACKUP_FAILURE, ecimBackupInfo.getBackupName(), EcimBackupConstants.DELETE_BACKUP, exception);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            //Update intermediate failure details and set the activity result. Notify the workflow.
            handleFailureOrException(activityJobId, neJobStaticData, jobPropertyList, jobLogList);
        }
    }

    /**
     * This method cancels the ongoing deletebackup job.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
        } catch (final JobDataNotFoundException jdnfEx) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCELLATION_FAILED, EcimBackupConstants.DELETE_BACKUP, jdnfEx.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            LOGGER.error(String.format(JobLogConstants.CANCELLATION_FAILED, EcimBackupConstants.DELETE_BACKUP, jdnfEx.getMessage()));
            return new ActivityStepResult();
        }
        final String nodeName = neJobStaticData.getNodeName();
        final String inputBackupData = getBackup(activityJobId);
        final Map<String, String> backupNameAndDomainAndTypeAndLocation = ecimBackupUtils.groupBackupNamesByDomainTypeAndLoc(inputBackupData, nodeName);
        if (backupNameAndDomainAndTypeAndLocation != null && backupNameAndDomainAndTypeAndLocation.get(LOCATION).equalsIgnoreCase(EcimBackupConstants.LOCATION_ENM)) {
            activityUtils.logCancelledByUser(jobLogList, neJobStaticData, EcimBackupConstants.DELETE_BACKUP);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, EcimBackupConstants.DELETE_BACKUP), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.WARN.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
            jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        } else if (backupNameAndDomainAndTypeAndLocation != null && backupNameAndDomainAndTypeAndLocation.get(LOCATION).equalsIgnoreCase(EcimBackupConstants.LOCATION_NODE)) {
            final String backupName = backupNameAndDomainAndTypeAndLocation.get(BACKUP_NAME);
            final EcimBackupInfo ecimBackupInfo = ecimBackupUtils.getEcimBackupInfo(backupName);
            return cancelBackupService.cancel(activityJobId, EcimBackupConstants.DELETE_BACKUP, ecimBackupInfo);
        }
        return new ActivityStepResult();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        JobResult jobResult = null;
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to complete cancel timeout. Reason : {}", jdnfEx.getMessage());
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCELLATION_FAILED, EcimBackupConstants.DELETE_BACKUP, jdnfEx.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            LOGGER.error(String.format(JobLogConstants.CANCELLATION_FAILED, EcimBackupConstants.DELETE_BACKUP, jdnfEx.getMessage()));
            return activityStepResult;
        }
        final String nodeName = neJobStaticData.getNodeName();
        final String logMessage = String.format(JobLogConstants.CANCEL_TIMEOUT, EcimBackupConstants.DELETE_BACKUP);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, null, jobLogList);
        jobLogList.clear();
        //Retrieve the backup that is currently being processed for deletion. It will be of the form BackupName|Domain|Type.
        final String inputBackupData = getBackup(activityJobId);
        final Map<String, String> backupNameAndDomainAndTypeAndLocation = ecimBackupUtils.groupBackupNamesByDomainTypeAndLoc(inputBackupData, nodeName);
        //handleTimeout for DeleteBackup on ENM
        if (backupNameAndDomainAndTypeAndLocation != null && backupNameAndDomainAndTypeAndLocation.get(LOCATION).equalsIgnoreCase(EcimBackupConstants.LOCATION_ENM)) {
            final String backupName = backupNameAndDomainAndTypeAndLocation.get(BACKUP_NAME);
            jobResult = handleTimeoutFordeleteBackupOnENM(backupName, nodeName, activityJobId, jobLogList);
        } else if (backupNameAndDomainAndTypeAndLocation != null && backupNameAndDomainAndTypeAndLocation.get(LOCATION).equalsIgnoreCase(EcimBackupConstants.LOCATION_NODE)) {
            final String backupName = backupNameAndDomainAndTypeAndLocation.get(BACKUP_NAME);
            final String backupDomain = backupNameAndDomainAndTypeAndLocation.get(DOMAIN);
            final String backupType = backupNameAndDomainAndTypeAndLocation.get(TYPE);
            final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupDomain, backupName, backupType);
            jobResult = handleTimeoutFordeleteBackupOnNode(activityJobId, neJobStaticData, ecimBackupInfo, nodeName, jobLogList);
        }
        if (jobResult != null) {
            final Map<String, Object> repeatRequiredAndActivityResult = deleteBackupUtility.evaluateRepeatRequiredAndActivityResult(activityJobId, jobResult, jobPropertyList);
            final boolean isActivitySuccess = (boolean) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
            if (isActivitySuccess || jobResult == JobResult.SKIPPED) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
            } else if (jobResult == JobResult.FAILED) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            }
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
        } else {
            if (finalizeResult) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                //need to check job log
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.STILL_EXECUTING, EcimBackupConstants.BACKUP_CANCEL_ACTION), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
            } else {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
            }
        }
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        // Not needed if there is no node read calls in precheck or async threads are still not increased.
    }

    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        // Implement this when (if) precheck is made async.
    }

    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        LOGGER.debug("Inside ECIM DeleteBackupService.asyncHandleTimeout with activityJobId : {}", activityJobId);
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        NEJobStaticData neJobStaticData = null;
        String nodeName = null;
        long neJobId = -1;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            neJobId = neJobStaticData.getNeJobId();
            activityStepResultEnum = processTimeOut(activityJobId, neJobStaticData).getActivityResultEnum();
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger timeout. Reason : {}", jdnfEx.getMessage());
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        } catch (final Exception ex) {
            LOGGER.error("An exception has occured in async handleTimeout activity with activityJobId {}.  Exception is: ", activityJobId, ex);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            exceptionMessage = exceptionMessage.isEmpty() ? ex.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.DELETE_BACKUP, exceptionMessage);
        }
        LOGGER.info("Sending back ActivityStepResult to WorkFlow from ECIM DeleteBackupService.asyncHandleTimeout with result : {} for node {} with activityJobId {} and neJobId {}",
                activityStepResultEnum, nodeName, activityJobId, neJobId);
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, EcimBackupConstants.DELETE_BACKUP, activityStepResultEnum);
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.info("Failing the activity and sending back to WorkFlow from ECIM DeleteBackupService.timeoutForAsyncHandleTimeout for activityJobId : {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, EcimBackupConstants.DELETE_BACKUP);
    }

}
