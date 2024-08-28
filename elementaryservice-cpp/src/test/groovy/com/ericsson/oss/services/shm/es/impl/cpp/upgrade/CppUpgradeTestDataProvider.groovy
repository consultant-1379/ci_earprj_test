/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.es.impl.cpp.upgrade
import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.services.shm.common.InventoryQueryConfigurationProvider
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl
import com.ericsson.oss.services.shm.common.modelservice.api.*
import com.ericsson.oss.services.shm.common.retry.DpsWriterRetryProxy
import com.ericsson.oss.services.shm.es.api.*
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.filestore.swpackage.remote.api.RemoteSoftwarePackageService
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.notifications.api.*
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier

public class CppUpgradeTestDataProvider extends CdiSpecification {

    @MockedImplementation
    protected RetryPolicy retryPolicy;

    @MockedImplementation
    protected ActionRetryPolicy moActionRetryPolicy

    @MockedImplementation
    protected OssModelInfoProvider ossModelInfoProvider

    @MockedImplementation
    protected OssModelInfo ossModelInfo

    @MockedImplementation
    protected PlatformTypeProviderImpl platformTypeProvider

    @MockedImplementation
    protected InventoryQueryConfigurationProvider inventoryQueryConfigurationListener

    @MockedImplementation
    SystemRecorder systemRecorder

    @MockedImplementation
    protected ActivityJobTBACValidator activityJobTBACValidator

    @MockedImplementation
    protected NeJobStaticDataProvider neJobStaticDataProvider

    @MockedImplementation
    protected RemoteSoftwarePackageService remoteSoftwarePackageService

    @MockedImplementation
    protected Notification notification

    @MockedImplementation
    protected JobStaticDataProvider jobStaticDataProvider;

    @MockedImplementation
    protected UpgradePackageService upgradePackageService

    @MockedImplementation
    protected DpsWriterRetryProxy dpsWriterProxy

    @MockedImplementation
    NotificationRegistry notificationRegistry

    @Inject
    ActivityUtils activityUtils

    @Inject
    WorkflowInstanceNotifier workflowInstanceNotifier

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)

    def activityJobId = 3L;
    def SESSION_ID = "sessionId";
    def ACTION_TRIGGERED = "actionTriggered";
    def CANCEL_ACTION_ID="cancelActionId"
    def cancelActionId=2
    def neJobId = 2L;
    def mainJobId = 1L;
    def nodeName = "LTE17_ERBS00002";
    def businessKey = "Some Business Key"
    def templateJobId=1L;
    def productNumber="CXP102051_1"
    def productRevision= "R4D25"
    def EXECUTING="EXECUTING"
    def PRODUCT_NUMBER="productNumber"
    def PRODUCT_REVISION="productRevision";
    def PERSISTED_ACTION_ID="persistedActionId";

    def actionId="1"
    def persistedActionId ="1"

    def Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
    def Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
    def Map<String, Object> neJobAttributes = new HashMap<String, Object>();
    def NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, businessKey,String.valueOf(PlatformTypeEnum.CPP) , (new Date()).getTime(), "LTE17");
    def upMoFdn ="ManagedElement=LTE17_ERBS00002,SystemFunctions=1,SwM=1,UpgradePackage=CXP102051_1_R4D25,actionId=1"
    def JobActivityInfo jobActivityInfo = new JobActivityInfo(activityJobId, "install", JobTypeEnum.UPGRADE, PlatformTypeEnum.CPP)
    def upgradePackageFilePath = "/home/smrs/smrsroot/CXP102051_1_R4D25/redUCF.xml";
    def Map<String, Object> jobConfiguration=new HashMap<String,Object>();
    def softwarePackagename = "CXP102051_1_R4D25";
    def FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId,jobActivityInfo)
    def ActivityStepResult activityStepResult = new ActivityStepResult();

    def buildTestDataForInstallAction(String productNumber, String productRevision,String nodeName,DpsAttributeChangedEvent dpsAttributeChangedEvent) {

        addSwModuleParentMO(nodeName)
        String createdMOFdn=addUpgradePackageOnNode(productNumber, productRevision, nodeName)
        List<String> upgradefilePaths = new ArrayList<>();
        upgradefilePaths.add(upgradePackageFilePath)
        remoteSoftwarePackageService.getSoftwarePackageDetails(softwarePackagename)  >> upgradefilePaths
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData
        platformTypeProvider.getPlatformTypeBasedOnCapability(_ as String, _ as String) >> PlatformTypeEnum.CPP;
        notification.getNotificationSubject() >> fdnNotificationSubject
        notification.getNotificationEventType() >> NotificationEventTypeEnum.AVC
        inventoryQueryConfigurationListener.getNeFdnBatchSize() >> 3
        notification.getDpsDataChangedEvent() >> dpsAttributeChangedEvent
    }

    def long loadJobProperties(String nodeName,String activityName,String upMoFdn) {

        Map<String, Object> mainjobAttributes = new HashMap<>();
        Map<String, Object> mainjobAttributesSwp = new HashMap<>();
        Map<String, Object> mainjobAttributesFi = new HashMap<>();
        Map<String, Object> mainjobAttributesSi = new HashMap<>();
        Map<String, Object> mainJobAttributesMap = new HashMap<>()
        Map<String, Object> mainJobAttrUpgradeActionType = new HashMap<>()
        List<Map<String, Object>> mainJobAttributesList = new ArrayList<>()
        mainjobAttributesSwp.put(ShmJobConstants.KEY, UpgradeActivityConstants.SWP_NAME);
        mainjobAttributesSwp.put(ShmJobConstants.VALUE, softwarePackagename);
        mainJobAttributesList.add(mainjobAttributesSwp);
        mainjobAttributesFi.put(ShmJobConstants.KEY, UpgradeActivityConstants.FORCEINSTALL);
        mainjobAttributesFi.put(ShmJobConstants.VALUE, "full");
        mainJobAttributesList.add(mainjobAttributesFi);
        mainjobAttributesSi.put(ShmJobConstants.KEY, UpgradeActivityConstants.SELECTIVEINSTALL);
        mainjobAttributesSi.put(ShmJobConstants.VALUE, "notselective");
        mainJobAttributesList.add(mainjobAttributesSi);
        mainjobAttributes.put(ShmJobConstants.KEY, UpgradeActivityConstants.UCF);
        mainjobAttributes.put(ShmJobConstants.VALUE, "redUCF.xml");
        mainJobAttributesList.add(mainjobAttributes)
        mainJobAttrUpgradeActionType.put(ShmJobConstants.KEY, UpgradeActivityConstants.REBOOTNODEUPGRADE)
        mainJobAttrUpgradeActionType.put(ShmJobConstants.VALUE,"true")
        mainJobAttributesList.add(mainJobAttrUpgradeActionType)
        jobConfiguration.put(ShmJobConstants.JOBPROPERTIES, mainJobAttributesList);
        mainJobAttributesMap.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);

        mainJobAttributesMap.put(ShmConstants.JOBTEMPLATEID,templateJobId);
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("MainJob").addAttributes(mainJobAttributesMap).build();
        loadNEJobAttributes(nodeName,upMoFdn);
        return loadActivityJobProperties(activityName);
    }

    private long loadActivityJobProperties(String activityName) {

        Map<String, Object> activityjobAttributes = new HashMap<>();
        Map<String, Object> activityjobAttributeAid = new HashMap<>();
        Map<String, Object> activityjobAttributesPid = new HashMap<>();
        Map<String, Object> activityJobAttributesMap = new HashMap<>()
        List<Map<String, Object>> activityJobAttributesList = new ArrayList<>()

        activityjobAttributes.put(ShmJobConstants.KEY, ACTION_TRIGGERED);
        activityjobAttributes.put(ShmJobConstants.VALUE, activityName);

        activityjobAttributeAid.put(ShmJobConstants.KEY, ActivityConstants.ACTION_ID);
        activityjobAttributeAid.put(ShmJobConstants.VALUE, actionId);
        activityjobAttributesPid.put(ShmJobConstants.KEY, PERSISTED_ACTION_ID)
        activityjobAttributesPid.put(ShmJobConstants.VALUE,persistedActionId)

        activityJobAttributesList.add(activityjobAttributes);
        activityJobAttributesList.add(activityjobAttributeAid);
        activityJobAttributesList.add(activityjobAttributesPid);

        activityJobAttributesMap.put(ShmConstants.ACTIVITY_NAME, activityName)
        activityJobAttributesMap.put(ShmConstants.PLATEFORM_TYPE, "CPP")
        activityJobAttributesMap.put(ShmConstants.ACTIVITY_START_DATE, new Date())
        activityJobAttributesMap.put(ShmConstants.PROGRESSPERCENTAGE, 0d)
        activityJobAttributesMap.put(ShmConstants.ACTIVITY_NE_JOB_ID, neJobId)

        activityJobAttributesMap.put(ShmJobConstants.JOBPROPERTIES, activityJobAttributesList);

        PersistenceObject activityJob=runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("ActivityJob").addAttributes(activityJobAttributesMap).build()
        return activityJob.getPoId()
    }

    private loadNEJobAttributes(String nodeName,String upMoFdn) {

        Map<String, Object> neJobAttributesMap = new HashMap<>();
        Map<String, Object> neJobPropertyUserInput = new HashMap<>();
        Map<String, Object> neJobPropertyCancelInformation = new HashMap<>();
        List<Map<String,Object>> neSpecificPropertyList = new ArrayList<>();

        neJobPropertyUserInput.put(ShmJobConstants.KEY, UpgradeActivityConstants.UP_FDN);
        neJobPropertyUserInput.put(ShmJobConstants.VALUE, upMoFdn);
        neJobPropertyCancelInformation.put(ShmJobConstants.KEY, ShmConstants.CANCELLEDBY);
        neJobPropertyCancelInformation.put(ShmJobConstants.VALUE, "administrator");
        neSpecificPropertyList.add(neJobPropertyUserInput);
        neSpecificPropertyList.add(neJobPropertyCancelInformation);

        neJobAttributesMap.put(ShmJobConstants.JOBPROPERTIES, neSpecificPropertyList);

        neJobAttributesMap.put(ShmConstants.MAIN_JOB_ID,mainJobId);
        neJobAttributesMap.put(ShmConstants.NE_NAME,nodeName);
        neJobAttributesMap.put(ShmConstants.JOBTEMPLATEID,templateJobId);
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("NeJob").addAttributes(neJobAttributesMap).build()
    }

    def String extractJobProperty(String propertyName, List<Map<String, String>> jobProperties){

        for(Map<String, String> jobProperty:jobProperties){
            if(propertyName.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))){
                return  jobProperty.get(ActivityConstants.JOB_PROP_VALUE)
            }
        }
        return null;
    }

    def Map<String, Object> buildUpMoAttributeMap(ActivityStepResultEnum upActionResult,String upMOState,String actionResultInfo) {

        Map<String, Object> upMoAttributeMap=new HashMap<String, Object>()
        Map<String, Object> upActioneMap=new HashMap<String, Object>()
        Map<String, Object> actionIdMap=new HashMap<String, Object>()
        actionIdMap.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, actionId)
        List<Map<String, Object>> upActionList = new ArrayList<Map<String, Object>>()
        upActioneMap.put(UpgradePackageMoConstants.UP_ACTION_RESULT,upActionResult)
        actionIdMap.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO,actionResultInfo)

        upActionList.add(upActioneMap)
        upActionList.add(actionIdMap)
        upMoAttributeMap.put(UpgradePackageMoConstants.UP_ACTION_RESULT,upActionList)
        upMoAttributeMap.put(UpgradePackageMoConstants.UP_MO_STATE, upMOState)
        return upMoAttributeMap
    }

    def buildDpsAttributeChangedEvent(String actionName) {
        AttributeChangeData attributeChangeDataProgHdr;
        AttributeChangeData attributeChangeDataState;
        List<Map<String, Object>> upActionList = new ArrayList<Map<String, Object>>()
        Map<String, Object> actionaResultMap = new HashMap<String, Object>();
        actionaResultMap.put("notifiableAttributeValue", ActionResultInformation.EXECUTED.toString());
        actionaResultMap.put("previousNotifiableAttributeValue", EXECUTING);

        if("install".equals(actionName)) {
            attributeChangeDataProgHdr = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradePackageState.NOT_INSTALLED.toString(), UpgradePackageState.INSTALL_COMPLETED.toString(), new Object(), new Object())
            attributeChangeDataState = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.INSTALL_EXECUTING, UpgradePackageState.INSTALL_COMPLETED.toString(), new Object(), new Object())
        } else {
            attributeChangeDataProgHdr = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradePackageState.VERIFICATION_EXECUTING.toString(), UpgradePackageState.VERIFICATION_EXECUTING.toString(), new Object(), new Object())
            attributeChangeDataState = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.VERIFICATION_EXECUTING, UpgradePackageState.VERIFICATION_EXECUTING.toString(), new Object(), new Object())
            actionaResultMap.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, actionId);
            actionaResultMap.put("persistedActionId", persistedActionId);
            actionaResultMap.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TYPE_OF_INVOKED_ACTION,String.valueOf(UpgradePackageInvokedAction.VERIFY_UPGRADE))
        }
        upActionList.add(actionaResultMap);
        AttributeChangeData attributeChangeDataResult = new AttributeChangeData(UpgradePackageMoConstants.UP_ACTION_RESULT,upActionList, upActionList, new Object(), new Object())
        Set<AttributeChangeData> attributeSet=new HashSet<>();
        attributeSet.add(attributeChangeDataProgHdr)
        attributeSet.add(attributeChangeDataState)
        attributeSet.add(attributeChangeDataResult)
        DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent("RcswM", "Upgradepackage", "2.0.0",3L, upMoFdn,"liveBucket",attributeSet)
        return dpsAttributeChangedEvent
    }

    def buildDpsAttributeChangedEventForFail(String actionName) {

        AttributeChangeData attributeChangeDataProgHdr;
        AttributeChangeData attributeChangeDataState;
        List<Map<String, Object>> upActionList = new ArrayList<Map<String, Object>>()
        Map<String, Object> actionaResultMap = new HashMap<String, Object>();
        actionaResultMap.put("notifiableAttributeValue", ActionResultInformation.EXECUTION_FAILED.toString());
        actionaResultMap.put("previousNotifiableAttributeValue",EXECUTING );

        if("install".equals(actionName)) {
            attributeChangeDataProgHdr = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradePackageState.NOT_INSTALLED.toString(), UpgradePackageState.INSTALL_NOT_COMPLETED.toString(), new Object(), new Object())
            attributeChangeDataState = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.INSTALL_EXECUTING, UpgradePackageState.INSTALL_NOT_COMPLETED.toString(), new Object(), new Object())
        } else if("upgrade".equals(actionName)) {
            attributeChangeDataProgHdr = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.VERIFICATION_FINISHED.toString(), UpgradeProgressInformation.EXECUTION_FAILED.toString(), new Object(), new Object())
            attributeChangeDataState = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.INSTALL_COMPLETED.toString(), UpgradePackageState.INSTALL_COMPLETED.toString(), new Object(), new Object())
        } else if ("confirm".equals(actionName)) {
            attributeChangeDataProgHdr = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.WAIT_FOR_CONF_UPGRADE.toString(), UpgradeProgressInformation.EXECUTION_FAILED.toString(), new Object(), new Object())
            attributeChangeDataState = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.AWAITING_CONFIRMATION.toString(), UpgradePackageState.UPGRADE_COMPLETED.toString(), new Object(), new Object())
            actionaResultMap.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, actionId);
            actionaResultMap.put("persistedActionId", persistedActionId);
            actionaResultMap.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TYPE_OF_INVOKED_ACTION,String.valueOf(UpgradePackageInvokedAction.VERIFY_UPGRADE))
        } else {
            attributeChangeDataProgHdr = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradePackageState.VERIFICATION_EXECUTING.toString(), ActionResultInformation.EXECUTION_FAILED.toString(), new Object(), new Object())
            attributeChangeDataState = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.VERIFICATION_EXECUTING, ActionResultInformation.EXECUTION_FAILED.toString(), new Object(), new Object())
            actionaResultMap.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, actionId);
            actionaResultMap.put("persistedActionId", persistedActionId);
            actionaResultMap.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TYPE_OF_INVOKED_ACTION,String.valueOf(UpgradePackageInvokedAction.VERIFY_UPGRADE))
        }

        upActionList.add(actionaResultMap);
        AttributeChangeData attributeChangeDataResult = new AttributeChangeData(UpgradePackageMoConstants.UP_ACTION_RESULT,upActionList, upActionList, new Object(), new Object())
        Set<AttributeChangeData> attributeSet=new HashSet<>();
        attributeSet.add(attributeChangeDataProgHdr)
        attributeSet.add(attributeChangeDataState)
        attributeSet.add(attributeChangeDataResult)
        DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent("RcswM", "Upgradepackage", "2.0.0",3L, upMoFdn,"liveBucket",attributeSet)
        return dpsAttributeChangedEvent
    }

    def addSwModuleParentMO(final String nodeName){

        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName)
                .addAttribute('SubNetworkId', nodeName)
                .namespace('OSS_TOP')
                .version("3.0.0")
                .type("SubNetwork")
                .build()
        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName)
                .addAttribute("MeContextId",nodeName)
                .addAttribute('neType', 'ERBS')
                .namespace('OSS_TOP')
                .version("3.0.0")
                .type("MeContext")
                .build()
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName)
                .addAttribute("NetworkElementId",nodeName)
                .addAttribute('neType', 'ERBS')
                .addAttribute("ossModelIdentity","17A-R2YX")
                .addAttribute("nodeModelIdentity","17A-R2YX")
                .addAttribute('ossPrefix',"SubNetwork="+nodeName+",MeContext="+nodeName)
                .namespace('OSS_NE_DEF')
                .version("2.0.0")
                .type("NetworkElement")
                .build()
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName+",CmFunction=1")
                .type("NetworkElement")
                .build()
        runtimeDps.build().getLiveBucket().findMoByFdn("NetworkElement="+nodeName+",CmFunction=1").addAssociation("nodeRootRef", runtimeDps.build().getLiveBucket().findMoByFdn("SubNetwork="+nodeName))
        runtimeDps.build().getLiveBucket().findMoByFdn("NetworkElement="+nodeName).addAssociation("nodeRootRef", runtimeDps.build().getLiveBucket().findMoByFdn("SubNetwork="+nodeName))

        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName+",ManagedElement="+nodeName)
                .addAttribute('ManagedElementId',nodeName)
                .addAttribute('managedElementType ', 'ERBS')
                .addAttribute('neType', 'ERBS')
                .type("ManagedElement")
                .build()
        runtimeDps.addManagedObject().withFdn("ManagedElement="+nodeName+",SystemFunctions=1,SwM=1")
                .addAttribute('swMId',1)
                .addAttribute('neType', 'ERBS')
                .version("3.1.1")
                .type("SwM")
                .build()
    }

    def String addUpgradePackageOnNode(final String productNumber, final String productRevision, final String nodeName) {

        Map<String, Object> nodeAdminData=new HashMap<String, Object>();
        nodeAdminData.put(PRODUCT_NUMBER, productNumber);
        nodeAdminData.put(PRODUCT_REVISION, productRevision);

        ManagedObject MO =  runtimeDps.addManagedObject().withFdn("ManagedElement="+nodeName+",SystemFunctions=1,SwM=1,UpgradePackage="+productNumber+"_"+productRevision)
                .addAttribute("UpgradePackageId", productNumber+"_"+productRevision)
                .addAttribute("administrativeData", nodeAdminData)
                .addAttribute('neType', 'ERBS')
                .namespace('CPP_NodeModule')
                .type("UpgradePackage")
                .build()

        return MO.getFdn();
    }

    def buildTestDataForUpgradeAction(String productNumber, String productRevision,String nodeName, DpsAttributeChangedEvent dpsAttributeChangedEvent) {
        addSwModuleParentMO(nodeName)
        String createdMOFdn=addUpgradePackageOnNode(productNumber, productRevision, nodeName)
        List<String> upgradefilePaths = new ArrayList<>();
        upgradefilePaths.add(upgradePackageFilePath)
        remoteSoftwarePackageService.getSoftwarePackageDetails(softwarePackagename)  >> upgradefilePaths
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData
        platformTypeProvider.getPlatformTypeBasedOnCapability(_ as String, _ as String) >> PlatformTypeEnum.CPP;
        inventoryQueryConfigurationListener.getNeFdnBatchSize() >> 3
        notification.getNotificationSubject() >> fdnNotificationSubject
        notification.getNotificationEventType() >> NotificationEventTypeEnum.AVC
        notification.getDpsDataChangedEvent() >> dpsAttributeChangedEvent
    }

    final Map<String, Object> upMoAndAttributesForUpgradeAndConfirm(String upMoSate){
        java.util.Map<String, Object> adminData = new HashMap();
        java.util.Map<String, Object> moAttributes = new HashMap();
        adminData.put("productName", null);
        adminData.put(UpgradeActivityConstants.PRODUCT_NUMBER, productNumber);
        adminData.put(UpgradeActivityConstants.PRODUCT_REVISION, productRevision);
        moAttributes.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, adminData);
        moAttributes.put(UpgradePackageMoConstants.UP_MO_STATE, upMoSate);
        return moAttributes;
    }

    def buildDpsAttributeChangedEventForUpgradeActivity() {

        AttributeChangeData attributeChangeDataProgHdr = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.VERIFICATION_FINISHED.toString(), UpgradeProgressInformation.WAIT_FOR_CONF_UPGRADE.toString(), new Object(), new Object())
        AttributeChangeData attributeChangeDataState = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.INSTALL_COMPLETED.toString(), UpgradePackageState.AWAITING_CONFIRMATION.toString(), new Object(), new Object())
        List<Map<String, Object>> upActionList = new ArrayList<Map<String, Object>>()
        Map<String, Object> actionaResultMap = new HashMap<String, Object>();
        actionaResultMap.put("notifiableAttributeValue", ActionResultInformation.EXECUTED.toString());
        actionaResultMap.put("previousNotifiableAttributeValue", EXECUTING);
        upActionList.add(actionaResultMap);
        AttributeChangeData attributeChangeDataResult = new AttributeChangeData(UpgradePackageMoConstants.UP_ACTION_RESULT,upActionList, upActionList, new Object(), new Object())
        Set<AttributeChangeData> attributeSet=new HashSet<>();
        attributeSet.add(attributeChangeDataProgHdr)
        attributeSet.add(attributeChangeDataState)
        attributeSet.add(attributeChangeDataResult)
        DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent("RcswM", "Upgradepackage", "2.0.0",3L, upMoFdn,"liveBucket",attributeSet)
        return dpsAttributeChangedEvent
    }


    def buildDpsAttributeChangedEventForConfirmActivity() {

        AttributeChangeData attributeChangeDataProgHdr = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_PROG_HEADER, UpgradeProgressInformation.WAIT_FOR_CONF_UPGRADE.toString(), UpgradeProgressInformation.UPGRADE_EXECUTED.toString(), new Object(), new Object())
        AttributeChangeData attributeChangeDataState = new AttributeChangeData(UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageState.AWAITING_CONFIRMATION.toString(), UpgradePackageState.UPGRADE_COMPLETED.toString(), new Object(), new Object())
        List<Map<String, Object>> upActionList = new ArrayList<Map<String, Object>>()
        Map<String, Object> actionaResultMap = new HashMap<String, Object>();
        actionaResultMap.put("notifiableAttributeValue", ActionResultInformation.EXECUTED.toString());
        actionaResultMap.put("previousNotifiableAttributeValue", EXECUTING);
        upActionList.add(actionaResultMap);
        AttributeChangeData attributeChangeDataResult = new AttributeChangeData(UpgradePackageMoConstants.UP_ACTION_RESULT,upActionList, upActionList, new Object(), new Object())
        Set<AttributeChangeData> attributeSet=new HashSet<>();
        attributeSet.add(attributeChangeDataProgHdr)
        attributeSet.add(attributeChangeDataState)
        attributeSet.add(attributeChangeDataResult)
        DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent("RcswM", "Upgradepackage", "2.0.0",3L, upMoFdn,"liveBucket",attributeSet)
        return dpsAttributeChangedEvent
    }
}
