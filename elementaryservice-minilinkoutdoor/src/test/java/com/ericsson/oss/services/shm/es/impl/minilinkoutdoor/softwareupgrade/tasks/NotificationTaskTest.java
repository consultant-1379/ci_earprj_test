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

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.MiniLinkOutdoorJobUtil;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants;
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(MockitoJUnitRunner.class)
public class NotificationTaskTest {

    @InjectMocks
    private NotificationTask notificationTask;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private SHMCommonCallBackNotificationJobProgressBean notification;

    @Mock
    private SHMCommonCallbackNotification commonNotification;

    @Mock
    private MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

    private static final long ACTIVITY_JOB_ID = 123;
    private static final String NODE_NAME = "nodeName";
    private static final String ACTIVATING = "ACTIVATING";
        private static final String DOWNLOADED = "DOWNLOADED";
        private static final String IDLE = "IDLE";
        private static final String COMPLETE = "COMPLETE";
        private static final String AWAITING_DOWNLOAD = "AWAITING_DOWNLOAD";
        private static final String SUBSCRIPTIONKEY = "subscriptionKey";

    @Test
    public void testProcessNotificationSuccess() throws JobDataNotFoundException {
        when(notification.getCommonNotification()).thenReturn(commonNotification);
        when(commonNotification.getFdn()).thenReturn("fdn");
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, ACTIVITY_JOB_ID);
        when(commonNotification.getAdditionalAttributes()).thenReturn(additionalAttributes);
        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(NODE_NAME);
        when(miniLinkOutdoorJobUtil.getSubscriptionKey(NODE_NAME, ActivityConstants.UPGRADE)).thenReturn(SUBSCRIPTIONKEY);
        when(activityUtils.unSubscribeToMoNotifications(SUBSCRIPTIONKEY, ACTIVITY_JOB_ID, jobActivityInfo)).thenReturn(true);
        when(commonNotification.getState()).thenReturn(COMPLETE);
        when(commonNotification.getProgressPercentage()).thenReturn("100");
        notificationTask.processRecivedNotification(notification, ActivityConstants.UPGRADE, jobActivityInfo);
        when(commonNotification.getState()).thenReturn(AWAITING_DOWNLOAD);
        notificationTask.processRecivedNotification(notification, ActivityConstants.UPGRADE, jobActivityInfo);
    }

    @Test
    public void testProcessNotificationUploading() throws JobDataNotFoundException {
        when(notification.getCommonNotification()).thenReturn(commonNotification);
        when(commonNotification.getFdn()).thenReturn("fdn");
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, ACTIVITY_JOB_ID);
        when(commonNotification.getAdditionalAttributes()).thenReturn(additionalAttributes);
        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(NODE_NAME);
        when(miniLinkOutdoorJobUtil.getSubscriptionKey(NODE_NAME, ActivityConstants.UPGRADE)).thenReturn(SUBSCRIPTIONKEY);
        when(activityUtils.unSubscribeToMoNotifications(SUBSCRIPTIONKEY, ACTIVITY_JOB_ID, jobActivityInfo)).thenReturn(true);
        when(commonNotification.getState()).thenReturn(DOWNLOADED);
        when(commonNotification.getProgressPercentage()).thenReturn("0");
        notificationTask.processRecivedNotification(notification, ActivityConstants.UPGRADE, jobActivityInfo);
        when(commonNotification.getState()).thenReturn(ACTIVATING);
        notificationTask.processRecivedNotification(notification, ActivityConstants.UPGRADE, jobActivityInfo);
        when(commonNotification.getState()).thenReturn(IDLE);
        notificationTask.processRecivedNotification(notification, ActivityConstants.UPGRADE, jobActivityInfo);
    }

    @Test
    public void testProcessNotificationActivityResultFailed() throws JobDataNotFoundException {
        when(notification.getCommonNotification()).thenReturn(commonNotification);
        when(commonNotification.getFdn()).thenReturn("fdn");
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, ACTIVITY_JOB_ID);
        when(commonNotification.getAdditionalAttributes()).thenReturn(additionalAttributes);
        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(NODE_NAME);
        when(miniLinkOutdoorJobUtil.getSubscriptionKey(NODE_NAME, ActivityConstants.UPGRADE)).thenReturn(SUBSCRIPTIONKEY);
        when(activityUtils.unSubscribeToMoNotifications(SUBSCRIPTIONKEY, ACTIVITY_JOB_ID, jobActivityInfo)).thenReturn(true);
        when(commonNotification.getState()).thenReturn("");
        when(commonNotification.getProgressPercentage()).thenReturn("0");
        notificationTask.processRecivedNotification(notification, ActivityConstants.UPGRADE, jobActivityInfo);
    }
}
