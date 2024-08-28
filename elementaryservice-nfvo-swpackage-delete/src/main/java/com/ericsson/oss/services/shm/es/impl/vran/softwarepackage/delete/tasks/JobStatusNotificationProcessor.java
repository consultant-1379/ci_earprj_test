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
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobEvents;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

@Stateless
public class JobStatusNotificationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobStatusNotificationProcessor.class);

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
    private SystemRecorder systemRecorder;

    @Inject
    private VranJobActivityUtil vranJobActivityUtil;

    public void processJobInprogressResponse(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final DeletePackageContextForNfvo deletePackageContextForNfvo,
            final JobActivityInfo jobActivityInformation) {

        LOGGER.debug("ActivityJob ID - [{}] : Processing job poll notification received from Nfvo. Response : {}", nfvoSoftwarePackageJobResponse.getActivityJobId(), nfvoSoftwarePackageJobResponse);

        updateJobLogsAndPropertiesForDeleteInProgress(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo);
        tasksBase.unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInformation);
        requestAgainForDeleteSoftwarePackageJobStatus(nfvoSoftwarePackageJobResponse.getActivityJobId(), nfvoSoftwarePackageJobResponse.getJobId(), deletePackageContextForNfvo, jobActivityInformation);
    }

    public void processJobSuccessResponse(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final DeletePackageContextForNfvo deletePackageContextForNfvo,
            final JobActivityInfo jobActivityInformation) {

        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final long activityJobId = nfvoSoftwarePackageJobResponse.getActivityJobId();

        LOGGER.debug("ActivityJob ID - [{}] : Processing delete success notification received from Nfvo. Response : {}", activityJobId, nfvoSoftwarePackageJobResponse);

        deleteJobPropertiesPersistenceProvider.incrementSuccessSoftwarePackageCountInNfvo(activityJobId, deletePackageContextForNfvo.getContext());
        updateJobLogsAndPropertiesForDeleteSuccessful(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo, jobLogs);
        tasksBase.unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInformation);
    }

    private void updateJobLogsAndPropertiesForDeleteInProgress(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final DeletePackageContextForNfvo deletePackageContextForNfvo) {
        final String jobLogMessage;
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();

        jobLogMessage = String.format(VranJobLogMessageTemplate.JOB_PROGRESS_INFORMATION_WITH_RESULT, deletePackageContextForNfvo.getCurrentPackage(),
                nfvoSoftwarePackageJobResponse.getActivityName(), VranJobConstants.PROGRESS_LEVEL2, nfvoSoftwarePackageJobResponse.getStatus(), nfvoSoftwarePackageJobResponse.getResult(),
                nfvoSoftwarePackageJobResponse.getDescription());
        jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, nfvoSoftwarePackageJobResponse.getNotificationTimeStamp(), JobLogLevel.INFO.toString()));

        jobAttributesPersistenceProvider.persistJobLogs(nfvoSoftwarePackageJobResponse.getActivityJobId(), jobLogs);
    }

    private void requestAgainForDeleteSoftwarePackageJobStatus(final long activityJobId, final String jobId, final DeletePackageContextForNfvo deletePackageContextForNfvo,
            final JobActivityInfo jobActivityInformation) {
        tasksBase.requestDeleteSoftwarePackageJobStatus(activityJobId, jobId, deletePackageContextForNfvo, jobActivityInformation);

    }

    private void updateJobLogsAndPropertiesForDeleteSuccessful(final NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse, final DeletePackageContextForNfvo deletePackageContextForNfvo,
            final List<Map<String, Object>> jobLogs) {

        String jobLogMessage = null;

        final JobEnvironment jobContext = deletePackageContextForNfvo.getContext();
        final String softwarePackageName = deletePackageContextForNfvo.getCurrentPackage();
        final String nodeName = jobContext.getNodeName();
        final long neJobId = jobContext.getNeJobId();

        jobLogMessage = String.format(VranJobLogMessageTemplate.JOB_PROGRESS_INFORMATION_WITH_RESULT, deletePackageContextForNfvo.getCurrentPackage(),
                nfvoSoftwarePackageJobResponse.getActivityName(), VranJobConstants.PROGRESS_LEVEL3, nfvoSoftwarePackageJobResponse.getStatus(), nfvoSoftwarePackageJobResponse.getResult(),
                nfvoSoftwarePackageJobResponse.getDescription());
        jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, nfvoSoftwarePackageJobResponse.getNotificationTimeStamp(), JobLogLevel.INFO.toString()));

        activityUtils.recordEvent(VranJobEvents.DELETE_SOFTWARE_PACKAGE_NOTIFICATION, nodeName, softwarePackageName,
                activityUtils.additionalInfoForEvent(nfvoSoftwarePackageJobResponse.getActivityJobId(), nodeName, jobLogMessage));

        jobLogMessage = String.format(VranJobLogMessageTemplate.PERCENTAGE_OF_JOB_COMPLETED, VranJobConstants.PROGRESS_LEVEL3, VranJobConstants.DEL_SW_ACTIVITY,
                deletePackageContextForNfvo.getCurrentPackage());
        final Calendar calendar = vranJobActivityUtil.incrementTime(nfvoSoftwarePackageJobResponse.getNotificationTimeStamp(), 10);
        jobLogs.add(vranJobActivityService.buildJobLog(jobLogMessage, calendar.getTime(), JobLogLevel.INFO.toString()));

        systemRecorder.recordCommand(VranJobEvents.DELETE_SOFTWARE_PACKAGE_NOTIFICATION, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, softwarePackageName,
                activityUtils.additionalInfoForCommand(nfvoSoftwarePackageJobResponse.getActivityJobId(), neJobId, JobTypeEnum.DELETE_SOFTWAREPACKAGE));

        jobAttributesPersistenceProvider.persistJobLogs(nfvoSoftwarePackageJobResponse.getActivityJobId(), jobLogs);
    }

}
