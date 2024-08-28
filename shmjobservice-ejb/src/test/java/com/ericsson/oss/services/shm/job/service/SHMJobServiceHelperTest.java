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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Event;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.api.CheckPeriodicity;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.webpush.api.JobWebPushEvent;
import com.ericsson.oss.services.shm.webpush.api.WebPushConstants;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.services.wfs.api.query.WorkflowObject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ JobWebPushEvent.class, SHMJobServiceHelper.class })
public class SHMJobServiceHelperTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SHMJobServiceImplTest.class);

    @InjectMocks
    SHMJobServiceHelper shmJobServiceHelper;

    @Mock
    JobsDeletionRetryProxy jobsDeletionRetryProxy;

    @Mock
    JobConfigurationService jobConfigurationService;

    @Mock
    private JobWebPushEvent jobWebPushEventMock;

    @Mock
    private Event<JobWebPushEvent> eventSender;

    @Mock
    WorkflowInstanceNotifier localWorkflowQueryServiceProxy;

    @Mock
    DpsReader dpsReader;

    @Mock
    CheckPeriodicity checkPeriodicity;

    @Test
    public void testFetchJobDetails() {
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
        when(jobsDeletionRetryProxy.retrieveJobDetails(poIdSet)).thenReturn(jobProjection);
        when(jobsDeletionRetryProxy.fetchJobTemplateAttributes(templateJobIds)).thenReturn(jobsDetailForDeletion);
        final Map<Long, List<Job>> actualResponse = shmJobServiceHelper.fetchJobDetails(poIdSet, Matchers.any(JobsDeletionReport.class));
        assertNotNull(actualResponse);
        assertEquals(1, actualResponse.get(firstJobTemplateId).size());
    }

    @Test
    public void testExtractListOfJobs() {
        final Map<Long, List<Job>> jobDetails = new HashMap<Long, List<Job>>();
        final long firstJobTemplateId = 11;
        final List<Job> firstJob = setup();

        jobDetails.put(firstJobTemplateId, firstJob);

        final List<Job> actualResponse = shmJobServiceHelper.extractListOfJobs(jobDetails);
        assertNotNull(actualResponse);
        assertEquals(1, actualResponse.size());
        assertEquals(firstJob.get(0).getJobName(), actualResponse.get(0).getJobName());
        assertEquals(firstJob.get(0).getJobState(), actualResponse.get(0).getJobState());
        assertEquals(firstJob.get(0).getWfsId(), actualResponse.get(0).getWfsId());
    }

    @Test
    public void testIsJobActive() {
        final boolean response = shmJobServiceHelper.isJobActive(JobState.RUNNING.toString());
        assertTrue(response);
    }

    @Test
    public void testIsJobActiveForCompletedJob() {
        final boolean response = shmJobServiceHelper.isJobActive(JobState.COMPLETED.toString());
        assertFalse(response);
    }

    @Test
    public void testdeleteJobHirerachyForImmediate() {

        final List<Job> firstJobsDeletionAttributes = setup();
        final Map<String, Object> firstJobConfiguration = new HashMap<String, Object>();
        final Map<String, Object> firstMainSchedule = new HashMap<String, Object>();
        firstMainSchedule.put(ShmConstants.EXECUTION_MODE, ExecMode.IMMEDIATE.toString());
        firstJobConfiguration.put(ShmConstants.MAIN_SCHEDULE, firstMainSchedule);
        firstJobsDeletionAttributes.get(0).setJobConfigurationDetails(firstJobConfiguration);
        when(jobsDeletionRetryProxy.deleteJobHierarchyWithJobTemplate(Matchers.any(Job.class))).thenReturn(1);
        assertEquals(1, shmJobServiceHelper.deleteJobHirerachy(firstJobsDeletionAttributes.get(0)));
        verify(eventSender, never()).fire(jobWebPushEventMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testdeleteJobHirerachyForPeriodic() {

        final long firstJobTemplateId = 11;
        final List<Job> firstJobsDeletionAttributes = setup();
        final List<WorkflowObject> batchWorkFlowList = new ArrayList<WorkflowObject>();
        when(localWorkflowQueryServiceProxy.executeWorkflowQuery(Matchers.any(com.ericsson.oss.services.wfs.api.query.Query.class))).thenReturn(batchWorkFlowList);

        final Map<String, Object> fourthJobConfiguration = new HashMap<String, Object>();
        final Map<String, Object> fourthMainSchedule = new HashMap<String, Object>();
        fourthMainSchedule.put(ShmConstants.EXECUTION_MODE, ExecMode.SCHEDULED.toString());
        final List<Map<String, Object>> fourthScheduleParameters = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatType = new HashMap<String, Object>();
        repeatType.put(ShmConstants.NAME, ShmConstants.REPEAT_TYPE);
        repeatType.put(ShmConstants.VALUE, "DAILY");
        fourthScheduleParameters.add(repeatType);
        fourthMainSchedule.put(ShmConstants.SCHEDULINGPROPERTIES, fourthScheduleParameters);
        fourthJobConfiguration.put(ShmConstants.MAIN_SCHEDULE, fourthMainSchedule);
        firstJobsDeletionAttributes.get(0).setJobConfigurationDetails(fourthJobConfiguration);
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(ShmConstants.JOB_TEMPLATE_ID, firstJobTemplateId);
        when(checkPeriodicity.isJobPeriodic((List<Map<String, Object>>) fourthMainSchedule.get(ShmConstants.SCHEDULINGPROPERTIES))).thenReturn(true);
        when(dpsReader.getCountForItemsQueried("shm", "JOB", restrictionAttributes)).thenReturn(1L);
        when(jobsDeletionRetryProxy.deleteJobHierarchyWithJobTemplate(Matchers.any(Job.class))).thenReturn(1);
        assertEquals(1, shmJobServiceHelper.deleteJobHirerachy(firstJobsDeletionAttributes.get(0)));
        verify(eventSender, never()).fire(jobWebPushEventMock);
    }

    @Test
    public void testdeleteJobHirerachyWithOutJobTemplateForShm() {
        final List<WorkflowObject> batchWorkFlowList = new ArrayList<>();
        try {
            PowerMockito.whenNew(JobWebPushEvent.class).withNoArguments().thenReturn(jobWebPushEventMock);
        } catch (Exception e) {
            LOGGER.info("NHC Exception while testing delete : {}", e);
        }
        when(localWorkflowQueryServiceProxy.executeWorkflowQuery(Matchers.any(com.ericsson.oss.services.wfs.api.query.Query.class))).thenReturn(batchWorkFlowList);
        final List<Job> firstJobsDeletionAttributes = setup();
        when(jobsDeletionRetryProxy.deleteJobHierarchyWithoutJobTemplate(Matchers.any(Job.class))).thenReturn(1);
        when(jobConfigurationService.getJobCategory(1l)).thenReturn("SHM");
        assertEquals(1, shmJobServiceHelper.deleteJobHirerachy(firstJobsDeletionAttributes.get(0)));
        verify(jobWebPushEventMock, times(1)).setApplicationType(WebPushConstants.SHM_JOBS_APPLICATION);
        verify(eventSender, times(1)).fire(jobWebPushEventMock);

    }

    @Test
    public void testdeleteJobHirerachyWithOutJobTemplateForNhc() {
        final List<WorkflowObject> batchWorkFlowList = new ArrayList<>();
        try {
            PowerMockito.whenNew(JobWebPushEvent.class).withNoArguments().thenReturn(jobWebPushEventMock);
        } catch (Exception e) {
            LOGGER.info("NHC Exception while testing delete : {}", e);
        }
        when(localWorkflowQueryServiceProxy.executeWorkflowQuery(Matchers.any(com.ericsson.oss.services.wfs.api.query.Query.class))).thenReturn(batchWorkFlowList);
        final List<Job> firstJobsDeletionAttributes = setup();
        when(jobsDeletionRetryProxy.deleteJobHierarchyWithoutJobTemplate(Matchers.any(Job.class))).thenReturn(1);
        when(jobConfigurationService.getJobCategory(1l)).thenReturn("NHC_UI");
        assertEquals(1, shmJobServiceHelper.deleteJobHirerachy(firstJobsDeletionAttributes.get(0)));
        verify(jobWebPushEventMock, times(1)).setApplicationType(WebPushConstants.NHC_JOBS_APPLICATION);
        verify(eventSender, times(1)).fire(jobWebPushEventMock);

    }

    private List<Job> setup() {
        final long firstJobId = 1;
        final long firstJobTemplateId = 11;
        final String firstJobState = "COMPLETED";
        final int executionIndex = 1;
        final String firstJobName = "firstJobName";
        final String jobType = "UPGRADE";
        final String firstWfsId = "firstWfsId";

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
        return firstJob;
    }

}
