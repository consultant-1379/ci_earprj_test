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
package com.ericsson.oss.services.shm.shared.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ScheduleProperty;
import com.ericsson.oss.services.shm.shared.constants.PeriodicSchedulerConstants;

/**
 * This class is used for setting process variables of schedule jobs.
 * 
 * @author xnitpar
 * 
 */
public class ProcessVariablesUtil {
    private static final String DATE_FORMAT_WITH_TIME = "yyyy-MM-dd HH:mm:ss";

    private static final Logger logger = LoggerFactory.getLogger(ProcessVariablesUtil.class);

    public Map<String, Object> setProcessVariablesForSchedule(final List<ScheduleProperty> schedulePropertyList, final Map<String, Object> processVariables) {
        for (final ScheduleProperty scheduleProperty : schedulePropertyList) {
            if (PeriodicSchedulerConstants.START_DATE.equalsIgnoreCase(scheduleProperty.getName())) {
                final Object startTime = scheduleProperty.getValue();
                logger.info("Schedule Job startDate {} for JobType {} from JSON :", startTime, processVariables.get(ShmConstants.JOB_TYPE));
                if (startTime != null) {
                    processVariables.put("startTime", formatTime((String) startTime));
                }
            } else if (PeriodicSchedulerConstants.END_DATE.equalsIgnoreCase(scheduleProperty.getName())) {
                final Object endTime = scheduleProperty.getValue();
                if (endTime != null) {
                    processVariables.put("endTime", formatTime((String) endTime));
                }
            } else if (PeriodicSchedulerConstants.OCCURRENCES.equalsIgnoreCase(scheduleProperty.getName())) {
                processVariables.put("occurences", parseValue(scheduleProperty.getValue()));
            } else if (PeriodicSchedulerConstants.REPEAT_TYPE.equalsIgnoreCase(scheduleProperty.getName())) {
                processVariables.put("repeatType", scheduleProperty.getValue());
                processVariables.put("periodic", true);
            } else if (PeriodicSchedulerConstants.REPEAT_COUNT.equalsIgnoreCase(scheduleProperty.getName())) {
                processVariables.put("repeatEvery", parseValue(scheduleProperty.getValue()));
            } else if (PeriodicSchedulerConstants.REPEAT_ON.equalsIgnoreCase(scheduleProperty.getName())) {
                processVariables.put("repeatOn", parseDaysInWeek(scheduleProperty.getValue()));
            } else if (PeriodicSchedulerConstants.CRON_EXP.equalsIgnoreCase(scheduleProperty.getName())) {
                logger.debug("Setting crop expression {}", scheduleProperty.getValue());
                processVariables.put(PeriodicSchedulerConstants.CRON_EXP, scheduleProperty.getValue());
                processVariables.put("periodic", true);
            }
        }
        return processVariables;
    }

    private int parseValue(final String value) {
        int parsedValue = 0;
        try {
            parsedValue = Integer.parseInt(value);
        } catch (final NumberFormatException nfEx) {
            logger.error("Entered periodic scheduling value is not a number");
            throw new IllegalArgumentException("Entered periodic scheduling value is not a number");
        } catch (final Exception e) {
            logger.error("Entered periodic scheduling value is not a number");
            throw new IllegalArgumentException("Entered periodic scheduling value is not a number");
        }
        return parsedValue;
    }

    private Date formatTime(final String startTime) {
        Date parsedstartDate = new Date();
        final String formattedStartTime = getFormattedTime(startTime);
        try {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_WITH_TIME);
            parsedstartDate = simpleDateFormat.parse(formattedStartTime);
        } catch (final ParseException parseEx) {
            logger.error("Unable to parse data due to:{}", parseEx.getMessage());
        } catch (Exception e) {
            logger.error("Unable to parse data due to:{}", e.getMessage());
        }
        logger.info("Schedule Job startDate {} ", parsedstartDate);
        return parsedstartDate;
    }

    private String[] parseDaysInWeek(final String repeatOn) {
        String[] days = new String[] {};
        if (repeatOn.contains(",")) {
            days = repeatOn.split(",");
        } else {
            days = new String[] { repeatOn };
        }
        return days;
    }

    private String getFormattedTime(final String startTime) {
        final String delims = " ";
        final StringTokenizer st = new StringTokenizer(startTime, delims);
        final String formattedScheduleTime = st.nextToken() + " " + st.nextToken();
        logger.debug("formattedScheduleTime {}", formattedScheduleTime);
        return formattedScheduleTime;
    }

}
