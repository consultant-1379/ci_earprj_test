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
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.VnfInformationProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;

@RunWith(MockitoJUnitRunner.class)
public class ConfirmActivityNotificationProcessorTest extends ProcessNotificationTestBase {

    @InjectMocks
    private ConfirmActivityNotificationProcessor confirmActivityNotificationProcessor;

    @Mock
    private VnfInformationProvider vnfInformationProvider;

    @Before
    public void mockJobEnvironment() {
        super.mockJobEnvironment();
        when(vranSoftwareUpgradeJobResponse.getActivityJobId()).thenReturn(activityJobId);
        when(vranSoftwareUpgradeJobResponse.getActivityName()).thenReturn(ActivityConstants.CONFIRM);

    }

    @Test
    public void testProcessNotification_isActivateInitiationSuccessResponse_FlowTypeAction_ActivityCompleted() {
        when(vranSoftwareUpgradeJobResponse.getActivityName()).thenReturn(ActivityConstants.CONFIRM);
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        when(vranSoftwareUpgradeJobResponse.getFlowType()).thenReturn(VranJobConstants.ACTION);
        when(notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(vranSoftwareUpgradeJobResponse.getState()).thenReturn(VranUprgradeConstants.CONFIRM_COMPLETED);
        when(activityStatusManager.isActivityCompleted(VranUprgradeConstants.CONFIRM_OPERATION, VranUprgradeConstants.CONFIRM_COMPLETED)).thenReturn(true);
        confirmActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationSuccessResponse_FlowTypeAction_ActivityInProgress() {
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        when(vranSoftwareUpgradeJobResponse.getFlowType()).thenReturn(VranJobConstants.ACTION);
        when(notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(vranSoftwareUpgradeJobResponse.getState()).thenReturn(VranUprgradeConstants.CONFIRM_COMPLETED);
        when(activityStatusManager.isActivityCompleted(VranUprgradeConstants.CONFIRM_OPERATION, VranUprgradeConstants.CONFIRM_COMPLETED)).thenReturn(false);
        when(activityStatusManager.isActivityInProgress(ActivityConstants.CONFIRM, VranUprgradeConstants.CONFIRM_IN_PROGRESS)).thenReturn(true);

        confirmActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationSuccessResponse_FlowTypeProgress() {
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        when(vranSoftwareUpgradeJobResponse.getFlowType()).thenReturn(VranJobConstants.PROGRESS);
        when(notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse)).thenReturn(false);
        when(notificationTaskUtils.isFlowTypeProgress(vranSoftwareUpgradeJobResponse)).thenReturn(true);
        when(upgradePackageContext.getJobEnvironment()).thenReturn(jobEnvironment);
        confirmActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationSuccessResponse_FlowTypeNone() {
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        when(vranSoftwareUpgradeJobResponse.getFlowType()).thenReturn("");
        when(notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse)).thenReturn(true);

        when(upgradePackageContext.getJobEnvironment()).thenReturn(jobEnvironment);
        confirmActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationCancelResponse() {
        vranSoftwareUpgradeJobResponse.setResult(ShmConstants.CANCELLED);
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.CANCELLED);
        confirmActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_isActivateInitiationNoResponse() {
        confirmActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test(expected = NullPointerException.class)
    public void testProceedWithNextSteps_whenVnFIsNull() {
        when(vnfInformationProvider.getVnfId(activityJobId, upgradePackageContext.getVnfId(), VranJobConstants.TO_VNF_ID, upgradePackageContext.getNodeName())).thenReturn(null);
        confirmActivityNotificationProcessor.proceedWithNextSteps(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInfo, upgradePackageContext.getSoftwarePackageName(),
                VranUprgradeConstants.CONFIRM_OPERATION);
    }

    @Test
    public void testProceedWithNextSteps_whenVnFNotNull() {
        when(vnfInformationProvider.getVnfId(activityJobId, upgradePackageContext.getVnfId(), VranJobConstants.TO_VNF_ID, upgradePackageContext.getNodeName())).thenReturn("123");
        confirmActivityNotificationProcessor.proceedWithNextSteps(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInfo, upgradePackageContext.getSoftwarePackageName(),
                VranUprgradeConstants.CONFIRM_OPERATION);
    }

    @Test
    public void testGetActivityName() {
        confirmActivityNotificationProcessor.getActivityName();
    }

}
