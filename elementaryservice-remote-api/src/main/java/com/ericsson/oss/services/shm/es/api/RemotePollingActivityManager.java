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
package com.ericsson.oss.services.shm.es.api;

import java.util.Map;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.polling.api.PollingData;

/**
 * Remote service to be used by NHC
 * 
 * @author xrajeke
 *
 */
@EService
@Remote
public interface RemotePollingActivityManager {

    void subscribe(JobActivityInfo jobActivityInfo, NetworkElement networkElement, final PollingData pollingData, String callbackQueue, final String pollingType);

    void updatePollingAttributesByActivityJobId(long activityJobId, Map<String, Object> attributes);

    void prepareAndAddPollingActivityDataToCache(long activityJobId, JobActivityInfo jobActivityInfo, String callbackQueue);

    void unsubscribeByActivityJobId(long activityJobId, String activityName, String nodeName);

}
