/*------------------------------------------------------------------------------
 ******************************************************************************** COPYRIGHT Ericsson 2016
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

import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.CreateActivityNotificationProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.PrepareActivityCancelProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.PrepareActivityExecuteProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.PrepareActivityNotificationProcessor;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;
import com.ericsson.oss.services.shm.vran.software.upgrade.notification.VranNotificationJobProgressBean;

@RunWith(MockitoJUnitRunner.class)
public class PrepareServiceTest extends BaseService {

    @InjectMocks
    private PrepareService prepareService;

    @Mock
    private PrepareActivityExecuteProcessor prepareActivityExecuteProcessor;

    @Mock
    private PrepareActivityNotificationProcessor prepareActivityNotificationProcessor;

    @Mock
    private CreateActivityNotificationProcessor createActivityNotificationProcessor;

    @Mock
    private PrepareActivityCancelProcessor prepareActivityCancelProcessor;

    @Test
    public void testPrecheck() {
        when(precheckTask.activityPreCheck(activityJobId, ActivityConstants.PREPARE)).thenReturn(activityStepResult);
        prepareService.precheck(activityJobId);
    }

    @Test
    public void testExecute() {
        prepareService.execute(activityJobId);
    }

    @Test
    public void testProcessNotification_DeleteActivity() {
        upgradePackageContext.setActionTriggered(VranUprgradeConstants.DELETE_ACTIVITY);
        when(taskBase.buildUpgradeJobContext(activityJobId)).thenReturn(upgradePackageContext);
        final VranNotificationJobProgressBean notificationBean = new VranNotificationJobProgressBean();
        notificationBean.setVranNotification(vranSoftwareUpgradeJobResponse);
        prepareService.processNotification(notificationBean);
    }

    @Test
    public void testProcessNotification_CreateActivity() {
        upgradePackageContext.setActionTriggered(ActivityConstants.CREATE);
        when(taskBase.buildUpgradeJobContext(activityJobId)).thenReturn(upgradePackageContext);
        final VranNotificationJobProgressBean notificationBean = new VranNotificationJobProgressBean();
        notificationBean.setVranNotification(vranSoftwareUpgradeJobResponse);
        prepareService.processNotification(notificationBean);
    }

    @Test
    public void testProcessNotification_PrepareActivity() {
        upgradePackageContext.setActionTriggered(ActivityConstants.PREPARE);
        when(taskBase.buildUpgradeJobContext(activityJobId)).thenReturn(upgradePackageContext);
        final VranNotificationJobProgressBean notificationBean = new VranNotificationJobProgressBean();
        notificationBean.setVranNotification(vranSoftwareUpgradeJobResponse);
        prepareService.processNotification(notificationBean);
    }

    @Test
    public void testProcessNotification_JobCancelActivity() {
        upgradePackageContext.setActionTriggered(VranJobConstants.JOB_CANCEL);
        when(taskBase.buildUpgradeJobContext(activityJobId)).thenReturn(upgradePackageContext);
        final VranNotificationJobProgressBean notificationBean = new VranNotificationJobProgressBean();
        notificationBean.setVranNotification(vranSoftwareUpgradeJobResponse);
        prepareService.processNotification(notificationBean);
    }

    @Test
    public void testHandleTimeout() {
        prepareService.handleTimeout(activityJobId);
    }

    @Test
    public void testCancel() {
        prepareService.cancel(activityJobId);
    }

    @Test
    public void testCancelTimeout() {
        prepareService.cancelTimeout(activityJobId, true);
    }

}
