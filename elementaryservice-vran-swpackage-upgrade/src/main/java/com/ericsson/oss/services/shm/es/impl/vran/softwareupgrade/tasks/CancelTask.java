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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranUpgradeJobContextBuilder;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;

public class CancelTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelTask.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private VranUpgradeJobContextBuilder vranUpgradeJobContextBuilder;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Inject
    private TaskBase taskBase;

    private final ActivityStepResult activityStepResult = new ActivityStepResult();
    private final List<Map<String, Object>> jobLogs = new ArrayList<>();
    private final List<Map<String, Object>> jobProperties = new ArrayList<>();
    private String jobLogMessage;

    public ActivityStepResult processCancelForSupportedActivity(final JobActivityInfo jobActivityInformation) {
        final long activityJobId = jobActivityInformation.getActivityJobId();
        final String activityName = jobActivityInformation.getActivityName();
        final UpgradePackageContext upgradePackageContext = vranUpgradeJobContextBuilder.build(activityJobId);
        final JobEnvironment jobContext = upgradePackageContext.getJobEnvironment();

        prepareJobLogsForCancelTask(jobActivityInformation, jobLogs, jobProperties, jobContext);
        taskBase.unSubscribeNotification(jobActivityInformation, upgradePackageContext, activityJobId);

        try {
            initiateCancelJobOnVnfm(jobActivityInformation, upgradePackageContext);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);

            LOGGER.debug("ActivityJob ID - [{}] : Processed cancel action for activity {} and the result is : [{}]", activityJobId, activityName, activityStepResult.getActivityResultEnum());
        } catch (final Exception exception) {
            LOGGER.error("ActivityJob ID - [{}] : Failed to perform cancel action because of  : ", activityJobId, exception);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            jobLogMessage = String.format(JobLogConstants.ACTION_TRIGGER_FAILED, VranJobConstants.JOB_CANCEL) + String.format(JobLogConstants.FAILURE_REASON, exception.getCause());
            jobLogs.add(vranJobActivityServiceHelper.buildJobLog(jobLogMessage, new Date(), JobLogLevel.INFO.toString()));
        }

        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobProperties, jobLogs);

        return activityStepResult;
    }

    private void initiateCancelJobOnVnfm(final JobActivityInfo jobActivityInformation, final UpgradePackageContext vranUpgradeInformation) {
        taskBase.performSoftwareUpgrade(jobActivityInformation.getActivityJobId(), vranUpgradeInformation, VranJobConstants.JOB_CANCEL, jobActivityInformation, VranJobConstants.JOB_CANCEL);
    }

    public ActivityStepResult processCancelForUnSupportedActivity(final JobActivityInfo jobActivityInformation) {

        final Map<String, Object> processVariables = new HashMap<>();
        final long activityJobId = jobActivityInformation.getActivityJobId();
        final UpgradePackageContext vranUpgradeInformation = vranUpgradeJobContextBuilder.build(activityJobId);
        final JobEnvironment jobContext = vranUpgradeInformation.getJobEnvironment();
        final String activityName = vranUpgradeInformation.getActionTriggered() != null ? vranUpgradeInformation.getActionTriggered() : ActivityConstants.ACTIVATE;
        LOGGER.debug("ActivityJob ID - [{}] :Processing cancel action for activity [{}]", activityJobId, activityName);

        prepareJobLogsForCancelTask(jobActivityInformation, jobLogs, jobProperties, jobContext);
        taskBase.unSubscribeNotification(jobActivityInformation, vranUpgradeInformation, activityJobId);
        buildUnsupportedActivityJobLogs(activityName);

        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.CANCELLED.toString());
        jobUpdateService.readAndUpdateJobAttributesForCancel(jobActivityInformation.getActivityJobId(), jobProperties, jobLogs);

        activityUtils.sendNotificationToWFS(jobContext, jobActivityInformation.getActivityJobId(), VranJobConstants.JOB_CANCEL, processVariables);

        LOGGER.debug("ActivityJob ID - [{}] : Cancel action not supported for activity {} ", jobActivityInformation.getActivityJobId(), activityName);
        return activityStepResult;
    }

    private void buildUnsupportedActivityJobLogs(final String activityName) {
        jobLogMessage = String.format(VranJobLogMessageTemplate.CANCEL_NOT_SUPPORT, activityName);
        jobLogs.add(vranJobActivityServiceHelper.buildJobLog(jobLogMessage, new Date(), JobLogLevel.INFO.toString()));
    }

    private void prepareJobLogsForCancelTask(final JobActivityInfo jobActivityInformation, final List<Map<String, Object>> jobLogs, final List<Map<String, Object>> jobProperties,
            final JobEnvironment jobContext) {
        jobLogs.add(vranJobActivityServiceHelper.buildJobLog(String.format(JobLogConstants.ACTIVITY_INITIATED, VranJobConstants.JOB_CANCEL), new Date(), JobLogLevel.INFO.toString()));
        activityUtils.logCancelledByUser(jobLogs, jobContext, jobActivityInformation.getActivityName());
        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.IS_CANCEL_TRIGGERED, ActivityConstants.CHECK_TRUE);
    }

}
