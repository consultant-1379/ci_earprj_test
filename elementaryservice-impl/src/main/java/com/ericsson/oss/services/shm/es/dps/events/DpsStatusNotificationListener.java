/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.dps.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.*;
import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;

/**
 * This class continuously listens for DPS events.
 * 
 * @author xprapav
 * 
 */
public class DpsStatusNotificationListener implements EMessageListener<DpsConnectionEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DpsStatusNotificationListener.class);

    private final DpsStatusInfoProvider dpsStatusInfoProvider;

    private final SystemRecorder systemRecorder;

    public DpsStatusNotificationListener(final DpsStatusInfoProvider dpsStatusInfoProvider, final SystemRecorder systemRecorder) {
        this.dpsStatusInfoProvider = dpsStatusInfoProvider;
        this.systemRecorder = systemRecorder;
    }

    /*
     * This method will be called whenever DPS service release a notifications about database status.
     * 
     */
    @Override
    public void onMessage(final DpsConnectionEvent message) {
        if (message instanceof DpsConnectionLostEvent) {
            dpsStatusInfoProvider.isDatabaseAvailable(false);
            systemRecorder.recordEvent(SHMEvents.DPS_CONNECTIVITY_LOST, EventLevel.COARSE, "SHM", "DPS", message.toString());
        } else if (message instanceof DpsReConnectionFailedEvent) {
            dpsStatusInfoProvider.isDatabaseAvailable(false);
            systemRecorder.recordEvent(SHMEvents.DPS_RE_CONNECTION_FAILED, EventLevel.COARSE, "SHM", "DPS", message.toString());
        } else if (message instanceof DpsReConnectionSuccessfulEvent) {
            try {
                dpsStatusInfoProvider.isDatabaseAvailable(true);
            } catch (final Exception ex) {
                LOGGER.error("In DpsStatusNotificationListener : Exception occured while processing pollingEntries from cache:", ex);
            }
            systemRecorder.recordEvent(SHMEvents.DPS_RE_CONNECTION_SUCCESFUL, EventLevel.COARSE, "SHM", "DPS", message.toString());
        } else {
            LOGGER.error("In DpsStatusNotificationListener : Invalid DPS notification : {} ", message);
        }
    }
}
