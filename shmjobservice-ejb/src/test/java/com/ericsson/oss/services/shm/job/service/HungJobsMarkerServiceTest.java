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
package com.ericsson.oss.services.shm.job.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.es.api.SHMLoadControllerCounterRequest;
import com.ericsson.oss.services.shm.jobs.common.api.CheckPeriodicity;
import com.ericsson.oss.services.shm.jobs.common.api.NEJob;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobservice.common.HungJobQueryService;
import com.ericsson.oss.services.shm.loadcontrol.local.api.SHMLoadControllerLocalService;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(MockitoJUnitRunner.class)
public class HungJobsMarkerServiceTest {

    @InjectMocks
    private HungJobsMarkerService hungJobsMarkerService;

    @Mock
    private HungJobsConfigParamChangeListener hangingJobsConfigParamChangeListener;

    @Mock
    private HungJobQueryService hungJobQueryService;

    @Mock
    protected Logger logger;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    protected JobUpdateService jobUpdateService;

    @Mock
    protected WorkflowInstanceNotifier workflowInstanceHelper;

    @Mock
    private SHMLoadControllerLocalService shmLoadControllerLocalService;

    @Mock
    private List<Map<String, Object>> schedulePropertiesList;

    @Mock
    private CheckPeriodicity checkPeriodicity;

    @Mock
    PersistenceObject persistenceObjectMock;

    @Mock
    List<PersistenceObject> persistenceObjectMockList;

    static final long TEMPLATE_ID = 1010201l;
    final String businessKey = String.valueOf(TEMPLATE_ID);
    static final String NE_SYSTEM_CANCELLED = "For \"%s\", \"%s\"'s Job is System Cancelled.";
    static final String INITIAL_STATE = "Initial State = ";

    /**
     * @param jobsHouseKeepingResponse
     * @param backupJobsHouseKeepingResponse
     * @param clusterCount
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void setUp() {
        final int maxTimeLimitForJobExecutionInHours = 48;
        final int maxTimeLimitForAxeUpgradeJobExecutionInHours = 240;

        final long mainJobId = 11l;
        final int executionIndex = 1;
        final String jobName = "BACKUP";

        final Map<String, Object> jobNameAndWorkflowAttributes = new HashMap<>();
        jobNameAndWorkflowAttributes.put(ShmConstants.NAME, jobName);
        jobNameAndWorkflowAttributes.put(ShmConstants.JOB_TYPE, "BACKUP");
        jobNameAndWorkflowAttributes.put(ShmConstants.WFS_ID, "wfsid");

        final List<Object[]> mainJobAttributes = new ArrayList<Object[]>();
        final Object[] mainJobAttribute = new Object[6];
        mainJobAttribute[0] = mainJobId;
        mainJobAttribute[1] = executionIndex;
        mainJobAttribute[2] = "RUNNING";
        mainJobAttribute[3] = TEMPLATE_ID;
        mainJobAttribute[4] = businessKey;
        mainJobAttribute[5] = schedulePropertiesList;
        mainJobAttributes.add(mainJobAttribute);

        when(hangingJobsConfigParamChangeListener.getMaxTimeLimitForJobExecutionInHours()).thenReturn(maxTimeLimitForJobExecutionInHours);
        when(hangingJobsConfigParamChangeListener.getMaxTimeLimitForAxeUpgradeJobExecutionInHours()).thenReturn(maxTimeLimitForAxeUpgradeJobExecutionInHours);
        when(hungJobQueryService.getLongRunningJobs(maxTimeLimitForJobExecutionInHours)).thenReturn(mainJobAttributes);
        when(hungJobQueryService.getJobNameAndWorkflowId(TEMPLATE_ID)).thenReturn(jobNameAndWorkflowAttributes);
    }

    @Test
    public void testUpdateHangingJobsToSystemCancledWhenNejobEmpty() {
        setUp();

        final int maxTimeLimitForJobExecutionInHours = 48;
        final int maxTimeLimitForAxeUpgradeJobExecutionInHours = 240;
        final long mainJobId = 11l;
        final String wfsId = "wfsid";
        final List<Long> jobTemplateIds = new ArrayList<Long>();
        jobTemplateIds.add(TEMPLATE_ID);
        final List<String> mainJobWorkflows = new ArrayList<String>();
        mainJobWorkflows.add(wfsId);
        final List<NEJob> neJobsDetails = new ArrayList<NEJob>();
        when(hungJobQueryService.getHungNeJobs(mainJobId, maxTimeLimitForJobExecutionInHours, maxTimeLimitForAxeUpgradeJobExecutionInHours, "UPGRADE")).thenReturn(neJobsDetails);
        hungJobsMarkerService.updateHungJobsToSystemCancelled();

    }

    @Test
    public void testUpdateHangingJobsToSystemCancledWhenActivityJobNotCancelled() {
        setUp();
        final int maxTimeLimitForJobExecutionInHours = 48;
        final int maxTimeLimitForAxeUpgradeJobExecutionInHours = 240;
        final long mainJobId = 11l;
        final int executionIndex = 1;

        final List<Long> jobTemplateIds = new ArrayList<Long>();
        jobTemplateIds.add(TEMPLATE_ID);

        final String wfsId = "wfsid";
        final List<String> mainJobWorkflows = new ArrayList<String>();
        mainJobWorkflows.add(wfsId);

        final long neJobId = 2l;
        final String neWorkflowInstanceId = "neWfsId";
        final String nodeName = "LTE005";
        final JobState state = JobState.RUNNING;
        final String jobName = "BACKUP";
        final List<Long> nejobIds = new ArrayList<Long>();
        nejobIds.add(neJobId);

        final List<NEJob> neJobsDetails = new ArrayList<NEJob>();
        final NEJob neJob = new NEJob();
        neJob.setNeJobId(neJobId);
        neJob.setNeWorkflowInstanceId(neWorkflowInstanceId);
        neJob.setNodeName(nodeName);
        neJob.setState(state);
        neJobsDetails.add(neJob);

        when(hungJobQueryService.getHungNeJobs(Matchers.eq(mainJobId), Matchers.eq(maxTimeLimitForJobExecutionInHours), Matchers.eq(maxTimeLimitForAxeUpgradeJobExecutionInHours),
                Matchers.anyString())).thenReturn(neJobsDetails);
        final Map<String, Object> activityJobsUpdatedStatusMap = new HashMap<String, Object>();
        activityJobsUpdatedStatusMap.put("isActivityJobsUpdated", false);
        when(hungJobQueryService.cancelActivitiesAndUpdateState(Matchers.eq(neJobId), Matchers.eq(jobName), Matchers.eq(executionIndex), Matchers.eq(maxTimeLimitForJobExecutionInHours)))
                .thenReturn(activityJobsUpdatedStatusMap);
        hungJobsMarkerService.updateHungJobsToSystemCancelled();
        verify(logger, times(0)).debug("NE Jobs or ActivityJob is in scheduled for the main JobId : {}, mainJob name:{} ", mainJobId, jobName);

    }

    @Test
    public void testUpdateHangingJobsToSystemCancled() {
        setUp();

        final int maxTimeLimitForJobExecutionInHours = 48;
        final int maxTimeLimitForAxeUpgradeJobExecutionInHours = 240;
        final long mainJobId = 11l;
        final int executionIndex = 1;

        final List<Long> jobTemplateIds = new ArrayList<Long>();
        jobTemplateIds.add(TEMPLATE_ID);

        final String wfsId = "wfsid";
        final List<String> mainJobWorkflows = new ArrayList<String>();
        mainJobWorkflows.add(wfsId);

        final long neJobId = 2l;
        final String neWorkflowInstanceId = "neWfsId";
        final String nodeName = "LTE005";
        final JobState state = JobState.RUNNING;
        final String jobName = "BACKUP";
        final List<Long> nejobIds = new ArrayList<Long>();
        nejobIds.add(neJobId);

        final List<NEJob> neJobsDetails = new ArrayList<NEJob>();
        final NEJob neJobAttributes = new NEJob();
        neJobAttributes.setNeJobId(neJobId);
        neJobAttributes.setNeWorkflowInstanceId(neWorkflowInstanceId);
        neJobAttributes.setNodeName(nodeName);
        neJobAttributes.setState(state);
        neJobAttributes.setPlatformType("CPP");
        neJobAttributes.setJobType("BACKUP");
        neJobsDetails.add(neJobAttributes);

        final String additionalInfo = INITIAL_STATE + neJobAttributes.getState();
        final String cancelledLogMessage = String.format(NE_SYSTEM_CANCELLED, jobName, nodeName);
        final Map<String, Object> attributesToBePersisted = new HashMap<String, Object>();
        attributesToBePersisted.put(ShmConstants.STATE, state.toString());
        attributesToBePersisted.put(ShmConstants.ENDTIME, new Date());

        when(hungJobQueryService.getHungNeJobs(Matchers.eq(mainJobId), Matchers.eq(maxTimeLimitForJobExecutionInHours), Matchers.eq(maxTimeLimitForAxeUpgradeJobExecutionInHours),
                Matchers.anyString())).thenReturn(neJobsDetails);
        final Map<String, Object> activityJobsUpdatedStatusMap = new HashMap<String, Object>();
        activityJobsUpdatedStatusMap.put("isActivityJobsUpdated", true);
        when(hungJobQueryService.cancelActivitiesAndUpdateState(Matchers.eq(neJobId), Matchers.eq(jobName), Matchers.eq(executionIndex), Matchers.eq(maxTimeLimitForJobExecutionInHours)))
                .thenReturn(activityJobsUpdatedStatusMap);
        when(hungJobQueryService.prepareAttributesToBeUpdated(JobState.SYSTEM_CANCELLED, cancelledLogMessage)).thenReturn(attributesToBePersisted);
        verify(systemRecorder, times(0)).recordEvent(SHMEvents.SYSTEM_CANCELLED, EventLevel.COARSE, jobName, neJobAttributes.getNodeName(), additionalInfo);
        hungJobsMarkerService.updateHungJobsToSystemCancelled();
        verify(systemRecorder).recordEvent(SHMEvents.SYSTEM_CANCELLED, EventLevel.COARSE, jobName, neJobAttributes.getNodeName(), additionalInfo);
        verify(shmLoadControllerLocalService, times(1)).decrementCounter(Matchers.any(SHMActivityRequest.class));

        verify(workflowInstanceHelper, times(0)).sendAllNeDone(businessKey);
        verify(workflowInstanceHelper, times(2)).cancelWorkflowInstance(Matchers.anyString());
    }

    @Test
    public void testUpdateHangingJobsToSystemCancledWhenNeJobsRunning() {
        setUp();
        final int maxTimeLimitForJobExecutionInHours = 48;
        final int maxTimeLimitForAxeUpgradeJobExecutionInHours = 240;
        final long mainJobId = 11l;
        final int executionIndex = 1;

        final List<Long> jobTemplateIds = new ArrayList<Long>();
        jobTemplateIds.add(TEMPLATE_ID);

        final String wfsId = "wfsid";
        final List<String> mainJobWorkflows = new ArrayList<String>();
        mainJobWorkflows.add(wfsId);

        final long neJobId = 2l;
        final String neWorkflowInstanceId = "neWfsId";
        final String nodeName = "LTE005";
        final JobState state = JobState.RUNNING;
        final String jobName = "BACKUP";
        final List<Long> nejobIds = new ArrayList<Long>();
        nejobIds.add(neJobId);

        final List<NEJob> neJobsDetails = new ArrayList<NEJob>();
        final NEJob neJobAttributes = new NEJob();
        neJobAttributes.setNeJobId(neJobId);
        neJobAttributes.setNeWorkflowInstanceId(neWorkflowInstanceId);
        neJobAttributes.setNodeName(nodeName);
        neJobAttributes.setState(state);
        neJobAttributes.setPlatformType("CPP");
        neJobsDetails.add(neJobAttributes);

        final String additionalInfo = INITIAL_STATE + neJobAttributes.getState();
        final String cancelledLogMessage = String.format(NE_SYSTEM_CANCELLED, jobName, nodeName);
        final Map<String, Object> attributesToBePersisted = new HashMap<String, Object>();
        attributesToBePersisted.put(ShmConstants.STATE, state.toString());
        attributesToBePersisted.put(ShmConstants.ENDTIME, new Date());

        when(hungJobQueryService.getHungNeJobs(Matchers.eq(mainJobId), Matchers.eq(maxTimeLimitForJobExecutionInHours), Matchers.eq(maxTimeLimitForAxeUpgradeJobExecutionInHours),
                Matchers.anyString())).thenReturn(neJobsDetails);
        final Map<String, Object> activityJobsUpdatedStatusMap = new HashMap<String, Object>();
        activityJobsUpdatedStatusMap.put("isActivityJobsUpdated", true);
        when(hungJobQueryService.cancelActivitiesAndUpdateState(Matchers.eq(neJobId), Matchers.eq(jobName), Matchers.eq(executionIndex), Matchers.eq(maxTimeLimitForAxeUpgradeJobExecutionInHours)))
                .thenReturn(activityJobsUpdatedStatusMap);
        when(hungJobQueryService.prepareAttributesToBeUpdated(JobState.SYSTEM_CANCELLED, cancelledLogMessage)).thenReturn(attributesToBePersisted);
        verify(systemRecorder, times(0)).recordEvent(SHMEvents.SYSTEM_CANCELLED, EventLevel.COARSE, jobName, neJobAttributes.getNodeName(), additionalInfo);
        when(hungJobQueryService.checkRunningNeJobs(mainJobId)).thenReturn(true);

        hungJobsMarkerService.updateHungJobsToSystemCancelled();
        verify(shmLoadControllerLocalService, times(1)).decrementCounter(Matchers.any(SHMActivityRequest.class));
        verify(shmLoadControllerLocalService, times(1)).decrementGlobalCounter(Matchers.any(SHMLoadControllerCounterRequest.class));
    }

    @Test
    public void testUpdateHangingJobsToSystemCancelledForPeriodicJob() {
        setUp();
        final int maxTimeLimitForJobExecutionInHours = 48;
        final int maxTimeLimitForAxeUpgradeJobExecutionInHours = 240;
        final long mainJobId = 11l;
        final int executionIndex = 1;

        final List<Long> jobTemplateIds = new ArrayList<>();
        jobTemplateIds.add(TEMPLATE_ID);

        final String wfsId = "wfsid";
        final List<String> mainJobWorkflows = new ArrayList<>();
        mainJobWorkflows.add(wfsId);

        final long neJobId = 2l;
        final String neWorkflowInstanceId = "neWfsId";
        final String nodeName = "LTE005";
        final JobState state = JobState.RUNNING;
        final String jobName = "BACKUP";
        final List<Long> nejobIds = new ArrayList<>();
        nejobIds.add(neJobId);

        final List<NEJob> neJobsDetails = new ArrayList<>();
        final NEJob neJobAttributes = new NEJob();
        neJobAttributes.setNeJobId(neJobId);
        neJobAttributes.setNeWorkflowInstanceId(neWorkflowInstanceId);
        neJobAttributes.setNodeName(nodeName);
        neJobAttributes.setState(state);
        neJobAttributes.setPlatformType("CPP");
        neJobAttributes.setJobType("BACKUP");
        neJobsDetails.add(neJobAttributes);

        final String additionalInfo = INITIAL_STATE + neJobAttributes.getState();
        final String cancelledLogMessage = String.format(NE_SYSTEM_CANCELLED, jobName, nodeName);
        final Map<String, Object> attributesToBePersisted = new HashMap<>();
        attributesToBePersisted.put(ShmConstants.STATE, state.toString());
        attributesToBePersisted.put(ShmConstants.ENDTIME, new Date());

        when(hungJobQueryService.getHungNeJobs(Matchers.eq(mainJobId), Matchers.eq(maxTimeLimitForJobExecutionInHours), Matchers.eq(maxTimeLimitForAxeUpgradeJobExecutionInHours),
                Matchers.anyString())).thenReturn(neJobsDetails);
        final Map<String, Object> activityJobsUpdatedStatusMap = new HashMap<>();
        activityJobsUpdatedStatusMap.put("isActivityJobsUpdated", true);
        when(hungJobQueryService.cancelActivitiesAndUpdateState(Matchers.eq(neJobId), Matchers.eq(jobName), Matchers.eq(executionIndex), Matchers.eq(maxTimeLimitForJobExecutionInHours)))
                .thenReturn(activityJobsUpdatedStatusMap);
        when(hungJobQueryService.prepareAttributesToBeUpdated(JobState.SYSTEM_CANCELLED, cancelledLogMessage)).thenReturn(attributesToBePersisted);
        when(checkPeriodicity.isJobPeriodic(schedulePropertiesList)).thenReturn(true);
        hungJobsMarkerService.updateHungJobsToSystemCancelled();
        verify(workflowInstanceHelper).sendAllNeDone(businessKey);
        verify(workflowInstanceHelper, times(1)).cancelWorkflowInstance(Matchers.anyString());
        verify(systemRecorder, times(1)).recordEvent(SHMEvents.SYSTEM_CANCELLED, EventLevel.COARSE, jobName, neJobAttributes.getNodeName(), additionalInfo);
        verify(shmLoadControllerLocalService, times(1)).decrementCounter(Matchers.any(SHMActivityRequest.class));
        verify(shmLoadControllerLocalService, times(1)).decrementGlobalCounter(Matchers.any(SHMLoadControllerCounterRequest.class));
    }

    @Test
    public void testdeleteStagedActivityPOs() {
        final int maxTimeLimitForJobExecutionInHours = 48;
        when(hangingJobsConfigParamChangeListener.getMaxTimeLimitForStagedActivitiesInHours()).thenReturn(maxTimeLimitForJobExecutionInHours);
        final List<PersistenceObject> stagedActivitesPoList = new ArrayList<>();
        stagedActivitesPoList.add(persistenceObjectMock);
        when(persistenceObjectMock.getPoId()).thenReturn(12345l);
        when(hungJobQueryService.getStagedActivityPOs(maxTimeLimitForJobExecutionInHours)).thenReturn(stagedActivitesPoList);
        hungJobsMarkerService.deleteStagedActivityPOs();
        verify(hungJobQueryService, times(1)).deleteStagedActivityPO(Matchers.anyLong());
    }

    @Test
    public void testdeleteStagedActivityPOsWhenNoPOsAvailabletoDelete() {
        final int maxTimeLimitForJobExecutionInHours = 48;
        when(hangingJobsConfigParamChangeListener.getMaxTimeLimitForStagedActivitiesInHours()).thenReturn(maxTimeLimitForJobExecutionInHours);
        final List<PersistenceObject> stagedActivitesPoList = new ArrayList<>();
        stagedActivitesPoList.add(persistenceObjectMock);
        when(hungJobQueryService.getStagedActivityPOs(maxTimeLimitForJobExecutionInHours)).thenReturn(Collections.<PersistenceObject> emptyList());
        hungJobsMarkerService.deleteStagedActivityPOs();
        verify(hungJobQueryService, times(0)).deleteStagedActivityPO(Matchers.anyLong());
    }

}
