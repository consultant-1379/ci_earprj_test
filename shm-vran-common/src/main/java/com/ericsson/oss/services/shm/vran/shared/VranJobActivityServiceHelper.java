/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.vran.shared;

import java.util.*;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;

/**
 * Service to retrieve, process and update job properties for VRAN jobs
 * 
 * @author xindkag
 */

public class VranJobActivityServiceHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(VranJobActivityServiceHelper.class);

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
        LOGGER.trace("Retrieving jobProperty {} from activity job properties {}", propertyName, activityJobProperties);
        if (activityJobProperties != null) {
            for (final Map<String, Object> jobProperty : activityJobProperties) {
                if (propertyName.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    propertyValue = jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                    break;
                }
            }
        }
        LOGGER.trace("Resolved jobProperty value of {} : {}", propertyName, propertyValue);
        return propertyValue;
    }

    public String getJobPropertyValueAsString(final String propertyName, final List<Map<String, Object>> properties) {
        final Object jobPropertyValue = getJobPropertyValue(propertyName, properties);
        return (jobPropertyValue != null ? (String) jobPropertyValue : "");
    }

    public int getJobPropertyValueAsInt(final String propertyName, final List<Map<String, Object>> properties) {
        final Object jobPropertyValue = getJobPropertyValue(propertyName, properties);
        return (jobPropertyValue != null ? Integer.parseInt((String) jobPropertyValue) : -1);
    }

    /**
     * Method to build job property
     * 
     * @param key
     * @param value
     * @return
     */
    public Map<String, Object> buildJobProperty(final String key, final Object value) {
        LOGGER.trace("Building job property with key: {}, value: {}", key, value);
        final Map<String, Object> jobProperty = new HashMap<String, Object>(2);
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
    public Map<String, Object> getMainJobAttributes(final JobEnvironment jobContext) {
        final Map<String, Object> mainJobAttributes = jobContext.getMainJobAttributes();
        final Map<String, Object> mainJobProperties = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        return mainJobProperties;
    }

    /**
     * Method to get the NeJobAttributes
     * 
     * @param jobContext
     * @return
     */
    public List<Map<String, Object>> getNeJobAttributes(final JobEnvironment jobContext) {
        final Map<String, Object> neJobAttributes = jobContext.getNeJobAttributes();
        final List<Map<String, Object>> neJobProperties = (List<Map<String, Object>>) neJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        return neJobProperties;
    }

    /**
     * Method to get the ActivityJobAttributes
     * 
     * @param jobContext
     * @return
     */
    public List<Map<String, Object>> getActivityJobAttributes(final JobEnvironment jobContext) {
        final Map<String, Object> activityJobAttributes = jobContext.getActivityJobAttributes();
        final List<Map<String, Object>> activityJobProperties = (List<Map<String, Object>>) activityJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        return activityJobProperties;
    }

    /**
     * Method to send notification to work flow service and update job attributes when the activity is failed
     * 
     * @param activityJobId
     * @param jobLogs
     * @param businessKey
     * @param jobContext
     * @param activity
     * @param processVariables
     */
    public void failActivity(final long activityJobId, final List<Map<String, Object>> jobLogs, final JobEnvironment jobContext, final String activity, final Map<String, Object> processVariables) {
        LOGGER.debug("ActivityJob ID - [{}] : Sending failed notification for activity {} to work flow service and  updating job properties", activityJobId, activity);
        final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
        activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs);

        activityUtils.sendNotificationToWFS(jobContext, activityJobId, activity, processVariables);
    }

    /**
     * Method to update action triggered job attribute in job properties for the given activity job id.
     * 
     * @param activityJobId
     * @param action
     */
    public void updateActionTiggeredJobAttribute(final long activityJobId, final String action) {
        LOGGER.debug("ActivityJob ID - [{}] : updating actionTriggered with the activity: {}", activityJobId, action);
        final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
        activityUtils.prepareJobPropertyList(jobProperties, VranJobConstants.ACTION_TRIGGERED, action);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, null);
    }

    /**
     * Method to update is activity triggered job attribute in job properties for the given activity job id.
     * 
     * @param activityJobId
     */
    public void updateIsActivityTriggeredJobAttribute(final long activityJobId) {
        LOGGER.debug("ActivityJob ID - [{}] : updating isActivityTriggered job attribute.", activityJobId);
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        jobProperties.add(buildJobProperty(ActivityConstants.IS_ACTIVITY_TRIGGERED, "true"));
        final Map<String, Object> activityJobAttributes = jobContext.getActivityJobAttributes();
        List<Map<String, Object>> neJobProperties = getNeJobAttributes(jobContext);
        final long neJobId = (long) activityJobAttributes.get(ShmConstants.NE_JOB_ID);
        if (neJobProperties == null) {
            neJobProperties = new ArrayList<>();
        }
        neJobProperties.add(buildJobProperty(ActivityConstants.IS_ACTIVITY_TRIGGERED, "true"));
        jobUpdateService.readAndUpdateRunningJobAttributes(neJobId, neJobProperties, null);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, null);
    }

    public String getPropertyFromNETypeJobPropeties(final String propertyName, final Map<String, Object> mainJobProperties, final String neType) {

        return getJobProperty(propertyName, mainJobProperties, ShmJobConstants.NETYPE, neType, ShmJobConstants.NETYPEJOBPROPERTIES);
    }

    public String getPropertyFromNEJobPropeties(final String propertyName, final Map<String, Object> mainJobProperties, final String neName) {

        return getJobProperty(propertyName, mainJobProperties, ShmJobConstants.NE_NAME, neName, ShmJobConstants.NEJOB_PROPERTIES);
    }

    private String getJobProperty(final String propertyName, final Map<String, Object> mainJobProperties, final String propertyType, final String propertyTypeValue, final String jobPropertiesType) {

        String value = null;
        final List<Map<String, Object>> jobProperties = (List<Map<String, Object>>) mainJobProperties.get(jobPropertiesType);
        if (jobProperties != null && !jobProperties.isEmpty()) {

            LOGGER.trace("jobProperties {} ", jobProperties);
            for (final Map<String, Object> jobProperty : jobProperties) {
                if (((String) jobProperty.get(propertyType)).equalsIgnoreCase(propertyTypeValue)) {
                    final List<Map<String, String>> properties = (List<Map<String, String>>) jobProperty.get(ShmJobConstants.JOBPROPERTIES);
                    if (properties != null) {
                        value = extractPropertyValue(propertyName, properties);
                    }
                }
            }
        }
        return value;
    }

    private String extractPropertyValue(final String propertyName, final List<Map<String, String>> properties) {
        String value = null;
        for (final Map<String, String> property : properties) {
            if (propertyName.equals(property.get(ShmJobConstants.KEY))) {
                value = property.get(ShmJobConstants.VALUE);
                LOGGER.trace("Value fetched from JobProperties {} ", value);
                break;
            }
        }
        return value;
    }

    public Map<String, Object> buildJobLog(final String message, final String severity) {

        return new JobLogBuilder().setMessage(message).setTime(new Date()).setSource(JobLogType.SYSTEM.toString()).setServerity(severity).buildJobLog();
    }

    public Map<String, Object> buildJobLog(final String message, final Date date, final String severity) {

        return new JobLogBuilder().setMessage(message).setTime(date).setSource(JobLogType.SYSTEM.toString()).setServerity(severity).buildJobLog();
    }
}
