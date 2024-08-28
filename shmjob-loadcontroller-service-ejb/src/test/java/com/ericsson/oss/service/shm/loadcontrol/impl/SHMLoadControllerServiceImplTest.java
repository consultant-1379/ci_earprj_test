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
package com.ericsson.oss.service.shm.loadcontrol.impl;

import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.sdk.cluster.counter.NamedCounter;
import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator;
import com.ericsson.oss.services.shm.cluster.MembershipListenerInterface;
import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.es.api.SHMLoadControllerCounterRequest;
import com.ericsson.oss.services.shm.loadcontrol.constants.LoadControlCheckState;
import com.ericsson.oss.services.shm.loadcontrol.impl.ConfigurationParamProvider;
import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControlActivityNameBuilder;
import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControlActivityNameBuilderFactory;
import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControlCounterManager;
import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControllerPersistenceManager;
import com.ericsson.oss.services.shm.loadcontrol.impl.SHMLoadControllerServiceImpl;
import com.ericsson.oss.services.shm.loadcontrol.instrumentation.LoadControlInstrumentationBean;
import com.ericsson.oss.services.shm.loadcontrol.local.api.LoadControllerLocalCache;
import com.ericsson.oss.services.shm.loadcontrol.monitor.LoadControlQueueProducer;

@RunWith(MockitoJUnitRunner.class)
public class SHMLoadControllerServiceImplTest {

    @InjectMocks
    SHMLoadControllerServiceImpl controllerService = new SHMLoadControllerServiceImpl();

    @Mock
    private LoadControlCounterManager counterManager;

    @Mock
    ConfigurationParamProvider maxCountProvider;

    @Mock
    NamedCounter counter;

    @Mock
    ChannelLocator channelLocator;

    @Mock
    Channel channel;

    @Mock
    private Logger logger;

    @Mock
    private LoadControlInstrumentationBean loadControlInstrumentationBean;

    @Mock
    LoadControlQueueProducer loadControlQueueProducer;

    @Mock
    private LoadControllerPersistenceManager loadControllerPersistenceManager;

    @Mock
    private MembershipListenerInterface membershipListenerInterface;

    @Mock
    LoadControlActivityNameBuilderFactory loadControllerActivityNameManagerFactory;

    @Mock
    LoadControlActivityNameBuilder loadControlActivityNameBuilder;

    @Mock
    private LoadControllerLocalCache loadControllerLocalCacheMock;

    @Before
    public void setup() {

        /*
         * counterManager = mock(LoadControlCounterManager.class); counter = mock(NamedCounter.class); maxCountProvider = mock(ConfigurationParamProvider.class);
         */

        MockitoAnnotations.initMocks(this);
        // Whitebox.setInternalState(controllerService, Logger.class, logger);
    }

    @Test
    public void testCheckAllowance() {
        final SHMActivityRequest jobActivityRequest = new SHMActivityRequest("workflowInstanceId", "businessKey", "jobType");
        jobActivityRequest.setPlatformType("platformType");
        jobActivityRequest.setActivityName("activityName");

        Mockito.when(maxCountProvider.getMaximumCount("platformType", "jobType", "activityName")).thenReturn((long) 5);
        final String counterName = counterManager.getCounterName("platformType", "jobType", "activityName");
        Mockito.when(counterManager.getCounter(counterName)).thenReturn(new AtomicLong());
        Mockito.when(counter.compareAndSet(0L, 1L)).thenReturn(true);
        /*
         * Mockito.doNothing().when(loadControlInstrumentationBean).updateCurrentCountJmx(counter.getName(), counter.get()); loadControlInstrumentationBean.updateCurrentCountJmx(counter.getName(),
         * counter.get());
         */

        final LoadControlCheckState loadControlCheckState = controllerService.checkAllowance(jobActivityRequest);

        Assert.assertEquals(LoadControlCheckState.PERMITTED.toString(), loadControlCheckState.toString());

    }

    @Test
    public void testCheckAllowanceNegative() {
        final SHMActivityRequest jobActivityRequest = new SHMActivityRequest("workflowInstanceId", "businessKey", "jobType");
        jobActivityRequest.setPlatformType("platformType");
        jobActivityRequest.setActivityName("activityName");

        Mockito.when(maxCountProvider.getMaximumCount("platformType", "jobType", "activityName")).thenReturn((long) 5);
        final String counterName = counterManager.getCounterName("platformType", "jobType", "activityName");
        Mockito.when(counterManager.getCounter(counterName)).thenReturn(new AtomicLong()).thenReturn(new AtomicLong()).thenReturn(new AtomicLong(1)).thenReturn(new AtomicLong(2));
        Mockito.when(counter.compareAndSet(0L, 1L)).thenReturn(false);
        Mockito.when(counter.compareAndSet(1L, 2L)).thenReturn(false);
        Mockito.when(counter.compareAndSet(2L, 3L)).thenReturn(true);

        final LoadControlCheckState loadControlCheckState = controllerService.checkAllowance(jobActivityRequest);

        Assert.assertEquals(LoadControlCheckState.PERMITTED.toString(), loadControlCheckState.toString());

    }

    @Test
    public void testCheckAllowanceWhenCounterExceedsLimit() {
        final SHMActivityRequest jobActivityRequest = new SHMActivityRequest("workflowInstanceId", "businessKey", "UPGRADE");
        jobActivityRequest.setPlatformType("CPP");
        jobActivityRequest.setActivityName("install");

        Mockito.when(maxCountProvider.getMaximumCount("CPP", "UPGRADE", "install")).thenReturn((long) 5);
        final String counterName = counterManager.getCounterName("CPP", "UPGRADE", "install");
        Mockito.when(counterManager.getCounter(counterName)).thenReturn(new AtomicLong(6));
        Mockito.when(channelLocator.lookupChannel("jms:/queue/ShmStagedActvityQueue")).thenReturn(channel);

        final LoadControlCheckState loadControlCheckState = controllerService.checkAllowance(jobActivityRequest);

        Assert.assertEquals(LoadControlCheckState.NOTPERMITTED, loadControlCheckState);

    }

    @Test
    public void testCheckAllowanceWhenCounterExceedsLimitAndChanelIsNull() {
        final SHMActivityRequest jobActivityRequest = new SHMActivityRequest("workflowInstanceId", "businessKey", "UPGRADE");
        jobActivityRequest.setPlatformType("CPP");
        jobActivityRequest.setActivityName("install");

        Mockito.when(maxCountProvider.getMaximumCount("CPP", "UPGRADE", "install")).thenReturn((long) 5);
        final String counterName = "LC_CPPUPGRADEinstall";
        Mockito.when(counterManager.getCounterName("CPP", "UPGRADE", "install")).thenReturn(counterName);
        Mockito.when(counterManager.getCounter(counterName)).thenReturn(new AtomicLong(6));
        Mockito.when(channelLocator.lookupChannel("jms:/queue/ShmStagedActvityQueue")).thenReturn(null);

        final LoadControlCheckState loadControlCheckState = controllerService.checkAllowance(jobActivityRequest);

        Assert.assertEquals(LoadControlCheckState.NOTPERMITTED, loadControlCheckState);

    }

    @Test
    public void testCheckAllowanceInByPassCase() {

        final SHMActivityRequest jobActivityRequest = new SHMActivityRequest("workflowInstanceId", "businessKey", "jobType");
        jobActivityRequest.setPlatformType("platformType");
        jobActivityRequest.setActivityName("activityName");

        Mockito.when(maxCountProvider.getMaximumCount("platformType", "jobType", "activityName")).thenReturn((long) 0);
        final LoadControlCheckState loadControlCheckState = controllerService.checkAllowance(jobActivityRequest);

        Assert.assertEquals(LoadControlCheckState.BYPASSED.toString(), loadControlCheckState.toString());

    }

    @Test
    public void testDecrementCounter() {
        final SHMActivityRequest jobActivityRequest = new SHMActivityRequest("workflowInstanceId", "businessKey", "UPGRADE");
        jobActivityRequest.setPlatformType("CPP");
        jobActivityRequest.setActivityName("install");
        final String counterName = counterManager.getCounterName("CPP", "UPGRADE", "install");
        Mockito.when(loadControllerActivityNameManagerFactory.getLoadControlActivityNameBuilder(Matchers.anyString(), Matchers.anyString())).thenReturn(loadControlActivityNameBuilder);
        Mockito.when(loadControlActivityNameBuilder.buildActivityName(Matchers.anyString())).thenReturn("install");
        Mockito.when(counterManager.getCounter(counterName)).thenReturn(new AtomicLong(2));
        Mockito.when(counter.compareAndSet(2L, 1L)).thenReturn(true);
        controllerService.decrementCounter(jobActivityRequest);
        Mockito.verify(counter, Mockito.atMost(1)).compareAndSet(2L, 1L);
    }

    @Test
    public void testDecrementCounterIfCountReachesZero() {
        final SHMActivityRequest jobActivityRequest = new SHMActivityRequest("workflowInstanceId", "businessKey", "UPGRADE");
        jobActivityRequest.setPlatformType("CPP");
        jobActivityRequest.setActivityName("install");
        final String counterName = counterManager.getCounterName("CPP", "UPGRADE", "install");
        Mockito.when(loadControllerActivityNameManagerFactory.getLoadControlActivityNameBuilder(Matchers.anyString(), Matchers.anyString())).thenReturn(loadControlActivityNameBuilder);
        Mockito.when(loadControlActivityNameBuilder.buildActivityName(Matchers.anyString())).thenReturn("install");
        Mockito.when(counterManager.getCounter(counterName)).thenReturn(new AtomicLong(2));
        Mockito.when(counter.compareAndSet(0L, -1L)).thenReturn(false);
        controllerService.decrementCounter(jobActivityRequest);
        Mockito.verify(counter, Mockito.never()).compareAndSet(0L, -1L);
    }

    @Test
    public void testIncrementGlobalCounter() {
        final SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest = new SHMLoadControllerCounterRequest();
        shmLoadControllerLocalCounterRequest.setActivityName("install");
        shmLoadControllerLocalCounterRequest.setPlatformType("CPP");
        shmLoadControllerLocalCounterRequest.setJobType("UPGRADE");

        Mockito.when(loadControllerActivityNameManagerFactory.getLoadControlActivityNameBuilder(Matchers.anyString(), Matchers.anyString())).thenReturn(loadControlActivityNameBuilder);
        Mockito.when(loadControlActivityNameBuilder.buildActivityName(Matchers.anyString())).thenReturn("install");
        Mockito.when(maxCountProvider.getMaximumCountByCounterKey(Matchers.anyString())).thenReturn((long) 5);
        Mockito.when(counterManager.getGlobalCounter(Matchers.anyString())).thenReturn(new AtomicLong(1));
        Mockito.when(membershipListenerInterface.getCurrentMembersCount()).thenReturn(1);
        Assert.assertTrue(controllerService.incrementGlobalCounter(shmLoadControllerLocalCounterRequest));

    }

    @Test
    public void testdecrementGlobalCounter() {

        final SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest = new SHMLoadControllerCounterRequest();
        shmLoadControllerLocalCounterRequest.setActivityName("install");
        shmLoadControllerLocalCounterRequest.setPlatformType("CPP");
        shmLoadControllerLocalCounterRequest.setJobType("UPGRADE");
        Mockito.when(loadControllerActivityNameManagerFactory.getLoadControlActivityNameBuilder(Matchers.anyString(), Matchers.anyString())).thenReturn(loadControlActivityNameBuilder);
        Mockito.when(loadControlActivityNameBuilder.buildActivityName(Matchers.anyString())).thenReturn("install");
        Mockito.when(counterManager.getGlobalCounter(Matchers.anyString())).thenReturn(new AtomicLong(1));
        Mockito.when(membershipListenerInterface.getCurrentMembersCount()).thenReturn(1);
        controllerService.decrementGlobalCounter(shmLoadControllerLocalCounterRequest);

    }

    @Test
    public void testIncrementGlobalCounterForAxeUpgrade() {
        final SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest = new SHMLoadControllerCounterRequest();
        shmLoadControllerLocalCounterRequest.setActivityName("health check script");
        shmLoadControllerLocalCounterRequest.setPlatformType("AXE");
        shmLoadControllerLocalCounterRequest.setJobType("UPGRADE");

        Mockito.when(loadControllerActivityNameManagerFactory.getLoadControlActivityNameBuilder(Matchers.anyString(), Matchers.anyString())).thenReturn(loadControlActivityNameBuilder);
        Mockito.when(loadControlActivityNameBuilder.buildActivityName(Matchers.anyString())).thenReturn("axeActivity");
        Mockito.when(maxCountProvider.getMaximumCountByCounterKey(Matchers.anyString())).thenReturn((long) 5);
        Mockito.when(counterManager.getGlobalCounter(Matchers.anyString())).thenReturn(new AtomicLong(1));
        Mockito.when(membershipListenerInterface.getCurrentMembersCount()).thenReturn(1);
        Assert.assertTrue(controllerService.incrementGlobalCounter(shmLoadControllerLocalCounterRequest));

    }

    @Test
    public void testdecrementGlobalCounterForAxeUpgrade() {

        final SHMLoadControllerCounterRequest shmLoadControllerLocalCounterRequest = new SHMLoadControllerCounterRequest();
        shmLoadControllerLocalCounterRequest.setActivityName("axe script");
        shmLoadControllerLocalCounterRequest.setPlatformType("AXE");
        shmLoadControllerLocalCounterRequest.setJobType("UPGRADE");
        Mockito.when(loadControllerActivityNameManagerFactory.getLoadControlActivityNameBuilder(Matchers.anyString(), Matchers.anyString())).thenReturn(loadControlActivityNameBuilder);
        Mockito.when(loadControlActivityNameBuilder.buildActivityName(Matchers.anyString())).thenReturn("axeActiviy");
        Mockito.when(counterManager.getGlobalCounter(Matchers.anyString())).thenReturn(new AtomicLong(1));
        Mockito.when(membershipListenerInterface.getCurrentMembersCount()).thenReturn(1);
        controllerService.decrementGlobalCounter(shmLoadControllerLocalCounterRequest);

    }

    @Test
    public void testDeleteShmStagedActivityPOs() {
        controllerService.deleteShmStageActivity(2);
        verify(loadControllerPersistenceManager).deleteStagedActivityPOs(2);
    }
}
