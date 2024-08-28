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
package com.ericsson.oss.services.shm.webpush.notifications.impl;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectCreatedEvent;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.loadcontrol.local.api.SHMLoadControllerLocalService;
import com.ericsson.oss.services.shm.webpush.utils.WebPushServiceUtil;

@RunWith(MockitoJUnitRunner.class)
public class JobNotificationUtilTest {

    @Mock
    @Inject
    WebPushServiceUtil webPushServiceUtil;

    @Mock
    SHMLoadControllerLocalService shmLoadControllerService;

    @InjectMocks
    JobNotificationUtil jobNotificationUtil;

    long mainJobId = 1;
    long neJobId = 2;
    long activityJobId = 3;

    String JOB_KIND = "Job";
    String NE_JOB_KIND = "NEJob";
    String ACTIVITY_JOB_KIND = "ActivityJob";

    @Test
    public void testNotifyAsUpdateForJob() {
        final DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent();
        dpsAttributeChangedEvent.setPoId(mainJobId);
        dpsAttributeChangedEvent.setType(JOB_KIND);

        jobNotificationUtil.notifyAsUpdate(dpsAttributeChangedEvent);
        verify(webPushServiceUtil).prepareAndPushMainJob(mainJobId);
        verify(shmLoadControllerService, times(0)).deleteShmStageActivity(mainJobId);
    }


    @Test
    public void testNotifyAsUpdateForNEJob() {
        final DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent();
        final Set<AttributeChangeData> attributeChangeData = new HashSet();
        dpsAttributeChangedEvent.setPoId(neJobId);
        dpsAttributeChangedEvent.setType(NE_JOB_KIND);
        dpsAttributeChangedEvent.setChangedAttributes(attributeChangeData);

        jobNotificationUtil.notifyAsUpdate(dpsAttributeChangedEvent);
        verify(webPushServiceUtil).prepareAndPushNeJob(neJobId);
        verify(shmLoadControllerService, times(0)).deleteShmStageActivity(neJobId);
    }

    @Test
    public void testNotifyAsUpdateForActivityJob() {
        final DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent();
        final Set<AttributeChangeData> attributeChangeDataSet = new HashSet<AttributeChangeData>();
        final AttributeChangeData attributeChangeData = new AttributeChangeData();
        attributeChangeData.setName(ShmConstants.ACTIVITY_RESULT);
        attributeChangeDataSet.add(attributeChangeData);
        dpsAttributeChangedEvent.setPoId(activityJobId);
        dpsAttributeChangedEvent.setType(ACTIVITY_JOB_KIND);
        dpsAttributeChangedEvent.setChangedAttributes(attributeChangeDataSet);
        jobNotificationUtil.notifyAsUpdate(dpsAttributeChangedEvent);
        verify(webPushServiceUtil).prepareAndPushActivityJob(activityJobId, attributeChangeDataSet);
        verify(shmLoadControllerService).deleteShmStageActivity(activityJobId);
    }

    @Test
    public void testNotifyAsUpdateForUnexpectedType() {
        final DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent();
        dpsAttributeChangedEvent.setPoId(4L);
        dpsAttributeChangedEvent.setType("unexpected type");

        jobNotificationUtil.notifyAsUpdate(dpsAttributeChangedEvent);

        verify(webPushServiceUtil, times(0)).prepareAndPushCreateJobEvent(mainJobId);
        verify(webPushServiceUtil, times(0)).prepareAndPushCreateNeJobEvent(mainJobId);
    }

    @Test
    public void testNotifyAsCreate() {
        final DpsObjectCreatedEvent dpsObjectCreatedEvent = new DpsObjectCreatedEvent();
        dpsObjectCreatedEvent.setType(JOB_KIND);
        dpsObjectCreatedEvent.setPoId(mainJobId);

        jobNotificationUtil.notifyAsCreate(dpsObjectCreatedEvent);
        verify(webPushServiceUtil).prepareAndPushCreateJobEvent(mainJobId);
    }

    @Test
    public void testNotifyAsCreateDefault() {
        final DpsObjectCreatedEvent dpsObjectCreatedEvent = new DpsObjectCreatedEvent();
        dpsObjectCreatedEvent.setType("");
        dpsObjectCreatedEvent.setPoId(mainJobId);
        jobNotificationUtil.notifyAsCreate(dpsObjectCreatedEvent);

        verify(webPushServiceUtil, times(0)).prepareAndPushCreateJobEvent(mainJobId);
        verify(webPushServiceUtil, times(0)).prepareAndPushCreateNeJobEvent(mainJobId);
    }
}
