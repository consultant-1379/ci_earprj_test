/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh.notification;

import java.io.Serializable;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.eventbus.Event;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.Consumes;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationHandler;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;

/**
 * Listens for AVC events from Dps during the License Refresh related MO actions.
 */
@ApplicationScoped
@Profiled
@Traceable
public class LicenseRefreshJobProgressQueueObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseRefreshJobProgressQueueObserver.class);

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry registry;

    @Inject
    private NotificationHandler notificationHandler;

    @Inject
    private ActivityUtils activityUtils;

    /**
     * Listens on a jms queue : shm-notification-queue. Filter on ShmNodeLicenseRefreshRequestData PO.
     */
    public void onEvent(@Observes @Consumes(endpoint = "jms://queue/shmNotificationQueue", filter = "type='ShmNodeLicenseRefreshRequestData'") final Event message) {
        LOGGER.debug("LicenseRefreshJob:Refresh activity - Notification Received through channel: {}", message);
        final Serializable payLoad = message.getPayload();
        if (!(payLoad instanceof DpsAttributeChangedEvent)) {
            LOGGER.debug("LicenseRefreshJob:Refresh activity - Discarded notification as its not of AVC type: {}", payLoad);
        } else {

            final DpsAttributeChangedEvent event = (DpsAttributeChangedEvent) payLoad;
            final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(event);
            final AttributeChangeData correlationIdAttributeChangeData = modifiedAttributes.get(LicenseRefreshConstants.CORRELATION_ID);

            if (correlationIdAttributeChangeData != null && correlationIdAttributeChangeData.getNewValue() != null) {
                final NotificationSubject notificationSubject = registry.getListener((String) correlationIdAttributeChangeData.getNewValue());
                notificationHandler.processNotification(event, notificationSubject);
            } else {
                LOGGER.debug("LicenseRefreshJob:Refresh activity - Discarded notification as CorrelationId not available for the modified PO having poId as [{}]", event.getPoId());

            }
        }

    }
}
