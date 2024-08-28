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
package com.ericsson.oss.services.shm.es.impl.license;

import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.CPP_NODE_MODEL;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.FINGER_PRINT;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.INSTALLED_ON;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LAST_LICENSING_PI_CHANGE;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_DATA_PO;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_DATA_PO_NAMESPACE;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_FILE_PATH;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_INVENTORY_FINGERPRINT;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_INVENTORY_INSTALLATION;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_INVENTORY_SEQUENCE_NUMBER;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_INVENTORY_TYPE;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_MO;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.NODE_NAME;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.NodeModelNameSpaceProvider;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReader;
import com.ericsson.oss.services.shm.common.license.InstallLicenseService;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@Stateless
@Traceable
@Profiled
public class LicensingService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private DpsReader dpsReader;

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private NodeModelNameSpaceProvider nodeModelNameSpaceProvider;

    @Inject
    private NodeAttributesReader nodeAttributesReader;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private InstallLicenseService installLicenseService;

    @Inject
    private JobUpdateService jobUpdateService;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private JobConfigurationService jobConfigurationService;

    protected static final String[] LICENSEMO_ATTRIBUTES = { FINGER_PRINT, LAST_LICENSING_PI_CHANGE };

    private List<PersistenceObject> getLicensePOs(final Map<String, Object> restrictions) {
        return dpsReader.findPOs(LICENSE_DATA_PO_NAMESPACE, LICENSE_DATA_PO, restrictions);
    }

    public List<Map<String, Object>> getAttributesListOfLicensePOs(final Map<String, Object> restrictions) {

        final List<PersistenceObject> licensePoList = getLicensePOs(restrictions);
        final List<Map<String, Object>> licensePoAttributeList = new ArrayList<>();
        if (!licensePoList.isEmpty()) {
            for (final PersistenceObject licensePo : licensePoList) {
                licensePoAttributeList.add(licensePo.getAllAttributes());
            }
        }
        return licensePoAttributeList;
    }

    public boolean updateLicenseInstalledTime(final Map<String, Object> restrictionAttributes) {
        Date installedOn;
        boolean isUpdated = true;
        final List<PersistenceObject> persistenceObjectList = getLicensePOs(restrictionAttributes);
        if (persistenceObjectList.isEmpty()) {
            isUpdated = false;
        }
        for (final PersistenceObject persistenceObject : persistenceObjectList) {
            final long poId = persistenceObject.getPoId();
            installedOn = new Date();
            final Map<String, Object> changedAttributes = new HashMap<>();
            changedAttributes.put(INSTALLED_ON, installedOn);
            // To persist the timestamp when the license key file is installed
            dpsWriter.update(poId, changedAttributes);
        }
        return isUpdated;
    }

    private ManagedObject getLicenseMO(final long activityJobId) {
        final String nodeName = activityUtils.getNodeName(activityJobId);
        final String namespace = nodeModelNameSpaceProvider.getNamespaceByNodeName(nodeName);
        logger.debug("Namespace for the node name {} is {}", nodeName, namespace);
        if (ShmCommonConstants.NAMESPACE_NOT_FOUND.equals(namespace)) {
            return null;
        }
        final Map<String, Object> restrictions = new HashMap<>();
        final List<ManagedObject> managedObjectsList = dpsReader.getManagedObjects(namespace, LICENSE_MO, restrictions, nodeName);
        if (managedObjectsList.isEmpty()) {
            logger.debug("The managed objects List of licensingMO is null");
            return null;
        }
        return managedObjectsList.get(0);
    }

    public String getLicenseMoFdn(final long activityJobId) {
        final ManagedObject licensingMo = getLicenseMO(activityJobId);
        if (licensingMo != null) {
            return licensingMo.getFdn();
        }
        return null;
    }

    public Map<String, Object> getLicenseMoAttributes(final long activityJobId) {
        final ManagedObject licensingMo = getLicenseMO(activityJobId);
        final Map<String, Object> moAttributesMap = new HashMap<>();
        if (licensingMo != null) {
            moAttributesMap.put(ShmConstants.FDN, licensingMo.getFdn());
            moAttributesMap.put(ShmConstants.MO_ATTRIBUTES, nodeAttributesReader.readAttributes(licensingMo, LICENSEMO_ATTRIBUTES));
            return moAttributesMap;
        }
        return null;
    }

    /**
     * This method gets the License File Path which acts as a restriction parameter while querying.
     * 
     * @param jobEnvironment
     * @return
     */
    @SuppressWarnings("unchecked")
    // To obtain the License File Path from neJobProperties
    public Map<String, Object> getRestrictedParameters(final NEJobStaticData neJobStaticData, final String neType, final String fingerPrint, final Map<String, Object> mainJobAttributes) {
        String licenseFilePath = null;
        final String nodeName = neJobStaticData.getNodeName();
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final List<Map<String, Object>> neJobPropertyList = (List<Map<String, Object>>) jobConfigurationDetails.get(ActivityConstants.NE_JOB_PROPERTIES);
        final String platform = neJobStaticData.getPlatformType();
        logger.debug("NeType {}, platform {} in getRestrictedParameters method ", neType, platform);
        final List<String> keyList = new ArrayList<>();
        keyList.add(LicensingActivityConstants.LICENSE_FILE_PATH);
        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neJobStaticData.getNodeName(), neType, platform);

        if (keyValueMap.containsKey(LicensingActivityConstants.LICENSE_FILE_PATH)) {
            licenseFilePath = keyValueMap.get(LicensingActivityConstants.LICENSE_FILE_PATH);
        }
        if (licenseFilePath == null) {
            logger.debug("License data not found in Main job configuration. So, fetching from NE job configuration.");
            licenseFilePath = getLicenseKeyFilePathFromNeJob(neJobStaticData.getNeJobId());
            if (licenseFilePath == null) {
                logger.debug("License data not found in NE job configuration. So, fetching from License Key file.");
                licenseFilePath = installLicenseService.generateLicenseKeyFilePath(fingerPrint);
                logger.info("The license file path retrieved when job triggered {}", licenseFilePath);
                for (final Map<String, Object> neJobProperty : neJobPropertyList) {
                    if (neJobProperty.get(NODE_NAME).equals(nodeName)) {
                        final List<Map<String, String>> jobPropertyList = (List<Map<String, String>>) neJobProperty.get(ActivityConstants.JOB_PROPERTIES);
                        final Map<String, String> jobProperty = new HashMap<>();
                        jobProperty.put(ActivityConstants.JOB_PROP_KEY, LICENSE_FILE_PATH);
                        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, licenseFilePath);
                        jobPropertyList.add(jobProperty);
                        final Map<String, Object> jobPropertiesMap = new HashMap<>();
                        jobPropertiesMap.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList);
                        jobUpdateService.updateJobAttributes(neJobStaticData.getNeJobId(), jobPropertiesMap);
                    }
                }
            }
        }
        logger.debug("The License File Path is:{}", licenseFilePath);

        // Forming the attributes map to set the restriction
        final Map<String, Object> restrictionAttributes = new HashMap<>();
        restrictionAttributes.put(LicensingActivityConstants.LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseFilePath);
        return restrictionAttributes;
    }

    public String getNodeSequenceNumber(final String fingerprint) {
        String sequenceNumber = "";
        final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(CPP_NODE_MODEL, LICENSE_INVENTORY_TYPE);
        final TypeRestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
        final Restriction restriction = restrictionBuilder.equalTo(LICENSE_INVENTORY_FINGERPRINT, fingerprint);
        query.setRestriction(restriction);
        final List<PersistenceObject> licenseInventoryList = dataPersistenceService.getLiveBucket().getQueryExecutor().getResultList(query);
        if (CollectionUtils.isNotEmpty(licenseInventoryList)) {
            final PersistenceObject licenseInventory = licenseInventoryList.get(0);
            final Map<String, Object> installation = licenseInventory.getAttribute(LICENSE_INVENTORY_INSTALLATION);
            sequenceNumber = (String) installation.get(LICENSE_INVENTORY_SEQUENCE_NUMBER);

        }
        return sequenceNumber;

    }

    public String getLicenseKeyFilePathFromNeJob(final long neJobId) {
        String licenseKeyFilePath = null;
        final Map<String, Object> neJobAttributes = jobConfigurationService.retrieveJob(neJobId);
        final List<Map<String, String>> neJobProperties = (List<Map<String, String>>) neJobAttributes.get(ShmJobConstants.JOBPROPERTIES);
        if (CollectionUtils.isNotEmpty(neJobProperties)) {
            for (final Map<String, String> nejobProperty : neJobProperties) {
                if (LICENSE_FILE_PATH.equals(nejobProperty.get(ShmConstants.KEY))) {
                    licenseKeyFilePath = nejobProperty.get(ShmConstants.VALUE);
                    break;
                }
            }
        }
        logger.debug("licenseKeyFilePath {} fetched form NeJob {}", licenseKeyFilePath, neJobProperties);
        return licenseKeyFilePath;
    }

}
