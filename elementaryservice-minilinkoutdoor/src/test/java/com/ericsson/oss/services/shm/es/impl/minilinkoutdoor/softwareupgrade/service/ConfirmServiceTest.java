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
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.service;

import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.NETWORKELEMENT;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.DELIMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.MiniLinkOutdoorJobUtil;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.helper.SoftwareUpgradeJobService;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.tasks.HandleTimeoutTask;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.tasks.NotificationTask;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.tasks.PrecheckTask;
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class ConfirmServiceTest {

    @InjectMocks
    private ConfirmService confirmService;

    @Mock
    private HandleTimeoutTask handleTimeoutTask;

    @Mock
    private NotificationTask notificationTask;

    @Mock
    private PrecheckTask precheckTask;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Mock
    private NetworkElementData networkElementData;

    @Mock
    private EventSender<MediationTaskRequest> eventSender;

    @Mock
    private SHMCommonCallBackNotificationJobProgressBean notification;

    @Mock
    private SHMCommonCallbackNotification commonNotification;

    @Mock
    private FdnNotificationSubject fdnNotificationSubject;

    @Mock
    private SoftwareUpgradeJobService softwareUpgradeActivityService;

    @Mock
    private MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmServiceTest.class);

    private static final String EXCEPTION = "Exception caught :: {}";
    private static final String THIS_ACTIVITY = "confirm";
    private static final long ACTIVITY_JOB_ID = 123;
    private static final String NODE_NAME = "ML-TN";

    @Test
    public void testPrecheck() {
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, ConfirmService.class)).thenReturn(jobActivityInfo);
        when(jobActivityInfo.getActivityName()).thenReturn(THIS_ACTIVITY);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        when(precheckTask.activityPreCheck(ACTIVITY_JOB_ID, THIS_ACTIVITY)).thenReturn(activityStepResult);
        final ActivityStepResult result = confirmService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, result.getActivityResultEnum());
    }

    @Test
    public void testExecute() {
        try {
            when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, ConfirmService.class)).thenReturn(jobActivityInfo);
            when(jobActivityInfo.getActivityName()).thenReturn(THIS_ACTIVITY);
            when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
            when(neJobStaticData.getNodeName()).thenReturn(NODE_NAME);
            when(networkElementRetrivalBean.getNetworkElementData(NODE_NAME)).thenReturn(networkElementData);
            when(networkElementData.getNeFdn()).thenReturn("neFdn");
            final String subscriptionKey = DELIMETER + NETWORKELEMENT + NODE_NAME + DELIMETER + ActivityConstants.CONFIRM + DELIMETER;
            when(activityUtils.subscribeToMoNotifications(subscriptionKey, ACTIVITY_JOB_ID, jobActivityInfo)).thenReturn(fdnNotificationSubject);
            confirmService.execute(ACTIVITY_JOB_ID);
        } catch (JobDataNotFoundException | MoNotFoundException e) {
            LOGGER.info(EXCEPTION, e);
        }
    }

    @Test
    public void testProcessNotificationSuccess() {
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put("ActivityJobId", ACTIVITY_JOB_ID);
        when(notification.getCommonNotification()).thenReturn(commonNotification);
        when(commonNotification.getAdditionalAttributes()).thenReturn(additionalAttributes);
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, ConfirmService.class)).thenReturn(jobActivityInfo);
        confirmService.processNotification(notification);
    }

    @Test
    public void testHandleTimeout() {
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, ConfirmService.class)).thenReturn(jobActivityInfo);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        when(handleTimeoutTask.handleTimeout(jobActivityInfo)).thenReturn(activityStepResult);
        final ActivityStepResult result = confirmService.handleTimeout(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, result.getActivityResultEnum());
    }

    @Test
    public void testCancel() throws JobDataNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(NODE_NAME);
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, ConfirmService.class)).thenReturn(jobActivityInfo);
        when(jobActivityInfo.getActivityName()).thenReturn(THIS_ACTIVITY);
        when(miniLinkOutdoorJobUtil.getSubscriptionKey(NODE_NAME, THIS_ACTIVITY)).thenReturn("subscriptionKey");
        when(activityUtils.unSubscribeToMoNotifications("subscriptionKey", ACTIVITY_JOB_ID, jobActivityInfo)).thenReturn(true);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        when(activityUtils.getActivityStepResult(ActivityStepResultEnum.EXECUTION_FAILED)).thenReturn(activityStepResult);
        final ActivityStepResult result = confirmService.cancel(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, result.getActivityResultEnum());
    }

    @Test
    public void testCancelTimeout() {
        confirmService.cancelTimeout(ACTIVITY_JOB_ID, true);
    }
}
