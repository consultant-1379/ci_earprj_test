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
package com.ericsson.oss.services.shm.instantaneouslicensing;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.services.shm.instantaneouslicensing.InstantaneousLicensingMOConstants.AdministrativeState;
import com.ericsson.oss.services.shm.instantaneouslicensing.InstantaneousLicensingMOConstants.Attributes;
import com.ericsson.oss.services.shm.notifications.api.NotificationReciever;

/**
 * An AVC Listener class which will continuously listen for InstantaneousLicensing AVC notifications and on notification receive start processing the notification
 * 
 * @author Team Royals
 *
 */
public class InstantaneousLicensingQueueListener implements EMessageListener<DpsDataChangedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstantaneousLicensingQueueListener.class);

    private InstantaneousLicensingMoMtrSender instantaneousLicensingMoMtrSender;

    private NotificationReciever notificationReciever;

    public InstantaneousLicensingQueueListener() {

    }

    public InstantaneousLicensingQueueListener(final InstantaneousLicensingMoMtrSender instantaneousLicensingMoMtrSender, final NotificationReciever notificationReciever) {
        this.instantaneousLicensingMoMtrSender = instantaneousLicensingMoMtrSender;
        this.notificationReciever = notificationReciever;
    }

    @Override
    public void onMessage(final DpsDataChangedEvent message) {
        LOGGER.info("DpsDataChange Event notification received for InstantaneousLicensingQueueListener : {}", message);
        if (!(message instanceof DpsAttributeChangedEvent)) {
            LOGGER.info("Discarded as its not an avc notification or expected data change notification");
            return;
        }

        final DpsAttributeChangedEvent dpsAttributeChangedEvent = (DpsAttributeChangedEvent) message;
        final Map<String, AttributeChangeData> modifiedAttributes = new HashMap<>();
        for (final AttributeChangeData avc : dpsAttributeChangedEvent.getChangedAttributes()) {
            modifiedAttributes.put(avc.getName(), avc);
        }
        LOGGER.info("modifiedAttributes : {}", modifiedAttributes);

        final AttributeChangeData administrativeStateChangeData = modifiedAttributes.get(Attributes.ADMINISTRATIVESTATE.getAttributeName());
        LOGGER.info("administrativeStateChangeData : {}", administrativeStateChangeData);
        if (administrativeStateChangeData != null) {
            try {
                if (administrativeStateChangeData.getNewValue().equals(AdministrativeState.UNLOCKED.getValue())) {
                    final Map<String, Object> instantaneousLicensingMOAttributes = instantaneousLicensingMoMtrSender.getInstantaneousLicensingMOAttributes(dpsAttributeChangedEvent.getFdn());
                    instantaneousLicensingMoMtrSender.sendMTR(instantaneousLicensingMOAttributes, dpsAttributeChangedEvent.getFdn());
                } else {
                    LOGGER.info("Discarded AVC notification as administrativeState is: {}", administrativeStateChangeData.getNewValue());
                }
            } catch (final Exception exception) {
                LOGGER.error("Unable to read and process AVC notification,Exception: {}", exception);
            }
        }

        final AttributeChangeData progressReportAttributeChangeData = modifiedAttributes.get(Attributes.PROGRESSREPORT.getAttributeName());
        LOGGER.info("progressReportAttributeChangeData : {}", progressReportAttributeChangeData);
        if (progressReportAttributeChangeData != null && progressReportAttributeChangeData.getNewValue() != null) {
            final Date notificationReceivedDate = new Date();
            notificationReciever.notify(message, notificationReceivedDate);
        }

    }

}
