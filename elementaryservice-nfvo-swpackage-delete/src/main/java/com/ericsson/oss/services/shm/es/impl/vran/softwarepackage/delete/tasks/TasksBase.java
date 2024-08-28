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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForEnm;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.MTRSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.common.NfvoVnfPackageSyncMTRSender;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class TasksBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TasksBase.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private DeleteJobPropertiesPersistenceProvider deleteJobPropertiesPersistenceProvider;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    private MTRSender deleteSoftwarePackageEventSender;

    @Inject
    private DeletePackageContextBuilder deletePackageContextBuilder;

    @Inject
    private NfvoVnfPackageSyncMTRSender nfvoVnfPackageSyncMTRSender;

    public void proceedWithNextStep(final long activityJobId) {

        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> activityJobProperties = new ArrayList<>();
        boolean repeatExecute = false;
        String jobLogMessage = null;

        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        final DeletePackageContextForEnm deletePackageContextForEnm = deletePackageContextBuilder.buildDeletePackageContextForEnm(jobContext);
        final DeletePackageContextForNfvo deletePackageContextForNfvo = deletePackageContextBuilder.buildDeletePackageContextForNfvo(jobContext);

        LOGGER.debug("ActivityJob ID - [{}] : Contextual information built to process post deletion activity are, for Enm {} and for Nfvo {}", activityJobId, deletePackageContextForEnm,
                deletePackageContextForNfvo);

        if (!deletePackageContextForEnm.isComplete()) {
            deleteJobPropertiesPersistenceProvider.incrementSoftwarePackageCurrentIndexInEnm(activityJobId, jobContext);
            repeatExecute = true;
        } else if (deletePackageContextForEnm.isComplete() && deletePackageContextForNfvo.areThereAnyPackagesToBeDeleted()) {
            repeatExecute = true;
        } else {
            jobLogMessage = buildConsolidatedJobLogMessageForEnm(deletePackageContextForEnm.getTotalCount(), deletePackageContextForEnm.getSuccessCount(),
                    deletePackageContextForEnm.getNoOfFailures(), deletePackageContextForEnm.getFailedPackages());
            jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString()));
            final int noOfFailures = deletePackageContextForEnm.getNoOfFailures();
            markSoftwarePackageDeleteActivityResult(activityJobProperties, noOfFailures);
        }

        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, activityJobProperties, jobLogs);
        notifyWorkFlowService(activityJobId, repeatExecute, jobContext);
    }

    protected boolean proceedWithNextStepForNfvo(final long activityJobId) {

        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        boolean repeatExecute = false;
        String jobLogMessage = null;
        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        final DeletePackageContextForEnm deletePackageContextForEnm = deletePackageContextBuilder.buildDeletePackageContextForEnm(jobContext);
        final DeletePackageContextForNfvo deletePackageContextForNfvo = deletePackageContextBuilder.buildDeletePackageContextForNfvo(jobContext);

        LOGGER.debug("ActivityJob ID - [{}] : Contextual information built to process post deletion activity are, for Enm {} and for Nfvo {}", activityJobId, deletePackageContextForEnm,
                deletePackageContextForNfvo);
        if (deletePackageContextForNfvo.isComplete()) {

            if (deletePackageContextForEnm.getTotalCount() > 0 && deletePackageContextForEnm.isComplete()) {
                jobLogMessage = buildConsolidatedJobLogMessageForEnm(deletePackageContextForEnm.getTotalCount(), deletePackageContextForEnm.getSuccessCount(),
                        deletePackageContextForEnm.getNoOfFailures(), deletePackageContextForEnm.getFailedPackages());
                jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString()));
            }

            final int noOfFailures = deletePackageContextForNfvo.getNoOfFailures();
            jobLogMessage = buildConsolidatedJobLogMessageForNfvo(deletePackageContextForNfvo.getTotalCount(), deletePackageContextForNfvo.getSuccessCount(), noOfFailures,
                    deletePackageContextForNfvo.getFailedPackages());
            jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString()));
            markSoftwarePackageDeleteActivityResult(jobProperties, noOfFailures, deletePackageContextForEnm.getNoOfFailures());

            sendNfvoVnfPackagesSyncRequest(activityJobId, deletePackageContextForNfvo);
        } else {
            deleteJobPropertiesPersistenceProvider.incrementSoftwarePackageCurrentIndexInNfvo(activityJobId, jobContext);
            repeatExecute = true;
        }

        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, jobProperties, jobLogs);
        return repeatExecute;
    }

    protected void handleDeletePackageFailureFromNfvo(final long activityJobId, final DeletePackageContextForNfvo deletePackageContextForNfvo, final String softwarePackageName,
            final String errorMessage) {

        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final String logMessage = String.format(VranJobLogMessageTemplate.ACTIVITY_FAILED_FOR_PACKAGE_WITH_REASON, VranJobConstants.DEL_SW_ACTIVITY, softwarePackageName, errorMessage);
        jobLogs.add(vranJobActivityService.buildJobLog(logMessage, JobLogLevel.ERROR.toString()));
        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, null, jobLogs);

        deleteJobPropertiesPersistenceProvider.incrementFailedSoftwarePackageCountInNfvo(activityJobId, deletePackageContextForNfvo.getContext());
        deleteJobPropertiesPersistenceProvider.updateFailedSoftwarePackagesInNfvo(activityJobId, softwarePackageName, deletePackageContextForNfvo.getContext());
    }

    protected void notifyWorkFlowService(final long activityJobId, final boolean repeatExecute, final JobEnvironment jobContext) {
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatExecute);
        activityUtils.sendNotificationToWFS(jobContext, activityJobId, VranJobConstants.DEL_SW_ACTIVITY, processVariables);
    }

    protected void markSoftwarePackageDeleteActivityResult(final List<Map<String, Object>> jobProperties, final int noOfFailures) {
        if (noOfFailures > 0) {
            activityUtils.prepareJobPropertyList(jobProperties, ShmConstants.RESULT, JobResult.FAILED.toString());
        } else {
            activityUtils.prepareJobPropertyList(jobProperties, ShmConstants.RESULT, JobResult.SUCCESS.toString());
        }
    }

    protected void markSoftwarePackageDeleteActivityResult(final List<Map<String, Object>> jobProperties, final int noOfFailuresForNfvo, final int noOfFailuresForEnm) {
        if (noOfFailuresForNfvo > 0 || noOfFailuresForEnm > 0) {
            activityUtils.prepareJobPropertyList(jobProperties, ShmConstants.RESULT, JobResult.FAILED.toString());
        } else {
            activityUtils.prepareJobPropertyList(jobProperties, ShmConstants.RESULT, JobResult.SUCCESS.toString());
        }
    }

    /**
     * Method to build consolidated job log message for ENM delete software package job.
     * 
     * @param totalPackageCountInEnm
     * @param successEnmPackageCount
     * @param failedPackageCountInEnm
     * @param failedPackagesInEnm
     * 
     */
    protected String buildConsolidatedJobLogMessageForEnm(final int totalPackageCountInEnm, final int successEnmPackageCount, final int failedPackageCountInEnm, final String failedPackagesInEnm) {
        String logMessage = null;
        if (failedPackageCountInEnm > 0) {
            logMessage = String.format(VranJobLogMessageTemplate.DELETE_SWPACKAGENAMES_FROM_ENM_LOCATION, totalPackageCountInEnm, successEnmPackageCount, failedPackageCountInEnm,
                    failedPackagesInEnm != null ? failedPackagesInEnm : "");
        } else {
            logMessage = String.format(VranJobLogMessageTemplate.DELETE_PACKAGES_RESULT_FROM_ENM, totalPackageCountInEnm, successEnmPackageCount, failedPackageCountInEnm);
        }
        return logMessage;
    }

    /**
     * Method to build consolidated job log message for Nfvo delete software package job.
     * 
     * @param totalCount
     * @param successCount
     * @param failedCount
     * @param failedPackages
     * @return
     */
    protected String buildConsolidatedJobLogMessageForNfvo(final int totalCount, final int successCount, final int failedCount, final String failedPackages) {
        String logMessage = null;
        if (failedCount > 0) {
            logMessage = String.format(VranJobLogMessageTemplate.DELETE_SWPACKAGENAMES_FROM_NFVO_LOCATION, totalCount, successCount, failedCount, failedPackages != null ? failedPackages : "");
        } else {
            logMessage = String.format(VranJobLogMessageTemplate.DELETE_PACKAGES_RESULT_FROM_NFVO, totalCount, successCount, failedCount);
        }
        return logMessage;
    }

    protected void requestDeleteSoftwarePackageJobStatus(final long activityJobId, final String jobId, final DeletePackageContextForNfvo deletePackageContextForNfvo,
            final JobActivityInfo jobActivityInformation) {
        try {

            subscribeNotifications(activityJobId, jobId, deletePackageContextForNfvo.getNodeFdn(), null, jobActivityInformation);

            deleteSoftwarePackageEventSender.sendJobStatusRequest(deletePackageContextForNfvo.getNodeFdn(), deletePackageContextForNfvo.getCurrentPackage(), jobId);

            LOGGER.debug("ActivityJob ID - [{}] : Delete softwarepackage job status request is sent with DeleteSoftwarePackage Information : {} ", activityJobId, deletePackageContextForNfvo);
        } catch (Exception jobPollFailedException) {
            LOGGER.error("ActivityJob ID - [{}] :Failed to send delete software package job status request for package {}. Reason : {}", activityJobId,
                    deletePackageContextForNfvo.getCurrentPackage(), jobPollFailedException.getMessage(), jobPollFailedException);
            handleDeletePackageFailureFromNfvo(activityJobId, deletePackageContextForNfvo, deletePackageContextForNfvo.getCurrentPackage(), jobPollFailedException.getMessage());

            final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
            final boolean repeatExecute = proceedWithNextStepForNfvo(activityJobId);
            notifyWorkFlowService(activityJobId, repeatExecute, jobContext);
        }
    }

    protected void subscribeNotifications(final long activityJobId, final String jobId, final String nodeAddress, final String vnfPackageId, final JobActivityInfo jobActivityInformation) {

        String subscriptionKey = null;
        if (jobId != null) {
            subscriptionKey = buildSubscriptionKeyForPollJob(nodeAddress, jobId);
        } else {
            subscriptionKey = buildSubscriptionKeyForCreateJob(nodeAddress, vnfPackageId);
        }
        LOGGER.debug("ActivityJob ID - [{}] : Subscription key for delete job is : {}", activityJobId, subscriptionKey);

        activityUtils.subscribeToMoNotifications(subscriptionKey, activityJobId, jobActivityInformation);
    }

    protected void unsubscribeNotifications(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final JobActivityInfo jobActivityInfo) {

        String subscriptionKey = null;
        if (nfvoSoftwarePackageJobResponse.getJobId() != null) {
            subscriptionKey = buildSubscriptionKeyForPollJob(nfvoSoftwarePackageJobResponse.getNodeAddress(), nfvoSoftwarePackageJobResponse.getJobId());
        } else {
            subscriptionKey = buildSubscriptionKeyForCreateJob(nfvoSoftwarePackageJobResponse.getNodeAddress(), nfvoSoftwarePackageJobResponse.getVnfPackageId());
        }

        LOGGER.debug("ActivityJob ID - [{}] : Unsubscribing notification with key: {}", nfvoSoftwarePackageJobResponse.getActivityJobId(), subscriptionKey);

        activityUtils.unSubscribeToMoNotifications(subscriptionKey, nfvoSoftwarePackageJobResponse.getActivityJobId(), jobActivityInfo);

    }

    protected String buildSubscriptionKeyForCreateJob(final String nodeAddress, final String vnfPackageId) {

        return nodeAddress + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + vnfPackageId;
    }

    protected String buildSubscriptionKeyForPollJob(final String nodeAddress, final String jobId) {

        return nodeAddress + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + jobId;
    }

    public boolean isPackageAvailableBothInEnmAndNfvo(final String softwarePackageLocation) {
        return VranJobConstants.SMRS_NFVO.equalsIgnoreCase(softwarePackageLocation);
    }

    public void sendNfvoVnfPackagesSyncRequest(final long activityJobId, final DeletePackageContextForNfvo deletePackageContextForNfvo) {
        if (!deletePackageContextForNfvo.areAllPackagesFailedToDelete()) {
            LOGGER.debug("ActivityJob ID - [{}] : Sending NfvoVnfPackagesSyncRequest with FDN : {}", activityJobId, deletePackageContextForNfvo.getNodeFdn());
            nfvoVnfPackageSyncMTRSender.sendNfvoVnfPackagesSyncRequest(deletePackageContextForNfvo.getNodeFdn());
        }
    }

}
