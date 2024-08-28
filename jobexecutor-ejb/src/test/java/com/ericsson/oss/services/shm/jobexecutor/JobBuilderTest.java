/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobexecutor;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Test class for JobBuilder.
 *
 * @author zkhtblr
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class JobBuilderTest {

    @InjectMocks
    private JobBuilder jobBuilder;

    @Mock
    private JobExecutorServiceHelper executorServiceHelper;

    @Mock
    private JobConfigurationService jobConfigurationService;

    private static final String NODE_TYPE="ERBS";
    private static final String BUSINESS_KEY="businessKey";
    private static final String STATE="CREATED";

    @Test
    public void testCreateNeJob() {
        final long mainJobId = 1L;
        final long neJobId = 3L;
        final NetworkElement cppNetworkElement = new NetworkElement();
        cppNetworkElement.setNeType(NODE_TYPE);
        cppNetworkElement.setName("selectedCppNE");
        final List<NetworkElement> neList = new ArrayList<>();
        neList.add(cppNetworkElement);
        Map<String, String> neDetailsWithParentName = new HashMap<>();
        final Map<String, Object> supportedAndUnsupported = new HashMap<>();
        final Map<PlatformTypeEnum, List<NetworkElement>> supportedNesGroupedByPlatform = new HashMap<>();
        supportedNesGroupedByPlatform.put(PlatformTypeEnum.CPP,neList);
        final Map<PlatformTypeEnum, List<NetworkElement>> unSupportedNesGroupedByPlatform = new HashMap<>();
        when(executorServiceHelper.groupNetworkElementsByPlatform(Matchers.anyList())).thenReturn(supportedNesGroupedByPlatform);
        when(executorServiceHelper.groupNetworkElementsByPlatform(Matchers.anyList())).thenReturn(unSupportedNesGroupedByPlatform);
        Map<String, Object> neMap = new HashMap<>();
        neMap.put(ShmConstants.NE_NAME,"selectedCppNE2");
        neMap.put(ShmConstants.STATE, STATE);
        final List<Map<String, Object>> projectedAttributes = new ArrayList<>();
        projectedAttributes.add(neMap);
        final Map<String, Object> poMap = new HashMap<>();
        poMap.put(ShmConstants.PO_ID,neJobId);
        when(executorServiceHelper.createPO(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(poMap);
        when(jobConfigurationService.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(),  Matchers.anyMap(),  Matchers.anyList())).thenReturn(projectedAttributes);
        final Map<String, Object> responseMap=jobBuilder.createNeJob(mainJobId, BUSINESS_KEY,cppNetworkElement,neDetailsWithParentName,supportedAndUnsupported);
        assertEquals(neJobId,responseMap.get(ShmConstants.NE_JOB_ID));
    }

    @Test
    public void testCreateNeJob_Cancel() {
        final long mainJobId = 1L;
        final NetworkElement cppNetworkElement = new NetworkElement();
        cppNetworkElement.setNeType(NODE_TYPE);
        cppNetworkElement.setName("selectedCppNE");
        final List<NetworkElement> neList = new ArrayList<>();
        neList.add(cppNetworkElement);
        Map<String, String> neDetailsWithParentName = new HashMap<>();
        final Map<String, Object> supportedAndUnsupported = new HashMap<>();
        final Map<PlatformTypeEnum, List<NetworkElement>> supportedNesGroupedByPlatform = new HashMap<>();
        supportedNesGroupedByPlatform.put(PlatformTypeEnum.CPP,neList);
        final Map<PlatformTypeEnum, List<NetworkElement>> unSupportedNesGroupedByPlatform = new HashMap<>();
        when(executorServiceHelper.groupNetworkElementsByPlatform(Matchers.anyList())).thenReturn(supportedNesGroupedByPlatform);
        when(executorServiceHelper.groupNetworkElementsByPlatform(Matchers.anyList())).thenReturn(unSupportedNesGroupedByPlatform);
        Map<String, Object> neMap = new HashMap<>();
        neMap.put(ShmConstants.NE_NAME,"selectedCppNE2");
        neMap.put(ShmConstants.STATE, ShmConstants.CANCELLING);
        final List<Map<String, Object>> projectedAttributes = new ArrayList<>();
        projectedAttributes.add(neMap);
        final Map<String, Object> poMap = new HashMap<>();
        when(executorServiceHelper.createPO(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(poMap);
        when(jobConfigurationService.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(),  Matchers.anyMap(),  Matchers.anyList())).thenReturn(projectedAttributes);
        final Map<String, Object> responseMap=jobBuilder.createNeJob(mainJobId, BUSINESS_KEY,cppNetworkElement,neDetailsWithParentName,supportedAndUnsupported);
        assertEquals(ShmConstants.CANCELLED,responseMap.get(ShmConstants.JOB_STATUS));
    }

    @Test
    public void testCreateNeJob_Failed() {
        final long mainJobId = 1L;
        final NetworkElement cppNetworkElement = new NetworkElement();
        cppNetworkElement.setNeType(NODE_TYPE);
        cppNetworkElement.setName("selectedCppNE");
        final List<NetworkElement> neList = new ArrayList<>();
        neList.add(cppNetworkElement);
        Map<String, String> neDetailsWithParentName = new HashMap<>();
        final Map<String, Object> supportedAndUnsupported=new HashMap<>();
        final Map<PlatformTypeEnum, List<NetworkElement>> supportedNesGroupedByPlatform = new HashMap<>();
        supportedNesGroupedByPlatform.put(PlatformTypeEnum.CPP,neList);
        final Map<PlatformTypeEnum, List<NetworkElement>> unSupportedNesGroupedByPlatform = new HashMap<>();
        when(executorServiceHelper.groupNetworkElementsByPlatform(Matchers.anyList())).thenReturn(supportedNesGroupedByPlatform);
        when(executorServiceHelper.groupNetworkElementsByPlatform(Matchers.anyList())).thenReturn(unSupportedNesGroupedByPlatform);
        Map<String, Object> neMap=new HashMap<>();
        neMap.put(ShmConstants.NE_NAME,"selectedCppNE2");
        neMap.put(ShmConstants.STATE, STATE);
        final List<Map<String, Object>> projectedAttributes = new ArrayList<>();
        projectedAttributes.add(neMap);
        final Map<String, Object> poMap = new HashMap<>();
        when(executorServiceHelper.createPO(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(poMap);
        when(jobConfigurationService.getProjectedAttributes(Matchers.anyString(), Matchers.anyString(),  Matchers.anyMap(),  Matchers.anyList())).thenReturn(projectedAttributes);
        final Map<String, Object> responseMap = jobBuilder.createNeJob(mainJobId, BUSINESS_KEY,cppNetworkElement,neDetailsWithParentName,supportedAndUnsupported);
        assertEquals(ShmConstants.CREATION_FAILED,responseMap.get(ShmConstants.JOB_STATUS));
    }

}
