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
package com.ericsson.oss.services.shm.shared;

/**
 * If a class wants to listen for a notification it has to implement this, so that all implementations can be stopped at one time during an Upgrade
 * 
 * @author xrajeke
 * 
 */
public interface NotificationListener {

    /**
     * Stops Listening for Notifications, on a specified Queue.
     * 
     * @return - true, if the listener has stopped
     */
    boolean stopNotificationListener();

}
