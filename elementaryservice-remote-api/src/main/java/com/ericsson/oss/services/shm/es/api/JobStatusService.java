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
package com.ericsson.oss.services.shm.es.api;

import java.util.List;
import java.util.Map;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;

@EService
@Remote
public interface JobStatusService {

    void updateJob(long poId, Map<String, Object> attrs);

    /**
     * update NE job start time as new Date() and state as specified;
     *
     * @param neJobId
     */
    void updateNeJobStart(long neJobId, JobState state);

    /**
     * update Activity job start time as new Date() and state as specified;
     *
     * @param activityJobId
     * @param state
     */
    void updateActivityJobStart(long activityJobId, JobState state);

    /* below 2 methods could be combined into one. 2 for now to keep it simple. */

    /**
     * update end date as new Date() and result by checking the individual activity results(=success if all activity are success). Job State should also be completed.
     *
     * @param jobId
     */
    void updateNEJobEnd(long jobId);

    /**
     * Updates the NE Job Progress.
     * 
     * @deprecated use bufferAndUpdateNEJobProgress as an alternative
     *
     * @param neJobId
     */
    @Deprecated
    void updateNEJobProgress(long neJobId);

    /**
     * Buffers the NE Job id and updates the NE Job Progress after the timer timeout. Since No Services should update the NE Job's progress directly. Callers: NHC
     * 
     * @param neJobId
     */
    void bufferAndUpdateNEJobProgress(long neJobId);

    /**
     *
     * @param jobId
     * @param jobPropertis
     * @param jobLogs
     * @param progressPercentage
     */
    void updateRunningJobAttributes(long jobId, List<Map<String, Object>> jobPropertis, List<Map<String, Object>> jobLogs, Double progressPercentage);

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

    void recordJobEndEvent(long jobId);

    void propagateCancelToMainJob(final long neJobId);

    void updateJobAsCancelled(final long neJobId, final Map<String, Object> attrs, final boolean isActivityJobUpdate);

    void createSkipJob(final long templateJobId, String executionMode);

    Boolean isExecuteCompleted(final long activityJobId);

    void abortActivity(long activityJobId);

    /**
     * This call will check NE job status , If it completed its execution then it stop proceeding execution otherwise it will proceeds for cancel
     *
     * @param neJobId
     * @return
     */

    boolean isNEJobProceedsForCancel(final long neJobId);

    void updateMainJobAsCancelled(final long mainJobId, Map<String, Object> mainJobAttributes);

    /**
     * This API will get the delay time between execute call initiated from WFS and actual action triggered on the node.
     *
     * @param activityJobId
     * @param neType
     * @param platform
     * @param jobType
     * @param activityName
     * @return
     */
    long getDelayInActionTriggeredTime(final long activityJobId, final int defaultActivityTimeout);

    /**
     * update Activity job start time as new Date() and state as specified in DPS and NeJobStaticData cache;
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

    /**
     * Checks whether MoAction is triggered or not, with the Action details from Job Properties and ClusterCache.
     * 
     * @param activityJobId
     * @return
     */
    boolean isMoActionTriggered(long activityJobId);

    /**
     * Gets the Modeled BestTimeout of activity using NEtype, platformType, jobTypeEnum and activityName.
     *
     * @param neType
     * @param platformType
     * @param jobType
     * @param activityName
     * @return
     */
    String getBestTimeout(final String neType, final String platformType, final String jobType, final String activityName);

    /**
     * This method will place a activity Started message in Topic.
     *
     * @param activityJobId
     * @param state
     * @param jobType
     * @param activityName
     */
    void keepActivityStartMessageInTopic(final long activityJobId, final JobState state, final String jobType, final String activityName);
}
