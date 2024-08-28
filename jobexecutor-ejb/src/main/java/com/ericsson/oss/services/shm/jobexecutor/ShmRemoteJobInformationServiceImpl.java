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

package com.ericsson.oss.services.shm.jobexecutor;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.job.entities.JobOutput;
import com.ericsson.oss.services.shm.job.remote.api.JobStatus;
import com.ericsson.oss.services.shm.job.remote.api.JobStatusQuery;
import com.ericsson.oss.services.shm.job.remote.api.JobStatusResponse;
import com.ericsson.oss.services.shm.job.remote.api.NeJobData;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobInformationService;
import com.ericsson.oss.services.shm.job.remote.api.exceptions.JobStatusException;
import com.ericsson.oss.services.shm.job.remote.impl.JobStatusHelper;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportData;
import com.ericsson.oss.services.shm.jobs.common.api.NeDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobDetails;
import com.ericsson.oss.shm.job.entities.SHMJobData;

/**
 * This class retrieves job status of corresponding job.
 * 
 * @author xgudpra
 * 
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ShmRemoteJobInformationServiceImpl implements ShmRemoteJobInformationService {

    @Inject
    private JobStatusHelper jobStatusHelper;

    private static final String JOB_NOT_FOUND = "Job not found with the Job Name : ";
    private static final String INTERNAL_ERROR = "Error while retrieving the Job Status for the Job Name : ";

    /**
     * It will retrieve job status of given jobName
     * 
     * @param jobName
     * @return JobResponse
     * @throws JobStatusException
     */
    @Override
    public JobStatus getJobStatus(final String jobName) throws JobStatusException {
        final JobStatusQuery jobStatusQueryParams = new JobStatusQuery();
        JobStatus jobstatus = null;
        jobStatusQueryParams.setJobName(jobName);
        final JobStatusResponse jobStatusResponse = prepareJobStatusResponse(jobStatusQueryParams);
        if (isValidJobResponse(jobStatusResponse)) {
            jobstatus = prepareJobResponse(jobStatusResponse);
        } else {
            throw new JobStatusException(JOB_NOT_FOUND + jobName);
        }
        return jobstatus;
    }

    private boolean isValidJobResponse(final JobStatusResponse jobStatusResponse) {
        return jobStatusResponse.getMainJobStatus() != null && !jobStatusResponse.getMainJobStatus().isEmpty();
    }

    private JobStatusResponse prepareJobStatusResponse(final JobStatusQuery jobStatusQueryParams) throws JobStatusException {
        JobStatusResponse jobStatusResponse = null;
        try {
            final JobOutput mainJobStatus = jobStatusHelper.fetchMainJobStatus(jobStatusQueryParams);
            final List<SHMJobData> mainJobData = new ArrayList<>((List<SHMJobData>) mainJobStatus.getResult());
            jobStatusResponse = new JobStatusResponse(mainJobData);
            if (!mainJobData.isEmpty()) {
                jobStatusResponse.setNeLevelJobStatus(jobStatusHelper.fetchDescendantNEJobStatus(mainJobData));
            }
        } catch (final Exception ex) {
            throw new JobStatusException(INTERNAL_ERROR + jobStatusQueryParams.getJobName());
        }
        return jobStatusResponse;
    }

    private JobStatus prepareJobResponse(final JobStatusResponse jobStatusResponse) {
        final JobStatus jobStatus = prepareMainJobResponse(jobStatusResponse);
        final List<NeJobData> neJobResponse = prepareNeJobResponse(jobStatusResponse);
        if (!neJobResponse.isEmpty()) {
            jobStatus.setNeJobResult(neJobResponse);
        }
        return jobStatus;
    }

    private JobStatus prepareMainJobResponse(final JobStatusResponse jobStatusResponse) {
        final JobStatus jobStatus = new JobStatus();
        final List<SHMJobData> mainJobStatus = jobStatusResponse.getMainJobStatus();
        final SHMJobData shmJobData = mainJobStatus.get(0);

        jobStatus.setProgressPercentage(shmJobData.getProgress());
        jobStatus.setEndTime(shmJobData.getEndDate());
        jobStatus.setStartTime(shmJobData.getStartDate());
        jobStatus.setNoOfNetworkElements(Integer.parseInt(shmJobData.getTotalNoOfNEs()));
        jobStatus.setJobResult(shmJobData.getResult());
        jobStatus.setState(shmJobData.getStatus());
        jobStatus.setJobName(shmJobData.getJobName());
        return jobStatus;
    }

    private List<NeJobData> prepareNeJobResponse(final JobStatusResponse jobStatusResponse) {
        final List<NeJobData> neJobResult = new ArrayList<>();
        final JobReportData neLevelJobStatus = jobStatusResponse.getNeLevelJobStatus();
        if (neLevelJobStatus.getNeDetails() != null) {
            final NeDetails neDetails = neLevelJobStatus.getNeDetails();
            if (neDetails.getResult() != null && !neDetails.getResult().isEmpty()) {
                final List<NeJobDetails> neJobDetailsList = neDetails.getResult();
                for (int i = 0; i < neJobDetailsList.size(); i++) {
                    final NeJobData neJobData = new NeJobData();
                    final NeJobDetails neJobDetails = neJobDetailsList.get(i);
                    neJobData.setNetworkElement(neJobDetails.getNeNodeName());
                    neJobData.setLastLogMessage(neJobDetails.getLastLogMessage());
                    neJobData.setNeResult(neJobDetails.getNeResult());
                    neJobResult.add(neJobData);
                }
            }
        }
        return neJobResult;
    }

}
