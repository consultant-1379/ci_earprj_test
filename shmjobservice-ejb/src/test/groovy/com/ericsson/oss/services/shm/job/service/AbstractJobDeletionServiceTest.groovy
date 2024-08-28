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

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants

class AbstractJobDeletionServiceTest extends CdiSpecification{
    
    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps);
    
    private static final String NODE_NAME1 = "LTE06dg2ERBS00001"
    private static final String NODE_NAME2 = "LTE06dg2ERBS00002"
    private static final String TYPE_RADIO_NODE = "RadioNode"
    private static final String OSS_MODEL_ID = "17A-R2YX"
    private static final String NODE_HEALTH_CHECK_ALARM_CAUSE = "cause";
    private static final String NODE_HEALTH_CHECK_ALARM_ADDITIONALINFO = "additionalInfo";
    private static final String NODE_HEALTH_CHECK_ALARM_PROBLEMATICMO = "problematicMo";
    private static final String NODE_HEALTH_CHECK_ALARM_SEVERITY = "severity";
    private static final String NODE_HEALTH_CHECK_ALARM_EVENT_TIME = "eventTime";
    private static final String NODE_HEALTH_CHECK_ALARM_HASH = "hash";
    
    protected long mainJobId;
    protected String hcJobFdn1;
    protected String hcJobFdn2;
    protected List<Long> neandActivityJoblst = new ArrayList<Long>();
    
    def buildJobData(final boolean isHcJobMoRequired,final boolean isAlarmDataRequired,final String jobType,final boolean isActivityJobRequired) {
        final Map<String, Object> mainJobData = new HashMap<>();
        final Map<String, Object> neJobData1 = new HashMap<>();
        final Map<String, Object> neJobData2 = new HashMap<>();
        final Map<String, Object> nodeactivityJobData = new HashMap<>();
        final Map<String, Object> enmactivityJobData = new HashMap<>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        final Map<String, Object> neTypeProp = new HashMap<>();
        final Map<String, Object> jobProp = new HashMap<>();
        final Map<String, Object> neJobProp1 = new HashMap<String, Object>();
        final Map<String, Object> neJobProp2 = new HashMap<String, Object>();
        final Map<String, Object> criticalAlarmneJobProp = new HashMap<String, Object>();
        final Map<String, Object> majorAlarmneJobProp = new HashMap<String, Object>();
        final Map<String, Object> hcJobAttributes =  new HashMap<String, Object>();
        final Map<String, Object> hcJobAttributesState =  new HashMap<String, Object>();
        final Map<String, Object> criticalAlarmPoAttributes = new HashMap<String, Object>();
        final Map<String, Object> majorAlarmPoAttributes = new HashMap<String, Object>();
        
        jobProp.put(ShmConstants.KEY, "NODE_HEALTH_CHECK_TEMPLATE")
        jobProp.put(ShmConstants.VALUE, "PRE_UPGRADE_TEMPLATE")
        jobProp.put(ShmConstants.JOB_TYPE, jobType);
        neTypeProp.put(ShmConstants.NETYPE, TYPE_RADIO_NODE)
        neTypeProp.put(ShmConstants.JOBPROPERTIES, Arrays.asList(jobProp))
        jobConfigurationDetails.put(ShmConstants.NETYPEJOBPROPERTIES, Arrays.asList(neTypeProp))
        mainJobData.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfigurationDetails);
        mainJobData.put(ShmConstants.JOB_TYPE, jobType);
        mainJobData.put(ShmConstants.JOB_TEMPLATE_ID,(long)1);
        mainJobData.put(ShmConstants.STATE,ShmConstants.COMPLETED);
        mainJobData.put(ShmConstants.EXECUTIONINDEX,1);
        
        PersistenceObject mainJob= runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.JOB).addAttributes(mainJobData).build();

        hcJobAttributesState.put(ShmConstants.STATE, ShmConstants.COMPLETED)
        hcJobAttributes.put("progressReport",hcJobAttributesState);
        
        if(isAlarmDataRequired) {
            criticalAlarmPoAttributes.put(NODE_HEALTH_CHECK_ALARM_CAUSE, "PROBLEM_1.1_%unique");
            criticalAlarmPoAttributes.put(NODE_HEALTH_CHECK_ALARM_ADDITIONALINFO, "PROBLEM_1.1_%unique");
            criticalAlarmPoAttributes.put(NODE_HEALTH_CHECK_ALARM_PROBLEMATICMO, "PROBLEM_1.1_%unique");
            criticalAlarmPoAttributes.put(NODE_HEALTH_CHECK_ALARM_SEVERITY, "critical");
            criticalAlarmPoAttributes.put(NODE_HEALTH_CHECK_ALARM_EVENT_TIME, new Date());
            criticalAlarmPoAttributes.put(ShmConstants.NODENAME, NODE_NAME1);
            
            majorAlarmPoAttributes.put(NODE_HEALTH_CHECK_ALARM_CAUSE, "PROBLEM_1.1_%unique");
            majorAlarmPoAttributes.put(NODE_HEALTH_CHECK_ALARM_ADDITIONALINFO, "PROBLEM_1.1_%unique");
            majorAlarmPoAttributes.put(NODE_HEALTH_CHECK_ALARM_PROBLEMATICMO, "PROBLEM_1.1_%unique");
            majorAlarmPoAttributes.put(NODE_HEALTH_CHECK_ALARM_SEVERITY, "major");
            majorAlarmPoAttributes.put(NODE_HEALTH_CHECK_ALARM_EVENT_TIME, new Date());
            majorAlarmPoAttributes.put(ShmConstants.NODENAME, NODE_NAME1);
            
            PersistenceObject criticalAlarmPO   = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type("NodeHealthCheckAlarm").addAttributes(criticalAlarmPoAttributes).build();
            neandActivityJoblst.add(criticalAlarmPO.getPoId());
            
            PersistenceObject majorAlarmPO   = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type("NodeHealthCheckAlarm").addAttributes(majorAlarmPoAttributes).build();
            neandActivityJoblst.add(majorAlarmPO.getPoId());
            
            criticalAlarmneJobProp.put(ShmConstants.KEY,ShmConstants.CRITICAL_ALARM_DATA);
            criticalAlarmneJobProp.put(ShmConstants.VALUE,criticalAlarmPO.getPoId().toString());
            
            
            majorAlarmneJobProp.put(ShmConstants.KEY,ShmConstants.MAJOR_ALARM_DATA);
            majorAlarmneJobProp.put(ShmConstants.VALUE,majorAlarmPO.getPoId().toString());
        }
        
        if(isHcJobMoRequired) {
            hcJobFdn1 = createHcJob(NODE_NAME1, "Test1", hcJobAttributes)
            neJobProp1.put(ShmConstants.KEY, "HcJobFdn")
            neJobProp1.put(ShmConstants.VALUE, hcJobFdn1)
            
            hcJobFdn2 = createHcJob(NODE_NAME2, "Test2", hcJobAttributes)
            neJobProp2.put(ShmConstants.KEY, "HcJobFdn")
            neJobProp2.put(ShmConstants.VALUE, hcJobFdn2)
        }
        
        mainJobId = mainJob.getPoId();
        neJobData1.put(ShmConstants.BUSINESS_KEY,NODE_NAME1+"@"+mainJob.getPoId());
        neJobData1.put(ShmConstants.MAIN_JOB_ID, mainJob.getPoId());
        neJobData1.put(ShmConstants.JOB_TYPE, jobType);
        neJobData1.put(ShmConstants.JOBPROPERTIES, Arrays.asList(neJobProp1,criticalAlarmneJobProp,majorAlarmneJobProp))
        neJobData1.put(ShmConstants.NE_NAME,NODE_NAME1);
        neJobData1.put(ShmConstants.NETYPE, TYPE_RADIO_NODE);
        neJobData1.put(ShmConstants.NEJOB_HEALTH_STATUS,"NOT_AVAILABLE");
        
        neJobData2.put(ShmConstants.BUSINESS_KEY,NODE_NAME1+"@"+mainJob.getPoId());
        neJobData2.put(ShmConstants.MAIN_JOB_ID, mainJob.getPoId());
        neJobData2.put(ShmConstants.JOB_TYPE, jobType);
        neJobData2.put(ShmConstants.JOBPROPERTIES, Arrays.asList(neJobProp2,criticalAlarmneJobProp,majorAlarmneJobProp))
        neJobData2.put(ShmConstants.NE_NAME,NODE_NAME1);
        neJobData2.put(ShmConstants.NETYPE, TYPE_RADIO_NODE);
        neJobData2.put(ShmConstants.NEJOB_HEALTH_STATUS,"NOT_AVAILABLE");

        PersistenceObject neJob1= runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.NE_JOB).addAttributes(neJobData1).build();
        PersistenceObject neJob2= runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.NE_JOB).addAttributes(neJobData2).build();
        neandActivityJoblst.add(neJob1.getPoId());
        neandActivityJoblst.add(neJob2.getPoId());
        if(isActivityJobRequired) {
            nodeactivityJobData.put(ShmConstants.ACTIVITY_NE_JOB_ID, neJob1.getPoId());
            nodeactivityJobData.put(ShmConstants.ACTIVITY_NAME, "nodeHealthCheck")
            enmactivityJobData.put(ShmConstants.ACTIVITY_NE_JOB_ID, neJob1.getPoId());
            enmactivityJobData.put(ShmConstants.ACTIVITY_NAME, "enmHealthCheck")
    
            PersistenceObject nodeActivityJob   = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.ACTIVITY_JOB).addAttributes(nodeactivityJobData).build();
            PersistenceObject enmActivityJob   = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.ACTIVITY_JOB).addAttributes(enmactivityJobData).build();
            neandActivityJoblst.add(nodeActivityJob.getPoId());
            neandActivityJoblst.add(enmActivityJob.getPoId());
            
        }
        
    }

    def String createHcJob(String nodeName,String jobName,Map hcJobAttributes){
        createHealthCheckM(nodeName)
        PersistenceObject hcJob = runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName+",ManagedElement="+nodeName+",SystemFunctions=1,HealthCheckM=1,HcJob="+jobName)
                .addAttribute('HcJobId',jobName)
                .addAttributes(hcJobAttributes)
                .namespace('RcsHcM')
                .version("1.0")
                .type("HcJob")
                .build()

        return ( (ManagedObject)hcJob).getFdn()
    }
    
    private createHealthCheckM(String nodeName) {
        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName)
                .addAttribute('SubNetworkId', nodeName)
                .namespace('OSS_TOP')
                .version("3.0.0")
                .type("SubNetwork")
                .build()
        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName)
                .addAttribute("MeContextId",nodeName)
                .addAttribute("neType", TYPE_RADIO_NODE)
                .namespace('OSS_TOP')
                .version("3.0.0")
                .type("MeContext")
                .build()
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName)
                .addAttribute("NetworkElementId",nodeName)
                .addAttribute('neType', TYPE_RADIO_NODE)
                .addAttribute("ossModelIdentity",OSS_MODEL_ID)
                .addAttribute("nodeModelIdentity",OSS_MODEL_ID)
                .addAttribute('ossPrefix',"SubNetwork="+nodeName+",MeContext="+nodeName)
                .namespace('OSS_NE_DEF')
                .version("2.0.0")
                .type("NetworkElement")
                .build()
        runtimeDps.build().getLiveBucket().findMoByFdn("NetworkElement="+nodeName).addAssociation("nodeRootRef", runtimeDps.build().getLiveBucket().findMoByFdn("SubNetwork="+nodeName))

        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName+",ManagedElement="+nodeName)
                .addAttribute('ManagedElementId',nodeName)
                .addAttribute('managedElementType ', TYPE_RADIO_NODE)
                .addAttribute('neType', TYPE_RADIO_NODE)
                .type("ManagedElement")
                .build()
        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName+",ManagedElement="+nodeName+",SystemFunctions=1,HealthCheckM=1")
                .addAttribute('healthCheckMId',1)
                .namespace('RcsHcM')
                .version("1.0")
                .type("HealthCheckM")
                .build()
    }

}
