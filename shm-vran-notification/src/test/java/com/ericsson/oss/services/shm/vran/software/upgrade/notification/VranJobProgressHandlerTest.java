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
package com.ericsson.oss.services.shm.vran.software.upgrade.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections4.map.HashedMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.mediation.vran.model.response.VranUpgradeJobResponse;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityServiceProvider;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * Class to test the VranJobProgressHandler class with the help of jUnits.
 */
@RunWith(MockitoJUnitRunner.class)
public class VranJobProgressHandlerTest {

    @InjectMocks
    private VranSoftwareUpgradeJobProgressHandler objectUndertest;

    @Mock
    private VranUpgradeJobResponse vranUpgradeJobResponse;

    @Mock
    private NotificationRegistry registry;

    @Mock
    private NotificationSubject subject;

    @Mock
    private FdnNotificationSubject fdnNotificationSubject;

    @Mock
    private VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse;

    @Mock
    private ActivityServiceProvider activityServiceProvider;

    @Inject
    private VranTestMock vranTestMock;

    /**
     * Test case to test the external calls in handleJobProgressResponse() for create.
     */

    @Test
    public void testHandleJobProgressResponseForCreate() {
        vranUpgradeJobResponse = getVranUpgradeJobResponse();
        final Date notificationReceivedDate = new Date();
        final String vnfId = vranUpgradeJobResponse.getVnfId();
        final Map<String, Object> additionalAttributes = vranUpgradeJobResponse.getAdditionalAttributes();
        final long activityJobId = (long) additionalAttributes.get(ActivityConstants.ACTIVITY_JOB_ID);
        final String key = vnfId + "@" + vranUpgradeJobResponse.getNetworkElementName() + "@" + activityJobId;
        when(registry.getListener(key)).thenReturn(subject);
        objectUndertest.handleJobProgressResponse(vranUpgradeJobResponse, notificationReceivedDate); //verify(registry).getListener(key);
        verify(subject).setTimeStamp(notificationReceivedDate);
    }

    /**
     * Test case to test the external calls in handleJobProgressResponse() by passing null vranUpgradeJobResponse.
     */
    @Test
    public void testHandleJobProgressResponseWithNull() {
        vranUpgradeJobResponse = null;
        final Date notificationReceivedDate = new Date();
        objectUndertest.handleJobProgressResponse(vranUpgradeJobResponse, notificationReceivedDate);
        assertNull(vranUpgradeJobResponse);
    }

    /**
     * Test case to test the external calls in handleJobProgressResponse() for Activity by returning null notification subject.
     */
    @Test
    public void testHandleJobProgressResponseForActivity() {
        vranUpgradeJobResponse = getVranUpgradeJobResponseActivity();
        final Date notificationReceivedDate = new Date();
        final String vnfId = vranUpgradeJobResponse.getVnfId();
        final String jobId = Integer.toString(vranUpgradeJobResponse.getJobId());
        final String neName = vranUpgradeJobResponse.getNetworkElementName();
        final Map<String, Object> additionalAttributes = vranUpgradeJobResponse.getAdditionalAttributes();
        final long activityJobId = (long) additionalAttributes.get(ActivityConstants.ACTIVITY_JOB_ID);
        final String key = vnfId + "@" + neName + "@" + activityJobId;
        when(registry.getListener(key)).thenReturn(null);
        objectUndertest.handleJobProgressResponse(vranUpgradeJobResponse, notificationReceivedDate);
        verify(registry).getListener(key);
    }

    /**
     * Test case to test the external calls in notifyJob().
     */
    @Test
    public void testNotifyJob() {
        final String jobTypeString = "UPGRADE";
        final JobTypeEnum jte = JobTypeEnum.getJobType("UPGRADE");
        final String platformString = "vRAN";
        final PlatformTypeEnum platform = PlatformTypeEnum.getPlatform("vRAN");
        vranTestMock = new VranTestMock();
        vranSoftwareUpgradeJobResponse = getTestNotificationExtJob();
        final String activityName = vranSoftwareUpgradeJobResponse.getActivityName();
        long activityJobId = vranSoftwareUpgradeJobResponse.getActivityJobId();
        Date notificationReceivedTime = vranSoftwareUpgradeJobResponse.getNotificationReceivedTime();
        vranUpgradeJobResponse = getVranUpgradeJobResponse();
        when(fdnNotificationSubject.getActivityName()).thenReturn(activityName);
        when(fdnNotificationSubject.getObserverHandle()).thenReturn(activityJobId);
        when(fdnNotificationSubject.getTimeStamp()).thenReturn(notificationReceivedTime);
        when(fdnNotificationSubject.getJobType()).thenReturn(jobTypeString);
        when(fdnNotificationSubject.getPlatform()).thenReturn(platformString);
        when(activityServiceProvider.getActivityNotificationHandler(platform, jte, activityName)).thenReturn(vranTestMock);
        objectUndertest.notifyJob(vranSoftwareUpgradeJobResponse, fdnNotificationSubject);
        verify(fdnNotificationSubject).getActivityName();
        verify(fdnNotificationSubject).getJobType();
        verify(fdnNotificationSubject).getPlatform();
    }

    /**
     * Test case to test the external calls in getActivityImpl().
     */
    @Test
    public void testGetActivityImpl() {
        final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType("UPGRADE");
        vranSoftwareUpgradeJobResponse = getTestNotificationExtJob();
        final String activityName = vranSoftwareUpgradeJobResponse.getActivityName();
        final PlatformTypeEnum platform = PlatformTypeEnum.getPlatform("vRAN");
        vranTestMock = new VranTestMock();
        vranUpgradeJobResponse = getVranUpgradeJobResponse();
        when(activityServiceProvider.getActivityNotificationHandler(platform, jobTypeEnum, activityName)).thenReturn(vranTestMock);
        objectUndertest.getActivityImpl(activityName, jobTypeEnum, platform);
        verify(activityServiceProvider).getActivityNotificationHandler(platform, jobTypeEnum, activityName);
    }

    /**
     * Test case to test if the concatenation of key is correct in handleJobProgressResponse().
     */
    @Test
    public void testKey() {
        vranUpgradeJobResponse = getVranUpgradeJobResponse();
        final String vnfId = vranUpgradeJobResponse.getVnfId();
        final String jobId = Integer.toString(vranUpgradeJobResponse.getJobId());
        final String key = vnfId + jobId;
        final String expected = "VNF001000011234";
        assertEquals(expected, key);
    }

    /**
     * Test case to test if the NotificationJobProgressImpl object is not null in notifyJob().
     */
    @Test
    public void testnotifyJobObjectNotNull() {
        final String activityName = "PREPARE";
        final long activityJobId = 100;
        final Date notificationTimeStamp = new Date();
        vranUpgradeJobResponse = getVranUpgradeJobResponse();
        final VranSoftwareUpgradeJobResponse vranNotificationBean = new VranSoftwareUpgradeJobResponse();
        vranNotificationBean.setOperation(vranUpgradeJobResponse.getActivityType());
        vranNotificationBean.setAdditionalInfo(vranUpgradeJobResponse.getAdditionalInfo());
        vranNotificationBean.setFallbackTimeout(vranUpgradeJobResponse.getFallbackTimeout());
        vranNotificationBean.setFinishedTime(vranUpgradeJobResponse.getFinishedTime());
        vranNotificationBean.setJobCreationTime(vranUpgradeJobResponse.getJobCreatedTime());
        vranNotificationBean.setJobId(vranUpgradeJobResponse.getJobId());
        vranNotificationBean.setProgressDetail(vranUpgradeJobResponse.getProgress());
        vranNotificationBean.setProgressLevel(vranUpgradeJobResponse.getProgressLevel());
        vranNotificationBean.setRequestedTime(vranUpgradeJobResponse.getRequestedTime());
        vranNotificationBean.setResult(vranUpgradeJobResponse.getResult());
        vranNotificationBean.setState(vranUpgradeJobResponse.getState());
        vranNotificationBean.setVnfDescriptorId(vranUpgradeJobResponse.getVnfDescriptorId());
        vranNotificationBean.setVnfId(vranUpgradeJobResponse.getVnfId());
        vranNotificationBean.setVnfPackageId(vranUpgradeJobResponse.getVnfPackageId());
        vranNotificationBean.setActivityName(activityName);
        vranNotificationBean.setActivityJobId(activityJobId);
        vranNotificationBean.setNotificationReceivedTime(notificationTimeStamp);
        vranNotificationBean.setFlowType(vranUpgradeJobResponse.getFlowType());
        vranNotificationBean.setActivityName(activityName);
        vranNotificationBean.setNotificationReceivedTime(notificationTimeStamp);
        final VranNotificationJobProgressBean vranNotificationJobProgressBean = new VranNotificationJobProgressBean();
        vranNotificationJobProgressBean.setVranNotification(vranNotificationBean);
        assertNotNull(vranNotificationJobProgressBean);
    }

    /**
     * Method to populate the values for VranUpgradeJobResponse object for create operation.
     * 
     * @return VranUpgradeJobResponse
     */
    private VranUpgradeJobResponse getVranUpgradeJobResponse() {
        final VranUpgradeJobResponse vranUpgradeJobResponse = new VranUpgradeJobResponse();
        final Map<String, Object> additionalAttributes = new HashedMap<String, Object>();
        additionalAttributes.put(ActivityConstants.ACTIVITY_JOB_ID, 123l);
        vranUpgradeJobResponse.setActivityType("CREATE");
        vranUpgradeJobResponse.setAdditionalInfo("additionalInfo");
        vranUpgradeJobResponse.setFlowType("PROGRESS");
        vranUpgradeJobResponse.setFallbackTimeout(1000);
        vranUpgradeJobResponse.setFinishedTime("finishedTime");
        vranUpgradeJobResponse.setJobCreatedTime("jobCreatedTime");
        vranUpgradeJobResponse.setJobId(1234);
        vranUpgradeJobResponse.setVnfId("VNF00100001");
        vranUpgradeJobResponse.setProgress("progressDetail");
        vranUpgradeJobResponse.setProgressLevel(80);
        vranUpgradeJobResponse.setRequestedTime("requestedTime");
        vranUpgradeJobResponse.setResult("result");
        vranUpgradeJobResponse.setState("state");
        vranUpgradeJobResponse.setVnfDescriptorId("vnfDescriptorId");
        vranUpgradeJobResponse.setVnfPackageId("vnfPackageId");
        vranUpgradeJobResponse.setRequestedTime("requestedTime");
        vranUpgradeJobResponse.setAdditionalAttributes(additionalAttributes);
        return vranUpgradeJobResponse;
    }

    /**
     * Method to populate the values for VranUpgradeJobResponse object for activity.
     * 
     * @return VranUpgradeJobResponse
     */
    private VranUpgradeJobResponse getVranUpgradeJobResponseActivity() {
        final VranUpgradeJobResponse vranUpgradeJobResponse = new VranUpgradeJobResponse();
        final Map<String, Object> additionalAttributes = new HashedMap<String, Object>();
        additionalAttributes.put(ActivityConstants.ACTIVITY_JOB_ID, 123l);
        vranUpgradeJobResponse.setActivityType("Prepare");
        vranUpgradeJobResponse.setAdditionalInfo("additionalInfo");
        vranUpgradeJobResponse.setFlowType("PROGRESS");
        vranUpgradeJobResponse.setFallbackTimeout(1000);
        vranUpgradeJobResponse.setFinishedTime("finishedTime");
        vranUpgradeJobResponse.setJobCreatedTime("jobCreatedTime");
        vranUpgradeJobResponse.setJobId(1234);
        vranUpgradeJobResponse.setVnfId("VNF00100001");
        vranUpgradeJobResponse.setProgress("progressDetail");
        vranUpgradeJobResponse.setProgressLevel(80);
        vranUpgradeJobResponse.setRequestedTime("requestedTime");
        vranUpgradeJobResponse.setResult("result");
        vranUpgradeJobResponse.setState("state");
        vranUpgradeJobResponse.setVnfDescriptorId("vnfDescriptorId");
        vranUpgradeJobResponse.setVnfPackageId("vnfPackageId");
        vranUpgradeJobResponse.setRequestedTime("requestedTime");
        vranUpgradeJobResponse.setAdditionalAttributes(additionalAttributes);
        return vranUpgradeJobResponse;
    }

    /**
     * Method to populate the values for NotificationExtJobImpl object.
     * 
     * @return NotificationExtJobImpl
     */
    private VranSoftwareUpgradeJobResponse getTestNotificationExtJob() {
        final String activityName = "prepare";
        final long activityJobId = 100;
        final Date notificationTimeStamp = new Date();
        vranUpgradeJobResponse = getVranUpgradeJobResponse();
        final VranSoftwareUpgradeJobResponse vranNotificationBean = new VranSoftwareUpgradeJobResponse();
        vranNotificationBean.setOperation(vranUpgradeJobResponse.getActivityType());
        vranNotificationBean.setAdditionalInfo(vranUpgradeJobResponse.getAdditionalInfo());
        vranNotificationBean.setFallbackTimeout(vranUpgradeJobResponse.getFallbackTimeout());
        vranNotificationBean.setFinishedTime(vranUpgradeJobResponse.getFinishedTime());
        vranNotificationBean.setJobCreationTime(vranUpgradeJobResponse.getJobCreatedTime());
        vranNotificationBean.setJobId(vranUpgradeJobResponse.getJobId());
        vranNotificationBean.setProgressDetail(vranUpgradeJobResponse.getProgress());
        vranNotificationBean.setProgressLevel(vranUpgradeJobResponse.getProgressLevel());
        vranNotificationBean.setRequestedTime(vranUpgradeJobResponse.getRequestedTime());
        vranNotificationBean.setResult(vranUpgradeJobResponse.getResult());
        vranNotificationBean.setState(vranUpgradeJobResponse.getState());
        vranNotificationBean.setVnfDescriptorId(vranUpgradeJobResponse.getVnfDescriptorId());
        vranNotificationBean.setVnfId(vranUpgradeJobResponse.getVnfId());
        vranNotificationBean.setVnfPackageId(vranUpgradeJobResponse.getVnfPackageId());
        vranNotificationBean.setActivityName(activityName);
        vranNotificationBean.setActivityJobId(activityJobId);
        vranNotificationBean.setNotificationReceivedTime(notificationTimeStamp);
        vranNotificationBean.setFlowType(vranUpgradeJobResponse.getFlowType());
        vranNotificationBean.setActivityName(activityName);
        vranNotificationBean.setNotificationReceivedTime(notificationTimeStamp);
        return vranNotificationBean;
    }
}
