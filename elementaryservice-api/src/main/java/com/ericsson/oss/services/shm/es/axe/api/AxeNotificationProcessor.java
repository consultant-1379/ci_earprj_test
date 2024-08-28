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
package com.ericsson.oss.services.shm.es.axe.api;

import java.util.Map;

import javax.ejb.Local;

/**
 * Interface to process the notifications received from OPS during script execution.
 * 
 * @author tcsvnag
 *
 */
@Local
public interface AxeNotificationProcessor {

    /**
     * Processes notifications received.
     * 
     * @param opsResponseAttributes
     */
    void processNotification(Map<String, Object> opsResponseAttributes);

}
