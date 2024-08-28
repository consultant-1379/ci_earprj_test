package com.ericsson.oss.services.shm.es.impl
import javax.ejb.Timer
import javax.inject.Inject

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy
import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface
import com.ericsson.oss.services.shm.common.enums.JobStateEnum
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants


class MainJobProgressUpdateInitiationTimerTest extends CdiSpecification{

    @MockedImplementation
    Timer timer;
    @MockedImplementation
    private MembershipListenerInterface membershipListenerInterface;
    @MockedImplementation
    protected RetryPolicy retryPolicy;
    @MockedImplementation
    protected DpsRetryConfigurationParamProvider dpsConfigurationParamProvider;
    @MockedImplementation
    protected DpsRetryPolicies dpsRetryPolicies;

    @ObjectUnderTest
    private MainJobProgressUpdateInitiationTimer mainJobProgressUpdateInitiationTimer;

    @Inject
    private com.ericsson.oss.services.shm.job.service.SHMJobService shmJobService;

    def Map<String, Object> mainJobAttributes=new HashMap<String,Object>();

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps);

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.service")
    }

    def buildJobPO(List progressPercentage,int noOfNetworkElements,double mainJobCurrentProgressPercentage){
        loadMainJobProperties(noOfNetworkElements,mainJobCurrentProgressPercentage);
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(ShmConstants.ACTIVITY_NE_JOB_ID, 1L);
        attributeMap.put(ShmConstants.NE_NAME,"LTE02ERBS00001");
        runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.ACTIVITY_JOB).addAttributes(attributeMap).build();
        PersistenceObject mainJobPo = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.JOB).addAttributes(mainJobAttributes).build();
        for(int index=0;index<noOfNetworkElements;index++) {
            addNeJob("LTE02ERBS0000"+index,mainJobPo.poId,progressPercentage[index]);
        }
    }

    def addNeJob(String nodeName,long mainJobId,double neProgressPercentage) {
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(ShmConstants.PROGRESSPERCENTAGE,Double.valueOf(neProgressPercentage));
        attributeMap.put(ShmConstants.MAINJOBID, mainJobId)
        attributeMap.put(ShmConstants.NE_NAME,nodeName);
        runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.NE_JOB).addAttributes(attributeMap).build();
    }

    def loadMainJobProperties(int noOfNetworkElements,double mainProgressPercentage) {
        mainJobAttributes.put(ShmConstants.NO_OF_NETWORK_ELEMENTS,(int)noOfNetworkElements);
        mainJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE,mainProgressPercentage);
        mainJobAttributes.put(ShmConstants.JOB_TEMPLATE_ID,(long)3);
    }

    protected mockRetries() {
        retryPolicy.getAttempts() >> 3;
        retryPolicy.waitTimeInMillis >> 1000;
        dpsRetryPolicies.getDpsMoActionRetryPolicy() >> retryPolicy;
        dpsRetryPolicies.getDpsGeneralRetryPolicy() >> retryPolicy;
        dpsConfigurationParamProvider.getdpsWaitIntervalInMS() >> 5;
        dpsConfigurationParamProvider.getdpsRetryCount() >> 3;
        dpsConfigurationParamProvider.getMoActionRetryCount() >> 3
    }

    def 'caluculate main job progress percentage' () {

        given: "creating required data with mainjob and nejobs"
        shmJobService.getMainJobIds(JobStateEnum.RUNNING.name(), JobStateEnum.CANCELLING.name()) >> Arrays.asList(2L);
        buildJobPO(neProgressPercentage,noOfNetworkElements,mainJobCurrentProgressPercentage);
        membershipListenerInterface.isMaster() >> true;

        when: "invoking main job progress update"
        mainJobProgressUpdateInitiationTimer.invokeMainJobsProgressUpdateService(timer)

        then: "validating main job progress update"
        runtimeDps.stubbedDps.getLiveBucket().findPoById(2L).attributes.get(ShmConstants.PROGRESSPERCENTAGE) == mainJobProgressPercentage;

        where:
        mainJobCurrentProgressPercentage | noOfNetworkElements  | neProgressPercentage                                                          | mainJobProgressPercentage
        2.0                              | 2                    | Arrays.asList(6.0d,6.0d)                                                      | 6.0
        15.0                             | 3                    | Arrays.asList(20.0d,45.0d,45.0d)                                              | 36.67
        2.0                              | 10                   | Arrays.asList(50.0d,60.0d,59.0d,33.0d,35.0d,0.0d,40.0d,100.0d,340.0d,10.0d)   | 72.70
        75.0                             | 10                   | Arrays.asList(50.0d,60.0d,59.0d,33.0d,35.0d,0.0d,40.0d,100.0d,340.0d,10.0d)   | 75.00
    }
}
