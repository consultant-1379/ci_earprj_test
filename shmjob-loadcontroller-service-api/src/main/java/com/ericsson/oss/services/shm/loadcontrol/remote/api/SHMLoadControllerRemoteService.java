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
package com.ericsson.oss.services.shm.loadcontrol.remote.api;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.es.api.SHMLoadControllerCounterRequest;
import com.ericsson.oss.services.shm.loadcontrol.constants.LoadControlCheckState;

@EService
@Remote
public interface SHMLoadControllerRemoteService {

    LoadControlCheckState checkAllowance(SHMActivityRequest shmActivityRequest);

    void decrementCounter(SHMActivityRequest shmActivityRequest);

    void decrementGlobalCounter(SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest);

}