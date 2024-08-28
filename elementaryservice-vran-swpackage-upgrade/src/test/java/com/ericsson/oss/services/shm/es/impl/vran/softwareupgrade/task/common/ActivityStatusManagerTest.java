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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.task.common;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranUprgradeConstants;

@RunWith(MockitoJUnitRunner.class)
public class ActivityStatusManagerTest {
    @InjectMocks
    private ActivityStatusManager activityStatusManager;

    @Test
    public void testIsActivityCompleted_Create() {
        activityStatusManager.isActivityCompleted(ActivityConstants.CREATE, VranUprgradeConstants.INITIALIZED);
    }

    @Test
    public void testIsActivityInProgress() {
        activityStatusManager.isActivityInProgress(ActivityConstants.PREPARE, VranUprgradeConstants.PREPARE_IN_PROGRESS);
    }

    @Test
    public void testIsActivityInProgress_Verify() {
        activityStatusManager.isActivityInProgress(ActivityConstants.VERIFY, VranUprgradeConstants.VERIFY_IN_PROGRESS);
    }

    @Test
    public void testIsActivityInProgress_Confirm() {
        activityStatusManager.isActivityInProgress(ActivityConstants.CONFIRM, VranUprgradeConstants.CONFIRM_IN_PROGRESS);
    }

    @Test
    public void testIsActivityInProgress_Activate() {
        activityStatusManager.isActivityInProgress(ActivityConstants.ACTIVATE, VranUprgradeConstants.ACTIVATION_IN_PROGRESS);
    }

    @Test
    public void testAssignCompletedStateBasedOnOperation_Create() {

        activityStatusManager.assignCompletedStateBasedOnOperation(ActivityConstants.CREATE);
    }

    @Test
    public void testAssignCompletedStateBasedOnOperation_Prepare() {

        activityStatusManager.assignCompletedStateBasedOnOperation(ActivityConstants.PREPARE);
    }

    @Test
    public void testAssignCompletedStateBasedOnOperation_Verify() {

        activityStatusManager.assignCompletedStateBasedOnOperation(ActivityConstants.VERIFY);
    }

    @Test
    public void testAssignCompletedStateBasedOnOperation_Activate() {

        activityStatusManager.assignCompletedStateBasedOnOperation(ActivityConstants.ACTIVATE);
    }

    @Test
    public void testAssignCompletedStateBasedOnOperation_Confirm() {

        activityStatusManager.assignCompletedStateBasedOnOperation(ActivityConstants.CONFIRM);
    }

    @Test
    public void testAssignInProgressStateBasedOnOperation_Prepare() {

        activityStatusManager.assignInProgressStateBasedOnOperation(ActivityConstants.PREPARE);
    }

    @Test
    public void testAssignInProgressStateBasedOnOperation_Verify() {

        activityStatusManager.assignInProgressStateBasedOnOperation(ActivityConstants.VERIFY);
    }

    @Test
    public void testAssignInProgressStateBasedOnOperation_Activate() {

        activityStatusManager.assignInProgressStateBasedOnOperation(ActivityConstants.ACTIVATE);
    }

    @Test
    public void testAssignInProgressStateBasedOnOperation_Confirm() {

        activityStatusManager.assignInProgressStateBasedOnOperation(ActivityConstants.CONFIRM);
    }

}
