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
package com.ericsson.oss.services.shm.es.polling;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityInfoProvider;
import com.ericsson.oss.services.shm.es.polling.api.ReadCallStatusEnum;

/**
 * This class will manages mainly the subscriptions and un-subscriptions activities for the polling mechanism.
 * 
 * @author tcsgusw
 * 
 */

@ApplicationScoped
public class PollingActivityStatusManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollingActivityStatusManager.class);

    private final Map<String, PollingActivityInfoProvider> pollingActivitiesCache = new ConcurrentHashMap<String, PollingActivityInfoProvider>();

    /**
     * This method will subscribe polling for the specified activity.
     * 
     * @param moFdn
     * @param activityJobId
     * @param pollingActivityInfoProvider
     */
    public void subscribeForPolling(final String moFdn, final JobActivityInfo jobActivityInfo, final int waitTimeToStartPolling) {
        final long activityJobId = jobActivityInfo.getActivityJobId();
        final String pollingCacheKey = getActivityPollingCacheKey(moFdn, activityJobId);

        final PollingActivityInfoProvider pollingActivityInfoProvider = getPollingActivityInfoProvider(moFdn, jobActivityInfo, waitTimeToStartPolling,
                ReadCallStatusEnum.NOT_TRIGGERED);
        pollingActivitiesCache.put(pollingCacheKey, pollingActivityInfoProvider);
        LOGGER.debug("Polling subscription successsful for the activity: {} with the CacheKey: {}", pollingActivityInfoProvider.getActivityName(), pollingCacheKey);
    }

    /**
     * This method is used to update the Polled Activity Information in the cache.
     * 
     * @param requestId
     */
    public void unsubscribeFromPolling(final String requestId) {
        LOGGER.debug("UnSubscribing from polling for the cacheKey: {}", requestId);
        pollingActivitiesCache.remove(requestId);
    }

    /**
     * This method is used to get the information stored in the cache based on the requestId.
     * 
     * @param requestId
     * @return PolledActivityInformation
     */
    private PollingActivityInfoProvider getPolledActivityInformation(final String requestId) {
        return pollingActivitiesCache.get(requestId);
    }

    /**
     * This will construct polled cache key by using moFdn and activityJob Id.
     * 
     * @param moFdn
     * @param activityJobId
     * @return PolledCacheKey
     */
    public String getActivityPollingCacheKey(final String moFdn, final long activityJobId) {
        return moFdn + "_" + activityJobId;
    }

    private Date getPollingStartTime(final int timeout) {
        final Date currentDate = new Date();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        cal.add(Calendar.MINUTE, timeout);
        final Date pollingStartTime = cal.getTime();
        return pollingStartTime;
    }

    private PollingActivityInfoProvider getPollingActivityInfoProvider(final String moFdn, final JobActivityInfo jobActivityInfo, final int timeout, final ReadCallStatusEnum readCallStatus) {

        final Date pollingStartTime = getPollingStartTime(timeout);
        final PollingActivityInfoProvider pollingActivityInfoProvider = new PollingActivityInfoProvider(jobActivityInfo.getActivityName(), jobActivityInfo.getJobType().name(), jobActivityInfo
                .getPlatform().name(), jobActivityInfo.getActivityJobId(), moFdn, pollingStartTime, readCallStatus);
        return pollingActivityInfoProvider;
    }

    public void updateReadCallStatus(final long activityJobId, final String upMoFdn, final ReadCallStatusEnum readCallStatus) {

        final String pollingCacheKey = getActivityPollingCacheKey(upMoFdn, activityJobId);
        final PollingActivityInfoProvider pollingActivityInfoProvider = getPolledActivityInformation(pollingCacheKey);
        if (pollingActivityInfoProvider != null) {
            pollingActivityInfoProvider.setReadCallStatus(readCallStatus);
            LOGGER.debug("Read call status updated to:{} successsfully for the activity: {} with the CacheKey: {}", readCallStatus, pollingActivityInfoProvider.getActivityName(), pollingCacheKey);
        } else {
            LOGGER.warn("Update of read call status to: {} failed, because local cache not contains entry with this cacheKey: {}", readCallStatus, pollingCacheKey);
        }
    }

    /**
     * This method will read the readCallStatus from the local polling cache by cache key.
     * 
     * @param activityJobId
     * @param upMoFdn
     * @return Return Returns readCallStatus from the cache if entry available with the cache key, else returns READ_CALL_STATUS_COMPLETED by assuming that the activity already completed.
     */

    public ReadCallStatusEnum getReadCallStatus(final long activityJobId, final String upMoFdn) {

        final String pollingCacheKey = getActivityPollingCacheKey(upMoFdn, activityJobId);
        final PollingActivityInfoProvider pollingActivityInfoProvider = getPolledActivityInformation(pollingCacheKey);
        if (pollingActivityInfoProvider != null) {
            return pollingActivityInfoProvider.getReadCallStatus();
        }
        return ReadCallStatusEnum.COMPLETED;
    }

    public Map<String, PollingActivityInfoProvider> getPollingActivitiesCache() {
        return pollingActivitiesCache;
    }

}