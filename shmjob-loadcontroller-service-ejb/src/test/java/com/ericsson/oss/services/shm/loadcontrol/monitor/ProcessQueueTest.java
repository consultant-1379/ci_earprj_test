/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.loadcontrol.monitor;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.loadcontrol.impl.ConfigurationParamProvider;
import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControllerPersistenceManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ LoadControlQueueConsumer.class, InitialContext.class, ProcessQueue.class })
public class ProcessQueueTest {

    @Mock
    private ConfigurationParamProvider maxCountProvider;

    @Mock
    private LoadControllerPersistenceManager loadControllerPersistenceManager;

    @InjectMocks
    private ProcessQueue processQueue;

    @Mock
    private LoadControlQueueConsumer consumer;

    @Mock
    private InitialContext context;

    @Mock
    private ConnectionFactory connFactory;

    @Mock
    private Connection connection;

    @Mock
    private Session session;

    @Mock
    private Queue queue;

    @Mock
    MessageConsumer messageConsumer;

    private static final String CONNECTION_FACTORY = "java:/ShmConnectionFactory";
    private static final String QUEUE_NAME = "java:jboss/exported/jms/queue/ShmCppBackupJobExportcvActvityQueue";
    private static final long CONSUMER_TIMEOUT = 3l;

    @Test
    public void testPersistQueueMessagesIntoDB() {
        try {
            final SHMActivityRequest shmActivityRequest = new SHMActivityRequest();
            when(maxCountProvider.getLoadControlQueueConsumerTimeout()).thenReturn(CONSUMER_TIMEOUT);
            PowerMockito.whenNew(LoadControlQueueConsumer.class).withNoArguments().thenReturn(consumer);
            PowerMockito.whenNew(InitialContext.class).withNoArguments().thenReturn(context);
            when(context.lookup(CONNECTION_FACTORY)).thenReturn(connFactory);
            when(connFactory.createConnection()).thenReturn(connection);
            when(connection.createSession(Matchers.anyBoolean(), Matchers.anyInt())).thenReturn(session);
            when(context.lookup(QUEUE_NAME)).thenReturn(queue);
            when(session.createConsumer(queue)).thenReturn(messageConsumer);
            when(consumer.getQueuedRequest(CONSUMER_TIMEOUT)).thenReturn(shmActivityRequest, shmActivityRequest, shmActivityRequest, null);
            processQueue.persistQueueMessagesIntoDB();
            verify(loadControllerPersistenceManager, times(3)).keepRequestInDB(Matchers.any(SHMActivityRequest.class));
        } catch (final Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testPersistQueueMessagesIntoDBWhenNomessages() {
        try {
            when(maxCountProvider.getLoadControlQueueConsumerTimeout()).thenReturn(CONSUMER_TIMEOUT);
            PowerMockito.whenNew(LoadControlQueueConsumer.class).withNoArguments().thenReturn(consumer);
            PowerMockito.whenNew(InitialContext.class).withNoArguments().thenReturn(context);
            when(context.lookup(CONNECTION_FACTORY)).thenReturn(connFactory);
            when(connFactory.createConnection()).thenReturn(connection);
            when(connection.createSession(Matchers.anyBoolean(), Matchers.anyInt())).thenReturn(session);
            when(context.lookup(QUEUE_NAME)).thenReturn(queue);
            when(session.createConsumer(queue)).thenReturn(messageConsumer);
            when(consumer.getQueuedRequest(CONSUMER_TIMEOUT)).thenReturn(null);
            processQueue.persistQueueMessagesIntoDB();
            verify(loadControllerPersistenceManager, times(0)).keepRequestInDB(Matchers.any(SHMActivityRequest.class));
            verify(consumer, times(ActivityEnum.values().length)).destroy();
        } catch (final Exception e) {
            Assert.fail();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPersistQueueMessagesIntoDBWhenExceptionthrown() {
        try {
            when(maxCountProvider.getLoadControlQueueConsumerTimeout()).thenReturn(CONSUMER_TIMEOUT);
            PowerMockito.whenNew(LoadControlQueueConsumer.class).withNoArguments().thenReturn(consumer);
            PowerMockito.whenNew(InitialContext.class).withNoArguments().thenReturn(context);
            when(context.lookup(CONNECTION_FACTORY)).thenReturn(connFactory);
            when(connFactory.createConnection()).thenReturn(connection);
            when(connection.createSession(Matchers.anyBoolean(), Matchers.anyInt())).thenReturn(session);
            when(context.lookup(QUEUE_NAME)).thenReturn(queue);
            when(session.createConsumer(queue)).thenReturn(messageConsumer);
            when(consumer.getQueuedRequest(CONSUMER_TIMEOUT)).thenThrow(Exception.class);
            processQueue.persistQueueMessagesIntoDB();
            verify(loadControllerPersistenceManager, times(0)).keepRequestInDB(Matchers.any(SHMActivityRequest.class));
            verify(consumer, times(ActivityEnum.values().length)).destroy();
        } catch (final Exception e) {
            Assert.fail();
        }
    }
}
