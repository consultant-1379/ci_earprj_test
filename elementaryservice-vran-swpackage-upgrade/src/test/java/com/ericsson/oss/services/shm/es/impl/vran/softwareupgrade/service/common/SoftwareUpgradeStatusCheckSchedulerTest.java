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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.service.common;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.mediation.vran.model.request.VranUpgradeJobStatusRequest;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.vran.configuration.ConfigurationParameterValueProvider;

@RunWith(MockitoJUnitRunner.class)
public class SoftwareUpgradeStatusCheckSchedulerTest {

    @InjectMocks
    private SoftwareUpgradeStatusCheckScheduler softwareUpgradeStatusCheckScheduler;

    @Mock
    private VranUpgradeJobStatusRequest vranUpgradeJobStatusRequest;

    @Mock
    private TimerConfig timerConfig;

    @Mock
    private ConfigurationParameterValueProvider configurationParameterValueProvider;

    @Mock
    private Timer timer;

    @Mock
    private TimerService timerService;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private EventSender<VranUpgradeJobStatusRequest> jobStatusRequestSender;

    @Mock
    private Logger LOGGER = LoggerFactory.getLogger(SoftwareUpgradeStatusCheckSchedulerTest.class);

    @Before
    public void environmentSetup() {
        vranUpgradeJobStatusRequest = new VranUpgradeJobStatusRequest();
        Map<String, Object> eventAttributes = new HashMap<String, Object>();
        vranUpgradeJobStatusRequest.setNodeAddress("NODE1");
        vranUpgradeJobStatusRequest.setActivityName("PREPARE");
        vranUpgradeJobStatusRequest.setVnfJobId(1212121);
        vranUpgradeJobStatusRequest.setEventAttributes(eventAttributes);
    }

    @Test
    public void testSchedule() {

    }

    @Test
    public void testSendStatusCheckRequest() {
        when(timer.getInfo()).thenReturn(vranUpgradeJobStatusRequest);
        softwareUpgradeStatusCheckScheduler.sendStatusCheckRequest(timer);
    }

}
