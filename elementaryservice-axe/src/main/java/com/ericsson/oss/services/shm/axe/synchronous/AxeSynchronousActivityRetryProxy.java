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
package com.ericsson.oss.services.shm.axe.synchronous;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;

public class AxeSynchronousActivityRetryProxy {
    @Inject
    private DpsRetryConfigurationParamProvider dpsConfigurationParamProvider;

    @Inject
    private AxeSynchronousActivityProxyService axeSynchronousActivityProxyService;

    private static final int NO_OF_DPS_RETRIES = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(AxeSynchronousActivityRetryProxy.class);

    public Map<Long, Map<String, String>> retryForTotalNeJobCreation(final long mainJobId, final String neType, final int totalNeCountByNeType) {
        Map<Long, Map<String, String>> neJobDetails = new HashMap<>();
        for (int i = 1; i <= NO_OF_DPS_RETRIES; i++) {
            neJobDetails = axeSynchronousActivityProxyService.getNeJobForMainJobByNeType(mainJobId, neType);
            if (totalNeCountByNeType == neJobDetails.size()) {
                LOGGER.debug("All NeJobs created Succesfully");
                break;
            } else {
                try {
                    Thread.sleep(dpsConfigurationParamProvider.getDpsOptimisticLockWaitIntervalInMS());
                } catch (InterruptedException e) {
                    LOGGER.error("Thread got interrupted {}", e);
                    Thread.currentThread().interrupt();
                }
                neJobDetails.clear();
            }
        }
        return neJobDetails;
    }

    public Map<Long, String> checkforActivityCompletion(final Map<Long, String> neJobDetails, final int activityOrder) {
        Map<Long, String> completedNes = new HashMap<>();
        for (int i = 1; i <= NO_OF_DPS_RETRIES; i++) {
            completedNes = axeSynchronousActivityProxyService.getCompletedActivityDetails(new ArrayList<Long>(neJobDetails.keySet()), activityOrder);
            if (completedNes.size() == neJobDetails.size()) {
                LOGGER.debug("Current activity completed on ,check For job Result");
                break;
            } else {
                try {
                    Thread.sleep(dpsConfigurationParamProvider.getDpsOptimisticLockWaitIntervalInMS());
                } catch (InterruptedException e) {
                    LOGGER.error("Thread got interrupted {}", e);
                    Thread.currentThread().interrupt();
                }
                completedNes.clear();
            }
        }
        return completedNes;
    }

}
