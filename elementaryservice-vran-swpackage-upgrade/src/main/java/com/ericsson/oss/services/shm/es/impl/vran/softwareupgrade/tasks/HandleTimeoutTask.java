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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.VranUpgradeJobContextBuilder;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class HandleTimeoutTask {

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private VranUpgradeJobContextBuilder vranUpgradeJobContextBuilder;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Inject
    private TaskBase taskBase;

    public ActivityStepResult handleTimeout(final JobActivityInfo jobActivityInformation) {

        final String activityName = jobActivityInformation.getActivityName();
        final long activityJobId = jobActivityInformation.getActivityJobId();
        final ActivityStepResult activityStepResult = new ActivityStepResult();

        final UpgradePackageContext upgradePackageContext = vranUpgradeJobContextBuilder.build(activityJobId);
        taskBase.unSubscribeNotification(jobActivityInformation, upgradePackageContext, activityJobId);
        persistJobLogsAndProperties(activityJobId, activityName);
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);

        return activityStepResult;
    }

    private void persistJobLogsAndProperties(final long activityJobId, final String activityName) {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final JobResult jobResult = JobResult.FAILED;
        final String logMessage = String.format(VranJobLogMessageTemplate.ACTIVITY_IN_TIMEOUT, activityName);

        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
        jobLogs.add(vranJobActivityServiceHelper.buildJobLog(logMessage, new Date(), JobLogLevel.ERROR.toString()));
        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(activityJobId, jobProperties, jobLogs);
    }
}