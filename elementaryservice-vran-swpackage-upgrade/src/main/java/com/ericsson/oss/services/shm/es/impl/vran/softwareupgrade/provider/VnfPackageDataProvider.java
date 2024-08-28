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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.vran.common.VNFMInformationProvider;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@Stateless
public class VnfPackageDataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(VnfPackageDataProvider.class);
    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Inject
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    @Inject
    private VNFMInformationProvider virtualNetwrokFunctionManagerInformationProvider;

    public List<Map<String, Object>> fetchVnfPackageDetails(final JobEnvironment jobContext, final List<NetworkElement> networkElements) {
        final Map<String, Object> jobConfigurationDetails = vranJobActivityServiceHelper.getMainJobAttributes(jobContext);
        final String vnfPackageName = getPackageName(jobConfigurationDetails, networkElements);
        List<Map<String, Object>> vnfPackageDetails = null;
        final String nfvoName = getNfvo(networkElements);
        try {
            vnfPackageDetails = getVnfDataForSoftwarePackage(vnfPackageName, nfvoName);
        } catch (final RuntimeException ex) {
            LOGGER.error("ActivityJob ID - [{}] : Failed to fetch vnf package details of {} due to {}", jobContext.getActivityJobId(), vnfPackageName, ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
        }
        return vnfPackageDetails;
    }

    private String getNfvo(final List<NetworkElement> networkElements) {
        final String nodeFdn = networkElements.get(0).getNetworkElementFdn();
        final String vnfmFdn = virtualNetwrokFunctionManagerInformationProvider.getVnfmFdn(nodeFdn);
        final String nfvoRef = virtualNetwrokFunctionManagerInformationProvider.getNfvoRefFromVnfmFDN(vnfmFdn);

        LOGGER.debug("NFVO Reference resolved for VNFM [{}]: {}", vnfmFdn, nfvoRef);
        return nfvoRef.split("=")[1];
    }

    private List<Map<String, Object>> getVnfDataForSoftwarePackage(final String softwarePackage, final String nfvoName) {
        final List<Map<String, Object>> nfvoProductDetails = vnfSoftwarePackagePersistenceProvider.getVnfPackageNfvoDetails(softwarePackage);
        return buildVnfDetails(nfvoProductDetails, nfvoName);
    }

    public List<Map<String, Object>> buildVnfDetails(final List<Map<String, Object>> nfvoProductDetails, final String nfvoName) {
        final List<Map<String, Object>> vnfDetails = new ArrayList<>();
        if (nfvoProductDetails != null && !nfvoProductDetails.isEmpty()) {
            extractVnfDetails(nfvoProductDetails, vnfDetails, nfvoName);
        }
        return vnfDetails;
    }

    private void extractVnfDetails(final List<Map<String, Object>> nfvoProductDetails, final List<Map<String, Object>> vnfDetails, final String nfvoName) {

        boolean isVnfInformationRetrieved = false;
        for (final Map<String, Object> nfvoProductDetail : nfvoProductDetails) {

            if (nfvoName != null) {

                final String nfvoId = (String) nfvoProductDetail.get(VranJobConstants.SOFTWAREPACKAGE_NFVOIDENTIFIER);
                if (nfvoId.equals(nfvoName)) {

                    setVnfDetails(vnfDetails, nfvoProductDetail);
                    isVnfInformationRetrieved = true;
                }
            } else {
                // This is to handler backward compatible case where Multiple NFVO support is not there.
                // By Default single NFVO configured in the system will be taken.
                setVnfDetails(vnfDetails, nfvoProductDetail);
                isVnfInformationRetrieved = true;
            }

            if (isVnfInformationRetrieved) {
                break;
            }
        }
    }

    private void setVnfDetails(final List<Map<String, Object>> vnfDetails, final Map<String, Object> nfvoProductDetail) {
        final String vnfDescriptionId = (String) nfvoProductDetail.get(VranJobConstants.VNFD_ID);
        final String vnfPackageId = (String) nfvoProductDetail.get(VranJobConstants.VNF_PACKAGE_ID);
        LOGGER.debug("Retrieved Vnf details for the software package from database. vnfPackageId value is {} , vnfDescriptionId value is {}", vnfPackageId, vnfDescriptionId);
        if (vnfDescriptionId != null && vnfPackageId != null) {
            vnfDetails.add(vranJobActivityServiceHelper.buildJobProperty(VranJobConstants.VNFD_ID, vnfDescriptionId));
            vnfDetails.add(vranJobActivityServiceHelper.buildJobProperty(VranJobConstants.VNF_PACKAGE_ID, vnfPackageId));
        }
    }

    private String getPackageName(final Map<String, Object> jobConfigurationDetails, final List<NetworkElement> networkElements) {
        final List<String> jobPropertyKeys = new ArrayList<>();
        jobPropertyKeys.add(UpgradeActivityConstants.SWP_NAME);

        final Map<String, String> propertyMap = jobPropertyUtils.getPropertyValue(jobPropertyKeys, jobConfigurationDetails, networkElements.get(0).getName(), networkElements.get(0).getNeType(),
                networkElements.get(0).getPlatformType().name());
        return propertyMap.get(UpgradeActivityConstants.SWP_NAME);
    }

}
