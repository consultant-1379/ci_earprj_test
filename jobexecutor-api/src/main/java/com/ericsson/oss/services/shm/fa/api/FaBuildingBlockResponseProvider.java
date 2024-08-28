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
package com.ericsson.oss.services.shm.fa.api;

import java.util.Map;

import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;

public interface FaBuildingBlockResponseProvider {

    /**
     * Method to send job status to building blocks.
     */
    void send(final Map<String, Object> response, final JobCategory jobCategory);

    /**
     * Method to send updated activityTimeout to building blocks based on the activities selected from FA .
     */
    void sendUpdatedActivityTimeout(final long activityJobId, final NEJobStaticData neJobStaticData, final Map<String, Object> activityJobAttributes, final String activityName,
            final int numberOfActivities);

}
