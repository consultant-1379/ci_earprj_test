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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

/**
 * This class hold NE job static data and provides methods to get NE job cache data during activity execution.
 * 
 * @author tcsgusw
 * 
 */
@ApplicationScoped
public class NeJobStaticDataCache {

    private final Map<Long, NEJobStaticData> neJobStaticCache = new ConcurrentHashMap<Long, NEJobStaticData>();

    protected void put(final long activityJobId, final NEJobStaticData jobStaticContext) {
        neJobStaticCache.put(activityJobId, jobStaticContext);
    }

    protected NEJobStaticData get(final long activityJobId) {
        return neJobStaticCache.get(activityJobId);
    }

    protected void clear(final long activityJobId) {
        if (neJobStaticCache.get(activityJobId) != null) {
            neJobStaticCache.remove(activityJobId);
        }
    }

    protected void clearAll() {
        neJobStaticCache.clear();
    }
}
