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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.service;

import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.ConfirmActivityCancelProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.ConfirmActivityExecuteProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.ConfirmActivityNotificationProcessor;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;
import com.ericsson.oss.services.shm.vran.software.upgrade.notification.VranNotificationJobProgressBean;

@RunWith(MockitoJUnitRunner.class)
public class ConfirmServiceTest extends BaseService {

    @InjectMocks
    private ConfirmService confirmService;

    @Mock
    private ConfirmActivityExecuteProcessor confirmActivityExecuteProcessor;

    @Mock
    private ConfirmActivityNotificationProcessor confirmActivityNotificationProcessor;

    @Mock
    private ConfirmActivityCancelProcessor confirmActivityCancelProcessor;

    @Test
    public void testPrecheck() {
        when(precheckTask.activityPreCheck(activityJobId, ActivityConstants.CONFIRM)).thenReturn(activityStepResult);
        confirmService.precheck(activityJobId);
    }

    @Test
    public void testExecute() {
        confirmService.execute(activityJobId);
    }

    @Test
    public void testProcessNotification_ConfirmActivity() {
        upgradePackageContext.setActionTriggered(ActivityConstants.CONFIRM);
        when(taskBase.buildUpgradeJobContext(activityJobId)).thenReturn(upgradePackageContext);
        final VranNotificationJobProgressBean notificationBean = new VranNotificationJobProgressBean();
        notificationBean.setVranNotification(vranSoftwareUpgradeJobResponse);
        confirmService.processNotification(notificationBean);
    }

    @Test
    public void testProcessNotification_DeleteActivity() {
        upgradePackageContext.setActionTriggered(VranUprgradeConstants.DELETE_ACTIVITY);
        when(taskBase.buildUpgradeJobContext(activityJobId)).thenReturn(upgradePackageContext);
        final VranNotificationJobProgressBean notificationBean = new VranNotificationJobProgressBean();
        notificationBean.setVranNotification(vranSoftwareUpgradeJobResponse);
        confirmService.processNotification(notificationBean);
    }

    @Test
    public void testProcessNotification_JobCancelActivity() {
        upgradePackageContext.setActionTriggered(VranJobConstants.JOB_CANCEL);
        when(taskBase.buildUpgradeJobContext(activityJobId)).thenReturn(upgradePackageContext);
        final VranNotificationJobProgressBean notificationBean = new VranNotificationJobProgressBean();
        notificationBean.setVranNotification(vranSoftwareUpgradeJobResponse);
        confirmService.processNotification(notificationBean);
    }

    @Test
    public void testHandleTimeout() {
        confirmService.handleTimeout(activityJobId);
    }

    @Test
    public void testCancel() {
        upgradePackageContext.setActionTriggered(ActivityConstants.CONFIRM);
        when(vranUpgradeJobContextBuilder.build(activityJobId)).thenReturn(upgradePackageContext);
        confirmService.cancel(activityJobId);
    }

    @Test
    public void testCancelTimeout() {
        confirmService.cancelTimeout(activityJobId, true);
    }

}
