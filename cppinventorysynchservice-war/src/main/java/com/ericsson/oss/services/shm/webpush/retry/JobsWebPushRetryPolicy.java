package com.ericsson.oss.services.shm.webpush.retry;

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

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.uisdk.restsdk.webpush.api.exception.WebPushBroadcastException;

@ApplicationScoped
public class JobsWebPushRetryPolicy {

    @Inject
    JobsWebPushRetryConfigurationParamProvider webPushConf;

    @SuppressWarnings("unchecked")
    private static final Class<? extends Exception>[] exceptionWebpushBroadcast = new Class[] { WebPushBroadcastException.class };

    public RetryPolicy getWebPushRetryPolicy() {
        return RetryPolicy.builder().attempts(webPushConf.getWebPushRetryCount()).waitInterval(webPushConf.getWebPushWaitInterval_ms(), TimeUnit.MILLISECONDS)
                .exponentialBackoff(ShmCommonConstants.EXPONENTIAL_BACK_OFF).retryOn(exceptionWebpushBroadcast).build();
    }

}
