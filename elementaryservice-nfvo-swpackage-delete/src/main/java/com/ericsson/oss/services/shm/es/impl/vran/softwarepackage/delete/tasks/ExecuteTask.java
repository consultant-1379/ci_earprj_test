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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.resource.ResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.DeleteSoftwarePackageService;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForEnm;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.MTRSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.SmrsFileDeleteService;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteSoftwarePackagePersistenceProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@Stateless
public class ExecuteTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteTask.class);

    @Inject
    private SmrsFileDeleteService deleteSoftwarePackageSMRSService;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private DeleteSoftwarePackagePersistenceProvider deleteSoftwarePackagePersistenceProvider;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    @Inject
    private DeleteJobPropertiesPersistenceProvider deleteJobPropertiesPersistenceProvider;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    private TasksBase tasksBase;

    @Inject
    private MTRSender deleteSoftwarePackageEventSender;

    public void deleteSoftwarePackageFromNfvo(final long activityJobId, final DeletePackageContextForNfvo deletePackageContextForNfvo) {

        final String softwarePackageName = deletePackageContextForNfvo.getCurrentPackage();
        final String nfvoFdn = deletePackageContextForNfvo.getNodeFdn();
        LOGGER.debug("ActivityJob ID - [{}] : Deleting software package {} from NFVO location", activityJobId, softwarePackageName);
        try {
            final String vnfPackageId = vnfSoftwarePackagePersistenceProvider.getVnfPackageId(softwarePackageName, nfvoFdn);
            requestNfvoToDeleteSoftwarePackage(activityJobId, softwarePackageName, vnfPackageId, nfvoFdn, activityUtils.getActivityInfo(activityJobId, DeleteSoftwarePackageService.class));

            LOGGER.debug("ActivityJob ID - [{}] : software package delete action from NFVO has been triggered successfully", activityJobId);
        } catch (Exception packageDeletionFailedException) {
            LOGGER.error("ActivityJob ID - [{}] :Failed to delete software package {}. Reason : {}", activityJobId, softwarePackageName, packageDeletionFailedException.getMessage(),
                    packageDeletionFailedException);
            tasksBase.handleDeletePackageFailureFromNfvo(activityJobId, deletePackageContextForNfvo, softwarePackageName, packageDeletionFailedException.getMessage());
            final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
            final boolean repeatExecute = tasksBase.proceedWithNextStepForNfvo(activityJobId);
            tasksBase.notifyWorkFlowService(activityJobId, repeatExecute, jobContext);
        }
    }

    public void deleteSoftwarePackageFromEnm(final long activityJobId, final DeletePackageContextForEnm deletePackageContextForEnm) {

        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        String softwarePackageName = null;
        String softwarePackageLocation = null;
        String jobLogMessage = null;

        LOGGER.debug("ActivityJob ID - [{}] : Contextual information built to delete Enm package is {}", activityJobId, deletePackageContextForEnm);

        softwarePackageName = deletePackageContextForEnm.getCurrentPackage();
        final boolean softwarePackageInUse = deleteSoftwarePackagePersistenceProvider.isSoftwarePackageInUse(softwarePackageName);

        if (softwarePackageInUse) {
            jobLogMessage = String.format(VranJobLogMessageTemplate.SWPACKAGE_IN_USE, softwarePackageName);
            jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.INFO.toString()));
            handleDeletePackageFailure(activityJobId, deletePackageContextForEnm);
        } else {
            final PersistenceObject vnfSoftwarePackageEntity = vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity(softwarePackageName);
            if (vnfSoftwarePackageEntity != null) {
                try {
                    softwarePackageLocation = vnfSoftwarePackageEntity.getAttribute(VranJobConstants.SW_PACKAGE_LOCATION);

                    if (isPackageAvailableOnlyInEnm(softwarePackageLocation)) {

                        processDeleteForPackageAvailableOnlyInEnm(activityJobId, jobLogs, deletePackageContextForEnm, vnfSoftwarePackageEntity);

                    } else if (tasksBase.isPackageAvailableBothInEnmAndNfvo(softwarePackageLocation)) {
                        processDeleteForPackageAvailableBothInEnmAndNfvo(activityJobId, jobLogs, deletePackageContextForEnm, vnfSoftwarePackageEntity);

                    } else {
                        jobLogMessage = String.format(VranJobLogMessageTemplate.SWPACKAGE_NOTFOUND_IN_ENM, softwarePackageName);
                        jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.INFO.toString()));
                        handleDeletePackageFailure(activityJobId, deletePackageContextForEnm);
                    }

                } catch (Exception packageDeletionFailedException) {
                    LOGGER.error("ActivityJob ID - [{}] : Failed to delete software package : {} from ENM", activityJobId, softwarePackageName, packageDeletionFailedException);
                    jobLogMessage = String.format(VranJobLogMessageTemplate.ACTIVITY_FAILED_FOR_PACKAGE_WITH_REASON, VranJobConstants.DEL_SW_ACTIVITY, softwarePackageName,
                            packageDeletionFailedException.getMessage());
                    jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString()));
                    handleDeletePackageFailure(activityJobId, deletePackageContextForEnm);
                }
            } else {
                LOGGER.warn("ActivityJob ID - [{}] : Unable to retrieve entity from database for vnf package {}", activityJobId, softwarePackageName);
                jobLogMessage = String.format(VranJobLogMessageTemplate.SWPACKAGE_NOTFOUND_IN_ENM, softwarePackageName);
                jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.INFO.toString()));
                handleDeletePackageFailure(activityJobId, deletePackageContextForEnm);
            }
        }

        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, null, jobLogs);

    }

    private void processDeleteForPackageAvailableBothInEnmAndNfvo(final long activityJobId, final List<Map<String, Object>> jobLogs, final DeletePackageContextForEnm deletePackageContextForEnm,
            final PersistenceObject vnfSoftwarePackageEntity) throws ResourceException, IOException {
        String jobLogMessage;

        final String filePath = vnfSoftwarePackageEntity.getAttribute(UpgradeActivityConstants.UP_PO_FILE_PATH);
        final String softwarePackageName = deletePackageContextForEnm.getCurrentPackage();

        jobLogMessage = String.format(VranJobLogMessageTemplate.DELETING_SWPACKAGE_FROM_ENM, filePath);
        jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.INFO.toString()));

        deleteSoftwarePackageSMRSService.delete(filePath, activityJobId);
        markSoftwarePackageToNfvo(vnfSoftwarePackageEntity);

        jobLogMessage = String.format(VranJobLogMessageTemplate.SWPACKAGE_LOCATION_UPDATE, softwarePackageName, VranJobConstants.NFVO);
        jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.INFO.toString()));

        deleteJobPropertiesPersistenceProvider.incrementSuccessSoftwarePackageCountForEnm(activityJobId, deletePackageContextForEnm.getContext());

    }

    private void processDeleteForPackageAvailableOnlyInEnm(final long activityJobId, final List<Map<String, Object>> jobLogs, final DeletePackageContextForEnm deletePackageContextForEnm,
            final PersistenceObject vnfSoftwarePackageEntity) throws ResourceException, IOException {
        String jobLogMessage;

        final String filePath = vnfSoftwarePackageEntity.getAttribute(UpgradeActivityConstants.UP_PO_FILE_PATH);
        final String softwarePackageName = deletePackageContextForEnm.getCurrentPackage();

        jobLogMessage = String.format(VranJobLogMessageTemplate.DELETING_SWPACKAGE_FROM_ENM, filePath);
        jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.INFO.toString()));

        deleteSoftwarePackageSMRSService.delete(filePath, activityJobId);
        vnfSoftwarePackagePersistenceProvider.deleteSoftwarePackageEntity(vnfSoftwarePackageEntity.getPoId());

        deleteJobPropertiesPersistenceProvider.incrementSuccessSoftwarePackageCountForEnm(activityJobId, deletePackageContextForEnm.getContext());

        jobLogMessage = String.format(VranJobLogMessageTemplate.SWPACKAGE_DELETED_FROM_ENM, softwarePackageName);
        jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.INFO.toString()));
    }

    private void handleDeletePackageFailure(final long activityJobId, final DeletePackageContextForEnm deletePackageContextForEnm) {

        deleteJobPropertiesPersistenceProvider.incrementFailedSoftwarePackageCountForEnm(activityJobId, deletePackageContextForEnm.getContext());
        deleteJobPropertiesPersistenceProvider.updateFailedSoftwarePackagesInEnm(activityJobId, deletePackageContextForEnm.getCurrentPackage(), deletePackageContextForEnm.getContext());
    }

    private void markSoftwarePackageToNfvo(final PersistenceObject vnfSoftwarePackageEntity) {
        final Map<String, Object> vnfPackageAttributes = vnfSoftwarePackageEntity.getAllAttributes();
        vnfPackageAttributes.put(VranJobConstants.SW_PACKAGE_LOCATION, VranJobConstants.NFVO);
        vnfSoftwarePackagePersistenceProvider.updateSoftwarePackageEntity(vnfSoftwarePackageEntity.getPoId(), vnfPackageAttributes);
    }

    private boolean isPackageAvailableOnlyInEnm(final String softwarePackageLocation) {
        return VranJobConstants.SMRS.equalsIgnoreCase(softwarePackageLocation);
    }

    private void requestNfvoToDeleteSoftwarePackage(final long activityJobId, final String softwarePackageName, final String vnfPackageId, final String nodeFdn,
            final JobActivityInfo jobActivityInformation) {

        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        LOGGER.trace("ActivityJob ID - [{}] : Software package delete job is going to be trigger with nodeFdn: {}, softwarePackageId: {}, vnfPackageId: {}", activityJobId, nodeFdn,
                softwarePackageName, vnfPackageId);

        final String subscriptionKey = nodeFdn + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + vnfPackageId;
        activityUtils.subscribeToMoNotifications(subscriptionKey, activityJobId, jobActivityInformation);

        LOGGER.debug("ActivityJob ID - [{}] : Subscription key generated before sending delete softwarepackage action is : {}", activityJobId, subscriptionKey);

        jobLogs.add(vranJobActivityService.buildJobLog(String.format(VranJobLogMessageTemplate.ACTIVITY_ABOUT_TO_START, VranJobConstants.DEL_SW_ACTIVITY, softwarePackageName),
                JobLogLevel.INFO.toString()));
        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, null, jobLogs);

        deleteSoftwarePackageEventSender.sendDeleteSoftwarePackageRequest(nodeFdn, vnfPackageId);
    }
}
