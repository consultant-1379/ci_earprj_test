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
package com.ericsson.oss.services.shm.backupservice.cpp.remote;

import java.util.List;
import java.util.Map;
import javax.ejb.*;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.services.shm.backupservice.remote.api.CVOperationRemoteException;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.RemoteActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.*;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CVActionResultInformation;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.notification.common.NotificationInformation;
import com.ericsson.oss.services.shm.notification.common.RemoteActivityNotificationHelper;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationCallbackResult;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

/**
 * This class facilitates the functionality of uploading the Configuration Version on cpp node.
 * 
 * 
 * 
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@RemoteActivityInfo(activityName = BackupActivityConstants.ACTION_UPLOAD_CV, jobType = JobTypeEnum.BACKUP, platform = PlatformTypeEnum.CPP)
public class UploadCVRemoteServiceImpl extends CVManagementAbstractService {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadCVRemoteServiceImpl.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private RemoteActivityNotificationHelper activityNotificationHelper;

    @Inject
    private BackupUtils backupActionResultUtility;

    @Inject
    private ConfigurationVersionService configurationVersionService;

    public boolean precheckOnMo(final Map<String, Object> cvMoAttr, final Map<String, Object> actionParameters) {

        return commonCvOperations.precheckForUploadCVAction(cvMoAttr, actionParameters);
    }

    public int executeAction(final String cvMoFdn, final String nodeName, final Map<String, Object> actionParameters) {

        final FdnNotificationSubject fdnNotificationSubject = activityNotificationHelper.subscribeToNotification(nodeName, cvMoFdn,
                UploadCVRemoteServiceImpl.class);
        NotificationCallbackResult notificationCallBackResult = null;
        boolean isUploadSuccessful = false;
        try {
            int invokedActionId = -1;
            final String actionType = BackupActivityConstants.ACTION_UPLOAD_CV;
            activityUtils.recordEvent(SHMEvents.UPLOAD_BACKUP_SERVICE_REQUEST_ACTION, nodeName, cvMoFdn, "SHM:" + nodeName
                    + ":Proceeding to upload CV by service request");
            try {
                invokedActionId = commonCvOperations.executeActionOnMo(actionType, cvMoFdn, actionParameters);
                notificationCallBackResult = fdnNotificationSubject.getNotificationCallBackResult();
                notificationCallBackResult.setActionId(invokedActionId);
            } catch (final Exception e) {
                LOGGER.error("Failed in executeActionOnMo due to : ", e);
                isUploadSuccessful = false;
                throw new CVOperationRemoteException("Exception while invoking upload action on the node: " + e.getMessage());
            }
            LOGGER.debug("Upload CV Action triggered successfully for the CV :{} on the node:{}", cvMoFdn, nodeName);
            notificationCallBackResult = activityNotificationHelper.waitForProcessNotifications(cvMoFdn);
            LOGGER.debug("notificationCallbackResult {}", notificationCallBackResult);
            isUploadSuccessful = evaluateActionStatus(cvMoFdn, nodeName, notificationCallBackResult);
        } catch (final Exception e) {
            LOGGER.error("Exception occured while triggering upload CV Action on CvMo:{}", cvMoFdn);
            isUploadSuccessful = false;
        }

        finally {
            try {
                activityNotificationHelper.unSubscribeToNotification(fdnNotificationSubject, cvMoFdn);
            } catch (final Exception e) {
                LOGGER.error("Unable to remove subject", e);
            }
        }
        return isUploadSuccessful ? 1 : 0;
    }

    private boolean evaluateActionStatus(final String cvMoFdn, final String nodeName, final NotificationCallbackResult notificationCallBackResult) {
        boolean isUploadSuccess = false;
        if (notificationCallBackResult.isSuccess()) {
            isUploadSuccess = true;
        } else if (notificationCallBackResult.isActionTimedOut()) {
            isUploadSuccess = onActionTimeOut(cvMoFdn, nodeName, notificationCallBackResult);

        }
        return isUploadSuccess;
    }

    /**
     * 
     * @param cvMoFdn
     * @param nodeName
     * @param notificationCallBackResult
     * @return boolean
     */

    @SuppressWarnings("unchecked")
    private boolean onActionTimeOut(final String cvMoFdn, final String nodeName, final NotificationCallbackResult notificationCallBackResult) {
        Map<String, Object> cvAttributes = null;
        final Map<String, Object> moAttributesMap = configurationVersionService.getCVMoAttr(nodeName);
        if (moAttributesMap != null) {
            cvAttributes = (Map<String, Object>) moAttributesMap.get(ShmConstants.MO_ATTRIBUTES);
            final Map<String, Object> currentActionResultData = (Map<String, Object>) cvAttributes.get(ConfigurationVersionMoConstants.ACTION_RESULT);
            final List<Map<String, Object>> additionalActionResultData = (List<Map<String, Object>>) cvAttributes
                    .get(ConfigurationVersionMoConstants.ADDITIONAL_ACTION_RESULT_DATA);
            if (backupActionResultUtility.isCorrectActionResult(currentActionResultData, notificationCallBackResult)) {
                final NotificationInformation notificationInformation = activityNotificationHelper.getNotificationInformation(cvMoFdn);
                processActionResult(notificationInformation, additionalActionResultData, currentActionResultData, notificationCallBackResult);
            }

        }
        return notificationCallBackResult.isSuccess();
    }

    /**
     * This method process notification those are received on Topic from Node.
     */
    @Override
    public void processNotification(final Notification notification) {
        if (!(notification.getDpsDataChangedEvent() instanceof DpsAttributeChangedEvent)) {
            return;
        }
        final FdnNotificationSubject subject = (FdnNotificationSubject) notification.getNotificationSubject();
        final NotificationInformation notificationInformation = activityNotificationHelper.getNotificationInformation(subject.getKey());
        processPayLoad(notification, notificationInformation);
    }

    @SuppressWarnings("unchecked")
    private void processPayLoad(final Notification notification, final NotificationInformation notificationInformation) {
        final Map<String, AttributeChangeData> modifiedAttr = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
        final Map<String, Object> actionResultData = backupActionResultUtility.getActionResultData(modifiedAttr);
        final NotificationCallbackResult result = notificationInformation.getNotificationCallbackResult();
        if (backupActionResultUtility.isCorrectActionResult(actionResultData, notificationInformation.getNotificationCallbackResult())) {
            result.setCompleted(true);
            final List<Map<String, Object>> additionalActionResultDataList = (List<Map<String, Object>>) modifiedAttr
                    .get(ShmConstants.CV_ADDITIONAL_ACTION_RESULT_DATA);

            processActionResult(notificationInformation, additionalActionResultDataList, actionResultData, result);
            notificationInformation.getPermit().release();
        }

    }

    private void processActionResult(final NotificationInformation notificationInformation,
                                     final List<Map<String, Object>> additionalActionResultDataList, final Map<String, Object> actionResultData,
                                     final NotificationCallbackResult result) {
        final String cvActionMainResultAsString = (String) actionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT);
        if (cvActionMainResultAsString != null && !cvActionMainResultAsString.isEmpty()) {
            if (backupActionResultUtility.isJobSuccess(actionResultData)) {
                result.setSuccess(true);
                activityUtils.recordEvent(SHMEvents.UPLOAD_BACKUP_SERVICE_REQUEST_ACTION, notificationInformation.getNodeName(), null,
                        ":Upload Backup Action Success on the node");
            } else {
                final CVActionMainResult cvActionMainResult = CVActionMainResult.getCvActionMainResult(cvActionMainResultAsString);
                if (backupActionResultUtility.isActionFailed(cvActionMainResult)) {
                    result.setSuccess(false);
                    processFailureReason(notificationInformation, additionalActionResultDataList);

                }

            }
        }
    }

    private void processFailureReason(final NotificationInformation notificationInformation,
                                      final List<Map<String, Object>> additionalActionResultDataList) {
        if (additionalActionResultDataList != null) {
            int index = 1;
            for (final Map<String, Object> additionalActionResultData : additionalActionResultDataList) {
                final CVActionResultInformation information = CVActionResultInformation
                        .getCvActionResultInformation((String) additionalActionResultData.get(ShmConstants.CV_INFORMATION));
                index++;
                notificationInformation.setFailureMessage(String.format(JobLogConstants.ADDITIONAL_FAILURE_RESULT, "", index,
                        information.getCVActionResultInformationDesc()));
                activityUtils.recordEvent(SHMEvents.UPLOAD_BACKUP_SERVICE_REQUEST_ACTION, notificationInformation.getNodeName(),
                        ":Upload Backup Action Failed on the node", notificationInformation.getFailureMessage());
                LOGGER.error("Upload Backup Failed on node {} due to  {}", notificationInformation.getNodeName(),
                        notificationInformation.getFailureMessage());
            }
        }
    }
}
