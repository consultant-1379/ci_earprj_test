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
package com.ericsson.oss.services.shm.backupservice.ecim.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.services.shm.backupservice.remote.api.BackupManagementServiceException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.notifications.api.Notification;

/**
 * This class facilitates the Confirm Restore functionality on ECIM node.
 * 
 * @author xsrabop
 * 
 */
public class ConfirmRestoreRemoteServiceImpl extends BackUpManagementAbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmRestoreRemoteServiceImpl.class);

    /**
     * This method validates whether Confirm Restore is required on the node or not
     * 
     * @param neName
     * @param ecimBackupInfo
     * @return boolean(true/false)
     * 
     */
    @Override
    public boolean precheck(final String nodeName, final EcimBackupInfo ecimBackupInfo) throws BackupManagementServiceException {
        LOGGER.debug("Entered into ConfirmRestoreRemoteServiceImpl:precheck() method with node : {} and backupName : {}", nodeName, ecimBackupInfo.getBackupName());
        try {
            final boolean isSpecifiedBackupRestored = brmMoServiceRetryProxy.isSpecifiedBackupRestored(nodeName, ecimBackupInfo);
            if (isSpecifiedBackupRestored) {
                LOGGER.debug("Precheck for confirm restore action is successfull on the node {}", nodeName);
                return true;
            } else {
                LOGGER.error("Precheck Action has Failed on the node due to specified backup is not restored on the node.");
                return false;
            }
        } catch (final MoNotFoundException e) {
            LOGGER.error("Precheck for Confirm Restore Action has failed on the node due to MoNotFoundException ", e);
            throw new BackupManagementServiceException("Precheck Action for Confirm Restore has failed on the node due to MoNotFoundException. Exception is : " + e + " backupName : "
                    + ecimBackupInfo.getBackupName() + " domain : " + ecimBackupInfo.getDomainName() + " type : " + ecimBackupInfo.getBackupType() + " node : " + nodeName);

        } catch (final UnsupportedFragmentException e1) {
            LOGGER.error("Precheck Action has failed for Confirm Restore on the node due to UnsupportedFragmentException ", e1);
            throw new BackupManagementServiceException("Precheck Action has failed on the node due to UnsupportedFragmentException. Exception is : " + e1 + "backupName : "
                    + ecimBackupInfo.getBackupName() + "domain : " + ecimBackupInfo.getDomainName() + " type : " + ecimBackupInfo.getBackupType() + " node : " + nodeName);
        } catch (final Exception e) {
            LOGGER.error("Precheck Action has failed for Confirm Restore on the node due to", e);
            throw new BackupManagementServiceException("Precheck Action has failed for Confirm Restore on the node due to ", e);
        }
    }

    /**
     * This method is the activity execute step called when pre-check stage is passed. This registers for notifications, initiates and performs the MO action on the node.
     * 
     * @param neName
     * @param ecimBackupInfo
     * @return integer
     * 
     */
    @Override
    public int executeMoAction(final String nodeName, final EcimBackupInfo ecimBackUpInfo) throws BackupManagementServiceException {
        LOGGER.debug("Entered into ConfirmRestoreRemoteServiceImpl:executeMoAction() method with node : {} and backupName : {}", nodeName, ecimBackUpInfo.getBackupName());
        int actionInvocationResult = -1;
        try {
            final String brmRollbackAtRestoreMoFdn = brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.CONFIRM_RESTORE, nodeName, null);

            if (brmRollbackAtRestoreMoFdn == null) {
                LOGGER.error("BRM RollBack At Restore Mo Fdn MO does not exist for the supplied node name : {}", nodeName);
                throw new BackupManagementServiceException("BRM RollBack At Restore Mo Fdn MO does not exist for the supplied node name : " + nodeName);
            }
            systemRecorder.recordCommand(SHMEvents.CONFIRM_RESTORE_EXECUTE, CommandPhase.STARTED, nodeName, brmRollbackAtRestoreMoFdn, ":Proceeding to Confirm Restore by service request");

            activityUtils.recordEvent(SHMEvents.CONFIRM_SERVICE, nodeName, brmRollbackAtRestoreMoFdn, "SHM:" + nodeName + ":Proceeding to Confirm Restore by service request");
            actionInvocationResult = executeAction(nodeName, null, brmRollbackAtRestoreMoFdn, EcimBackupConstants.CONFIRM_RESTORE);
            LOGGER.debug("Confirm Restore Action triggered successfully for the node:{}", nodeName);

            if (actionInvocationResult == 0) {
                LOGGER.debug("Execution of Confirm Restore activity completed successfully on the node : {}", nodeName);
                return actionInvocationResult;
            } else {
                LOGGER.error("Execution of Confirm Restore activity failed on the node: {}", nodeName);
                throw new BackupManagementServiceException("Confirm Restore Action has Failed");
            }
        } catch (final Exception e) {
            LOGGER.error("Confirm Restore action has Failed due to", e);
            throw new BackupManagementServiceException("Confirm Restore Action has Failed due to ", e);
        }
    }

    /**
     * Confirm Restore is Synchronous Action. So for Synchronous actions there is no need of Notifications
     * 
     * @param Notification
     * 
     */
    @Override
    public void processNotification(final Notification notification) {

    }

}
