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

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityServiceProvider;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.license.refresh.api.LkfImportResponse;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.model.notification.ShmElisLicenseRefreshNotification;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

public class LkfImportNotificationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LkfImportNotificationProcessor.class);

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    private ActivityServiceProvider activityServiceProvider;

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry registry;

    public void processElisNotification(final ShmElisLicenseRefreshNotification shmElisLicenseRefreshNotification) {

        try {
            if (shmElisLicenseRefreshNotification != null) {
                LOGGER.debug("ShmElisLicenseRefreshNotification received from ELIS queue is : {}", shmElisLicenseRefreshNotification);
                final String subscriptionKey = getSubscriptionKey(shmElisLicenseRefreshNotification);
                LOGGER.debug("LicenseRefresh: Request activity subscriptionKey from ShmElisLicenseRefreshNotification response is : {}", subscriptionKey);

                final NotificationSubject notificationSubject = registry.getListener(subscriptionKey);

                if (notificationSubject == null) {
                    LOGGER.error("No listeners for the event - {} ", shmElisLicenseRefreshNotification);
                    return;
                }
                notifyJob(shmElisLicenseRefreshNotification, notificationSubject);
            } else {
                LOGGER.error("shmElisLicenseRefreshNotification from queue is null");
            }
        } catch (final Exception e) {
            LOGGER.error("Failed to process ShmElisLicenseRefreshNotification received from ELIS due to {}", e.getMessage(), e);
        }

    }

    private void notifyJob(final ShmElisLicenseRefreshNotification shmElisLicenseRefreshNotification, final NotificationSubject subject) {
        try {
            LOGGER.trace("Identifying elementary service for ShmElisLicenseRefreshNotification : {}", shmElisLicenseRefreshNotification);

            final FdnNotificationSubject fdnNotificationSubject = (FdnNotificationSubject) subject;

            final String activityName = fdnNotificationSubject.getActivityName();
            final long activityJobId = Long.parseLong(fdnNotificationSubject.getObserverHandle().toString());
            final Date notificationReceivedTime = fdnNotificationSubject.getTimeStamp();

            final LkfImportResponse lkfImportResponse = transformElisMediationResponse(shmElisLicenseRefreshNotification, activityName, activityJobId, notificationReceivedTime);
            final LkfImportProcessNotificationImpl lkfImportProcessNotificationImpl = new LkfImportProcessNotificationImpl();
            lkfImportProcessNotificationImpl.setLkfImportResponse(lkfImportResponse);

            final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(fdnNotificationSubject.getJobType());
            final PlatformTypeEnum platformType = PlatformTypeEnum.getPlatform(fdnNotificationSubject.getPlatform());
            final ActivityCallback activityImpl = activityServiceProvider.getActivityNotificationHandler(platformType, jobTypeEnum, activityName);
            activityImpl.processNotification(lkfImportProcessNotificationImpl);

        } catch (final Exception e) {
            LOGGER.error("Failed identifying elementary service due to : {} ", e.getMessage(), e);
        }
    }

    private LkfImportResponse transformElisMediationResponse(final ShmElisLicenseRefreshNotification shmElisLicenseRefreshNotification, final String activityName, final long activityJobId,
            final Date notificationReceivedTime) {

        final LkfImportResponse lkfImportResponse = new LkfImportResponse();
        lkfImportResponse.setNotificationReceivedTime(notificationReceivedTime);
        lkfImportResponse.setActivityName(activityName);
        lkfImportResponse.setActivityJobId(activityJobId);
        lkfImportResponse.setAdditionalInfo(shmElisLicenseRefreshNotification.getAdditionalInfo());
        lkfImportResponse.setEventAttributes(shmElisLicenseRefreshNotification.getEventAttributes());
        lkfImportResponse.setFingerprint(shmElisLicenseRefreshNotification.getFingerPrint());
        lkfImportResponse.setNeJobId(shmElisLicenseRefreshNotification.getNeJobId());
        lkfImportResponse.setState(shmElisLicenseRefreshNotification.getState());
        lkfImportResponse.setStatus(shmElisLicenseRefreshNotification.getStatus());
        LOGGER.debug("Transformed ELIS mediation response is : {}", lkfImportResponse);
        return lkfImportResponse;
    }

    private String getSubscriptionKey(final ShmElisLicenseRefreshNotification shmElisLicenseRefreshNotification) {
        LOGGER.trace("Building subscription key  using shmElisLicenseRefreshNotification : {} ", shmElisLicenseRefreshNotification);
        final String fingerPrint = shmElisLicenseRefreshNotification.getFingerPrint();
        final String neJobId = shmElisLicenseRefreshNotification.getNeJobId();
        return fingerPrint + LicenseRefreshConstants.SUBSCRIPTION_KEY_DELIMETER + neJobId;
    }

}
