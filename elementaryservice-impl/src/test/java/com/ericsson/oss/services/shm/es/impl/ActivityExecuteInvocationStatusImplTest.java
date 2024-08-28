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
package com.ericsson.oss.services.shm.es.impl;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class ActivityExecuteInvocationStatusImplTest {

    @InjectMocks
    private ActivityExecuteInvocationStatusImpl activityExecuteInvocationStatusImpl;

    @Mock
    private JobConfigurationService jobConfigServiceMock;

    @Mock
    private JobUpdateService jobUpdateServiceMock;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobEnvironment jobenvironment;

    @Test
    public void testActivityIsNotExecuted() {
        final long activityJobId = 1L;

        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobPropertyMap = new HashMap<String, Object>();
        jobPropertyMap.put(ShmConstants.KEY, ShmConstants.IS_EXECUTED);
        jobPropertyMap.put(ShmConstants.VALUE, Boolean.FALSE.toString());
        activityJobPropertyList.add(jobPropertyMap);

        Map<String, Object> jobAttributeMap = Mockito.mock(Map.class);

        when(jobConfigServiceMock.retrieveJob(activityJobId)).thenReturn(jobAttributeMap);
        when(jobAttributeMap.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(jobAttributeMap.get(ActivityConstants.ACTIVITY_NAME)).thenReturn("activity");

        final boolean isExecuted = activityExecuteInvocationStatusImpl.isActivityExecuted(activityJobId);
        Assert.assertFalse(isExecuted);
    }

    @Test
    public void testActivityIsExecuted() {
        final long activityJobId = 1L;

        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobPropertyMap = new HashMap<String, Object>();
        jobPropertyMap.put(ShmConstants.KEY, ShmConstants.IS_EXECUTED);
        jobPropertyMap.put(ShmConstants.VALUE, Boolean.TRUE.toString());
        activityJobPropertyList.add(jobPropertyMap);
        Map<String, Object> jobAttributeMap = Mockito.mock(Map.class);

        when(jobUpdateServiceMock.retrieveJobWithRetry(activityJobId)).thenReturn(jobAttributeMap);
        when(jobAttributeMap.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        when(jobAttributeMap.get(ActivityConstants.ACTIVITY_NAME)).thenReturn("activity");

        boolean isExecuted = activityExecuteInvocationStatusImpl.isActivityExecuted(activityJobId);
        Assert.assertTrue(isExecuted);
    }

    @Test
    public void testWhenJobPropertiesAreEmpty() {
        final long activityJobId = 1L;

        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        Map<String, Object> jobAttributeMap = Mockito.mock(Map.class);

        when(jobConfigServiceMock.retrieveJob(activityJobId)).thenReturn(jobAttributeMap);
        when(jobAttributeMap.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);

        boolean isExecuted = activityExecuteInvocationStatusImpl.isActivityExecuted(activityJobId);
        Assert.assertFalse(isExecuted);
    }

    @Test
    public void testWhenJobPropertiesListIsNull() {
        final long activityJobId = 1L;

        Map<String, Object> jobAttributeMap = Mockito.mock(Map.class);

        when(jobConfigServiceMock.retrieveJob(activityJobId)).thenReturn(jobAttributeMap);
        when(jobAttributeMap.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(null);

        boolean isExecuted = activityExecuteInvocationStatusImpl.isActivityExecuted(activityJobId);
        Assert.assertFalse(isExecuted);
    }

    @Test
    public void testWhenRequriedJobPropertyIsNotFound() {
        final long activityJobId = 1L;

        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobPropertyMap = new HashMap<String, Object>();
        jobPropertyMap.put(ShmConstants.KEY, "DummyKey");
        jobPropertyMap.put(ShmConstants.VALUE, Boolean.FALSE.toString());
        activityJobPropertyList.add(jobPropertyMap);
        Map<String, Object> jobAttributeMap = Mockito.mock(Map.class);

        when(jobConfigServiceMock.retrieveJob(activityJobId)).thenReturn(jobAttributeMap);
        when(jobAttributeMap.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);

        boolean isExecuted = activityExecuteInvocationStatusImpl.isActivityExecuted(activityJobId);
        Assert.assertFalse(isExecuted);
    }

    @Test
    public void testUpdateIsExecutedActivityJobParameter() {

        final long activityJobId = 1L;

        Map<String, Object> jobAttributeMap = Mockito.mock(Map.class);
        //Mockito.doNothing().when(jobUpdateServiceMock).addOrUpdateOrRemoveJobProperties(Mockito.anyLong(), Mockito.anyMap(), Mockito.anyList());
        // activityExecuteInvocationStatusImpl.updateActivityExecuteStatus(activityJobId);
        //    Mockito.verify(jobUpdateServiceMock, Mockito.atLeastOnce()).readAndUpdateRunningJobAttributes(Mockito.anyLong(), Mockito.anyList(), Mockito.anyList());
    }

    @Test
    public void testupdateActivityExecuteStatus() {
        final long activityJobId = 1L;
        Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
        mainJobAttributes.put("templateJobId",activityJobId);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobenvironment);
        when(jobenvironment.getMainJobAttributes()).thenReturn(mainJobAttributes);
        activityExecuteInvocationStatusImpl.updateActivityExecuteStatus(activityJobId);
        Mockito.verify(jobUpdateServiceMock, Mockito.atLeastOnce()).readAndUpdateRunningJobAttributes(Mockito.anyLong(), Mockito.anyList(), Mockito.anyList());
    }
}
