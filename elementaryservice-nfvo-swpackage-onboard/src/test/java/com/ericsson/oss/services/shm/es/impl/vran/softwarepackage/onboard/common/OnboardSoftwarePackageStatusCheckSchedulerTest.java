package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common;
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

import static org.mockito.Mockito.when;

import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.mediation.vran.model.request.NfvoSwPackageCreateOnBoardJobRequest;
import com.ericsson.oss.mediation.vran.model.request.NfvoSwPackagePollJobRequest;
import com.ericsson.oss.services.shm.es.vran.configuration.ConfigurationParameterValueProvider;

@RunWith(value = MockitoJUnitRunner.class)
public class OnboardSoftwarePackageStatusCheckSchedulerTest {

    @InjectMocks
    private OnboardSoftwarePackageStatusCheckScheduler onboardSoftwarePackageStatusCheckScheduler;

    @Mock
    private NfvoSwPackageCreateOnBoardJobRequest nfvoSwPackageCreateOnboardJobRequest;

    @Mock
    private EventSender<NfvoSwPackageCreateOnBoardJobRequest> eventSender;

    @Mock
    private ConfigurationParameterValueProvider configurationParameterValueProvider;

    @Mock
    private NfvoSwPackagePollJobRequest jobStatusRequest;

    @Mock
    private TimerService timerService;

    @Mock
    private Timer timer;

    @Mock
    private TimerConfig timerConfig;

    @Test
    public void testSendJobStatusRequest() {
        when(configurationParameterValueProvider.getSoftwarePackageOnboardStatusCheckInterval()).thenReturn(2);
        when(timerService.createSingleActionTimer((2 * 1000), timerConfig)).thenReturn(timer);
        onboardSoftwarePackageStatusCheckScheduler.scheduleSingleNfvoSwPackagePollJobRequest(jobStatusRequest);
    }

    @Test
    public void testSendSwPackageOnboardJobStatusRequest() {
        when(configurationParameterValueProvider.getSoftwarePackageOnboardStatusCheckInterval()).thenReturn(2);
        when(timerService.createSingleActionTimer((2 * 1000), timerConfig)).thenReturn(timer);
        onboardSoftwarePackageStatusCheckScheduler.sendSwPackageOnboardJobStatusRequest(timer);
    }

}
