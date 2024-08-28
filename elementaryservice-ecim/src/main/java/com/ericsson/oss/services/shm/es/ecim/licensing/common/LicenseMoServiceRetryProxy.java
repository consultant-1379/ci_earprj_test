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
package com.ericsson.oss.services.shm.es.ecim.licensing.common;

import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.DefaultActionRetryPolicy;
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;

/**
 * Retry Proxy class for retrying few License MO operations
 * 
 * @author xswagud
 * 
 */
public class LicenseMoServiceRetryProxy {
    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private LicenseMoService licenseMoService;

    @Inject
    @DefaultActionRetryPolicy
    protected ActionRetryPolicy moActionRetryPolicy;

    /**
     * This API will perform retries of FDN retrieval call, if it fails because of DatabaseNotAvailableException.
     * 
     * @param ecimLicensingInfo
     * @param activityName
     * @return
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     */

    public String getNotifiableMoFdn(final NetworkElementData networkElement, final String activityName) throws UnsupportedFragmentException, MoNotFoundException {
        try {
            final String notifiableFdn = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
                @Override
                public String execute() throws UnsupportedFragmentException, MoNotFoundException {
                    return licenseMoService.getNotifiableMoFdn(networkElement, activityName);
                }
            });
            return notifiableFdn;
        } catch (final RetriableCommandException ex) {
            handleExceptions(ex);
            throw ex;
        }
    }

    /**
     * This will performs the retries of retrieving of license PO existence call, if it fails because of DatabaseNotAvailableException.
     * 
     * @param ecimLicensingInfo
     * @return
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     */

    public boolean isLicensingPoExists(final String licenseKeyFileName) throws UnsupportedFragmentException, MoNotFoundException {
        try {
            final boolean isLicensePoExists = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() throws UnsupportedFragmentException, MoNotFoundException {
                    return licenseMoService.isLicensingPOExists(licenseKeyFileName);
                }
            });
            return isLicensePoExists;
        } catch (final RetriableCommandException ex) {
            handleExceptions(ex);
            throw ex;
        }
    }

    /**
     * This will performs the retries of retrieving of license key file existence call, if it fails because of DatabaseNotAvailableException.
     * 
     * @param ecimLicensingInfo
     * @return
     * @throws MoNotFoundException
     */

    public boolean isLicenseKeyFileExistsInSMRS(final String licenseKeyFileName) throws MoNotFoundException {
        try {

            final boolean isLicenseKeyFileExistsInSMRS = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() throws MoNotFoundException {
                    return licenseMoService.isLicenseKeyFileExistsInSMRS(licenseKeyFileName);
                }
            });
            return isLicenseKeyFileExistsInSMRS;
        } catch (final RetriableCommandException ex) {
            if (ex.getCause() instanceof MoNotFoundException) {
                throw new MoNotFoundException(ex.getCause().getMessage());
            }
            throw ex;
        }
    }

    /**
     * 
     * This will performs retries of retrieving of progress report information, if it fails because of DatabaseNotAvailableException.
     * 
     * @param nodeName
     * @param modifiedAttributes
     * @param activityName
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */

    public AsyncActionProgress getValidAsyncActionProgress(final String nodeName, final Map<String, AttributeChangeData> modifiedAttributes, final String activityName) throws MoNotFoundException,
            UnsupportedFragmentException {
        try {
            final AsyncActionProgress actionProgress = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<AsyncActionProgress>() {
                @Override
                public AsyncActionProgress execute() throws UnsupportedFragmentException, MoNotFoundException {
                    return licenseMoService.getValidAsyncActionProgress(nodeName, modifiedAttributes, activityName);
                }
            });
            return actionProgress;
        } catch (final RetriableCommandException ex) {
            handleExceptions(ex);
            throw ex;
        }
    }

    /**
     * 
     * This will performs retries of triggering an action ,if this will get DatabaseNotAvailableException from licenseMoService
     * 
     * @param ecimLicensingInfo
     * @param keyFileMgmtMOFdn
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     * @throws ArgumentBuilderException
     */

    public short executeMoAction(final LicensePrecheckResponse licensePrecheckResponse, final NEJobStaticData neJobStaticData) throws MoNotFoundException, UnsupportedFragmentException,
            ArgumentBuilderException {
        try {
            final short actionId = retryManager.executeCommand(moActionRetryPolicy.getDpsMoActionRetryPolicy(), new ShmDpsRetriableCommand<Short>() {
                @Override
                public Short execute() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
                    return licenseMoService.executeMoAction(licensePrecheckResponse, neJobStaticData);
                }
            });
            return actionId;
        } catch (final RetriableCommandException ex) {
            if (ex.getCause() instanceof ArgumentBuilderException) {
                throw new ArgumentBuilderException(ex.getCause().getMessage());
            }
            handleExceptions(ex);
            throw ex;
        }
    }

    /**
     * This will performs retries of retrieving of progress report information, if it fails because of DatabaseNotAvailableException.
     * 
     * @param nodeName
     * @return
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     */

    public AsyncActionProgress getActionProgressOfKeyFileMgmtMO(final NetworkElementData networkElement) throws UnsupportedFragmentException, MoNotFoundException {

        try {
            final AsyncActionProgress asyncActionProgress = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<AsyncActionProgress>() {
                @Override
                public AsyncActionProgress execute() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
                    return licenseMoService.getActionProgressOfKeyFileMgmtMO(networkElement);
                }
            });
            return asyncActionProgress;
        } catch (final RetriableCommandException ex) {
            handleExceptions(ex);
            throw ex;
        }
    }

    public boolean getEmergencyUnlockActivationState(final NetworkElementData networkElement, final NEJobStaticData neJobStaticData) throws UnsupportedFragmentException, MoNotFoundException {
        try {
            final boolean isEmergencyLckInActiveState = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() throws UnsupportedFragmentException, MoNotFoundException {
                    return licenseMoService.getEmergencyUnlockActivationState(networkElement);
                }
            });
            return isEmergencyLckInActiveState;
        } catch (final RetriableCommandException ex) {
            handleExceptions(ex);
            throw ex;
        }
    }

    public String getLicenseKeyFileName(final NEJobStaticData neJobStaticData, final NetworkElementData networkElement) throws UnsupportedFragmentException, MoNotFoundException {

        try {
            final String licenseKeyFileName = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
                @Override
                public String execute() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
                    return licenseMoService.getLicenseKeyFileName(neJobStaticData, networkElement);
                }
            });
            return licenseKeyFileName;
        } catch (final RetriableCommandException ex) {
            handleExceptions(ex);
            throw ex;
        }
    }

    public String getLicenseKeyFileNameFromFingerPrint(final String fingerprint) throws UnsupportedFragmentException, MoNotFoundException {

        try {
            final String licenseKeyFileName = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
                @Override
                public String execute() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
                    return licenseMoService.getLicenseKeyFileNameFromFingerPrint(fingerprint);
                }
            });
            return licenseKeyFileName;
        } catch (final RetriableCommandException ex) {
            handleExceptions(ex);
            throw ex;
        }
    }

    public String getSequenceNumber(final String fingerprint) throws UnsupportedFragmentException, MoNotFoundException {

        try {
            return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
                @Override
                public String execute() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
                    return licenseMoService.getSequenceNumber(fingerprint);
                }
            });
        } catch (final RetriableCommandException ex) {
            handleExceptions(ex);
            throw ex;
        }
    }

    public String getSequenceNumberFromNode(final NetworkElementData networkElementData) throws UnsupportedFragmentException, MoNotFoundException {

        try {
            return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
                @Override
                public String execute() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
                    return licenseMoService.getSequenceNumberFromNode(networkElementData);
                }
            });
        } catch (final RetriableCommandException ex) {
            handleExceptions(ex);
            throw ex;
        }
    }

    private void handleExceptions(final RetriableCommandException commandException) throws MoNotFoundException, UnsupportedFragmentException {
        final Throwable throwable = commandException.getCause();
        if (throwable instanceof MoNotFoundException) {
            throw new MoNotFoundException(throwable.getMessage());
        } else if (throwable instanceof UnsupportedFragmentException) {
            throw new UnsupportedFragmentException(throwable.getMessage());
        }
    }
}
