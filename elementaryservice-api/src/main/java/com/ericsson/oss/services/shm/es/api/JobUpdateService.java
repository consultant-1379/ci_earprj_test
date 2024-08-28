/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
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

public interface JobUpdateService {

    void updateJobAttributes(final long jobId, final Map<String, Object> attrs);

    void updateJobAttributesWihoutRetries(final long jobId, final Map<String, Object> attrs);

    boolean updateRunningJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList);

    /**
     * This method updates the NE jobs completed count in Main JobProperty. Also it checks whether all NE Jobs are done.
     * 
     * @param jobId
     * @return boolean
     * 
     *         This method is no more needed, alternatively the same functionality is placed inside the method updateNEJobEndAttributes(...)
     */
    @Deprecated
    void updateNEJobsCompletedCount(long mainJobId);

    /**
     * @param neJobId
     */
    void updateNEJobEndAttributes(long neJobId);

    /**
     * This method will read and update the job properties in the same transaction.
     * 
     * @param jobId
     * @param jobPropertyList
     * @param jobLogList
     */

    /*
     * This method is no more needed, alternatively the same functionality is placed inside the method boolean readAndUpdateRunningJobAttributes(long jobId, List<Map<String, Object>> jobPropertyList,
     * List<Map<String, Object>> jobLogList, Double activityProgressPercentage)
     */
    @Deprecated
    boolean readAndUpdateRunningJobAttributes(long jobId, List<Map<String, Object>> jobPropertyList, List<Map<String, Object>> jobLogList);

    /**
     * This method will read and update the job properties based on activityProgressPercentage in the same transaction.
     * 
     * @param jobId
     * @param jobPropertyList
     * @param jobLogList
     * @param activityProgressPercentage
     */
    boolean readAndUpdateRunningJobAttributes(long jobId, List<Map<String, Object>> jobPropertyList, List<Map<String, Object>> jobLogList, Double activityProgressPercentage);

    /**
     * Adds the given Properties to the ActivityJobProperties , if they are not present. If already present then replaces the values to the specified keys.
     * 
     * 
     * @param jobId
     * @param propertyTobeAdded
     *            - Property Key - Value pair
     * @param jobLogList
     * @return Boolean - true , if the database transaction for the given query is successful otherwise false.
     */
    boolean addOrUpdateOrRemoveJobProperties(long jobId, Map<String, String> propertyTobeAdded, List<Map<String, Object>> jobLogList);

    /**
     * This method will read the job properties for the given Id with retries for Data Persistence Service.
     * 
     * @param jobId
     * @return all the attributes of the given persistence object's id.
     */
    Map<String, Object> retrieveJobWithRetry(long jobId);

    /**
     * This method will read and update the job properties in the same transaction. Irrespective of Result's existence in Activity Job Property, it will persist the items.
     * 
     * @param jobId
     * @param jobPropertyList
     * @param jobLogList
     */
    boolean readAndUpdateJobAttributesForCancel(long jobId, List<Map<String, Object>> jobPropertyList, List<Map<String, Object>> jobLogList);

    /**
     * @param jobId
     * @param attributes
     * @param noOfRetries
     */
    void updateJobAttributes(final long jobId, final Map<String, Object> attributes, final int noOfRetries);

    /**
     * This method will read, update and persist the job activity step duration in DPS.
     * 
     * @param jobId
     *            - The job ID.
     * @param stepNameAndDurationToPersist
     *            - The value to persist.
     * @param stepName
     *            - Name/identifier for the step.
     * @return True if stepNameAndDurationToPersist is persisted within noOfRetries, false otherwise.
     */
    boolean readAndUpdateStepDurations(final long jobId, final String stepNameAndDurationToPersist, final String stepName);

    void updateActivityAsSkipped(final long activityJobId);

}
