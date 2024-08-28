/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.activity.timeout.models.NodeHealthCheckJobActivityTimeouts;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.fa.api.FaBuildingBlockResponseProvider;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.HealthStatus;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class FaBuildingBlockResponseProcessorTest {

    @InjectMocks
    private FaBuildingBlockResponseProcessor blockResponseProcessor;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private NeJobStaticDataProvider jobStaticDataProvider;

    @Mock
    private FaBuildingBlockResponseProvider blockResponseProvider;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    private final long activityJobId = 12l;
    private final long activityJobId1 = 13l;

    private final long neJobId = 123l;
    private final String jobType = JobType.NODE_HEALTH_CHECK.getJobTypeName();

    private final String jobResult = "SUCCESS";
    private final long mainJobId = 134l;

    @Test
    public void testSendFaResponse() throws JobDataNotFoundException {

        final Map<String, Object> activityJobPoAttributes = new HashMap<>();
        List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        when(activityUtils.getCapabilityByJobType(jobType)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);

        final Map<String, String> property = new HashMap<>();
        property.put(ActivityConstants.JOB_PROP_KEY, ShmConstants.JOB_CATEGORY);
        property.put(ActivityConstants.JOB_PROP_VALUE, JobCategory.FA.getAttribute());
        List<Map<String, String>> properties = new ArrayList<>();
        properties.add(property);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, properties);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttributes);
        NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, "nodeName", "neJobBusinessKey", "CPP", 0l, "parentNodeName");
        when(jobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        final Map<String, String> fbbTypeProperty = new HashMap<String, String>();
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FBB_TYPE);
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_VALUE, "createJobFbb");

        final Map<String, String> userNameProperty = new HashMap<String, String>();
        userNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.USERNAME);
        userNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "administrator");

        final Map<String, String> requestIdProperty = new HashMap<String, String>();
        requestIdProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FA_REQUEST_ID);
        requestIdProperty.put(ActivityConstants.JOB_PROP_VALUE, "requestId");

        final Map<String, String> executionNameProperty = new HashMap<String, String>();
        executionNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME);
        executionNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "test-nhc-job");

        jobProperties.add(executionNameProperty);
        jobProperties.add(userNameProperty);
        jobProperties.add(fbbTypeProperty);
        jobProperties.add(requestIdProperty);

        activityJobPoAttributes.put(ShmJobConstants.NE_JOB_ID, neJobId);
        activityJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity completed Successfully");
        activityJobPoAttributes.put(ShmConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        activityJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity completed Successfully");
        activityJobPoAttributes.put(ShmJobConstants.JOBPROPERTIES, jobProperties);
        blockResponseProcessor.sendFaResponse(activityJobId, jobType, jobResult, activityJobPoAttributes);

        verify(blockResponseProvider, times(1)).send(Matchers.any(), Matchers.any());
    }

    @Test
    public void testSendFaResponse_noLastLogMessage() throws JobDataNotFoundException {

        final Map<String, Object> activityJobPoAttributes = new HashMap<>();
        List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        when(activityUtils.getCapabilityByJobType(jobType)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);

        final Map<String, String> property = new HashMap<>();
        property.put(ActivityConstants.JOB_PROP_KEY, ShmConstants.JOB_CATEGORY);
        property.put(ActivityConstants.JOB_PROP_VALUE, JobCategory.FA.getAttribute());
        List<Map<String, String>> properties = new ArrayList<>();
        properties.add(property);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, properties);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttributes);
        NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, "nodeName", "neJobBusinessKey", "CPP", 0l, "parentNodeName");
        when(jobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        final Map<String, String> fbbTypeProperty = new HashMap<String, String>();
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FBB_TYPE);
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_VALUE, "createJobFbb");

        final Map<String, String> userNameProperty = new HashMap<String, String>();
        userNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.USERNAME);
        userNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "administrator");

        final Map<String, String> requestIdProperty = new HashMap<String, String>();
        requestIdProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FA_REQUEST_ID);
        requestIdProperty.put(ActivityConstants.JOB_PROP_VALUE, "requestId");

        final Map<String, String> executionNameProperty = new HashMap<String, String>();
        executionNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME);
        executionNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "test-nhc-job");

        jobProperties.add(executionNameProperty);
        jobProperties.add(userNameProperty);
        jobProperties.add(fbbTypeProperty);
        jobProperties.add(requestIdProperty);

        activityJobPoAttributes.put(ShmJobConstants.NE_JOB_ID, neJobId);
        activityJobPoAttributes.put(ShmConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        // activityJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity completed Successfully");
        activityJobPoAttributes.put(ShmJobConstants.JOBPROPERTIES, jobProperties);
        blockResponseProcessor.sendFaResponse(activityJobId, jobType, jobResult, activityJobPoAttributes);

        verify(blockResponseProvider, times(1)).send(Matchers.any(), Matchers.any());
    }

    @Test
    public void testSendNHCResponse() throws JobDataNotFoundException {

        final Map<String, Object> activityJobPoAttributes = new HashMap<>();
        List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        when(activityUtils.getCapabilityByJobType(jobType)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);

        final Map<String, String> property = new HashMap<>();
        property.put(ActivityConstants.JOB_PROP_KEY, ShmConstants.JOB_CATEGORY);
        property.put(ActivityConstants.JOB_PROP_VALUE, JobCategory.NHC_FA.getAttribute());
        List<Map<String, String>> properties = new ArrayList<>();
        properties.add(property);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, properties);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttributes);

        final Map<String, Object> neJobPoAttributes = new HashMap<>();
        neJobPoAttributes.put(ShmConstants.NEJOB_HEALTH_STATUS, "HEALTHY");
        neJobPoAttributes.put(ShmJobConstants.RESULT, "SUCCESS");
        neJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity Completed Successfully||INFO");
        neJobPoAttributes.put(ShmConstants.NE_NAME, "neName");

        final Map<String, Object> restrictions = new HashMap<>();
        // restrictions.put(ShmJobConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        restrictions.put(ShmJobConstants.NE_JOB_ID, neJobId);

        NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, "neName", "neJobBusinessKey", "CPP", 0l, "parentNodeName");
        when(jobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        final Map<String, String> fbbTypeProperty = new HashMap<String, String>();
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FBB_TYPE);
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_VALUE, "createJobFbb");

        final Map<String, String> userNameProperty = new HashMap<String, String>();
        userNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.USERNAME);
        userNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "administrator");

        final Map<String, String> requestIdProperty = new HashMap<String, String>();
        requestIdProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FA_REQUEST_ID);
        requestIdProperty.put(ActivityConstants.JOB_PROP_VALUE, "requestId");

        final Map<String, String> executionNameProperty = new HashMap<String, String>();
        executionNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME);
        executionNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "test-nhc-job");

        jobProperties.add(executionNameProperty);
        jobProperties.add(userNameProperty);
        jobProperties.add(fbbTypeProperty);
        jobProperties.add(requestIdProperty);

        activityJobPoAttributes.put(ShmJobConstants.NE_JOB_ID, neJobId);
        //activityJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity completed Successfully");
        activityJobPoAttributes.put(ShmConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        activityJobPoAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        Map<String, Object> poAttributes = new HashMap<>();
        poAttributes.put(ShmConstants.PO_ATTRIBUTES, activityJobPoAttributes);
        poAttributes.put(ShmConstants.PO_ID, activityJobId);

        when(jobConfigurationServiceRetryProxy.getActivityJobAttributesByNeJobId(neJobId, restrictions)).thenReturn(Arrays.asList(poAttributes));
        blockResponseProcessor.sendNhcFaResponse(neJobId, mainJobId, JobCategory.NHC_FA.getAttribute(), neJobPoAttributes, mainJobAttributes);

        verify(blockResponseProvider, times(1)).send(Matchers.any(), Matchers.any());

    }

    @Test
    public void testSendNHCResponseForBothNHCAndAHC() throws JobDataNotFoundException {

        final Map<String, Object> activityJobPoAttributes = new HashMap<>();
        final Map<String, Object> activityJobPoAttributes1 = new HashMap<>();
        List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        when(activityUtils.getCapabilityByJobType(jobType)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);

        final Map<String, String> property = new HashMap<>();
        property.put(ActivityConstants.JOB_PROP_KEY, ShmConstants.JOB_CATEGORY);
        property.put(ActivityConstants.JOB_PROP_VALUE, JobCategory.NHC_FA.getAttribute());
        List<Map<String, String>> properties = new ArrayList<>();
        properties.add(property);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, properties);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttributes);

        final Map<String, Object> neJobPoAttributes = new HashMap<>();
        neJobPoAttributes.put(ShmConstants.NEJOB_HEALTH_STATUS, "HEALTHY");
        neJobPoAttributes.put(ShmJobConstants.RESULT, "SUCCESS");
        neJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity Completed Successfully||INFO");
        neJobPoAttributes.put(ShmConstants.NE_NAME, "neName");

        final Map<String, Object> restrictions = new HashMap<>();
        // restrictions.put(ShmJobConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        restrictions.put(ShmJobConstants.NE_JOB_ID, neJobId);

        NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, "neName", "neJobBusinessKey", "CPP", 0l, "parentNodeName");
        when(jobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        final Map<String, String> fbbTypeProperty = new HashMap<String, String>();
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FBB_TYPE);
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_VALUE, "createJobFbb");

        final Map<String, String> userNameProperty = new HashMap<String, String>();
        userNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.USERNAME);
        userNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "administrator");

        final Map<String, String> requestIdProperty = new HashMap<String, String>();
        requestIdProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FA_REQUEST_ID);
        requestIdProperty.put(ActivityConstants.JOB_PROP_VALUE, "requestId");

        final Map<String, String> executionNameProperty = new HashMap<String, String>();
        executionNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME);
        executionNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "test-nhc-job");

        jobProperties.add(executionNameProperty);
        jobProperties.add(userNameProperty);
        jobProperties.add(fbbTypeProperty);
        jobProperties.add(requestIdProperty);

        activityJobPoAttributes.put(ShmJobConstants.NE_JOB_ID, neJobId);
        //activityJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity completed Successfully");
        activityJobPoAttributes.put(ShmConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        activityJobPoAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        activityJobPoAttributes1.put(ShmJobConstants.NE_JOB_ID, neJobId);
        activityJobPoAttributes1.put(ShmConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.ENM_HEALTH_CHECK);
        activityJobPoAttributes1.put(ShmConstants.JOBPROPERTIES, jobProperties);
        Map<String, Object> poAttributes = new HashMap<>();
        poAttributes.put(ShmConstants.PO_ATTRIBUTES, activityJobPoAttributes);
        poAttributes.put(ShmConstants.PO_ID, activityJobId);
        Map<String, Object> poAttributes1 = new HashMap<>();
        poAttributes1.put(ShmConstants.PO_ATTRIBUTES, activityJobPoAttributes1);
        poAttributes1.put(ShmConstants.PO_ID, activityJobId1);

        when(jobConfigurationServiceRetryProxy.getActivityJobAttributesByNeJobId(neJobId, restrictions)).thenReturn(Arrays.asList(poAttributes, poAttributes1));
        blockResponseProcessor.sendNhcFaResponse(neJobId, mainJobId, JobCategory.NHC_FA.getAttribute(), neJobPoAttributes, mainJobAttributes);

        verify(blockResponseProvider, times(1)).send(Matchers.any(), Matchers.any());

    }

    @Test
    public void testSendNHCResponseForBothNHCAndAHCForNotSendingReponseToFACase() throws JobDataNotFoundException {

        final Map<String, Object> activityJobPoAttributes = new HashMap<>();
        final Map<String, Object> activityJobPoAttributes1 = new HashMap<>();
        List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        when(activityUtils.getCapabilityByJobType(jobType)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);

        final Map<String, String> property = new HashMap<>();
        property.put(ActivityConstants.JOB_PROP_KEY, ShmConstants.JOB_CATEGORY);
        property.put(ActivityConstants.JOB_PROP_VALUE, JobCategory.NHC_FA.getAttribute());
        List<Map<String, String>> properties = new ArrayList<>();
        properties.add(property);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, properties);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttributes);

        final Map<String, Object> neJobPoAttributes = new HashMap<>();
        neJobPoAttributes.put(ShmConstants.NEJOB_HEALTH_STATUS, "HEALTHY");
        neJobPoAttributes.put(ShmJobConstants.RESULT, "SUCCESS");
        neJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity Completed Successfully||INFO");
        neJobPoAttributes.put(ShmConstants.NE_NAME, "neName");

        final Map<String, Object> restrictions = new HashMap<>();
        // restrictions.put(ShmJobConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        restrictions.put(ShmJobConstants.NE_JOB_ID, neJobId);

        NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, "neName", "neJobBusinessKey", "CPP", 0l, "parentNodeName");
        when(jobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        final Map<String, String> fbbTypeProperty = new HashMap<String, String>();
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FBB_TYPE);
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_VALUE, "createJobFbb");

        final Map<String, String> userNameProperty = new HashMap<String, String>();
        userNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.USERNAME);
        userNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "administrator");

        final Map<String, String> requestIdProperty = new HashMap<String, String>();
        requestIdProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FA_REQUEST_ID);
        requestIdProperty.put(ActivityConstants.JOB_PROP_VALUE, "requestId");

        final Map<String, String> executionNameProperty = new HashMap<String, String>();
        executionNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME);
        executionNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "test-nhc-job");

        jobProperties.add(executionNameProperty);
        jobProperties.add(userNameProperty);
        jobProperties.add(fbbTypeProperty);
        jobProperties.add(requestIdProperty);

        activityJobPoAttributes.put(ShmJobConstants.NE_JOB_ID, neJobId);
        //activityJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity completed Successfully");
        activityJobPoAttributes.put(ShmConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.ENM_HEALTH_CHECK);
        activityJobPoAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        activityJobPoAttributes1.put(ShmJobConstants.NE_JOB_ID, neJobId);
        activityJobPoAttributes1.put(ShmConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.ENM_HEALTH_CHECK);
        activityJobPoAttributes1.put(ShmConstants.JOBPROPERTIES, jobProperties);
        Map<String, Object> poAttributes = new HashMap<>();
        poAttributes.put(ShmConstants.PO_ATTRIBUTES, activityJobPoAttributes);
        poAttributes.put(ShmConstants.PO_ID, activityJobId);
        Map<String, Object> poAttributes1 = new HashMap<>();
        poAttributes1.put(ShmConstants.PO_ATTRIBUTES, activityJobPoAttributes1);
        poAttributes1.put(ShmConstants.PO_ID, activityJobId1);

        when(jobConfigurationServiceRetryProxy.getActivityJobAttributesByNeJobId(neJobId, restrictions)).thenReturn(Arrays.asList(poAttributes, poAttributes1));
        blockResponseProcessor.sendNhcFaResponse(neJobId, mainJobId, JobCategory.NHC_FA.getAttribute(), neJobPoAttributes, mainJobAttributes);

        verify(blockResponseProvider, times(0)).send(Matchers.any(), Matchers.any());

    }

    @Test
    public void testSendNHCResponse_noLastlogMessage() throws JobDataNotFoundException {

        final Map<String, Object> activityJobPoAttributes = new HashMap<>();
        List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        when(activityUtils.getCapabilityByJobType(jobType)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);

        final Map<String, String> property = new HashMap<>();
        property.put(ActivityConstants.JOB_PROP_KEY, ShmConstants.JOB_CATEGORY);
        property.put(ActivityConstants.JOB_PROP_VALUE, JobCategory.NHC_FA.getAttribute());
        List<Map<String, String>> properties = new ArrayList<>();
        properties.add(property);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, properties);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttributes);

        final Map<String, Object> neJobPoAttributes = new HashMap<>();
        neJobPoAttributes.put(ShmConstants.NEJOB_HEALTH_STATUS, "HEALTHY");
        neJobPoAttributes.put(ShmJobConstants.RESULT, "SKIPPED");
        // neJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity Completed Successfully");
        neJobPoAttributes.put(ShmConstants.NE_NAME, "neName");

        final Map<String, Object> restrictions = new HashMap<>();
        // restrictions.put(ShmJobConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        restrictions.put(ShmJobConstants.NE_JOB_ID, neJobId);

        NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, "neName", "neJobBusinessKey", "CPP", 0l, "parentNodeName");
        when(jobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        final Map<String, String> fbbTypeProperty = new HashMap<String, String>();
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FBB_TYPE);
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_VALUE, "createJobFbb");

        final Map<String, String> userNameProperty = new HashMap<String, String>();
        userNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.USERNAME);
        userNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "administrator");

        final Map<String, String> requestIdProperty = new HashMap<String, String>();
        requestIdProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FA_REQUEST_ID);
        requestIdProperty.put(ActivityConstants.JOB_PROP_VALUE, "requestId");

        final Map<String, String> executionNameProperty = new HashMap<String, String>();
        executionNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME);
        executionNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "test-nhc-job");

        jobProperties.add(executionNameProperty);
        jobProperties.add(userNameProperty);
        jobProperties.add(fbbTypeProperty);
        jobProperties.add(requestIdProperty);

        activityJobPoAttributes.put(ShmJobConstants.NE_JOB_ID, neJobId);
        //activityJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity completed Successfully");
        activityJobPoAttributes.put(ShmConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        activityJobPoAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        Map<String, Object> poAttributes = new HashMap<>();
        poAttributes.put(ShmConstants.PO_ATTRIBUTES, activityJobPoAttributes);
        poAttributes.put(ShmConstants.PO_ID, activityJobId);

        when(jobConfigurationServiceRetryProxy.getActivityJobAttributesByNeJobId(neJobId, restrictions)).thenReturn(Arrays.asList(poAttributes));
        blockResponseProcessor.sendNhcFaResponse(neJobId, mainJobId, JobCategory.NHC_FA.getAttribute(), neJobPoAttributes, mainJobAttributes);

        verify(blockResponseProvider, times(1)).send(Matchers.any(), Matchers.any());

    }

    @Test
    public void testSendNHCResponse_noLastlogMessage_failed() throws JobDataNotFoundException {

        final Map<String, Object> activityJobPoAttributes = new HashMap<>();
        List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        when(activityUtils.getCapabilityByJobType(jobType)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);

        final Map<String, String> property = new HashMap<>();
        property.put(ActivityConstants.JOB_PROP_KEY, ShmConstants.JOB_CATEGORY);
        property.put(ActivityConstants.JOB_PROP_VALUE, JobCategory.NHC_FA.getAttribute());
        List<Map<String, String>> properties = new ArrayList<>();
        properties.add(property);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, properties);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttributes);

        final Map<String, Object> neJobPoAttributes = new HashMap<>();
        neJobPoAttributes.put(ShmConstants.NEJOB_HEALTH_STATUS, "HEALTHY");
        neJobPoAttributes.put(ShmJobConstants.RESULT, "FAILED");
        // neJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity Completed Successfully");
        neJobPoAttributes.put(ShmConstants.NE_NAME, "neName");

        final Map<String, Object> restrictions = new HashMap<>();
        //restrictions.put(ShmJobConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        restrictions.put(ShmJobConstants.NE_JOB_ID, neJobId);

        NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, "neName", "neJobBusinessKey", "CPP", 0l, "parentNodeName");
        when(jobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        final Map<String, String> fbbTypeProperty = new HashMap<String, String>();
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FBB_TYPE);
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_VALUE, "createJobFbb");

        final Map<String, String> userNameProperty = new HashMap<String, String>();
        userNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.USERNAME);
        userNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "administrator");

        final Map<String, String> requestIdProperty = new HashMap<String, String>();
        requestIdProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FA_REQUEST_ID);
        requestIdProperty.put(ActivityConstants.JOB_PROP_VALUE, "requestId");

        final Map<String, String> executionNameProperty = new HashMap<String, String>();
        executionNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME);
        executionNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "test-nhc-job");

        jobProperties.add(executionNameProperty);
        jobProperties.add(userNameProperty);
        jobProperties.add(fbbTypeProperty);
        jobProperties.add(requestIdProperty);

        activityJobPoAttributes.put(ShmJobConstants.NE_JOB_ID, neJobId);
        //activityJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity completed Successfully");
        activityJobPoAttributes.put(ShmConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        activityJobPoAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        Map<String, Object> poAttributes = new HashMap<>();
        poAttributes.put(ShmConstants.PO_ATTRIBUTES, activityJobPoAttributes);
        poAttributes.put(ShmConstants.PO_ID, activityJobId);

        when(jobConfigurationServiceRetryProxy.getActivityJobAttributesByNeJobId(neJobId, restrictions)).thenReturn(Arrays.asList(poAttributes));
        blockResponseProcessor.sendNhcFaResponse(neJobId, mainJobId, JobCategory.NHC_FA.getAttribute(), neJobPoAttributes, mainJobAttributes);

        verify(blockResponseProvider, times(1)).send(Matchers.any(), Matchers.any());

    }

    @Test
    public void testSendNHCResponse_ActivityNotExists() throws JobDataNotFoundException {

        final Map<String, Object> activityJobPoAttributes = new HashMap<>();
        List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, Object> mainJobAttributes = new HashMap<>();
        when(activityUtils.getCapabilityByJobType(jobType)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);

        final Map<String, String> property = new HashMap<>();
        property.put(ActivityConstants.JOB_PROP_KEY, ShmConstants.JOB_CATEGORY);
        property.put(ActivityConstants.JOB_PROP_VALUE, JobCategory.NHC_FA.getAttribute());
        List<Map<String, String>> properties = new ArrayList<>();
        properties.add(property);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, properties);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttributes);

        final Map<String, Object> neJobPoAttributes = new HashMap<>();
        neJobPoAttributes.put(ShmConstants.NEJOB_HEALTH_STATUS, "HEALTHY");
        neJobPoAttributes.put(ShmJobConstants.RESULT, "FAILED");
        // neJobPoAttributes.put(ShmConstants.LAST_LOG_MESSAGE, "Activity Completed Successfully");
        neJobPoAttributes.put(ShmConstants.NE_NAME, "neName");

        final Map<String, Object> restrictions = new HashMap<>();
        //restrictions.put(ShmJobConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        restrictions.put(ShmJobConstants.NE_JOB_ID, neJobId);

        NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, "neName", "neJobBusinessKey", "CPP", 0l, "parentNodeName");
        when(jobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        final Map<String, String> fbbTypeProperty = new HashMap<String, String>();
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FBB_TYPE);
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_VALUE, "createJobFbb");

        final Map<String, String> userNameProperty = new HashMap<String, String>();
        userNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.USERNAME);
        userNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "administrator");

        final Map<String, String> requestIdProperty = new HashMap<String, String>();
        requestIdProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FA_REQUEST_ID);
        requestIdProperty.put(ActivityConstants.JOB_PROP_VALUE, "requestId");

        final Map<String, String> executionNameProperty = new HashMap<String, String>();
        executionNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME);
        executionNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "test-nhc-job");

        jobProperties.add(executionNameProperty);
        jobProperties.add(userNameProperty);
        jobProperties.add(fbbTypeProperty);
        jobProperties.add(requestIdProperty);

        activityJobPoAttributes.put(ShmJobConstants.NE_JOB_ID, neJobId);
        activityJobPoAttributes.put(ShmConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        activityJobPoAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        Map<String, Object> poAttributes = new HashMap<>();
        poAttributes.put(ShmConstants.PO_ATTRIBUTES, activityJobPoAttributes);
        poAttributes.put(ShmConstants.PO_ID, activityJobId);

        when(jobConfigurationServiceRetryProxy.getActivityJobAttributesByNeJobId(neJobId, restrictions)).thenReturn(Collections.emptyList());
        blockResponseProcessor.sendNhcFaResponse(neJobId, mainJobId, JobCategory.NHC_FA.getAttribute(), neJobPoAttributes, mainJobAttributes);

        verify(blockResponseProvider, times(0)).send(Matchers.any(), Matchers.any());

    }

    @Test
    public void testSendNHCResponse_NotHealthy() throws JobDataNotFoundException {

        final Map<String, Object> activityJobPoAttributes = new HashMap<>();
        List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, Object> mainJobAttributes = new HashMap<>();

        final Map<String, String> property = new HashMap<>();
        property.put(ActivityConstants.JOB_PROP_KEY, ShmConstants.JOB_CATEGORY);
        property.put(ActivityConstants.JOB_PROP_VALUE, JobCategory.NHC_FA.getAttribute());
        List<Map<String, String>> properties = new ArrayList<>();
        properties.add(property);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, properties);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttributes);

        final Map<String, Object> neJobPoAttributes = new HashMap<>();
        neJobPoAttributes.put(ShmConstants.NEJOB_HEALTH_STATUS, HealthStatus.NOT_HEALTHY.name());
        neJobPoAttributes.put(ShmJobConstants.RESULT, "FAILED");
        neJobPoAttributes.put(ShmConstants.NE_NAME, "neName");

        final Map<String, Object> restrictions = new HashMap<>();
        //restrictions.put(ShmJobConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        restrictions.put(ShmJobConstants.NE_JOB_ID, neJobId);

        final Map<String, String> fbbTypeProperty = new HashMap<String, String>();
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FBB_TYPE);
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_VALUE, "createJobFbb");

        final Map<String, String> userNameProperty = new HashMap<String, String>();
        userNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.USERNAME);
        userNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "administrator");

        final Map<String, String> requestIdProperty = new HashMap<String, String>();
        requestIdProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FA_REQUEST_ID);
        requestIdProperty.put(ActivityConstants.JOB_PROP_VALUE, "requestId");

        final Map<String, String> executionNameProperty = new HashMap<String, String>();
        executionNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME);
        executionNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "test-nhc-job");

        jobProperties.add(executionNameProperty);
        jobProperties.add(userNameProperty);
        jobProperties.add(fbbTypeProperty);
        jobProperties.add(requestIdProperty);

        activityJobPoAttributes.put(ShmJobConstants.NE_JOB_ID, neJobId);
        activityJobPoAttributes.put(ShmConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        activityJobPoAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        Map<String, Object> poAttributes = new HashMap<>();
        poAttributes.put(ShmConstants.PO_ATTRIBUTES, activityJobPoAttributes);
        poAttributes.put(ShmConstants.PO_ID, activityJobId);

        when(jobConfigurationServiceRetryProxy.getActivityJobAttributesByNeJobId(neJobId, restrictions)).thenReturn(Arrays.asList(poAttributes));
        blockResponseProcessor.sendNhcFaResponse(neJobId, mainJobId, JobCategory.NHC_FA.getAttribute(), neJobPoAttributes, mainJobAttributes);

        verify(blockResponseProvider, times(1)).send(Matchers.any(), Matchers.any());
    }

    @Test
    public void testSendNHCResponse_lastLogFromMainJob() throws JobDataNotFoundException {

        final Map<String, Object> activityJobPoAttributes = new HashMap<>();
        List<Map<String, String>> jobProperties = new ArrayList<>();
        final Map<String, Object> mainJobAttributes = new HashMap<>();

        final Map<String, String> property = new HashMap<>();
        property.put(ActivityConstants.JOB_PROP_KEY, ShmConstants.JOB_CATEGORY);
        property.put(ActivityConstants.JOB_PROP_VALUE, JobCategory.NHC_FA.getAttribute());
        List<Map<String, String>> properties = new ArrayList<>();
        properties.add(property);
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, properties);

        Map<String, String> log = new HashMap<>();
        log.put("message", "last log message");
        log.put("logLevel", "INFO");
        List<Map<String, String>> logs = new ArrayList<>();
        logs.add(log);
        mainJobAttributes.put(ShmConstants.LOG, logs);
        when(activityUtils.getPoAttributes(mainJobId)).thenReturn(mainJobAttributes);

        final Map<String, Object> neJobPoAttributes = new HashMap<>();
        neJobPoAttributes.put(ShmConstants.NEJOB_HEALTH_STATUS, "UN-HEALTHY");
        neJobPoAttributes.put(ShmJobConstants.RESULT, "SUCCESS");
        neJobPoAttributes.put(ShmConstants.NE_NAME, "neName");

        final Map<String, Object> restrictions = new HashMap<>();
        //restrictions.put(ShmJobConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        restrictions.put(ShmJobConstants.NE_JOB_ID, neJobId);

        final Map<String, String> fbbTypeProperty = new HashMap<String, String>();
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FBB_TYPE);
        fbbTypeProperty.put(ActivityConstants.JOB_PROP_VALUE, "createJobFbb");

        final Map<String, String> userNameProperty = new HashMap<String, String>();
        userNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.USERNAME);
        userNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "administrator");

        final Map<String, String> requestIdProperty = new HashMap<String, String>();
        requestIdProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FA_REQUEST_ID);
        requestIdProperty.put(ActivityConstants.JOB_PROP_VALUE, "requestId");

        final Map<String, String> executionNameProperty = new HashMap<String, String>();
        executionNameProperty.put(ActivityConstants.JOB_PROP_KEY, ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME);
        executionNameProperty.put(ActivityConstants.JOB_PROP_VALUE, "test-nhc-job");

        jobProperties.add(executionNameProperty);
        jobProperties.add(userNameProperty);
        jobProperties.add(fbbTypeProperty);
        jobProperties.add(requestIdProperty);

        activityJobPoAttributes.put(ShmJobConstants.NE_JOB_ID, neJobId);
        activityJobPoAttributes.put(ShmConstants.ACTIVITY_NAME, NodeHealthCheckJobActivityTimeouts.NODE_HEALTH_CHECK);
        activityJobPoAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        Map<String, Object> poAttributes = new HashMap<>();
        poAttributes.put(ShmConstants.PO_ATTRIBUTES, activityJobPoAttributes);
        poAttributes.put(ShmConstants.PO_ID, activityJobId);

        when(jobConfigurationServiceRetryProxy.getActivityJobAttributesByNeJobId(neJobId, restrictions)).thenReturn(Arrays.asList(poAttributes));
        blockResponseProcessor.sendNhcFaResponse(neJobId, mainJobId, JobCategory.NHC_FA.getAttribute(), neJobPoAttributes, mainJobAttributes);

        verify(blockResponseProvider, times(0)).send(Matchers.any(), Matchers.any());

    }

    @Test
    public void testSendNHCResponse_Exception() throws JobDataNotFoundException {

        blockResponseProcessor.sendNhcFaResponse(neJobId, mainJobId, JobCategory.NHC_FA.getAttribute(), null, null);

        verify(blockResponseProvider, times(0)).send(Matchers.any(), Matchers.any());
    }

    @Test
    public void testSendFaResponse_Exception() throws JobDataNotFoundException {
        when(activityUtils.getCapabilityByJobType(jobType)).thenReturn(SHMCapabilities.BACKUP_JOB_CAPABILITY);
        when(jobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(null);
        blockResponseProcessor.sendFaResponse(activityJobId, jobType, jobResult, null);

        verify(blockResponseProvider, times(0)).send(Matchers.any(), Matchers.any());
    }

}
