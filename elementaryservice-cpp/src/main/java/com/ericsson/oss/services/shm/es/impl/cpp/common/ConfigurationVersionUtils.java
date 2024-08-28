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
package com.ericsson.oss.services.shm.es.impl.cpp.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVActionMainResult;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVCurrentDetailedActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVCurrentMainActivity;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.shm.inventory.backup.entities.AdminProductData;

/**
 * This class contain methods to retrieve configuration version attributes.
 *
 * @author tcsvisr
 *
 */
@Profiled
@Traceable
public class ConfigurationVersionUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationVersionUtils.class);

    @Inject
    ActivityUtils activityUtils;

    /**
     * This method retrieves the Configuration Version name.
     *
     * @param mainJobAttributes
     * @param neJobAttributes
     * @return String
     */
    @SuppressWarnings("unchecked")
    public String getConfigurationVersionName(final Map<String, Object> mainJobAttributes, final Map<String, Object> neJobAttributes) {
        logger.debug("Main Job Attributes: {} ", mainJobAttributes);
        List<Map<String, String>> mainJobPropertyList = null;
        List<Map<String, String>> neJobPropertyList = null;
        if (mainJobAttributes != null) {
            mainJobPropertyList = (List<Map<String, String>>) mainJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        } else {
            logger.debug("Main Job has no Properties.");
        }
        logger.debug("Main Job Property List: {}", mainJobPropertyList);
        String configurationVersionName = null;
        if (mainJobPropertyList != null && !mainJobPropertyList.isEmpty()) {
            for (final Map<String, String> mainJobProperty : mainJobPropertyList) {
                if (mainJobProperty.get(ShmConstants.KEY).equals(BackupActivityConstants.CV_NAME)) {
                    configurationVersionName = mainJobProperty.get(ShmConstants.VALUE);
                }
            }
        }

        if (configurationVersionName == null) {
            if (neJobAttributes != null) {
                neJobPropertyList = (List<Map<String, String>>) neJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
            } else {
                logger.debug("NE Job has no Properties.");
            }
            if (neJobPropertyList != null && !neJobPropertyList.isEmpty()) {
                for (final Map<String, String> neJobProperty : neJobPropertyList) {
                    if (BackupActivityConstants.CV_NAME.equals(neJobProperty.get(ShmConstants.KEY))) {
                        configurationVersionName = neJobProperty.get(ShmConstants.VALUE);
                    }
                }
            }

        }
        logger.debug("Configuration Version Name : {}", configurationVersionName);
        return configurationVersionName;
    }

    /**
     * This method returns the backup list to be deleted
     *
     * @param storedConfigurationVersionList
     * @return cvNameStoredConfigurationVersionList
     */

    public List<String> getCvNames(final List<Map<String, String>> storedConfigurationVersionList) {
        final List<String> cvNameStoredConfigurationVersionList = new ArrayList<String>();
        String cvNameStoredConfigurationVersion;
        for (final Map<String, String> storedConfigurationVersion : storedConfigurationVersionList) {
            cvNameStoredConfigurationVersion = storedConfigurationVersion.get(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME);
            cvNameStoredConfigurationVersionList.add(cvNameStoredConfigurationVersion);
        }
        logger.debug("List of Backups to be deleted : {}", cvNameStoredConfigurationVersionList);
        return cvNameStoredConfigurationVersionList;
    }

    @SuppressWarnings("unchecked")
    public CvActionMainAndAdditionalResultHolder retrieveActionResultDataWithAddlInfo(final Map<String, Object> cvMoAttr) {

        int actionIdFromCvMo = -1;

        CvActionAdditionalInfo additionalInfo = null;
        final List<CvActionAdditionalInfo> additionalInfoList = new ArrayList<CvActionAdditionalInfo>();
        final Map<String, Object> actionResult = (Map<String, Object>) cvMoAttr.get(ConfigurationVersionMoConstants.ACTION_RESULT);

        if (actionResult.get(ActivityConstants.ACTION_ID) != null) {
            actionIdFromCvMo = (int) actionResult.get(ActivityConstants.ACTION_ID);
        }

        final String cvActionMainResultValue = (String) actionResult.get(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT);
        final CVActionMainResult cvActionMainResult = CVActionMainResult.getCvActionMainResult(cvActionMainResultValue);
        final String pathToDetail = (String) actionResult.get(ConfigurationVersionMoConstants.CV_PATH_TO_DETAILED_INFORMATION);

        final List<Map<String, Object>> additionalActionResultDataList = (List<Map<String, Object>>) cvMoAttr.get(ConfigurationVersionMoConstants.ADDITIONAL_ACTION_RESULT_DATA);
        if (additionalActionResultDataList != null) {
            for (final Map<String, Object> additionalInformationMap : additionalActionResultDataList) {
                additionalInfo = new CvActionAdditionalInfo();
                additionalInfo.setAdditionalInformation((String) additionalInformationMap.get(ConfigurationVersionMoConstants.CV_ADDITIONAL_INFORMATION));
                final String cvActionResultInformationValue = (String) additionalInformationMap.get(ConfigurationVersionMoConstants.CV_INFORMATION);
                final CVActionResultInformation cvActionResultInformation = CVActionResultInformation.getCvActionResultInformation(cvActionResultInformationValue);
                additionalInfo.setInformation(cvActionResultInformation);
                additionalInfoList.add(additionalInfo);
            }
        }
        final CvActionMainAndAdditionalResultHolder cvResultHolder = new CvActionMainAndAdditionalResultHolder(actionIdFromCvMo, cvActionMainResult, pathToDetail, additionalInfoList);
        return cvResultHolder;

    }

    /**
     * validate if Cv function is currently busy or not
     *
     * @param cvActivity
     * @return
     */
    public boolean isCvFunctionBusy(final CvActivity cvActivity) {
        final boolean isCvFunctionBusy = cvActivity.getMainActivity() != CVCurrentMainActivity.IDLE || cvActivity.getDetailedActivity() != CVCurrentDetailedActivity.IDLE;
        return isCvFunctionBusy;

    }

    /**
     * return the {@link CvActivity} by reading the node CVMO .
     *
     * @param cvMOAttr
     * @return CvActivity
     */
    public CvActivity getCvActivity(final Map<String, Object> cvMOAttr) {

        final String currentMainActivityValue = (String) cvMOAttr.get(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY);
        final CVCurrentMainActivity currentMainActivity = CVCurrentMainActivity.getMainActivity(currentMainActivityValue);

        final String currentDetailedActivityValue = (String) cvMOAttr.get(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY);
        final CVCurrentDetailedActivity currentDetailedActivity = CVCurrentDetailedActivity.getDetailedActivity(currentDetailedActivityValue);

        return new CvActivity(currentMainActivity, currentDetailedActivity);
    }

    /**
     * return the new {@link CvActivity} by reading the notification got.
     *
     * @param notification
     * @return CvActivity
     */
    public CvActivity getNewCvActivity(final Notification notification) {

        final Map<String, AttributeChangeData> modifiedAttributes = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
        CVCurrentMainActivity currentMainActivity = CVCurrentMainActivity.UNKNOWN;
        CVCurrentDetailedActivity currentDetailedActivity = CVCurrentDetailedActivity.UNKNOWN;
        if (modifiedAttributes.get(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY) != null) {
            final AttributeChangeData newAttrChangeddata = modifiedAttributes.get(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY);
            final String currentMainActivityValue = (String) newAttrChangeddata.getNewValue();
            currentMainActivity = CVCurrentMainActivity.getMainActivity(currentMainActivityValue);

        }
        if (modifiedAttributes.get(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY) != null) {
            final AttributeChangeData newAttrChangeddata = modifiedAttributes.get(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY);
            final String currentDetailedActivityValue = (String) newAttrChangeddata.getNewValue();
            currentDetailedActivity = CVCurrentDetailedActivity.getDetailedActivity(currentDetailedActivityValue);

        }

        return new CvActivity(currentMainActivity, currentDetailedActivity);
    }

    /**
     * return the old {@link CvActivity} by reading the notification got.
     *
     * @param notification
     * @return CvActivity
     */
    public CvActivity getOldCvActivity(final Notification notification) {

        final Map<String, AttributeChangeData> modifiedAttribute = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
        CVCurrentMainActivity currentMainActivity = CVCurrentMainActivity.UNKNOWN;
        CVCurrentDetailedActivity currentDetailedActivity = CVCurrentDetailedActivity.UNKNOWN;
        if (modifiedAttribute.get(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY) != null) {
            final AttributeChangeData newAttrChangeddata = modifiedAttribute.get(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY);
            final String currentMainActivityValue = (String) newAttrChangeddata.getOldValue();
            currentMainActivity = CVCurrentMainActivity.getMainActivity(currentMainActivityValue);

        }
        if (modifiedAttribute.get(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY) != null) {
            final AttributeChangeData newAttrChangeddata = modifiedAttribute.get(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY);
            final String currentDetailedActivityValue = (String) newAttrChangeddata.getOldValue();
            currentDetailedActivity = CVCurrentDetailedActivity.getDetailedActivity(currentDetailedActivityValue);

        }

        return new CvActivity(currentMainActivity, currentDetailedActivity);
    }

    /**
     * returns true if main action result notified for the same invoked action id. else return false. This notification is considered as end action execution.
     *
     * @param invokedActionId
     * @param notification
     * @return
     */

    @SuppressWarnings("unchecked")
    public boolean isInvokedActionResultNotified(final int invokedActionId, final Notification notification) {
        final Map<String, AttributeChangeData> modifiedAttribute = activityUtils.getModifiedAttributes(notification.getDpsDataChangedEvent());
        CVActionMainResult cvActionMainResult = CVActionMainResult.NOTFOUND;
        boolean isActionResultNotified = false;
        if (modifiedAttribute.get(ConfigurationVersionMoConstants.ACTION_RESULT) != null) {
            final AttributeChangeData newAttrChangeddata = modifiedAttribute.get(ConfigurationVersionMoConstants.ACTION_RESULT);
            final Map<String, Object> actionResultMap = (Map<String, Object>) newAttrChangeddata.getNewValue();
            if (actionResultMap.get(ConfigurationVersionMoConstants.ACTION_ID) != null) {
                final int actionIdFromNode = (int) actionResultMap.get(ConfigurationVersionMoConstants.ACTION_ID);
                if (actionResultMap.get(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT) != null) {
                    final String cvActionMainResultValue = (String) actionResultMap.get(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT);
                    cvActionMainResult = CVActionMainResult.getCvActionMainResult(cvActionMainResultValue);

                }
                if (invokedActionId == actionIdFromNode && CVActionMainResult.NOTFOUND != cvActionMainResult) {
                    isActionResultNotified = true;
                }

            }
        }
        return isActionResultNotified;
    }

    @SuppressWarnings("unchecked")
    public int getActionId(final Map<String, Object> activityJobAttributes) {
        int actionId = -1;
        final List<Map<String, Object>> activityJobPropertyList = (List<Map<String, Object>>) activityJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        if (activityJobPropertyList != null) {
            for (final Map<String, Object> activityJobProperty : activityJobPropertyList) {
                if (ActivityConstants.ACTION_ID.equals(activityJobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    actionId = Integer.parseInt((String) activityJobProperty.get(ActivityConstants.JOB_PROP_VALUE));
                    logger.debug("Action ID from database for confirm restore action {}", actionId);
                }
            }
        }
        return actionId;
    }

    /**
     * @param cvMOAttributes
     * @return List<AdminProductData>
     */
    @SuppressWarnings("unchecked")
    public List<AdminProductData> getCorrputedUps(final Map<String, Object> cvMOAttributes) {
        final List<AdminProductData> corruptedUpsList = new ArrayList<AdminProductData>();
        AdminProductData adminProduct = null;
        final List<Map<String, Object>> upList = (List<Map<String, Object>>) cvMOAttributes.get(ConfigurationVersionMoConstants.CORRUPTED_UPS);
        if (upList != null) {
            for (final Map<String, Object> corruptedUp : upList) {
                final String productRevision = (String) corruptedUp.get(UpgradeActivityConstants.UP_PO_PROD_REVISION);
                final String productNumber = (String) corruptedUp.get(UpgradeActivityConstants.UP_PO_PROD_NUMBER);
                final String productName = (String) corruptedUp.get(UpgradeActivityConstants.UP_PO_PROD_NAME);
                final String productionDate = (String) corruptedUp.get(UpgradeActivityConstants.UP_PO_PROD_DATE);
                final String productInfo = (String) corruptedUp.get(UpgradeActivityConstants.UP_PO_PROD_INFO);
                adminProduct = new AdminProductData(productInfo, productionDate, productName, productNumber, productRevision);
                corruptedUpsList.add(adminProduct);
            }
        }
        return corruptedUpsList;

    }

    /**
     * @param cvMOAttributes
     * @return List<AdminProductData>
     */
    @SuppressWarnings("unchecked")
    public List<AdminProductData> getMissingUps(final Map<String, Object> cvMOAttributes) {
        final List<AdminProductData> missingUpsList = new ArrayList<AdminProductData>();
        AdminProductData adminProduct = null;
        final List<Map<String, Object>> upList = (List<Map<String, Object>>) cvMOAttributes.get(ConfigurationVersionMoConstants.MISSING_UPS);
        if (upList != null) {
            for (final Map<String, Object> missingUp : upList) {
                final String productRevision = (String) missingUp.get(UpgradeActivityConstants.UP_PO_PROD_REVISION);
                final String productNumber = (String) missingUp.get(UpgradeActivityConstants.UP_PO_PROD_NUMBER);
                final String productName = (String) missingUp.get(UpgradeActivityConstants.UP_PO_PROD_NAME);
                final String productionDate = (String) missingUp.get(UpgradeActivityConstants.UP_PO_PROD_DATE);
                final String productInfo = (String) missingUp.get(UpgradeActivityConstants.UP_PO_PROD_INFO);
                adminProduct = new AdminProductData(productInfo, productionDate, productName, productNumber, productRevision);
                missingUpsList.add(adminProduct);
            }
        }
        return missingUpsList;

    }

    /**
     *
     * @param activityJobId
     * @return
     */
    @SuppressWarnings("unchecked")
    public String getNeJobPropertyValue(final Map<String, Object> mainJobAttributes, final String nodeName, final String key) {
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final List<Map<String, Object>> neJobPropertyList = (List<Map<String, Object>>) jobConfigurationDetails.get(ActivityConstants.NE_JOB_PROPERTIES);
        logger.debug("NE Property list for node {} : {}", nodeName, neJobPropertyList);
        String value = null;
        if (key != null && !key.isEmpty()) {
            if (neJobPropertyList != null && !neJobPropertyList.isEmpty()) {
                for (final Map<String, Object> jobConfigJobProperty : neJobPropertyList) {
                    if (jobConfigJobProperty.get(ShmConstants.NE_NAME).equals(nodeName)) {
                        final List<Map<String, Object>> neJobProperty = (List<Map<String, Object>>) jobConfigJobProperty.get(ShmConstants.JOBPROPERTIES);
                        if (neJobProperty != null && !neJobProperty.isEmpty()) {
                            for (final Map<String, Object> jobProperty : neJobProperty) {
                                if (key.equals(jobProperty.get(ShmConstants.KEY))) {
                                    value = (String) jobProperty.get(ShmConstants.VALUE);
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }
            }
        } else {
            logger.debug("Property key is not provided for Node {}", nodeName);
        }
        return value;
    }

    /**
     * @param cvMo
     * @return true if MO is valid
     */
    public boolean isValidCvMO(final ConfigurationVersionMO cvMo) {
        return cvMo != null && cvMo.getFdn() != null;
    }

    public boolean isCVActivityChanged(final CvActivity newCvActivity) {
        return newCvActivity.getDetailedActivity() != CVCurrentDetailedActivity.UNKNOWN || newCvActivity.getMainActivity() != CVCurrentMainActivity.UNKNOWN;
    }

    /**
     *
     * Reports the notification message as node log to GUI
     *
     * @param newCvActivity
     * @param jobLogList
     */

    public void reportNotification(final CvActivity newCvActivity, final List<Map<String, Object>> jobLogList) {
        if (newCvActivity.getDetailedActivity() != CVCurrentDetailedActivity.UNKNOWN) {
            activityUtils.addJobLog(newCvActivity.getDetailedActivityDesc(), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
        }
    }

    /**
     * return the new {@link CvActivity} by reading the polling response got.
     *
     * @param modifiedAttributes
     * @return CvActivity
     */
    public CvActivity getNewCvActivity(final Map<String, Object> modifiedAttributes) {
        final CVCurrentMainActivity currentMainActivity = CVCurrentMainActivity.UNKNOWN;
        CVCurrentDetailedActivity currentDetailedActivity = CVCurrentDetailedActivity.UNKNOWN;
        if (modifiedAttributes != null && !modifiedAttributes.isEmpty() && modifiedAttributes.get(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY) != null) {
            final String currentDetailedActivityValue = (String) modifiedAttributes.get(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY);
            currentDetailedActivity = CVCurrentDetailedActivity.getDetailedActivity(currentDetailedActivityValue);
        }
        return new CvActivity(currentMainActivity, currentDetailedActivity);
    }
}
