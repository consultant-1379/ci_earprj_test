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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.services.shm.backupservice.remote.api.BackupManagementServiceException;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.BrmBackupStatusInCompleteException;
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
 * @author tcsnean
 * 
 */

@RemoteActivityInfo(activityName = EcimBackupConstants.UPLOAD_BACKUP, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
public class UploadBackUpRemoteServiceImpl extends BackUpManagementAbstractService {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    /**
     * This method validates backup upload request to verify whether the creation of Backup on the node is completed or not
     * 
     * @param neName
     * @param ecimBackupInfo
     * @return boolean(true/false)
     * 
     */

    @Override
    public boolean precheck(final String neName, final EcimBackupInfo ecimBackupInfo) throws BackupManagementServiceException {
        try {
            if (brmMoServiceRetryProxy.isBackupExist(neName, ecimBackupInfo)) {
                LOGGER.info("Precheck Action has Success on the node {} due to Backup  present in proper state", neName);
                return true;
            } else {
                try {
                    if (brmMoServiceRetryProxy.isBackupExistsWithStatusComplete(neName, ecimBackupInfo)) {
                        LOGGER.info("Precheck Action has Success on the node  {} due to Backup  present in proper state", neName);
                        return true;
                    }
                } catch (BrmBackupStatusInCompleteException e) {
                    LOGGER.error("Precheck Action has Failed on the node {} due to Backup is present in improper state. Exception is {}", neName, e);
                }
                return false;
            }
        } catch (final MoNotFoundException e) {
            LOGGER.error("Precheck Action has failed on the node due to MoNotFoundException ", e);
            throw new BackupManagementServiceException("Precheck Action has failed on the node due to MoNotFoundException. Exception is : " + e + " backupName : " + ecimBackupInfo.getBackupName()
                    + " domain : " + ecimBackupInfo.getDomainName() + " type : " + ecimBackupInfo.getBackupType() + " node : " + neName);

        } catch (final UnsupportedFragmentException e1) {
            LOGGER.error("Precheck Action has failed on the node due to UnsupportedFragmentException ", e1);
            throw new BackupManagementServiceException("Precheck Action has failed on the node due to UnsupportedFragmentException. Exception is : " + e1 + "backupName : "
                    + ecimBackupInfo.getBackupName() + "domain : " + ecimBackupInfo.getDomainName() + " type : " + ecimBackupInfo.getBackupType() + " node : " + neName);

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

        final String bmMoFdn = getNotifiableFdn(EcimBackupConstants.UPLOAD_BACKUP, nodeName, ecimBackUpInfo);
        final FdnNotificationSubject fdnNotificationSubject = remoteActivityNotificationHelper.subscribeToNotification(nodeName, bmMoFdn, UploadBackUpRemoteServiceImpl.class);
        try {
            if ((bmMoFdn == null) || (("").equals(bmMoFdn))) {
                LOGGER.error("BM MO does not exist for the supplied node name:{}", nodeName);
                throw new BackupManagementServiceException("BM MO does not exist for the supplied node name:" + nodeName);
            }
            systemRecorder.recordCommand(SHMEvents.UPLOAD_BACKUP_SERVICE_REQUEST_ACTION, CommandPhase.STARTED, nodeName, bmMoFdn, ":Proceeding to upload CV by service request");
            LOGGER.debug("Registering for Notification is finished on the Node {}", nodeName);
            activityUtils.recordEvent(SHMEvents.UPLOAD_BACKUP_SERVICE_REQUEST_ACTION, nodeName, bmMoFdn, "SHM:" + nodeName + ":Proceeding to upload backup by service request");
            executeAction(nodeName, ecimBackUpInfo, bmMoFdn, EcimBackupConstants.UPLOAD_BACKUP);
            LOGGER.debug("Upload BM Action triggered successfully for the node:{}", nodeName);
            return getActionCompletionStatus(nodeName, ecimBackUpInfo, bmMoFdn);
        } catch (final Exception e) {
            LOGGER.error("Backup Uploading has Failed due to", e);
            throw new BackupManagementServiceException(e.getMessage());

        } finally {
            try {
                remoteActivityNotificationHelper.unSubscribeToNotification(fdnNotificationSubject, bmMoFdn);
            } catch (final Exception e) {
                LOGGER.error("Unable to remove subject", e);
            }
        }

    }

    /**
     * @param nodeName
     * @param ecimBackUpInfo
     * @param bmMoFdn
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     * @throws BackupManagementServiceException
     */
    private int getActionCompletionStatus(final String nodeName, final EcimBackupInfo ecimBackUpInfo, final String bmMoFdn)
            throws MoNotFoundException, UnsupportedFragmentException, BackupManagementServiceException {

        final NotificationCallbackResult notificationCallbackResult = remoteActivityNotificationHelper.waitForProcessNotifications(bmMoFdn);
        return evaluateActionStatus(nodeName, ecimBackUpInfo, bmMoFdn, notificationCallbackResult);

    }

    /**
     * @param nodeName
     * @param ecimBackUpInfo
     * @param bmMoFdn
     * @param notificationCallbackResult
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     * @throws BackupManagementServiceException
     */
    private int evaluateActionStatus(final String nodeName, final EcimBackupInfo ecimBackUpInfo, final String bmMoFdn, final NotificationCallbackResult notificationCallbackResult)
            throws MoNotFoundException, UnsupportedFragmentException, BackupManagementServiceException {
        if (notificationCallbackResult.isSuccess()) {
            LOGGER.debug("notificationCallbackResult Success on node = {} for activity name {}", nodeName, EcimBackupConstants.UPLOAD_BACKUP);
            return 1;
        } else {
            if (notificationCallbackResult.isActionTimedOut()) {
                final AsyncActionProgress progressReport = brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.UPLOAD_BACKUP, ecimBackUpInfo);
                return onActionTimeOut(bmMoFdn, progressReport);
            }
            return 0;
        }
    }

    /**
     * @param bmMoFdn
     * @param progressReport
     * @return
     * @throws BackupManagementServiceException
     */
    private int onActionTimeOut(final String bmMoFdn, final AsyncActionProgress progressReport) throws BackupManagementServiceException {
        if (progressReport != null && !isUploadedSuccess(progressReport)) {
            final NotificationInformation notificationInformation = remoteActivityNotificationHelper.getNotificationInformation(bmMoFdn);
            LOGGER.error("Upload Backup action failed on Node '{}' Result Info : {} Additional Info : {}", notificationInformation.getNodeName(), progressReport.getResultInfo(),
                    progressReport.getAdditionalInfo());
            throw new BackupManagementServiceException(
                    "Upload Backup action failed on Node :" + "Result Info : {} " + progressReport.getResultInfo() + "Additional Info : " + progressReport.getAdditionalInfo());
        } else {
            return 1;
        }
    }

    @Override
    public void processNotification(final Notification notification) {
        LOGGER.debug("Entered in processNotification");
        if (!(notification.getDpsDataChangedEvent() instanceof DpsAttributeChangedEvent)) {
            return;
        }
        final DpsAttributeChangedEvent event = (DpsAttributeChangedEvent) notification.getDpsDataChangedEvent();
        final FdnNotificationSubject subject = (FdnNotificationSubject) notification.getNotificationSubject();
        final NotificationInformation notificationInformation = remoteActivityNotificationHelper.getNotificationInformation(subject.getKey());
        LOGGER.debug("Before calling processNotification");
        processPayLoad(event, notificationInformation);

    }

    /**
     * This method processes the notifications on change of DpsAttributeChangedEvent and set the status in DB
     * 
     * @param FdnNotificationSubject
     * @param DpsAttributeChangedEvent
     * 
     */

    private void processPayLoad(final DpsAttributeChangedEvent event, final NotificationInformation notificationInformation) {
        final NotificationCallbackResult result = notificationInformation.getNotificationCallbackResult();
        final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(event);
        try {
            final AsyncActionProgress progressReport = brmMoServiceRetryProxy.getValidAsyncActionProgress(notificationInformation.getNodeName(), EcimBackupConstants.UPLOAD_BACKUP, modifiedAttributes);
            LOGGER.debug("Information about Progress Action :{}", progressReport);
            final ActionStateType state = progressReport.getState();
            switch (state) {
            case RUNNING:
                final String logMessage = String.format(JobLogConstants.PROGRESS_INFORMATION, progressReport.getActionName(), progressReport.getProgressPercentage(), progressReport.getState());
                LOGGER.debug(logMessage);
                break;
            case FINISHED:
                LOGGER.debug("Information about Progress Action :{} ", progressReport);
                result.setCompleted(true);
                if (ActionResultType.SUCCESS.equals(progressReport.getResult())) {
                    result.setSuccess(true);

                } else if (ActionResultType.FAILURE.equals(progressReport.getResult())) {
                    result.setSuccess(false);
                    notificationInformation.setFailureMessage("Result Info : " + progressReport.getResultInfo() + "Additional Info : " + progressReport.getAdditionalInfo());
                    LOGGER.error(" Upload Backup action failed on Node {} due to {} ", notificationInformation.getNodeName(), notificationInformation.getFailureMessage());
                }
                notificationInformation.getPermit().release();
                break;
            default:
                LOGGER.warn("Unsupported Action State Type :{}", state);
            }
        } catch (UnsupportedFragmentException | MoNotFoundException | NullPointerException e) {
            LOGGER.error("Exception Occured while getting Progress Report for node {}", notificationInformation.getNodeName());
        }
    }

    private boolean isUploadedSuccess(final AsyncActionProgress progressReport) {
        if (ActionResultType.SUCCESS == progressReport.getResult()) {
            return true;
        }
        return false;
    }
}
