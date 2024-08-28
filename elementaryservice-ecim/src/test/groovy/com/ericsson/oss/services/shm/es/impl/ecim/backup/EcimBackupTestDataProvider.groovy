/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.backup

import javax.inject.Inject

import org.apache.commons.collections4.map.HashedMap

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.services.shm.common.InventoryQueryConfigurationProvider
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.exception.BackupDataNotFoundException
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider
import com.ericsson.oss.services.shm.common.networkelement.NetworkElementAttributes
import com.ericsson.oss.services.shm.common.retry.DpsWriterRetryProxy
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress
import com.ericsson.oss.services.shm.ecim.common.FragmentType
import com.ericsson.oss.services.shm.ecim.common.EcimCommonConstants.ReportProgress
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.es.dps.events.DpsStatusInfoProvider
import com.ericsson.oss.services.shm.es.ecim.backup.common.*
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.*
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.es.impl.JobEnvironment
import com.ericsson.oss.services.shm.es.moaction.MoActionMTRManager
import com.ericsson.oss.services.shm.es.moaction.cache.MoActionCacheProvider
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants
import com.ericsson.oss.services.shm.es.upgrade.remote.api.RemoteSoftwarePackageManager
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo
import com.ericsson.oss.services.shm.jobs.common.constants.*
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.model.event.based.mediation.ShmEcimMOActionMediationTaskRequest
import com.ericsson.oss.services.shm.nejob.cache.*
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean
import com.ericsson.oss.services.shm.notifications.api.*
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.shared.util.EncryptAndDecryptConverter
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier



public class EcimBackupTestDataProvider extends CdiSpecification {

    @MockedImplementation
    protected JobEnvironment jobEnvironment

    @MockedImplementation
    protected NeJobStaticDataProvider neJobStaticDataProvider

    @MockedImplementation
    protected NetworkElementRetrievalBean networkElementRetrievalBean

    @MockedImplementation
    protected RemoteSoftwarePackageManager remoteSoftwarePackageManager

    @MockedImplementation
    protected BrmMoServiceRetryProxy brmMoServiceRetryProxy

    @MockedImplementation
    protected OssModelInfoProvider ossModelInfoProvider

    @MockedImplementation
    protected EventSender<ShmEcimMOActionMediationTaskRequest> eventSender

    @Inject
    protected ActivityUtils activityUtils

    @Inject
    protected MoActionMTRManager moActionMTRManager

    @MockedImplementation
    protected MoActionCacheProvider moActionCacheProvider

    @MockedImplementation
    protected NetworkElementAttributes networkElement

    @MockedImplementation
    protected InventoryQueryConfigurationProvider inventoryQueryConfigurationListener

    @MockedImplementation
    protected ActivityJobTBACValidator activityJobTBACValidator

    @Inject
    protected SystemRecorder systemRecorder

    @Inject
    protected WorkflowInstanceNotifier workflowInstanceNotifier

    @MockedImplementation
    protected NotificationRegistry notificationRegistry

    @MockedImplementation
    protected Notification notification

    @MockedImplementation
    protected BrmMoService brmMoService

    @MockedImplementation
    protected DpsWriterRetryProxy dpsWriterProxy

    @Inject
    protected PollingActivityManager pollingActivityManager

    @Inject
    protected  DpsStatusInfoProvider dpsStatusInfoProvider
    
    @MockedImplementation
    EncryptAndDecryptConverter encryptAndDecryptConverter;

    def nodeName ="LTE01dg2ERBS00002"
    def neType ="RadioNode"
    def activityJobId = 1L
    def platform = "ECIM"
    def activityName = "backup"
    def backupLocation = "ENM"
    def backupType = "Systemdata"
    def backupName = "2"
    def domainName = "System"
    def JOB_NOT_FOUND_EXCEPTION="JOB_NOT_FOUND_EXCEPTION"
    def EXCEPTION="EXCEPTION"
    def BACKUP_DATA_NOT_FOUND="BACKUP_DATA_NOT_FOUND"
    def MO_EXCEPTION="MO_EXCEPTION"
    def UNSUPPORTED_FRAGEMENT_EXCEPTION="UNSUPPORTED_FRAGEMENT_EXCEPTION"
    def SUCCESS="SUCCESS"
    def FAIL="FAILED"
    def ARGUMENT_BUILDER_EXCEPTION="ARGUMENT_BUILDER_EXCEPTION"
    def RUNTIME_EXCEPTION="RUNTIME_EXCEPTION"
    def UPLOAD_BACKUP="UploadBackup"
    def BRM_BACKUP_MANAGER_ID="brmBackupManagerID"
    def FAILED_MOFDN=null
    def CANCEL="CANCEL"
    def backupFileName="CXP101-R501_"//+(new Date()).getTime()
    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
    def NEJobStaticData neJobStaticData = new NEJobStaticData(activityJobId, 2L, nodeName, "1234", PlatformTypeEnum.ECIM.name(), (new Date()).getTime(), "LTE17")
    def JobActivityInfo jobActivityInfo = new JobActivityInfo(activityJobId,activityName,JobTypeEnum.BACKUP, PlatformTypeEnum.ECIM)
    def OssModelInfo ossModelInfo = new OssModelInfo("ECIM_BrM","3.5.1","ECIM_BrM", "3.5.1")
    def moFdn = "SubNetwork=LTE01dg2ERBS00002,MeContext=LTE01dg2ERBS00002,ManagedElement=LTE01dg2ERBS00002,SystemFunctions=1,BrM=1,BrmBackupManager=1,BrmBackup=2"
    def EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType)
    def backupNameFromResponse ="CXP101-R501_" /*"shm_testbackup1"*/
    def FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(moFdn, activityJobId,jobActivityInfo)
    final Map<String,Object> jobProperties = new HashMap<String,Object>()
    final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
    def Map<String, Object> mainJobAttributes=new HashMap<String,Object>();
    def Map<String, Object> jobConfiguration=new HashMap<String,Object>();
    def Map<String, Object> responseAttributes=new HashMap<String,Object>();

    def AttributeChangeData attributeChangeData = new AttributeChangeData("createBackupState", "CREATING_BACKUP", "BACKUP_COMPLETED", new Object(), new Object())
    def DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent("RcBrM", "Createbackup", "2.0.0",3L, moFdn,"liveBucket", Arrays.asList(attributeChangeData))

    def buildDataToProcessMTRAttributes(String senario){
        final Map<String,Object> activityJobAttributes = new HashMap<String,Object>()
        final Map<String, Object> actionArguments = new HashMap<String, Object>()
        actionArguments.put(ShmConstants.KEY,"uri")
        actionArguments.put(ShmConstants.VALUE,"Password")
        ecimBackupInfo.setBackupFileName(backupFileName)
        ecimBackupInfo.setBackupLocation(backupLocation)
        buildJobActivityInfo()
        buildNetworkElementData()

        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY) >> neJobStaticData
        switch(senario) {
            case JOB_NOT_FOUND_EXCEPTION:
                brmMoServiceRetryProxy.getBrmBackupManagerMoFdn(networkElement, nodeName, ecimBackupInfo) >> FAILED_MOFDN
                break;
            case EXCEPTION:
                brmMoServiceRetryProxy.getNotifiableMoFdn(_, _, _) >> {throw new Exception("Exception occurred")}
                break;
            case MO_EXCEPTION:
                brmMoServiceRetryProxy.getNotifiableMoFdn(_, _, _) >> {throw new MoNotFoundException("MoNotFoundException occurred")}
                break;
            case UNSUPPORTED_FRAGEMENT_EXCEPTION:
                brmMoServiceRetryProxy.getNotifiableMoFdn(_, _, _) >> {throw new UnsupportedFragmentException("UnsupportedFragmentException occurred")}
                break;
            case RUNTIME_EXCEPTION:
                brmMoServiceRetryProxy.getNotifiableMoFdn(_, _, _) >> {throw new RuntimeException("RuntimeException occurred")}
                break;

            default:
                brmMoServiceRetryProxy.getBrmBackupManagerMoFdn(networkElement, nodeName, ecimBackupInfo) >> moFdn
                brmMoServiceRetryProxy.getNotifiableMoFdn(_, _, _) >> moFdn
        }
        brmMoServiceRetryProxy.prepareActionArgumentsForUploadBackup(networkElement, nodeName) >> actionArguments
        jobEnvironment.getActivityJobAttributes() >> activityJobAttributes
        networkElementRetrievalBean.getNeType(nodeName) >> neType

        fdnNotificationSubject.setTimeStamp(new Date())
        notification.getNotificationSubject() >> fdnNotificationSubject
        notification.getNotificationEventType() >> NotificationEventTypeEnum.AVC
        inventoryQueryConfigurationListener.getNeFdnBatchSize() >> 3
        notification.getDpsDataChangedEvent() >> dpsAttributeChangedEvent
    }

    def buildJobActivityInfo(){
        activityUtils.getActivityInfo(activityJobId, this.getClass()) >> jobActivityInfo
    }

    def buildNetworkElementData(){

        networkElement.getNeType() >> neType
        networkElement.getNodeModelIdentity() >> "ModelIndentity"
        networkElement.getOssModelIdentity() >> "OssModelIdentity"
        FragmentType.ECIM_BRM_TYPE.getFragmentName() >> "ECIM_BrM"
        networkElementRetrievalBean.getNetworkElementData(nodeName) >> networkElement
        ossModelInfoProvider.getOssModelInfo(networkElement.getNeType(), networkElement.getOssModelIdentity(),
                FragmentType.ECIM_BRM_TYPE.getFragmentName()) >> ossModelInfo
        ossModelInfo.getReferenceMIMVersion() >> "3.5.1"
        ossModelInfo.getReferenceMIMNameSpace() >> "ECIM_BrM"
    }

    def Map<String, Object > buildMoActionResponseAttributes(final long actionId, final boolean isActionAlreadyRunningOnTheNode, final String errorMessage){
        final Map<String, Object> responseAttributes = new HashMap<String, Object>();
        responseAttributes.put(ShmConstants.ACTION_RESPONSE, actionId);
        responseAttributes.put(ShmConstants.FDN, moFdn);
        responseAttributes.put(PollingActivityConstants.MO_NAME, backupNameFromResponse);
        responseAttributes.put(PollingActivityConstants.IS_ACTION_ALREADY_RUNNING, isActionAlreadyRunningOnTheNode);
        if(errorMessage !=null){
            responseAttributes.put(PollingActivityConstants.ERROR_MESSAGE, errorMessage);
        }
        return responseAttributes;
    }

    def buildJobPO(String senario){
        final List<Map<String,Object>> mainJobPropertiesList = new ArrayList<>();
        final List<Map<String, Object>> schedulePropertiesList = new ArrayList<>();
        final Map<String, Object> mainScheduleMap=new HashMap<>()
        final Map<String, Object> schedulePropCrnExpMap=new HashMap<>()
        final Map<String, Object> schedulePropRptTypeMap=new HashMap<>()
        final Map<String,Object> mainJobPO = new HashMap<>();
        final Map<String,Object> mainJobProperties = new HashMap<>();
        final List<Map<String, Object>> activities =new ArrayList<>()
        final Map<String, Object> activityMap=new HashMap<>()
        inventoryQueryConfigurationListener.getNeFdnBatchSize() >> 3

        if(UPLOAD_BACKUP.equals(senario)) {
            mainJobProperties.put(ShmConstants.KEY, "UPLOAD_BACKUP_DETAILS")
            mainJobProperties.put(ShmConstants.VALUE,"CXP101-R501_/System/Systemdata")
            activityMap.put(JobModelConstants.ACTIVITY_NAME,ActivityConstants.CREATE_BACKUP)
            activities.add(activityMap)
            jobConfiguration.put(JobModelConstants.ACTIVITIES, activities);

        } else {
            mainJobProperties.put(ShmConstants.KEY, "jobName")
            mainJobProperties.put(ShmConstants.VALUE, "BackupJob_administrator")
        }

        schedulePropRptTypeMap.put(ShmConstants.KEY,JobPropertyConstants.REPEAT_TYPE)
        schedulePropertiesList.add(schedulePropRptTypeMap)

        mainScheduleMap.put(ShmConstants.SCHEDULINGPROPERTIES,schedulePropertiesList)
        mainJobPropertiesList.add(mainJobProperties)
        jobConfiguration.put("jobProperties", mainJobPropertiesList);
        mainJobPO.put("jobConfigurationDetails", jobConfiguration);

        Map<String,Object> attributeMap = getAttributeMap(senario)

        runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.ACTIVITY_JOB).addAttributes(attributeMap).build();

        switch(senario) {
            case EXCEPTION:
                jobConfiguration.put(ShmConstants.MAIN_SCHEDULE, new HashedMap<>());
                break;
            case JOB_NOT_FOUND_EXCEPTION:
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY) >> {throw new JobDataNotFoundException("Exception occurred while executing Create Backup job")}
                break;
            case FAIL:
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY) >> null
                break;
            case "SECURE_BACKUP":
                jobConfiguration.put(ShmConstants.MAIN_SCHEDULE, mainScheduleMap);
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY) >> neJobStaticData
                jobConfiguration.put(ShmConstants.NETYPEJOBPROPERTIES,getNeTypeJobProperties("pwdText","ultext"));
                break;
            default :
                jobConfiguration.put(ShmConstants.MAIN_SCHEDULE, mainScheduleMap);
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY) >> neJobStaticData
        }

        final PersistenceObject mainJobPo = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.JOB).addAttributes(mainJobPO).build();
        addNeJob(nodeName,mainJobPo.poId);

        buildNetworkElementData();
        networkElement.getNeType() >> neType
        networkElementRetrievalBean.getNeType(nodeName) >> neType
        buildDataToProcessMTRAttributes(senario)
    }

    protected Map<String,Object> getAttributeMap(final String scenario) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String,Object>>()
        final Map<String,Object> activityJobPropertiesType = new HashMap<String,Object>()
        final Map<String,Object> activityJobPropertiesDomain = new HashMap<String,Object>()
        final Map<String,Object> activityJobPropertiesName = new HashMap<String,Object>()
        final Map<String,Object> activityJobPropertiesMgrId = new HashMap<String,Object>()
        final Map<String,Object> activityJobProp = new HashMap<String,Object>()
        final Map<String,Object> activityJobPropCurBackup = new HashMap<String,Object>()
        final Map<String,Object> activityJobPropTotalBackup = new HashMap<String,Object>()
        final Map<String,Object> activityJobPropProcessedBackup = new HashMap<String,Object>()
        final Map<String,Object> activityJobPropCancel = new HashMap<String,Object>()
        final Map<String,Object> secureBackupJobPasswordProperty = new HashMap<String,Object>()
        final Map<String,Object> secureBackupJobUserLabelProperty = new HashMap<String,Object>()
        final Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(ShmConstants.ACTIVITY_NE_JOB_ID, 1L);
        attributeMap.put(ShmConstants.NE_NAME,nodeName);
        final double progressPercentage = 10.0;
        attributeMap.put(ShmConstants.PROGRESSPERCENTAGE,progressPercentage);
        attributeMap.put(ShmConstants.ACTIVITY_START_DATE, new Date())
        attributeMap.put(ShmConstants.ENTRY_TIME, new Date())



        activityJobPropCurBackup.put(ShmConstants.KEY,EcimBackupConstants.CURRENT_BACKUP)
        activityJobPropCurBackup.put(ShmConstants.VALUE,backupFileName+"/System/Systemdata")

        activityJobPropProcessedBackup.put(ShmConstants.KEY,BackupActivityConstants.PROCESSED_BACKUPS)
        activityJobPropProcessedBackup.put(ShmConstants.VALUE,"0")

        activityJobPropTotalBackup.put(ShmConstants.KEY,EcimBackupConstants.TOTAL_BACKUPS)
        activityJobPropTotalBackup.put(ShmConstants.VALUE,"0")

        activityJobPropertiesDomain.put(ShmConstants.KEY, "GENERATE_BACKUP_NAME")
        activityJobPropertiesDomain.put(ShmConstants.VALUE,"true")
        activityJobPropertiesName.put(ShmConstants.KEY, EcimBackupConstants.BRM_BACKUP_NAME)
        activityJobPropertiesName.put(ShmConstants.VALUE,backupFileName)

        activityJobPropertiesMgrId.put(ShmConstants.KEY,BRM_BACKUP_MANAGER_ID)
        activityJobPropertiesMgrId.put(ShmConstants.VALUE,"1")

        activityJobProp.put(ShmConstants.KEY,ActivityConstants.IS_ACTIVITY_TRIGGERED)
        activityJobProp.put(ShmConstants.VALUE,"true")

        activityJobPropCancel.put(ShmConstants.KEY,ShmConstants.CANCELLEDBY)
        activityJobPropCancel.put(ShmConstants.VALUE,"true")

        activityJobPropertiesType.put(ShmConstants.KEY, "BACKUP_DOMAIN_TYPE")
        activityJobPropertiesType.put(ShmConstants.VALUE,"System/Systemdata")
        

        jobPropertyList.add(activityJobPropertiesDomain)
        jobPropertyList.add(activityJobPropertiesType)
        jobPropertyList.add(activityJobPropertiesName)
        jobPropertyList.add(activityJobPropertiesMgrId)
        jobPropertyList.add(activityJobProp)
        jobPropertyList.add(activityJobPropCancel)
        jobPropertyList.add(activityJobPropCurBackup)
        jobPropertyList.add(activityJobPropProcessedBackup)
        jobPropertyList.add(activityJobPropTotalBackup)

        if("SECURE_BACKUP".equals(scenario)){
            
            secureBackupJobUserLabelProperty.put(ShmConstants.KEY, "Userlabel")
            secureBackupJobUserLabelProperty.put(ShmConstants.VALUE,"testUserlabel")
            secureBackupJobPasswordProperty.put(ShmConstants.KEY, "Password")
            secureBackupJobPasswordProperty.put(ShmConstants.VALUE,"bB02kydIDYEi9+MCnlhETdCLNj6ur+xEGrYelZK3250=")
        }
         if("SECURE_BACKUP_WITH_EMPTY_USERLABEL".equals(scenario)){
            
            secureBackupJobUserLabelProperty.put(ShmConstants.KEY, "Userlabel")
            secureBackupJobUserLabelProperty.put(ShmConstants.VALUE,"")
            secureBackupJobPasswordProperty.put(ShmConstants.KEY, "Password")
            secureBackupJobPasswordProperty.put(ShmConstants.VALUE,"bB02kydIDYEi9+MCnlhETdCLNj6ur+xEGrYelZK3250=")
        }
        if("SECURE_BACKUP_WITH_EMPTY_PWD".equals(scenario)){
            
            secureBackupJobUserLabelProperty.put(ShmConstants.KEY, "Userlabel")
            secureBackupJobUserLabelProperty.put(ShmConstants.VALUE,"testUserlabel")
            secureBackupJobPasswordProperty.put(ShmConstants.KEY, "Password")
            secureBackupJobPasswordProperty.put(ShmConstants.VALUE,"")
        }
        jobPropertyList.add(secureBackupJobPasswordProperty)
        jobPropertyList.add(secureBackupJobUserLabelProperty)
        attributeMap.put("jobProperties",jobPropertyList)

        return attributeMap
    }

    def addNeJob(String nodeName,long mainJobId) {

        final Map<String, Object> attributeMap1 = new HashMap<>();
        attributeMap1.put(ShmConstants.MAINJOBID, mainJobId)
        attributeMap1.put(ShmConstants.NE_NAME,nodeName);
        runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.NE_JOB).addAttributes(attributeMap1).build();
    }

    def String getJobProperty(String propertyName, List<Map<String, String>> jobProperties){

        for(final Map<String, String> jobProperty:jobProperties){
            if(propertyName.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))){
                return  jobProperty.get(ActivityConstants.JOB_PROP_VALUE)
            }
        }
        return null;
    }

    def AsyncActionProgress getAsyncActionProgress(String activityName,String senario,String state) {
        final AsyncActionProgress asyncActionProgress=new AsyncActionProgress(getReportProgress(activityName,senario,state));

        return asyncActionProgress
    }

    def Map<String, Object> getReportProgress(String activityName,String senario,String state) {
        final  Map<String, Object> reportProgressAttributes=new HashMap<>()
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_ACTION_ID, "1")
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_ACTION_NAME,activityName)
        final  String additionalInfo=EcimBackupConstants.BACKUP_NAME_IN_ADDITIONAL_INFO+backupFileName
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_ADDITIONAL_INFO,additionalInfo )
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_PROGRESS_INFO, "progressinfo")
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_PROGRESS_PERCENTAGE, "10")
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_STEP_PROGRESS_PERCENTAGE, "2")
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_RESULT, senario)
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_STATE,state)
        reportProgressAttributes.put(ReportProgress.REPORT_PROGRESS_TIME_ACTION_STARTED,  (new Date()).getTime())


        return reportProgressAttributes
    }

    def void throwExceptionSenario(String senario) {

        switch(senario) {
            case JOB_NOT_FOUND_EXCEPTION:
                brmMoServiceRetryProxy.isBackupExist(_,_ as EcimBackupInfo) >> {throw new JobDataNotFoundException("JobDataNotFoundException occurred")}
                break;
            case BACKUP_DATA_NOT_FOUND:
                brmMoServiceRetryProxy.isBackupExist(_,_ as EcimBackupInfo) >> {throw new BackupDataNotFoundException("BackupDataNotFoundException occurred")}
                break;
            case MO_EXCEPTION:
                brmMoServiceRetryProxy.isBackupExist(_,_ as EcimBackupInfo) >> {throw new MoNotFoundException("MoNotFoundException occurred")}
                break;
            case EXCEPTION:
                brmMoServiceRetryProxy.isBackupExist(_,_ as EcimBackupInfo) >> {throw new Exception("Exception occurred")}
                break;
            case UNSUPPORTED_FRAGEMENT_EXCEPTION:
                brmMoServiceRetryProxy.isBackupExist(_,_ as EcimBackupInfo) >> {throw new UnsupportedFragmentException("UnsupportedFragmentException occurred")}
        }
    }
    
    List<Map<String,Object>> getNeTypeJobProperties(String password,String userLabel){
        List<Map<String,String>> properties = new ArrayList<>()
        Map<String, Object> neTypeProp = new HashMap<>();
        neTypeProp.put(ShmConstants.NETYPE, "vMTAS")
        if(password!=null){
            Map<String,Object> prop1 = new HashMap<>();
            prop1.put(ShmConstants.KEY,"Password")
            prop1.put(ShmConstants.VALUE,"bB02kydIDYEi9+MCnlhETdCLNj6ur+xEGrYelZK3250=")
            properties.add(prop1)
        }
        if(userLabel!=null){
            Map<String,String> prop1 = new HashMap<>();
            prop1.put(ShmConstants.KEY,"Userlabel")
            prop1.put(ShmConstants.VALUE,userLabel)
            properties.add(prop1)
        }
        neTypeProp.put(ShmConstants.JOBPROPERTIES, properties)
        return Arrays.asList(neTypeProp);

    }
}
