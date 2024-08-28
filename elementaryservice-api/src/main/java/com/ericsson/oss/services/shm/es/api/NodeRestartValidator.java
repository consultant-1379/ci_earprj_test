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
package com.ericsson.oss.services.shm.es.api;

/**
 * Interface to define the validation for node reachability specific to platform type .
 */
public interface NodeRestartValidator {

    /**
     * this method is used to get Managed Object Up And Running Status.
     * 
     * @param activityJobId
     * @return
     */
    boolean isNodeReachable(final String nodeName);

}
