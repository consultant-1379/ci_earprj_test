/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.common;

import java.util.Map;

import javax.inject.Inject;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;

import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;

/**
 * Retry Proxy class for retrying few AutoProvisioning MO operations
 * @author xmadupp
 *
 */
public class ApMoHandlerRetryProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApMoHandlerRetryProxy.class);

    @Inject
    protected ApMoHandlerImpl apMoHandlerImpl;

    @Inject
    protected RetryManager retryManager;

    @Inject
    protected DpsRetryPolicies dpsRetryPolicies;

    /**
     * Retry Proxy in getting AP MO Details.
     * @param nodeName
     * @return
     * @throws MoNotFoundException
     */
    public Map<String, Object> getApMoDetails(final String nodeName) throws MoNotFoundException {
        LOGGER.debug("In getApMoDetails ApMoHandlerRetryProxy for node: {}", nodeName);
        try {
            return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
                @Override
                public Map<String, Object> execute() throws MoNotFoundException {
                    return apMoHandlerImpl.getApMoAttritues(nodeName);
                }
            });
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * Retry Proxy in updating AP MO Details.
     * @param fdn
     * @param moArguments
     */
    public void updateApMoAttributes(final String fdn, final Map<String, Object> moArguments) {
       LOGGER.debug("In updateApMoAttributes ApMoHandlerRetryProxy for MOfdn: {} and MO Arguments: {}", fdn, moArguments);
       try {
           retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Void>() {
               @Override
               public Void execute() {
                   apMoHandlerImpl.updateApMoAttributes(fdn, moArguments);
                   return null;
               }
           });
        } catch (final RetriableCommandException ex) {
            throw ex;
       }
    }

    protected void handleException(final RetriableCommandException commandException) throws MoNotFoundException {
        final Throwable throwable = commandException.getCause();

        if (throwable instanceof MoNotFoundException) {
            throw new MoNotFoundException(throwable.getMessage());
        }
    }
}
