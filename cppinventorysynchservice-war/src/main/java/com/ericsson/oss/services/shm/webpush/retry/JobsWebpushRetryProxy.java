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
package com.ericsson.oss.services.shm.webpush.retry;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.shm.webpush.NhcWebPushHandler;
import com.ericsson.oss.services.shm.webpush.ShmWebPushHandler;
import com.ericsson.oss.services.shm.webpush.api.JobWebPushEvent;
import com.ericsson.oss.uisdk.restsdk.webpush.api.exception.WebPushBroadcastException;

public class JobsWebpushRetryProxy {

    @Inject
    private RetryManager retryManager;

    @Inject
    private JobsWebPushRetryPolicy webPushRetryPolicy;

    @Inject
    private ShmWebPushHandler shmWebPushHandler;

    @Inject
    private NhcWebPushHandler nhcWebPushHandler;

    public void pushToShmPage(final JobWebPushEvent jobWebPushEvent) {
        retryManager.executeCommand(webPushRetryPolicy.getWebPushRetryPolicy(), new RetriableCommand<Void>() {

            @Override
            public Void execute(final RetryContext retryContext) throws WebPushBroadcastException {
                shmWebPushHandler.pushToShmPage(jobWebPushEvent);
                return null;
            }

        });
    }

    public void pushToShmJobDetailsPage(final JobWebPushEvent jobWebPushEvent) {
        retryManager.executeCommand(webPushRetryPolicy.getWebPushRetryPolicy(), new RetriableCommand<Void>() {

            @Override
            public Void execute(final RetryContext retryContext) throws WebPushBroadcastException {
                shmWebPushHandler.pushToShmJobDetailsPage(jobWebPushEvent);
                return null;
            }

        });
    }

    public void pushToShmJobsPage(final JobWebPushEvent jobWebPushEvent) {
        retryManager.executeCommand(webPushRetryPolicy.getWebPushRetryPolicy(), new RetriableCommand<Void>() {

            @Override
            public Void execute(final RetryContext retryContext) throws WebPushBroadcastException {
                shmWebPushHandler.pushToShmJobsPage(jobWebPushEvent);
                return null;
            }

        });
    }

    public void pushToShmJobLogsPage(final JobWebPushEvent jobWebPushEvent) {
        retryManager.executeCommand(webPushRetryPolicy.getWebPushRetryPolicy(), new RetriableCommand<Void>() {

            @Override
            public Void execute(final RetryContext retryContext) throws WebPushBroadcastException {
                shmWebPushHandler.pushToShmJobLogsPage(jobWebPushEvent);
                return null;
            }

        });
    }

    public void pushToNhcPage(final JobWebPushEvent jobWebPushEvent) {
        retryManager.executeCommand(webPushRetryPolicy.getWebPushRetryPolicy(), new RetriableCommand<Void>() {

            @Override
            public Void execute(final RetryContext retryContext) throws WebPushBroadcastException {
                nhcWebPushHandler.pushToNhcPage(jobWebPushEvent);
                return null;
            }
        });
    }

    public void pushToNhcJobDetailsPage(final JobWebPushEvent jobWebPushEvent) {
        retryManager.executeCommand(webPushRetryPolicy.getWebPushRetryPolicy(), new RetriableCommand<Void>() {

            @Override
            public Void execute(final RetryContext retryContext) throws WebPushBroadcastException {
                nhcWebPushHandler.pushToNhcJobDetailsPage(jobWebPushEvent);
                return null;
            }

        });
    }

    public void pushToNhcJobsPage(final JobWebPushEvent jobWebPushEvent) {
        retryManager.executeCommand(webPushRetryPolicy.getWebPushRetryPolicy(), new RetriableCommand<Void>() {

            @Override
            public Void execute(final RetryContext retryContext) throws WebPushBroadcastException {
                nhcWebPushHandler.pushToNhcJobsPage(jobWebPushEvent);
                return null;
            }

        });
    }

    public void pushToNhcJobLogsPage(final JobWebPushEvent jobWebPushEvent) {
        retryManager.executeCommand(webPushRetryPolicy.getWebPushRetryPolicy(), new RetriableCommand<Void>() {

            @Override
            public Void execute(final RetryContext retryContext) throws WebPushBroadcastException {
                nhcWebPushHandler.pushToNhcJobLogsPage(jobWebPushEvent);
                return null;
            }

        });
    }

}