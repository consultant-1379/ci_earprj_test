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

package com.ericsson.oss.services.shm.job.remote.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FilterDetails;
import com.ericsson.oss.services.shm.common.enums.FilterOperatorEnum;
import com.ericsson.oss.services.shm.job.entities.JobInput;
import com.ericsson.oss.services.shm.job.entities.JobOutput;
import com.ericsson.oss.services.shm.job.remote.api.JobStatusQuery;
import com.ericsson.oss.services.shm.job.service.SHMJobService;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportData;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobInput;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.shm.job.entities.SHMJobData;

/**
 * Helper to fetch Main Jobs and NE jobs details.
 * 
 * @author xmadupp
 */
public class JobStatusHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStatusHelper.class);
    public static final String JOB_STATUS = "status";
    public static final String CREATED_BY = "createdBy";
    public static final String JOB_NAME = "jobName";
    public static final String JOB_TYPE = "jobType";
    public static final String START_DATE = "startDate";
    public static final String DESC_SORT = "desc";

    @Inject
    private SHMJobService shmJobService;

    /**
     * Method retrieves Main Job details Job Output for requested query.
     * 
     * @param jobStatusQueryParams
     * @return JobOutput
     */
    public JobOutput fetchMainJobStatus(final JobStatusQuery jobStatusQueryParams) {
        final JobInput jobInput = new JobInput();
        jobInput.setLimit(Integer.MAX_VALUE);
        jobInput.setOffset(1);
        String sortBy = JOB_STATUS;

        jobInput.setOrderBy(DESC_SORT);
        JobCategory jobCategory = jobStatusQueryParams.getJobCategory();
        if (jobCategory == null) {
            LOGGER.debug("Found JobCategory null. Setting it default to UI.");
            jobCategory = JobCategory.UI;
        }
        final List<FilterDetails> filterDetailsList = new ArrayList<FilterDetails>();
        if (jobStatusQueryParams.getJobName() != null) {
            LOGGER.debug("Received a request to retrieve job with jobName: {}", jobStatusQueryParams.getJobName());
            final FilterDetails filter = new FilterDetails();
            filter.setColumnName(JOB_NAME);
            filter.setFilterText(jobStatusQueryParams.getJobName());
            filter.setFilterOperator(FilterOperatorEnum.EQUALS.getAttribute());
            filterDetailsList.add(filter);
            sortBy = START_DATE;
        }
        if (jobStatusQueryParams.getUserName() != null) {
            LOGGER.debug("Received a request to retrieve jobs created by user: {}", jobStatusQueryParams.getUserName());
            final FilterDetails filter = new FilterDetails();
            filter.setColumnName(CREATED_BY);
            filter.setFilterText(jobStatusQueryParams.getUserName());
            filter.setFilterOperator(FilterOperatorEnum.EQUALS.getAttribute());
            filterDetailsList.add(filter);
        }
        if (jobStatusQueryParams.getJobState() != null) {
            LOGGER.debug("Received a request to retrieve jobs with status: {}", jobStatusQueryParams.getJobState());
            final FilterDetails filter = new FilterDetails();
            filter.setColumnName(JOB_STATUS);
            filter.setFilterText(jobStatusQueryParams.getJobState().getJobStateName());
            filter.setFilterOperator(FilterOperatorEnum.EQUALS.getAttribute());
            filterDetailsList.add(filter);
        }
        if (jobStatusQueryParams.getJobType() != null) {
            LOGGER.debug("Received a request to retrieve jobs with jobtype: {}", jobStatusQueryParams.getJobType());
            final FilterDetails filter = new FilterDetails();
            filter.setColumnName(JOB_TYPE);
            filter.setFilterText(jobStatusQueryParams.getJobType().getAttribute());
            filter.setFilterOperator(FilterOperatorEnum.EQUALS.getAttribute());
            filterDetailsList.add(filter);
        }
        jobInput.setSortBy(sortBy);
        jobInput.setFilterDetails(filterDetailsList);

        return shmJobService.getJobDetails(jobInput);
    }

    /**
     * Method retrieves Main Job details Job Output for requested query.
     * 
     * @param mainJobsData
     * @return JobReportData
     */
    public JobReportData fetchDescendantNEJobStatus(final List<SHMJobData> mainJobsData) {
        LOGGER.debug("Queried with a Job ID for job status. Main job and it's descendant job status will be retrieved.");
        final NeJobInput jobInput = new NeJobInput();
        jobInput.setJobIdsList(Arrays.asList(Long.parseLong(mainJobsData.get(0).getJobId())));
        jobInput.setLimit(Integer.MAX_VALUE);
        jobInput.setOffset(1);
        jobInput.setSortBy(JOB_STATUS);
        jobInput.setOrderBy(DESC_SORT);
        return shmJobService.getJobReportDetails(jobInput);
    }
}
