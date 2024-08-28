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
package com.ericsson.oss.services.shm.es.ecim.backup.common;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.services.shm.common.exception.BrmBackupMoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.BrmBackupStatusInCompleteException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.retry.BrmBkpStatusInCompleteRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackup;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;

/**
 * Retry Proxy class for retrying few BrM/BackupManager/BrmBackup MO operations
 * 
 * @author xswagud
 * 
 */

public class BrmMoServiceRetryProxy extends BrmMoActionArgumentsRetryProxy {

    @Inject
    private BrmBkpStatusInCompleteRetryPolicies brmBkpStatusInCompleteRetryPolicies;

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

    public String getNotifiableMoFdn(final String activityName, final String nodeName, final EcimBackupInfo ecimBackupInfo) throws MoNotFoundException, UnsupportedFragmentException {
        try {
            final String fdn = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
                @Override
                public String execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.getNotifiableMoFdn(activityName, nodeName, ecimBackupInfo);
                }
            });
            return fdn;
        } catch (final RetriableCommandException ex) {
            if (ex.getCause() instanceof BrmBackupMoNotFoundException) {
                throw new BrmBackupMoNotFoundException(ex.getCause().getMessage());
            }
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This Method will apply retry logic of retrieving progress information call , if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param nodeName
     * @param activityName
     * @param modifiedAttributes
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */

    public AsyncActionProgress getValidAsyncActionProgress(final String nodeName, final String activityName, final Map<String, AttributeChangeData> modifiedAttributes)
            throws MoNotFoundException, UnsupportedFragmentException {
        try {
            final AsyncActionProgress actionProgress = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<AsyncActionProgress>() {
                @Override
                public AsyncActionProgress execute() throws UnsupportedFragmentException, MoNotFoundException {
                    return brmMoService.getValidAsyncActionProgress(nodeName, activityName, modifiedAttributes);
                }
            });
            return actionProgress;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This API will apply retry logic for retrieving progress information call, if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param nodeName
     * @param activityName
     * @param ecimBackupInfo
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */

    public AsyncActionProgress getAsyncActionProgressFromBrmBackupForSpecificActivity(final String nodeName, final String activityName, final EcimBackupInfo ecimBackupInfo)
            throws MoNotFoundException, UnsupportedFragmentException {
        try {
            final AsyncActionProgress actionProgress = retryManager.executeCommand(dpsRetryPolicies.getReadAttributesRetryPolicy(), new ShmDpsRetriableCommand<AsyncActionProgress>() {
                @Override
                public AsyncActionProgress execute() throws UnsupportedFragmentException, MoNotFoundException {
                    return brmMoService.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, activityName, ecimBackupInfo);
                }
            });
            return actionProgress;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This API will apply retry logic for backup retrieval call of specific node, if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param nodeName
     * @param ecimBackupInfo
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */

    public boolean isBackupExist(final String nodeName, final EcimBackupInfo ecimBackupInfo) throws MoNotFoundException, UnsupportedFragmentException {
        try {
            final boolean isBackUPExist = retryManager.executeCommand(dpsRetryPolicies.getReadAttributesRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.isBackupExist(nodeName, ecimBackupInfo);
                }
            });
            return isBackUPExist;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This method is to execute retries when status attribute in BrmBackup MO is not updated.
     * 
     */
    public boolean isBackupExistsWithStatusComplete(final String neName, final EcimBackupInfo ecimBackupInfo)
            throws BrmBackupStatusInCompleteException, MoNotFoundException, UnsupportedFragmentException {
        try {
            final boolean isBackUPExist = retryManager.executeCommand(brmBkpStatusInCompleteRetryPolicies.getBrmBkpStatusInCompleteRetryPolicy(), new RetriableCommand<Boolean>() {
                @Override
                public Boolean execute(final RetryContext retryContext) throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.isBackupExistsWithStatusComplete(neName, ecimBackupInfo);
                }
            });
            return isBackUPExist;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This will apply retry logic for triggering an action ,if this will get DatabaseNotAvailableException from brmMoService
     * 
     * @param nodeName
     * @param ecimBackupInfo
     * @param moFdn
     * @param actionType
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     * @throws ArgumentBuilderException
     */

    public int executeMoAction(final String nodeName, final EcimBackupInfo ecimBackupInfo, final String moFdn, final String actionType)
            throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        try {
            final int actionid = retryManager.executeCommand(moActionRetryPolicy.getDpsMoActionRetryPolicy(), new ShmDpsRetriableCommand<Integer>() {
                @Override
                public Integer execute() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
                    return brmMoService.executeMoAction(nodeName, ecimBackupInfo, moFdn, actionType);
                }
            });
            return actionid;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This will apply retry logic for triggering a cancel MO action ,if this will get DatabaseNotAvailableException from brmMoService
     * 
     * @param nodeName
     * @param moFdn
     * @param actionName
     * @param actionArguments
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     * @throws ArgumentBuilderException
     */
    public int executeCancelAction(final String nodeName, final String moFdn, final String actionName, final Map<String, Object> actionArguments)
            throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        try {
            final int actionid = retryManager.executeCommand(moActionRetryPolicy.getDpsMoActionRetryPolicy(), new ShmDpsRetriableCommand<Integer>() {
                @Override
                public Integer execute() throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException {
                    return brmMoService.executeCancelAction(nodeName, moFdn, actionName, actionArguments);
                }
            });
            return actionid;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * this API will apply retry logic when database call fails because of database service un-availability
     * 
     * @param nodeName
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    public boolean isConfirmRequired(final String nodeName) throws MoNotFoundException, UnsupportedFragmentException {
        try {
            final boolean isConfirmRequired = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.isConfirmRequired(nodeName);
                }
            });
            return isConfirmRequired;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * this API will apply retry logic when database call fails because of database service un-availability
     * 
     * @param nodeName
     * @param ecimBackupInfo
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */

    public boolean isSpecifiedBackupRestored(final String nodeName, final EcimBackupInfo ecimBackupInfo) throws MoNotFoundException, UnsupportedFragmentException {
        try {
            final boolean isGivenBackUpRestored = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.isSpecifiedBackupRestored(nodeName, ecimBackupInfo);
                }
            });
            return isGivenBackUpRestored;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This API will apply retry logic for retrieving progress information call, if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param nodeName
     * @param ecimBackupInfo
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */

    public AsyncActionProgress getProgressFromBrmBackupManagerMO(final String nodeName, final EcimBackupInfo ecimBackupInfo) throws MoNotFoundException, UnsupportedFragmentException {
        try {
            final AsyncActionProgress asyncActionProgress = retryManager.executeCommand(dpsRetryPolicies.getReadAttributesRetryPolicy(), new ShmDpsRetriableCommand<AsyncActionProgress>() {
                @Override
                public AsyncActionProgress execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.getProgressFromBrmBackupManagerMO(nodeName, ecimBackupInfo);
                }
            });
            return asyncActionProgress;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This API will apply retry logic for backup retrieval call, if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param backupNameList
     * @param nodeName
     * @param domainName
     * @param backupType
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */

    public List<String> getBackupDetails(final List<String> backupNameList, final String nodeName, final String domainName, final String backupType)
            throws MoNotFoundException, UnsupportedFragmentException {
        try {
            final List<String> backUpDetails = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<List<String>>() {
                @Override
                public List<String> execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.getBackupDetails(backupNameList, nodeName, domainName, backupType);
                }
            });
            return backUpDetails;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This API will apply retry logic for retrieving action status , if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param nodeName
     * @param ecimBackupInfo
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    public boolean isBackupDeletionCompleted(final String nodeName, final EcimBackupInfo ecimBackupInfo) throws MoNotFoundException, UnsupportedFragmentException {
        try {
            final boolean bkpDeletionCompleted = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.isBackupDeletionCompleted(nodeName, ecimBackupInfo);
                }
            });
            return bkpDeletionCompleted;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This API will apply retry logic for retrieving action status , if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param backupname
     * @param brmBackupManagerMoFdn
     * @param nodeName
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    public boolean isBackupDeletionCompleted(final String backupname, final String brmBackupManagerMoFdn, final String nodeName) throws MoNotFoundException, UnsupportedFragmentException {
        try {
            final boolean bkpDeletionCompleted = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.isBackupDeletionCompleted(backupname, brmBackupManagerMoFdn, nodeName);
                }
            });
            return bkpDeletionCompleted;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * 
     * This API will apply retry logic for retrieving Brm Fragment Version , if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param nodeName
     * @return
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     */

    public String getBrmFragmentVersion(final String nodeName) throws UnsupportedFragmentException, MoNotFoundException {
        try {
            final String brmFragmentVersion = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
                @Override
                public String execute() throws UnsupportedFragmentException, MoNotFoundException {
                    return brmMoService.getBrmFragmentVersion(nodeName);
                }
            });
            return brmFragmentVersion;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * 
     * This API will apply retry logic for retrieving BrmBackups , if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param nodeName
     * @return List of BrmBackups
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     */

    public List<BrmBackup> getBrmBackups(final String nodeName) throws UnsupportedFragmentException, MoNotFoundException {
        try {
            final List<BrmBackup> brmBackups = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<List<BrmBackup>>() {
                @Override
                public List<BrmBackup> execute() throws UnsupportedFragmentException, MoNotFoundException {
                    return brmMoService.getBrmBackups(nodeName);
                }
            });
            return brmBackups;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * 
     * This API will apply retry logic for retrieving BrmBackups , if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param nodeName
     * @return List of BrmBackups
     * @throws BackupNotFoundException
     */

    public List<BrmBackup> getAllBrmBackups(final String nodeName) throws BackupNotFoundException {
        try {
            return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<List<BrmBackup>>() {
                @Override
                public List<BrmBackup> execute() throws BackupNotFoundException {
                    return brmMoService.getAllBrmBackups(nodeName);
                }
            });
        } catch (final RetriableCommandException ex) {
            throw new BackupNotFoundException(ex);
        }
    }

    /**
     * This API will apply retry logic for retrieving Brm Failsafe Backup MO , if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param nodeName
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    public ManagedObject getBrmFailsafeBackupMo(final String nodeName) throws MoNotFoundException, UnsupportedFragmentException {
        try {
            final ManagedObject brmFailSafeBkpUpMo = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<ManagedObject>() {
                @Override
                public ManagedObject execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.getBrmFailsafeBackupMo(nodeName);
                }
            });
            return brmFailSafeBkpUpMo;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This API will apply retry logic for activate action trigger on Brm FailSafe Brm MO , if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param nodeName
     * @param brmFailsafeBackupMoFdn
     * @return
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     */
    public int performBrmFailSafeActivate(final String nodeName, final String brmFailsafeBackupMoFdn) throws UnsupportedFragmentException, MoNotFoundException {
        try {
            final int actionId = retryManager.executeCommand(moActionRetryPolicy.getDpsMoActionRetryPolicy(), new ShmDpsRetriableCommand<Integer>() {
                @Override
                public Integer execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.performBrmFailSafeActivate(nodeName, brmFailsafeBackupMoFdn);
                }
            });
            return actionId;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * 
     * This API will apply retry logic De-activate action trigger on Brm FailSafe Brm MO , if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param nodeName
     * @param brmFailsafeBackupMoFdn
     * @return
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     */
    public int performBrmFailSafeDeActivate(final String nodeName, final String brmFailsafeBackupMoFdn) throws UnsupportedFragmentException, MoNotFoundException {
        try {
            final int actionId = retryManager.executeCommand(moActionRetryPolicy.getDpsMoActionRetryPolicy(), new ShmDpsRetriableCommand<Integer>() {
                @Override
                public Integer execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.performBrmFailSafeDeActivate(nodeName, brmFailsafeBackupMoFdn);
                }
            });
            return actionId;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This API will apply retry logic for retrieving progress information call, if this will get DatabaseNotAvailableException from brmMoService.
     * 
     * @param nodeName
     * @param activityName
     * @param ecimBackupInfo
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    public boolean validateActionProgressReport(final String nodeName, final AsyncActionProgress progressReport, final String activityName) throws UnsupportedFragmentException, MoNotFoundException {
        try {
            final boolean isValidProgressReport = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
                @Override
                public Boolean execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.validateActionProgressReport(nodeName, progressReport, activityName);
                }
            });
            return isValidProgressReport;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * This will performs retries of retrieving of progress report information, if it fails because of DatabaseNotAvailableException.
     * 
     * @param nodeName
     * @return AsyncActionProgress
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     */

    public AsyncActionProgress getActionProgressOfBrmfailsafeMO(final String nodeName) throws UnsupportedFragmentException, MoNotFoundException {

        try {
            final AsyncActionProgress asyncActionProgress = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<AsyncActionProgress>() {
                @Override
                public AsyncActionProgress execute() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
                    return brmMoService.getActionProgressOfBrmFailsafeMO(nodeName);
                }
            });
            return asyncActionProgress;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }

    /**
     * Method to retrieve backup name from BrmBackupMO FDN.
     * 
     * @param brmBackupFdn
     * @return backupName
     */
    public String getBackupNameFromBrmBackupMOFdn(final String brmBackupMoFdn) throws MoNotFoundException, UnsupportedFragmentException {
        try {
            final String backupName = retryManager.executeCommand(dpsRetryPolicies.getReadAttributesRetryPolicy(), new ShmDpsRetriableCommand<String>() {
                @Override
                public String execute() throws MoNotFoundException, UnsupportedFragmentException {
                    return brmMoService.getBackupNameFromBrmBackupMOFdn(brmBackupMoFdn);
                }
            });
            return backupName;
        } catch (final RetriableCommandException ex) {
            handleException(ex);
            throw ex;
        }
    }
}
