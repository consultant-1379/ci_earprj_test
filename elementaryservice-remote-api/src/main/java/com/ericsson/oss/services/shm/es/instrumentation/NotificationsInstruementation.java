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
package com.ericsson.oss.services.shm.es.instrumentation;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;

@EService
@Remote
public interface NotificationsInstruementation {

    /**
     * Captures the given time duration synchronously and record the system event for every 10 minutes, to be processed by DDP to show the metrics.
     * <p>
     * Consumers : NHC and SHM
     * 
     * @param notificationProcessingTimeInMillis
     */
    void capture(long notificationProcessingTimeInMillis);

}
