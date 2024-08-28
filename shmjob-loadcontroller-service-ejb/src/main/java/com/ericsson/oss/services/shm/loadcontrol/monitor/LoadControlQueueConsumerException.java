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

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadControlQueueConsumerException extends RuntimeException implements ExceptionListener {
    private final static Logger LOGGER = LoggerFactory.getLogger(LoadControlQueueConsumerException.class);
    private Connection conn;

    /**
     * 
     */
    private static final long serialVersionUID = 5374050652218142703L;

    LoadControlQueueConsumerException(final Exception e) {
        super(e);
    }

    /**
     * @param connection
     */
    public LoadControlQueueConsumerException(final Connection connection) {
        super();
        this.conn = connection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.ExceptionListener#onException(javax.jms.JMSException)
     */
    @Override
    public void onException(final JMSException exception) {
        try {
            conn.close();
            LOGGER.info("JMS connection is closed");
        } catch (final JMSException je) {
            LOGGER.error("Error caught while closing the JMS connection :: ", je);
        }
    }
}
