/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.axe.service

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.exception.UnsupportedPlatformException
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.job.service.BatchParameterChangeListener
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType
import com.ericsson.oss.services.shm.jobservice.axe.OpsInputData
import com.ericsson.oss.services.shm.jobservice.axe.OpsResponseData
import com.ericsson.oss.services.shm.jobservice.axe.OpsSessionAndClusterIdInfo
import com.ericsson.oss.services.shm.jobservice.common.NEJobInfo
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants

class ShmAxeServiceImplSpec extends CdiSpecification {

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps);

    @ObjectUnderTest
    private ShmAxeServiceImpl shmAxeServiceImpl
    @MockedImplementation
    private PlatformTypeProviderImpl platformTypeProviderImpl;
    @MockedImplementation
    private BatchParameterChangeListener batchParameterChangeListener;
    @MockedImplementation
    protected JobStaticDataProvider jobStaticDataProvider;

    static final OpsInputData opsInputData=new OpsInputData();
    def Map<String, Object> activitySchedule= new HashMap<>();

    static{
        opsInputData.setJobType(JobTypeEnum.UPGRADE);
        opsInputData.setNeTypeToNeJobIds(getNeTypeToNeJobIds());
        opsInputData.setUser("administrator")
        opsInputData.setMainJobId(1L)
    }

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.addProxyController(new EjbProxyController(true))
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job")
    }

    def "verifying getSessionIdAndClusterId method response"(){
        given:"Activitiy job data"
        platformTypeProviderImpl.getPlatformTypeBasedOnCapability("MSC-BC-BSP","UpgradeJob_workflowPrefix") >> PlatformTypeEnum.AXE;
        platformTypeProviderImpl.getPlatformTypeBasedOnCapability("ERBS","UpgradeJob_workflowPrefix") >> PlatformTypeEnum.CPP;
        platformTypeProviderImpl.getPlatformTypeBasedOnCapability("ABC","UpgradeJob_workflowPrefix") >> { throw new UnsupportedPlatformException("Exception in retrieving platform type.") };
        platformTypeProviderImpl.getPlatformTypeBasedOnCapability("HLR-FE-IS","UpgradeJob_workflowPrefix") >> PlatformTypeEnum.AXE;
        batchParameterChangeListener.getJobDetailsQueryBatchSize()>>10;
        def JobStaticData jobStaticData= new JobStaticData("administrator",activitySchedule,"Immediate",JobType.UPGRADE)
        jobStaticDataProvider.getJobStaticData(_)>>jobStaticData
        buildActivityData();


        when:"invoking getSessionIdAndClusterId "
        final OpsResponseData opsResponseData= shmAxeServiceImpl.getSessionIdAndClusterId(input);

        then:"validate response data of getSessionIdAndClusterId"
        def unsupportedNodes =  opsResponseData.getUnSupportedNodes().toArray().sort();
        Map<String, List<OpsSessionAndClusterIdInfo>> OpsSessionAndClusterIdInfoResult=opsResponseData.getOpsSessionAndClusterIdInfo();

        List<OpsSessionAndClusterIdInfo> successNodesDetails=OpsSessionAndClusterIdInfoResult.get(ShmConstants.SUCCESS_NODES);
        def supportedNodes = [successNodesDetails.get(0).getNodeName(), successNodesDetails.get(1).getNodeName()].toArray().sort();
        def sessionIds=[successNodesDetails.get(0).getSessionID(), successNodesDetails.get(1).getSessionID()].toArray().sort();
        def clusterIds=[successNodesDetails.get(0).getClusterID(), successNodesDetails.get(1).getClusterID()].toArray().sort();

        List<OpsSessionAndClusterIdInfo> failuredNodesDetails=OpsSessionAndClusterIdInfoResult.get(ShmConstants.FAILURED_NODES);
        def failuredNodes = [failuredNodesDetails.get(0).getNodeName(), failuredNodesDetails.get(1).getNodeName(), failuredNodesDetails.get(2).getNodeName()].toArray().sort();
        def failureReasons=[failuredNodesDetails.get(0).getFailureReason(), failuredNodesDetails.get(1).getFailureReason(), failuredNodesDetails.get(2).getFailureReason()].toArray().sort();


        unsupportedNodes==expectedUnsupportedNode && supportedNodes==expectedSupportedNode && sessionIds==expectedSessionId && clusterIds==expectedClusterId && failuredNodes==expectedFailureNode && failureReasons==expectedFailureReason && opsResponseData.isHasAccessToOPSGUI()==true
        where:
        input            |   expectedUnsupportedNode | expectedSupportedNode | expectedClusterId | expectedSessionId | expectedFailureNode | expectedFailureReason
        opsInputData     |   ["ABC1", "ABC2", "LTEdg2ERBS1", "LTEdg2ERBS112",]| ["HLR-FE-IS-18A-V201-AXE_cluster", "MSC-BC-BSP-18A-V202-AXE_cluster"]|  ["CL_ID_HLRFE_2", "CL_ID_MSC_2"]| ["SE_ID_HLRFE_2", "SE_ID_MSC_2"]|  ["HLR-FE-IS-18A-V201_BC01", "MSC-BC-BSP-18A-V202_BC01", "MSC-BC-BSP-18A-V202_BC02"]|["Ops cluster Id or session Id not found", "Ops cluster Id or session Id not found", "Ops cluster Id or session Id not found"]
    }

    public static  Map<String, List<NEJobInfo>> getNeTypeToNeJobIds(){
        final Map<String, List<NEJobInfo>> neTypeToNeJobIds=new HashMap<>();

        List<NEJobInfo> listOfneJobInfo1=new ArrayList<>();
        listOfneJobInfo1.add(getNeJobInfo(281474987230820L,"MSC-BC-BSP-18A-V202-AXE_cluster"));
        listOfneJobInfo1.add(getNeJobInfo(281474987230821L,"MSC-BC-BSP-18A-V202_BC01"));
        listOfneJobInfo1.add(getNeJobInfo(281474987230822L,"MSC-BC-BSP-18A-V202_BC02"));
        neTypeToNeJobIds.put("MSC-BC-BSP", listOfneJobInfo1);

        List<NEJobInfo> listOfneJobInfo2=new ArrayList<>();
        listOfneJobInfo2.add(getNeJobInfo(281474987230823L,"LTEdg2ERBS112"));
        listOfneJobInfo2.add(getNeJobInfo(281474987230824L,"LTEdg2ERBS1"));
        neTypeToNeJobIds.put("ERBS", listOfneJobInfo2);


        List<NEJobInfo> listOfneJobInfo3=new ArrayList<>();
        listOfneJobInfo3.add(getNeJobInfo(281474987230825L,"ABC1"));
        listOfneJobInfo3.add(getNeJobInfo(281474987230826L,"ABC2"));
        neTypeToNeJobIds.put("ABC", listOfneJobInfo3);

        List<NEJobInfo> listOfneJobInfo4=new ArrayList<>();
        listOfneJobInfo4.add(getNeJobInfo(281474987230827L,"HLR-FE-IS-18A-V201-AXE_cluster"));
        listOfneJobInfo4.add(getNeJobInfo(281474987230828L,"HLR-FE-IS-18A-V201_BC01"));
        neTypeToNeJobIds.put("HLR-FE-IS", listOfneJobInfo4);

        return neTypeToNeJobIds;
    }
    public static  NEJobInfo getNeJobInfo(long neJobId,String nodeName){
        NEJobInfo neJobInfo=new NEJobInfo();
        neJobInfo.setNeJobId(neJobId);
        neJobInfo.setNodeName(nodeName);
        return neJobInfo;
    }

    def buildActivityData(){
        build_ActivityData(281474987230820L,JobState.COMPLETED.getJobStateName(),"CL_ID_MSC_1","SE_ID_MSC_1");
        build_ActivityData(281474987230820L,JobState.RUNNING.getJobStateName(),"CL_ID_MSC_2","SE_ID_MSC_2");
        build_ActivityData(281474987230820L,JobState.CREATED.getJobStateName(),"CL_ID_MSC_3","SE_ID_MSC_3");

        build_ActivityData(281474987230827L,JobState.COMPLETED.getJobStateName(),"CL_ID_HLRFE_1","SE_ID_HLRFE_1");
        build_ActivityData(281474987230827L,JobState.RUNNING.getJobStateName(),"CL_ID_HLRFE_2","SE_ID_HLRFE_2");
        build_ActivityData(281474987230827L,JobState.CREATED.getJobStateName(),"CL_ID_HLRFE_3","SE_ID_HLRFE_3");
    }

    def build_ActivityData(long neJobId ,String status , String sessionId, String clusterId ){
        Map<String,Object> activityJobData = new HashMap<>();
        activityJobData.put(ShmConstants.NE_JOB_ID, neJobId)
        activityJobData.put(ShmConstants.STATE, status)
        List<Map<String, Object>> jobproperties=new ArrayList<>();
        Map<String,Object> eachJobProperty1 = new HashMap<>();
        Map<String,Object> eachJobProperty2 = new HashMap<>();
        eachJobProperty1.put(ShmConstants.KEY, ActivityConstants.OPS_CLUSTER_ID);
        eachJobProperty1.put(ShmConstants.VALUE, sessionId);
        eachJobProperty2.put(ShmConstants.KEY, ActivityConstants.OPS_SESSION_ID);
        eachJobProperty2.put(ShmConstants.VALUE, clusterId);
        jobproperties.add(eachJobProperty1);
        jobproperties.add(eachJobProperty2);
        activityJobData.put(ShmConstants.JOBPROPERTIES, jobproperties)
        setActivityJobData(activityJobData);
    }
    def setActivityJobData(Map<String,Object> activityJobData){
        runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.ACTIVITY_JOB).addAttributes(activityJobData).build();
    }
}
