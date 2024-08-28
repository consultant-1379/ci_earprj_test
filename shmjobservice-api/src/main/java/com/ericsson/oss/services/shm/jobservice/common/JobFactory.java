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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.UserContextBean;
import com.ericsson.oss.services.shm.job.constants.SchedulePropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;

public class JobFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    ScheduleHelperService scheduleHelperService;

    @Inject
    UserContextBean userContextBean;

    /**
     * @param createJobInfo
     * @return Job
     */
    public JobInfo createJob(final JobInfo jobInfo) {
        logger.debug("Start of JobFactory:createJob()");
        final JobInfo mainJob = new JobInfo();
        String description = null;
        String owner = null;
        JobTypeEnum jobType = null;
        String name = null;
        JobCategory jobCategory = null;
        List<Map<String, Object>> activitySchedules = new ArrayList<Map<String, Object>>();
        Map<String, Object> mainJobSchedule = new HashMap<String, Object>();

        if (jobInfo.getDescription() != null) {
            description = jobInfo.getDescription();
        }

        if (jobInfo.getJobType() != null && jobInfo.getName() != null && jobInfo.getJobCategory() != null) {
            jobType = jobInfo.getJobType();
            name = jobInfo.getName();
            jobCategory = jobInfo.getJobCategory();
        }

        if (userContextBean.getLoggedInUserName() != null) {
            owner = userContextBean.getLoggedInUserName();
        }

        if (jobInfo.getActivitySchedules() != null) {
            activitySchedules = jobInfo.getActivitySchedules();
        }

        final List<Map<String, Object>> listOfActivitySchedules = new ArrayList<Map<String, Object>>();

        retrieveMapOfSchedules(activitySchedules, listOfActivitySchedules);
        logger.debug("map of schedules is retrieved");

        if (jobInfo.getMainSchedule() != null) {
            mainJobSchedule = jobInfo.getMainSchedule();
        }

        final Map<String, Object> mainJobScheduleConfiguration = new HashMap<String, Object>();

        if (mainJobSchedule != null && !mainJobSchedule.isEmpty()) {
            populateMainScheduleAttributes(mainJobSchedule, mainJobScheduleConfiguration);
            logger.debug("main schedule attributes are populated");
            mainJobScheduleConfiguration.put("execMode", mainJobSchedule.get("execMode"));

        }

        mainJob.setName(name);
        mainJob.setJobType(jobType);
        mainJob.setOwner(owner);
        mainJob.setDescription(description);
        mainJob.setActivitySchedules(listOfActivitySchedules);
        mainJob.setMainSchedule(mainJobScheduleConfiguration);
        mainJob.setJobCategory(jobCategory);

        return mainJob;
    }

    /**
     * @param activitySchedules
     * @param listOfActivitySchedules
     */
    private void retrieveMapOfSchedules(final List<Map<String, Object>> activitySchedules, final List<Map<String, Object>> listOfActivitySchedules) {
        if (activitySchedules != null && !activitySchedules.isEmpty()) {
            logger.debug("Retrieving activity schedules: {} activitySchedules {}", listOfActivitySchedules, activitySchedules);

            for (int i = 0; i < activitySchedules.size(); i++) {
                final Map<String, Object> schedule = activitySchedules.get(i);
                if (schedule != null) {
                    scheduleHelperService.buildActivitySchedulesMap(schedule, listOfActivitySchedules);

                }
            }
        }
    }

    /**
     * @param schedule
     * @param scheduleAttributesMap
     */
    private void populateMainScheduleAttributes(final Map<String, Object> schedule, final Map<String, Object> scheduleAttributesMap) {
        logger.debug("Start of JobFactory:populateMainScheduleAttributes");
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> scheduleAttributes = (List<Map<String, Object>>) schedule.get(SchedulePropertyConstants.SCHEDULE_ATTRIBUTES);
        final List<Map<String, Object>> ListOfScheduleProperties = new ArrayList<Map<String, Object>>();

        if (scheduleAttributes != null && !scheduleAttributes.isEmpty()) {
            logger.debug("Populating main schedule attributes");
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
