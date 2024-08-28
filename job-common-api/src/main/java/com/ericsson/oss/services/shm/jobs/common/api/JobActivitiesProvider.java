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
package com.ericsson.oss.services.shm.jobs.common.api;

import java.util.List;

import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;

/**
 * To provide the Activity Information
 * 
 * 
 * @author xrajeke
 * 
 */
public interface JobActivitiesProvider {

    /**
     * Provides the activity information for a the given request. It reads the cached xmls' data with the prepared key. Key will be prepared based on the query.
     * 
     * If default activity information needs to be retrieved, NeType must be empty or null.
     * 
     * @param jobActivitiesQueryList
     * @param jobType
     * @return
     */
    List<JobActivitiesResponse> getNeTypeActivities(List<JobActivitiesQuery> jobActivitiesQueryList);

    /**
     * Provides the response (activityname , order etc ) for given inputs.
     * 
     * @param ecim
     * @param backup
     * @return
     */
    List<ActivityInfo> getActivityInfo(String platformType, String neType, String jobType);

    /**
     * Fetch Activity params & NE params for the provided platformtype , netype & JobType.
     * 
     * @param platformType
     * @param neType
     * @param jobType
     * @return
     */
    List<String> getActivityProperties(final String platformType, final String neType, final String jobType);

}
