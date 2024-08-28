package com.ericsson.oss.services.shm.webpush;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.webpush.api.JobWebPushEvent;
import com.ericsson.oss.services.shm.webpush.api.WebPushConstants;
import com.ericsson.oss.services.shm.webpush.retry.JobsWebpushRetryProxy;
import com.ericsson.oss.uisdk.restsdk.webpush.api.exception.WebPushBroadcastException;

@RunWith(MockitoJUnitRunner.class)
public class JobWebpushEventListenerTest {

    @InjectMocks
    private JobWebpushEventListener jobWebpushEventListener;

    @Mock
    private JobWebPushEvent jobWebPushEvent;

    @Mock
    private Map<String, Object> mapMock;

    @Mock
    private JobsWebpushRetryProxy jobsWebpushRetryProxyMock;

    @Test
    public void listenForJobWebPushEventTest_mainJobsPage() throws WebPushBroadcastException {
        when(jobWebPushEvent.getApplicationType()).thenReturn(WebPushConstants.MAIN_JOBS_APPLICATION);
        when(jobWebPushEvent.getAttributeMap()).thenReturn(mapMock);
        when(jobWebPushEvent.getJobId()).thenReturn("1");
        jobWebpushEventListener.listenForJobWebPushEvent(jobWebPushEvent);
        verify(jobsWebpushRetryProxyMock, times(1)).pushToShmPage(jobWebPushEvent);
    }

    @Test
    public void listenForJobWebPushEventTest_jobDetaisPage() throws WebPushBroadcastException {

        when(jobWebPushEvent.getApplicationType()).thenReturn(WebPushConstants.JOB_DETAILS_APPLICATION);
        when(jobWebPushEvent.getAttributeMap()).thenReturn(mapMock);
        when(jobWebPushEvent.getJobId()).thenReturn("1");
        jobWebpushEventListener.listenForJobWebPushEvent(jobWebPushEvent);
        verify(jobsWebpushRetryProxyMock, times(1)).pushToShmJobDetailsPage(jobWebPushEvent);
    }

    @Test
    public void listenForJobWebPushEventTest_createDeleteJobs() throws WebPushBroadcastException {

        when(jobWebPushEvent.getApplicationType()).thenReturn(WebPushConstants.SHM_JOBS_APPLICATION);
        when(jobWebPushEvent.getAttributeMap()).thenReturn(mapMock);
        when(jobWebPushEvent.getJobId()).thenReturn("1");
        jobWebpushEventListener.listenForJobWebPushEvent(jobWebPushEvent);
        verify(jobsWebpushRetryProxyMock, times(1)).pushToShmJobsPage(jobWebPushEvent);
    }

    @Test
    public void listenForJobWebPushEventTest_jobLogs() throws WebPushBroadcastException {

        when(jobWebPushEvent.getApplicationType()).thenReturn(WebPushConstants.JOB_LOGS_APPLICATION);
        when(jobWebPushEvent.getAttributeMap()).thenReturn(mapMock);
        when(jobWebPushEvent.getJobId()).thenReturn("1");
        jobWebpushEventListener.listenForJobWebPushEvent(jobWebPushEvent);
        verify(jobsWebpushRetryProxyMock, times(1)).pushToShmJobLogsPage(jobWebPushEvent);
    }

    @Test
    public void listenForJobWebPushEventTest_invalidEvent() throws WebPushBroadcastException {

        when(jobWebPushEvent.getApplicationType()).thenReturn("");
        when(jobWebPushEvent.getAttributeMap()).thenReturn(mapMock);
        when(jobWebPushEvent.getJobId()).thenReturn("1");
        jobWebpushEventListener.listenForJobWebPushEvent(jobWebPushEvent);

        verify(jobsWebpushRetryProxyMock, never()).pushToShmPage(jobWebPushEvent);
        verify(jobsWebpushRetryProxyMock, never()).pushToShmJobDetailsPage(jobWebPushEvent);
        verify(jobsWebpushRetryProxyMock, never()).pushToShmJobsPage(jobWebPushEvent);
        verify(jobsWebpushRetryProxyMock, never()).pushToShmJobLogsPage(jobWebPushEvent);
    }

    @Test
    public void listenForJobWebPushEventTest_null() throws WebPushBroadcastException {

        when(jobWebPushEvent.getApplicationType()).thenReturn(null);
        jobWebpushEventListener.listenForJobWebPushEvent(null);
        verify(jobsWebpushRetryProxyMock, times(0)).pushToShmPage(jobWebPushEvent);
        verify(jobsWebpushRetryProxyMock, times(0)).pushToShmJobDetailsPage(jobWebPushEvent);
        verify(jobsWebpushRetryProxyMock, times(0)).pushToShmJobsPage(jobWebPushEvent);
        verify(jobsWebpushRetryProxyMock, times(0)).pushToShmJobLogsPage(jobWebPushEvent);
    }

}
