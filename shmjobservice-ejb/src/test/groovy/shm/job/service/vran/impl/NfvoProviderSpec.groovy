/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.job.service.vran.impl

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.MoPopulator
import com.ericsson.oss.services.shm.common.DpsReaderImpl

import spock.lang.Unroll

class NfvoProviderSpec extends CdiSpecification {

    private static final String NFVO_MO = "NetworkFunctionVirtualizationOrchestrator"
    private static final String TEST_NFVO_NAME_MASK = "HPE-NFV-Director-00"
    private static final String TEST_NFVO_FDN_MASK = NFVO_MO + "=" + TEST_NFVO_NAME_MASK
    private static final String HP_NFVO_TYPE = "HPE-NFV-Director"

    @ImplementationClasses
    protected def definedImplementations = [DpsReaderImpl.class]

    private MoPopulator moPopulator = new MoPopulator(getCdiInjectorRule().getService(RuntimeConfigurableDps))

    @ObjectUnderTest
    private NfvoProvider nfvoProvider

    @Unroll
    def "Listing NFVO Names with #numberOfNfvos existing NFVOs"() {
        given: "#numberOfNfvos NFVOs in dps"
            moPopulator.createMultipleNfvos(TEST_NFVO_FDN_MASK, HP_NFVO_TYPE, numberOfNfvos)
        when: "listing NFVO names"
            final nfvoNames = nfvoProvider.getNfvoNames()
        then: "returns list with #numberOfNfvos NFVO names"
            nfvoNames.size() == numberOfNfvos
        and: "the list contains all expected NFVO names"
            nfvoNames.containsAll(getExpectedNfvoNames(numberOfNfvos))
        where:
            numberOfNfvos << [0, 1, 2]
    }

    private static List<String> getExpectedNfvoNames(final int numberOfNfvoNames) {
        final List<String> nfvoNames = new ArrayList<>()
        for (final int nfvoId = 1; nfvoId <= numberOfNfvoNames; nfvoId++) {
            nfvoNames.add(TEST_NFVO_NAME_MASK + nfvoId)
        }
        return nfvoNames
    }

}