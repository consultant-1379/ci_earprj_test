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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskutils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class NotificationTaskUtilsTest {

    @InjectMocks
    private NotificationTaskUtils notificationTaskUtils;

    @Mock
    private VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse;

    @Mock
    private UpgradePackageContext upgradePackageContext;

    static final long activityJobId = 345;

    @Test
    public void testAssignJobEventNameBasedOnActivity_Prepare() {
        notificationTaskUtils.assignJobEventNameBasedOnActivity(ActivityConstants.PREPARE);
    }

    @Test
    public void testAssignJobEventNameBasedOnActivity_Verify() {
        notificationTaskUtils.assignJobEventNameBasedOnActivity(ActivityConstants.VERIFY);
    }

    @Test
    public void testAssignJobEventNameBasedOnActivity_Activate() {
        notificationTaskUtils.assignJobEventNameBasedOnActivity(ActivityConstants.ACTIVATE);
    }

    @Test
    public void testAssignJobEventNameBasedOnActivity_Confirm() {
        notificationTaskUtils.assignJobEventNameBasedOnActivity(ActivityConstants.CONFIRM);
    }

    @Test
    public void testAssignServiceNameBasedOnActivity_Prepare() {
        notificationTaskUtils.assignServiceNameBasedOnActivity(ActivityConstants.PREPARE);
    }

    @Test
    public void testAssignServiceNameBasedOnActivity_Verify() {
        notificationTaskUtils.assignServiceNameBasedOnActivity(ActivityConstants.VERIFY);
    }

    @Test
    public void testAssignServiceNameBasedOnActivity_Confirm() {
        notificationTaskUtils.assignServiceNameBasedOnActivity(ActivityConstants.CONFIRM);
    }

    @Test
    public void testAssignServiceNameBasedOnActivity_Activate() {
        notificationTaskUtils.assignServiceNameBasedOnActivity(ActivityConstants.ACTIVATE);
    }

    @Test
    public void testIsJobSuccessResponse() {
        vranSoftwareUpgradeJobResponse.setResult(ShmConstants.SUCCESS);
        notificationTaskUtils.isJobSuccessResponse(vranSoftwareUpgradeJobResponse);
    }

    @Test
    public void testIsFlowTypeAction() {
        notificationTaskUtils.isFlowTypeAction(vranSoftwareUpgradeJobResponse);
    }

    @Test
    public void testIsFlowTypeProgress() {
        notificationTaskUtils.isFlowTypeProgress(vranSoftwareUpgradeJobResponse);
    }

    @Test
    public void testPrepareEventAttributes() {
        notificationTaskUtils.prepareEventAttributes(upgradePackageContext, activityJobId);
    }

}
