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

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForEnm;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class HandleTimeoutTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandleTimeoutTask.class);

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

    public boolean handleTimeout(final long activityJobId, final JobActivityInfo jobActivityInformation) {

        boolean repeatExecute = false;
        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        final DeletePackageContextForEnm deletePackageContextForEnm = deletePackageContextBuilder.buildDeletePackageContextForEnm(jobContext);
        final DeletePackageContextForNfvo deletePackageContextForNfvo = deletePackageContextBuilder.buildDeletePackageContextForNfvo(jobContext);

        final String nfvoJobId = deletePackageContextForNfvo.getNfvoJobId();
        final String nodeFdn = deletePackageContextForNfvo.getNodeFdn();

        final String subscriptionKey = nodeFdn + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + nfvoJobId;
        activityUtils.unSubscribeToMoNotifications(subscriptionKey, activityJobId, jobActivityInformation);

        if (!deletePackageContextForEnm.isComplete()) {

            LOGGER.debug("ActivityJob ID - [{}] : Handling timeout for Enm package {}.", activityJobId, deletePackageContextForEnm.getCurrentPackage());

            deleteJobPropertiesPersistenceProvider.incrementFailedSoftwarePackageCountForEnm(activityJobId, jobContext);
            deleteJobPropertiesPersistenceProvider.updateFailedSoftwarePackagesInEnm(activityJobId, deletePackageContextForEnm.getCurrentPackage(), jobContext);

            updateJobLog(activityJobId, deletePackageContextForEnm);

            repeatExecute = proceedWithNextStepForEnmFailure(activityJobId);

        } else {

            LOGGER.debug("ActivityJob ID - [{}] : Handling timeout for Nfvo package {}.", activityJobId, deletePackageContextForNfvo.getCurrentPackage());

            deleteJobPropertiesPersistenceProvider.incrementFailedSoftwarePackageCountInNfvo(activityJobId, jobContext);
            deleteJobPropertiesPersistenceProvider.updateFailedSoftwarePackagesInNfvo(activityJobId, deletePackageContextForNfvo.getCurrentPackage(), jobContext);

            updateJobLog(activityJobId, deletePackageContextForNfvo);

            repeatExecute = proceedWithNextStepForNfvoFailure(activityJobId);

        }

        return repeatExecute;
    }

    /**
     * @param activityJobId
     * @param deletePackageContext
     */
    private void updateJobLog(final long activityJobId, final DeletePackageContext deletePackageContext) {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final String logMessage = String.format(VranJobLogMessageTemplate.SOFTWARE_PACKAGE_IN_TIMEOUT, VranJobConstants.DEL_SW_ACTIVITY, deletePackageContext.getCurrentPackage());
        jobLogs.add(vranJobActivityService.buildJobLog(logMessage, JobLogLevel.ERROR.toString()));
        jobAttributesPersistenceProvider.persistJobLogs(activityJobId, jobLogs);
    }

    private boolean proceedWithNextStepForNfvoFailure(final long activityJobId) {

        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> activityJobProperties = new ArrayList<>();
        boolean repeatExecute = false;
        String jobLogMessage = null;
        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);

        final DeletePackageContextForEnm deletePackageContextForEnm = deletePackageContextBuilder.buildDeletePackageContextForEnm(jobContext);
        final DeletePackageContextForNfvo deletePackageContextForNfvo = deletePackageContextBuilder.buildDeletePackageContextForNfvo(jobContext);

        LOGGER.debug("ActivityJob ID - [{}] : Contextual information built for Nfvo to process post deletion activity is {}", activityJobId, deletePackageContextForNfvo);

        if (deletePackageContextForNfvo.isComplete()) {

            if (deletePackageContextForEnm.getTotalCount() > 0) {
                jobLogMessage = tasksBase.buildConsolidatedJobLogMessageForEnm(deletePackageContextForEnm.getTotalCount(), deletePackageContextForEnm.getSuccessCount(),
                        deletePackageContextForEnm.getNoOfFailures(), deletePackageContextForEnm.getFailedPackages());
                jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString()));
            }

            final int noOfFailures = deletePackageContextForNfvo.getNoOfFailures();
            jobLogMessage = tasksBase.buildConsolidatedJobLogMessageForNfvo(deletePackageContextForNfvo.getTotalCount(), deletePackageContextForNfvo.getSuccessCount(), noOfFailures,
                    deletePackageContextForNfvo.getFailedPackages());
            jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString()));
            tasksBase.markSoftwarePackageDeleteActivityResult(activityJobProperties, noOfFailures);
            tasksBase.sendNfvoVnfPackagesSyncRequest(activityJobId, deletePackageContextForNfvo);
        } else {
            repeatExecute = true;
            deleteJobPropertiesPersistenceProvider.incrementSoftwarePackageCurrentIndexInNfvo(activityJobId, deletePackageContextForNfvo.getContext());
        }

        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, activityJobProperties, jobLogs);
        return repeatExecute;
    }

    private boolean proceedWithNextStepForEnmFailure(final long activityJobId) {

        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> activityJobProperties = new ArrayList<Map<String, Object>>();
        boolean repeatExecute = false;
        String jobLogMessage = null;

        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        final DeletePackageContextForEnm deletePackageContextForEnm = deletePackageContextBuilder.buildDeletePackageContextForEnm(jobContext);
        final DeletePackageContextForNfvo deletePackageContextForNfvo = deletePackageContextBuilder.buildDeletePackageContextForNfvo(jobContext);

        LOGGER.debug("ActivityJob ID - [{}] : Contextual information built to process post deletion activity are, for Enm {} and for Nfvo {}", activityJobId, deletePackageContextForEnm,
                deletePackageContextForNfvo);

        if (!deletePackageContextForEnm.isComplete()) {
            repeatExecute = true;
            deleteJobPropertiesPersistenceProvider.incrementSoftwarePackageCurrentIndexInEnm(activityJobId, deletePackageContextForEnm.getContext());
        } else if (deletePackageContextForEnm.isComplete() && deletePackageContextForNfvo.areThereAnyPackagesToBeDeleted()) {
            deleteJobPropertiesPersistenceProvider.incrementSoftwarePackageCurrentIndexInNfvo(activityJobId, deletePackageContextForNfvo.getContext());
            repeatExecute = true;
        } else {
            jobLogMessage = tasksBase.buildConsolidatedJobLogMessageForEnm(deletePackageContextForEnm.getTotalCount(), deletePackageContextForEnm.getSuccessCount(),
                    deletePackageContextForEnm.getNoOfFailures(), deletePackageContextForEnm.getFailedPackages());
            jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString()));
            final int noOfFailures = deletePackageContextForEnm.getNoOfFailures();
            tasksBase.markSoftwarePackageDeleteActivityResult(activityJobProperties, noOfFailures, deletePackageContextForEnm.getNoOfFailures());
            repeatExecute = false;
        }

        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, activityJobProperties, jobLogs);
        return repeatExecute;
    }
}
