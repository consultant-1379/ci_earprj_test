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

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.MTRSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardPackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardSoftwarePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.nfvo.OnboardSoftwarePackageService;
import com.ericsson.oss.services.shm.es.vran.onboard.api.exceptions.SmrsFilePathNotFoundException;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@Stateless
public class ExecuteTask {

    @Inject
    private TasksBase tasksBase;

    @Inject
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private MTRSender onboardSoftwarepackageEventSender;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    private OnboardPackageContextBuilder onboardPackageContextBuilder;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteTask.class);

    public void execute(final long activityJobId) {

        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo = onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext);

        onboardSoftwarePackageToNfvo(activityJobId, onboardSoftwarePackageContextForNfvo);
    }

    private void onboardSoftwarePackageToNfvo(final long activityJobId, final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo) {

        final String softwarePackageName = onboardSoftwarePackageContextForNfvo.getCurrentPackage();
        final String nfvoFdn = onboardSoftwarePackageContextForNfvo.getNodeFdn();
        LOGGER.debug("ActivityJob ID - [{}] : Onboarding software package {} to NFVO location", activityJobId, softwarePackageName);
        try {
            performOnboardAction(activityJobId, softwarePackageName, nfvoFdn, activityUtils.getActivityInfo(activityJobId, OnboardSoftwarePackageService.class));
        } catch (Exception packageOnboardFailedException) {
            LOGGER.error("ActivityJob ID - [{}] :Failed to onboard software package {}. Reason : {}", activityJobId, softwarePackageName, packageOnboardFailedException.getMessage(),
                    packageOnboardFailedException);
            tasksBase.handleOnboardPackageFailure(activityJobId, onboardSoftwarePackageContextForNfvo, softwarePackageName, packageOnboardFailedException.getMessage());
            tasksBase.proceedWithNextStep(activityJobId);
        }
    }

    /**
     * Performs software package onboard on NFVO
     *
     * @param activityJobId
     * @param onboardSoftwarepackageInformation
     * @param jobActivityInformation
     */
    public void performOnboardAction(final long activityJobId, final String softwarePackageName, final String nfvoFdn, final JobActivityInfo jobActivityInformation) {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();

        final String filePathInEnm = vnfSoftwarePackagePersistenceProvider.getVnfPackageSMRSPath(softwarePackageName);
        final Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put(ActivityConstants.ACTIVITY_JOB_ID, activityJobId);

        if (filePathInEnm != null && !filePathInEnm.isEmpty()) {
            jobLogs.add(
                    vranJobActivityService.buildJobLog(String.format(VranJobLogMessageTemplate.ACTIVITY_ABOUT_TO_START, VranJobConstants.ONBOARD, softwarePackageName), JobLogLevel.INFO.toString()));

            tasksBase.subscribeNotifications(activityJobId, nfvoFdn, jobActivityInformation);
            jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, null, jobLogs);
            onboardSoftwarepackageEventSender.sendOnboardSoftwarePackageRequest(nfvoFdn, filePathInEnm, eventAttributes);
        } else {
            throw new SmrsFilePathNotFoundException(VranJobConstants.SMRS_FILEPATH_NOT_FOUND + softwarePackageName);
        }
    }
}
