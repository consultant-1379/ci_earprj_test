/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageState;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * class to fetch data from node
 * 
 * @author xkalkil
 *
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class DeleteUpgradePackageDataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteUpgradePackageDataCollector.class);

    private static List<String> deletableUPMOStates = Arrays.asList(UpgradePackageState.NOT_INSTALLED.name(), UpgradePackageState.INSTALL_COMPLETED.name(),
            UpgradePackageState.INSTALL_NOT_COMPLETED.name(), UpgradePackageState.ONLY_DELETEABLE.name(), UpgradePackageState.UPGRADE_COMPLETED.name());

    /**
     * This method returns deletable upgrade package product details by filtering upgrade packages on the node with below criteria applied on each upgrade package present on the node.<br>
     * 1. UP MO state should be in one of states: <i>NOT_INSTALLED, INSTALL_COMPLETED,INSTALL_NOT_COMPLETED, ONLY_DELETABLE or UPGRADE_COMPLETED</i>.<br>
     * 2. Upgrade Package should not be the <i>active upgrade package</i>.<br>
     * 3. Upgrade Package shouldn't contain <i>startable and loaded CVs</i>.
     *
     * @param nodeName
     * @param activeUpgradePackage
     * @return A String with comma separated deletable upgrade package ids that are in the pattern: <i>productNumber**|**productRevision</i>.
     */
    public String getDeletableUpgradePackageIds(final List<UpgradePackageMO> upgradePackageMOs, final UpgradePackageMO activeUpgradePackage, final ConfigurationVersionMO cvMo) {
        LOGGER.debug("Entering getDeletableUpgradePackageIds() with upgradePackageMOs: {} activeUpgradePackage:{}, cvMO: {} ", upgradePackageMOs, activeUpgradePackage, cvMo);
        final Map<String, Object> cvMOAttributes = cvMo.getAllAttributes();
        final String startableCVName = (String) cvMOAttributes.get(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION);
        final String loadedCVName = (String) cvMOAttributes.get(ConfigurationVersionMoConstants.LOADED_CONFIGURATION_VERSION);
        final List<Map<String, Object>> deletableUpgradePackageDetails = new ArrayList<>();
        if (!upgradePackageMOs.isEmpty()) {
            for (final UpgradePackageMO upgradePackageMO : upgradePackageMOs) {
                CollectionUtils.addIgnoreNull(deletableUpgradePackageDetails, filterByDeletableUPMOStates(upgradePackageMO));
            }
            removeActiveUpgradePackage(deletableUpgradePackageDetails, activeUpgradePackage);
            removeUPswithStartableAndLoadableCVs(deletableUpgradePackageDetails, startableCVName, loadedCVName);
            return prepareDletableUpgradePackageIds(deletableUpgradePackageDetails);
        } else {
            return ShmConstants.EMPTY;
        }

    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> filterByDeletableUPMOStates(final UpgradePackageMO upgradePackageMO) {
        LOGGER.debug("Entering filterByDeletableUPMOStates.upgradePackageDetails: {}", upgradePackageMO);
        final Map<String, Object> upMoAttributes = upgradePackageMO.getAllAttributes();
        final String upState = (String) upMoAttributes.get(UpgradePackageMoConstants.UP_MO_STATE);
        final Map<String, Object> deletableUpgradePackageDetails = new HashMap<>();
        if (deletableUPMOStates.contains(upState)) {
            deletableUpgradePackageDetails.put(UpgradeActivityConstants.UPGRADE_PACKAGE_ID,
                    getUpgradePackageId((Map<String, String>) upMoAttributes.get(UpgradeActivityConstants.ADMINISTRATIVE_DATA)));
            deletableUpgradePackageDetails.put(UpgradeActivityConstants.DELETE_PREVENTING_CVS, upMoAttributes.get(UpgradeActivityConstants.DELETE_PREVENTING_CVS));
            deletableUpgradePackageDetails.put(UpgradeActivityConstants.DELETE_PREVENTING_UPS, upMoAttributes.get(UpgradeActivityConstants.DELETE_PREVENTING_UPS));
        }
        LOGGER.debug("Exit from filterByDeletableUPMOStates.deletableUpgradePackageDetails:{} ", deletableUpgradePackageDetails);
        return MapUtils.isNotEmpty(deletableUpgradePackageDetails) ? deletableUpgradePackageDetails : null;
    }

    private String getUpgradePackageId(final Map<String, String> administrativeData) {
        final String productNumber = administrativeData.get(UpgradeActivityConstants.PRODUCT_NUMBER);
        final String productRevision = administrativeData.get(UpgradeActivityConstants.PRODUCT_REVISION);
        return productNumber + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + productRevision;
    }

    @SuppressWarnings("unchecked")
    private void removeActiveUpgradePackage(final List<Map<String, Object>> deletableUpgradePackageDetails, final UpgradePackageMO activeUpgradePackage) {
        LOGGER.debug("Entering removeActiveUpgradePackage.upgradePackageDetails: {}, activeUpgradePackage: {}", deletableUpgradePackageDetails, activeUpgradePackage);
        final Map<String, String> adminData = (Map<String, String>) activeUpgradePackage.getAllAttributes().get(UpgradeActivityConstants.ADMINISTRATIVE_DATA);
        final String activeUpgradePackageId = getUpgradePackageId(adminData);
        for (Iterator<Map<String, Object>> deletableUpgradePackageItr = deletableUpgradePackageDetails.iterator(); deletableUpgradePackageItr.hasNext();) {
            final Map<String, Object> deletableUpgradePackage = deletableUpgradePackageItr.next();
            final String deletableUpgradePackageId = (String) deletableUpgradePackage.get(UpgradeActivityConstants.UPGRADE_PACKAGE_ID);
            if (deletableUpgradePackageId.equals(activeUpgradePackageId)) {
                deletableUpgradePackageItr.remove();
            }
        }
        LOGGER.debug("Exit from removeActiveUpgradePackage.deletableUpgradePackageDetails: {}", deletableUpgradePackageDetails);
    }

    @SuppressWarnings("unchecked")
    private void removeUPswithStartableAndLoadableCVs(final List<Map<String, Object>> deletableUpgradePackageDetails, final String startableCVName, final String loadedCVName) {
        LOGGER.debug("Entering removeUPswithStartableAndLoadableCVs.upgradePackageDetails: {}, startableCVName: {}, loadedCVName: {} ", deletableUpgradePackageDetails, startableCVName, loadedCVName);
        for (Iterator<Map<String, Object>> deletableUpgradePackageItr = deletableUpgradePackageDetails.iterator(); deletableUpgradePackageItr.hasNext();) {
            final Map<String, Object> deletableUpgradePackage = deletableUpgradePackageItr.next();
            final List<String> deletePreventingCVs = (List<String>) deletableUpgradePackage.get(UpgradeActivityConstants.DELETE_PREVENTING_CVS);
            if (CollectionUtils.isNotEmpty(deletePreventingCVs) && (deletePreventingCVs.contains(startableCVName) || deletePreventingCVs.contains(loadedCVName))) {
                deletableUpgradePackageItr.remove();
            }
        }
        LOGGER.debug("Exit from removeUPswithStartableAndLoadableCVs.deletableUpgradePackageDetails: {}", deletableUpgradePackageDetails);
    }

    private String prepareDletableUpgradePackageIds(final List<Map<String, Object>> deletableUpgradePackageDetails) {
        LOGGER.debug("Entering prepareDeletableUPs.upgradePackageDetails: {}", deletableUpgradePackageDetails);
        final List<String> deletableUpgradePackageIds = new ArrayList<>();
        for (Map<String, Object> deletableUpgradePackage : deletableUpgradePackageDetails) {
            deletableUpgradePackageIds.add((String) deletableUpgradePackage.get(UpgradeActivityConstants.UPGRADE_PACKAGE_ID));
        }
        LOGGER.debug("Exit from prepareDeletableUPs.deletableUpgradePackageIds: {}", deletableUpgradePackageIds);
        return String.join(",", deletableUpgradePackageIds);
    }

}
