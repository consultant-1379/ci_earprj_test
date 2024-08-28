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

import static com.ericsson.oss.services.shm.jobs.common.constants.JobSchedulerConstants.MAIN_SCHEDULE;
import static com.ericsson.oss.services.shm.jobs.common.constants.JobSchedulerConstants.NAME;
import static com.ericsson.oss.services.shm.jobs.common.constants.JobSchedulerConstants.SCHEDULE_ATTRIBUTES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.TimeSpendOnJob;
import com.ericsson.oss.services.shm.job.entities.JobInput;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.entities.SHMMainJobDto;
import com.ericsson.oss.services.shm.shared.constants.PeriodicSchedulerConstants;

public class JobsReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobsReader.class);

    @Inject
    private MainJobDetailsReaderRetryProxy mainJobDetailsRetryProxy;

    @Inject
    private JobParameterChangeListener jobParameterChangeListener;

    @Inject
    private SystemRecorder systemRecorder;

    public List<SHMMainJobDto> getSHMJobData(final JobInput jobInput) {
        final StringBuilder timeConsumptionForJobRetrieval = new StringBuilder(ShmConstants.MAIN_JOB_RETRIEVAL);
        final Map<Long, Map<String, Object>> shmMainJobs = new HashMap<>();

        final Date jobTemplateRetrievalStartTime = new Date();
        final Map<Long, Map<String, Object>> shmTemplates = mainJobDetailsRetryProxy.getTemplates(jobInput);
        final List<Long> shmTemplateIds = new ArrayList<>(shmTemplates.keySet());
        final Date jobTemplateRetrievalEndTime = new Date();

        final List<List<Long>> batchedShmTemplateIds = ListUtils.partition(shmTemplateIds, jobParameterChangeListener.getJobBatchSize());
        int countOfBatch = 0;
        final Date mainJobRetrievalStartTime = new Date();
        for (final List<Long> eachBatchOfTemplatePoIds : batchedShmTemplateIds) {
            countOfBatch++;
            shmMainJobs.putAll(mainJobDetailsRetryProxy.getMainJobs(eachBatchOfTemplatePoIds));
        }
        final Date mainJobRetrievalEndTime = new Date();

        timeConsumptionForJobRetrieval
                .append(String.format(ShmConstants.TIME_CONSUMPTION_FOR_MAIN_JOBS_RETRIEVAL, countOfBatch, TimeSpendOnJob.getDifference(mainJobRetrievalEndTime, mainJobRetrievalStartTime)));
        LOGGER.info("Under SHM category, there are {} JobTemplates, {} Main jobs present in the system", shmTemplates.size(), shmMainJobs.size());
        systemRecorder.recordEvent(ShmConstants.JOB_RETRIEVAL_EVENT_TYPE, EventLevel.DETAILED, ShmConstants.SOURCE_FOR_JOBS_RETRIEVAL, ShmConstants.RESOURCE_FOR_JOBS_RETRIEVAL,
                timeConsumptionForJobRetrieval
                        .insert(0, String.format(ShmConstants.TIME_CONSUMPTION_FOR_JOBS_RETRIEVAL, TimeSpendOnJob.getDifference(jobTemplateRetrievalEndTime, jobTemplateRetrievalStartTime)))
                        .toString());
        return generateSHMJobDataDtos(shmTemplates, shmMainJobs);
    }

    private List<SHMMainJobDto> generateSHMJobDataDtos(final Map<Long, Map<String, Object>> shmTemplates, final Map<Long, Map<String, Object>> shmMainJobs) {
        final List<SHMMainJobDto> jobDataDtos = new ArrayList<>();

        for (Entry<Long, Map<String, Object>> entrySet : shmMainJobs.entrySet()) {
            final Map<String, Object> mainJob = entrySet.getValue();

            final Map<String, Object> template = shmTemplates.get(mainJob.get(ShmConstants.JOBTEMPLATEID));

            setPeriodicState(template);

            jobDataDtos.add(new SHMMainJobDto(template, mainJob));
        }

        return jobDataDtos;
    }

    private void setPeriodicState(final Map<String, Object> template) {
        final List<Map<String, Object>> scheduleAttributes = getScheduleAttributes(template);
        if (scheduleAttributes != null) {
            for (Map<String, Object> scheduleProperty : scheduleAttributes) {
                final String propertyName = (String) scheduleProperty.get(NAME);
                if (ShmConstants.REPEAT_COUNT.equals(propertyName)) {
                    template.put(ShmConstants.PERIODIC, Boolean.TRUE);
                    LOGGER.debug("This job is scheduled to execute periodically");
                }
                if (PeriodicSchedulerConstants.CRON_EXP.equals(propertyName)) {
                    template.put(ShmConstants.PERIODIC, Boolean.TRUE);
                    LOGGER.debug("This job is scheduled to execute periodically");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getScheduleAttributes(final Map<String, Object> template) {
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) template.get(ShmConstants.JOBCONFIGURATIONDETAILS);
        if (jobConfigurationDetails != null) {
            final Map<String, Object> mainSchedule = (Map<String, Object>) jobConfigurationDetails.get(MAIN_SCHEDULE);

            if (mainSchedule != null && ExecMode.SCHEDULED.getMode().equals(mainSchedule.get(ShmConstants.EXECUTION_MODE))) {
                return (List<Map<String, Object>>) mainSchedule.get(SCHEDULE_ATTRIBUTES);
            }
        }
        return Collections.emptyList();
    }
    
    public Map<String, Object> getMainJob(final long poId) {
        return mainJobDetailsRetryProxy.getMainJob(poId);
    }
}
