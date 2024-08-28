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
package com.ericsson.oss.services.shm.es.polling

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants

class PollingActivityManagerTest extends AbstractPollingTimer{

    @ObjectUnderTest
    private PollingActivityManager pollingActivityManager;

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps);


    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.service")
    }

     def 'Get the PollingActivity POs of type NODE and null from DPS' () {
        given: "Create PollingActivity POs with different polling types"
        buildPollingActivityPO(System.currentTimeMillis(), "NODE");
        buildPollingActivityPO(System.currentTimeMillis(), "ENM");
        buildPollingActivityPO(System.currentTimeMillis(), null);
        when: "Getting the PollingActivity POs"
        final List<Map<String,Object>> pollingPos = pollingActivityManager.getPollingActivityPOs();
        then: "Check if retrieved POs are of type NODE and null only"
        pollingPos.size() == 2;
        final Set<String> pollingTypes = new HashSet();
        for (final Map<String,Object> po : pollingPos) {
            println po.get(PollingActivityConstants.POLLING_TYPE)
            pollingTypes.add(po.get(PollingActivityConstants.POLLING_TYPE));
        }
        [null, "NODE"]== pollingTypes.toArray();
       
    }
}
