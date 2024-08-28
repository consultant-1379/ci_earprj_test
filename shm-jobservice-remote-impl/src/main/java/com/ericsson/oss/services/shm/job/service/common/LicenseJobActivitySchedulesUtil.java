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
package com.ericsson.oss.services.shm.job.service.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.impl.JobActivitiesProviderImpl;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;

public class LicenseJobActivitySchedulesUtil {

    @Inject
    JobActivitiesProviderImpl jobActivitiesProviderImpl;

    public List<Map<String, Object>> prepareActivitySchedules(final PlatformTypeEnum platformTypeEnum, final String neType, final String jobType, final String activity) {
        final List<ActivityInfo> activities = jobActivitiesProviderImpl.getActivityInfo(platformTypeEnum.getName(), neType, jobType);
        final List<Map<String, Object>> invidualActivitySchedules = new ArrayList<>();
        for (ActivityInfo activityInfo : activities) {
            if (activity.equals(activityInfo.getActivityName())) {
                invidualActivitySchedules.add(getActivitySchedule(activityInfo.getActivityName(), activityInfo.getOrder()));
            }
        }
        return invidualActivitySchedules;
    }

    private static Map<String, Object> getActivitySchedule(final String activityName, final int activityOrder) {
        final Map<String, Object> activitySchedule = new HashMap<>();
        activitySchedule.put(ShmConstants.ACTIVITYNAME, activityName);
        activitySchedule.put(ShmConstants.EXECUTION_MODE, ExecMode.IMMEDIATE.name());
        activitySchedule.put(ShmConstants.ORDER, activityOrder);
        return activitySchedule;
    }

}
