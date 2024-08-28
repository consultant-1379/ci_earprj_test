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

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReader;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * This Utility is to defined to provide the common functionalities on node.
 */
@Stateless
public class NodeRestartUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeRestartUtility.class);

    @Inject
    private NodeAttributesReader nodeAttributesReader;

    @Inject
    private CppNodeRestartMOUtil cppNodeRestartMOUtil;

    /**
     * This method performs the action with given actionArguments and action name.
     * 
     */
    public boolean performAction(final String moFdn, final String actionName, final Map<String, Object> actionArguments) {
        boolean performActionStatus = false;
        try {
            final ManagedObject managedObject = cppNodeRestartMOUtil.getManagedElementMOByFdn(moFdn);
            if (managedObject != null) {
                managedObject.performAction(actionName, actionArguments);
                performActionStatus = true;
                LOGGER.debug("Action : {} triggered on the Managed Object with Fdn: {} ", actionName, moFdn);
            }
        } catch (final Exception ex) {
            LOGGER.error("Failed to get managed object of Fdn {} to trigger  :{}. Reason ", moFdn, RestartActivityConstants.RESTART_NODE, ex);
        }
        return performActionStatus;
    }

    /**
     * This method check whether the node is up and running or not.
     * 
     * @param managedObject
     * @return boolean flag
     * 
     */
    private boolean getMoAttributesStatus(final ManagedObject managedObject) {
        boolean moAttributesStatus = false;
        if (managedObject != null) {
            try {
                final Map<String, Object> moAttributes = nodeAttributesReader.readAttributes(managedObject, RestartActivityConstants.attributeNames);
                if (moAttributes.size() > 0) {
                    moAttributesStatus = true;
                }
            } catch (final Exception ex) {
                LOGGER.error("Exception while evaluating final activity result for : " + RestartActivityConstants.RESTART_NODE + " Exception : ", ex);
                moAttributesStatus = false;
            }
        }
        return moAttributesStatus;
    }

    /**
     * This method is used to return the node Fdn.
     * 
     * @param nodeName
     * @return nodeFdn
     * 
     */
    public String getManagedElementFdn(final String nodeName) {
        final String managedElementMoFdn = cppNodeRestartMOUtil.getManagedElementMOFdnByNodeName(nodeName);
        return managedElementMoFdn;
    }

    /**
     * This method is used to return the status of MO
     * 
     * @param nodeName
     * @return flag
     * 
     */
    public boolean isNodeReachable(final String nodeName) {
        final ManagedObject managedElementMo = cppNodeRestartMOUtil.getManagedElementMOByNodename(nodeName);
        return getMoAttributesStatus(managedElementMo);
    }

    /**
     * This method retrieves the Managed Element and it's MO attributes.
     * 
     * @param nodeName
     * @return Map<String, Object>
     */
    public Map<String, Object> getManagedElementAttributes(final String nodeName) {
        final Map<String, Object> moAttributesMap = new HashMap<String, Object>();
        final ManagedObject managedElementMo = cppNodeRestartMOUtil.getManagedElementMOByNodename(nodeName);
        if (managedElementMo != null) {
            moAttributesMap.put(ShmConstants.FDN, managedElementMo.getFdn());
            moAttributesMap.put(ShmConstants.MO_ATTRIBUTES, managedElementMo.getAllAttributes());
        }
        return moAttributesMap;
    }
}
