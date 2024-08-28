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
package com.ericsson.oss.services.shm.job.service;

import static com.ericsson.oss.services.shm.jobs.common.constants.JobSchedulerConstants.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobParamMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NEInfo;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NEJobProperty;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ScheduleProperty;
import com.ericsson.oss.services.shm.jobs.common.modelentities.SelectedNEInfo;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;
import com.ericsson.oss.services.shm.jobs.common.restentities.RestJobConfigurationData;
import com.ericsson.oss.services.shm.jobs.common.restentities.ScheduleJobConfiguration;

public class JobConfigurationDetailServiceImpl implements JobConfigurationDetailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobConfigurationDetailServiceImpl.class);

    @Inject
    private SHMJobService shmJobService;

    @Inject
    JobParamMapper jobParamMapper;

    @Inject
    private JobConfigurationSummaryFactory jobConfigurationSummaryFactory;

    @Inject
    private JobCapabilityProvider jobCapabilityProvider;

    @Override
    public RestJobConfigurationData getJobConfigurationDetails(final JobTemplate jobTemplate) {
        String startTime = "";
        String mode = null;
        final String jobName = jobTemplate.getName();
        final String description = jobTemplate.getDescription();
        final Date createdOn = jobTemplate.getCreationTime();
        final String owner = jobTemplate.getOwner();
        final JobConfiguration jobConfiguration = jobTemplate.getJobConfigurationDetails();
        final NEInfo neInfo = jobConfiguration.getSelectedNEs();

        final String jobCreatedTime = String.valueOf(createdOn.getTime());

        final ScheduleJobConfiguration scheduleJobParameters = new ScheduleJobConfiguration();
        final Schedule mainSchedule = jobConfiguration.getMainSchedule();
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

        final String jobStartTime = getStartDate(scheduleJobParameters.getStartDate(), startTime);

        /*
         * JobConfigurationSummaryProvider has been introduced to provide different implementations to get target based on JobType. Currently NFVO is not being treated as NetworkElement in ENM. So to
         * treat NFVO as NetworkElement and fit the solution in existing SHM job framework, separate implementation has been provided.
         *
         * By default for all JobTypes, 'DefaultJobConfigurationDetailsProvider' is the job configuration provider and only for Onboard job OnboardJobConfigurationDetailsProvider is the job
         * configuration provider.
         */
        final JobType jobType = jobTemplate.getJobType();
        final JobConfigurationSummary jobConfigurationSummaryProvider = jobConfigurationSummaryFactory.getJobConfigurationSummaryProvider(jobType.getJobTypeName());
        final List<JobConfigurationDetails> jobConfigurationDetails = jobConfigurationSummaryProvider.getJobConfigurationDetails(jobTemplate, jobConfiguration);

        final List<NetworkElement> networkElements = jobConfigurationSummaryProvider.getNetworkElementsByNeNames(neInfo.getNeNames(),
                jobCapabilityProvider.getCapability(JobTypeEnum.getJobType(jobType.name())));

        jobParamMapper.sortJobConfigurationDetails(jobConfigurationDetails);
        final List<NEJobProperty> neJobProperties = jobConfiguration.getNeJobProperties();

        final int skippedNeCount = shmJobService.getSkippedNeJobCount(jobTemplate.getJobTemplateId());

        final SelectedNEInfo selectedNEs = new SelectedNEInfo(networkElements, neInfo);
        final RestJobConfigurationData restJobConfigurationData = new RestJobConfigurationData(jobName, description, jobCreatedTime, jobTemplate.getJobType().name(), jobStartTime, mode,
                jobConfigurationDetails, neJobProperties, owner, selectedNEs, neInfo.getNeNames(), skippedNeCount, scheduleJobParameters);

        LOGGER.debug("DomainData mapped to RestData {} ", restJobConfigurationData);
        LOGGER.debug("NeJobConfigurationDetails : {} for the jobType: {}", jobConfigurationDetails.size(), jobTemplate.getJobType());
        return restJobConfigurationData;
    }

    private String getStartDate(final String startDateFromScheduleParams, final String startTime) {
        String parsedDate = "";
        if (startDateFromScheduleParams != null) {

            parsedDate = startDateFromScheduleParams;

        } else if (startTime != null) {
            parsedDate = startTime;
        }
        LOGGER.debug("Parsed date is {}", parsedDate);
        return parsedDate;
    }

    private void setScheduleAttributes(final List<ScheduleProperty> scheduleAttributes, final ScheduleJobConfiguration scheduleJobParameters) {
        for (final ScheduleProperty scheduleProperty : scheduleAttributes) {
            final String propertyName = scheduleProperty.getName();
            LOGGER.info("Schedule property is {}", propertyName);
            // changing it to switch case as there are many properties to be
            // checked and set
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
                scheduleJobParameters.setStartDate(getFormattedDate(scheduleProperty.getValue()));
                break;
            case END_DATE:
                scheduleJobParameters.setEndDate(getFormattedDate(scheduleProperty.getValue()));
                break;
            case CRON_EXP:
                scheduleJobParameters.setCronExpression(scheduleProperty.getValue());
                break;
            default:
                break;
            }

        }
        LOGGER.debug("ScheduleJobParameters : {}", scheduleJobParameters);

    }

    private String getFormattedDate(final String time) {
        String parsedDate = "";
        try {
            final String delims = " ";
            if (time != null) {
                final StringTokenizer st = new StringTokenizer(time, delims);
                final String formattedScheduleTime = st.nextToken() + " " + st.nextToken();
                LOGGER.debug("formattedScheduleTime :{}", formattedScheduleTime);

                final Date formattedTime = getFormattedTime(formattedScheduleTime);

                if (formattedTime != null) {
                    parsedDate = String.valueOf(formattedTime.getTime());
                } else {
                    parsedDate = "";
                }
            }
            LOGGER.debug("parsedDate:{}", parsedDate);
        } catch (final StringIndexOutOfBoundsException e) {
            LOGGER.error("StartTime does not contain Time Zone info. StartTime : {},  Details are {}", time, e);
        }
        return parsedDate;
    }

    private Date getFormattedTime(final String formattedScheduleTime) {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date formattedTime = new Date();
        try {
            formattedTime = formatter.parse(formattedScheduleTime);
            LOGGER.debug("startTime or endTime :{}", formattedTime);
        } catch (final ParseException e) {
            LOGGER.error("Cannot parse date. due to :", e);
        }
        return formattedTime;
    }

}
