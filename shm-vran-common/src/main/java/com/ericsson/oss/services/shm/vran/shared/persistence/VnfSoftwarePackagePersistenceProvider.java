/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.vran.shared.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.vran.common.VranActivityUtil;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;

@Stateless
public class VnfSoftwarePackagePersistenceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(VnfSoftwarePackagePersistenceProvider.class);

    @Inject
    private DpsReader dpsReader;

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    private VranActivityUtil vranActivityUtil;

    /**
     * Retrieves VNF software package PO for the given VNF package name.
     * 
     * @param softwarePackageName
     * @return softwarePackageLocation
     */
    public PersistenceObject getVnfSoftwarePackageEntity(final String softwarePackageName) {
        LOGGER.debug("Retrieving Vnf software package entity for software package: {}", softwarePackageName);

        return getVnfSoftwarePackage(softwarePackageName);
    }

    private PersistenceObject getVnfSoftwarePackage(final String softwarePackageName) {
        final Map<String, Object> restrictionsMap = new HashMap<>();
        restrictionsMap.put(UpgradeActivityConstants.UP_PO_PACKAGE_NAME, softwarePackageName);

        final List<PersistenceObject> vnfPackages = dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, VranJobConstants.VNF_PACKAGE_TYPE, restrictionsMap);
        PersistenceObject vnfPackage = null;

        if (vnfPackages != null && !vnfPackages.isEmpty()) {
            vnfPackage = vnfPackages.get(0);
            LOGGER.debug("Vnf package details retreived for package {}  are: {} ", softwarePackageName, vnfPackage.getAllAttributes());
        }
        return vnfPackage;
    }

    /**
     * Retrieves VNF package id for the given VNF software package
     * 
     * @param softwarePackageName
     * @param nfvoFdn
     * @param nfvoFdn
     * @return
     */
    public String getVnfPackageId(final String softwarePackageName, final String nfvoFdn) {

        final PersistenceObject vnfPackage = getVnfSoftwarePackage(softwarePackageName);

        final String vnfPackageId = retrieveVnfPackageIdForNFVOFdn(nfvoFdn, vnfPackage);
        LOGGER.debug("Retrieved VnfPackageId is : {} ", vnfPackageId);

        return vnfPackageId;
    }

    /**
     * @param nfvoFdn
     * @param vnfPackages
     * @return
     */
    @SuppressWarnings("unchecked")
    private String retrieveVnfPackageIdForNFVOFdn(final String nfvoFdn, final PersistenceObject vnfPackage) {
        String vnfPackageId = null;
        if (vnfPackage != null) {
            final Map<String, Object> attributes = vnfPackage.getAllAttributes();
            final List<Map<String, Object>> productDetailsInNfvo = (List<Map<String, Object>>) attributes.get(VranJobConstants.SOFTWAREPACKAGE_NFVODETAILS);
            LOGGER.trace("Retrieved Nfvo product details [{}] ", productDetailsInNfvo);
            if (productDetailsInNfvo != null && !productDetailsInNfvo.isEmpty()) {
                for (Map<String, Object> productDetails : productDetailsInNfvo) {
                    if (!productDetails.isEmpty() && productDetails.containsKey(VranJobConstants.SOFTWAREPACKAGE_NFVOIDENTIFIER)
                            && String.valueOf(productDetails.get(VranJobConstants.SOFTWAREPACKAGE_NFVOIDENTIFIER)).equalsIgnoreCase(vranActivityUtil.getNeNameFromFdn(nfvoFdn))) {
                        vnfPackageId = (String) productDetails.get(VranJobConstants.VNF_PACKAGE_ID);
                        break;
                    }
                }
            }
        }
        return vnfPackageId;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getVnfPackageNfvoDetails(final String softwarePackageName) {
        List<Map<String, Object>> nfvoDetails = null;
        final PersistenceObject vnfPackage = getVnfSoftwarePackage(softwarePackageName);

        if (vnfPackage != null) {
            final Map<String, Object> attributes = vnfPackage.getAllAttributes();
            nfvoDetails = (List<Map<String, Object>>) attributes.get(VranJobConstants.SOFTWAREPACKAGE_NFVODETAILS);
        }
        return nfvoDetails;
    }

    /**
     * 
     * Method to get software package path from ENM
     * 
     * @param softwarePackage
     * @return softwarePackagePathInEnm
     */
    public String getVnfPackageSMRSPath(final String softwarePackageName) {

        LOGGER.debug("Retrieving filepath for the software package : {} from ENM", softwarePackageName);
        String softwarePackagePathInEnm = null;
        final PersistenceObject vnfPackage = getVnfSoftwarePackage(softwarePackageName);

        if (vnfPackage != null) {
            final Map<String, Object> attributes = vnfPackage.getAllAttributes();
            softwarePackagePathInEnm = (String) attributes.get(UpgradeActivityConstants.UP_PO_FILE_PATH);
            LOGGER.info("SMRS File path for the software package : {} is {}", vnfPackage, softwarePackagePathInEnm);
        }
        return softwarePackagePathInEnm;
    }

    /**
     * To update the software package location in database using poId and map.
     * 
     * @param poId
     * @param vnfPackageDetails
     */
    public void updateSoftwarePackageEntity(final long poId, final Map<String, Object> vnfPackageDetails) {
        try {
            dpsWriter.update(poId, vnfPackageDetails);
            LOGGER.trace("Software package record has been updated successfully for the poId {} :", poId);
        } catch (final Exception exception) {
            LOGGER.error("Failed to update the software package location in the database. Reason : ", exception);
        }
    }

    /**
     * To delete the software package from the database using poId.
     * 
     * @param poId
     */
    public void deleteSoftwarePackageEntity(final long poId) {
        try {
            final int deletedPackagesCount = dpsWriter.deletePoByPoId(poId);
            LOGGER.trace("{} software package records have been deleted successfully :", deletedPackagesCount);
        } catch (final Exception exception) {
            LOGGER.error("Failed to delete the software package from the database. Reason : ", exception);
        }
    }

}
