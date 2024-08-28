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
package com.ericsson.oss.services.shm.job.api;

import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.jobs.common.enums.HealthStatus;
import com.ericsson.oss.shm.job.entities.SHMJobData;

public interface JobConfigurationService {

    List<Map<String, String>> fetchJobProperty(final long jobId);

    Map<String, Object> retrieveJob(long jobId);

    Map<String, String> retrieveWorkflowAttributes(final long activityJobId);

    List<SHMJobData> getJobTemplateDetails(final List<SHMJobData> shmJobDataList);

    Map<String, Object> getActivitiesCount(final long neJobId);

    String retrieveActivityJobResult(final long neJobId);

    String retrieveNeJobResult(final long mainJobId);

    List<Map<String, Object>> retrieveJobs(List<Long> jobIds);

    List<Map<String, Object>> getProjectedAttributes(String namespace, String type, Map<Object, Object> restrictionAttributes, List<String> projectedAttributes);

    boolean persistRunningJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList);

    String getJobType(final long mainJobId);

    Map<String, Object> getJobDetailsToRaiseAlarm(final long mainJobId);

    /**
     * In future this should be used instead of persistRunningJobAttributes methid
     *
     * @param jobId
     * @param jobPropertyList
     * @param jobLogList
     * @param activityProgressPercentage
     */
    boolean readAndPersistRunningJobAttributes(long jobId, List<Map<String, Object>> jobPropertyList, List<Map<String, Object>> jobLogList, Double activityProgressPercentage);

    /**
     * Adds the given Properties to the ActivityJobProperties , if they are not present. If already present then replaces the values to the specified keys.
     *
     *
     * @param jobId
     * @param activityJobAttributes
     *            - Property Key - Value pair
     * @param propertyTobeAdded
     */
    void addOrUpdateOrRemoveJobProperties(long jobId, Map<String, String> propertyTobeAdded, List<Map<String, Object>> jobLogList);

    boolean readAndPersistJobAttributesForCancel(long jobId, List<Map<String, Object>> jobPropertyList, List<Map<String, Object>> jobLogList);

    /**
     * This call will check NE job status , If it completed its execution then it stop proceeding execution otherwise it will proceeds for cancel
     *
     * @param neJobId
     * @return
     */
    boolean isNEJobProceedsForCancel(final long neJobId);

    List<Map<String, Object>> getUpdatedJobProperties(final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> storedJobPropertiesList);

    /**
     *
     * @param neJobId
     * @return
     */
    Map<String, Object> retrieveWaitingActivityDetails(final long neJobId);

    /**
     *
     * @param jobId
     * @return
     */
    boolean isJobResultEvaluated(final long jobId);

    /**
     * @param neJobId
     * @return
     */
    Map<String, Object> getActivitiesCountAndPercentage(long neJobId);

    /**
     * This method will try to read, update and persist step duration in DPS.
     *
     * @param activityID
     *            - ID for the Activity.
     * @param existingStepDurations
     *            - The value to persist.
     * @param stepName
     *            - Name/identifier for the step.
     * @return True if data is persisted, false otherwise.
     */
    boolean readAndPersistRunningJobStepDuration(final long activityID, final String stepNameAndDurationToPersist, final String stepName);

    /**
     * Get NE job IDs corresponding to the mainJobPoId.
     *
     * @param mainJobPoId
     *            - The main job identifier.
     * @return {@link List} of network element job IDs.
     */
    List<Long> getNeJobIDs(final long mainJobPoId);

    String getJobCategory(final long mainJobId);

    Map<HealthStatus, Integer> getNodesByHealthStatus(final long mainJobId);

    String getReportCategory(final Long mainJobId);

    List<String> convertStringToList(final String strToConvert);

    List<Long> getJobPoIdsFromParentJobId(final long neJobPoId, final String typeOfJob, final String restrictionAttribute);

    /**
     * Return activityJob attributes by neJobId
     * 
     * @param neJobId
     * @param restrictions
     * @return
     */

    List<Map<String, Object>> getActivityAttributesByNeJobId(final long neJobId, final Map<String, Object> restrictions);

}
