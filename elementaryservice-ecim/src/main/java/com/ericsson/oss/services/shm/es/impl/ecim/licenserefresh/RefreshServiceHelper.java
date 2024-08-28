/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh;

import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

public class RefreshServiceHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshServiceHelper.class);

    private static final ActivityInfo activityAnnotation = RefreshService.class.getAnnotation(ActivityInfo.class);

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected JobLogUtil jobLogUtil;

    @Inject
    protected SystemRecorder systemRecorder;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private LicenseRefreshServiceProvider licenseRefreshServiceProvider;

    public ActivityStepResult precheck(long activityJobId) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        long mainJobId = 0L;
        String nodeName = "";
        String businessKey = "";
        NEJobStaticData neJobStaticData = null;

        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY);
            mainJobId = neJobStaticData.getMainJobId();
            nodeName = neJobStaticData.getNodeName();
            businessKey = neJobStaticData.getNeJobBusinessKey();
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(mainJobId);
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(PRECHECK_INITIATED, ActivityConstants.LICENSE_REFRESH_REFRESH), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            systemRecorder.recordCommand(SHMEvents.LICENSEREFRESH_REFRESH_PRECHECK, CommandPhase.STARTED, nodeName, businessKey,
                    activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.LICENSE));

            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.LICENSE_REFRESH_REFRESH);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
                return activityStepResult;
            }

            if (skipExecutionIfRequestDataPOsAvailable(activityJobId, neJobStaticData)) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION);
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.PRE_CHECK_SUCCESS_SKIP_EXECUTION, ActivityConstants.LICENSE_REFRESH_REFRESH, MO_ACTION_SKIPPED),
                        new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

            } else {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.PRECHECK_SUCCESS, ActivityConstants.LICENSE_REFRESH_REFRESH), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
            }

            systemRecorder.recordCommand(SHMEvents.LICENSEREFRESH_REFRESH_PRECHECK, CommandPhase.FINISHED_WITH_SUCCESS, neJobStaticData.getNodeName(), neJobStaticData.getNeJobBusinessKey(),
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE_REFRESH));

            LOGGER.info("LicenseRefreshJob:Refresh activity {} - Precheck result {}", activityJobId, activityStepResult.getActivityResultEnum());

        } catch (Exception exception) {
            LOGGER.error("Exception occured in precheck() for activityJobId:{}. Reason: ", activityJobId, exception);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION);
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.LICENSE_REFRESH_REFRESH, exception.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
        return activityStepResult;
    }

    public void execute(long activityJobId, JobActivityInfo jobActivityInfo) {

        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        long mainJobId = 0L;
        long neJobId = 0L;
        String nodeName = "";
        String businessKey = "";
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY);
            mainJobId = neJobStaticData.getMainJobId();
            nodeName = neJobStaticData.getNodeName();
            businessKey = neJobStaticData.getNeJobBusinessKey();
            neJobId = neJobStaticData.getNeJobId();

            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.LICENSE_REFRESH_REFRESH), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            final String instantaneousLicensingMOFdn = licenseRefreshServiceProvider.findInstantaneousLicensingMOFdn(nodeName);

            activityUtils.subscribeToMoNotifications(instantaneousLicensingMOFdn, activityJobId, jobActivityInfo);
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(BEFORE_MO_ACTION_INVOCATION, nodeName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

            final int actionId = performAction(instantaneousLicensingMOFdn);
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(MO_ACTION_TRIGGERED, nodeName, actionId), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            if (actionId == -1) {
                systemRecorder.recordCommand(SHMEvents.LICENSEREFRESH_REFRESH_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, nodeName, activityAnnotation.activityName(),
                        activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.LICENSE_REFRESH));
                jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(MO_ACTION_FAILURE_REASON, nodeName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
                activityUtils.unSubscribeToMoNotifications(instantaneousLicensingMOFdn, activityJobId, jobActivityInfo);
                activityUtils.failActivity(activityJobId, jobLogs, businessKey, activityAnnotation.activityName());
            } else {
                final String subscriptionKey = buildSubscriptionKey(neJobId, actionId);
                activityUtils.subscribeToMoNotifications(subscriptionKey, activityJobId, jobActivityInfo);
                licenseRefreshServiceProvider.createShmNodeLicenseRefreshRequestDataPO(actionId, nodeName, neJobStaticData.getNeJobId());

                activityUtils.recordEvent(SHMEvents.LICENSEREFRESH_REFRESH_EXECUTE, nodeName, activityAnnotation.activityName(),
                        activityUtils.additionalInfoForEvent(activityJobId, nodeName, JobTypeEnum.LICENSE_REFRESH.toString()));
                systemRecorder.recordCommand(SHMEvents.LICENSEREFRESH_REFRESH_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, neJobStaticData.getNodeName(), neJobStaticData.getNeJobBusinessKey(),
                        activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE_REFRESH));
                LOGGER.debug("LicenseRefreshJob:Refresh activity {} - execute result finished with success with actionId {} and subscriptionKey {}", activityJobId, actionId, subscriptionKey);

                activityUtils.prepareJobPropertyList(jobProperties, LicenseRefreshConstants.CORRELATION_ID, subscriptionKey);
                activityUtils.prepareJobPropertyList(jobProperties, LicenseRefreshConstants.INSTANTANEOUS_LICENSING_MO_FDN, instantaneousLicensingMOFdn);
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);
            }

        } catch (JobDataNotFoundException e) {
            failExecute(activityJobId, jobLogs, nodeName, businessKey, mainJobId, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE);
        } catch (Exception e) {
            failExecute(activityJobId, jobLogs, nodeName, businessKey, mainJobId, e.getMessage());
        }
    }

    public void processNotification(Notification notification, JobActivityInfo jobActivityInfo) {
        final DpsAttributeChangedEvent event = (DpsAttributeChangedEvent) notification.getDpsDataChangedEvent();

        final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(event);
        final AttributeChangeData correlationIdAttributeChangeData = modifiedAttributes.get(LicenseRefreshConstants.CORRELATION_ID);
        if (correlationIdAttributeChangeData != null && correlationIdAttributeChangeData.getNewValue() != null) {
            processNotification(notification, jobActivityInfo, (String) correlationIdAttributeChangeData.getNewValue());
        }
        final AttributeChangeData progressReportAttributeChangeData = modifiedAttributes.get(LicenseRefreshConstants.PROGRESS_REPORT);
        if (progressReportAttributeChangeData != null && progressReportAttributeChangeData.getNewValue() != null) {
            final Map<String, Object> progressReportAttributes = (Map<String, Object>) progressReportAttributeChangeData.getNewValue();
            processNodeNotifications(notification, jobActivityInfo, progressReportAttributes);
        }
    }

    public ActivityStepResult handleTimeout(final long activityJobId, final JobActivityInfo jobActivityInfo) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY);

            final Map<String, Object> activityJobProperties = jobUpdateService.retrieveJobWithRetry(activityJobId);
            LOGGER.debug("LicenseRefreshJob:Refresh activity having activityJobProperties : {}", activityJobProperties);
            final List<Map<String, Object>> jobProperties = (List<Map<String, Object>>) activityJobProperties.get(ActivityConstants.JOB_PROPERTIES);
            for (final Map<String, Object> jobProperty : jobProperties) {
                if (LicenseRefreshConstants.CORRELATION_ID.equals(jobProperty.get(ShmConstants.KEY))) {
                    activityUtils.unSubscribeToMoNotifications((String) jobProperty.get(ShmConstants.VALUE), activityJobId, jobActivityInfo);
                }
                if (LicenseRefreshConstants.INSTANTANEOUS_LICENSING_MO_FDN.equals(jobProperty.get(ShmConstants.KEY))) {
                    activityUtils.unSubscribeToMoNotifications((String) jobProperty.get(ShmConstants.VALUE), activityJobId, jobActivityInfo);
                }
            }

            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());

            licenseRefreshServiceProvider.deleteShmNodeLicenseRefreshRequestDataPO(neJobStaticData.getNodeName());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(HANDLE_TIMEOUT, activityAnnotation.activityName(), neJobStaticData.getNodeName()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to fetch NeJobStaticData. Reason :{} ", jdnfEx.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, activityAnnotation.activityName(), jdnfEx.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final Exception exception) {
            LOGGER.error("Exception ocuured in handleTimeout().reason: {}", exception.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, activityAnnotation.activityName(), exception.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, 0d);
        LOGGER.debug("LicenseRefreshJob:Refresh activity {} - handletimeout finished", activityJobId);
        return activityStepResult;
    }

    private boolean skipExecutionIfRequestDataPOsAvailable(long activityJobId, NEJobStaticData neJobStaticData) {
        boolean skipExecution = false;
        if (isShmNodeLicenseRefreshRequestDataPOsAvailable(neJobStaticData.getNeJobId())) {
            final Map<String, Object> processVariables = new HashMap<>();
            processVariables.put("skipStepExecution", true);
            skipExecution = true;
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityAnnotation.activityName(), processVariables);
        }
        return skipExecution;
    }

    private void failExecute(final long activityJobId, final List<Map<String, Object>> jobLogs, final String nodeName, final String businessKey, final Long mainJobId, final String message) {
        LOGGER.error("LicenseRefreshJob:Refresh activity {} failed due to: {}", activityJobId, message);
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ACTIVITY_EXECUTE_FAILED_WITH_REASON, activityAnnotation.activityName(), message), new Date(),
                JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        systemRecorder.recordCommand(SHMEvents.LICENSEREFRESH_REFRESH_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, nodeName, activityAnnotation.activityName(),
                activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.LICENSE));
        activityUtils.failActivity(activityJobId, jobLogs, businessKey, activityAnnotation.activityName());
    }

    private void processNotification(final Notification notification, final JobActivityInfo jobActivityInfo, final String correlationId) {
        LOGGER.debug("LicenseRefreshJob:Refresh activity - AVC notification with notificationSubject as {} ", notification.getNotificationSubject());
        final long activityJobId = activityUtils.getActivityJobId(notification.getNotificationSubject());
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        NEJobStaticData neJobStaticData = null;
        String nodeName = "";
        String businessKey = "";
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            businessKey = neJobStaticData.getNeJobBusinessKey();
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(IL_REQUEST_PROCESS_NOTIFICATION, activityAnnotation.activityName()), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
            systemRecorder.recordCommand(SHMEvents.LICENSEREFRESH_REFRESH_NOTIFICATION, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, businessKey,
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, 100d);
        } catch (final JobDataNotFoundException jdnfEx) {
            LOGGER.error("Unable to trigger action. Reason :{} ", jdnfEx.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogs, businessKey, activityAnnotation.activityName());
        } catch (final Exception exception) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, exception.getMessage(), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogs, businessKey, activityAnnotation.activityName());
        }
        activityUtils.unSubscribeToMoNotifications(correlationId, activityJobId, jobActivityInfo);

        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityAnnotation.activityName(), processVariables);
        LOGGER.debug("LicenseRefreshJob:Refresh activity {} - processAVCNotification completed", activityJobId);
    }

    private void processNodeNotifications(final Notification notification, final JobActivityInfo jobActivityInfo, Map<String, Object> progressReportAttributes) {

        final long activityJobId = activityUtils.getActivityJobId(notification.getNotificationSubject());
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final String actionName = (String) progressReportAttributes.get("actionName");
        final Short progressPercentage = (Short) progressReportAttributes.get("progressPercentage");
        final String state = (String) progressReportAttributes.get("state");
        final String result = (String) progressReportAttributes.get("result");
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.PROGRESS_INFORMATION_WITH_RESULT, actionName, progressPercentage, state, result), new Date(),
                JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
        LOGGER.debug("LicenseRefreshJob:Refresh activity {} - process Node notifications attributes", progressReportAttributes);
        if (result.equalsIgnoreCase("FAILURE") || result.equalsIgnoreCase("SUCCESS") || result.equalsIgnoreCase("CANCELLED") || result.equalsIgnoreCase("FINISHED")) {
            activityUtils.unSubscribeToMoNotifications(notification.getDpsDataChangedEvent().getFdn(), activityJobId, jobActivityInfo);
        }
    }

    private boolean isShmNodeLicenseRefreshRequestDataPOsAvailable(final long neJobId) {

        boolean isLicenseRefreshRequestPosAvailable = false;

        List<PersistenceObject> licenseRefreshRequestPos = licenseRefreshServiceProvider.findShmNodeLicenseRefreshRequestDataPOs(neJobId);
        if (licenseRefreshRequestPos != null && !licenseRefreshRequestPos.isEmpty())
            isLicenseRefreshRequestPosAvailable = true;

        return isLicenseRefreshRequestPosAvailable;
    }

    private int performAction(final String instantaneousLicensingMOFdn) {
        return licenseRefreshServiceProvider.performMOAction(instantaneousLicensingMOFdn);
    }

    private String buildSubscriptionKey(final long neJobId, final int actionId) {
        return neJobId + LicenseRefreshConstants.SUBSCRIPTION_KEY_DELIMETER + actionId;

    }

}
