
package com.ericsson.oss.services.shm.job.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.context.ContextService;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.UserContextBean;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.job.entities.ExportJobLogRequest;
import com.ericsson.oss.services.shm.job.entities.JobInput;
import com.ericsson.oss.services.shm.job.entities.JobLogRequest;
import com.ericsson.oss.services.shm.job.entities.JobOutput;
import com.ericsson.oss.services.shm.job.exceptions.NoJobConfigurationException;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.impl.JobConfigurationServiceImpl;
import com.ericsson.oss.services.shm.job.service.SHMJobService;
import com.ericsson.oss.services.shm.job.utils.SHMJobUtil;
import com.ericsson.oss.services.shm.jobexecutorlocal.JobExecutorLocal;
import com.ericsson.oss.services.shm.jobs.common.api.JobConfigurationException;
import com.ericsson.oss.services.shm.jobs.common.api.JobQuery;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportData;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobInput;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Job;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobs.common.restentities.RestJobConfiguration;
import com.ericsson.oss.services.shm.jobservice.common.CollectionOrSavedSearchDetails;
import com.ericsson.oss.services.shm.jobservice.common.CommentInfo;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

@RunWith(MockitoJUnitRunner.class)
public class SHMJobResourceTest {

    @Mock
    SHMJobService shmJobServiceMock;

    @Mock
    JobConfigurationServiceImpl jobConfigurationService;

    @Mock
    JobLogRequest jobLogMock;

    @Mock
    JobMapper jobMapper;

    @Mock
    RestJobConfiguration restJobConfMock;

    @Mock
    JobReportData jobReportDataMock;

    @Mock
    JobInput jobInputMock;

    @Mock
    JobOutput jobOutputMock;

    @Mock
    JobInfo jobInfoMock;

    @Mock
    Map<String, Object> mapMock;

    @Mock
    JobExecutorLocal jobExecutorMock;

    @InjectMocks
    SHMJobResourceImpl shmJobResourceImpl;

    @Mock
    CommentInfo commentInfoMock;

    @Mock
    SHMJobUtil shmJobUtilMock;

    @Mock
    ContextService contextServiceMock;

    @Mock
    List<String> contextListMock;

    @Mock
    ExportJobLogRequest exportJobLogRequestMock;

    @Mock
    JobTemplate jobTemplateMock;

    @Mock
    private RestDataMapper dataMapper;

    @Mock
    private UserContextBean userContextBean;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private NetworkElementData networkElementData;

    @Mock
    private PlatformTypeProviderImpl platformTypeProviderImpl;

    private static final String COLLECTIONID = "281474979304775";

    @Test
    public void shouldGetResponseOfJobsForAListOfPoIds() {
        final List<Long> poIds = new ArrayList<>();
        poIds.add(123L);

        final List<Job> jobs = new ArrayList<>();
        jobs.add(new Job());

        Mockito.when(shmJobServiceMock.getJobProgressDetails(poIds)).thenReturn(jobs);

        final Response response = shmJobResourceImpl.getJobProgressDetails(poIds);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void shouldGet204EmptyForAEmptyListOfPoIds() {
        final List<Long> poIds = new ArrayList<>();

        Mockito.when(shmJobServiceMock.getJobProgressDetails(poIds)).thenReturn(new ArrayList<Job>());

        final Response response = shmJobResourceImpl.getJobProgressDetails(poIds);

        assertEquals(204, response.getStatus());
    }

    @Test
    public void testDeleteJobs() {
        final List<String> asList = Arrays.asList("123456876");
        final Response actualResponse = shmJobResourceImpl.deleteJobs(asList);
        assertEquals(200, actualResponse.getStatus());
        verify(shmJobServiceMock).deleteShmJobs(asList);
    }

    @Test
    public void test_getJobConfigurationFails_whenNodataFound() {
        final Long jobConfigId = (long) 123456789;
        final Response actualResponse = shmJobResourceImpl.getJobConfiguration(jobConfigId);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), actualResponse.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_getJobConfigurationFails_whenThrowsException() throws JobConfigurationException {
        final Long jobConfigId = (long) 123456789;
        when(jobMapper.getJobTemplateDetails(any(Map.class), any(Long.class))).thenThrow(JobConfigurationException.class);
        final Response actualResponse = shmJobResourceImpl.getJobConfiguration(jobConfigId);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void test_getJobConfigurationSuccess_whendataFound() throws JobConfigurationException {
        final Long jobConfigId = (long) 123456789;
        final JobTemplate conf = new JobTemplate();
        when(shmJobServiceMock.getJobTemplates(any(JobQuery.class))).thenReturn(Arrays.asList(conf));
        when(dataMapper.mapJobConfigToRestDataFormat(conf)).thenReturn(restJobConfMock);

        final Response actualResponse = shmJobResourceImpl.getJobConfiguration(jobConfigId);
        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void test_retrieveJobLogs() {
        when(shmJobServiceMock.retrieveJobLogs(jobLogMock)).thenReturn(new JobOutput());
        final Response actualResponse = shmJobResourceImpl.retrieveJobLogs(jobLogMock);
        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void testGetSHMJobReportDetailsShouldReturnResponseAsNotFoundWhenJobReportDataIsNull() {
        final Long jobConfigId = (long) 123456789;
        when(shmJobServiceMock.getJobReportDetails(any(NeJobInput.class))).thenReturn(null);
        final Response actualResponse = shmJobResourceImpl.getSHMJobReportDetails(jobConfigId, 1, 1, "colums", "desc", false);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void testGetSHMJobReportDetailsShouldReturnResponseAsNotFoundWhenJobReportDataIsEmpty() {
        final Long jobConfigId = (long) 123456789;
        jobReportDataMock = new JobReportData();
        when(shmJobServiceMock.getJobReportDetails(any(NeJobInput.class))).thenReturn(jobReportDataMock);

        final Response actualResponse = shmJobResourceImpl.getSHMJobReportDetails(jobConfigId, 1, 1, "colums", "desc", false);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void testGetSHMJobReportDetailsShouldReturnResponseAsNotFoundWhenNeDetailsIsEmpty() {
        final Long jobConfigId = (long) 123456789;
        final JobReportDetails jobDetails = new JobReportDetails();
        jobDetails.setJobName("job_1");
        jobDetails.setJobType("backup");
        jobReportDataMock = new JobReportData(jobDetails, null);
        when(shmJobServiceMock.getJobReportDetails(any(NeJobInput.class))).thenReturn(jobReportDataMock);

        final Response actualResponse = shmJobResourceImpl.getSHMJobReportDetails(jobConfigId, 1, 1, "colums", "desc", false);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void testGetSHMJobReportDetailsShouldReturnResponseAsNotFoundWhenJobDetailsIsEmpty() {
        final Long jobConfigId = (long) 123456789;
        final NeDetails neDetails = new NeDetails();
        neDetails.setTotalCount(1);
        neDetails.setClearOffset(true);
        jobReportDataMock = new JobReportData(null, neDetails);
        when(shmJobServiceMock.getJobReportDetails(any(NeJobInput.class))).thenReturn(jobReportDataMock);

        final Response actualResponse = shmJobResourceImpl.getSHMJobReportDetails(jobConfigId, 1, 1, "colums", "desc", false);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), actualResponse.getStatus());
    }


    @Test
    public void test_getSHMJobReportDetailsSuccess() {
        final Long jobConfigId = (long) 123456789;

        final JobReportDetails jobDetails = new JobReportDetails();
        jobDetails.setJobName("job_1");
        jobDetails.setJobType("backup");

        final NeDetails neDetails = new NeDetails();
        neDetails.setTotalCount(1);
        neDetails.setClearOffset(true);

        jobReportDataMock = new JobReportData(jobDetails, neDetails);
        when(shmJobServiceMock.getJobReportDetails(any(NeJobInput.class))).thenReturn(jobReportDataMock);

        final Response actualResponse = shmJobResourceImpl.getSHMJobReportDetails(jobConfigId, 1, 1, "colums", "desc", true);
        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void test_getSHMJobsSuccess() {
        when(shmJobServiceMock.getJobDetails(jobInputMock)).thenReturn(jobOutputMock);
        when(jobOutputMock.getTotalCount()).thenReturn(1);

        final Response actualResponse = shmJobResourceImpl.getSHMJobs(jobInputMock);
        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
        verify(systemRecorder, times(1)).recordEvent(Matchers.anyString(), (EventLevel) Matchers.any(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_createJobFails_whenThrowsException() throws IllegalArgumentException, NoJobConfigurationException, NoMeFDNSProvidedException {
        when(shmJobServiceMock.createShmJob(jobInfoMock)).thenThrow(NoMeFDNSProvidedException.class);
        final Response actualResponse = shmJobResourceImpl.createJob(jobInfoMock);
        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void test_createJob() throws IllegalArgumentException, NoJobConfigurationException, NoMeFDNSProvidedException {
        final JobInfo jobInfo = new JobInfo();
        when(shmJobServiceMock.createShmJob(jobInfo)).thenReturn(mapMock);
        when(mapMock.get("errorCode")).thenReturn("success");
        final Response actualResponse = shmJobResourceImpl.createJob(jobInfo);
        assertEquals(Response.Status.CREATED.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void test_createJob_nameNull() throws IllegalArgumentException, NoJobConfigurationException, NoMeFDNSProvidedException {
        final JobInfo jobInfo = new JobInfo();
        jobInfo.setName("name");
        when(shmJobServiceMock.createShmJob(jobInfo)).thenReturn(mapMock);
        when(mapMock.get("errorCode")).thenReturn("success");
        final Response actualResponse = shmJobResourceImpl.createJob(jobInfo);
        assertEquals(Response.Status.CREATED.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void test_createJobOk() throws IllegalArgumentException, NoJobConfigurationException, NoMeFDNSProvidedException {
        final JobInfo jobInfo = new JobInfo();
        when(shmJobServiceMock.createShmJob(jobInfo)).thenReturn(mapMock);
        when(mapMock.get("errorCode")).thenReturn("ok");
        final Response actualResponse = shmJobResourceImpl.createJob(jobInfo);
        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_createJobException() throws IllegalArgumentException, NoJobConfigurationException, NoMeFDNSProvidedException {
        final JobInfo jobInfo = new JobInfo();
        when(shmJobServiceMock.createShmJob(jobInfo)).thenThrow(NoJobConfigurationException.class);
        when(mapMock.get("errorCode")).thenReturn("ok");
        final Response actualResponse = shmJobResourceImpl.createJob(jobInfo);
        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void test_addComment() {
        final Response actualResponse = shmJobResourceImpl.addComment(commentInfoMock);
        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
        verify(shmJobServiceMock).addJobComment(commentInfoMock);

    }

    @Test
    public void testCancelJobs() {
        final List<Long> jobIds = getJobIds();
        when(shmJobServiceMock.cancelJobs(jobIds)).thenReturn(mapMock);
        final Response actualResponse = shmJobResourceImpl.cancelJobs(jobIds);
        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
        verify(shmJobServiceMock).cancelJobs(jobIds);
    }

    @Test
    public void testInvokeMainJobsManually() {
        final List<Long> jobIds = getJobIds();
        when(userContextBean.getLoggedInUserName()).thenReturn("user");
        shmJobResourceImpl.invokeMainJobsManually(jobIds);
        verify(jobExecutorMock).invokeMainJobsManually(jobIds, "user");
    }

    @Test
    public void testInvokeNeJobsManually() {
        final List<Long> jobIds = getJobIds();
        when(userContextBean.getLoggedInUserName()).thenReturn("user");
        shmJobResourceImpl.invokeNeJobsManually(jobIds);
        verify(jobExecutorMock).invokeNeJobsManually(jobIds, "user");
    }

    @Test
    public void testExportJobLogsWithEmptyneJobIdList() {
        final Response actualResponse = shmJobResourceImpl.exportJobLogs("", "12312");
        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void testExportJobLogs() {
        final List<Long> neJobIdList = new ArrayList<>();
        neJobIdList.add(1L);
        neJobIdList.add(2L);
        neJobIdList.add(3L);
        when(shmJobUtilMock.getNEJobIdListforExport("1,2,3")).thenReturn(neJobIdList);
        final Long neJobId = neJobIdList.get(0);
        final JobTemplate templateJobAttributeMap = new JobTemplate();
        templateJobAttributeMap.setName("abc");
        final JobType jobType = JobType.BACKUP;
        templateJobAttributeMap.setJobType(jobType);
        when(shmJobServiceMock.getJobTemplateByNeJobId(neJobId)).thenReturn(templateJobAttributeMap);
        final Response actualResponse = shmJobResourceImpl.exportJobLogs("1,2,3", "123L");
        when(shmJobServiceMock.exportJobLogs(exportJobLogRequestMock)).thenReturn("csvOutput");
        when(contextServiceMock.getContextValue("X-Tor-UserID")).thenReturn("user");
        when(contextListMock.get(0)).thenReturn("X-TOR-xid");
        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
    }

    /**
     * @return
     */
    private List<Long> getJobIds() {
        final long jobId1 = 123L;
        final long jobId2 = 1234L;
        final long jobId3 = 12345L;
        final List<Long> jobIds = new ArrayList<>();
        jobIds.add(jobId1);
        jobIds.add(jobId2);
        jobIds.add(jobId3);
        return jobIds;
    }

    private Map<String, Object> getCollectionOrSS() throws IOException {

        final List<Map<String, Object>> mainJobsPropertyList = new ArrayList<>();
        final Map<String, Object> mainJobAttribute = new HashMap<>();
        final Map<String, Object> map = new HashMap<>();

        final Map<String, Object> map1 = new HashMap<>();
        final Set<String> set = new HashSet<>();
        set.add("NetworkElement=LTE102ERBS00001");

        map1.put(COLLECTIONID, set);
        map1.put(ShmConstants.COLLECTIONORSSNAME, "testcollection");

        final List<Map<String, Object>> list = new LinkedList<>();
        list.add(map1);

        final ObjectMapper mapper = new ObjectMapper();
        final String convertedString = mapper.writeValueAsString(list);

        map.put("key", "collectionIdsOrsaveSearchedIdsInfo");
        map.put("value", convertedString);
        mainJobsPropertyList.add(map);
        mainJobAttribute.put("jobProperties", mainJobsPropertyList);

        return mainJobAttribute;
    }

    @Test
    public void testGetJobTemplateNull() {
        final String name = null;
        when(shmJobServiceMock.getJobTemplate(name)).thenReturn(null);

        final Response actualResponse = shmJobResourceImpl.getJobTemplate(name);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void testGetJobTemplateNotNull() {
        final String name = null;
        when(shmJobServiceMock.getJobTemplate(name)).thenReturn(jobTemplateMock);

        final Response actualResponse = shmJobResourceImpl.getJobTemplate(name);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void testGetCollectionOrSavedSearchDetails() throws IOException, MoNotFoundException {
        when(jobConfigurationService.retrieveJob(1L)).thenReturn(getCollectionOrSS());
        when(networkElementRetrievalBean.getNetworkElementData("LTE102ERBS00001")).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("ERBS");
        when(networkElementData.getNeFdn()).thenReturn("NetworkElement=LTE102ERBS00001");
        when(platformTypeProviderImpl.getPlatformType(Matchers.anyString())).thenReturn(PlatformTypeEnum.CPP);
        final Response actualResponse = shmJobResourceImpl.getCollectionOrSavedSearchDetails(1L);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
        final List<CollectionOrSavedSearchDetails> collectionDetails = (List<CollectionOrSavedSearchDetails>) actualResponse.getEntity();
        Assert.assertEquals("testcollection", collectionDetails.get(0).getName());
    }

    @Test
    public void testGetCollectionOrSavedSearchDetailsFailure() throws IOException, MoNotFoundException {
        final Response actualResponse = shmJobResourceImpl.getCollectionOrSavedSearchDetails(1L);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), actualResponse.getStatus());
    }
}
