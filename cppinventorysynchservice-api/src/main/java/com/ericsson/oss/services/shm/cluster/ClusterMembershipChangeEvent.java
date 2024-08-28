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
package com.ericsson.oss.services.shm.cluster;

/**
 * This POJO class represents an event generated in SHM when a MembershipChangeEvent is received
 * 
 * @author xprapav
 * 
 */

public class ClusterMembershipChangeEvent {

    private final String message;

    public ClusterMembershipChangeEvent(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
