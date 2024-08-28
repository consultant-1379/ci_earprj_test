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
package com.ericsson.oss.services.shm.job.housekeeping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.SortDirection;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.job.service.Job;
import com.ericsson.oss.services.shm.job.service.JobParameterChangeListener;
import com.ericsson.oss.services.shm.job.service.JobsDeletionReport;
import com.ericsson.oss.services.shm.job.service.SHMJobServiceHelper;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class JobsHouseKeepingHelperUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobsHouseKeepingHelperUtil.class);

    @Inject
    private SHMJobServiceHelper shmJobServiceHelper;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Inject
    private SystemRecorder systemRecorder;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private JobParameterChangeListener jobParameterChangeListener;

    public JobsDeletionReport deleteJobPoOneByOneAndGetDeletionReport(final Long poId) {

        final Set<Long> poIdSet = new HashSet<Long>();
        poIdSet.add(poId);
        final int jobsToBeDeleted = poIdSet.size();
        //Initializing JobsDeletionReport
        final JobsDeletionReport jobsDeletionReport = new JobsDeletionReport(jobsToBeDeleted);
        //Fetching attributes of MainJob and JobTemplate which are required during deletion of jobs.
        final Map<Long, List<Job>> jobDetailsForDeletion = shmJobServiceHelper.fetchJobDetails(poIdSet, jobsDeletionReport);
        if (jobDetailsForDeletion.isEmpty()) {
            return jobsDeletionReport;
        }
        final List<Job> listOfJobDetailsForDeletion = shmJobServiceHelper.extractListOfJobs(jobDetailsForDeletion);
        for (final Job job : listOfJobDetailsForDeletion) {
            final int countOfJobDeleted = shmJobServiceHelper.deleteJobHirerachy(job);
            if (countOfJobDeleted > 0) {
                systemRecorder.recordEvent(SHMEvents.JOB_DELETED_SUCCESSFULLY, EventLevel.COARSE, job.getJobName(), job.getJobType(), "Execution Index : " + job.getExecutionIndex());
                jobsDeletionReport.incrementJobsDeletedCount();
                LOGGER.debug("{} deleted Successfully", job.getJobName());
            }
        }
        return jobsDeletionReport;
    }

    public List<Object[]> fetchJobTypeSpecificPoIdsByCount(final String jobType) {
        final List<Object[]> poIds = new ArrayList<Object[]>();
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final List<Long> jobTypeTemplatePoIds = getJobTemplateIds(jobType, liveBucket);
        if (jobTypeTemplatePoIds != null && !jobTypeTemplatePoIds.isEmpty()) {
            final List<List<Long>> batchedTemplatePoIds = ListUtils.partition(jobTypeTemplatePoIds, jobParameterChangeListener.getJobBatchSize());
            for (final List<Long> eachBatchOfTemplatePoIds : batchedTemplatePoIds) {
                final List<Object[]> jobPoIds = fetchJobPoIds(eachBatchOfTemplatePoIds, liveBucket, jobType);
                poIds.addAll(jobPoIds);
            }
        }
        return poIds;
    }

    public List<Long> fetchJobTypeSpecificPoIdsByAge(final String jobType, final int maxJobAge) {
        final List<Long> poIds = new ArrayList<Long>();
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final List<Long> jobTypeTemplatePoIds = getJobTemplateIds(jobType, liveBucket);
        if (jobTypeTemplatePoIds != null && !jobTypeTemplatePoIds.isEmpty()) {
            final List<List<Long>> batchedTemplatePoIds = ListUtils.partition(jobTypeTemplatePoIds, jobParameterChangeListener.getJobBatchSize());
            for (final List<Long> eachBatchOfTemplatePoIds : batchedTemplatePoIds) {
                final List<Long> jobPoIds = fetchPoIdsForOldJobs(liveBucket, eachBatchOfTemplatePoIds, maxJobAge);
                poIds.addAll(jobPoIds);
            }
        }
        return poIds;
    }

    private List<Object[]> fetchJobPoIds(final List<Long> jobTemplatePoIds, final DataBucket liveBucket, final String jobType) {
        List<Object[]> databaseEntries = new ArrayList<Object[]>();
        boolean databaseIsDown = false;
        try {
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB);
            final Restriction queryRestriction = query.getRestrictionBuilder().in(ShmConstants.JOB_TEMPLATE_ID, jobTemplatePoIds.toArray());

            query.setRestriction(queryRestriction);
            query.addSortingOrder(ShmConstants.ENDTIME, SortDirection.ASCENDING);
            final Projection projectionAttributes[] = { ProjectionBuilder.attribute(ShmConstants.ENDTIME) };

            databaseEntries = queryExecutor.executeProjection(query, ProjectionBuilder.field(ObjectField.PO_ID), projectionAttributes);

            if (databaseEntries != null && !databaseEntries.isEmpty()) {
                LOGGER.info("HouseKeeping of poIds Based On Count {}", databaseEntries.size());
                return databaseEntries;
            }
        } catch (final RuntimeException runtimeException) {
            databaseIsDown = dpsAvailabilityInfoProvider.isDatabaseDown();
            LOGGER.error("Exception while fetching Job PoIds Based on Count-Reason {} ,Checking is Database Down: {}", runtimeException, databaseIsDown);
            throw new ServerInternalException("Exception Ocuured while fetching MainJobPoIds Based on Count for [{}] Job " + jobType);
        }

        return databaseEntries;
    }

    private List<Long> getJobTemplateIds(final String jobType, final DataBucket liveBucket) {
        boolean databaseIsDown = false;
        LOGGER.debug("Fetching {} JobTemplatePoIds", jobType);
        List<Long> poIds = new ArrayList<Long>();
        try {
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE);
            final Restriction queryRestriction = query.getRestrictionBuilder().equalTo(ShmConstants.JOB_TYPE, jobType);
            query.setRestriction(queryRestriction);
            poIds = queryExecutor.executeProjection(query, ProjectionBuilder.field(ObjectField.PO_ID));
            if (poIds != null && !poIds.isEmpty()) {
                return poIds;
            }
        } catch (final RuntimeException runtimeException) {
            databaseIsDown = dpsAvailabilityInfoProvider.isDatabaseDown();
            LOGGER.error("Exception while fetching Template Ids-Reason {} ,Checking is Database Down: {}", runtimeException, databaseIsDown);
            throw new ServerInternalException("Exception Ocuured while fetching JobTemplateIds for [{}] Job " + jobType);
        }
        return poIds;
    }

    private List<Long> fetchPoIdsForOldJobs(final DataBucket liveBucket, final List<Long> jobTypeTemplatePoIds, final int maxJobAge) {
        List<Long> poIdsBasedOnAge = new ArrayList<Long>();
        boolean databaseIsDown = false;
        try {
            LOGGER.info("{} TemplateIds  passed to Age Query for", jobTypeTemplatePoIds.size());
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB);
            final TypeRestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
            final Restriction templateIdsRestriction = restrictionBuilder.in(ShmConstants.JOB_TEMPLATE_ID, jobTypeTemplatePoIds.toArray());
            final Restriction endTimeRestriction = restrictionBuilder.lessThan(ShmConstants.ENDTIME, new DateTime().minusDays(maxJobAge).toDate());
            final Restriction restriction = restrictionBuilder.allOf(templateIdsRestriction, endTimeRestriction);
            query.setRestriction(restriction);
            poIdsBasedOnAge = queryExecutor.executeProjection(query, ProjectionBuilder.field(ObjectField.PO_ID));
            if (poIdsBasedOnAge != null && !poIdsBasedOnAge.isEmpty()) {
                return poIdsBasedOnAge;
            }
        } catch (final RuntimeException runtimeException) {
            databaseIsDown = dpsAvailabilityInfoProvider.isDatabaseDown();
            LOGGER.error("Exception while fetching Job PoIds Based on Age-Reason {} ,Checking is Database Down: {}", runtimeException, databaseIsDown);
            throw new ServerInternalException("Exception Ocuured while fetching JobTemplateIds based on age");
        }
        return poIdsBasedOnAge;
    }

    public List<Long> fetchJobsInDeletingStatus() {
        List<Long> mainJobIdsInDeletingState = new ArrayList<Long>();
        boolean databaseIsDown = false;

        try {
            final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB);
            final Restriction queryRestriction = query.getRestrictionBuilder().equalTo(ShmConstants.STATE, ShmConstants.DELETING);
            query.setRestriction(queryRestriction);
            mainJobIdsInDeletingState = queryExecutor.executeProjection(query, ProjectionBuilder.field(ObjectField.PO_ID));
            if (mainJobIdsInDeletingState != null && !mainJobIdsInDeletingState.isEmpty()) {
                return mainJobIdsInDeletingState;
            }
        } catch (final RuntimeException runtimeException) {
            databaseIsDown = dpsAvailabilityInfoProvider.isDatabaseDown();
            LOGGER.error("Exception while fetching Job PoIds Based on job state {} ,Checking if Database is Down: {}", runtimeException, databaseIsDown);
            throw new ServerInternalException("Exception Ocuured while fetching JobTemplateIds based on job state.");
        }
        return mainJobIdsInDeletingState;
    }
}
