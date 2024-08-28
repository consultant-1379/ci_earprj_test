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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardPackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardSoftwarePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.persistence.OnboardJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class HandleTimeoutTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandleTimeoutTask.class);

    @Inject
    private TasksBase tasksBase;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    private OnboardJobPropertiesPersistenceProvider jobDetailsPersistenceProvider;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private OnboardPackageContextBuilder onboardPackageContextBuilder;

    public boolean handleTimeout(final long activityJobId, final JobActivityInfo jobActivityInformation) {

        boolean repeatExecute = false;
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo = onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext);

        final String nodeFdn = onboardSoftwarePackageContextForNfvo.getNodeFdn();

        final String subscriptionKey = nodeFdn + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + activityJobId;
        activityUtils.unSubscribeToMoNotifications(subscriptionKey, activityJobId, jobActivityInformation);

        LOGGER.debug("ActivityJob ID - [{}] : Handling timeout for the software package {}.", activityJobId, onboardSoftwarePackageContextForNfvo.getCurrentPackage());
        jobDetailsPersistenceProvider.incrementOnboardFailedSoftwarePackagesCount(activityJobId, jobContext);
        final String logMessage = String.format(VranJobLogMessageTemplate.SOFTWARE_PACKAGE_IN_TIMEOUT, VranJobConstants.ONBOARD, onboardSoftwarePackageContextForNfvo.getCurrentPackage());
        jobLogs.add(vranJobActivityService.buildJobLog(logMessage, JobLogLevel.ERROR.toString()));
        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, null, jobLogs);
        repeatExecute = proceedWithNextStepAfterHandlingTimeout(activityJobId);
        return repeatExecute;
    }

    private boolean proceedWithNextStepAfterHandlingTimeout(final long activityJobId) {

        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        boolean repeatExecute = false;
        String jobLogMessage = null;
        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);

        final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo = onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext);

        if (onboardSoftwarePackageContextForNfvo.isComplete()) {
            final int noOfFailures = onboardSoftwarePackageContextForNfvo.getNoOfFailures();
            jobLogMessage = tasksBase.updateJobLogMessage(onboardSoftwarePackageContextForNfvo.getTotalCount(), onboardSoftwarePackageContextForNfvo.getSuccessCount(), noOfFailures);
            jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString()));
            tasksBase.markSoftwarePackageOnboardActivityResult(jobProperties, noOfFailures);
            tasksBase.sendNfvoVnfPackageSyncRequest(activityJobId, onboardSoftwarePackageContextForNfvo);
        } else {
            repeatExecute = true;
            jobDetailsPersistenceProvider.incrementSoftwarePackageCurrentIndexToBeOnboarded(activityJobId, jobContext);
        }

        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, jobProperties, jobLogs);
        return repeatExecute;
    }

}
