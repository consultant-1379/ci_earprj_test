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
package com.ericsson.oss.services.shm.es.impl.ecim.deleteup

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy
import com.ericsson.oss.services.shm.common.InventoryQueryConfigurationProvider
import com.ericsson.oss.services.shm.common.NodeModelNameSpaceProvider
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReader
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy
import com.ericsson.oss.services.shm.inventory.backup.api.BackupInventoryConstants
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackup
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackupManager
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SwMHandler
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SwMVersionHandlersProviderFactory
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator

class EcimAbstractDeleteUpTest extends CdiSpecification {

    @MockedImplementation
    protected RetryPolicy retryPolicy;

    @MockedImplementation
    protected DpsRetryPolicies dpsRetryPolicies;

    @MockedImplementation
    protected NeJobStaticDataProvider neJobStaticDataProvider;

    @MockedImplementation
    protected NodeModelNameSpaceProvider nodeModelNameSpaceProvider;

    @MockedImplementation
    protected InventoryQueryConfigurationProvider inventoryQueryConfigurationProvider;

    @MockedImplementation
    protected OssModelInfoProvider ossModelInfoProvider;

    @MockedImplementation
    protected OssModelInfo ossModelInfo;

    @MockedImplementation
    protected PlatformTypeProviderImpl platformTypeProvider;

    @MockedImplementation
    protected BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @MockedImplementation
    protected SwMHandler swMHandler;

    @MockedImplementation
    protected SwMVersionHandlersProviderFactory swMprovidersFactory;

    @MockedImplementation
    private BrmBackupManager brmBackupManager

    @MockedImplementation
    protected NodeAttributesReader nodeAttributesReader;

    @MockedImplementation
    protected ActivityJobTBACValidator activityJobTBACValidator;

    def Map<String, Object> jobConfigurationDetails=new HashMap<String,Object>();
    def Map<String, Object> mainJobAttributesDetails=new HashMap<String,Object>();

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps);
    protected EcimUPConfigurationProvider ecimUPConfigurationProvider=new EcimUPConfigurationProvider(runtimeDps);

    def Map<String, Object> jobConfiguration=new HashMap<String,Object>();
    def Map<String, Object> activityJobAttributes=new HashMap<String,Object>();
    def Map<String, Object> mainJobAttributes=new HashMap<String,Object>();

    def activityJobId;
    def neJobId;
    def mainJobId;
    def nodeName ="LTE01dg2ERBS00002";
    def String inputProductNumber = "CXP";
    def String inputProductRevision = "RS101";
    def String deleteReferredBackups = "false";
    def String deleteReferredBackupsTrue = "true";
    def NEJobStaticData neJobStaticData;
    def JobActivityInfo jobActivityInfo;

    public static final String UP_MO_ADMINISTRATIVE_DATA = "administrativeData";
    protected buildNodeAndUpgradepackageData(final List<Map<String,String>> productDataList,final String deleteReferredBackups ){

        ecimUPConfigurationProvider.addSwMParentMO(nodeName);
        addUpgradePkgAndAdminDataOnNode(nodeName, productDataList);
        platformTypeProvider.getPlatformTypeBasedOnCapability(_ as String, _ as String) >> PlatformTypeEnum.ECIM;

        final Map<String, Object> neJobAttributesMap=new HashMap<String, Object>();
        neJobAttributesMap.putAt(ShmConstants.NE_NAME,nodeName);

        inventoryQueryConfigurationProvider.getNeFdnBatchSize() >> 3;
        ossModelInfoProvider.getOssModelInfo(_ as String,_ as String, _ as String) >> ossModelInfo;
        ossModelInfo.getReferenceMIMVersion() >> "2.0.0";
        ossModelInfo.getNamespace() >> "RcsSwM";

        List<BrmBackup> brmBkps = new ArrayList<BrmBackup>();

        if(deleteReferredBackups.equals("true")){
            brmMoServiceRetryProxy.getAllBrmBackups(nodeName) >> brmBkps;
        }else{
            List<Map<String, Object>> eachVersion = new ArrayList<Map<String, Object>>();
            Map<String, Object> productData1 = new HashMap<String, Object>();
            productData1.put(EcimCommonConstants.ProductData.PRODUCT_NUMBER,"CXP");
            productData1.put(EcimCommonConstants.ProductData.PRODUCT_REVISION,"RS103");
            productData1.put(EcimCommonConstants.ProductData.PRODUCT_NAME,"CXP");
            eachVersion.add(productData1);

            Map<String, Object> brmBackupMoAttributes = new HashMap<String, Object>();
            brmBackupMoAttributes.put(BackupInventoryConstants.PRODUCT_DATA, eachVersion);
            brmBackupMoAttributes.put(BackupInventoryConstants.BACKUP_NAME, "Bkp1");

            BrmBackup brmBkp =new BrmBackup("brmmofdn",brmBackupMoAttributes,brmBackupManager);
            brmBkps.add(brmBkp);
            brmMoServiceRetryProxy.getAllBrmBackups(nodeName) >> brmBkps;
        }

        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY) >> neJobStaticData;
        swMprovidersFactory.getSoftwareManagementHandler(_ as String) >> swMHandler
    }


    protected buildNodeAndUpgradepackageDataForRefferedBackup1(final List<Map<String,String>> productDataList,final String deleteReferredBackups ){

        ecimUPConfigurationProvider.addSwMParentMO(nodeName);
        addUpgradePkgAndAdminDataOnNode(nodeName, productDataList);
        platformTypeProvider.getPlatformTypeBasedOnCapability(_ as String, _ as String) >> PlatformTypeEnum.ECIM;

        final Map<String, Object> neJobAttributesMap=new HashMap<String, Object>();
        neJobAttributesMap.putAt(ShmConstants.NE_NAME,nodeName);

        inventoryQueryConfigurationProvider.getNeFdnBatchSize() >> 3;
        ossModelInfoProvider.getOssModelInfo(_ as String,_ as String, _ as String) >> ossModelInfo;
        ossModelInfo.getReferenceMIMVersion() >> "2.0.0";
        ossModelInfo.getNamespace() >> "RcsSwM";

        List<BrmBackup> brmBkps = new ArrayList<BrmBackup>();

        List<Map<String, Object>> eachVersion = new ArrayList<Map<String, Object>>();
        Map<String, Object> productData1 = new HashMap<String, Object>();
        productData1.put(EcimCommonConstants.ProductData.PRODUCT_NUMBER,"CXP");


        productData1.put(EcimCommonConstants.ProductData.PRODUCT_REVISION,"RS101");
        productData1.put(EcimCommonConstants.ProductData.PRODUCT_NAME,"CXP");
        eachVersion.add(productData1);

        Map<String, Object> brmBackupMoAttributes = new HashMap<String, Object>();
        brmBackupMoAttributes.put(BackupInventoryConstants.PRODUCT_DATA, eachVersion);
        brmBackupMoAttributes.put(BackupInventoryConstants.BACKUP_NAME, "Bkp1");
        brmBackupMoAttributes.put(BackupInventoryConstants.CREATION_TYPE, "SYSTEM_CREATED");

        BrmBackup brmBkp =new BrmBackup("brmmofdn",brmBackupMoAttributes,brmBackupManager);
        brmBkps.add(brmBkp);
        brmMoServiceRetryProxy.getAllBrmBackups(nodeName) >> brmBkps;

        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY) >> neJobStaticData;
        swMprovidersFactory.getSoftwareManagementHandler(_ as String) >> swMHandler
    }

    protected buildNodeAndTwoUpgradepackageDataForTwoRefferedBackups(final List<Map<String,String>> productDataList,final String deleteReferredBackups, final boolean isSyscrBackup) {

        ecimUPConfigurationProvider.addSwMParentMO(nodeName);
        addUpgradePkgAndAdminDataOnNode(nodeName, productDataList);
        platformTypeProvider.getPlatformTypeBasedOnCapability(_ as String, _ as String) >> PlatformTypeEnum.ECIM;

        final Map<String, Object> neJobAttributesMap = new HashMap<String, Object>();
        neJobAttributesMap.putAt(ShmConstants.NE_NAME, nodeName);

        inventoryQueryConfigurationProvider.getNeFdnBatchSize() >> 3;
        ossModelInfoProvider.getOssModelInfo(_ as String, _ as String, _ as String) >> ossModelInfo;
        ossModelInfo.getReferenceMIMVersion() >> "2.0.0";
        ossModelInfo.getNamespace() >> "RcsSwM";

        List<BrmBackup> brmBkps = new ArrayList<BrmBackup>();

        List<Map<String, Object>> eachVersion = new ArrayList<Map<String, Object>>();
        Map<String, Object> productData1 = new HashMap<String, Object>();
        productData1.put(EcimCommonConstants.ProductData.PRODUCT_NUMBER, "CXP");
        productData1.put(EcimCommonConstants.ProductData.PRODUCT_REVISION, "RS101");
        productData1.put(EcimCommonConstants.ProductData.PRODUCT_NAME, "CXP");
        eachVersion.add(productData1);

        Map<String, Object> brmBackupMoAttributes = new HashMap<String, Object>();
        brmBackupMoAttributes.put(BackupInventoryConstants.PRODUCT_DATA, eachVersion);
        brmBackupMoAttributes.put(BackupInventoryConstants.BACKUP_NAME, "Bkp1");
        if (isSyscrBackup){
            brmBackupMoAttributes.put(BackupInventoryConstants.CREATION_TYPE, "SYSTEM_CREATED");
        }

        BrmBackup brmBkp = new BrmBackup("brmmofdn", brmBackupMoAttributes, brmBackupManager);
        brmBkps.add(brmBkp);
        List<Map<String, Object>> eachVersion2 = new ArrayList<Map<String, Object>>();
        Map<String, Object> productData2 = new HashMap<String, Object>();
        productData2.put(EcimCommonConstants.ProductData.PRODUCT_NUMBER, "CXP");
        productData2.put(EcimCommonConstants.ProductData.PRODUCT_REVISION, "RS102");
        productData2.put(EcimCommonConstants.ProductData.PRODUCT_NAME, "CXP");
        eachVersion.add(productData2);

        Map<String, Object> brmBackupMoAttributes2 = new HashMap<String, Object>();
        brmBackupMoAttributes2.put(BackupInventoryConstants.PRODUCT_DATA, eachVersion);
        brmBackupMoAttributes2.put(BackupInventoryConstants.BACKUP_NAME, "Bkp2");
        if (isSyscrBackup){
            brmBackupMoAttributes2.put(BackupInventoryConstants.CREATION_TYPE, "SYSTEM_CREATED");
        }
        BrmBackup brmBkp2 = new BrmBackup("brmmofdn2", brmBackupMoAttributes2, brmBackupManager);
        brmBkps.add(brmBkp2);
        brmMoServiceRetryProxy.getAllBrmBackups(nodeName) >> brmBkps;

        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY) >> neJobStaticData;
        swMprovidersFactory.getSoftwareManagementHandler(_ as String) >> swMHandler
    }

    protected addUpgradePkgAndAdminDataOnNode(String nodeName, List<Map<String,String>> productDataList){

        List<String> upgradePackages = new ArrayList<String>();
        List<Map<String, Object>> moAttributesList = new ArrayList<Map<String, Object>>();
        Map<String, Object> moAttributes=new HashMap<String, Object>();

        for(Map<String,String> productData :productDataList){
            upgradePackages.add(ecimUPConfigurationProvider.addUpgradePackageOnNode(productData.get(EcimCommonConstants.ProductData.PRODUCT_NUMBER), productData.get(EcimCommonConstants.ProductData.PRODUCT_REVISION),nodeName));
        }

        for(String upgradePackage : upgradePackages){
            Map<String, Object> moAttribute=new HashMap<String, Object>();
            Map<String, Object> adminData =runtimeDps.stubbedDps.liveBucket.findMoByFdn(upgradePackage).getAttribute("administrativeData")

            moAttribute.put(EcimCommonConstants.ProductData.PRODUCT_NUMBER,adminData.get(EcimCommonConstants.ProductData.PRODUCT_NUMBER));
            moAttribute.put(EcimCommonConstants.ProductData.PRODUCT_REVISION,adminData.get(EcimCommonConstants.ProductData.PRODUCT_REVISION));
            moAttributesList.add(moAttribute);
        }

        moAttributes.putAt("administrativeData", moAttributesList);
        nodeAttributesReader.readAttributes(_ as ManagedObject, _ as String[]) >> moAttributes;
    }

    def  loadJobProperties(final String productNumber, final String productRevision,final String deleteReferredBackups, final String nonActiveUpsToDelete) {
        def List<Map<String, Object>> neSpecificPropertyList = new ArrayList<Map<String, Object>>();
        def Map<String, Object> neJobPropertyUserInput = new HashMap<String, Object>();
        def Map<String, Object> neJobPropertyIsDeleteReferred = new HashMap<String, Object>();
        def Map<String, Object> nonActiveUpsToDeleteMap = new HashMap<String, Object>();

        if(productNumber != null && productRevision!= null){
            neJobPropertyUserInput.put(ShmJobConstants.KEY, UpgradeActivityConstants.DELETE_UP_LIST);
            if(Boolean.parseBoolean(nonActiveUpsToDelete)){
                neJobPropertyUserInput.put(ShmJobConstants.VALUE, "");
            }else{
                neJobPropertyUserInput.put(ShmJobConstants.VALUE, productNumber+"**|**"+productRevision);
            }
            neSpecificPropertyList.add(neJobPropertyUserInput);
        }
        if(deleteReferredBackups != null){
            neJobPropertyIsDeleteReferred.put(ShmJobConstants.KEY, JobPropertyConstants.DELETE_REFERRED_BACKUPS);
            neJobPropertyIsDeleteReferred.put(ShmJobConstants.VALUE, deleteReferredBackups);
            neSpecificPropertyList.add(neJobPropertyIsDeleteReferred);
        }

        if(nonActiveUpsToDelete != null){
            nonActiveUpsToDeleteMap.put(ShmJobConstants.KEY, JobPropertyConstants.DELETE_NON_ACTIVE_UPS);
            nonActiveUpsToDeleteMap.put(ShmJobConstants.VALUE, nonActiveUpsToDelete);
            neSpecificPropertyList.add(nonActiveUpsToDeleteMap);
        }

        jobConfiguration.putAt(ShmJobConstants.JOBPROPERTIES, neSpecificPropertyList);
        mainJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE,new java.lang.Double(0.0));
        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        Map<String, Object> attributeMap = new HashMap<>();

        attributeMap.put(ShmConstants.NE_NAME,, nodeName);
        attributeMap.put("progressPercentage",Double.valueOf(0.1));
        PersistenceObject mainJob = runtimeDps.addPersistenceObject().namespace("shm").type("MainJob").addAttributes(mainJobAttributes).build();
        mainJobId = mainJob.getPoId();
        attributeMap.put(ShmConstants.MAIN_JOB_ID,mainJobId);
        PersistenceObject neJob= runtimeDps.addPersistenceObject().namespace("shm").type("NeJob").addAttributes(attributeMap).build();
        neJobId = neJob.getPoId();
        attributeMap.put(ShmConstants.NE_JOB_ID, neJobId);
        PersistenceObject activityJob = runtimeDps.addPersistenceObject().namespace("shm").type("ActivityJob").addAttributes(attributeMap).build();
        activityJobId = activityJob.getPoId();
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "1234", "ECIM", 5L, null);
        jobActivityInfo = new JobActivityInfo(activityJobId, ShmConstants.DELETEUPGRADEPKG_ACTIVITY, JobTypeEnum.DELETE_UPGRADEPACKAGE, PlatformTypeEnum.ECIM)
    }

    def List<Map<String,String>> prepareInputProductData(final String inputProductNumber, final String inputProductRevision){
        List<Map<String,String>> productDataList = new ArrayList<Map<String,String>>();
        Map<String,String> productData = new HashMap<String,String>()
        productData.put(EcimCommonConstants.ProductData.PRODUCT_NUMBER,inputProductNumber);
        productData.put(EcimCommonConstants.ProductData.PRODUCT_REVISION,inputProductRevision);
        productDataList.add(productData);

        return productDataList;
    }
    def List<Map<String, Object>> prepareNeJobProperties(final String inputProductNumber, final String inputProductRevision){
        final List<Map<String, Object>> neJobPropertiesList = new ArrayList<>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, JobPropertyConstants.DELETE_UP_LIST);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, inputProductNumber+UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER+inputProductRevision);
        neJobPropertiesList.add(jobProperty);
        return neJobPropertiesList;
    }

    def String readProperty(final long neJobId){
        PersistenceObject neJob = runtimeDps.stubbedDps.liveBucket.findPoById(neJobId)
        Map<String, Object> attributes = neJob.getAllAttributes()
        List<Map<String, String>> jobProperties  = attributes.get(ShmConstants.JOBPROPERTIES)
        String propetyValue = getJobProperty(JobPropertyConstants.DELETE_UP_LIST, jobProperties)
        return propetyValue;
    }

    def String getJobProperty(String propertyName, List<Map<String, String>> jobProperties){

        for(final Map<String, String> jobProperty:jobProperties){
            if(propertyName.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))){
                return  jobProperty.get(ActivityConstants.JOB_PROP_VALUE)
            }
        }
        return null;
    }

    def List<Map<String,String>> prepareInputProductDataWith2Pkgs(){
        List<Map<String,String>> productDataList = new ArrayList<Map<String,String>>();
        Map<String,String> productData1 = new HashMap<String,String>()
        productData1.put(EcimCommonConstants.ProductData.PRODUCT_NUMBER,inputProductNumber);
        productData1.put(EcimCommonConstants.ProductData.PRODUCT_REVISION,inputProductRevision);

        Map<String,String> productData2 = new HashMap<String,String>()
        productData2.put(EcimCommonConstants.ProductData.PRODUCT_NUMBER,"CXP");
        productData2.put(EcimCommonConstants.ProductData.PRODUCT_REVISION,"RS102");
        productDataList.add(productData1);
        productDataList.add(productData2);

        return productDataList;
    }

    def loadJobPropertiesFor2PkgsForDeleteAllInactiveUps(final String productData, final String deleteReferredBackups, final String nonActiveUpsToDelete) {
        def List<Map<String, Object>> neSpecificPropertyList = new ArrayList<Map<String, Object>>();
        def Map<String, Object> neJobPropertyUserInput = new HashMap<String, Object>();
        def Map<String, Object> neJobPropertyIsDeleteReferred = new HashMap<String, Object>();
        def Map<String, Object> nonActiveUpsToDeleteMap = new HashMap<String, Object>();

        neJobPropertyUserInput.put(ShmJobConstants.KEY, UpgradeActivityConstants.DELETE_UP_LIST);
        neJobPropertyUserInput.put(ShmJobConstants.VALUE, productData);

        if(nonActiveUpsToDelete != null){
            nonActiveUpsToDeleteMap.put(ShmJobConstants.KEY, JobPropertyConstants.DELETE_NON_ACTIVE_UPS);
            nonActiveUpsToDeleteMap.put(ShmJobConstants.VALUE, nonActiveUpsToDelete);
            neSpecificPropertyList.add(nonActiveUpsToDeleteMap);
        }
        neJobPropertyIsDeleteReferred.put(ShmJobConstants.KEY, JobPropertyConstants.DELETE_REFERRED_BACKUPS);
        neJobPropertyIsDeleteReferred.put(ShmJobConstants.VALUE, deleteReferredBackups);

        neSpecificPropertyList.add(neJobPropertyUserInput);
        neSpecificPropertyList.add(neJobPropertyIsDeleteReferred);

        jobConfigurationDetails.putAt(ShmJobConstants.JOBPROPERTIES, neSpecificPropertyList);
        mainJobAttributesDetails.put(ShmConstants.PROGRESSPERCENTAGE,new java.lang.Double(0.0));
        mainJobAttributesDetails.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);

        Map<String, Object> attributeMap = new HashMap<>();


        attributeMap.put(ShmConstants.NE_NAME, nodeName);
        attributeMap.put("progressPercentage",Double.valueOf(0.1));
        PersistenceObject mainJob  = runtimeDps.addPersistenceObject().namespace("shm").type("MainJob").addAttributes(mainJobAttributesDetails).build();
        mainJobId = mainJob.getPoId();
        attributeMap.put(ShmConstants.MAIN_JOB_ID,mainJobId);
        PersistenceObject neJob  = runtimeDps.addPersistenceObject().namespace("shm").type("NeJob").addAttributes(attributeMap).build();
        neJobId = neJob.getPoId();
        attributeMap.put(ShmConstants.NE_JOB_ID, neJobId);
        PersistenceObject activityJob = runtimeDps.addPersistenceObject().namespace("shm").type("ActivityJob").addAttributes(attributeMap).build();
        activityJobId = activityJob.getPoId();
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "1234", "ECIM", 5L, null);
        jobActivityInfo = new JobActivityInfo(activityJobId, ShmConstants.DELETEUPGRADEPKG_ACTIVITY, JobTypeEnum.DELETE_UPGRADEPACKAGE, PlatformTypeEnum.ECIM)
    }
}
