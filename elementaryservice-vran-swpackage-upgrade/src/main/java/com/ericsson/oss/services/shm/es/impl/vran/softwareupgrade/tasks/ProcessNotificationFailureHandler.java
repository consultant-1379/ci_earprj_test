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

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskutils.NotificationTaskUtils;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class ProcessNotificationFailureHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessNotificationFailureHandler.class);

    @Inject
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Inject
    private TaskBase taskBase;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private NotificationTaskUtils notificationTaskUtils;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    public void handle(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final String activityName, final UpgradePackageContext upgradePackageContext,
            final JobActivityInfo jobActivityInformation) {
        LOGGER.debug("ActivityJob ID - [{}] : Sending failed notification for activity {} to work flow service and  updating job properties", vranSoftwareUpgradeJobResponse.getActivityJobId(),
                activityName);
        final Map<String, Object> processVariables = new HashMap<>();

        final String subscriptionKey = taskBase.buildSubscriptionKey(upgradePackageContext, vranSoftwareUpgradeJobResponse.getActivityJobId());
        activityUtils.unSubscribeToMoNotifications(subscriptionKey, vranSoftwareUpgradeJobResponse.getActivityJobId(), jobActivityInformation);

        persistJobLogsAndProperties(vranSoftwareUpgradeJobResponse);

        activityUtils.sendNotificationToWFS(upgradePackageContext.getJobEnvironment(), vranSoftwareUpgradeJobResponse.getActivityJobId(), activityName, processVariables);
    }

    private void persistJobLogsAndProperties(final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse) {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();

        final String message = notificationTaskUtils.prepareErrorLogMessage(vranSoftwareUpgradeJobResponse, vranSoftwareUpgradeJobResponse.getActivityName());
        jobLogs.add(vranJobActivityServiceHelper.buildJobLog(message, new Date(), JobLogLevel.ERROR.toString()));
        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        jobAttributesPersistenceProvider.persistJobPropertiesAndLogs(vranSoftwareUpgradeJobResponse.getActivityJobId(), jobProperties, jobLogs);

    }

}
