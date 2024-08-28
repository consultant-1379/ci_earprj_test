package com.ericsson.oss.services.shm.generic.notification;
/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/


import java.util.*;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.impl.ActivityServiceProvider;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

public class SHMCommonCallbackNotificationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SHMCommonCallbackNotificationHandler.class);

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry registry;

    @Inject
    private ActivityServiceProvider activityServiceProvider;
    
    private static final String MINILINKOUTDOOR = "MINI-LINK-Outdoor";
    private static final String MINI_LINK_OUTDOOR = "MINI_LINK_OUTDOOR";

    public void handleJobProgressResponse(final SHMCommonCallbackNotification shmCommonCallbackNotification, final Date notificationReceivedDate) {
        try {
            if (shmCommonCallbackNotification == null) {
                LOGGER.debug("SHMCommonCallbackNotificationHandler from queue is null - {} ", shmCommonCallbackNotification);
                return;
            }
            LOGGER.info("SHMCommonCallbackNotification mediation response received from queue is :{}", shmCommonCallbackNotification);
            final String key = shmCommonCallbackNotification.getFdn();
            LOGGER.debug("Retrieving notification subject with key {} from registry {}", key, registry);
            final NotificationSubject notificationSubject = registry.getListener(key);
            if (notificationSubject == null) {
                LOGGER.error("No listeners for the event - {} ", shmCommonCallbackNotification);
                return;
            }
            LOGGER.debug("Sending mediation response {} to {}", shmCommonCallbackNotification, notificationSubject);
            notificationSubject.setTimeStamp(notificationReceivedDate);
            notifyJob(transformSoftwareUpgradeJobResponse(shmCommonCallbackNotification, notificationSubject), notificationSubject);
            
        } catch (final Exception e) {
            LOGGER.error("Failed to handle the software upgrade mediation response. Reason : ", e);
        }
    }

    /**
     * @param stnUpgradeJobResponse
     * @return
     */
    private SHMCommonCallbackNotification transformSoftwareUpgradeJobResponse(final SHMCommonCallbackNotification callBackNotification, final NotificationSubject subject) {
    	
        final FdnNotificationSubject fdnNotificationSubject = (FdnNotificationSubject) subject;
        final String activityName = fdnNotificationSubject.getActivityName();
        final long activityJobId = Long.parseLong(fdnNotificationSubject.getObserverHandle().toString());
        final Date notificationTimeStamp = fdnNotificationSubject.getTimeStamp();
        Map<String, Object> attributes = callBackNotification.getAdditionalAttributes();
        if(attributes==null){
        	attributes = new HashMap<>();
        }	
        attributes.put("ActivityName", activityName);
        attributes.put("ActivityJobId", activityJobId);
        attributes.put("NotificationTimeStamp", notificationTimeStamp);
        
        callBackNotification.setAdditionalAttributes(attributes);
        return callBackNotification;
    }

    /**
     * Verifies the notification subject and redirects to the corresponding elementary services to process the notification.
     * 
     * @param SHMCommonCallbackNotification
     *            callBackNotification
     * @param NotificationSubject
     *            subject
     * 
     */
    protected void notifyJob(final SHMCommonCallbackNotification callBackNotification, final NotificationSubject subject) {
        try {
            String platform = "";
            LOGGER.trace("Identifying elementary service for response: {}", subject);
            final FdnNotificationSubject fdnNotificationSubject = (FdnNotificationSubject) subject;
            final String activityName = fdnNotificationSubject.getActivityName();
            final SHMCommonCallBackNotificationJobProgressBean notificationJobProgressImpl = new SHMCommonCallBackNotificationJobProgressBean();
            notificationJobProgressImpl.setCommonNotification(callBackNotification);
            final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(fdnNotificationSubject.getJobType());
            if(fdnNotificationSubject.getPlatform().equals(MINI_LINK_OUTDOOR)){
                platform = MINILINKOUTDOOR;
            }else{
                platform = fdnNotificationSubject.getPlatform();
            }
            final PlatformTypeEnum platformType = PlatformTypeEnum.getPlatform(platform);
            final ActivityCallback activityImpl = getActivityImpl(activityName, jobTypeEnum, platformType);
            LOGGER.debug("ElementaryService resolved to process notifications : is{}", activityImpl);
            activityImpl.processNotification(notificationJobProgressImpl);
        } catch (final Exception e) {
            LOGGER.error("Failed identifying elementary service due to :", e);
            throw e;
        }
    }

    public ActivityCallback getActivityImpl(final String activityName, final JobTypeEnum jobType, final PlatformTypeEnum platform) {
        return activityServiceProvider.getActivityNotificationHandler(platform, jobType, activityName);
    }

}
