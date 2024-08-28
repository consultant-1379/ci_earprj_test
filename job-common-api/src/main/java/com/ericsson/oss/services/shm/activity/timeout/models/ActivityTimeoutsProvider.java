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
package com.ericsson.oss.services.shm.activity.timeout.models;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * This interface is used to fetch the neType specific timeout values for each activity
 * 
 * @author xsrabop
 * 
 */
public interface ActivityTimeoutsProvider {

    /**
     * This method is used to get the activity timeout value which will be used by work flows
     * 
     * @return String
     * 
     */
    String getActivityTimeout(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName);

    /**
     * This method is used to get the activity timeout value which will be used to display in the job logs
     * 
     * @return Integer
     * 
     */
    Integer getActivityTimeoutAsInteger(final String neType, final String platform, final String jobType, final String activityName);

    /**
     * @param neType
     * @param platform
     * @param jobType
     * @param activityName
     * @return
     */
    String getActivityPollWaitTime(final String neType, final PlatformTypeEnum platformTypeEnum, final JobTypeEnum jobTypeEnum, final String activityName);

    /**
     * @param neType
     * @param platform
     * @param jobType
     * @param activityName
     * @return
     */
    Integer getActivityPollWaitTimeAsInteger(final String neType, final String platform, final String jobType, final String activityName);

    /**
     * This method is used to get the activity Polling Wait Time value which will be used by work flows
     * 
     * @return String
     * 
     */

}
