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
package com.ericsson.oss.services.shm.job.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
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
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

@RunWith(MockitoJUnitRunner.class)
public class JobsDeletionServiceTest {

    @Mock
    PersistenceObject jobTemplate;

    @Mock
    PersistenceObject mainJob;

    @Mock
    PersistenceObject neJob;

    @Mock
    PersistenceObject activityJob;

    @Mock
    DataPersistenceService dataPersistenceService;

    @Mock
    DataBucket liveBucket;

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    QueryExecutor queryExecutor;

    @Mock
    Query<TypeRestrictionBuilder> typeQuery;

    @Mock
    TypeRestrictionBuilder typeRestrictionBuilder;

    @Mock
    Restriction restriction;

    @Mock
    Iterator<Object> iteratorMock;

    @InjectMocks
    JobsDeletionService objectUnderTest;

    @Mock
    SHMJobService shmJobServiceTest;

    @Mock
    JobDeletionService jobDeletionServiceTest;

    @Test
    public void testDeleteJobHierarchyWithoutJobTemplate1() {
        final long mainJobId = 1;
        final long neJobId = 45676;
        final long activityJobId = 2333;
        final String jobName = "jobName";
        final String jobType = JobType.UPGRADE.toString();

        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo("attributeName", "attrbuteValue")).thenReturn(restriction);
        when(queryExecutor.execute(any(Query.class))).thenReturn(iteratorMock);
        when(iteratorMock.hasNext()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
        when(iteratorMock.next()).thenReturn(neJob).thenReturn(activityJob);

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        when(neJob.getPoId()).thenReturn(neJobId);
        when(neJob.getAttribute(ShmConstants.NE_NAME)).thenReturn("NodeName");

        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityJob.getPoId()).thenReturn(activityJobId);
        when(activityJob.getAttribute(ShmConstants.ACTIVITY_NAME)).thenReturn("install");
        when(activityJob.getAttribute(ShmConstants.STATE)).thenReturn("COMPLETED");

        when(liveBucket.findPoById(mainJobId)).thenReturn(mainJob);
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.deletePo(activityJob)).thenReturn(1);
        when(liveBucket.deletePo(neJob)).thenReturn(1);
        when(liveBucket.deletePo(mainJob)).thenReturn(1);
        when(shmJobServiceTest.getNeJobIdsForMainJob(mainJobId)).thenReturn(Arrays.asList(neJobId));
        //when(jobDeletionServiceTest.deleteJobs(Arrays.asList(activityJob)).thenReturn(futureboolean);

        final Job jobsDeletionAttributes = new Job();
        jobsDeletionAttributes.setJobName(jobName);
        jobsDeletionAttributes.setJobType(jobType);
        jobsDeletionAttributes.setMainJobId(mainJobId);
        final int actualCountOfJobsDeleted = objectUnderTest.deleteJobHierarchyWithoutJobTemplate(jobsDeletionAttributes);

        verify(liveBucket, never()).deletePo(activityJob);
        verify(liveBucket, never()).deletePo(neJob);
        verify(liveBucket, times(1)).deletePo(mainJob);
        verify(liveBucket, never()).deletePo(jobTemplate);
        assertEquals(1, actualCountOfJobsDeleted);
    }

    /**
     * Test Scenario : Main Job Not found
     */
    @Test
    public void testDeleteJobHierarchyWithoutJobTemplate2() {
        final long mainJobId = 1;
        final long neJobId = 45676;
        final long activityJobId = 2333;
        final String jobName = "jobName";
        final String jobType = JobType.UPGRADE.toString();

        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo("attributeName", "attrbuteValue")).thenReturn(restriction);
        when(queryExecutor.execute(any(Query.class))).thenReturn(iteratorMock);
        when(iteratorMock.hasNext()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
        when(iteratorMock.next()).thenReturn(neJob).thenReturn(activityJob);

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        when(neJob.getPoId()).thenReturn(neJobId);
        when(neJob.getAttribute(ShmConstants.NE_NAME)).thenReturn("NodeName");

        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityJob.getPoId()).thenReturn(activityJobId);
        when(activityJob.getAttribute(ShmConstants.ACTIVITY_NAME)).thenReturn("install");
        when(activityJob.getAttribute(ShmConstants.STATE)).thenReturn("COMPLETED");

        when(liveBucket.findPoById(mainJobId)).thenReturn(null);
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.deletePo(activityJob)).thenReturn(1);
        when(liveBucket.deletePo(neJob)).thenReturn(1);

        final Job jobsDeletionAttributes = new Job();
        jobsDeletionAttributes.setJobName(jobName);
        jobsDeletionAttributes.setJobType(jobType);
        jobsDeletionAttributes.setMainJobId(mainJobId);
        final int actualCountOfJobsDeleted = objectUnderTest.deleteJobHierarchyWithoutJobTemplate(jobsDeletionAttributes);

        //verify(liveBucket, times(1)).deletePo(activityJob);
        //verify(liveBucket, times(1)).deletePo(neJob);
        verify(liveBucket, never()).deletePo(mainJob);
        verify(liveBucket, never()).deletePo(jobTemplate);
        assertEquals(0, actualCountOfJobsDeleted);
    }

    @Test
    public void testDeleteJobHierarchyWithJobTemplate1() {
        final long jobTemplateId = 23;
        final long mainJobId = 1;
        final long neJobId = 45676;
        final long activityJobId = 2333;
        final String jobName = "jobName";
        final String jobType = JobType.UPGRADE.toString();
        final List<Long> neJobPoIds = new ArrayList<>();
        neJobPoIds.add(1234L);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo("attributeName", "attrbuteValue")).thenReturn(restriction);
        when(queryExecutor.execute(any(Query.class))).thenReturn(iteratorMock);
        when(iteratorMock.hasNext()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
        when(iteratorMock.next()).thenReturn(neJob).thenReturn(activityJob);
        when(jobDeletionServiceTest.deleteActivityAndNeJobAndReturnStatus(Matchers.anyLong(), Matchers.anyString())).thenReturn(true);
        when(jobDeletionServiceTest.getJobPoIdsFromJobId(ShmConstants.NE_JOB, ShmConstants.MAIN_JOB_ID, mainJobId)).thenReturn(neJobPoIds);
        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        when(liveBucket.findPoById(Matchers.anyLong())).thenReturn(mainJob);
        when(neJob.getPoId()).thenReturn(neJobId);
        when(neJob.getAttribute(ShmConstants.NE_NAME)).thenReturn("NodeName");

        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityJob.getPoId()).thenReturn(activityJobId);
        when(activityJob.getAttribute(ShmConstants.ACTIVITY_NAME)).thenReturn("install");
        when(activityJob.getAttribute(ShmConstants.STATE)).thenReturn("COMPLETED");

        when(liveBucket.findPoById(mainJobId)).thenReturn(mainJob);
        when(liveBucket.findPoById(jobTemplateId)).thenReturn(jobTemplate);
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);

        when(liveBucket.deletePo(activityJob)).thenReturn(1);
        when(liveBucket.deletePo(neJob)).thenReturn(1);
        when(liveBucket.deletePo(mainJob)).thenReturn(1);
        when(liveBucket.deletePo(jobTemplate)).thenReturn(1);

        final Job jobsDeletionAttributes = new Job();
        jobsDeletionAttributes.setJobName(jobName);
        jobsDeletionAttributes.setJobType(jobType);
        jobsDeletionAttributes.setMainJobId(mainJobId);
        jobsDeletionAttributes.setJobTemplateId(jobTemplateId);

        final int actualCountOfJobsDeleted = objectUnderTest.deleteJobHierarchyWithJobTemplate(jobsDeletionAttributes);
        verify(liveBucket, times(1)).deletePo(mainJob);
        verify(liveBucket, times(1)).deletePo(jobTemplate);
        assertEquals(1, actualCountOfJobsDeleted);

    }

    @Test
    public void testDeleteJobHierarchyWithJobTypeNHC() {
        final long jobTemplateId = 23;
        final long mainJobId = 1;
        final long neJobId = 45676;
        final long activityJobId = 2333;
        final String jobName = "jobName";
        final String jobType = JobType.NODE_HEALTH_CHECK.toString();
        final List<Long> neJobPoIds = new ArrayList<>();
        neJobPoIds.add(1234L);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo("attributeName", "attrbuteValue")).thenReturn(restriction);
        when(queryExecutor.execute(any(Query.class))).thenReturn(iteratorMock);
        when(iteratorMock.hasNext()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
        when(iteratorMock.next()).thenReturn(neJob).thenReturn(activityJob);
        when(jobDeletionServiceTest.deleteActivityAndNeJobAndReturnStatus(Matchers.anyLong(), Matchers.anyString())).thenReturn(true);
        when(jobDeletionServiceTest.getJobPoIdsFromJobId(ShmConstants.NE_JOB, ShmConstants.MAIN_JOB_ID, mainJobId)).thenReturn(neJobPoIds);
        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        when(liveBucket.findPoById(Matchers.anyLong())).thenReturn(mainJob);
        when(neJob.getPoId()).thenReturn(neJobId);
        when(neJob.getAttribute(ShmConstants.NE_NAME)).thenReturn("NodeName");

        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityJob.getPoId()).thenReturn(activityJobId);
        when(activityJob.getAttribute(ShmConstants.ACTIVITY_NAME)).thenReturn("install");
        when(activityJob.getAttribute(ShmConstants.STATE)).thenReturn("COMPLETED");

        when(liveBucket.findPoById(mainJobId)).thenReturn(mainJob);
        when(liveBucket.findPoById(jobTemplateId)).thenReturn(jobTemplate);
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);

        when(liveBucket.deletePo(activityJob)).thenReturn(1);
        when(liveBucket.deletePo(neJob)).thenReturn(1);
        when(liveBucket.deletePo(mainJob)).thenReturn(1);
        when(liveBucket.deletePo(jobTemplate)).thenReturn(1);

        final Job jobsDeletionAttributes = new Job();
        jobsDeletionAttributes.setJobName(jobName);
        jobsDeletionAttributes.setJobType(jobType);
        jobsDeletionAttributes.setMainJobId(mainJobId);
        jobsDeletionAttributes.setJobTemplateId(jobTemplateId);
        
        final int actualCountOfJobsDeleted = objectUnderTest.deleteJobHierarchyWithJobTemplate(jobsDeletionAttributes);
        verify(liveBucket, times(1)).deletePo(mainJob);
        verify(liveBucket, times(1)).deletePo(jobTemplate);
        assertEquals(1, actualCountOfJobsDeleted);

    }
    
    /**
     * Test Scenario : Job Template not found.
     */
    @Test
    public void testDeleteJobHierarchyWithJobTemplate2() {
        final long jobTemplateId = 23;
        final long mainJobId = 1;
        final long neJobId = 45676;
        final long activityJobId = 2333;
        final String jobName = "jobName";
        final String jobType = JobType.UPGRADE.toString();

        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo("attributeName", "attrbuteValue")).thenReturn(restriction);
        when(queryExecutor.execute(any(Query.class))).thenReturn(iteratorMock);
        when(iteratorMock.hasNext()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
        when(iteratorMock.next()).thenReturn(neJob).thenReturn(activityJob);

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        when(neJob.getPoId()).thenReturn(neJobId);
        when(neJob.getAttribute(ShmConstants.NE_NAME)).thenReturn("NodeName");

        final Map<String, Object> activityAttributesMap = new HashMap<String, Object>();
        activityAttributesMap.put(ShmConstants.NE_JOB_ID, neJobId);
        when(activityJob.getPoId()).thenReturn(activityJobId);
        when(activityJob.getAttribute(ShmConstants.ACTIVITY_NAME)).thenReturn("install");
        when(activityJob.getAttribute(ShmConstants.STATE)).thenReturn("COMPLETED");

        when(liveBucket.findPoById(mainJobId)).thenReturn(mainJob);
        when(liveBucket.findPoById(jobTemplateId)).thenReturn(null);
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);

        when(liveBucket.deletePo(activityJob)).thenReturn(1);
        when(liveBucket.deletePo(neJob)).thenReturn(1);
        when(liveBucket.deletePo(mainJob)).thenReturn(1);

        final Job jobsDeletionAttributes = new Job();
        jobsDeletionAttributes.setJobName(jobName);
        jobsDeletionAttributes.setJobType(jobType);
        jobsDeletionAttributes.setMainJobId(mainJobId);
        jobsDeletionAttributes.setJobTemplateId(jobTemplateId);

        final int actualCountOfJobsDeleted = objectUnderTest.deleteJobHierarchyWithJobTemplate(jobsDeletionAttributes);

        assertEquals(1, actualCountOfJobsDeleted);
        //verify(liveBucket, times(1)).deletePo(activityJob);
        //verify(liveBucket, times(1)).deletePo(neJob);
        verify(liveBucket, times(1)).deletePo(mainJob);
        verify(liveBucket, never()).deletePo(jobTemplate);
    }

    @Test
    public void testRetrieveJobDetails() {
        final long firstMainJobId = 1;
        final long secondMainJobId = 2;
        final long firstJobTemplateId = 3;
        final String firstJobState = JobState.COMPLETED.toString();
        final int firstExecutionIndex = 5;
        final Set<Long> mainJobIds = new HashSet<Long>();
        mainJobIds.add(firstMainJobId);
        mainJobIds.add(secondMainJobId);

        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.in(ObjectField.PO_ID, mainJobIds.toArray())).thenReturn(restriction);
        doNothing().when(typeQuery).setRestriction(restriction);

        final List<Object[]> jobProjection = new ArrayList<Object[]>();
        final Object[] firstJobObject = new Object[4];
        firstJobObject[0] = firstMainJobId;
        firstJobObject[1] = firstJobTemplateId;
        firstJobObject[2] = firstJobState;
        firstJobObject[3] = firstExecutionIndex;
        jobProjection.add(firstJobObject);
        when(queryExecutor.executeProjection(Matchers.any(Query.class), Matchers.any(Projection.class), Matchers.any(Projection.class), Matchers.any(Projection.class), Matchers.any(Projection.class)))
                .thenReturn(jobProjection);
        final List<Object[]> actualJobProjection = objectUnderTest.retrieveJobDetails(mainJobIds);
        assertEquals(jobProjection.size(), actualJobProjection.size());
    }

    @Test
    public void testFetchJobTemplateAttributes() {
        final long jobTemplateId = 1;
        final String jobName = "Some Job Name";
        final List<Long> jobTemplateIds = new ArrayList<Long>();
        jobTemplateIds.add(jobTemplateId);
        final List<PersistenceObject> jobTemplates = new ArrayList<PersistenceObject>();
        jobTemplates.add(jobTemplate);
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findPosByIds(jobTemplateIds)).thenReturn(jobTemplates);
        when(jobTemplate.getPoId()).thenReturn(jobTemplateId);
        final Map<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(ShmConstants.NAME, jobName);
        when(jobTemplate.getAllAttributes()).thenReturn(attributeMap);
        final List<Map<String, Object>> actualListOfJobTemplateAttributes = objectUnderTest.fetchJobTemplateAttributes(jobTemplateIds);

        assertEquals(1, actualListOfJobTemplateAttributes.size());
        assertEquals(jobTemplateId, actualListOfJobTemplateAttributes.get(0).get(ShmConstants.JOBTEMPLATEID));
        assertEquals(jobName, actualListOfJobTemplateAttributes.get(0).get(ShmConstants.NAME));
    }
}
