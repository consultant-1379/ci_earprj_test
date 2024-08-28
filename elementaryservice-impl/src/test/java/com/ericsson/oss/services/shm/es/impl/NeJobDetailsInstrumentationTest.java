/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.enums.NodeType;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobResult;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * Test class to test NeJobDetailsInstrumentation methods
 *
 * @author zchourv
 */

@RunWith(MockitoJUnitRunner.class)
public class NeJobDetailsInstrumentationTest {

    @InjectMocks
    NeJobDetailsInstrumentation neJobDetailsInstrumentation;

    @Mock
    private DataPersistenceService dataPersistenceService;

    @Mock
    private QueryBuilder queryBuilder;

    @Mock
    private Query<TypeRestrictionBuilder> query;

    @Mock
    private DataBucket liveBucket;

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private TypeRestrictionBuilder restrictionBuilder;

    @Mock
    private Restriction restriction;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    private static final long mainJobId = 1001L;
    private static final long jobTemplateId = 1002L;
    private final String jobType = JobTypeEnum.BACKUP.name();
    private final String jobName = "Job1";

    @Test
    public void testRecordNeTypeDataSuccess() {
        when(jobConfigurationServiceRetryProxy.getPOAttributes(Mockito.anyLong())).thenReturn(buildJobTemplateAttributes());
        buildNeJobDataList();
        neJobDetailsInstrumentation.recordNeJobResultBasedOnNeType(mainJobId, buildMainJobAttributes());
        verify(query).setRestriction(restriction);
        verify(systemRecorder, times(3)).recordEventData(eq(SHMEvents.MAIN_JOB_COMPLETED), anyMap());
    }

    @Test
    public void testRecordNeTypeDataWhenJobTemplateAttributesEmpty() {
        when(jobConfigurationServiceRetryProxy.getPOAttributes(Mockito.anyLong())).thenReturn(new HashMap<String, Object>());
        neJobDetailsInstrumentation.recordNeJobResultBasedOnNeType(mainJobId, buildMainJobAttributes());
        verify(query, times(0)).setRestriction(restriction);
        verify(systemRecorder, times(0)).recordEventData(eq(SHMEvents.MAIN_JOB_COMPLETED), anyMap());
    }

    @Test(expected = RuntimeException.class)
    public void testRecordNeTypeDataException() {
        when(jobConfigurationServiceRetryProxy.getPOAttributes(Mockito.anyLong())).thenReturn(buildJobTemplateAttributes());
        doThrow(new RuntimeException("Negative Scenario")).when(dataPersistenceService).getQueryBuilder();
        neJobDetailsInstrumentation.recordNeJobResultBasedOnNeType(mainJobId, buildMainJobAttributes());
        verify(query, times(0)).setRestriction(restriction);
        verify(systemRecorder, times(0)).recordEventData(SHMEvents.MAIN_JOB_COMPLETED, anyMap());
    }

    @Test
    public void testRecordNeTypeDataForMainJobPoAttributesEmpty() {
        when(jobConfigurationServiceRetryProxy.getPOAttributes(Mockito.anyLong())).thenReturn(buildJobTemplateAttributes());
        neJobDetailsInstrumentation.recordNeJobResultBasedOnNeType(mainJobId, new HashMap<>());
        verify(query, times(0)).setRestriction(restriction);
        final Map<String, Object> recordEventData = buildEventDataMap();
        verify(systemRecorder, times(0)).recordEventData(SHMEvents.MAIN_JOB_COMPLETED, recordEventData);
    }

    @Test
    public void testRecordNeTypeDataForJobTemplateIDZero() {
        final Map<String, Object> mainJobAttributesMap = buildJobTemplateAttributes();
        mainJobAttributesMap.put(ShmConstants.JOBTEMPLATEID, 0l);
        neJobDetailsInstrumentation.recordNeJobResultBasedOnNeType(mainJobId, mainJobAttributesMap);
        verify(query, times(0)).setRestriction(restriction);
        final Map<String, Object> eventData = new HashMap<>();
        verify(systemRecorder, times(0)).recordEventData(SHMEvents.MAIN_JOB_COMPLETED, eventData);
    }

    private Map<String, Object> buildMainJobAttributes() {
        final Map<String, Object> poAttributeMap = new HashMap<>();
        poAttributeMap.put(ShmConstants.JOBTEMPLATEID, jobTemplateId);
        return poAttributeMap;
    }

    private Map<String, Object> buildJobTemplateAttributes() {
        final Map<String, Object> poAttributeMap = new HashMap<>();
        poAttributeMap.put(ShmConstants.NAME, jobName);
        poAttributeMap.put(ShmConstants.JOB_TYPE, jobType);
        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        jobConfigurationDetails.put(ShmConstants.JOB_ACTIVITIES, buildJobActivityList());
        poAttributeMap.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfigurationDetails);
        return poAttributeMap;
    }

    private List<Map<String, Object>> buildJobActivityList() {
        final List<Map<String, Object>> activityList = new ArrayList();
        activityList.add(getActivityObject(NodeType.RADIONODE, ActivityConstants.CREATE_BACKUP));
        activityList.add(getActivityObject(NodeType.RADIONODE, ActivityConstants.UPLOAD));
        activityList.add(getActivityObject(NodeType.SGSN_MME, ActivityConstants.CREATE_BACKUP));
        activityList.add(getActivityObject(NodeType.SGSN_MME, ActivityConstants.UPLOAD));
        activityList.add(getActivityObject(NodeType.ERBS, ActivityConstants.CREATE_CV_ACTIVITY_NAME));
        activityList.add(getActivityObject(NodeType.ERBS, ActivityConstants.SET_AS_STARTABLE_ACTIVITY_NAME));
        activityList.add(getActivityObject(NodeType.ERBS, ActivityConstants.SET_FIRST_IN_ROLLBACK_ACTIVITY_NAME));
        activityList.add(getActivityObject(NodeType.ERBS, ActivityConstants.EXPORT_CV_ACTIVITY_NAME));
        return activityList;
    }

    private Map<String, Object> getActivityObject(final NodeType nodeType, final String activityName) {
        final Map<String, Object> activityMap = new HashMap<>();
        activityMap.put(ShmConstants.NETYPE, nodeType.name());
        activityMap.put(ShmConstants.NAME, activityName);
        return activityMap;
    }

    private void buildNeJobDataList() {
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NE_JOB)).thenReturn(query);
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(query.getRestrictionBuilder()).thenReturn(restrictionBuilder);
        when(restrictionBuilder.equalTo(ShmConstants.MAINJOBID, mainJobId)).thenReturn(restriction);

        final List<Object[]> neJobs = new ArrayList<>();

        neJobs.add(getNodeData(NodeType.ERBS, JobResult.SUCCESS));
        neJobs.add(getNodeData(NodeType.ERBS, JobResult.SUCCESS));
        neJobs.add(getNodeData(NodeType.ERBS, JobResult.FAILED));
        neJobs.add(getNodeData(NodeType.ERBS, JobResult.FAILED));
        neJobs.add(getNodeData(NodeType.ERBS, JobResult.FAILED));
        neJobs.add(getNodeData(NodeType.ERBS, JobResult.SKIPPED));

        neJobs.add(getNodeData(NodeType.RADIONODE, JobResult.SUCCESS));
        neJobs.add(getNodeData(NodeType.RADIONODE, JobResult.FAILED));
        neJobs.add(getNodeData(NodeType.RADIONODE, JobResult.FAILED));
        neJobs.add(getNodeData(NodeType.RADIONODE, JobResult.FAILED));
        neJobs.add(getNodeData(NodeType.RADIONODE, JobResult.FAILED));
        neJobs.add(getNodeData(NodeType.RADIONODE, JobResult.SKIPPED));
        neJobs.add(getNodeData(NodeType.RADIONODE, JobResult.SKIPPED));
        neJobs.add(getNodeData(NodeType.RADIONODE, JobResult.CANCELLED));

        neJobs.add(getNodeData(NodeType.SGSN_MME, JobResult.SUCCESS));
        neJobs.add(getNodeData(NodeType.SGSN_MME, JobResult.SUCCESS));
        neJobs.add(getNodeData(NodeType.SGSN_MME, JobResult.SUCCESS));
        neJobs.add(getNodeData(NodeType.SGSN_MME, JobResult.FAILED));
        neJobs.add(getNodeData(NodeType.SGSN_MME, JobResult.FAILED));
        neJobs.add(getNodeData(NodeType.SGSN_MME, JobResult.FAILED));
        neJobs.add(getNodeData(NodeType.SGSN_MME, JobResult.FAILED));
        neJobs.add(getNodeData(NodeType.SGSN_MME, JobResult.FAILED));
        neJobs.add(getNodeData(NodeType.SGSN_MME, JobResult.SKIPPED));
        neJobs.add(getNodeData(NodeType.SGSN_MME, JobResult.CANCELLED));
        neJobs.add(getNodeData(NodeType.SGSN_MME, JobResult.CANCELLED));
        when(queryExecutor.executeProjection(eq(query), any(Projection.class), any(Projection.class))).thenReturn(neJobs);
    }

    private Map<String, Object> buildEventDataMap() {
        final Map<String, Object> erbsEventData = buildNodeEventData(2, 3, 1, 0);
        final Set<String> set = new HashSet<String>();
        set.add(ActivityConstants.EXPORT_CV_ACTIVITY_NAME);
        set.add(ActivityConstants.SET_FIRST_IN_ROLLBACK_ACTIVITY_NAME);
        set.add(ActivityConstants.SET_AS_STARTABLE_ACTIVITY_NAME);
        set.add(ActivityConstants.CREATE_CV_ACTIVITY_NAME);
        final List<String> erbsList = new ArrayList<>(set);
        erbsEventData.put(ShmConstants.ACTIVITIES, erbsList);

        final Map<String, Object> radioEventData = buildNodeEventData(1, 4, 2, 1);
        final List<String> radioList = new ArrayList<>();
        radioList.add("uploadbackup");
        radioList.add(ActivityConstants.CREATE_BACKUP);
        radioEventData.put(ShmConstants.ACTIVITIES, radioList);

        final Map<String, Object> sgsnEventData = buildNodeEventData(3, 5, 1, 2);
        final List<String> sgsnList = new ArrayList<>();
        sgsnList.add(("uploadbackup"));
        sgsnList.add(ActivityConstants.CREATE_BACKUP);
        sgsnEventData.put(ShmConstants.ACTIVITIES, sgsnList);

        final Map<String, Map<String, Object>> nodeEventData = new HashMap<>();
        nodeEventData.put(NodeType.ERBS.name(), erbsEventData);
        nodeEventData.put(NodeType.RADIONODE.name(), radioEventData);
        nodeEventData.put(NodeType.SGSN_MME.name(), sgsnEventData);

        final Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put(ShmConstants.JOB_TYPE, jobType);
        eventDataMap.put(ShmConstants.JOBNAME, jobName);
        return eventDataMap;
    }

    private Map<String, Object> buildNodeEventData(final Integer successCount, final Integer failureCount, final Integer skippedCount, final Integer cancelledCount) {
        final Map<String, Object> nodeEventData = new HashMap<>();
        nodeEventData.put(ShmConstants.SUCCESS_COUNT, successCount);
        nodeEventData.put(ShmConstants.FAILED_COUNT, failureCount);
        nodeEventData.put(ShmConstants.SKIPPED_COUNT, skippedCount);
        nodeEventData.put(ShmConstants.CANCELLED_COUNT, cancelledCount);
        nodeEventData.put(ShmConstants.TOTAL_COUNT, successCount + failureCount + skippedCount + cancelledCount);
        return nodeEventData;
    }

    private Object[] getNodeData(final NodeType nodeType, final JobResult result) {
        final Object[] nodeData = new Object[2];
        nodeData[0] = nodeType.name();
        nodeData[1] = result.name();
        return nodeData;
    }

}
