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
package com.ericsson.oss.services.shm.cpp.inventory.service.upgrade;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.upgrade.UpgradeEvent;
import com.ericsson.oss.itpf.sdk.upgrade.UpgradePhase;

@ApplicationScoped
public class CppInventorySynchEventListenerUpgradeEventObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(CppInventorySynchEventListenerUpgradeEventObserver.class);

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private UpgradeEventHandler upgradeEventHandler;

    public void upgradeNotificationObserver(@Observes final UpgradeEvent event) {
        final UpgradePhase phase = event.getPhase();
        final String logMessage = phase.toString();
        systemRecorder.recordEvent("Upgrade Notification Observer in CppInventorySynchEventListener", EventLevel.COARSE, "CppInventorySynchEventListener Upgrade",
                "CppInventorySynchEventListener Upgrade", logMessage + " Upgrade event is recieved For CppInventorySynchEventListener");
        switch (phase) {
        case SERVICE_INSTANCE_UPGRADE_PREPARE:
            LOGGER.debug("CppInventorySynchEventListener Instance is ready for Upgrade {}.", logMessage);
            if (event.isServiceRestartRequired()) {
                //Below Method has timer thread to wait for max time out value if applicable and then accepts the Upgrade event in it.
                //So sending the event as a parameter.
                LOGGER.info("Shm Services are needed to restart after upgrade. So going to stop all the notification listeners before accepting the event");
                upgradeEventHandler.verifyOngoingNotificationsAndAcceptUpgrade(event);
            } else {
                LOGGER.info("Services Restart is not required, after Upgrade. So the listeners will not be stopped.");
                event.accept("CppInventorySynchEventListener Instance is ready for Upgrade.");
                systemRecorder.recordEvent("Upgrade Notification Observer in CppInventorySynchEventListener", EventLevel.COARSE, "CppInventorySynchEventListener Upgrade",
                        "CppInventorySynchEventListener Upgrade", phase.toString() + "Accepted the Upgrade event, and services are no need to stop");
            }
            break;
        case SERVICE_CLUSTER_UPGRADE_PREPARE:
            LOGGER.debug("CppInventorySynchEventListener Cluster is ready for Upgrade {}.", logMessage);
            event.accept("CppInventorySynchEventListener Cluster is ready for upgrade.");
            break;
        case SERVICE_CLUSTER_UPGRADE_FAILED:
            LOGGER.error("CppInventorySynchEventListener Cluster upgrade Failed {}.", logMessage);
            event.accept("CppInventorySynchEventListener Cluster upgrade Failed.");
            break;
        case SERVICE_CLUSTER_UPGRADE_FINISHED_SUCCESSFULLY:
            LOGGER.debug("CppInventorySynchEventListener Cluster upgrade Success {}.", logMessage);
            event.accept("CppInventorySynchEventListener Cluster upgrade Success.");
            break;
        case SERVICE_INSTANCE_UPGRADE_FAILED:
            LOGGER.error("CppInventorySynchEventListener Instance upgrade Failed {}.", logMessage);
            event.accept("CppInventorySynchEventListener Instance upgrade Failed.");
            break;
        case SERVICE_INSTANCE_UPGRADE_FINISHED_SUCCESSFULLY:
            LOGGER.debug("CppInventorySynchEventListener Instance upgrade Success {}.", logMessage);
            event.accept("CppInventorySynchEventListener Instance upgrade Success.");
            break;
        default:
            LOGGER.error("Unexpected UpgradePhase {}", logMessage);
            event.accept("Unexpected UpgradePhase");
            break;
        }

    }

}
