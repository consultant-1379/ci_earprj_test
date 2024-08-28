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

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.services.shm.backupservice.remote.api.BackupManagementServiceException;
import com.ericsson.oss.services.shm.backupservice.remote.impl.RemoteActivityCompleteTimer;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.RemoteActivityInfo;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.notification.common.NotificationInformation;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationCallbackResult;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

/**
 * This class facilitates the functionality of uploading the backup stored on ECIM node to an external location.
 * 
 * @author xsrabop
 * 
 */

@RemoteActivityInfo(activityName = EcimBackupConstants.RESTORE_BACKUP, jobType = JobTypeEnum.RESTORE, platform = PlatformTypeEnum.ECIM)
public class RestoreBackupRemoteServiceImpl extends BackUpManagementAbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreBackupRemoteServiceImpl.class);

    @Inject
    private ConfirmRestoreRemoteServiceImpl confirmRestoreRemoteServiceImpl;

    @Inject
    protected RemoteActivityCompleteTimer remoteActivityCompleteTimer;

    /**
     * This method validates backup upload request to verify whether the creation of Backup on the node is completed or not
     * 
     * @param neName
     * @param ecimBackupInfo
     * @return boolean(true/false)
     * 
     */
    @Override
    public boolean precheck(final String nodeName, final EcimBackupInfo ecimBackupInfo) throws BackupManagementServiceException {
        LOGGER.debug("Entered into RestoreBackupRemoteServiceImpl:precheck() method with node : {} and backupName : {}", nodeName, ecimBackupInfo.getBackupName());
        try {
            if (brmMoServiceRetryProxy.isBackupExist(nodeName, ecimBackupInfo)) {
                LOGGER.debug("Precheck for restore action is successfull on the node.");
                return true;
            } else {
                LOGGER.error("Precheck Action has Failed on the node due to Backup is present in improper state.");
                return false;
            }
        } catch (final MoNotFoundException e) {
            LOGGER.error("Precheck Action has failed on the node due to MoNotFoundException ", e);
            throw new BackupManagementServiceException("Precheck Action has failed on the node due to MoNotFoundException. Exception is : " + e + " backupName : " + ecimBackupInfo.getBackupName()
                    + " domain : " + ecimBackupInfo.getDomainName() + " type : " + ecimBackupInfo.getBackupType() + " node : " + nodeName);

        } catch (final UnsupportedFragmentException e1) {
            LOGGER.error("Precheck Action has failed on the node due to UnsupportedFragmentException ", e1);
            throw new BackupManagementServiceException("Precheck Action has failed on the node due to UnsupportedFragmentException. Exception is : " + e1 + "backupName : "
                    + ecimBackupInfo.getBackupName() + "domain : " + ecimBackupInfo.getDomainName() + " type : " + ecimBackupInfo.getBackupType() + " node : " + nodeName);
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
        LOGGER.debug("Entered into RestoreBackupRemoteServiceImpl:executeMoAction() method with node : {} and backupName : {}", nodeName, ecimBackUpInfo.getBackupName());
        final String brmBackupMoFdn = getNotifiableFdn(EcimBackupConstants.RESTORE_BACKUP, nodeName, ecimBackUpInfo);
        int actionInvocationResult = -1;
        if (brmBackupMoFdn == null) {
            LOGGER.error("BRM MO does not exist for the supplied node name:{}", nodeName);
            throw new BackupManagementServiceException("BRM MO does not exist for the supplied node name:" + nodeName);
        }
        final FdnNotificationSubject fdnNotificationSubject = remoteActivityNotificationHelper.subscribeToNotification(nodeName, brmBackupMoFdn, RestoreBackupRemoteServiceImpl.class, ecimBackUpInfo);
        try {
            systemRecorder.recordCommand(SHMEvents.RESTORE_BACKUP_EXECUTE, CommandPhase.STARTED, nodeName, brmBackupMoFdn, ":Proceeding to retore backup by service request");
            activityUtils.recordEvent(SHMEvents.RESTORE_SERVICE, nodeName, brmBackupMoFdn, "SHM:" + nodeName + ":Proceeding to restore backup by service request");
            actionInvocationResult = executeAction(nodeName, ecimBackUpInfo, brmBackupMoFdn, EcimBackupConstants.RESTORE_BACKUP);
            LOGGER.debug("Restore Backup Action triggered successfully for the node:{}", nodeName);
            return actionInvocationResult;
        } catch (final Exception e) {
            LOGGER.error("Restore Backup has Failed due to", e);
            throw new BackupManagementServiceException("Restore Backup Action has Failed due to ", e);
        } finally {
            if (isActionInvocationSuccess(actionInvocationResult)) {
                LOGGER.debug("Restore action triggered successfuly. Starting wait timer");
                remoteActivityCompleteTimer.startTimer(fdnNotificationSubject);
            } else {
                remoteActivityNotificationHelper.unSubscribeToNotification(fdnNotificationSubject, fdnNotificationSubject.getFdn());
            }
        }
    }

    /**
     * This method checks whether actionInveocationResult is equal to zero. If zero then returns true else false.
     * 
     * @param actionInveocationResult
     * @return boolean
     * 
     */
    private boolean isActionInvocationSuccess(final int actionInvocationResult) {
        return actionInvocationResult == 0;
    }

    /**
     * This method processes the notifications on change of DpsAttributeChangedEvent and set the status in DB
     * 
     * @param Notification
     * 
     */
    @Override
    public void processNotification(final Notification notification) {
        LOGGER.debug("Entered into RestoreBackupRemoteServiceImpl:processNotification() method.");
        if (!(notification.getDpsDataChangedEvent() instanceof DpsAttributeChangedEvent)) {
            return;
        }
        final DpsAttributeChangedEvent event = (DpsAttributeChangedEvent) notification.getDpsDataChangedEvent();
        final FdnNotificationSubject fdnNotificationSubject = (FdnNotificationSubject) notification.getNotificationSubject();
        final NotificationInformation notificationInformation = remoteActivityNotificationHelper.getNotificationInformation(fdnNotificationSubject.getKey());
        LOGGER.debug("Before calling processNotification");
        processPayLoad(event, notificationInformation, fdnNotificationSubject);

    }

    /**
     * This method processes the notifications on change of DpsAttributeChangedEvent and set the status in DB
     * 
     * @param DpsAttributeChangedEvent
     * @param NotificationInformation
     * @param FdnNotificationSubject
     */
    public void processPayLoad(final DpsAttributeChangedEvent event, final NotificationInformation notificationInformation, final FdnNotificationSubject fdnNotoficationSubject) {
        LOGGER.debug("Entered into RestoreBackupRemoteServiceImpl:processPayLoad() method with nodeName : {}", notificationInformation.getNodeName());
        final NotificationCallbackResult result = notificationInformation.getNotificationCallbackResult();
        final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(event);
        try {
            final AsyncActionProgress progressReport = brmMoServiceRetryProxy
                    .getValidAsyncActionProgress(notificationInformation.getNodeName(), EcimBackupConstants.RESTORE_BACKUP, modifiedAttributes);
            LOGGER.debug("Information about Progress Action {} ", progressReport);
            final ActionStateType state = progressReport.getState();
            LOGGER.debug("Progress Report State is : {} with action name : {}", progressReport.getState(), progressReport.getActionName());
            final String logMessage = String.format(JobLogConstants.PROGRESS_INFORMATION, progressReport.getActionName(), progressReport.getProgressPercentage(), progressReport.getState());
            switch (state) {
            case RUNNING:
                LOGGER.debug("Case Running: {}", logMessage);
                break;
            case FINISHED:
                LOGGER.debug("Case Finished: {}", logMessage);
                remoteActivityNotificationHelper.unSubscribeToNotification(fdnNotoficationSubject, fdnNotoficationSubject.getFdn());
                result.setCompleted(true);
                processRestoreCompletion(notificationInformation, fdnNotoficationSubject, result, progressReport);
                break;
            default:
                LOGGER.warn("Unsupported Action State Type {}", state);
            }
        } catch (UnsupportedFragmentException | MoNotFoundException e) {
            LOGGER.error("Exception occured while performing Restore Backup action on the node : {}.", notificationInformation.getNodeName());
        } catch (final BackupManagementServiceException backupManagementServiceException) {
            LOGGER.error("Exception occured while performing Restore Backup action on the node : {}", backupManagementServiceException.getMessage());
        }
    }

    /**
     * This method checks whether Restore action is successful on the node or node. If successful then invokes Confirm Restore action.
     * 
     * @param notificationInformation
     * @param fdnNotoficationSubject
     * @param result
     * @param progressReport
     */
    private void processRestoreCompletion(final NotificationInformation notificationInformation, final FdnNotificationSubject fdnNotoficationSubject, final NotificationCallbackResult result,
            final AsyncActionProgress progressReport) throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {
        result.setCompleted(true);
        if (isActivitySuccess(progressReport)) {
            result.setSuccess(true);
            systemRecorder.recordCommand(SHMEvents.RESTORE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, notificationInformation.getNodeName(), fdnNotoficationSubject.getFdn(),
                    ":Restore action is successful on the node.");
            activityUtils.recordEvent(SHMEvents.RESTORE_SERVICE, notificationInformation.getNodeName(), fdnNotoficationSubject.getFdn(), "SHM:" + notificationInformation.getNodeName()
                    + ":Restore action is successful on the node.");
            invokeConfirmRestore(notificationInformation);
        } else if (isActivityFailed(progressReport)) {
            result.setSuccess(false);
            notificationInformation.setFailureMessage("Result Info : " + progressReport.getResultInfo() + "Additional Info : " + progressReport.getAdditionalInfo());
            systemRecorder.recordCommand(SHMEvents.RESTORE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, notificationInformation.getNodeName(), fdnNotoficationSubject.getFdn(),
                    ":Restore action is failed on the node.");
            activityUtils.recordEvent(SHMEvents.RESTORE_SERVICE, notificationInformation.getNodeName(), fdnNotoficationSubject.getFdn(), "SHM:" + notificationInformation.getNodeName()
                    + ":Restore action is failed on the node.");
        }
    }

    /**
     * This method checks whether the result in progress report is FAILURE.
     * 
     * @param progressReport
     * @return boolean
     */
    private boolean isActivityFailed(final AsyncActionProgress progressReport) {
        return ActionResultType.FAILURE.equals(progressReport.getResult());
    }

    /**
     * This method checks whether the result in progress report is SUCCESS.
     * 
     * @param progressReport
     * @return boolean
     */
    private boolean isActivitySuccess(final AsyncActionProgress progressReport) {
        return ActionResultType.SUCCESS.equals(progressReport.getResult());
    }

    /**
     * This method checks whether Confirm Restore is required on the node or not and then triggers Confirm Restore action.
     * 
     * @param nodeName
     * @throws BackupManagementServiceException
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     */
    private void invokeConfirmRestore(final NotificationInformation notificationInformation) throws BackupManagementServiceException, MoNotFoundException, UnsupportedFragmentException {
        if (notificationInformation.getEcimBackupInfo() == null) {
            LOGGER.error("Exception occured while triggering Confirm Restore Action on node {} as backup information doesn't exist.", notificationInformation.getNodeName());
            throw new BackupManagementServiceException("Exception occured while triggering Confirm Restore Action as backup information doesn't exist.");
        }
        if (brmMoServiceRetryProxy.isConfirmRequired(notificationInformation.getNodeName())) {
            if (confirmRestoreRemoteServiceImpl.precheck(notificationInformation.getNodeName(), notificationInformation.getEcimBackupInfo())) {
                if (confirmRestoreRemoteServiceImpl.executeMoAction(notificationInformation.getNodeName(), notificationInformation.getEcimBackupInfo()) == 0) {
                    LOGGER.debug("Confirm Restore action of backup {} is successful on the node with node name : {} .", notificationInformation.getEcimBackupInfo().getBackupName(),
                            notificationInformation.getNodeName());
                } else {
                    LOGGER.error("Confirm Restore Action is failed on the node : {}", notificationInformation.getNodeName());
                    throw new BackupManagementServiceException("Confirm Restore Action is failed on the node. " + notificationInformation.getFailureMessage());
                }
            }
        } else {
            LOGGER.debug("Restore Backup action is successful on the node and Confirm Restore is not required.");
        }
    }
}
