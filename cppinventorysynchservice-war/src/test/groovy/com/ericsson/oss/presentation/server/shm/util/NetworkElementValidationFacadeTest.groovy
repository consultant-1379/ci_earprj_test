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
package com.ericsson.oss.presentation.server.shm.util

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.common.NetworkElementFilterResponseProvider
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementFilterResponse
import com.ericsson.oss.services.shm.inventory.common.FilterRequestQuery
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse

class NetworkElementValidationFacadeTest extends CdiSpecification{

    @MockedImplementation
    private NetworkElementFilterResponseProvider networkElementFilterResponseProvider;

    @Inject
    private NetworkElementResponse networkElementResponse;

    @ObjectUnderTest
    private NetworkElementValidationFacade networkElementValidationFacade;

    final List<NetworkElement> networkElementList = new ArrayList<>();

    final List<String> neNames = new ArrayList<>();

    final Map<String, List<NetworkElement>> assertMap = new HashMap<>();

    def setup(){
        getNetworkList()
        getNenames()
    }

    def "Filter Supported Axe platform Nes "(){
        given: "Request Query"
        final FilterRequestQuery filterRequestQuery=new FilterRequestQuery();
        filterRequestQuery.setNetworkElements(neNames);
        NetworkElementFilterResponse networkElementFilterResponse=new NetworkElementFilterResponse(networkElementList,new ArrayList<>());
        networkElementFilterResponseProvider.getNetworkElementsByNeNames(_, _)>>networkElementFilterResponse

        when:"Filtering supported Nes"
        NetworkElementResponse response= networkElementValidationFacade.filterSupportedNEs(filterRequestQuery);

        then:"asserting the response"
        response.getSupportedNes().size==networkElementList.size();
        response.getNesWithComponents().size()==assertMap.size()
        response.getNesWithComponents().get("MSC07").size()==assertMap.get("MSC07").size()
    }

    def getNetworkList(){
        def NetworkElement networkElement1 = new NetworkElement()
        def NetworkElement mscNetworkElement1 = new NetworkElement()
        mscNetworkElement1.setName("MSC07")
        mscNetworkElement1.setNeType("MSC-BSC")
        mscNetworkElement1.setPlatformType(PlatformTypeEnum.AXE)
        def NetworkElement cp1NetworkElement = new NetworkElement()
        def NetworkElement ap2NetworkElement = new NetworkElement()
        networkElement1.setName("LTE02ERBS00002")
        networkElement1.setNeType("Cpppp")
        networkElement1.setPlatformType(PlatformTypeEnum.CPP)
        ap2NetworkElement.setName("MSC07__APG2")
        ap2NetworkElement.setNeType("MSC-BSC")
        ap2NetworkElement.setPlatformType(PlatformTypeEnum.AXE)
        cp1NetworkElement.setName("MSC07__CP1")
        final List<NetworkElement> componentnetworkElementList = new ArrayList<>();
        componentnetworkElementList.add(cp1NetworkElement);
        componentnetworkElementList.add(ap2NetworkElement);
        assertMap.put("MSC07",componentnetworkElementList)
        networkElementList.add(mscNetworkElement1)
        networkElementList.add(networkElement1)
    }

    def getNenames(){
        neNames.add("MSC07__APG2")
        neNames.add("MSC07__CP1")
        neNames.add("LTE02ERBS00002")
    }
}
