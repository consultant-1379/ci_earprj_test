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
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;

@RunWith(MockitoJUnitRunner.class)
public class VerifyActivityNotificationProcessorTest extends ProcessNotificationTestBase {

    @InjectMocks
    private VerifyActivityNotificationProcessor verifyActivityNotificationProcessor;

    @Before
    public void mockJobEnvironment() {
        super.mockJobEnvironment();
        when(vranSoftwareUpgradeJobResponse.getActivityJobId()).thenReturn(activityJobId);
        when(vranSoftwareUpgradeJobResponse.getActivityName()).thenReturn(ActivityConstants.VERIFY);

    }

    @Test
    public void testProcessNotification_isActivateInitiationSuccessResponse_FlowTypeAction_ActivityCompleted() {
        when(vranSoftwareUpgradeJobResponse.getActivityName()).thenReturn(ActivityConstants.VERIFY);
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        when(vranSoftwareUpgradeJobResponse.getFlowType()).thenReturn(VranJobConstants.ACTION);
        when(notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(vranSoftwareUpgradeJobResponse.getState()).thenReturn(VranUprgradeConstants.PREPARE_COMPLETED);
        when(activityStatusManager.isActivityCompleted(VranUprgradeConstants.VERIFY_OPERATION, VranUprgradeConstants.PREPARE_COMPLETED)).thenReturn(true);
        verifyActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationSuccessResponse_FlowTypeAction_ActivityInProgress() {
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        when(vranSoftwareUpgradeJobResponse.getFlowType()).thenReturn(VranJobConstants.ACTION);
        when(notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(vranSoftwareUpgradeJobResponse.getState()).thenReturn(VranUprgradeConstants.PREPARE_COMPLETED);
        when(activityStatusManager.isActivityCompleted(VranUprgradeConstants.VERIFY_OPERATION, ActivityConstants.VERIFY)).thenReturn(false);
        when(activityStatusManager.isActivityInProgress(ActivityConstants.VERIFY, VranUprgradeConstants.VERIFY_IN_PROGRESS)).thenReturn(true);

        verifyActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationSuccessResponse_FlowTypeProgress() {
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        when(vranSoftwareUpgradeJobResponse.getFlowType()).thenReturn(VranJobConstants.PROGRESS);
        when(notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse)).thenReturn(false);
        when(notificationTaskUtils.isFlowTypeProgress(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(upgradePackageContext.getJobEnvironment()).thenReturn(jobEnvironment);
        verifyActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationSuccessResponse_FlowTypeNone() {
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        when(vranSoftwareUpgradeJobResponse.getFlowType()).thenReturn("");
        when(notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse)).thenReturn(true);

        when(upgradePackageContext.getJobEnvironment()).thenReturn(jobEnvironment);
        verifyActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationNoResponse() {
        verifyActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testGetActivityName() {
        verifyActivityNotificationProcessor.getActivityName();
    }

}
