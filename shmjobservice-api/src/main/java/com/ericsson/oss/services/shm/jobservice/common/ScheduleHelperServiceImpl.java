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
package com.ericsson.oss.services.shm.jobservice.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.job.constants.SchedulePropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

public class ScheduleHelperServiceImpl implements ScheduleHelperService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleHelperServiceImpl.class);

    /**
     * @param schedule
     * @param listOfActivitySchedules
     * @param platformType
     */
    @SuppressWarnings("unchecked")
    @Override
    public void buildActivitySchedulesMap(final Map<String, Object> schedule, final List<Map<String, Object>> listOfActivitySchedules) {
        final String platformType = (String) schedule.get(ShmConstants.PLATEFORM_TYPE);
        final List<Map<String, Object>> activities = (List<Map<String, Object>>) schedule.get(ShmConstants.VALUE);
        logger.debug("platformType {} and activities {}", platformType, activities);
        for (Map<String, Object> activityValueMap : activities) {
            final String neType = (String) activityValueMap.get(ShmConstants.NETYPE);
            final List<Map<String, Object>> activityList = (List<Map<String, Object>>) activityValueMap.get(ShmConstants.VALUE);
            logger.debug("NeType {} activitiesMap {} ", neType, activityList);
            for (Map<String, Object> activitiesMap : activityList) {
                if (activitiesMap != null) {
                    final Map<String, Object> schedules = new HashMap<String, Object>();
                    int defaultOrder = 0;
                    final Map<String, Object> mapOfActivitySchedules = new HashMap<String, Object>();
                    mapOfActivitySchedules.put(ShmConstants.NAME, activitiesMap.get(ShmConstants.ACTIVITYNAME).toString());
                    mapOfActivitySchedules.put(ShmConstants.PLATFORM, platformType);
                    mapOfActivitySchedules.put(ShmConstants.NETYPE, neType);
                    schedules.put(ShmConstants.EXECUTION_MODE, activitiesMap.get(ShmConstants.EXECUTION_MODE));
                    populateScheduleAttributes(activitiesMap, schedules);
                    mapOfActivitySchedules.put("schedule", schedules);
                    if (activitiesMap.get(ShmConstants.ORDER) != null) {
                        defaultOrder = (int) activitiesMap.get(ShmConstants.ORDER);
                    }
                    mapOfActivitySchedules.put(ShmConstants.ORDER, defaultOrder);
                    listOfActivitySchedules.add(mapOfActivitySchedules);
                }
            }
        }
        logger.debug("ListOfActivitySchedules {}", listOfActivitySchedules);
    }

    private void populateScheduleAttributes(final Map<String, Object> schedule, final Map<String, Object> scheduleAttributesMap) {

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> scheduleAttributes = (List<Map<String, Object>>) schedule.get("scheduleAttributes");
        final List<Map<String, Object>> ListOfScheduleProperties = new ArrayList<Map<String, Object>>();

        if (scheduleAttributes != null && !scheduleAttributes.isEmpty()) {
            for (int i = 0; i < scheduleAttributes.size(); i++) {

                final Map<String, Object> scheduleProperties = new HashMap<String, Object>();
                final String propertyName = scheduleAttributes.get(i).get(SchedulePropertyConstants.NAME).toString();
                final String propertyValue = scheduleAttributes.get(i).get(SchedulePropertyConstants.VALUE).toString();

                scheduleProperties.put(SchedulePropertyConstants.NAME, propertyName);
                scheduleProperties.put(SchedulePropertyConstants.VALUE, propertyValue);

                ListOfScheduleProperties.add(scheduleProperties);
            }
            scheduleAttributesMap.put(SchedulePropertyConstants.SCHEDULE_ATTRIBUTES, ListOfScheduleProperties);
        }

    }

}
