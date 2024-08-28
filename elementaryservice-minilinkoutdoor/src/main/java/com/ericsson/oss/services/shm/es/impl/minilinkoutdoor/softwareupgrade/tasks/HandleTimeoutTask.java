/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.tasks;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.MiniLinkOutdoorJobUtil;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

public class HandleTimeoutTask {

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

    @Inject
    private JobUpdateService jobUpdateService;

    public static final String ACTIVITY_IN_TIMEOUT = "Notifications not received for the \"%s\" activity";

    private static final Logger LOGGER = LoggerFactory.getLogger(HandleTimeoutTask.class);

    public ActivityStepResult handleTimeout(final JobActivityInfo jobActivityInformation) {

        NEJobStaticData neJobStaticData = null;
        final String activityName = jobActivityInformation.getActivityName();
        final long activityJobId = jobActivityInformation.getActivityJobId();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final String nodeName = neJobStaticData.getNodeName();
            activityUtils.unSubscribeToMoNotifications(miniLinkOutdoorJobUtil.getSubscriptionKey(nodeName, activityName), activityJobId, jobActivityInformation);
            persistJobLogsAndProperties(activityName, activityJobId);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        } catch (JobDataNotFoundException e) {
            LOGGER.error("NE job static data not found in neJob cache and failed to get from DPS. {}", e);
        }

        return activityStepResult;
    }

    private void persistJobLogsAndProperties(final String activityName, final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final JobResult jobResult = JobResult.FAILED;

        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, jobResult.toString());
        jobLogList.add(activityUtils.createNewLogEntry(String.format(ACTIVITY_IN_TIMEOUT, activityName), JobLogLevel.INFO.getLogLevel()));
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogList, null);
    }

}
