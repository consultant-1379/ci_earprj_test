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
package com.ericsson.oss.services.shm.es.impl.axe.upgrade

import org.codehaus.jackson.map.ObjectMapper

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType
import com.ericsson.oss.services.shm.common.wfs.WfsRetryConfigurationParamProvider
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.job.service.BatchParameterChangeListener
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobservice.common.PlatformProperty
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator
import com.ericsson.oss.services.shm.topology.axe.rest.api.AxeNodeTopologyResponse
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier


public abstract class AxeAbstractUpgradeTest extends CdiSpecification{

    @MockedImplementation
    protected ActivityJobTBACValidator activityJobTBACValidator;

    @MockedImplementation
    protected JobStaticDataProvider jobStaticDataProvider;

    @MockedImplementation
    protected NeJobStaticDataProvider neJobStaticDataProvider;

    @MockedImplementation
    private BatchParameterChangeListener batchParameterChangeListener;

    @MockedImplementation
    WfsRetryConfigurationParamProvider wfsRetryConfigurationParamProvider;

    @MockedImplementation
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)


    def Map<String, Object> activitySchedule= new HashMap<>();
    def activityJobId;
    def mainJobId;
    def neJobId;
    def templateJobId;
    def neJobId1;

    def NEJobStaticData neJobStaticData = new NEJobStaticData(3L, 2L, "MSC-BC-BSP-01__BC01", "1234", "AXE", 5L,"MSC-BC-BSP-01");

    def JobStaticData jobStaticData= new JobStaticData("administrator",activitySchedule,"Immediate",JobType.UPGRADE,"TEST_USER")
    def Map<String,Object> jobAttributes= new HashMap<>();
    Map<String, Object> jobConfigurationDetails = new HashMap<>();


    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.shared")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.jobs")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.nejob")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.networkelement")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.cache")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.axe")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.api")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common.job.utils")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es.upgrade.remote")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    def buildPo(){

        final Map<String, Object> templateJobMap = new HashMap<>();
        templateJobMap.put(ShmConstants.JOBCONFIGURATIONDETAILS, getJobConfigurationDetailsMap());
        PersistenceObject templateJobPo=runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type(ShmJobConstants.JOBTEMPLATEID).addAttributes(templateJobMap).build();
        templateJobId=templateJobPo.getPoId();

        final Map<String, Object> attributesMapForMainJob = new HashMap<>();
        attributesMapForMainJob.put(ShmConstants.JOBTEMPLATEID, templateJobId);
        attributesMapForMainJob.put(ShmConstants.JOBCONFIGURATIONDETAILS, getJobConfigurationDetailsMap());
        attributesMapForMainJob.put(ShmConstants.JOBPROPERTIES, getMainJobProperties())
        PersistenceObject mainJobPo=runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type(ShmJobConstants.MAIN_JOB_ID).addAttributes(attributesMapForMainJob).build();
        mainJobId=mainJobPo.getPoId()

        final Map<String, Object> neJobAttributesMap = new HashMap<>();
        neJobAttributesMap.put(ShmConstants.MAIN_JOB_ID, mainJobId)
        neJobAttributesMap.put(ShmConstants.AXE_NES,"MSC-BC-BSP-01__BC01");
        neJobAttributesMap.put("neName","MSC-BC-BSP-01__BC01#ParentName_MSC-BC-BSP-01");
        neJobAttributesMap.put(ShmConstants.NETYPE, "MSC-BC-BSP");
        Map<String, Object> jobParameters1= new HashMap<>();
        Map<String, Object> jobParameters2= new HashMap<>();
        Map<String, Object> jobParameters3= new HashMap<>();
        Map<String, Object> jobParameters4= new HashMap<>();
        jobParameters1.put(ShmConstants.KEY, ShmConstants.IS_COMPONENT_JOB)
        jobParameters1.put(ShmConstants.VALUE, true)
        jobParameters2.put(ShmConstants.KEY, ShmConstants.PARENT_NAME)
        jobParameters2.put(ShmConstants.VALUE, "MSC-BC-BSP-01")
        jobParameters3.put(ShmConstants.KEY, ShmConstants.CANCELLEDBY)
        jobParameters3.put(ShmConstants.VALUE, "administrator")
        jobParameters4.put(ShmConstants.KEY, ActivityConstants.ACTIVITY_RESULT)
        jobParameters4.put(ShmConstants.VALUE, "true")
        jobParameters4.put(ShmConstants.KEY, ShmConstants.AXE_NES)
        jobParameters4.put(ShmConstants.VALUE, "MSC-BC-BSP-01__BC01#ParentName_MSC-BC-BSP-01,MSC-BC-BSP-01__BC02#ParentName_MSC-BC-BSP-01")

        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        jobProperties.add(jobParameters1)
        jobProperties.add(jobParameters2)
        jobProperties.add(jobParameters3)
        jobProperties.add(jobParameters4)
        neJobAttributesMap.put(ShmConstants.JOBPROPERTIES, jobProperties)
        PersistenceObject neJobAttributesPo=runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type(ShmJobConstants.NE_JOB).addAttributes(neJobAttributesMap).build();
        neJobId=neJobAttributesPo.getPoId();

        batchParameterChangeListener.getJobDetailsQueryBatchSize() >> 100;
        wfsRetryConfigurationParamProvider.getWfsRetryCount()>>2;
        wfsRetryConfigurationParamProvider.getWfsWaitIntervalInMS()>>1500
        workflowInstanceNotifier.executeWorkflowQuery(_)>>Collections.emptyList()
    }

    def addNetworkElementMOs(){

        runtimeDps.addManagedObject().withFdn("NetworkElement="+"MSC-BC-BSP-01")
                .addAttribute("NetworkElementId","MSC-BC-BSP-01")
                .addAttribute('neType', "MSC-BC-BSP")
                .addAttribute("ossModelIdentity","17A-R2YX")
                .addAttribute("nodeModelIdentity","17A-R2YX")
                .addAttribute('ossPrefix',"SubNetwork="+"MSC-BC-BSP-01"+",MeContext="+"MSC-BC-BSP-01")
                .addAttribute("utcOffset","utcOffset")
                .addAttribute("timeZone","timeZone")
                .namespace('OSS_NE_DEF')
                .version("2.0.0")
                .type("NetworkElement")
                .build()

        runtimeDps.addManagedObject().withFdn("NetworkElement="+"MSC-BC-BSP-01"+",CmFunction=1")
                .addAttribute("syncStatus","SYNCHRONIZED")
                .type("NetworkElement")
                .build()
    }

    final List<Map<String, String>> getMainJobProperties(){
        List<Map<String, String>> jobPropertiesList = new ArrayList<>()
        final Map<String, Object> jobPropertyMap1 = new HashMap<String, Object>()
        jobPropertyMap1.put("key", "jobName")
        jobPropertyMap1.put("value", "UpgradeJob_administrator_21112018120052")
        final Map<String, Object> jobPropertyMap2 = new HashMap<String, Object>()
        jobPropertyMap2.put("key", "submittedNEs")
        jobPropertyMap2.put("value", "2")
        final Map<String, Object> jobPropertyMap3 = new HashMap<String, Object>()
        jobPropertyMap3.put("key", "allNeJobsCreated")
        jobPropertyMap3.put("value", "TRUE")
        final Map<String, Object> jobPropertyMap4 = new HashMap<String, Object>()
        jobPropertyMap4.put("key", "jobCategory")
        jobPropertyMap4.put("value", "UI")
        final Map<String, Object> jobPropertyMap5 = new HashMap<String, Object>()
        jobPropertyMap5.put("key", "neCompleted")
        jobPropertyMap5.put("value", "")
        jobPropertiesList.add(jobPropertyMap1)
        jobPropertiesList.add(jobPropertyMap2)
        jobPropertiesList.add(jobPropertyMap3)
        jobPropertiesList.add(jobPropertyMap4)
        jobPropertiesList.add(jobPropertyMap5)
        return jobPropertiesList
    }

    final Map<String, Object> getJobConfigurationDetailsMap() {
        final Map<String, Object> jobConfigurationDetailsMap = new HashMap<>();
        jobConfigurationDetailsMap.put(ShmConstants.JOBPROPERTIES, getJobProperties());
        jobConfigurationDetailsMap.put("neJobProperties", getNejobProperties());
        jobConfigurationDetailsMap.put(ShmConstants.NETYPEJOBPROPERTIES, getNeTypeJobProperties())
        jobConfigurationDetailsMap.put(ShmConstants.PLATFORMJOBPROPERTIES, getPlatformJobProperties());
        jobConfigurationDetailsMap.put("selectedNEs", getNeInfo());
        jobConfigurationDetailsMap.put("mainSchedule", getScheduleAttributes());
        jobConfigurationDetailsMap.put("activities", getActivities());
        jobConfigurationDetailsMap.put(ShmConstants.NETYPE_ACTIVITYJOBPROPERTIES, getActivityJobProperties())
        return jobConfigurationDetailsMap
    }
    final List<Map<String, Object>> getActivityJobProperties() {
        final List<Map<String, Object>> neTypeActivityJobProperties = new ArrayList<>()
        final Map<String, Object> neTypeActivityJobPropertyMap = new HashMap<String, Object>()
        neTypeActivityJobPropertyMap.put(ShmConstants.ACTIVITYJOB_PROPERTIES, getScriptActivityJobProperties())
        neTypeActivityJobPropertyMap.put(ShmConstants.NETYPE,"MSC-BC-BSP")
        neTypeActivityJobProperties.add(neTypeActivityJobPropertyMap)
        return neTypeActivityJobProperties
    }

    final List<Map<String, String>> getScriptActivityJobProperties(){

        final List<Map<String, Object>> activityJobPropertiesList = new ArrayList<>()

        Map<String, Object> activityJobProperties1 = new HashMap<>();
        activityJobProperties1.put("activityName", "activity1")
        activityJobProperties1.put(ShmConstants.JOBPROPERTIES, getnonSynJobPropList())
        activityJobPropertiesList.add(activityJobProperties1)

        Map<String, Object> activityJobProperties2 = new HashMap<>();
        activityJobProperties2.put("activityName", "activity2")
        activityJobProperties2.put(ShmConstants.JOBPROPERTIES, getSyncJobPropList())
        activityJobPropertiesList.add(activityJobProperties2)

        return activityJobPropertiesList
    }

    final List<Map<String, String>> getnonSynJobPropList(){
        List<Map<String, String>> jobPropertiesList = new ArrayList<>()
        final Map<String, Object> jobPropertyMap1 = new HashMap<String, Object>()
        jobPropertyMap1.put("key", "Synchronous")
        jobPropertyMap1.put("value", "false")
        final Map<String, Object> jobPropertyMap2 = new HashMap<String, Object>()
        jobPropertyMap2.put("key", "Script")
        jobPropertyMap2.put("value", "OPS/enm_test.ccf")
        final Map<String, Object> jobPropertyMap3 = new HashMap<String, Object>()
        jobPropertyMap3.put("key", "_POPUP_WINDOWS")
        jobPropertyMap3.put("value", "N")
        final Map<String, Object> jobPropertyMap4 = new HashMap<String, Object>()
        jobPropertyMap4.put("key", "SWP_NAME")
        jobPropertyMap4.put("value", "Dim_Pkg_ENM_TEST_PACK_MSC__A")
        final Map<String, Object> jobPropertyMap5 = new HashMap<String, Object>()
        jobPropertyMap5.put("key", "network_element")
        jobPropertyMap5.put("value", "")
        final Map<String, Object> jobPropertyMap6 = new HashMap<String, Object>()
        jobPropertyMap6.put("key", "productRevision")
        jobPropertyMap6.put("value", "A")
        final Map<String, Object> jobPropertyMap7 = new HashMap<String, Object>()
        jobPropertyMap7.put("key", "productName")
        jobPropertyMap7.put("value", "Dim_Pkg_ENM TEST PACK MSC")
        jobPropertiesList.add(jobPropertyMap1)
        jobPropertiesList.add(jobPropertyMap2)
        jobPropertiesList.add(jobPropertyMap3)
        jobPropertiesList.add(jobPropertyMap4)
        jobPropertiesList.add(jobPropertyMap5)
        jobPropertiesList.add(jobPropertyMap6)
        jobPropertiesList.add(jobPropertyMap7)
        return jobPropertiesList
    }

    def List<Map<String, Object>> getSyncJobPropList() {
        List<Map<String, Object>> jobProplist = new ArrayList<>();
        final Map<String, Object> jobPropertyMap1 = new HashMap<String, Object>()
        jobPropertyMap1.put("key", "Synchronous")
        jobPropertyMap1.put("value", "true")
        jobProplist.add(jobPropertyMap1)
        return jobProplist
    }

    final List<Map<String, Object>> getNejobProperties(){
        final List<Map<String, Object>> neJobProperties = new ArrayList()
        final Map<String, Object> neJobPropertyMap = new HashMap<>()
        neJobPropertyMap.put(ShmJobConstants.JOBPROPERTIES, getScriptNeJobProperties())
        neJobPropertyMap.put(ShmConstants.NE_NAME,"MSC-BC-BSP-01")
        neJobProperties.add(neJobPropertyMap)
        return neJobProperties
    }

    final List<Map<String, String>> getScriptNeJobProperties(){
        List<Map<String, String>> jobPropertiesList = new ArrayList<>()
        final Map<String, Object> jobPropertyMap1 = new HashMap<String, Object>()
        jobPropertyMap1.put("key", "Synchronous")
        jobPropertyMap1.put("value", "false")
        jobPropertiesList.add(jobPropertyMap1)
        return jobPropertiesList
    }

    final List<Map<String, Object>> getNeTypeJobProperties(){
        final List<Map<String, Object>> neTypeJobProperties = new ArrayList<Map<String, Object>>()
        final Map<String, Object> neTypeJobPropertyMap= new HashMap<>()
        neTypeJobPropertyMap.put(ShmConstants.JOBPROPERTIES, getJobProperties())
        neTypeJobPropertyMap.put(ShmConstants.NETYPE, "MSC-BC-BSP")
        neTypeJobProperties.add(neTypeJobPropertyMap)
        return neTypeJobProperties
    }

    final List<Map<String, String>> getJobProperties(){
        List<Map<String, String>> jobPropertiesList = new ArrayList<>()
        final Map<String, Object> jobPropertyMap1 = new HashMap<String, Object>()
        jobPropertyMap1.put("key", "SWP_NAME")
        jobPropertyMap1.put("value", "Dim_Pkg_ENM_TEST_PACK_MSC__A")
        final Map<String, Object> jobPropertyMap2 = new HashMap<String, Object>()
        jobPropertyMap2.put("key", "productNumber")
        jobPropertyMap2.put("value", "")
        final Map<String, Object> jobPropertyMap3 = new HashMap<String, Object>()
        jobPropertyMap3.put("key", "productRevision")
        jobPropertyMap3.put("value", "A")
        final Map<String, Object> jobPropertyMap4 = new HashMap<String, Object>()
        jobPropertyMap4.put("key", "productName")
        jobPropertyMap4.put("value", "Dim_Pkg_ENM TEST PACK MSC")
        final Map<String, Object> jobPropertyMap5 = new HashMap<String, Object>()
        jobPropertyMap5.put("key", "network_element")
        jobPropertyMap5.put("value", "")
        jobPropertiesList.add(jobPropertyMap1)
        jobPropertiesList.add(jobPropertyMap2)
        jobPropertiesList.add(jobPropertyMap3)
        jobPropertiesList.add(jobPropertyMap4)
        jobPropertiesList.add(jobPropertyMap5)
        return jobPropertiesList
    }

    final List<Map<String, Object>> getPlatformJobProperties(){
        final List<Map<String, Object>>  platformJobProperties = new ArrayList<Map<String, Object>>()
        final Map<String, Object> platformJobPropertyMap = new HashMap<>()
        final PlatformProperty platformProperty = new PlatformProperty()
        platformProperty.setPlatform("AXE")
        final List<Map<String, String>> jobProperties = getJobProperties()
        platformProperty.setJobProperties(jobProperties)
        platformJobPropertyMap.put("platformTypeJobProperties", platformProperty )
        platformJobProperties.add(platformJobPropertyMap)
        return platformJobProperties
    }

    def Map<String, Object> getNeInfo() {
        final Map<String, Object> neInfo = new HashMap<String, Object>()
        neInfo.put("collectionNames", Arrays.asList("collection1"))
        neInfo.put("neNames", Arrays.asList("neName"))
        neInfo.put(ShmConstants.SAVED_SEARCH_IDS, Arrays.asList("savedSearchId"))
        return neInfo
    }
    def Map<String, Object> getScheduleAttributes() {
        final List<Map<String, Object>> schedulePropertyList = new ArrayList<>()
        final Map<String, Object> schedulePropertyMap = new HashMap<String, Object>()
        schedulePropertyMap.put("name", "schedule")
        schedulePropertyMap.put("value", "nextday")
        schedulePropertyList.add(schedulePropertyMap)
        final Map<String, Object> scheduleMap = new HashMap<String, Object>()
        scheduleMap.put("execMode", "SCHEDULED")
        scheduleMap.put("scheduleAttributes", schedulePropertyList)
        return scheduleMap
    }
    def List<Map<String, Object>> getActivities() {
        final List<Map<String, Object>> activitiesMapList = new ArrayList<>()
        final Map<String, Object> activityMap1 = new HashMap<>()
        activityMap1.put("name", "activity1")
        activityMap1.put("order", 1)
        activityMap1.put("platform", "AXE")
        activityMap1.put("schedule", getScheduleAttributes())
        activityMap1.put("neType", "MSC-BC-BSP")
        final Map<String, Object> activityMap2 = new HashMap<>()
        activityMap2.put("name", "activity2")
        activityMap2.put("order", 2)
        activityMap2.put("platform", "AXE")
        activityMap2.put("neType", "MSC-BC-BSP")
        activitiesMapList.add(activityMap1)
        activitiesMapList.add(activityMap2)
        return activitiesMapList
    }

    def  Map<String, Object> getOpsResponseAttributes(status,progressPercentage) {
        final Map<String, Object> OpsResponseAttributes = new HashMap<>()
        OpsResponseAttributes.put(AxeUpgradeActivityConstants.ACTIVITY_ID, activityJobId)
        OpsResponseAttributes.put(ActivityConstants.OPS_SESSION_ID, 1)
        OpsResponseAttributes.put(ActivityConstants.OPS_CLUSTER_ID,"svc-1-ops")
        OpsResponseAttributes.put(AxeUpgradeActivityConstants.STATUS, status)
        OpsResponseAttributes.put(SHMCapabilities.UPGRADE_JOB_CAPABILITY, "UPGRADE")
        OpsResponseAttributes.put(ActivityConstants.PROGRESS_PERCENTAGE, progressPercentage)
        return OpsResponseAttributes;
    }

    def buildActivityjobs(timeout,isCancelTriggered ,Activity1ResultForNe1,Activity1ResultForNe2,Activity2ResultForNe1,Activity2ResultForNe2,Activity1StateForNe1,Activity1StateForNe2,Activity2StateForNe1,Activity2StateForNe2,ne2Result){

        final Map<String, Object> neJobAttributesMap1 = new HashMap<>();
        neJobAttributesMap1.put(ShmConstants.MAIN_JOB_ID, mainJobId)
        neJobAttributesMap1.put(ShmConstants.AXE_NES,"MSC-BC-BSP-01__BC02");
        neJobAttributesMap1.put("neName","MSC-BC-BSP-01__BC02#ParentName_MSC-BC-BSP-01");
        neJobAttributesMap1.put(ShmConstants.NETYPE, "MSC-BC-BSP");
        neJobAttributesMap1.put(ShmConstants.BUSINESS_KEY, "2@MSC-BC-BSP-01");
        neJobAttributesMap1.put("result",ne2Result)
        PersistenceObject neJobAttributesPo1=runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type(ShmJobConstants.NE_JOB).addAttributes(neJobAttributesMap1).build();
        neJobId1=neJobAttributesPo1.getPoId();

        final Map<String, Object> attributesMap = new HashMap<>();
        attributesMap.put(ShmConstants.ACTIVITY_NAME,"activity1");
        attributesMap.put(ShmConstants.ORDER,1);
        attributesMap.put(ShmConstants.PROGRESSPERCENTAGE,2.0d);
        attributesMap.put(ShmConstants.NE_JOB_ID,neJobId);
        attributesMap.put("result", Activity1ResultForNe1);
        attributesMap.put("state", Activity1StateForNe1)
        Map<String, Object> jobParametersForActivityId1= new HashMap<>();
        jobParametersForActivityId1.put(ShmConstants.KEY, AxeUpgradeActivityConstants.IS_AXE_HANDLETIMEOUT_TRIGGERED)
        jobParametersForActivityId1.put(ShmConstants.VALUE, timeout)
        Map<String, Object> jobParametersForActivityId2= new HashMap<>();
        jobParametersForActivityId2.put(ShmConstants.KEY, AxeUpgradeActivityConstants.RESPONSE_TIMESTAMP_ATTRIBUTE)
        jobParametersForActivityId2.put(ShmConstants.VALUE,"true")
        final List<Map<String, Object>> jobParametersForActivityIdList = new ArrayList<>();
        Map<String, Object> jobParametersForActivityId3= new HashMap<>();
        jobParametersForActivityId3.put(ShmConstants.KEY, ActivityConstants.OPS_CLUSTER_ID)
        jobParametersForActivityId3.put(ShmConstants.VALUE,"svc-1-ops")
        Map<String, Object> jobParametersForActivityId4= new HashMap<>();
        jobParametersForActivityId4.put(ShmConstants.KEY, ActivityConstants.OPS_SESSION_ID)
        jobParametersForActivityId4.put(ShmConstants.VALUE,"1")
        if(isCancelTriggered.equals("true")){
            Map<String, Object> jobParametersForActivityId5= new HashMap<>();
            jobParametersForActivityId5.put(ShmConstants.KEY, ActivityConstants.IS_CANCEL_TRIGGERED)
            jobParametersForActivityId5.put(ShmConstants.VALUE,"true")
            jobParametersForActivityIdList.add(jobParametersForActivityId5)
        }
        jobParametersForActivityIdList.add(jobParametersForActivityId1)
        jobParametersForActivityIdList.add(jobParametersForActivityId2)
        jobParametersForActivityIdList.add(jobParametersForActivityId3)
        jobParametersForActivityIdList.add(jobParametersForActivityId4)
        attributesMap.put(ShmConstants.JOBPROPERTIES,jobParametersForActivityIdList)
        PersistenceObject activityJobPo=runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.ACTIVITY_JOB).addAttributes(attributesMap).build();
        activityJobId = activityJobPo.getPoId();

        final Map<String, Object> attributesMapForActivityJob1 = new HashMap<>();
        attributesMapForActivityJob1.put("neJobId", neJobId1)
        attributesMapForActivityJob1.put("order", 1)
        attributesMapForActivityJob1.put("result", Activity1ResultForNe2)
        attributesMapForActivityJob1.put(ShmConstants.PROGRESSPERCENTAGE,0.0d);
        attributesMapForActivityJob1.put("state", Activity1StateForNe2)
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type(ShmJobConstants.ACTIVITY_JOB).addAttributes(attributesMapForActivityJob1).build();

        final Map<String, Object> attributesMapForActivityJob2 = new HashMap<>();
        attributesMapForActivityJob2.put("neJobId", neJobId)
        attributesMapForActivityJob2.put("order", 2)
        attributesMapForActivityJob2.put("result", Activity2ResultForNe1)
        attributesMapForActivityJob2.put(ShmConstants.PROGRESSPERCENTAGE,0.0d);
        attributesMapForActivityJob2.put("state", Activity2StateForNe1)
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type(ShmJobConstants.ACTIVITY_JOB).addAttributes(attributesMapForActivityJob2).build();

        final Map<String, Object> attributesMapForActivityJob3 = new HashMap<>();
        attributesMapForActivityJob3.put("neJobId", neJobId1)
        attributesMapForActivityJob3.put("order", 2)
        attributesMapForActivityJob3.put("result", Activity2ResultForNe2)
        attributesMapForActivityJob3.put(ShmConstants.PROGRESSPERCENTAGE,0.0d);
        attributesMapForActivityJob3.put("state", Activity2StateForNe2)
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type(ShmJobConstants.ACTIVITY_JOB).addAttributes(attributesMapForActivityJob3).build();
    }

    def AxeNodeTopologyResponse prepareTopologyResponse(){
        AxeNodeTopologyResponse axeNodeTopologyResponse=  new ObjectMapper().readValue("{\"nodeTopology\":[{\"nodeName\":\"MSC-BC-BSP-01\",\"neType\":\"MSC-BC-BSP\",\"axeClusterName\":\"MSC-BC-BSP-01-AXE_CLUSTER\",\"components\":[{\"name\":\"MSC-BC\",\"cpNames\":[\"BC1\",\"BC2\",\"BC3\",\"BC4\"]},{\"name\":\"SPX\",\"cpNames\":[\"CP1\",\"CP2\"]}],\"numberOfAPG\":2}],\"failureReason\":[],\"nodesWithoutComponents\":[]}",AxeNodeTopologyResponse.class);
        return axeNodeTopologyResponse;
    }

    def AxeNodeTopologyResponse prepareTopologyFailureResponse(){
        AxeNodeTopologyResponse axeNodeTopologyResponse=  new ObjectMapper().readValue("{\"nodeTopology\":[],\"failureReason\":[{\"userMessage\": \"Cannot connect to Network Element.\",\"nodeNames\": [\"MSC-BC-BSP-01\"]}],\"nodesWithoutComponents\":[]}",AxeNodeTopologyResponse.class);
        return axeNodeTopologyResponse;
    }
}
