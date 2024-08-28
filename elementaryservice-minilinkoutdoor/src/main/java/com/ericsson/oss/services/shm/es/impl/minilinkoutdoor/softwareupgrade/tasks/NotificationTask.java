/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.tasks;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ActivityProgressStateEnm;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ActivitySuccessStateEnm;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.MiniLinkOutdoorJobUtil;
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

public class NotificationTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationTask.class);

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    protected SystemRecorder systemRecorder;

    private static final String ACTIVITY_JOB_ID = "ActivityJobId";

    public void processRecivedNotification(final Notification message, final String activityName,
        final JobActivityInfo jobActivityInformation) {
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final SHMCommonCallBackNotificationJobProgressBean notification = (SHMCommonCallBackNotificationJobProgressBean) message;
        LOGGER.info("notification : {}", notification);
        LOGGER.info("Notification received from mediation for activate activity for {}", notification.getCommonNotification().getFdn());
        try {
            LOGGER.info("entered try block in notification task");
            final long activityJobId = (long) notification.getCommonNotification().getAdditionalAttributes().get(ACTIVITY_JOB_ID);
            LOGGER.info("activityJobId : {}; jobActivityInformation : {}", activityJobId, jobActivityInformation);
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final String nodeName = neJobStaticData.getNodeName();
            LOGGER.info("neJobStaticData : {}", neJobStaticData);
            if (notification.getCommonNotification() != null) {
                final String result = notification.getCommonNotification().getResult();
                LOGGER.info("result : {}", result);
                final String state = notification.getCommonNotification().getState();
                LOGGER.info("state : {}", state);
                final double progressPercent = Double.parseDouble(notification.getCommonNotification().getProgressPercentage().trim());
                LOGGER.info("progressPercent : {}", progressPercent);
                if (ActivitySuccessStateEnm.isContains(state)) {
                    activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
                    jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTIVITY_COMPLETED_THROUGH_NOTIFICATIONS, activityName), JobLogLevel.INFO.getLogLevel()));
                    activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobProperties, jobLogList, progressPercent);
                    activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                    activityUtils.unSubscribeToMoNotifications(miniLinkOutdoorJobUtil.getSubscriptionKey(nodeName, activityName), activityJobId, jobActivityInformation);
                    activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityName, Collections.<String, Object> emptyMap());
                    systemRecorder.recordCommand(SHMEvents.UPGRADE_PROCESS_NOTIFICATION, CommandPhase.FINISHED_WITH_SUCCESS, nodeName,
                            notification.getCommonNotification().getFdn(), activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.UPGRADE));
                } else if (ActivityProgressStateEnm.isContains(state)) {
                    LOGGER.debug("getActivityResult : {} , progressPercent : {}", result, progressPercent);
                    jobLogList.add(activityUtils.createNewLogEntry(String.format("\"%s\" activity is ongoing with progressPercent \"%s\".", activityName, progressPercent),
                            JobLogLevel.ERROR.getLogLevel()));
                    activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobProperties, jobLogList, progressPercent);
                    activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                    systemRecorder.recordCommand(SHMEvents.UPGRADE_PROCESS_NOTIFICATION, CommandPhase.ONGOING, nodeName,
                            notification.getCommonNotification().getFdn(), activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.UPGRADE));
                } else if (state.equalsIgnoreCase("RUNNING_NR")) {
                    jobLogList.add(activityUtils.createNewLogEntry(String.format("\"%s\" activity failed with status \"Tried to upgrade to already running NR.\"", activityName), JobLogLevel.ERROR.getLogLevel()));
                    activityUtils.unSubscribeToMoNotifications(miniLinkOutdoorJobUtil.getSubscriptionKey(nodeName, activityName), activityJobId, jobActivityInformation);
                    activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityName, null);
                    activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
                    activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobProperties, jobLogList, progressPercent);
                    activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                    systemRecorder.recordCommand(SHMEvents.UPGRADE_PROCESS_NOTIFICATION, CommandPhase.ONGOING, nodeName,
                            notification.getCommonNotification().getFdn(), activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.UPGRADE));
                    LOGGER.debug("ActivityJob ID - [{}] : Create  Job has been completed successfully. Notifying to workflow service.", activityJobId);
                } else {
                    jobLogList.add(activityUtils.createNewLogEntry(String.format("\"%s\" activity failed with state \"%s\".", activityName, state), JobLogLevel.ERROR.getLogLevel()));
                    activityUtils.unSubscribeToMoNotifications(miniLinkOutdoorJobUtil.getSubscriptionKey(nodeName, activityName), activityJobId, jobActivityInformation);
                    activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityName, null);
                    activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
                    activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, jobProperties, jobLogList, progressPercent);
                    activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobStaticData.getNeJobId());
                    systemRecorder.recordCommand(SHMEvents.UPGRADE_PROCESS_NOTIFICATION, CommandPhase.FINISHED_WITH_ERROR, nodeName,
                            notification.getCommonNotification().getFdn(), activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.UPGRADE));
                    LOGGER.debug("ActivityJob ID - [{}] : Create  Job has been completed successfully. Notifying to workflow service.", activityJobId);
                }
            }
        } catch (Exception e) {
            LOGGER.error("msg:{}", e);
        }
    }

}
