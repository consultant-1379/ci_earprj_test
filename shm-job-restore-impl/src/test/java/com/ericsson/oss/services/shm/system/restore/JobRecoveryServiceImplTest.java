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
package com.ericsson.oss.services.shm.system.restore;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.jobs.common.api.CheckPeriodicity;
import com.ericsson.oss.services.shm.jobs.common.api.NEJob;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ScheduleProperty;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.shared.util.ProcessVariablesUtil;
import com.ericsson.oss.services.shm.system.restore.common.JobQueryService;
import com.ericsson.oss.services.shm.system.restore.common.JobRestoreHandlingServiceUtil;
import com.ericsson.oss.services.shm.system.restore.common.MainJob;
import com.ericsson.oss.services.shm.workflow.BatchWorkFlowProcessVariables;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;
import com.ericsson.oss.services.wfs.api.instance.WorkflowInstance;

@RunWith(MockitoJUnitRunner.class)
public class JobRecoveryServiceImplTest {

    private static final long mainJobId = 123L;
    private static final long neJobId = 123L;
    private static final long templateJobId = 456L;
    private static final String workflowInstanceId = "anyWFid";
    private static final String mainJobName = "Job123";
    private static final String jobType = "BACKUP";
    private static final int executionIndex = 1;

    @InjectMocks
    JobRecoveryServiceImpl objectUnderTest;

    @Mock
    JobRestoreHandlingServiceUtil jobRestoreHandlingServiceUtil;

    @Mock
    DpsReader dpsReader;

    @Mock
    NEJob neJobAttributes;

    @Mock
    WorkflowInstanceNotifier workflowInstanceServiceLocal;

    @Mock
    PersistenceObject persistenceObject;

    @Mock
    JobLogUtil jobLogUtil;

    @Mock
    SystemRecorder systemRecorder;

    @Mock
    ProcessVariablesUtil shmJobHandler;

    @Mock
    DpsWriter dpsWriter;

    @Mock
    JobQueryService jobQueryService;

    @Mock
    CheckPeriodicity checkPeriodicity;

    private final WorkflowInstance workflowInstance = new WorkflowInstance("workflowDefID", workflowInstanceId, Long.toString(templateJobId));

    @Test
    public void testHandleNeJobRestore() {
        when(jobRestoreHandlingServiceUtil.getSuspendedNEJobDetails(mainJobId)).thenAnswer(new Answer<List<NEJob>>() {
            @Override
            public List<NEJob> answer(final InvocationOnMock invocation) throws Throwable {
                when(neJobAttributes.getNeJobId()).thenReturn(neJobId);
                when(neJobAttributes.getNeWorkflowInstanceId()).thenReturn(workflowInstanceId);

                final List<NEJob> neJobAttributesList = new ArrayList<>(1);
                neJobAttributesList.add(neJobAttributes);

                return neJobAttributesList;
            }
        });
        Mockito.doNothing().when(workflowInstanceServiceLocal).cancelWorkflowInstance(workflowInstanceId);

        //when(dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.ACTIVITYJOB_TYPE, Matchers.anyMap())).thenReturn(value);
        objectUnderTest.handleNeJobRestore(mainJobId, mainJobName, executionIndex);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleJobRestore() {
        final List<MainJob> mainJobAttributesList = new ArrayList<MainJob>();
        final MainJob mainJobAttributes = new MainJob();
        mainJobAttributes.setMainJobState(JobState.RUNNING);
        mainJobAttributes.setTemplateJobId(templateJobId);

        final Schedule mainSchedule = new Schedule();
        final List<Map<String, Object>> scheduledProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> attributeList = new HashMap<String, Object>();
        mainSchedule.setExecMode(ExecMode.SCHEDULED);
        final List<ScheduleProperty> scheduleAttributes = new ArrayList<ScheduleProperty>();

        final ScheduleProperty scheduleAttributeRepeatType = new ScheduleProperty();
        scheduleAttributeRepeatType.setName(BatchWorkFlowProcessVariables.REPEAT_TYPE);
        scheduleAttributeRepeatType.setValue("Daily");
        scheduleAttributes.add(scheduleAttributeRepeatType);

        final ScheduleProperty scheduleAttributeRepeatCount = new ScheduleProperty();
        scheduleAttributeRepeatCount.setName("REPEAT_COUNT");
        scheduleAttributeRepeatCount.setValue("2");
        scheduleAttributes.add(scheduleAttributeRepeatCount);

        final ScheduleProperty scheduleAttributeStartDate = new ScheduleProperty();
        scheduleAttributeStartDate.setName("START_DATE");
        scheduleAttributeStartDate.setValue("2015-9-02 11:00:00 GMT+0530");
        scheduleAttributes.add(scheduleAttributeStartDate);
        for (final ScheduleProperty scheduleProperty : scheduleAttributes) {
            attributeList.put(scheduleProperty.getName(), scheduleProperty.getValue());
        }
        scheduledProperties.add(attributeList);
        mainSchedule.setScheduleAttributes(scheduleAttributes);
        mainJobAttributes.setMainSchedule(mainSchedule);
        mainJobAttributesList.add(mainJobAttributes);
        when(jobRestoreHandlingServiceUtil.getSuspendedJobDetails()).thenReturn(mainJobAttributesList);
        when(jobQueryService.getMainJobsInActiveState()).thenReturn(mainJobAttributesList);
        final Map<String, Object> templateJobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();

        jobConfigurationDetails.put(ShmConstants.MAIN_SCHEDULE, mainSchedule);
        templateJobAttributes.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfigurationDetails);
        templateJobAttributes.put(ShmConstants.JOB_TYPE, jobType);
        when(checkPeriodicity.isJobPeriodic(scheduledProperties)).thenReturn(true);
        when(persistenceObject.getAllAttributes()).thenReturn(templateJobAttributes);
        when(dpsReader.findPOByPoId(templateJobId)).thenReturn(persistenceObject);

        when(workflowInstanceServiceLocal.submitAndGetWorkFlowInstance(Matchers.anyString(), Matchers.anyMap())).thenReturn(workflowInstance);

        objectUnderTest.handleJobRestore();

        verify(jobRestoreHandlingServiceUtil).getSuspendedJobDetails();
        verify(workflowInstanceServiceLocal).cancelWorkflowInstance(Matchers.anyString());
        verify(systemRecorder, times(4)).recordEvent(Matchers.anyString(), Matchers.any(EventLevel.class), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        //        verify(workflowInstanceServiceLocal).startWorkflowInstanceByDefinitionId(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap());
    }
}
