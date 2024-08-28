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
package com.ericsson.oss.services.shm.job.service.axe.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.job.utils.SHMJobUtil;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.*;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * Activity Mapper class to map AXE up activityparams persisting in NeTypeActivityJobProperties with activity names
 */

public class AxeActivityParamsMapper {

    public static final Logger LOGGER = LoggerFactory.getLogger(AxeActivityParamsMapper.class);

    @SuppressWarnings("unchecked")
    public JobConfigurationDetails getJobConfigurationDetails(final JobConfiguration jobConfigurationDetails, final String neType, final String platformType) {

        final List<String> activitySpecificParamNames = new ArrayList<>();
        final List<ActivityInfo> activityInfos = new ArrayList<>();

        final Map<String, List<JobProperty>> neTypeJobProperties = groupNetypeJobPropertiesBasedOnNeType(jobConfigurationDetails);
        final Map<String, List<Activity>> activitiesByNeType = groupActivitiesBasedOnNeType(jobConfigurationDetails.getActivities());

        LOGGER.debug("jobConfigurationDetails.getNeTypeActivityJobProperties() {}", jobConfigurationDetails.getNeTypeActivityJobProperties());
        for (final NeTypeActivityJobProperties neTypeSpecificActivities : jobConfigurationDetails.getNeTypeActivityJobProperties()) {
            final List<Activity> activities = activitiesByNeType.get(neTypeSpecificActivities.getNeType());
            if (activities != null) {
                for (final Activity activity : activities) {
                    prepareActivitySpecificparams(neType, activitySpecificParamNames, activityInfos, neTypeSpecificActivities, activity);
                }
            }
        }
        final List<JobProperty> neTypeSpcificJobParams = getNeTypeSpecificJobProperties(jobConfigurationDetails, platformType, neTypeJobProperties.get(neType), activitySpecificParamNames);

        final JobConfigurationDetails neJobConfigurationDetails = new JobConfigurationDetails();

        neJobConfigurationDetails.setActivityInfoList(activityInfos);
        neJobConfigurationDetails.setJobProperties(neTypeSpcificJobParams);
        neJobConfigurationDetails.setNeType(neType);
        return neJobConfigurationDetails;
    }

    /**
     * @param neType
     * @param activitySpecificParamNames
     * @param activityInfos
     * @param neTypeSpecificActivities
     * @param activity
     */
    private void prepareActivitySpecificparams(final String neType, final List<String> activitySpecificParamNames, final List<ActivityInfo> activityInfos,
            final NeTypeActivityJobProperties neTypeSpecificActivities, final Activity activity) {
        final List<JobProperty> activitySpecificParameters = new ArrayList<>();
        for (final Map<String, Object> eachActivityData : neTypeSpecificActivities.getActivityJobProperties()) {
            if (activity.getName().equals((String) eachActivityData.get(ShmConstants.ACTIVITYNAME)) && activity.getNeType().equals(neType)) {
                activitySpecificParameters.addAll((List<JobProperty>) eachActivityData.get(ShmJobConstants.JOBPROPERTIES));
                final ActivityInfo activityInfo = new ActivityInfo((String) eachActivityData.get(ShmConstants.ACTIVITYNAME), activity.getSchedule().getExecMode().getMode(),
                        activity.getOrder(), getActivityScheduledTime(activity), activitySpecificParameters);
                activityInfos.add(activityInfo);
            }
        }
        LOGGER.debug("Activity specific parameter names : {} for the activity : {} and neType: {}", activitySpecificParamNames, activity.getName(), neType);
    }

    //TODO refactor this class to reuse the methods that are available in ActivityParamMapper class
    public Map<String, List<JobProperty>> groupNetypeJobPropertiesBasedOnNeType(final JobConfiguration jobConfigurationDetails) {
        final Map<String, List<JobProperty>> neTypeJobProperties = new HashMap<>();
        if (jobConfigurationDetails.getNeTypeJobProperties() != null && !jobConfigurationDetails.getNeTypeJobProperties().isEmpty()) {
            for (final NeTypeJobProperty neTypeJobProperty : jobConfigurationDetails.getNeTypeJobProperties()) {
                final String neType = neTypeJobProperty.getNeType();
                if (!neTypeJobProperties.containsKey(neType)) {
                    neTypeJobProperties.put(neType, neTypeJobProperty.getJobProperties());
                } else {
                    final List<JobProperty> jobPropertyList = neTypeJobProperties.get(neType);
                    jobPropertyList.addAll(neTypeJobProperty.getJobProperties());
                    neTypeJobProperties.put(neType, jobPropertyList);

                }
            }
        }
        return neTypeJobProperties;
    }

    public Map<String, List<JobProperty>> groupNeJobPropertiesBasedOnNename(final JobConfiguration jobConfigurationDetails) {
        final Map<String, List<JobProperty>> neJobProperties = new HashMap<>();
        if (jobConfigurationDetails.getNeJobProperties() != null && !jobConfigurationDetails.getNeJobProperties().isEmpty()) {
            for (final NEJobProperty neJobProperty : jobConfigurationDetails.getNeJobProperties()) {
                final String nename = neJobProperty.getNeName();
                if (!neJobProperties.containsKey(nename)) {
                    neJobProperties.put(nename, neJobProperty.getJobProperties());
                } else {
                    final List<JobProperty> jobPropertyList = neJobProperties.get(nename);
                    jobPropertyList.addAll(neJobProperty.getJobProperties());
                    neJobProperties.put(nename, jobPropertyList);

                }
            }
        }
        return neJobProperties;
    }

    protected List<JobProperty> getNeTypeSpecificJobProperties(final JobConfiguration jobConfigurationDetails, final String platformType, final List<JobProperty> jobProperties,
            final List<String> activitySpecificParamNames) {

        final List<JobProperty> neTypeSpcificJobParams = new ArrayList<>();
        final Map<String, List<JobProperty>> platformTypeJobProperties = groupPlatformJobPropertiesBasedOnPlatform(jobConfigurationDetails);
        if (jobProperties != null) {
            for (final JobProperty jobProperty : jobProperties) {
                if (!activitySpecificParamNames.contains(jobProperty.getKey())) {
                    neTypeSpcificJobParams.add(jobProperty);
                    LOGGER.debug("neType specific parameters : {} ", jobProperty.getKey());
                }
            }
        }
        /* Backward compatibility for already existing jobs */
        if (jobConfigurationDetails.getJobProperties() != null && !jobConfigurationDetails.getJobProperties().isEmpty()) {
            neTypeSpcificJobParams.addAll(jobConfigurationDetails.getJobProperties());
        }

        if (!platformTypeJobProperties.isEmpty()) {
            neTypeSpcificJobParams.addAll(platformTypeJobProperties.get(platformType));
        }

        return neTypeSpcificJobParams;
    }

    private Map<String, List<JobProperty>> groupPlatformJobPropertiesBasedOnPlatform(final JobConfiguration jobConfigurationDetails) {
        final Map<String, List<JobProperty>> platformjobProperties = new HashMap<>();
        if (jobConfigurationDetails.getPlatformJobProperties() != null && !jobConfigurationDetails.getPlatformJobProperties().isEmpty()) {
            for (final PlatformJobProperty platformJobProperty : jobConfigurationDetails.getPlatformJobProperties()) {
                final String platform = platformJobProperty.getPlatform();
                if (!platformjobProperties.containsKey(platform)) {
                    platformjobProperties.put(platform, platformJobProperty.getJobProperties());
                } else {
                    final List<JobProperty> jobPropertyList = platformjobProperties.get(platform);
                    jobPropertyList.addAll(platformJobProperty.getJobProperties());
                    platformjobProperties.put(platform, jobPropertyList);
                }
            }
        }
        return platformjobProperties;
    }

    protected Map<String, List<Activity>> groupActivitiesBasedOnNeType(final List<Activity> allActivities) {
        final Map<String, List<Activity>> activitiesByNeType = new HashMap<>();
        List<Activity> activities;

        for (final Activity eachActivity : allActivities) {

            final String neType = eachActivity.getNeType();
            if (!activitiesByNeType.containsKey(neType)) {
                activities = new ArrayList<>();
                activities.add(eachActivity);
            } else {
                activities = activitiesByNeType.get(neType);
                activities.add(eachActivity);
            }
            activitiesByNeType.put(neType, activities);
            LOGGER.debug("Size of activities : {} for the neType : {}", activities.size(), neType);
        }

        return activitiesByNeType;
    }

    protected String getActivityScheduledTime(final Activity activity) {
        final Schedule schedules = activity.getSchedule();
        if (schedules.getExecMode().getMode().equals(ExecMode.SCHEDULED.getMode())) {
            final List<ScheduleProperty> scheduleAttributes = schedules.getScheduleAttributes();
            if (scheduleAttributes != null) {
                for (final ScheduleProperty scheduleProperty : scheduleAttributes) {
                    final String propertyName = scheduleProperty.getName();
                    if (scheduleProperty.getName().equals(ShmConstants.START_DATE) && scheduleProperty.getValue() != null) {
                        return SHMJobUtil.getFormattedDate(scheduleProperty.getValue());
                    }
                    LOGGER.debug("Schedule property is {}", propertyName);
                }
            }
        }

        return null;
    }
}
