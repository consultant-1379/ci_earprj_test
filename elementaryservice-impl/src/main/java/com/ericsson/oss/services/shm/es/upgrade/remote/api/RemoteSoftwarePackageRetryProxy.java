/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.upgrade.remote.api;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;

public class RemoteSoftwarePackageRetryProxy {

    @Inject
    private RemoteSoftwarePackageManager remoteSoftwarePackageManager;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    public Map<String, Object> validateUPMoState(final Map<String, String> nodeSwPkgDetailsMap) {

        final Map<String, Object> upMoState = retryManager.executeCommand(dpsRetryPolicies.getReadAttributesRetryPolicy(), new RetriableCommand<Map<String, Object>>() {
            @Override
            public Map<String, Object> execute(final RetryContext retryContext) {
                Map<String, Object> upMoState = new HashMap<String, Object>();
                try {
                    upMoState = remoteSoftwarePackageManager.validateUPMoState(nodeSwPkgDetailsMap);
                } catch (final Exception ex) {
                    final boolean isNodeReachable = NodeMediationServiceExceptionParser.isNodeUnreachable(ex);
                    if (!isNodeReachable) {
                        throw ex;
                    }
                }
                return upMoState;
            }
        });
        return upMoState;
    }

}
