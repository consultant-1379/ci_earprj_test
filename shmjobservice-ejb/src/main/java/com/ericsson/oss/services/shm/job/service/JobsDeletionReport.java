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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

public class JobsDeletionReport {

    private static final Logger logger = LoggerFactory.getLogger(JobsDeletionReport.class);

    private static final String NO_JOBS = "No jobs selected for deletion.";
    private static final String NOT_FOUND = "%d job(s) not found.";
    private static final String SUCCESS = "%d job(s) submitted for deletion.";
    private static final String FAILED = "%d job(s) failed to delete.";
    private static final String SKIPPED_ACTIVE = "%d job(s) is/are still active.";

    private int jobsToBeDeleted;
    private int jobsDeleted;
    private int jobsFailedToDelete;
    private int jobsNotFound;
    private int activeJobs;
    private String failedJobIds;
    private boolean isDatabaseDown;
    private Set<Long> jobPoIdsFailedForDeletion;

    public JobsDeletionReport(final int jobsToBeDeleted) {
        this.jobsToBeDeleted = jobsToBeDeleted;
        this.jobsDeleted = 0;
        this.jobsFailedToDelete = 0;
        this.jobsNotFound = 0;
        this.activeJobs = 0;
        this.failedJobIds = "Failed Jobs are : ";
    }

    public int getJobsToBeDeleted() {
        return jobsToBeDeleted;
    }

    public int getJobsDeleted() {
        return jobsDeleted;
    }

    public int getJobsDeletionFailed() {
        return jobsFailedToDelete;
    }

    public int getJobsNotFound() {
        return jobsNotFound;
    }

    public int getActiveJobs() {
        return activeJobs;
    }

    public String getFailedJobIds() {
        return failedJobIds.trim();
    }

    public void incrementJobsDeletedCount() {
        this.jobsDeleted = this.jobsDeleted + 1;
    }

    public void incrementfailedJobsDeletionCount() {
        this.jobsFailedToDelete = this.jobsFailedToDelete + 1;
    }

    public void decrementJobsToDeleteCount() {
        this.jobsToBeDeleted = this.jobsToBeDeleted - 1;
    }

    public void incrementJobsNotFoundCount(final int countOfJobsNotFound) {
        this.jobsNotFound = this.jobsNotFound + countOfJobsNotFound;
    }

    public void incrementActiveJobsCount() {
        this.activeJobs = this.activeJobs + 1;
    }

    public void setFailedJobIds(final String failedJobIds) {
        this.failedJobIds = failedJobIds;
    }

    public boolean isDatabaseDown() {
        return isDatabaseDown;
    }

    public void setDatabaseDown(final boolean isDatabaseDown) {
        this.isDatabaseDown = isDatabaseDown;
    }

    public Set<Long> getJobPoIdsFailedForDeletion() {
        return jobPoIdsFailedForDeletion;
    }

    public void setJobPoIdsFailedForDeletion(final Set<Long> jobPoIdsFailedForDeletion) {
        this.jobPoIdsFailedForDeletion = jobPoIdsFailedForDeletion;
    }

    public Map<String, String> generateResponseForUser() {
        logger.debug("Delete Job : generateResponseForUser() entry: JobsToBeDeleted {}, JobsDeleted {}, DeleteFailed {}, JobsNotFound {} and isDatabaseDown {}, activeJobs {}", jobsToBeDeleted,
                jobsDeleted, jobsFailedToDelete, jobsNotFound, isDatabaseDown, activeJobs);
        final Map<String, String> response = new HashMap<String, String>();
        String status = ShmConstants.SUCCESS;
        final StringBuilder message = new StringBuilder();

        if (jobsToBeDeleted == 0) {
            message.append(NO_JOBS);
        } else {
            if (jobsDeleted > 0) {
                message.append(String.format(SUCCESS, jobsDeleted));
                status = ShmConstants.SUCCESS;
            }
            if (jobsFailedToDelete > 0) {
                message.append(String.format(FAILED, jobsFailedToDelete));
                status = ShmConstants.ERROR;
            }
            if (jobsNotFound > 0) {
                message.append(String.format(NOT_FOUND, jobsNotFound));
            }
            if (activeJobs > 0) {
                message.append(String.format(SKIPPED_ACTIVE, activeJobs));
            }
        }

        if (isDatabaseDown) {
            message.append(ShmCommonConstants.DATABASE_SERVICE_NOT_AVAILABE);
        }

        if (isDatabaseDown || jobsFailedToDelete > 0 || activeJobs > 0) {
            status = ShmConstants.ERROR;
        }

        response.put(ShmConstants.MESSAGE, message.toString());
        response.put(ShmConstants.STATUS, status);
        logger.debug("Delete Job : generateResponseForUser() exit: Response {}", response);
        return response;
    }

}
