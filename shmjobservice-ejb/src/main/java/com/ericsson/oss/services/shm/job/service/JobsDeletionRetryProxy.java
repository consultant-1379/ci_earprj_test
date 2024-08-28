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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;

public class JobsDeletionRetryProxy {

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private JobsDeletionService jobsDeletionService;

    public List<Object[]> retrieveJobDetails(final Set<Long> mainJobIds) {
        try {
            final List<Object[]> jobDetails = retryManager.executeCommand(dpsRetryPolicies.getOptimisticLockRetryPolicy(), new ShmDpsRetriableCommand<List<Object[]>>() {
                @Override
                public List<Object[]> execute() {
                    return jobsDeletionService.retrieveJobDetails(mainJobIds);
                }
            });
            return jobDetails;
        } catch (final RetriableCommandException ex) {
            throw ex;
        }

    }

    public List<Map<String, Object>> fetchJobTemplateAttributes(final List<Long> jobTemplateIds) {
        try {
            final List<Map<String, Object>> jobTemplateAttributes = retryManager.executeCommand(dpsRetryPolicies.getOptimisticLockRetryPolicy(),
                    new ShmDpsRetriableCommand<List<Map<String, Object>>>() {
                        @Override
                        public List<Map<String, Object>> execute() {
                            return jobsDeletionService.fetchJobTemplateAttributes(jobTemplateIds);
                        }
                    });
            return jobTemplateAttributes;
        } catch (final RetriableCommandException ex) {
            throw ex;
        }
    }

    public int deleteJobHierarchyWithoutJobTemplate(final Job jobsDeletionAttributes) {
        try {
            final int deleteJobStatus = retryManager.executeCommand(dpsRetryPolicies.getOptimisticLockRetryPolicy(), new ShmDpsRetriableCommand<Integer>() {
                @Override
                public Integer execute() {
                    return jobsDeletionService.deleteJobHierarchyWithoutJobTemplate(jobsDeletionAttributes);
                }
            });
            return deleteJobStatus;
        } catch (final RetriableCommandException ex) {
            throw ex;
        }
    }

    public int deleteJobHierarchyWithJobTemplate(final Job jobsDeletionAttributes) {
        try {
            final int deleteJobStatus = retryManager.executeCommand(dpsRetryPolicies.getOptimisticLockRetryPolicy(), new ShmDpsRetriableCommand<Integer>() {
                @Override
                public Integer execute() {
                    return jobsDeletionService.deleteJobHierarchyWithJobTemplate(jobsDeletionAttributes);
                }
            });
            return deleteJobStatus;
        } catch (final RetriableCommandException ex) {
            throw ex;
        }
    }

    public JobsDeletionReport updateJobStatusAndGetJobDeletionReport(final List<Long> poIds) {
        try {
            final JobsDeletionReport jobsDeletionReport = retryManager.executeCommand(dpsRetryPolicies.getOptimisticLockRetryPolicy(), new ShmDpsRetriableCommand<JobsDeletionReport>() {
                @Override
                public JobsDeletionReport execute() {
                    return jobsDeletionService.updateJobStatusAndGetJobDeletionReport(poIds);
                }
            });
            return jobsDeletionReport;
        } catch (final RetriableCommandException ex) {
            throw ex;
        }
    }
}
