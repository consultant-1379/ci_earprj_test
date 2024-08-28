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
package com.ericsson.oss.services.shm.es.api;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;

/**
 * This is a new interface through which shm-workflows will make calls towards sync service for polling subscription
 * 
 * @author xsrirda
 */
@EService
@Remote
public interface AsynchronousPollingActivity {

    /**
     * This will be called by work-flow whenever the best timer expires
     * 
     * @param activityJobId
     *            Id for the activity
     * 
     */
    void subscribeForPolling(final long activityJobId);

}
