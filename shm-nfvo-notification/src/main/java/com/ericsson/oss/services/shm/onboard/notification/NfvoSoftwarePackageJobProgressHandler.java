/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.onboard.notification;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.vran.model.response.NfvoSwPackageMediationResponse;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.impl.ActivityServiceProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;

/**
 * This class used to redirect the response from shm notification queue to the corresponding elementary services for further processing.
 * 
 * @author xjhosye
 */
public class NfvoSoftwarePackageJobProgressHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NfvoSoftwarePackageJobProgressHandler.class);

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry registry;

    @Inject
    private ActivityServiceProvider activityServiceProvider;

    /**
     * Method to read the registry with the key and get notification subject based on activity.
     *
     * @param nfvoSwPackageMediationResponse
     * @param notificationReceivedDate
     *
     */
    protected void handleJobProgressResponse(final NfvoSwPackageMediationResponse nfvoSwPackageMediationResponse, final Date notificationReceivedDate) {
        try {
            if (nfvoSwPackageMediationResponse == null) {
                LOGGER.info("Nfvo software package mediation response received from queue is null - {} ", nfvoSwPackageMediationResponse);
                return;
            }
            LOGGER.debug("Nfvo software package mediation response received from queue is :{}", nfvoSwPackageMediationResponse);

            final String key = getSubscriptionKey(nfvoSwPackageMediationResponse);

            LOGGER.debug("Retrieving notification subject with key {} from registry {}", key, registry);
            final NotificationSubject notificationSubject = registry.getListener(key);
            if (notificationSubject == null) {
                LOGGER.info("No listeners for the event - {} ", nfvoSwPackageMediationResponse);
                return;
            }
            LOGGER.debug("Sending Nfvo software package mediation response {} to {}", nfvoSwPackageMediationResponse, notificationSubject);
            notificationSubject.setTimeStamp(notificationReceivedDate);

            notifyJob(nfvoSwPackageMediationResponse, notificationSubject);
        } catch (final Exception e) {
            LOGGER.error("Failed process Nfvo software package mediation response received due to {}", e.getMessage(), e);
        }
    }

    /**
     * @param nfvoSwPackageMediationResponse
     * @return key
     */
    private String getSubscriptionKey(final NfvoSwPackageMediationResponse nfvoSwPackageMediationResponse) {

        String key;
        final String nodeAddress = nfvoSwPackageMediationResponse.getNodeAddress();
        final Map<String, Object> additionalAttributes = nfvoSwPackageMediationResponse.getAdditionalAttributes();
        final String vnfPackageId = (String) additionalAttributes.get(VranJobConstants.VNF_PACKAGE_ID);
        final String responseType = nfvoSwPackageMediationResponse.getResponseType().trim();

        LOGGER.trace("Building subscription key using node address: {}, additionalAttributes : {}, vnf package id: {} and response type: {} ", nodeAddress, additionalAttributes, vnfPackageId,
                responseType);
        if (VranJobConstants.CREATE_SW_PACKAGE_DELETE_JOB.equalsIgnoreCase(responseType)) {
            key = nodeAddress + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + vnfPackageId;
        } else {
            final long activityJoId = (long) additionalAttributes.get(VranJobConstants.ACTIVITY_JOB_ID);
            key = nodeAddress + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + activityJoId;
        }
        return key;
    }

    /**
     * Method delegate the response to elementary services processNotification method based on the notification subject.
     *
     * @param nfvoSwPackageMediationResponse
     * @param subject
     *
     */
    private void notifyJob(final NfvoSwPackageMediationResponse nfvoSwPackageMediationResponse, final NotificationSubject subject) {
        try {
            LOGGER.trace("Identifying elementary service for response: {}", nfvoSwPackageMediationResponse);
            final FdnNotificationSubject fdnNotificationSubject = (FdnNotificationSubject) subject;
            final String activityName = fdnNotificationSubject.getActivityName();
            final long activityJobId = Long.parseLong(fdnNotificationSubject.getObserverHandle().toString());
            final Date notificationReceivedTime = fdnNotificationSubject.getTimeStamp();

            final NfvoSoftwarePackageJobResponseImpl nfvoSoftwarePackageJobResponseImpl = transformNfvoMediationResponse(nfvoSwPackageMediationResponse, activityName, activityJobId,
                    notificationReceivedTime);

            final NfvoSoftwarePackageJobNotificationWrapper notificationJobProgressImpl = new NfvoSoftwarePackageJobNotificationWrapper();
            notificationJobProgressImpl.setNfvoSoftwarePackageJobNotification(nfvoSoftwarePackageJobResponseImpl);
            final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(fdnNotificationSubject.getJobType());
            final PlatformTypeEnum platformType = PlatformTypeEnum.getPlatform(fdnNotificationSubject.getPlatform());
            final ActivityCallback activityImpl = getActivityImpl(activityName, jobTypeEnum, platformType);
            LOGGER.debug("Elementary serivce resolved to process notifications is: {}", activityImpl);
            activityImpl.processNotification(notificationJobProgressImpl);

        } catch (final Exception e) {
            LOGGER.error("Failed identifying elementary service due to :", e);
        }
    }

    /**
     * @param nfvoSwPackageMediationResponse
     * @param activityName
     * @param activityJobId
     * @param notificationReceivedTime
     */
    private NfvoSoftwarePackageJobResponseImpl transformNfvoMediationResponse(final NfvoSwPackageMediationResponse nfvoSwPackageMediationResponse, final String activityName, final long activityJobId,
            final Date notificationReceivedTime) {
        final NfvoSoftwarePackageJobResponseImpl nfvoSoftwarePackageJobResponseImpl = new NfvoSoftwarePackageJobResponseImpl();
        final Map<String, Object> additionalAttributes = nfvoSwPackageMediationResponse.getAdditionalAttributes();
        final String jobId = (String) additionalAttributes.get(VranJobConstants.NFVO_JOB_ID);
        final String vnfPackageId = (String) additionalAttributes.get(VranJobConstants.VNF_PACKAGE_ID);
        final String status = (String) additionalAttributes.get(VranJobConstants.NFVO_JOB_STATUS);
        final String description = (String) additionalAttributes.get(VranJobConstants.NFVO_JOB_STATUS_DESCRIPTION);
        final String fullFilePath = (String) additionalAttributes.get(VranJobConstants.FULL_FOLDER_PATH);

        nfvoSoftwarePackageJobResponseImpl.setNodeAddress(nfvoSwPackageMediationResponse.getNodeAddress());
        nfvoSoftwarePackageJobResponseImpl.setVnfPackageId(vnfPackageId);
        nfvoSoftwarePackageJobResponseImpl.setFullFilePath(fullFilePath);
        nfvoSoftwarePackageJobResponseImpl.setJobId(jobId);
        nfvoSoftwarePackageJobResponseImpl.setResponseType(nfvoSwPackageMediationResponse.getResponseType());
        nfvoSoftwarePackageJobResponseImpl.setStatus(status);
        nfvoSoftwarePackageJobResponseImpl.setActivityJobId(activityJobId);
        nfvoSoftwarePackageJobResponseImpl.setActivityName(activityName);
        nfvoSoftwarePackageJobResponseImpl.setNotificationTimeStamp(notificationReceivedTime);
        nfvoSoftwarePackageJobResponseImpl.setResult(nfvoSwPackageMediationResponse.getErrorCode() == 0 ? ShmConstants.SUCCESS : VranJobConstants.FAIL);
        nfvoSoftwarePackageJobResponseImpl.setErrorMessage(nfvoSwPackageMediationResponse.getErrorMessage());
        nfvoSoftwarePackageJobResponseImpl.setErrorCode(nfvoSwPackageMediationResponse.getErrorCode());
        nfvoSoftwarePackageJobResponseImpl.setDescription(description);

        LOGGER.trace("Transformed nfvo mediation response is : {}", nfvoSoftwarePackageJobResponseImpl);

        return nfvoSoftwarePackageJobResponseImpl;
    }

    protected ActivityCallback getActivityImpl(final String activityName, final JobTypeEnum jobType, final PlatformTypeEnum platform) {
        return activityServiceProvider.getActivityNotificationHandler(platform, jobType, activityName);
    }

}
