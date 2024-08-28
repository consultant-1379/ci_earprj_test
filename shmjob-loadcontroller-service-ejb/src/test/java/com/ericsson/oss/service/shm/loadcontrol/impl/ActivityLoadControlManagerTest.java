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
package com.ericsson.oss.service.shm.loadcontrol.impl;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.es.api.SHMStagedActivityRequest;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.loadcontrol.impl.ActivityLoadControlManager;
import com.ericsson.oss.services.shm.loadcontrol.impl.ConfigurationParamProvider;
import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControlWorkflowNotifier;
import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControllerPersistenceManager;
import com.ericsson.oss.services.shm.loadcontrol.local.api.SHMLoadControllerLocalService;
import com.ericsson.oss.services.shm.loadcontrol.local.api.StagedActivityRequestBean;

@RunWith(MockitoJUnitRunner.class)
public class ActivityLoadControlManagerTest {

    @Mock
    private SHMLoadControllerLocalService shmLoadControllerLocalService;

    @Mock
    private LoadControlWorkflowNotifier loadControlWorkflowNotifier;

    @Mock
    LoadControllerPersistenceManager loadControllerPersistenceManager;

    @Mock
    private ConfigurationParamProvider maxCountProvider;

    @InjectMocks
    private ActivityLoadControlManager objectUnderTest;

    private static final String WORKFLOW_INSTANCE_ID = "123456789";
    private static final String BUSINESS_KEY = "1234567@LTE02ERBS0001";
    private static final String JOBTYPE = "BACKUP";
    private static final String PLATFORMT_TYPE = "CPP";
    private static final String ACTIVITY_NAME = "exprtcv";
    private static final long ACTIVITY_JOB_ID = 1223l;

    @SuppressWarnings("unchecked")
    @Test
    public void testcheckAllowanceWhenActivityAllowed() {
        final List<SHMStagedActivityRequest> shmStagedActivityRequestList = new ArrayList<>();
        final StagedActivityRequestBean stagedActivityRequest = new StagedActivityRequestBean();
        final SHMStagedActivityRequest shmStagedActivityRequest = setUp();
        shmStagedActivityRequestList.add(shmStagedActivityRequest);
        stagedActivityRequest.setShmStagedActivityRequest(shmStagedActivityRequestList);
        when(maxCountProvider.getLoadControllerCorrelationFailureRetryCount()).thenReturn(3);
        when(shmLoadControllerLocalService.incrementCounter(Matchers.any(SHMActivityRequest.class))).thenReturn(true);
        objectUnderTest.checkActivityAllowance(stagedActivityRequest);
        verify(shmLoadControllerLocalService, times(1)).incrementCounter(Matchers.any(SHMActivityRequest.class));
        verify(loadControlWorkflowNotifier, times(1)).correlate(shmStagedActivityRequest.getBusinessKey(), shmStagedActivityRequest.getWorkflowInstanceId());
        verify(loadControllerPersistenceManager, times(1)).createPO(Matchers.anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testcheckAllowanceWhenActivityNotAllowed() {
        final List<SHMStagedActivityRequest> shmStagedActivityRequestList = new ArrayList<>();
        final StagedActivityRequestBean stagedActivityRequest = new StagedActivityRequestBean();
        final SHMStagedActivityRequest shmStagedActivityRequest = setUp();
        shmStagedActivityRequestList.add(shmStagedActivityRequest);
        stagedActivityRequest.setShmStagedActivityRequest(shmStagedActivityRequestList);
        when(maxCountProvider.getLoadControllerCorrelationFailureRetryCount()).thenReturn(3);
        when(shmLoadControllerLocalService.incrementCounter(Matchers.any(SHMActivityRequest.class))).thenReturn(false);
        objectUnderTest.checkActivityAllowance(stagedActivityRequest);
        verify(shmLoadControllerLocalService, times(1)).incrementCounter(Matchers.any(SHMActivityRequest.class));
        verify(loadControlWorkflowNotifier, times(0)).correlate(shmStagedActivityRequest.getBusinessKey(), shmStagedActivityRequest.getWorkflowInstanceId());
        verify(loadControllerPersistenceManager, times(1)).createPO(Matchers.anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testcheckAllowanceWhenActivityAllowedAndCorrelationFails() {
        final List<SHMStagedActivityRequest> shmStagedActivityRequestList = new ArrayList<>();
        final StagedActivityRequestBean stagedActivityRequest = new StagedActivityRequestBean();
        final SHMStagedActivityRequest shmStagedActivityRequest = setUp();
        shmStagedActivityRequestList.add(shmStagedActivityRequest);
        stagedActivityRequest.setShmStagedActivityRequest(shmStagedActivityRequestList);
        when(maxCountProvider.getLoadControllerCorrelationFailureRetryCount()).thenReturn(3);
        when(shmLoadControllerLocalService.incrementCounter(Matchers.any(SHMActivityRequest.class))).thenReturn(true);
        when(loadControlWorkflowNotifier.correlate(Matchers.anyString(), Matchers.anyString())).thenReturn(false);
        objectUnderTest.checkActivityAllowance(stagedActivityRequest);
        verify(shmLoadControllerLocalService, times(1)).incrementCounter(Matchers.any(SHMActivityRequest.class));
        verify(shmLoadControllerLocalService, times(1)).decrementCounter(Matchers.any(SHMActivityRequest.class));
        verify(loadControllerPersistenceManager, times(1)).createPO(Matchers.anyMap());
    }

    private SHMStagedActivityRequest setUp() {
        final Map<String, Object> stagedActivityPOMap = new HashMap<>();
        stagedActivityPOMap.put(ShmConstants.ACTIVITYNAME, ACTIVITY_NAME);
        stagedActivityPOMap.put(ShmConstants.BUSINESS_KEY, BUSINESS_KEY);
        stagedActivityPOMap.put(ShmConstants.JOB_TYPE, JOBTYPE);
        stagedActivityPOMap.put(ShmConstants.PLATFORM, PLATFORMT_TYPE);
        stagedActivityPOMap.put(ShmConstants.WORKFLOW_INSTANCE_ID, WORKFLOW_INSTANCE_ID);
        stagedActivityPOMap.put(ShmConstants.RETRY_COUNT, 0);
        stagedActivityPOMap.put(ShmConstants.ACTIVITY_JOB_ID, ACTIVITY_JOB_ID);
        final SHMStagedActivityRequest shmStagedActivityRequest = new SHMStagedActivityRequest(stagedActivityPOMap);
        shmStagedActivityRequest.setStagedActivityPoId(123456l);
        return shmStagedActivityRequest;

    }
}
