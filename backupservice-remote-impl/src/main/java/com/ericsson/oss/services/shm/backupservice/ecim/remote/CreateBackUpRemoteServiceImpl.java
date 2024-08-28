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
 * This class facilitates the functionality of creating backup for a ECIM node.
 * 
 * @author tcsagu
 * 
 */

@RemoteActivityInfo(activityName = EcimBackupConstants.CREATE_BACKUP, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
public class CreateBackUpRemoteServiceImpl extends BackUpManagementAbstractService {

    private static final long serialVersionUID = 2531989274986659330L;

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    /**
     * This method validates returns true by default
     * 
     * @param neName
     * @param ecimBackupInfo
     * @return boolean(true/false)
     * 
     */
    @Override
    public boolean precheck(final String nodeName, final EcimBackupInfo ecimBackupInfo) {
        return true;
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

        final String bmMoFdn = getNotifiableFdn(EcimBackupConstants.CREATE_BACKUP, nodeName, ecimBackUpInfo);
        final FdnNotificationSubject fdnNotificationSubject = remoteActivityNotificationHelper.subscribeToNotification(nodeName, bmMoFdn, CreateBackUpRemoteServiceImpl.class);
        try {
            if (bmMoFdn == null) {
                LOGGER.error("BM MO does not exist for the supplied node name:{}", nodeName);
                throw new BackupManagementServiceException("BM MO does not exist for the supplied node name");
            }
            systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_SERVICE_REQUEST_ACTION, CommandPhase.STARTED, nodeName, bmMoFdn, ":Proceeding to create CV by service request");
            activityUtils.recordEvent(SHMEvents.CREATE_BACKUP_SERVICE_REQUEST_ACTION, nodeName, bmMoFdn, "SHM:" + nodeName + ":Proceeding to create backup by service request");
            executeAction(nodeName, ecimBackUpInfo, bmMoFdn, EcimBackupConstants.CREATE_BACKUP);
            LOGGER.debug("CreateBackUp Action triggered successfully for the node:{}", nodeName);
            return getActionCompletionStatus(nodeName, ecimBackUpInfo, bmMoFdn);
        } catch (final Exception e) {
            LOGGER.error("CreateBackUp has Failed due to", e);
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
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     * @throws BackupManagementServiceException
     */
    private int getActionCompletionStatus(final String nodeName, final EcimBackupInfo ecimBackUpInfo, final String bmMoFdn) throws MoNotFoundException, UnsupportedFragmentException,
            BackupManagementServiceException {
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
            LOGGER.debug("notificationCallbackResult Success for nodeName = {} and activityname = {} ", nodeName, EcimBackupConstants.CREATE_BACKUP);
            return 1;
        } else {
            if (notificationCallbackResult.isActionTimedOut()) {
                final AsyncActionProgress progressReport = brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.CREATE_BACKUP, ecimBackUpInfo);
                return onActionTimeOut(nodeName, bmMoFdn, progressReport);
            }
            return 0;
        }
    }

    /**
     * @param nodeName
     * @param bmMoFdn
     * @param progressReport
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     * @throws BackupManagementServiceException
     */
    private int onActionTimeOut(final String nodeName, final String bmMoFdn, final AsyncActionProgress progressReport) throws MoNotFoundException, UnsupportedFragmentException,
            BackupManagementServiceException {
        if (progressReport != null) {
            if (ActionStateType.FINISHED != progressReport.getState()) {
                systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_SERVICE_REQUEST_ACTION, CommandPhase.FINISHED_WITH_ERROR, nodeName, bmMoFdn, ": Create Backup MO action progress report is: ["
                        + progressReport.toString() + "]");
                throw new BackupManagementServiceException("Create Backup action progress report on Node : " + progressReport.toString() + " and the addition Information: "
                        + progressReport.getAdditionalInfo());
            } else {
                systemRecorder.recordCommand(SHMEvents.CREATE_BACKUP_SERVICE_REQUEST_ACTION, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, bmMoFdn, ": Create Backup MO action progress report is: ["
                        + progressReport.toString() + "]");
                return 1;
            }
        } else {
            LOGGER.error("No Progress Report received for node: [{}], for Create Backup.", nodeName);
            return 0;
        }
    }

    /**
     * 
     * This method process the notifications received On Topic for CreatebackUp on Ecim Node.
     * 
     * 
     */

    @Override
    public void processNotification(final Notification notification) {
        if (!(notification.getDpsDataChangedEvent() instanceof DpsAttributeChangedEvent)) {
            return;
        }
        final DpsAttributeChangedEvent event = (DpsAttributeChangedEvent) notification.getDpsDataChangedEvent();
        final FdnNotificationSubject subject = (FdnNotificationSubject) notification.getNotificationSubject();
        final NotificationInformation notificationInformation = remoteActivityNotificationHelper.getNotificationInformation(subject.getKey());
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
            final AsyncActionProgress progressReport = brmMoServiceRetryProxy.getValidAsyncActionProgress(notificationInformation.getNodeName(), EcimBackupConstants.CREATE_BACKUP, modifiedAttributes);
            final ActionStateType state = progressReport.getState();
            switch (state) {
            case RUNNING:
                LOGGER.debug("Information about Progress Action running : {}", progressReport);
                String.format(JobLogConstants.PROGRESS_INFORMATION, progressReport.getActionName(), progressReport.getProgressPercentage(), progressReport.getState());

                break;
            case FINISHED:
                LOGGER.debug("Information about Progress Action Finished : {} ", progressReport);
                result.setCompleted(true);
                if (ActionResultType.SUCCESS.equals(progressReport.getResult())) {
                    result.setSuccess(true);
                } else if (ActionResultType.FAILURE.equals(progressReport.getResult())) {
                    result.setSuccess(false);
                    notificationInformation.setFailureMessage("Result Info : " + progressReport.getResultInfo() + "Additional Info : " + progressReport.getAdditionalInfo());
                    LOGGER.error(" Create Backup action failed on Node {} due to {} ", notificationInformation.getNodeName(), notificationInformation.getFailureMessage());
                }
                notificationInformation.getPermit().release();
                break;
            default:
                LOGGER.warn("Unsupported Action State Type {}", state);
            }
        } catch (UnsupportedFragmentException | MoNotFoundException | NullPointerException e) {
            LOGGER.error("Exception Occurred while getting Progress Report for node {}. Exception is ", notificationInformation.getNodeName(), e);
        }
    }
}
