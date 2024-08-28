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
package com.ericsson.oss.services.shm.job.housekeeping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.job.service.Job;
import com.ericsson.oss.services.shm.job.service.JobsDeletionReport;
import com.ericsson.oss.services.shm.job.service.JobsDeletionService;
import com.ericsson.oss.services.shm.job.service.SHMJobServiceHelper;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(MockitoJUnitRunner.class)
public class JobsHouseKeepingHelperTest {

    @InjectMocks
    JobsHouseKeepingHelper objectUnderTest;

    @Mock
    @Inject
    SystemRecorder systemRecorder;
    @Mock
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Mock
    JobsDeletionService jobsDeletionService;

    @Mock
    private WorkflowInstanceNotifier localWorkflowQueryServiceProxy;

    @Mock
    private SHMJobServiceHelper shmJobServiceHelper;

    @Mock
    private JobsHouseKeepingHelperUtil jobsHouseKepingServiceHelperUtil;

    @Test
    public void testDeleteJobsSuccess() {

        final Map<Long, List<Job>> jobDetails = new HashMap<Long, List<Job>>();

        final long firstJobId = 1;
        final long firstJobTemplateId = 11;
        final String firstJobState = "COMPLETED";
        final int executionIndex = 1;
        final String firstJobName = "firstJobName";
        final String jobType = "UPGRADE";
        final String firstWfsId = "firstWfsId";

        final List<Long> houseKeepingJobsPoIds = new ArrayList<Long>();
        houseKeepingJobsPoIds.add(firstJobId);

        final Set<Long> poIdSet = new HashSet<Long>();

        for (final Long poId : houseKeepingJobsPoIds) {
            poIdSet.add(poId);
        }

        final List<Long> templateJobIds = new ArrayList<Long>();
        templateJobIds.add(firstJobTemplateId);

        final List<Object[]> jobProjection = new ArrayList<Object[]>();
        final Object[] firstObject = new Object[4];
        firstObject[0] = firstJobId;
        firstObject[1] = firstJobTemplateId;
        firstObject[2] = firstJobState;
        firstObject[3] = executionIndex;

        jobProjection.add(firstObject);

        final List<Map<String, Object>> jobsDetailForDeletion = new ArrayList<Map<String, Object>>();
        final Map<String, Object> firstJobTemplateAttributes = new HashMap<String, Object>();
        firstJobTemplateAttributes.put(ShmConstants.NAME, firstJobName);
        firstJobTemplateAttributes.put(ShmConstants.JOB_TYPE, jobType);
        firstJobTemplateAttributes.put(ShmConstants.WFS_ID, firstWfsId);
        firstJobTemplateAttributes.put(ShmConstants.JOBTEMPLATEID, firstJobTemplateId);
        jobsDetailForDeletion.add(firstJobTemplateAttributes);

        final List<Job> firstJob = new ArrayList<Job>();
        final Job firstJobsDeletionAttributes = new Job();
        firstJobsDeletionAttributes.setExecutionIndex(executionIndex);
        firstJobsDeletionAttributes.setJobName(firstJobName);
        firstJobsDeletionAttributes.setJobState(firstJobState);
        firstJobsDeletionAttributes.setJobTemplateId(firstJobTemplateId);
        firstJobsDeletionAttributes.setJobType(jobType);
        firstJobsDeletionAttributes.setMainJobId(firstJobId);
        firstJobsDeletionAttributes.setWfsId(firstWfsId);
        firstJob.add(firstJobsDeletionAttributes);

        jobDetails.put(firstJobTemplateId, firstJob);

        final JobsDeletionReport jobsDeletionReport = new JobsDeletionReport(poIdSet.size());
        jobsDeletionReport.incrementJobsDeletedCount();
        when(jobsHouseKepingServiceHelperUtil.deleteJobPoOneByOneAndGetDeletionReport(Matchers.anyLong())).thenReturn(jobsDeletionReport);

        JobsHouseKeepingResponse actualResponse = objectUnderTest.deleteJobs(jobType, houseKeepingJobsPoIds);
        assertNotNull(actualResponse);
        assertEquals(1, actualResponse.getSuccessfullyDeletedJobsCount());
        assertEquals(0, actualResponse.getFailedToDeleteJobsCount());
        assertEquals(0, actualResponse.getJobsNotFoundCount());
    }

    @Test
    public void testDeleteJobsNotFound() {
        final long firstJobId = 1;
        final String jobType = "UPGRADE";
        final List<Long> houseKeepingJobsPoIds = new ArrayList<Long>();
        houseKeepingJobsPoIds.add(firstJobId);
        final Set<Long> poIdSet = new HashSet<Long>();
        for (final Long poId : houseKeepingJobsPoIds) {
            poIdSet.add(poId);
        }
        final JobsDeletionReport jobsDeletionReport = new JobsDeletionReport(poIdSet.size());
        jobsDeletionReport.incrementJobsNotFoundCount(1);

        when(jobsHouseKepingServiceHelperUtil.deleteJobPoOneByOneAndGetDeletionReport(Matchers.anyLong())).thenReturn(jobsDeletionReport);
        JobsHouseKeepingResponse actualResponse = objectUnderTest.deleteJobs(jobType, houseKeepingJobsPoIds);
        assertNotNull(actualResponse);
        assertEquals(0, actualResponse.getSuccessfullyDeletedJobsCount());
        assertEquals(0, actualResponse.getFailedToDeleteJobsCount());
        assertEquals(1, jobsDeletionReport.getJobsNotFound());
    }

    @Test
    public void testDeleteJobsFailed() {
        final Map<Long, List<Job>> jobDetails = new HashMap<Long, List<Job>>();

        final long firstJobId = 1;
        final long firstJobTemplateId = 11;
        final String firstJobState = "COMPLETED";
        final int executionIndex = 1;
        final String firstJobName = "firstJobName";
        final String jobType = "UPGRADE";
        final String firstWfsId = "firstWfsId";

        final List<Long> houseKeepingJobsPoIds = new ArrayList<Long>();
        houseKeepingJobsPoIds.add(firstJobId);

        final Set<Long> poIdSet = new HashSet<Long>();

        for (final Long poId : houseKeepingJobsPoIds) {
            poIdSet.add(poId);
        }

        final List<Long> templateJobIds = new ArrayList<Long>();
        templateJobIds.add(firstJobTemplateId);

        final List<Object[]> jobProjection = new ArrayList<Object[]>();
        final Object[] firstObject = new Object[4];
        firstObject[0] = firstJobId;
        firstObject[1] = firstJobTemplateId;
        firstObject[2] = firstJobState;
        firstObject[3] = executionIndex;

        jobProjection.add(firstObject);

        final List<Map<String, Object>> jobsDetailForDeletion = new ArrayList<Map<String, Object>>();
        final Map<String, Object> firstJobTemplateAttributes = new HashMap<String, Object>();
        firstJobTemplateAttributes.put(ShmConstants.NAME, firstJobName);
        firstJobTemplateAttributes.put(ShmConstants.JOB_TYPE, jobType);
        firstJobTemplateAttributes.put(ShmConstants.WFS_ID, firstWfsId);
        firstJobTemplateAttributes.put(ShmConstants.JOBTEMPLATEID, firstJobTemplateId);
        jobsDetailForDeletion.add(firstJobTemplateAttributes);

        final List<Job> firstJob = new ArrayList<Job>();
        final Job firstJobsDeletionAttributes = new Job();
        firstJobsDeletionAttributes.setExecutionIndex(executionIndex);
        firstJobsDeletionAttributes.setJobName(firstJobName);
        firstJobsDeletionAttributes.setJobState(firstJobState);
        firstJobsDeletionAttributes.setJobTemplateId(firstJobTemplateId);
        firstJobsDeletionAttributes.setJobType(jobType);
        firstJobsDeletionAttributes.setMainJobId(firstJobId);
        firstJobsDeletionAttributes.setWfsId(firstWfsId);
        firstJob.add(firstJobsDeletionAttributes);

        final JobsDeletionReport jobsDeletionReport = new JobsDeletionReport(poIdSet.size());
        jobsDeletionReport.incrementfailedJobsDeletionCount();
        jobDetails.put(firstJobTemplateId, firstJob);

        when(jobsHouseKepingServiceHelperUtil.deleteJobPoOneByOneAndGetDeletionReport(Matchers.anyLong())).thenReturn(jobsDeletionReport);

        JobsHouseKeepingResponse actualResponse = objectUnderTest.deleteJobs(jobType, houseKeepingJobsPoIds);
        assertNotNull(actualResponse);
        assertEquals(0, actualResponse.getSuccessfullyDeletedJobsCount());
        assertEquals(1, actualResponse.getFailedToDeleteJobsCount());
        assertEquals(0, actualResponse.getJobsNotFoundCount());
    }

}
