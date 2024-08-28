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

package com.ericsson.oss.services.shm.es.impl.cpp.deletebackup;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.EXECUTE_REPEAT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.HANDLE_TIMEOUT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.retry.SmrsRetryPolicies;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.DeleteSmrsBackupUtil;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVCurrentMainActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * This elementary service class performs the delete action on MO to delete CVs on node.
 * 
 */
@EServiceQualifier("CPP.DELETEBACKUP.deletecv")
@ActivityInfo(activityName = "deletecv", jobType = JobTypeEnum.DELETEBACKUP, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DeleteBackupService extends AbstractBackupActivity implements Activity {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteBackupService.class);

    @Inject
    private SmrsRetryPolicies smrsRetryPolicies;

    @Inject
    private SmrsFileStoreService smrsServiceUtil;

    @Inject
    private DeleteSmrsBackupUtil deleteSmrsBackupService;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    private FileResource fileResource;

    private static final String BACKUP_DATA_SPLIT_CHARACTER = "\\|";
    private static final String LOCATION = "LOCATION";
    private static final String BACKUP_NAME = "BACKUP_NAME";

    /**
     * This method performs the PreCheck for Delete backup activity that includes the availability of CV MO
     * 
     * @param activityJobId
     * @return ActivityStepResult
     */
    @SuppressWarnings({ "PMD.ExcessiveMethodLength", "unchecked" })
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.debug("Inside deletebackup activity precheck with activityJobId : {}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        String cvMoFdn = null;
        int successfulCvsCount = 0;
        String nodeName = "";
        long neJobId = 0;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.DELETE_CV_ACTIVITY_NAME);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }

            final String jobExecutedUser = activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId());

            neJobId = (long) activityUtils.getPoAttributes(activityJobId).get(ShmConstants.NE_JOB_ID);
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
            final Map<String, Object> neJobAttributes = activityUtils.getPoAttributes(neJobId);
            nodeName = (String) neJobAttributes.get(ShmConstants.NE_NAME);
            final Map<String, Object> mainJobAttributes = activityUtils.getPoAttributes((long) neJobAttributes.get(ShmConstants.MAIN_JOB_ID));
            final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
            LOGGER.debug("job Configuration in prepareActionArguments() {}", jobConfigurationDetails);
            final List<String> neFdns = new ArrayList<>();
            neFdns.add(nodeName);
            final List<NetworkElement> networkElementList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neFdns);
            final String neType = networkElementList.get(0).getNeType();
            final String platform = networkElementList.get(0).getPlatformType().name();
            // Preparing the action arguments to fetch list of CV Names and fetching Current main Activity value
            final Map<String, Object> actionArguments = prepareActionArguments(jobLogList, mainJobAttributes, nodeName);
            final List<String> cvsOnNode = (List<String>) actionArguments.get(ActivityConstants.CV_LIST);
            final List<String> enmBackups = (List<String>) actionArguments.get(ActivityConstants.CV_LIST_SMRS);
            activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.DELETE_CV), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            if (isValidNonEmptyList(cvsOnNode)) {
                final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(Arrays.asList(ActivityConstants.ROLL_BACK), jobConfigurationDetails, nodeName, neType, platform);
                final String isDeletionOfRollBackCVsSelected = keyValueMap.get(ActivityConstants.ROLL_BACK);
                final Map<String, Object> moAttributesMap = getConfigurationVersionMo(nodeName);
                final String treatAsInfo = activityUtils.isTreatAs(nodeName);
                if (treatAsInfo != null) {
                    activityUtils.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                }
                if (isInvalidMOAttributesMap(moAttributesMap)) {
                    updateJobResultAndJobLogsForPrecheckFailure(activityJobId, activityStepResult, jobLogList);
                }
                final StringBuilder setStartableStringBuilder = new StringBuilder();
                final StringBuilder currLoadedStringBuilder = new StringBuilder();
                final StringBuilder setFirstinRollbackList = new StringBuilder();
                cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
                successfulCvsCount = validateEligibleCVsForDeletion(successfulCvsCount, cvsOnNode, moAttributesMap, setStartableStringBuilder, currLoadedStringBuilder, setFirstinRollbackList,
                        isDeletionOfRollBackCVsSelected);
                if (successfulCvsCount > 0) {
                    activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.DELETE_CV), new Date(), JobLogType.SYSTEM.toString(),
                            JobLogLevel.INFO.toString());
                    final String logMessage = "Proceeding with Deletion of CV which are not currently loaded or set as startable.";//TODO modify log message
                    systemRecorder.recordEvent(jobExecutedUser, SHMEvents.DELETE_BACKUP_PRECHECK, EventLevel.COARSE, nodeName, cvMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
                } else {
                    if (moAttributesMap != null && !moAttributesMap.isEmpty()) {
                        updateJobLogsForDeletionFailedCVs(activityJobId, cvMoFdn, nodeName, setStartableStringBuilder, currLoadedStringBuilder, setFirstinRollbackList, jobLogList);
                    }
                }
            }
            //backups related to SMRS.
            if (isValidNonEmptyList(enmBackups)) {
                successfulCvsCount = updateCVCountWithEligibleENMBackups(activityJobId, successfulCvsCount, jobLogList, nodeName, enmBackups, jobExecutedUser);
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
                LOGGER.debug("Number of CVs to be deleted On Smrs:{}", enmBackups.size());
            }
            if (!isAtleastOneCVEligibleForDeletion(successfulCvsCount)) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            }
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        } catch (final Exception exception) {
            final String exceptionMessage = exception.getMessage();
            final String logMessage = getActivityType() + " Precheck failed." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            LOGGER.error("Exception occurred in precheck of DeleteBackup for node: {} due to: ", nodeName, exception);
        }
        if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        } else {
            LOGGER.debug("Skipping persisting step duration as activity is to be skipped or failed.");
        }
        LOGGER.debug("Activity Step Result = {}", activityStepResult.getActivityResultEnum());
        LOGGER.debug("Exiting DeleteCV Service precheck with activityJobID : {}", activityJobId);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        return activityStepResult;
    }

    private boolean isAtleastOneCVEligibleForDeletion(final int successfulCvsCount) {
        return successfulCvsCount > 0;
    }

    private int updateCVCountWithEligibleENMBackups(final long activityJobId, int successfulCvsCount, final List<Map<String, Object>> jobLogList, final String nodeName, final List<String> enmBackups,
            final String jobExecutedUser) {
        if (precheckForBkpsOnEnm(activityJobId, enmBackups)) {
            final String logMessage = "Proceed with deletion of CV on ENM";
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            successfulCvsCount++;
        } else {
            final String logMessage = "Cannot proceed with deletion of CV On ENM";
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.recordEvent(jobExecutedUser, SHMEvents.DELETE_BACKUP_PRECHECK, nodeName, null, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        }
        return successfulCvsCount;
    }

    private boolean isValidNonEmptyList(final List<String> cvsOnNode) {
        return cvsOnNode != null && !cvsOnNode.isEmpty();
    }

    private void updateJobResultAndJobLogsForPrecheckFailure(final long activityJobId, final ActivityStepResult activityStepResult, final List<Map<String, Object>> jobLogList) {
        activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.MO_NOT_EXIST, ActivityConstants.DELETE_CV, BackupActivityConstants.CV_MO_TYPE), new Date(),
                JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        LOGGER.debug("ActivityStepResult Status : {}  Activity Job ID : {} ", activityStepResult, activityJobId);
    }

    @SuppressWarnings("unchecked")
    private int validateEligibleCVsForDeletion(int successfulCvsCount, final List<String> cvsOnNode, final Map<String, Object> moAttributesMap, final StringBuilder setStartableStringBuilder,
            final StringBuilder currLoadedStringBuilder, final StringBuilder setFirstinRollbackList, final String isDeletionOfRollBackCVsSelected) {
        if (moAttributesMap != null && !moAttributesMap.isEmpty()) {
            final Map<String, Object> cvMoAttr = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
            // Fetching the CV which is set as Startable and fetching current loaded CV.
            if (cvMoAttr != null) {
                final String startableCV = (String) cvMoAttr.get(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION);
                final String currentLoadedCV = (String) cvMoAttr.get(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION);
                final String currentMainActivityValue = (String) cvMoAttr.get(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY);
                final List<String> rollbackCVList = (List<String>) cvMoAttr.get(ConfigurationVersionMoConstants.ROLLBACK_LIST);
                final CVCurrentMainActivity currentMainActivity = CVCurrentMainActivity.getMainActivity(currentMainActivityValue);
                LOGGER.debug("Current Main Activity : [{}], Number of CVs to be deleted On Node: [{}]", currentMainActivity, cvsOnNode.size());
                successfulCvsCount = eligibleCVsForDeletion(successfulCvsCount, cvsOnNode, startableCV, currentLoadedCV, setStartableStringBuilder, currLoadedStringBuilder, setFirstinRollbackList,
                        rollbackCVList, isDeletionOfRollBackCVsSelected);
            }
        }
        return successfulCvsCount;
    }

    private int eligibleCVsForDeletion(int successfulCvsCount, final List<String> cvsOnNode, final String startableCV, final String currentLoadedCV, final StringBuilder setStartableStringBuilder,
            final StringBuilder currLoadedStringBuilder, final StringBuilder setFirstinRollbackList, final List<String> rollbackCVList, final String isDeletionOfRollBackCVsSelected) {
        for (final String cvName : cvsOnNode) {
            if (cvName.equalsIgnoreCase(startableCV)) {
                setStartableStringBuilder.append(cvName);
                setStartableStringBuilder.append(",");
            }
            if (cvName.equalsIgnoreCase(currentLoadedCV)) {
                currLoadedStringBuilder.append(cvName);
                currLoadedStringBuilder.append(",");
            }
            if (rollbackCVList.contains(cvName)) {
                if (ShmConstants.FALSE.equals(isDeletionOfRollBackCVsSelected)) {
                    setFirstinRollbackList.append(cvName);
                    setFirstinRollbackList.append(",");
                } else if (ShmConstants.TRUE.equals(isDeletionOfRollBackCVsSelected)) {
                    successfulCvsCount++;
                }
            } else {
                successfulCvsCount++;
            }
        }
        LOGGER.debug("The successful count is:{}", successfulCvsCount);
        return successfulCvsCount;
    }

    private boolean isInvalidMOAttributesMap(final Map<String, Object> moAttributesMap) {
        return moAttributesMap.size() == 0 || moAttributesMap == null;
    }

    private boolean precheckForBkpsOnEnm(final long activityJobId, final List<String> backupsToBeDeletedOnSmrs) {
        boolean precheckResult = false;
        if (!backupsToBeDeletedOnSmrs.isEmpty()) {
            LOGGER.debug("Precheck - atleast one backup should present on ENM for deletion with activity job id : {}", activityJobId);
            precheckResult = true;
        } else {
            LOGGER.debug("Precheck - atleast one backup is NOT present On ENM for deletion with activity job id : {}", activityJobId);
            precheckResult = false;
        }
        return precheckResult;
    }

    private void updateJobLogsForDeletionFailedCVs(final long activityJobId, final String cvMoFdn, final String nodeName, final StringBuilder setStartableStringBuilder,
            final StringBuilder currLoadedStringBuilder, final StringBuilder setFirstinRollbackList, final List<Map<String, Object>> jobLogList) {

        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        String logMessage = null;
        if (setStartableStringBuilder.length() > 0) {
            LOGGER.debug("CV which is Set as Startable : {}", setStartableStringBuilder.toString());
            logMessage = "Cannot proceed with deletion of CV::" + setStartableStringBuilder.toString() + " as it is set as Startable.";
        }
        if (currLoadedStringBuilder.length() > 0) {
            LOGGER.debug("CV which is currently loaded:{}", currLoadedStringBuilder.toString());
            logMessage = "Cannot proceed with deletion of CV::" + currLoadedStringBuilder.toString() + " as it is currently loaded Configuration Version.";
        }
        if (setFirstinRollbackList.length() > 0) {
            LOGGER.debug("CV which is Set First in rollbackList:{}", currLoadedStringBuilder.toString());
            logMessage = "Cannot proceed with deletion of CV::" + setFirstinRollbackList.toString() + " as it is set first in rollbackList.";
        }
        activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        activityUtils.recordEvent(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.DELETE_BACKUP_PRECHECK, nodeName, cvMoFdn,
                "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
    }

    /**
     * This method deletes the CV from Node.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     */
    @SuppressWarnings("unchecked")
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        String cvMoFdn = null;
        JobResult jobResult = null;
        Map<String, Object> cvMoAttr = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();
        final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
        final Map<String, Object> processVariables = new HashMap<>();
        try {
            final Map<String, Object> moAttributesMap = getConfigurationVersionMo(nodeName);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
            if (moAttributesMap != null) {
                cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
                cvMoAttr = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
            }
            final Map<String, Object> actionArguments = prepareActionArguments(jobLogList, mainJobAttributes, nodeName);
            final Map<String, Object> activityJobAttributes = jobEnvironment.getActivityJobAttributes();
            final String cvNameAndLoc = (String) actionArguments.get(ActivityConstants.CV_NAME_LOCATION);
            final String backupData = getCvNameToBeProcessed(cvNameAndLoc, activityJobAttributes, jobPropertyList);
            activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.CURRENT_BACKUP, backupData);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            final Map<String, String> cvNameLocationMap = prepareCVNameAndLocation(backupData);
            final String cvName = cvNameLocationMap.get(BACKUP_NAME);
            final String cvLocation = cvNameLocationMap.get(LOCATION);
            if (cvLocation.equalsIgnoreCase(ShmCommonConstants.LOCATION_ENM)) {
                jobResult = deleteCvFromEnm(nodeName, cvName, activityJobId, jobLogList, activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()));
            } else if (cvLocation.equalsIgnoreCase(ShmCommonConstants.LOCATION_NODE)) {
                final boolean rollBackFlag = (boolean) actionArguments.get(ActivityConstants.ROLL_BACK_FLAG);
                jobResult = deleteCvFromNode(nodeName, cvName, activityJobId, rollBackFlag, cvMoFdn, cvMoAttr, jobLogList);
            }
            jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(jobResult, jobPropertyList, jobEnvironment);
            final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
            if (repeatRequired) {
                processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
            }
            final String existingStepDurations = ((String) activityJobAttributes.get(ShmConstants.STEP_DURATIONS));
            if (existingStepDurations != null && !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {
                final long activityStartTime = ((Date) activityJobAttributes.get(ShmConstants.ACTIVITY_START_DATE)).getTime();
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
            }
            if (jobResult == JobResult.SUCCESS || jobResult == JobResult.SKIPPED) {
                final List<Map<String, String>> activityJobPropertyList = (List<Map<String, String>>) jobEnvironment.getActivityJobAttributes().get(ShmConstants.JOBPROPERTIES);
                final int totalBackups = getCountOfTotalBackups(activityJobPropertyList);
                final Double currentProgressPercentage = activityAndNEJobProgressPercentageCalculator.calculateActivityProgressPercentage(jobEnvironment, totalBackups,
                        EXECUTE_REPEAT_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, currentProgressPercentage);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
            } else {
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            }
            activityUtils.sendNotificationToWFS(jobEnvironment, activityJobId, "execute", processVariables);
        } catch (final Exception e) {
            activityUtils.sendNotificationToWFS(jobEnvironment, activityJobId, "failed", processVariables);
            LOGGER.error("Exception occured while in DeleteBackupService activityJobId: {}:", activityJobId, e);
        }
    }

    private JobResult deleteCvFromEnm(final String nodeName, final String cvName, final Long activityJobId, final List<Map<String, Object>> jobLogList, final String jobExecutedUser) {
        String neType = "";
        JobResult jobResult = null;
        try {
            final NetworkElement networkElement = deleteSmrsBackupService.getNetworkElement(nodeName);
            neType = networkElement.getNeType();
        } catch (final MoNotFoundException e) {
            LOGGER.error("deleteBackupOnSmrs has failed due to {}", e);
            activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.NETWORKELEMENT_NOT_FOUND, nodeName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
            jobResult = JobResult.FAILED;
        }
        if (!deleteSmrsBackupService.isBackupExistsOnSmrs(nodeName, cvName, neType)) {
            LOGGER.warn("Either CV {} doesn't exists or already deleted", cvName);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CV_CANNOT_BE_DELETED_ON_SMRS, cvName), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            jobResult = JobResult.SKIPPED;
        } else {
            if (!neType.isEmpty()) {
                if (executeForBkpsOnEnm(activityJobId, nodeName, cvName, neType)) {
                    systemRecorder.recordCommand(jobExecutedUser, SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, cvName, Long.toString(activityJobId));
                    activityUtils.prepareJobLogAtrributesList(jobLogList, "CV On Smrs " + cvName + " has been deleted successfully.", new Date(), JobLogType.SYSTEM.toString(),
                            JobLogLevel.INFO.toString());
                    jobResult = JobResult.SUCCESS;
                } else {
                    systemRecorder.recordCommand(jobExecutedUser, SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, cvName, Long.toString(activityJobId));
                    activityUtils.prepareJobLogAtrributesList(jobLogList, "CV On Smrs:" + cvName + " cannot be deleted .", new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                    jobResult = JobResult.FAILED;
                }
            }
        }
        return jobResult;
    }

    @SuppressWarnings("unchecked")
    private JobResult deleteCvFromNode(final String nodeName, final String cvName, final Long activityJobId, final boolean rollBackFlag, final String cvMoFdn, final Map<String, Object> cvMoAttr,
            final List<Map<String, Object>> jobLogList) {

        boolean proceedToDeleteCV = true;
        JobResult jobResult = null;
        JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);

        systemRecorder.recordCommand(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.STARTED, nodeName, cvMoFdn,
                Long.toString(activityJobId));
        final Map<String, Object> actionArgument = new HashMap<>();
        actionArgument.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, cvName);
        if (cvMoAttr != null) {
            final List<String> rollBackList = (List<String>) cvMoAttr.get(ConfigurationVersionMoConstants.ROLLBACK_LIST);
            if (rollBackFlag) {
                executeActionMoWhenRollbackIsTrue(activityJobId, cvMoFdn, jobLogList, cvName, actionArgument, rollBackList);
            } else {
                if (rollBackList != null) {
                    for (final String rollBackCVName : rollBackList) {
                        if (cvName.equals(rollBackCVName)) {
                            activityUtils.prepareJobLogAtrributesList(jobLogList, "CV:" + cvName + " cannot be deleted as it is in RollbackList.", new Date(), JobLogType.SYSTEM.toString(),
                                    JobLogLevel.ERROR.toString());
                            jobResult = JobResult.FAILED;
                            proceedToDeleteCV = false;
                            break;
                        } else {
                            proceedToDeleteCV = true;
                        }
                    }
                }
            }
            if (proceedToDeleteCV) {
                try {
                    final String setStartable = (String) cvMoAttr.get(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION);
                    final String currentLoadedConfigurationVersion = (String) cvMoAttr.get(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION);
                    if (!(setStartable.equals(cvName) || currentLoadedConfigurationVersion.equals(cvName))) {
                        executeActionMo(cvMoFdn, jobLogList, cvName, actionArgument);
                        jobResult = JobResult.SUCCESS;
                    } else {
                        activityUtils.prepareJobLogAtrributesList(jobLogList, "CV:" + cvName + " cannot be deleted as it is setStartable or it is the current loaded version.", new Date(),
                                JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                        jobResult = JobResult.FAILED;
                    }
                } catch (final Exception e) {
                    LOGGER.error("Exception while deleting CV from Node. Reason :", e);
                    if (e.getMessage().contains(JobLogConstants.CV_DOES_NOT_EXISTS)) {
                        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CV_CANNOT_BE_DELETED_ON_NODE, cvName), new Date(), JobLogType.SYSTEM.toString(),
                                JobLogLevel.ERROR.toString());
                        jobResult = JobResult.SKIPPED;
                    } else {
                        logReasonForDeleteFailure(jobLogList, cvName, cvMoAttr, activityJobId, e);
                        jobResult = JobResult.FAILED;
                    }
                }
            }
        } else {
            jobResult = JobResult.FAILED;
        }
        return jobResult;
    }

    private Map<String, Object> evaluateRepeatRequiredAndActivityResult(final JobResult moActionResult, final List<Map<String, Object>> jobPropertyList, final JobEnvironment jobEnvironment) {
        LOGGER.debug("Evaluate whether repeat is Required and activity result. moActionResult {}, jobPropertyList {}", moActionResult, jobPropertyList);
        boolean recentDeleteFailed = false;
        boolean repeatExecute = true;
        boolean isActivitySuccess = false;
        JobResult activityJobResult = null;
        final long activityJobId = jobEnvironment.getActivityJobId();
        if (moActionResult == JobResult.FAILED) {
            recentDeleteFailed = true;
            activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.INTERMEDIATE_FAILURE, JobResult.FAILED.toString());
        } else if (moActionResult == JobResult.SUCCESS) {
            activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.INTERMEDIATE_SUCCESS, String.valueOf(Boolean.TRUE));
        }
        final boolean allBackupsProcessed = isAllBackupsProcessed(jobEnvironment);
        if (allBackupsProcessed) {
            final boolean intermediateFailureHappened = isAnyIntermediateFailureHappened(jobEnvironment);
            final boolean activitySuccess = isDeleteActivityPassed(jobEnvironment);
            if (intermediateFailureHappened || recentDeleteFailed) {
                activityJobResult = JobResult.FAILED;
            } else if (activitySuccess || moActionResult == JobResult.SUCCESS) {
                activityJobResult = JobResult.SUCCESS;
            } else {
                activityJobResult = JobResult.SKIPPED;
            }
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, activityJobResult.toString());
            repeatExecute = false;
        } else if (activityUtils.cancelTriggered(activityJobId)) {
            activityJobResult = JobResult.CANCELLED;
            repeatExecute = false;
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, activityJobResult.toString());
        }
        if (activityJobResult == JobResult.SUCCESS || activityJobResult == JobResult.SKIPPED) {
            isActivitySuccess = true;
        }
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatExecute);
        map.put(ActivityConstants.ACTIVITY_RESULT, isActivitySuccess);
        LOGGER.debug("Is Repeat Required or ActivityResult evaluated : {}", map);
        return map;
    }

    @SuppressWarnings("unchecked")
    private boolean isAllBackupsProcessed(final JobEnvironment jobEnvironment) {
        final List<Map<String, String>> activityJobPropertyList = (List<Map<String, String>>) jobEnvironment.getActivityJobAttributes().get(ShmConstants.JOBPROPERTIES);
        final int processedBackups = getCountOfProcessedBackups(activityJobPropertyList);
        final int totalBackups = getCountOfTotalBackups(activityJobPropertyList);
        if (processedBackups == totalBackups) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean isAnyIntermediateFailureHappened(final JobEnvironment jobEnvironment) {
        final List<Map<String, String>> activityJobPropertyList = (List<Map<String, String>>) jobEnvironment.getActivityJobAttributes().get(ShmConstants.JOBPROPERTIES);
        LOGGER.info("isAnyIntermediateFailureHappened activityJobPropertyList : {}", activityJobPropertyList);
        for (final Map<String, String> eachJobProperty : activityJobPropertyList) {
            if (BackupActivityConstants.INTERMEDIATE_FAILURE.equals(eachJobProperty.get(ShmConstants.KEY))) {
                return true;
            }
        }
        return false;

    }

    private boolean isDeleteActivityPassed(final JobEnvironment jobEnvironment) {
        final List<Map<String, String>> activityJobPropertyList = (List<Map<String, String>>) jobEnvironment.getActivityJobAttributes().get(ShmConstants.JOBPROPERTIES);
        LOGGER.debug("isDeleteActivityPassed for each deleted backup in activityJobPropertyList : {}", activityJobPropertyList);
        for (final Map<String, String> eachJobProperty : activityJobPropertyList) {
            if (BackupActivityConstants.INTERMEDIATE_SUCCESS.equals(eachJobProperty.get(ShmConstants.KEY))) {
                return true;
            }
        }
        return false;

    }

    /**
     * @param cvMoFdn
     * @param jobLogList
     * @param cvName
     * @param actionArgument
     * @return
     */
    private void executeActionMo(final String cvMoFdn, final List<Map<String, Object>> jobLogList, final String cvName, final Map<String, Object> actionArgument) {
        commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_DELETE_CV, cvMoFdn, actionArgument);
        activityUtils.prepareJobLogAtrributesList(jobLogList, "CV " + cvName + " has been deleted successfully.", new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

    }

    private boolean executeForBkpsOnEnm(final long activityJobId, final String nodeName, final String backupToBeDeletedOnSmrs, final String neType) {
        boolean result = false;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        LOGGER.debug("Deleting Backups on Smrs with activity job id : {}", activityJobId);
        try {
            final String logMessage = String.format(JobLogConstants.EXECUTION_STARTED, SHMEvents.DELETE_BACKUP_PRECHECK, backupToBeDeletedOnSmrs);
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            if (deleteSmrsBackupService.deleteBackupOnSmrs(nodeName, backupToBeDeletedOnSmrs, neType)) {
                result = true;
            }
        } catch (final Exception e) {
            LOGGER.error("deleteBackupOnSmrs has failed due to {}", e);
            result = false;
        }
        return result;
    }

    private void executeActionMoWhenRollbackIsTrue(final long activityJobId, final String cvMoFdn, final List<Map<String, Object>> jobLogList, final String cvName,
            final Map<String, Object> actionArgument, final List<String> rollBackList) {
        try {
            for (final String rollBackCVName : rollBackList) {
                if (cvName.equals(rollBackCVName)) {
                    commonCvOperations.executeActionOnMo(BackupActivityConstants.REMOVE_ROLLBACK_LIST, cvMoFdn, actionArgument);
                    activityUtils.prepareJobLogAtrributesList(jobLogList, "CV:" + cvName + " removed from Rollback List.", new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                    break;
                }
            }
        } catch (final Exception e) {
            logDeleteFailAsCVInRollbackList(jobLogList, cvName, e, activityJobId);
        }
    }

    /**
     * This method logs message if delete is failed due to presence of CV in rollback list
     * 
     * @param cvName
     * @param e
     * @param activityJobId
     */
    private void logDeleteFailAsCVInRollbackList(final List<Map<String, Object>> jobLogList, final String cvName, final Exception e, final long activityJobId) {
        LOGGER.error("Exception while removing CV {} from roll back list for activityJobId {}. Exception is {}", cvName, activityJobId, e);
        final String logMsg = "Unable to remove Configuration Version " + cvName;
        final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
        if (!exceptionMessage.isEmpty()) {
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMsg + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        } else {
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMsg, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
    }

    /**
     * This method logs the reason for delete failure on node
     * 
     * @param cvName
     */
    private void logReasonForDeleteFailure(final List<Map<String, Object>> jobLogList, final String cvName, final Map<String, Object> cvMoAttr, final long activityJobId, final Exception e) {
        LOGGER.error("Exception while deleting CV from Node. Perform Action Status:CVName {}. Exception::", cvName, e.getMessage());
        LOGGER.debug("Job Log List:{} activity job ID:{} CV Name:{} Cv Mo Attr:{}", jobLogList, activityJobId, cvName, cvMoAttr);
        final String logMsg = "Unable to delete Configuration Version:" + cvName;
        final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
        if (!exceptionMessage.isEmpty()) {
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMsg + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        } else {
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMsg, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
    }

    /**
     * This method prepares Action arguments with list of CVs and RollBack check
     * 
     * @param activityJobId
     * @param cvMoAttr
     * @param nodeName
     * @return actionArguments
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> prepareActionArguments(final List<Map<String, Object>> jobLogList, final Map<String, Object> mainJobAttr, final String nodeName) {
        LOGGER.debug("Prepare Action Arguments Entry for  NodeName : {}", nodeName);
        String cvNameAndLoc = null;
        boolean rollBackListFlag = false;
        List<String> cvNameAndLocList = new ArrayList<String>();
        Map<String, List<String>> cvNameList = new HashMap<String, List<String>>();
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttr.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        LOGGER.debug("job Configuration in prepareActionArguments() {}", jobConfigurationDetails);
        final List<String> neFdns = new ArrayList<String>();
        neFdns.add(nodeName);
        final List<NetworkElement> networkElementList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neFdns);
        final String neType = networkElementList.get(0).getNeType();
        final String platform = networkElementList.get(0).getPlatformType().name();
        LOGGER.debug("NeType {}, platform {} ", neType, platform);
        final List<String> keyList = new ArrayList<String>();
        keyList.add(BackupActivityConstants.CV_NAME);
        keyList.add(ActivityConstants.ROLL_BACK);
        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, nodeName, neType, platform);
        cvNameAndLoc = keyValueMap.get(ActivityConstants.CV_NAME);
        final String flag = keyValueMap.get(ActivityConstants.ROLL_BACK);
        LOGGER.debug("cvNameAndLoc {} ROLL Back flag {} in prepareActionArguments method ", cvNameAndLoc, flag);
        // Preparing the list of CV names from comma separated string
        try {
            if (cvNameAndLoc != null) {
                cvNameAndLocList = prepareCVNamesAndLocList(cvNameAndLoc);
                cvNameList = prepareCVNamesList(cvNameAndLocList);
                LOGGER.debug("cvNameList {}", cvNameList);
            } else {
                LOGGER.debug("CV name not provided to delete.");
            }
        } catch (final NumberFormatException numberFormatException) {
            LOGGER.error("Unable to parse cvNameList : {}, exception: {}", cvNameList, numberFormatException);
        }

        if (flag != null) {
            if (ShmConstants.TRUE.equalsIgnoreCase(flag)) {
                rollBackListFlag = true;
            }
        }

        final List<String> cvNamesOnNode = cvNameList.get(ShmCommonConstants.LOCATION_NODE);
        final List<String> cvNamesOnEnm = cvNameList.get(ShmCommonConstants.LOCATION_ENM);
        if (cvNamesOnNode == null && cvNamesOnEnm == null) {
            final String logMessage = "CV names not provided to delete On Node/SMRS.";
            LOGGER.debug("CV is not available on Node/SMRS to delete");
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }

        final HashMap<String, Object> actionArguments = new HashMap<String, Object>();
        actionArguments.put(ActivityConstants.CV_LIST, cvNamesOnNode);
        actionArguments.put(ActivityConstants.CV_LIST_SMRS, cvNamesOnEnm);
        actionArguments.put(ActivityConstants.ROLL_BACK_FLAG, rollBackListFlag);
        actionArguments.put(ActivityConstants.CV_NAME_LOCATION, cvNameAndLoc);
        LOGGER.debug("Returning action arguments : {}", actionArguments);
        return actionArguments;
    }

    /**
     * This method handles timeout scenario for Delete backup Job.
     * 
     * @param activityJobId
     * @return activityStepResult
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        // Delete action is a synchronous call so it will not call handle
        // timeout method. But to support the existing BPMN model where the flow
        // can call timeout this method has been implemented.
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        LOGGER.debug("Inside DeleteCV Service handleTimeout() with activityJobId : {}", activityJobId);
        JobResult jobResult = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        jobResult = timeout(activityJobId, jobLogList, activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()));
        final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(jobResult, jobPropertyList, jobEnvironment);
        final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
        final boolean isActivitySuccess = (boolean) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (repeatRequired) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
        } else if (isActivitySuccess) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
        if (!repeatRequired) {
            final Map<String, Object> activityJobAttrs = jobEnvironment.getActivityJobAttributes();
            final long activityStartTime = ((Date) activityJobAttrs.get(ShmConstants.ACTIVITY_START_DATE)).getTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        }
        return activityStepResult;
    }

    @SuppressWarnings("unchecked")
    public JobResult timeout(final long activityJobId, final List<Map<String, Object>> jobLogList, final String jobExecutedUser) {
        JobResult jobResult = null;
        Map<String, Object> cvMoAttr = new HashMap<>();
        String cvMoFdn = null;
        final Map<String, Object> activityJobAttributes = activityUtils.getPoAttributes(activityJobId);
        final long neJobId = (long) activityJobAttributes.get(ShmConstants.NE_JOB_ID);
        final Map<String, Object> neJobAttributes = activityUtils.getPoAttributes(neJobId);
        final String nodeName = (String) neJobAttributes.get(ShmConstants.NE_NAME);
        final Map<String, Object> moAttributesMap = getConfigurationVersionMo(nodeName);
        boolean verifiedOnNode = true;
        boolean verifiedOnEnm = true;
        if (moAttributesMap != null) {
            cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
            cvMoAttr = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
        }
        String logMessage = "Deletion of BackupService Timed out";
        activityUtils.recordEvent(jobExecutedUser, SHMEvents.TIME_OUT_FOR_DELETE_BACKUP, nodeName, cvMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        final String backupData = getCurrentBackup(activityJobId, activityJobAttributes);
        final Map<String, String> cvNameLocationMap = prepareCVNameAndLocation(backupData);
        final String cvName = cvNameLocationMap.get(BACKUP_NAME);
        final String cvLocation = cvNameLocationMap.get(LOCATION);
        if (cvLocation.equalsIgnoreCase(ShmCommonConstants.LOCATION_ENM)) {
            NetworkElement networkElement;
            try {
                networkElement = deleteSmrsBackupService.getNetworkElement(nodeName);
                verifiedOnEnm = verifyActionForEnm(cvName, nodeName, networkElement.getNeType());
            } catch (final MoNotFoundException e) {
                LOGGER.error("Exception occurred while retrieving information from smrs" + e);
            }
        } else if (cvLocation.equalsIgnoreCase(ShmCommonConstants.LOCATION_NODE)) {
            verifiedOnNode = verifyActionForNode(cvMoAttr, cvName);
        }
        if (!(verifiedOnNode && verifiedOnEnm)) {
            logMessage = "Verified by timeout that delete backup CV has failed. CV " + cvName + " has not been deleted.";
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobResult = JobResult.FAILED;
        } else {
            logMessage = "Verified by timeout that  CV " + cvName + " has been deleted.";
            activityUtils.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobResult = JobResult.SUCCESS;
        }
        return jobResult;
    }

    /**
     * This method handles timeout scenario for Delete backup Job.
     * 
     * @param activityJobId
     * @return activityStepResult
     */
    @SuppressWarnings("unchecked")
    private boolean verifyActionForNode(final Map<String, Object> cvMoAttributes, final String cvName) {
        final List<Map<String, String>> storedConfigurationVersionList = (List<Map<String, String>>) cvMoAttributes.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION);
        LOGGER.debug("StoredConfigurationVersionList: {}", storedConfigurationVersionList);
        boolean verified = ActivityConstants.TRUE;
        String cvNameStoredConfigurationVersion;

        for (final Map<String, String> storedConfigurationVersion : storedConfigurationVersionList) {
            cvNameStoredConfigurationVersion = storedConfigurationVersion.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME);
            if (cvName.equals(cvNameStoredConfigurationVersion)) {
                verified = ActivityConstants.FALSE;
            }
        }
        LOGGER.debug("Verify Action result for backups on node : {}", verified);
        return verified;
    }

    private boolean verifyActionForEnm(final String cvName, final String nodeName, final String neType) {
        boolean verified = deleteSmrsBackupService.isBackupExistsOnSmrs(nodeName, cvName, neType);
        LOGGER.debug("Verify Action result for backups on enm : {}", verified);
        return verified;
    }

    /**
     * This method prepares the list of CV Names in the form of cvName|Location.
     * 
     * @param cvName
     * @return List<String>
     */
    private List<String> prepareCVNamesAndLocList(final String cvName) {
        final List<String> cvNameAndLocList = new ArrayList<String>();
        if (cvName.contains(",")) {
            final String[] cvNames = cvName.split(",");
            for (final String cv : cvNames) {
                cvNameAndLocList.add(cv);
            }
        } else {
            cvNameAndLocList.add(cvName);
        }
        return cvNameAndLocList;
    }

    /**
     * This method prepares the list of CV Names.
     * 
     * @param cvName
     * @return List<String>
     */
    private Map<String, List<String>> prepareCVNamesList(final List<String> cvNameANdLocList) {
        final Map<String, List<String>> backupsGroupedByNameAndLoc = new HashMap<String, List<String>>();
        final List<String> backupNameListOnNode = new ArrayList<String>();
        final List<String> backupNameListOnEnm = new ArrayList<String>();
        for (final String backupData : cvNameANdLocList) {
            final String[] backupDetails = backupData.split(BACKUP_DATA_SPLIT_CHARACTER);
            final int backupDetailsLength = backupDetails.length;
            if (backupDetailsLength == 2) {
                final String backupName = backupDetails[0];
                final String backupLocation = backupDetails[1];
                if (backupLocation.equalsIgnoreCase(ShmCommonConstants.LOCATION_NODE)) {
                    backupNameListOnNode.add(backupName);
                }
                if (backupLocation.equalsIgnoreCase(ShmCommonConstants.LOCATION_ENM)) {
                    backupNameListOnEnm.add(backupName);
                }
            }
        }
        backupsGroupedByNameAndLoc.put(ShmCommonConstants.LOCATION_NODE, backupNameListOnNode);
        backupsGroupedByNameAndLoc.put(ShmCommonConstants.LOCATION_ENM, backupNameListOnEnm);
        return backupsGroupedByNameAndLoc;
    }

    private Map<String, String> prepareCVNameAndLocation(final String backupData) {
        final Map<String, String> backupsGroupedByNameAndLoc = new HashMap<String, String>();
        final String[] backupDetails = backupData.split(BACKUP_DATA_SPLIT_CHARACTER);
        final int backupDetailsLength = backupDetails.length;
        String cvName = "";
        String cvLocation = "";
        if (backupDetailsLength == 2) {
            cvName = backupDetails[0];
            cvLocation = backupDetails[1];
        }
        backupsGroupedByNameAndLoc.put(BACKUP_NAME, cvName);
        backupsGroupedByNameAndLoc.put(LOCATION, cvLocation);
        return backupsGroupedByNameAndLoc;
    }

    /**
     * This method cancels the action.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("Inside DeleteBackupService cancel() with activityJobId:{}", activityJobId);

        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();

        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);

        activityUtils.logCancelledByUser(jobLogList, jobEnvironment, ActivityConstants.DELETE_CV);
        activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, ActivityConstants.DELETE_CV), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.WARN.toString());
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");

        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);

        // Return type will be changed to void after making cancel() asynchronous.
        return new ActivityStepResult();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity#getActivityType()
     */
    @Override
    public String getActivityType() {
        return ActivityConstants.DELETE_CV;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity#getNotificationEventType()
     */
    @Override
    public String getNotificationEventType() {
        return SHMEvents.DELETE_BACKUP_EXECUTE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        LOGGER.debug("Inside DeleteCV Service cancelTimeout() with activityJobId : {}", activityJobId);
        JobResult jobResult = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        jobResult = timeout(activityJobId, jobLogList, activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()));
        final Map<String, Object> repeatRequiredAndActivityResult = evaluateRepeatRequiredAndActivityResult(jobResult, jobPropertyList, jobEnvironment);
        final boolean isActivitySuccess = (boolean) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
        if (isActivitySuccess) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        return activityStepResult;
    }
}
