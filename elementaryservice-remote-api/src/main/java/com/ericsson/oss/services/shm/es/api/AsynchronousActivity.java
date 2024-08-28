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
package com.ericsson.oss.services.shm.es.api;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;

/**
 * This is a new interface through shm-workflows will make calls towards sync service in an asynchronous manner for all the blocks e.g. precheck, timeout.
 * 
 * 
 */
@EService
@Remote
public interface AsynchronousActivity {

    void asyncPrecheck(long activityJobId);

    void precheckHandleTimeout(long activityJobId);

    void asyncHandleTimeout(long activityJobId);

    void timeoutForAsyncHandleTimeout(long activityJobId);

}
