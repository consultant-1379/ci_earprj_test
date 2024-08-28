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
package com.ericsson.oss.services.shm.jobservice.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;

@RunWith(MockitoJUnitRunner.class)
public class HungJobQueryServiceTest {

    @Mock
    private DataBucket liveBucket;

    @Mock
    private DataPersistenceService dataPersistenceService;

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private QueryBuilder queryBuilder;

    @Mock
    private Query<TypeRestrictionBuilder> typeQuery;

    @Mock
    private Restriction restriction;

    @Mock
    private TypeRestrictionBuilder typeRestrictionBuilder;

    @Mock
    private List<Object> listOfObjects;

    @Mock
    private Iterator<Object> iterator;

    @Mock
    protected PersistenceObject persistenceObject;

    @Mock
    private JobCapabilityProvider jobCapabilityProvider;

    @Mock
    protected DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @InjectMocks
    private HungJobQueryService hungJobQueryService;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelperMock;

    @Mock
    private Map<String, Object> jobConfigurationDetails;

    @Mock
    private Map<String, Object> mainSchedule;

    @Mock
    private List<Map<String, Object>> schedulePropertiesList;

    @Mock
    private DpsReader dpsReader;

    @Test
    public void testgetAllHangingMainJobsAttributes() {
        final long poId = 1;
        final long executionIndex = 2;
        final String state = "RUNNING";
        final long templateId = 34;
        final String businessKey = String.valueOf(templateId);
        final int maxTimeLimitForJobExecutionInHours = 3;
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.lessThan(ShmConstants.STARTTIME, new DateTime().minusHours(maxTimeLimitForJobExecutionInHours).toDate())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(ShmConstants.STATE, JobState.RUNNING.getJobStateName())).thenReturn(restriction);
        when(typeRestrictionBuilder.nullValue(ShmConstants.ENDTIME)).thenReturn(restriction);
        when(typeRestrictionBuilder.allOf(restriction, restriction, restriction)).thenReturn(restriction);
        final List<Object> queryResult = new ArrayList<>();
        queryResult.add(persistenceObject);
        when(queryExecutor.getResultList(typeQuery)).thenReturn(queryResult);
        when(persistenceObject.getPoId()).thenReturn(poId);
        when(persistenceObject.getAttribute(ShmConstants.EXECUTIONINDEX)).thenReturn(executionIndex);
        when(persistenceObject.getAttribute(ShmConstants.STATE)).thenReturn(state);
        when(persistenceObject.getAttribute(ShmConstants.JOB_TEMPLATE_ID)).thenReturn(templateId);
        when(persistenceObject.getAttribute(ShmConstants.BUSINESS_KEY)).thenReturn(businessKey);
        when(persistenceObject.getAttribute(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(jobConfigurationDetails);
        when(jobConfigurationDetails.get(ShmConstants.MAIN_SCHEDULE)).thenReturn(mainSchedule);
        when(mainSchedule.get(ShmConstants.SCHEDULINGPROPERTIES)).thenReturn(schedulePropertiesList);

        final List<Object[]> actualResult = hungJobQueryService.getLongRunningJobs(maxTimeLimitForJobExecutionInHours);

        assertEquals(1, actualResult.size());
        assertEquals(poId, actualResult.get(0)[0]);
        assertEquals(executionIndex, actualResult.get(0)[1]);
        assertEquals(state, actualResult.get(0)[2]);
        assertEquals(templateId, actualResult.get(0)[3]);
        assertEquals(businessKey, actualResult.get(0)[4]);
        assertNotNull(actualResult.get(0)[5]);
    }

    @Test
    public void testgetAllHangingMainJobsAttributesNull() {
        final int maxTimeLimitForJobExecutionInHours = 3;
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.lessThan(ShmConstants.STARTTIME, new DateTime().minusHours(maxTimeLimitForJobExecutionInHours).toDate())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(ShmConstants.STATE, JobState.RUNNING.getJobStateName())).thenReturn(restriction);
        when(typeRestrictionBuilder.nullValue(ShmConstants.ENDTIME)).thenReturn(restriction);
        when(typeRestrictionBuilder.allOf(restriction, restriction, restriction)).thenReturn(restriction);
        when(queryExecutor.executeProjection(typeQuery, ProjectionBuilder.field(ObjectField.PO_ID), ProjectionBuilder.attribute(ShmConstants.EXECUTIONINDEX),
                ProjectionBuilder.attribute(ShmConstants.STATE), ProjectionBuilder.attribute(ShmConstants.JOB_TEMPLATE_ID))).thenReturn(null);
        Assert.assertEquals(Collections.emptyList(), hungJobQueryService.getLongRunningJobs(maxTimeLimitForJobExecutionInHours));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testgetAllHangingMainJobsAttributesException() {
        final int maxTimeLimitForJobExecutionInHours = 3;
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenThrow(RuntimeException.class);
        Assert.assertEquals(Collections.emptyList(), hungJobQueryService.getLongRunningJobs(maxTimeLimitForJobExecutionInHours));
        verify(typeQuery, Mockito.times(0)).setRestriction(restriction);
    }

    @Test
    public void testgetJobName() {
        final long jobTemplateId = 1L;
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(ObjectField.PO_ID, jobTemplateId)).thenReturn(restriction);
        when(queryExecutor.executeProjection(typeQuery, ProjectionBuilder.attribute(ShmConstants.NAME))).thenReturn(listOfObjects);
        hungJobQueryService.getJobNameAndWorkflowId(jobTemplateId);
        verify(listOfObjects, Mockito.times(0)).get(0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testgetJobNameException() {
        final int maxTimeLimitForJobExecutionInHours = 3;
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenThrow(RuntimeException.class);
        Assert.assertEquals(Collections.emptyList(), hungJobQueryService.getLongRunningJobs(maxTimeLimitForJobExecutionInHours));
        verify(typeQuery, Mockito.times(0)).setRestriction(restriction);
    }

    @Test
    public void testgetHungNeJobs() {
        final int mainJobId = 1234;
        final String jobName = "UPGRADE";
        final int maxTimeLimitForJobExecutionInHours = 3;
        final Map<String, Object> neJobPoProperties = new HashMap<>();
        neJobPoProperties.put(ShmConstants.WFS_ID, "dummy123");
        neJobPoProperties.put(ShmConstants.STATE, "IMMEDIATE");
        neJobPoProperties.put(ShmConstants.NE_NAME, "neName");
        final List<PersistenceObject> poList = new ArrayList<>();
        poList.add(persistenceObject);
        final List<NetworkElement> networkElementList = new ArrayList<>();
        final NetworkElement ne = new NetworkElement();
        ne.setName("node2");
        ne.setPlatformType(PlatformTypeEnum.CPP);
        networkElementList.add(ne);
        final int maxTimeLimitForAxeUpgradeJobExecutionInHours = 4;
        final Object[] dummy = { 1234L, "IMMEDIATE", "dummy123", "neName" };
        final List<Object[]> queryProjections = new LinkedList<>();
        queryProjections.add(dummy);
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NE_JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(Matchers.anyString(), Matchers.anyInt())).thenReturn(restriction);
        when(typeRestrictionBuilder.lessThan(Matchers.anyString(), Matchers.any(Date.class))).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(ShmConstants.STATE, JobState.RUNNING.getJobStateName())).thenReturn(restriction);
        when(typeRestrictionBuilder.nullValue(ShmConstants.ENDTIME)).thenReturn(restriction);
        when(typeRestrictionBuilder.allOf(restriction, restriction, restriction, restriction)).thenReturn(restriction);
        PersistenceObject po = HungJobQueryServiceTestBase.buildNeJobPo(PlatformTypeEnum.CPP);
        when(queryExecutor.execute(Matchers.any(Query.class))).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true, false);
        when(iterator.next()).thenReturn(po);
        when(jobCapabilityProvider.getCapability(JobTypeEnum.getJobType(jobName))).thenReturn(SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList(), Matchers.anyString())).thenReturn(networkElementList);
        assertNotNull(hungJobQueryService.getHungNeJobs(mainJobId, maxTimeLimitForJobExecutionInHours, maxTimeLimitForAxeUpgradeJobExecutionInHours, jobName));
    }

    @Test
    public void testgetHungAxeNeJobs() {
        final int mainJobId = 1234;
        final String jobName = "UPGRADE";
        final int maxTimeLimitForJobExecutionInHours = 3;
        final Map<String, Object> neJobPoProperties = new HashMap<>();
        neJobPoProperties.put(ShmConstants.WFS_ID, "dummy123");
        neJobPoProperties.put(ShmConstants.STATE, "IMMEDIATE");
        neJobPoProperties.put(ShmConstants.NE_NAME, "neName");
        final List<PersistenceObject> poList = new ArrayList<>();
        poList.add(persistenceObject);
        final List<NetworkElement> networkElementList = new ArrayList<>();
        final NetworkElement node1 = new NetworkElement();
        node1.setName("node1");
        node1.setPlatformType(PlatformTypeEnum.AXE);
        networkElementList.add(node1);
        final int maxTimeLimitForAxeUpgradeJobExecutionInHours = 4;
        final Object[] dummy = { 1234L, "IMMEDIATE", "dummy123", "neName" };
        final List<Object[]> queryProjections = new LinkedList<Object[]>();
        queryProjections.add(dummy);
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NE_JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(Matchers.anyString(), Matchers.anyInt())).thenReturn(restriction);
        when(typeRestrictionBuilder.lessThan(Matchers.anyString(), Matchers.any(Date.class))).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(ShmConstants.STATE, JobState.RUNNING.getJobStateName())).thenReturn(restriction);
        when(typeRestrictionBuilder.nullValue(ShmConstants.ENDTIME)).thenReturn(restriction);
        when(typeRestrictionBuilder.allOf(restriction, restriction, restriction, restriction)).thenReturn(restriction);
        when(queryExecutor.execute(Matchers.any(Query.class))).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true, false);
        when(iterator.next()).thenReturn(HungJobQueryServiceTestBase.buildNeJobPo(PlatformTypeEnum.AXE));
        when(jobCapabilityProvider.getCapability(JobTypeEnum.getJobType(jobName))).thenReturn(SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Matchers.anyList(), Matchers.anyString())).thenReturn(networkElementList);
        assertNotNull(hungJobQueryService.getHungNeJobs(mainJobId, maxTimeLimitForJobExecutionInHours, maxTimeLimitForAxeUpgradeJobExecutionInHours, jobName));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testgetHungNeJobsEmptyQueryProjections() {
        final Map<String, Object> neJobPoProperties = new HashMap<>();

        neJobPoProperties.put(ShmConstants.WFS_ID, "dummy123");
        neJobPoProperties.put(ShmConstants.STATE, "IMMEDIATE");
        neJobPoProperties.put(ShmConstants.NE_NAME, "neName");
        final int mainJobId = 1234;
        final int maxTimeLimitForJobExecutionInHours = 3;
        final int maxTimeLimitForAxeUpgradeJobExecutionInHours = 4;
        final String jobName = "BACKUP";
        final Object[] dummy = { 1234L, "IMMEDIATE", "dummy123", "neName" };
        final List<Object[]> queryProjections = new LinkedList<Object[]>();
        queryProjections.add(dummy);
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NE_JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(Matchers.anyString(), Matchers.anyInt())).thenReturn(restriction);
        when(typeRestrictionBuilder.lessThan(Matchers.anyString(), Matchers.any(Date.class))).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(ShmConstants.STATE, JobState.RUNNING.getJobStateName())).thenReturn(restriction);
        when(typeRestrictionBuilder.nullValue(ShmConstants.ENDTIME)).thenReturn(restriction);
        when(typeRestrictionBuilder.allOf(restriction, restriction, restriction, restriction)).thenReturn(restriction);
        when(queryExecutor.execute(Matchers.any(Query.class))).thenReturn(iterator);
        when(persistenceObject.getAllAttributes()).thenReturn(neJobPoProperties);
        Assert.assertEquals(Collections.emptyList(), hungJobQueryService.getHungNeJobs(mainJobId, maxTimeLimitForJobExecutionInHours, maxTimeLimitForAxeUpgradeJobExecutionInHours, jobName));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ServerInternalException.class)
    public void testgetHungNeJobsException() {
        final int mainJobId = 1234;
        final int maxTimeLimitForJobExecutionInHours = 3;
        final String jobName = "BACKUP";
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenThrow(RuntimeException.class);
        hungJobQueryService.getHungNeJobs(mainJobId, maxTimeLimitForJobExecutionInHours, maxTimeLimitForJobExecutionInHours, jobName);
    }

    @Test
    public void testcancelActivitiesAndUpdateStateWhenIteratorEmpty() {
        final long neJobId = 1234L;
        final String jobName = "JobName";
        final int executionIndex = 999;
        final int maxTimeLimitForJobExecutionInHours = 3;
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(Matchers.anyString(), Matchers.anyLong())).thenReturn(restriction);
        when(typeRestrictionBuilder.anyOf(restriction)).thenReturn(restriction);
        when(typeRestrictionBuilder.lessThan(Matchers.anyString(), Matchers.any(Date.class))).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(ShmConstants.STATE, JobState.RUNNING.getJobStateName())).thenReturn(restriction);
        when(typeRestrictionBuilder.nullValue(ShmConstants.ENDTIME)).thenReturn(restriction);
        when(typeRestrictionBuilder.allOf(restriction, restriction, restriction, restriction)).thenReturn(restriction);
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryExecutor.execute(typeQuery)).thenReturn(iterator);
        final Map<String, Object> activityJobsUpdatedStatusMap = hungJobQueryService.cancelActivitiesAndUpdateState(neJobId, jobName, executionIndex, maxTimeLimitForJobExecutionInHours);
        final boolean isActivityJobsUpdated = (boolean) activityJobsUpdatedStatusMap.get("isActivityJobsUpdated");
        assertTrue(isActivityJobsUpdated);
    }

    @Test
    public void testgetMainJobWorkflowIds() {
        final long jobTemplateId = 12345L;
        when(dpsReader.findPOByPoId(jobTemplateId)).thenReturn(persistenceObject);
        assertNotNull(hungJobQueryService.getJobNameAndWorkflowId(jobTemplateId));
    }

    @Test
    public void testprepareAttributesToBeUpdated() {
        final JobState state = JobState.SYSTEM_CANCELLED;
        final String logMessage = "SystemCancelled";
        hungJobQueryService.prepareAttributesToBeUpdated(state, logMessage);
    }

    @Test
    public void testCheckRunningNeJobs_ReturnFalseWhenNEJobNotInRunning() {
        final List<Object> mainJobProjection = new ArrayList<Object>();
        final long mainJobId = 12345L;
        mainJobProjection.add("projection1");
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(Matchers.anyString(), Matchers.anyObject())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(ShmConstants.STATE, JobState.RUNNING.getJobStateName())).thenReturn(restriction);
        when(typeRestrictionBuilder.nullValue(ShmConstants.ENDTIME)).thenReturn(restriction);
        when(queryExecutor.executeProjection(typeQuery, ProjectionBuilder.attribute(ShmConstants.STATE))).thenReturn(mainJobProjection);
        assertFalse(hungJobQueryService.checkRunningNeJobs(mainJobId));
    }

    @Test
    public void testCheckRunningNeJobs_ReturnTrueWhenNEJobInRunning() {
        final List<Object> mainJobProjection = new ArrayList<Object>();
        final long mainJobId = 12345L;
        mainJobProjection.add("projection1");
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(Matchers.anyString(), Matchers.anyObject())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(ShmConstants.STATE, JobState.RUNNING.getJobStateName())).thenReturn(restriction);
        when(typeRestrictionBuilder.nullValue(ShmConstants.ENDTIME)).thenReturn(restriction);
        when(queryExecutor.executeProjection(typeQuery, ProjectionBuilder.attribute(ShmConstants.STATE))).thenReturn(mainJobProjection);
        when(typeRestrictionBuilder.allOf(restriction, restriction, restriction)).thenReturn(restriction);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class))).thenReturn(mainJobProjection);
        assertTrue(hungJobQueryService.checkRunningNeJobs(mainJobId));

    }

    @Test
    public void testDeleteStagedActivityPO() {
        final long stagedActivityPOId = 12345L;
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(dpsReader.findPOByPoId(stagedActivityPOId)).thenReturn(persistenceObject);
        hungJobQueryService.deleteStagedActivityPO(stagedActivityPOId);
        verify(liveBucket, times(1)).deletePo(persistenceObject);
    }

    @Test
    public void testGetStagedActivityPOs() {
        final int maxTimeLimitForJobExecutionInHours = 48;
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(ShmConstants.STAGED_ACTIVITY_STATUS, ShmConstants.READY_STATE)).thenReturn(restriction);
        when(typeRestrictionBuilder.lessThan(Matchers.anyString(), Matchers.any())).thenReturn(restriction);
        when(typeRestrictionBuilder.allOf(restriction, restriction)).thenReturn(restriction);
        when(queryExecutor.execute(typeQuery)).thenReturn(iterator);
        assertNotNull(hungJobQueryService.getStagedActivityPOs(maxTimeLimitForJobExecutionInHours));
    }

    @Test(expected = Exception.class)
    public void testGetStagedActivityPOsWhenExceptionOccured() {
        final int maxTimeLimitForJobExecutionInHours = 48;
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo(ShmConstants.STAGED_ACTIVITY_STATUS, ShmConstants.READY_STATE)).thenReturn(restriction);
        when(typeRestrictionBuilder.lessThan(Matchers.anyString(), Matchers.any())).thenReturn(restriction);
        when(typeRestrictionBuilder.allOf(restriction, restriction)).thenReturn(restriction);
        when(queryExecutor.execute(typeQuery)).thenThrow(Exception.class);
        hungJobQueryService.getStagedActivityPOs(maxTimeLimitForJobExecutionInHours);
    }
}
