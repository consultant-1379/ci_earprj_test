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

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.HANDLE_TIMEOUT_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityCompleteTimer;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CVActionResultInformation;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActionAdditionalInfo;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActionMainAndAdditionalResultHolder;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.moaction.retry.cpp.backup.BackupRetryPolicy;
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
 * This class facilitates the common methods or implementations required for all activities of Backup Use Case.
 *
 * @author tcsrohc
 *
 */
@SuppressWarnings("PMD.TooManyFields")
public abstract class AbstractBackupActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBackupActivity.class);

    @Inject
    protected JobUpdateService jobUpdateService;

    @Inject
    protected JobConfigurationService jobConfigurationService;

    @Inject
    protected CommonCvOperations commonCvOperations;

    @Inject
    protected ConfigurationVersionService configurationVersionService;

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected SystemRecorder systemRecorder;

    @Inject
    protected JobPropertyUtils jobPropertyUtils;

    @Inject
    protected FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    protected ActivityCompleteTimer activityCompleteTimer;

    @Inject
    protected ConfigurationVersionUtils configurationVersionUtils;

    @Inject
    protected BackupUtils backupActionResultUtility;

    @Inject
    protected BackupRetryPolicy backupMoActionRetryPolicy;

    @Inject
    protected ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Inject
    protected JobLogUtil jobLogUtil;

    @Inject
    protected NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    protected JobConfigurationServiceRetryProxy jobConfigServiceRetryProxy;

    @Inject
    protected ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    protected JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private CvNameProvider cvNameProvider;

    public abstract String getActivityType();

    public abstract String getNotificationEventType();

    /**
     * This method will updates existing job property list.
     *
     * @param activityJobPropertyList
     * @param currentMainActivityValue
     * @param currentDetailedActivityValue
     * @return
     */
    private List<Map<String, Object>> updateMainAndDetailedActivity(final String currentMainActivityValue, final String currentDetailedActivityValue) {
        List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        String previousCurrentMainActivityValue;
        String previousCurrentDetailedActivityValue;
        boolean mainActivityExists = false;
        boolean detailedActivityExists = false;
        for (final Map<String, Object> jobProperty : activityJobPropertyList) {
            if (BackupActivityConstants.CURRENT_MAIN_ACTIVITY.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                mainActivityExists = true;
                previousCurrentMainActivityValue = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                if (currentMainActivityValue != null && !currentMainActivityValue.equals(previousCurrentMainActivityValue)) {
                    jobProperty.put(ActivityConstants.JOB_PROP_VALUE, currentMainActivityValue);
                }
            }

            if (BackupActivityConstants.CURRENT_DETAILED_ACTIVITY.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                detailedActivityExists = true;
                previousCurrentDetailedActivityValue = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                if (currentDetailedActivityValue != null && !currentDetailedActivityValue.equals(previousCurrentDetailedActivityValue)) {
                    jobProperty.put(ActivityConstants.JOB_PROP_VALUE, currentDetailedActivityValue);
                }
            }
        }
        if (!mainActivityExists) {
            activityJobPropertyList = AbstractBackupActivity.createMainActivityProperty(currentMainActivityValue);
        }
        if (!detailedActivityExists) {
            activityJobPropertyList = AbstractBackupActivity.createDetailedActivityProperty(currentDetailedActivityValue);
        }
        return activityJobPropertyList;
    }

    /**
     * This method will creates job property list.
     *
     * @param activityJobPropertyList
     * @param currentMainActivityValue
     * @return
     */
    private static List<Map<String, Object>> createMainActivityProperty(final String currentMainActivityValue) {
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        if (currentMainActivityValue != null) {
            final Map<String, Object> jobProperty = new HashMap<String, Object>();
            jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
            jobProperty.put(ActivityConstants.JOB_PROP_VALUE, currentMainActivityValue);
            activityJobPropertyList.add(jobProperty);
        }
        return activityJobPropertyList;
    }

    /**
     * This method will creates job property list.
     *
     * @param activityJobPropertyList
     * @param currentDetailedActivityValue
     * @return
     */
    private static List<Map<String, Object>> createDetailedActivityProperty(final String currentDetailedActivityValue) {
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        if (currentDetailedActivityValue != null) {
            final Map<String, Object> jobProperty = new HashMap<String, Object>();
            jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CURRENT_DETAILED_ACTIVITY);
            jobProperty.put(ActivityConstants.JOB_PROP_VALUE, currentDetailedActivityValue);
            activityJobPropertyList.add(jobProperty);
        }
        return activityJobPropertyList;
    }

    /**
     * This method retrieves the Configuration Version MO attributes.
     *
     * @param neName
     * @return Map<String, Object>
     */
    public Map<String, Object> getConfigurationVersionMo(final String neName) {
        LOGGER.debug("Inside AbstractBackupActivity getConfigurationVersionMo with neName= {}", neName);
        final Map<String, Object> moAttributesMap = configurationVersionService.getCVMoAttr(neName);
        LOGGER.debug("MO attributes map = {}", moAttributesMap);
        return moAttributesMap;
    }

    /**
     * This method retrieves the Configuration Version MO .
     *
     * @param neName
     * @return Map<String, Object>
     */
    public String getCVMoFdn(final String neName) {
        return configurationVersionService.getCVMoFdn(neName);
    }

    /**
     * This method has been Deprecated, use {@link AbstractBackupActivity.getConfigurationVersionName(final Map<String, Object>,final Map<String, Object>,String, String) method instead.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public String getConfigurationVersionName(final JobEnvironment jobEnvironment, final String specificKey) {
        String cvName = "";
        final List<Map<String, String>> neJobPropertyList = (List<Map<String, String>>) jobEnvironment.getNeJobAttributes().get(ActivityConstants.JOB_PROPERTIES);
        LOGGER.debug("NeJobPropertyList while getConfigurationVersionName with key {} is {}", specificKey, neJobPropertyList);
        //CV Name will be retrieved from NeJob's JobProperty, this will return cvName only when the job is created from the normal create backup job wizard.
        cvName = CvNameProvider.getValueForSpecificKey(specificKey, neJobPropertyList);
        if (cvName != null && !cvName.isEmpty()) {
            return cvName;
        }

        //Mainly for ManageBackups Use Case from Backup Inventory.
        //However, in case NeJob's JobProperty doesn't contain the cvName, it will try to search for it in job configuration also.
        cvName = getCvNameFromJobConfiguration(specificKey, jobEnvironment);
        if (cvName != null && !cvName.isEmpty()) {
            return cvName;
        }
        //Backward Compatibility
        return CvNameProvider.getCvNameWithDefaultKey(neJobPropertyList);
    }

    public String getConfigurationVersionName(final NEJobStaticData neJobStaticData, final String specificKey) throws JobDataNotFoundException {
        return cvNameProvider.getConfigurationVersionName(neJobStaticData, specificKey);
    }

    /**
     * This method has been Deprecated, use {@link AbstractBackupActivity.getCvNameFromJobConfiguration(String, final Map<String, Object>,String) method instead.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    protected String getCvNameFromJobConfiguration(final String specificKey, final JobEnvironment jobEnvironment) {
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) jobEnvironment.getMainJobAttributes().get(ShmConstants.JOBCONFIGURATIONDETAILS);
        LOGGER.debug("getCvNameFrom JobConfiguration {} with key {}", jobConfigurationDetails, specificKey);
        final String nodeName = jobEnvironment.getNodeName();
        String neType = null;
        String platform = null;
        if (jobConfigurationDetails != null && !jobConfigurationDetails.isEmpty()) {
            LOGGER.debug("Inside getCvNameFromJobConfiguration : {} and neName {} ", jobConfigurationDetails, nodeName);
            final List<String> neFdns = new ArrayList<String>();
            neFdns.add(nodeName);
            try {
                final List<NetworkElement> networkElementsList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neFdns);
                if (!networkElementsList.isEmpty()) {
                    neType = networkElementsList.get(0).getNeType();
                    platform = networkElementsList.get(0).getPlatformType().name();
                }
            } catch (final RetriableCommandException | IllegalArgumentException e) {
                LOGGER.error("Exception while fetching neType of node :  {}. Reason : {}", neFdns, e.getMessage());
            }
            LOGGER.debug("NeType {}, platform {} ", neType, platform);
            final List<String> keyList = new ArrayList<String>();
            keyList.add(specificKey);
            final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, nodeName, neType, platform);
            final String cvName = keyValueMap.get(specificKey);
            LOGGER.debug("cvName in getCvNameFromJobConfiguration method {} ", cvName);
            return cvName;

        }
        return "";
    }

    protected Map<String, String> getPropertyValueFromJobConfiguration(final List<String> keyList, final Map<String, Object> mainJobAttributes, final String nodeName, final String platform) {
        return cvNameProvider.getPropertyValueFromJobConfiguration(keyList, mainJobAttributes, nodeName, platform);
    }

    /**
     * This method persists Activity Job property.
     *
     * @param activityJobAttr
     * @param modifiedAttributes
     * @return List<Map<String, Object>>
     */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> persistActivityJobProperty(final Map<String, Object> activityJobAttr, final Map<String, Object> modifiedAttributes) {
        LOGGER.debug("Activity Job Attributes {}", activityJobAttr);
        int actionId = -1;
        List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        if (activityJobAttr.get(ActivityConstants.JOB_PROPERTIES) != null) {
            activityJobPropertyList = (List<Map<String, Object>>) activityJobAttr.get(ActivityConstants.JOB_PROPERTIES);
        }
        String currentMainActivityValue = null;
        if (modifiedAttributes.get(BackupActivityConstants.CURRENT_MAIN_ACTIVITY) != null) {
            currentMainActivityValue = (String) modifiedAttributes.get(BackupActivityConstants.CURRENT_MAIN_ACTIVITY);
        }
        String currentDetailedActivityValue = null;
        if (modifiedAttributes.get(BackupActivityConstants.CURRENT_DETAILED_ACTIVITY) != null) {
            currentDetailedActivityValue = (String) modifiedAttributes.get(BackupActivityConstants.CURRENT_DETAILED_ACTIVITY);
        }

        if (!activityJobPropertyList.isEmpty()) {
            activityJobPropertyList = updateMainAndDetailedActivity(currentMainActivityValue, currentDetailedActivityValue);
        } else {
            activityJobPropertyList = AbstractBackupActivity.createMainActivityProperty(currentMainActivityValue);
            activityJobPropertyList = AbstractBackupActivity.createDetailedActivityProperty(currentDetailedActivityValue);
        }

        LOGGER.debug("Result value of modifiedAttributes is {}", modifiedAttributes.get(ActivityConstants.ACTIVITY_RESULT));
        if (modifiedAttributes.get(ActivityConstants.ACTIVITY_RESULT) != null) {
            LOGGER.debug("Copying result to activityJobPropertyList: {}", activityJobPropertyList);
            boolean resultUpdated = false;
            if (activityJobPropertyList != null && !activityJobPropertyList.isEmpty()) {
                for (final Map<String, Object> jobProperty : activityJobPropertyList) {
                    if (ActivityConstants.ACTIVITY_RESULT.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, modifiedAttributes.get(ActivityConstants.ACTIVITY_RESULT));
                        resultUpdated = true;
                    }
                }

            if (!resultUpdated) {
                final Map<String, Object> jobProperty = new HashMap<String, Object>();
                jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTIVITY_RESULT);
                jobProperty.put(ActivityConstants.JOB_PROP_VALUE, modifiedAttributes.get(ActivityConstants.ACTIVITY_RESULT));
                activityJobPropertyList.add(jobProperty);
                LOGGER.info("Successfully copied the jobProperty {} to activityJobPropertyList", jobProperty);
            }
        }
        }

        if (modifiedAttributes.get(ActivityConstants.ACTION_ID) != null) {
            actionId = (int) modifiedAttributes.get(ActivityConstants.ACTION_ID);
            LOGGER.debug("Action Id : {}", actionId);
        }

        if (actionId != -1) {
            final Map<String, Object> jobProperty = new HashMap<String, Object>();
            jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
            jobProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(actionId));
            activityJobPropertyList.add(jobProperty);
        }

        LOGGER.debug("Activity JobProperty List after comparison: {}", activityJobPropertyList);
        return activityJobPropertyList;
    }

    /**
     * This method will persist the job properties and job logs in job.
     *
     * @param activityJobId
     * @param modifiedAttributes
     * @return
     */
    public boolean persistActivityJobAttributes(final long activityJobId, final Map<String, Object> activityJobAttr, final Map<String, Object> modifiedAttributes,
            final List<Map<String, Object>> jobLogList) {
        final List<Map<String, Object>> activityJobPropertyList = persistActivityJobProperty(activityJobAttr, modifiedAttributes);
        final boolean isJobAttributesPersisted = jobUpdateService.updateRunningJobAttributes(activityJobId, activityJobPropertyList, jobLogList);
        return isJobAttributesPersisted;
    }

    protected void handleNotification(final Notification notification, final String shmCapabilities) {
        long activityJobId = 0;
        final String activityType = getActivityType();
        LOGGER.debug("Entered cpp backup activity {} - processNotification with event type : {} and notification subject : {}", activityType, notification.getNotificationEventType(),
                notification.getNotificationSubject());
        if (!NotificationEventTypeEnum.AVC.equals(notification.getNotificationEventType())) {
            LOGGER.debug("cpp backup activity - Discarding non-AVC notification.");
            return;
        }
        try {
            final NotificationSubject notificationSubject = notification.getNotificationSubject();
            activityJobId = activityUtils.getActivityJobId(notificationSubject);
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, shmCapabilities);
            final Map<String, AttributeChangeData> modifiedAttr = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
            LOGGER.debug("Inside {} processNotification with modifiedAttr = {}", activityType, modifiedAttr);
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            configurationVersionUtils.reportNotification(configurationVersionUtils.getNewCvActivity(notification), jobLogList);
            final Map<String, Object> actionResultData = backupActionResultUtility.getActionResultData(modifiedAttr);
            if (backupActionResultUtility.isActionResultNotified(actionResultData)) {
                final String cvMoFdn = activityUtils.getNotifiedFDN(notification);
                activityUtils.unSubscribeToMoNotifications(cvMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
                final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
                if (isJobResultSuccess(activityJobId, actionResultData, neJobStaticData, activityJobAttributes)) {
                    activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PROCESS_NOTIFICATION);
                    final String configurationVersionName = (String) actionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME);
                    activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_FOR_CV_COMPLETED_SUCCESSFULLY, activityType, configurationVersionName), JobLogType.SYSTEM.toString(), jobLogList,
                            JobLogLevel.INFO.toString());
                    persistResultAndNotifyWFS(activityJobId, jobLogList, neJobStaticData, activityJobAttributes, cvMoFdn, JobResult.SUCCESS);
                } else {
                    final JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, this.getClass());
                    activityCompleteTimer.startTimer(jobActivityInfo);
                    LOGGER.debug("Activity wait timer is started for {} activity with activityJobId:{}", activityType, activityJobId);
                }
                LOGGER.debug("Exiting {} processNotification() with notificationSubject = {}, NodeName = {}", activityType, notification.getNotificationSubject(), neJobStaticData.getNodeName());
            } else {
                jobUpdateService.updateRunningJobAttributes(activityJobId, null, jobLogList);
            }
        } catch (final JobDataNotFoundException jdnfEx) {
            final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
            LOGGER.error("Unable to process notification(s). Reason : {}", jdnfEx);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        } catch (final Exception ex) {
            final String errorMsg = String.format("An exception occurred while processing %s notification. Notification : %s, Exception is : {}", activityType, notification);
            LOGGER.error(errorMsg, ex);
        }
    }

    protected boolean isJobResultSuccess(final long activityJobId, final Map<String, Object> actionResultData, final NEJobStaticData neJobStaticData, final Map<String, Object> activityJobAttributes) {
        final int actionIdFromNode = (int) actionResultData.get(ConfigurationVersionMoConstants.ACTION_ID);
        final int actionIdFromDatabase = activityUtils.getPersistedActionId(activityJobId, neJobStaticData.getNodeName(), activityJobAttributes);
        if (actionIdFromNode == actionIdFromDatabase) {
            return backupActionResultUtility.isJobSuccess(actionResultData);
        } else {
            final String configurationVersionNameFromNode = (String) actionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME);
            final String invokedActionOnNode = (String) actionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION);
            String configurationVersionName = "";
            String actionTriggered = "";
            switch (getActivityType()) {
            case ActivityConstants.UPLOAD_CV:
                configurationVersionName = getCurrentBackup(activityJobId, activityJobAttributes);
                actionTriggered = CVInvokedAction.PUT_TO_FTP_SERVER.toString();
                break;
            case ActivityConstants.CONFIRM_RESTORE_CV:
                configurationVersionName = configurationVersionUtils.getNeJobPropertyValue(jobConfigServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId()),
                        neJobStaticData.getNodeName(), BackupActivityConstants.CV_NAME);
                actionTriggered = CVInvokedAction.CONFIRM_RESTORE.toString();
                break;
            default:
                return false; //Only Upload, Download and Confirm CV Services are calling this method.
            }
            LOGGER.debug("Current Cv name from db :{}, current cv name from node: {}, triggered action name from db : {} and invoked action name on node :{} ", configurationVersionName,
                    configurationVersionNameFromNode, actionTriggered, invokedActionOnNode);
            if (configurationVersionName.equals(configurationVersionNameFromNode) && actionTriggered.equals(invokedActionOnNode)) {
                return backupActionResultUtility.isJobSuccess(actionResultData);
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    protected String getCurrentBackup(final long activityJobId, final Map<String, Object> activityJobAttributes) {
        String currentBackup = "";
        final List<Map<String, String>> activityJobProperties = (List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES);
        if (activityJobProperties != null) {
            for (final Map<String, String> eachJobProperty : activityJobProperties) {
                if (BackupActivityConstants.CURRENT_BACKUP.equals(eachJobProperty.get(ShmConstants.KEY))) {
                    currentBackup = eachJobProperty.get(ShmConstants.VALUE);
                    break;
                }
            }
        }
        return currentBackup;
    }

    /**
     * This method has been Deprecated, use {@link AbstractBackupActivity.sendNotificationResultToWfs(long, List, NEJobStaticData, String, JobResult) method instead.
     */
    @Deprecated
    protected void sendNotificationResultToWfs(final List<Map<String, Object>> jobLogList, final JobEnvironment jobEnvironment, final String cvMoFdn, final JobResult jobResult) {
        final String activityType = getActivityType();
        final String eventType = getNotificationEventType();
        final long activityJobId = jobEnvironment.getActivityJobId();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        activityUtils.recordEvent(eventType, jobEnvironment.getNodeName(), cvMoFdn, "SHM:" + activityJobId + ":" + jobEnvironment.getNodeName() + ":" + activityType + jobResult);
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.toString(), jobPropertyList);
        final boolean isJobResultedPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (isJobResultedPersisted) {
            activityUtils.sendNotificationToWFS(jobEnvironment, activityJobId, activityType, null);
        }
    }

    protected void persistResultAndNotifyWFS(final long activityJobId, final List<Map<String, Object>> jobLogList, final NEJobStaticData neJobStaticData,
            final Map<String, Object> activityJobAttributes, final String cvMoFdn, final JobResult jobResult) {
        final String activityType = getActivityType();
        final String eventType = getNotificationEventType();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), eventType, neJobStaticData.getNodeName(), cvMoFdn,
                "SHM:" + activityJobId + ":" + neJobStaticData.getNodeName() + ":" + activityType + jobResult);
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.toString(), jobPropertyList);
        final boolean isJobResultPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        if (isJobResultPersisted) {
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityType, null);
        }
    }

    protected boolean isActionSucess(final CVActionMainResult cvActionMainResult) {
        return cvActionMainResult.equals(CVActionMainResult.EXECUTED) || cvActionMainResult.equals(CVActionMainResult.EXECUTED_WITH_WARNINGS);
    }

    /**
     * This method has been Deprecated, use {@link AbstractBackupActivity.onActionComplete(long, NEJobStaticData) method instead.
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    protected void onActionComplete(final long activityJobId) {
        final String activityType = getActivityType();
        final String eventType = getNotificationEventType();
        LOGGER.debug("Entered onActionComplete with activity job id {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        JobEnvironment jobEnvironment = null;
        String cvMoFdn = null;
        try {
            jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            final Map<String, Object> moAttributesMap = getConfigurationVersionMo(jobEnvironment.getNodeName());
            Map<String, Object> cvAttributes = null;
            if (moAttributesMap != null) {
                cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
                cvAttributes = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
            }
            final JobResult jobResult = evaluateJobResult(activityJobId, cvAttributes, jobLogList);
            final long activityStartTime = ((Date) jobEnvironment.getActivityJobAttributes().get(ShmConstants.ACTIVITY_START_DATE)).getTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
            sendNotificationResultToWfs(jobLogList, jobEnvironment, cvMoFdn, jobResult);
        } catch (final Exception ex) {
            LOGGER.error("Exception while evaluating final activity result for : " + activityType + " Exception : ", ex);
            final String logMessage = "Exception while evaluating final activity result for : " + activityType;
            if (jobEnvironment != null) {
                activityUtils.recordEvent(eventType, jobEnvironment.getNodeName(), cvMoFdn, activityUtils.additionalInfoForEvent(activityJobId, jobEnvironment.getNodeName(), logMessage));
            }
        }
    }

    protected void onActionComplete(final long activityJobId, final NEJobStaticData neJobStaticData) {
        final String activityType = getActivityType();
        final String eventType = getNotificationEventType();
        LOGGER.debug("Entered onActionComplete with activity job id {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        String cvMoFdn = null;
        try {
            final Map<String, Object> moAttributesMap = getConfigurationVersionMo(neJobStaticData.getNodeName());
            Map<String, Object> cvAttributes = null;
            if (moAttributesMap != null) {
                cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
                cvAttributes = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
            }
            final JobResult jobResult = evaluateJobResult(activityJobId, cvAttributes, jobLogList);
            final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PROCESS_NOTIFICATION);
            persistResultAndNotifyWFS(activityJobId, jobLogList, neJobStaticData, activityJobAttributes, cvMoFdn, jobResult);
        } catch (final Exception ex) {
            final String logMessage = "Exception while evaluating final activity result for : " + activityType;
            LOGGER.error("{}. Exception : {}", logMessage, ex);
            activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), eventType, neJobStaticData.getNodeName(), cvMoFdn,
                    activityUtils.additionalInfoForEvent(activityJobId, neJobStaticData.getNodeName(), logMessage));
        }
    }

    @SuppressWarnings("unchecked")
    private JobResult evaluateJobResult(final long activityJobId, final Map<String, Object> cvAttributes, final List<Map<String, Object>> jobLogList) {
        final String activityType = getActivityType();
        JobResult jobResult = JobResult.FAILED;
        final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
        final Map<String, Object> currentActionResultData = (Map<String, Object>) cvAttributes.get(ConfigurationVersionMoConstants.ACTION_RESULT);
        try {
            final int actionIdFromDatabase = configurationVersionUtils.getActionId(activityJobAttributes);
            final int actionIdFromNode = (int) currentActionResultData.get(ConfigurationVersionMoConstants.ACTION_ID);
            reportActionResultJobLogs(jobLogList, cvAttributes);
            if (actionIdFromNode == actionIdFromDatabase) {
                if (currentActionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT) != null) {
                    jobResult = processMainAction(cvAttributes, jobLogList, currentActionResultData);
                }
            } else {
                final String logMessage = String.format("Action ID present on node : %s does not match with the triggered action ID : %s. Assuming job failed.", actionIdFromNode,
                        actionIdFromDatabase);
                activityUtils.addJobLog(logMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            }
            if (jobResult == JobResult.FAILED) {
                activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_FAILED, activityType), JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            }
        } catch (final Exception ex) {
            final Throwable th = ex.getCause();
            final String exceptionMessage = th != null ? ex.getCause().getMessage() : ex.getMessage();
            final String logMessage = "Activity Failed as an error occurred while deriving the result : " + exceptionMessage;
            activityUtils.addJobLog(logMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            LOGGER.error("Exception occurred while deriving jobResult {} for activityJobId : {}, Exception : {}", activityType, activityJobId, ex);
        }
        return jobResult;
    }

    /**
     * Prepares jobLog List with action result info.
     *
     * @param currentActionResultData
     * @return
     */
    @SuppressWarnings("unchecked")
    private void reportActionResultJobLogs(final List<Map<String, Object>> jobLogList, final Map<String, Object> cvAttributes) {
        final CvActionMainAndAdditionalResultHolder actionResultInfoHolder = configurationVersionUtils.retrieveActionResultDataWithAddlInfo(cvAttributes);
        reportActionResult(actionResultInfoHolder, jobLogList);
        if (ActivityConstants.DOWNLOAD_CV.equalsIgnoreCase(getActivityType())) {
            final Map<String, Object> currentActionResultData = (Map<String, Object>) cvAttributes.get(ConfigurationVersionMoConstants.ACTION_RESULT);
            if (currentActionResultData != null) {
                final String configurationVersionName = (String) currentActionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME);
                if (configurationVersionName != null && !configurationVersionName.isEmpty()) {
                    final String jobLog = "Resulting CV name: " + configurationVersionName;
                    activityUtils.addJobLog(jobLog, JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
                }
            }
        }
        reportActionAdditionalInfo(actionResultInfoHolder, jobLogList);
    }

    /**
     * report actionAdditionalInfo
     *
     * @param @CvActionMainAndAdditionalResultHolder
     *            actionResultInfoHolder
     * @param jobLogList
     */
    protected void reportActionAdditionalInfo(final CvActionMainAndAdditionalResultHolder actionResultInfoHolder, final List<Map<String, Object>> jobLogList) {
        final List<CvActionAdditionalInfo> actionAdditionalResult = actionResultInfoHolder.getActionAdditionalResult();
        String jobLog = null;
        for (final CvActionAdditionalInfo additionalInfo : actionAdditionalResult) {
            if (additionalInfo.getInformation() == CVActionResultInformation.CV_BACKUP_NAME) {
                jobLog = "CV backup name: " + additionalInfo.getAdditionalInformation();
                activityUtils.addJobLog(jobLog, JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
            } else if (additionalInfo.getAdditionalInformation().length() > 0) {
                jobLog = "Additional info: " + additionalInfo.getAdditionalInformation();
                activityUtils.addJobLog(jobLog, JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
            }
        }
    }

    /**
     * report actionResult info.
     *
     * @param actionResultInfoHolder
     * @param jobLogList
     */
    protected void reportActionResult(final CvActionMainAndAdditionalResultHolder actionResultInfoHolder, final List<Map<String, Object>> jobLogList) {
        final CVActionMainResult actionMainResult = actionResultInfoHolder.getCvActionMainResult();
        if (actionMainResult != null) {
            String jobLog = actionMainResult.getMainResultMessage();
            activityUtils.addJobLog("Main Action Result: " + jobLog, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.INFO.toString());
            if (actionResultInfoHolder.getPathToDetailInformation() != null && !actionResultInfoHolder.getPathToDetailInformation().trim().isEmpty()) {
                jobLog = "Path to detail information: " + actionResultInfoHolder.getPathToDetailInformation();
                activityUtils.addJobLog(jobLog, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.INFO.toString());
            }
        }
    }

    protected JobResult processMainAction(final Map<String, Object> cvAttributes, final List<Map<String, Object>> jobLogList, final Map<String, Object> currentActionResultData) {
        final String activityType = getActivityType();
        final String cvActionMainResultValue = (String) currentActionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT);
        if (cvActionMainResultValue != null && !ActivityConstants.EMPTY.equals(cvActionMainResultValue)) {
            final CVActionMainResult cvActionMainResult = CVActionMainResult.getCvActionMainResult(cvActionMainResultValue);
            if (isActionSucess(cvActionMainResult)) {
                final String configurationVersionName = (String) currentActionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME);
                activityUtils.addJobLog("For CV " + configurationVersionName + " " + String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, activityType), JobLogType.SYSTEM.toString(),
                        jobLogList, JobLogLevel.INFO.toString());
                return JobResult.SUCCESS;
            } else if (backupActionResultUtility.isActionFailed(cvActionMainResult)) {
                processFailureReason(cvAttributes, jobLogList);
            }
        }
        return JobResult.FAILED;
    }

    @SuppressWarnings("unchecked")
    protected void processFailureReason(final Map<String, Object> cvAttributes, final List<Map<String, Object>> jobLogList) {
        final String activityType = getActivityType();
        final List<Map<String, Object>> additionalActionResultDataList = (List<Map<String, Object>>) cvAttributes.get(ShmConstants.CV_ADDITIONAL_ACTION_RESULT_DATA);
        if (additionalActionResultDataList != null) {
            int i = 1;
            for (final Map<String, Object> additionalActionResultData : additionalActionResultDataList) {
                final CVActionResultInformation information = CVActionResultInformation.getCvActionResultInformation((String) additionalActionResultData.get(ShmConstants.CV_INFORMATION));
                final String failureLogMessage = String.format(JobLogConstants.ADDITIONAL_FAILURE_RESULT, activityType, i, information.getCVActionResultInformationDesc());
                activityUtils.addJobLog(failureLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
                i++;
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected ActivityStepResult handleTimeoutActivity(final long activityJobId, final NEJobStaticData neJobStaticData) {
        final String activityType = getActivityType();
        final String eventType = getNotificationEventType();
        LOGGER.debug("Entering {} handleTimeout() for  activityJobId = {}", activityType, activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        try {
            final String logMessage = String.format(JobLogConstants.TIMEOUT, activityType);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            final String nodeName = neJobStaticData.getNodeName();
            String cvMoFdn = null;
            Map<String, Object> cvAttributes = null;
            final Map<String, Object> moAttributesMap = configurationVersionService.getCVMoAttr(nodeName);
            if (moAttributesMap != null && !moAttributesMap.isEmpty()) {
                cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
                cvAttributes = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
            } else {
                throw new MoNotFoundException("Configuration Version MO not found for node " + nodeName);
            }
            activityUtils.unSubscribeToMoNotifications(cvMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), eventType, nodeName, cvMoFdn, "SHM:" + activityJobId + ":" + nodeName + ":" + logMessage);
            final JobResult jobResult = evaluateJobResult(activityJobId, cvAttributes, jobLogList);
            setTimeoutResult(activityJobId, neJobStaticData, activityStepResult, jobResult, jobPropertyList, jobLogList);
        } catch (final MoNotFoundException moNotFoundException) {
            LOGGER.error("Handle time out failed for node {} due to : {}", neJobStaticData.getNodeName(), moNotFoundException);
            final String logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, getActivityType()) + String.format(JobLogConstants.FAILURE_REASON, moNotFoundException.getMessage());
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        } catch (final Exception exception) {
            String exceptionMessage = ExceptionParser.getReason(exception);
            if (exceptionMessage == null || exceptionMessage.isEmpty()) {
                exceptionMessage = exception.getMessage();
            }
            final String logMessage = getActivityType() + " handleTimeout failed." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Handle time out failed for node {} due to : {}", neJobStaticData.getNodeName(), exception);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.HANDLE_TIMEOUT);
        return activityStepResult;
    }

    protected void setTimeoutResult(final long activityJobId, final NEJobStaticData neJobStaticData, final ActivityStepResult activityStepResult, final JobResult jobResult,
            final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
        if (jobResult == JobResult.SUCCESS) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
            activityAndNEJobProgressCalculator.updateActivityJobProgressPercentage(activityJobId, jobPropertyList, jobLogList, HANDLE_TIMEOUT_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        }
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
    }

    /**
     * This method has been Deprecated, use {@link AbstractBackupActivity.cancelTimeoutASctivity(long, NEJobStaticData, boolean) method instead.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    protected ActivityStepResult cancelTimeoutActivity(final long activityJobId, final boolean finalizeResult) {
        final String activityType = getActivityType();
        final String eventType = getNotificationEventType();
        LOGGER.debug("Entering {} cancelTimeout() for activityJobId={}", activityType, activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        try {
            final String logMessage = String.format(JobLogConstants.CANCEL_TIMEOUT, activityType);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
            String cvMoFdn = null;
            Map<String, Object> cvAttributes = null;
            final Map<String, Object> moAttributesMap = configurationVersionService.getCVMoAttr(jobEnvironment.getNodeName());
            if (moAttributesMap != null) {
                cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
                cvAttributes = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
            }
            activityUtils.recordEvent(eventType, jobEnvironment.getNodeName(), cvMoFdn, "SHM:" + activityJobId + ":" + jobEnvironment.getNodeName() + ":" + logMessage);
            final JobResult moActionResult = evaluateMOActionResultForCancel(jobEnvironment, cvAttributes, jobLogList);
            setTimeoutResultForCancel(activityStepResult, moActionResult, jobEnvironment, finalizeResult);
            if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS) {
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
            } else if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_FAIL) {
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            }
            if (finalizeResult && moActionResult == null) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.STILL_EXECUTING, activityType), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
            }
        } catch (final Exception e) {
            final String errorMsg = "An exception occurred while handlingTimeout for activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
        }
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        return activityStepResult;
    }

    @SuppressWarnings("unchecked")
    protected ActivityStepResult cancelTimeoutActivity(final long activityJobId, final NEJobStaticData neJobStaticData, final boolean finalizeResult) {
        final String activityType = getActivityType();
        final String eventType = getNotificationEventType();
        LOGGER.debug("Entering {} cancelTimeout() for activityJobId={}", activityType, activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        try {
            final String logMessage = String.format(JobLogConstants.CANCEL_TIMEOUT, activityType);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
            String cvMoFdn = null;
            Map<String, Object> cvAttributes = null;
            final Map<String, Object> moAttributesMap = configurationVersionService.getCVMoAttr(neJobStaticData.getNodeName());
            if (moAttributesMap != null) {
                cvMoFdn = (String) moAttributesMap.get(ShmConstants.FDN);
                cvAttributes = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
            }
            activityUtils.recordEvent(activityUtils.getJobExecutionUser(neJobStaticData.getMainJobId()), eventType, neJobStaticData.getNodeName(), cvMoFdn,
                    "SHM:" + activityJobId + ":" + neJobStaticData.getNodeName() + ":" + logMessage);
            final JobResult moActionResult = evaluateMOActionResultForCancel(activityJobId, cvAttributes, jobLogList);
            setTimeoutResultForCancel(activityJobId, neJobStaticData, activityStepResult, moActionResult, finalizeResult);
            if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS) {
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
            } else if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_FAIL) {
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            }
            if (finalizeResult && moActionResult == null) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.STILL_EXECUTING, activityType), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.ERROR.toString());
            }
        } catch (final Exception e) {
            LOGGER.error("An exception occurred while handlingTimeout for activityJobId : {}. Exception is : {}", activityJobId, e);
        }
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
        return activityStepResult;
    }

    /**
     * This method has been Deprecated, use {@link AbstractBackupActivity.evaluateMOActionResultForCancel(long, NEJobStaticData, Map, List) method instead.
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    private JobResult evaluateMOActionResultForCancel(final JobEnvironment jobEnvironment, final Map<String, Object> cvAttributes, final List<Map<String, Object>> jobLogList) {
        final String activityType = getActivityType();
        final long activityJobId = jobEnvironment.getActivityJobId();
        JobResult jobResult = null;
        final Map<String, Object> activityJobAttributes = jobEnvironment.getActivityJobAttributes();
        final Map<String, Object> currentActionResultData = (Map<String, Object>) cvAttributes.get(ConfigurationVersionMoConstants.ACTION_RESULT);
        try {
            final int actionIdFromDatabase = configurationVersionUtils.getActionId(activityJobAttributes);
            final int actionIdFromNode = (int) currentActionResultData.get(ConfigurationVersionMoConstants.ACTION_ID);
            if (actionIdFromNode == actionIdFromDatabase) {
                if (currentActionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT) != null) {
                    jobResult = processMainAction(cvAttributes, jobLogList, currentActionResultData);
                }
            } else {
                final String logMessage = String.format("Action ID present on node : %s does not match with the triggered action ID : %s.", actionIdFromNode, actionIdFromDatabase);
                activityUtils.addJobLog(logMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            }
            if (jobResult == JobResult.FAILED) {
                activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_FAILED, activityType), JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            }
        } catch (final Exception ex) {
            final Throwable th = ex.getCause();
            final String exceptionMessage = th != null ? ex.getCause().getMessage() : ex.getMessage();
            final String logMessage = "Activity Failed as an error occurred while deriving the result : " + exceptionMessage;
            activityUtils.addJobLog(logMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            LOGGER.error("Exception occurred while deriving jobResult {} for activityJobId : {}, Exception : ", activityType, activityJobId, ex);
        }
        return jobResult;
    }

    @SuppressWarnings("unchecked")
    private JobResult evaluateMOActionResultForCancel(final long activityJobId, final Map<String, Object> cvAttributes, final List<Map<String, Object>> jobLogList) {
        final String activityType = getActivityType();
        JobResult jobResult = null;
        final Map<String, Object> activityJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
        final Map<String, Object> currentActionResultData = (Map<String, Object>) cvAttributes.get(ConfigurationVersionMoConstants.ACTION_RESULT);
        try {
            final int actionIdFromDatabase = configurationVersionUtils.getActionId(activityJobAttributes);
            final int actionIdFromNode = (int) currentActionResultData.get(ConfigurationVersionMoConstants.ACTION_ID);
            if (actionIdFromNode == actionIdFromDatabase) {
                if (currentActionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT) != null) {
                    jobResult = processMainAction(cvAttributes, jobLogList, currentActionResultData);
                }
            } else {
                final String logMessage = String.format("Action ID present on node : %s does not match with the triggered action ID : %s.", actionIdFromNode, actionIdFromDatabase);
                activityUtils.addJobLog(logMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            }
            if (jobResult == JobResult.FAILED) {
                activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_FAILED, activityType), JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            }
        } catch (final Exception ex) {
            final Throwable th = ex.getCause();
            final String exceptionMessage = th != null ? ex.getCause().getMessage() : ex.getMessage();
            final String logMessage = "Activity Failed as an error occurred while deriving the result : " + exceptionMessage;
            activityUtils.addJobLog(logMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
            LOGGER.error("Exception occurred while deriving jobResult {} for activityJobId : {}, Exception : ", activityType, activityJobId, ex);
        }
        return jobResult;
    }

    /**
     * This method has been Deprecated, use {@link AbstractBackupActivity.setTimeoutResultForCancel(long, NEJobStaticData, ActivityStepResult, JobResult, boolean) method instead.
     */
    @Deprecated
    protected void setTimeoutResultForCancel(final ActivityStepResult activityStepResult, final JobResult jobResult, final JobEnvironment jobEnvironment, final boolean finalizeResult) {
        if ((finalizeResult && jobResult == null) || jobResult == JobResult.FAILED) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        } else if (jobResult == JobResult.SUCCESS || jobResult == JobResult.SKIPPED) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
        }
    }

    protected void setTimeoutResultForCancel(final long activityJobId, final NEJobStaticData neJobStaticData, final ActivityStepResult activityStepResult, final JobResult jobResult,
            final boolean finalizeResult) {
        if ((finalizeResult && jobResult == null) || jobResult == JobResult.FAILED) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        } else if (jobResult == JobResult.SUCCESS || jobResult == JobResult.SKIPPED) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        } else {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_ACTIVITY_ONGOING);
        }
    }

    @SuppressWarnings("unchecked")
    protected String getCvNameToBeProcessed(final String cvNames, final Map<String, Object> activityJobAttributes, final List<Map<String, Object>> jobPropertyList) {
        LOGGER.debug("Get individual CV Name from CV Names list. cvNames {}, activityJobAttributes {}, jobPropertyList {}", cvNames, activityJobAttributes, jobPropertyList);
        final int processedBackups = getCountOfProcessedBackups((List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES));
        final String[] listOfCvNames = cvNames.split(ActivityConstants.COMMA);
        activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.PROCESSED_BACKUPS, Integer.toString(processedBackups + 1));
        activityUtils.prepareJobPropertyList(jobPropertyList, BackupActivityConstants.TOTAL_BACKUPS, Integer.toString(listOfCvNames.length));
        return listOfCvNames[processedBackups];
    }

    @SuppressWarnings("unchecked")
    protected String getProcessedCvName(final String cvNames, final Map<String, Object> activityJobAttributes) {
        LOGGER.debug("Get individual CV Name from CV Names list. cvNames {}, activityJobAttributes {}", cvNames, activityJobAttributes);
        final int processedBackups = getCountOfProcessedBackups((List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES));
        final String[] listOfCvNames = cvNames.split(ActivityConstants.COMMA);
        return listOfCvNames[processedBackups - 1];
    }

    protected int getCountOfProcessedBackups(final List<Map<String, String>> jobPropertyList) {
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

    protected int getCountOfTotalBackups(final List<Map<String, String>> activityJobPropertyList) {
        int totalBackups = 0;
        if (activityJobPropertyList != null) {
            for (final Map<String, String> eachJobProperty : activityJobPropertyList) {
                if (BackupActivityConstants.TOTAL_BACKUPS.equals(eachJobProperty.get(ShmConstants.KEY))) {
                    totalBackups = Integer.parseInt(eachJobProperty.get(ShmConstants.VALUE));
                    break;
                }
            }
        }
        LOGGER.debug("Total backups count = {}", totalBackups);
        return totalBackups;
    }

}
