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
package com.ericsson.oss.services.shm.webpush.utils;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
//import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobLogResponse;
import com.ericsson.oss.services.shm.webpush.api.JobWebPushEvent;
import com.ericsson.oss.services.shm.webpush.api.WebPushConstants;

@RunWith(MockitoJUnitRunner.class)
public class WebPushServiceUtilImplTest {

    @InjectMocks
    private WebPushServiceUtil webPushServiceUtilImpl;

    @Mock
    private JobConfigurationService jobConfigurationServiceMock;

    @Mock
    private DpsDataChangedEvent DpsDataChangedEventMock;

    @Mock
    private JobLogResponse jobLogResponse;

    @Mock
    private Map<String, Object> mapMock;

    @Mock
    @Inject
    private Event<JobWebPushEvent> eventSender;

    @Mock
    private JobWebPushEvent jobWebPushEvent;

    @Mock
    private JobUpdateService jobUpdateService;

    long mainJobId = 1L;
    long neJobId = 2L;
    long activityJobId = 3L;
    String jobState = "RUNNING";

    @Test
    public void testPrepareAndPushMainJob() {
        when(jobUpdateService.retrieveJobWithRetry(mainJobId)).thenReturn(mapMock);
        when((String) mapMock.get(ShmConstants.STATE)).thenReturn(jobState);
        when(jobConfigurationServiceMock.getJobCategory(mainJobId)).thenReturn("UI");
        webPushServiceUtilImpl.prepareAndPushMainJob(mainJobId);
        verify(eventSender, times(2)).fire((JobWebPushEvent) Matchers.anyObject());
    }

    @Test
    public void testPrepareAndPushNeJob() {
        Mockito.doNothing().when(eventSender).fire(jobWebPushEvent);
        setupForJobDetails();
        webPushServiceUtilImpl.prepareAndPushNeJob(neJobId);
        verify(eventSender, times(1)).fire((JobWebPushEvent) Matchers.anyObject());
    }

    @Test
    public void testPrepareAndPushActivityJob() {
        final Set<AttributeChangeData> attributeChangeDataSet = new HashSet<AttributeChangeData>();
        Mockito.doNothing().when(eventSender).fire(jobWebPushEvent);
        setupForJobDetails();
        webPushServiceUtilImpl.prepareAndPushActivityJob(activityJobId, attributeChangeDataSet);
        verify(eventSender, times(1)).fire((JobWebPushEvent) Matchers.anyObject());
    }

    @Test
    public void testPrepareAndPushActivityJobwithLogs() {
        final Set<AttributeChangeData> attributeChangeDataSet = new HashSet<AttributeChangeData>();
        final List<Map<String, Object>> newValeList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> newValeMap = new HashMap<String, Object>();
        newValeMap.put("message", "Set as Startable CV activity is initiated");
        newValeMap.put("entryTime", new Date());
        newValeList.add(newValeMap);
        final Map<String, Object> ValeMap = new HashMap<String, Object>();
        ValeMap.put("new value", newValeList);
        final AttributeChangeData attributeChangeData = new AttributeChangeData("log", ValeMap, newValeList, "deltaRemoved", "deltaAdded");
        attributeChangeDataSet.add(attributeChangeData);
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.NE_JOB_ID)).thenReturn(neJobId);
        when(mapMock.get(ShmConstants.ACTIVITY_NAME)).thenReturn("install");
        when(mapMock.get(ShmConstants.NE_NAME)).thenReturn("LTEERBS2001");
        Mockito.doNothing().when(eventSender).fire(jobWebPushEvent);
        setupForJobDetails();
        webPushServiceUtilImpl.prepareAndPushActivityJob(activityJobId, attributeChangeDataSet);
        verify(eventSender, times(1)).fire((JobWebPushEvent) Matchers.anyObject());
    }

    @Test
    public void testPrepareAndPushCreateJobEventSuccess() {
        final String createJobIdAsString = Long.toString(mainJobId);
        final Map<String, Object> craeteJobAttributes = new HashMap<String, Object>();
        craeteJobAttributes.put(WebPushConstants.JOB_ID, createJobIdAsString);
        craeteJobAttributes.put(WebPushConstants.JOB_EVENT, WebPushConstants.CREATE_JOB);
        Mockito.doNothing().when(eventSender).fire(jobWebPushEvent);
        when(jobConfigurationServiceMock.getJobCategory(mainJobId)).thenReturn("UI");
        webPushServiceUtilImpl.prepareAndPushCreateJobEvent(mainJobId);
        verify(eventSender, times(1)).fire((JobWebPushEvent) Matchers.anyObject());
    }

    private void setupForJobDetails() {
        Map<String, Object> activityJobAttr = new HashMap<>();
        activityJobAttr.put(ShmConstants.NE_JOB_ID, neJobId);
        Map<String, Object> neJobAttr = new HashMap<>();
        neJobAttr.put("mainJobId", mainJobId);
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(activityJobAttr);
        when(jobUpdateService.retrieveJobWithRetry(neJobId)).thenReturn(neJobAttr);
        when(jobConfigurationServiceMock.getJobCategory(mainJobId)).thenReturn("UI");
    }

    @Test
    public void testPrepareAndPushCreateNeJobEvent() {
        Mockito.doNothing().when(eventSender).fire(jobWebPushEvent);
        setupForJobDetails();
        webPushServiceUtilImpl.prepareAndPushCreateNeJobEvent(neJobId);
        verify(eventSender, times(1)).fire((JobWebPushEvent) Matchers.anyObject());
    }

    @Test
    public void prepareAndPushCreateJobEvent() {
        Mockito.doNothing().when(eventSender).fire(jobWebPushEvent);
        setupForJobDetails();
        webPushServiceUtilImpl.prepareAndPushCreateJobEvent(mainJobId);
        verify(eventSender, times(1)).fire((JobWebPushEvent) Matchers.anyObject());
    }
}
