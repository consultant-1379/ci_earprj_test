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
package com.ericsson.oss.services.shm.es.impl.cpp.polling

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradeService
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants

public class SubscribeForPollingRequestTest extends AbstractPollingData {

    @ObjectUnderTest
    private UpgradeService upgradeService;

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }

    def 'subscribe for polling when job progress notifications are getting delayed'(){
        given : "Details of the node and type of the job and activity of the job"
        activityName = ACTIVITY_NAME_UPGRADE;
        buildJobPO(false)
        buildDataToSubscribeForPolling()
        dspStatusInfoProvider.isDatabaseAvailable() >> true
        when: "Subscribe for polling is called"
        upgradeService.subscribeForPolling(activityJobId)
        then: "Verify moFdn, mimVersion, namespace and pollCycleStatus are persisted in the DPS or not"
        final PersistenceObject persistenceObject = getPollingActivityPos(activityJobId)
        persistenceObject.attributes.get(PollingActivityConstants.MO_FDN).equals(moFdn)
        persistenceObject.attributes.get(PollingActivityConstants.MIM_VERSION).equals("10.1.280")
        persistenceObject.attributes.get(PollingActivityConstants.NAMESPACE).equals("ERBS_NODE_MODEL")
        persistenceObject.attributes.get(PollingActivityConstants.POLL_CYCLE_STATUS).equals("READY")
    }

    def 'subscribe for polling when job progress notifications are getting delayed and dps is down'(){
        given : "Details of the node and type of the job and activity of the job"
        activityName = ACTIVITY_NAME_UPGRADE;
        buildJobPO(false)
        buildDataToSubscribeForPolling()
        dspStatusInfoProvider.isDatabaseAvailable() >> false
        when: "Subscribe for polling is called"
        upgradeService.subscribeForPolling(activityJobId)
        then: "Verify entry is placed in the Cache with the given activityJobId"
        pollingActivityCacheManager.getPollingEntriesFromCache().get(0).getActivityJobId().equals(activityJobId)
    }
}
