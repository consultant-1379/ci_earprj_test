package com.ericsson.oss.services.shm.job.service.common;

import com.ericsson.oss.services.shm.topologyservice.TopologyEvaluationService


import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.*
import com.ericsson.cds.cdi.support.spock.CdiSpecification;
import com.ericsson.oss.services.shm.common.FdnServiceBean
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.modelservice.NodeTypeProviderImpl
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement
import com.ericsson.oss.services.shm.job.remote.api.ShmDeleteUpgradePkgJobData
import com.ericsson.oss.services.shm.job.remote.api.errorcodes.JobCreationResponseCode
import com.ericsson.oss.services.shm.job.remote.impl.*
import com.ericsson.oss.services.shm.jobservice.common.JobInfo

import spock.lang.Unroll


public class DeleteUpgradePkgJobInfoConvertorTest extends CdiSpecification{

    @ObjectUnderTest
    private DeleteUpgradePkgJobInfoConvertor deleteUpgradePkgJobInfoConvertor;

    @ImplementationInstance
    FdnServiceBeanRetryHelper networkElementsProvider = Mock(FdnServiceBeanRetryHelper)

    @MockedImplementation
    private FdnServiceBean fdnServiceBean;

    @MockedImplementation
    private NodeTypeProviderImpl nodeTypeProviderImpl;

    @MockedImplementation
    private PlatformTypeProviderImpl platformTypeProviderImpl;


    def addAdditionalInjectionProperties(InjectionProperties i){
        i.autoLocateFrom("com.ericsson.oss.services.shm.job.service.common")
        i.autoLocateFrom("com.ericsson.oss.services.shm.job.remote.impl")
    }

    def Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes = new HashMap<PlatformTypeEnum, List<String>>();
    def neTypes = new ArrayList<Object>()
    def List<Map<String, Object>> configurations = new ArrayList<Map<String, Object>>();
    def List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
    def Set<String> neNamesSet = new HashSet<String>();
    def List<String> neNames = new ArrayList<String>(neNamesSet);

    def setup() {
        neTypes.add("ERBS")
        neTypes.add("RBS")
        supportedPlatformTypeAndNodeTypes.put(PlatformTypeEnum.CPP, neTypes)
        NetworkElement networkElement = new NetworkElement();
        networkElement.setPlatformType(PlatformTypeEnum.CPP);
        networkElements.add(networkElement);
        nodeTypeProviderImpl.getsupportedNeTypes() >> neTypes
        platformTypeProviderImpl.getPlatformTypeBasedOnCapability(_ as String, _ as String) >> PlatformTypeEnum.CPP

        networkElementsProvider.getNetworkElements(_) >> networkElements
        fdnServiceBean.getNetworkElementsByNeNames(neNames, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY) >> networkElements
    }


    def 'Prepare the JobInfo data by Converting the shmRemoteJobData' () {
        given:
        def Set<String> neNamesSet = new HashSet<String>();
        neNamesSet.add("LTE01ERBS01");
        ShmDeleteUpgradePkgJobData shmDeleteUpgradePkgJobData = new ShmDeleteUpgradePkgJobData(productNumber:'ProductNum', productRevision:'ProducrRev', activity:'deleteupgradepackage', neNames:neNamesSet);
        when:
        JobInfo jobInfo = deleteUpgradePkgJobInfoConvertor.prepareJobInfoData(shmDeleteUpgradePkgJobData);
        then:
        jobInfo != null
    }

    @Unroll
    def 'Validate the shmRemoteJobData' () {
        given:
        ShmDeleteUpgradePkgJobData shmDeleteUpgradePkgJobData = new ShmDeleteUpgradePkgJobData(productNumber:ProductNumber, productRevision:ProductRevision);
        when:
        JobCreationResponseCode jobCreationResponseCode = deleteUpgradePkgJobInfoConvertor.isValidData(shmDeleteUpgradePkgJobData);
        then:
        jobCreationResponseCode.getErrorCode() == errorCode
        jobCreationResponseCode.getErrorMessage() == errorMessage
        where:
        ProductNumber | ProductRevision | errorCode | errorMessage
        ''            |      ''         |   13300   | 'ProductNumber cannot not be Null/Empty'
        'ProductNum'  |      ''         |   13301   | 'ProductRevision cannot not be Null/Empty'
    }

    def cleanup() {
        neTypes.clear()
    }
}