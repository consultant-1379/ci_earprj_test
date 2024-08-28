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
package com.ericsson.oss.services.shm.job.remote.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.job.entities.JobInput;
import com.ericsson.oss.services.shm.job.entities.JobOutput;
import com.ericsson.oss.services.shm.job.remote.api.JobStatusQuery;
import com.ericsson.oss.services.shm.job.service.SHMJobService;
import com.ericsson.oss.services.shm.jobs.common.api.*;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.shm.job.entities.SHMJobData;

@RunWith(MockitoJUnitRunner.class)
public class JobStatusHelperTest {

    @InjectMocks
    private JobStatusHelper objectUnderTest;

    @Mock
    private JobInput jobInput;

    @Mock
    private SHMJobService shmJobService;

    @SuppressWarnings("unchecked")
    @Test
    public void testGetJobStatusWithJobState() {

        JobOutput jobOut = new JobOutput();
        jobOut.setResult(getMainJobData());
        when(shmJobService.getJobDetails(any(JobInput.class))).thenReturn(jobOut);
        JobStatusQuery jobStatusQueryParams = new JobStatusQuery();
        jobStatusQueryParams.setJobState(JobState.RUNNING);
        jobStatusQueryParams.setUserName("administrator");
        final JobOutput jobStatusResponse = objectUnderTest.fetchMainJobStatus(jobStatusQueryParams);
        List<SHMJobData> jobdata = (List<SHMJobData>) jobStatusResponse.getResult();
        assertEquals(jobdata.size(), getMainJobData().size());
        assertEquals(jobdata.get(0).getCreatedBy(), "administrator");
        assertEquals(jobdata.get(0).getStatus(), JobState.RUNNING.toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetJobStatusWithJobName() {
        JobOutput jobOut = new JobOutput();
        jobOut.setResult(getMainJobData());
        when(shmJobService.getJobDetails(any(JobInput.class))).thenReturn(jobOut);
        JobStatusQuery jobStatusQueryParams = new JobStatusQuery();
        jobStatusQueryParams.setJobName("dummyJobName");
        jobStatusQueryParams.setUserName("administrator");
        final JobOutput jobStatusResponse = objectUnderTest.fetchMainJobStatus(jobStatusQueryParams);
        List<SHMJobData> jobdata = (List<SHMJobData>) jobStatusResponse.getResult();
        assertEquals(jobdata.size(), getMainJobData().size());
        assertEquals(jobdata.get(0).getCreatedBy(), "administrator");
        assertEquals(jobdata.get(0).getJobName(), "DummyJob");
    }

    @Test
    public void testfetchNEJobDetails() {
        when(shmJobService.getJobReportDetails(any(NeJobInput.class))).thenReturn(getJobReportData());
        final JobReportData jobReportData = objectUnderTest.fetchDescendantNEJobStatus(getMainJobData());
        assertEquals(jobReportData.getJobDetails().getJobName(), getMainJobData().get(0).getJobName());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetJobStatusWithJobType() {
        JobOutput jobOut = new JobOutput();
        jobOut.setResult(getMainJobData());
        when(shmJobService.getJobDetails(any(JobInput.class))).thenReturn(jobOut);
        JobStatusQuery jobStatusQueryParams = new JobStatusQuery();
        jobStatusQueryParams.setJobType(JobTypeEnum.BACKUP);
        jobStatusQueryParams.setUserName("administrator");
        final JobOutput jobStatusResponse = objectUnderTest.fetchMainJobStatus(jobStatusQueryParams);
        List<SHMJobData> jobdata = (List<SHMJobData>) jobStatusResponse.getResult();
        assertEquals(jobdata.size(), getMainJobData().size());
        assertEquals(jobdata.get(0).getCreatedBy(), "administrator");
        assertEquals(jobdata.get(0).getJobType(), JobTypeEnum.BACKUP.getAttribute());
    }

    private List<SHMJobData> getMainJobData() {
        List<SHMJobData> mainJobStatus = new ArrayList<SHMJobData>();
        SHMJobData shmJobData = new SHMJobData();
        shmJobData.setJobName("DummyJob");
        shmJobData.setJobId(12345);
        shmJobData.setJobType("BACKUP");
        shmJobData.setCreatedBy("administrator");
        shmJobData.setTotalNoOfNEs("5");
        shmJobData.setProgress(70);
        shmJobData.setStatus("RUNNING");
        shmJobData.setResult("");
        shmJobData.setStartDate("1474951216250");
        shmJobData.setEndDate("1474953062283");
        mainJobStatus.add(shmJobData);
        return mainJobStatus;
    }

    private JobReportData getJobReportData() {
        JobReportData jobReportData = new JobReportData();

        NeDetails neDetails = new NeDetails();
        NeJobDetails neJobDetails = new NeJobDetails();
        neJobDetails.setNeActivity("DummyActivity");
        neJobDetails.setNeStatus("RUNNING");
        neJobDetails.setNeResult("");
        neJobDetails.setNeProgress(50);
        neJobDetails.setNeStartDate("");
        neJobDetails.setNeEndDate("");
        List<NeJobDetails> neJobDetailsLst = new ArrayList<NeJobDetails>();
        neJobDetailsLst.add(neJobDetails);
        neDetails.setResult(neJobDetailsLst);

        jobReportData.setNeDetails(neDetails);

        JobReportDetails jobDetails = new JobReportDetails();
        jobDetails.setJobName("DummyJob");
        jobDetails.setJobType("BACKUP");
        jobDetails.setJobCreatedBy("adminstrator");

        jobReportData.setJobReportDetails(jobDetails);

        return jobReportData;
    }

}
