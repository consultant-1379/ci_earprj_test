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
package com.ericsson.oss.services.shm.jobexecutor.axe

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.modelservice.CapabilityProviderImpl
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse

import spock.lang.Unroll

class AxeJobExecutionValidatiorImplSpec extends CdiSpecification {

    @ObjectUnderTest
    private AxeJobExecutionValidatiorImpl axeJobExecutionValidatior

    @MockedImplementation
    CapabilityProviderImpl capabilityProviderImpl

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common.modelservice")
    }

    final List<NetworkElement> networkElementList = new ArrayList<>()


    def "Validate selected AXE network elements and prepare NesWithComponents"(neTypes, neNames, hasComponents, expected){

        given : "network element information"
        Map<String, String> neDetailsWithParentName = new HashMap<>();
        boolean isValidateNesforCreateNejobs = true;
        final List<NetworkElement> networkElementList = preparePlatformSpecificNesList(neTypes, neNames, hasComponents )
        final List<Map<String, Object>> selectedNesWithComponentInfo = prepareSelectedNesWithComponentInfo()
        NetworkElementResponse networkElementResponse = new NetworkElementResponse()
        networkElementResponse.setSupportedNes(networkElementList)

        when : "find Nes With Components "
        final Map<String, NetworkElement> nesWithComponentsMap = axeJobExecutionValidatior.findNesWithComponents(networkElementList, selectedNesWithComponentInfo, neDetailsWithParentName, networkElementResponse,isValidateNesforCreateNejobs)
        then : "Verify response"
        nesWithComponentsMap.size() == expected
        networkElementResponse.getSupportedNes().size()==2
        networkElementResponse.getInvalidNes().size()==1
        where :
        neTypes | neNames | hasComponents | expected
        ["MSC-BC-BSP", "MSC-DB", "BSC", "MSC-BC-BSP"] |  ["MSC-BC-BSP-18A-201", "MSC-DB-01", "GSM02BSC01", "MSC-BC-BSP-18A-202"] | ["true", "false", "false", "true"] |1
    }

    def "findNesWithComponents when the request is from supported and unsupported rest call"() {
        given : "network element information"
        final JobTypeEnum jobTypeEnum = JobTypeEnum.UPGRADE;
        final List<NetworkElement> platformSpecificNEList = new ArrayList<>();
        final List<Map<String, Object>> selectedNesWithComponentInfo = new ArrayList<>();
        final Map<String, String> neDetailsWithParentName = new HashMap<>();
        final NetworkElementResponse networkElementResponse = new NetworkElementResponse();
        final boolean flagForValidateNes = false;

        when : "find Nes with Components"
        final Map<String, NetworkElement> nesWithComponentsMap = axeJobExecutionValidatior.findNesWithComponents(jobTypeEnum,platformSpecificNEList,selectedNesWithComponentInfo,neDetailsWithParentName,flagForValidateNes)

        then : "Verify response"
        assert(nesWithComponentsMap.size() == 0)
    }

    @Unroll
    def 'findUnSupportedNEs for LicenseRefresh Job'(){
        given: 'network element information'
        final List<NetworkElement> networkElementList = prepareAxeSupportedNes(neTypes, neNames)

        when: 'NEs that are unsupported for LicenseRefresh are collcted as map'
        final Map<NetworkElement, String> nesWithComponentsMap = axeJobExecutionValidatior.findUnSupportedNEs(jobType,networkElementList)

        then: 'assert unsupported Nes count'
        assert nesWithComponentsMap.size() == count

        where:
        jobType | neTypes | neNames | count
        JobTypeEnum.LICENSE_REFRESH | ["MSC-DB", "BSC"]| ["MSC-DB-01", "GSM02BSC01"]| 2
        JobTypeEnum.UPGRADE   | ["MSC-DB", "BSC"]| ["MSC-DB-01", "GSM02BSC01"]| 0
    }

    public List<NetworkElement> preparePlatformSpecificNesList(neTypes,neNames, hasComponents ){
        for(int i=0; i<4; i++){
        final NetworkElement networkElement= new NetworkElement()
        networkElement.setNeType(neTypes[i])
        networkElement.setPlatformType(PlatformTypeEnum.AXE)
        networkElement.setName(neNames[i])
        networkElementList.add(networkElement)
        capabilityProviderImpl.getCapabilityValueForNeType(neTypes[i],_) >> hasComponents[i]
        }
        return networkElementList
    }

    public List<Map<String, Object>> prepareSelectedNesWithComponentInfo(){
        List<Map<String, Object>> selectedNesWithComponentInfo = new ArrayList<>();
        final Map<String, Object> neWithComponents= new HashMap<>()
        neWithComponents.put(ShmConstants.NE_NAME,"MSC-BC-BSP-18A-201")
        final List<String> mscBcBspComponents = new ArrayList<>()
        mscBcBspComponents.add("BC01")
        mscBcBspComponents.add("BC02")
        mscBcBspComponents.add("BC03")
        neWithComponents.put(ShmConstants.SELECTED_COMPONENTS,mscBcBspComponents)
        selectedNesWithComponentInfo.add(neWithComponents)
        return selectedNesWithComponentInfo
    }

    def List<NetworkElement> prepareAxeSupportedNes(neTypes, neNames){
        for(int i=0; i<2; i++){
            final NetworkElement networkElement= new NetworkElement()
            networkElement.setNeType(neTypes[i])
            networkElement.setPlatformType(PlatformTypeEnum.AXE)
            networkElement.setName(neNames[i])
            networkElementList.add(networkElement)
        }
        return networkElementList
    }
}
