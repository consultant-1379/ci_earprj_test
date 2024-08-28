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
package com.ericsson.oss.services.shm.es.impl.ecim.deleteup;

import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.exception.MoActionAbortRetryException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.DefaultActionRetryPolicy;
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy;

public class DeleteUpgradePackageJobDataCollectorRetryProxy {

    @Inject
    private DeleteUpgradePackageDataCollector deleteUpDataCollector;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    @DefaultActionRetryPolicy
    private ActionRetryPolicy moActionRetryPolicy;

    public String getSwmManagedObjectFdn(final String nodeName, final String swmNameSpace) {

        final String moFdn = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
            @Override
            public String execute() throws MoNotFoundException {
                return deleteUpDataCollector.getSwmFdn(nodeName, swmNameSpace);
            }
        });
        return moFdn;
    }

    public boolean performAction(final String swmmoFdn, final String moAction, final Map<String, Object> actionArguments, final String neType, final String ossModelIdentity)
            throws MoNotFoundException, UnsupportedFragmentException {
        try {
            final boolean isActionSuccess = retryManager.executeCommand(moActionRetryPolicy.getDpsMoActionRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() throws UnsupportedFragmentException, MoNotFoundException {

                    return deleteUpDataCollector.performAction(swmmoFdn, moAction, actionArguments, neType, ossModelIdentity);
                }
            });
            return isActionSuccess;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;

        }
    }

    private void handleException(final RetriableCommandException commandException) throws MoNotFoundException, UnsupportedFragmentException {
        final Throwable throwable = commandException.getCause();
        if (throwable instanceof MoNotFoundException) {
            throw new MoNotFoundException(throwable.getMessage());
        } else if (throwable instanceof UnsupportedFragmentException) {
            throw new UnsupportedFragmentException(throwable.getMessage());
        } else if (throwable instanceof MoActionAbortRetryException) {
            throw new MoActionAbortRetryException(throwable.getMessage());
        }
    }

    /**
     * This API will apply retry logic for retrieving progress information , if it fails because of DatabaseNotAvailableException.
     * 
     * @param nodeName
     * @param modifiedAttributes
     * @return
     * @throws UnsupportedFragmentException
     */
    public AsyncActionProgress getValidAsyncActionProgress(final String nodeName, final Map<String, AttributeChangeData> modifiedAttributes, final String neType, final String ossModelIdentity)
            throws UnsupportedFragmentException {
        try {
            final AsyncActionProgress asyncActionProgress = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<AsyncActionProgress>() {
                @Override
                public AsyncActionProgress execute() throws UnsupportedFragmentException {
                    return deleteUpDataCollector.getValidAsyncActionProgress(nodeName, modifiedAttributes, neType, ossModelIdentity);
                }
            });
            return asyncActionProgress;
        } catch (final RetriableCommandException ex) {
            if (ex.getCause() instanceof UnsupportedFragmentException) {
                throw new UnsupportedFragmentException(ex.getCause().getMessage());
            }
            throw ex;
        }
    }

}
