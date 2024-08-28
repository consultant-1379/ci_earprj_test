/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.noderestart;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.annotations.PlatformAnnotation;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.NodeRestartValidator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;

/**
 * CppNodeRestartValidatorImpl is used to validate for node reachability specific to CPP platform type.
 */
@PlatformAnnotation(name = PlatformTypeEnum.CPP)
public class CppNodeRestartValidatorImpl implements NodeRestartValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CppNodeRestartValidatorImpl.class);

    @Inject
    private NodeRestartUtility nodeRestartUtility;

    @Inject
    protected ActivityUtils activityUtils;

    /**
     * this method is to check if node is reachable or not.
     * 
     * @param activityJobId
     * @return
     */

    @Override
    public boolean isNodeReachable(final String nodeName) {
        String logMessage = null;
        boolean nodeRestartStatus = false;
        try {
            nodeRestartStatus = nodeRestartUtility.isNodeReachable(nodeName);
            logMessage = nodeRestartStatus ? String.format(RestartActivityConstants.RESTART_NODE_SUCCESS, nodeName) : String.format(RestartActivityConstants.RESTART_NODE_RETRY, nodeName);
            LOGGER.trace(logMessage);
        } catch (Exception ex) {
            logMessage = String.format(RestartActivityConstants.RESTART_NODE_ERROR, nodeName, ex);
            LOGGER.error(logMessage);
        }

        return nodeRestartStatus;
    }

}
