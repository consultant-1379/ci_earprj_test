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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobEvents;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class ProcessNotificationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessNotificationTask.class);

    @Inject
    private JobStatusNotificationProcessor jobStatusNotificationProcessor;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private DeleteJobPropertiesPersistenceProvider deleteJobPropertiesPersistenceProvider;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    private TasksBase tasksBase;

    @Inject
    private DeletePackageContextBuilder deletePackageContextBuilder;

    /**
     * Method to process notification received from Nfvo for Software package delete request.
     *
     * @param nfvoSoftwarePackageJobResponse
     * @param jobActivityInformation
     */
    public void processNotification(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final JobActivityInfo jobActivityInformation) {

        final long activityJobId = nfvoSoftwarePackageJobResponse.getActivityJobId();
        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        final DeletePackageContextForNfvo deletePackageContextForNfvo = deletePackageContextBuilder.buildDeletePackageContextForNfvo(jobContext);

        LOGGER.debug("ActivityJob ID - [{}] : Contextual information built to process Nfvo notification is  : {}", activityJobId, deletePackageContextForNfvo);

        if (isResponseReceivedForCreateJob(nfvoSoftwarePackageJobResponse)) {

            if (isCreationOfJobSuccessful(nfvoSoftwarePackageJobResponse)) {
                jobStatusNotificationProcessor.processJobSuccessResponse(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo, jobActivityInformation);
                final boolean repeatExecute = tasksBase.proceedWithNextStepForNfvo(activityJobId);
                tasksBase.notifyWorkFlowService(activityJobId, repeatExecute, jobContext);
            } else {
                processCreateJobFailureResponse(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo, jobActivityInformation);
            }
        } else {
            processActivityFailureResponse(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo, jobActivityInformation);
        }
    }

    private void processCreateJobFailureResponse(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final DeletePackageContextForNfvo deletePackageContextForNfvo,
            final JobActivityInfo jobActivityInformation) {
        processActivityFailureResponse(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo, jobActivityInformation);
    }

    @SuppressWarnings("deprecation")
    private void processActivityFailureResponse(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final DeletePackageContextForNfvo deletePackageContextForNfvo,
            final JobActivityInfo jobActivityInformation) {

        LOGGER.debug("ActivityJob ID - [{}] : Processing job failure notification received from Nfvo. Response : {}", nfvoSoftwarePackageJobResponse.getActivityJobId(), nfvoSoftwarePackageJobResponse);

        final long activityJobId = nfvoSoftwarePackageJobResponse.getActivityJobId();
        final JobEnvironment jobContext = deletePackageContextForNfvo.getContext();

        deleteJobPropertiesPersistenceProvider.incrementFailedSoftwarePackageCountInNfvo(activityJobId, jobContext);
        deleteJobPropertiesPersistenceProvider.updateFailedSoftwarePackagesInNfvo(activityJobId, deletePackageContextForNfvo.getCurrentPackage(), jobContext);

        updateJobLogsForFailureResponse(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo, activityJobId);
        tasksBase.unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInformation);

        systemRecorder.recordCommand(VranJobEvents.DELETE_SOFTWARE_PACKAGE_NOTIFICATION, CommandPhase.FINISHED_WITH_ERROR, nfvoSoftwarePackageJobResponse.getNodeAddress(),
                deletePackageContextForNfvo.getCurrentPackage(),
                activityUtils.additionalInfoForCommand(nfvoSoftwarePackageJobResponse.getActivityJobId(), jobContext.getNeJobId(), JobTypeEnum.DELETE_SOFTWAREPACKAGE));

        final boolean repeatExecute = tasksBase.proceedWithNextStepForNfvo(activityJobId);
        tasksBase.notifyWorkFlowService(activityJobId, repeatExecute, jobContext);

    }

    private void updateJobLogsForFailureResponse(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final DeletePackageContextForNfvo deletePackageContextForNfvo,
            final long activityJobId) {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final String jobLogMessage = String.format(VranJobLogMessageTemplate.ACTIVITY_FAILED_FOR_PACKAGE_WITH_REASON, VranJobConstants.DEL_SW_ACTIVITY,
                deletePackageContextForNfvo.getCurrentPackage(), nfvoSoftwarePackageJobResponse.getErrorMessage());
        jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString()));

        jobAttributesPersistenceProvider.persistJobLogs(activityJobId, jobLogs);
    }

    private boolean isCreationOfJobSuccessful(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse) {
        return nfvoSoftwarePackageJobResponse.getResult().equalsIgnoreCase(ShmConstants.SUCCESS);
    }

    private boolean isResponseReceivedForCreateJob(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse) {
        return VranJobConstants.CREATE_SW_PACKAGE_DELETE_JOB.equalsIgnoreCase(nfvoSoftwarePackageJobResponse.getResponseType());
    }

}
