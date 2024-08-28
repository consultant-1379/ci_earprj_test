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
package com.ericsson.oss.services.shm.job.remote.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.ESecuritySubject;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.DatabaseNotAvailableException;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.job.entities.JobOutput;
import com.ericsson.oss.services.shm.job.exceptions.NoJobConfigurationException;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.impl.DomainTypeProviderFactory;
import com.ericsson.oss.services.shm.job.remote.api.DomainTypeResponse;
import com.ericsson.oss.services.shm.job.remote.api.JobStatusQuery;
import com.ericsson.oss.services.shm.job.remote.api.JobStatusResponse;
import com.ericsson.oss.services.shm.job.remote.api.ShmBackupJobData;
import com.ericsson.oss.services.shm.job.remote.api.ShmJobResponseResult;
import com.ericsson.oss.services.shm.job.service.SHMJobService;
import com.ericsson.oss.services.shm.jobs.common.api.DomainTypeProvider;
import com.ericsson.oss.services.shm.jobs.common.api.NeInfoQuery;
import com.ericsson.oss.services.shm.jobs.common.constants.JobModelConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.topologyservice.TopologyEvaluationService;
import com.ericsson.oss.services.topologyCollectionsService.api.TopologyCollectionsEjbService;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException;
import com.ericsson.oss.shm.job.entities.SHMJobData;

@RunWith(MockitoJUnitRunner.class)
public class ShmJobRemoteServiceImplTest {

    private static final Map<String, Object> RESPONSE = new HashMap<>();
    private static final int NODECOUNT = 5;
    private static final Long MAINJOBID = 8978997L;

    private static final String ERROR_CODE = "errorCode";

    @Mock
    JobInfoConverterFactory jobInfoConverterFactory;

    @Mock
    JobInfoConverterImpl jobInfoConverterImpl;

    @InjectMocks
    ShmJobRemoteServiceImpl objectUnderTest;

    @Mock
    private FdnServiceBean fdnServiceBeanMock;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private PlatformTypeProviderImpl platformTypeProviderImplMock;

    @Mock
    private DomainTypeProvider domainTypeProviderMock;

    @Mock
    private DomainTypeProviderFactory domainTypeProviderFactory;

    @Mock
    private NeInfoQuery neInfoQueryMock;

    @Mock
    SHMJobService shmJobService;

    @Mock
    private ESecuritySubject eSecuritySubjectMock;

    @Mock
    private EAccessControl accessControl;

    @Mock
    JobStatusHelper jobStatusHelper;

    @Mock
    private JobCapabilityProvider jobCapabilityProviderMock;

    @Mock
    private DpsRetryPolicies dpsRetryPoliciesMock;

    @Mock
    private RetryManager retryManager;

    @Mock
    RetryPolicy retryPolicyMock;

    @Mock
    TopologyCollectionsEjbService topologyCollectionsEjbServiceMock;

    @Mock
    private TopologyEvaluationService topologyEvaluationServiceMock;

    private static final String nodeName = "SGSN_Node";

    @Before
    public void setup() {
        RESPONSE.put(ERROR_CODE, "success");
    }

    @Test
    public void testGetJobStatusWithJobState() {
        final JobOutput jobOut = new JobOutput();
        when(accessControl.getAuthUserSubject()).thenReturn(eSecuritySubjectMock);
        jobOut.setResult(getMainJobData());
        when(jobStatusHelper.fetchMainJobStatus(any(JobStatusQuery.class))).thenReturn(jobOut);
        final JobStatusQuery jobStatusQueryParams = new JobStatusQuery();
        jobStatusQueryParams.setJobState(JobState.RUNNING);
        jobStatusQueryParams.setUserName("administrator");
        final JobStatusResponse jobStatusResponse = objectUnderTest.getJobStatus(jobStatusQueryParams);

        assertEquals(jobStatusResponse.getMainJobStatus().size(), getMainJobData().size());
        assertEquals(jobStatusResponse.getMainJobStatus().get(0).getCreatedBy(), "administrator");
        assertEquals(jobStatusResponse.getMainJobStatus().get(0).getStatus(), JobState.RUNNING.toString());
    }

    @Test
    public void testGetJobStatusWithJobName() {
        final JobOutput jobOut = new JobOutput();
        jobOut.setResult(getMainJobData());
        when(jobStatusHelper.fetchMainJobStatus(any(JobStatusQuery.class))).thenReturn(jobOut);
        final JobStatusQuery jobStatusQueryParams = new JobStatusQuery();
        jobStatusQueryParams.setJobName("dummyJobName");
        jobStatusQueryParams.setUserName("administrator");
        final JobStatusResponse jobStatusResponse = objectUnderTest.getJobStatus(jobStatusQueryParams);

        assertEquals(jobStatusResponse.getMainJobStatus().size(), getMainJobData().size());
        assertEquals(jobStatusResponse.getMainJobStatus().get(0).getCreatedBy(), "administrator");
        assertEquals(jobStatusResponse.getMainJobStatus().get(0).getJobName(), "dummyJobName");
    }

    @Test
    public void testGetJobStatusWithJobType() {
        final JobOutput jobOut = new JobOutput();
        jobOut.setResult(getMainJobData());
        when(jobStatusHelper.fetchMainJobStatus(any(JobStatusQuery.class))).thenReturn(jobOut);
        final JobStatusQuery jobStatusQueryParams = new JobStatusQuery();
        jobStatusQueryParams.setJobType(JobTypeEnum.BACKUP);
        jobStatusQueryParams.setUserName("administrator");
        final JobStatusResponse jobStatusResponse = objectUnderTest.getJobStatus(jobStatusQueryParams);

        assertEquals(jobStatusResponse.getMainJobStatus().size(), getMainJobData().size());
        assertEquals(jobStatusResponse.getMainJobStatus().get(0).getCreatedBy(), "administrator");
        assertEquals(jobStatusResponse.getMainJobStatus().get(0).getJobType(), JobTypeEnum.BACKUP.getAttribute());
    }

    private List<SHMJobData> getMainJobData() {
        final List<SHMJobData> mainJobStatus = new ArrayList<>();
        final SHMJobData shmJobData = new SHMJobData();
        shmJobData.setJobName("dummyJobName");
        shmJobData.setJobId(12345);
        shmJobData.setJobType("BACKUP");
        shmJobData.setCreatedBy("administrator");
        shmJobData.setTotalNoOfNEs("5");
        shmJobData.setProgress(70);
        shmJobData.setStatus("RUNNING");
        shmJobData.setResult("");
        shmJobData.setStartDate("1474951216250");
        shmJobData.setEndDate("1474953062283");
        mainJobStatus.add(shmJobData);
        return mainJobStatus;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetDomainTypeList() {

        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList(), Matchers.anyString())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("SGSN-MME");
        when(jobCapabilityProviderMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        when(platformTypeProviderImplMock.getPlatformTypeBasedOnCapability("SGSN-MME", SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(PlatformTypeEnum.ECIM);
        final Set<String> domainType = new HashSet<>();
        domainType.add("system/systemdata");
        domainType.add("system/systemlocal");

        when(domainTypeProviderFactory.getDomainTypeProvider(PlatformTypeEnum.ECIM)).thenReturn(domainTypeProviderMock);
        when(domainTypeProviderMock.getDomainTypeList(Matchers.any(NeInfoQuery.class))).thenReturn(domainType);
        final DomainTypeResponse domainTypeResponse = objectUnderTest.getDomainTypeList(nodeName);
        assertTrue(domainTypeResponse != null);
        final List<String> domainTypeList = domainTypeResponse.getDomainTypeList();
        assertTrue(domainTypeList.size() == 2);
        assertTrue(domainTypeList.contains("system/systemlocal"));
        assertTrue(domainTypeList.contains("system/systemdata"));
    }

    @Test
    public void testGetDomainTypeListWhenNoNeFound() {
        when(jobCapabilityProviderMock.getCapability(JobTypeEnum.BACKUP)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        final DomainTypeResponse domainTypeResponse = objectUnderTest.getDomainTypeList(nodeName);
        assertTrue(domainTypeResponse != null);
        assertTrue(domainTypeResponse.getDomainTypeErrorCode().equals(ShmJobResponseResult.FAILED));
        assertTrue(domainTypeResponse.getNeName().equals(nodeName));
        assertTrue(domainTypeResponse.getDomainTypeList() == null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetDomainTypeListWithOtherThanEcimPlatform() {
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(platformTypeProviderImplMock.getPlatformType("ERBS")).thenReturn(PlatformTypeEnum.CPP);
        when(domainTypeProviderFactory.getDomainTypeProvider(PlatformTypeEnum.CPP)).thenReturn(null);
        final DomainTypeResponse domainTypeResponse = objectUnderTest.getDomainTypeList(nodeName);
        assertTrue(domainTypeResponse != null);
        assertTrue(domainTypeResponse.getDomainTypeErrorCode().equals(ShmJobResponseResult.FAILED));
        assertTrue(domainTypeResponse.getDomainTypeList() == null);
    }

    @Test
    public void testCreateJob() throws IllegalArgumentException, NoJobConfigurationException, NoMeFDNSProvidedException, TopologyCollectionsServiceException {
        final ShmBackupJobData shmBackupJobData = new ShmBackupJobData();
        final JobInfo jobInfo = new JobInfo();
        when(shmJobService.createShmJob(Matchers.any(JobInfo.class))).thenReturn(RESPONSE);
        when(jobInfoConverterFactory.getJobInfoConverter(shmBackupJobData.getJobType())).thenReturn(jobInfoConverterImpl);
        when(jobInfoConverterImpl.prepareJobInfoData(shmBackupJobData)).thenReturn(jobInfo);
        objectUnderTest.createJob(shmBackupJobData);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateJobWithNoJobConfigurationException() throws IllegalArgumentException, NoJobConfigurationException, NoMeFDNSProvidedException, TopologyCollectionsServiceException {
        final ShmBackupJobData shmBackupJobData = new ShmBackupJobData();
        final JobInfo jobInfo = new JobInfo();
        when(shmJobService.createShmJob(Matchers.any(JobInfo.class))).thenThrow(NoJobConfigurationException.class);
        when(jobInfoConverterFactory.getJobInfoConverter(shmBackupJobData.getJobType())).thenReturn(jobInfoConverterImpl);
        when(jobInfoConverterImpl.prepareJobInfoData(shmBackupJobData)).thenReturn(jobInfo);
        objectUnderTest.createJob(shmBackupJobData);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateJobWithNoMeFDNSProvidedException() throws IllegalArgumentException, NoJobConfigurationException, NoMeFDNSProvidedException, TopologyCollectionsServiceException {
        final ShmBackupJobData shmBackupJobData = new ShmBackupJobData();
        final JobInfo jobInfo = new JobInfo();
        when(shmJobService.createShmJob(Matchers.any(JobInfo.class))).thenThrow(NoMeFDNSProvidedException.class);
        when(jobInfoConverterFactory.getJobInfoConverter(shmBackupJobData.getJobType())).thenReturn(jobInfoConverterImpl);
        when(jobInfoConverterImpl.prepareJobInfoData(shmBackupJobData)).thenReturn(jobInfo);
        objectUnderTest.createJob(shmBackupJobData);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateTotalNodeCount() {
        final Map<String, Object> jobAttributes = new HashMap<>();
        jobAttributes.put(JobModelConstants.NUMBER_OF_NETWORK_ELEMENTS, NODECOUNT);
        when(dpsRetryPoliciesMock.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManager.executeCommand(any(RetryPolicy.class), any(RetriableCommand.class))).thenReturn(true);
        when(shmJobService.updateNodeCountOfJob(MAINJOBID, NODECOUNT)).thenReturn(true);
        assertEquals(true, objectUnderTest.updateNodeCount(MAINJOBID, NODECOUNT));
    }

    @Test
    public void testNotupdateTotalNodeCount() {
        assertEquals(false, objectUnderTest.updateNodeCount(MAINJOBID, NODECOUNT));
    }

    @Test(expected = DatabaseNotAvailableException.class)
    public void testupdatetTotalNodeCountThrowsDatabaseNotAvailableException() {
        Mockito.when(shmJobService.updateNodeCountOfJob(MAINJOBID, NODECOUNT)).thenThrow(new DatabaseNotAvailableException("DB not available at this moment"));
        assertNull(objectUnderTest.updateNodeCount(MAINJOBID, NODECOUNT));
    }

    @Test
    public void testGetCollectionPoId() {
        final String collectionName = "collectionName";
        final String jobOwner = "jobOwner";
        final String collectionPoId = "collectionPoId";
        when(topologyEvaluationServiceMock.getCollectionPoId(collectionName, jobOwner)).thenReturn(collectionPoId);
        assertEquals(collectionPoId, objectUnderTest.getCollectionPoId(collectionName, jobOwner));
    }

    @Test
    public void testGetSavedSearchPoId() {
        final String savedSearchName = "savedSearchName";
        final String jobOwner = "jobOwner";
        final String savedSearchPoId = "savedSearchPoId";
        when(topologyEvaluationServiceMock.getSavedSearchPoId(savedSearchName, jobOwner)).thenReturn(savedSearchPoId);
        assertEquals(savedSearchPoId, objectUnderTest.getSavedSearchPoId(savedSearchName, jobOwner));
    }

    @Test
    public void testGetCollectionInfo() {
        final String jobOwner = "jobOwner";
        final String collectionPoId = "collectionPoId";
        final Set<String> neNames = Stream.of("LTE01ERBS", "LTE01dg2").collect(Collectors.toSet());
        when(topologyEvaluationServiceMock.getCollectionInfo(jobOwner, collectionPoId)).thenReturn(neNames);
        assertEquals(neNames, objectUnderTest.getCollectionInfo(collectionPoId, jobOwner));
    }

    @Test
    public void testGetSavedSearchInfo() {
        final String jobOwner = "jobOwner";
        final String savedSearchPoId = "savedSearchPoId";
        final Set<String> neNames = Stream.of("LTE01ERBS", "LTE01dg2").collect(Collectors.toSet());
        when(topologyEvaluationServiceMock.getSavedSearchInfo(jobOwner, savedSearchPoId)).thenReturn(neNames);
        assertEquals(neNames, objectUnderTest.getSavedSearchInfo(savedSearchPoId, jobOwner));
    }

}
