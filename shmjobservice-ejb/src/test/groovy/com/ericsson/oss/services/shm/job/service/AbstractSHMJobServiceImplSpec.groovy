/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.services.shm.common.FdnServiceBean
import com.ericsson.oss.services.shm.common.InventoryQueryConfigurationProvider
import com.ericsson.oss.services.shm.common.UserContextBean
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementIdResponse
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.jobs.common.modelentities.ComponentActivity
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory
import com.ericsson.oss.services.shm.jobs.common.modelentities.NeTypeComponentActivityDetails
import com.ericsson.oss.services.shm.inventory.remote.axe.api.AxeApgProductRevisionProvider
import com.ericsson.oss.services.shm.jobservice.common.JobInfo
import com.ericsson.oss.services.shm.jobservice.common.NeNamesWithSelectedComponents
import com.ericsson.oss.services.shm.jobservice.common.NeTypeJobProperty
import com.ericsson.oss.services.shm.jobservice.common.NeTypesInfo
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier
import com.ericsson.oss.services.shm.shared.util.EncryptAndDecryptConverter;

/**
 * Class to provide input data to execute the test classes like creation of SHM Job
 * 
 * @author xkalkil
 *
 */
class AbstractSHMJobServiceImplSpec extends CdiSpecification {

    @MockedImplementation
    protected PlatformTypeProviderImpl platformTypeProviderImpl;

    @MockedImplementation
    private UserContextBean userContextBean;

    @MockedImplementation
    private SystemRecorder systemRecorder;

    @MockedImplementation
    private WorkflowInstanceNotifier workflowInstanceHelper;

    @MockedImplementation
    private FdnServiceBean fdnServiceBean;

    @MockedImplementation
    protected InventoryQueryConfigurationProvider inventoryQueryConfigurationListener;
    
    @MockedImplementation
    private AxeApgProductRevisionProvider axeApgProductRevisionProvider;
    
    @MockedImplementation
    private EncryptAndDecryptConverter encryptAndDecryptConverter;

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps);

    private static final List<String> ACTIVITIES = Arrays.asList("createbackup", "uploadbackup")
    private static final String PARENT_NENAME = "MSC18"
    private static final List<String> COMPONENTS = Arrays.asList("MSC18-AXE_CLUSTER","MSC18__APG1","MSC18__APG2", "MSC18__CP2")

    private static final PLATFORMTYPE_AXE = "AXE";
    private static final CREATEBACKUP = "createbackup"
    private static final NETYPE ="MSC-BC-IS"
    private static final EXEMODE_IMMEDIATE ="IMMEDIATE"

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.addProxyController(new EjbProxyController(true))
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.service")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.jobservice.common")
    }

    NetworkElementIdResponse getNetworkElementPoidsInDps(final  List<String> neNames) {
        List<Long> poId=new ArrayList<>();
        poId.add(4);
        poId.add(5);
        NetworkElementIdResponse networkElementIdResponse;
        networkElementIdResponse=new NetworkElementIdResponse(poId,null,0)
        fdnServiceBean.getNetworkElementPoIds(_) >> networkElementIdResponse;
        return networkElementIdResponse;
    }

    NetworkElementIdResponse getNetworkElementPoidsNotInDps(final  List<String> neNames) {
        List<Long> poId=new ArrayList<>();
        NetworkElementIdResponse networkElementIdResponse;
        networkElementIdResponse=new NetworkElementIdResponse(poId,"Network Elements Not Found",13199)
        fdnServiceBean.getNetworkElementPoIds(_) >> networkElementIdResponse;
        return networkElementIdResponse;
    }
    final Set<String> neTypes = new HashSet<String>()

    public NeTypesInfo getInValidNeTypesInfo(){
        final NeTypesInfo neTypesInfo = new NeTypesInfo()
        neTypes.add("abc")
        neTypes.add("SGSE")
        neTypes.add("123")
        neTypesInfo.setJobType(JobTypeEnum.UPGRADE)
        neTypesInfo.setNeTypes(neTypes)
        return neTypesInfo
    }


    public NeTypesInfo getAxeNeTypesInfo(){
        final NeTypesInfo neTypesInfo = new NeTypesInfo()
        neTypes.add("MSC-BC-BSP")
        neTypes.add("MSC-BC-IS")
        neTypesInfo.setJobType(JobTypeEnum.UPGRADE)
        neTypesInfo.setNeTypes(neTypes)
        return neTypesInfo
    }


    JobInfo getJobInfo() {
        final List<Map<String, Object>> listOfActivitySchedules = new ArrayList<>()
        final Map<String, Object> schedule = new HashMap<>()

        schedule.put(ShmConstants.PLATEFORM_TYPE, PLATFORMTYPE_AXE)
        Map<String, Object> neTypeActivitySchedule =  getNeTypeActivitySchedules(ACTIVITIES, NETYPE)
        List<Map<String, Object>> neTypeActivityScheduleList = new ArrayList<>()
        neTypeActivityScheduleList.add(neTypeActivitySchedule)
        schedule.put(ShmConstants.VALUE, neTypeActivityScheduleList)

        final JobInfo jobInfo = new JobInfo();
        jobInfo.setJobType(JobTypeEnum.BACKUP);
        jobInfo.setName("jobName");
        jobInfo.setOwner("shmtest");

        jobInfo.setMainSchedule(getMainSchedule(EXEMODE_IMMEDIATE));
        final List<Map<String, Object>> fdnNames = new ArrayList<>();
        final HashMap<String, Object> fdns = new HashMap<>();
        fdns.put("name", "ne1");
        fdnNames.add(fdns);
        jobInfo.setNeNames(fdnNames);

        final List<Map<String, Object>> configurations = new ArrayList<>();
        final Map<String, Object> platformtypePro = new HashMap<String, Object>();
        platformtypePro.put("AXE", {});
        configurations.add(platformtypePro);
        jobInfo.setConfigurations(configurations);
        jobInfo.setDescription("desc")
        listOfActivitySchedules.add(schedule)
        jobInfo.setActivitySchedules(listOfActivitySchedules);
        jobInfo.setParentNeWithComponents(prepareNeNamesWithSelectedComponents())
        jobInfo.setNeTypeComponentActivityDetails(getNeTypeComponentActivityDetails())

        userContextBean.getLoggedInUserName() >> "administrator"
        platformTypeProviderImpl.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.AXE;
        workflowInstanceHelper.submitWorkFlowInstance(_, _)>> "123456"

        return jobInfo;
    }

    void buildNeTypeJobProperties(JobInfo jobInfo){
        List<NeTypeJobProperty> neTypeJobProperties = new ArrayList<>()
        NeTypeJobProperty neTypeJobProperty = new NeTypeJobProperty();
        neTypeJobProperty.setNeType("MSC-BC-BSP")
        List<Map<String,String>> jobProperties = new ArrayList<>()
        Map<String,String> eachProp = new HashMap<>()
        eachProp.put("key", "Password")
        eachProp.put("value", "12345678")
        jobProperties.add(eachProp)
        neTypeJobProperty.setJobProperties(jobProperties)
        neTypeJobProperties.add(neTypeJobProperty)
        jobInfo.setNETypeJobProperties(neTypeJobProperties)
    }
    
    List<NeNamesWithSelectedComponents> prepareNeNamesWithSelectedComponents(){
        List<NeNamesWithSelectedComponents> neNamesWithSelectedComponentList = new ArrayList<>()
        NeNamesWithSelectedComponents neNamesWithSelectedComponents = new NeNamesWithSelectedComponents()
        neNamesWithSelectedComponents.setParentNeName(PARENT_NENAME)
        neNamesWithSelectedComponents.setSelectedComponents(COMPONENTS)
        neNamesWithSelectedComponentList.add(neNamesWithSelectedComponents)
        return neNamesWithSelectedComponentList
    }

    List<NeTypeComponentActivityDetails> getNeTypeComponentActivityDetails(){
        List<NeTypeComponentActivityDetails> neTypeComponentActivityDetailList = new ArrayList<>();
        NeTypeComponentActivityDetails neTypeComponentActivityDetails = new NeTypeComponentActivityDetails()
        neTypeComponentActivityDetails.setNeType(NETYPE)
        List<ComponentActivity> componentActivities = new ArrayList<>()
        ComponentActivity componentActivity = new ComponentActivity()
        componentActivity.setComponentName((String)COMPONENTS.get(0))
        componentActivity.setActivityNames(ACTIVITIES)
        componentActivities.add(componentActivity)
        neTypeComponentActivityDetails.setComponentActivities(componentActivities)
        neTypeComponentActivityDetailList.add(neTypeComponentActivityDetails)
        return neTypeComponentActivityDetailList
    }


    Map<String, Object> getNeTypeActivitySchedules(final List<String> activities, final String neType){
        Map<String, Object> neTypeActivities = new HashMap<>()
        List<Map<String, Object>> activityList = new ArrayList<>()
        int i=1;
        for(String activitySelected : activities) {
            Map<String, Object>  activitySchedule = new HashMap<>()
            List<Map<String, Object>> schedules = new ArrayList<>()
            Map<String, Object>  scheduleInfo = new HashMap<>()
            scheduleInfo.put("execMode", EXEMODE_IMMEDIATE)
            schedules.add(scheduleInfo)
            activitySchedule.put("schedule", schedules)
            activitySchedule.put("activityName", activitySelected)
            activitySchedule.put("neType", neType)
            activitySchedule.put("platform", PLATFORMTYPE_AXE)
            activitySchedule.put("order", i++)
            activitySchedule.put("execMode", EXEMODE_IMMEDIATE)
            activityList.add(activitySchedule)
        }
        neTypeActivities.put(ShmConstants.NETYPE, neType)
        neTypeActivities.put(ShmConstants.VALUE, activityList)
        return neTypeActivities
    }

    Map<String, Object> getMainSchedule(final String exeMode){
        Map<String, Object>  mainSchedule = new HashMap<>()
        mainSchedule.put("scheduleAttributes", Collections.EMPTY_LIST)
        mainSchedule.put("execMode", exeMode)
        return mainSchedule
    }

    def long createTemplatePO(){
        Map templateData = new HashMap();
        templateData.put(ShmConstants.NAME, "SampleTemplatePO");
        templateData.put(ShmConstants.CREATION_TIME, new Date());
        templateData.put(ShmConstants.JOB_CATEGORY, JobCategory.UI.toString())
        PersistenceObject po = runtimeDps.addPersistenceObject().namespace("shm").type("JobTemplate").addAttributes(templateData).build();
        long poId = po.getPoId();
        return poId;
    }
}
