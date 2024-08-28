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
package com.ericsson.oss.services.shm.notification.common;

import java.util.concurrent.Semaphore;

import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.notifications.api.NotificationCallbackResult;

public class NotificationInformation {

    final String nodeName;

    final Semaphore permit = new Semaphore(0);

    String failureMessage;

    NotificationCallbackResult notificationCallbackResult;

    private final EcimBackupInfo ecimBackupInfo;

    /**
     * @param notificationCallbackResult2
     * @param nodeName2
     * @param createBackup
     * @param string
     */

    public NotificationInformation(final NotificationCallbackResult notificationCallbackResult, final String nodeName,
                                   final EcimBackupInfo ecimBackupInfo) {
        this.notificationCallbackResult = notificationCallbackResult;
        this.nodeName = nodeName;
        this.ecimBackupInfo = ecimBackupInfo;
    }

    /**
     * @return the notificationCallbackResult
     */
    public NotificationCallbackResult getNotificationCallbackResult() {
        return notificationCallbackResult;
    }

    /**
     * @return the permit
     */
    public Semaphore getPermit() {
        return permit;
    }

    /**
     * @return the nodeName
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * @return the ecimBackupInfo
     */
    public EcimBackupInfo getEcimBackupInfo() {
        return ecimBackupInfo;
    }

    /**
     * @return the failureMessage
     */
    public String getFailureMessage() {
        return failureMessage;
    }

    /**
     * @return the failureMessage
     */
    public void setFailureMessage(final String failureMessage) {
        this.failureMessage += failureMessage;
    }

}
