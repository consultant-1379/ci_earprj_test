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
package com.ericsson.oss.services.shm.job.resources;

import static com.ericsson.oss.services.shm.jobs.common.constants.JobSchedulerConstants.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.UnsupportedPlatformException;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.service.SHMJobService;
import com.ericsson.oss.services.shm.job.utils.SHMJobUtil;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobParamMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.*;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobParam;
import com.ericsson.oss.services.shm.jobs.common.restentities.RestJobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.restentities.ScheduleJobConfiguration;

/**
 * Maps the Domain data to UI data
 * 
 */
public class RestDataMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestDataMapper.class);

    @Inject
    private SHMJobService shmJobService;

    @Inject
    private JobParamMapper jobParamMapper;

    @Inject
    private PlatformTypeProviderImpl platformTypeProvider;

    @Inject
    private FdnServiceBean fdnServiceBean;

    @Inject
    private JobCapabilityProvider jobCapabilityProvider;

    /**
     * Maps the domain data to RestJobConfiguration which can be converted to Json.
     * 
     * @param jobTemplate
     * @return RestJobConfiguration
     */
    public RestJobConfiguration mapJobConfigToRestDataFormat(final JobTemplate jobTemplate) {
        final List<JobParam> jobParamsList = new ArrayList<JobParam>();
        String startTime = "";
        String mode = null;
        String formattedString = null;
        final String jobName = jobTemplate.getName();
        final String description = jobTemplate.getDescription();
        final Date createdOn = jobTemplate.getCreationTime();
        final String owner = jobTemplate.getOwner();
        final JobConfiguration jobConfigurationDetails = jobTemplate.getJobConfigurationDetails();
        final NEInfo neInfo = jobConfigurationDetails.getSelectedNEs();

        final String capability = jobCapabilityProvider.getCapability(JobTypeEnum.getJobType(jobTemplate.getJobType().toString()));
        final List<NetworkElement> networkElements = fdnServiceBean.getNetworkElementsByNeNames(neInfo.getNeNames(), capability);
        final SelectedNEInfo selectedNEs = new SelectedNEInfo(networkElements, neInfo);
        String parsedCreationDate = null;
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        parsedCreationDate = formatter.format(createdOn);
        Date jobTemplateCreationDate = null;
        final ScheduleJobConfiguration scheduleJobParameters = new ScheduleJobConfiguration();
        try {
            jobTemplateCreationDate = formatter.parse(parsedCreationDate);
        } catch (final ParseException e) {
            LOGGER.error("Cannot parse date : {}", parsedCreationDate);
        }
        final Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(jobTemplateCreationDate);
        final String sheduledTime = SHMJobUtil.convertTimeZones(calendar.getTimeZone().getID(), TimeZone.getDefault().getID(), jobTemplateCreationDate);
        LOGGER.debug("Scheduled Date:{}", sheduledTime);
        final String formattedScheduledTime = sheduledTime.replace("ZUTC", "");
        Date formattedDate = null;
        try {
            formattedDate = formatter.parse(formattedScheduledTime);
        } catch (final ParseException e) {
            LOGGER.error("Cannot parse date : {}", formattedScheduledTime);
        }
        if (formattedDate != null) {
            formattedString = String.valueOf(formattedDate.getTime());
        } else {
            formattedString = "";
        }
        final Schedule mainSchedule = jobConfigurationDetails.getMainSchedule();
        if (mainSchedule != null) {
            mode = mainSchedule.getExecMode().getMode();
            if (ShmConstants.SCHEDULED.equalsIgnoreCase(mode)) {
                final List<ScheduleProperty> scheduleAttributes = mainSchedule.getScheduleAttributes();
                if (scheduleAttributes != null && !scheduleAttributes.isEmpty()) {
                    setScheduleAttributes(scheduleAttributes, scheduleJobParameters);
                }
            } else {
                startTime = shmJobService.getJobStartTime(jobTemplate.getJobTemplateId());
            }
        }
        String parsedDate = "";
        if (scheduleJobParameters.getStartDate() != null) {
            parsedDate = getFormattedDate(scheduleJobParameters.getStartDate());
        } else if (startTime != null) {
            parsedDate = getFormattedDate(startTime);
        }
        LOGGER.debug("Parsed date is {}", parsedDate);
        final Map<String, List<JobProperty>> neTypeJobProperties = groupNetypeJobPropertiesBasedOnNeType(jobConfigurationDetails);
        final Map<String, List<JobProperty>> platformTypeJobProperties = groupPlatformJobPropertiesBasedOnPlatform(jobConfigurationDetails);
        final Map<String, List<ActivityInfo>> neTypes = groupActivitiesBasedOnNeType(jobConfigurationDetails.getActivities());
        for (final Map.Entry<String, List<ActivityInfo>> entry : neTypes.entrySet()) {
            JobParam jobparam = null;
            final List<ActivityInfo> avtivityInfoList = entry.getValue();
            List<JobProperty> jobProperties = new ArrayList<JobProperty>();
            /* Backward compatibility for already existing jobs */
            if (jobConfigurationDetails.getJobProperties() != null && !jobConfigurationDetails.getJobProperties().isEmpty()) {
                jobProperties = jobConfigurationDetails.getJobProperties();
            }
            if (neTypeJobProperties != null && !jobConfigurationDetails.getNeTypeJobProperties().isEmpty()) {
                jobProperties.addAll(neTypeJobProperties.get(entry.getKey()));
            }
            if (platformTypeJobProperties != null && !platformTypeJobProperties.isEmpty()) {
                try {
                    final PlatformTypeEnum platform = platformTypeProvider.getPlatformTypeBasedOnCapability(entry.getKey(), capability);
                    jobProperties.addAll(platformTypeJobProperties.get(platform.name()));
                } catch (final UnsupportedPlatformException ex) {
                    LOGGER.error("Platform Type {} is not supported by the application", ex);
                }
            }
            jobparam = jobParamMapper.createJobparam(jobProperties, avtivityInfoList);
            if (jobparam != null) {
                jobparam.setNeType(entry.getKey());
            }
            jobParamsList.add(jobparam);
        }
        final RestJobConfiguration jobConfigTobeDisplayed = new RestJobConfiguration(jobName, description, formattedString, jobTemplate.getJobType().name(), parsedDate, mode, jobParamsList, owner,
                selectedNEs, scheduleJobParameters);
        LOGGER.debug("DomainData mapped to RestData {}", jobConfigTobeDisplayed);
        return jobConfigTobeDisplayed;
    }

    private String getFormattedDate(final String time) {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String parsedDate = "";
        try {
            final String delims = " ";
            Date formattedTime1 = new Date();
            final StringTokenizer st = new StringTokenizer(time, delims);
            final String formattedScheduleTime = st.nextToken() + " " + st.nextToken();
            LOGGER.debug("formattedScheduleTime :{}", formattedScheduleTime);
            try {
                formattedTime1 = formatter.parse(formattedScheduleTime);
                LOGGER.debug("startTime or endTime :{}", formattedTime1);
            } catch (final ParseException e) {
                LOGGER.error("Cannot parse date. due to :", e);
            }
            if (formattedTime1 != null) {
                parsedDate = String.valueOf(formattedTime1.getTime());
            } else {
                parsedDate = "";
            }
            LOGGER.debug("parsedDate:{}", parsedDate);
        } catch (final StringIndexOutOfBoundsException e) {
            LOGGER.error("StartTime does not contain Time Zone info. StartTime : {}. Exception is : {}", time, e.getMessage());
        }
        return parsedDate;
    }

    private void setScheduleAttributes(final List<ScheduleProperty> scheduleAttributes, final ScheduleJobConfiguration scheduleJobParameters) {
        for (final ScheduleProperty scheduleProperty : scheduleAttributes) {
            final String propertyName = scheduleProperty.getName();
            LOGGER.debug("Schedule property is {}", propertyName);
            // changing it to switch case as there are many properties to be checked and set
            switch (propertyName) {
            case REPEAT_TYPE:
                scheduleJobParameters.setRepeatType(scheduleProperty.getValue());
                break;
            case REPEAT_COUNT:
                scheduleJobParameters.setRepeatCount(scheduleProperty.getValue());
                break;
            case REPEAT_ON:
                scheduleJobParameters.setRepeatOn(scheduleProperty.getValue());
                break;
            case OCCURRENCES:
                scheduleJobParameters.setOccurences(scheduleProperty.getValue());
                break;
            case START_DATE:
                scheduleJobParameters.setStartDate(scheduleProperty.getValue());
                break;
            case END_DATE:
                scheduleJobParameters.setEndDate(getFormattedDate(scheduleProperty.getValue()));
                break;
            }
        }
        LOGGER.debug("ScheduleJobParameters :{}", scheduleJobParameters);
    }

    /**
     * Method to group the Activities according to their platform type. So each activity will be forwarded to the respective platform Mapper and there it stores into the List.
     * 
     * @param activities
     * @return set of PlatformMappers
     */

    private Map<String, List<ActivityInfo>> groupActivitiesBasedOnNeType(final List<Activity> activities) {
        final Map<String, List<ActivityInfo>> neTypes = new HashMap<String, List<ActivityInfo>>();
        List<ActivityInfo> avtivityInfoList = new ArrayList<ActivityInfo>();
        int order;
        for (final Activity eachActivity : activities) {
            final Schedule schedule = eachActivity.getSchedule();
            final String mode = schedule.getExecMode().getMode();
            order = eachActivity.getOrder();
            final ActivityInfo activityInfo = new ActivityInfo(eachActivity.getName(), mode, order);
            String neType = eachActivity.getNeType();
            /**
             * Backward compatibility for the already existing jobs, not having neType
             */
            if (neType == null || neType.length() == 0) {
                neType = "ERBS";
                avtivityInfoList.add(activityInfo);
            } else {
                if (!neTypes.containsKey(neType)) {
                    avtivityInfoList = new ArrayList<ActivityInfo>();
                    avtivityInfoList.add(activityInfo);
                } else {
                    avtivityInfoList = neTypes.get(neType);
                    avtivityInfoList.add(activityInfo);
                }
            }
            neTypes.put(neType, avtivityInfoList);
        }
        return neTypes;
    }

    private Map<String, List<JobProperty>> groupNetypeJobPropertiesBasedOnNeType(final JobConfiguration jobConfigurationDetails) {
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

}
