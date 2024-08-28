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
package com.ericsson.oss.services.shm.job.resources;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.service.SHMJobService;
import com.ericsson.oss.services.shm.job.utils.SHMJobUtil;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.JobSchedulerConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobParamMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.*;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobParam;
import com.ericsson.oss.services.shm.jobs.common.restentities.RestJobConfiguration;

@RunWith(PowerMockRunner.class)
public class RestDataMapperTest {

    @Mock
    JobConfiguration jobConfiguration;

    @Mock
    JobTemplate jobTemplateMock;

    @Mock
    Date dateMock;

    @Mock
    Schedule scheduleMock;

    @Mock
    ScheduleProperty schedulePropertyMock;

    @Mock
    Activity activityMock;

    @Mock
    Map<String, Object> mapMock;

    @Mock
    JobParam jobParamMock;

    @Mock
    JobProperty jobPropertyMock;

    @Mock
    SHMJobUtil shmJobUtilMock;

    @InjectMocks
    RestDataMapper restMapper;

    @Mock
    SHMJobService shmJobServiceMock;

    @Mock
    private PersistenceObject poMock;

    @Mock
    private DpsReader dpsReader;

    @Mock
    private List<PersistenceObject> persistenceObjectsMock;

    long jobConfigId = 12345678;

    @Mock
    JobParamMapper jobParamMapperMock;

    @Mock
    ActivityInfo activityInfoMock;

    @Mock
    NeTypeJobProperty neTypeJobPropertyMock;

    @Mock
    FdnServiceBean fdnServiceBeanMock;

    @Mock
    NEInfo neInfoMock;

    @Mock
    NetworkElement networkElementMock;

    @Mock
    JobCapabilityProvider jobCapabilityProvider;

    final List<NetworkElement> networkElementList = new ArrayList<>();

    @Before
    public void setMockData() {
        networkElementList.add(networkElementMock);
        when(jobConfiguration.getSelectedNEs()).thenReturn(neInfoMock);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(networkElementList);
    }

    @Test
    public void test_mapJobConfigToRestDataFormat_withLimitedData() {
        final List<String> neNames = Arrays.asList("LTE01", "LTE02");
        final String capabilityBackup = SHMCapabilities.BACKUP_JOB_CAPABILITY;
        when(jobTemplateMock.getCreationTime()).thenReturn(dateMock);
        when(dateMock.toString()).thenReturn("dummyDate");
        when(jobTemplateMock.getJobType()).thenReturn(JobType.BACKUP);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobTemplateMock.getJobTemplateId()).thenReturn(jobConfigId);
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(jobCapabilityProvider.getCapability(JobTypeEnum.BACKUP)).thenReturn(capabilityBackup);
        when(dpsReader.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(persistenceObjectsMock);
        when(shmJobServiceMock.getJobStartTime(jobConfigId)).thenReturn("2014-09-08 16:22:55 GMT+0530");
        when(neInfoMock.getNeNames()).thenReturn(neNames);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(neNames, capabilityBackup)).thenReturn(networkElementList);

        final RestJobConfiguration result = restMapper.mapJobConfigToRestDataFormat(jobTemplateMock);
        Assert.assertNotNull(result);
        Assert.assertEquals(JobType.BACKUP.name(), result.getJobType());
        Assert.assertNotNull(result.getCreatedOn());
        Assert.assertNotNull(result.getStartTime());
        Assert.assertEquals(0, result.getJobParams().size());
        Assert.assertEquals(networkElementList, result.getSelectedNEs().getNetworkElements());
    }

    @Test
    public void test_mapJobConfigToRestDataFormat_tocheckMainSchedule() {
        when(jobTemplateMock.getCreationTime()).thenReturn(dateMock);
        when(dateMock.toString()).thenReturn("dummyDate");
        when(jobTemplateMock.getJobType()).thenReturn(JobType.BACKUP);
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobTemplateMock.getJobTemplateId()).thenReturn(jobConfigId);
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.SCHEDULED);
        when(scheduleMock.getScheduleAttributes()).thenReturn(Arrays.asList(schedulePropertyMock));
        when(schedulePropertyMock.getName()).thenReturn(JobSchedulerConstants.START_DATE);
        when(schedulePropertyMock.getValue()).thenReturn("2014-09-08 16:22:55 GMT+0530");
        when(dpsReader.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(persistenceObjectsMock);
        when(shmJobServiceMock.getJobStartTime(jobConfigId)).thenReturn("2014-09-08 16:22:55 GMT+0530");

        final RestJobConfiguration result = restMapper.mapJobConfigToRestDataFormat(jobTemplateMock);
        Assert.assertNotNull(result);
        Assert.assertEquals(JobType.BACKUP.name(), result.getJobType());
        Assert.assertNotNull(result.getCreatedOn());
        Assert.assertNotNull(result.getStartTime());
        Assert.assertEquals(0, result.getJobParams().size());
    }

    @Test
    public void test_mapJobConfigToRestDataFormat_toCheckActivities_AndJobParam_asNull() {
        when(jobTemplateMock.getCreationTime()).thenReturn(dateMock);
        when(dateMock.toString()).thenReturn("dummyDate");
        when(jobTemplateMock.getJobType()).thenReturn(JobType.UPGRADE);
        when(jobConfiguration.getActivities()).thenReturn(Arrays.asList(activityMock));
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobTemplateMock.getJobTemplateId()).thenReturn(jobConfigId);
        when(activityMock.getNeType()).thenReturn("ERBS");
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(jobConfiguration.getJobProperties()).thenReturn(Arrays.asList(jobPropertyMock));
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(jobParamMapperMock.createJobparam(Arrays.asList(jobPropertyMock), Arrays.asList(activityInfoMock))).thenReturn(null);
        when(activityMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        when(activityMock.getSchedule()).thenReturn(scheduleMock);
        when(activityMock.getName()).thenReturn(JobPropertyConstants.CREATE_CV);
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(dpsReader.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(persistenceObjectsMock);
        when(shmJobServiceMock.getJobStartTime(jobConfigId)).thenReturn("2014-09-08 16:22:55 GMT+0530");

        final RestJobConfiguration result = restMapper.mapJobConfigToRestDataFormat(jobTemplateMock);
        Assert.assertNotNull(result);
        Assert.assertEquals(JobType.UPGRADE.name(), result.getJobType());
        Assert.assertNotNull(result.getCreatedOn());
        Assert.assertNotNull(result.getStartTime());
        Assert.assertEquals(1, result.getJobParams().size());
        Assert.assertNull(result.getJobParams().get(0));
    }

    @Test
    public void test_mapJobConfigToRestDataFormat_toCheckActivities_jobParams() {
        when(jobTemplateMock.getCreationTime()).thenReturn(dateMock);
        when(dateMock.toString()).thenReturn("dummyDate");
        when(jobTemplateMock.getJobType()).thenReturn(JobType.BACKUP);

        when(jobConfiguration.getActivities()).thenReturn(Arrays.asList(activityMock));
        when(jobConfiguration.getJobProperties()).thenReturn(Arrays.asList(jobPropertyMock));
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobTemplateMock.getJobTemplateId()).thenReturn(jobConfigId);
        when(activityMock.getNeType()).thenReturn("ERBS");
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(activityMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        when(activityMock.getSchedule()).thenReturn(scheduleMock);
        when(activityMock.getName()).thenReturn(JobPropertyConstants.SET_CV_AS_STARTABLE);
        when(activityMock.getOrder()).thenReturn(0);
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(dpsReader.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(persistenceObjectsMock);
        when(shmJobServiceMock.getJobStartTime(jobConfigId)).thenReturn("2014-09-08 16:22:55 GMT+0530");
        when(jobParamMapperMock.createJobparam(Matchers.anyList(), Matchers.anyList())).thenReturn(jobParamMock);
        final RestJobConfiguration result = restMapper.mapJobConfigToRestDataFormat(jobTemplateMock);
        Assert.assertNotNull(result);
        Assert.assertEquals(JobType.BACKUP.name(), result.getJobType());
        Assert.assertNotNull(result.getCreatedOn());
        Assert.assertNotNull(result.getStartTime());
        Assert.assertEquals(1, result.getJobParams().size());
        Assert.assertEquals(jobParamMock, result.getJobParams().get(0));
    }

    @Test
    public void test_mapJobConfigToRestDataFormat_toCheckActivities_jobParamsCallsConvertTimeZone() {
        when(jobTemplateMock.getCreationTime()).thenReturn(dateMock);
        when(dateMock.toString()).thenReturn("dummyDate");
        when(jobTemplateMock.getJobType()).thenReturn(JobType.BACKUP);

        when(jobConfiguration.getActivities()).thenReturn(Arrays.asList(activityMock));
        when(jobConfiguration.getJobProperties()).thenReturn(Arrays.asList(jobPropertyMock));
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobTemplateMock.getJobTemplateId()).thenReturn(jobConfigId);
        when(activityMock.getNeType()).thenReturn("SGSN-MME");
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(activityMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        when(activityMock.getSchedule()).thenReturn(scheduleMock);
        when(activityMock.getName()).thenReturn(JobPropertyConstants.SET_CV_FIRST_IN_ROLLBACK_LIST);
        when(activityMock.getOrder()).thenReturn(0);
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(scheduleMock.getScheduleAttributes()).thenReturn(Arrays.asList(schedulePropertyMock));
        when(schedulePropertyMock.getName()).thenReturn(JobSchedulerConstants.START_DATE);
        when(schedulePropertyMock.getValue()).thenReturn("2014-09-08 16:22:55 GMT+0530");
        when(dpsReader.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(persistenceObjectsMock);
        when(shmJobServiceMock.getJobStartTime(jobConfigId)).thenReturn("2014-09-08 16:22:55 GMT+0530");
        when(jobParamMapperMock.createJobparam(Matchers.anyList(), Matchers.anyList())).thenReturn(jobParamMock);

        final RestJobConfiguration result = restMapper.mapJobConfigToRestDataFormat(jobTemplateMock);
        Assert.assertNotNull(result);
        Assert.assertEquals(JobType.BACKUP.name(), result.getJobType());
        Assert.assertNotNull(result.getCreatedOn());
        Assert.assertNotNull(result.getStartTime());
        Assert.assertEquals(1, result.getJobParams().size());
        Assert.assertEquals(jobParamMock, result.getJobParams().get(0));
    }

    @Test
    public void test_mapJobConfigToRestDataFormat_tocheckScheduledAttributes() {
        final List<ScheduleProperty> schedulePropertyList = new ArrayList<>();
        final ScheduleProperty scheduleProperty1 = new ScheduleProperty();
        scheduleProperty1.setName(JobSchedulerConstants.OCCURRENCES);
        scheduleProperty1.setValue("7");
        final ScheduleProperty scheduleProperty2 = new ScheduleProperty();
        scheduleProperty2.setName(JobSchedulerConstants.REPEAT_TYPE);
        scheduleProperty2.setValue("Daily");
        final ScheduleProperty scheduleProperty3 = new ScheduleProperty();
        scheduleProperty3.setName(JobSchedulerConstants.END_DATE);
        scheduleProperty3.setValue("2015-7-31 00:00:00 GMT+0530");
        final ScheduleProperty scheduleProperty4 = new ScheduleProperty();
        scheduleProperty4.setName(JobSchedulerConstants.START_DATE);
        scheduleProperty4.setValue("2015-7-31 00:00:00 GMT+0130");
        schedulePropertyList.add(scheduleProperty1);
        schedulePropertyList.add(scheduleProperty2);
        schedulePropertyList.add(scheduleProperty3);
        schedulePropertyList.add(scheduleProperty4);
        when(jobTemplateMock.getCreationTime()).thenReturn(dateMock);
        when(jobTemplateMock.getJobTemplateId()).thenReturn(jobConfigId);
        when(dateMock.toString()).thenReturn("dummyDate");
        when(jobTemplateMock.getJobType()).thenReturn(JobType.BACKUP);
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.SCHEDULED);
        when(scheduleMock.getScheduleAttributes()).thenReturn(schedulePropertyList);
        when(dpsReader.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(persistenceObjectsMock);
        when(shmJobServiceMock.getJobStartTime(jobConfigId)).thenReturn("2014-09-08 16:22:55 GMT+0530");

        final RestJobConfiguration result = restMapper.mapJobConfigToRestDataFormat(jobTemplateMock);
        Assert.assertNotNull(result);
        Assert.assertEquals(JobType.BACKUP.name(), result.getJobType());
        Assert.assertNotNull(result.getCreatedOn());
        Assert.assertNotNull(result.getStartTime());
        Assert.assertEquals(0, result.getJobParams().size());
        Assert.assertNotNull(result.getScheduleJobConfiguration());
        Assert.assertEquals("7", result.getScheduleJobConfiguration().getOccurences());
        Assert.assertEquals("Daily", result.getScheduleJobConfiguration().getRepeatType());
        // Assert.assertEquals("1438281000000", result.getScheduleJobConfiguration().getEndDate());
    }

    /*
     * @Test public void test_mapJobConfigToRestDataFormat_toCheckActivities_AndJobParam_asNull_And_neTypeAsNull() { when(jobTemplateMock.getCreationTime()).thenReturn(dateMock);
     * when(jobConfiguration.getNeTypeJobProperties()).thenReturn(Arrays.asList(neTypeJobPropertyMock)); when(dateMock.toString()).thenReturn("dummyDate");
     * when(jobTemplateMock.getJobType()).thenReturn(JobType.UPGRADE); when(jobConfiguration.getActivities()).thenReturn(Arrays.asList(activityMock));
     * when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration); when(activityMock.getNeType()).thenReturn(null);
     * when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock); when(jobConfiguration.getJobProperties()).thenReturn(null); when(scheduleMock.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
     * when(jobParamMapperMock.createJobparam(Arrays.asList(jobPropertyMock), Arrays.asList(activityInfoMock))).thenReturn(null); when(activityMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
     * when(activityMock.getSchedule()).thenReturn(scheduleMock); when(activityMock.getName()).thenReturn(JobPropertyConstants.CREATE_CV);
     * when(scheduleMock.getExecMode()).thenReturn(ExecMode.IMMEDIATE); when(dpsReader.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class,
     * Object.class))).thenReturn(persistenceObjectsMock); when(shmJobServiceMock.getStartTimeForImmediateAndManualJobs(jobConfigId)).thenReturn("2014-09-08 16:22:55 GMT+0530");
     *
     * final RestJobConfiguration result = restMapper.mapJobConfigToRestDataFormat(jobTemplateMock, jobConfigId); Assert.assertNotNull(result); Assert.assertEquals(JobType.UPGRADE.name(),
     * result.getJobType()); Assert.assertNotNull(result.getCreatedOn()); Assert.assertNotNull(result.getStartTime()); Assert.assertEquals(1, result.getJobParams().size());
     * Assert.assertNull(result.getJobParams().get(0)); }
     */
}
