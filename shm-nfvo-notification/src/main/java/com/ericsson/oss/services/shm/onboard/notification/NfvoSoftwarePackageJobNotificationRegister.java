/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.onboard.notification;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.classic.ModeledEventConsumerBean;
import com.ericsson.oss.mediation.vran.model.response.NfvoSwPackageMediationResponse;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;

/**
 * 
 * Service to register the observer to observe the NFVO software package job response.
 * 
 * @author xjhosye
 */
@Singleton
@Startup
public class NfvoSoftwarePackageJobNotificationRegister {

    private static final Logger LOGGER = LoggerFactory.getLogger(NfvoSoftwarePackageJobNotificationRegister.class);

    @Inject
    private NfvoSoftwarePackageJobProgressQueueObserver onBoardJobProgressQueueListener;

    private ModeledEventConsumerBean modeledeventConsumerBean;

    /**
     * Subscribes for Software package Onboard and delete job events and listens for notifications on a Queue
     */
    @PostConstruct
    protected void startListener() {
        LOGGER.info("Starting listener to listen onboard and delete software package notifications sent from mediation");
        modeledeventConsumerBean = getModeledEventConsumerBean();
        final boolean isListening = startListeningForJobNotifications(modeledeventConsumerBean);
        LOGGER.info("Subscribed to Queue for receiving onboard and delete software package notifications, is listener started: {} ", isListening);
    }

    /**
     * To stop listening for job notifications on a Queue (<code>jms:/queue/shmNotificationQueue</code>)
     * 
     * @return - boolean
     */
    public boolean stopNotificationListener() {
        return modeledeventConsumerBean.stopListening();
    }

    /**
     * Building modeled event consumer bean of type NfvoSwPackageMediationResponse
     * 
     * @return - ModeledEventConsumerBean
     */
    protected ModeledEventConsumerBean getModeledEventConsumerBean() {
        final ModeledEventConsumerBean modeledeventConsumerBean = new ModeledEventConsumerBean.Builder(NfvoSwPackageMediationResponse.class)
                .filter(VranJobConstants.VRAN_ONBOARD_JOB_PROGRESS_NOTIFICATION_FILTER).build();
        return modeledeventConsumerBean;
    }

    /**
     * To start listening for job Notifications using modeled event consumer bean
     */
    protected boolean startListeningForJobNotifications(final ModeledEventConsumerBean modeledeventConsumerBean) {
        LOGGER.debug("Listening for the OnBoard and delte job notfications {}", modeledeventConsumerBean);
        return modeledeventConsumerBean.startListening(onBoardJobProgressQueueListener);
    }
}
