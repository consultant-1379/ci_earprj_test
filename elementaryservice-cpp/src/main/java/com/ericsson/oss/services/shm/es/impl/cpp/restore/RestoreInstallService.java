/*------------------------------------------------------------------------------
 *******************************************************************************
fine fine
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

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.EXECUTE_REPEAT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.impl.cpp.restore.RestoreServiceConstants.*;

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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.InstallActivityHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageService;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

/**
 * 
 * This class facilitates the installation of multiple Missing/Corrupted upgrade packages of CPP based node by invoking the UpgradePackage MO action(depending on the action type) that initializes the
 * install activity. <li>Install Activity of each upgrade package is similar to the install activity of an Upgrade Job.</li> <li>Both install activities of Restore and Upgrade Jobs have the common
 * handling in {@link InstallActivityHandler.java}.</li>
 * <p>
 * It is needed to have Two separate Elementary services for Install activity of Restore and Upgrade Jobs, since we need to handle Multiple Upgrade Packages and the job logs will vary depending on the
 * job type.
 * 
 * @author xrajeke
 * 
 */
@EServiceQualifier("CPP.RESTORE.install")
@ActivityInfo(activityName = "install", jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RestoreInstallService implements Activity, ActivityCallback {

    /**
     * Its a key word to represent the type of action to perform on Node
     */
    private static final String PKG_TYPE = "ACTION";
    /**
     * Its a key word to represent the product Ids separated by <code>DELIMITER_PIPE</code>
     */
    private static final String PRODUCT_IDS_STRING = "PRODUCT_IDS_STRING";
    /**
     * Its a key word to represent the no of items to be processed
     */
    private static final String REMAINING_PKGS_COUNT = "REMAINING_PKGS_COUNT";
    /**
     * Delimiter used for separating the product Ids
     */
    private static final String DELIMITER_PIPE = "|";

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreInstallService.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private InstallActivityHandler installActivityHandler;

    @Inject
    private UpgradePackageService upgradePackageService;

    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private RestorePrecheckHandler restorePrecheckHandler;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#precheck(long)
     */

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = restorePrecheckHandler.getRestorePrecheckResult(activityJobId, ActivityConstants.RESTORE_INSTALL_CV, ActivityConstants.INSTALL);
        return activityStepResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#execute(long)
     */
    @SuppressWarnings("unchecked")
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        final Map<String, Object> activityJobAttributes = activityUtils.getPoAttributes(activityJobId);
        final List<Map<String, Object>> activityJobPropertyList = (List<Map<String, Object>>) activityJobAttributes.get(ActivityConstants.JOB_PROPERTIES);

        String productID = "";
        String action = "";
        String typeOfPackage = "";
        LOGGER.debug("Before Starting Execution, activityJobPropertyList is:: {}", activityJobPropertyList);
        for (final Map<String, Object> jobProperty : activityJobPropertyList) {
            LOGGER.debug("jobProperty is {}", jobProperty);
            if (MISSING_PKGS.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                final String productIds = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                productID = productIds.split("\\" + DELIMITER_PIPE)[0];
                action = UpgradeActivityConstants.ACTION_INSTALL;
                typeOfPackage = MISSING_PKGS;
                break;
            } else if (CORRUPTED_PKGS.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                final String productIds = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                productID = productIds.split("\\" + DELIMITER_PIPE)[0];
                action = UpgradeActivityConstants.ACTION_FORCED_INSTALL;
                typeOfPackage = CORRUPTED_PKGS;
                break;
            }
        }
        String productNumber = null;
        String productRevision = null;
        if (productID != null && !productID.equalsIgnoreCase("")) {
            productNumber = productID.split(ShmConstants.DELIMITER_COLON)[0];
            productRevision = productID.split(ShmConstants.DELIMITER_COLON)[1];
        }
        LOGGER.info("Trying to execute for productID....{}. action....{}", productID, action);
        if (StringUtils.isEmpty(productID) || action.isEmpty()) {
            LOGGER.error("Exiting from Restore_Installation without performing any action on the Node, due to neither MISSING_PackageInstallation nor CORRUPTED_PackageInstallation is found for this RESTORE job. So failing execute.");

            //Persist Result as Failed in case of unable to trigger action.
            final Map<String, Object> result = new HashMap<String, Object>();
            result.put(ShmConstants.KEY, ActivityConstants.ACTIVITY_RESULT);
            result.put(ShmConstants.VALUE, JobResult.FAILED.getJobResult());
            activityJobPropertyList.add(result);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, activityJobPropertyList, null, null);

            final long neJobId = (long) activityJobAttributes.get(ShmConstants.NE_JOB_ID);
            final Map<String, Object> neJobAttributes = activityUtils.getPoAttributes(neJobId);
            final String businessKey = (String) neJobAttributes.get(ShmConstants.BUSINESS_KEY);
            sendActivateToWFS(activityJobId, businessKey, null);
            return;
        }
        LOGGER.debug("Proceeding to install productID {} with {}", productID, action);
        installActivityHandler.execute(productNumber, productRevision, action, typeOfPackage, activityUtils.getActivityInfo(activityJobId, RestoreInstallService.class));
        // since installActivityHandler.execute will be called for Upgrade job also, we will persist stepDuration here. Even if moAction was not triggered or failed.
        final String existingStepDurations = ((String) activityJobAttributes.get(ShmConstants.STEP_DURATIONS));
        if (existingStepDurations != null && !existingStepDurations.contains(ActivityStepsEnum.EXECUTE.getStep())) {
            final long activityStartTime = ((Date) activityUtils.getActivityJobAttributes(activityJobId).get(ShmConstants.ACTIVITY_START_DATE)).getTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#handleTimeout(long)
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        final Map<String, Object> resultData = getMissingAndCorruptedUPs(activityJobId);
        final String productIDsString = (String) resultData.get(PRODUCT_IDS_STRING) == null ? "" : (String) resultData.get(PRODUCT_IDS_STRING);
        final String pkgType = (String) resultData.get(PKG_TYPE);
        final int remainigPkgsCount = (int) resultData.get(REMAINING_PKGS_COUNT);
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        LOGGER.debug("Inside RestoreInstallService.handleTimeout(). ProductIDs found {} with activityJobId :{}", productIDsString, activityJobId);
        final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
        final String productID = productIDsString.split("\\" + DELIMITER_PIPE)[0];
        LOGGER.debug("Product ID to be handled now is {} with activityJobId:{}", productID, activityJobId);
        if (productID.isEmpty()) {
            final ActivityStepResult activityStepResult = new ActivityStepResult();
            LOGGER.warn("Exiting from Restore_Handlingtimeout with failed status,  due to no items to be processed");
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
            return activityStepResult;
        }
        final ActivityStepResult timeoutResponse = installActivityHandler.handleTimeout(activityUtils.getActivityInfo(activityJobId, RestoreInstallService.class));
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        if (timeoutResponse.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_FAIL) {
            if (remainigPkgsCount > 1) {
                final String logMessage = String.format(INSTALL_CAN_NOT_BE_CONTINUED, pkgType, productID);
                jobLogList.add(activityUtils.createNewLogEntry(logMessage, JobLogLevel.ERROR.toString()));
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            }
            LOGGER.error("No notifications received and so Activity failed due to timeout");
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
            return timeoutResponse;
        }

        final Map<String, String> attributesTobeAdded = new HashMap<String, String>();
        String remainingPkgs = "";
        if (productIDsString.contains(DELIMITER_PIPE)) {
            remainingPkgs = productIDsString.substring(productIDsString.indexOf(DELIMITER_PIPE) + 1, productIDsString.length());
        }
        attributesTobeAdded.put(pkgType, remainingPkgs);

        if (pkgType.equals(MISSING_PKGS)) {
            jobLogList.add(activityUtils.createNewLogEntry(String.format(MISSING_PKG_INSTALLED, productID), JobLogLevel.INFO.toString()));
        } else if (pkgType.equals(CORRUPTED_PKGS)) {
            jobLogList.add(activityUtils.createNewLogEntry(String.format(CORRUPTED_PKG_REPLACED, productID), JobLogLevel.INFO.toString()));
        }
        jobUpdateService.addOrUpdateOrRemoveJobProperties(activityJobId, attributesTobeAdded, jobLogList);
        final Double currentProgressPercentage = activityAndNEJobProgressCalculator.calculateActivityProgressPercentage(jobEnvironment, remainigPkgsCount, EXECUTE_REPEAT_PROGRESS_PERCENTAGE);
        LOGGER.debug("remainigPkgsCount {} and currentProgressPercentage {}", remainigPkgsCount, currentProgressPercentage);
        activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, currentProgressPercentage);
        activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());

        //  If we found more packages left to install, then notify the WFS  that it has to repeat from the EXECUTE step again.
        if (remainigPkgsCount > 1) {
            LOGGER.debug("Still loop has to repeat for execute action since, remaining packages{{}} are left ", remainigPkgsCount - 1);
            timeoutResponse.setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
        }
        LOGGER.debug("returning back the result {} to WFS with activityJobId:{}", timeoutResponse.getActivityResultEnum(), activityJobId);
        if (timeoutResponse.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS || timeoutResponse.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_FAIL) {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        }
        return timeoutResponse;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancel(long)
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        return installActivityHandler.cancel(activityUtils.getActivityInfo(activityJobId, RestoreInstallService.class));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.ActivityCallback#processNotification(com.ericsson.oss.services.shm.notifications.api.Notification)
     */
    @Override
    public void processNotification(final Notification message) {
        LOGGER.debug("Entered cpp restore install - processNotification with event type : {} ", message.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(message.getNotificationEventType())) {
            LOGGER.debug("cpp restore install activity - Discarding non-AVC notification.");
            return;
        }

        final String notifiedMoFdn = message.getNotificationSubject().getKey();
        LOGGER.debug("Recevied a notification for Processing {}", notifiedMoFdn);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final long activityJobId = activityUtils.getActivityJobId(message.getNotificationSubject());
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final Map<String, Object> missingAndCorruptedUPs = getMissingAndCorruptedUPs(activityJobId);
        final String productIDsString = (String) missingAndCorruptedUPs.get(PRODUCT_IDS_STRING) == null ? "" : (String) missingAndCorruptedUPs.get(PRODUCT_IDS_STRING);
        final String packageType = (String) missingAndCorruptedUPs.get(PKG_TYPE);
        final boolean isRepeatExecuteRequired = (int) missingAndCorruptedUPs.get(REMAINING_PKGS_COUNT) > 1 ? true : false;
        final Map<String, String> jobProperties = new HashMap<String, String>();
        final Map<String, Object> processNotificationResult = installActivityHandler.processNotification(message, activityUtils.getActivityInfo(activityJobId, RestoreInstallService.class));
        final boolean isActivityCompleted = (boolean) processNotificationResult.get(ActivityConstants.ACTIVITY_STATUS);

        LOGGER.debug("isActivityCompleted = {} for fdn {} activity job {}", isActivityCompleted, notifiedMoFdn, activityJobId);

        if (isActivityCompleted) {
            final boolean isSuccessfullyUnsubscribed = activityUtils.unSubscribeToMoNotifications(notifiedMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            if (isFirstNotificationOfActivityCompletion(isSuccessfullyUnsubscribed)) {
                final JobResult jobResult = (JobResult) processNotificationResult.get(ActivityConstants.ACTIVITY_RESULT);
                final String productNumberRevision = getProductDetails(message);
                final Map<String, Object> processVariables = new HashMap<String, Object>();

                final String jobLogMessage = getJobLogMessage(packageType, productNumberRevision, jobResult);
                int remainingPkgsCount = (int) missingAndCorruptedUPs.get(REMAINING_PKGS_COUNT);
                final Double currentProgressPercentage = activityAndNEJobProgressCalculator.calculateActivityProgressPercentage(jobEnvironment, remainingPkgsCount, EXECUTE_REPEAT_PROGRESS_PERCENTAGE);
                activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, currentProgressPercentage);
                activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(jobEnvironment.getNeJobId());
                if (jobResult == JobResult.SUCCESS && isRepeatExecuteRequired) {
                    LOGGER.debug("Current Package is {}, Notifiying wfs to repeat execution for remaining packages: {}", notifiedMoFdn, productIDsString);
                    addRemainingPackagesToJobProperties(missingAndCorruptedUPs, productNumberRevision, jobProperties);
                    processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, isRepeatExecuteRequired);
                    remainingPkgsCount--;
                } else {
                    jobProperties.put(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
                }
                if (!isRepeatExecuteRequired) {
                    final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
                    activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
                }
                activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.INFO.toString());
                final boolean isJobResultPropertyPersisted = jobUpdateService.addOrUpdateOrRemoveJobProperties(activityJobId, jobProperties, jobLogList);

                if (isJobResultPropertyPersisted) {
                    final String businessKey = (String) jobEnvironment.getNeJobAttributes().get(ShmConstants.BUSINESS_KEY);
                    sendActivateToWFS(activityJobId, businessKey, processVariables);
                } else {
                    LOGGER.warn(
                            "ActivityJob attributes[jobProperties={},jobLogList={}] are not updated in Database for Restore-Install activity[activityjob poId ={}]. Skipped to notify WFS. It will wait untill till timeout occurs to finish the job.",
                            jobProperties, jobLogList, activityJobId);
                }
            } else {
                LOGGER.warn("UnSubscribe to notifications for the given MO fdn: {} already attempted, found a subject as: {}. Discard the duplicate notifications process for completed activity.",
                        notifiedMoFdn, isSuccessfullyUnsubscribed);
            }
        }
    }

    /**
     * This method retrieves joblogmessage based on the job result and package type
     * 
     * @param pkgType
     * @param productNumberRevision
     * @param jobResult
     * @return
     */
    private String getJobLogMessage(final String pkgType, final String productNumberRevision, final JobResult jobResult) {
        String jobLogMessage = "";

        if (jobResult != null) {

            switch (jobResult) {
            case SUCCESS:
                if (pkgType.equals(MISSING_PKGS)) {
                    jobLogMessage = String.format(MISSING_PKG_INSTALLED, productNumberRevision);
                } else if (pkgType.equals(CORRUPTED_PKGS)) {
                    jobLogMessage = String.format(CORRUPTED_PKG_REPLACED, productNumberRevision);
                }
                break;
            default:
                jobLogMessage = String.format(INSTALL_CAN_NOT_BE_CONTINUED, pkgType, productNumberRevision);
            }
        }
        return jobLogMessage;
    }

    private void addRemainingPackagesToJobProperties(final Map<String, Object> missingAndCorruptedUPs, final String notifiedPNumberPRevision, final Map<String, String> jobProperties) {

        final String productIDsString = (String) missingAndCorruptedUPs.get(PRODUCT_IDS_STRING) == null ? "" : (String) missingAndCorruptedUPs.get(PRODUCT_IDS_STRING);

        final String pkgType = (String) missingAndCorruptedUPs.get(PKG_TYPE);
        String remainingPkgs = productIDsString.replaceFirst(notifiedPNumberPRevision, "");

        remainingPkgs = remainingPkgs.startsWith(DELIMITER_PIPE) ? remainingPkgs.substring(1) : remainingPkgs;
        remainingPkgs = remainingPkgs.endsWith(DELIMITER_PIPE) ? remainingPkgs.substring(0, remainingPkgs.length() - 1) : remainingPkgs;
        jobProperties.put(pkgType, remainingPkgs);

    }

    public String getProductDetails(final Notification notification) {

        final Map<String, Object> adminData = upgradePackageService.getUpAdminData(notification);
        String notifiedPNumberPRevision = activityUtils.getProductId((String) adminData.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_NUMBER),
                (String) adminData.get(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_REVISION));
        notifiedPNumberPRevision = notifiedPNumberPRevision != null ? notifiedPNumberPRevision : "";
        return notifiedPNumberPRevision;
    }

    /**
     * To Retrieve the UpgradePackages to be installed. In Precheck step UpgradePackages which are to be installed are being stored in activity job properties.
     * <p>
     * First it searches for Missing software Packages, if not available then searches for Corrupted software packages, this same order is followed while inserting these attributes in precheck step.
     * 
     * @param activityJobId
     * @return Map - containing <li>REMAINING_PKGS_COUNT - of Missing/Corrupted</li> <li>PRODUCT_IDS_STRING - of Missing/Corrupted</li> <li>PKG_TYPE - Type of current package (Missing/Corrupted)</li>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMissingAndCorruptedUPs(final long activityJobId) {
        final Map<String, Object> resultData = new HashMap<String, Object>();
        final Map<String, Object> activityJobAttributes = activityUtils.getPoAttributes(activityJobId);
        final List<Map<String, Object>> activityJobPropertyList = (List<Map<String, Object>>) activityJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        int remainigPkgsCount = 0;
        LOGGER.debug("Verifying packages in lookup {}", activityJobPropertyList);
        for (final Map<String, Object> jobProperty : activityJobPropertyList) {
            final String value = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
            if (MISSING_PKGS.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                remainigPkgsCount = remainigPkgsCount + value.split("\\" + DELIMITER_PIPE).length;
                if (resultData.isEmpty()) {
                    resultData.put(PRODUCT_IDS_STRING, value);
                    resultData.put(PKG_TYPE, MISSING_PKGS);
                }
            } else if (CORRUPTED_PKGS.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                remainigPkgsCount = remainigPkgsCount + value.split("\\" + DELIMITER_PIPE).length;
                if (resultData.isEmpty()) {
                    resultData.put(PRODUCT_IDS_STRING, value);
                    resultData.put(PKG_TYPE, CORRUPTED_PKGS);
                }
            }
        }
        resultData.put(REMAINING_PKGS_COUNT, remainigPkgsCount);
        return resultData;
    }

    /**
     * @param activityJobId
     * @param businessKey
     */
    private void sendActivateToWFS(final long activityJobId, final String businessKey, final Map<String, Object> processVariables) {
        try {
            workflowInstanceNotifier.sendActivate(businessKey, processVariables);
        } catch (final WorkflowServiceInvocationException e) {
            LOGGER.error("{}", e.getMessage());
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            jobLogList.add(activityUtils.createNewLogEntry(e.getMessage(), JobLogLevel.INFO.toString()));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long, boolean)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return installActivityHandler.cancelTimeout(activityJobId, finalizeResult);
    }

    /**
     * Discard the duplicate notifications process, if more than one notification evaluate the activity as completed.
     * 
     * @param isSuccessfullyUnsubscribed
     * @return boolean, true if first Notification of activity completed
     */
    private boolean isFirstNotificationOfActivityCompletion(final boolean isSuccessfullyUnsubscribed) {
        return isSuccessfullyUnsubscribed;
    }
}
