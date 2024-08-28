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
package com.ericsson.oss.services.shm.es.vran.configuration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.services.shm.jobs.vran.constants.VranConstants;

/**
 * This service provides access to values of configuration parameters that belong to vRAN jobs.
 * 
 * @author xsripod
 * 
 */
@ApplicationScoped
public class ConfigurationParameterValueProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationParameterValueProvider.class);

    @Inject
    @Configured(propertyName = "onboardStatusRequestInterval")
    private int onboardStatusCheckInterval;

    @Inject
    @Configured(propertyName = "upgradeStatusRequestInterval")
    private int upgradeStatusCheckInterval;

    @Inject
    @Configured(propertyName = VranConstants.RETRIES_FOR_ACTIVITY_WHEN_FAILURE)
    private int retriesForActivityWhenFailure;

    @Inject
    @Configured(propertyName = VranConstants.RETRY_INTERVAL)
    private int retryIntervalInSec;

    public int getSoftwarePackageOnboardStatusCheckInterval() {
        return onboardStatusCheckInterval;
    }

    public int getSoftwareUpgradeStatusCheckInterval() {
        return upgradeStatusCheckInterval;
    }

    /**
     * It returns no of retries for activity failure
     * 
     * @return the retriesForActivityWhenFailure
     */
    public int getRetriesForActivityWhenFailure() {
        return retriesForActivityWhenFailure;
    }

    /**
     * It returns Interval between retry activity level
     * 
     * @return the retryIntervalInSec
     */
    public int getRetryIntervalInSec() {
        return retryIntervalInSec;
    }

    void listenForChangeInSoftwarePackageOnboardStatusCheckInterval(@Observes @ConfigurationChangeNotification(propertyName = "onboardStatusRequestInterval") final int onboardStatusCheckInterval) {
        LOGGER.info("SoftwarePackageOnboardStatusCheckInterval old value is {}", this.onboardStatusCheckInterval, " new value received is {}", onboardStatusCheckInterval);
        this.onboardStatusCheckInterval = onboardStatusCheckInterval;

    }

    void listenForChangeInSoftwareUpgradeStatusCheckInterval(@Observes @ConfigurationChangeNotification(propertyName = "upgradeStatusRequestInterval") final int upgradeStatusCheckInterval) {
        LOGGER.info("SoftwareUpgradeStatusCheckInterval old value is {}", this.upgradeStatusCheckInterval, " new value received is {}", upgradeStatusCheckInterval);
        this.upgradeStatusCheckInterval = upgradeStatusCheckInterval;
    }

    /**
     * Listens for change in 'retriesForActivityWhenFailure'
     * 
     * @param retriesForActivityWhenFailure
     */
    void listenForChangeInRetriesForActivityWhenFailure(
            @Observes @ConfigurationChangeNotification(propertyName = VranConstants.RETRIES_FOR_ACTIVITY_WHEN_FAILURE) final int retriesForActivityWhenFailure) {
        this.retriesForActivityWhenFailure = retriesForActivityWhenFailure;
    }

    /**
     * Listens for change in 'retryIntervalInSec'
     * 
     * @param retryIntervalInSec
     */
    void listenForChangeInRetryIntervalInsecAttribute(@Observes @ConfigurationChangeNotification(propertyName = VranConstants.RETRY_INTERVAL) final int retryIntervalInSec) {
        this.retryIntervalInSec = retryIntervalInSec;
    }

}
