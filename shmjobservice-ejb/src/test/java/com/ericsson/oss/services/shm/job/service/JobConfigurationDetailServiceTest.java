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

import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.DateTimeUtils;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.JobSchedulerConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobParamMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Activity;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobProperty;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NEInfo;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ScheduleProperty;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;
import com.ericsson.oss.services.shm.jobs.common.restentities.RestJobConfigurationData;

@RunWith(PowerMockRunner.class)
public class JobConfigurationDetailServiceTest {

    @Mock
    private NetworkElement networkElementMock;

    @Mock
    private JobConfiguration jobConfiguration;

    @Mock
    private JobTemplate jobTemplateMock;

    @Mock
    private Date dateMock;

    @Mock
    private Schedule scheduleMock;

    @Mock
    private FdnServiceBean fdnServiceBeanMock;

    @Mock
    private PersistenceObject poMock;

    @Mock
    private DpsReader dpsReader;

    @Mock
    NEInfo neInfoMock;

    @Mock
    private SHMJobService shmJobServiceMock;

    @InjectMocks
    private JobConfigurationDetailServiceImpl jobConfigurationDetailService;

    @Mock
    private List<PersistenceObject> persistenceObjectsMock;

    @Mock
    private ActivityParamMapper activityParamMapper;

    @Mock
    private JobParamMapper jobParamMapper;

    @Mock
    private Activity activityMock;

    @Mock
    private JobTypeDetailsProvider jobTypeDetailsProvider;

    @Mock
    private JobProperty jobPropertyMock;

    @Mock
    private JobConfigurationDetails neJobConfigurationDetails;

    @Mock
    private PlatformTypeProviderImpl platformTypeProviderImpl;

    @Mock
    private JobTypeDetailsProviderFactory jobTypeDetailsProviderFactory;

    @Mock
    private ScheduleProperty schedulePropertyMock;

    @Mock
    private DateTimeUtils dateTimeUtils;

    @Mock
    SimpleDateFormat simpleFormatMock;

    @Mock
    private JobConfigurationDetails jobConfigurationDetailsMock;

    @Mock
    private JobConfigurationSummary jobConfigurationSummaryProvider;

    @Mock
    private JobConfigurationSummaryFactory jobConfigurationSummaryFactory;

    @Mock
    private DefaultJobConfigurationDetailsProvider defaultJobConfigurationDetailsProviderMock;

    @Mock
    private JobCapabilityProvider jobCapabilityProviderMock;

    final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
    long jobConfigId = 12345678;

    @Before
    public void setMockData() {
        networkElementList.add(networkElementMock);

        when(jobConfiguration.getSelectedNEs()).thenReturn(neInfoMock);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(networkElementList);
    }

    @Test
    public void test_mapJobConfigToRestDataFormat_withLimitedData() {

        final List<JobConfigurationDetails> jobConfig = new ArrayList<JobConfigurationDetails>();
        jobConfig.add(jobConfigurationDetailsMock);

        when(jobTemplateMock.getCreationTime()).thenReturn(dateMock);
        when(dateMock.toString()).thenReturn("dummyDate");
        when(dateTimeUtils.getDatewithDefaultTimeZone(dateMock)).thenReturn("dummyDate");
        when(jobTemplateMock.getJobType()).thenReturn(JobType.BACKUP);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobTemplateMock.getJobTemplateId()).thenReturn(jobConfigId);
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(dpsReader.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(persistenceObjectsMock);
        when(shmJobServiceMock.getJobStartTime(jobConfigId)).thenReturn("2014-09-08 16:22:55 GMT+0530");
        when(neInfoMock.getNeNames()).thenReturn(Arrays.asList("LTE01", "LTE02"));
        when(jobCapabilityProviderMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        when(jobConfigurationSummaryFactory.getJobConfigurationSummaryProvider(JobType.BACKUP.name())).thenReturn(defaultJobConfigurationDetailsProviderMock);
        when(defaultJobConfigurationDetailsProviderMock.getNetworkElementsByNeNames(Matchers.anyList(), Matchers.anyString())).thenReturn(networkElementList);

        final RestJobConfigurationData result = jobConfigurationDetailService.getJobConfigurationDetails(jobTemplateMock);
        Assert.assertNotNull(result);
        Assert.assertEquals(JobType.BACKUP.name(), result.getJobType());
        Assert.assertNotNull(result.getCreatedOn());
        Assert.assertNotNull(result.getStartTime());
        Assert.assertEquals(0, result.getJobParams().size());
        Assert.assertEquals(networkElementList, result.getSelectedNEs().getNetworkElements());
    }

    @Test
    public void test_mapJobConfigToRestDataFormat_toCheckActivities_jobParams() throws Exception {

        final List<JobConfigurationDetails> jobConfig = new ArrayList<JobConfigurationDetails>();
        jobConfig.add(neJobConfigurationDetails);

        final Map<String, List<JobProperty>> neTypeJobProperties = new HashMap<String, List<JobProperty>>();
        neTypeJobProperties.put("ERBS", Arrays.asList(jobPropertyMock));
        when(jobTemplateMock.getCreationTime()).thenReturn(dateMock);
        when(dateMock.toString()).thenReturn("dummyDate");
        when(dateTimeUtils.getDatewithDefaultTimeZone(dateMock)).thenReturn("dummyDate");
        when(jobTemplateMock.getJobType()).thenReturn(JobType.BACKUP);
        when(jobTemplateMock.getJobTemplateId()).thenReturn(jobConfigId);

        when(jobConfiguration.getJobProperties()).thenReturn(Arrays.asList(jobPropertyMock));
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);

        when(platformTypeProviderImpl.getPlatformTypeBasedOnCapability("ERBS", SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(PlatformTypeEnum.CPP);
        final com.ericsson.oss.services.shm.job.activity.JobType jobTypeValue = com.ericsson.oss.services.shm.job.activity.JobType.fromValue(JobType.BACKUP.getJobTypeName());
        when(jobTypeDetailsProviderFactory.getJobTypeDetailsProvider(PlatformTypeEnum.CPP, jobTypeValue)).thenReturn(jobTypeDetailsProvider);
        when(activityMock.getNeType()).thenReturn("ERBS");
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(activityMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
        when(activityMock.getSchedule()).thenReturn(scheduleMock);
        when(activityMock.getName()).thenReturn(JobPropertyConstants.SET_CV_AS_STARTABLE);
        when(activityMock.getOrder()).thenReturn(0);
        Set<String> neTypes = new HashSet<String>();
        neTypes.add("ERBS");
        when(activityParamMapper.getNeTypes(Arrays.asList(activityMock))).thenReturn(neTypes);
        when(jobConfiguration.getActivities()).thenReturn(Arrays.asList(activityMock));
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.IMMEDIATE);
        when(dpsReader.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(persistenceObjectsMock);
        when(shmJobServiceMock.getJobStartTime(jobConfigId)).thenReturn("2014-09-08 16:22:55 GMT+0530");
        when(jobCapabilityProviderMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        when(jobConfigurationSummaryFactory.getJobConfigurationSummaryProvider(JobType.BACKUP.name())).thenReturn(defaultJobConfigurationDetailsProviderMock);
        when(defaultJobConfigurationDetailsProviderMock.getJobConfigurationDetails(jobTemplateMock, jobConfiguration)).thenReturn(jobConfig);

        when(jobTypeDetailsProvider.getJobConfigParamDetails(jobConfiguration, "ERBS")).thenReturn(neJobConfigurationDetails);
        when(jobTypeDetailsProvider.getJobConfigParamDetails(jobConfiguration, "AXE")).thenReturn(neJobConfigurationDetails);
        final RestJobConfigurationData result = jobConfigurationDetailService.getJobConfigurationDetails(jobTemplateMock);
        Assert.assertNotNull(result);
        Assert.assertEquals(JobType.BACKUP.name(), result.getJobType());
        Assert.assertNotNull(result.getCreatedOn());
        Assert.assertNotNull(result.getStartTime());
        Assert.assertEquals(1, result.getJobParams().size());
        Assert.assertEquals(neJobConfigurationDetails, result.getJobParams().get(0));
    }

    @Test
    public void test_mapJobConfigToRestDataFormat_tocheckMainSchedule() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        when(jobTemplateMock.getCreationTime()).thenReturn(dateMock);
        when(dateMock.toString()).thenReturn("dummyDate");
        when(dateTimeUtils.getDatewithDefaultTimeZone(dateMock)).thenReturn("dummyDate");
        when(jobTemplateMock.getJobType()).thenReturn(JobType.BACKUP);
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobTemplateMock.getJobTemplateId()).thenReturn(jobConfigId);
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.SCHEDULED);
        when(scheduleMock.getScheduleAttributes()).thenReturn(Arrays.asList(schedulePropertyMock));
        when(schedulePropertyMock.getName()).thenReturn(JobSchedulerConstants.START_DATE);
        when(schedulePropertyMock.getValue()).thenReturn("2014-09-08 16:22:55 GMT+0530");
        when(simpleFormatMock.parse("2014-09-08 16:22:55")).thenReturn(dateMock);
        when(dpsReader.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(persistenceObjectsMock);
        when(jobCapabilityProviderMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        when(shmJobServiceMock.getJobStartTime(jobConfigId)).thenReturn("2014-09-08 16:22:55 GMT+0530");
        when(jobConfigurationSummaryFactory.getJobConfigurationSummaryProvider(JobType.BACKUP.name())).thenReturn(defaultJobConfigurationDetailsProviderMock);

        final RestJobConfigurationData result = jobConfigurationDetailService.getJobConfigurationDetails(jobTemplateMock);
        Assert.assertNotNull(result);
        Assert.assertEquals(JobType.BACKUP.name(), result.getJobType());
        Assert.assertNotNull(result.getCreatedOn());
        Assert.assertNotNull(result.getStartTime());
        Assert.assertEquals(0, result.getJobParams().size());
    }

    @Test
    public void test_mapJobConfigToRestDataFormat_tocheckScheduledAttributes() throws ParseException {

        final List<ScheduleProperty> schedulePropertyList = new ArrayList<ScheduleProperty>();
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
        when(dateMock.toString()).thenReturn("dummyDate");
        when(dateTimeUtils.getDatewithDefaultTimeZone(dateMock)).thenReturn("dummyDate");
        when(jobTemplateMock.getJobType()).thenReturn(JobType.BACKUP);
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(jobTemplateMock.getJobTemplateId()).thenReturn(jobConfigId);
        when(jobTemplateMock.getJobConfigurationDetails()).thenReturn(jobConfiguration);
        when(jobConfiguration.getMainSchedule()).thenReturn(scheduleMock);
        when(scheduleMock.getExecMode()).thenReturn(ExecMode.SCHEDULED);
        when(scheduleMock.getScheduleAttributes()).thenReturn(schedulePropertyList);
        when(simpleFormatMock.parse("2014-09-08 16:22:55")).thenReturn(dateMock);
        when(dpsReader.findPOs(Matchers.anyString(), Matchers.anyString(), Matchers.anyMapOf(String.class, Object.class))).thenReturn(persistenceObjectsMock);
        when(jobCapabilityProviderMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        when(shmJobServiceMock.getJobStartTime(jobConfigId)).thenReturn("2014-09-08 16:22:55 GMT+0530");
        when(jobConfigurationSummaryFactory.getJobConfigurationSummaryProvider(JobType.BACKUP.name())).thenReturn(defaultJobConfigurationDetailsProviderMock);

        final RestJobConfigurationData result = jobConfigurationDetailService.getJobConfigurationDetails(jobTemplateMock);
        Assert.assertNotNull(result);
        Assert.assertEquals(JobType.BACKUP.name(), result.getJobType());
        Assert.assertNotNull(result.getCreatedOn());
        Assert.assertNotNull(result.getStartTime());
        Assert.assertEquals(0, result.getJobParams().size());
        Assert.assertNotNull(result.getScheduleJobConfiguration());
        Assert.assertEquals("7", result.getScheduleJobConfiguration().getOccurences());
        Assert.assertEquals("Daily", result.getScheduleJobConfiguration().getRepeatType());
    }
}