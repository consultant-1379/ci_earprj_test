/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.nejob.cache;

import javax.inject.Inject;

/**
 * Provides a methods to get NE job static cache cleanup parameters to NeJobDataCacheCleanupTimer.
 * 
 * @author tcsgusw
 * 
 */

public class NeJobStaticDataConfigProvider {

    @Inject
    private NeJobStaticDataConfigListener neJobCacheConfigParamListener;

    public String getDailyScheduleTimeForNeCacheCleanup() {
        return neJobCacheConfigParamListener.getDailyScheduleTimeForNeCacheCleanup();
    }

}
