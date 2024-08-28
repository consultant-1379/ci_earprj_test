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
package com.ericsson.oss.services.shm.notifications.impl;

import java.util.concurrent.Semaphore;

import javax.ejb.Asynchronous;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAssociationCreatedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.GenericNotification;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;

@RunWith(MockitoJUnitRunner.class)
public class AbstractNotificationCallBackTest {

    @Mock
    private final Semaphore permit = new Semaphore(0);

    @InjectMocks
    AbstractNotificationCallBack abstractNotificationCallBack = new MockNotificationCallBack();

    @Mock
    JobActivityInfo jobActivityInfoMock;

    @Before
    public void setup() {
        abstractNotificationCallBack.setAcquireTimeToProcessNotification(0);
    }

    @Test
    public void testWaitForProcessNotifications() {
        abstractNotificationCallBack.result.setCompleted(true);
        abstractNotificationCallBack.waitForProcessNotifications();
    }

    @Test
    public void testProcessNotification() {
        mockJobActivityInfo();
        FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject("FDN", 123L, jobActivityInfoMock);
        DpsAttributeChangedEvent dataChangedEvent = new DpsAttributeChangedEvent();
        GenericNotification avcNotification = new GenericNotification(dataChangedEvent, fdnNotificationSubject, NotificationEventTypeEnum.AVC);
        abstractNotificationCallBack.processNotification(avcNotification);
    }

    @Test
    public void testProcessNotificationWithDifferentEvent() {
        mockJobActivityInfo();
        FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject("FDN", 123L, jobActivityInfoMock);
        DpsAssociationCreatedEvent dataChangedEvent = new DpsAssociationCreatedEvent();
        GenericNotification avcNotification = new GenericNotification(dataChangedEvent, fdnNotificationSubject, NotificationEventTypeEnum.AVC);
        abstractNotificationCallBack.processNotification(avcNotification);
    }

    private static class MockNotificationCallBack extends AbstractNotificationCallBack {

        /**
         * 
         */
        private static final long serialVersionUID = 3514811626724747009L;

        /*
         * (non-Javadoc)
         * 
         * @see com.ericsson.oss.services.shm.notifications.impl.AbstractNotificationCallBack#processPayLoad(com.ericsson.oss.services.shm.notifications .impl.FdnNotificationSubject,
         * com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent)
         */
        @Override
        @Asynchronous
        public void processPayLoad(final FdnNotificationSubject subject, final DpsAttributeChangedEvent event) {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            result.setSuccess(true);
        }

    }

    private void mockJobActivityInfo() {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.BACKUP);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
    }

}
