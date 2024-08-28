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
package com.ericsson.oss.services.shm.jobservice.common;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy.RetryPolicyBuilder;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.exception.DuplicateEntityException;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.job.utils.CreateJobAdditionalDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.shared.constants.PeriodicSchedulerConstants;
import com.ericsson.oss.services.shm.shared.util.JobPropertyUtil;
import com.ericsson.oss.services.shm.shared.util.ProcessVariablesUtil;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RetryPolicy.class)
public class ShmJobHandlerTest {

    @Mock
    private JobFactory jobFactory;

    @Mock
    SystemRecorder systemRecorder;
    @Mock
    private DpsWriter dpsWriter;

    @Mock
    private PersistenceObject poMock;

    @Mock
    private WorkflowInstanceNotifier workflowInstanceServiceLocal;

    @InjectMocks
    ShmJobHandler shmJobHandlerTest;

    @Mock
    JobInfo jobInfoMock;

    @Mock
    NeJobProperty neJobPropertyMock;

    @Mock
    private Map<String, Object> mapMock;

    @Mock
    private DpsReader dpsReaderMock;

    @Mock
    private List<PersistenceObject> persistenceObjectsMock;

    @Mock
    private JobTemplatePersistenceService duplicateJobHandler;

    @Mock
    RetryPolicyBuilder retryPolicyBuilderMock;

    @Mock
    DpsRetryConfigurationParamProvider dpsConfigMock;

    @Mock
    RetryPolicy retryPolicyMock;

    @Mock
    private RetryManager retryManagerMock;

    @Mock
    ProcessVariablesUtil processVariablesUtil;

    @Mock
    CreateJobAdditionalDataProvider axeBackupJobAdditionalDataProvider;

    @Mock
    JobPropertyUtil jobPropertyUtil;

    @Test
    public void testPopulate_withLimitedData() {
        when(jobInfoMock.getMainSchedule()).thenReturn(mapMock);
        when(jobInfoMock.getJobType()).thenReturn(JobTypeEnum.UPGRADE);
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(JobVariables.ACTIVITY_STARTUP_SCHEDULED);
        when(mapMock.get(ShmConstants.SCHEDULINGPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.NAME)).thenReturn("");
        when(jobInfoMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(duplicateJobHandler.createJobTemplate(anyMap(), anyString())).thenReturn(11225l);

        when(dpsWriter.createPO(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class)))
                .thenReturn(poMock);
        when(workflowInstanceServiceLocal.submitWorkFlowInstance(anyMapOf(String.class, Object.class), anyString())).thenReturn("wfsId");
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn("wfsId");

        final Map<String, Object> response = shmJobHandlerTest.populateAndPersistJobConfigurationData(jobInfoMock);
        Assert.assertNotNull(response);
        Assert.assertEquals(JobHandlerErrorCodes.SUCCESS.getResponseDescription(), response.get("errorCode").toString());

    }

    @Test
    public void testPopulate() {
        when(jobInfoMock.getMainSchedule()).thenReturn(mapMock);
        when(jobInfoMock.getJobType()).thenReturn(JobTypeEnum.UPGRADE);
        when(jobInfoMock.getActivitySchedules()).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.NAME)).thenReturn("", "install");
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(JobVariables.ACTIVITY_STARTUP_SCHEDULED);
        when(mapMock.get(ShmConstants.SCHEDULINGPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(jobInfoMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(duplicateJobHandler.createJobTemplate(anyMap(), anyString())).thenReturn(11225l);
        when(dpsWriter.createPO(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class)))
                .thenReturn(poMock);
        when(workflowInstanceServiceLocal.submitWorkFlowInstance(anyMapOf(String.class, Object.class), anyString())).thenReturn("wfsId");
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn("wfsId");
        final Map<String, Object> response = shmJobHandlerTest.populateAndPersistJobConfigurationData(jobInfoMock);
        Assert.assertNotNull(response);
        Assert.assertEquals(JobHandlerErrorCodes.SUCCESS.getResponseDescription(), response.get("errorCode").toString());

    }

    @Test
    public void testPopulate_1() {
        when(jobInfoMock.getMainSchedule()).thenReturn(mapMock);
        when(jobInfoMock.getJobType()).thenReturn(JobTypeEnum.LICENSE);
        when(jobInfoMock.getActivitySchedules()).thenReturn(Arrays.asList(mapMock));
        when(jobInfoMock.getNeJobProperties()).thenReturn(Arrays.asList(neJobPropertyMock));
        when(jobInfoMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(mapMock.get(ShmConstants.NAME)).thenReturn("", "install", PeriodicSchedulerConstants.REPEAT_ON);
        when(mapMock.get(ShmConstants.VALUE)).thenReturn("", "", "some , date");
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(JobVariables.ACTIVITY_STARTUP_SCHEDULED);
        when(mapMock.get(ShmConstants.SCHEDULINGPROPERTIES)).thenReturn(Arrays.asList(mapMock, mapMock));
        when(duplicateJobHandler.createJobTemplate(anyMap(), anyString())).thenReturn(11225l);
        when(dpsWriter.createPO(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class)))
                .thenReturn(poMock);
        when(workflowInstanceServiceLocal.submitWorkFlowInstance(anyMapOf(String.class, Object.class), anyString())).thenReturn("wfsId");
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn("wfsId");

        final Map<String, Object> response = shmJobHandlerTest.populateAndPersistJobConfigurationData(jobInfoMock);
        Assert.assertNotNull(response);
        Assert.assertEquals(JobHandlerErrorCodes.SUCCESS.getResponseDescription(), response.get("errorCode").toString());

    }

    @Test
    public void testPopulate_2() {
        when(jobInfoMock.getMainSchedule()).thenReturn(mapMock);
        when(jobInfoMock.getJobType()).thenReturn(JobTypeEnum.BACKUP);
        when(jobInfoMock.getActivitySchedules()).thenReturn(Arrays.asList(mapMock));
        when(jobInfoMock.getNeJobProperties()).thenReturn(Arrays.asList(neJobPropertyMock));
        when(jobInfoMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(mapMock.get(ShmConstants.NAME)).thenReturn(ShmConstants.REPEAT_COUNT, "install", PeriodicSchedulerConstants.REPEAT_COUNT);
        when(mapMock.get(ShmConstants.VALUE)).thenReturn("2", "5");
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(JobVariables.ACTIVITY_STARTUP_SCHEDULED);
        when(mapMock.get(ShmConstants.SCHEDULINGPROPERTIES)).thenReturn(Arrays.asList(mapMock, mapMock));
        when(duplicateJobHandler.createJobTemplate(anyMap(), anyString())).thenReturn(11225l);
        when(dpsReaderMock.findPOs(anyString(), anyString(), anyMap())).thenReturn(persistenceObjectsMock);
        when(persistenceObjectsMock.isEmpty()).thenReturn(true);

        when(dpsWriter.createPO(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class)))
                .thenReturn(poMock);

        final Map<String, Object> response = shmJobHandlerTest.populateAndPersistJobConfigurationData(jobInfoMock);
        Assert.assertNotNull(response);

    }

    @Test
    public void testPopulateFail() {
        when(jobInfoMock.getMainSchedule()).thenReturn(mapMock);
        when(jobInfoMock.getJobType()).thenReturn(JobTypeEnum.UPGRADE);
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(JobVariables.ACTIVITY_STARTUP_SCHEDULED);
        when(mapMock.get(ShmConstants.SCHEDULINGPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.NAME)).thenReturn(ShmConstants.REPEAT_TYPE);

        when(dpsWriter.createPO(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class)))
                .thenReturn(poMock);
        when(workflowInstanceServiceLocal.submitWorkFlowInstance(anyMapOf(String.class, Object.class), anyString())).thenReturn("wfsId");

        final Map<String, Object> response = shmJobHandlerTest.populateAndPersistJobConfigurationData(jobInfoMock);
        Assert.assertNotNull(response);
        Assert.assertEquals(JobHandlerErrorCodes.JOB_CFGN_PERSISTENCE_FAILED.getResponseDescription() + ". " + ShmConstants.ERROR_MSG,
                response.get("errorCode").toString());
    }

    @Test
    public void testPopulate_jobTemplateCreationFailed() {
        when(jobInfoMock.getMainSchedule()).thenReturn(mapMock);
        when(jobInfoMock.getJobType()).thenReturn(JobTypeEnum.UPGRADE);
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(JobVariables.ACTIVITY_STARTUP_SCHEDULED);
        when(mapMock.get(ShmConstants.SCHEDULINGPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.NAME)).thenReturn("");
        when(jobInfoMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(dpsWriter.createPO(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class)))
                .thenReturn(null);
        when(workflowInstanceServiceLocal.submitWorkFlowInstance(anyMapOf(String.class, Object.class), anyString())).thenReturn("wfsId");

        final Map<String, Object> response = shmJobHandlerTest.populateAndPersistJobConfigurationData(jobInfoMock);
        Assert.assertNotNull(response);
        Assert.assertEquals(JobHandlerErrorCodes.JOB_CFGN_PERSISTENCE_FAILED.getResponseDescription(), response.get("errorCode").toString());
        verify(workflowInstanceServiceLocal, never()).submitWorkFlowInstance(anyMapOf(String.class, Object.class), anyString());
        verify(systemRecorder).recordEvent(SHMEvents.JOB_CREATE, EventLevel.COARSE, "", "", "Job creation is failed");
    }

    @Test(expected = DuplicateEntityException.class)
    public void createJobShouldFailForDuplicateJobNameTest() {

        when(jobInfoMock.getMainSchedule()).thenReturn(mapMock);
        when(jobInfoMock.getJobType()).thenReturn(JobTypeEnum.BACKUP);
        when(jobInfoMock.getActivitySchedules()).thenReturn(Arrays.asList(mapMock));
        when(jobInfoMock.getNeJobProperties()).thenReturn(Arrays.asList(neJobPropertyMock));
        when(mapMock.get(ShmConstants.NAME)).thenReturn(ShmConstants.REPEAT_COUNT, "install", PeriodicSchedulerConstants.REPEAT_COUNT);
        when(mapMock.get(ShmConstants.VALUE)).thenReturn("2", "5");
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(JobVariables.ACTIVITY_STARTUP_SCHEDULED);
        when(mapMock.get(ShmConstants.SCHEDULINGPROPERTIES)).thenReturn(Arrays.asList(mapMock, mapMock));
        when(jobInfoMock.getJobCategory()).thenReturn(JobCategory.UI);
        when(duplicateJobHandler.createJobTemplate(anyMap(), anyString())).thenThrow(DuplicateEntityException.class);
        when(dpsReaderMock.findPOs(anyString(), anyString(), anyMap())).thenReturn(persistenceObjectsMock);
        when(persistenceObjectsMock.isEmpty()).thenReturn(false);

        shmJobHandlerTest.populateAndPersistJobConfigurationData(jobInfoMock);
    }

    @Test
    public void testPopulateAndPersistJobConfigurationData() {
        when(jobInfoMock.getMainSchedule()).thenReturn(mapMock);
        when(jobInfoMock.getJobType()).thenReturn(JobTypeEnum.BACKUP);
        when(jobInfoMock.getActivitySchedules()).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.NAME)).thenReturn("", "createbackup");
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(JobVariables.ACTIVITY_STARTUP_SCHEDULED);
        when(mapMock.get(ShmConstants.SCHEDULINGPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(jobInfoMock.getJobCategory()).thenReturn(JobCategory.UI);
        List<NeNamesWithSelectedComponents> neNamesWithSelectedComponentsList = new ArrayList<>();
        NeNamesWithSelectedComponents neNamesWithSelectedComponents = new NeNamesWithSelectedComponents();
        neNamesWithSelectedComponents.setParentNeName("MSC18");
        neNamesWithSelectedComponentsList.add(neNamesWithSelectedComponents);
        List<String> comp = new ArrayList<>();
        comp.add("MSC18__APG");
        neNamesWithSelectedComponents.setSelectedComponents(comp);
        neNamesWithSelectedComponents.getSelectedComponents();
        Map<String, String> neComponentWithApgVersion = new HashMap<>();
        neComponentWithApgVersion.put("MSC18__APG", "3.7.0");
        List<Map<String, String>> jobProperties = new ArrayList<>();
        NeJobProperty neJobProperty = new NeJobProperty();
        neJobProperty.setNeName("MSC18__APG");
        neJobProperty.setJobProperties(jobProperties);
        List<NeJobProperty> neJobPropertiesInJobInfo = new ArrayList<>();
        neJobPropertiesInJobInfo.add(neJobProperty);
        NeTypeJobProperty neTypeJobProperty = new NeTypeJobProperty();
        neTypeJobProperty.setNeType("MSC-BC-BSP");
        neTypeJobProperty.setJobProperties(jobProperties);
        List<NeTypeJobProperty> neTypeJobPropertiesInJobInfo = new ArrayList<>();
        neTypeJobPropertiesInJobInfo.add(neTypeJobProperty);
        Map<String, Object> jobConfiguration = new HashMap<>();
        when(jobInfoMock.getParentNeWithComponents()).thenReturn(neNamesWithSelectedComponentsList);
        when(jobInfoMock.getNeJobProperties()).thenReturn(neJobPropertiesInJobInfo);
        when(duplicateJobHandler.createJobTemplate(anyMap(), anyString())).thenReturn(11225l);
        when(dpsWriter.createPO(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class)))
                .thenReturn(poMock);
        when(workflowInstanceServiceLocal.submitWorkFlowInstance(anyMapOf(String.class, Object.class), anyString())).thenReturn("wfsId");
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn("wfsId");
        final Map<String, Object> response = shmJobHandlerTest.populateAndPersistJobConfigurationData(jobInfoMock);
        verify(axeBackupJobAdditionalDataProvider, never()).readAndEncryptPasswordProperty(jobProperties, "12345");
        Assert.assertNotNull(response);
        Assert.assertEquals(JobHandlerErrorCodes.SUCCESS.getResponseDescription(), response.get("errorCode").toString());

    }

    @Test
    public void testPopulateAndPersistJobConfigurationDataWithEmptyPasswordInNeTypeJobProperty() {
        when(jobInfoMock.getMainSchedule()).thenReturn(mapMock);
        when(jobInfoMock.getJobType()).thenReturn(JobTypeEnum.BACKUP);
        when(jobInfoMock.getActivitySchedules()).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.NAME)).thenReturn("", "createbackup");
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(JobVariables.ACTIVITY_STARTUP_SCHEDULED);
        when(mapMock.get(ShmConstants.SCHEDULINGPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(jobInfoMock.getJobCategory()).thenReturn(JobCategory.UI);
        List<NeNamesWithSelectedComponents> neNamesWithSelectedComponentsList = new ArrayList<>();
        NeNamesWithSelectedComponents neNamesWithSelectedComponents = new NeNamesWithSelectedComponents();
        neNamesWithSelectedComponents.setParentNeName("MSC18");
        neNamesWithSelectedComponentsList.add(neNamesWithSelectedComponents);
        List<String> comp = new ArrayList<>();
        comp.add("MSC18__APG");
        neNamesWithSelectedComponents.setSelectedComponents(comp);
        neNamesWithSelectedComponents.getSelectedComponents();
        Map<String, String> neComponentWithApgVersion = new HashMap<>();
        neComponentWithApgVersion.put("MSC18__APG", "3.7.0");
        List<Map<String, String>> neJobProperties = new ArrayList<>();
        List<Map<String, String>> neTypeJobProperties = new ArrayList<>();
        Map<String, String> neTypeJobProp = new HashMap<>();
        neTypeJobProp.put("key", "Password");
        neTypeJobProp.put("value", "");
        NeJobProperty neJobProperty = new NeJobProperty();
        neJobProperty.setNeName("MSC18__APG");
        neJobProperty.setJobProperties(neJobProperties);
        List<NeJobProperty> neJobPropertiesInJobInfo = new ArrayList<>();
        neJobPropertiesInJobInfo.add(neJobProperty);
        NeTypeJobProperty neTypeJobProperty = new NeTypeJobProperty();
        neTypeJobProperty.setNeType("MSC-BC-BSP");
        neTypeJobProperty.setJobProperties(neTypeJobProperties);
        List<NeTypeJobProperty> neTypeJobPropertiesInJobInfo = new ArrayList<>();
        neTypeJobPropertiesInJobInfo.add(neTypeJobProperty);
        Map<String, Object> jobConfiguration = new HashMap<>();
        when(jobInfoMock.getParentNeWithComponents()).thenReturn(neNamesWithSelectedComponentsList);
        when(jobInfoMock.getNeJobProperties()).thenReturn(neJobPropertiesInJobInfo);
        when(jobInfoMock.getNETypeJobProperties()).thenReturn(neTypeJobPropertiesInJobInfo);
        when(duplicateJobHandler.createJobTemplate(anyMap(), anyString())).thenReturn(11225l);
        when(dpsWriter.createPO(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class)))
                .thenReturn(poMock);
        when(workflowInstanceServiceLocal.submitWorkFlowInstance(anyMapOf(String.class, Object.class), anyString())).thenReturn("wfsId");
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn("wfsId");
        final Map<String, Object> response = shmJobHandlerTest.populateAndPersistJobConfigurationData(jobInfoMock);
        verify(axeBackupJobAdditionalDataProvider, never()).readAndEncryptPasswordProperty(neTypeJobProperties, "1234");
        Assert.assertNotNull(response);
        Assert.assertEquals(JobHandlerErrorCodes.SUCCESS.getResponseDescription(), response.get("errorCode").toString());
    }

    @Test
    public void testPopulateAndPersistJobConfigurationDataWithNonEmptyPasswordInNeTypeJobProperty() {
        when(jobInfoMock.getMainSchedule()).thenReturn(mapMock);
        when(jobInfoMock.getJobType()).thenReturn(JobTypeEnum.BACKUP);
        when(jobInfoMock.getActivitySchedules()).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.NAME)).thenReturn("", "createbackup");
        when(mapMock.get(ShmConstants.EXECUTION_MODE)).thenReturn(JobVariables.ACTIVITY_STARTUP_SCHEDULED);
        when(mapMock.get(ShmConstants.SCHEDULINGPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(jobInfoMock.getJobCategory()).thenReturn(JobCategory.UI);
        List<NeNamesWithSelectedComponents> neNamesWithSelectedComponentsList = new ArrayList<>();
        NeNamesWithSelectedComponents neNamesWithSelectedComponents = new NeNamesWithSelectedComponents();
        neNamesWithSelectedComponents.setParentNeName("MSC18");
        neNamesWithSelectedComponentsList.add(neNamesWithSelectedComponents);
        List<String> comp = new ArrayList<>();
        comp.add("MSC18__APG");
        neNamesWithSelectedComponents.setSelectedComponents(comp);
        neNamesWithSelectedComponents.getSelectedComponents();
        Map<String, String> neComponentWithApgVersion = new HashMap<>();
        neComponentWithApgVersion.put("MSC18__APG", "3.7.0");
        List<Map<String, String>> neJobProperties = new ArrayList<>();
        List<Map<String, String>> neTypeJobProperties = new ArrayList<>();
        Map<String, String> neTypeJobProp = new HashMap<>();
        neTypeJobProp.put("key", "Password");
        neTypeJobProp.put("value", "1234");
        NeJobProperty neJobProperty = new NeJobProperty();
        neJobProperty.setNeName("MSC18__APG");
        neJobProperty.setJobProperties(neJobProperties);
        List<NeJobProperty> neJobPropertiesInJobInfo = new ArrayList<>();
        neJobPropertiesInJobInfo.add(neJobProperty);
        NeTypeJobProperty neTypeJobProperty = new NeTypeJobProperty();
        neTypeJobProperty.setNeType("MSC-BC-BSP");
        neTypeJobProperties.add(neTypeJobProp);
        neTypeJobProperty.setJobProperties(neTypeJobProperties);
        List<NeTypeJobProperty> neTypeJobPropertiesInJobInfo = new ArrayList<>();
        neTypeJobPropertiesInJobInfo.add(neTypeJobProperty);
        Map<String, Object> jobConfiguration = new HashMap<>();
        when(jobInfoMock.getParentNeWithComponents()).thenReturn(neNamesWithSelectedComponentsList);
        when(jobInfoMock.getNeJobProperties()).thenReturn(null);
        when(jobInfoMock.getNETypeJobProperties()).thenReturn(neTypeJobPropertiesInJobInfo);
        when(duplicateJobHandler.createJobTemplate(anyMap(), anyString())).thenReturn(11225l);
        when(dpsWriter.createPO(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class)))
                .thenReturn(poMock);
        when(workflowInstanceServiceLocal.submitWorkFlowInstance(anyMapOf(String.class, Object.class), anyString())).thenReturn("wfsId");
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn("wfsId");
        final Map<String, Object> response = shmJobHandlerTest.populateAndPersistJobConfigurationData(jobInfoMock);
        verify(axeBackupJobAdditionalDataProvider, times(1)).readAndEncryptPasswordProperty(neTypeJobProperties, "1234");
        Assert.assertNotNull(response);
        Assert.assertEquals(JobHandlerErrorCodes.SUCCESS.getResponseDescription(), response.get("errorCode").toString());
    }

}
