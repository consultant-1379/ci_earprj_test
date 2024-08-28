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

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender
import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface
import com.ericsson.oss.services.shm.common.networkelement.NetworkElementAttributes
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.model.NetworkElementData
import com.ericsson.oss.services.shm.model.event.based.mediation.MOReadRequest
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean

class AbstractPollingTimer extends CdiSpecification {

    @MockedImplementation
    protected MembershipListenerInterface membershipListenerInterface;

    @MockedImplementation
    protected RetryPolicy retryPolicy;

    @MockedImplementation
    protected DpsRetryConfigurationParamProvider dpsConfigurationParamProvider;

    @MockedImplementation
    protected DpsRetryPolicies dpsRetryPolicies;

    @MockedImplementation
    protected EventSender<MOReadRequest> eventSender;

    @MockedImplementation
    protected ActivityTimeoutsService activityTimeoutsService;

    @MockedImplementation
    protected NeJobStaticDataProvider neJobsStaticDataProvider;

    @MockedImplementation
    protected NetworkElementRetrievalBean networkElementRetrievalBean;

    protected long poId = 0L;
    protected long activityPoId = 0L;



    protected mockRetries() {
        retryPolicy.getAttempts() >> 3;
        retryPolicy.waitTimeInMillis >> 1000;
        dpsRetryPolicies.getDpsMoActionRetryPolicy() >> retryPolicy;
        dpsRetryPolicies.getDpsGeneralRetryPolicy() >> retryPolicy;
        dpsConfigurationParamProvider.getdpsWaitIntervalInMS() >> 5;
        dpsConfigurationParamProvider.getdpsRetryCount() >> 3;
        dpsConfigurationParamProvider.getMoActionRetryCount() >> 3
    }

    def buildPollingActivityPO(final long currentTime, final String pollingType) {
        final Map<String, Object> pollingEntry = new HashMap<String, Object>();
        final Map<String, String> properties = new HashMap<String, String>();
        NetworkElementData networkElementData = null;
        networkElementData = new NetworkElementAttributes("Radio", new ArrayList<>(),"LTE02dg2ERBS06", "LTE02","", "", "");

        properties.put(ShmConstants.JOB_TYPE, "UPGRADE");
        properties.put(ShmConstants.ACTIVITYNAME, "Activate");
        properties.put(ShmConstants.PLATFORM, "CPP");
        List<String> attributes = new ArrayList<String>();

        attributes.add("reportProgress");

        pollingEntry.put("activityJobId", 281474980155459);
        pollingEntry.put("moFdn", "mofdn");
        pollingEntry.put("moAttributes", attributes);
        pollingEntry.put("mimVersion", "4.2.0");
        pollingEntry.put("namespace", "ECIM_SwM");
        pollingEntry.put("additionalInformation", properties);
        pollingEntry.put("pollCycleStatus", "READY");
        pollingEntry.put("type", pollingType);

        PersistenceObject pollingActivityPO = runtimeDps.addPersistenceObject().namespace("shm").type("PollingActivity").addAttributes(pollingEntry).build();
        neJobsStaticDataProvider.getActivityStartTime(_) >> currentTime
        networkElementRetrievalBean.getNetworkElementData(_) >> networkElementData
        activityTimeoutsService.getActivityTimeoutAsInteger(_,_,_,_) >> 5
        poId = pollingActivityPO.getPoId();
    }
}
