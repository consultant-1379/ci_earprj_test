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

import java.util.ArrayList;
import java.util.List;
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
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.notification.common.NotificationInformation;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationCallbackResult;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

/**
 * This class facilitates the functionality of deleting the backup on specified node.
 * 
 * @author tcsmukm
 * 
 */

@RemoteActivityInfo(activityName = EcimBackupConstants.DELETE_BACKUP, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.ECIM)
public class DeleteBackUpRemoteServiceImpl extends BackUpManagementAbstractService {

    private static final long serialVersionUID = -7948934858960849245L;

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    /**
     * This method validates backup delete request to verify whether backup is present on node or not
     * 
     * @param neName
     * @param ecimBackupInfo
     * @return boolean(true/false)
     * 
     */
    @Override
    public boolean precheck(final String neName, final EcimBackupInfo ecimBackupInfo) throws BackupManagementServiceException {

        final String backupDomain = ecimBackupInfo.getDomainName();
        final String backupType = ecimBackupInfo.getBackupType();
        final List<String> backupList = new ArrayList<>();
        backupList.add(ecimBackupInfo.getBackupName());

        try {
            final List<String> validBackupList = brmMoServiceRetryProxy.getBackupDetails(backupList, neName, backupDomain, backupType);
            if (!validBackupList.isEmpty()) {
                LOGGER.info("Precheck Action is Success as backup is present on node ");
                return true;
            } else {
                LOGGER.error("Precheck Action has Failed as backup is not present on node  ");
                return false;
            }
        } catch (final MoNotFoundException e) {
            LOGGER.error("Precheck Action has failed on the node due to MoNotFoundException ", e);
            throw new BackupManagementServiceException("Precheck Action has failed on the node due to MoNotFoundException. Exception is : " + e + " backupName : " + ecimBackupInfo.getBackupName()
                    + " domain : " + ecimBackupInfo.getDomainName() + " type : " + ecimBackupInfo.getBackupType() + " node : " + neName);

        } catch (final UnsupportedFragmentException e1) {
            LOGGER.error("Precheck Action has failed during delete backup activity on the node due to UnsupportedFragmentException ", e1);
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
        final String bmMoFdn = getNotifiableFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackUpInfo);
        final FdnNotificationSubject fdnNotificationSubject = remoteActivityNotificationHelper.subscribeToNotification(nodeName, bmMoFdn, DeleteBackUpRemoteServiceImpl.class);
        try {
            if ((bmMoFdn == null) || ((bmMoFdn).equals(""))) {
                LOGGER.error("BM MO does not exist for the supplied node name:{}", nodeName);
                throw new BackupManagementServiceException("BM MO does not exist for the supplied node name:" + nodeName);
            }
            systemRecorder.recordCommand(SHMEvents.DELETE_BACKUP_SERVICE_REQUEST_ACTION, CommandPhase.STARTED, nodeName, bmMoFdn, ":Proceeding to delete backup by service request");
            activityUtils.recordEvent(SHMEvents.DELETE_BACKUP_SERVICE_REQUEST_ACTION, nodeName, bmMoFdn, "SHM:" + nodeName + ":Proceeding to delete backup by service request");
            executeAction(nodeName, ecimBackUpInfo, bmMoFdn, EcimBackupConstants.DELETE_BACKUP);
            LOGGER.info("Delete BM Action triggered successfully for the node:{}", nodeName);
            return getActionCompletionStatus(nodeName, ecimBackUpInfo, bmMoFdn);
        } catch (final Exception e) {
            LOGGER.error("deleting backup has Failed due to", e);
            throw new BackupManagementServiceException("Delete Backup Action has Failed due to ", e);

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
            LOGGER.debug("notificationCallbackResult Success for nodeName = {} and activity name {}", nodeName, EcimBackupConstants.DELETE_BACKUP);
            return 1;
        } else {
            if (notificationCallbackResult.isActionTimedOut()) {
                final AsyncActionProgress progressReport = brmMoServiceRetryProxy.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, EcimBackupConstants.DELETE_BACKUP, ecimBackUpInfo);
                return onActionTimeOut(nodeName, ecimBackUpInfo, bmMoFdn, progressReport);
            }
            return 0;
        }
    }

    /**
     * @param nodeName
     * @param ecimBackUpInfo
     * @param bmMoFdn
     * @param progressReport
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     * @throws BackupManagementServiceException
     */
    private int onActionTimeOut(final String nodeName, final EcimBackupInfo ecimBackUpInfo, final String bmMoFdn, final AsyncActionProgress progressReport) throws MoNotFoundException,
            UnsupportedFragmentException, BackupManagementServiceException {
        if (progressReport != null && !brmMoServiceRetryProxy.isBackupDeletionCompleted(nodeName, ecimBackUpInfo)) {
            final NotificationInformation notificationInformation = remoteActivityNotificationHelper.getNotificationInformation(bmMoFdn);
            LOGGER.error("Delete Backup action failed on Node : {} ,Result Info : {} , Additional Info : {} ", notificationInformation.getNodeName(), progressReport.getResultInfo(),
                    progressReport.getAdditionalInfo());
            throw new BackupManagementServiceException("Delete Backup action failed on Node :" + "Result Info : " + progressReport.getResultInfo() + "Additional Info : "
                    + progressReport.getAdditionalInfo());
        } else {
            return 1;
        }
    }

    /**
     * 
     * This method process the notifications received on Topic for DeleteBackup On Ecim Node.
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

    public void processPayLoad(final DpsAttributeChangedEvent event, final NotificationInformation notificationInformation) {
        final NotificationCallbackResult result = notificationInformation.getNotificationCallbackResult();
        final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(event);
        try {
            final AsyncActionProgress progressReport = brmMoServiceRetryProxy.getValidAsyncActionProgress(notificationInformation.getNodeName(), EcimBackupConstants.DELETE_BACKUP, modifiedAttributes);
            LOGGER.debug("Information about Progress Action : {} , on Node {}", progressReport, notificationInformation.getNodeName());
            final ActionStateType state = progressReport.getState();
            switch (state) {
            case RUNNING:
                LOGGER.debug("Progress Info, Action Name : {}, ProgressPercentage : {}, State : {}", progressReport.getActionName(), progressReport.getProgressPercentage(), progressReport.getState());

                break;
            case FINISHED:
                LOGGER.debug("Information about Progress Action : {} , on Node : {} ", progressReport, notificationInformation.getNodeName());
                result.setCompleted(true);
                if (ActionResultType.SUCCESS.equals(progressReport.getResult())) {
                    result.setSuccess(true);

                } else if (ActionResultType.FAILURE.equals(progressReport.getResult())) {
                    notificationInformation.setFailureMessage("Result Info : " + progressReport.getResultInfo() + "Additional Info : " + progressReport.getAdditionalInfo());
                    result.setSuccess(false);
                    LOGGER.error(" Delete Backup action failed on Node {} with message {} ", notificationInformation.getNodeName(), notificationInformation.getFailureMessage());
                }
                notificationInformation.getPermit().release();
                break;
            default:
                LOGGER.warn("Unsupported Action State Type {} ", state);
            }
        } catch (UnsupportedFragmentException | MoNotFoundException | NullPointerException e) {
            LOGGER.error("Exception Occured during delete backup while getting Progress Report for node {}. Exception is ", notificationInformation.getNodeName(), e);
        }
    }

}
