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

import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.VerifyActivityCancelProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.VerifyActivityExecuteProcessor;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors.VerifyActivityNotificationProcessor;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.software.upgrade.notification.VranNotificationJobProgressBean;

@RunWith(MockitoJUnitRunner.class)
public class VerifyServiceTest extends BaseService {

    @InjectMocks
    private VerifyService verifyService;

    @Mock
    private VerifyActivityExecuteProcessor verifyActivityExecuteProcessor;

    @Mock
    private VerifyActivityNotificationProcessor verifyActivityNotificationProcessor;

    @Mock
    private VerifyActivityCancelProcessor verifyActivityCancelProcessor;

    @Test
    public void testPrecheck() {
        when(precheckTask.activityPreCheck(activityJobId, ActivityConstants.VERIFY)).thenReturn(activityStepResult);
        verifyService.precheck(activityJobId);
    }

    @Test
    public void testExecute() {
        verifyService.execute(activityJobId);
    }

    @Test
    public void testProcessNotification_DeleteActivity() {
        upgradePackageContext.setActionTriggered(ActivityConstants.VERIFY);
        when(taskBase.buildUpgradeJobContext(activityJobId)).thenReturn(upgradePackageContext);
        final VranNotificationJobProgressBean notificationBean = new VranNotificationJobProgressBean();
        notificationBean.setVranNotification(vranSoftwareUpgradeJobResponse);
        verifyService.processNotification(notificationBean);
    }

    @Test
    public void testHandleTimeout() {
        verifyService.handleTimeout(activityJobId);
    }

    @Test
    public void testCancel() {
        verifyService.cancel(activityJobId);
    }

    @Test
    public void testCancelTimeout() {
        verifyService.cancelTimeout(activityJobId, true);
    }
}
