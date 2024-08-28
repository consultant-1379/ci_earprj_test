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
package com.ericsson.oss.services.shm.es.polling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.ProductData;
import com.ericsson.oss.services.shm.common.networkelement.NetworkElementAttributes;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.RemotePollingActivityManager;
import com.ericsson.oss.services.shm.es.polling.api.PollingData;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RemotePollingActivityManagerImpl implements RemotePollingActivityManager {

    @Inject
    private PollingActivityManager pollingActivityManager;

    @Override
    public void subscribe(final JobActivityInfo jobActivityInfo, final NetworkElement networkElement, final PollingData pollingData, final String callbackQueue, final String pollingType) {
        final List<ProductData> productData = networkElement.getNeProductVersion();
        final List<Map<String, String>> productDetails = new ArrayList<>();
        for (final ProductData neProductVersion : productData) {
            final Map<String, String> productVersion = new HashMap<>();
            productVersion.put(ShmConstants.REVISION, neProductVersion.getRevision());
            productVersion.put(ShmConstants.IDENTITY, neProductVersion.getIdentity());
            productDetails.add(productVersion);
        }
        final NetworkElementAttributes networkElementData = new NetworkElementAttributes(networkElement.getNeType(), productDetails, networkElement.getNetworkElementFdn(),
                networkElement.getNodeRootFdn(), networkElement.getOssModelIdentity(), networkElement.getUtcOffset(), networkElement.getNodeModelIdentity());
        networkElementData.setOssPrefix(networkElement.getOssPrefix());
        pollingActivityManager.subscribe(jobActivityInfo, networkElementData, pollingData, callbackQueue, pollingType);

    }

    @Override
    public void updatePollingAttributesByActivityJobId(final long activityJobId, final Map<String, Object> attributes) {
        pollingActivityManager.updatePollingAttributesByActivityJobId(activityJobId, attributes);
    }

    @Override
    public void prepareAndAddPollingActivityDataToCache(final long activityJobId, final JobActivityInfo jobActivityInfo, final String callbackQueue) {
        pollingActivityManager.prepareAndAddPollingActivityDataToCache(activityJobId, jobActivityInfo, callbackQueue);
    }

    @Override
    public void unsubscribeByActivityJobId(final long activityJobId, final String activityName, final String nodeName) {
        pollingActivityManager.unsubscribeByActivityJobId(activityJobId, activityName, nodeName);
    }

}
