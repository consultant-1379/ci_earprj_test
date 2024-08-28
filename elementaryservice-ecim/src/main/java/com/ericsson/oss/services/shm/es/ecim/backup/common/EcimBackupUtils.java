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
package com.ericsson.oss.services.shm.es.ecim.backup.common;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.exception.BackupDataNotFoundException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroup;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroupPreparator;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.ecim.common.EcimCommonConstants;
import com.ericsson.oss.services.shm.ecim.common.EcimDateSorterUtil;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.backuphousekeeping.BackupSortException;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.UpMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActiveSoftwareProvider;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.AutoGenerateNameValidator;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmHandler;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmVersionHandlersProviderFactory;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.EcimBackupItem;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackup;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.SecureEcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.api.NeInfoQuery;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.EncryptAndDecryptConverter;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

public class EcimBackupUtils {

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private FdnServiceBean fdnServiceBean;

    @Inject
    private NetworkElementGroupPreparator neBatchPreparator;

    @Inject
    private BrmVersionHandlersProviderFactory handlersProviderFactory;

    @Inject
    private BrmMoService brmMoService;

    @Inject
    private JobConfigurationServiceRetryProxy configServiceRetryProxy;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private EAccessControl accessControl;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private ActiveSoftwareProvider activeSoftwareProvider;

    @Inject
    private UpMoServiceRetryProxy upMoServiceRetryProxy;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private AutoGenerateNameValidator autoGenerateNameValidator;
    
    @Inject
    EncryptAndDecryptConverter encryptAndDecryptConverter;

    private final Logger LOGGER = LoggerFactory.getLogger(EcimBackupUtils.class);

    private static final String BACKUP_DELIMITER = "|";
    private static final String BACKUP_DATA_SPLIT_CHARACTER = "\\|";
    private static final String BACKUP_DATA_SPLIT_CHARACTER_FOR_UPLOAD = "/";
    private static final String COMMA_SEPARATOR = ",";
    private static final String DOMAIN = "DOMAIN";
    private static final String TYPE = "TYPE";
    private static final String LOCATION = "LOCATION";
    private static final String BACKUP_NAME = "BACKUP_NAME";
    private static final String PRODUCT_DATA_SPLIT_CHARACTER = "\\|\\|";
    private static final String DEFAULT_BACKUP_NAME_MESSAGE = ". Trying to generate backup with default backup name.";
    private static final String ACTIVE_SOFTWARE_DETAILS_NOT_FOUND_FOR_NE = JobLogConstants.ACTIVE_SOFTWARE_DETAILS_NOT_FOUND.substring(0,
            JobLogConstants.ACTIVE_SOFTWARE_DETAILS_NOT_FOUND.length() - 1) + " for node \"%s\"";

    /**
     * Fetch Ecim backup job information.
     *
     * @param activityJobId
     * @param nodeName
     * @param capability
     * @return
     * @throws JobDataNotFoundException
     * @throws MoNotFoundException
     */
    public EcimBackupInfo getBackup(final NEJobStaticData neJobStaticData) throws MoNotFoundException, JobDataNotFoundException {
        final Map<String, String> backupDetailsMap = getBackupManagerDetails(neJobStaticData);
        String domainName = null;
        String backupType = null;
        // backupName will be overwritten in the else block.
        final String backupName = backupDetailsMap.get(EcimBackupConstants.BRM_BACKUP_NAME);
        final String backupManagerId = backupDetailsMap.get(EcimBackupConstants.BRM_BACKUP_MANAGER_ID);
        LOGGER.debug("backupName and backupManagerId : {}, {}", backupName, backupManagerId);
        if (backupManagerId != null) {
            final Map<String, String> domainBackupTypeMap = getDomainIdAndBackupType(backupManagerId);
            domainName = domainBackupTypeMap.get(EcimBackupConstants.DOMAIN_NAME);
            backupType = domainBackupTypeMap.get(EcimBackupConstants.BACKUP_TYPE);
            final EcimBackupInfo backupEnvironment = new EcimBackupInfo(domainName, backupName, backupType);
            return backupEnvironment;
        } else {
            // backupNameJobProperty = BackupName1|Domain1|Type1,BackupName2|Domain2|Type2,BackupName3|Domain3|Type3
            final String backupNameJobProperty = backupName;
            final String[] backupDataSplitValues = backupNameJobProperty.split(COMMA_SEPARATOR);
            final String backupToBeDeleted = backupDataSplitValues[0];
            LOGGER.debug("backupToBeDeleted : {}", backupToBeDeleted);
            final EcimBackupInfo ecimBackupInfoForDeletion = getEcimBackupInfo(backupToBeDeleted);
            return ecimBackupInfoForDeletion;
        }
    }

    private String appendTimeStampAndValidateBackupName(String backupName, final boolean defaultBackupName) {
        final Date dateTime = new Date();
        final SimpleDateFormat formatter = new SimpleDateFormat(JobPropertyConstants.AUTO_GENERATE_DATE_FORMAT);
        final String timestamp = formatter.format(dateTime);
        if (defaultBackupName) {
            return backupName.concat(ActivityConstants.UNDERSCORE).concat(timestamp);
        } else {
            if (backupName.contains(ShmConstants.TIMESTAMP_PLACEHOLDER)) {
                return backupName.replace(ShmConstants.TIMESTAMP_PLACEHOLDER, timestamp);
            } else {
                return backupName;
            }
        }
    }

    public String getInputBackupName(final NEJobStaticData neJobStaticData) throws MoNotFoundException, JobDataNotFoundException {
        final Map<String, String> backupDetailsMap = getBackupManagerDetails(neJobStaticData);
        return backupDetailsMap.get(EcimBackupConstants.BRM_BACKUP_NAME);
    }

    /**
     * This method should be removed once the precheck and execute changes done for Restore/ Delete Backup/Housekeeping jobs for ECIM nodes. Removing usage of JobEnvironment from all elementary
     * services.
     *
     * @param jobEnvironment
     * @return
     * @throws MoNotFoundException
     */
    @Deprecated
    public EcimBackupInfo getBackup(final JobEnvironment jobEnvironment) throws MoNotFoundException {
        final Map<String, String> backupDetailsMap = getBackupManagerDetails(jobEnvironment);
        String domainName = null;
        String backupType = null;
        // backupName will be overwritten in the else block.
        final String backupName = backupDetailsMap.get(EcimBackupConstants.BRM_BACKUP_NAME);
        final String backupManagerId = backupDetailsMap.get(EcimBackupConstants.BRM_BACKUP_MANAGER_ID);
        LOGGER.debug("backupName and backupManagerId : {}, {}", backupName, backupManagerId);
        if (backupManagerId != null) {
            final Map<String, String> domainBackupTypeMap = getDomainIdAndBackupType(backupManagerId);
            domainName = domainBackupTypeMap.get(EcimBackupConstants.DOMAIN_NAME);
            backupType = domainBackupTypeMap.get(EcimBackupConstants.BACKUP_TYPE);
            final EcimBackupInfo backupEnvironment = new EcimBackupInfo(domainName, backupName, backupType);
            return backupEnvironment;
        } else {
            // backupNameJobProperty = BackupName1|Domain1|Type1,BackupName2|Domain2|Type2,BackupName3|Domain3|Type3
            final String backupNameJobProperty = backupName;
            final String[] backupDataSplitValues = backupNameJobProperty.split(COMMA_SEPARATOR);
            final String backupToBeDeleted = backupDataSplitValues[0];
            LOGGER.debug("backupToBeDeleted : {}", backupToBeDeleted);
            final EcimBackupInfo ecimBackupInfoForDeletion = getEcimBackupInfo(backupToBeDeleted);
            return ecimBackupInfoForDeletion;
        }

    }

    public EcimBackupInfo getBackupInfoForRestore(final NEJobStaticData neJobStaticData, final NetworkElementData networkElement) throws MoNotFoundException, UnsupportedFragmentException {
        final Map<String, Object> neJobAttributes = configServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId());
        LOGGER.debug("neJobAttributes for NeJobId {} : {}", neJobStaticData.getNeJobId(), neJobAttributes);
        if (neJobAttributes.get(ActivityConstants.JOB_PROPERTIES) == null) {
            return getEcimBackupInfo(neJobStaticData, networkElement);
        } else {
            final List<String> keyList = Arrays.asList(EcimBackupConstants.BACKUP_NAME, EcimBackupConstants.BACKUP_DOMAIN, EcimBackupConstants.BACKUP_TYPE, EcimBackupConstants.BACKUP_FILE_NAME);
            final Map<String, String> backupDetails = getPropertyValue(keyList, neJobAttributes);
            final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupDetails.get(EcimBackupConstants.BACKUP_DOMAIN), backupDetails.get(EcimBackupConstants.BACKUP_NAME),
                    backupDetails.get(EcimBackupConstants.BACKUP_TYPE));
            ecimBackupInfo.setBackupFileName(backupDetails.get(EcimBackupConstants.BACKUP_FILE_NAME));
            return ecimBackupInfo;
        }
    }

    /**
     * @param jobEnvironment
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    @SuppressWarnings("unchecked")
    private EcimBackupInfo getEcimBackupInfo(final NEJobStaticData neJobStaticData, final NetworkElementData networkElement) throws MoNotFoundException, UnsupportedFragmentException {
        final String nodeName = neJobStaticData.getNodeName();
        String backupFileName = "";
        EcimBackupInfo ecimBackupInfo = null;

        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) configServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId())
                .get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final String backupLocation = getPropertyValueFromNEJob(EcimBackupConstants.BACKUP_FILE_LOCATION, jobConfigurationDetails, nodeName);
        LOGGER.debug("Location of the backupfile : {}", backupLocation);
        if (backupLocation.equalsIgnoreCase(ShmCommonConstants.LOCATION_ENM)) {
            backupFileName = getPropertyValueFromNEJob(EcimBackupConstants.ABSOLUTE_BACKUP_FILE_NAME, jobConfigurationDetails, neJobStaticData.getNodeName());
            LOGGER.debug("Name of the backupfile present on SMRS: {}", backupFileName);
            ecimBackupInfo = brmMoService.extractAndAddBackupNameInNeProperties(backupFileName, nodeName, neJobStaticData.getNeJobId(), networkElement);
        } else {
            final List<String> keyList = new ArrayList<String>();
            keyList.add(EcimBackupConstants.BRM_BACKUP_NAME);
            keyList.add(EcimBackupConstants.BRM_BACKUP_DOMAIN);
            keyList.add(EcimBackupConstants.BRM_BACKUP_TYPE);
            final Map<String, String> backupDetailsMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neJobStaticData.getNodeName(), networkElement.getNeType(),
                    neJobStaticData.getPlatformType());
            final String backupName = backupDetailsMap.get(EcimBackupConstants.BRM_BACKUP_NAME);
            final String backupDomain = backupDetailsMap.get(EcimBackupConstants.BRM_BACKUP_DOMAIN);
            final String backupType = backupDetailsMap.get(EcimBackupConstants.BRM_BACKUP_TYPE);
            LOGGER.debug("backupfile details present on NODE: {} {} {} ", backupName, backupDomain, backupType);
            ecimBackupInfo = new EcimBackupInfo(backupDomain, backupName, backupType);
        }
        ecimBackupInfo.setBackupLocation(backupLocation);
        return ecimBackupInfo;
    }

    /**
     * @param brmBackupName
     * @param jobConfigurationDetails
     * @return
     */
    @SuppressWarnings("unchecked")
    private String getPropertyValueFromNEJob(final String key, final Map<String, Object> jobConfigurationDetails, final String nodeName) {
        final List<Map<String, Object>> neJobPropertyList = (List<Map<String, Object>>) jobConfigurationDetails.get(ShmJobConstants.NEJOB_PROPERTIES);
        if (neJobPropertyList != null) {
            for (final Map<String, Object> neJobProperty : neJobPropertyList) {
                final String neNameFromProp = (String) neJobProperty.get(ShmJobConstants.NE_NAME);
                if (nodeName != null && nodeName.equals(neNameFromProp)) {
                    final List<Map<String, String>> properties = (List<Map<String, String>>) neJobProperty.get(ShmJobConstants.JOBPROPERTIES);
                    if (properties != null) {
                        for (final Map<String, String> property : properties) {
                            if (property.get(ShmJobConstants.KEY).equals(key)) {
                                final String value = property.get(ShmJobConstants.VALUE);
                                LOGGER.debug("Value fetched from neJobProperties {} ", value);
                                return value;
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getBackupManagerDetails(final JobEnvironment jobEnvironment) throws MoNotFoundException {
        final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
        final String nodeName = jobEnvironment.getNodeName();
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        NetworkElement networkElement = null;
        final List<String> neNames = new ArrayList<String>();
        neNames.add(nodeName);
        final List<NetworkElement> networkElements = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neNames, SHMCapabilities.BACKUP_JOB_CAPABILITY);
        if (!networkElements.isEmpty()) {
            networkElement = networkElements.get(0);
        }
        if (networkElement == null) {
            throw new MoNotFoundException("Network Element is not found for the supplied node name:" + nodeName);
        }
        final List<String> keyList = Arrays.asList(EcimBackupConstants.BRM_BACKUP_NAME, EcimBackupConstants.BRM_BACKUP_MANAGER_ID);
        final Map<String, String> backupManagerDetails = getPropertyValue(keyList, jobEnvironment.getNeJobAttributes());
        if (!backupManagerDetails.isEmpty()) {
            return backupManagerDetails;
        } else {
            return jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, nodeName, networkElement.getNeType(), networkElement.getPlatformType().name());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getBackupManagerDetails(final NEJobStaticData neJobStaticData) throws MoNotFoundException, JobDataNotFoundException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(neJobStaticData.getNodeName());
        final Map<String, Object> mainJobAttributes = configServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
        final Map<String, Object> neJobAttributes = configServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId());
        final List<String> keyList = Arrays.asList(EcimBackupConstants.BRM_BACKUP_NAME, EcimBackupConstants.BRM_BACKUP_MANAGER_ID, JobPropertyConstants.AUTO_GENERATE_BACKUP, JobPropertyConstants.SECURE_BACKUP_KEY, JobPropertyConstants.USER_LABEL);
        final Map<String, String> backupManagerDetails = getPropertyValue(keyList, neJobAttributes);
        if (!backupManagerDetails.isEmpty()) {
            return backupManagerDetails;
        } else {
            final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
            return jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neJobStaticData.getNodeName(), networkElementData.getNeType(), neJobStaticData.getPlatformType());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getPropertyValue(final List<String> keyList, final Map<String, Object> neJobAttributes) {
        final List<Map<String, String>> neJobPropertyList = (List<Map<String, String>>) neJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        final Map<String, String> jobPropertyMap = new HashMap<String, String>();
        if (neJobPropertyList != null) {
            for (final String key : keyList) {
                for (final Map<String, String> neJobProperty : neJobPropertyList) {
                    if (key.equals(neJobProperty.get(ShmConstants.KEY))) {
                        jobPropertyMap.put(key, neJobProperty.get(ShmConstants.VALUE));
                    }
                }
            }
        }
        return jobPropertyMap;

    }

    public Map<String, String> getDomainIdAndBackupType(final String backupManagerId) {
        final String[] domainIdAndBackupTypeArray = backupManagerId.split("/");
        final Map<String, String> domainIdAndBackupTypeMap = new HashMap<String, String>();
        final String domainName = backupManagerId.startsWith("/") ? "" : domainIdAndBackupTypeArray[0];
        final String backupType = backupManagerId.endsWith("/") ? "" : domainIdAndBackupTypeArray[domainIdAndBackupTypeArray.length - 1];
        domainIdAndBackupTypeMap.put(EcimBackupConstants.DOMAIN_NAME, domainName);
        domainIdAndBackupTypeMap.put(EcimBackupConstants.BACKUP_TYPE, backupType);
        return domainIdAndBackupTypeMap;
    }

    /**
     * This method fetches the input JSON for deleting backups on ECIM nodes and converts it into a list. (Format of entries in the list - BkupName|Domain|Type)
     *
     * @param mainJobAttributes
     * @param nodeName
     * @return backupDataList
     */
    @SuppressWarnings("unchecked")
    public List<String> getBackupDataToBeDeleted(final Map<String, Object> mainJobAttributes, final String nodeName) {
        LOGGER.debug("Prepare Backup Data To Be Deleted for node name : {}", nodeName);
        String backupData = null;
        List<String> backupDataList = new ArrayList<String>();

        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        LOGGER.debug("job Configuration in getBackupDataToBeDeleted() {}", jobConfigurationDetails);

        final List<String> networkElements = new ArrayList<String>();
        networkElements.add(nodeName);

        final List<NetworkElement> networkElementList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(networkElements, SHMCapabilities.BACKUP_JOB_CAPABILITY);
        final String neType = networkElementList.get(0).getNeType();
        final String platform = networkElementList.get(0).getPlatformType().name();
        LOGGER.debug("NeType {}, platform {} ", neType, platform);

        final List<String> keyList = new ArrayList<String>();
        keyList.add(EcimBackupConstants.BRM_BACKUP_NAME);

        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, nodeName, neType, platform);
        backupData = keyValueMap.get(EcimBackupConstants.BRM_BACKUP_NAME);

        LOGGER.debug("backupData from job property is : {} ", backupData);

        // Preparing the list of Backup names from comma separated string
        if (backupData != null && !backupData.isEmpty()) {
            backupDataList = prepareBackupDataList(backupData);
        } else {
            LOGGER.error("No Backups provided to delete on node : {}", nodeName);
        }
        return backupDataList;
    }

    /**
     * This method prepares the list of Backup data. Eg: The backup Data - "BkUp_1|domain|type, Bkup_2|domain|type, Bkup_3|domain|type" from UI will be converted to separate Strings and stored in a
     * List.
     *
     * @param backupData
     * @return backupDataList
     */
    public List<String> prepareBackupDataList(final String backupData) {
        final List<String> backupDataList = new ArrayList<String>();
        if (backupData.contains(COMMA_SEPARATOR)) {
            final String[] backupDetails = backupData.split(COMMA_SEPARATOR);
            Collections.addAll(backupDataList, backupDetails);
        } else {
            backupDataList.add(backupData);
        }
        return backupDataList;
    }

    /**
     * Method to retrieve domain and type for the input backup data string.
     *
     * @param backupDetails
     * @return array of backup information (Domain, Type,Location)
     */
    public Map<String, String> getBackupDomainTypeAndLocation(final String backupGroupKey) {
        String domain = null;
        String type = null;
        String location = null;
        final Map<String, String> backupDataMap = new HashMap<String, String>();

        final String[] backupData = backupGroupKey.split(BACKUP_DATA_SPLIT_CHARACTER);

        final int backupDataLength = backupData.length;
        LOGGER.debug("backupData.length in get backup domain type and location : {}", backupData.length);

        if (backupDataLength == 3) {
            domain = backupData[0];
            type = backupData[1];
            location = backupData[2];
        } else if (backupDataLength == 2) {
            domain = backupData[0];
            type = "";
            location = backupData[1];
        } else if (backupDataLength == 1) {
            domain = "";
            type = "";
            location = backupData[0];
        }
        backupDataMap.put(DOMAIN, domain);
        backupDataMap.put(TYPE, type);
        backupDataMap.put(LOCATION, location);
        LOGGER.debug("backupDataMap in EcimBackupUtils : {}", backupDataMap);
        return backupDataMap;

    }

    /**
     * Method to group backup names based on node name, backup domain,type and location.
     *
     * @param backupDataList
     * @return backupsGroupedByDomainTypeAndLoc
     */

    public Map<String, String> groupBackupNamesByDomainTypeAndLoc(final String backupData, final String nodeName) {
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();

        final String[] backupDetails = backupData.split(BACKUP_DATA_SPLIT_CHARACTER);
        LOGGER.debug("backupDetails length : {}", backupDetails.length);

        String backupDomain = "";
        String backupType = "";
        String backupLocation = "";
        final String backupName = backupDetails[0];

        final int backupDetailsLength = backupDetails.length;
        LOGGER.debug("backupData.length in groupBackupNamesByDomainTypeAndLocation : {}", backupDetailsLength);

        // Either the domain or type or both can be empty. So, consider them empty accordingly so that existence of BrmBackupManager can be checked in these cases.
        if (backupDetailsLength == 4) {
            backupDomain = backupDetails[1];
            backupType = backupDetails[2];
            backupLocation = backupDetails[3];
        } else if (backupDetailsLength == 3) {
            backupDomain = backupDetails[1];
            backupType = "";
            backupLocation = backupDetails[2];
        } else if (backupDetailsLength == 2) {
            backupDomain = "";
            backupType = "";
            backupLocation = backupDetails[1];
        }

        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, backupName);
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, backupDomain);
        backupsGroupedByDomainTypeAndLoc.put(TYPE, backupType);
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, backupLocation);
        LOGGER.debug("backupsGroupedByDomainTypeAndLocation in ecimbackuputils : {} for nodeName : {} ", backupsGroupedByDomainTypeAndLoc, nodeName);
        return backupsGroupedByDomainTypeAndLoc;
    }

    /**
     * This method splits the input backup string (of the form "Bkup1|Domain1|Type1,Bkup2|Domain1|Type1,Bkup3|Domain2|Type2") based on comma separator and returns the resulting values in an array.
     *
     * @param backupsFromJobProperty
     * @return array of backup strings
     */
    public String[] splitBackupString(final String backupsFromJobProperty) {
        return backupsFromJobProperty.split(COMMA_SEPARATOR);
    }

    /**
     * Method to get the remaining backups to delete from the comma separated backup string by ignoring the first backup.
     *
     * @param commaSeparatedBackups
     * @return subString of commaSeparatedBackups
     */

    public String getRemainingBackupsToDelete(final String commaSeparatedBackupString) {
        final int index = commaSeparatedBackupString.indexOf(COMMA_SEPARATOR);
        LOGGER.debug("index of comma : {}", index);
        return index != -1 ? commaSeparatedBackupString.substring(index + 1) : "";
    }

    /**
     * Method to retrieve backup names in the form of a list for the input backup data of the form - BackupName|Domain|Type.
     *
     * @param inputBackupDataList
     * @return backupNameList
     */
    public List<String> getBackupNameList(final List<String> inputBackupDataList) {
        final List<String> backupNameList = new ArrayList<String>();
        String backupName = null;
        for (final String backupData : inputBackupDataList) {
            backupName = backupData.split(BACKUP_DATA_SPLIT_CHARACTER)[0];
            backupNameList.add(backupName);
        }
        return backupNameList;
    }

    /**
     * Method to group backup names based on node name, backup domain,type and location.
     *
     * @param backupDataList
     * @return backupsGroupedByDomainTypeAndLoc
     */

    public Map<String, List<String>> groupBackupNamesByDomainTypeAndLoc(final List<String> backupDataList) {
        final Map<String, List<String>> backupsGroupedByDomainTypeAndLoc = new HashMap<String, List<String>>();

        for (final String backupData : backupDataList) {

            final String[] backupDetails = backupData.split(BACKUP_DATA_SPLIT_CHARACTER);
            LOGGER.debug("backupDetails length : {}", backupDetails.length);

            String backupDomain = null;
            String backupType = null;
            String backupLocation = null;
            final String backupName = backupDetails[0];

            final int backupDetailsLength = backupDetails.length;
            LOGGER.debug("backupData.length in groupBackupNamesByDomainTypeAndLocation : {}", backupDetailsLength);

            // Either the domain or type or both can be empty. So, consider them empty accordingly so that existence of BrmBackupManager can be checked in these cases.
            if (backupDetailsLength == 4) {
                backupDomain = backupDetails[1];
                backupType = backupDetails[2];
                backupLocation = backupDetails[3];
            } else if (backupDetailsLength == 3) {
                backupDomain = backupDetails[1];
                backupType = "";
                backupLocation = backupDetails[2];
            } else if (backupDetailsLength == 2) {
                backupDomain = "";
                backupType = "";
                backupLocation = backupDetails[1];
            }

            final String groupKey = backupDomain + BACKUP_DELIMITER + backupType + BACKUP_DELIMITER + backupLocation;
            LOGGER.debug("group key in ecimbackuputils : {}", groupKey);

            List<String> backupNameList = backupsGroupedByDomainTypeAndLoc.get(groupKey);
            if (backupNameList == null) {
                backupNameList = new ArrayList<String>();
                backupsGroupedByDomainTypeAndLoc.put(groupKey, backupNameList);
            }
            backupNameList.add(backupName);
        }
        LOGGER.debug("backupsGroupedByDomainTypeAndLocation in ecimbackuputils : {}", backupsGroupedByDomainTypeAndLoc);
        return backupsGroupedByDomainTypeAndLoc;
    }

    /**
     * Method returns the EcimBackupInfo object from the input backup data - BackupName|Domain|Type.
     *
     * @param backupToBeDeleted
     * @return ecimBackupInfo
     */
    public EcimBackupInfo getEcimBackupInfo(final String backupToBeDeleted) {
        final String[] backupData = backupToBeDeleted.split(BACKUP_DATA_SPLIT_CHARACTER);
        final String backupName = backupData[0];

        final int backupDetailsLength = backupData.length;
        LOGGER.debug("backupData.length in getEcimBackupInfo : {}", backupDetailsLength);

        String backupDomain = null;
        String backupType = null;

        // Either the domain or type or both can be empty. So, consider them
        // empty accordingly so that existence of BrmBackupManager can be
        // checked in these cases.
        if (backupDetailsLength == 3) {
            backupDomain = backupData[1];
            backupType = backupData[2];
        } else if (backupDetailsLength == 2) {
            backupDomain = backupData[1];
            backupType = "";
        } else if (backupDetailsLength == 1) {
            backupDomain = "";
            backupType = "";
        }

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupDomain, backupName, backupType);
        return ecimBackupInfo;

    }

    /**
     * Method returns the EcimBackupInfo object from the input backup data - BackupName|Domain|Type.
     *
     * @param backupToBeUploaded
     * @return ecimBackupInfo
     */
    public EcimBackupInfo getEcimBackupInfoForUpload(final String backupToBeUploaded) {
        String backupType = "";
        String backupDomain = "";
        String backupName = "";
        try {
            final int typeIndex = backupToBeUploaded.lastIndexOf(BACKUP_DATA_SPLIT_CHARACTER_FOR_UPLOAD);

            final int domainIndex = backupToBeUploaded.substring(0, typeIndex).lastIndexOf(BACKUP_DATA_SPLIT_CHARACTER_FOR_UPLOAD);
            // Either the domain or type or both can be empty. So, consider them empty accordingly so that existence of BrmBackupManager can be checked in these cases.

            backupType = backupToBeUploaded.substring(typeIndex + 1);
            backupDomain = backupToBeUploaded.substring(domainIndex + 1, typeIndex);
            backupName = backupToBeUploaded.substring(0, domainIndex);

            LOGGER.debug("backupName:{},backupDomain:{},backupType:{}", backupName, backupDomain, backupType);
        } catch (final StringIndexOutOfBoundsException ex) {
            LOGGER.error("Backup doesn't exists with provided data BackupName:{},BackupDomain:{},BackupType:{}", backupName, backupDomain, backupType);
        }

        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupDomain, backupName, backupType);
        return ecimBackupInfo;

    }

    /**
     * This method converts the input list of backups into comma separated values.
     *
     * @param backupsToBeDeleted
     * @return backupDataStringBuilder
     */
    public String getCommaSeparatedBackupData(final List<String> backupsToBeDeleted) {
        LOGGER.debug("Inside getCommaSeparatedBackupData() with backupsToBeDeleted - {}.", backupsToBeDeleted);
        final StringBuilder backupDataStringBuilder = new StringBuilder();
        for (final String backup : backupsToBeDeleted) {
            backupDataStringBuilder.append(backup);
            backupDataStringBuilder.append(",");
        }

        final int backupDataLength = backupDataStringBuilder.length();
        final String backupDataString = backupDataStringBuilder.substring(0, backupDataLength - 1);
        LOGGER.debug("BackupDataLength : {} and backupDataString : {} ", backupDataLength, backupDataString);
        return backupDataLength > 0 ? backupDataString : "";
    }

    /**
     * This method will sort the list of BrmBackups in Ascending or Descending order based on the input given.
     *
     * @param brmBackups
     * @param isAscending
     */
    public void sortBrmBackUpsByCreationTime(final List<BrmBackup> brmBackups, final boolean isAscending) {
        Collections.sort(brmBackups, new Comparator<BrmBackup>() {
            @Override
            public int compare(final BrmBackup brmBackup1, final BrmBackup brmBackup2) {
                Date date1 = null;
                Date date2 = null;
                try {
                    if (brmBackup1.getCreationTime() != null && !brmBackup1.getCreationTime().getDateTime().isEmpty()) {
                        date1 = EcimDateSorterUtil.getFormatedDateForEcim(brmBackup1.getCreationTime().getDateTime());
                    } else {
                        date1 = new Date(0);
                    }
                    if (brmBackup2.getCreationTime() != null && !brmBackup2.getCreationTime().getDateTime().isEmpty()) {
                        date2 = EcimDateSorterUtil.getFormatedDateForEcim(brmBackup2.getCreationTime().getDateTime());
                    } else {
                        date2 = new Date(0);
                    }
                } catch (final Exception ex) {
                    LOGGER.error("Failed to parse the date while sorting Brmbackups. Exception: {}, Message: {}", ex, ex.getMessage());
                    final String exceptionMessage = "Failed to parse the date while sorting Brmbackups. Exception: " + ex;
                    throw new BackupSortException(exceptionMessage);
                }
                if (isAscending) {
                    if (date1.compareTo(date2) > 0) {
                        return +1;
                    } else if (date1.compareTo(date2) == 0) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    if (date1.compareTo(date2) > 0) {
                        return -1;
                    } else if (date1.compareTo(date2) == 0) {
                        return 0;
                    } else {
                        return +1;
                    }
                }
            }
        });
    }

    public Map<String, List<EcimBackupItem>> getNodeBackupActivityItems(final NeInfoQuery neInfoQuery) {
        final Map<String, List<EcimBackupItem>> allNodesBackupItems = new HashMap<String, List<EcimBackupItem>>();
        LOGGER.info("Need to update the NodeParams attributes to the Response json, since it needs Domain for Backup items (domain/backuptype)");
        final List<NetworkElement> networkElements = fdnServiceBean.getNetworkElements(neInfoQuery.getNeFdns(), SHMCapabilities.BACKUP_JOB_CAPABILITY);
        LOGGER.info("All the queried fdns transformed to {}-NetworkElement objects.", networkElements.size());
        final NetworkElementGroup batchedNEs = neBatchPreparator.groupNetworkElementsByModelidentity(networkElements, FragmentType.ECIM_BRM_TYPE.getFragmentName());
        LOGGER.info("According to the Fragment version, networkElements are divided into {} number of batches", batchedNEs.getNetworkElementMap().size());
        LOGGER.info("Unsupported/Unsynched Nodes are found as ::{}", batchedNEs.getUnSupportedNetworkElements());

        for (final Entry<OssModelInfo, List<NetworkElement>> entrySet : batchedNEs.getNetworkElementMap().entrySet()) {
            final String referenceMIMVersion = entrySet.getKey().getReferenceMIMVersion();

            try {
                final BrmHandler brmHandler = handlersProviderFactory.getBrmHandler(referenceMIMVersion);
                final Map<String, List<EcimBackupItem>> backupActivityItems = brmHandler.getBackupActivityItems(entrySet.getValue(), entrySet.getKey().getReferenceMIMNameSpace());
                allNodesBackupItems.putAll(backupActivityItems);
            } catch (final Exception exception) {
                LOGGER.error("Fragment will Not be Supported, due to {} ", exception);
            }
        }
        // }
        LOGGER.info("Found {} Domain activity information elements", allNodesBackupItems.size());
        return allNodesBackupItems;
    }

    public Set<String> getCommonBackupActivityItems(final Map<String, List<EcimBackupItem>> allNodesBackupItems) {
        Set<String> result = null;
        for (final Entry<String, List<EcimBackupItem>> backupItemList : allNodesBackupItems.entrySet()) {
            final Set<String> domainItems = new HashSet<String>();
            for (final EcimBackupItem eachBackup : backupItemList.getValue()) {
                domainItems.add(eachBackup.getDomain() + "/" + eachBackup.getType());
            }
            if (result == null) {
                result = new HashSet<String>();
                result.addAll(domainItems);
            }
            result.retainAll(domainItems);
            if (result.isEmpty()) {
                // for Fast failing
                return null;
            }
        }

        return result;
    }

    public int getCountOfTotalBackups(final Map<String, Object> activityJobAttributes) {
        int totalBackups = 0;
        final String totalBackupsString = activityUtils.getActivityJobAttributeValue(activityJobAttributes, EcimBackupConstants.TOTAL_BACKUPS);
        if (totalBackupsString != null && !totalBackupsString.isEmpty()) {
            totalBackups = Integer.parseInt(totalBackupsString);
        }
        LOGGER.debug("Total number of backups: {}", totalBackups);
        return totalBackups;
    }

    public int fetchActionIdFromResponseAttributes(final Object actionIdFromResponse) {
        int actionId = -1;
        if (actionIdFromResponse != null) {
            final String actionIdString = actionIdFromResponse.toString();
            try {
                if ("TRUE".equalsIgnoreCase(actionIdString)) {
                    actionId = 0;
                } else if ("FALSE".equalsIgnoreCase(actionIdString)) {
                    actionId = -1;
                } else {//except SAPC all nodes returns integer when backup is created on the node
                    actionId = Integer.parseInt(actionIdString);
                }
            } catch (final NumberFormatException nfe) {
                actionId = -1;
            }
        }
        return actionId;
    }

    /**
     * Fetches {@linkplain EcimCommonConstants.ProductData.PRODUCT_NUMBER} and {@linkplain EcimCommonConstants.ProductData.PRODUCT_REVISION}from NE through DPS based delegator and return
     * {@link EcimBackupInfo} having the backup name generated with active software details appended with timestamp.
     *
     * @param neJobStaticData
     * @param jobLogList
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     * @throws JobDataNotFoundException
     *             If required details are not available to generate the backup name.
     * @return {@link EcimBackupInfo}
     */
    public EcimBackupInfo getBackupWithAutoGeneratedName(final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList)
            throws UnsupportedFragmentException, MoNotFoundException, JobDataNotFoundException {
        final Map<String, Object> neJobAttributes = configServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId());
        final Map<String, String> backupDetailsMap = getBackupManagerDetails(neJobStaticData);
        final String nodeName = neJobStaticData.getNodeName();
        String domainName = null;
        String backupType = null;
        String customizedBackupName = backupDetailsMap.get(EcimBackupConstants.BRM_BACKUP_NAME);
        final String backupManagerId = backupDetailsMap.get(EcimBackupConstants.BRM_BACKUP_MANAGER_ID);
        LOGGER.debug("In method getBackupWithAutoGeneratedName with backupName {}, backupManagerId {} and backupDetailsMap {}.", customizedBackupName, backupManagerId, backupDetailsMap);
        final String autoGenerateBackup = backupDetailsMap.get(JobPropertyConstants.AUTO_GENERATE_BACKUP);
        if ((customizedBackupName == null || customizedBackupName.isEmpty())
                && (autoGenerateBackup == null || autoGenerateBackup.isEmpty() || !ActivityConstants.CHECK_TRUE.equals(autoGenerateBackup))) {
            throw new BackupDataNotFoundException(JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST);
        }
        String backupName = prepareBackupNameWithActiveSoftwareDetails(neJobStaticData, nodeName, customizedBackupName, neJobAttributes);
        LOGGER.debug("Backup name to create for {} having neJobId as {} is {}", nodeName, neJobStaticData.getNeJobId(), backupName);
        backupName = prepareDefaultBackupName(backupName, autoGenerateBackup, nodeName, jobLogList);
        final Map<String, String> domainBackupTypeMap = getDomainIdAndBackupType(backupManagerId);
        domainName = domainBackupTypeMap.get(EcimBackupConstants.DOMAIN_NAME);
        backupType = domainBackupTypeMap.get(EcimBackupConstants.BACKUP_TYPE);
        if (backupDetailsMap.get(JobPropertyConstants.SECURE_BACKUP_KEY) != null) {
            final String password = backupDetailsMap.get(JobPropertyConstants.SECURE_BACKUP_KEY);
            String userLabel = backupDetailsMap.get(JobPropertyConstants.USER_LABEL);
            if (userLabel == null || userLabel.isEmpty()) {
                userLabel = JobPropertyConstants.DEFAULT_USER_LABEL_FROM_ENM;
            }
            return new SecureEcimBackupInfo(domainName, backupName, backupType, password, userLabel);
        } else {
            return new EcimBackupInfo(domainName, backupName, backupType);
        }
    }

    private String prepareDefaultBackupName(String backupName, final String autoGenerateBackup, final String nodeName, final List<Map<String, Object>> jobLogList) {
        if (backupName == null || backupName.isEmpty()) {
            if (autoGenerateBackup == null || autoGenerateBackup.isEmpty() || !ActivityConstants.CHECK_TRUE.equals(autoGenerateBackup)) {
                final String errorMessage = String.format(ACTIVE_SOFTWARE_DETAILS_NOT_FOUND_FOR_NE, nodeName) + " and autogeneration is not selected. So failing the activity having backupName "
                        + backupName;
                LOGGER.error(errorMessage);
                throw new BackupDataNotFoundException(JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST);
            } else {
                backupName = appendTimeStampAndValidateBackupName(EcimBackupConstants.DEFAULT_BACKUP_NAME, true);
                final String jobLogMessage = String.format(JobLogConstants.AUTOGENERATE_DEFAULT_BACKUP, backupName);
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
                final String warningMessage = jobLogMessage.substring(0, jobLogMessage.length() - 1) + " for node " + nodeName;
                LOGGER.warn(warningMessage);
            }
        }
        return backupName;
    }

    private String prepareBackupNameWithActiveSoftwareDetails(final NEJobStaticData neJobStaticData, final String nodeName, final String customizedBackupName,
            final Map<String, Object> neJobAttributes) throws JobDataNotFoundException, UnsupportedFragmentException, MoNotFoundException {
        String backupName = activityUtils.getActivityJobAttributeValue(neJobAttributes, EcimBackupConstants.BRM_BACKUP_NAME);
        if (backupName == null || backupName.isEmpty()) {
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            final Map<String, String> activeSoftwareDetails = upMoServiceRetryProxy.getActiveSoftwareDetailsFromNode(nodeName);
            LOGGER.debug("In method getBackupWithAutoGeneratedName, fetched active software for node {} as {}", nodeName, activeSoftwareDetails);
            if (activeSoftwareDetails == null || activeSoftwareDetails.isEmpty()) {
                // If we are not able to get data from node due to any reason then get from DB the existing data ( which might be old due to not synced yet in DB).
                final String message = String.format(ACTIVE_SOFTWARE_DETAILS_NOT_FOUND_FOR_NE, nodeName) + ". Trying to fetch the details present in DB.";
                LOGGER.warn(message);
                backupName = prepareBackupNameWithActiveSoftwareDetailsFromDB(jobStaticData, nodeName, customizedBackupName);
            } else {
                backupName = prepareBackupNameWithActiveSoftwareDetailsFromNE(activeSoftwareDetails, nodeName, customizedBackupName);
            }
        }
        return backupName;
    }

    private String prepareBackupNameWithActiveSoftwareDetailsFromNE(final Map<String, String> activeSoftwareDetails, final String nodeName, String customizedBackupName) {
        final String productNumber = activeSoftwareDetails.get(EcimCommonConstants.ProductData.PRODUCT_NUMBER);
        final String productRevision = activeSoftwareDetails.get(EcimCommonConstants.ProductData.PRODUCT_REVISION);
        if (isNullOrEmptyProductNumRev(productNumber, productRevision)) {
            final String message = String.format(ACTIVE_SOFTWARE_DETAILS_NOT_FOUND_FOR_NE, nodeName) + " from NE.";
            LOGGER.warn(message);
            return validateAndAppendTimeStampForActiveSoftwareDetails(customizedBackupName, "", "", nodeName);
        }
        return validateAndAppendTimeStampForActiveSoftwareDetails(customizedBackupName, productNumber, productRevision, nodeName);
    }

    private String prepareBackupNameWithActiveSoftwareDetailsFromDB(final JobStaticData jobStaticData, final String nodeName, String customizedBackupName) {

        accessControl.setAuthUserSubject(jobStaticData.getOwner());
        final Map<String, String> activeSoftwareDetails = activeSoftwareProvider.getActiveSoftwareDetails(Arrays.asList(nodeName));
        LOGGER.debug("In method prepareBackupNameWithActiveSoftwareDetailsFromDB, fetched active software from DB {} as {}", nodeName, activeSoftwareDetails);
        if (activeSoftwareDetails == null || activeSoftwareDetails.isEmpty()) {
            final String message = String.format(ACTIVE_SOFTWARE_DETAILS_NOT_FOUND_FOR_NE, nodeName) + " from DB" + DEFAULT_BACKUP_NAME_MESSAGE;
            LOGGER.warn(message);
            return validateAndAppendTimeStampForActiveSoftwareDetails(customizedBackupName, "", "", nodeName);
        }
        return getActiveSoftwareDetailsInBackupName(activeSoftwareDetails, nodeName, customizedBackupName);

    }

    private String getActiveSoftwareDetailsInBackupName(final Map<String, String> activeSoftwareDetails, final String nodeName, String customizedBackupName) {
        String backupName = ActivityConstants.EMPTY;
        if (activeSoftwareDetails.get(nodeName) != null) {
            final String[] productDataDetails = activeSoftwareDetails.get(nodeName).split(PRODUCT_DATA_SPLIT_CHARACTER);
            if (productDataDetails != null && productDataDetails.length >= 2) {
                final String productNumber = productDataDetails[0];
                final String productRevision = productDataDetails[1];
                if (!isNullOrEmptyProductNumRev(productNumber, productRevision)) {
                    backupName = validateAndAppendTimeStampForActiveSoftwareDetails(customizedBackupName, productNumber, productRevision, nodeName);
                }
            }
        }
        return backupName;
    }

    private String getCustomizedBackupName(String customizedBackupName, final String nodeName, final String productNumber, final String productRevision) {
        if (customizedBackupName.contains(ShmConstants.NODE_NAME_PLACEHOLDER)) {
            customizedBackupName = customizedBackupName.replace(ShmConstants.NODE_NAME_PLACEHOLDER, nodeName);
        }
        if (customizedBackupName.contains(ShmConstants.PRODUCT_NUMBER_PLACEHOLDER)) {
            customizedBackupName = getCustomizedName(productNumber, ShmConstants.PRODUCT_NUMBER_PLACEHOLDER, customizedBackupName);
        }
        if (customizedBackupName.contains(ShmConstants.PRODUCT_REVISION_PLACEHOLDER)) {
            customizedBackupName = getCustomizedName(productRevision, ShmConstants.PRODUCT_REVISION_PLACEHOLDER, customizedBackupName);
        }
        return customizedBackupName;
    }

    private String getCustomizedName(final String productData, final String productDataPlaceholder, String customizedBackupName) {
        if (productData.isEmpty()) {
            final String name = ShmConstants.DELIMITER_UNDERSCORE + productDataPlaceholder;
            if (customizedBackupName.contains(name)) {
                return customizedBackupName.replace(name, productData);
            } else {
                return customizedBackupName.replace(productDataPlaceholder, productData);
            }
        } else {
            return customizedBackupName.replace(productDataPlaceholder, productData);
        }
    }

    private String validateAndAppendTimeStampForActiveSoftwareDetails(String customizedBackupName, final String productNumber, final String productRevision, final String nodeName) {
        customizedBackupName = getCustomizedBackupName(customizedBackupName, nodeName, productNumber, productRevision);
        customizedBackupName = customizedBackupName.replace(ActivityConstants.SLASH, ActivityConstants.UNDERSCORE);
        final String validatedProdNum_ProdRev = autoGenerateNameValidator.getValidatedAutoGenerateBackupName(customizedBackupName);
        return appendTimeStampAndValidateBackupName(validatedProdNum_ProdRev, false);
    }

    private boolean isNullOrEmptyProductNumRev(final String productNumber, final String productRevision) {
        return (productNumber == null || productRevision == null || productNumber.isEmpty() || productRevision.isEmpty() || ShmCommonConstants.STRING_NULL.equalsIgnoreCase(productNumber)
                || ShmCommonConstants.STRING_NULL.equalsIgnoreCase(productRevision));
    }

}
