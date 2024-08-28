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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common;

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
import com.ericsson.oss.mediation.vran.model.request.NfvoSwPackagePollJobRequest;
import com.ericsson.oss.mediation.vran.model.request.VranUpgradeJobStatusRequest;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeleteJobStatusCheckScheduler;
import com.ericsson.oss.services.shm.es.vran.configuration.ConfigurationParameterValueProvider;

@RunWith(MockitoJUnitRunner.class)
public class DeleteSoftwarePackageStatusCheckSchedulerTest {

    @InjectMocks
    DeleteJobStatusCheckScheduler deleteJobStatusCheckScheduler;

    @Mock
    ConfigurationParameterValueProvider configurationParameterValueProvider;

    @Mock
    TimerConfig timerconfig;

    @Mock
    Timer timer;

    @Mock
    TimerService timerService;

    @Mock
    private EventSender<VranUpgradeJobStatusRequest> jobStatusRequestSender;

    @Test
    public void scheduleJobStatusRequestTest() {
        when(configurationParameterValueProvider.getSoftwareUpgradeStatusCheckInterval()).thenReturn(5);
        NfvoSwPackagePollJobRequest jobStatusRequest = new NfvoSwPackagePollJobRequest();
        jobStatusRequest.setNodeAddress("PQ");
        jobStatusRequest.setNfvoJobId("QR");
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerConfig.setInfo(jobStatusRequest);
        when(timerService.createSingleActionTimer((5 * 1000), timerConfig)).thenReturn(timer);
        deleteJobStatusCheckScheduler.scheduleNfvoSwPackageSinglePollJobRequest(jobStatusRequest);
    }

    @Test
    public void triggerJobStatusRequestTest() {
        deleteJobStatusCheckScheduler.sendSwPackageDeleteJobStatusRequest(timer);
    }

}
