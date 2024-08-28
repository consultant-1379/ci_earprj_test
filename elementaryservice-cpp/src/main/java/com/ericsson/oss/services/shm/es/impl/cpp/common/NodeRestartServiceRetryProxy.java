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
package com.ericsson.oss.services.shm.es.impl.cpp.common;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.es.api.DefaultActionRetryPolicy;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.noderestart.NodeRestartUtility;
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy;

public class NodeRestartServiceRetryProxy {

    private final static Logger LOGGER = LoggerFactory.getLogger(NodeRestartServiceRetryProxy.class);
    @Inject
    private NodeRestartUtility nodeRestartUtility;

    @Inject
    private RetryManager retryManager;

    @Inject
    @DefaultActionRetryPolicy
    private ActionRetryPolicy moActionRetryPolicy;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    public String getManagedObjectFdn(final String nodeName) {

        final String moFdn = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
            @Override
            public String execute() {
                return nodeRestartUtility.getManagedElementFdn(nodeName);
            }
        });
        return moFdn;
    }

    public boolean isNodeReachable(final String nodeName) {
        final boolean isNodeRechable = retryManager.executeCommand(dpsRetryPolicies.getReadAttributesRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
            @Override
            public Boolean execute() {
                return nodeRestartUtility.isNodeReachable(nodeName);
            }
        });
        return isNodeRechable;
    }

    public boolean performAction(final String nodeName, final String moFdn, final String moAction, final Map<String, Object> actionArguments) {

        final boolean isActionSuccess = retryManager.executeCommand(moActionRetryPolicy.getDpsMoActionRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
            @Override
            public Boolean execute() {
                boolean isActionSuccess = false;
                try {
                    isActionSuccess = nodeRestartUtility.performAction(moFdn, moAction, actionArguments);
                } catch (final Exception ex) {
                    final boolean isNodeReachable = NodeMediationServiceExceptionParser.isNodeUnreachable(ex);
                    LOGGER.error("Failed to trigger action : {} and isNodeReachable {}. Reason for failure ", moAction, isNodeReachable, ex.getMessage());
                    if (!isNodeReachable) {
                        throw ex;
                    }
                }
                return isActionSuccess;
            }
        });
        return isActionSuccess;
    }

    public Map<String, Object> getManagedElementAttributes(final String nodeName) {
        final Map<String, Object> moAttributes = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
            @Override
            public Map<String, Object> execute() {
                return nodeRestartUtility.getManagedElementAttributes(nodeName);
            }
        });
        return moAttributes;
    }
}
