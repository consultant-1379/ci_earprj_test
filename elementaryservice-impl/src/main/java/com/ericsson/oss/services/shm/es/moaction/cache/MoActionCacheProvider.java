/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.moaction.cache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.shm.model.ShmEBMCMoActionData;

/**
 * This class provides implementation methods to add, get and remove the MTR data for MoAction in the cache
 * 
 * @author xpavdeb
 */

@ApplicationScoped
public class MoActionCacheProvider {

    private static final Logger logger = LoggerFactory.getLogger(MoActionCacheProvider.class);

    @Inject
    @NamedCache("ShmEBMCMoActionCache")
    private Cache<Long, ShmEBMCMoActionData> cache;

    public void add(final long activityJobId, final ShmEBMCMoActionData requestMTR) {
        try {
            cache.put(activityJobId, requestMTR);
            logger.debug("Placed ShmEBMCMoActionData in Cache for activityJobId: {}", activityJobId);
        } catch (final Exception ex) {
            logger.error("Failed to add ShmEBMCMoActionData in cache with activityJobId {} . Exception is ", activityJobId, ex);
        }

    }

    public List<ShmEBMCMoActionData> getAll() {
        final List<ShmEBMCMoActionData> shmEcimReadMOMediationTaskRequestList = new ArrayList<>();
        try {
            final Iterator<Entry<Long, ShmEBMCMoActionData>> iterator = cache.iterator();
            while (iterator.hasNext()) {
                final Entry<Long, ShmEBMCMoActionData> requestMTREntry = iterator.next();
                shmEcimReadMOMediationTaskRequestList.add(requestMTREntry.getValue());
            }
        } catch (final Exception ex) {
            logger.error("Failed to get ShmEBMCMoActionData from cluster cache. Exception is: ", ex);
        }
        return shmEcimReadMOMediationTaskRequestList;
    }

    public ShmEBMCMoActionData get(final long activityJobId) {
        ShmEBMCMoActionData ebmcMoActionData = null;
        try {
            ebmcMoActionData = cache.get(activityJobId);
            return ebmcMoActionData;
        } catch (final Exception ex) {
            logger.error("Failed to get ShmEBMCMoActionData from cluster cache. Exception is: ", ex);
        }
        return ebmcMoActionData;
    }

    public void remove(final long activityJobId) {
        try {
            if (cache.get(activityJobId) != null) {
                logger.debug("Found cache for Mo Action with activity job ID {}", activityJobId);
                cache.remove(activityJobId);
            }
        } catch (final Exception ex) {
            logger.error("Failed to remove entry from ShmEBMCMoActionData with key: {}. Exception is: ", activityJobId, ex);
        }
    }
}
