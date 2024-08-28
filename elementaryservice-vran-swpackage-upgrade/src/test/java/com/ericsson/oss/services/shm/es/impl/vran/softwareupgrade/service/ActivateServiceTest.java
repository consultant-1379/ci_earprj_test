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

import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.ActivateActivityCancelProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.ActivateActivityExecuteProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.ActivateActivityNotificationProcessor;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;
import com.ericsson.oss.services.shm.vran.software.upgrade.notification.VranNotificationJobProgressBean;

@RunWith(MockitoJUnitRunner.class)
public class ActivateServiceTest extends BaseService {

    @InjectMocks
    private ActivateService activateService;

    @Mock
    private ActivateActivityExecuteProcessor activateActivityExecuteProcessor;

    @Mock
    private ActivateActivityNotificationProcessor activateActivityNotificationProcessor;

    @Mock
    private ActivateActivityCancelProcessor activateActivityCancelProcessor;

    @Test
    public void testPrecheck() {
        when(precheckTask.activityPreCheck(activityJobId, ActivityConstants.ACTIVATE)).thenReturn(activityStepResult);
        activateService.precheck(activityJobId);
    }

    @Test
    public void testExecute() {
        activateService.execute(activityJobId);
    }

    @Test
    public void testProcessNotification_ActivateActivity() {
        upgradePackageContext.setActionTriggered(ActivityConstants.ACTIVATE);
        when(taskBase.buildUpgradeJobContext(activityJobId)).thenReturn(upgradePackageContext);
        final VranNotificationJobProgressBean notificationBean = new VranNotificationJobProgressBean();
        notificationBean.setVranNotification(vranSoftwareUpgradeJobResponse);
        activateService.processNotification(notificationBean);
    }

    @Test
    public void testProcessNotification_DeleteActivity() {
        upgradePackageContext.setActionTriggered(VranUprgradeConstants.DELETE_ACTIVITY);
        when(taskBase.buildUpgradeJobContext(activityJobId)).thenReturn(upgradePackageContext);
        final VranNotificationJobProgressBean notificationBean = new VranNotificationJobProgressBean();
        notificationBean.setVranNotification(vranSoftwareUpgradeJobResponse);
        activateService.processNotification(notificationBean);
    }

    @Test
    public void testProcessNotification_JobCancelActivity() {
        upgradePackageContext.setActionTriggered(VranJobConstants.JOB_CANCEL);
        when(taskBase.buildUpgradeJobContext(activityJobId)).thenReturn(upgradePackageContext);
        final VranNotificationJobProgressBean notificationBean = new VranNotificationJobProgressBean();
        notificationBean.setVranNotification(vranSoftwareUpgradeJobResponse);
        activateService.processNotification(notificationBean);
    }

    @Test
    public void testHandleTimeout() {
        activateService.handleTimeout(activityJobId);
    }

    @Test
    public void testCancel() {
        activateService.cancel(activityJobId);
    }

    @Test
    public void testCancelTimeout() {
        activateService.cancelTimeout(activityJobId, true);
    }

}
