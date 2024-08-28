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
package com.ericsson.oss.services.shm.es.noderestart;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.annotations.PlatformAnnotation;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.NodeRestartValidator;

/**
 * Implementation to fetch the Node Restart validation class specific to in put
 * platform type.
 * 
 * @author
 */
@ApplicationScoped
public class NodeRestartPlatformFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    @PlatformAnnotation(name = PlatformTypeEnum.CPP)
    private NodeRestartValidator cppNodeRestartValidatorImpl;

    /**
     * Method to fetch the Node Restart validation class specific to input
     * platform type.
     * 
     * @return specific implementation of NodeRestartValidator or null in case
     *         of non-existence of validator.
     */
    public NodeRestartValidator getNodeRestartValidator(final PlatformTypeEnum platformType) {
        NodeRestartValidator nodeRestartValidator = null;
        if (platformType == PlatformTypeEnum.CPP) {
            nodeRestartValidator = cppNodeRestartValidatorImpl;
        } else {
            logger.warn("Unspecified Qualifier : {}", platformType);
        }
        return nodeRestartValidator;
    }

}