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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.service.common;

import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.vran.model.request.VranUpgradeJobStatusRequest;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.vran.configuration.ConfigurationParameterValueProvider;
import com.ericsson.oss.services.shm.vran.constants.VranJobEvents;

/**
 * Service to schedule software upgrade job status requests
 *
 * @author xsripod
 *
 */
@Stateless
public class SoftwareUpgradeStatusCheckScheduler {

    @Inject
    @Modeled
    private EventSender<VranUpgradeJobStatusRequest> jobStatusRequestSender;

    @Inject
    private ConfigurationParameterValueProvider configurationParameterValueProvider;

    @Inject
    private TimerService timerService;

    @Inject
    private ActivityUtils activityUtils;

    private static final Logger LOGGER = LoggerFactory.getLogger(SoftwareUpgradeStatusCheckScheduler.class);

    public void schedule(final VranUpgradeJobStatusRequest jobStatusRequest) {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerConfig.setInfo(jobStatusRequest);

        final int intervalInSec = configurationParameterValueProvider.getSoftwareUpgradeStatusCheckInterval();

        final Timer timer = timerService.createSingleActionTimer((intervalInSec * 1000), timerConfig);
        LOGGER.debug("Time interval set for software upgrade job status request {} is : {} and the next request will be sent at : {}", jobStatusRequest, intervalInSec, timer.getNextTimeout());

    }

    @Timeout
    public void sendStatusCheckRequest(final Timer timer) {
        final VranUpgradeJobStatusRequest jobStatusRequest = (VranUpgradeJobStatusRequest) timer.getInfo();
        jobStatusRequestSender.send(jobStatusRequest);

        activityUtils.recordEvent(VranJobEvents.JOB_PROGRESS, jobStatusRequest.getNodeAddress(), jobStatusRequest.getActivityName(),
                activityUtils.additionalInfoForEvent(jobStatusRequest.getVnfJobId(), jobStatusRequest.getNodeAddress(), jobStatusRequest.toString()));

        LOGGER.debug("Mediation task request has been sent successfully. Event : {}", jobStatusRequest);

    }

}
