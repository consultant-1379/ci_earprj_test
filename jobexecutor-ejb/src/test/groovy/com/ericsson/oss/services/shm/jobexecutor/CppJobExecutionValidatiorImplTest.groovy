package com.ericsson.oss.services.shm.jobexecutor

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroup
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroupPreparator
import com.ericsson.oss.services.shm.jobexecutor.cpp.CppJobExecutionValidatiorImpl
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum

import spock.lang.Unroll

class CppJobExecutionValidatiorImplTest extends CdiSpecification {

    @ObjectUnderTest
    private CppJobExecutionValidatiorImpl cppJobExecutionValidatiorImpltest

    @Inject
    private NetworkElementGroupPreparator neGroupPreparator;

    @MockedImplementation
    NetworkElementGroup networkElementGroup;

    final List<NetworkElement> networkElementList = new ArrayList<>()

    def "finding Unsupproted NES"() {
        given : "network element information"
        final JobTypeEnum jobTypeEnum = JobTypeEnum.NODE_HEALTH_CHECK;
        final List<NetworkElement> platformSpecificNEList = getUnSupportedNetworkElements();
        final List<Map<String, Object>> selectedNesWithComponentInfo = new ArrayList<>();
        final Map<String, String> neDetailsWithParentName = new HashMap<>();
        final boolean flagForValidateNes = false;
        neGroupPreparator.groupNetworkElementsByModelidentity(platformSpecificNEList) >> networkElementGroup
        Map<String, String> unSupportedNelist=new HashMap<String, String>();
        networkElementGroup.getUnSupportedNetworkElements() >> unSupportedNelist
        unSupportedNelist.put("LTE06dg2ERBS00001", "Value")
        when : "find Nes with Components"
        final Map<NetworkElement, String> nesWithComponentsMap = cppJobExecutionValidatiorImpltest.findUnSupportedNEs(jobTypeEnum,platformSpecificNEList);
        then : "Verify response"
        assert(nesWithComponentsMap.size() == 1)
        NetworkElement networkElement = nesWithComponentsMap.entrySet().iterator().next().getKey()
        assert(networkElement.getName()=="LTE06dg2ERBS00001")
        assert(networkElement.getNeType()=="RadioNode")
        assert(networkElement.getPlatformType().toString()=="ECIM")
    }

    def"finding Unsupproted NES when input null"(){
        given : "network element information"
        final JobTypeEnum jobTypeEnum = JobTypeEnum.NODE_HEALTH_CHECK;
        final List<NetworkElement> platformSpecificNEList =new ArrayList<>();
        final List<Map<String, Object>> selectedNesWithComponentInfo = new ArrayList<>();
        final Map<String, String> neDetailsWithParentName = new HashMap<>();
        final boolean flagForValidateNes = false;
        neGroupPreparator.groupNetworkElementsByModelidentity(platformSpecificNEList) >> networkElementGroup
        Map<String, String> unSupportedNelist=new HashMap<String, String>();
        networkElementGroup.getUnSupportedNetworkElements() >> unSupportedNelist
        unSupportedNelist.put("LTE06dg2ERBS00001", "Value")
        when : "find Nes with Components"
        final Map<NetworkElement, String> nesWithComponentsMap = cppJobExecutionValidatiorImpltest.findUnSupportedNEs(jobTypeEnum,platformSpecificNEList);
        then : "Verify response"
        assert(nesWithComponentsMap.size() == 0)
        assert(nesWithComponentsMap.isEmpty())
    }

    @Unroll
    def 'findUnSupportedNEs for LicenseRefresh Job'(){
        given: 'network element information'
        def cppNetworkElementMap = ['LTE01dg2ERBS001' : 'ERBS', 'LTE01dg2ERBS002' : 'ERBS']
        final List<NetworkElement> networkElementList = prepareCppSupportedNes(neType, neNames)
        neGroupPreparator.groupNetworkElementsByModelidentity(networkElementList) >> networkElementGroup
        networkElementGroup.getUnSupportedNetworkElements() >> cppNetworkElementMap

        when: 'NEs that are unsupported for LicenseRefresh are collcted as map'
        final Map<NetworkElement, String> nesWithComponentsMap = cppJobExecutionValidatiorImpltest.findUnSupportedNEs(jobType,networkElementList)

        then: 'assert unsupported Nes count'
        assert nesWithComponentsMap.size() == count
        NetworkElement networkElement = nesWithComponentsMap.entrySet().iterator().next().getKey()
        assert(networkElement.getNeType()=="ERBS")
        assert(networkElement.getPlatformType().toString()=="CPP")

        where:
        jobType | neType | neNames | count
        JobTypeEnum.LICENSE_REFRESH | 'ERBS'| ["LTE01dg2ERBS001", "LTE01dg2ERBS002"]| 2
        JobTypeEnum.UPGRADE   | 'ERBS'| ["LTE01dg2ERBS001", "LTE01dg2ERBS002"]| 2
    }

    def List<NetworkElement> getUnSupportedNetworkElements(){
        final List<NetworkElement> neList = new ArrayList<>()
        final NetworkElement networkElement = new NetworkElement()
        networkElement.setNeType("RadioNode")
        networkElement.setPlatformType(PlatformTypeEnum.ECIM)
        networkElement.setName("LTE06dg2ERBS00001")
        final NetworkElement unsupportedNetworkelement = new NetworkElement()
        unsupportedNetworkelement.setNeType("cppnode")
        unsupportedNetworkelement.setPlatformType(PlatformTypeEnum.CPP)
        unsupportedNetworkelement.setName("LTE100ERBS00001")
        neList.add(networkElement)
        neList.add(unsupportedNetworkelement)
        return neList
    }

    def List<NetworkElement> prepareCppSupportedNes(neType, neNames){
        for(int i=0; i<2; i++){
            final NetworkElement networkElement= new NetworkElement()
            networkElement.setNeType(neType)
            networkElement.setPlatformType(PlatformTypeEnum.CPP)
            networkElement.setName(neNames[i])
            networkElementList.add(networkElement)
        }
        return networkElementList
    }
}
