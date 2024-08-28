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
package com.ericsson.oss.services.shm.job.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Activity;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobProperty;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NeTypeJobProperty;
import com.ericsson.oss.services.shm.jobs.common.modelentities.PlatformJobProperty;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ScheduleProperty;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * 
 * @author tcsgusw
 * 
 */
public class ActivityParamMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityParamMapper.class);

    private List<JobProperty> mapActivitiesWithActivityParams(final List<JobProperty> neTypeJobProperties, final List<String> activityNames, final List<String> activitySpecificParamNames) {

        final List<JobProperty> activitySpecificParameters = new ArrayList<JobProperty>();

        for (final JobProperty jobProperty : neTypeJobProperties) {
            if (activityNames.contains(jobProperty.getKey())) {
                activitySpecificParameters.add(jobProperty);
                activitySpecificParamNames.add(jobProperty.getKey());
            }
        }

        return activitySpecificParameters;
    }

    public JobConfigurationDetails getJobConfigurationDetails(final JobConfiguration jobConfigurationDetails, final String neType, final String platformType,
            final Map<String, List<String>> acitvityParameters) {

        final List<String> activitySpecificParamNames = new ArrayList<String>();
        final List<ActivityInfo> activityInfos = new ArrayList<ActivityInfo>();

        final Map<String, List<JobProperty>> neTypeJobProperties = groupNetypeJobPropertiesBasedOnNeType(jobConfigurationDetails);
        final Map<String, List<Activity>> activitiesByNeType = groupActivitiesBasedOnNeType(jobConfigurationDetails.getActivities());
        List<JobProperty> activitySpecificParameters = null;
        final List<Activity> activities = activitiesByNeType.get(neType);
        if (activities != null) {
            for (final Activity activity : activities) {
                activitySpecificParameters = new ArrayList<JobProperty>();
                if (acitvityParameters.containsKey(activity.getName()) && !acitvityParameters.get(activity.getName()).isEmpty()) {
                    final List<String> activityNames = acitvityParameters.get(activity.getName());
                    activitySpecificParameters.addAll(mapActivitiesWithActivityParams(neTypeJobProperties.get(neType), activityNames, activitySpecificParamNames));
                }

                final ActivityInfo activityInfo = new ActivityInfo(activity.getName(), activity.getSchedule().getExecMode().getMode(), activity.getOrder(), getActivityScheduledTime(activity),
                        activitySpecificParameters);
                activityInfos.add(activityInfo);
                LOGGER.debug("Activity specific parameter names : {} for the activity : {} and neType: {}", activitySpecificParamNames, activity.getName(), neType);
            }
        }
        final List<JobProperty> neTypeSpcificJobParams = getNeTypeSpecificJobProperties(jobConfigurationDetails, platformType, neTypeJobProperties.get(neType), activitySpecificParamNames);

        final JobConfigurationDetails neJobConfigurationDetails = new JobConfigurationDetails();

        neJobConfigurationDetails.setActivityInfoList(activityInfos);
        neJobConfigurationDetails.setJobProperties(neTypeSpcificJobParams);
        neJobConfigurationDetails.setNeType(neType);
        return neJobConfigurationDetails;
    }

    private String getActivityScheduledTime(final Activity activity) {
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

    public Map<String, List<JobProperty>> groupNetypeJobPropertiesBasedOnNeType(final JobConfiguration jobConfigurationDetails) {
        final Map<String, List<JobProperty>> neTypeJobProperties = new HashMap<String, List<JobProperty>>();
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

    private Map<String, List<JobProperty>> groupPlatformJobPropertiesBasedOnPlatform(final JobConfiguration jobConfigurationDetails) {
        final Map<String, List<JobProperty>> platformjobProperties = new HashMap<String, List<JobProperty>>();
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

    private List<JobProperty> getNeTypeSpecificJobProperties(final JobConfiguration jobConfigurationDetails, final String platformType, final List<JobProperty> jobProperties,
            final List<String> activitySpecificParamNames) {

        final List<JobProperty> neTypeSpcificJobParams = new ArrayList<JobProperty>();
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

        if (platformTypeJobProperties != null && !platformTypeJobProperties.isEmpty()) {
            neTypeSpcificJobParams.addAll(platformTypeJobProperties.get(platformType));
        }

        return neTypeSpcificJobParams;
    }

    private Map<String, List<Activity>> groupActivitiesBasedOnNeType(final List<Activity> allActivities) {
        final Map<String, List<Activity>> activitiesByNeType = new HashMap<String, List<Activity>>();
        List<Activity> activities = new ArrayList<Activity>();

        for (final Activity eachActivity : allActivities) {

            String neType = eachActivity.getNeType();
            /**
             * Backward compatibility for the already existing jobs, not having neType
             */
            if (neType == null) {
                neType = "ERBS";
            }
            if (!activitiesByNeType.containsKey(neType)) {

                activities = new ArrayList<Activity>();
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

    public Set<String> getNeTypes(final List<Activity> activities) {

        final Set<String> neTypes = new HashSet<String>();
        String neType = null;
        for (final Activity activity : activities) {
            neType = activity.getNeType();
            /**
             * Backward compatibility for the already existing jobs, not having neType
             */
            if (neType == null) {
                neType = "ERBS";
            }
            neTypes.add(neType);
        }
        return neTypes;

    }

}
