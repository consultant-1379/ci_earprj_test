/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.impl.JobActivitiesProviderImpl;
import com.ericsson.oss.services.shm.job.remote.api.BackupActivityEnum;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;

/**
 * Utility class to prepare backup job schedule attributes to create backup job when external request is received for job creation
 * 
 * @author tcsmaup
 * 
 */
public class BackupActivitySchedulesUtil {

    @Inject
    JobActivitiesProviderImpl jobActivitiesProviderImpl;

    /**
     * Funtion to prepare backup job schedule attributes to create backup job when external request is received for job creation based on platform, netype, jobtype and backup activities
     * 
     * @param platformTypeEnum
     * @param neType
     * @param jobType
     * @param backupActivities
     * @return
     */

    public List<Map<String, Object>> prepareActivitySchedules(final PlatformTypeEnum platformTypeEnum, final String neType, final String jobType, final List<BackupActivityEnum> backupActivities) {
        final List<ActivityInfo> activityInfos = jobActivitiesProviderImpl.getActivityInfo(platformTypeEnum.getName(), neType, jobType);
        final List<Map<String, Object>> invidualActivitySchedules = new ArrayList<Map<String, Object>>();
        for (BackupActivityEnum backupActivityEnum : backupActivities) {
            for (ActivityInfo activityInfo : activityInfos) {
                if (backupActivityEnum.getActivityName().equals(activityInfo.getActivityName())) {
                    invidualActivitySchedules.add(getActivitySchedule(activityInfo.getActivityName(), activityInfo.getOrder()));
                }
            }
        }
        return invidualActivitySchedules;
    }

    /**
     * prepare activity schedule map based on activity name and activity order
     * 
     * @param activityName
     * @param activityOrder
     * @return
     */
    private static Map<String, Object> getActivitySchedule(final String activityName, final int activityOrder) {
        final Map<String, Object> activitySchedule = new HashMap<String, Object>();
        activitySchedule.put(ShmConstants.ACTIVITYNAME, activityName);
        activitySchedule.put(ShmConstants.EXECUTION_MODE, ExecMode.IMMEDIATE.name());
        activitySchedule.put(ShmConstants.ORDER, activityOrder);
        return activitySchedule;
    }
}
