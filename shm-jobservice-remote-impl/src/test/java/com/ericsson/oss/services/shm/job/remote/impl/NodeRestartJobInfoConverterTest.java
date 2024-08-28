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
package com.ericsson.oss.services.shm.job.remote.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.UserContextBean;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.remote.api.ShmNodeRestartJobData;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.job.service.common.NodeRestartActivitySchedulesUtil;
import com.ericsson.oss.services.shm.job.service.common.NodeRestartJobInfoConverter;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.topologyservice.TopologyEvaluationService;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException;

@RunWith(MockitoJUnitRunner.class)
public class NodeRestartJobInfoConverterTest {

    @Mock
    FdnServiceBean fdnServiceBean;

    @Mock
    ShmRemoteJobData shmRemoteJobData;

    @Mock
    UserContextBean userContextBean;

    @Mock
    SupportedPlatformAndNeTypeFinder platformAndNeTypeFinder;

    @Mock
    NodeRestartActivitySchedulesUtil activitySchedulesUtil;

    @Mock
    NeTypePropertiesUtil neTypePropertiesUtil;

    @InjectMocks
    NodeRestartJobInfoConverter nodeRestartJobInfoConverter;

    @Mock
    Logger logger;

    @Mock
    JobInfoConverterImpl jobInfoConverterImpl;

    @Mock
    JobInfo jobInfo;

    @Mock
    TopologyEvaluationService topologyEvaluation;

    @Mock
    private JobCapabilityProvider jobCapabilityProviderMock;

    Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes;

    private static final String JOB_NAME = "CliJobName001";

    private static final Long poid = 899797L;

    @Before
    public void setup() {
        supportedPlatformTypeAndNodeTypes = new EnumMap<PlatformTypeEnum, List<String>>(PlatformTypeEnum.class);
        List<String> neTypes = new ArrayList<String>();
        neTypes.add("ERBS");
        supportedPlatformTypeAndNodeTypes.put(PlatformTypeEnum.CPP, neTypes);
    }

    @Test
    public void testPrepareJobInfoData() throws TopologyCollectionsServiceException, NoMeFDNSProvidedException {
        Set<String> neNamesSet = new HashSet<String>();
        List<String> neNames = new ArrayList<String>(neNamesSet);
        NetworkElement networkElement = new NetworkElement();
        ShmNodeRestartJobData shmNodeRestartJobData = new ShmNodeRestartJobData();
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        neNamesSet.add("LTE01ERBS01");
        shmNodeRestartJobData.setNeNames(neNamesSet);
        List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        networkElements.add(networkElement);
        shmNodeRestartJobData.setActivity(ShmConstants.NODERESTART_ACTIVITY);
        shmNodeRestartJobData.setLoggedInUser("administrator");
        shmNodeRestartJobData.setJobType(JobType.NODERESTART);
        when(fdnServiceBean.getNetworkElementsByNeNames(neNames, SHMCapabilities.NODE_RESTART_JOB_CAPABILITY)).thenReturn(networkElements);
        when(platformAndNeTypeFinder.findSupportedPlatformAndNodeTypes(SHMCapabilities.NODE_RESTART_JOB_CAPABILITY, shmNodeRestartJobData)).thenReturn(supportedPlatformTypeAndNodeTypes);
        final JobInfo jobInfo = nodeRestartJobInfoConverter.prepareJobInfoData(shmNodeRestartJobData);
        assertNotNull(jobInfo);
        assertTrue(jobInfo.getJobType().equals(JobTypeEnum.NODERESTART));
        assertTrue("administrator".equals(jobInfo.getOwner()));
    }

    @Test
    public void testrepareJobInfoDataWithCollectionAndFdns() throws TopologyCollectionsServiceException, NoMeFDNSProvidedException {
        Set<String> neNamesSet = new HashSet<String>();
        List<String> neNamesList = new ArrayList<String>(neNamesSet);
        Set<String> fdns = new HashSet<String>();
        NetworkElement networkElement = new NetworkElement();
        ShmNodeRestartJobData shmNodeRestartJobData = new ShmNodeRestartJobData();
        shmNodeRestartJobData.setJobType(JobType.NODERESTART);
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        neNamesSet.add("LTE01ERBS01");
        shmNodeRestartJobData.setNeNames(neNamesSet);
        List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        networkElements.add(networkElement);
        shmNodeRestartJobData.setCollection("Collection");
        fdns.add("NetworkElement=LTE01ERBS01");
        shmNodeRestartJobData.setFdns(fdns);
        when(fdnServiceBean.getNetworkElementsByNeNames(neNamesList, SHMCapabilities.NODE_RESTART_JOB_CAPABILITY)).thenReturn(networkElements);
        when(platformAndNeTypeFinder.findSupportedPlatformAndNodeTypes(SHMCapabilities.NODE_RESTART_JOB_CAPABILITY, shmNodeRestartJobData)).thenReturn(supportedPlatformTypeAndNodeTypes);
        when(topologyEvaluation.getCollectionPoId("Collection", "Admin")).thenReturn("poid");
        when(topologyEvaluation.getSavedSearchPoId("SavedSearchId", "Admin")).thenReturn("poid");
        final JobInfo jobInfo = nodeRestartJobInfoConverter.prepareJobInfoData(shmNodeRestartJobData);
        assertNotNull(jobInfo);
        assertTrue(jobInfo.getJobType().equals(JobTypeEnum.NODERESTART));
        assertTrue("NetworkElement=LTE01ERBS01".equals(jobInfo.getFdns().get(0)));
    }
}
