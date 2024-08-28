/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.polling;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;

/**
 * Retry Proxy class for retrying few PollingActivity PO operations
 * 
 * @author xsrabop
 * 
 */
public class PollingActivityOperationsRetryProxy {

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private PollingActivityOperations pollingActivityOperations;

    /**
     * This method creates the polling entry in the DB.
     * 
     * @param pollingEntry
     */
    public long createPO(final Map<String, Object> pollingEntry) {
        return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new RetriableCommand<Long>() {
            @Override
            public Long execute(final RetryContext retryContext) {
                return pollingActivityOperations.createPO(pollingEntry);
            }
        });
    }

    /**
     * This method deletes the PO with the given PoId.
     * 
     * @param poId
     */
    public void deletePOByPOId(final long poId) {
        retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Void>() {
            @Override
            public Void execute() {
                pollingActivityOperations.deletePOByPOId(poId);
                return null;
            }
        });
    }

    /**
     * This method fetches the poId of the polling entry for the given activityJobId and deletes the PO using default {@link RetriableCommand}.
     * 
     * @param activityJobId
     */
    public void deletePOByActivityJobId(final long activityJobId) {
        retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new RetriableCommand<Void>() {
            @Override
            public Void execute(final RetryContext retryContext) {
                pollingActivityOperations.deletePOByActivityJobId(activityJobId);
                return null;
            }
        });
    }

    /**
     * This method fetches the polling entry POID which contains the given activityJobId.
     * 
     * @param activityJobId
     */
    public long getPoIdByActivityJobId(final long activityJobId) {
        return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Long>() {
            @Override
            public Long execute() {
                return pollingActivityOperations.getPoIdByActivityJobId(activityJobId);
            }
        });
    }

    /**
     * This method updates the given attributes for the given polling entry poId. Here RetriableCommand is used instead of ShmDpsRetriableCommand because Container do not provide the Bean Manager
     * reference as there is an issue with CDI 1.0
     * 
     * @param poId
     * @param attributes
     */
    public void updatePollingAttributesByPoId(final long poId, final Map<String, Object> attributes) {
        retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new RetriableCommand<Void>() {
            @Override
            public Void execute(final RetryContext retryContext) {
                pollingActivityOperations.updatePollingAttributes(poId, attributes);
                return null;
            }
        });
    }

    /**
     * This method updates the given attributes for the given polling entry with the given activityJobId. Here RetriableCommand is used instead of ShmDpsRetriableCommand because Container do not
     * provide the Bean Manager reference as there is an issue with CDI 1.0
     * 
     * @param activityJobId
     * @param attributes
     */
    public void updatePollingAttributesByActivityJobId(final long activityJobId, final Map<String, Object> attributes) {
        retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new RetriableCommand<Void>() {
            @Override
            public Void execute(final RetryContext retryContext) {
                pollingActivityOperations.updatePollingAttributesByActivityJobId(activityJobId, attributes);
                return null;
            }
        });
    }

    /**
     * This method gets all the Polling Entries from DPS. Here RetriableCommand is used instead of ShmDpsRetriableCommand because Container do not provide the Bean Manager reference as there is an
     * issue with CDI 1.0.
     */
    public List<Map<String,Object>> getPollingActivityPOs() {
        return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<List<Map<String,Object>>>() {
            @Override
            public List<Map<String,Object>> execute() {
                return pollingActivityOperations.getPollingActivityPOs();
            }
        });
    }
    
   
}
