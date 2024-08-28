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

import static com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.NodeModelNameSpaceProvider;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReader;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@Stateless
public class CommonCvOperationsImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonCvOperationsImpl.class);

    @Inject
    private DpsReader dpsReader;

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    private NodeModelNameSpaceProvider nodeModelNameSpaceProvider;

    @Inject
    private NodeAttributesReader nodeAttributesReader;

    final String[] CVMO_ATTRIBUTES = { CURRENT_MAIN_ACTIVITY, CURRENT_DETAILED_ACTIVITY, CURRENT_UPGRADE_PACKAGE, STORED_CONFIGRUATION_VERSION, MISSING_UPS, ACTION_RESULT,
            ADDITIONAL_ACTION_RESULT_DATA, CORRUPTED_UPS, ROLLBACK_LIST, STARTABLE_CONFIGURATION_VERSION, LOADED_CONFIGURATION_VERSION, ShmConstants.CV_ADDITIONAL_ACTION_RESULT_DATA };

    final String[] ADMINISTRATIVE_DATA = { UpgradeActivityConstants.ADMINISTRATIVE_DATA };

    public int executeActionOnMo(final String actionType, final String cvMoFdn, final Map<String, Object> actionParameters) {
        LOGGER.debug("Inside CommonCvOperationsImpl executeActionOnMo with actionType= {} cvMoFdn={} actionParameters={} ", actionType, cvMoFdn, actionParameters);
        return dpsWriter.performAction(cvMoFdn, actionType, actionParameters);

    }

    @SuppressWarnings("unchecked")
    public boolean precheckForSetStartCVSetFistInRolback(final Map<String, Object> cvMoAttr, final String cvName) {
        final List<Map<String, Object>> storedConfigurationVersionsList = (List<Map<String, Object>>) cvMoAttr.get(STORED_CONFIGRUATION_VERSION);
        LOGGER.debug("storedConfigurationVersionsList : {}", storedConfigurationVersionsList);
        if (storedConfigurationVersionsList != null) {
            for (final Map<String, Object> storedConfigurationVersion : storedConfigurationVersionsList) {
                final String storedCvName = (String) storedConfigurationVersion.get(STORED_CONFIGRUATION_VERSION_NAME);
                LOGGER.debug("storedCvName : {} ", storedCvName);
                if (storedCvName.equals(cvName)) {
                    final String storedCvStatus = (String) storedConfigurationVersion.get(STORED_CONFIGRUATION_VERSION_STATUS);
                    if (!(storedCvStatus.contains("NOT") || storedCvStatus.contains("NOK") || storedCvStatus.contains("not") || storedCvStatus.contains("nok"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public ConfigurationVersionMO getCVMo(final String nodeName) {

        final ManagedObject configVersionMO = getConfigurationVersionMo(nodeName);
        if (configVersionMO == null) {
            return null;
        }
        return new ConfigurationVersionMO(configVersionMO.getFdn(), nodeAttributesReader.readAttributes(configVersionMO, CVMO_ATTRIBUTES));
    }

    private ManagedObject getConfigurationVersionMo(final String nodeName) {
        ManagedObject configurationVersion = null;
        final String namespace = nodeModelNameSpaceProvider.getNamespaceByNodeName(nodeName);
        LOGGER.debug("Namespace for the node name {} is {}", nodeName, namespace);
        if (!ShmCommonConstants.NAMESPACE_NOT_FOUND.equals(namespace)) {
            final Map<String, Object> restrictions = new HashMap<String, Object>();
            final List<ManagedObject> managedObjectList = dpsReader.getManagedObjects(namespace, BackupActivityConstants.CV_MO_TYPE, restrictions, nodeName);

            if (managedObjectList != null && !managedObjectList.isEmpty()) {
                configurationVersion = managedObjectList.get(0);
            }
        }
        return configurationVersion;
    }

    public String getCVMoFdn(final String nodeName) {

        final ManagedObject configVersionMO = getConfigurationVersionMo(nodeName);
        if (configVersionMO == null) {
            return null;
        }
        return configVersionMO.getFdn();
    }

    public UpgradePackageMO getUPMo(final String nodeName, final String searchValue) {

        final String namespace = nodeModelNameSpaceProvider.getNamespaceByNodeName(nodeName);
        LOGGER.debug("Namespace for the node name {} is {}", nodeName, namespace);
        if (ShmCommonConstants.NAMESPACE_NOT_FOUND.equals(namespace)) {
            return null;
        }
        final Map<String, Object> restrictions = new HashMap<String, Object>();
        restrictions.put(UpgradeActivityConstants.UPGRADE_PACKAGE_ID, searchValue);
        final List<ManagedObject> managedObjectList = dpsReader.getManagedObjects(namespace, UpgradeActivityConstants.UP_MO_TYPE, restrictions, nodeName);
        if (managedObjectList.isEmpty()) {
            return null;
        }
        final ManagedObject upMO = managedObjectList.get(0);

        return new UpgradePackageMO(upMO.getFdn(), nodeAttributesReader.readAttributes(upMO, ADMINISTRATIVE_DATA));
    }

    public boolean precheckForUploadCVAction(final Map<String, Object> cvMoAttr, final Map<String, Object> actionParameters) {

        final String currentMainActivityValue = (String) cvMoAttr.get(CURRENT_MAIN_ACTIVITY);
        final CVCurrentMainActivity currentMainActivity = CVCurrentMainActivity.getMainActivity(currentMainActivityValue);

        final String currentDetailedActivityValue = (String) cvMoAttr.get(CURRENT_DETAILED_ACTIVITY);
        final CVCurrentDetailedActivity currentDetailedActivity = CVCurrentDetailedActivity.getDetailedActivity(currentDetailedActivityValue);

        if (currentMainActivity != CVCurrentMainActivity.IDLE || currentDetailedActivity != CVCurrentDetailedActivity.IDLE) {
            return false;
        }
        return true;

    }

    public int executeActionOnMo(final String actionType, final String cvMoFdn, final Map<String, Object> actionParameters, final RetryPolicy retryPolicy) {
        return dpsWriter.performAction(cvMoFdn, actionType, actionParameters);
    }

    public Map<String, Object> getCVMoAttributesFromNode(final String cvMoFdn, final String[] requiredCvMoAttributes) {

        Map<String, Object> cvMoAttributes = new HashMap<String, Object>();
        final ManagedObject configVersionMO = dpsReader.findMoByFdn(cvMoFdn);
        if (configVersionMO != null) {
            cvMoAttributes = nodeAttributesReader.readAttributes(configVersionMO, requiredCvMoAttributes);
        }
        return cvMoAttributes;
    }
}