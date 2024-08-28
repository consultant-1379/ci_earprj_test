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
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;

@RunWith(MockitoJUnitRunner.class)
public class JobCancelActivityNotificationProcessorTest extends ProcessNotificationTestBase {

    @InjectMocks
    private JobCancelActivityNotificationProcessor jobCancelActivityNotificationProcessor;

    @Before
    public void mockJobEnvironment() {
        super.mockJobEnvironment();
        when(vranSoftwareUpgradeJobResponse.getActivityJobId()).thenReturn(activityJobId);
        when(vranSoftwareUpgradeJobResponse.getActivityName()).thenReturn(VranJobConstants.JOB_CANCEL);

    }

    @Test
    public void testProcessNotification_ActivityCompleted() {
        when(vranSoftwareUpgradeJobResponse.getResult()).thenReturn(ShmConstants.CANCELLED);
        when(vranSoftwareUpgradeJobResponse.getState()).thenReturn(VranUprgradeConstants.PREPARE_COMPLETED);
        jobCancelActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProcessNotification_ActivityInProgress() {

        jobCancelActivityNotificationProcessor.processNotification(jobActivityInfo, vranSoftwareUpgradeJobResponse, upgradePackageContext);
    }

    @Test
    public void testProceedWithNextSteps() {
        jobCancelActivityNotificationProcessor.proceedWithNextSteps(vranSoftwareUpgradeJobResponse, upgradePackageContext, jobActivityInfo, upgradePackageContext.getNodeName(),
                VranJobConstants.JOB_CANCEL);
    }

    @Test
    public void testTrackActivityStatusOnVnfm() {
        jobCancelActivityNotificationProcessor.trackActivityStatusOnVnfm(vranSoftwareUpgradeJobResponse, upgradePackageContext);

    }

    @Test
    public void testGetActivityName() {
        jobCancelActivityNotificationProcessor.getActivityName();
    }

}
