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

import javax.ejb.*;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.vran.model.request.NfvoSwPackagePollJobRequest;
import com.ericsson.oss.services.shm.es.vran.configuration.ConfigurationParameterValueProvider;

/**
 * Service to schedule single time execution timer to send job poll requests.
 * 
 * @author xsripod
 * 
 */
@Stateless
public class DeleteJobStatusCheckScheduler {

    @Inject
    @Modeled
    private EventSender<NfvoSwPackagePollJobRequest> jobStatusRequestSender;

    @Inject
    private ConfigurationParameterValueProvider configurationParameterValueProvider;

    @Inject
    private TimerService timerService;

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteJobStatusCheckScheduler.class);

    /**
     * This method creates a single action timer to schedule software package delete job status requests reading time interval from config parameter
     * 
     * @param jobStatusRequest
     * 
     */
    public void scheduleNfvoSwPackageSinglePollJobRequest(final NfvoSwPackagePollJobRequest jobStatusRequest) {

        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerConfig.setInfo(jobStatusRequest);

        try {
            final int intervalInSec = configurationParameterValueProvider.getSoftwarePackageOnboardStatusCheckInterval();

            final Timer timer = timerService.createSingleActionTimer((intervalInSec * 1000), timerConfig);
            LOGGER.debug("Time interval set for delete software package job status request {} is : {} and the next request will be sent at : {}", jobStatusRequest, intervalInSec,
                    timer.getNextTimeout());

        } catch (Exception exception) {
            LOGGER.error("Unable to set the time interval for delete software package job status request. Reason : ", exception);
        }

    }

    @Timeout
    public void sendSwPackageDeleteJobStatusRequest(final Timer timer) {
        final NfvoSwPackagePollJobRequest jobStatusRequest = (NfvoSwPackagePollJobRequest) timer.getInfo();
        jobStatusRequestSender.send(jobStatusRequest);
        LOGGER.info("Delete software package jobstatus request has been sent successfully. Event : {}", jobStatusRequest);
    }

}
