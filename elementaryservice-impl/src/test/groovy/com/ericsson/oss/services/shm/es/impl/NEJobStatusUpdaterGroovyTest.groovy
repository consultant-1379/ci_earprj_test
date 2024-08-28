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
package com.ericsson.oss.services.shm.es.impl

import javax.ejb.EJBTransactionRolledbackException

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies
import com.ericsson.oss.services.shm.inventory.remote.axe.api.AxeInvSupervisionRemoteHandler
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType

class NEJobStatusUpdaterGroovyTest extends CdiSpecification{
    @ObjectUnderTest
    private NEJobStatusUpdater neJobStatusUpdater;

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps);

    @MockedImplementation
    protected RetryPolicy retryPolicy;
    @MockedImplementation
    protected DpsRetryConfigurationParamProvider dpsConfigurationParamProvider;
    @MockedImplementation
    protected DpsRetryPolicies dpsRetryPolicies;
    @MockedImplementation
    private JobStaticDataProvider jobStaticDataProvider;
    @MockedImplementation
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;
    @MockedImplementation
    private AxeInvSupervisionRemoteHandler axeInvSupervisionRemoteHandler;

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    def Map<String, Object> neJobAttributes=new HashMap<String,Object>();

    def Map<String, Object> activitiyJobAttributes=new HashMap<String,Object>();

    def Map<String, Object> mainJobAttributes=new HashMap<String,Object>();

    def 'verfy_NEJobs_Updated_or_not'(){
        given: "creating required data with mainjob,nejobs,ActivityJob"
        final long neJobId=buildJobPos(neName,activityJobresult,neJobState);
        JobStaticData jobStaticData= new JobStaticData("",new HashMap<String,String>(),"",jobType);
        jobStaticDataProvider.getJobStaticData(_)>>jobStaticData;
        NetworkElement networkElement = new NetworkElement();
        networkElement.setPlatformType(platformType);
        List<NetworkElement> networkElementMock = new ArrayList<>();
        networkElementMock.add(networkElement);
        fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(_,_)>>networkElementMock;

        when: "updating NEJob Attributes"
        neJobStatusUpdater.updateNEJobEndAttributes(neJobId);
        then: "NEJob Attributes must be updated"
        runtimeDps.stubbedDps.getLiveBucket().findPoById(neJobId).attributes.get(ShmConstants.RESULT)== expectedJobResult
        runtimeDps.stubbedDps.getLiveBucket().findPoById(neJobId).attributes.get(ShmConstants.ENDTIME)!= null
        runtimeDps.stubbedDps.getLiveBucket().findPoById(neJobId).attributes.get(ShmConstants.STATE)== "COMPLETED"
        where:

        neName           | neJobState|activityJobresult|  platformType          |jobType                | expectedJobResult

        "MSC_07"         | "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.AXE  | JobType.UPGRADE       |  "SUCCESS"
        "MSC_07"         | "RUNNING"| "CANCELLED"      |  PlatformTypeEnum.AXE  | JobType.UPGRADE       |  "CANCELLED"
        "MSC_07"         | "RUNNING"| "SKIPPED"        |  PlatformTypeEnum.AXE  | JobType.UPGRADE       |  "SKIPPED"
        "MSC_07"         | "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.AXE  | JobType.BACKUP        |  "SUCCESS"
        "MSC_07"         | "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.AXE  | JobType.LICENSE       |  "SUCCESS"
        "MSC_07"         | "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.AXE  | JobType.DELETEBACKUP  |  "SUCCESS"
        "LTE100ERBS00001"| "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.CPP  | JobType.UPGRADE       |  "SUCCESS"
        "LTE100ERBS00001"| "RUNNING"| "CANCELLED"      |  PlatformTypeEnum.CPP  | JobType.UPGRADE       |  "CANCELLED"
        "LTE100ERBS00001"| "RUNNING"| "SKIPPED"        |  PlatformTypeEnum.CPP  | JobType.UPGRADE       |  "SKIPPED"
        "LTE100ERBS00001"| "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.CPP  | JobType.BACKUP        |  "SUCCESS"
        "LTE100ERBS00001"| "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.CPP  | JobType.LICENSE       |  "SUCCESS"
        "LTE100ERBS00001"| "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.CPP  | JobType.DELETEBACKUP  |  "SUCCESS"
        "LTE100ERBS00001"| "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.CPP  | JobType.RESTORE       |  "SUCCESS"
    }

    def 'verfy_NEJobs_Updated_or_not_when_getJobType thwrows_Exception:'(){
        given: "creating required data with mainjob,nejobs,ActivityJob"
        final long neJobId=buildJobPos(neName,activityJobresult,neJobState);
        JobStaticData jobStaticData= new JobStaticData("",new HashMap<String,String>(),"",JobType.UPGRADE);
        jobStaticDataProvider.getJobStaticData(_)>>{throw new EJBTransactionRolledbackException()};
        NetworkElement networkElement = new NetworkElement();
        networkElement.setPlatformType(platformType);
        List<NetworkElement> networkElementMock = new ArrayList<>();
        networkElementMock.add(networkElement);
        fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(_,_)>>networkElementMock;

        when: "updating NEJob Attributes"
        neJobStatusUpdater.updateNEJobEndAttributes(neJobId);
        then: "NEJob Attributes must be updated"
        runtimeDps.stubbedDps.getLiveBucket().findPoById(neJobId).attributes.get(ShmConstants.RESULT)== expectedJobResult
        runtimeDps.stubbedDps.getLiveBucket().findPoById(neJobId).attributes.get(ShmConstants.ENDTIME)!= null
        runtimeDps.stubbedDps.getLiveBucket().findPoById(neJobId).attributes.get(ShmConstants.STATE)== "COMPLETED"
        where:

        neName           | neJobState|activityJobresult|  platformType          |jobType                | expectedJobResult

        "MSC_07"         | "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.AXE  | JobType.UPGRADE       |  "SUCCESS"
        "MSC_07"         | "RUNNING"| "CANCELLED"      |  PlatformTypeEnum.AXE  | JobType.UPGRADE       |  "CANCELLED"
        "MSC_07"         | "RUNNING"| "SKIPPED"        |  PlatformTypeEnum.AXE  | JobType.UPGRADE       |  "SKIPPED"
        "MSC_07"         | "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.AXE  | JobType.BACKUP        |  "SUCCESS"
        "MSC_07"         | "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.AXE  | JobType.LICENSE       |  "SUCCESS"
        "MSC_07"         | "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.AXE  | JobType.DELETEBACKUP  |  "SUCCESS"
        "LTE100ERBS00001"| "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.CPP  | JobType.UPGRADE       |  "SUCCESS"
        "LTE100ERBS00001"| "RUNNING"| "CANCELLED"      |  PlatformTypeEnum.CPP  | JobType.UPGRADE       |  "CANCELLED"
        "LTE100ERBS00001"| "RUNNING"| "SKIPPED"        |  PlatformTypeEnum.CPP  | JobType.UPGRADE       |  "SKIPPED"
        "LTE100ERBS00001"| "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.CPP  | JobType.BACKUP        |  "SUCCESS"
        "LTE100ERBS00001"| "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.CPP  | JobType.LICENSE       |  "SUCCESS"
        "LTE100ERBS00001"| "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.CPP  | JobType.DELETEBACKUP  |  "SUCCESS"
        "LTE100ERBS00001"| "RUNNING"| "SUCCESS"        |  PlatformTypeEnum.CPP  | JobType.RESTORE       |  "SUCCESS"
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

    def long buildJobPos(final String neName,final String activityJobresult,final String neJobState){
        loadMainJobAttributes();
        PersistenceObject mainJobPo =runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.JOB).addAttributes(mainJobAttributes).build();
        loadNeJobAttributes(neName,mainJobPo.poId,neJobState);
        PersistenceObject neJobPo =runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.NE_JOB).addAttributes(neJobAttributes).build();
        loadActivityJobAttributes(activityJobresult,neJobPo.poId);
        runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.ACTIVITY_JOB).addAttributes(activitiyJobAttributes).build();
        return neJobPo.getPoId();
    }

    def loadMainJobAttributes(final List<Map<String, String>> mainJobPropertyList){
        mainJobAttributes.put(ShmConstants.JOBPROPERTIES, null);
    }

    def loadNeJobAttributes(final String neName,final long mainJobPo,final String neJobState){
        //        neJobAttributes.put(ShmConstants.JOBPROPERTIES, neJobProperties);
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobPo);
        neJobAttributes.put(ShmConstants.NE_NAME, neName);
        neJobAttributes.put(ShmConstants.STARTTIME, "Thu Feb 21 10:06:58 GMT 2019");
        neJobAttributes.put(ShmConstants.STATE, neJobState);
    }
    def loadActivityJobAttributes(final String activityJobresult,final long neJobPo){
        activitiyJobAttributes.put(ShmConstants.NE_JOB_ID, neJobPo);
        activitiyJobAttributes.put(ShmConstants.RESULT, activityJobresult);
    }
}
