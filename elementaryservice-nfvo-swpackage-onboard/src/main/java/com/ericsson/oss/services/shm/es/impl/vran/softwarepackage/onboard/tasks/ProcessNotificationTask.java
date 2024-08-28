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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardPackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardSoftwarePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.persistence.OnboardJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class ProcessNotificationTask {

    @Inject
    private TasksBase taskBase;

    @Inject
    protected VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    private SuccessfulJobCreationNotificationProcessor successfulJobCreationNotificationProcessor;

    @Inject
    private JobStatusNotificationProcessor jobStatusNotificationProcessor;

    @Inject
    private OnboardJobPropertiesPersistenceProvider onboardJobPropertiesPersistenceProvider;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private OnboardPackageContextBuilder onboardPackageContextBuilder;

    public void processNotification(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final JobActivityInfo jobActivityInformation) {

        final long activityJobId = nfvoSoftwarePackageJobResponse.getActivityJobId();
        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo = onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext);

        if (isResponseReceivedForCreateJob(nfvoSoftwarePackageJobResponse)) {

            if (isCreationOfJobSuccessful(nfvoSoftwarePackageJobResponse)) {
                successfulJobCreationNotificationProcessor.processCreateJobResponse(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, jobActivityInformation,
                        jobContext.getNeJobId());
            } else {
                processActivityFailureResponse(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, jobActivityInformation);
            }

        } else if (isResponseReceivedForPollJob(nfvoSoftwarePackageJobResponse)) {

            if (isJobStillInProcessing(nfvoSoftwarePackageJobResponse)) {
                jobStatusNotificationProcessor.processJobInprogressResponse(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, jobActivityInformation);

            } else if (isJobSuccessful(nfvoSoftwarePackageJobResponse)) {
                jobStatusNotificationProcessor.processJobSuccessResponse(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, jobActivityInformation);
            } else {
                processActivityFailureResponse(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, jobActivityInformation);
            }

        } else {
            processActivityFailureResponse(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, jobActivityInformation);
        }

    }

    private void processActivityFailureResponse(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo,
            final JobActivityInfo jobActivityInformation) {

        final long activityJobId = nfvoSoftwarePackageJobResponse.getActivityJobId();
        final JobEnvironment jobContext = onboardSoftwarePackageContextForNfvo.getContext();

        onboardJobPropertiesPersistenceProvider.incrementOnboardFailedSoftwarePackagesCount(activityJobId, jobContext);

        taskBase.unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInformation);

        updateJobLogsForFailureResponse(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, activityJobId);

        taskBase.proceedWithNextStep(activityJobId);

    }

    private void updateJobLogsForFailureResponse(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo,
            final long activityJobId) {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final String jobLogMessage = String.format(VranJobLogMessageTemplate.ACTIVITY_FAILED_FOR_PACKAGE_WITH_REASON, VranJobConstants.ONBOARD,
                onboardSoftwarePackageContextForNfvo.getCurrentPackage(), nfvoSoftwarePackageJobResponse.getErrorMessage());
        jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString()));

        jobAttributesPersistenceProvider.persistJobLogs(activityJobId, jobLogs);
    }

    private boolean isJobSuccessful(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse) {
        return ShmConstants.SUCCESS.equalsIgnoreCase(nfvoSoftwarePackageJobResponse.getStatus());
    }

    private boolean isJobStillInProcessing(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse) {
        return VranJobConstants.PROCESSING_STATUS.equalsIgnoreCase(nfvoSoftwarePackageJobResponse.getStatus());
    }

    private boolean isCreationOfJobSuccessful(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse) {
        return nfvoSoftwarePackageJobResponse.getResult().equalsIgnoreCase(ShmConstants.SUCCESS);
    }

    private boolean isResponseReceivedForPollJob(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse) {
        return VranJobConstants.SW_PACKAGE_POLL_JOB.equalsIgnoreCase(nfvoSoftwarePackageJobResponse.getResponseType());
    }

    private boolean isResponseReceivedForCreateJob(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse) {
        return VranJobConstants.CREATE_SW_PACKAGE_ONBOARD_JOB.equalsIgnoreCase(nfvoSoftwarePackageJobResponse.getResponseType());
    }

}
