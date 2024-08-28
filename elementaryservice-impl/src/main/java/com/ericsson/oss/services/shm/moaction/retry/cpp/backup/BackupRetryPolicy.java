/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.moaction.retry.cpp.backup;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.exception.DatabaseNotAvailableException;
import com.ericsson.oss.services.shm.common.exception.MoActionAbortRetryException;
import com.ericsson.oss.services.shm.common.exception.MoActionRetryException;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy;

public class BackupRetryPolicy extends ActionRetryPolicy {

    @Inject
    private DpsRetryConfigurationParamProvider dpsConf;

    @SuppressWarnings("unchecked")
    private static final Class<? extends Exception>[] moActionRetryExceptions = new Class[] { DatabaseNotAvailableException.class, RuntimeException.class, MoActionRetryException.class };

    @SuppressWarnings("unchecked")
    private static final Class<? extends Exception>[] moActionRetryStopExceptions = new Class[] { MoActionAbortRetryException.class };

    /**
     * Provides {@link RetryPolicy}, object for dps retry calls. This method should be used only when an action needs to be performed on the node
     * 
     * @return RetryPolicy
     */
    @Override
    public RetryPolicy getDpsMoActionRetryPolicy() {
        return RetryPolicy.builder().attempts(dpsConf.getdpsMoActionRetryCount()).waitInterval(dpsConf.getMoActionWaitIntervalInMS(), TimeUnit.MILLISECONDS)
                .exponentialBackoff(ShmCommonConstants.EXPONENTIAL_BACK_OFF).retryOn(moActionRetryExceptions).stopOn(moActionRetryStopExceptions).build();
    }

}
