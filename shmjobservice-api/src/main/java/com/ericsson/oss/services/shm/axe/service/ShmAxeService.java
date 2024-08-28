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
package com.ericsson.oss.services.shm.axe.service;

import com.ericsson.oss.services.shm.jobservice.axe.OpsInputData;
import com.ericsson.oss.services.shm.jobservice.axe.OpsResponseData;

/**
 * This interface is used to provide business logic for ShmAxeResource
 * 
 * @author Team Royals
 *
 */
public interface ShmAxeService {

    /**
     * This method will read ops cluster id and session id for axe nodes from currently running activity job
     * 
     * @param opsInputData
     * @return
     */
    OpsResponseData getSessionIdAndClusterId(final OpsInputData opsInputData);
}
