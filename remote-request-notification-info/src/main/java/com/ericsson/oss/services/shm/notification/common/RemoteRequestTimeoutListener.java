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
package com.ericsson.oss.services.shm.notification.common;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

/**
 * Used to listen the Remote Request Timeout value changes
 * 
 * @author xneranu
 * 
 */
@ApplicationScoped
public class RemoteRequestTimeoutListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteRequestTimeoutListener.class);
    
    private static final String REMOTE_REQUEST_TIMEOUT_VALUE= "remote_request_timeout_value";
    
    @Inject
    @Configured(propertyName = REMOTE_REQUEST_TIMEOUT_VALUE)
    private int remoteRequestTimeoutValue;
    
    void listenForRemoteRequestTimeoutValueChange(@Observes @ConfigurationChangeNotification(propertyName = REMOTE_REQUEST_TIMEOUT_VALUE) final int remoteRequestTimeoutValue) {
        this.remoteRequestTimeoutValue = remoteRequestTimeoutValue;
        LOGGER.info("remoteRequestTimeout value : {}", remoteRequestTimeoutValue);
    }

    public int getRemoteRequestTimeoutValue() {
        return remoteRequestTimeoutValue;
    }
    

}
