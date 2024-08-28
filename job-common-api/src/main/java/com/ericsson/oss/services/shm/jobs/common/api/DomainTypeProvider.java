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
package com.ericsson.oss.services.shm.jobs.common.api;

import java.util.Set;

/**
 * To provide the DomainType Information
 * 
 * 
 * @author xnagvar
 * 
 */
public interface DomainTypeProvider {

    /**
     * Provides the response (domain & type ) for given inputs.
     * 
     * @param neInfoQuery
     * @return
     */
    Set<String> getDomainTypeList(NeInfoQuery neInfoQuery);

}
