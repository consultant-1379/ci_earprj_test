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
import java.util.concurrent.Future;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;

/**
 * 
 * This class is used to delegate the delete request to JobsHouseKeepingHelper to delete the jobs.
 * 
 * @author xsrakon
 * 
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class JobsHouseKeepingDelegator {

    private static final Logger logger = LoggerFactory.getLogger(JobsHouseKeepingDelegator.class);

    @Inject
    JobsHouseKeepingHelper jobsHouseKeepingHelper;

    @Inject
    private SystemRecorder systemRecorder;

    /**
     * 
     * This method is delegating request to Helper to delete the jobs in Asynchronous way.
     * 
     */
    @Asynchronous
    public Future<JobsHouseKeepingResponse> houseKeepingOfJobs(final List<Long> jobPoIds, final String jobType) {
        final long startTime = System.currentTimeMillis();
        final JobsHouseKeepingResponse houseKeepingResponse = jobsHouseKeepingHelper.deleteJobs(jobType, jobPoIds);
        systemRecorder.recordEvent(SHMEvents.HOUSEKEEPING_COMPLETED_SUCCESSFULLY, EventLevel.COARSE, jobType, jobPoIds.toString(), "Time spent : " + (System.currentTimeMillis() - startTime));
        return new AsyncResult<JobsHouseKeepingResponse>(houseKeepingResponse);
    }

    /**
     * 
     * This method is delegating request to Helper to delete the jobs in Asynchronous way.it is used to delete jobs whose state is DELETING in housekeeping.
     * 
     */
    @Asynchronous
    public Future<JobsHouseKeepingResponse> houseKeepingOfJobs(final List<Long> jobPoIds) {
        final long startTime = System.currentTimeMillis();
        final JobsHouseKeepingResponse houseKeepingResponse = jobsHouseKeepingHelper.deleteJobs(jobPoIds);
        logger.info("Jobs housekeeping successfully completed for jobs STATE as :[DELETING] and total Time Spent:{}", (System.currentTimeMillis() - startTime));
        return new AsyncResult<JobsHouseKeepingResponse>(houseKeepingResponse);
    }
}
