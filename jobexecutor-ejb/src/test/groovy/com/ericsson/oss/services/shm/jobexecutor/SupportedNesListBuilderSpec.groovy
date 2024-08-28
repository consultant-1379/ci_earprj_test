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
package com.ericsson.oss.services.shm.jobexecutor

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.activities.NeComponentBuilder
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate
import com.ericsson.oss.services.shm.jobs.common.modelentities.NEInfo

class SupportedNesListBuilderSpec extends CdiSpecification{

    @MockedImplementation
    NeComponentBuilder axeNeComponentBuilder
    
    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }

    @ObjectUnderTest
    private SupportedNesListBuilder supportedNesListBuilder

    def "build network elements for components of selected AXE NEs and removes selected AXE NE from unSupported NetworkElements and adding their components to supported NetworkElements"() {

        given : "network element information"
        final JobTemplate jobTemplate = getJobTemplate()
        final Map<NetworkElement, String> unSupportedNetworkElements = getUnSupportedNetworkElements()
        final Map<String, String> neDetailsWithParentName = new HashMap<>()
        final List<NetworkElement> supportedNetworkElements = getSupportedNetworkElements()
        final int countSupportedNEsBeforeFilter = supportedNetworkElements.size()
        final int countunSupportedNEsBeforeFilter = unSupportedNetworkElements.size()

        when : "get supportedNEs list"
        final List<NetworkElement> supportedNEs = supportedNesListBuilder.prepareSupportedNesListForNeJobsCreation(unSupportedNetworkElements, jobTemplate, supportedNetworkElements, neDetailsWithParentName)

        then : "verify supportedNEs list count , unsupportedNEs map size and neDetailsWithParentName"

        neDetailsWithParentName.entrySet().size() != 0

        final int countSupportedNEsAfterFilter = supportedNEs.size()
        countSupportedNEsAfterFilter > countSupportedNEsBeforeFilter

        final int countunSupportedNEsafterFilter = unSupportedNetworkElements.size()
        countunSupportedNEsBeforeFilter > countunSupportedNEsafterFilter
        
        for(NetworkElement ne : unSupportedNetworkElements.keySet()){
            unSupportedNetworkElements.get(ne).equals("No component is selected for this NetworkElement")
        }
    }

    public Map<NetworkElement, String> getUnSupportedNetworkElements(){
        final Map<NetworkElement, String> neMap = new HashMap<>()
        final NetworkElement networkElement = new NetworkElement()
        networkElement.setNeType("MSC-BC-BSP")
        networkElement.setPlatformType(PlatformTypeEnum.AXE)
        networkElement.setName("MSC-BC-BSP-V18-01")
        final NetworkElement ne = new NetworkElement()
        ne.setNeType("MSC-BC-IS")
        ne.setPlatformType(PlatformTypeEnum.AXE)
        ne.setName("MSC-BC-IS-01")
        final NetworkElement networkelement = new NetworkElement()
        networkelement.setNeType("vMSC-HC")
        networkelement.setPlatformType(PlatformTypeEnum.AXE)
        networkelement.setName("vMSC-HC-01")
        neMap.put(networkElement,"abc")
        neMap.put(ne,"xyz")
        neMap.put(networkelement, "value")

        return neMap
    }

    public List<NetworkElement> getSupportedNetworkElements(){
        final List<NetworkElement> neList = new ArrayList<>()
        final NetworkElement networkElement = new NetworkElement()
        networkElement.setNeType("MSC-DB")
        networkElement.setPlatformType(PlatformTypeEnum.AXE)
        networkElement.setName("MSC-DB-01")
        final NetworkElement ne = new NetworkElement()
        ne.setNeType("BSC")
        ne.setPlatformType(PlatformTypeEnum.AXE)
        ne.setName("BSC-01")
        neList.add(networkElement)
        neList.add(ne)
        return neList
    }

    public JobTemplate getJobTemplate(){
        final JobTemplate jobTemplate=new JobTemplate()
        final JobConfiguration jobConfigurationDetails = getjobConfigurationDetails()
        jobTemplate.setJobConfigurationDetails(jobConfigurationDetails)
        return jobTemplate
    }

    public JobConfiguration getjobConfigurationDetails() {
        final JobConfiguration jobConfigurationDetails= new JobConfiguration()
        jobConfigurationDetails.setSelectedNEs(getSelectedNEs())
        return jobConfigurationDetails
    }

    public NEInfo getSelectedNEs() {
        final List<String> networkElementList = new ArrayList<>()
        networkElementList.add("MSC-BC-BSP-V18-01")
        networkElementList.add("MSC-BC-IS-01")
        networkElementList.add("MSC-DB-01")
        networkElementList.add("BSC-01")
        networkElementList.add("vMSC-HC")
        final Map<String, Object> neWithComponents= new HashMap<>()
        neWithComponents.put(ShmConstants.NE_NAME,"MSC-BC-BSP-V18-01")
        final List<String> components = new ArrayList<>()
        components.add("BC-01")
        neWithComponents.put(ShmConstants.SELECTED_COMPONENTS,components)
        final Map<String, Object> neWithComponent= new HashMap<>()
        neWithComponent.put(ShmConstants.NE_NAME,"")
        final List<String> component = new ArrayList<>()
        neWithComponent.put(ShmConstants.SELECTED_COMPONENTS,component)
        final Map<String, Object> neWithComponentMap= new HashMap<>()
        neWithComponentMap.put(ShmConstants.NE_NAME,"")
        final List<String> componentList = new ArrayList<>()
        neWithComponentMap.put(ShmConstants.SELECTED_COMPONENTS,componentList)
        final List<Map<String, Object>> neNamesHavingcomponents = new ArrayList<>()
        neNamesHavingcomponents.add(neWithComponents)
        neNamesHavingcomponents.add(neWithComponent)
        neNamesHavingcomponents.add(neWithComponentMap)
        final NEInfo neInfo= new NEInfo()
        neInfo.setNeWithComponentInfo(neNamesHavingcomponents)
        neInfo.setNeNames(networkElementList)

        return neInfo
    }
}
