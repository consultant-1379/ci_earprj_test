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
package com.ericsson.oss.services.shm.axe.service;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.jobservice.axe.OpsSessionAndClusterIdInfo;

/**
 * This is a proxy class, used to manage retries of given method
 * 
 * @author Team Royals
 *
 */
public class ShmAxeServiceRetryProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShmAxeServiceRetryProxy.class);
    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;
    @Inject
    private ShmAxeServiceImplHelper shmAxeServiceImplHelper;
    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    public Map<Long, OpsSessionAndClusterIdInfo> getSessionIdAndClusterId(final List<Long> eachBatchOfNeJobIds) {
        try {
            return retryManager.executeCommand(dpsRetryPolicies.getViewJobDetailsRetryPolicy(), new ShmDpsRetriableCommand<Map<Long, OpsSessionAndClusterIdInfo>>() {
                @Override
                public Map<Long, OpsSessionAndClusterIdInfo> execute() {
                    return shmAxeServiceImplHelper.getSessionIdAndClusterId(eachBatchOfNeJobIds);
                }
            });
        } catch (final RetriableCommandException ex) {
            LOGGER.error("RetriableCommandException while fetching ops gui sessionId and ClusterId for AXE Nodes. Reason is: ", ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw ex;
        }
    }
}