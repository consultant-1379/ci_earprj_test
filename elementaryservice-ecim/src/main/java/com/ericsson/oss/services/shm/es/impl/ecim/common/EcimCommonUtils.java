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
package com.ericsson.oss.services.shm.es.impl.ecim.common;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimCommonConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

public class EcimCommonUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(EcimCommonUtils.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxy;

    @SuppressWarnings("unchecked")
    public AsyncActionProgress getValidAsyncActionProgress(final String activityName, final Map<String, AttributeChangeData> modifiedAttributes) {
        AsyncActionProgress asyncActionProgress = null;
        final AttributeChangeData modifiedNotifiableAttribute = modifiedAttributes.get(EcimCommonConstants.LicenseMoConstants.KEYFILEMANAGEMENT_REPORT_PROGRESS);
        if (modifiedNotifiableAttribute != null) {
            LOGGER.info("modifiedNotifiableAttribute=={}, activityName={}", modifiedNotifiableAttribute, activityName);
            asyncActionProgress = new AsyncActionProgress((Map<String, Object>) modifiedNotifiableAttribute.getNewValue());
        }
        return asyncActionProgress;
    }

    /**
     * It will handle the MoAction Progress Report State and prepare Job log messages based on ProgressReport and will return the JobResult
     * 
     * @param jobLogList
     * @param activityJobId
     * @param progressReport
     * @param notificationTime
     * @param activityName
     * @param backupName
     * @return JobResult
     */
    public JobResult handleMoActionProgressReportState(final List<Map<String, Object>> jobLogList, final long activityJobId, final AsyncActionProgress progressReport, final Date notificationTime,
            final String activityName, final String backupName) {
        final ActionStateType state = progressReport.getState();
        final ActionResultType result = progressReport.getResult();
        LOGGER.debug("handleMoActionProgressReportState: state {},result {}", state, result);
        JobResult jobResult = null;
        String jobLogMessage = null;
        switch (state) {
            case RUNNING:
                if (backupName != null && !backupName.isEmpty()) {
                    jobLogMessage = "For the Backup: " + backupName + " "
                            + String.format(JobLogConstants.PROGRESS_INFORMATION, progressReport.getActionName(), progressReport.getProgressPercentage(), progressReport.getState());
                } else {
                    jobLogMessage = String.format(JobLogConstants.PROGRESS_INFORMATION, progressReport.getActionName(), progressReport.getProgressPercentage(), progressReport.getState());
                }
                if (progressReport.getProgressInfo() != null && progressReport.getProgressInfo() != "") {
                    jobLogMessage = jobLogMessage + " Progress Information = \"" + progressReport.getProgressInfo() + "\"";
                }
                if (progressReport.getAdditionalInfo() != null && progressReport.getAdditionalInfo() != "") {
                    jobLogMessage = jobLogMessage + " Additional Information = \"" + progressReport.getAdditionalInfo() + "\"";
                }
                activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, notificationTime, JobLogType.NE.toString(), JobLogLevel.INFO.toString());
                break;
            case FINISHED:
                if (backupName != null && !backupName.isEmpty()) {
                    jobLogMessage = "For the Backup: "
                            + backupName
                            + " "
                            + String.format(JobLogConstants.PROGRESS_INFORMATION_WITH_RESULT, progressReport.getActionName(), progressReport.getProgressPercentage(), progressReport.getState(),
                                    progressReport.getResult());
                } else {
                    jobLogMessage = String.format(JobLogConstants.PROGRESS_INFORMATION_WITH_RESULT, progressReport.getActionName(), progressReport.getProgressPercentage(), progressReport.getState(),
                            progressReport.getResult());
                }
                if (progressReport.getProgressInfo() != null && progressReport.getProgressInfo() != "") {
                    jobLogMessage = jobLogMessage + " Progress Information = \"" + progressReport.getProgressInfo() + "\"";
                }
                if (progressReport.getAdditionalInfo() != null && progressReport.getAdditionalInfo() != "") {
                    jobLogMessage = jobLogMessage + " Additional Information = \"" + progressReport.getAdditionalInfo() + "\"";
                }
                activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, notificationTime, JobLogType.NE.toString(), JobLogLevel.INFO.toString());
                if (ActionResultType.SUCCESS.equals(result)) {
                    jobResult = JobResult.SUCCESS;
                    jobLogMessage = String.format(JobLogConstants.ACTIVITY_FOR_BACKUP_COMPLETED_SUCCESSFULLY, activityName, backupName);
                    LOGGER.debug(jobLogMessage);
                    activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, notificationTime, JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                } else if (ActionResultType.FAILURE.equals(result)) {
                    jobResult = JobResult.FAILED;
                    jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, activityName + " for " + backupName) + progressReport.getResultInfo();
                    LOGGER.debug(jobLogMessage);
                    activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, notificationTime, JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                }
                break;
            case CANCELLING:
                jobLogMessage = String.format(JobLogConstants.CANCEL_IN_PROGRESS, progressReport.getProgressPercentage(), progressReport.getProgressInfo());
                if (progressReport.getAdditionalInfo() != null && progressReport.getAdditionalInfo() != "") {
                    jobLogMessage = jobLogMessage + " Additional Information = \"" + progressReport.getAdditionalInfo() + "\"";
                }
                activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, notificationTime, JobLogType.NE.toString(), JobLogLevel.INFO.toString());
                break;
            case CANCELLED:
                jobResult = JobResult.FAILED;
                jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, activityName) + progressReport.getResultInfo();
                LOGGER.debug(jobLogMessage);
                activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, notificationTime, JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                break;
            default:
                jobResult = JobResult.FAILED;
                LOGGER.warn("Unsupported Action State Type {} for activityJobId {}", state, activityJobId);
        }
        return jobResult;
    }

    /**
     * It will handle the Cancel Action Progress Report State and prepare Job log messages based ProgressReport and will return the JobResult
     * 
     * @param jobLogList
     * @param activityJobId
     * @param progressReport
     * @param notificationTime
     * @param activityName
     * @return JobResult
     */
    public JobResult handleCancelProgressReportState(final List<Map<String, Object>> jobLogList, final long activityJobId, final AsyncActionProgress progressReport, final Date notificationTime,
            final String activityName) {
        final ActionStateType state = progressReport.getState();
        final ActionResultType result = progressReport.getResult();
        LOGGER.debug("handleCancelProgressReportState: state {},result {}", state, result);
        JobResult jobResult = JobResult.FAILED;
        String jobLogMessage = "";
        switch (state) {
            case RUNNING:
                jobLogMessage = String.format(JobLogConstants.PROGRESS_INFORMATION, progressReport.getActionName(), progressReport.getProgressPercentage(), progressReport.getState());
                if (progressReport.getProgressInfo() != null && progressReport.getProgressInfo() != "") {
                    jobLogMessage = jobLogMessage + " Progress Information = \"" + progressReport.getProgressInfo() + "\"";
                }
                if (progressReport.getAdditionalInfo() != null && progressReport.getAdditionalInfo() != "") {
                    jobLogMessage = jobLogMessage + " Additional Information = \"" + progressReport.getAdditionalInfo() + "\"";
                }
                activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, notificationTime, JobLogType.NE.toString(), JobLogLevel.INFO.toString());
                break;
            case FINISHED:
                jobLogMessage = String.format(JobLogConstants.PROGRESS_INFORMATION_WITH_RESULT, progressReport.getActionName(), progressReport.getProgressPercentage(), progressReport.getState(),
                        progressReport.getResult());
                if (progressReport.getProgressInfo() != null && progressReport.getProgressInfo() != "") {
                    jobLogMessage = jobLogMessage + " Progress Information = \"" + progressReport.getProgressInfo() + "\"";
                }
                if (progressReport.getAdditionalInfo() != null && progressReport.getAdditionalInfo() != "") {
                    jobLogMessage = jobLogMessage + " Additional Information = \"" + progressReport.getAdditionalInfo() + "\"";
                }
                activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, notificationTime, JobLogType.NE.toString(), JobLogLevel.INFO.toString());
                if (ActionResultType.SUCCESS.equals(result)) {
                    jobResult = JobResult.CANCELLED;
                    jobLogMessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, progressReport.getActionName());
                    LOGGER.debug(jobLogMessage);
                } else if (ActionResultType.FAILURE.equals(result)) {
                    jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, progressReport.getActionName()) + progressReport.getResultInfo();
                }
                activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, notificationTime, JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                break;
            default:
                LOGGER.warn("Unsupported Action State Type {} for activityJobId {}", state, activityJobId);
        }
        return jobResult;
    }

    /**
     * This method used to get the Count of Total backups.
     * 
     * @param activityJobAttributes
     * @return totalBackups
     * 
     */
    public int getCountOfTotalBackups(final Map<String, Object> activityJobAttributes) {
        int totalBackups = 0;
        final String totalBackupsString = activityUtils.getActivityJobAttributeValue(activityJobAttributes, EcimBackupConstants.TOTAL_BACKUPS);
        if (totalBackupsString != null && !totalBackupsString.isEmpty()) {
            totalBackups = Integer.parseInt(totalBackupsString);
        }
        LOGGER.debug("Count of total backups : {}", totalBackups);
        return totalBackups;
    }

    /**
     * To calculate the Activity Progress Percentage
     * 
     * @param activityJobId
     * @param progressReport
     * @return
     */
    public double calculateActivityProgressPercentage(final long activityJobId, final AsyncActionProgress progressReport) {
        double currentProgressPercentage = 0.0;
        double totalActivityProgressPercentage = 0.0;
        double eachMO_End_ProgressPercentage = 0.0;
        final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
        final int totalBkps = getCountOfTotalBackups(activityJobAttributes);
        final String eachMO_End_ProgressPercentageString = activityUtils.getActivityJobAttributeValue(activityJobAttributes, BackupActivityConstants.MO_ACTIVITY_END_PROGRESS);
        if (eachMO_End_ProgressPercentageString != null && !eachMO_End_ProgressPercentageString.isEmpty()) {
            eachMO_End_ProgressPercentage = Double.parseDouble(eachMO_End_ProgressPercentageString);
        }
        final Double progressPercentage = (double) progressReport.getProgressPercentage();
        currentProgressPercentage = Math.round((progressPercentage / totalBkps) * 100);
        currentProgressPercentage = currentProgressPercentage / 100;
        totalActivityProgressPercentage = eachMO_End_ProgressPercentage + currentProgressPercentage;
        totalActivityProgressPercentage = Math.round(totalActivityProgressPercentage * 100.0) / 100.0;
        return totalActivityProgressPercentage;
    }

    public boolean validateActionProgressReportAlongWithBackupName(final AsyncActionProgress progressReport, final String activityName, final String correlationActivityName, final String backupName) {
        if ((progressReport == null)
                || (!(activityName.equals(progressReport.getActionName()) || correlationActivityName.equals(progressReport.getActionName()) || EcimBackupConstants.BACKUP_CANCEL_ACTION
                        .equals(progressReport.getActionName())))) {
            return false;
        }

        //Check for backup name in additionalInfo. This check is implemented to identify whether the action triggered is of the same backup or of a different backup.
        return validateBackupNameInProgressReport(progressReport.getAdditionalInfo(), backupName);
    }

    private boolean validateBackupNameInProgressReport(final String additionalInfo, final String backupName) {
        boolean isValidNotification = true;
        if (!StringUtils.isEmpty(additionalInfo) && additionalInfo.contains(EcimBackupConstants.BACKUP_NAME_IN_ADDITIONAL_INFO)) {
            String backupNameInAdditionalInfo = null;
            final String[] listOfAdditionalInfo = additionalInfo.split(",");
            for (String everyAdditionalInfo : listOfAdditionalInfo) {
                everyAdditionalInfo = everyAdditionalInfo.trim();
                if (everyAdditionalInfo.startsWith(EcimBackupConstants.BACKUP_NAME_IN_ADDITIONAL_INFO)) {

                    backupNameInAdditionalInfo = everyAdditionalInfo.substring(EcimBackupConstants.BACKUP_NAME_IN_ADDITIONAL_INFO.length());
                    if (backupNameInAdditionalInfo.endsWith("]")) {
                        backupNameInAdditionalInfo = backupNameInAdditionalInfo.substring(0, backupNameInAdditionalInfo.lastIndexOf(']'));
                    }
                    if (!backupName.equalsIgnoreCase(backupNameInAdditionalInfo)) {
                        isValidNotification = false;
                    }
                    break;
                }
            }
        }
        return isValidNotification;
    }

}
