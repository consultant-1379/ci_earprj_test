/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.polling

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants
import com.ericsson.oss.services.shm.model.event.based.mediation.MOReadRequest


public class PollingTimerTest extends AbstractPollingTimer {

    @ObjectUnderTest
    private PollingTimer pollingTimer;

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps);


    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.service")
    }


    def 'Get the PollingActivity Data from DPS and Send it to the Request Queue' () {
        given: "Create Required Data for PollingActivity PO"
        buildPollingActivityPO(System.currentTimeMillis(), "NODE");
        membershipListenerInterface.isMaster() >> true;
        when: "Invoking Polling Service"
        pollingTimer.timeout()
        then: "Check if the data is kept in the queue"
        runtimeDps.stubbedDps.liveBucket.findPoById(poId).attributes.get(PollingActivityConstants.ACTIVITY_JOB_ID).equals(281474980155459)
        runtimeDps.stubbedDps.liveBucket.findPoById(poId).attributes.get(PollingActivityConstants.MO_FDN).equals("mofdn")
        runtimeDps.stubbedDps.liveBucket.findPoById(poId).attributes.get(PollingActivityConstants.MO_ATTRIBUTES).contains("reportProgress")
        runtimeDps.stubbedDps.liveBucket.findPoById(poId).attributes.get(PollingActivityConstants.MIM_VERSION).equals("4.2.0")
        runtimeDps.stubbedDps.liveBucket.findPoById(poId).attributes.get(PollingActivityConstants.NAMESPACE).equals("ECIM_SwM")
        runtimeDps.stubbedDps.liveBucket.findPoById(poId).attributes.get(PollingActivityConstants.POLLING_TYPE).equals("NODE")
        1 * eventSender.send(_ as MOReadRequest)
    }

    def 'Unsubscribe for polling when activity is elapsed' () {

        given: "Create Required Data for PollingActivity PO"
        buildPollingActivityPO(1542951088332, "NODE");
        membershipListenerInterface.isMaster() >> true;

        when: "Invoking Polling Service"
        pollingTimer.timeout()

        then: "Check the data is not kept in the queue"
        0 * eventSender.send(_ as MOReadRequest)
    }
}
