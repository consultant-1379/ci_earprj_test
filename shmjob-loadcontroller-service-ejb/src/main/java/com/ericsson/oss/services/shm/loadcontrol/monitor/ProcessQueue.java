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
package com.ericsson.oss.services.shm.loadcontrol.monitor;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.loadcontrol.impl.ConfigurationParamProvider;
import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControllerPersistenceManager;
import com.ericsson.oss.services.shm.loadcontrol.schedule.TimerContext;

@Profiled
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ProcessQueue {

    @Inject
    private ConfigurationParamProvider maxCountProvider;

    @Inject
    private LoadControllerPersistenceManager loadControllerPersistenceManager;

    private final static Logger logger = LoggerFactory.getLogger(ProcessQueue.class);

    /**
     * 
     * This method will read all activity Queue messages and persist them in DB and it is for maintaining backward compatibility.
     * 
     * @param timerContext
     */
    @Asynchronous
    public void persistQueueMessagesIntoDB() {
        for (final ActivityEnum a : ActivityEnum.values()) {
            final TimerContext timerContext = new TimerContext();
            timerContext.setActivity(a.getName());
            timerContext.setPlatform(a.getPlatform());
            timerContext.setJobType(a.getJobType());
            logger.debug("persisting of QueueMessages Into DB for activity {} , Platform {} and JobType{}", timerContext.getActivity(),timerContext.getPlatform(),timerContext.getJobType());
            LoadControlQueueConsumer consumer = null;
            final long startTime = System.nanoTime();
            SHMActivityRequest poppedRequest = null;
            try {
                consumer = new LoadControlQueueConsumer();
                consumer.initialize(timerContext);
                while ((poppedRequest = consumer.getQueuedRequest(maxCountProvider.getLoadControlQueueConsumerTimeout())) != null) {
                    logger.debug("getting poppedRequest at On StartUp {}", poppedRequest);
                    loadControllerPersistenceManager.keepRequestInDB(poppedRequest);
                }
            } catch (final Exception e) {
                logger.warn("Problem occured in initializing the queue consumer/QueueNameNotFound or persisting into DB {}" , e.getMessage());
            } finally {
                if (consumer != null) {
                    consumer.destroy();
                }
                final long endTime = System.nanoTime();
                logger.info("Persisting of Queue messages into DB took {} seconds , timercontext::{}", ((endTime - startTime) * 1E-9), timerContext);
            }
        }
    }
}
