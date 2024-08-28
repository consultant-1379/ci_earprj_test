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
package com.ericsson.oss.service.shm.loadcontrol.schedule;

import javax.jms.*;
import javax.naming.InitialContext;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.slf4j.Logger;

import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.loadcontrol.monitor.LoadControlQueueConsumer;

@RunWith(MockitoJUnitRunner.class)
@PrepareForTest({ LoadControlQueueConsumer.class, InitialContext.class })
public class LoadControlQueueConsumerTest {

    @InjectMocks
    LoadControlQueueConsumer loadControlQueueConsumer;

    @Mock
    private InitialContext context;

    @Mock
    private ConnectionFactory connFactory;

    @Mock
    private Session session;

    @Mock
    private Connection connection;

    @Mock
    private Queue queue;

    @Mock
    private MessageConsumer messageConsumer;

    @Mock
    private ObjectMessage mockObjectMessage;

    @Mock
    private SHMActivityRequest activityRequest;

    @Mock
    private Logger logger;

    private static final String CONNECTION_FACTORY = "java:/ConnectionFactory";

    /**
     * When JMS MessageConsumer is not initialized properly or ConnectionFactory is not available JMSException should be thrown and there by null should be returned
     * 
     * @throws JMSException
     */
    @Test
    public void testGetNullRequestWhenMessageConsumerIsNotAvailable() throws JMSException {
        messageConsumer = null;
        SHMActivityRequest activityRequest = loadControlQueueConsumer.getQueuedRequest(1);
        Assert.assertNull(activityRequest);

    }

    /**
     * When Proper message is popped from the queue, SHMActivityRequest Object (non null) should be returned
     * 
     * @throws JMSException
     */
    @Test
    public void testGetRequestWhenProperMessageWasPoppedFromQueue() throws JMSException {

        Mockito.when(messageConsumer.receive(100)).thenReturn(mockObjectMessage);
        Mockito.when(mockObjectMessage.getObject()).thenReturn(activityRequest);
        SHMActivityRequest actualRequest = loadControlQueueConsumer.getQueuedRequest(100);
        Assert.assertNotNull(actualRequest);
        Assert.assertEquals(activityRequest, actualRequest);
    }

    /**
     * When there is no message in the queue, null should be returned
     * 
     * @throws JMSException
     */
    @Test
    public void testGetRequestWhenWhenThereIsNoMessageInTheQueue() throws JMSException {

        Mockito.when(messageConsumer.receive(100)).thenReturn(null);
        SHMActivityRequest actualRequest = loadControlQueueConsumer.getQueuedRequest(100);
        Assert.assertNull(actualRequest);
    }

    @Test
    public void testDestroyInSuccessfullyClosedConnection() throws JMSException {

        loadControlQueueConsumer.destroy();
        Mockito.verify(session, Mockito.atMost(1)).close();
        Mockito.verify(connection, Mockito.atMost(1)).close();

    }

    @Test
    public void testDestroyThrowJMSExceptionWhenClosingSession() throws JMSException {

        Mockito.doThrow(JMSException.class).when(session).close();
        loadControlQueueConsumer.destroy();
        Mockito.verify(session, Mockito.atMost(1)).close();
        Mockito.verify(connection, Mockito.atMost(1)).close();

    }

}
