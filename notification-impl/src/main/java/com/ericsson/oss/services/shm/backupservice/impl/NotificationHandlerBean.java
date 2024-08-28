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
package com.ericsson.oss.services.shm.backupservice.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectCreatedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectDeletedEvent;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.GenericNotification;
import com.ericsson.oss.services.shm.es.api.JobsNotificationLoadCounter;
import com.ericsson.oss.services.shm.es.api.RemoteActivityCallBack;
import com.ericsson.oss.services.shm.es.impl.ActivityServiceProvider;
import com.ericsson.oss.services.shm.es.impl.RemoteActivityServiceProvider;
import com.ericsson.oss.services.shm.es.instrumentation.NotificationsInstruementation;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.NotificationHandler;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

/**
 * processes the notification;
 */
@ApplicationScoped
@Traceable
@Profiled
public class NotificationHandlerBean implements NotificationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationHandlerBean.class);

    @Inject
    private ActivityServiceProvider activityServiceProvider;

    @Inject
    private RemoteActivityServiceProvider remoteactivityServiceProvider;

    @Inject
    private JobsNotificationLoadCounter counterBean;

    @EServiceRef
    private NotificationsInstruementation instruementation;

    @Override
    public void processNotification(final DpsDataChangedEvent event, final NotificationSubject subject) {
        LOGGER.debug("Inside NotificationHandlerBean.processNotification() with Processing event: {}", event);
        counterBean.increment();

        if (event instanceof DpsAttributeChangedEvent) {
            try {
                final DpsAttributeChangedEvent dpsAttributeChangeEvent = (DpsAttributeChangedEvent) event;
                final NotificationType notificationType = subject.getNotificationType();
                switch (notificationType) {
                case JOB:
                    notifyJob(dpsAttributeChangeEvent, subject);
                    break;
                case SYNCHRONOUS_REQUEST:
                    notify(dpsAttributeChangeEvent, subject);
                    break;
                default:
                    throw new IllegalArgumentException("UnSupported type for DpsAttributeChangedEvent :" + notificationType.toString());
                }
            } catch (final Exception e) {
                LOGGER.error("DpsAttributeChangedEvent Notification processing failed due to :", e);
            }
        } else if (event instanceof DpsObjectCreatedEvent) {
            try {
                final DpsObjectCreatedEvent dpsObjectCreatedEvent = (DpsObjectCreatedEvent) event;
                final NotificationType notificationType = subject.getNotificationType();
                switch (notificationType) {
                case JOB:
                    notifyJob(dpsObjectCreatedEvent, subject);
                    break;
                default:
                    throw new IllegalArgumentException("UnSupported type for DpsObjectCreatedEvent :" + notificationType.toString());
                }
            } catch (final Exception e) {
                LOGGER.error("DpsObjectCreatedEvent Notification processing failed due to :", e);
            }
        } else if (event instanceof DpsObjectDeletedEvent) {
            try {
                final DpsObjectDeletedEvent dpsObjectDeletedEvent = (DpsObjectDeletedEvent) event;
                final NotificationType notificationType = subject.getNotificationType();
                switch (notificationType) {
                case JOB:
                    notifyJob(dpsObjectDeletedEvent, subject);
                    break;
                default:
                    throw new IllegalArgumentException("UnSupported type for DpsObjectDeletedEvent :" + notificationType.toString());
                }
            } catch (final Exception e) {
                LOGGER.error("DpsObjectDeletedEvent Notification processing failed due to :", e);
            }
        }
        counterBean.decrement();
    }

    private void notify(final DpsAttributeChangedEvent event, final NotificationSubject subject) {
        notifyActivity(event, subject);
    }

    private void notifyJob(final DpsDataChangedEvent event, final NotificationSubject subject) { //Changing only the parameter type to parent of DpsAttributeChangedEvent. i.e., DpsDataChangedEvent
        LOGGER.debug("NotificationHandlerBean - notifyJob() - event {}", event);
        final FdnNotificationSubject fdnNotificationSubject = (FdnNotificationSubject) subject;
        Notification message = null;
        final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(fdnNotificationSubject.getJobType());
        final PlatformTypeEnum platformType = PlatformTypeEnum.valueOf(fdnNotificationSubject.getPlatform());
        final String activityName = fdnNotificationSubject.getActivityName();

        //Create notification object based on the incoming DpsEvent.
        if (event instanceof DpsAttributeChangedEvent) {
            message = new GenericNotification(event, subject, NotificationEventTypeEnum.AVC);
        } else if (event instanceof DpsObjectCreatedEvent) {
            message = new GenericNotification(event, subject, NotificationEventTypeEnum.CREATE);
        } else if (event instanceof DpsObjectDeletedEvent) {
            message = new GenericNotification(event, subject, NotificationEventTypeEnum.DELETE);
        }

        final ActivityCallback activityImpl = getActivityImpl(activityName, jobTypeEnum, platformType);
        LOGGER.debug("The Service call = {}", activityImpl);
        if (activityImpl != null) {
            activityImpl.processNotification(message);
            if (subject.getTimeStamp() != null) {
                instruementation.capture(System.currentTimeMillis() - subject.getTimeStamp().getTime());
            }
        } else {
            LOGGER.error("Implementation class not found for interface RemoteActivityCallBack with activity name = {}, job type = {} and platform = {} ", activityName, jobTypeEnum, platformType);
        }

    }

    private void notifyActivity(final DpsAttributeChangedEvent event, final NotificationSubject subject) {

        final FdnNotificationSubject fdnNotificationSubject = (FdnNotificationSubject) subject;
        final Notification message = new GenericNotification(event, subject, NotificationEventTypeEnum.AVC);
        final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(fdnNotificationSubject.getJobType());
        final PlatformTypeEnum platformType = PlatformTypeEnum.valueOf(fdnNotificationSubject.getPlatform());
        final String activityName = fdnNotificationSubject.getActivityName();
        final RemoteActivityCallBack activityImpl = getRemoteActivityImpl(activityName, jobTypeEnum, platformType);
        LOGGER.debug("The Service call = {}", activityImpl);
        activityImpl.processNotification(message);
    }

    private ActivityCallback getActivityImpl(final String activityName, final JobTypeEnum jobType, final PlatformTypeEnum platform) {
        return activityServiceProvider.getActivityNotificationHandler(platform, jobType, activityName);
    }

    private RemoteActivityCallBack getRemoteActivityImpl(final String activityName, final JobTypeEnum jobType, final PlatformTypeEnum platform) {
        return remoteactivityServiceProvider.getActivityNotificationHandler(platform, jobType, activityName);
    }
}
