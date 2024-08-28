/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.axe.licensing;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.es.axe.common.WinFIOLServiceBusyException;

@ApplicationScoped
public class AxeLicenseRetryPolicy {

    @Inject
    @Configured(propertyName = "axeLicenseStatusRetryCount")
    private int axeLicenseStatusRetryCount;

    @Inject
    @Configured(propertyName = "axeLicenseStatusRetryIntervalInMS")
    private int axeLicenseStatusRetryIntervalInMS;

    /**
     * Listener for AXE INV DPS Wait Interval
     *
     * @param axeLicenseStatusRetryIntervalInMS
     */
    void listenFoAxeLicenseStatusWaitInterval(@Observes @ConfigurationChangeNotification(propertyName = "axeLicenseStatusRetryIntervalInMS") final int axeLicenseStatusRetryIntervalInMS) {
        this.axeLicenseStatusRetryIntervalInMS = axeLicenseStatusRetryIntervalInMS;
    }

    /**
     * Listener for AXE INV DPS Retry Count
     *
     * @param axeLicenseStatusRetryCount
     */
    void listenForAxeLicenseStatusRetryCount(@Observes @ConfigurationChangeNotification(propertyName = "axeLicenseStatusRetryCount") final int axeLicenseStatusRetryCount) {
        this.axeLicenseStatusRetryCount = axeLicenseStatusRetryCount;
    }

    /**
     * Provides {@link RetryPolicy}, object for winfiol retry calls.
     *
     * @return RetryPolicy
     */
    public RetryPolicy getAxeLicenseRetryPolicy() {
        return RetryPolicy.builder().attempts(axeLicenseStatusRetryCount).waitInterval(axeLicenseStatusRetryIntervalInMS, TimeUnit.MILLISECONDS)
                .exponentialBackoff(ShmCommonConstants.EXPONENTIAL_BACK_OFF).retryOn(WinFIOLServiceBusyException.class).build();
    }

}
