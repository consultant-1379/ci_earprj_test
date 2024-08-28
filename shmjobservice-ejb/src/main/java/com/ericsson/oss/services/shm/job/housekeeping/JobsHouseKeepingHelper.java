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

import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.job.service.JobsDeletionReport;

/**
 * 
 * This class is used to delete the jobs one by one.
 * 
 * @author xsrakon
 * 
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class JobsHouseKeepingHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobsHouseKeepingHelper.class);

    @Inject
    private JobsHouseKeepingHelperUtil jobsHouseKepingServiceHelperUtil;

    public JobsHouseKeepingResponse deleteJobs(final String jobType, final List<Long> poIdsList) {

        LOGGER.info("HouseKeeping triggered for {} poids of {} type", poIdsList.size(), jobType);
        final JobsHouseKeepingResponse houseKeepingResponse = new JobsHouseKeepingResponse();
        int totalSuccessCount = 0;
        int totalFailureCount = 0;
        int totalJobsNotFoundCount = 0;

        //Converting Job Ids from String to Long.
        for (final Long poId : poIdsList) {
            //Passing PoId of Main Job one by one to delete
            try {
                final JobsDeletionReport jobsDeletionReport = jobsHouseKepingServiceHelperUtil.deleteJobPoOneByOneAndGetDeletionReport(poId);
                if (jobsDeletionReport.getJobsDeleted() > 0) {
                    totalSuccessCount++;
                } else if (jobsDeletionReport.getJobsDeletionFailed() > 0) {
                    totalFailureCount++;
                } else if (jobsDeletionReport.getJobsNotFound() > 0) {
                    totalJobsNotFoundCount++;
                }
            } catch (final Exception exception) {
                totalFailureCount++;
                LOGGER.error("Exception occured while deleting poId {}. Exception is {}", poId, exception);
            }
        }
        //Preparing HouseKeepingResponse for Specific JobType
        if (totalSuccessCount > 0) {
            houseKeepingResponse.setSuccessfullyDeletedJobsCount(totalSuccessCount);
        }
        if (totalFailureCount > 0) {
            houseKeepingResponse.setFailedToDeleteJobsCount(totalFailureCount);
        }
        if (totalJobsNotFoundCount > 0) {
            houseKeepingResponse.setJobsNotFoundCount(totalJobsNotFoundCount);
        }
        LOGGER.info("Successfully Deleted {} {} job/jobs,Failed To Delete {} {},{} Jobs Not Found to Delete {}", totalSuccessCount, jobType, totalFailureCount, jobType, jobType,
                totalJobsNotFoundCount);
        return houseKeepingResponse;
    }

    public JobsHouseKeepingResponse deleteJobs(final List<Long> poIdsList) {

        LOGGER.info("HouseKeeping triggered for {} main jobs.", poIdsList.size());
        final JobsHouseKeepingResponse houseKeepingResponse = new JobsHouseKeepingResponse();
        int totalSuccessCount = 0;
        int totalFailureCount = 0;
        int totalJobsNotFoundCount = 0;

        //Converting Job Ids from String to Long.
        for (final Long poId : poIdsList) {
            //Passing PoId of Main Job one by one to delete
            try {
                final JobsDeletionReport jobsDeletionReport = jobsHouseKepingServiceHelperUtil.deleteJobPoOneByOneAndGetDeletionReport(poId);
                if (jobsDeletionReport.getJobsDeleted() > 0) {
                    totalSuccessCount++;
                } else if (jobsDeletionReport.getJobsDeletionFailed() > 0) {
                    totalFailureCount++;
                } else if (jobsDeletionReport.getJobsNotFound() > 0) {
                    totalJobsNotFoundCount++;
                }
            } catch (final Exception exception) {
                totalFailureCount++;
                LOGGER.error("Exception occured while deleting poId {}. Exception is {}", poId, exception);
            }
        }
        //Preparing HouseKeepingResponse for Specific JobType
        if (totalSuccessCount > 0) {
            houseKeepingResponse.setSuccessfullyDeletedJobsCount(totalSuccessCount);
            LOGGER.info("Successfully Deleted {} job/jobs", totalSuccessCount);
        }
        if (totalFailureCount > 0) {
            houseKeepingResponse.setFailedToDeleteJobsCount(totalFailureCount);
            LOGGER.info("Failed To Delete {}", totalFailureCount);
        }
        if (totalJobsNotFoundCount > 0) {
            houseKeepingResponse.setJobsNotFoundCount(totalJobsNotFoundCount);
            LOGGER.info("{} Jobs Not Found to Delete", totalJobsNotFoundCount);
        }
        return houseKeepingResponse;
    }

}
