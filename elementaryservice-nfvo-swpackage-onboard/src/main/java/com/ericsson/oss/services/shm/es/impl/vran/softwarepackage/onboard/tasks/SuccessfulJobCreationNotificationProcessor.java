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

import java.util.*;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardSoftwarePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

/**
 * Delegator which handles onboard software package notifications received from Nfvo related to create job
 * 
 * @author xjhosye
 *
 */
public class SuccessfulJobCreationNotificationProcessor {

    @Inject
    private TasksBase tasksBase;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private VranJobActivityUtil vranJobActivityUtil;

    private static final Logger LOGGER = LoggerFactory.getLogger(SuccessfulJobCreationNotificationProcessor.class);

    public void processCreateJobResponse(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo,
            final JobActivityInfo jobActivityInformation, final long neJobId) {

        LOGGER.debug("ActivityJob ID - [{}] : Processing create job notification received from Nfvo. Response : {}", nfvoSoftwarePackageJobResponse.getActivityJobId(), nfvoSoftwarePackageJobResponse);

        updateJobLogsAndProperties(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, neJobId);
        tasksBase.unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInformation);
        startTrackingOnboardSoftwarePackageJobStatus(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo,
                jobActivityInformation);
    }

    private void startTrackingOnboardSoftwarePackageJobStatus(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo,
            final JobActivityInfo jobActivityInformation) {
        tasksBase.requestOnboardsoftwarePackageJobStatus(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, jobActivityInformation);
    }

    private void updateJobLogsAndProperties(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo,
            final long neJobId) {

        final List<Map<String, Object>> activityJobLogs = new ArrayList<Map<String, Object>>();
        String jobLogMessage = null;
        final List<Map<String, Object>> neJobProperties = onboardSoftwarePackageContextForNfvo.getNeJobProperties();

        jobLogMessage = String.format(VranJobLogMessageTemplate.JOB_PROGRESS_INFORMATION_WITH_JOB_ID, nfvoSoftwarePackageJobResponse.getActivityName(),
                onboardSoftwarePackageContextForNfvo.getCurrentPackage(), nfvoSoftwarePackageJobResponse.getJobId());
        activityJobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, nfvoSoftwarePackageJobResponse.getNotificationTimeStamp(), JobLogLevel.INFO.toString()));

        final Calendar calendar = vranJobActivityUtil.incrementTime(nfvoSoftwarePackageJobResponse.getNotificationTimeStamp(), 5);
        jobLogMessage = String.format(VranJobLogMessageTemplate.JOB_PROGRESS_INFORMATION_WITH_RESULT, onboardSoftwarePackageContextForNfvo.getCurrentPackage(),
                nfvoSoftwarePackageJobResponse.getActivityName(), VranJobConstants.PROGRESS_LEVEL1, nfvoSoftwarePackageJobResponse.getStatus(), nfvoSoftwarePackageJobResponse.getResult(),
                nfvoSoftwarePackageJobResponse.getDescription());
        activityJobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, calendar.getTime(), JobLogLevel.INFO.toString()));

        jobAttributesPersistenceProvider.persistJobProperties(neJobId, neJobProperties);
        jobAttributesPersistenceProvider.persistJobLogs(nfvoSoftwarePackageJobResponse.getActivityJobId(), activityJobLogs);
    }

}
