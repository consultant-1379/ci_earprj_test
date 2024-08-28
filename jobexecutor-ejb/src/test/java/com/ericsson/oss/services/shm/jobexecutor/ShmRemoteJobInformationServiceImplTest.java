package com.ericsson.oss.services.shm.jobexecutor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.ericsson.oss.services.shm.job.entities.JobOutput;
import com.ericsson.oss.services.shm.job.remote.api.JobStatusQuery;
import com.ericsson.oss.services.shm.job.remote.api.JobStatusResponse;
import com.ericsson.oss.services.shm.job.remote.impl.JobStatusHelper;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportData;
import com.ericsson.oss.services.shm.jobs.common.api.NeDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobDetails;
import com.ericsson.oss.shm.job.entities.SHMJobData;

@RunWith(MockitoJUnitRunner.class)
@PrepareForTest({ JobStatusQuery.class })
public class ShmRemoteJobInformationServiceImplTest {

    @InjectMocks
    private ShmRemoteJobInformationServiceImpl jobRemoteImpl;

    @Mock
    private JobStatusResponse jobStatusResponse;

    @Mock
    private List<SHMJobData> mainJobData;

    @Mock
    private JobStatusHelper jobStatusHelper;

    @Mock
    private JobStatusQuery jobStatusQueryParams;

    @Mock
    private JobOutput mainJobStatus;

    @Test
    public void test_getJobStatus() throws Exception {
        final String JOB_NAME = "TEST_JOB";

        JobOutput jobOut = new JobOutput();
        jobOut.setResult(getMainJobData());
        JobReportData jobReportData = prepareNeLevelresponse();
        when(jobStatusHelper.fetchMainJobStatus(any(JobStatusQuery.class))).thenReturn(jobOut);
        when(jobStatusHelper.fetchDescendantNEJobStatus(Matchers.anyList())).thenReturn(jobReportData);
        JobStatusQuery jobStatusQueryParams = new JobStatusQuery();
        jobStatusQueryParams.setJobName(JOB_NAME);
        jobRemoteImpl.getJobStatus(JOB_NAME);

    }

    private List<SHMJobData> getMainJobData() {

        final List<SHMJobData> mainJobStatus = new ArrayList<SHMJobData>();
        SHMJobData shmJobData = new SHMJobData();
        shmJobData.setJobName("UPGRADE_SOFTWAREPACKAGE_20171027");
        shmJobData.setJobId(12345);
        shmJobData.setJobType("UPGRADE");
        shmJobData.setCreatedBy("administrator");
        shmJobData.setTotalNoOfNEs("5");
        shmJobData.setProgress(100);
        shmJobData.setStatus("COMPLETED");
        shmJobData.setResult("SUCCESS");
        shmJobData.setStartDate("1474951216250");
        shmJobData.setEndDate("1474953062283");
        mainJobStatus.add(shmJobData);

        return mainJobStatus;
    }

    private JobReportData prepareNeLevelresponse() {

        JobReportData neLevelJobStatus = new JobReportData();
        final NeDetails neDetails = new NeDetails();
        List<NeJobDetails> neJobDetailslist = new ArrayList<NeJobDetails>();
        final NeJobDetails neJobDetails = new NeJobDetails();
        neJobDetails.setNeNodeName("vRC01");
        neJobDetails.setLastLogMessage("Prepare activity got success");
        neJobDetails.setNeResult("SUCCESS");
        neJobDetailslist.add(neJobDetails);
        neDetails.setResult(neJobDetailslist);
        neLevelJobStatus.setNeDetails(neDetails);

        return neLevelJobStatus;
    }

}