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
package com.ericsson.oss.services.shm.job.remote.api;

import java.util.List;
import java.util.Map;

/**
 * To provide the NeTypeProperties
 * 
 * @author xnagvar
 * 
 */

public interface NeTypePropertiesProvider {

    /**
     * Provides the NeTypeProperties
     * 
     * @param activityProperties
     * @return
     */
    List<Map<String, Object>> getNeTypeProperties(List<String> activityProperties, ShmRemoteJobData shmRemoteJobData);

}
