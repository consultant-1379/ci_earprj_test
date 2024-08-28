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
package com.ericsson.oss.services.shm.es.impl.ecim.deleteup;

import static com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants.UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReader;
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReaderRetryProxy;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.common.NodeReadAttributeFailedException;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackup;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackupCreationType;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.ProductData;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.EcimSwMConstants;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SwMHandler;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SwMVersionHandlersProviderFactory;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@Stateless
public class DeleteUpgradePackageDataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteUpgradePackageDataCollector.class);

    private final String[] UP_MO_ADMINISTRATIVE_DATA_ARRAY = { UP_MO_ADMINISTRATIVE_DATA };

    @Inject
    private DpsReader dpsReader;

    @Inject
    private NodeAttributesReader nodeAttributesReader;

    @Inject
    private NodeAttributesReaderRetryProxy nodeAttributesReaderRetryProxy;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private FragmentVersionCheck fragmentVersionCheck;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private SwMVersionHandlersProviderFactory swMprovidersFactory;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private OssModelInfoProvider ossModelInfoProvider;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void persistUPAndReferredBkpsData(final Map<String, Set<String>> upMOData, final long activityJobId, final String swmNamespace) {
        final String upAndRefferredBkpsAsString = convertToString(upMOData);
        if (upAndRefferredBkpsAsString != null) {
            final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
            activityUtils.prepareJobPropertyList(jobPropertyList, EcimCommonConstants.CVS_AND_UPS_TOBE_DELETED, upAndRefferredBkpsAsString);
            activityUtils.prepareJobPropertyList(jobPropertyList, EcimCommonConstants.SWM_NAMESPACE, swmNamespace);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
        }
    }

    /**
     * Persists UPs and their System Created Backups (SYSCR) in Job property. This executes only for RadioNode type Nodes.
     * @param upMOData
     * @param activityJobId
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void persistUPsWithSyscrBkpsDataInActivityJobProperties(final Map<String, Set<String>> upMOData, final long activityJobId) {
        final String upWithSyscrBkpsAsString = convertToString(upMOData);
        if (upWithSyscrBkpsAsString != null) {
            LOGGER.debug("Persisting upsWithSyscrBackup: {}", upWithSyscrBkpsAsString);
            final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
            activityUtils.prepareJobPropertyList(jobPropertyList, EcimCommonConstants.UPS_WITH_SYSCR_BACKUPS, upWithSyscrBkpsAsString);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void persistUPDataToBeProcessed(final String currentBackup, final String currentUpMoData, final Set<String> listOfBackups, final List<String> listOfUpFdns, final long activityJobId) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();

        final String listOfUpMoAsString = convertToString(listOfUpFdns);
        final String listOfBkpsAsString = convertToString(listOfBackups);
        if (listOfUpMoAsString != null && listOfBkpsAsString != null) {
            activityUtils.prepareJobPropertyList(jobPropertyList, DeleteUpgradePackageConstants.CURRENT_BKPNAME, currentBackup);
            activityUtils.prepareJobPropertyList(jobPropertyList, DeleteUpgradePackageConstants.CURRENT_UP_MO_DATA, currentUpMoData);
            activityUtils.prepareJobPropertyList(jobPropertyList, DeleteUpgradePackageConstants.REFFERED_BKPS_TO_BE_DELETED, listOfBkpsAsString);
            activityUtils.prepareJobPropertyList(jobPropertyList, DeleteUpgradePackageConstants.PRODUCT_DATA_LIST_TO_BE_DELETED, listOfUpMoAsString);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private void persistBackupDataToBeProcessed(final String currentBackup, final Set<String> listOfBackups, final long activityJobId) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final String listOfBkpsAsString = convertToString(listOfBackups);
        if (listOfBkpsAsString != null) {
            activityUtils.prepareJobPropertyList(jobPropertyList, DeleteUpgradePackageConstants.CURRENT_BKPNAME, currentBackup);
            activityUtils.prepareJobPropertyList(jobPropertyList, DeleteUpgradePackageConstants.REFFERED_BKPS_TO_BE_DELETED, listOfBkpsAsString);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void persistUpMoFdns(final Set<UpgradePackageBean> upFdnData, final Map<String, Set<String>> upMOReferredBKPMap, final long activityJobId) {
        for (final Map.Entry<String, Set<String>> entry : upMOReferredBKPMap.entrySet()) {
            if (!entry.getKey().isEmpty()) {
                for (final UpgradePackageBean bean : upFdnData) {
                    if (entry.getKey().equals(bean.getProductNumber() + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + bean.getProductRevision())) {
                        persistUpMoFdnToBeProcessed(entry.getKey(), bean.getMoFdn(), activityJobId);
                    }
                }
            }
        }
    }

    private void persistUpMoFdnToBeProcessed(final String currentUpMoProductDataAsKey, final String currentUpMoFdnAsValue, final long activityJobId) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.prepareJobPropertyList(jobPropertyList, currentUpMoProductDataAsKey, currentUpMoFdnAsValue);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Map<String, String> getUPMODataToBeProcessed(final JobEnvironment jobEnvironment) {
        final List<Map<String, String>> activityJobPropertyList = (List<Map<String, String>>) jobEnvironment.getActivityJobAttributes().get(ShmConstants.JOBPROPERTIES);
        final Map<String, String> uPDataToBeProcessed = new HashMap<>();
        if (activityJobPropertyList != null) {
            for (final Map<String, String> eachJobProperty : activityJobPropertyList) {
                if (DeleteUpgradePackageConstants.CURRENT_BKPNAME.equals(eachJobProperty.get(ShmConstants.KEY))) {
                    uPDataToBeProcessed.put(DeleteUpgradePackageConstants.CURRENT_BKPNAME, eachJobProperty.get(ShmConstants.VALUE));
                }
                if (DeleteUpgradePackageConstants.CURRENT_UP_MO_DATA.equals(eachJobProperty.get(ShmConstants.KEY))) {
                    uPDataToBeProcessed.put(DeleteUpgradePackageConstants.CURRENT_UP_MO_DATA, eachJobProperty.get(ShmConstants.VALUE));
                }
                if (DeleteUpgradePackageConstants.REFFERED_BKPS_TO_BE_DELETED.equals(eachJobProperty.get(ShmConstants.KEY))) {
                    uPDataToBeProcessed.put(DeleteUpgradePackageConstants.REFFERED_BKPS_TO_BE_DELETED, eachJobProperty.get(ShmConstants.VALUE));
                }
                if (DeleteUpgradePackageConstants.PRODUCT_DATA_LIST_TO_BE_DELETED.equals(eachJobProperty.get(ShmConstants.KEY))) {
                    uPDataToBeProcessed.put(DeleteUpgradePackageConstants.PRODUCT_DATA_LIST_TO_BE_DELETED, eachJobProperty.get(ShmConstants.VALUE));
                }
            }
        }
        return uPDataToBeProcessed;
    }

    public String getSwmFdn(final String nodeName, final String swmNameSpace) {
        String swmMOFdn = null;
        final List<ManagedObject> managedObjectList = getManagedObjects(nodeName, swmNameSpace, EcimSwMConstants.SWM_MO_TYPE);
        if (!managedObjectList.isEmpty()) {
            final ManagedObject managedObject = managedObjectList.get(0);
            swmMOFdn = managedObject.getFdn();
        }
        return swmMOFdn;
    }

    /**
     * This method returns the AsyncActionProgress ENUM containing the attributes having details of current activity in progress.
     * 
     * @param nodeName
     *            , activityName, modifiedAttributes
     * @return AsyncActionProgress
     */
    public AsyncActionProgress getValidAsyncActionProgress(final String nodeName, final Map<String, AttributeChangeData> modifiedAttributes, final String neType, final String ossModelIdentity)
            throws UnsupportedFragmentException {
        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(neType, ossModelIdentity);
        return softwareManagementHandler.getValidUpgradePackageAndBackupAsyncActionProgress(modifiedAttributes);
    }

    private Set<String> getNodeUpAdministrativeData(final List<ManagedObject> managedObjectList) {
        final Set<String> adminDataSet = new HashSet<>();
        for (final ManagedObject managedObjectItem : managedObjectList) {
            Map<String, Object> moAttributes;
            try {
                moAttributes = nodeAttributesReaderRetryProxy.readAttributesWithRetry(managedObjectItem, UP_MO_ADMINISTRATIVE_DATA_ARRAY);
            } catch (final NodeAttributesReaderException nodeAttributesReaderException) {
                LOGGER.error("Node read attributes with retries has failed for the MO {}. Reason : {}", managedObjectItem.getFdn(), nodeAttributesReaderException);
                throw new NodeReadAttributeFailedException(nodeAttributesReaderException);
            }
            final List<Map<String, Object>> administrativeDataList = (List<Map<String, Object>>) moAttributes.get(UP_MO_ADMINISTRATIVE_DATA);
            if (administrativeDataList != null && !administrativeDataList.isEmpty()) {
                for (final Map<String, Object> administrativeData : administrativeDataList) {
                    if (administrativeData != null && !administrativeData.isEmpty() && administrativeData.get(EcimCommonConstants.ProductData.PRODUCT_NUMBER) != null
                            && administrativeData.get(EcimCommonConstants.ProductData.PRODUCT_REVISION) != null) {
                        final String adminData = (String) administrativeData.get(EcimCommonConstants.ProductData.PRODUCT_NUMBER) + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR
                                + (String) administrativeData.get(EcimCommonConstants.ProductData.PRODUCT_REVISION) + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + managedObjectItem.getFdn();
                        adminDataSet.add(adminData);
                    }
                }
            }
            LOGGER.debug("Delete Upgradepackage administrativeDataSet: {} ", adminDataSet);
        }
        return adminDataSet;
    }

    public Set<String> getUpData(final String nodeName, final String sWMNamespace) {
        Set<String> nodeUpData = null;
        final List<ManagedObject> nodeUPMOList = getManagedObjects(nodeName, sWMNamespace, UpgradeActivityConstants.UP_MO_TYPE);
        if (!nodeUPMOList.isEmpty()) {
            nodeUpData = getNodeUpAdministrativeData(nodeUPMOList);
        }
        return nodeUpData;
    }

    public Set<String> fetchValidUPDataOverInputData(final Set<String> nodeData, final Set<String> inputProductDataSet) {
        final Set<String> nodeProductData = new HashSet<>();
        final Set<String> inputData = new HashSet<>(inputProductDataSet);
        if (CollectionUtils.isNotEmpty(nodeData)) {
            for (final String data : nodeData) {
                final String productData[] = data.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
                nodeProductData.add(productData[0] + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + productData[1]);
            }
        }
        inputData.retainAll(nodeProductData);
        return inputData;
    }

    public Set<String> prepareNodeProductData(final Set<String> nodeData) {
        final Set<String> nodeProductData = new HashSet<>();
        if (!nodeData.isEmpty()) {
            for (final String data : nodeData) {
                final String[] productData = data.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
                nodeProductData.add(productData[0] + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + productData[1]);
            }
        }
        return nodeProductData;
    }

    private String getActiveUPData(final ManagedObject activeSwMO) {
        String productData = null;
        final Map<String, Object> moAttributes = nodeAttributesReader.readAttributes(activeSwMO, UP_MO_ADMINISTRATIVE_DATA_ARRAY);
        LOGGER.info("moAttributes in ecim {}", moAttributes);
        if (moAttributes != null) {
            final Map<String, Object> administrativeData = (Map<String, Object>) moAttributes.get(UP_MO_ADMINISTRATIVE_DATA);
            if (administrativeData != null && !administrativeData.isEmpty()) {
                productData = (String) administrativeData.get(EcimCommonConstants.ProductData.PRODUCT_NUMBER) + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR
                        + (String) administrativeData.get(EcimCommonConstants.ProductData.PRODUCT_REVISION);
            }
        }
        return productData;
    }

    public Set<String> filterActiveUps(final String nodeName, final String sWIMNamespace, final Set<String> filteredValidUPMOData) {
        final List<ManagedObject> activeSwMOs = getActiveSoftwareMos(nodeName, sWIMNamespace);
        final Set<String> activeUpDataList = new HashSet<>();
        final Set<String> finalValidData = new HashSet<>(filteredValidUPMOData);
        if (!activeSwMOs.isEmpty()) {
            for (final ManagedObject activeSwMO : activeSwMOs) {
                final String activeUpProductData = getActiveUPData(activeSwMO);
                if (activeUpProductData != null) {
                    activeUpDataList.add(activeUpProductData);
                }
            }
        }
        finalValidData.removeAll(activeUpDataList);
        LOGGER.info("In Delete UP ECIM filterActiveUps  filteredValidUPMOData {} finalValidData {}", filteredValidUPMOData, finalValidData);
        return finalValidData;
    }

    public List<ManagedObject> getActiveSoftwareMos(final String nodeName, final String namespace) {
        ManagedObject activeSwVersionFdn = null;
        final List<ManagedObject> activeSwVersionFdnList = new ArrayList<>();
        final List<ManagedObject> swInventoryMos = getManagedObjects(nodeName, namespace, UpgradeActivityConstants.ECIM_SWINVENTORY_TYPE);
        if (!swInventoryMos.isEmpty()) {
            for (final ManagedObject swInventory : swInventoryMos) {
                LOGGER.debug("Fetching the active software versions for the swInventory: {}", swInventory.getFdn());
                final List<String> activeSwVersionFdns = (List<String>) swInventory.getAttribute(EcimCommonConstants.SWMO_ACTIVE);
                if (activeSwVersionFdns != null && !activeSwVersionFdns.isEmpty()) {
                    for (final String fdn : activeSwVersionFdns) {
                        LOGGER.debug("Delete Upgrade Package activity active SWMO fdn: {} ", fdn);
                        activeSwVersionFdn = dpsReader.findMoByFdn(fdn);
                        activeSwVersionFdnList.add(activeSwVersionFdn);
                    }
                }
            }
        }
        return activeSwVersionFdnList;
    }

    /**
     * Method to return MO list based on the type passed
     * 
     * @param neName
     * @param namespace
     * @param type
     * @return
     */

    public List<ManagedObject> getManagedObjects(final String nodeName, final String namespace, final String type) {
        final Map<String, Object> restrictions = new HashMap<>();
        return dpsReader.getManagedObjects(namespace, type, restrictions, nodeName);
    }

    /**
     * Gets Map of UP MO and referred backups based on given UP MO data and isNonActiveUpsFlagEnabled flag. If getOnlyUpsWithSyscrBackups is true, only UPs with SYSCR backups will be retrieved.
     * If getOnlyUpsWithSyscrBackups is false, only UPs with Referred backups (not SYSCR) will be retrieved. 
     *
     * @param activityJobId
     * @param upDataSet
     * @param nodeUpData
     * @param brmBackupList
     * @param isNonActiveUpsFlagEnabled
     *
     * @return : if isNonActiveUpsFlagEnabled is enabled then this method will not consider system created backups and respective UPs, else this method returns all inactive UPs and referred backups
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Map<String, Set<String>> getReferredBackups(final long activityJobId, final Set<String> upDataSet, final Set<String> nodeUpData, final List<BrmBackup> brmBackupList,
            final boolean isNonActiveUpsFlagEnabled, final boolean isOnlyUpsWithSyscrBackupsRequired) {
        Map<String, Set<String>> cvsAndUpsTobeDeleted = new HashMap<>();
        final Set<UpgradePackageBean> upgradePackageBeans = getUpMoFdnData(upDataSet, nodeUpData);
        persistUpgradePackageBeanData(upgradePackageBeans, activityJobId);
        final List<UpgradePackageBean> bkpUpList = getBackupData(brmBackupList);
        if (!bkpUpList.isEmpty()) {
            cvsAndUpsTobeDeleted = getUpsAndReferredBackups(activityJobId, upgradePackageBeans, bkpUpList, isNonActiveUpsFlagEnabled, isOnlyUpsWithSyscrBackupsRequired);
        } else {
            final Set<String> referredBkpSet = new HashSet<>();
            for (final UpgradePackageBean upData : upgradePackageBeans) {
                cvsAndUpsTobeDeleted.put(upData.getProductNumber() + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + upData.getProductRevision(), referredBkpSet);
            }
        }
        LOGGER.debug("In ECIM Delete UP getReferredBackups cvsAndUpsTobeDeleted {} , activityJobId {}", cvsAndUpsTobeDeleted, activityJobId);
        return cvsAndUpsTobeDeleted;
    }

    private Map<String, Set<String>> getUpsAndReferredBackups(final long activityJobId, final Set<UpgradePackageBean> upgradePackageBeans, final List<UpgradePackageBean> bkpUpList,
            final boolean isNonActiveUpsFlagEnabled, final boolean isOnlyUpsWithSyscrBackupsRequired) {
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final Map<String, Set<String>> upsWithDeletableBackups = new HashMap<>();
        final Map<String, Set<String>> upsWithOnlySyscrBackups = new HashMap<>();
        for (final UpgradePackageBean upData : upgradePackageBeans) {
            final Set<String> referredBkpSet = new HashSet<>();
            boolean isSystemCreatedBkpExists = false;
            for (final UpgradePackageBean referredBkpUp : bkpUpList) {
                if (upData.getProductNumber().equals(referredBkpUp.getProductNumber()) && upData.getProductRevision().equals(referredBkpUp.getProductRevision())) {
                    if (isNonActiveUpsFlagEnabled && referredBkpUp.getCreationType() == BrmBackupCreationType.SYSTEM_CREATED) {
                        LOGGER.debug("Found UP: data - {} with SYSCR Backup: data - {}", upData, referredBkpUp);
                        isSystemCreatedBkpExists = true;
                        final String logMessage = "Upgrade Package with ProductNumber: \"%s\" and ProductRevision: \"%s\" has system created backup. Hence cannot be deleted."; // 
                        final String jobLogMessage = String.format(logMessage, upData.getProductNumber(), upData.getProductRevision());
                        jobLogUtil.prepareJobLogAtrributesList(jobLogs, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                    }
                    referredBkpSet.add(referredBkpUp.getBackupName() + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR +
                            referredBkpUp.getBrmBackupManagerMoFdn() + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR +
                            referredBkpUp.getCreationType() + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + referredBkpUp.getMoFdn());
                }
            }
            if(isSystemCreatedBkpExists) {
                if(isOnlyUpsWithSyscrBackupsRequired) {
                    upsWithOnlySyscrBackups.put(upData.getProductNumber() + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + upData.getProductRevision(), referredBkpSet);
                }
            } else {
                upsWithDeletableBackups.put(upData.getProductNumber() + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + upData.getProductRevision(), referredBkpSet);
            }
        }
        if (!jobLogs.isEmpty()) {
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
        }
        LOGGER.debug("upsWithOnlySyscrBackups: {} and upsWithDeletableBackups {} with flag getOnlyUpsWithSyscrBackups {}", upsWithOnlySyscrBackups, upsWithDeletableBackups, isOnlyUpsWithSyscrBackupsRequired);
        if (isOnlyUpsWithSyscrBackupsRequired) {
            return upsWithOnlySyscrBackups;
        } else {
            return upsWithDeletableBackups;
        }
    }

    private void persistUpgradePackageBeanData(final Set<UpgradePackageBean> beans, final long activityJobId) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        for (final UpgradePackageBean bean : beans) {
            activityUtils.prepareJobPropertyList(jobPropertyList, bean.getProductNumber() + bean.getProductRevision(), bean.getMoFdn());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, null, null);
        }
    }

    public Set<UpgradePackageBean> getUpMoFdnData(final Set<String> upDataSet, final Set<String> nodeUpData) {
        final Set<UpgradePackageBean> bean = new HashSet<>();
        for (final String nodeUp : nodeUpData) {
            final String nodeData[] = nodeUp.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
            for (final String upData : upDataSet) {
                final String data[] = upData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
                if (nodeData[0].equals(data[0]) && nodeData[1].equals(data[1])) {
                    bean.add(new UpgradePackageBean(nodeData[0], nodeData[1], nodeData[2]));
                }
            }
        }
        return bean;
    }

    private List<UpgradePackageBean> getBackupData(final List<BrmBackup> brmBackupList) {
        final List<UpgradePackageBean> bkpUpList = new ArrayList<>();
        if (brmBackupList != null && !brmBackupList.isEmpty()) {
            for (final BrmBackup brmBackup : brmBackupList) {
                if (brmBackup.getSwVersion() != null && !brmBackup.getSwVersion().isEmpty()) {
                    bkpUpList.addAll(getBackupData(brmBackup));
                }
            }
        }
        LOGGER.debug("ECIM Delete Upgrade Package activity backups on node : {} ", bkpUpList);
        return bkpUpList;
    }

    private List<UpgradePackageBean> getBackupData(final BrmBackup brmBackup) {
        final List<UpgradePackageBean> bkpUpList = new ArrayList<>();
        final List<ProductData> bkpProductDataList = brmBackup.getSwVersion();
        final String brmBackupManagerMoFdn = brmBackup.getBrmBackupManager().getBrmBackupManagerMoFdn();
        for (final ProductData bkpProductData : bkpProductDataList) {
            if (bkpProductData.getProductNumber() != null && bkpProductData.getProductRevision() != null) {
                final UpgradePackageBean upgradePackageProductData = new UpgradePackageBean(bkpProductData.getProductNumber(), bkpProductData.getProductRevision());
                upgradePackageProductData.setMoFdn(brmBackup.getBrmBackupMoFdn());
                upgradePackageProductData.setBackupName((brmBackup.getBackupName()));
                upgradePackageProductData.setBrmBackupManagerMoFdn(brmBackupManagerMoFdn);
                upgradePackageProductData.setCreationType(brmBackup.getCreationType());
                bkpUpList.add(upgradePackageProductData);
            }
        }
        return bkpUpList;
    }

    public String getSWMNameSpace(final String nodeName, final String referenceNamespace) throws MoNotFoundException {
        String namespace = null;
        final NetworkElement networkElement = activityUtils.getNetworkElement(nodeName, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
        final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(networkElement.getNeType(), networkElement.getOssModelIdentity(), referenceNamespace);
        if (ossModelInfo != null) {
            namespace = ossModelInfo.getNamespace();
            LOGGER.debug("Delete Upgrade Package activity SWIM namespace {}", namespace);
        }
        return namespace;
    }

    public void checkFragmentAndUpdateLog(final List<Map<String, Object>> jobLogList, final String neType, final String ossModelIdentity) throws UnsupportedFragmentException {
        String logMessage = null;
        final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(neType, ossModelIdentity, FragmentType.ECIM_SWM_TYPE.getFragmentName());
        if (ossModelInfo != null) {
            logMessage = fragmentVersionCheck.checkFragmentVersion(FragmentType.ECIM_SWM_TYPE, ossModelInfo.getReferenceMIMVersion());
            if (logMessage != null) {
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            }
        } else {
            logMessage = String.format(JobLogConstants.UNSUPPORTED_NODE_MODEL, neType, ossModelIdentity);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
    }

    public Map<String, String> getUserProvidedData(final JobEnvironment jobEnvironment, final String nodeName, final String neType, final String platformType) {
        final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes();
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);

        final List<String> keyList = new ArrayList<>();
        keyList.add(JobPropertyConstants.DELETE_UP_LIST);
        keyList.add(JobPropertyConstants.DELETE_REFERRED_BACKUPS);
        keyList.add(JobPropertyConstants.DELETE_NON_ACTIVE_UPS);

        final Map<String, String> inputData = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, nodeName, neType, platformType);
        return inputData;
    }

    public Map<String, Object> getUserProvidedUPs(final Map<String, Object> jobConfigurationDetails, final String neName, final String neType, final String platformType) {
        final List<String> keyList = new ArrayList<>();
        keyList.add(EcimCommonConstants.ProductData.PRODUCT_NUMBER);
        keyList.add(EcimCommonConstants.ProductData.PRODUCT_REVISION);

        final Map<String, String> actionArguments = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType);
        final Map<String, Object> deleteUpgradePackageActionArguments = new HashMap<>();
        for (final Entry<String, String> arguments : actionArguments.entrySet()) {
            deleteUpgradePackageActionArguments.put(arguments.getKey(), arguments.getValue());
        }
        return deleteUpgradePackageActionArguments;
    }

    public Set<String> getInputProductData(final String inputProductData) {
        final Set<String> inputProductDataSet = new HashSet<>();
        if (!inputProductData.isEmpty()) {
            final String[] inputProductDataArray = inputProductData.split(ActivityConstants.COMMA);
            for (int i = 0; i < inputProductDataArray.length; i++) {
                final String pair = inputProductDataArray[i];
                final String[] keyValue = pair.split(DeleteUpgradePackageConstants.UP_DATA_INPUT_SEPERATOR);
                if (keyValue.length != 0) {
                    inputProductDataSet.add(keyValue[0] + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + keyValue[1]);
                }
            }
        }
        return inputProductDataSet;
    }

    public boolean performAction(final String swmMOFdn, final String actionName, final Map<String, Object> actionArguments, final String neType, final String ossModelIdentity)
            throws UnsupportedFragmentException, MoNotFoundException {
        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(neType, ossModelIdentity);

        return softwareManagementHandler.removeUpgradePackageAction(swmMOFdn, actionName, actionArguments);
    }

    private SwMHandler getSoftwareManagementHandler(final String neType, final String ossModelIdentity) throws UnsupportedFragmentException {

        final SwMHandler softwareManagementHandler = swMprovidersFactory.getSoftwareManagementHandler(getFragmentVersion(neType, ossModelIdentity));
        return softwareManagementHandler;
    }

    private String getFragmentVersion(final String neType, final String ossModelIdentity) {
        final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(neType, ossModelIdentity, FragmentType.ECIM_SWM_TYPE.getFragmentName());
        return ossModelInfo == null ? "" : ossModelInfo.getReferenceMIMVersion();
    }

    public String convertToString(final Map<String, Set<String>> upAndRefferredBkps) {
        String upAndRefferredBkpsAsString = null;
        try {
            final ObjectMapper mapper = new ObjectMapper();
            upAndRefferredBkpsAsString = mapper.writeValueAsString(upAndRefferredBkps);

        } catch (final Exception exception) {
            LOGGER.error("Unable to persist preventingUpsAndCvs {} ", exception);
        }
        return upAndRefferredBkpsAsString;
    }

    public String convertToString(final List<String> list) {
        String listOfDataAsString = null;
        try {
            final ObjectMapper mapper = new ObjectMapper();
            listOfDataAsString = mapper.writeValueAsString(list);

        } catch (final Exception exception) {
            LOGGER.error("In ECIM DeleteUp data conversion to String from list is failed, reason : ", exception.getMessage());
        }
        return listOfDataAsString;
    }

    public String convertToString(final Set<String> set) {
        String listOfDataAsString = null;
        try {
            final ObjectMapper mapper = new ObjectMapper();
            listOfDataAsString = mapper.writeValueAsString(set);

        } catch (final Exception exception) {
            LOGGER.error("In ECIM DeleteUp data conversion to String from set is failed, reason : ", exception.getMessage());
        }
        return listOfDataAsString;
    }

    public List<String> convertToList(final String input) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final List<String> listOfUpData = mapper.readValue(input, new TypeReference<List<String>>() {
            });
            return listOfUpData;
        } catch (final Exception exception) {
            LOGGER.error("In ECIM DeleteUp data conversion to list is failed, reason : {}", exception.getMessage());
        }
        return new ArrayList<>();
    }

    public Set<String> convertToSet(final String input) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final Set<String> setOfUpData = mapper.readValue(input, new TypeReference<Set<String>>() {
            });
            return setOfUpData;
        } catch (final Exception exception) {
            LOGGER.error("In ECIM DeleteUp data conversion to set is failed, reason : {}", exception.getMessage());
        }
        return new HashSet<>();
    }

    public Map<String, Set<String>> converToMap(final String input) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final Map<String, Set<String>> upAndReferredBkps = mapper.readValue(input, new TypeReference<Map<String, Set<String>>>() {
            });
            return upAndReferredBkps;
        } catch (final Exception exception) {
            LOGGER.error("In ECIM DeleteUp data conversion to map is failed, reason : {}", exception.getMessage());
        }
        return new HashMap<>();
    }

    public double calculateActivityProgressPercentage(final JobEnvironment jobEnvironment, final int totalProgressPercentage) {
        double currentProgressPercentage = 0.0;
        double totalActivityProgressPercentage = 0.0;
        double eachMO_End_ProgressPercentage = 0.0;
        final int totalUpgradepackages = getCountOfTotalUpgradePackages(jobEnvironment);
        final double totalUpgradepackagesAsDouble = totalUpgradepackages;
        final double totalProgressPercentageAsDouble = totalProgressPercentage;
        final String eachMO_End_ProgressPercentageString = activityUtils.getActivityJobAttributeValue(jobEnvironment.getActivityJobAttributes(), BackupActivityConstants.MO_ACTIVITY_END_PROGRESS);
        if (eachMO_End_ProgressPercentageString != null && !eachMO_End_ProgressPercentageString.isEmpty()) {
            eachMO_End_ProgressPercentage = Double.parseDouble(eachMO_End_ProgressPercentageString);
        }
        currentProgressPercentage = (totalProgressPercentageAsDouble / totalUpgradepackagesAsDouble);
        LOGGER.debug("ECIM delete upgrade package - totalUpgradepackages to delete on the node: {}", totalUpgradepackages);
        totalActivityProgressPercentage = eachMO_End_ProgressPercentage + currentProgressPercentage;
        LOGGER.debug("ECIM delete upgrade package - totalActivityProgressPercentage to delete on the node: {}", totalActivityProgressPercentage);
        return totalActivityProgressPercentage;
    }

    public int getCountOfTotalUpgradePackages(final JobEnvironment jobEnvironment) {
        final String productDataList = activityUtils.getActivityJobAttributeValue(jobEnvironment.getActivityJobAttributes(), DeleteUpgradePackageConstants.PRODUCT_DATA_LIST_TO_BE_DELETED);
        final List<String> persitedUpMoData = convertToList(productDataList);
        return persitedUpMoData.size();
    }

    /**
     * Gets the deleted SYSCR Backup Info from UPs with SYSCR backups, with the BrmBackupMO fdn got from Delete Event notification.
     * @param upsWithSyscrBkps
     * @param brmBackupMoFdn
     * @return
     */
    public String getDeletedSysCreatedBackupInfo(final Map<String, Set<String>> upsWithSyscrBkps, final String brmBackupMoFdn) {
        LOGGER.info("Got Delete event for BrmBackup with FDN: {}. So, checking on available USs with SYSCR Backups: {}.", brmBackupMoFdn, upsWithSyscrBkps);
        for(Map.Entry<String, Set<String>> entry :upsWithSyscrBkps.entrySet()){
            for(String backupData : entry.getValue()) {
                LOGGER.debug("Pointer on UP: {} and Backup {}", entry.getKey(), backupData);
                String[] backupInfo = backupData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
                if(BrmBackupCreationType.SYSTEM_CREATED.name().equals(backupInfo[2]) && backupInfo[3].equals(brmBackupMoFdn)){
                    LOGGER.info("Found deleted Backup with Name: {} And FDN: {}, among UPs data - {}", backupInfo[2], backupInfo[3], upsWithSyscrBkps);
                    return backupData;
                }
            }
        }
        return "";
    }

    /**
     * Checks for deleted SYSCR Backup info by reading all BrmBackups from DPS, of UPs with SYSCR Backups.
     * @param upsWithSyscrBkps
     * @param nodeName
     * @return
     */
    public String checkForDeletedSysCreatedBackupInfo(final Map<String, Set<String>> upsWithSyscrBkps, final String nodeName) {
        LOGGER.info("Checking for deleted BrmBackup on available USs with SYSCR Backups: {}.", nodeName, upsWithSyscrBkps);
        for(Map.Entry<String, Set<String>> entry :upsWithSyscrBkps.entrySet()){
            for(String backupData : entry.getValue()) {
                LOGGER.debug("Pointer on UP: {} and Backup {}", entry.getValue(), backupData);
                String[] backupInfo = backupData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
                if(BrmBackupCreationType.SYSTEM_CREATED.name().equals(backupInfo[2])){
                    try {
                        if(brmMoServiceRetryProxy.isBackupDeletionCompleted(backupInfo[0], backupInfo[1], nodeName)) {
                            LOGGER.info("Found deleted Backup with Name: {} And FDN: {}, among UPs data - {}", backupInfo[2], backupInfo[3], upsWithSyscrBkps);
                            return backupData;
                        }
                    } catch (MoNotFoundException | UnsupportedFragmentException e) {
                        LOGGER.error("Got Exception for Backup {} with reason", backupInfo[0], e);
                    }
                }
            }
        }
        return "";
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean setUpAndBackupDataForNextItration(final long activityJobId, final Map<String, Object> activityJobAttributes, final String deletedUp) {
        boolean isRepeatExecuteRequired = false;
        final List<String> upsToUpdate = new ArrayList<>();
        final String productDataList = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.PRODUCT_DATA_LIST_TO_BE_DELETED);
        final List<String> persitedUpMoData = convertToList(productDataList);

        final String cvsAndUpsTobeDeleted = activityUtils.getActivityJobAttributeValue(activityJobAttributes, EcimCommonConstants.CVS_AND_UPS_TOBE_DELETED);
        final Map<String, Set<String>> cvsAndUpsTobeDeletedMap = converToMap(cvsAndUpsTobeDeleted);
        LOGGER.debug("ECIM delete upgrade package setUpAndBackupDataForNextItration - persitedUpMoData {} and cvsAndUpsTobeDeleted {}", persitedUpMoData, cvsAndUpsTobeDeleted);
        String currentUpMoFdn = "";

        if (!persitedUpMoData.isEmpty()) {
            upsToUpdate.addAll(persitedUpMoData);
            upsToUpdate.remove(deletedUp);
            if (!upsToUpdate.isEmpty()) {
                currentUpMoFdn = upsToUpdate.get(0);
                setBackupDataToCurrentUp(activityJobId, cvsAndUpsTobeDeletedMap, currentUpMoFdn, upsToUpdate);
                isRepeatExecuteRequired = true;
            }
        }
        return isRepeatExecuteRequired;
    }

    private void setBackupDataToCurrentUp(final long activityJobId, final Map<String, Set<String>> persitedWholeUpDataMap, final String currentUpProductData, final List<String> listOfUpFdns) {
        String currentBkpName = "";
        Set<String> persistedBkps = new HashSet<>();

        String log = "";
        if (!persitedWholeUpDataMap.isEmpty()) {
            for (final Map.Entry<String, Set<String>> entry : persitedWholeUpDataMap.entrySet()) {
                if (currentUpProductData.equals(entry.getKey())) {
                    persistedBkps = entry.getValue();
                }
            }
            final String currentUpProductDataArray[] = currentUpProductData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);

            if (!persistedBkps.isEmpty()) {
                currentBkpName = persistedBkps.iterator().next();
                log = String.format(JobLogConstants.UP_AND_BACKUPDATA_FOR_DELETION, currentUpProductDataArray[0], currentUpProductDataArray[1], getBackNamesToLog(persistedBkps));
            } else {
                log = String.format(JobLogConstants.UP_MO_FDN_NO_BACKUPS, currentUpProductDataArray[0], currentUpProductDataArray[1]);
            }
            final List<Map<String, Object>> jobLogs = new ArrayList<>();
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, log, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogs, null);
            jobLogs.clear();
            LOGGER.debug("ECIM delete upgrade package setBackupDataToCurrentUp - currentBkpName {}, currentUpProductData {}, persistedBkps {}, listOfUpFdns{}", currentBkpName, currentUpProductData,
                    persistedBkps, listOfUpFdns);
            persistUPDataToBeProcessed(currentBkpName, currentUpProductData, persistedBkps, listOfUpFdns, activityJobId);
        }
    }

    private String getBackNamesToLog(final Set<String> listOfBkps) {
        final Set<String> backNames = new HashSet<>();
        for (final String backpData : listOfBkps) {
            final String backupDataArray[] = backpData.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
            backNames.add(backupDataArray[0]);
        }
        return backNames.toString();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean setBackupDataForNextItration(final long activityJobId, final String deletedBkp) {
        String currentBkpName = "";
        final Set<String> bkpsToUpdate = new HashSet<>();

        final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
        final String persistedBkps = activityUtils.getActivityJobAttributeValue(activityJobAttributes, DeleteUpgradePackageConstants.REFFERED_BKPS_TO_BE_DELETED);
        final Set<String> persistedBackupData = convertToSet(persistedBkps);

        if (!persistedBackupData.isEmpty()) {
            bkpsToUpdate.addAll(persistedBackupData);
            bkpsToUpdate.remove(deletedBkp);
            if (!bkpsToUpdate.isEmpty()) {
                currentBkpName = bkpsToUpdate.iterator().next();
            }
            persistBackupDataToBeProcessed(currentBkpName, bkpsToUpdate, activityJobId);
        } else {
            persistBackupDataToBeProcessed(currentBkpName, bkpsToUpdate, activityJobId);
        }
        final boolean repeatAlwaysRequiredAfterBackups = true;
        return repeatAlwaysRequiredAfterBackups;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void updateInactiveUps(final long neJobId, final Set<UpgradePackageBean> upFdnData, final Map<String, Set<String>> upMOReferredBKPMap) {
        try {
            final String updatedNonActiveDeletableUPs = getUpdatedInactiveUps(upFdnData, upMOReferredBKPMap);
            if (!ActivityConstants.EMPTY.equalsIgnoreCase(updatedNonActiveDeletableUPs)) {
                final List<Map<String, Object>> neJobPropertiesList = new ArrayList<>();
                activityUtils.prepareJobPropertyList(neJobPropertiesList, JobPropertyConstants.DELETE_UP_LIST, updatedNonActiveDeletableUPs);
                jobUpdateService.readAndUpdateRunningJobAttributes(neJobId, neJobPropertiesList, null, null);
            } else {
                LOGGER.warn("No inactive UPs to be deleted for NEJob with Id :{}", neJobId);
            }
        } catch (final Exception ex) {
            LOGGER.error("Failed to update inactive UP list in NEJob PO with NE Job Id: {}. Exception is:", neJobId, ex);
        }
    }

    private String getUpdatedInactiveUps(final Set<UpgradePackageBean> upFdnData, final Map<String, Set<String>> upMOReferredBKPMap) {
        final StringBuilder processedDeletableUPs = new StringBuilder();
        for (final Map.Entry<String, Set<String>> entry : upMOReferredBKPMap.entrySet()) {
            if (!entry.getKey().isEmpty()) {
                for (final UpgradePackageBean bean : upFdnData) {
                    if (entry.getKey().equals(bean.getProductNumber() + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + bean.getProductRevision())) {
                        processedDeletableUPs.append(bean.getProductNumber()).append(UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER).append(bean.getProductRevision())
                                .append(ActivityConstants.COMMA);
                    }
                }
            }
        }
        final String updatedInactiveDeletableUPs = processedDeletableUPs.toString();
        if (!ActivityConstants.EMPTY.equalsIgnoreCase(updatedInactiveDeletableUPs)) {
            return updatedInactiveDeletableUPs.substring(0, updatedInactiveDeletableUPs.length() - 1);
        }
        return ActivityConstants.EMPTY;
    }

    /**
     * Un-subscribe to BrmBackups MO & BrmBackupManager for Notifications. Only for RadioNode with 2 UPs with SYSCR backups handling.
     * @param upsWithSyscrBackups
     * @param activityJobId
     */
    public void unSubscribeToBrmBackupMosNotifications(Map<String, Set<String>> upsWithSyscrBackups, long activityJobId) {
        LOGGER.debug("Unsubscribing for BrmBackups of UPs data {} for notificaitons with Activity ID {}.", upsWithSyscrBackups, activityJobId);
        String brmBackupManagerMoFdn = "";
        JobActivityInfo jobActivityInfo = activityUtils.getActivityInfo(activityJobId, DeleteUpgradePackageService.class);
        for (final Entry<String, Set<String>> upData : upsWithSyscrBackups.entrySet()) {
            Set<String> refferedBackups = upData.getValue();
            for (final String referredBkpUp : refferedBackups) {
                String[] backupData = referredBkpUp.split(DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR);
                if (BrmBackupCreationType.SYSTEM_CREATED.toString().equals(backupData[2])) {
                    activityUtils.unSubscribeToMoNotifications(backupData[3], activityJobId, jobActivityInfo);
                }
                if(brmBackupManagerMoFdn.isEmpty()) {
                    brmBackupManagerMoFdn = backupData[1];
                }
            }
        }
        LOGGER.debug("Un Subscribing for Manager: {} with activity iD: {}", brmBackupManagerMoFdn, activityJobId);
        activityUtils.unSubscribeToMoNotifications(brmBackupManagerMoFdn, activityJobId, jobActivityInfo);
    }
}
