/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.activity.timeout.models;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * This interface is used to fetch the neType specific wait interval and retry attempt for precheck for each activity
 * 
 * 
 */
public interface PrecheckConfigurationProvider {

    /**
     * This method is used to get the wait interval before repeating precheck which will be used by workflows.
     * 
     * @return String
     * 
     */
    String getRepeatPrecheckWaitInterval(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName);

    /**
     * This method is used to get the wait interval before repeating precheck which will be used to display in the job logs.
     * 
     * @return Integer
     * 
     */
    Integer getPrecheckWaitInterval(final String neType, final String platform, final String jobType, final String activityName);

    /**
     * This method is used to get the retry attempt for repeating precheck which will be used to display in the job logs.
     * 
     * @return Integer
     * 
     */
    int getPrecheckRetryAttempt(final String neType, final String platform, final String jobType, final String activityName);
}
