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
package com.ericsson.oss.services.shm.es.local;

import java.util.Map;

import javax.ejb.Local;

import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;

@Local
public interface JobStatusServiceLocal {

    void updateJob(long poId, Map<String, Object> attrs);

    /**
     * update state time as new Date(); and state as specified;
     * 
     * @param neJobId
     */
    void updateNeJobStart(long neJobId, JobState state);

    /**
     * This method will update the activity job state in DPS.
     * 
     * @param activityJobId
     * @param state
     */
    void updateActivityJobStart(long activityJobId, JobState state);

    /* below 2 methods could be combined into one. 2 for now to keep it simple. */

    /**
     * update end date as new Date() and result by checking the individual activity results(=success if all activity are success). Job State should also be completed.
     * 
     * 
     * @param jobId
     */
    void updateNEJobEnd(long jobId);

    /**
     * update end date as new Date() and result by reading 'result' from job property. Job State should also be completed.
     * 
     * @param jobId
     */
    ActivityStepResultEnum updateActivityJobEnd(long jobId);

    /**
     * update main job 'progress percentage','state','end date' and result.
     * 
     * @param mainjobId
     */
    void updateJobProgress(long mainjobId);

    /**
     * update activity job result as failed.
     * 
     * @param jobId
     */
    void updateActivityJobAsFailed(long jobId);

    void propagateCancelToMainJob(final long neJobId);

    /**
     * This call will check NE job status , If it completed its execution then it stop proceeding execution otherwise it will proceeds for cancel
     * 
     * @param neJobId
     * @return
     */

    boolean isNEJobProceedsForCancel(final long neJobId);

    /**
     * update Activity job start time as new Date() and state as specified in DPS and NeJobStaticData cache.
     * 
     * @param activityJobId
     * @param state
     * @param jobType
     */
    void updateActivityJobStart(final long activityJobId, final JobState state, final String jobType);

    /**
     * @param jobId
     * @param jobType
     * @return
     */
    ActivityStepResultEnum updateActivityJobEnd(final long jobId, final String jobType);

    /**
     * @param jobId
     * @param jobType
     */
    void updateActivityJobAsFailed(final long jobId, final String jobType);
}
