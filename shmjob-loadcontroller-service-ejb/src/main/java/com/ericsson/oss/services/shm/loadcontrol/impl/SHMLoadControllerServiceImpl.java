package com.ericsson.oss.services.shm.loadcontrol.impl;

import java.util.concurrent.atomic.AtomicLong;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface;
import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.es.api.SHMLoadControllerCounterRequest;
import com.ericsson.oss.services.shm.loadcontrol.constants.LoadControlCheckState;
import com.ericsson.oss.services.shm.loadcontrol.instrumentation.LoadControlInstrumentationBean;
import com.ericsson.oss.services.shm.loadcontrol.local.api.LoadControllerLocalCache;
import com.ericsson.oss.services.shm.loadcontrol.local.api.SHMLoadControllerLocalService;
import com.ericsson.oss.services.shm.loadcontrol.remote.api.SHMLoadControllerRemoteService;

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
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SHMLoadControllerServiceImpl implements SHMLoadControllerRemoteService, SHMLoadControllerLocalService {

    @Inject
    private LoadControlCounterManager counterManager;

    @Inject
    private ConfigurationParamProvider maxCountProvider;

    @Inject
    private LoadControlInstrumentationBean loadControlInstrumentationBean;

    @Inject
    private LoadControllerPersistenceManager loadControllerPersistenceManager;

    @Inject
    private MembershipListenerInterface membershipListenerInterface;
    
    @Inject
    LoadControlActivityNameBuilderFactory loadControllerActivityNameManagerFactory;
    
    @Inject
    private LoadControllerLocalCache loadControllerLocalCache;

    private static final Logger LOGGER = LoggerFactory.getLogger(SHMLoadControllerServiceImpl.class);

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.service.shm.shmjob.loadbalance.service.api. SHMLoadControllerService #checkAllowance(com.ericsson.oss.services.shm.es.api.SHMActivityRequest)
     */
    @Override
    public LoadControlCheckState checkAllowance(final SHMActivityRequest activityRequest) {
        LOGGER.debug("Entering LoadControllerService... Checking for allowance :{}", activityRequest);
        if (maxCountProvider.getMaximumCount(activityRequest.getPlatformType(), activityRequest.getJobType(), activityRequest.getActivityName()) <= 0) {
            return LoadControlCheckState.BYPASSED;
        }
        final boolean allow = incrementCounter(activityRequest);
        if (!allow) {
            loadControllerPersistenceManager.keepRequestInDB(activityRequest);
            LOGGER.info("Exiting checkAllowance with LoadControlCheckState NOTPERMITTED for {}", activityRequest);
            return LoadControlCheckState.NOTPERMITTED;
        }

        LOGGER.trace("Exiting checkAllowance with LoadControlCheckState PERMITTED");

        return LoadControlCheckState.PERMITTED;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.service.shm.shmjob.loadbalance.service.api. SHMLoadControllerService#incrementCounter()
     */
    @Override
    public boolean incrementCounter(final SHMActivityRequest activityRequest) {
        LOGGER.trace("Incrementing counter : {}", activityRequest.toString());
        final String counterName = counterManager.getCounterName(activityRequest.getPlatformType(), activityRequest.getJobType(), activityRequest.getActivityName());
        final AtomicLong counter = counterManager.getCounter(counterName);
        final long maximumValue = maxCountProvider.getMaximumCount(activityRequest.getPlatformType(), activityRequest.getJobType(), activityRequest.getActivityName());
        boolean acquired = false;
        long count = -1;
        LOGGER.trace(" Incrementing current counter value {} with countername {} for request:{}",counter.get(), counterName, activityRequest);
        while ((count = counter.get()) < maximumValue) {
            if (counter.compareAndSet(count, count + 1)) {
                acquired = true;
                loadControlInstrumentationBean.updateCurrentCountJmx(counterName, count + 1);
                loadControllerLocalCache.addActivityJobIdToCache(activityRequest.getActivityJobId());
                LOGGER.info("Counter incremented by 1 for {} and current value is {} , max value is {}", activityRequest, count + 1, maximumValue);
                break;
            }
        }

        if (!acquired) {
            LOGGER.error("Counter not acquired , for {} current value is {} , maximum value is {} ", activityRequest, count, maximumValue);
        }
        LOGGER.trace("Exiting Increment counter with acquired = {}", acquired);
        return acquired;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.service.shm.shmjob.loadbalance.service.api. SHMLoadControllerService#decrementCounter()
     */
    @Override
    public void decrementCounter(final SHMActivityRequest activityRequest) {
        LOGGER.trace(" Decrementing counter :{}", activityRequest.toString());
        
        setLoadControllerActivityNameInDecrementCounter(activityRequest);
        
        final String counterName = counterManager.getCounterName(activityRequest.getPlatformType(), activityRequest.getJobType(), activityRequest.getActivityName());
        final AtomicLong counter = counterManager.getCounter(counterName);
        boolean acquired = false;
        long count = -1;
        LOGGER.trace(" Decrementing current counter value {} with countername {} for request:{}",counter.get(), counterName, activityRequest);
        while ((count = counter.get()) > 0) {
            if (counter.compareAndSet(count, count - 1)) {
                acquired = true;
                loadControlInstrumentationBean.updateCurrentCountJmx(counterName, count - 1);
                LOGGER.trace("Counter decremented by 1  for {} and current value is {}", activityRequest, count - 1);
                break;
            }
        }

        if (!acquired) {
            LOGGER.error("Decrement skipped for {} , current count is : {}", activityRequest, count);
        }
        LOGGER.trace("Exiting Decrement counter");
    }

    /**
     * @param activityRequest
     */
    private void setLoadControllerActivityNameInDecrementCounter(final SHMActivityRequest activityRequest) {
        final LoadControlActivityNameBuilder loadControlActivityNameBuilder = loadControllerActivityNameManagerFactory.getLoadControlActivityNameBuilder(activityRequest.getPlatformType(),
                activityRequest.getJobType());
        final String activityName = loadControlActivityNameBuilder.buildActivityName(activityRequest.getActivityName());
        activityRequest.setActivityName(activityName);
    }

    @Override
    public AtomicLong getCurrentLoadControllerValue(final String platform, final String jobType, final String name) {
        return counterManager.getCounter(counterManager.getCounterName(platform, jobType, name));
    }

    @Override
    public boolean incrementGlobalCounter(final SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest) {
        LOGGER.trace("Incrementing Global counter for: {}", shmLoadControllerLocalCounterRequest);
        
        loadControllerActivityNameBuilder(shmLoadControllerLocalCounterRequest);
        final String counterName = shmLoadControllerLocalCounterRequest.getPlatformType() + shmLoadControllerLocalCounterRequest.getJobType() + shmLoadControllerLocalCounterRequest.getActivityName();
        final AtomicLong globalCounter = counterManager.getGlobalCounter(counterName);
        final Long maxLocalCounterValue = maxCountProvider.getMaximumCountByCounterKey(counterName);
        final long maximumValue = (maxLocalCounterValue * membershipListenerInterface.getCurrentMembersCount());
        
        boolean acquired = false;
        long count = -1;
        LOGGER.trace(" Incrementing Global counter value {} with countername {} maxLocalCounterValue is {} and maximumValue is {} for request:{}",globalCounter.get(), counterName, maxLocalCounterValue, maximumValue, shmLoadControllerLocalCounterRequest);
        while ((count = globalCounter.get()) < maximumValue) {
            if (globalCounter.compareAndSet(count, count + 1)) {
                acquired = true;
                LOGGER.trace("GlobalCounter incremented by 1 for {} and current value is {} , max value is {}", shmLoadControllerLocalCounterRequest, count + 1, maximumValue);
                break;
            }
        }

        if (!acquired) {
            LOGGER.error("GlobalCounter not acquired , for {} current value is {} , maximum value is {} ", shmLoadControllerLocalCounterRequest, count, maximumValue);
        }
        LOGGER.trace("Exiting Increment GlobalCounter for {} with acquired = {}", shmLoadControllerLocalCounterRequest, acquired);
        return acquired;
    }

    /**
     * @param shmLoadControllerLocalCounterRequest
     */
    private void loadControllerActivityNameBuilder(final SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest) {
        final LoadControlActivityNameBuilder loadControlActivityNameBuilder = loadControllerActivityNameManagerFactory
                .getLoadControlActivityNameBuilder(shmLoadControllerLocalCounterRequest.getPlatformType(), shmLoadControllerLocalCounterRequest.getJobType());
        final String activityName = loadControlActivityNameBuilder.buildActivityName(shmLoadControllerLocalCounterRequest.getActivityName());
        shmLoadControllerLocalCounterRequest.setActivityName(activityName);
    }

    @Override
    public void decrementGlobalCounter(final SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest) {
        LOGGER.trace("Decrementing GlobalCounter for:{}", shmLoadControllerLocalCounterRequest);
        
        loadControllerActivityNameBuilder(shmLoadControllerLocalCounterRequest);
        
        final String counterName = shmLoadControllerLocalCounterRequest.getPlatformType() + shmLoadControllerLocalCounterRequest.getJobType() + shmLoadControllerLocalCounterRequest.getActivityName();
        final AtomicLong globalCounter = counterManager.getGlobalCounter(counterName);
        
        boolean acquired = false;
        long count = -1;
        LOGGER.trace(" Decrementing Global counter value {} with countername {} for request:{}",globalCounter.get(), counterName, shmLoadControllerLocalCounterRequest);
        while ((count = globalCounter.get()) > 0) {
            if (globalCounter.compareAndSet(count, count - 1)) {
                acquired = true;
                LOGGER.trace("GlobalCounter decremented by 1  for {} and current value is {}", shmLoadControllerLocalCounterRequest, count - 1);
                break;
            }
        }
        if (!acquired) {
            LOGGER.error("Decrement skipped for {} ,current globalCount is : {}", shmLoadControllerLocalCounterRequest, count);
        }
        LOGGER.trace("Exiting Decrement counter for {}", shmLoadControllerLocalCounterRequest);
    }

    @Override
    public void deleteShmStageActivity(final long activityJobId) {
        loadControllerPersistenceManager.deleteStagedActivityPOs(activityJobId);
    }
}
