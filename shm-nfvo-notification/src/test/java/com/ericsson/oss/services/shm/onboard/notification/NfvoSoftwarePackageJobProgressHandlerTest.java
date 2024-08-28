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
package com.ericsson.oss.services.shm.onboard.notification;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.mediation.vran.model.response.NfvoSwPackageMediationResponse;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.impl.ActivityServiceProvider;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

/**
 * Class to test the OnBoardProgressHandlerTest class with the help of jUnits.
 */
@RunWith(MockitoJUnitRunner.class)
public class NfvoSoftwarePackageJobProgressHandlerTest {

    @InjectMocks
    private NfvoSoftwarePackageJobProgressHandler objectUndertest;

    @Mock
    private NfvoSwPackageMediationResponse onBoardJobResponse;

    @Mock
    private NotificationRegistry registry;

    @Mock
    private NotificationSubject subject;

    @Mock
    private FdnNotificationSubject fdnNotificationSubject;

    @Mock
    private NfvoSoftwarePackageJobResponseImpl onBoardNotificationBean;

    @Mock
    private ActivityServiceProvider activityServiceProvider;

    @Mock
    ActivityCallback activityCallbackMock;

    /**
     * Test case to test the external calls in handleJobProgressResponse() for create.
     */
    @Test
    public void testHandleJobProgressResponseForCreate() {
        onBoardJobResponse = getonBoardJobResponse();
        final Date notificationReceivedDate = new Date();
        final Map<String, Object> additionalAttributes = onBoardJobResponse.getAdditionalAttributes();
        additionalAttributes.put("jobId", "VNF1001");
        additionalAttributes.put("vnfPackageId", "VNF1001");
        final String key = "nodeAddress@12345";
        when(registry.getListener(key)).thenReturn(subject);
        objectUndertest.handleJobProgressResponse(onBoardJobResponse, notificationReceivedDate);
        verify(registry).getListener(key);
        verify(subject).setTimeStamp(notificationReceivedDate);
    }

    /**
     * Method to populate the values for onBoardJobResponse object
     */
    private NfvoSwPackageMediationResponse getonBoardJobResponse() {
        final NfvoSwPackageMediationResponse onBoardJobResponse = new NfvoSwPackageMediationResponse();
        onBoardJobResponse.setNodeAddress("nodeAddress");
        onBoardJobResponse.setResponseType("responseType");
        onBoardJobResponse.setErrorMessage("errorMessage");
        onBoardJobResponse.setErrorCode(0);
        onBoardJobResponse.setAdditionalAttribute("jobId", "jobid");
        onBoardJobResponse.setAdditionalAttribute("vnfPackageId", "vnfPackageId");
        onBoardJobResponse.setAdditionalAttribute("activityJobId", (long) 12345);
        return onBoardJobResponse;
    }

    /**
     * Test case to test the external calls in handleJobProgressResponse() by passing null onBoardJobResponse.
     */
    @Test
    public void testHandleJobProgressResponseWithNull() {
        onBoardJobResponse = null;
        final Date notificationReceivedDate = new Date();
        objectUndertest.handleJobProgressResponse(onBoardJobResponse, notificationReceivedDate);
        assertNull(onBoardJobResponse);
    }

    /**
     * Test case to test the external calls in handleJobProgressResponse() by passing null JobId.
     */
    @Test
    public void testHandleJobProgressResponseWithJobIdNull() {
        onBoardJobResponse = getonBoardJobResponseForNullJobID();
        final Date notificationReceivedDate = new Date();
        objectUndertest.handleJobProgressResponse(null, notificationReceivedDate);
    }

    /**
     * Method to populate the values for onBoardJobResponse object for Null JobID check
     * 
     * @return onBoardJobResponse
     */
    private NfvoSwPackageMediationResponse getonBoardJobResponseForNullJobID() {
        final NfvoSwPackageMediationResponse onBoardJobResponse = new NfvoSwPackageMediationResponse();
        onBoardJobResponse.setNodeAddress("nodeAddress");
        onBoardJobResponse.setResponseType("responseType");
        onBoardJobResponse.setErrorMessage("errorMessage");
        onBoardJobResponse.setErrorCode(0);
        onBoardJobResponse.setAdditionalAttribute("jobId", null);
        onBoardJobResponse.setAdditionalAttribute("vnfPackageId", "vnfPackageId");
        return onBoardJobResponse;
    }

    /**
     * Test case to test the external calls in getActivityImpl().
     */

    @Test
    public void testGetActivityImpl() {
        final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType("ONBOARD");
        onBoardNotificationBean = getTestNotificationExtJob();
        final String activityName = onBoardNotificationBean.getActivityName();
        final PlatformTypeEnum platform = PlatformTypeEnum.getPlatform("vRAN");
        onBoardJobResponse = getonBoardJobResponse();
        when(activityServiceProvider.getActivityNotificationHandler(platform, jobTypeEnum, activityName)).thenReturn(activityCallbackMock);
        objectUndertest.getActivityImpl(activityName, jobTypeEnum, platform);
        verify(activityServiceProvider).getActivityNotificationHandler(platform, jobTypeEnum, activityName);
    }

    /**
     * Method to populate the values for NotificationExtJobImpl object.
     * 
     * @return NotificationExtJobImpl
     */

    private NfvoSoftwarePackageJobResponseImpl getTestNotificationExtJob() {
        final String activityName = "onboard";
        final long activityJobId = 100;
        final Date notificationTimeStamp = new Date();
        onBoardJobResponse = getonBoardJobResponse();
        final NfvoSoftwarePackageJobResponseImpl onBoardNotificationBean = new NfvoSoftwarePackageJobResponseImpl();
        onBoardNotificationBean.setNodeAddress(onBoardJobResponse.getNodeAddress());
        onBoardNotificationBean.setVnfPackageId("vnfPackageId");
        onBoardNotificationBean.setJobId("jobId");
        onBoardNotificationBean.setResponseType(onBoardJobResponse.getResponseType());
        onBoardNotificationBean.setStatus("status");
        onBoardNotificationBean.setActivityJobId(activityJobId);
        onBoardNotificationBean.setActivityName(activityName);
        onBoardNotificationBean.setNotificationTimeStamp(notificationTimeStamp);
        onBoardNotificationBean.setResult("result");
        onBoardNotificationBean.setErrorMessage(onBoardJobResponse.getErrorMessage());
        onBoardNotificationBean.setErrorCode(onBoardJobResponse.getErrorCode());
        onBoardNotificationBean.setDescription("description");
        return onBoardNotificationBean;
    }

}
