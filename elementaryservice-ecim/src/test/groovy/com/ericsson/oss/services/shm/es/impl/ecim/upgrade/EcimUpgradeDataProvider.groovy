/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.upgrade

import static org.junit.Assert.*

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.services.shm.common.InventoryQueryConfigurationProvider
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider
import com.ericsson.oss.services.shm.ecim.common.*
import com.ericsson.oss.services.shm.ecim.common.EcimCommonConstants.ReportProgress
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants.UpgradePackageMoConstants
import com.ericsson.oss.services.shm.es.ecim.backup.common.*
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.*
import com.ericsson.oss.services.shm.es.impl.ecim.common.SoftwarePackageNameNotFound
import com.ericsson.oss.services.shm.es.impl.ecim.deleteup.EcimUPConfigurationProvider
import com.ericsson.oss.services.shm.es.upgrade.remote.api.RemoteSoftwarePackageManager
import com.ericsson.oss.services.shm.inventory.software.ecim.api.*
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.jobs.common.constants.*
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.model.NetworkElementData
import com.ericsson.oss.services.shm.nejob.cache.*
import com.ericsson.oss.services.shm.notifications.api.*
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator

class EcimUpgradeDataProvider extends CdiSpecification {

    @MockedImplementation
    protected OssModelInfoProvider ossModelInfoProvider;

    @MockedImplementation
    protected OssModelInfo ossModelInfo;

    @Inject
    protected SwMHandler swMHandler;

    @MockedImplementation
    protected PlatformTypeProviderImpl platformTypeProvider;

    @MockedImplementation
    protected SwMVersionHandlersProviderFactory swMprovidersFactory;

    @MockedImplementation
    protected InventoryQueryConfigurationProvider inventoryQueryConfigurationListener

    @Inject
    SystemRecorder systemRecorder

    @MockedImplementation
    protected ActivityJobTBACValidator activityJobTBACValidator;

    @MockedImplementation
    protected NeJobStaticDataProvider neJobStaticDataProvider;

    @MockedImplementation
    protected RemoteSoftwarePackageManager remoteSoftwarePackageManager;

    /* @Inject
     protected ActivityUtils activityUtil*/

    @MockedImplementation
    protected Notification notification

    @MockedImplementation
    protected JobStaticDataProvider jobStaticDataProvider

    def nodeName = "LTE01dg2ERBS00002"
    def activityJobId = 3L
    def neJobId = 2L
    def mainJobId = 1L
    def upMoFdn ="ManagedElement=LTE01dg2ERBS00002,SystemFunctions=1,SwM=1,UpgradePackage=16ARadioNodePackage1"
    JobActivityInfo jobActivityInfo = new JobActivityInfo(activityJobId, "prepare", JobTypeEnum.UPGRADE, PlatformTypeEnum.ECIM)
    def NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "1234", "ECIM", 5L, "");
    def upgradePackageFilePath = "/home/smrs/smrsroot/CXP_R501";
    def ActionResult actionResult = new ActionResult();
    def Map<String, Object> jobConfiguration=new HashMap<String,Object>();
    def ActivityAllowed activityAllowed = new ActivityAllowed()

    def softwarePackagename = "16ARadioNodePackage1";
    def FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(upMoFdn, activityJobId,jobActivityInfo)
    def SUCCESS="SUCCESS"
    def EXCEPTION="EXCEPTION"
    def FAIL="FAIL"
    def PRECHECK_DONE="PRECHECK_DONE"
    def CANCEL_EXCEPTION="CANCEL_EXCEPTION"
    final String FDN_EXCEPTION="FDN_EXCEPTION"
    final String FDN_UNSUPPORTED_FRAGMENT="FDN_UNSUPPORTED_FRAGMENT"
    final String FDN_MO_NOT_FOUND="FDN_MO_NOT_FOUND"
    final String FDN_NODE_ATTR_READER_EXCEPTION="FDN_NODE_ATTR_READER_EXCEPTION"
    final String FDN_SOFTWARE_PCGPO_EXCEPTION="FDN_SOFTWARE_PCGPO_EXCEPTION"
    final String FDN_SOFTWARE_PCGNAME_EXCEPTION="FDN_SOFTWARE_PCGNAME_EXCEPTION"
    final String FDN_ARG_BUILDER_EXCEPTION="FDN_ARG_BUILDER_EXCEPTION"

    def JOB_NOT_FOUND_EXCEPTION="JOB_NOT_FOUND_EXCEPTION"
    def AttributeChangeData attributeChangeData = new AttributeChangeData("upgradePackageState", UpgradePackageState.INITIALIZED.toString(), UpgradePackageState.PREPARE_COMPLETED.toString(), new Object(), new Object())
    def DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent("RcswM", "Upgradepackage", "2.0.0",3L, upMoFdn,"liveBucket", Arrays.asList(attributeChangeData))
    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps);
    protected EcimUPConfigurationProvider ecimUPConfigurationProvider=new EcimUPConfigurationProvider(runtimeDps);

    def buildDataForPrepareAction(final String nodeName) {
        ecimUPConfigurationProvider.addSwMParentMO(nodeName)
        ecimUPConfigurationProvider.addUpgradePackageOnNode("CXP101", "R501", nodeName)
        List<String> upgradefilePaths = new ArrayList<>();
        upgradefilePaths.add(upgradePackageFilePath)

        remoteSoftwarePackageManager.getUpgradePackageDetails(softwarePackagename) >> upgradefilePaths
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData
        platformTypeProvider.getPlatformTypeBasedOnCapability(_ as String, _ as String) >> PlatformTypeEnum.ECIM;
        notification.getNotificationSubject() >> fdnNotificationSubject
        notification.getNotificationEventType() >> NotificationEventTypeEnum.AVC
        ossModelInfoProvider.getOssModelInfo(_ as String,_ as String, _ as String) >> ossModelInfo;
        ossModelInfo.getReferenceMIMVersion() >> "2.0.0";
        ossModelInfo.getNamespace() >> "RcsSwM";
        inventoryQueryConfigurationListener.getNeFdnBatchSize() >> 3
        activityAllowed.setActivityAllowed(true)
        activityAllowed.setUpMoFdn(upMoFdn)
        swMprovidersFactory.getSoftwareManagementHandler(_ as String) >> swMHandler
        swMHandler.isActivityAllowed(_ as String, _ as String, _ as String, _ as String, _) >> activityAllowed
        swMHandler.getUpgradePkgState(_ as String, _ as String, _ as String,, _) >> UpgradePackageState.INITIALIZED
        swMHandler.isUpgradePackageMoExists(_ as String, _ as String, _ as String, _) >> true
        swMHandler.getNotifiableMoFdn(_ as String, _ as String, _ as String, _ as String, _) >> upMoFdn
        swMHandler.isValidAsyncActionProgress(_ as String, _) >> false

        notification.getDpsDataChangedEvent() >> dpsAttributeChangedEvent
    }
    def buildDataForUpgradeActivity(final String nodeName,final String senario) {

        ecimUPConfigurationProvider.addSwMParentMO(nodeName)
        ecimUPConfigurationProvider.addUpgradePackageOnNode("CXP101", "R501", nodeName)

        final List<String> upgradefilePaths = new ArrayList<>();
        upgradefilePaths.add(upgradePackageFilePath)

        remoteSoftwarePackageManager.getUpgradePackageDetails(softwarePackagename) >> upgradefilePaths

        switch(senario) {
            case EXCEPTION:
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> { throw new Exception("Exception Occurred") }
                break;
            case JOB_NOT_FOUND_EXCEPTION:
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> { throw new JobDataNotFoundException("JobDataNotFoundException Occurred") }
                break;
            case FDN_EXCEPTION:
                swMHandler.getNotifiableMoFdn(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> {throw new Exception("Exception Occurred")}
                break;
            case FDN_UNSUPPORTED_FRAGMENT:
                swMHandler.getNotifiableMoFdn(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> {throw new UnsupportedFragmentException("UnsupportedFragmentException Occurred")}
                break;
            case FDN_MO_NOT_FOUND:
                swMHandler.getNotifiableMoFdn(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> {throw new MoNotFoundException("MoNotFoundException Occurred")}
                break;
            case FDN_ARG_BUILDER_EXCEPTION:
                swMHandler.getNotifiableMoFdn(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> {throw new ArgumentBuilderException("ArgumentBuilderException Occurred")}
                break;
            case FDN_SOFTWARE_PCGNAME_EXCEPTION:
                swMHandler.getNotifiableMoFdn(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> {throw new SoftwarePackageNameNotFound("SoftwarePackageNameNotFound Occurred")}
                break;
            case FDN_SOFTWARE_PCGPO_EXCEPTION:
                swMHandler.getNotifiableMoFdn(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> {throw new SoftwarePackagePoNotFound("SoftwarePackagePoNotFound Occurred")}
                break;
            case FDN_NODE_ATTR_READER_EXCEPTION:
                swMHandler.getNotifiableMoFdn(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> {throw new NodeAttributesReaderException("NodeAttributesReaderException Occurred")}
                break;
            case CANCEL_EXCEPTION:
                swMHandler.executeCancelAction(_, _, _, _,_ as NetworkElementData) >> {throw new Exception("Exception Occurred")}
                break;
            default :
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData
                swMHandler.getNotifiableMoFdn(_ as String, _ as String, _ as String, _ as String, _ as NetworkElementData) >> upMoFdn
        }

        platformTypeProvider.getPlatformTypeBasedOnCapability(_ as String, _ as String) >> PlatformTypeEnum.ECIM;

        fdnNotificationSubject.setTimeStamp(new Date())
        notification.getNotificationSubject() >> fdnNotificationSubject

        //notification.getNotificationEventType() >> NotificationEventTypeEnum.AVC
        ossModelInfoProvider.getOssModelInfo(_ as String,_ as String, _ as String) >> ossModelInfo;
        ossModelInfo.getReferenceMIMVersion() >> "2.0.0";
        ossModelInfo.getNamespace() >> "RcsSwM";
        inventoryQueryConfigurationListener.getNeFdnBatchSize() >> 3
        swMprovidersFactory.getSoftwareManagementHandler(_ as String) >> swMHandler
        //swMHandler.getUpgradePkgState(_ as String, _ as String, _ as String,, _) >> UpgradePackageState.INITIALIZED
        swMHandler.isUpgradePackageMoExists(_ as String, _ as String, _ as String, _) >> true

        notification.getDpsDataChangedEvent() >> dpsAttributeChangedEvent
    }

    def AsyncActionProgress getAsyncActionProgress(final String activityName,final ActionStateType actionStateType,final ActionResultType actionResultType) {
        final AsyncActionProgress asyncActionProgress=new AsyncActionProgress(getReportProgress(activityName,actionStateType,actionResultType))
        return asyncActionProgress
    }

    def getAsyncActionProgressWhenJobisRunning(final String activityName) {

        final AsyncActionProgress asyncActionProgress=getAsyncActionProgress(activityName,ActionStateType.FINISHED,ActionResultType.NOT_AVAILABLE)
        swMHandler.getAsyncActionProgress(_ as String, _ as String, _ as String, _ as String, _) >> asyncActionProgress
        swMHandler.getValidAsyncActionProgress(_) >> asyncActionProgress
        asyncActionProgress.getState() >> ActionStateType.FINISHED
    }

    def getAsyncActionProgressWhenJobisSuccess(final String activityName) {

        final AsyncActionProgress asyncActionProgress=getAsyncActionProgress(activityName,ActionStateType.FINISHED,ActionResultType.SUCCESS)
        swMHandler.getAsyncActionProgress(_ as String, _ as String, _ as String, _ as String, _) >> asyncActionProgress
        swMHandler.getValidAsyncActionProgress(_) >> asyncActionProgress
        asyncActionProgress.getState() >> ActionStateType.FINISHED
    }

    def getAsyncActionProgressWhenJobisFailed(final String activityName) {

        final AsyncActionProgress asyncActionProgress=getAsyncActionProgress(activityName,ActionStateType.FINISHED,ActionResultType.FAILURE)
        swMHandler.getAsyncActionProgress(_ as String, _ as String, _ as String, _ as String, _) >> asyncActionProgress
        swMHandler.getValidAsyncActionProgress(_) >> asyncActionProgress
        asyncActionProgress.getState() >> ActionStateType.FINISHED
    }

    def getActionResultWhenActionTriggeredisFailed() {
        actionResult.setActionId(-1)
        actionResult.setTriggerSuccess(false)
        swMHandler.executeMoAction(_ as String, _ as String, _ as String, _ as String, _) >> actionResult
    }

    def getActionResultWhenActionTriggeredisSuccess() {
        actionResult.setActionId(1)
        actionResult.setTriggerSuccess(true)
        swMHandler.executeMoAction(_ as String, _ as String, _ as String, _ as String, _) >> actionResult
    }

    def loadJobProperties(final String nodeName,final String activityName) {

        loadMainJob()
        loadNEJobAttributes(nodeName)
        loadActivityJobProperties(activityName)
    }

    def loadJobPropertiesForUpgrade(final String nodeName,final String activityName,final String senario) {
        loadMainJob()
        loadNEJobAttributes(nodeName)
        loadActivityJobPropertiesForUpgrade(activityName,senario)
    }

    def loadMainJob() {
        final Map<String, Object> mainjobAttributes = new HashMap<>();
        final Map<String, Object> mainjobAttributesSwpName = new HashMap<>();
        final Map<String, Object> mainJobAttributesMap = new HashMap<>()
        final List<Map<String, Object>> mainJobAttributesList = new ArrayList<>()

        mainjobAttributes.put(ShmJobConstants.KEY, UpgradePackageMoConstants.UP_MO_IGNORE_BREAK_POINTS);
        mainjobAttributes.put(ShmJobConstants.VALUE, "true");
        mainJobAttributesList.add(mainjobAttributes);

        mainjobAttributesSwpName.put(ShmJobConstants.KEY, UpgradeActivityConstants.SWP_NAME);
        mainjobAttributesSwpName.put(ShmJobConstants.VALUE, softwarePackagename);
        mainJobAttributesList.add(mainjobAttributesSwpName);

        jobConfiguration.put(ShmJobConstants.JOBPROPERTIES, mainJobAttributesList);
        mainJobAttributesMap.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("MainJob").addAttributes(mainJobAttributesMap).build();
    }

    private loadActivityJobProperties(final String activityName) {

        final Map<String, Object> activityJobAttributesMap=getactivityJobAttributesMap(activityName,SUCCESS)
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("ActivityJob").addAttributes(activityJobAttributesMap).build()
    }

    private loadActivityJobPropertiesForUpgrade(final String activityName,final String senario) {

        final Map<String, Object> activityJobAttributesMap=getactivityJobAttributesMap(activityName,senario)
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("ActivityJob").addAttributes(activityJobAttributesMap).build()
    }

    private loadNEJobAttributes(final String nodeName) {
        final Map<String, Object> neJobAttributesMap = new HashMap<>();
        final Map<String, Object> neJobPropertyUserInput = new HashMap<>();
        final List<Map<String,Object>> neSpecificPropertyList = new ArrayList<>();

        neJobPropertyUserInput.put(ShmJobConstants.KEY, UpgradeActivityConstants.UP_FDN);
        neJobPropertyUserInput.put(ShmJobConstants.VALUE, upMoFdn);
        neSpecificPropertyList.add(neJobPropertyUserInput);

        neJobAttributesMap.put(ShmJobConstants.JOBPROPERTIES, neSpecificPropertyList);

        neJobAttributesMap.put(ShmConstants.MAIN_JOB_ID,mainJobId);
        neJobAttributesMap.put(ShmConstants.NE_NAME,, nodeName);
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("NeJob").addAttributes(neJobAttributesMap).build()
    }

    protected Map<String,Object> getactivityJobAttributesMap(final String activityName,final String senario) {
        final  Map<String, Object> activityjobAttributes = new HashMap<>();
        final Map<String, Object> activityJobAttributesMap = new HashMap<>()
        final List<Map<String, Object>> activityJobAttributesList = new ArrayList<>()
        final Map<String,Object> activityJobProp = new HashMap<String,Object>()
        activityjobAttributes.put(ShmJobConstants.KEY, EcimCommonConstants.ACTION_TRIGGERED);
        activityjobAttributes.put(ShmJobConstants.VALUE, activityName);

        if(PRECHECK_DONE.equals(senario)) {
            activityJobProp.put(ShmConstants.KEY,ActivityConstants.IS_PRECHECK_DONE)
            activityJobProp.put(ShmConstants.VALUE,"true")
            activityJobAttributesList.add(activityJobProp)
        }
        activityJobAttributesList.add(activityjobAttributes)
        activityJobAttributesMap.put(ShmJobConstants.JOBPROPERTIES, activityJobAttributesList)
        activityJobAttributesMap.put(ShmConstants.ACTIVITY_NE_JOB_ID, 1L)
        activityJobAttributesMap.put(ShmConstants.NE_NAME,nodeName)
        final double progressPercentage = 10.0
        activityJobAttributesMap.put(ShmConstants.PROGRESSPERCENTAGE,progressPercentage)
        activityJobAttributesMap.put(ShmConstants.ACTIVITY_START_DATE, new Date())
        activityJobAttributesMap.put(ShmConstants.ENTRY_TIME, new Date())
        return activityJobAttributesMap;
    }
    protected String getJobProperty(final String propertyName,final List<Map<String, String>> jobProperties){

        for(Map<String, String> jobProperty:jobProperties){
            if(propertyName.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))){
                return  jobProperty.get(ActivityConstants.JOB_PROP_VALUE)
            }
        }
        return null;
    }

    def Map<String, Object> getReportProgress(final String activityName,final ActionStateType actionStateType,final ActionResultType actionResultType) {
        final  Map<String, Object> reportProgressAttributes=new HashMap<>()
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_ACTION_ID, "1")
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_ACTION_NAME,activityName)
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_ADDITIONAL_INFO,"Upgrade"+softwarePackagename )
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_PROGRESS_INFO, "progressinfo")
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_PROGRESS_PERCENTAGE, "10")
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_STEP_PROGRESS_PERCENTAGE, "2")
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_RESULT, actionResultType.toString())
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_STATE,actionStateType.toString())
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_TIME_ACTION_STARTED,  (new Date()).getTime())

        return reportProgressAttributes
    }

    protected ActivityAllowed getActivityAllowed(boolean isActivityAllowed) {
        final ActivityAllowed activityAllowed = new ActivityAllowed()
        activityAllowed.setActivityAllowed(isActivityAllowed)
        activityAllowed.setUpMoFdn(upMoFdn)
        return activityAllowed
    }

    def getActionResult(boolean isTriggerSuccess) {

        actionResult.setTriggerSuccess(isTriggerSuccess)
        if(isTriggerSuccess) {
            actionResult.setActionId(1)
        } else {
            actionResult.setActionId(-1)
        }
        swMHandler.executeMoAction(_ as String, _ as String, _ as String, _ as String, _) >> actionResult
    }
}
