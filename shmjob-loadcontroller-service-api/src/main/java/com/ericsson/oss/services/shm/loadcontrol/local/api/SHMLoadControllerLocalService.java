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
package com.ericsson.oss.services.shm.loadcontrol.local.api;

import java.util.concurrent.atomic.AtomicLong;

import javax.ejb.Local;

import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.es.api.SHMLoadControllerCounterRequest;
import com.ericsson.oss.services.shm.loadcontrol.constants.LoadControlCheckState;

@Local
public interface SHMLoadControllerLocalService {

    LoadControlCheckState checkAllowance(SHMActivityRequest shmActivityRequest);

    void decrementCounter(SHMActivityRequest shmActivityRequest);

    boolean incrementCounter(SHMActivityRequest activityRequest);

    AtomicLong getCurrentLoadControllerValue(String platform, String jobType, String name);

    void decrementGlobalCounter(SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest);

    boolean incrementGlobalCounter(SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest);

    /**
     * This method is being invoked from WebPush module to ensure that all the ShmStagedActivity POs are deleted after completion of its respective activity.
     * 
     * @param activityJobId
     */
    void deleteShmStageActivity(long activityJobId);

}
