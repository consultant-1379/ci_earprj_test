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
package com.ericsson.oss.services.shm.vran.software.upgrade.notification;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.vran.model.response.VranUpgradeJobResponse;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.impl.ActivityServiceProvider;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;

/**
 * Handler to processes the software upgrade job response from shm notification queue and redirects to corresponding elementary services for further processing.
 */
public class VranSoftwareUpgradeJobProgressHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VranSoftwareUpgradeJobProgressHandler.class);

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry registry;

    @Inject
    private ActivityServiceProvider activityServiceProvider;

    /**
     * Reads the registry with the key and gets notification subject based on activity.
     * 
     * @param VranSoftwareUpgradeJobResponse
     *            vranUpgradeJobResponse
     * @param Date
     *            notificationReceivedDate
     * 
     */
    public void handleJobProgressResponse(final VranUpgradeJobResponse vranUpgradeJobResponse, final Date notificationReceivedDate) {
        try {
            if (vranUpgradeJobResponse == null) {
                LOGGER.info("VranUpgradeJobResponse from queue is null - {} ", vranUpgradeJobResponse);
                return;
            }
            LOGGER.debug("VRAN software upgrade mediation response received from queue is :{}", vranUpgradeJobResponse);
            final String key = getSubscriptionKey(vranUpgradeJobResponse);
            LOGGER.debug("Retrieving notification subject with key {} from registry {}", key, registry);
            final NotificationSubject notificationSubject = registry.getListener(key);
            if (notificationSubject == null) {
                LOGGER.info("No listeners for the event - {} ", vranUpgradeJobResponse);
                return;
            }
            LOGGER.debug("Sending VRAN software upgrade mediation response {} to {}", vranUpgradeJobResponse, notificationSubject);
            notificationSubject.setTimeStamp(notificationReceivedDate);
            final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse = transformSoftwareUpgradeJobResponse(vranUpgradeJobResponse, notificationSubject);
            notifyJob(vranSoftwareUpgradeJobResponse, notificationSubject);
        } catch (final Exception e) {
            LOGGER.error("Failed to handle the VRAN software upgrade mediation response. Reason : ", e);
        }
    }

    /**
     * @param vranUpgradeJobResponse
     * @return key
     */
    public String getSubscriptionKey(final VranUpgradeJobResponse vranUpgradeJobResponse) {
        LOGGER.trace("Building subscription key  using vranUpgradeJobResponse: {} ", vranUpgradeJobResponse);
        final String vnfId = vranUpgradeJobResponse.getVnfId();
        final String neName = vranUpgradeJobResponse.getNetworkElementName();
        final Map<String, Object> additionalAttributes = vranUpgradeJobResponse.getAdditionalAttributes();
        final long activityJobId = (long) additionalAttributes.get(ActivityConstants.ACTIVITY_JOB_ID);
        return vnfId + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + neName + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + activityJobId;
    }

    /**
     * @param vranUpgradeJobResponse
     * @return
     */
    private VranSoftwareUpgradeJobResponse transformSoftwareUpgradeJobResponse(final VranUpgradeJobResponse vranUpgradeJobResponse, final NotificationSubject subject) {
        final FdnNotificationSubject fdnNotificationSubject = (FdnNotificationSubject) subject;
        final String activityName = fdnNotificationSubject.getActivityName();
        final long activityJobId = Long.parseLong(fdnNotificationSubject.getObserverHandle().toString());
        final Date notificationTimeStamp = fdnNotificationSubject.getTimeStamp();
        final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse = new VranSoftwareUpgradeJobResponse();
        vranSoftwareUpgradeJobResponse.setOperation(vranUpgradeJobResponse.getActivityType());
        vranSoftwareUpgradeJobResponse.setAdditionalInfo(vranUpgradeJobResponse.getAdditionalInfo());
        vranSoftwareUpgradeJobResponse.setFallbackTimeout(vranUpgradeJobResponse.getFallbackTimeout());
        vranSoftwareUpgradeJobResponse.setFinishedTime(vranUpgradeJobResponse.getFinishedTime());
        vranSoftwareUpgradeJobResponse.setJobCreationTime(vranUpgradeJobResponse.getJobCreatedTime());
        vranSoftwareUpgradeJobResponse.setJobId(vranUpgradeJobResponse.getJobId());
        vranSoftwareUpgradeJobResponse.setProgressDetail(vranUpgradeJobResponse.getProgress());
        vranSoftwareUpgradeJobResponse.setProgressLevel(vranUpgradeJobResponse.getProgressLevel());
        vranSoftwareUpgradeJobResponse.setRequestedTime(vranUpgradeJobResponse.getRequestedTime());
        vranSoftwareUpgradeJobResponse.setResult(vranUpgradeJobResponse.getResult());
        vranSoftwareUpgradeJobResponse.setState(vranUpgradeJobResponse.getState());
        vranSoftwareUpgradeJobResponse.setVnfDescriptorId(vranUpgradeJobResponse.getVnfDescriptorId());
        vranSoftwareUpgradeJobResponse.setVnfId(vranUpgradeJobResponse.getVnfId());
        vranSoftwareUpgradeJobResponse.setVnfPackageId(vranUpgradeJobResponse.getVnfPackageId());
        vranSoftwareUpgradeJobResponse.setActivityName(activityName);
        vranSoftwareUpgradeJobResponse.setActivityJobId(activityJobId);
        vranSoftwareUpgradeJobResponse.setNotificationReceivedTime(notificationTimeStamp);
        vranSoftwareUpgradeJobResponse.setFlowType(vranUpgradeJobResponse.getFlowType());
        vranSoftwareUpgradeJobResponse.setNetworkElementName(vranUpgradeJobResponse.getNetworkElementName());
        vranSoftwareUpgradeJobResponse.setErrorMessage(vranUpgradeJobResponse.getErrorMessage());
        vranSoftwareUpgradeJobResponse.setErrorTime(vranUpgradeJobResponse.getErrorTime());
        vranSoftwareUpgradeJobResponse.setErrorCode(vranUpgradeJobResponse.getErrorCode());
        LOGGER.trace("Transformed mediation response is: {}", vranSoftwareUpgradeJobResponse);
        return vranSoftwareUpgradeJobResponse;
    }

    /**
     * Verifies the notification subject and redirects to the corresponding elementary services to process the notification.
     * 
     * @param VranSoftwareUpgradeJobResponse
     *            vranUpgradeJobResponse
     * @param NotificationSubject
     *            subject
     * 
     */
    protected void notifyJob(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final NotificationSubject subject) {
        try {
            LOGGER.trace("Identifying elementary service for response: {}", subject);
            final FdnNotificationSubject fdnNotificationSubject = (FdnNotificationSubject) subject;
            final String activityName = fdnNotificationSubject.getActivityName();
            final VranNotificationJobProgressBean notificationJobProgressImpl = new VranNotificationJobProgressBean();
            notificationJobProgressImpl.setVranNotification(vranSoftwareUpgradeJobResponse);
            final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(fdnNotificationSubject.getJobType());
            final PlatformTypeEnum platformType = PlatformTypeEnum.getPlatform(fdnNotificationSubject.getPlatform());
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
