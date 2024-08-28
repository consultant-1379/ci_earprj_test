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
package com.ericsson.oss.services.shm.es.ecim.upgrade.common;

import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.exception.MoActionAbortRetryException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.UnsupportedAttributeException;
import com.ericsson.oss.services.shm.common.exception.UpgradePackageMoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.DefaultActionRetryPolicy;
import com.ericsson.oss.services.shm.es.impl.ecim.common.SoftwarePackageNameNotFound;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActionResult;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActivateState;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActivityAllowed;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SoftwarePackagePoNotFound;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.UpgradePackageState;
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy;

/**
 * Retry Proxy class for retrying few UpgradePackage MO operations
 * 
 * @author xswagud
 */
public class UpMoServiceRetryProxy {
    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private UpMoService upMoService;

    @Inject
    @DefaultActionRetryPolicy
    private ActionRetryPolicy moActionRetryPolicy;

    /**
     * This API will apply retry logic for verifying activity allowed call, if it fails because of DatabaseNotAvailableException.
     * 
     * @param activityName
     * @param ecimUpgradeInfo
     * @return
     * @throws SoftwarePackageNameNotFound
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     * @throws SoftwarePackagePoNotFound
     * @throws ArgumentBuilderException
     */

    public ActivityAllowed isActivityAllowed(final String activityName, final EcimUpgradeInfo ecimUpgradeInfo) throws SoftwarePackageNameNotFound, MoNotFoundException, UnsupportedFragmentException,
            SoftwarePackagePoNotFound, ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        try {
            final ActivityAllowed activityAllowed = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<ActivityAllowed>() {
                @Override
                public ActivityAllowed execute() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
                        MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {
                    return upMoService.isActivityAllowed(activityName, ecimUpgradeInfo);
                }
            });
            return activityAllowed;
        } catch (final RetriableCommandException ex) {
            if (ex.getCause() instanceof NodeAttributesReaderException) {
                throw new NodeAttributesReaderException(ex.getCause().getMessage());
            } else if (ex.getCause() instanceof UpgradePackageMoNotFoundException) {
                throw new UpgradePackageMoNotFoundException(ex.getCause().getMessage());
            }
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This API will apply retry logic for verifying existence of UP MO, if it fails because of DatabaseNotAvailableException.
     * 
     * @param ecimUpgradeInfo
     * @return
     */
    public boolean isUpgradePackageMoExists(final EcimUpgradeInfo ecimUpgradeInfo) {

        final boolean isUpMoExists = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
            @Override
            public Boolean execute() {
                return upMoService.isUpgradePackageMoExists(ecimUpgradeInfo);
            }
        });
        return isUpMoExists;
    }

    /**
     * This API will apply retry logic for retrieving Upgrade Package URI, if it fails because of DatabaseNotAvailableException.
     * 
     * @param ecimUpgradeInfo
     * @return
     * @throws SoftwarePackageNameNotFound
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     * @throws SoftwarePackagePoNotFound
     * @throws ArgumentBuilderException
     * @throws UnsupportedAttributeException
     */

    public Map<String, Object> getUpgradePackageUri(final EcimUpgradeInfo ecimUpgradeInfo) throws SoftwarePackageNameNotFound, UnsupportedFragmentException, MoNotFoundException,
            SoftwarePackagePoNotFound, ArgumentBuilderException, UnsupportedAttributeException {

        try {
            final Map<String, Object> upgradePackageUri = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {

                @Override
                public Map<String, Object> execute() throws SoftwarePackageNameNotFound, UnsupportedFragmentException, MoNotFoundException, SoftwarePackagePoNotFound, ArgumentBuilderException,
                        NodeAttributesReaderException, UnsupportedAttributeException {
                    return upMoService.getUpgradePackageUri(ecimUpgradeInfo);
                }
            });
            return upgradePackageUri;
        } catch (final RetriableCommandException ex) {
            if (ex.getCause() instanceof UnsupportedAttributeException) {
                throw new UnsupportedAttributeException(ex.getCause().getMessage());
            }
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This API will apply retry logic for retrieving SW package name, if it fails because of DatabaseNotAvailableException.
     * 
     * @param ecimUpgradeInfo
     * @return
     * @throws SoftwarePackageNameNotFound
     * @throws MoNotFoundException
     */
    public String getSwPkgName(final EcimUpgradeInfo ecimUpgradeInfo) throws SoftwarePackageNameNotFound, MoNotFoundException {
        try {
            final String swPkgname = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
                @Override
                public String execute() throws SoftwarePackageNameNotFound, MoNotFoundException {
                    return upMoService.getSwPkgName(ecimUpgradeInfo);
                }
            });
            return swPkgname;
        } catch (final RetriableCommandException ex) {
            if (ex.getCause() instanceof SoftwarePackageNameNotFound) {
                throw new SoftwarePackageNameNotFound(ex.getCause().getMessage());
            }
            if (ex.getCause() instanceof MoNotFoundException) {
                throw new MoNotFoundException(ex.getCause().getMessage());
            }
            throw ex;
        }
    }

    /**
     * This API will apply retry logic of building Upgrade package URI , if it fails because of DatabaseNotAvailableException.
     * 
     * @param nodeName
     * @return
     * @throws ArgumentBuilderException
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    public Map<String, Object> buildUpgradePackageUri(final EcimUpgradeInfo ecimUpgradeInfo) throws ArgumentBuilderException, MoNotFoundException, UnsupportedFragmentException {
        try {
            final Map<String, Object> upgradePkgUri = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
                @Override
                public Map<String, Object> execute() throws ArgumentBuilderException, MoNotFoundException, UnsupportedFragmentException {
                    return upMoService.buildUpgradePackageUri(ecimUpgradeInfo);
                }
            });
            return upgradePkgUri;
        } catch (final RetriableCommandException ex) {
            if (ex.getCause() instanceof MoNotFoundException) {
                throw new MoNotFoundException(ex.getCause().getMessage());
            } else if (ex.getCause() instanceof ArgumentBuilderException) {
                throw new ArgumentBuilderException(ex.getCause().getMessage());
            } else if (ex.getCause() instanceof UnsupportedFragmentException) {
                throw new UnsupportedFragmentException(ex.getCause().getMessage());
            }
            throw ex;
        }
    }

    /**
     * This API will apply retry logic for updating UP MO attributes , if it fails because of DatabaseNotAvailableException.
     * 
     * @param ecimUpgradeInfo
     * @param changedAttributes
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     * @throws ArgumentBuilderException
     */
    public void updateMOAttributes(final EcimUpgradeInfo ecimUpgradeInfo, final Map<String, Object> changedAttributes) throws UnsupportedFragmentException, MoNotFoundException,
            SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
        try {
            retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Void>() {
                @Override
                public Void execute() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
                    upMoService.updateMOAttributes(ecimUpgradeInfo, changedAttributes);
                    return null;
                }
            });
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
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
    public AsyncActionProgress getValidAsyncActionProgress(final String nodeName, final Map<String, AttributeChangeData> modifiedAttributes) throws UnsupportedFragmentException {
        try {
            final AsyncActionProgress asyncActionProgress = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<AsyncActionProgress>() {
                @Override
                public AsyncActionProgress execute() throws UnsupportedFragmentException {
                    return upMoService.getValidAsyncActionProgress(nodeName, modifiedAttributes);
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

    /**
     * This API will apply retry logic for verifying progress information , if it fails because of DatabaseNotAvailableException.
     * 
     * @param ecimUpgradeInfo
     * @param asyncActionProgress
     * @param activityName
     * @return
     * @throws UnsupportedFragmentException
     */
    public boolean isValidAsyncActionProgress(final EcimUpgradeInfo ecimUpgradeInfo, final AsyncActionProgress asyncActionProgress, final String activityName) throws UnsupportedFragmentException {
        try {
            final boolean isValidActionProgress = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() throws UnsupportedFragmentException {
                    return upMoService.isValidAsyncActionProgress(ecimUpgradeInfo, asyncActionProgress, activityName);
                }
            });
            return isValidActionProgress;
        } catch (final RetriableCommandException ex) {
            if (ex.getCause() instanceof UnsupportedFragmentException) {
                throw new UnsupportedFragmentException(ex.getCause().getMessage());
            }
            throw ex;
        }
    }

    /**
     * This will apply retry logic for triggering an action ,if this will get DatabaseNotAvailableException from upMoService
     * 
     * @param ecimUpgradeInfo
     * @param activityName
     * @return
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     * @throws ArgumentBuilderException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     */
    public ActionResult executeMoAction(final EcimUpgradeInfo ecimUpgradeInfo, final String activityName) throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException,
            SoftwarePackageNameNotFound, SoftwarePackagePoNotFound {
        try {
            final ActionResult actionResult = retryManager.executeCommand(moActionRetryPolicy.getDpsMoActionRetryPolicy(), new ShmDpsRetriableCommand<ActionResult>() {
                @Override
                public ActionResult execute() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound {
                    return upMoService.executeMoAction(ecimUpgradeInfo, activityName);
                }
            });
            return actionResult;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This will apply retry logic for triggering a cancel action ,if this will get DatabaseNotAvailableException from upMoService
     * 
     * @param ecimUpgradeInfo
     * @param activityName
     * @return
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     * @throws ArgumentBuilderException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     */
    public ActionResult executeCancelAction(final EcimUpgradeInfo ecimUpgradeInfo, final String activityName) throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException,
            SoftwarePackageNameNotFound, SoftwarePackagePoNotFound {
        try {
            final ActionResult actionResult = retryManager.executeCommand(moActionRetryPolicy.getDpsMoActionRetryPolicy(), new ShmDpsRetriableCommand<ActionResult>() {
                @Override
                public ActionResult execute() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound {
                    return upMoService.executeCancelAction(ecimUpgradeInfo, activityName);
                }
            });
            return actionResult;
        } catch (final RetriableCommandException ex) {

            handleException(ex);
            throw ex;
        }
    }

    /**
     * This API will apply retry logic for retrieving progress information , if it fails because of DatabaseNotAvailableException.
     * 
     * @param ecimUpgradeInfo
     * @return
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     * @throws ArgumentBuilderException
     */
    public AsyncActionProgress getAsyncActionProgress(final EcimUpgradeInfo ecimUpgradeInfo) throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        try {
            final AsyncActionProgress asyncActionProgress = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<AsyncActionProgress>() {
                @Override
                public AsyncActionProgress execute() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
                        NodeAttributesReaderException {
                    return upMoService.getAsyncActionProgress(ecimUpgradeInfo);
                }
            });
            return asyncActionProgress;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This api will apply the retry logic of FDN retrieval call , if this will get DatabaseNotAvailableException from upMoService.
     * 
     * @param activityName
     * @param ecimUpgradeInfo
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     * @throws ArgumentBuilderException
     */
    public String getNotifiableMoFdn(final String activityName, final EcimUpgradeInfo ecimUpgradeInfo) throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException {
        try {
            final String notifiableFdn = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
                @Override
                public String execute() throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {
                    return upMoService.getNotifiableMoFdn(activityName, ecimUpgradeInfo);
                }
            });
            return notifiableFdn;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This api will apply the retry logic for verifying action completion call , if this will get DatabaseNotAvailableException from upMoService.
     * 
     * @param activityName
     * @param ecimUpgradeInfo
     * @return
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     * @throws ArgumentBuilderException
     */
    public boolean isActivityCompleted(final String activityName, final EcimUpgradeInfo ecimUpgradeInfo) throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        try {
            final boolean isActivityCompleted = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
                        NodeAttributesReaderException {
                    return upMoService.isActivityCompleted(activityName, ecimUpgradeInfo);
                }
            });
            return isActivityCompleted;
        } catch (final RetriableCommandException ex) {
            if (ex.getCause() instanceof NodeAttributesReaderException) {
                throw new NodeAttributesReaderException(ex.getCause().getMessage());
            }
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This api will apply the retry logic to get the UpgradePackage State , if this will get DatabaseNotAvailableException from upMoService.
     * 
     * @param ecimUpgradeInfo
     * @return UpgradePackageState
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     * @throws ArgumentBuilderException
     */
    public UpgradePackageState getUpgradePkgState(final EcimUpgradeInfo ecimUpgradeInfo) throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException {
        try {
            final UpgradePackageState upgradePackageState = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<UpgradePackageState>() {
                @Override
                public UpgradePackageState execute() throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException,
                        NodeAttributesReaderException {
                    return upMoService.getUpgradePackageState(ecimUpgradeInfo);
                }
            });
            return upgradePackageState;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    public Map<String, Object> isActionCompleted(final AsyncActionProgress reportProgress) {
        return upMoService.isActionCompleted(reportProgress);
    }

    /**
     * This api will apply the retry logic for verifying action trigger status call , if this will get DatabaseNotAvailableException from upMoService.
     * 
     * @param ecimUpgradeInfo
     * @param activityName
     * @return
     * @throws UnsupportedFragmentException
     */
    public boolean isCreateActionTriggered(final EcimUpgradeInfo ecimUpgradeInfo, final String activityName) throws UnsupportedFragmentException {
        try {
            final boolean isCreatedTriggered = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() throws UnsupportedFragmentException {
                    return upMoService.isCreateActionTriggered(ecimUpgradeInfo, activityName);
                }
            });
            return isCreatedTriggered;
        } catch (final RetriableCommandException ex) {

            if (ex.getCause() instanceof UnsupportedFragmentException) {
                throw new UnsupportedFragmentException(ex.getCause().getMessage());
            }
            throw ex;
        }
    }

    /**
     * This api will apply the retry logic for retrieving activation steps call, if this will get DatabaseNotAvailableException from upMoService.
     * 
     * @param ecimUpgradeInfo
     * @param changedAttributes
     * @return
     * @throws UnsupportedFragmentException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     * @throws MoNotFoundException
     * @throws ArgumentBuilderException
     */
    public int getActivationSteps(final EcimUpgradeInfo ecimUpgradeInfo, final Map<String, Object> changedAttributes) throws UnsupportedFragmentException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, MoNotFoundException, ArgumentBuilderException, NodeAttributesReaderException {
        try {
            final int activationSteps = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Integer>() {
                @Override
                public Integer execute() throws UnsupportedFragmentException, SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, MoNotFoundException, ArgumentBuilderException,
                        NodeAttributesReaderException {
                    return upMoService.getActivationSteps(ecimUpgradeInfo, changedAttributes);
                }
            });
            return activationSteps;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * @param nodeName
     * @param modifiedAttributes
     * @return
     * @throws UnsupportedFragmentException
     */
    public ActivateState getUpgradePackageState(final String nodeName, final Map<String, AttributeChangeData> modifiedAttributes) throws UnsupportedFragmentException {
        try {
            final ActivateState activateState = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<ActivateState>() {
                @Override
                public ActivateState execute() throws UnsupportedFragmentException {
                    return upMoService.getUpgradePackageState(nodeName, modifiedAttributes);
                }
            });
            return activateState;
        } catch (final RetriableCommandException ex) {
            if (ex.getCause() instanceof UnsupportedFragmentException) {
                throw new UnsupportedFragmentException(ex.getCause().getMessage());
            }
            throw ex;
        }
    }

    private void handleException(final RetriableCommandException commandException) throws SoftwarePackageNameNotFound, MoNotFoundException, UnsupportedFragmentException, SoftwarePackagePoNotFound,
            ArgumentBuilderException {
        final Throwable throwable = commandException.getCause();
        if (throwable instanceof MoNotFoundException) {
            throw new MoNotFoundException(throwable.getMessage());
        } else if (throwable instanceof SoftwarePackageNameNotFound) {
            throw new SoftwarePackageNameNotFound(throwable.getMessage());
        } else if (throwable instanceof SoftwarePackagePoNotFound) {
            throw new SoftwarePackagePoNotFound(throwable.getMessage());
        } else if (throwable instanceof UnsupportedFragmentException) {
            throw new UnsupportedFragmentException(throwable.getMessage());
        } else if (throwable instanceof ArgumentBuilderException) {
            throw new ArgumentBuilderException(throwable.getMessage());
        } else if (throwable instanceof MoActionAbortRetryException) {
            throw new MoActionAbortRetryException(throwable.getMessage());
        }
    }

    public String getUriFromUpgradePackageFdn(final String nodeName, final String upgradePackageMoFdn) {
        try {
            final String uri = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
                @Override
                public String execute() throws UnsupportedFragmentException {
                    return upMoService.getUriFromUpgradePackageFdn(nodeName, upgradePackageMoFdn);
                }
            });
            return uri;
        } catch (final RetriableCommandException ex) {
            throw ex;
        }
    }

    public String getFilePath(final EcimUpgradeInfo ecimUpgradeInfo) {
        try {
            final String filePath = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
                @Override
                public String execute() throws MoNotFoundException, ArgumentBuilderException {
                    return upMoService.getFilePath(ecimUpgradeInfo);
                }
            });
            return filePath;
        } catch (final RetriableCommandException ex) {
            throw ex;
        }
    }

    public Map<String, String> getActiveSoftwareDetailsFromNode(final String nodeName) throws UnsupportedFragmentException, MoNotFoundException {
        try {
            final Map<String, String> activeSoftwareDetails = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, String>>() {
                @Override
                public Map<String, String> execute() throws UnsupportedFragmentException, MoNotFoundException {
                    return upMoService.getActiveSoftwareDetailsFromNode(nodeName);
                }
            });
            return activeSoftwareDetails;
        } catch (final RetriableCommandException ex) {
            throw ex;
        }
    }

}
