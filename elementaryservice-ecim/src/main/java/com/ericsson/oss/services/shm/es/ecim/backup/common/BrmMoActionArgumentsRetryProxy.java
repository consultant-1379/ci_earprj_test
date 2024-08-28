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
package com.ericsson.oss.services.shm.es.ecim.backup.common;

import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.BrmBackupStatusInCompleteException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.es.api.DefaultActionRetryPolicy;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy;
import com.ericsson.oss.services.shm.model.NetworkElementData;

public class BrmMoActionArgumentsRetryProxy {

    @Inject
    protected BrmMoService brmMoService;

    @Inject
    protected RetryManager retryManager;

    @Inject
    protected DpsRetryPolicies dpsRetryPolicies;

    @Inject
    @DefaultActionRetryPolicy
    protected ActionRetryPolicy moActionRetryPolicy;

    public Map<String, Object> prepareActionArgumentsForUploadBackup(final NetworkElementData networkElement, final String nodeName)
            throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        try {
            return retryManager.executeCommand(moActionRetryPolicy.getDpsMoActionRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
                @Override
                public Map<String, Object> execute() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
                    return brmMoService.prepareActionArgumentsForUploadBackup(networkElement, nodeName);
                }
            });
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This api will apply the retry logic of FDN retrieval call , if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param activityName
     * @param nodeName
     * @param ecimBackupInfo
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */

    public String getBrmBackupManagerMoFdn(final NetworkElementData networkElement, final String nodeName, final EcimBackupInfo ecimBackupInfo)
            throws MoNotFoundException, UnsupportedFragmentException {
        try {
            return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
                @Override
                public String execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.getBrmBackupManagerMoFdn(networkElement, nodeName, ecimBackupInfo);
                }
            });
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    protected void handleException(final RetriableCommandException commandException) throws MoNotFoundException, UnsupportedFragmentException, BrmBackupStatusInCompleteException {
        final Throwable throwable = commandException.getCause();

        if (throwable instanceof MoNotFoundException) {
            throw new MoNotFoundException(throwable.getMessage());

        } else if (throwable instanceof UnsupportedFragmentException) {
            throw new UnsupportedFragmentException(throwable.getMessage());
        } else if (throwable instanceof BrmBackupStatusInCompleteException) {
            throw new BrmBackupStatusInCompleteException(throwable.getMessage());
        }
    }

}
