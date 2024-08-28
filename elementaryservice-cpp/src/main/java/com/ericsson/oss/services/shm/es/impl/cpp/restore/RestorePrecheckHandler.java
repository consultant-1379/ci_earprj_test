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
package com.ericsson.oss.services.shm.es.impl.cpp.restore;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.impl.cpp.restore.RestoreServiceConstants.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.retry.SmrsRetryPolicies;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVCurrentDetailedActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionService;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.InstallActivityHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@SuppressWarnings("PMD.TooManyFields")
public class RestorePrecheckHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestorePrecheckHandler.class);

    @Inject
    SmrsFileStoreService smrsServiceUtil;

    @Inject
    ActivityUtils activityUtils;

    @Inject
    JobUpdateService jobUpdateService;

    @Inject
    ConfigurationVersionService cvService;

    @Inject
    ConfigurationVersionUtils configurationVersionUtils;

    @Inject
    SmrsRetryPolicies smrsRetryPolicies;

    @Inject
    private InstallActivityHandler installActivityHandler;

    @Inject
    private UpgradePackageService upgradePackageService;

    @Inject
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    protected SystemRecorder systemRecorder;

    public static final String DOWNLOADED_CV_TYPE = "DOWNLOADED";

    private static final String DELIMITER_PIPE = "|";

    public ActivityStepResult getRestorePrecheckResult(final long activityJobId, final String activity, final String activityName) {
        NEJobStaticData neJobStaticData;
        JobStaticData jobStaticData = null;
        final ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, activityName);
            if (!isUserAuthorized) {
                return activityStepResult;
            } else {
                activityStepResult = getRestorePrecheckStatus(activityJobId, activity);
            }
        } catch (final JobDataNotFoundException e) {
            final String jobLogMessage = JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE;
            LOGGER.error("JobDataNotFoundException occurred for activityJobId : {}. Exception is : {}", activityJobId, e);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception e) {
            LOGGER.error("Exception Occured at restore precheck  for activityId={} ,Exception is : {}", activityJobId, e);
            final String jobLogMessage = String.format(JobLogConstants.PRE_CHECK_FAILURE, activityName, e.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        return activityStepResult;

    }

    public ActivityStepResult getRestorePrecheckStatus(final long activityJobId, final String activity) {
        LOGGER.debug("Entering preCheck of {} with activityJobId : {}", activity, activityJobId);
        NEJobStaticData neJobStaticData = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        ActivityStepResultEnum preCheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
        String nodeName = "";
        long neJobId = 0L;
        long activityStartTime = 0L;
        activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_INITIATED, activity), JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.INFO.toString());
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY);
            if (neJobStaticData != null) {
                nodeName = neJobStaticData.getNodeName();
                neJobId = neJobStaticData.getNeJobId();
                activityStartTime = neJobStaticData.getActivityStartTime();
            }

            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, propertyList, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobId);
            final Map<String, Object> cvMo = cvService.getCVMoAttr(nodeName);
            final String treatAsInfo = activityUtils.isTreatAs(nodeName);
            if (treatAsInfo != null) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }
            if (cvMo.size() == 0) {
                LOGGER.warn("CVMO does not exist for ActivityJobId:{}", activityJobId);
                activityUtils.addJobLog(String.format(JobLogConstants.MO_NOT_EXIST, activity, BackupActivityConstants.CV_MO_TYPE), JobLogType.SYSTEM.toString(), jobLogList,
                        JobLogLevel.ERROR.toString());
                preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            } else {
                preCheckStatus = getPrecheckStatus(cvMo, neJobStaticData, jobLogList, propertyList, activity, activityJobId);
            }
        } catch (final Exception e) {
            LOGGER.error("Exception Occured at restore precheck  for activityId={} ,NodeName={},with preCheckStatus={}", activityJobId, nodeName, preCheckStatus.toString(), e);
        }
        if (preCheckStatus == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, propertyList, jobLogList, PRECHECK_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobId);
        } else {
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList, null);
        }
        if (preCheckStatus == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            try {
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PRECHECK);
            } catch (final Exception ex) {
                LOGGER.error("Skipping persisting step duration. Exception occurred {} ", ex.getMessage());
            }
        } else {
            LOGGER.debug("Skipping persisting step duration as activity is to be skipped.");
        }
        LOGGER.debug("Exiting preCheck of {} for activityId={} ,NodeName={},with preCheckStatus={}", activity, activityJobId, nodeName, preCheckStatus);
        return activityUtils.getActivityStepResult(preCheckStatus);
    }

    /**
     * @param cvMo
     * @param jobLogList
     * @param activityState
     * @param activity
     * @return
     */
    private ActivityStepResultEnum getPrecheckStatus(final Map<String, Object> cvMo, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList,
            final List<Map<String, Object>> propertyList, final String activity, final long activityJobId) {
        switch (activity) {
        case ActivityConstants.DOWNLOAD_CV: {
            return downloadPrecheckResult(cvMo, neJobStaticData, activityJobId, jobLogList);
        }
        case ActivityConstants.VERIFY_RESTORE_CV: {
            return verifyRestorePrecheckResult(cvMo, neJobStaticData, activityJobId, jobLogList);
        }
        case ActivityConstants.CONFIRM_RESTORE_CV: {
            return confirmPrecheckResult(cvMo, neJobStaticData, activityJobId, jobLogList);
        }
        case ActivityConstants.RESTORE_INSTALL_CV: {
            return installPrecheckResult(cvMo, neJobStaticData, propertyList, jobLogList, activityJobId);
        }
        default: {
            return defaultPrecheck(neJobStaticData.getNodeName(), (String) cvMo.get(ShmConstants.FDN), activityJobId, activity, jobLogList,
                    activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()));
        }
        }
    }

    /**
     * @param jobLogList
     * @param activityState
     * @param activityJobId
     * @param cvMoFdn
     * @param nodeName
     * @param actionType
     * @return
     */
    private ActivityStepResultEnum defaultPrecheck(final String nodeName, final String cvMoFdn, final long activityJobId, final String activity, final List<Map<String, Object>> jobLogList,
            final String jobExecutionUser) {
        final String eventType = getEventType(activity);
        systemRecorder.recordEvent(jobExecutionUser, eventType, EventLevel.COARSE, nodeName, cvMoFdn, "SHM:" + activityJobId + ":" + nodeName);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.PRE_CHECK_SUCCESS, activity), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        return ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
    }

    /**
     * @param activity
     * @return
     */
    private String getEventType(final String activity) {
        switch (activity) {
        case ActivityConstants.VERIFY_RESTORE_CV:
            return SHMEvents.VERIFY_PRECHECK;
        case ActivityConstants.RESTORE:
            return SHMEvents.RESTORE_PRECHECK;
        default:
            return null;
        }
    }

    /**
     * @param actionType
     * @param jobLogList
     * @param activityJobId
     * @param string
     * @param cvMoAttribute
     * @param cvMoFdn
     * @param mainJobAttributes
     * @return
     */
    @SuppressWarnings("unchecked")
    private ActivityStepResultEnum confirmPrecheckResult(final Map<String, Object> cvMo, final NEJobStaticData neJobStaticData, final long activityJobId, final List<Map<String, Object>> jobLogList) {
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
        final String nodeName = neJobStaticData.getNodeName();
        final String cvMoFdn = (String) cvMo.get(ShmConstants.FDN);
        final Map<String, Object> cvMoAttribute = (Map<String, Object>) cvMo.get(ShmConstants.MO_ATTRIBUTES);
        final String configurationVersionName = configurationVersionUtils.getNeJobPropertyValue(mainJobAttributes, nodeName, BackupActivityConstants.CV_NAME); //Fetching CV Name
        final String currentDetailedActivityValue = (String) cvMoAttribute.get(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY);
        final CVCurrentDetailedActivity currentDetailedActivity = CVCurrentDetailedActivity.getDetailedActivity(currentDetailedActivityValue);
        ActivityStepResultEnum preCheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
        String logMessage = null;

        final String cvType = getCVTypeIfPresentOnNode(((List<Map<String, Object>>) cvMoAttribute.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)), configurationVersionName);

        if (cvType == null) {
            preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            logMessage = "Precheck failed for the confirm activity as selected CV does not exist on node ";
        } else if (!cvType.equalsIgnoreCase(DOWNLOADED_CV_TYPE)) {
            preCheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION;
            logMessage = String.format(JobLogConstants.SKIP_RESTORE_JOB_CV_EXISTS_ON_NODE, configurationVersionName, cvType, ShmConstants.RESTORE_CONFIRM_ACTIVITY);
        } else {
            if (CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION.equals(currentDetailedActivity)) {
                preCheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
                logMessage = "Proceeding with Confirm Restore action for activity job id " + activityJobId;
            } else {
                preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
                logMessage = "Unable to process Confirm Restore action for activity job id " + activityJobId;
            }
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        systemRecorder.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.CONFIRM_RESTORE_PRECHECK, EventLevel.COARSE, nodeName, cvMoFdn,
                "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        return preCheckStatus;
    }

    /**
     * @param actionType
     * @param jobLogList
     * @param activityJobId
     * @param string
     * @param cvMoAttribute
     * @param cvMoFdn
     * @param mainJobAttributes
     * @return
     */

    @SuppressWarnings("unchecked")
    private ActivityStepResultEnum installPrecheckResult(final Map<String, Object> cvMo, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> propertyList,
            final List<Map<String, Object>> jobLogList, final long activityJobId) {
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
        final String nodeName = neJobStaticData.getNodeName();
        final Map<String, Object> cvMoAttribute = (Map<String, Object>) cvMo.get(ShmConstants.MO_ATTRIBUTES);
        final String configurationVersionName = configurationVersionUtils.getNeJobPropertyValue(mainJobAttributes, nodeName, BackupActivityConstants.CV_NAME); //Fetching CV Name
        ActivityStepResultEnum preCheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
        String logMessage = null;
        final String cvMoFdn = (String) cvMo.get(ShmConstants.FDN);
        String jobExecutionUser = activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId());
        final String cvType = getCVTypeIfPresentOnNode(((List<Map<String, Object>>) cvMoAttribute.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)), configurationVersionName);

        if (cvType == null) {
            preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            logMessage = "Precheck failed for the install activity as selected CV does not exist on node ";
        } else if (!cvType.equalsIgnoreCase(DOWNLOADED_CV_TYPE)) {
            preCheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION;
            logMessage = String.format(JobLogConstants.SKIP_RESTORE_JOB_CV_EXISTS_ON_NODE, configurationVersionName, cvType, ShmConstants.RESOTRE_INSTALL_ACTIVITY);
        } else {
            int preCheckFailedItems = 0;
            final List<String> missingPkgsTobeInstalled = new ArrayList<String>();
            final List<String> corruptedPkgsTobeInstalled = new ArrayList<String>();
            final Map<String, Object> jobConfiguration = (Map<String, Object>) mainJobAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
            //Read the missing packages and go for validation
            if (isPackagesSelectedToInstall(jobConfiguration, JobPropertyConstants.MISSING_PKG_SELECTION, nodeName)) {
                final List<Map<String, Object>> missingPkgsFromCV = (List<Map<String, Object>>) cvMoAttribute.get(CVMO_MISSING_UPGRADE_PACKAGES);
                LOGGER.debug("missingPkgsFromCV {}", missingPkgsFromCV);
                preCheckFailedItems = preCheckFailedItems + validateInstallPreCheck(activityJobId, missingPkgsFromCV, missingPkgsTobeInstalled, MISSING_PKGS, jobLogList, neJobStaticData);
                LOGGER.debug("Found {} missingPkgAdminData elements from CV Mo, and {} packages can be proceed for installation", missingPkgsFromCV.size(), missingPkgsTobeInstalled.size());
            }
            //Read the corrupted packages and go for validation
            if (isPackagesSelectedToInstall(jobConfiguration, JobPropertyConstants.CORRUPTED_PKG_SELECTION, nodeName)) {
                final List<Map<String, Object>> corruptedPkgsFromCV = (List<Map<String, Object>>) cvMoAttribute.get(CVMO_CORRUPTED_UPGRADE_PACKAGES);
                LOGGER.debug("corruptedPkgsFromCV {}", corruptedPkgsFromCV);
                preCheckFailedItems = preCheckFailedItems + validateInstallPreCheck(activityJobId, corruptedPkgsFromCV, corruptedPkgsTobeInstalled, CORRUPTED_PKGS, jobLogList, neJobStaticData);
                LOGGER.debug("Found {} corruptedPkgAdminData elements from CV Mo, and {} packages can be proceed for installation", corruptedPkgsFromCV.size(), corruptedPkgsTobeInstalled.size());
            }
            //Fail the precheck if any of the items failed.
            if (preCheckFailedItems > 0) {
                preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
                logMessage = "Exiting from Restore_Precheck step with status as Failed, due to " + preCheckFailedItems + " packages can not be proceed for installation.";
                LOGGER.error(logMessage);
                systemRecorder.recordEvent(jobExecutionUser, SHMEvents.INSTALL_PRECHECK, EventLevel.COARSE, nodeName, "", activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
                jobLogList.add(activityUtils.createNewLogEntry(preCheckFailedItems + " software package(s) failed in PreCheck, Install Activity can not be continued.", JobLogLevel.ERROR.toString()));
                return preCheckStatus;
            }
            //Skip the execution if packages are empty
            if (missingPkgsTobeInstalled.isEmpty() && corruptedPkgsTobeInstalled.isEmpty()) {
                preCheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION;
                logMessage = "Exiting from Restore_Precheck step with status as Success and remaining steps will not be executed, since No Packages found to be installed.";
                LOGGER.debug(logMessage);
                systemRecorder.recordEvent(jobExecutionUser, SHMEvents.INSTALL_PRECHECK, EventLevel.COARSE, nodeName, "", activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
                jobLogList.add(activityUtils.createNewLogEntry("No Packages found to be installed.", JobLogLevel.INFO.toString()));
                return preCheckStatus;
            }
            final Map<String, String> attributesTobeAdded = new HashMap<String, String>();
            attributesTobeAdded.put(CORRUPTED_PKGS, StringUtils.join(corruptedPkgsTobeInstalled, DELIMITER_PIPE));
            attributesTobeAdded.put(MISSING_PKGS, StringUtils.join(missingPkgsTobeInstalled, DELIMITER_PIPE));
            LOGGER.debug("Packages found in Precheck are ::{}", attributesTobeAdded);
            //Persist the Product IDs to be installed into ActivityJobProperties, so that they will be read in remaining steps to proceed
            if (!missingPkgsTobeInstalled.isEmpty()) {
                activityUtils.prepareJobPropertyList(propertyList, MISSING_PKGS, StringUtils.join(missingPkgsTobeInstalled, DELIMITER_PIPE));
            }
            if (!corruptedPkgsTobeInstalled.isEmpty()) {
                activityUtils.prepareJobPropertyList(propertyList, CORRUPTED_PKGS, StringUtils.join(corruptedPkgsTobeInstalled, DELIMITER_PIPE));
            }
            return preCheckStatus;
        }
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        systemRecorder.recordEvent(jobExecutionUser, SHMEvents.INSTALL_PRECHECK, EventLevel.COARSE, nodeName, cvMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
        return preCheckStatus;
    }

    /**
     * @param activityJobId
     * @param admindataList
     * @param SucceededPackagesList
     * @param typeTobeLogged
     * @param jobLogList
     * @return
     */
    private int validateInstallPreCheck(final long activityJobId, final List<Map<String, Object>> admindataList, final List<String> SucceededPackagesList, final String typeTobeLogged,
            final List<Map<String, Object>> jobLogList, final NEJobStaticData neJobStaticData) {
        int failedToExecuteItems = 0;
        for (final Map<String, Object> pkgAdminData : admindataList) {

            LOGGER.debug("Calling Common Precheck method to process for productID {}", pkgAdminData);
            final String productNumber = (String) pkgAdminData.get(CVMO_ADMINDATA_PRODUCT_NUMBER);
            final String productRevision = (String) pkgAdminData.get(CVMO_ADMINDATA_PRODUCT_REVISION);
            final String productId = activityUtils.getProductId(productNumber, productRevision);
            final String pkgNameTobeAppended = String.format(SW_PKG_NAME_APPENDER, typeTobeLogged, productId);
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PROCESSING_PRECHECK + pkgNameTobeAppended, ActivityConstants.INSTALL), JobLogLevel.INFO.toString()));
            final ActivityStepResult preCheckStatus = installActivityHandler.precheck(activityJobId, productNumber, productRevision,
                    activityUtils.getActivityInfo(activityJobId, RestoreInstallService.class).getJobType().name(), neJobStaticData);
            LOGGER.info("PreCheck Status for productID/Package::{}...{}", productId, preCheckStatus.getActivityResultEnum());
            //Once all the calls are successful for all the packages, then only send back the ActivityStepResult as success, else fail.
            if (preCheckStatus.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
                //Check Whether a software package is available to install with this productId
                final Map<String, Object> upPOData = upgradePackageService.getUpPoData(productNumber, productRevision);
                if (!upPOData.isEmpty()) {
                    SucceededPackagesList.add(productNumber + ShmConstants.DELIMITER_COLON + productRevision);
                    final String formattedLog = String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.INSTALL);
                    final String logMessage = formattedLog.substring(0, formattedLog.length() - 1).concat(pkgNameTobeAppended);
                    jobLogList.add(activityUtils.createNewLogEntry(logMessage, JobLogLevel.INFO.toString()));
                } else {
                    failedToExecuteItems++;
                    LOGGER.warn("No Software Package Imported/Found in database with name {}, this package installation can not be done", productId);
                    jobLogList.add(activityUtils.createNewLogEntry(
                            String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.INSTALL, "software packages are not available for " + productId + " in ENM."),
                            JobLogLevel.ERROR.toString()));
                }
            } else if (preCheckStatus.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION) {
                failedToExecuteItems++;
                LOGGER.warn("UP MO state is invalid for product ID {}", productId);
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.INSTALL, "MO current state is not supported" + pkgNameTobeAppended),
                        JobLogLevel.ERROR.toString()));
            } else if (preCheckStatus.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION) {
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRE_CHECK_ACTIVITY_SKIP + pkgNameTobeAppended, ActivityConstants.INSTALL), JobLogLevel.INFO.toString()));
            }
        }
        return failedToExecuteItems;
    }

    /**
     * Returns the Selection(boolean) of UP installation Types for missing/corrupted packages
     * 
     * @param jobConfigurationDetails
     * @param missingPkgSelection
     * @param neName
     */
    private boolean isPackagesSelectedToInstall(final Map<String, Object> jobConfigurationDetails, final String missingCurreptedtedPkgSelection, final String neName) {
        boolean returnValue = false;
        String neType = null;
        String platform = null;
        final List<String> keyList = new ArrayList<String>();
        keyList.add(missingCurreptedtedPkgSelection);
        final List<String> neFdns = new ArrayList<String>();
        neFdns.add(neName);
        try {
            final List<NetworkElement> networkElementsList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neFdns);
            if (!networkElementsList.isEmpty()) {
                neType = networkElementsList.get(0).getNeType();
                platform = networkElementsList.get(0).getPlatformType().name();
            }
        } catch (final RetriableCommandException | IllegalArgumentException e) {
            LOGGER.error("Exception while fetching neType of node :  {}", neFdns);
        }
        LOGGER.debug("neType {} platform {}", neType, platform);
        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platform);
        final String value = keyValueMap.get(missingCurreptedtedPkgSelection);
        returnValue = Boolean.parseBoolean(value);
        LOGGER.debug("returnValue {} for missingCurreptedtedPkgSelection {}", returnValue, missingCurreptedtedPkgSelection);
        return returnValue;
    }

    /**
     * @param cvMoAttr
     * @param jobLogList
     * @param actionType
     * @return
     */
    @SuppressWarnings("unchecked")
    private ActivityStepResultEnum downloadPrecheckResult(final Map<String, Object> cvMo, final NEJobStaticData neJobStaticData, final long activityJobId, final List<Map<String, Object>> jobLogList) {
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
        final String nodeName = neJobStaticData.getNodeName();
        final String cvMoFdn = (String) cvMo.get(ShmConstants.FDN);
        final Map<String, Object> cvMoAttribute = (Map<String, Object>) cvMo.get(ShmConstants.MO_ATTRIBUTES);
        final String configurationVersionName = configurationVersionUtils.getNeJobPropertyValue(mainJobAttributes, nodeName, BackupActivityConstants.CV_NAME);
        String logMessage = String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.DOWNLOAD_CV);
        final String logLevel = JobLogLevel.INFO.toString();
        ActivityStepResultEnum preCheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
        final String cvType = getCVTypeIfPresentOnNode(((List<Map<String, Object>>) cvMoAttribute.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)), configurationVersionName);
        if (cvType != null) {
            preCheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION;
            logMessage = String.format(JobLogConstants.CV_ALREADY_EXISTS_ON_NODE, configurationVersionName, cvType, ShmConstants.RESTORE_DOWNLOAD_ACTIVITY);
        }
        LOGGER.debug("{} for activity {}", logMessage, activityJobId);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), logLevel);
        systemRecorder.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.DOWNLOAD_RESTORE_PRECHECK, EventLevel.COARSE, nodeName, cvMoFdn,
                activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        return preCheckStatus;
    }

    /**
     * @param cvMoAttr
     * @param jobLogList
     * @param actionType
     * @return
     */
    @SuppressWarnings("unchecked")
    private ActivityStepResultEnum verifyRestorePrecheckResult(final Map<String, Object> cvMo, final NEJobStaticData neJobStaticData, final long activityJobId,
            final List<Map<String, Object>> jobLogList) {
        final Map<String, Object> mainJobAttributes = jobConfigurationServiceProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
        final String nodeName = neJobStaticData.getNodeName();
        final String cvMoFdn = (String) cvMo.get(ShmConstants.FDN);
        final Map<String, Object> cvMoAttribute = (Map<String, Object>) cvMo.get(ShmConstants.MO_ATTRIBUTES);
        final String configurationVersionName = configurationVersionUtils.getNeJobPropertyValue(mainJobAttributes, nodeName, BackupActivityConstants.CV_NAME);
        String logMessage = String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.VERIFY_RESTORE_CV);
        String logLevel = JobLogLevel.INFO.toString();
        ActivityStepResultEnum preCheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
        final String cvType = getCVTypeIfPresentOnNode(((List<Map<String, Object>>) cvMoAttribute.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION)), configurationVersionName);
        if (cvType == null) {
            preCheckStatus = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
            logMessage = "Precheck failed for the verify activity as selected CV does not exist on node ";
            logLevel = JobLogLevel.ERROR.toString();
        } else if (!cvType.equalsIgnoreCase(DOWNLOADED_CV_TYPE)) {
            preCheckStatus = ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION;
            logMessage = String.format(JobLogConstants.SKIP_RESTORE_JOB_CV_EXISTS_ON_NODE, configurationVersionName, cvType, ShmConstants.RESOTRE_VERIFY_ACTIVITY);
        }
        LOGGER.debug("{} for activity {}", logMessage, activityJobId);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), logLevel);
        systemRecorder.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), SHMEvents.VERIFY_RESTORE_PRECHECK, EventLevel.COARSE, nodeName, cvMoFdn,
                activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        return preCheckStatus;
    }

    /**
     * this method will be deleted once the logic of showing location of cv in backup inventory page is implemented
     * 
     * @param list
     */
    public String getCVTypeIfPresentOnNode(final List<Map<String, Object>> cvList, final String configurationVersionName) {
        LOGGER.debug("Inside cvTypeIfPresentOnNode with cvList {} and configurationVersionName {}", cvList, configurationVersionName);
        if (cvList.size() > 0) {
            for (final Map<String, Object> cv : cvList) {
                if (((String) cv.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME)).equals(configurationVersionName)) {
                    final String cvType = (String) cv.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE);
                    LOGGER.debug("CV : {} found on Node with Type : {}", configurationVersionName, cvType);
                    return cvType;
                }
            }
        }
        return null;
    }

}
