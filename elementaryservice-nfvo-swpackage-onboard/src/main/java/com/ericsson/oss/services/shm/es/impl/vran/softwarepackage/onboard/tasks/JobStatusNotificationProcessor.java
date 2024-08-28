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

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardSoftwarePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.persistence.OnboardJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.vran.constants.*;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class JobStatusNotificationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobStatusNotificationProcessor.class);

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private TasksBase tasksBase;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private OnboardJobPropertiesPersistenceProvider jobDetailsPersistenceProvider;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private VranJobActivityUtil vranJobActivityUtil;

    protected void processJobInprogressResponse(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo,
            final JobActivityInfo jobActivityInformation) {
        String jobLogMessage;
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();

        LOGGER.debug("ActivityJob ID - [{}] : Processing job progress response and sending job poll request.", nfvoSoftwarePackageJobResponse.getActivityJobId());

        jobLogMessage = String.format(VranJobLogMessageTemplate.JOB_PROGRESS_INFORMATION_WITH_RESULT, onboardSoftwarePackageContextForNfvo.getCurrentPackage(),
                nfvoSoftwarePackageJobResponse.getActivityName(), VranJobConstants.PROGRESS_LEVEL2, nfvoSoftwarePackageJobResponse.getStatus(), nfvoSoftwarePackageJobResponse.getResult(),
                nfvoSoftwarePackageJobResponse.getDescription());
        jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, nfvoSoftwarePackageJobResponse.getNotificationTimeStamp(), JobLogLevel.INFO.toString()));
        jobAttributesPersistenceProvider.persistJobLogs(nfvoSoftwarePackageJobResponse.getActivityJobId(), jobLogs);

        tasksBase.unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInformation);

        tasksBase.requestOnboardsoftwarePackageJobStatus(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo,
                jobActivityInformation);
    }

    protected void processJobSuccessResponse(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo,
            final JobActivityInfo jobActivityInformation) {
        final long activityJobId = nfvoSoftwarePackageJobResponse.getActivityJobId();
        final JobEnvironment jobContext = onboardSoftwarePackageContextForNfvo.getContext();

        jobDetailsPersistenceProvider.incrementOnboardSuccessSoftwarePackagesCount(activityJobId, jobContext);

        updateJobLogsAndPropertiesForOnboardSucess(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo);
        tasksBase.unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInformation);

        tasksBase.proceedWithNextStep(activityJobId);
    }

    private void updateJobLogsAndPropertiesForOnboardSucess(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse,
            final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo) {

        String jobLogmessage = null;
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();

        final JobEnvironment jobContext = onboardSoftwarePackageContextForNfvo.getContext();
        final String softwarePackageName = onboardSoftwarePackageContextForNfvo.getCurrentPackage();
        final String nodeName = jobContext.getNodeName();
        final long neJobId = jobContext.getNeJobId();

        jobLogmessage = String.format(VranJobLogMessageTemplate.JOB_PROGRESS_INFORMATION_WITH_RESULT, onboardSoftwarePackageContextForNfvo.getCurrentPackage(),
                nfvoSoftwarePackageJobResponse.getActivityName(), VranJobConstants.PROGRESS_LEVEL3, nfvoSoftwarePackageJobResponse.getStatus(), nfvoSoftwarePackageJobResponse.getResult(),
                nfvoSoftwarePackageJobResponse.getDescription());
        jobLogs.add(vranJobActivityService.buildJobLog(jobLogmessage, nfvoSoftwarePackageJobResponse.getNotificationTimeStamp(), JobLogLevel.INFO.toString()));
        activityUtils.recordEvent(VranJobEvents.ONBOARD_SOFTWARE_PACKAGE_NOTIFICATION, nodeName, softwarePackageName,
                activityUtils.additionalInfoForEvent(nfvoSoftwarePackageJobResponse.getActivityJobId(), nodeName, jobLogmessage));

        jobLogmessage = String.format(VranJobLogMessageTemplate.PERCENTAGE_OF_JOB_COMPLETED, VranJobConstants.PROGRESS_LEVEL3, VranJobConstants.ONBOARD,
                onboardSoftwarePackageContextForNfvo.getCurrentPackage());
        final Calendar calendar = vranJobActivityUtil.incrementTime(nfvoSoftwarePackageJobResponse.getNotificationTimeStamp(), 10);
        jobLogs.add(vranJobActivityService.buildJobLog(jobLogmessage, calendar.getTime(), JobLogLevel.INFO.toString()));

        systemRecorder.recordCommand(VranJobEvents.ONBOARD_SOFTWARE_PACKAGE_NOTIFICATION, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, softwarePackageName,
                activityUtils.additionalInfoForCommand(nfvoSoftwarePackageJobResponse.getActivityJobId(), neJobId, JobTypeEnum.ONBOARD));

        jobAttributesPersistenceProvider.persistJobLogs(nfvoSoftwarePackageJobResponse.getActivityJobId(), jobLogs);
    }

}
