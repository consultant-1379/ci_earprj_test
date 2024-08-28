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
package com.ericsson.oss.services.shm.es.impl.cpp.polling

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.query.*
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider
import com.ericsson.oss.services.shm.common.networkelement.NetworkElementAttributes
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants
import com.ericsson.oss.services.shm.es.dps.events.DpsStatusInfoProvider
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.*
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.es.impl.JobEnvironment
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageMoConstants
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants
import com.ericsson.oss.services.shm.es.polling.cache.PollingActivityCacheManager
import com.ericsson.oss.services.shm.es.upgrade.remote.api.RemoteSoftwarePackageManager
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
    protected OssModelInfoProvider ossModelInfoProvider

    @MockedImplementation
    protected DpsStatusInfoProvider dspStatusInfoProvider

    @Inject
    protected ActivityUtils activityUtils

    @MockedImplementation
    protected NetworkElementAttributes networkElementData;

    @Inject
    protected DataPersistenceService dataPersistenceService;

    @Inject
    protected PollingActivityCacheManager pollingActivityCacheManager;


    def Map<String, Object> jobConfiguration=new HashMap<>();
    def Map<String, Object> activityJobAttributes=new HashMap<>();
    def Map<String, Object> mainJobAttributes=new HashMap<>();
    def Map<String, String> jobProperties = new HashMap<>();
    def Map<String, Object> processVariables = new HashMap<>();

    def nodeName ="LTE02ERBS00001"
    def neType ="ERBS"
    def activityJobId;
    def mainJobId;
    def neJobId;
    def platform = "CPP"
    def activityName;
    def upgradePackageFilePath = "/home/smrs/smrsroot/software/erbs/CXP102051_1_R4D71"
    def OssModelInfo ossModelInfo = new OssModelInfo("ERBS_NODE_MODEL","10.1.280","ERBS_NODE_MODEL", "10.1.280")
    def List<OssModelInfo> ossModelInfoList = Arrays.asList(ossModelInfo);
    def moFdn = "MeContext=LTE02ERBS00001,ManagedElement=1,SwManagement=1,UpgradePackage=CXP102051/1_R4D71"
    def Map<String, Object> responseAttributes = new HashMap<>();
    def NEJobStaticData neJobStaticData;
    def cvMoFdn = "MeContext=LTE02ERBS00001,ManagedElement=1,SwManagement=1,ConfigurationVersion=1"
    def CURRENTDETAILEDACTIVITY_IDLE = "IDLE";
    def MAIN_ACTION_RESULT_EXECUTING = "EXECUTING";
    def MAIN_ACTION_RESULT_EXECUTED = "EXECUTED";
    def UP_STATE_AWAITING_CONFIRMATION = "AWAITING_CONFIRMATION";
    def UP_STATE_VERIFY_EXECUTING = "VERIFICATION_EXECUTING";
    def ACTIVITY_NAME_UPGRADE = "upgrade";
    def ACTIVITY_NAME_UPLOAD = "exportcv";

    def buildDataToSubscribeForPolling(){
        Map<String,Object> activityJobAttributes = new HashMap<>()
        List<Map<String, Object>> jobPropertyList = new ArrayList<>()
        Map<String,Object> jobProperties = new HashMap<>()
        jobProperties.put(ShmConstants.KEY, ActivityConstants.ACTION_TRIGGERED)
        jobProperties.put(ShmConstants.VALUE,activityName)
        jobProperties.put(ShmConstants.KEY, UpgradeActivityConstants.UP_FDN)
        jobProperties.put(ShmConstants.VALUE,moFdn)
        jobPropertyList.add(jobProperties)
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES,jobPropertyList)
        jobEnvironment.getActivityJobAttributes() >> activityJobAttributes
        networkElementRetrievalBean.getNeType(nodeName) >> neType
        remoteSoftwarePackageManager.getUpgradePackageDetails(neType) >> upgradePackageFilePath
        buildNetworkElementData()
    }

    def buildResponseDataForUpgrade(final String state){
        final Map<String, Object> moAttributesMap = new HashMap<>();
        final List<Map<String, Object>> actionResultDataList = new ArrayList<>();
        moAttributesMap.put(UpgradePackageMoConstants.UP_MO_PROG_HEADER, "VERIFY_UPGRADE");
        moAttributesMap.put(UpgradePackageMoConstants.UP_MO_STATE, state);
        moAttributesMap.put(UpgradePackageMoConstants.UP_ACTION_RESULT, actionResultDataList);
        responseAttributes.put(ShmConstants.MO_ATTRIBUTES, moAttributesMap);
        responseAttributes.put(ShmConstants.FDN, moFdn);
    }

    def buildJobPO(final boolean isActivityCompleted){
        jobProperties.put(UpgradeActivityConstants.SWP_NAME, "CXP102051_1_R4D71");
        jobConfiguration.putAt(UpgradeActivityConstants.SWP_NAME, "CXP102051_1_R4D71");
        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration);
        PersistenceObject mainJobPo = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.JOB).addAttributes(mainJobAttributes).build();
        mainJobId = mainJobPo.getPoId()
        addNeJob(nodeName, mainJobId, isActivityCompleted);
        buildNeJobStaticData()
        buildNetworkElementData();
    }

    def addNeJob(final String nodeName, final long mainJobId, final boolean isActivityCompleted) {
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(ShmConstants.MAINJOBID, mainJobId)
        attributeMap.put(ShmConstants.NE_NAME,nodeName);

        List<Map<String, Object>> jobPropertyList = new ArrayList<>()
        Map<String,Object> neJobProperties = new HashMap<>()
        if(ACTIVITY_NAME_UPLOAD.equals(activityName)){
            neJobProperties.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.UPLOAD_CV_NAME)
            neJobProperties.put(ActivityConstants.JOB_PROP_VALUE, "testbackup1")
        }else if(ACTIVITY_NAME_UPGRADE.equals(activityName)){
            neJobProperties.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN)
            neJobProperties.put(ActivityConstants.JOB_PROP_VALUE, moFdn)
        }
        jobPropertyList.add(neJobProperties)
        attributeMap.put("jobProperties",jobPropertyList)

        PersistenceObject neJobPo = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.NE_JOB).addAttributes(attributeMap).build();
        neJobId = neJobPo.getPoId()
        addActivityJob(isActivityCompleted);
    }

    def addActivityJob(final boolean isActivityCompleted) {
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(ShmConstants.ACTIVITY_NE_JOB_ID, neJobId);
        attributeMap.put(ShmConstants.NE_NAME, nodeName);
        final double progressPercentage = 20.0;
        attributeMap.put(ShmConstants.PROGRESSPERCENTAGE,progressPercentage);
        List<Map<String, Object>> jobPropertyList = new ArrayList<>()
        Map<String,Object> activityJobProperties = new HashMap<>()
        activityJobProperties.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_TRIGGERED)
        activityJobProperties.put(ActivityConstants.JOB_PROP_VALUE, activityName)
        jobPropertyList.add(activityJobProperties)
        if(ACTIVITY_NAME_UPLOAD.equals(activityName)){
            activityJobProperties = new HashMap<>()
            activityJobProperties.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID)
            activityJobProperties.put(ActivityConstants.JOB_PROP_VALUE, "1")
            jobPropertyList.add(activityJobProperties)
            activityJobProperties = new HashMap<>()
            activityJobProperties.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.PROCESSED_BACKUPS)
            activityJobProperties.put(ActivityConstants.JOB_PROP_VALUE, "1")
            jobPropertyList.add(activityJobProperties)
        }
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

    def buildNeJobStaticData(){
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "1234", platform, 5L, null)
        if("exportcv".equals(activityName)){
            neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY) >> neJobStaticData
        }else{
            neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData
        }
    }

    def buildNetworkElementData(){
        networkElementRetrievalBean.getNetworkElementData(nodeName) >> networkElementData
        networkElementData.getNeType() >> neType
        networkElementData.getNodeModelIdentity() >> "ModelIndentity"
        networkElementData.getOssModelIdentity() >> "OssModelIdentity"
        ossModelInfoProvider.getOssModelInfo(neType, "OssModelIdentity") >> ossModelInfoList
    }

    def buildResponseDataForBackup(final String currentDetailedActivity, final String mainActionResult){
        final Map<String, Object> moAttributesMap = new HashMap<>();
        moAttributesMap.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, currentDetailedActivity);
        final Map<String, Object> actionResultData = new HashMap<>();
        actionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_CV_NAME, "testbackup1");
        actionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_INVOKED_ACTION, "export")
        actionResultData.put(ConfigurationVersionMoConstants.ACTION_ID, 1);
        actionResultData.put(ConfigurationVersionMoConstants.ACTION_RESULT_MAIN_RESULT, mainActionResult)
        moAttributesMap.put(ConfigurationVersionMoConstants.ACTION_RESULT, actionResultData);
        responseAttributes.put(ShmConstants.MO_ATTRIBUTES, moAttributesMap);
        responseAttributes.put(ShmConstants.FDN, cvMoFdn);
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
}
