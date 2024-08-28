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
package com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.constants.StnJobConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

/**
 * Service to retrieve Stn information
 * 
 * @author xgowbom
 */
@Stateless
public class StnInformationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(StnInformationProvider.class);

    @Inject
    private DpsReader dpsReader;

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Inject
    private StnJobActivityServiceHelper stnJobActivityService;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    /**
     * Method to persist StnId, StnPackageId and StndId in NE job properties.
     *
     * @param activityJobId
     */
    @SuppressWarnings("deprecation")
    public void persistStnData(final long activityJobId) {

        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        List<Map<String, Object>> neJobProperties = stnJobActivityService.getNeJobAttributes(jobContext);
        NEJobStaticData neJobStaticData = null;

        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final String platform = neJobStaticData.getPlatformType();
            final String nodeName = neJobStaticData.getNodeName();

            final NetworkElementData networkElement = networkElementRetrivalBean.getNetworkElementData(nodeName);
            final String neType = networkElement.getNeType();
            final List<String> jobPropertyKeys = new ArrayList<>();
            final Map<String, Object> jobConfigurationDetails = stnJobActivityService.getMainJobAttributes(jobContext);

            final String stnPackageName = getJobProperty(jobPropertyKeys, jobConfigurationDetails, nodeName, neType, platform, UpgradeActivityConstants.SWP_NAME);

            final List<Map<String, Object>> stnPackageDetails = getStnDataForSoftwarePackage(stnPackageName);
            LOGGER.debug("ActivityJob ID - [{}] : package details retrieved for nodeFdn from the database are {}", activityJobId, stnPackageDetails);

            if (neJobProperties == null) {
                neJobProperties = new ArrayList<>();
            }
            neJobProperties.addAll(stnPackageDetails);
        } catch (final RuntimeException ex) {
            LOGGER.error("Error while Persisting the Data" + StnJobConstants.ERROR_MESSAGE, StnJobConstants.STN_PACKAGE_TYPE, ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
        } catch (JobDataNotFoundException ex) {
            LOGGER.error("Error while getting nodeName and platform" + StnJobConstants.ERROR_MESSAGE, StnJobConstants.STN_PACKAGE_TYPE, ex);
        } catch (MoNotFoundException ex) {
            LOGGER.error("Error while fetching nodeType" + StnJobConstants.ERROR_MESSAGE, StnJobConstants.STN_PACKAGE_TYPE, ex);
        }

        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, neJobProperties, null);
    }

    /**
     * Method to retrieve stnId and stnpackageID for the software package from database
     */
    private List<Map<String, Object>> getStnDataForSoftwarePackage(final String softwarePackage) {

        final Map<String, Object> restrictionsMap = new HashMap<>();
        restrictionsMap.put(UpgradeActivityConstants.UP_PO_PACKAGE_NAME, softwarePackage);

        final List<PersistenceObject> stnPackages = dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, StnJobConstants.STN_PACKAGE_TYPE, restrictionsMap);

        final List<Map<String, Object>> stnDetails = new ArrayList<>();

        if (stnPackages != null && !stnPackages.isEmpty()) {
            LOGGER.debug("Retrieved STN Package from the database : {}", stnPackages.toString());
            final PersistenceObject stnPackage = stnPackages.get(0);
            final Map<String, Object> attributes = stnPackage.getAllAttributes();
            LOGGER.debug("Attributes of STN id {}", attributes);
            stnDetails.add(stnJobActivityService.buildJobProperty(StnJobConstants.STN_PACKAGE_FILE_PATH, attributes.get(StnJobConstants.STN_PACKAGE_FILE_PATH)));
            stnDetails.add(stnJobActivityService.buildJobProperty(StnJobConstants.STN_PACKAGE_NAME, attributes.get(StnJobConstants.STN_PACKAGE_NAME)));

            final HashMap<String, String> productDetails = (HashMap<String, String>) fetchProductDetails(attributes);
            stnDetails.add(stnJobActivityService.buildJobProperty(StnJobConstants.PRODUCT_NUMBER, productDetails.get(StnJobConstants.PRODUCT_NUMBER)));
            stnDetails.add(stnJobActivityService.buildJobProperty(StnJobConstants.PRODUCT_REVISION, productDetails.get(StnJobConstants.PRODUCT_REVISION)));

            final String fileName = fetchFileName(attributes);
            stnDetails.add(stnJobActivityService.buildJobProperty(StnJobConstants.SOFTWAREPACKAGE_FILENAME, fileName));
        }
        return stnDetails;

    }

    @SuppressWarnings("unchecked")
    private String fetchFileName(final Map<String, Object> attributes) {
        final List<Map<String, Object>> activities = (List<Map<String, Object>>) attributes.get(StnJobConstants.SOFTWAREPACKAGE_ACTIVITES);
        if (activities != null && !activities.isEmpty()) {
            for (Map<String, Object> activity : activities) {
                final List<Map<String, Object>> activitieparams = (List<Map<String, Object>>) activity.get(StnJobConstants.SOFTWAREPACKAGE_ACTIVITESPARAMS);
                for (Map<String, Object> activitieparam : activitieparams) {
                    if (activitieparam.containsValue(StnJobConstants.SMO_BUNDLE_FILENAME)) {
                        return (String) activitieparam.get(StnJobConstants.VALUE);
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> fetchProductDetails(final Map<String, Object> attributes) {
        final HashMap<String, String> productDetails = new HashMap<>();
        final List<Map<String, Object>> swpProductDetails = (List<Map<String, Object>>) attributes.get(StnJobConstants.SOFTWAREPACKAGE_NFVODETAILS);
        if (swpProductDetails != null && !swpProductDetails.isEmpty()) {

            for (Map<String, Object> swpProductDetail : swpProductDetails) {
                final String productNumber = (String) swpProductDetail.get(StnJobConstants.PRODUCT_NUMBER);
                final String productRevision = (String) swpProductDetail.get(StnJobConstants.PRODUCT_REVISION);
                LOGGER.debug("Retrieved Fdn details for the software package from database. stnPackageId value is {} , stnDescriptionId value is {}", productNumber, productRevision);
                if (productNumber != null && productRevision != null) {
                    productDetails.put(StnJobConstants.PRODUCT_NUMBER, productNumber);
                    productDetails.put(StnJobConstants.PRODUCT_REVISION, productRevision);
                }
            }
        }
        return productDetails;
    }

    private String getJobProperty(final List<String> jobPropertyKeys, final Map<String, Object> jobConfigurationDetails,

            final String nodeName, final String nodeType, final String platform, final String propertyName) {
        jobPropertyKeys.add(propertyName);
        final Map<String, String> propertyMap = jobPropertyUtils.getPropertyValue(jobPropertyKeys, jobConfigurationDetails, nodeName, nodeType, platform);
        return propertyMap.get(propertyName);

    }

}
