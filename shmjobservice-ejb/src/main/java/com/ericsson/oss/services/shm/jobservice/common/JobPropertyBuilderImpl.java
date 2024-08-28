package com.ericsson.oss.services.shm.jobservice.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

public class JobPropertyBuilderImpl implements JobPropertyBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobPropertyBuilderImpl.class);

    @Override
    public void populateJobConfiguration(final JobInfo jobInfo, final Map<String, Object> configuration) {

        LOGGER.debug("configuration {}", configuration);
        if (configuration.containsKey(ShmConstants.PLATFORM)) {
            addPlatformJobProperties(jobInfo, configuration);
        }
        if (configuration.containsKey(ShmConstants.NETYPE)) {
            if (configuration.containsKey(ShmConstants.PROPERTIES)) {
                LOGGER.debug("in side properties {}", configuration.containsKey(ShmConstants.PROPERTIES));
                addNeTypeJobProperties(jobInfo, configuration);
            }
        }
        if (configuration.containsKey(ShmConstants.NE_PROPERTIES)) {
            LOGGER.debug("in side neProperties {}", configuration.containsKey(ShmConstants.NE_PROPERTIES));
            addNeJobProperties(jobInfo, configuration);
        }
        if (configuration.containsKey(ShmConstants.NETYPE_ACTIVITYJOBPROPERTIES)) {
            LOGGER.debug("in side activityProperties {}", configuration.containsKey(ShmConstants.NETYPE_ACTIVITYJOBPROPERTIES));
            addNeTypeActivityJobProperties(jobInfo, configuration);
        }
    }

    /**
     * This method add PlatformJobProperties to jobInfo object.
     * 
     * @param jobInfo
     *            , configuration
     * @return void
     */
    @SuppressWarnings("unchecked")
    private void addPlatformJobProperties(final JobInfo jobInfo, final Map<String, Object> configuration) {
        final List<Map<String, String>> jobpropertiesList = (List<Map<String, String>>) configuration.get(JobPropertyConstants.PROPERTIES);
        if (jobpropertiesList != null) {

            final PlatformProperty platformJobProperty = new PlatformProperty();
            platformJobProperty.setPlatform((String) configuration.get("platform"));
            platformJobProperty.setJobProperties(jobpropertiesList);
            jobInfo.addPlatformJobProperty(platformJobProperty);
        }
    }

    /**
     * This method add NeTypeJobProperties to jobInfo object.
     * 
     * @param jobInfo
     *            , configuration
     * @return void
     */
    @SuppressWarnings("unchecked")
    private void addNeTypeJobProperties(final JobInfo jobInfo, final Map<String, Object> configuration) {
        final List<Map<String, String>> neTypeJobpropertiesList = (List<Map<String, String>>) configuration.get(JobPropertyConstants.PROPERTIES);
        if (neTypeJobpropertiesList != null) {

            final NeTypeJobProperty neTypeJobProperty = new NeTypeJobProperty();
            neTypeJobProperty.setNeType((String) configuration.get("neType"));
            neTypeJobProperty.setJobProperties(neTypeJobpropertiesList);
            jobInfo.addNETypeJobProperty(neTypeJobProperty);

        }
    }

    /**
     * This method add NeJobProperties to jobInfo object.
     * 
     * @param jobInfo
     *            , configuration
     * @return void
     */
    @SuppressWarnings("unchecked")
    private void addNeJobProperties(final JobInfo jobInfo, final Map<String, Object> configuration) {
        final List<Map<String, Object>> nePropertyList = (List<Map<String, Object>>) configuration.get(JobPropertyConstants.NE_PROPERTIES);
        LOGGER.info("neProperties List {}", nePropertyList);
        for (final Map<String, Object> neProperties : nePropertyList) {
            final NeJobProperty neJobProperty = new NeJobProperty();
            LOGGER.debug("NE_NAMES {}", neProperties.get(JobPropertyConstants.NE_NAMES));
            neJobProperty.setNeName((String) neProperties.get(JobPropertyConstants.NE_NAMES));
            final List<Map<String, String>> propList = (List<Map<String, String>>) neProperties.get(JobPropertyConstants.PROPERTIES);
            LOGGER.debug("properties List {}", propList);
            neJobProperty.setJobProperties(propList);
            jobInfo.addNeJobProperties(neJobProperty);
        }
        LOGGER.debug("Exit of addNeJobProperties {}", jobInfo);
    }

    /**
     * This method add NeTypeActivityJobProperties to jobInfo object.
     * 
     * @param jobInfo
     *            , configuration
     * @return void
     */
    @SuppressWarnings("unchecked")
    private void addNeTypeActivityJobProperties(final JobInfo jobInfo, final Map<String, Object> configuration) {
        final String neType = (String) configuration.get(ShmConstants.NETYPE);
        final List<Map<String, Object>> activityPropertyList = (List<Map<String, Object>>) configuration.get(ShmConstants.NETYPE_ACTIVITYJOBPROPERTIES);
        final List<Map<String, Object>> allActivitiesInformation = new ArrayList<>();
        final NeTypeActivityJobProperties neTypeActivityProperties = new NeTypeActivityJobProperties();
        neTypeActivityProperties.setNeType(neType);
        for (final Map<String, Object> propList : activityPropertyList) {
            final Map<String, Object> eachActivity = new HashMap<>();
            eachActivity.put(JobPropertyConstants.ACTIVITY_NAME, propList.get(JobPropertyConstants.ACTIVITY_NAME));
            eachActivity.put(ShmConstants.JOBPROPERTIES, (List<Map<String, String>>) propList.get(ShmConstants.PROPERTIES));
            allActivitiesInformation.add(eachActivity);
        }
        neTypeActivityProperties.setActivityJobProperties(allActivitiesInformation);
        jobInfo.addNeTypeActivityJobProperties(neTypeActivityProperties);
        LOGGER.debug("Exit of addActivityProperties {}", jobInfo);
    }
}
