package com.ericsson.oss.services.shm.job.impl;

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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.activity.NeActivityInformation;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesQuery;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesResponse;
import com.ericsson.oss.services.shm.jobs.common.api.ManageBackupActivitiesQuery;
import com.ericsson.oss.services.shm.jobs.common.api.NeInfoQuery;
import com.ericsson.oss.services.shm.jobs.common.api.SHMJobActivitiesResponseModifier;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;

@RunWith(MockitoJUnitRunner.class)
public class JobActivitiesProviderImplTest {

    @InjectMocks
    private JobActivitiesProviderImpl objectUnderTest;

    @Mock
    private JobActivitiesResponse responseMock;

    @Mock
    private JobActivitiesQuery jobActivitiesQueryMock;

    @Mock
    private ManageBackupActivitiesQuery manageBackupActivitiesQuery;

    @Mock
    private NeInfoQuery neInfoQueryMock;

    @Mock
    private SHMJobActivitiesResponseModifier activitiesResponseModifier;

    @Mock
    private ManageBackupActivitiesResponseModifier manageBackupActivitiesResponseModifier;

    @Mock
    private JobActivityResponseModificationProviderFactory responseModificationProviderFactory;

    @Mock
    private PlatformTypeProviderImpl platformTypeProviderImplMock;

    @Mock
    private JobCapabilityProvider capabilityProviderMock;

    @Mock
    private SystemRecorder systemRecorder;

    private static final int NE_ACTIVITIES_DATAMAP_SIZE = 55;

    @Test
    public void testParseWithNonExistingActivityXML() {
        objectUnderTest.constructActivities();
        when(responseModificationProviderFactory.getActivitiesResponseModifier(PlatformTypeEnum.vRAN, JobType.DELETE_UPGRADEPACKAGE)).thenReturn(activitiesResponseModifier);
        when(jobActivitiesQueryMock.getNeTypes()).thenReturn(Arrays.asList(neInfoQueryMock));
        when(jobActivitiesQueryMock.getJobType()).thenReturn("DELETE_UPGRADEPACKAGE");
        when(neInfoQueryMock.getNeType()).thenReturn("DummyNeType");
        when(activitiesResponseModifier.getUpdatedJobActivities(eq(neInfoQueryMock), any(JobActivitiesResponse.class))).thenReturn(responseMock);
        when(platformTypeProviderImplMock.getPlatformTypeBasedOnCapability(anyString(), anyString())).thenReturn(PlatformTypeEnum.vRAN);

        final List<String> response = objectUnderTest.getActivityProperties("vRAN", "vRAN", "DELETE_UPGRADEPACKAGE");
        Assert.assertNotNull(response);
        Assert.assertEquals(0, response.size());

        final Map<String, NeActivityInformation> NE_ACTIVITIES_DATAMAP = (Map<String, NeActivityInformation>) Whitebox.getInternalState(objectUnderTest, "NE_ACTIVITIES_DATAMAP");

        Assert.assertEquals(NE_ACTIVITIES_DATAMAP_SIZE, NE_ACTIVITIES_DATAMAP.size());

        final String JOBACTIVITIES_XSD_RELATIVEPATH = (String) Whitebox.getInternalState(objectUnderTest, "JOBACTIVITIES_XSD_RELATIVEPATH");
        Assert.assertEquals("/activity_information.xsd", JOBACTIVITIES_XSD_RELATIVEPATH);

        final String XMLSFOLDER_RELATIVEPATH = (String) Whitebox.getInternalState(objectUnderTest, "XMLSFOLDER_RELATIVEPATH");
        Assert.assertEquals("/activityxmls/", XMLSFOLDER_RELATIVEPATH);
    }

    @Test
    public void testgetNeTypeActivitiesWithEmptyInput() {
        final List<JobActivitiesResponse> response = objectUnderTest.getNeTypeActivities(Collections.EMPTY_LIST);
        Assert.assertNotNull(response);
        Assert.assertEquals(0, response.size());
        verify(responseModificationProviderFactory, never()).getActivitiesResponseModifier(any(PlatformTypeEnum.class), any(JobType.class));
        verify(activitiesResponseModifier, never()).getUpdatedJobActivities(any(NeInfoQuery.class), any(JobActivitiesResponse.class));

    }

    @Test
    public void testgetNeTypeActivitiesNoDataReturned() {
        when(responseModificationProviderFactory.getActivitiesResponseModifier(PlatformTypeEnum.ECIM, JobType.BACKUP)).thenReturn(activitiesResponseModifier);
        when(jobActivitiesQueryMock.getNeTypes()).thenReturn(Arrays.asList(neInfoQueryMock));
        when(jobActivitiesQueryMock.getJobType()).thenReturn("BACKUP");
        when(activitiesResponseModifier.getUpdatedJobActivities(eq(neInfoQueryMock), any(JobActivitiesResponse.class))).thenReturn(responseMock);
        when(capabilityProviderMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(null);
        when(platformTypeProviderImplMock.getPlatformTypeBasedOnCapability(anyString(), anyString())).thenReturn(PlatformTypeEnum.ECIM);
        final List<JobActivitiesResponse> response = objectUnderTest.getNeTypeActivities(Arrays.asList(jobActivitiesQueryMock));
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.size());
        verify(activitiesResponseModifier, times(1)).getUpdatedJobActivities(eq(neInfoQueryMock), any(JobActivitiesResponse.class));

    }

    @Test
    public void testgetNeTypeActivitiesNoDataReturnedAndWhenCapabilityIsProvided() {
        when(responseModificationProviderFactory.getActivitiesResponseModifier(PlatformTypeEnum.ECIM, JobType.BACKUP)).thenReturn(activitiesResponseModifier);
        when(jobActivitiesQueryMock.getNeTypes()).thenReturn(Arrays.asList(neInfoQueryMock));
        when(jobActivitiesQueryMock.getJobType()).thenReturn("BACKUP");
        when(activitiesResponseModifier.getUpdatedJobActivities(eq(neInfoQueryMock), any(JobActivitiesResponse.class))).thenReturn(responseMock);
        when(capabilityProviderMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        when(platformTypeProviderImplMock.getPlatformTypeBasedOnCapability(anyString(), anyString())).thenReturn(PlatformTypeEnum.ECIM);
        final List<JobActivitiesResponse> response = objectUnderTest.getNeTypeActivities(Arrays.asList(jobActivitiesQueryMock));
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.size());
        verify(activitiesResponseModifier, times(1)).getUpdatedJobActivities(eq(neInfoQueryMock), any(JobActivitiesResponse.class));

    }

    @Test
    public void testgetManageBackupNeTypeActivitiesWithEmptyInput() {
        final List<JobActivitiesResponse> response = objectUnderTest.getManageBackupNeTypeActivities(Collections.EMPTY_LIST);
        Assert.assertNotNull(response);
        Assert.assertEquals(0, response.size());
        verify(responseModificationProviderFactory, never()).getBackupActivitiesResponseModifier(any(PlatformTypeEnum.class), any(JobType.class));
        verify(manageBackupActivitiesResponseModifier, never()).getManageBackupActivities(any(JobActivitiesResponse.class), Matchers.anyBoolean());

    }

    @Test
    public void testgetManageBackupNeTypeActivities() {
        when(responseModificationProviderFactory.getBackupActivitiesResponseModifier(PlatformTypeEnum.ECIM, JobType.BACKUP)).thenReturn(manageBackupActivitiesResponseModifier);
        when(manageBackupActivitiesResponseModifier.getManageBackupActivities(any(JobActivitiesResponse.class), Matchers.anyBoolean())).thenReturn(responseMock);
        when(platformTypeProviderImplMock.getPlatformTypeBasedOnCapability(anyString(), anyString())).thenReturn(PlatformTypeEnum.ECIM);
        final List<JobActivitiesResponse> response = objectUnderTest.getManageBackupNeTypeActivities(Arrays.asList(manageBackupActivitiesQuery));
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.size());
        verify(manageBackupActivitiesResponseModifier, times(1)).getManageBackupActivities(any(JobActivitiesResponse.class), Matchers.anyBoolean());

    }

    @Test
    public void testGetNeTypeActivitiesIncludingConstructHandlersAndInternalStates() {

        objectUnderTest.constructActivities();
        when(responseModificationProviderFactory.getActivitiesResponseModifier(PlatformTypeEnum.ECIM, JobType.BACKUP)).thenReturn(activitiesResponseModifier);
        when(jobActivitiesQueryMock.getNeTypes()).thenReturn(Arrays.asList(neInfoQueryMock));
        when(jobActivitiesQueryMock.getJobType()).thenReturn("BACKUP");
        when(neInfoQueryMock.getNeType()).thenReturn("DummyNeType");
        when(activitiesResponseModifier.getUpdatedJobActivities(eq(neInfoQueryMock), any(JobActivitiesResponse.class))).thenReturn(responseMock);
        when(capabilityProviderMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        when(platformTypeProviderImplMock.getPlatformTypeBasedOnCapability(anyString(), anyString())).thenReturn(PlatformTypeEnum.ECIM);
        final List<JobActivitiesResponse> response = objectUnderTest.getNeTypeActivities(Arrays.asList(jobActivitiesQueryMock));
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.size());
        verify(activitiesResponseModifier, times(1)).getUpdatedJobActivities(eq(neInfoQueryMock), any(JobActivitiesResponse.class));

        final Map<String, NeActivityInformation> NE_ACTIVITIES_DATAMAP = (Map<String, NeActivityInformation>) Whitebox.getInternalState(objectUnderTest, "NE_ACTIVITIES_DATAMAP");
        Assert.assertEquals(NE_ACTIVITIES_DATAMAP_SIZE, NE_ACTIVITIES_DATAMAP.size());

        final String JOBACTIVITIES_XSD_RELATIVEPATH = (String) Whitebox.getInternalState(objectUnderTest, "JOBACTIVITIES_XSD_RELATIVEPATH");
        Assert.assertEquals("/activity_information.xsd", JOBACTIVITIES_XSD_RELATIVEPATH);

        final String XMLSFOLDER_RELATIVEPATH = (String) Whitebox.getInternalState(objectUnderTest, "XMLSFOLDER_RELATIVEPATH");
        Assert.assertEquals("/activityxmls/", XMLSFOLDER_RELATIVEPATH);
    }

    @Test
    public void testGetActivityPropertiesForNodeRestart() {
        objectUnderTest.constructActivities();
        when(responseModificationProviderFactory.getActivitiesResponseModifier(PlatformTypeEnum.CPP, JobType.NODERESTART)).thenReturn(activitiesResponseModifier);
        when(jobActivitiesQueryMock.getNeTypes()).thenReturn(Arrays.asList(neInfoQueryMock));
        when(jobActivitiesQueryMock.getJobType()).thenReturn("NODERESTART");
        when(neInfoQueryMock.getNeType()).thenReturn("DummyNeType");
        when(activitiesResponseModifier.getUpdatedJobActivities(eq(neInfoQueryMock), any(JobActivitiesResponse.class))).thenReturn(responseMock);
        when(platformTypeProviderImplMock.getPlatformTypeBasedOnCapability(anyString(), anyString())).thenReturn(PlatformTypeEnum.CPP);

        final List<String> response = objectUnderTest.getActivityProperties("CPP", "ERBS", "NODERESTART");
        Assert.assertNotNull(response);
        Assert.assertEquals(3, response.size());

        final Map<String, NeActivityInformation> NE_ACTIVITIES_DATAMAP = (Map<String, NeActivityInformation>) Whitebox.getInternalState(objectUnderTest, "NE_ACTIVITIES_DATAMAP");

        Assert.assertEquals(NE_ACTIVITIES_DATAMAP_SIZE, NE_ACTIVITIES_DATAMAP.size());

        final String JOBACTIVITIES_XSD_RELATIVEPATH = (String) Whitebox.getInternalState(objectUnderTest, "JOBACTIVITIES_XSD_RELATIVEPATH");
        Assert.assertEquals("/activity_information.xsd", JOBACTIVITIES_XSD_RELATIVEPATH);

        final String XMLSFOLDER_RELATIVEPATH = (String) Whitebox.getInternalState(objectUnderTest, "XMLSFOLDER_RELATIVEPATH");
        Assert.assertEquals("/activityxmls/", XMLSFOLDER_RELATIVEPATH);
    }
}
