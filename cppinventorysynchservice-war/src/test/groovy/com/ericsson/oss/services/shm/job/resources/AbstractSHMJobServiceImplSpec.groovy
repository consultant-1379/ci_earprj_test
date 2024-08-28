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
package com.ericsson.oss.services.shm.job.resources

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.common.InventoryQueryConfigurationProvider
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementIdResponse
import com.ericsson.oss.services.shm.job.service.SHMJobService
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory

/**
 * Class to provide input data like Job Template info
 * 
 * @author xkalkil
 *
 */
public class AbstractSHMJobServiceImplSpec extends CdiSpecification {
    @MockedImplementation
    private SHMJobService shmJobService;
    @MockedImplementation
    protected InventoryQueryConfigurationProvider inventoryQueryConfigurationListener;

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps);

    protected static final List<String> BOTH_ACTIVITIES = Arrays.asList("createbackup", "uploadbackup")
    protected static final List<String> SINGLE_ACTIVITY = Arrays.asList("createbackup")
    private static final String PARENT_NENAME = "MSC18"
    protected static final List<String> COMPONENTS = Arrays.asList("MSC18-AXE_CLUSTER","MSC18__APG1","MSC18__APG2", "MSC18__CP2")

    private static final String PLATFORMTYPE_AXE = "AXE";
    private static final String CREATEBACKUP = "createbackup"
    protected static final String NETYPE ="MSC-BC-IS"
    private static final String EXEMODE_IMMEDIATE ="IMMEDIATE"

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.addProxyController(new EjbProxyController(true))
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.jobservice.common")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.jobs.common")
    }

    long getJobTemplatePOId(final String neType, final List<String> activities, final List<String> components) {
        Map templateData = new HashMap();
        templateData.put(ShmConstants.NAME, "SampleTemplatePO");
        templateData.put(ShmConstants.CREATION_TIME, new Date());
        templateData.put(ShmConstants.JOB_CATEGORY, JobCategory.UI.toString())
        templateData.put(ShmConstants.OWNER, "admin");
        templateData.put(ShmConstants.JOB_TYPE, JobTypeEnum.BACKUP.toString());

        Map selectedNEs = new HashMap<>()
        selectedNEs.put("neNames", Arrays.asList(PARENT_NENAME))
        selectedNEs.put("neWithComponentInfo", prepareNeNamesWithSelectedComponents(components))
        selectedNEs.put("neTypeComponentActivityDetails", getNeTypeComponentActivityDetails(components, activities))

        Map jobConfiguration = new HashMap<>()
        jobConfiguration.put("selectedNEs", selectedNEs)
        jobConfiguration.put("activities", getActivities(activities, neType))
        Map<String, Object>  scheduleInfo = new HashMap<>()
        scheduleInfo.put("execMode", EXEMODE_IMMEDIATE)
        jobConfiguration.put("mainSchedule", scheduleInfo)

        templateData.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfiguration);
        PersistenceObject po = runtimeDps.addPersistenceObject().namespace("shm").type("JobTemplate").addAttributes(templateData).build();
        long poId = po.getPoId();
        return poId;
    }

    Map<String, Object> getJobTemplate(final long poId){
        PersistenceObject po = runtimeDps.stubbedDps.liveBucket.findPoById(poId)
        return po.getAllAttributes()
    }

    List<Map<String, Object>> prepareNeNamesWithSelectedComponents( final List<String> components){
        List<Map<String, Object>> neNamesWithSelectedComponentList = new ArrayList<>()
        Map<String, Object> neNamesWithSelectedComponents = new HashMap<>()
        neNamesWithSelectedComponents.put("neName", PARENT_NENAME)
        neNamesWithSelectedComponents.put("selectedComponenets", components)
        neNamesWithSelectedComponentList.add(neNamesWithSelectedComponents)
        return neNamesWithSelectedComponentList
    }

    List<Map<String, Object>> getNeTypeComponentActivityDetails( final List<String> components, final List<String> activities){
        List<Map<String, Object>> neTypeComponentActivityDetailList = new ArrayList<>();
        Map<String, Object> neTypeComponentActivityDetails = new HashMap<>()
        neTypeComponentActivityDetails.put("neType", NETYPE)
        List<Map<String, Object>> componentActivities = new ArrayList<>()
        Map<String, Object> componentActivity = new HashMap<>()
        for(String component : components) {
            componentActivity.put("componentName", component)
            componentActivity.put("activityNames", activities)
            componentActivities.add(componentActivity)
        }
        neTypeComponentActivityDetails.put("componentActivities",componentActivities)
        neTypeComponentActivityDetailList.add(neTypeComponentActivityDetails)
        return neTypeComponentActivityDetailList
    }


    List<Map<String, Object>> getActivities(final List<String> activities, final String neType){
        final List<Map<String, Object>> activityList = new ArrayList<>()
        int i=1;
        for(String activitySelected : activities) {
            Map<String, Object>  activitySchedule = new HashMap<>()
            activitySchedule.put("schedule", getScheduleInfo())
            activitySchedule.put("neType", neType)
            activitySchedule.put("platform", PLATFORMTYPE_AXE)
            activitySchedule.put("order", i++)
            activitySchedule.put("execMode", EXEMODE_IMMEDIATE)
            activityList.add(activitySchedule)
        }
        return activityList
    }

    Map<String, Object>  getScheduleInfo(){
        Map<String, Object>  scheduleInfo = new HashMap<>()
        scheduleInfo.put("execMode", EXEMODE_IMMEDIATE)
        return scheduleInfo;
    }


    NetworkElementIdResponse getNetworkElementPoIds(List<String> neName) {
        List<Long> poId=new ArrayList<>();
        poId.add(4);
        poId.add(5);
        final NetworkElementIdResponse networkElementIdResponce;
        networkElementIdResponce=new NetworkElementIdResponse( poId,null,0)
        shmJobService.getNetworkElementPoIds(_) >> networkElementIdResponce
        return networkElementIdResponce;
    }

    NetworkElementIdResponse getNetworkElementPoidsnotindps(final List<String> nodenames) {
        List<Long> poId=new ArrayList<>();
        final NetworkElementIdResponse networkElementIdResponce;
        networkElementIdResponce=new NetworkElementIdResponse( poId,"Network Elements Not Found",13199)
        shmJobService.getNetworkElementPoIds(_) >> networkElementIdResponce;
        return networkElementIdResponce;
    }
}
