/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobEvents;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;

@RunWith(MockitoJUnitRunner.class)
public class ActivateActivityNotificationProcessorTest extends ProcessNotificationTestBase {

    @InjectMocks
    private ActivateActivityNotificationProcessor activateActivityNotificationProcessor;

    @Before
    public void mockJobEnvironment() {
        super.mockJobEnvironment();
        when(vranSoftwareUpgradeJobResponse.getActivityJobId()).thenReturn(activityJobId);
        when(vranSoftwareUpgradeJobResponse.getActivityName()).thenReturn(ActivityConstants.ACTIVATE);

    }

    @Test
    public void testProcessNotification_isActivateInitiationSuccessResponse_FlowTypeAction_ActivityCompleted() {
        when(vranSoftwareUpgradeJobResponse.getActivityName()).thenReturn(ActivityConstants.ACTIVATE);
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        when(vranSoftwareUpgradeJobResponse.getFlowType()).thenReturn(VranJobConstants.ACTION);
        when(notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(vranSoftwareUpgradeJobResponse.getState()).thenReturn(VranUprgradeConstants.WAITING_FOR_CONFIRM);
        when(activityStatusManager.isActivityCompleted(VranUprgradeConstants.ACTIVATE_OPERATION, VranUprgradeConstants.WAITING_FOR_CONFIRM)).thenReturn(true);
        activateActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationSuccessResponse_FlowTypeAction_ActivityInProgress() {
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        when(vranSoftwareUpgradeJobResponse.getFlowType()).thenReturn(VranJobConstants.ACTION);
        when(notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(vranSoftwareUpgradeJobResponse.getState()).thenReturn(VranUprgradeConstants.WAITING_FOR_CONFIRM);
        when(activityStatusManager.isActivityCompleted(VranUprgradeConstants.ACTIVATE_OPERATION, VranUprgradeConstants.WAITING_FOR_CONFIRM)).thenReturn(false);
        when(activityStatusManager.isActivityInProgress(ActivityConstants.ACTIVATE, VranUprgradeConstants.ACTIVATION_IN_PROGRESS)).thenReturn(true);

        activateActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationSuccessResponse_FlowTypeProgress() {
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        when(vranSoftwareUpgradeJobResponse.getFlowType()).thenReturn(VranJobConstants.PROGRESS);
        when(notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse)).thenReturn(false);
        when(notificationTaskUtils.isFlowTypeProgress(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(upgradePackageContext.getJobEnvironment()).thenReturn(jobEnvironment);
        activateActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationSuccessResponse_FlowTypeNone() {
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        when(vranSoftwareUpgradeJobResponse.getFlowType()).thenReturn("");
        when(notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse)).thenReturn(true);

        when(upgradePackageContext.getJobEnvironment()).thenReturn(jobEnvironment);
        activateActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationCancelResponse() {
        vranSoftwareUpgradeJobResponse.setResult(ShmConstants.CANCELLED);
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.CANCELLED);
        activateActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationNoResponse() {
        activateActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProceedWithNextSteps() {
        when(vranSoftwareUpgradeJobResponse.getAdditionalInfo()).thenReturn("To-VNF ID: 6504c040-4f8d-11e7-bdff-fa163e2546dd");
        when(upgradePackageContext.getJobEnvironment().getNeJobId()).thenReturn(786L);
        activateActivityNotificationProcessor.proceedWithNextSteps(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInfo, upgradePackageContext.getSoftwarePackageName(),
                VranUprgradeConstants.ACTIVATE_OPERATION);
    }

    @Test
    public void testGetActivityName() {
        activateActivityNotificationProcessor.getActivityName();
    }

    @Test
    public void testProcessActivityProgressNotification() {
        vranSoftwareUpgradeJobResponse.setState(ActivityConstants.ACTIVATE);
        activateActivityNotificationProcessor.processActivityProgressNotification(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInfo, VranUprgradeConstants.ACTIVATE_OPERATION);
    }

    @Test
    public void testTrackActivityStatusOnVnfm() {
        activateActivityNotificationProcessor.trackActivityStatusOnVnfm(vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testRecordNotification() {
        activateActivityNotificationProcessor.recordNotification(vranSoftwareUpgradeJobResponse, activityJobId, upgradePackageContext, VranJobEvents.ACTIVATE_PROCESS_NOTIFICATION);
    }

    @Test
    public void testGetNextProcessor() {
        activateActivityNotificationProcessor.getNextProcessor();
    }

    @Test
    public void testSetNextProcessor() {
        activateActivityNotificationProcessor.setNextProcessor(nextProcessor);
    }

}
