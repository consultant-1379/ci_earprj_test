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
package com.ericsson.oss.services.shm.es.polling;

import java.util.Map;

import javax.inject.Inject;

/**
 * This class is used to delegate the calls to PollingActivitymanager with the response received.
 * 
 * @author tcssbop
 */
public class PollingActivityCallbackManager {

    @Inject
    private PollingActivityManager pollingActivityManager;

    /**
     * This method updates the given attributes for the poll entry with the given activityJobId.
     * 
     * @param activityJobId
     * @param attributes
     */
    public void updatePollingAttributesByActivityJobId(final long activityJobId, final Map<String, Object> attributes) {
        pollingActivityManager.updatePollingAttributesByActivityJobId(activityJobId, attributes);
    }
}
