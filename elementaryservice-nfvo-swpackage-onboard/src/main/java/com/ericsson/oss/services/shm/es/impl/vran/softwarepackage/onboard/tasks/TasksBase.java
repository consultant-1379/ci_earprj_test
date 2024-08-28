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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.MTRSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardPackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardSoftwarePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.persistence.OnboardJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.common.NfvoVnfPackageSyncMTRSender;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class TasksBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TasksBase.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private MTRSender onboardSoftwarepackageEventSender;

    @Inject
    private OnboardJobPropertiesPersistenceProvider onboardJobPropertiesPersistenceProvider;

    @Inject
    private OnboardPackageContextBuilder onboardPackageContextBuilder;

    @Inject
    private NfvoVnfPackageSyncMTRSender nfvoVnfPackageSyncMTRSender;

    public void proceedWithNextStep(final long activityJobId) {

        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> activityJobProperties = new ArrayList<>();
        boolean repeatExecute = false;
        String jobLogMessage = null;

        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo = onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext);

        if (!onboardSoftwarePackageContextForNfvo.isComplete()) {
            onboardJobPropertiesPersistenceProvider.incrementSoftwarePackageCurrentIndexToBeOnboarded(activityJobId, jobContext);
            repeatExecute = true;
        } else if (onboardSoftwarePackageContextForNfvo.isComplete()) {
            repeatExecute = false;
            jobLogMessage = updateJobLogMessage(onboardSoftwarePackageContextForNfvo.getTotalCount(), onboardSoftwarePackageContextForNfvo.getSuccessCount(),
                    onboardSoftwarePackageContextForNfvo.getNoOfFailures());
            jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString()));
            final int noOfFailures = onboardSoftwarePackageContextForNfvo.getNoOfFailures();
            markSoftwarePackageOnboardActivityResult(activityJobProperties, noOfFailures);
            sendNfvoVnfPackageSyncRequest(activityJobId, onboardSoftwarePackageContextForNfvo);
        }

        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, activityJobProperties, jobLogs);
        notifyWorkFlowService(activityJobId, repeatExecute, jobContext);
    }

    protected void handleOnboardPackageFailure(final long activityJobId, final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo, final String softwarePackageName,
            final String errorMessage) {

        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final String logMessage = String.format(VranJobLogMessageTemplate.ACTIVITY_FAILED_FOR_PACKAGE_WITH_REASON, VranJobConstants.ONBOARD, softwarePackageName, errorMessage);
        jobLogs.add(vranJobActivityService.buildJobLog(logMessage, JobLogLevel.ERROR.toString()));
        jobAttributesPersistenceProvider.persistJobLogs(activityJobId, jobLogs);

        onboardJobPropertiesPersistenceProvider.incrementOnboardFailedSoftwarePackagesCount(activityJobId, onboardSoftwarePackageContextForNfvo.getContext());
    }

    public void notifyWorkFlowService(final long activityJobId, final boolean repeatExecute, final JobEnvironment jobContext) {
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatExecute);
        activityUtils.sendNotificationToWFS(jobContext, activityJobId, VranJobConstants.ONBOARD, processVariables);
    }

    public void markSoftwarePackageOnboardActivityResult(final List<Map<String, Object>> jobProperties, final int noOfFailures) {
        if (noOfFailures > 0) {
            activityUtils.prepareJobPropertyList(jobProperties, ShmConstants.RESULT, JobResult.FAILED.toString());
        } else {
            activityUtils.prepareJobPropertyList(jobProperties, ShmConstants.RESULT, JobResult.SUCCESS.toString());
        }
    }

    public String updateJobLogMessage(final int totalPackageCount, final int onboardSuccessPackagesCount, final int onboardFailedPackagesCount) {
        String logMessage = null;
        logMessage = String.format(VranJobLogMessageTemplate.ONBOARD_SWPACKAGE_RESULT, totalPackageCount, onboardSuccessPackagesCount, onboardFailedPackagesCount);
        return logMessage;
    }

    protected void subscribeNotifications(final long activityJobId, final String nodeAddress, final JobActivityInfo jobActivityInformation) {

        final String subscriptionKey = buildSubscriptionKey(nodeAddress, activityJobId);
        LOGGER.debug("ActivityJob ID - [{}] : Subscription key for onboard job is : {}", activityJobId, subscriptionKey);

        activityUtils.subscribeToMoNotifications(subscriptionKey, activityJobId, jobActivityInformation);
    }

    protected String buildSubscriptionKey(final String nodeAddress, final long activityJobId) {

        return nodeAddress + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + activityJobId;
    }

    protected void unsubscribeNotifications(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final JobActivityInfo jobActivityInfo) {
        final String subscriptionKey = nfvoSoftwarePackageJobResponse.getNodeAddress() + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + jobActivityInfo.getActivityJobId();
        LOGGER.debug("ActivityJob ID - [{}] : Unsubscribing notification with key: {}", nfvoSoftwarePackageJobResponse.getActivityJobId(), subscriptionKey);
        activityUtils.unSubscribeToMoNotifications(subscriptionKey, nfvoSoftwarePackageJobResponse.getActivityJobId(), jobActivityInfo);
    }

    public void requestOnboardsoftwarePackageJobStatus(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo,
            final JobActivityInfo jobActivityInformation) {
        try {
            final Map<String, Object> eventAttributes = new HashMap<>();
            eventAttributes.put(ActivityConstants.ACTIVITY_JOB_ID, jobActivityInformation.getActivityJobId());
            final String subscriptionKey = onboardSoftwarePackageContextForNfvo.getNodeFdn() + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + nfvoSoftwarePackageJobResponse.getJobId();
            LOGGER.debug("ActivityJob ID - [{}] : Subscription key for software package onboard job status request is : {}", nfvoSoftwarePackageJobResponse.getActivityJobId(), subscriptionKey);
            subscribeNotifications(nfvoSoftwarePackageJobResponse.getActivityJobId(), onboardSoftwarePackageContextForNfvo.getNodeFdn(), jobActivityInformation);
            onboardSoftwarepackageEventSender.sendJobStatusRequest(onboardSoftwarePackageContextForNfvo.getNodeFdn(), nfvoSoftwarePackageJobResponse.getVnfPackageId(), nfvoSoftwarePackageJobResponse.getJobId(),eventAttributes);

        } catch (Exception e) {
            LOGGER.error("ActivityJob ID - [{}] : Failed to send software package onboard job status request. Reason : {} ", nfvoSoftwarePackageJobResponse.getActivityJobId(), e);
        }
    }

    public void sendNfvoVnfPackageSyncRequest(final long activityJobId, final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo) {
        if (!onboardSoftwarePackageContextForNfvo.areAllPackagesFailedToOnboard()) {
            LOGGER.debug("ActivityJob ID - [{}] : Sending NfvoVnfPackagesSyncRequest with FDN : {}", activityJobId, onboardSoftwarePackageContextForNfvo.getNodeFdn());
            nfvoVnfPackageSyncMTRSender.sendNfvoVnfPackagesSyncRequest(onboardSoftwarePackageContextForNfvo.getNodeFdn());
        }
    }

}
