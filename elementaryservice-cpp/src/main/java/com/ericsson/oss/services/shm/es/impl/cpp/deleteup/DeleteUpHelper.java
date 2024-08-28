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
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;

public class DeleteUpHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteUpHelper.class);

    @Inject
    private RetryManager retryManager;

    @Inject
    private DeleteUpMO deleteUpMO;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    public int deleteUP(final String upMoFdn) {
        try {
            final int deleteUpResult = retryManager.executeCommand(dpsRetryPolicies.getUPMoDeleteActionRetryPolicy(), new ShmDpsRetriableCommand<Integer>() {
                @Override
                protected Integer execute() throws EJBException {
                    return deleteUpMO.deleteUP(upMoFdn);
                }
            });
            LOGGER.debug("Deleted {} UP's for {}", deleteUpResult, upMoFdn);
            return deleteUpResult;
        } catch (Exception e) {
            LOGGER.error("exception in deleteuphelper when try to delete the up having fdn: {}:", upMoFdn, e);
            if (e.getCause() instanceof EJBTransactionRolledbackException) {
                //Here Transaction is getting rolledback, since the node is taking more time to delete. Then wait for the DELETE notification
                return -1;
            }
            throw e;
        }
    }
}
