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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.RemoteActivityCallBack;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationCallbackResult;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

/**
 * This class provides methods which allows us to subscribe and unsubscribe notifications for remote backup requests..
 * 
 * @author tcssagu
 * 
 */

@ApplicationScoped
public class RemoteActivityNotificationHelper {

    @Inject
    ActivityUtils activityUtils;

    @Inject
    @NotificationTypeQualifier(type = NotificationType.SYNCHRONOUS_REQUEST)
    NotificationRegistry notificationRegistry;

    @Inject
    RemoteRequestTimeoutListener remoteRequestTimeoutListener;

    private static final Logger logger = LoggerFactory.getLogger(RemoteActivityNotificationHelper.class);

    public static Map<String, NotificationInformation> notificationSubjectMap = new HashMap<String, NotificationInformation>();

    /**
     * @param notificationSubjectInformation
     *            the notificationSubjectInformation to set
     */
    public NotificationInformation getNotificationInformation(final String moFDN) {
        return notificationSubjectMap.get(moFDN);
    }

    public static void setNotificationSubjectInformation(final String moFdn, final NotificationInformation notificationInformation) {
        notificationSubjectMap.put(moFdn, notificationInformation);
    }

    /**
     * This method will wait for certain time Interval to process notification which are received on Topic.
     * 
     * @param uuid
     * @return
     */
    public NotificationCallbackResult waitForProcessNotifications(final String moFdn) {
        logger.debug("Inside waitForProcessNotifications");
        boolean acq = false;
        final NotificationInformation notificationInformation = notificationSubjectMap.get(moFdn);
        final NotificationCallbackResult result = notificationInformation.getNotificationCallbackResult();
        boolean complete = result.isCompleted();
        try {
            final Semaphore permit = notificationInformation.getPermit();
            while (!complete && (acq = permit.tryAcquire(remoteRequestTimeoutListener.getRemoteRequestTimeoutValue(), TimeUnit.SECONDS))) {
                complete = result.isCompleted();
            }
            if (!acq) {
                result.setMessage(ShmConstants.TIMEOUT);
                logger.debug("timeout while processing notifications!");
                return result;
            }
        } catch (InterruptedException e) {
            logger.error("interrupted processing of notifications", e);
            result.setMessage("processing notifications interrupted");
        }
        return result;

    }

    /**
     * @param moFDN
     */
    public static void removeNotificationSubjectInformation(final String moFDN) {
        notificationSubjectMap.remove(moFDN);

    }

    /**
     * This method is used only in RestoreBackupServiceImpl to subscribe for notifications along with EcimBackupInfo. This EcimBackupInfo is required
     * for Confirm Restore Precheck method.
     * 
     * @param nodeName
     * @param moFdn
     * @param className
     * @param backupInfo
     * @return {@link FdnNotificationSubject}
     */
    public FdnNotificationSubject subscribeToNotification(final String nodeName, final String moFdn,
                                                          final Class<? extends RemoteActivityCallBack> className, final EcimBackupInfo backupInfo) {
        logger.debug("Registering for Notification on the Node {} for execute action", nodeName);
        FdnNotificationSubject fdnNotificationSubject;
        final JobActivityInfo activityInfo = activityUtils.getRemoteActivityInfo(-1, className);
        final NotificationCallbackResult notificationCallBackResult = setNotificationInformation(moFdn, nodeName, backupInfo);
        fdnNotificationSubject = new FdnNotificationSubject(moFdn, activityInfo, notificationCallBackResult);
        notificationRegistry.register(fdnNotificationSubject);
        logger.debug("Registering for Notification is finished on the Node {}", nodeName);
        return fdnNotificationSubject;
    }

    /**
     * This method is used to subscribe for notifications in all other Ap related services except RestoreBackupServiceImpl
     * 
     * @param nodeName
     * @param moFdn
     * @param className
     * @return {@link FdnNotificationSubject}
     */
    public FdnNotificationSubject subscribeToNotification(final String nodeName, final String moFdn,
                                                          final Class<? extends RemoteActivityCallBack> className) {
        logger.debug("Registering for Notification on the Node {} for execute action", nodeName);
        FdnNotificationSubject fdnNotificationSubject;
        final JobActivityInfo activityInfo = activityUtils.getRemoteActivityInfo(-1, className);
        final NotificationCallbackResult notificationCallBackResult = setNotificationInformation(moFdn, nodeName, null);
        fdnNotificationSubject = new FdnNotificationSubject(moFdn, activityInfo, notificationCallBackResult);
        notificationRegistry.register(fdnNotificationSubject);
        logger.debug("Registering for Notification is finished on the Node {}", nodeName);
        return fdnNotificationSubject;
    }

    private NotificationCallbackResult setNotificationInformation(final String moFDN, final String nodeName, final EcimBackupInfo ecimBackupInfo) {
        final NotificationCallbackResult notificationCallbackResult = new NotificationCallbackResult();
        final NotificationInformation notificationInformation = new NotificationInformation(notificationCallbackResult, nodeName, ecimBackupInfo);
        setNotificationSubjectInformation(moFDN, notificationInformation);
        return notificationCallbackResult;
    }

    private void unSetNotificationInformation(final String moFDN) {
        removeNotificationSubjectInformation(moFDN);
    }

    public void unSubscribeToNotification(final FdnNotificationSubject notificationSubject, final String moFdn) {
        unSetNotificationInformation(moFdn);
        notificationRegistry.removeSubject(notificationSubject);
    }

}
