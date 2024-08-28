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
package com.ericsson.oss.services.shm.es.impl.cpp.backuphousekeeping;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.*;

import java.util.ArrayList;
import java.util.Collections;
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
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.backuphousekeeping.BackupHouseKeepingCriteria;
import com.ericsson.oss.services.shm.es.backuphousekeeping.NodeBackupHousekeepingConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.AbstractBackupActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

@EServiceQualifier("CPP.BACKUP_HOUSEKEEPING.cleancv")
@ActivityInfo(activityName = "cleancv", jobType = JobTypeEnum.BACKUP_HOUSEKEEPING, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CleanupCvService extends AbstractBackupActivity implements Activity {

    @Inject
    ConfigurationVersionUtils cvUtil;

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupCvService.class);

    /**
     * This method performs the PreCheck for HouseKeeping of backups on node.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     */

    @SuppressWarnings("unchecked")
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        String jobLogMessage;
        String jobLogLevel;
        String nodeName = null;
        int backupsToKeepOnNode = 0;
        int backupsonNode = 0;
        int backupsToKeepInRollbackList = 0;
        long neJobId = 0;
        long activityStartTime = 0;
        String jobExecutionUser = null;

        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            jobExecutionUser = activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.CLEANCV_ACTIVITY_NAME);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }

            final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
            final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
            nodeName = jobEnvironment.getNodeName();
            neJobId = jobEnvironment.getNeJobId();
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobId);
            LOGGER.debug("CleanupCvService precheck() with nodeName {}", nodeName);
            final Map<String, Object> cvMoAttributesMap = getConfigurationVersionMo(nodeName);
            if (!isCvMOExist(cvMoAttributesMap)) {
                return failPreCheck(activityJobId);
            }
            final Map<String, Object> cvMoAttr = (Map<String, Object>) cvMoAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
            final List<Map<String, String>> storedConfigurationVersionList = (List<Map<String, String>>) cvMoAttr.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION);
            backupsonNode = storedConfigurationVersionList.size();
            final List<String> rollbackList = (List<String>) cvMoAttr.get(ConfigurationVersionMoConstants.ROLLBACK_LIST);
            final BackupHouseKeepingCriteria backupHouseKeepingCriteria = getHousekeepingCriteria(mainJobAttributes, nodeName);
            backupsToKeepOnNode = backupHouseKeepingCriteria.getMaxbackupsToKeepOnNode();
            backupsToKeepInRollbackList = backupHouseKeepingCriteria.getBackupsToKeepInRollBackList();
            if (((removalFromNodeRequested(backupsToKeepOnNode) && backupsToKeepOnNode >= backupsonNode))
                    && ((removalFromRblRequested(backupsToKeepInRollbackList)) && backupsToKeepInRollbackList >= rollbackList.size())) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
                jobLogMessage = String.format(JobLogConstants.CV_AVAILABLE_LESS_THAN_OR_EQUAL_TO_CV_TO_KEEP_ON_NODE, backupsonNode, backupsToKeepOnNode, getActivityType());
                preCheckUpdateLog(activityJobId, jobLogMessage, nodeName, jobExecutionUser);
                jobLogMessage = String.format(JobLogConstants.CV_IN_ROLLBACK_LIST_IS_LESS_THAN_OR_EQUAL_TO_CV_TO_KEEP_IN_ROLLBACK_LIST, rollbackList.size(), backupsToKeepInRollbackList,
                        getActivityType());
                preCheckUpdateLog(activityJobId, jobLogMessage, nodeName, jobExecutionUser);
            } else if ((removalFromNodeRequested(backupsToKeepOnNode) && backupsToKeepOnNode >= backupsonNode) && !removalFromRblRequested(backupsToKeepInRollbackList)) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
                jobLogMessage = String.format(JobLogConstants.CV_AVAILABLE_LESS_THAN_OR_EQUAL_TO_CV_TO_KEEP_ON_NODE, backupsonNode, backupsToKeepOnNode, getActivityType());
                preCheckUpdateLog(activityJobId, jobLogMessage, nodeName, jobExecutionUser);
            } else if ((removalFromRblRequested(backupsToKeepInRollbackList) && backupsToKeepInRollbackList >= rollbackList.size()) && !removalFromNodeRequested(backupsToKeepOnNode)) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
                jobLogMessage = String.format(JobLogConstants.CV_IN_ROLLBACK_LIST_IS_LESS_THAN_OR_EQUAL_TO_CV_TO_KEEP_IN_ROLLBACK_LIST, rollbackList.size(), backupsToKeepInRollbackList,
                        getActivityType());
                preCheckUpdateLog(activityJobId, jobLogMessage, nodeName, jobExecutionUser);
            } else {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
                jobLogMessage = String.format(JobLogConstants.PRECHECK_SUCCESS, getActivityType());
                preCheckUpdateLog(activityJobId, jobLogMessage, nodeName, jobExecutionUser);
            }
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobId);
        } catch (final Exception e) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            jobLogMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, getActivityType(), e.getMessage());
            jobLogLevel = JobLogLevel.ERROR.toString();
            LOGGER.error(jobLogMessage, e);
            updateJobLog(activityJobId, jobLogMessage, null, jobLogLevel);
            activityUtils.recordEvent(jobExecutionUser, SHMEvents.CLEAN_CV_PRECHECK, nodeName, "", "SHM:" + activityJobId + ":" + nodeName + ":" + jobLogMessage);
        }
        if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
        } else {
            LOGGER.info("Skipping persisting step duration as activity is to be skipped or failed for activityJobId {}", activityJobId);
        }
        return activityStepResult;
    }

    private void preCheckUpdateLog(final long activityJobId, final String jobLogMessage, final String nodeName, final String jobExecutionUser) {
        String jobLogLevel;
        jobLogLevel = JobLogLevel.INFO.toString();
        updateJobLog(activityJobId, jobLogMessage, null, jobLogLevel);
        activityUtils.recordEvent(jobExecutionUser, SHMEvents.CLEAN_CV_PRECHECK, nodeName, "", "SHM:" + activityJobId + ":" + nodeName + ":" + jobLogMessage);
    }

    /**
     * @param cvMoAttributesMap
     * @return
     */
    private boolean isCvMOExist(final Map<String, Object> cvMoAttributesMap) {
        if (cvMoAttributesMap != null && !cvMoAttributesMap.isEmpty()) {
            return true;
        }
        return false;
    }

    private ActivityStepResult failPreCheck(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final String logMessage = String.format(JobLogConstants.MO_NOT_EXIST, getActivityType(), BackupActivityConstants.CV_MO_TYPE);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
        return activityStepResult;
    }

    /**
     * This method performs HouseKeeping of backups on node.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     */
    @SuppressWarnings("unchecked")
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();
        LOGGER.debug("CleanupCvService execute() with nodeName {}", nodeName);
        final long mainJobId = jobEnvironment.getMainJobId();
        String cvMoFdn = null;
        Map<String, Object> cvMoAttr = new HashMap<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        int backupsToKeepOnNode = 0;
        int backupsToKeepInRollbackList = 0;
        boolean removalFromRollbackListFailed = false;
        boolean isDeleteEligibleBackupsFromRollBackList = false;
        boolean isDeletionFromNodeFailed = false;
        boolean isJobFailed = false;
        try {
            final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
            final Map<String, Object> cvMoAttributesMap = getConfigurationVersionMo(nodeName);
            if (cvMoAttributesMap != null) {
                cvMoFdn = (String) cvMoAttributesMap.get(ShmConstants.FDN);
                cvMoAttr = (Map<String, Object>) cvMoAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
            }
            String jobExecutionUser = activityUtils.getJobExecutionUser(mainJobId);
            activityUtils.recordEvent(jobExecutionUser, getNotificationEventType(), nodeName, cvMoFdn, "SHM:" + activityJobId + ":" + nodeName);
            systemRecorder.recordCommand(jobExecutionUser, SHMEvents.CLEAN_CV_SERVICE, CommandPhase.STARTED, nodeName, "",
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP_HOUSEKEEPING));
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, "true");
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
            jobPropertyList.clear();
            final BackupHouseKeepingCriteria backupHouseKeepingCriteria = getHousekeepingCriteria(mainJobAttributes, nodeName);
            if (!backupHouseKeepingCriteria.isCvPurgeRequested()) {
                backupsToKeepOnNode = backupHouseKeepingCriteria.getMaxbackupsToKeepOnNode();
                backupsToKeepInRollbackList = backupHouseKeepingCriteria.getBackupsToKeepInRollBackList();
                isDeleteEligibleBackupsFromRollBackList = backupHouseKeepingCriteria.isDeleteEligibleBackupsFromRollBackList();
            }
            final List<String> rollbackList = (List<String>) cvMoAttr.get(ConfigurationVersionMoConstants.ROLLBACK_LIST);
            if (removalFromRblRequested(backupsToKeepInRollbackList)) {
                removalFromRollbackListFailed = removeFromRollBackList(cvMoFdn, rollbackList, backupsToKeepInRollbackList, jobLogList, activityJobId);
            }

            if (removalFromNodeRequested(backupsToKeepOnNode)) {
                isDeletionFromNodeFailed = deleteCvs(backupsToKeepOnNode, jobLogList, activityJobId, nodeName, isDeleteEligibleBackupsFromRollBackList);
            }

            isJobFailed = isJobFailed(removalFromRollbackListFailed, isDeletionFromNodeFailed);
        } catch (final Exception exception) {
            isJobFailed = true;
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, "Unable to proceed for deletion of CV's from Node." + String.format(JobLogConstants.FAILURE_REASON, exception.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            LOGGER.error("Unable to proceed for deletion of CV's from Node." + String.format(JobLogConstants.FAILURE_REASON, exception.getMessage()), exception);
        }
        final Map<String, Object> activityJobAttrs = jobEnvironment.getActivityJobAttributes();
        final String existingStepDurations = ((String) activityJobAttrs.get(ShmConstants.STEP_DURATIONS));
        if (existingStepDurations != null && !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {
            final long activityStartTime = ((Date) activityJobAttrs.get(ShmConstants.ACTIVITY_START_DATE)).getTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
        }
        persistActivityJobAttributes(activityJobId, cvMoFdn, jobLogList, jobPropertyList, isJobFailed);
    }

    private boolean removalFromRblRequested(final int backupsToKeepInRollbackList) {
        return backupsToKeepInRollbackList != -1;
    }

    private boolean removalFromNodeRequested(final int backupsToKeepOnNode) {
        return backupsToKeepOnNode != -1;
    }

    private boolean isJobFailed(final boolean removalFromRollbackListFailed, final boolean isDeletionFromNodeFailed) {
        boolean jobFailed = false;
        if (removalFromRollbackListFailed || isDeletionFromNodeFailed) {
            jobFailed = true;
        }
        return jobFailed;
    }

    private void persistActivityJobAttributes(final long activityJobId, final String cvMoFdn, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList,
            final boolean isJobFailed) {
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();
        final long mainJobId = jobEnvironment.getMainJobId();
        JobResult jobResult = null;
        final String jobExecutionUser = activityUtils.getJobExecutionUser(mainJobId);
        if (activityUtils.cancelTriggered(activityJobId)) {
            jobResult = JobResult.CANCELLED;
            systemRecorder.recordCommand(jobExecutionUser, SHMEvents.CLEAN_CV_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP_HOUSEKEEPING));
        } else if (isJobFailed) {
            jobResult = JobResult.FAILED;
            systemRecorder.recordCommand(jobExecutionUser, SHMEvents.CLEAN_CV_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP_HOUSEKEEPING));
        } else {
            jobResult = JobResult.SUCCESS;
            systemRecorder.recordCommand(jobExecutionUser, SHMEvents.CLEAN_CV_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, cvMoFdn,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP_HOUSEKEEPING));
        }
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.sendNotificationToWFS(jobEnvironment, activityJobId, getActivityType(), null);
    }

    private boolean removeFromRollBackList(final String cvMoFdn, final List<String> rollbackList, final int backupsToKeepInRollbackList, final List<Map<String, Object>> jobLogList,
            final long activityJobId) {
        boolean removalFromRollbackListFailed = true;
        final int maxNoOfCvsToBeDeletedFromRbl = rollbackList.size() - backupsToKeepInRollbackList;
        if (rollbackList != null && backupsToKeepInRollbackList >= rollbackList.size()) {
            JobEnvironment jobEnvironment = null;
            jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    "No CV was removed from the Rollback list. Number of CV's in RollbackList : " + rollbackList.size() + " , Number of CV's to keep in RollbackList : " + backupsToKeepInRollbackList,
                    new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            final Double currentProgressPercentage = activityAndNEJobProgressCalculator.calculateActivityProgressPercentage(jobEnvironment, 1, DELETE_ROLLBACK_LIST_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, currentProgressPercentage);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
            removalFromRollbackListFailed = false;
        } else {
            String[] rblArray = new String[rollbackList.size()];
            rblArray = rollbackList.toArray(rblArray);
            int noOfRemovedCvs = 0;
            for (int arrayIndex = rollbackList.size() - 1; arrayIndex > backupsToKeepInRollbackList - 1; arrayIndex--) {
                JobEnvironment jobEnvironment = null;
                jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
                try {
                    if (activityUtils.cancelTriggered(activityJobId)) {
                        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_CANCELLED, getActivityType()), new Date(), JobLogType.SYSTEM.toString(),
                                JobLogLevel.ERROR.toString());
                        break;
                    }
                    final Map<String, Object> actionArgument = new HashMap<String, Object>();
                    actionArgument.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, rblArray[arrayIndex]);
                    commonCvOperations.executeActionOnMo(BackupActivityConstants.REMOVE_ROLLBACK_LIST, cvMoFdn, actionArgument);
                    jobLogUtil.prepareJobLogAtrributesList(jobLogList, "CV : " + rblArray[arrayIndex] + " removed from Rollback List.", new Date(), JobLogType.SYSTEM.toString(),
                            JobLogLevel.INFO.toString());
                    noOfRemovedCvs++;
                    final Double currentProgressPercentage = activityAndNEJobProgressCalculator.calculateActivityProgressPercentage(jobEnvironment, maxNoOfCvsToBeDeletedFromRbl,
                            DELETE_ROLLBACK_LIST_END_PROGRESS_PERCENTAGE);
                    activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, currentProgressPercentage);
                    activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
                } catch (final Exception e) {
                    final String logMsg = "Unable to remove CV : " + rblArray[arrayIndex] + " from Rollback List.";
                    logFailure(jobLogList, logMsg, e);
                }
            }
            if (noOfRemovedCvs == maxNoOfCvsToBeDeletedFromRbl) {
                removalFromRollbackListFailed = false;
            }

            addLogMsgForDeletedRblCount(noOfRemovedCvs, rollbackList.size(), backupsToKeepInRollbackList, jobLogList);
        }
        return removalFromRollbackListFailed;
    }

    private void addLogMsgForDeletedRblCount(final int noOfRemovedCvs, final int noOfRollbackListCvs, final int backupsToKeepInRollbackList, final List<Map<String, Object>> jobLogList) {
        String logMessage = "Number of CV's in RollbackList : " + noOfRollbackListCvs + " , Number of CV's to keep in RollbackList : " + backupsToKeepInRollbackList;
        if (noOfRemovedCvs == 0) {
            logMessage = logMessage + " , No CV was removed from the Rollback list";
        } else {
            logMessage = logMessage + " , Total No of CV's removed from Rollback List : " + noOfRemovedCvs;
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
    }

    private void logFailure(final List<Map<String, Object>> jobLogList, final String logMessage, final Exception e) {
        LOGGER.error("{} Exception: ", logMessage, e);
        final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
        if (!exceptionMessage.isEmpty()) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        } else {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage + String.format(JobLogConstants.FAILURE_REASON, e.getMessage()), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        }
    }

    /**
     * This method deletes CVs from the node which are not part of loaded, setStartable and rollback List. But deletes CVs from rollback list and from the node when
     * DeleteEligibleBackupsFromRollBackList is requested.
     * 
     * @param backupsToKeepOnNode
     * @param jobLogList
     * @param activityJobId
     * @param nodeName
     * @return
     */

    @SuppressWarnings({ "unchecked", "unused" })
    private boolean deleteCvs(final int backupsToKeepOnNode, final List<Map<String, Object>> jobLogList, final long activityJobId, final String nodeName,
            final boolean isDeleteEligibleBackupsFromRollBackList) {
        boolean isDeletionFailed = true;
        Map<String, Object> cvMoAttr = null;
        String cvMoFdn = null;
        final Map<String, Object> cvMoAttrMap = getConfigurationVersionMo(nodeName);
        if (cvMoAttrMap != null) {
            cvMoAttr = (Map<String, Object>) cvMoAttrMap.get(ShmConstants.MO_ATTRIBUTES);
            cvMoFdn = (String) cvMoAttrMap.get(ShmConstants.FDN);
        }
        final List<Map<String, String>> storedConfigurationVersionList = (List<Map<String, String>>) cvMoAttr.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION);
        final List<String> rollbackListAfterRemoval = (List<String>) cvMoAttr.get(ConfigurationVersionMoConstants.ROLLBACK_LIST);
        if (storedConfigurationVersionList != null && backupsToKeepOnNode >= storedConfigurationVersionList.size()) {
            JobEnvironment jobEnvironment = null;
            jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList,
                    "No CV was deleted from Node. Number of CV's on Node : " + storedConfigurationVersionList.size() + ", Number of CV's to keep on Node : " + backupsToKeepOnNode, new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            final Double currentProgressPercentage = activityAndNEJobProgressCalculator.calculateActivityProgressPercentage(jobEnvironment, 1, DELETE_NODE_BACKUP_LIST_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, currentProgressPercentage);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
            return false;
        }
        final StoredConfigurationListComparator storedConfig = new StoredConfigurationListComparator();
        try {
            Collections.sort(storedConfigurationVersionList, storedConfig);
            final List<String> cvNameList = cvUtil.getCvNames(storedConfigurationVersionList);
            String[] storedCvArray = new String[cvNameList.size()];
            storedCvArray = cvNameList.toArray(storedCvArray);
            int noOfDeletedCvs = 0;
            int noOfNonDeletableCvs = 0;
            final int maxNoOfCvsToBeDeleted = storedCvArray.length - backupsToKeepOnNode;
            int arrayIndex = 0;
            while (arrayIndex < maxNoOfCvsToBeDeleted) {
                JobEnvironment jobEnvironment = null;
                jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
                final String cvToBeDeleted = storedCvArray[arrayIndex];
                try {
                    if (activityUtils.cancelTriggered(activityJobId)) {
                        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTION_CANCELLED, getActivityType()), new Date(), JobLogType.SYSTEM.toString(),
                                JobLogLevel.ERROR.toString());
                        break;
                    }
                    if (isCvDeletable(cvMoAttr, cvToBeDeleted, rollbackListAfterRemoval, jobLogList, isDeleteEligibleBackupsFromRollBackList)) {
                        deleteFromRblIfRequested(cvMoFdn, jobLogList, cvToBeDeleted, rollbackListAfterRemoval, isDeleteEligibleBackupsFromRollBackList);
                        deleteCv(cvMoFdn, jobLogList, cvToBeDeleted);
                        noOfDeletedCvs++;
                        final Double currentProgressPercentage = activityAndNEJobProgressCalculator.calculateActivityProgressPercentage(jobEnvironment, maxNoOfCvsToBeDeleted,
                                DELETE_NODE_BACKUP_LIST_END_PROGRESS_PERCENTAGE);
                        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, currentProgressPercentage);
                        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
                    } else {
                        noOfNonDeletableCvs++;
                    }
                } catch (final Exception e) {
                    final String logMsg = "Unable to delete CV : " + cvToBeDeleted + ". ";
                    logFailure(jobLogList, logMsg, e);
                }
                arrayIndex++;
            }
            if (noOfDeletedCvs == (maxNoOfCvsToBeDeleted - noOfNonDeletableCvs)) {
                isDeletionFailed = false;
            }
            addLogMessageForDeletedCvCount(noOfDeletedCvs, storedConfigurationVersionList.size(), backupsToKeepOnNode, jobLogList);
            LOGGER.info("Number of CVs to be deleted from node {} = {} and number of CVs deleted = {}", nodeName, maxNoOfCvsToBeDeleted, noOfDeletedCvs);
        } finally {
            storedConfig.cleanUp();
        }
        return isDeletionFailed;
    }

    /**
     * 
     * @param cvMoFdn
     * @param jobLogList
     * @param cvNameTobeDeleted
     */
    private void deleteCv(final String cvMoFdn, final List<Map<String, Object>> jobLogList, final String cvNameTobeDeleted) {
        final Map<String, Object> actionArgument = new HashMap<>();
        actionArgument.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, cvNameTobeDeleted);
        commonCvOperations.executeActionOnMo(BackupActivityConstants.ACTION_DELETE_CV, cvMoFdn, actionArgument);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, "CV : " + cvNameTobeDeleted + " has been deleted successfully from Node.", new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.INFO.toString());
    }

    private void deleteFromRblIfRequested(final String cvMoFdn, final List<Map<String, Object>> jobLogList, final String cvNameTobeDeleted, final List<String> rollbackListAfterRemoval,
            final boolean isDeleteEligibleBackupsFromRollBackList) {
        if (rollbackListAfterRemoval.contains(cvNameTobeDeleted) && isDeleteEligibleBackupsFromRollBackList) {
            final Map<String, Object> actionArgument = new HashMap<>();
            actionArgument.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, cvNameTobeDeleted);
            commonCvOperations.executeActionOnMo(BackupActivityConstants.REMOVE_ROLLBACK_LIST, cvMoFdn, actionArgument);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, "CV : " + cvNameTobeDeleted + " has been deleted successfully from rollback list.", new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
        }
    }

    private void addLogMessageForDeletedCvCount(final int noOfDeletedCvs, final int noOfCvsOnNode, final int backupsToKeepOnNode, final List<Map<String, Object>> jobLogList) {
        String logMessage = "Number of CV's on Node : " + noOfCvsOnNode + " , Number of CV's to keep on Node : " + backupsToKeepOnNode;
        if (noOfDeletedCvs == 0) {
            logMessage = logMessage + " , No CV was removed from Node";
        } else {
            logMessage = logMessage + " , Total No of CV's deleted from Node : " + noOfDeletedCvs;
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
    }

    private boolean isCvDeletable(final Map<String, Object> cvMoAttr, final String cvName, final List<String> rollbackList, final List<Map<String, Object>> jobLogList,
            final boolean isDeleteEligibleBackupsFromRollBackList) {
        boolean isCvDeletable = false;
        final String startableCv = (String) cvMoAttr.get(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION);
        final String currentLoadedCv = (String) cvMoAttr.get(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION);
        if (startableCv.equals(cvName)) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, "CV : " + cvName + " cannot be deleted as it is startable CV", new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } else if (currentLoadedCv.equals(cvName)) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, "CV : " + cvName + " cannot be deleted as it is the current loaded CV.", new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.ERROR.toString());
        } else if (rollbackList.contains(cvName)) {
            if (isDeleteEligibleBackupsFromRollBackList) {
                isCvDeletable = true;
            } else {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, "CV : " + cvName + " cannot be deleted as it is in RollbackList.", new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
            }
        } else {
            isCvDeletable = true;
        }
        return isCvDeletable;
    }

    /**
     * This method handles timeout scenario for Delete backup Job.
     * 
     * @param activityJobId
     * @return activityStepResult
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        LOGGER.debug("Inside DeleteCV Service handleTimeout() with activityJobId : {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final String nodeName = jobEnvironment.getNodeName();
        final long mainJobId = jobEnvironment.getMainJobId();
        final String logMessage = String.format(JobLogConstants.OPERATION_TIMED_OUT, getActivityType());
        activityUtils.recordEvent(activityUtils.getJobExecutionUser(mainJobId), SHMEvents.CLEAN_CV_TIME_OUT, nodeName, "", activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        final JobResult jobResult = evaluateJobResult(jobEnvironment, jobLogList, jobPropertyList);
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (jobResult == JobResult.SUCCESS) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
        final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
        activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        return activityStepResult;
    }

    @SuppressWarnings("unchecked")
    public JobResult evaluateJobResult(final JobEnvironment jobEnvironment, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> jobPropertyList) {
        JobResult jobResult = null;
        Map<String, Object> cvMoAttr = new HashMap<>();
        final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
        final String nodeName = jobEnvironment.getNodeName();
        int backupsToKeepOnNode = 0;
        int backupsToKeepInRollbackList = 0;
        final BackupHouseKeepingCriteria backupHouseKeepingCriteria = getHousekeepingCriteria(mainJobAttributes, nodeName);
        if (!backupHouseKeepingCriteria.isCvPurgeRequested()) {
            backupsToKeepOnNode = backupHouseKeepingCriteria.getMaxbackupsToKeepOnNode();
            backupsToKeepInRollbackList = backupHouseKeepingCriteria.getBackupsToKeepInRollBackList();
        }
        final Map<String, Object> moAttributesMap = getConfigurationVersionMo(nodeName);
        if (moAttributesMap != null) {
            cvMoAttr = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
        }
        final List<Map<String, String>> storedConfigurationVersionList = (List<Map<String, String>>) cvMoAttr.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION);
        final List<String> rollBackList = (List<String>) cvMoAttr.get(ConfigurationVersionMoConstants.ROLLBACK_LIST);
        if (isCleanupSuccess(backupsToKeepOnNode, backupsToKeepInRollbackList, storedConfigurationVersionList, rollBackList)) {
            final String logMessage = "Deletion of backups on the Node and from Rollback list is successful";
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobResult = JobResult.SUCCESS;
        } else {
            String logMessage = "Cleanup CV has failed. ";
            if (rollBackList != null && rollBackList.size() > backupsToKeepInRollbackList) {
                final String logMessageRollBackList = "Number of CV's in RollbackList : " + rollBackList.size() + " , Number of CV's to keep in RollbackList : " + backupsToKeepInRollbackList + ". ";
                logMessage = logMessage + logMessageRollBackList;
            }
            if (storedConfigurationVersionList != null && storedConfigurationVersionList.size() > backupsToKeepOnNode) {
                final String logMessageDeleteCv = "Number of CV's on Node : " + storedConfigurationVersionList.size() + " , Number of CV's to keep on Node : " + backupsToKeepOnNode;
                logMessage = logMessage + logMessageDeleteCv;
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobResult = JobResult.FAILED;
        }
        return jobResult;
    }

    private boolean isCleanupSuccess(final int backupsToKeepOnNode, final int backupsToKeepInRollbackList, final List<Map<String, String>> storedConfigurationVersionList,
            final List<String> rollBackList) {
        return (rollBackList != null && rollBackList.size() <= backupsToKeepInRollbackList) && (storedConfigurationVersionList != null && storedConfigurationVersionList.size() <= backupsToKeepOnNode);
    }

    /**
     * This method cancels the action.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        activityUtils.logCancelledByUser(jobLogList, jobEnvironment, getActivityType());
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_NOT_POSSIBLE, getActivityType()), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.WARN.toString());
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, "true");
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        // Return type will be changed to void after making cancel() asynchronous.
        return new ActivityStepResult();
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        LOGGER.debug("Inside CleanupCvService cancelTimeout() with activityJobId : {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final JobResult jobResult = evaluateJobResult(jobEnvironment, jobLogList, jobPropertyList);
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
        if (jobResult == JobResult.SUCCESS) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        return activityStepResult;
    }

    private void updateJobLog(final long activityJobId, final String logMessage, final List<Map<String, Object>> propertyList, final String logLevel) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), logLevel);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList, null);
    }

    private BackupHouseKeepingCriteria getHousekeepingCriteria(final Map<String, Object> mainJobAttr, final String nodeName) {
        LOGGER.debug("prepareJobConfigurationDetails Entry for  NodeName : {}", nodeName);
        final List<String> keyList = new ArrayList<String>();
        keyList.add(NodeBackupHousekeepingConstants.CLEAR_ALL_BACKUPS);
        keyList.add(NodeBackupHousekeepingConstants.CLEAR_ELIGIBLE_BACKUPS);
        keyList.add(NodeBackupHousekeepingConstants.BACKUPS_TO_KEEP_IN_ROLLBACK_LIST);
        keyList.add(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE);
        keyList.add(NodeBackupHousekeepingConstants.DELETE_ELIGIBLE_BACKUPS_FROM_ROLLBACK_LIST);
        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keyList, mainJobAttr, nodeName);
        LOGGER.debug("keyValueMap in prepareJobConfigurationDetails {}", keyValueMap);
        final BackupHouseKeepingCriteria backupHouseKeepingCriteria = new BackupHouseKeepingCriteria(keyValueMap);
        return backupHouseKeepingCriteria;
    }

    @Override
    public String getActivityType() {
        return ActivityConstants.CLEAN_CV;
    }

    @Override
    public String getNotificationEventType() {
        return SHMEvents.CLEAN_CV_EXECUTE;
    }

}
