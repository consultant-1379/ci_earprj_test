/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.vran.software.upgrade.notification;

import javax.ejb.*;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.vran.software.upgrade.notification.VranNotificationJobProgressBean;

/**
 * Test class to support ShmJobProgressHandlerTest class test cases.
 */
@EServiceQualifier("vRAN.UPGRADE.prepare")
@ActivityInfo(activityName = "prepare", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.vRAN)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class VranTestMock implements Activity, ActivityCallback {

    @Override
    public void processNotification(final Notification message) {
        final VranNotificationJobProgressBean notification = (VranNotificationJobProgressBean) message;
        final VranSoftwareUpgradeJobResponse notificationMessage = notification.getVranNotification();
        final long activityJobId = notificationMessage.getActivityJobId();
        System.out.println("Activity JobId is : " + activityJobId);
        System.out.println("Inside Prepare Service Test class :");
    }

    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void execute(final long activityJobId) {
        // TODO Auto-generated method stub

    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        // TODO Auto-generated method stub
        return null;
    }

}