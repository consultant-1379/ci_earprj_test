/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.webpush;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.webpush.api.JobWebPushEvent;
import com.ericsson.oss.services.shm.webpush.api.WebPushConstants;
import com.ericsson.oss.services.shm.webpush.retry.JobsWebpushRetryProxy;

/**
 * This class listens to the events fired for main jobs, job details and job logs Web push.
 * 
 * @author xvishsr
 */

@ApplicationScoped
public class JobWebpushEventListener {

    @Inject
    private JobsWebpushRetryProxy cppinventorysynchserviceWebpushRetryProxy;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobWebpushEventListener.class);

    /**
     * This method listens to the events fired with JobWebPushEvent object.
     * 
     * @param JobWebPushEvent
     *            jobWebPushEvent
     */
    public void listenForJobWebPushEvent(@Observes final JobWebPushEvent jobWebPushEvent) {

        LOGGER.info("Recieved jobs Webpush Data CDI Event : {}", jobWebPushEvent);

        if (jobWebPushEvent != null) {
            switch (jobWebPushEvent.getApplicationType()) {
            case WebPushConstants.MAIN_JOBS_APPLICATION:
                cppinventorysynchserviceWebpushRetryProxy.pushToShmPage(jobWebPushEvent);
                break;
            case WebPushConstants.JOB_DETAILS_APPLICATION:
                cppinventorysynchserviceWebpushRetryProxy.pushToShmJobDetailsPage(jobWebPushEvent);
                break;
            case WebPushConstants.SHM_JOBS_APPLICATION:
                cppinventorysynchserviceWebpushRetryProxy.pushToShmJobsPage(jobWebPushEvent);
                break;
            case WebPushConstants.JOB_LOGS_APPLICATION:
                cppinventorysynchserviceWebpushRetryProxy.pushToShmJobLogsPage(jobWebPushEvent);
                break;
            case WebPushConstants.NHC_MAIN_JOBS_APPLICATION:
                cppinventorysynchserviceWebpushRetryProxy.pushToNhcPage(jobWebPushEvent);
                break;
            case WebPushConstants.NHC_JOB_DETAILS_APPLICATION:
                cppinventorysynchserviceWebpushRetryProxy.pushToNhcJobDetailsPage(jobWebPushEvent);
                break;
            case WebPushConstants.NHC_JOBS_APPLICATION:
                cppinventorysynchserviceWebpushRetryProxy.pushToNhcJobsPage(jobWebPushEvent);
                break;
            case WebPushConstants.NHC_JOB_LOGS_APPLICATION:
                cppinventorysynchserviceWebpushRetryProxy.pushToNhcJobLogsPage(jobWebPushEvent);
                break;
            default:
                LOGGER.error("Invalid jobs webpush event received : {}", jobWebPushEvent);
                break;
            }
        } else {
            LOGGER.error("Invalid job webpush event received: {}", jobWebPushEvent);
        }
    }
}
