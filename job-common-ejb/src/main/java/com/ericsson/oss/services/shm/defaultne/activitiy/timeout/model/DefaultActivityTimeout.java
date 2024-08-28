package com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.services.shm.jobs.common.api.ActivityTimeoutValues;

@ApplicationScoped
public class DefaultActivityTimeout implements ActivityTimeoutValues {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    @Configured(propertyName = "SHM_DEFAULT_ACTIVITY_TIMEOUT")
    private int defaultActivityTimeout;

    public void listenForSetFirstInRollBackActivityTimeoutAttribute(@Observes @ConfigurationChangeNotification(propertyName = "SHM_DEFAULT_ACTIVITY_TIMEOUT") final int defaultActivityTimeout) {
        this.defaultActivityTimeout = defaultActivityTimeout;
        logger.info("Changed Default activity timeout value : {}", this.defaultActivityTimeout);
    }

    private String convertToIsoFormat(final int timeout) {
        return "PT" + timeout + "M";
    }

    public int getDefaultActivityTimeout() {
        return defaultActivityTimeout;
    }

    @Override
    public String getActivityTimeout(final String activityName) {
        return convertToIsoFormat(this.defaultActivityTimeout);
    }

}