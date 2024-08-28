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

import com.ericsson.oss.itpf.sdk.upgrade.UpgradeEvent;

/**
 * To monitor the processes, while system going for an Upgrade
 * 
 * 
 */
public interface UpgradeEventHandler {

    /**
     * This will stop listeners, which are listening for the notifications on the <code>shmNotificationQueue</code>channel and waits for finishing the on going processes, before the service instance
     * going for an Upgrade
     * 
     * @param event
     *            - An Upgrade Event to be Accepted
     */

    void verifyOngoingNotificationsAndAcceptUpgrade(UpgradeEvent event);
}
