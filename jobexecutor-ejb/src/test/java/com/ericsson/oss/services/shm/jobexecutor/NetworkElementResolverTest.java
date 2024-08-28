/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobexecutor;

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
import org.mockito.runners.MockitoJUnitRunner;
import org.testng.Assert;

import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.DatabaseNotAvailableException;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

/**
 * Test class to test NetworkElementResolver functionality
 * 
 * @author xeswpot
 * 
 */
@RunWith(value = MockitoJUnitRunner.class)
public class NetworkElementResolverTest {

    @InjectMocks
    private NetworkElementResolver networkElementResolver;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelperMock;

    @Mock
    private JobUpdateService jobUpdateServiceMock;

    @Mock
    private WorkflowInstanceNotifier workflowInstanceNotifierMock;

    @Mock
    private JobCapabilityProvider capabilityMapperMock;

    @Test
    public void testGetNetworkElementResponseForSUpportedNode() {

        final long mainJobId = 123456L;
        final long templateJobId = 456787L;
        final List<String> neNames = new ArrayList<String>();
        neNames.add("node1");

        final Map<String, Object> attributeMap = new HashMap<String, Object>();

        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        final NetworkElement ne = new NetworkElement();
        ne.setName("node1");
        ne.setNetworkElementFdn("NetworkElement=node1");
        ne.setNeType("ERBS");
        ne.setNodeModelIdentity("nodeModelIdentity");
        ne.setOssModelIdentity("ossModelIdentity");
        ne.setNodeRootFdn("MeContext=node1");
        ne.setPlatformType(PlatformTypeEnum.CPP);
        networkElementList.add(ne);

        Mockito.when(capabilityMapperMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        Mockito.when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neNames, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(networkElementList);
        final NetworkElementResponse response = networkElementResolver.getNetworkElementResponse(mainJobId, neNames, templateJobId, attributeMap, JobTypeEnum.BACKUP, true);

        Assert.assertEquals(response.getSupportedNes().size(), 1);
    }

    @Test
    public void testGetNetworkElementResponseForSupportedNodeWhenJobTypeIsNotProvided() {

        final long mainJobId = 123456L;
        final long templateJobId = 456787L;
        final List<String> neNames = new ArrayList<String>();
        neNames.add("node1");

        final Map<String, Object> attributeMap = new HashMap<String, Object>();

        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        final NetworkElement ne = new NetworkElement();
        ne.setName("node1");
        ne.setNetworkElementFdn("NetworkElement=node1");
        ne.setNeType("ERBS");
        ne.setNodeModelIdentity("nodeModelIdentity");
        ne.setOssModelIdentity("ossModelIdentity");
        ne.setNodeRootFdn("MeContext=node1");
        ne.setPlatformType(PlatformTypeEnum.CPP);
        networkElementList.add(ne);

        Mockito.when(capabilityMapperMock.getCapability(null)).thenReturn(null);
        Mockito.when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neNames, null)).thenReturn(networkElementList);
        final NetworkElementResponse response = networkElementResolver.getNetworkElementResponse(mainJobId, neNames, templateJobId, attributeMap, null, true);

        Assert.assertEquals(response.getSupportedNes().size(), 1);
    }

    @Test
    public void testGetNetworkElementResponseWhenUnsupportedNodesArePresent() {

        final long mainJobId = 123456L;
        final long templateJobId = 456787L;
        final List<String> neNames = new ArrayList<String>();
        neNames.add("node1");
        neNames.add("node2");
        final Map<String, Object> attributeMap = new HashMap<String, Object>();

        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        final NetworkElement ne = new NetworkElement();
        ne.setName("node1");
        ne.setNetworkElementFdn("NetworkElement=node1");
        ne.setNeType("ERBS");
        ne.setNodeModelIdentity("nodeModelIdentity");
        ne.setOssModelIdentity("ossModelIdentity");
        ne.setNodeRootFdn("MeContext=node1");
        ne.setPlatformType(PlatformTypeEnum.CPP);
        networkElementList.add(ne);

        Mockito.when(capabilityMapperMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        Mockito.when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neNames, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(networkElementList);
        Mockito.doNothing().when(jobUpdateServiceMock).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        final NetworkElementResponse response = networkElementResolver.getNetworkElementResponse(mainJobId, neNames, templateJobId, attributeMap, JobTypeEnum.BACKUP, true);

        Assert.assertEquals(response.getSupportedNes().size(), 1);
        Assert.assertEquals(response.getInvalidNes().size(), 1);
    }

    @Test
    public void testGetNetworkElementResponseWhenUnsupportedNodesArePresentAndJobTypeNotProvided() {

        final long mainJobId = 123456L;
        final long templateJobId = 456787L;
        final List<String> neNames = new ArrayList<String>();
        neNames.add("node1");
        neNames.add("node2");
        final Map<String, Object> attributeMap = new HashMap<String, Object>();

        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        final NetworkElement ne = new NetworkElement();
        ne.setName("node1");
        ne.setNetworkElementFdn("NetworkElement=node1");
        ne.setNeType("ERBS");
        ne.setNodeModelIdentity("nodeModelIdentity");
        ne.setOssModelIdentity("ossModelIdentity");
        ne.setNodeRootFdn("MeContext=node1");
        ne.setPlatformType(PlatformTypeEnum.CPP);
        networkElementList.add(ne);

        Mockito.when(capabilityMapperMock.getCapability(null)).thenReturn(null);
        Mockito.when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neNames, null)).thenReturn(networkElementList);
        Mockito.doNothing().when(jobUpdateServiceMock).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        final NetworkElementResponse response = networkElementResolver.getNetworkElementResponse(mainJobId, neNames, templateJobId, attributeMap, null, true);

        Assert.assertEquals(response.getSupportedNes().size(), 1);
        Assert.assertEquals(response.getInvalidNes().size(), 1);
    }

    @Test
    public void testGetNetworkElementResponseWhenInternalServerIssueOccurs() {

        final long mainJobId = 123456L;
        final long templateJobId = 456787L;
        final List<String> neNames = new ArrayList<String>();
        neNames.add("node1");
        final Map<String, Object> attributeMap = new HashMap<String, Object>();

        Mockito.when(capabilityMapperMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        Mockito.when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neNames, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenThrow(new ServerInternalException("Internal Server Error"));
        Mockito.doNothing().when(jobUpdateServiceMock).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        Mockito.when(workflowInstanceNotifierMock.sendAllNeDone(Matchers.anyString())).thenReturn(true);
        final NetworkElementResponse response = networkElementResolver.getNetworkElementResponse(mainJobId, neNames, templateJobId, attributeMap, JobTypeEnum.BACKUP, true);

        Mockito.verify(jobUpdateServiceMock, Mockito.times(1)).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        Assert.assertNull(response);
    }

    @Test
    public void testGetNetworkElementResponseWhenInternalServerIssueOccursAndJobTypeNotProvided() {

        final long mainJobId = 123456L;
        final long templateJobId = 456787L;
        final List<String> neNames = new ArrayList<String>();
        neNames.add("node1");
        final Map<String, Object> attributeMap = new HashMap<String, Object>();

        Mockito.when(capabilityMapperMock.getCapability(null)).thenReturn(null);
        Mockito.when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neNames, null)).thenThrow(new ServerInternalException("Internal Server Error"));
        Mockito.doNothing().when(jobUpdateServiceMock).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        Mockito.when(workflowInstanceNotifierMock.sendAllNeDone(Matchers.anyString())).thenReturn(true);
        final NetworkElementResponse response = networkElementResolver.getNetworkElementResponse(mainJobId, neNames, templateJobId, attributeMap, null, true);

        Mockito.verify(jobUpdateServiceMock, Mockito.times(1)).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        Assert.assertNull(response);
    }

    @Test
    public void testGetNetworkElementResponseWhenDatabaseIsDown() {

        final long mainJobId = 123456L;
        final long templateJobId = 456787L;
        final List<String> neNames = new ArrayList<String>();
        neNames.add("node1");
        final Map<String, Object> attributeMap = new HashMap<String, Object>();

        Mockito.when(capabilityMapperMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        Mockito.when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neNames, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenThrow(
                new DatabaseNotAvailableException("DB not available at this moment"));
        Mockito.doNothing().when(jobUpdateServiceMock).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        Mockito.when(workflowInstanceNotifierMock.sendAllNeDone(Matchers.anyString())).thenReturn(true);
        final NetworkElementResponse response = networkElementResolver.getNetworkElementResponse(mainJobId, neNames, templateJobId, attributeMap, JobTypeEnum.BACKUP, true);

        Mockito.verify(jobUpdateServiceMock, Mockito.times(1)).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        Assert.assertNull(response);
    }

    @Test
    public void testGetNetworkElementResponseWhenDatabaseIsDownAndWhenJobTypeNotProvided() {

        final long mainJobId = 123456L;
        final long templateJobId = 456787L;
        final List<String> neNames = new ArrayList<String>();
        neNames.add("node1");
        final Map<String, Object> attributeMap = new HashMap<String, Object>();

        Mockito.when(capabilityMapperMock.getCapability(null)).thenReturn(null);
        Mockito.when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neNames, null)).thenThrow(new DatabaseNotAvailableException("DB not available at this moment"));
        Mockito.doNothing().when(jobUpdateServiceMock).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        Mockito.when(workflowInstanceNotifierMock.sendAllNeDone(Matchers.anyString())).thenReturn(true);
        final NetworkElementResponse response = networkElementResolver.getNetworkElementResponse(mainJobId, neNames, templateJobId, attributeMap, null, true);

        Mockito.verify(jobUpdateServiceMock, Mockito.times(1)).updateJobAttributes(Matchers.anyLong(), Matchers.anyMap());
        Assert.assertNull(response);
    }
}
