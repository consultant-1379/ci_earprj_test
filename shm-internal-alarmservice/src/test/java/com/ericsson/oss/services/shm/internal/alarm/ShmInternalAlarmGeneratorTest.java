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
package com.ericsson.oss.services.shm.internal.alarm;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@RunWith(MockitoJUnitRunner.class)
@PrepareForTest({ ClientResponse.class, ClientRequest.class, ShmInternalAlarmGenerator.class })
public class ShmInternalAlarmGeneratorTest {

    @InjectMocks
    private ShmInternalAlarmGenerator shmInternalAlarmGenerator;

    @Mock
    private ClientRequest request;

    @Mock
    private ClientResponse<String> response;

    @Mock
    protected SystemRecorder systemRecorder;

    @SuppressWarnings("deprecation")
    @Test
    public void test_raiseFmAlarmSuccess() {
        final String jobName = "backupjob";
        final String user = "admnistrator";
        final Date creationDate = new Date();
        final String jobType = "Backup";
        final Integer totalNesSize = 4;
        final Integer failedNesSize = 2;
        MockitoAnnotations.initMocks(this);
        final Map<String, Object> alarmDetails = new HashMap<String, Object>();
        alarmDetails.put(ShmConstants.NAME, jobName);
        alarmDetails.put(ShmConstants.OWNER, user);
        alarmDetails.put(ShmConstants.CREATION_TIME, creationDate);
        alarmDetails.put(ShmConstants.JOB_TYPE, jobType);
        alarmDetails.put("TOTAL_NES", totalNesSize);
        alarmDetails.put("FAILED_NES", failedNesSize);
        alarmDetails.put(ShmConstants.EVENT_TYPE, "SHM_ERROR");
        alarmDetails.put(ShmConstants.PROBABLE_CAUSE, "SHM_JOB_FAILURE");
        alarmDetails.put(ShmConstants.SPECIFIC_PROBLEM, jobType.toUpperCase() + "_JOB_FAILURE");
        alarmDetails.put(ShmConstants.PERCEIVED_SEVERITY, "MAJOR");
        alarmDetails.put(ShmConstants.RECORD_TYPE, "ERROR");
        alarmDetails.put(ShmConstants.MANAGED_OBJECT_INSTANCE, "ManagementSystem=ENM");
        alarmDetails.put(ShmConstants.ADDITIONAL_TEXT, "Test for the additional text");
        request = PowerMockito.mock(ClientRequest.class);

        response = PowerMockito.mock(ClientResponse.class);
        try {
            PowerMockito.whenNew(ClientRequest.class).withArguments(Matchers.anyString()).thenReturn(request);

            PowerMockito.when(request.post(String.class)).thenAnswer(new Answer<ClientResponse<String>>() {

                @Override
                public ClientResponse<String> answer(final InvocationOnMock invocation) throws Throwable {

                    return response;
                }
            });

            Mockito.when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        } catch (final Exception e1) {
            e1.printStackTrace();
        }

        shmInternalAlarmGenerator.raiseInternalAlarm(alarmDetails);
        verify(systemRecorder, times(0)).recordEvent(Matchers.anyString(), Matchers.any(EventLevel.class), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void test_raiseFmAlarmFailure() {
        final String jobName = "backupjob";
        final String user = "admnistrator";
        final Date creationDate = new Date();
        final String jobType = "Backup";
        final Integer totalNesSize = 4;
        final Integer failedNesSize = 2;
        MockitoAnnotations.initMocks(this);
        final Map<String, Object> alarmDetails = new HashMap<String, Object>();
        alarmDetails.put(ShmConstants.NAME, jobName);
        alarmDetails.put(ShmConstants.OWNER, user);
        alarmDetails.put(ShmConstants.CREATION_TIME, creationDate);
        alarmDetails.put(ShmConstants.JOB_TYPE, jobType);
        alarmDetails.put("TOTAL_NES", totalNesSize);
        alarmDetails.put("FAILED_NES", failedNesSize);
        alarmDetails.put(ShmConstants.EVENT_TYPE, "SHM_ERROR");
        alarmDetails.put(ShmConstants.PROBABLE_CAUSE, "SHM_JOB_FAILURE");
        alarmDetails.put(ShmConstants.SPECIFIC_PROBLEM, jobType.toUpperCase() + "_JOB_FAILURE");
        alarmDetails.put(ShmConstants.PERCEIVED_SEVERITY, "MAJOR");
        alarmDetails.put(ShmConstants.RECORD_TYPE, "ERROR");
        alarmDetails.put(ShmConstants.MANAGED_OBJECT_INSTANCE, "ManagementSystem=ENM");
        alarmDetails.put(ShmConstants.ADDITIONAL_TEXT, "Test for the additional text");
        request = PowerMockito.mock(ClientRequest.class);

        response = PowerMockito.mock(ClientResponse.class);
        try {
            PowerMockito.whenNew(ClientRequest.class).withArguments(Matchers.anyString()).thenReturn(request);

            PowerMockito.when(request.post(String.class)).thenAnswer(new Answer<ClientResponse<String>>() {

                @Override
                public ClientResponse<String> answer(final InvocationOnMock invocation) throws Throwable {

                    return response;
                }
            });

            Mockito.when(response.getStatus()).thenReturn(Response.Status.PRECONDITION_FAILED.getStatusCode());
        } catch (final Exception e1) {
            e1.printStackTrace();
        }

        shmInternalAlarmGenerator.raiseInternalAlarm(alarmDetails);
        verify(systemRecorder, times(1)).recordError(Matchers.anyString(), Matchers.any(ErrorSeverity.class), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
    }
}
