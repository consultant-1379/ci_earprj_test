/*------------------------------------------------------------------------------
 *******************************************************************************
0 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.service;

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
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.common.StnInformationProvider;
import com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.common.StnJobActivityServiceHelper;
import com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.common.StnJobInformation;
import com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.common.StnSoftwareEndUpgradeEventSender;
import com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.common.StnSoftwareUpgradeAdjustEventSender;
import com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.common.StnSoftwareUpgradeJobService;
import com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.constants.StnJobConstants;
import com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.exception.StnUpgradeException;
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

/**
 * This class performs SwAdjust activity of Software Upgrade job on STN nodes.
 * 
 * @author xsamven/xvenupe
 * 
 */
@EServiceQualifier("STN.UPGRADE.swadjust")
@ActivityInfo(activityName = "swadjust", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.STN)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SwAdjustService implements Activity, ActivityCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwAdjustService.class);

    @Inject
    SystemRecorder systemRecorder;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private StnJobActivityServiceHelper stnJobActivityService;

    @Inject
    private StnSoftwareUpgradeAdjustEventSender stnSoftwareUpgradeEventSender;

    @Inject
    private StnSoftwareUpgradeJobService stnSoftwareUpgradeActivityService;

    @Inject
    private CancelSoftwareUpgradeService cancelSoftwareUpgradeService;

    @Inject
    private StnInformationProvider stnInfoProvider;

    @Inject
    private StnSoftwareEndUpgradeEventSender stnSoftwareEndUpgradeEventSender;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobLogUtil jobLogUtil;

    /**
     * Precheck for SwAdjust activity of Software Upgrade on STN nodes.
     * 
     * @param activityJobId
     * @return activityStepResult
     */
    @SuppressWarnings("deprecation")
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogs = new ArrayList<>();

        LOGGER.debug("ActivityJob ID - [{}] :Entered precheck of SwAdjust activity of software upgrade on STN node.", activityJobId); //fetch Node name
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.SW_ADJUST);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }
            activityUtils.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.PRECHECK_SUCCESS, ActivityConstants.SW_ADJUST), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
            LOGGER.info("ActivityJob ID - [{}] : Precheck of SwAdjust activity is completed. Result : {}", activityJobId, activityStepResult.getActivityResultEnum());
        } catch (Exception exception) {
            LOGGER.error("Exception occured in precheck() for activityJobId:{}. Reason: ", activityJobId, exception);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.SW_ADJUST, exception.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs);
        return activityStepResult;
    }

    /**
     * Method to register for notifications, initiate and perform the SwAdjust action on NODE.
     * 
     * @param activityJobId
     * 
     */

    @SuppressWarnings("deprecation")
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final Map<String, Object> processVariables = new HashMap<>();
        StnJobInformation stnUpgradeInformation = null;
        JobEnvironment jobContext = null;
        String logMessage = "";
        stnInfoProvider.persistStnData(activityJobId);

        try {
            stnUpgradeInformation = stnSoftwareUpgradeActivityService.buildStnUpgradeInformation(activityJobId);
            if (stnUpgradeInformation == null) {
                LOGGER.error("Unable to fetch the required information of the activityId {} ", activityJobId);
                logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.SW_ADJUST);
                activityUtils.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                stnJobActivityService.failActivity(activityJobId, jobLogs, activityUtils.getJobEnvironment(activityJobId), ActivityConstants.SW_ADJUST, processVariables);
                return;
            }
            LOGGER.info("ActivityJob ID - [{}] : executing SwAdjust activity of software upgrade on node:{}", activityJobId, stnUpgradeInformation.getNodeFdn());
            jobContext = stnUpgradeInformation.getJobEnvironment();

            systemRecorder.recordCommand(SHMEvents.STN_ADJUST_SERVICE, CommandPhase.STARTED, stnUpgradeInformation.getNeName(), stnUpgradeInformation.getNodeFdn(), StnJobConstants.ADJUST_SW_ACTIVITY);
            sendSoftwareUpgradeActivityRequest(activityJobId, stnUpgradeInformation, activityUtils.getActivityInfo(activityJobId, this.getClass()));
            activityUtils.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.SW_ADJUST), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            LOGGER.debug("ActivityJob ID - [{}] : Execute of SwAdjust activity is triggered successfully.", activityJobId);

        } catch (final StnUpgradeException stnUpgradeException) {
            LOGGER.error("StnUpgradeException received for activityId {} for Node:{} Reason : {}", activityJobId, stnUpgradeInformation.getNodeFdn(), stnUpgradeException);
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.SW_ADJUST);
            activityUtils.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            stnJobActivityService.failActivity(activityJobId, jobLogs, jobContext, ActivityConstants.SW_ADJUST, processVariables);
        } catch (final Exception exception) {
            LOGGER.error("Failed to execute SwAdjust activity and cannot be proceeded with activity with activityId {} on Node. Reason : {}", activityJobId, stnUpgradeInformation.getNodeFdn(),
                    exception);
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.SW_ADJUST);
            activityUtils.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            stnJobActivityService.failActivity(activityJobId, jobLogs, jobContext, ActivityConstants.SW_ADJUST, processVariables);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs);
    }

    /**
     * Method to send request to STN to perform ApproveSw action and subscribe to Mo Notifications
     * 
     * @param activityJobId
     * @param stnUpgradeInformation
     * @param jobActivityInformation
     */
    @SuppressWarnings("deprecation")
    private void sendSoftwareUpgradeActivityRequest(final long activityJobId, final StnJobInformation stnUpgradeInformation, final JobActivityInfo jobActivityInformation) {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        LOGGER.debug("ActivityJob ID - [{}] : SwAdjust activity of STN software upgrade job is triggered. Software Upgrade Job Information : {} ", activityJobId, stnUpgradeInformation);
        activityUtils.prepareJobLogAtrributesList(jobLogs, String.format(StnJobConstants.ACTION_ABOUT_TO_TRIGGER, ActivityConstants.SW_ADJUST, stnUpgradeInformation.getNodeFdn()), new Date(),
                JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        final String nodeFdn = stnUpgradeInformation.getNodeFdn();
        final String subscriptionKey = StnJobConstants.SUBSCRIPTION_KEY_DELIMETER + stnUpgradeInformation.getNodeFdn() + StnJobConstants.SUBSCRIPTION_KEY_DELIMETER
                + StnJobConstants.ADJUST_SW_ACTIVITY + StnJobConstants.SUBSCRIPTION_KEY_DELIMETER;
        if (nodeFdn != null) {
            final Map<String, Object> eventAttributes = new HashMap<>();
            eventAttributes.put(ShmConstants.NE_NAME, stnUpgradeInformation.getNeName());
            eventAttributes.put(ActivityConstants.ACTIVITY_JOB_ID, activityJobId);
            LOGGER.debug("ActivityJob ID - [{}] : Subscription key for SwAdjust action is : {}", activityJobId, subscriptionKey);
            activityUtils.subscribeToMoNotifications(subscriptionKey, activityJobId, jobActivityInformation);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs);
            stnSoftwareUpgradeEventSender.sendSoftwareUpgradeActionRequest(activityJobId, ActivityConstants.SW_ADJUST, nodeFdn, eventAttributes);
        } else {
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs);
            throw new StnUpgradeException(StnJobConstants.FDN_NOT_FOUND);
        }

    }

    /**
     * This method processes the notifications by fetching the notification subject and validating it.
     * 
     * @param message
     * 
     */

    @Override
    public void processNotification(final Notification message) {

        final SHMCommonCallBackNotificationJobProgressBean notification = (SHMCommonCallBackNotificationJobProgressBean) message;

        LOGGER.debug("Notification received from mediation for activate activity is {} for {}", message, notification.getCommonNotification().getFdn());

        final long activityJobId = (long) notification.getCommonNotification().getAdditionalAttributes().get(StnJobConstants.ACTIVITY_JOB_ID);
        final StnJobInformation stnUpgradeInformation = stnSoftwareUpgradeActivityService.buildStnUpgradeInformation(activityJobId);
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(activityJobId, this.getClass());

        if (notification.getCommonNotification() != null) {
            handleActivityProgressNotification(notification, stnUpgradeInformation, jobActivityInformation);
        }
    }

    /**
     * Method that validates the Notification and updates the job status .
     * 
     * @param notification
     * @param stnUpgradeInformation
     * @param jobActivityInformation
     */
    @SuppressWarnings("deprecation")
    private void handleActivityProgressNotification(final SHMCommonCallBackNotificationJobProgressBean notification, final StnJobInformation stnUpgradeInformation,
            final JobActivityInfo jobActivityInformation) {

        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final Map<String, Object> processVariables = new HashMap<>();
        final Map<String, Object> eventAttributes = new HashMap<>();
        final long activityJobId = (long) notification.getCommonNotification().getAdditionalAttributes().get(StnJobConstants.ACTIVITY_JOB_ID);

        LOGGER.debug("ActivityJob ID - [{}] : Job notification received for SwAdjust activity is: {}", activityJobId, notification.getCommonNotification());
        eventAttributes.put(ShmConstants.NE_NAME, stnUpgradeInformation.getNeName());
        eventAttributes.put(ActivityConstants.ACTIVITY_JOB_ID, activityJobId);

        final JobEnvironment jobContext = stnUpgradeInformation.getJobEnvironment();

        final String result = notification.getCommonNotification().getResult();

        final String subscriptionKey = StnJobConstants.SUBSCRIPTION_KEY_DELIMETER + stnUpgradeInformation.getNodeFdn() + StnJobConstants.SUBSCRIPTION_KEY_DELIMETER
                + StnJobConstants.ADJUST_SW_ACTIVITY + StnJobConstants.SUBSCRIPTION_KEY_DELIMETER;

        final String activityName = (String) notification.getCommonNotification().getAdditionalAttributes().get(StnJobConstants.ACTIVITY_NAME);

        final String nodeAddress = stnUpgradeInformation.getNodeFdn();

        try {

            final double progressPercent = Double.parseDouble(notification.getCommonNotification().getProgressPercentage().trim());

            if (ShmConstants.SUCCESS.equalsIgnoreCase(result)) {
                activityUtils.prepareJobLogAtrributesList(jobLogs, "SwAdjust Activity is Success ",
                        (Date) notification.getCommonNotification().getAdditionalAttributes().get(StnJobConstants.NOTIFICATION_TIME_STAMP), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
                LOGGER.debug("Updating Job attributes with JobId {} Properties {} Logs {} Percent {}", activityJobId, jobProperties, jobLogs, progressPercent);
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobProperties, jobLogs, progressPercent);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(stnUpgradeInformation.getNeJobId());
                activityUtils.unSubscribeToMoNotifications(subscriptionKey, activityJobId, jobActivityInformation);
                activityUtils.sendNotificationToWFS(jobContext, jobActivityInformation.getActivityJobId(), ActivityConstants.SW_ADJUST, processVariables);
                systemRecorder.recordCommand(SHMEvents.STN_ADJUST_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, stnUpgradeInformation.getNeName(), stnUpgradeInformation.getNodeFdn(),
                        StnJobConstants.ADJUST_SW_ACTIVITY);
                LOGGER.debug("ActivityJob ID - [{}] : Create Software Upgrade Job has been completed successfully. Notifying to workflow service.", activityJobId);

            } else {
                activityUtils.prepareJobLogAtrributesList(jobLogs, "SwAdjust Activity is Failed ",
                        (Date) notification.getCommonNotification().getAdditionalAttributes().get(StnJobConstants.NOTIFICATION_TIME_STAMP), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
                activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobProperties, jobLogs, progressPercent);
                activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(stnUpgradeInformation.getNeJobId());
                activityUtils.unSubscribeToMoNotifications(subscriptionKey, activityJobId, jobActivityInformation);
                activityUtils.sendNotificationToWFS(jobContext, jobActivityInformation.getActivityJobId(), ActivityConstants.SW_ADJUST, processVariables);
                stnSoftwareEndUpgradeEventSender.sendSoftwareUpgradeActionRequest(activityJobId, activityName, nodeAddress, eventAttributes);
                systemRecorder.recordCommand(SHMEvents.STN_ADJUST_SERVICE, CommandPhase.FINISHED_WITH_ERROR, stnUpgradeInformation.getNeName(), stnUpgradeInformation.getNodeFdn(),
                        StnJobConstants.ADJUST_SW_ACTIVITY);
                LOGGER.debug("ActivityJob ID - [{}] : Create Software Upgrade Job has been completed successfully. Notifying to workflow service.", activityJobId);

            }

        } catch (Exception exception) {
            LOGGER.error("ActivityJob ID - [{}] : Failed to process Notification of SwAdjust activity. Reason : {}", activityJobId, exception);
            final String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.SW_ADJUST);
            activityUtils.prepareJobLogAtrributesList(jobLogs, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
    }

    /**
     * Method to handle the software upgrade job execution in timeout scenario.
     */
    @SuppressWarnings("deprecation")
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final JobResult jobResult = JobResult.FAILED;

        LOGGER.debug("ActivityJob ID - [{}] : Handling SwAdjust Activity of software upgrade in timeout.", activityJobId);
        final StnJobInformation stnUpgradeInformation = stnSoftwareUpgradeActivityService.buildStnUpgradeInformation(activityJobId);
        if (stnUpgradeInformation == null) {
            LOGGER.error("Unable to fetch the required information of the activityId {} ", activityJobId);
            final String logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.SW_ADJUST);
            activityUtils.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            stnJobActivityService.failActivity(activityJobId, jobLogs, activityUtils.getJobEnvironment(activityJobId), ActivityConstants.SW_ADJUST, new HashMap<String, Object>());
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
            return activityStepResult;
        }
        final JobActivityInfo jobActivityInformation = activityUtils.getActivityInfo(activityJobId, this.getClass());
        final String nodeAddress = stnUpgradeInformation.getNodeFdn();
        final Map<String, Object> eventAttributes = new HashMap<>();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);

        final String subscriptionKey = StnJobConstants.SUBSCRIPTION_KEY_DELIMETER + stnUpgradeInformation.getNodeFdn() + StnJobConstants.SUBSCRIPTION_KEY_DELIMETER
                + StnJobConstants.ADJUST_SW_ACTIVITY + StnJobConstants.SUBSCRIPTION_KEY_DELIMETER;

        eventAttributes.put(ShmConstants.NE_NAME, stnUpgradeInformation.getNeName());
        eventAttributes.put(ActivityConstants.ACTIVITY_JOB_ID, activityJobId);
        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
        final String logMessage = String.format(StnJobConstants.ACTIVITY_IN_TIMEOUT, StnJobConstants.ADJUST_SW_ACTIVITY);
        activityUtils.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs);
        stnSoftwareEndUpgradeEventSender.sendSoftwareUpgradeActionRequest(activityJobId, StnJobConstants.ADJUST_SW_ACTIVITY, nodeAddress, eventAttributes);
        activityUtils.unSubscribeToMoNotifications(subscriptionKey, activityJobId, jobActivityInformation);
        return activityStepResult;
    }

    /**
     * Method to cancel the software upgrade job execution in activate activity.
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.debug("ActivityJob ID - [{}] : Processing cancel action", activityJobId);
        final String actionTriggered = StnJobConstants.ADJUST_SW_ACTIVITY;
        return cancelSoftwareUpgradeService.cancel(activityJobId, actionTriggered, activityUtils.getActivityInfo(activityJobId, this.getClass()));
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        LOGGER.debug("ActivityJob ID - [{}] : Processing timeout for cancel action", activityJobId);
        return new ActivityStepResult();
    }

}
