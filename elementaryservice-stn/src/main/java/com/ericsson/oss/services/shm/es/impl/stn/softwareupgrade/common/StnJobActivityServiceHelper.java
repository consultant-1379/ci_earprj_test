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
package com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * Service to retrieve, process and update job properties for STN jobs
 * 
 * @author xsamven
 */

public class StnJobActivityServiceHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(StnJobActivityServiceHelper.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    /**
     * @param propertyName
     * @param activityJobProperties
     * @return
     */
    public Object getJobPropertyValue(final String propertyName, final List<Map<String, Object>> activityJobProperties) {
        Object propertyValue = null;
        LOGGER.debug("Retrieving jobProperty {} from activity job properties {}", propertyName, activityJobProperties);
        if (activityJobProperties != null) {
            for (final Map<String, Object> jobProperty : activityJobProperties) {
                if (propertyName.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    propertyValue = jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                    break;
                }
            }
        }
        LOGGER.debug("Resolved jobProperty value of {} : {}", propertyName, propertyValue);
        return propertyValue;
    }

    /**
     * Method to build job property
     * 
     * @param key
     * @param value
     * @return
     */
    public Map<String, Object> buildJobProperty(final String key, final Object value) {
        LOGGER.debug("Building job property with key: {}, value: {}", key, value);
        final Map<String, Object> jobProperty = new HashMap<>(2);
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, key);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, value);
        return jobProperty;
    }

    /**
     * Method to get the MainJobAttributes
     * 
     * @param jobContext
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMainJobAttributes(final JobEnvironment jobContext) {
        final Map<String, Object> mainJobAttributes = jobContext.getMainJobAttributes();
        return (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
    }

    /**
     * Method to get the NeJobAttributes
     * 
     * @param jobContext
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getNeJobAttributes(final JobEnvironment jobContext) {
        final Map<String, Object> neJobAttributes = jobContext.getNeJobAttributes();
        return (List<Map<String, Object>>) neJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
    }

    /**
     * Method to get the ActivityJobAttributes
     * 
     * @param jobContext
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getActivityJobAttributes(final JobEnvironment jobContext) {
        final Map<String, Object> activityJobAttributes = jobContext.getActivityJobAttributes();
        return (List<Map<String, Object>>) activityJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
    }

    /**
     * Method to send notification to work flow service and update job attributes when the activity is failed
     * 
     * @param activityJobId
     * @param jobLogs
     * @param jobContext
     * @param activity
     * @param processVariables
     */
    @SuppressWarnings("deprecation")
    public void failActivity(final long activityJobId, final List<Map<String, Object>> jobLogs, final JobEnvironment jobContext, final String activity,
        final Map<String, Object> processVariables) {
        LOGGER.debug("ActivityJob ID - [{}] : Sending failed notification for activity {} to work flow service and  updating job properties", activityJobId, activity);
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        activityUtils.sendNotificationToWFS(jobContext, activityJobId, activity, processVariables);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs);
    }

}
