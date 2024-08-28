/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.vran.software.upgrade.notification;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.classic.ModeledEventConsumerBean;
import com.ericsson.oss.mediation.vran.model.response.VranUpgradeJobResponse;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;

/**
 * Service to register the observer to observe the VRAN software upgrade job response.
 * 
 */
@Singleton
@Startup
public class VranSoftwareUpgradeJobNotificationRegister {

    private static final Logger LOGGER = LoggerFactory.getLogger(VranSoftwareUpgradeJobNotificationRegister.class);

    @Inject
    private VranSoftwareUpgradeJobProgressQueueObserver vranJobProgressQueueListener;

    private ModeledEventConsumerBean modeledeventConsumerBean;

    /**
     * Subscribes for vRAN Software Upgrade job events and listens for notifications on a Queue
     */
    @PostConstruct
    protected void startListener() {
        LOGGER.info("Starting listener to listen vran software upgrade notifications sent from mediation");
        modeledeventConsumerBean = buildModeledEventConsumerBean();
        final boolean isListening = startListeningForJobNotifications(modeledeventConsumerBean);
        LOGGER.info("Subscribing to queue for receiving vran software upgrade notifications, and is listener started: {} ", isListening);
    }

    /**
     * Stops listening for Software Upgrade job notifications on a Queue (<code>jms:/queue/shmNotificationQueue</code>)
     * 
     * @return - boolean
     */
    public boolean stopNotificationListener() {
        return modeledeventConsumerBean.stopListening();
    }

    /**
     * Building modeled event consumer bean of type VranUpgradeJobResponse
     * 
     * @return - ModeledEventConsumerBean
     */
    protected ModeledEventConsumerBean buildModeledEventConsumerBean() {
        final ModeledEventConsumerBean modeledeventConsumerBean = new ModeledEventConsumerBean.Builder(VranUpgradeJobResponse.class)
                .filter(VranJobConstants.VRAN_UPGRADE_JOB_PROGRESS_NOTIFICATION_FILTER).build();
        return modeledeventConsumerBean;
    }

    /**
     * To start listening for job Notifications using modeled event consumer bean
     */
    protected boolean startListeningForJobNotifications(final ModeledEventConsumerBean modeledeventConsumerBean) {
        LOGGER.debug("Listening for the VRAN upgrade job progress notfications {}", modeledeventConsumerBean);
        return modeledeventConsumerBean.startListening(vranJobProgressQueueListener);
    }
}
