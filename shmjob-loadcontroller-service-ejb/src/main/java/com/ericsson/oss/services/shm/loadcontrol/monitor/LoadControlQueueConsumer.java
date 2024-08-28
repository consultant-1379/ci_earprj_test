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
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.loadcontrol.schedule.TimerContext;
import com.ericsson.oss.services.shm.loadcontrol.util.ChannelURIBuilder;

public class LoadControlQueueConsumer {

    private static final String CONNECTION_FACTORY = "java:/ShmConnectionFactory";
    private Connection connection = null;
    private Session session = null;
    private MessageConsumer messageConsumer = null;

    private final static Logger LOGGER = LoggerFactory.getLogger(LoadControlQueueConsumer.class);

    public void initialize(final TimerContext timerContext) {

        try {
            LOGGER.trace("LoadControlQueueConsumer initializing");
            final InitialContext context = new InitialContext();
            final ConnectionFactory connFactory = (ConnectionFactory) context.lookup(CONNECTION_FACTORY);
            connection = connFactory.createConnection();
            final ExceptionListener exceptionlistener = new LoadControlQueueConsumerException(connection);
            connection.setExceptionListener(exceptionlistener);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            final String channelJNDIName = ChannelURIBuilder.buildChannelJNDIName(timerContext.getPlatform(), timerContext.getJobType(), timerContext.getActivity());
            final Queue queue = (Queue) context.lookup(channelJNDIName);
            messageConsumer = session.createConsumer(queue);
            connection.start();
            LOGGER.trace("LoadControlQueueConsumer initialized for channelJNDIName={}.", channelJNDIName);
        } catch (NamingException | JMSException e) {
            throw new LoadControlQueueConsumerException(e);
        }
    }

    public SHMActivityRequest getQueuedRequest(final long consumerTimeout) {
        try {
            ObjectMessage poppedRequest = null;
            if (messageConsumer != null) {
                poppedRequest = (ObjectMessage) messageConsumer.receive(consumerTimeout);
            }
            if (poppedRequest == null) {
                LOGGER.debug("Queue is not initialized properly or No request is there in the queue");
                return null;
            }
            LOGGER.trace("poppedRequest [destination::{}, propertyNames::{}, consumer::{}]", poppedRequest.getJMSDestination(), poppedRequest.getPropertyNames(), messageConsumer);
            final SHMActivityRequest shmActivityRequest = (SHMActivityRequest) poppedRequest.getObject();
            LOGGER.trace("Body content of JMS message is::{}", shmActivityRequest);
            return shmActivityRequest;
        } catch (final JMSException e) {
            throw new LoadControlQueueConsumerException(e);
        }

    }

    public void destroy() {
        if (session != null) {
            try {
                session.close();
            } catch (final JMSException e) {

                LOGGER.error("Error caught while closing the JMS session : ", e);
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (final JMSException e) {
                LOGGER.error("Error caught while closing the JMS connection :", e);
            }
        }
    }

}
