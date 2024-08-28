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

import java.util.*;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.enums.JobStateEnum;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * This class is responsible for deleting a job including its NE level jobs and activity jobs. If any one of the job deletion fails then whole job deletion will be reverted back.
 */
@Traceable
@Profiled
@Stateless
public class JobsDeletionService {

    private static final Logger logger = LoggerFactory.getLogger(JobsDeletionService.class);

    private static final String DELETING_MAIN_JOB = "Deleting %s job %s";
    private static final String MAIN_JOB_DELETED = "%s job %s deleted.";
    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private JobDeletionService jobDeletionService;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int deleteJobHierarchyWithoutJobTemplate(final Job jobsDeletionAttributes) {
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final int deleteJobStatus = deleteJobHierarchy(jobsDeletionAttributes, liveBucket);
        return deleteJobStatus;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int deleteJobHierarchyWithJobTemplate(final Job jobsDeletionAttributes) {
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final int deleteMainJobCount = deleteJobHierarchy(jobsDeletionAttributes, liveBucket);
        logger.info("deleteJobHierarchyWithJobTemplate-deleteJobStatus {} for mainJobId {}", deleteMainJobCount, jobsDeletionAttributes.getMainJobId());
        if (deleteMainJobCount > 0) {
            deleteJobTemplate(jobsDeletionAttributes, liveBucket);
            logger.info("deleteJobHierarchyWithJobTemplate-deleteJobStatus {} ", deleteMainJobCount, jobsDeletionAttributes.getMainJobId());
        }
        return deleteMainJobCount;
    }

    private int deleteJobHierarchy(final Job jobsDeletionAttributes, final DataBucket liveBucket) {
        int countOfJobsDeleted = 0;
        boolean deleteMainJob = true;
        final String jobName = jobsDeletionAttributes.getJobName();
        final String jobType = jobsDeletionAttributes.getJobType();
        final long mainJobId = jobsDeletionAttributes.getMainJobId();
        try {
            final List<Long> neJobPoIds = jobDeletionService.getJobPoIdsFromJobId(ShmConstants.NE_JOB, ShmConstants.MAIN_JOB_ID, mainJobId);
            logger.debug("Number of NE jobs are {} for main job {}", neJobPoIds.size(), mainJobId);
            for (final Long neJobPoId : neJobPoIds) {
                final boolean neJobDeletionStatus = jobDeletionService.deleteActivityAndNeJobAndReturnStatus(neJobPoId, jobType);

                if (!neJobDeletionStatus) {
                    deleteMainJob = false;
                }
            }

            //Delete Main Job
            if (deleteMainJob) {
                final PersistenceObject mainJob = findPOByPoId(mainJobId, liveBucket);
                final String mainJobLogBeforeDeletion = String.format(DELETING_MAIN_JOB, jobType, jobName);
                final String mainJobLogAfterDeletion = String.format(MAIN_JOB_DELETED, jobType, jobName);
                if (mainJob != null) {
                    logger.info(mainJobLogBeforeDeletion);
                    countOfJobsDeleted = deleteJobPo(mainJob, liveBucket);
                    logger.info(mainJobLogAfterDeletion);
                } else {
                    logger.warn("{} Job {} with id {} is not present in db.", jobsDeletionAttributes.getJobType(), jobsDeletionAttributes.getJobName(), jobsDeletionAttributes.getMainJobId());
                }
            }
        } catch (final Exception e) {
            logger.error("Exception Occured while fetching/delting MainjobId {} Reason {}", mainJobId, e);
        }
        return countOfJobsDeleted;
    }

    private void deleteJobTemplate(final Job jobsDeletionAttributes, final DataBucket liveBucket) {
        final String jobName = jobsDeletionAttributes.getJobName();
        final String jobType = jobsDeletionAttributes.getJobType();
        final long jobTemplateId = jobsDeletionAttributes.getJobTemplateId();
        //Delete Job Template
        try {
            final PersistenceObject jobTemplate = findPOByPoId(jobTemplateId, liveBucket);
            if (jobTemplate != null) {
                deleteJobPo(jobTemplate, liveBucket);
            } else {
                logger.warn("Job Template with id {} is not present in db for {} job {}.", jobsDeletionAttributes.getJobTemplateId(), jobsDeletionAttributes.getJobType(),
                        jobsDeletionAttributes.getJobName());
            }
        } catch (Exception ex) {
            logger.error("Exception occured while deleting JobTemplate having JobName %s jobType %s", jobName, jobType);
        }
    }

    private int deleteJobPo(final PersistenceObject jobPo, final DataBucket liveBucket) {
        return liveBucket.deletePo(jobPo);
    }

    public List<Object[]> retrieveJobDetails(final Set<Long> mainJobIds) {
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB);
        final Restriction restriction = typeQuery.getRestrictionBuilder().in(ObjectField.PO_ID, mainJobIds.toArray());
        typeQuery.setRestriction(restriction);
        final List<Object[]> jobProjection = queryExecutor.executeProjection(typeQuery, ProjectionBuilder.field(ObjectField.PO_ID), ProjectionBuilder.attribute(ShmConstants.JOB_TEMPLATE_ID),
                ProjectionBuilder.attribute(ShmConstants.STATE), ProjectionBuilder.attribute(ShmConstants.EXECUTIONINDEX));

        if (jobProjection != null && !jobProjection.isEmpty()) {
            return jobProjection;
        }
        return Collections.emptyList();

    }

    public List<Map<String, Object>> fetchJobTemplateAttributes(final List<Long> jobTemplateIds) {
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final List<Map<String, Object>> listOfJobTemplateAttributes = new ArrayList<Map<String, Object>>();
        final List<PersistenceObject> jobTemplates = findPOsByPoIds(jobTemplateIds, liveBucket);
        for (final PersistenceObject eachJobTemplate : jobTemplates) {
            final Map<String, Object> attributesOfEachJobTemplate = eachJobTemplate.getAllAttributes();
            attributesOfEachJobTemplate.put(ShmConstants.JOBTEMPLATEID, eachJobTemplate.getPoId());
            listOfJobTemplateAttributes.add(attributesOfEachJobTemplate);
        }
        return listOfJobTemplateAttributes;
    }

    private PersistenceObject findPOByPoId(final long poId, final DataBucket liveBucket) {
        return liveBucket.findPoById(poId);
    }

    private List<PersistenceObject> findPOsByPoIds(final List<Long> poIds, final DataBucket liveBucket) {
        return liveBucket.findPosByIds(poIds);
    }

    public JobsDeletionReport updateJobStatusAndGetJobDeletionReport(final List<Long> poIds) {
        final long startTime = System.currentTimeMillis();

        final JobsDeletionReport jobsDeletionReport = new JobsDeletionReport(poIds.size());
        try {
            final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
            final Set<Long> poIdsFailedForDeletion = new HashSet<Long>();
            final Set<Long> poIdsFailedForDeletion2 = new HashSet<Long>();
            for (final Long mainJobPoId : poIds) {
                final boolean isSetSucceded = setMainJobStatusToDeleting(jobsDeletionReport, mainJobPoId, liveBucket);
                if (!isSetSucceded) {
                    poIdsFailedForDeletion.add(mainJobPoId);
                }
            }
            logger.debug("poIds Failed For Jobs Deletion=={}", poIdsFailedForDeletion);
            for (int index = 0; index < ShmConstants.RETIESCOUNTFORFAILEDTODELETEJOBS; index++) {
                if (poIdsFailedForDeletion.size() > 0) {
                    for (Long failedMainJobPoId : poIdsFailedForDeletion) {
                        final boolean isSetSucceded = setMainJobStatusToDeleting(jobsDeletionReport, failedMainJobPoId, liveBucket);
                        if (!isSetSucceded) {
                            poIdsFailedForDeletion2.add(failedMainJobPoId);
                        }
                    }
                    poIdsFailedForDeletion.clear();
                    poIdsFailedForDeletion.addAll(poIdsFailedForDeletion2);

                } else {
                    break;
                }
            }
            logger.info("Final poIdsFailedForDeletion=={}", poIdsFailedForDeletion);
            jobsDeletionReport.setJobPoIdsFailedForDeletion(poIdsFailedForDeletion);
        } catch (final RuntimeException e) {
            logger.error("Failed to fetch jobPoIds : Reason {} ", e);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(e);
        }
        logger.info("Total time taken for updatation of job status to [DELETING] : {} miliseconds and failed job id are is :{}.", System.currentTimeMillis() - startTime,
                jobsDeletionReport.getJobPoIdsFailedForDeletion());
        return jobsDeletionReport;
    }

    /**
     * @param poIdsFailedForDeletion
     * @param jobsDeletionReport
     * @param mainJobPoId
     * @return
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean setMainJobStatusToDeleting(final JobsDeletionReport jobsDeletionReport, final Long mainJobPoId, final DataBucket liveBucket) {
        boolean isSetSucceded = false;
        try {
            final PersistenceObject mainJobPo = findPOByPoId(mainJobPoId, liveBucket);
            mainJobPo.setAttribute(ShmConstants.STATE, JobStateEnum.DELETING.name());
            logger.info("update job state as DELETING for the main job id : {} ", mainJobPoId);
            jobsDeletionReport.incrementJobsDeletedCount();
            isSetSucceded = true;
        } catch (final RuntimeException ex) {
            logger.error("Failed to update job state as DELETING for the main job id : {}. Reason : ", mainJobPoId, ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
        }
        return isSetSucceded;
    }
}
