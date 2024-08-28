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
package com.ericsson.oss.services.shm.es.impl.ecim.polling

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.query.*
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider
import com.ericsson.oss.services.shm.common.networkelement.NetworkElementAttributes
import com.ericsson.oss.services.shm.ecim.common.FragmentType
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants.UpgradePackageMoConstants;
import com.ericsson.oss.services.shm.es.dps.events.DpsStatusInfoProvider
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.*
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.es.impl.JobEnvironment
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants
import com.ericsson.oss.services.shm.es.polling.cache.PollingActivityCacheManager
import com.ericsson.oss.services.shm.es.upgrade.remote.api.RemoteSoftwarePackageManager
import com.ericsson.oss.services.shm.inventory.software.ecim.api.EcimSwMConstants
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants

class AbstractPollingData extends CdiSpecification {

    @MockedImplementation
    protected JobEnvironment jobEnvironment

    @MockedImplementation
    protected NeJobStaticDataProvider neJobStaticDataProvider

    @MockedImplementation
    protected NetworkElementRetrievalBean networkElementRetrievalBean

    @MockedImplementation
    protected RemoteSoftwarePackageManager remoteSoftwarePackageManager

    @MockedImplementation
    protected UpMoServiceRetryProxy upMoServiceRetryProxy

    @MockedImplementation
    protected OssModelInfoProvider ossModelInfoProvider

    @MockedImplementation
    protected DpsStatusInfoProvider dspStatusInfoProvider

    @MockedImplementation
    protected BrmMoServiceRetryProxy brmMoServiceRetryProxy

    @Inject
    protected ActivityUtils activityUtils

    @Inject
    protected EcimUpgradeInfo ecimUpgradeInfo

    @Inject
    protected PollingActivityManager pollingActivityManager

    @MockedImplementation
    protected NetworkElementAttributes networkElementData

    @Inject
    protected DataPersistenceService dataPersistenceService;

    @Inject
    protected PollingActivityCacheManager pollingActivityCacheManager;

    protected long poId;
    def activityJobId;
    def neJobId;
    def mainJobId;
    def actionId;
    def activityName;
    def NEJobStaticData neJobStaticData
    def JobActivityInfo jobActivityInfo;

    def List<Map<String, String>> activityJobPropertyList = new ArrayList<>()
    def  Map<String, String> activityJobProperty = new HashMap<>()
    def JobEnvironment jobEnvironment = new JobEnvironment(activityJobId, activityUtils)
    def List<String> moAttributes = new ArrayList<>()
    def Map<String, Object> mainJobAttributes = new HashMap<>()
    def Map<String, Object> neJobAttributes = new HashMap<>()
    def Map<String,Object> jobConfig = new HashMap<>();
    def Map<String, String> jobProperties = new HashMap<>();
    def Map<String, Object> jobConfiguration=new HashMap<>();
    def Map<String, Object> processVariables = new HashMap<>();

    def buildJobPO(final boolean isActivityCompleted, final String activityName, final String fragmentName, final String mimVersion, final String nameSpace){

        def List<Map<String, String>> mainJobPropertyList = new ArrayList<>();
        def Map<String, String> mainJobProperty = new HashMap<>();
        if(PollingTestConstants.ACTIVITY_ACTIVATE.equals(activityName)) {
            mainJobProperty.put(ShmConstants.KEY,UpgradePackageMoConstants.UP_MO_IGNORE_BREAK_POINTS)
            mainJobProperty.put(ShmConstants.VALUE, String.valueOf(true))
            mainJobPropertyList.add(mainJobProperty);
            mainJobProperty = new HashMap<>();
            mainJobProperty.put(ShmConstants.KEY,UpgradeActivityConstants.SWP_NAME)
            mainJobProperty.put(ShmConstants.VALUE, PollingTestConstants.softwarePackageName)
            mainJobPropertyList.add(mainJobProperty);

            def  List<String> upgradePackageFilePath = new ArrayList<>()
            upgradePackageFilePath.add("/home/smrs/smrsroot/software/radionode/16ARadioNodePackage1")
            remoteSoftwarePackageManager.getUpgradePackageDetails(PollingTestConstants.softwarePackageName) >> upgradePackageFilePath
        }
        else {
            mainJobProperty.put(ShmConstants.KEY,EcimBackupConstants.BRM_BACKUP_NAME);
            mainJobProperty.put(ShmConstants.VALUE, PollingTestConstants.backupName);
            mainJobPropertyList.add(mainJobProperty);
        }
        jobConfig.putAt(ShmJobConstants.JOBPROPERTIES,mainJobPropertyList);
        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfig);

        PersistenceObject mainJobPo = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.JOB).addAttributes(mainJobAttributes).build();
        mainJobId = mainJobPo.getPoId();

        addNeJob(PollingTestConstants.nodeName, mainJobId, isActivityCompleted, activityName);
        buildNeJobStaticData(activityName);
        buildNetworkElementData(activityName, fragmentName, mimVersion, nameSpace);
    }

    def addActivityJob(final boolean isActivityCompleted, final String activityName) {
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(ShmConstants.ACTIVITY_NE_JOB_ID, neJobId);
        attributeMap.put(ShmConstants.NE_NAME, PollingTestConstants.nodeName);
        final double progressPercentage = 20.0;
        attributeMap.put(ShmConstants.PROGRESSPERCENTAGE,progressPercentage);
        List<Map<String, Object>> jobPropertyList = new ArrayList<>()
        Map<String,Object> activityJobProperties = new HashMap<>()
        if((PollingTestConstants.ACTIVITY_UPLOAD.equals(activityName)) || (PollingTestConstants.ACTIVITY_CREATE.equals(activityName))) {
            activityJobProperties.put(ShmConstants.KEY, EcimBackupConstants.CURRENT_BACKUP)
            activityJobProperties.put(ShmConstants.VALUE,PollingTestConstants.backupName+"/"+PollingTestConstants.domainName+"/"+PollingTestConstants.backupType)
            jobPropertyList.add(activityJobProperties)
        }
        activityJobProperties = new HashMap<>()
        activityJobProperties.put(ShmConstants.KEY, ActivityConstants.ACTION_TRIGGERED)
        activityJobProperties.put(ShmConstants.VALUE,activityName)
        jobPropertyList.add(activityJobProperties)
        if(isActivityCompleted){
            activityJobProperties = new HashMap<>()
            activityJobProperties.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTIVITY_RESULT)
            activityJobProperties.put(ActivityConstants.JOB_PROP_VALUE, "SUCCESS")
            jobPropertyList.add(activityJobProperties)
        }
        attributeMap.put(ActivityConstants.JOB_PROPERTIES, jobPropertyList)
        PersistenceObject activityJobPo = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.ACTIVITY_JOB).addAttributes(attributeMap).build();
        activityJobId = activityJobPo.getPoId();
    }


    def buildNeJobStaticData(final String activityName){
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, PollingTestConstants.nodeName, PollingTestConstants.neJobBusinessKey, PollingTestConstants.platform, 5L, null)
        if(PollingTestConstants.ACTIVITY_ACTIVATE.equals(activityName)){
            neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData
        }else{
            neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY) >> neJobStaticData
        }
    }

    def addNeJob(final String nodeName, final long mainJobId, final boolean isActivityCompleted, final String activityName) {
        def List<Map<String, String>> neJobPropertyList = new ArrayList<>();
        def Map<String, String> neJobProperty = new HashMap<>();

        if(!(PollingTestConstants.ACTIVITY_ACTIVATE.equals(activityName))) {

            Map<String, Object> attributeMap = new HashMap<>();
            attributeMap.put(ShmConstants.MAINJOBID, mainJobId)
            attributeMap.put(ShmConstants.NE_NAME,nodeName);

            neJobProperty.put(ShmConstants.KEY,EcimBackupConstants.BRM_BACKUP_NAME)
            neJobProperty.put(ShmConstants.VALUE, PollingTestConstants.backupName)
            neJobPropertyList.add(neJobProperty)
            neJobProperty = new HashMap<>()
            neJobProperty.put(ShmConstants.KEY,EcimBackupConstants.BRM_BACKUP_MANAGER_ID)
            neJobProperty.put(ShmConstants.VALUE, PollingTestConstants.backupName+"/"+PollingTestConstants.domainName+"/"+PollingTestConstants.backupType)
            neJobPropertyList.add(neJobProperty)
            neJobProperty = new HashMap<>()
            neJobProperty.put(ShmConstants.KEY,JobPropertyConstants.AUTO_GENERATE_BACKUP)
            neJobProperty.put(ShmConstants.VALUE, String.valueOf(false))

            neJobPropertyList.add(neJobProperty)

            jobConfig.putAt(ShmJobConstants.JOBPROPERTIES,neJobPropertyList);
            neJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfig);
        }
        PersistenceObject neJobPo = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.NE_JOB).addAttributes(neJobAttributes).build();
        neJobId = neJobPo.getPoId();

        addActivityJob(isActivityCompleted, activityName);
    }



    def buildDataToSubscribeForPolling(capability, activityName, jobType, fragmentName, mimVersion, nameSpace){

        Map<String,Object> activityJobAttributes = new HashMap<>()
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, capability) >> neJobStaticData
        buildJobActivityInfo(activityName, jobType)
        buildNetworkElementData(activityName, fragmentName, mimVersion, nameSpace)

        if(PollingTestConstants.ACTIVITY_ACTIVATE.equals(activityName)) {

            networkElementRetrievalBean.getNeType(PollingTestConstants.nodeName) >> PollingTestConstants.neType
        }
        if(PollingTestConstants.ACTIVITY_UPLOAD.equals(activityName)) {

            activityJobProperty.put(ShmConstants.KEY,EcimBackupConstants.CURRENT_BACKUP)
            activityJobProperty.put(ShmConstants.VALUE,PollingTestConstants.backupName+"/"+PollingTestConstants.domainName+"/"+PollingTestConstants.backupType)
            activityJobPropertyList.add(activityJobProperty)
            activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList)
            brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.UPLOAD_BACKUP, PollingTestConstants.nodeName ,_) >> PollingTestConstants.brmBackupMoFdn
        }
        else {

            brmMoServiceRetryProxy.getNotifiableMoFdn(EcimBackupConstants.CREATE_BACKUP, PollingTestConstants.nodeName , _) >> PollingTestConstants.brmBackupMoFdn
        }
    }

    def buildJobActivityInfo(activityName, jobType){
        jobActivityInfo = new JobActivityInfo(activityJobId, activityName, jobType, PlatformTypeEnum.ECIM)
        activityUtils.getActivityInfo(activityJobId, this.getClass()) >> jobActivityInfo
    }



    def buildNetworkElementData(activityName, fragmentName, mimVersion, nameSpace){
        networkElementRetrievalBean.getNetworkElementData(PollingTestConstants.nodeName) >> networkElementData

        def OssModelInfo ossModelInfo = new OssModelInfo(nameSpace,mimVersion,nameSpace, mimVersion)
        networkElementData.getNeType() >> PollingTestConstants.neType
        networkElementData.getNodeModelIdentity() >> "ModelIndentity"
        networkElementData.getOssModelIdentity() >> "OssModelIdentity"
        FragmentType.ECIM_SWM_TYPE.getFragmentName() >>  fragmentName
        networkElementRetrievalBean.getNeType(PollingTestConstants.nodeName) >> PollingTestConstants.neType
        neJobStaticData.getNodeName() >> PollingTestConstants.nodeName
        upMoServiceRetryProxy.getNotifiableMoFdn(activityName, _) >> PollingTestConstants.upMoFdn

        moAttributes.add(EcimSwMConstants.SWM_REPORT_PROGRESS)
        moAttributes.add(ShmConstants.STATE)
        Arrays.asList(EcimCommonConstants.ReportProgress.ASYNC_ACTION_PROGRESS) >> moAttributes
        ossModelInfoProvider.getOssModelInfo(_, _,_) >> ossModelInfo
        ossModelInfo.getReferenceMIMVersion() >> mimVersion
        ossModelInfo.getReferenceMIMNameSpace() >> nameSpace
        networkElementData.getOssPrefix() >> ""
    }

    protected PersistenceObject getPollingActivityPos(activityJobId) {
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final Query<TypeRestrictionBuilder> pollingActivityQuery = queryBuilder.createTypeQuery(ShmJobConstants.NAMESPACE, ShmConstants.POLLING_ACTIVITY);
        final TypeRestrictionBuilder restrictionBuilder = pollingActivityQuery.getRestrictionBuilder();
        final Restriction activityJobIdRestriction = restrictionBuilder.equalTo(PollingActivityConstants.ACTIVITY_JOB_ID, activityJobId);
        pollingActivityQuery.setRestriction(activityJobIdRestriction);
        final Iterator<PersistenceObject> pollingActivityPOs =  runtimeDps.stubbedDps.liveBucket.getQueryExecutor().execute(pollingActivityQuery)
        while (pollingActivityPOs.hasNext()) {
            return pollingActivityPOs.next();
        }
    }
    def buildResponseData(final String fdn, final String state, final long progressPercentage, String additionalInfo, final String progressInfo, final String upMoState, final String result, final String actionName) {

        final  Map<String, Object> reportProgressData = new HashMap<>()
        final short progressPer = progressPercentage;
        reportProgressData.put("result", result);
        reportProgressData.put("stepProgressPercentage", progressPer);
        reportProgressData.put("state", state);
        reportProgressData.put("actionName", actionName);
        reportProgressData.put("progressPercentage", progressPer);
        reportProgressData.put("timeActionCompleted", "2018-03-28T10:48:32.437Z");
        reportProgressData.put("timeOfLastStatusUpdate", "2018-03-28T10:48:37.612Z");
        reportProgressData.put("resultInfo", null);
        reportProgressData.put("additionalInfo", additionalInfo);
        reportProgressData.put("progressInfo", progressInfo);
        reportProgressData.put("timeActionStarted", "2018-03-28T10:48:37.612Z");
        reportProgressData.put("actionId", 7);

        final Map<String, Object> moAttributesMap = new HashMap<>();
        if (PollingTestConstants.ACTIVITY_ACTIVATE.equals(actionName)) {
            reportProgressData.put("step", 1);
            moAttributesMap.put(EcimCommonConstants.UpgradePackageMoConstants.UP_MO_REPORT_PROGRESS, reportProgressData);
            moAttributesMap.put(EcimCommonConstants.UpgradePackageMoConstants.UP_MO_STATE, upMoState);
        }
        else {
            moAttributesMap.put(EcimCommonConstants.ReportProgress.ASYNC_ACTION_PROGRESS, reportProgressData);
        }
        final Map<String, Object> responseAttributes = new HashMap<>();
        responseAttributes.put(ShmConstants.MO_ATTRIBUTES, moAttributesMap);
        responseAttributes.put(ShmConstants.FDN, fdn);

        return responseAttributes
    }
}
