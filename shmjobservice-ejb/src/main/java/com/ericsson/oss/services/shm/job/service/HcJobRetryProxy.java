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
package com.ericsson.oss.services.shm.job.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;

public class HcJobRetryProxy {

    private static final Logger logger = LoggerFactory.getLogger(HcJobRetryProxy.class);
    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private MoDeletionService moDeletionService;

    public Integer deletePo(final PersistenceObject jobPo, final DataBucket liveBucket) {
        try {
            return retryManager.executeCommand(dpsRetryPolicies.getOptimisticLockRetryPolicy(), new ShmDpsRetriableCommand<Integer>() {
                @Override
                public Integer execute() {
                    return liveBucket.deletePo(jobPo);
                }
            });
        } catch (final RetriableCommandException exception) {
            logger.error("All retries exhausted while deleting PO's from Database. Reason: ", exception);
            throw exception;
        }
    }

    public Integer deleteMoByFDN(final String hcJobFdn) {
        try {
            return retryManager.executeCommand(dpsRetryPolicies.getOptimisticLockRetryPolicy(), new ShmDpsRetriableCommand<Integer>() {
                @Override
                public Integer execute() {
                    return moDeletionService.deleteMoByFDN(hcJobFdn);
                }
            });
        } catch (final RetriableCommandException exception) {
            logger.error("All retries exhausted while deleting HC Jobs from Node. Reason: {}, for  HcJobFdn {}", exception, hcJobFdn);
            throw exception;
        }
    }

}
