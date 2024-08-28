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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.BrmBackupStatusInCompleteException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReader;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmDataDescriptorParser;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmHandler;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmVersionHandlersProviderFactory;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackup;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackupManager;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.inventory.backup.ecim.impl.EcimBrmDataParsersProviderFactory;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

@Stateless
public class BrmMoService {

    @Inject
    private OssModelInfoProvider ossModelInfoProvider;

    @Inject
    private BrmVersionHandlersProviderFactory handlersProviderFactory;

    @Inject
    private EcimBrmDataParsersProviderFactory parsersProviderFactory;

    @Inject
    private EcimOssBackupItemsReader smrsDataReader;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private ActivityUtils activityUtils;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private NodeAttributesReader nodeAttributesReader;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    private static final Logger LOGGER = LoggerFactory.getLogger(BrmMoService.class);

    public BrmBackup getBrmBackup(final String backupName, final String nodeName, final String domainName, final String backupType) throws MoNotFoundException, UnsupportedFragmentException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.getBrmBackup(networkElementData, backupName, nodeName, domainName, backupType);
    }

    public List<BrmBackup> getBrmBackups(final String nodeName) throws UnsupportedFragmentException, MoNotFoundException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.getBrmBackups(networkElementData);
    }

    // Get All Backups including System created backups.
    public List<BrmBackup> getAllBrmBackups(final String nodeName) throws BackupNotFoundException {
        try {
            final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
            final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
            final NetworkElement networkElement = activityUtils.getNetworkElement(nodeName, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(networkElement.getNeType(), networkElement.getOssModelIdentity(), FragmentType.ECIM_BRM_TYPE.getFragmentName());
            return brmHandler.getAllBrmBackups(ossModelInfo, networkElement);
        } catch (MoNotFoundException | UnsupportedFragmentException exception) {
            throw new BackupNotFoundException(exception);
        }
    }

    public String getBrmBackupManagerMoFdn(final NetworkElementData networkElementData, final String nodeName, final EcimBackupInfo ecimBackupInfo)
            throws MoNotFoundException, UnsupportedFragmentException {
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.getBrmBackupManagerMoFdn(networkElementData, nodeName, ecimBackupInfo);
    }

    /**
     * This method returns the list of backups (of the format backupName|domain|type) that exist on the node.
     * 
     * @param backupNameList
     * @param nodeName
     * @param domainName
     * @param backupType
     * @return List<String>
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    public List<String> getBackupDetails(final List<String> backupNameList, final String nodeName, final String domainName, final String backupType) throws MoNotFoundException,
            UnsupportedFragmentException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.getBackupDetails(networkElementData, backupNameList, nodeName, domainName, backupType);
    }

    public int executeMoAction(final String nodeName, final EcimBackupInfo ecimBackupInfo, final String moFdn, final String actionType) throws UnsupportedFragmentException, MoNotFoundException,
            ArgumentBuilderException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.executeMoAction(networkElementData, nodeName, ecimBackupInfo, moFdn, actionType);
    }

    public int executeCancelAction(final String nodeName, final String moFdn, final String actionName, final Map<String, Object> actionArguments) throws UnsupportedFragmentException,
            MoNotFoundException, ArgumentBuilderException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.executeCancelAction(moFdn, actionName, actionArguments);
    }

    public AsyncActionProgress getValidAsyncActionProgress(final String nodeName, final String activityName, final Map<String, AttributeChangeData> modifiedAttributes)
            throws UnsupportedFragmentException, MoNotFoundException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.getValidAsyncActionProgress(activityName, modifiedAttributes);
    }

    public String getNotifiableMoFdn(final String activityName, final String nodeName, final EcimBackupInfo ecimBackupInfo) throws MoNotFoundException, UnsupportedFragmentException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.getNotifiableMoFdn(networkElementData, activityName, nodeName, ecimBackupInfo);
    }

    public boolean isBackupExist(final String nodeName, final EcimBackupInfo ecimBackupInfo) throws MoNotFoundException, UnsupportedFragmentException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.isBackupCreationCompleted(networkElementData, nodeName, ecimBackupInfo);
    }

    public boolean isBackupExistsWithStatusComplete(final String nodeName, final EcimBackupInfo ecimBackupInfo) throws MoNotFoundException, BrmBackupStatusInCompleteException,
            UnsupportedFragmentException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.isBackupCreationCompletedWithStatusComplete(networkElementData, nodeName, ecimBackupInfo);
    }

    /**
     * Method to check whether backup deletion on the node is complete.
     * 
     * @param nodeName
     * @param ecimBackupInfo
     * @return boolean
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    public boolean isBackupDeletionCompleted(final String nodeName, final EcimBackupInfo ecimBackupInfo) throws MoNotFoundException, UnsupportedFragmentException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.isBackupDeletionCompleted(networkElementData, nodeName, ecimBackupInfo);
    }

    /**
     * Method to check whether backup deletion on the node is complete.
     * 
     * @param backupname
     * @param brmBackupManagerMoFdn
     * @param nodeName
     * @return boolean
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    public boolean isBackupDeletionCompleted(final String backupname, final String brmBackupManagerMoFdn, final String nodeName) throws MoNotFoundException, UnsupportedFragmentException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.isBackupDeletionCompleted(backupname, brmBackupManagerMoFdn);
    }

    public AsyncActionProgress getAsyncActionProgressFromBrmBackupForSpecificActivity(final String nodeName, final String activityName, final EcimBackupInfo ecimBackupInfo)
            throws MoNotFoundException, UnsupportedFragmentException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.getAsyncActionProgressFromBrmBackup(networkElementData, nodeName, activityName, ecimBackupInfo);
    }

    /**
     * @param networkElement
     * @return
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     */
    private BrmHandler getBrmHandler(final String nodeName, final String neType, final String ossModelIdentity) throws UnsupportedFragmentException {
        LOGGER.info("BrmMoService getBrmHandler : {}", nodeName);

        final String referenceMIMVersion = getFragmentVersion(neType, ossModelIdentity);
        LOGGER.info("referenceMIMVersion : {}", referenceMIMVersion);
        final BrmHandler brmHandler = handlersProviderFactory.getBrmHandler(referenceMIMVersion);
        LOGGER.info("brmHandler : {}", brmHandler);
        return brmHandler;

    }

    private String getFragmentVersion(final String neType, final String ossModelIdentity) throws UnsupportedFragmentException {
        final List<OssModelInfo> ossModelInfoList = ossModelInfoProvider.getOssModelInfo(neType, ossModelIdentity);
        String inputVersion = "";
        if (ossModelInfoList != null && !(ossModelInfoList.isEmpty())) {
            for (final OssModelInfo ossModelInfo : ossModelInfoList) {
                if (FragmentType.ECIM_BRM_TYPE.getFragmentName().equals(ossModelInfo.getReferenceMIMNameSpace())) {
                    inputVersion = ossModelInfo.getReferenceMIMVersion();
                    break;
                }
            }
        }
        return inputVersion;
    }

    /**
     * Method to check whether confirmation of restore backup activity is required or not.
     * 
     * @param nodeName
     * @return boolean
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    public boolean isConfirmRequired(final String nodeName) throws MoNotFoundException, UnsupportedFragmentException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.isConfirmRequired(networkElementData, nodeName);
    }

    /**
     * Method to verify BrmBackupLabelStore MO details during the confirmation activity on the Restore job.
     * 
     * @param nodeName
     * @param ecimBackupInfo
     * @return boolean
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    public boolean isSpecifiedBackupRestored(final String nodeName, final EcimBackupInfo ecimBackupInfo) throws MoNotFoundException, UnsupportedFragmentException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.isSpecifiedBackupRestored(networkElementData, nodeName, ecimBackupInfo);
    }

    /**
     * Method to get BrmBackupManagerProgressReport
     * 
     * @param nodeName
     * @param ecimBackupInfo
     * @return boolean
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    public AsyncActionProgress getProgressFromBrmBackupManagerMO(final String nodeName, final EcimBackupInfo ecimBackupInfo) throws MoNotFoundException, UnsupportedFragmentException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        final BrmBackupManager brmBackupManager = brmHandler.getBrmBackupManager(networkElementData, nodeName, ecimBackupInfo.getDomainName(), ecimBackupInfo.getBackupType());
        return brmBackupManager.getProgressReport();

    }

    /**
     * @param backupFileName
     * @param nodeName
     * @param neJobId
     * @return
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public EcimBackupInfo extractAndAddBackupNameInNeProperties(final String backupFileName, final String nodeName, final long neJobId, final NetworkElementData networkElement)
            throws UnsupportedFragmentException {
        LOGGER.debug("backupFileName : {}", backupFileName);
        final Map<String, String> parsedBackupData = new HashMap<String, String>();
        final String referenceMIMVersion = getFragmentVersion(networkElement.getNeType(), networkElement.getOssModelIdentity());
        LOGGER.debug("referenceMIMVersion : {}", referenceMIMVersion);
        EcimBackupInfo ecimBackupInfo = null;
        final BrmDataDescriptorParser ecimOSSDataXmlParser = parsersProviderFactory.getBrmParserHandler(referenceMIMVersion);
        final Map<String, Object> backupDetail = smrsDataReader.getBackupItems(networkElement, ecimOSSDataXmlParser, backupFileName, nodeName);
        if (backupDetail != null && !backupDetail.isEmpty()) {
            final String backupName = (String) backupDetail.get(EcimBackupConstants.BACKUP_NAME);
            final String domain = (String) backupDetail.get(EcimBackupConstants.BRM_BKP_MNGR_BACKUP_DOMAIN);
            final String type = (String) backupDetail.get(EcimBackupConstants.BRM_BKP_MNGR_BACKUP_TYPE);
            parsedBackupData.put(EcimBackupConstants.BACKUP_NAME, backupName);
            parsedBackupData.put(EcimBackupConstants.BACKUP_DOMAIN, domain);
            parsedBackupData.put(EcimBackupConstants.BACKUP_TYPE, type);
            parsedBackupData.put(EcimBackupConstants.BACKUP_FILE_NAME, backupFileName);
            setBackupNameInNeProperties(parsedBackupData, neJobId);
            ecimBackupInfo = new EcimBackupInfo(domain, backupName, type);
        } else {
            ecimBackupInfo = new EcimBackupInfo("", "", "");
        }
        ecimBackupInfo.setBackupFileName(backupFileName);
        return ecimBackupInfo;
    }

    /**
     * @param parsedBackupData
     * @param neJobId
     */
    private void setBackupNameInNeProperties(final Map<String, String> parsedBackupData, final long neJobId) {
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        for (final Entry<String, String> backupData : parsedBackupData.entrySet()) {
            activityUtils.addJobProperty(backupData.getKey(), backupData.getValue(), propertyList);
        }
        jobUpdateService.updateRunningJobAttributes(neJobId, propertyList, null);

    }

    public String getBrmFragmentVersion(final String nodeName) throws UnsupportedFragmentException, MoNotFoundException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.getBrmFragmentVersion(networkElementData, nodeName);

    }

    public ManagedObject getBrmFailsafeBackupMo(final String nodeName) throws MoNotFoundException, UnsupportedFragmentException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.getBrmFailsafeBackupMO(networkElementData, nodeName);
    }

    public int performBrmFailSafeActivate(final String nodeName, final String brmFailsafeBackupMoFdn) throws UnsupportedFragmentException, MoNotFoundException {
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.activate(brmFailsafeBackupMoFdn, actionArguments);
    }

    public int performBrmFailSafeDeActivate(final String nodeName, final String brmFailsafeBackupMoFdn) throws UnsupportedFragmentException, MoNotFoundException {
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.deactivate(brmFailsafeBackupMoFdn, actionArguments);
    }

    /**
     * This method validates action progress report based on activity name .
     * 
     * @param backupsToBeDeleted
     * @return backupDataStringBuilder
     * @throws MoNotFoundException
     * @throws UnsupportedFragmentException
     */
    public boolean validateActionProgressReport(final String nodeName, final AsyncActionProgress progressReport, final String activityName) throws UnsupportedFragmentException, MoNotFoundException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.validateActionProgressReport(progressReport, activityName);
    }

    public AsyncActionProgress getActionProgressOfBrmFailsafeMO(final String nodeName) throws UnsupportedFragmentException, MoNotFoundException {
        final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        final AsyncActionProgress actionProgress = brmHandler.getAsyncActionProgressOfBrmfailsafeMO(networkElementData, nodeName);
        return actionProgress;
    }

    /**
     * Method to retrieve backup name from BrmBackupMO FDN.
     * 
     * @param brmBackupFdn
     * @return backupName
     */
    public String getBackupNameFromBrmBackupMOFdn(final String brmBackupFdn) {
        String backupName = "";
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final ManagedObject brmBackupMO = liveBucket.findMoByFdn(brmBackupFdn);
        if (brmBackupMO != null) {
            final String[] attributeNames = { EcimBackupConstants.BACKUP_NAME };
            final Map<String, Object> backupMoAttributes = nodeAttributesReader.readAttributes(brmBackupMO, attributeNames);
            backupName = (String) backupMoAttributes.get(EcimBackupConstants.BACKUP_NAME);
        }
        LOGGER.debug("getBackupNameFromBrmBackupMOFdn with fdn {} has backupName : {}", brmBackupFdn, backupName);
        return backupName;
    }

    /**
     * Method to retrieve action arguments for UploadBackup Action.
     * 
     * @param networkElement
     * @param nodeName
     * @return actionArguments
     */
    public Map<String, Object> prepareActionArgumentsForUploadBackup(final NetworkElementData networkElementData, final String nodeName) throws MoNotFoundException, ArgumentBuilderException,
            UnsupportedFragmentException {
        final BrmHandler brmHandler = getBrmHandler(nodeName, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
        return brmHandler.prepareActionArgumentsForUploadBackup(networkElementData, nodeName);
    }

}
