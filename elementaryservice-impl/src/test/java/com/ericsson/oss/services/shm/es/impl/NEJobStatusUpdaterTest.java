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
package com.ericsson.oss.services.shm.es.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import java.util.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobConsolidationService;
import com.ericsson.oss.services.shm.inventory.remote.axe.api.AxeInvSupervisionRemoteHandler;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ JobConsolidationService.class, ServiceFinderBean.class, NEJobStatusUpdater.class })
public class NEJobStatusUpdaterTest {

    @InjectMocks
    private NEJobStatusUpdater objectUnderTest;

    @Mock
    private DpsWriter dpsWriterMock;

    @Mock
    private JobConfigurationService jobConfigurationServiceMock;

    @Mock
    private SystemRecorder systemRecorderMock;

    @Mock
    private Map<String, Object> mapMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelperMock;

    @Mock
    private AxeInvSupervisionRemoteHandler axeInvSupervisionRemoteHandler;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private ServiceFinderBean serviceFinderBeanMock;

    @Mock
    private JobConsolidationService jobConsolidationService;

    @Mock
    private FaBuildingBlockResponseProcessor faBuildingBlockResponseProcessor;

    private final long jobId = 2l;
    private final long mainJobId = 1l;

    private Map<String, Object> getConsolidatedData() {
        final Map<String, Object> map = new HashMap<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        map.put(ShmConstants.JOBPROPERTIES, jobProperties);
        map.put(ShmConstants.NEJOB_HEALTH_STATUS, "HEALTHY");
        return map;
    }

    private void mockConsolidatation() {
        try {
            PowerMockito.whenNew(ServiceFinderBean.class).withNoArguments().thenReturn(serviceFinderBeanMock);
            PowerMockito.when(serviceFinderBeanMock.find(Matchers.any(), Matchers.any())).thenReturn(jobConsolidationService);
            Mockito.when(jobConsolidationService.consolidateNeJobData(Matchers.anyLong())).thenReturn(getConsolidatedData());
        } catch (final Exception e1) {
            e1.printStackTrace();
        }
    }

    @Test
    public void test_updateNEJobEndAttributes_withSuccessForNHCJob() throws JobDataNotFoundException {
        NetworkElement networkElement = new NetworkElement();
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        List<NetworkElement> networkElementMock = new ArrayList<>();
        networkElementMock.add(networkElement);
        mockConsolidatation();
        final JobStaticData jobStaticData = new JobStaticData("", new HashMap<String, Object>(), "", JobType.NODE_HEALTH_CHECK, "");
        when(jobConfigurationServiceMock.retrieveActivityJobResult(jobId)).thenReturn("SUCCESS");
        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_JOB_ID)).thenReturn(mainJobId);
        when(mapMock.get(ShmConstants.JOBPROPERTIES)).thenReturn(Arrays.asList(getConsolidatedData()));
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(mapMock.get(ShmConstants.ENDTIME)).thenReturn(null, new Date());
        when(mapMock.get(ShmConstants.NE_NAME)).thenReturn("ERBS01");
        when(mapMock.get(ShmConstants.LAST_LOG_MESSAGE)).thenReturn("\"Node Health Check\" activity is completed successfully.||1562572849136");
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList("ERBS01"), SHMCapabilities.NO_CAPABILITY)).thenReturn(networkElementMock);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticData);

        Map<String, Object> mainJobAttributes = new HashMap<>();
        List<Map<String, String>> jobProperties = new ArrayList<>();
        Map<String, String> property = new HashMap<>();
        property.put(ShmConstants.KEY, ShmConstants.JOB_CATEGORY);
        property.put(ShmConstants.VALUE, JobCategory.NHC_FA.getAttribute());

        Map<String, String> propertySubmitted = new HashMap<>();
        propertySubmitted.put(ShmConstants.KEY, ShmConstants.SUBMITTED_NES);
        propertySubmitted.put(ShmConstants.VALUE, "1");

        Map<String, String> propertyCompleted = new HashMap<>();
        propertyCompleted.put(ShmConstants.KEY, ShmConstants.NE_COMPLETED);
        propertyCompleted.put(ShmConstants.VALUE, "0");
        jobProperties.add(property);
        jobProperties.add(propertySubmitted);
        jobProperties.add(propertyCompleted);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        when(jobConfigurationServiceMock.retrieveJob(mainJobId)).thenReturn(mainJobAttributes);

        objectUnderTest.updateNEJobEndAttributes(jobId);
        verify(faBuildingBlockResponseProcessor, times(1)).sendNhcFaResponse(Matchers.anyLong(), Matchers.anyLong(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyMap());
    }

    @Test
    public void test_updateNEJobEndAttributes_withNoExistingPropertiesNHCJob() throws JobDataNotFoundException {
        NetworkElement networkElement = new NetworkElement();
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        List<NetworkElement> networkElementMock = new ArrayList<>();
        networkElementMock.add(networkElement);
        mockConsolidatation();
        final JobStaticData jobStaticData = new JobStaticData("", new HashMap<String, Object>(), "", JobType.NODE_HEALTH_CHECK, "");
        when(jobConfigurationServiceMock.retrieveActivityJobResult(jobId)).thenReturn("SUCCESS");
        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_JOB_ID)).thenReturn(mainJobId);
        when(mapMock.get(ShmConstants.JOBPROPERTIES)).thenReturn(null);
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(mapMock.get(ShmConstants.ENDTIME)).thenReturn(null, new Date());
        when(mapMock.get(ShmConstants.NE_NAME)).thenReturn("ERBS01");
        when(mapMock.get(ShmConstants.LAST_LOG_MESSAGE)).thenReturn("\"Node Health Check\" activity is completed successfully.||1562572849136");
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList("ERBS01"), SHMCapabilities.NO_CAPABILITY)).thenReturn(networkElementMock);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticData);

        Map<String, Object> mainJobAttributes = new HashMap<>();
        List<Map<String, String>> jobProperties = new ArrayList<>();
        Map<String, String> property = new HashMap<>();
        property.put(ShmConstants.KEY, ShmConstants.JOB_CATEGORY);
        property.put(ShmConstants.VALUE, JobCategory.NHC_FA.getAttribute());

        Map<String, String> propertySubmitted = new HashMap<>();
        propertySubmitted.put(ShmConstants.KEY, ShmConstants.SUBMITTED_NES);
        propertySubmitted.put(ShmConstants.VALUE, "1");

        Map<String, String> propertyCompleted = new HashMap<>();
        propertyCompleted.put(ShmConstants.KEY, ShmConstants.NE_COMPLETED);
        propertyCompleted.put(ShmConstants.VALUE, "0");
        jobProperties.add(property);
        jobProperties.add(propertySubmitted);
        jobProperties.add(propertyCompleted);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        when(jobConfigurationServiceMock.retrieveJob(mainJobId)).thenReturn(mainJobAttributes);

        objectUnderTest.updateNEJobEndAttributes(jobId);
        verify(faBuildingBlockResponseProcessor, times(1)).sendNhcFaResponse(Matchers.anyLong(), Matchers.anyLong(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyMap());
    }

    @Test
    public void test_updateNEJobEndAttributes_withSuccess() throws JobDataNotFoundException {
        NetworkElement networkElement = new NetworkElement();
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        List<NetworkElement> networkElementMock = new ArrayList<>();
        networkElementMock.add(networkElement);
        final JobStaticData jobStaticData = new JobStaticData("", new HashMap<String, Object>(), "", JobType.UPGRADE, "");
        when(jobConfigurationServiceMock.retrieveActivityJobResult(jobId)).thenReturn("SUCCESS");
        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_JOB_ID)).thenReturn(mainJobId);
        when(mapMock.get(ShmConstants.JOBPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(mapMock.get(ShmConstants.KEY)).thenReturn(ShmConstants.SUBMITTED_NES, ShmConstants.NE_COMPLETED);
        when(mapMock.get(ShmConstants.ENDTIME)).thenReturn(null, new Date());
        when(mapMock.get(ShmConstants.NE_NAME)).thenReturn("ERBS01");

        Map<String, Object> mainJobAttributes = new HashMap<>();
        List<Map<String, String>> jobProperties = new ArrayList<>();
        Map<String, String> property = new HashMap<>();
        property.put(ShmConstants.KEY, ShmConstants.JOB_CATEGORY);
        property.put(ShmConstants.VALUE, JobCategory.FA.getAttribute());

        Map<String, String> propertySubmitted = new HashMap<>();
        propertySubmitted.put(ShmConstants.KEY, ShmConstants.SUBMITTED_NES);
        propertySubmitted.put(ShmConstants.VALUE, "1");

        Map<String, String> propertyCompleted = new HashMap<>();
        propertyCompleted.put(ShmConstants.KEY, ShmConstants.NE_COMPLETED);
        propertyCompleted.put(ShmConstants.VALUE, "0");
        jobProperties.add(property);
        jobProperties.add(propertySubmitted);
        jobProperties.add(propertyCompleted);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        when(jobConfigurationServiceMock.retrieveJob(mainJobId)).thenReturn(mainJobAttributes);

        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList("ERBS01"), SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(networkElementMock);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList("ERBS01"), SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(networkElementMock);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticData);
        assertEquals(true, objectUnderTest.updateNEJobEndAttributes(jobId));
        verify(axeInvSupervisionRemoteHandler, never()).refreshNetworkInventoryForAxeNodes(networkElementMock.get(0).getNetworkElementFdn(), "SOFTWARE");
        verify(jobConsolidationService, never()).consolidateNeJobData(Matchers.anyLong());
    }

    @Test
    public void test_updateNEJobEndAttributes_withFailed() throws JobDataNotFoundException {
        final JobStaticData jobStaticData = new JobStaticData("", new HashMap<String, Object>(), "", null, "");
        when(jobConfigurationServiceMock.retrieveActivityJobResult(jobId)).thenReturn("FAILED");
        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_JOB_ID)).thenReturn(mainJobId);
        when(mapMock.get(ShmConstants.JOBPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.KEY)).thenReturn(ShmConstants.SUBMITTED_NES, ShmConstants.NE_COMPLETED);
        when(mapMock.get(ShmConstants.VALUE)).thenReturn("1", "1");
        when(mapMock.get(ShmConstants.ENDTIME)).thenReturn(null, new Date());
        when(jobStaticDataProvider.getJobStaticData(1L)).thenReturn(jobStaticData);

        assertEquals(false, objectUnderTest.updateNEJobEndAttributes(jobId));
    }

    @Test
    public void test_updateNEJobEndAttributes_withSuccessForAXENodesForUpgrade() throws JobDataNotFoundException {
        NetworkElement networkElement = new NetworkElement();
        networkElement.setPlatformType(PlatformTypeEnum.AXE);
        List<NetworkElement> networkElementMock = new ArrayList<>();
        networkElementMock.add(networkElement);
        final JobStaticData jobStaticData = new JobStaticData("", new HashMap<String, Object>(), "", JobType.UPGRADE, "");
        when(jobConfigurationServiceMock.retrieveActivityJobResult(jobId)).thenReturn("SUCCESS");
        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_JOB_ID)).thenReturn(mainJobId);
        when(mapMock.get(ShmConstants.JOBPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(mapMock.get(ShmConstants.KEY)).thenReturn(ShmConstants.SUBMITTED_NES, ShmConstants.NE_COMPLETED);
        when(mapMock.get(ShmConstants.VALUE)).thenReturn("0", "0", "1", "0");
        when(mapMock.get(ShmConstants.ENDTIME)).thenReturn(null, new Date());
        when(mapMock.get(ShmConstants.NE_NAME)).thenReturn("MSC07__CP2");
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList("MSC07"), SHMCapabilities.UPGRADE_JOB_CAPABILITY)).thenReturn(networkElementMock);
        when(jobStaticDataProvider.getJobStaticData(1L)).thenReturn(jobStaticData);
        objectUnderTest.updateNEJobEndAttributes(jobId);
        verify(axeInvSupervisionRemoteHandler, times(1)).refreshNetworkInventoryForAxeNodes(networkElementMock.get(0).getNetworkElementFdn(), "SOFTWARE");
        verify(axeInvSupervisionRemoteHandler, times(1)).refreshNetworkInventoryForAxeNodes(networkElementMock.get(0).getNetworkElementFdn(), "BACKUP");
        verify(jobConsolidationService, never()).consolidateNeJobData(Matchers.anyLong());
    }

    @Test
    public void test_updateNEJobEndAttributes_withSuccessForAXENodesForBackup() throws JobDataNotFoundException {
        NetworkElement networkElement = new NetworkElement();
        networkElement.setPlatformType(PlatformTypeEnum.AXE);
        List<NetworkElement> networkElementMock = new ArrayList<>();
        networkElementMock.add(networkElement);
        final JobStaticData jobStaticData = new JobStaticData("", new HashMap<String, Object>(), "", JobType.BACKUP, "");
        when(jobConfigurationServiceMock.retrieveActivityJobResult(jobId)).thenReturn("SUCCESS");
        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_JOB_ID)).thenReturn(mainJobId);
        when(mapMock.get(ShmConstants.JOBPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(mapMock.get(ShmConstants.KEY)).thenReturn(ShmConstants.SUBMITTED_NES, ShmConstants.NE_COMPLETED);
        when(mapMock.get(ShmConstants.VALUE)).thenReturn("0", "0", "1", "0");
        when(mapMock.get(ShmConstants.ENDTIME)).thenReturn(null, new Date());
        when(mapMock.get(ShmConstants.NE_NAME)).thenReturn("MSC07__CP2");
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList("MSC07"), SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(networkElementMock);
        when(jobStaticDataProvider.getJobStaticData(1L)).thenReturn(jobStaticData);
        objectUnderTest.updateNEJobEndAttributes(jobId);
        verify(axeInvSupervisionRemoteHandler, times(1)).refreshNetworkInventoryForAxeNodes(networkElementMock.get(0).getNetworkElementFdn(), "BACKUP");
        verify(jobConsolidationService, never()).consolidateNeJobData(Matchers.anyLong());
    }

    @Test
    public void test_updateNEJobEndAttributes_withSuccessForAXENodesForLicense() throws JobDataNotFoundException {
        NetworkElement networkElement = new NetworkElement();
        networkElement.setPlatformType(PlatformTypeEnum.AXE);
        List<NetworkElement> networkElementMock = new ArrayList<>();
        networkElementMock.add(networkElement);
        final JobStaticData jobStaticData = new JobStaticData("", new HashMap<String, Object>(), "", JobType.LICENSE, "");
        when(jobConfigurationServiceMock.retrieveActivityJobResult(jobId)).thenReturn("SUCCESS");
        when(jobConfigurationServiceMock.retrieveJob(jobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.MAIN_JOB_ID)).thenReturn(mainJobId);
        when(mapMock.get(ShmConstants.JOBPROPERTIES)).thenReturn(Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.STARTTIME)).thenReturn(new Date());
        when(mapMock.get(ShmConstants.KEY)).thenReturn(ShmConstants.SUBMITTED_NES, ShmConstants.NE_COMPLETED);
        when(mapMock.get(ShmConstants.VALUE)).thenReturn("0", "0", "1", "0");
        when(mapMock.get(ShmConstants.ENDTIME)).thenReturn(null, new Date());
        when(mapMock.get(ShmConstants.NE_NAME)).thenReturn("MSC07__CP2");
        when(jobStaticDataProvider.getJobStaticData(1L)).thenReturn(jobStaticData);
        objectUnderTest.updateNEJobEndAttributes(jobId);
        verify(axeInvSupervisionRemoteHandler, never()).refreshNetworkInventoryForAxeNodes(networkElementMock.get(0).getNetworkElementFdn(), "LICENSE");
        verify(jobConsolidationService, never()).consolidateNeJobData(Matchers.anyLong());
    }
}
