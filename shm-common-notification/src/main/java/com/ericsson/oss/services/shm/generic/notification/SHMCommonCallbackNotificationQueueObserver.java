package com.ericsson.oss.services.shm.generic.notification;

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

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification;

/**
 * Listens for Software Upgrade Job progress Notifications
 *
 * @author xgowbom
 */
public class SHMCommonCallbackNotificationQueueObserver implements
		EMessageListener<SHMCommonCallbackNotification> {

	@Inject
	private SHMCommonCallbackNotificationHandler shmCommonCallbackNotificationHandler;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(SHMCommonCallbackNotificationQueueObserver.class);

	/**
	 * Listens to the mediation response of SHMCommonCallbackNotification type from shm
	 * notification queue and delegates it to corresponding handler for further
	 * processing.
	 *
	 * @param SHMCommonCallbackNotification
	 *            SHMCommonCallbackNotification
	 */
	@Override
	public void onMessage( final SHMCommonCallbackNotification shmCommonCallbackNotification) {
	    if(shmCommonCallbackNotification instanceof SHMCommonCallbackNotification){
		    LOGGER.debug("shmCommonCallbackNotification received at SHM " + shmCommonCallbackNotification.getFdn());
		    final Date notificationReceivedDate = new Date();
		    shmCommonCallbackNotificationHandler.handleJobProgressResponse(shmCommonCallbackNotification, notificationReceivedDate);
	    } 
	    else{
	        LOGGER.debug("Discarding the notification as it is not SHMCommonCallbackNotification for nodeFdn: {} with Response as: {} " ,
	                      shmCommonCallbackNotification.getFdn(),shmCommonCallbackNotification);
	    }
	}
}
