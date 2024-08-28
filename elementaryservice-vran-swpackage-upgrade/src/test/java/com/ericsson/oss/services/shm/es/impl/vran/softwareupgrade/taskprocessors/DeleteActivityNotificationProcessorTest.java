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

import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;

@RunWith(MockitoJUnitRunner.class)
public class DeleteActivityNotificationProcessorTest extends ProcessNotificationTestBase {

    @InjectMocks
    private DeleteActivityNotificationProcessor deleteActivityNotificationProcessor;

    @Before
    public void mockJobEnvironment() {
        super.mockJobEnvironment();
        when(vranSoftwareUpgradeJobResponse.getActivityJobId()).thenReturn(activityJobId);
        when(vranSoftwareUpgradeJobResponse.getActivityName()).thenReturn(VranUprgradeConstants.DELETE_ACTIVITY);

    }

    @Test
    public void testProcessNotification_DeleteIntiatedFromConfirm_AdditionalInfoExists() {
        when(vranSoftwareUpgradeJobResponse.getAdditionalInfo()).thenReturn("To-VNF ID: 6504c040-4f8d-11e7-bdff-fa163e2546dd");
        when(jobActivityInfo.getActivityName()).thenReturn(ActivityConstants.CONFIRM);
        deleteActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_DeleteIntiatedFromConfirm_AdditionalInfoNull() {
        when(vranSoftwareUpgradeJobResponse.getAdditionalInfo()).thenReturn(null);
        when(jobActivityInfo.getActivityName()).thenReturn(ActivityConstants.CONFIRM);
        deleteActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_DeleteIntiatedFromPrepare() {
        when(vranSoftwareUpgradeJobResponse.getAdditionalInfo()).thenReturn("To-VNF ID: 6504c040-4f8d-11e7-bdff-fa163e2546dd");
        when(jobActivityInfo.getActivityName()).thenReturn(ActivityConstants.PREPARE);
        deleteActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testGetActivityName() {
        deleteActivityNotificationProcessor.getActivityName();
    }

}
