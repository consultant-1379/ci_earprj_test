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
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common

import static org.mockito.Mockito.when
import org.spockframework.util.Assert

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;

class DPSUtilsSpec extends CdiSpecification {

    @MockedImplementation
    private DataPersistenceService dataPersistenceService

    @MockedImplementation
    private DataBucket liveBucket

    @MockedImplementation
    private ManagedObject nodeMo

    @ObjectUnderTest
    DPSUtils dpsUtils

    private static final String neName = "CORE42ML6352";
    private static final String MINI_LINK_Outdoor = "MINI_LINK_Outdoor";

    def setup() {
        final String networkElementFdn = "NetworkElement=" + neName
        dataPersistenceService.getLiveBucket() >> liveBucket
        liveBucket.findMoByFdn(networkElementFdn) >> nodeMo
        nodeMo.getAttribute("neType") >> MINI_LINK_Outdoor
    }

    def "testGetNeType" () {
        when: "invoke get NeType"
        String neType = dpsUtils.getNeType(neName)
        then : "return value should not be null"
        Assert.notNull(neType)
        neType ==  MINI_LINK_Outdoor
    }
}
