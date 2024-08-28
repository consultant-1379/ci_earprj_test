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
package com.ericsson.oss.services.shm.es.polling.cache;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.polling.PollingActivityUtil;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityAttributes;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants;
import com.ericsson.oss.services.shm.model.ShmPollingActivityData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * This class provides the methods to add and remove data in the cache when DPS is not available
 * 
 * @author xprapav
 */
public class PollingActivityCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollingActivityCacheManager.class);

    @Inject
    private PollingActivityCacheProvider pollingActivityCacheProvider;

    @Inject
    private PollingActivityUtil pollingActivityUtil;

    public void addPollingActivityDataInCache(final Map<String, Object> pollingEntry) {
        if (pollingEntry != null && !pollingEntry.isEmpty()) {
            final long activityJobId = (long) pollingEntry.get(PollingActivityConstants.ACTIVITY_JOB_ID);
            ShmPollingActivityData shmPollingActivityData = null;
            if (pollingEntry.get(PollingActivityConstants.MO_FDN) != null) {
                shmPollingActivityData = pollingActivityUtil.getPollingActivityData(activityJobId, pollingEntry);
            } else {
                final Map<String, String> additionalInformation = (Map<String, String>) pollingEntry.get(PollingActivityConstants.ADDITIONAL_INFORMATION);
                shmPollingActivityData = new PollingActivityAttributes(activityJobId, ActivityConstants.EMPTY, ActivityConstants.EMPTY, ActivityConstants.EMPTY, Collections.<String> emptyList(),
                        additionalInformation, ActivityConstants.EMPTY);
            }
            LOGGER.info("Adding ShmPollingActivityData in Cache for activityJobId: {}", activityJobId);
            pollingActivityCacheProvider.add(activityJobId, shmPollingActivityData);
        }
    }

    public List<ShmPollingActivityData> getPollingEntriesFromCache() {
        LOGGER.debug("Getting ShmPollingActivityData from Cache");
        return pollingActivityCacheProvider.get();
    }

    public void removePollingEntriesFromCache(final long activityJobId) {
        LOGGER.debug("Removed ShmPollingActivityData from Cache with activityjobId: {}", activityJobId);
        pollingActivityCacheProvider.remove(activityJobId);
    }
}
