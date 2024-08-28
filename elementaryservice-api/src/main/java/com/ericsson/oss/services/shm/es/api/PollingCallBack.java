/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.api;

import java.util.Map;

import javax.ejb.Local;

/**
 * This interface is used to validate the response data from ClusteredCallbackNotificationQueue.
 * 
 * @author xsrabop
 * 
 */
@Local
public interface PollingCallBack extends ActivityCallback {

    /**
     * This will be called by ClusteredCallbackNotificationQueueListener for SHM MTR.
     * 
     * @param activityJobId
     *            Id for the activity
     * @param responseAttributes
     *            Response attributes
     */
    void processPollingResponse(final long activityJobId, final Map<String, Object> responseAttributes);

}
