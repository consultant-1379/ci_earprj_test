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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.notifications.api.NotificationCallbackResult;

/**
 * This class provides methods which will help to process notifications received from Cpp Node.
 * 
 * @author tcssagu
 * 
 */
public class BackupUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupUtils.class);

    @Inject
    ActivityUtils activityUtils;

    /**
     * This is a utility method to retrieve new currentActionResultData from the notification received.
     * 
     * @param modifiedAttr
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getActionResultData(final Map<String, AttributeChangeData> modifiedAttr) {
        Map<String, Object> currentActionResultData = new HashMap<>();
        final Map<String, Object> actionResultDataMap = activityUtils.getNotifiableAttribute(modifiedAttr, ConfigurationVersionMoConstants.ACTION_RESULT);
        if (actionResultDataMap.get(ShmConstants.NOTIFIABLE_ATTRIBUTE_VALUE) != null) {
            currentActionResultData = (Map<String, Object>) actionResultDataMap.get(ShmConstants.NOTIFIABLE_ATTRIBUTE_VALUE);
        }
        return currentActionResultData;
    }

    /**
     * @param actionResultData
     * @param notificationCallBackResult
     * @return boolean
     */
    public boolean isCorrectActionResult(final Map<String, Object> actionResultData, final NotificationCallbackResult notificationCallBackResult) {
        boolean isActionResultNotified = false;
        int actionIdOnNode = 0;
        try {

            actionIdOnNode = (int) actionResultData.get(ConfigurationVersionMoConstants.ACTION_ID);
            LOGGER.debug("actionIdOnNode{}", actionIdOnNode);
            if (notificationCallBackResult.getActionId() == actionIdOnNode) {
                isActionResultNotified = true;
            }

        } catch (final Exception ex) {
            LOGGER.error("Exception Occured while getting ActionId, actionIdOnNode {} actionIdOnDPS {}. Reason is : {}", actionIdOnNode, notificationCallBackResult.getActionId(), ex);
            isActionResultNotified = false;
        }
        return isActionResultNotified;
    }

    public boolean isActionResultNotified(final Map<String, Object> actionResultData) {

        boolean isActionResultNotified = false;
        if (actionResultData != null && !actionResultData.isEmpty()) {
            isActionResultNotified = true;
        }

        return isActionResultNotified;
    }

    /**
     * @param actionResultData
     */
    public boolean isJobSuccess(final Map<String, Object> actionResultData) {
        final String cvActionMainResultAsString = (String) actionResultData.get(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT);
        if (cvActionMainResultAsString != null && !cvActionMainResultAsString.isEmpty()) {
            final CVActionMainResult cvActionMainResult = CVActionMainResult.getCvActionMainResult(cvActionMainResultAsString);
            return isActionSucess(cvActionMainResult);
        }
        return false;
    }

    /**
     * 
     * @param cvActionMainResult
     * @return
     */
    public boolean isActionSucess(final CVActionMainResult cvActionMainResult) {
        return cvActionMainResult.equals(CVActionMainResult.EXECUTED) || cvActionMainResult.equals(CVActionMainResult.EXECUTED_WITH_WARNINGS);
    }

    /**
     * This method verifies whether notification is failure.
     * 
     * @param cvActionMainResult
     * @return
     */
    public boolean isActionFailed(final CVActionMainResult cvActionMainResult) {
        return cvActionMainResult.equals(CVActionMainResult.EXECUTION_FAILED);
    }

}
