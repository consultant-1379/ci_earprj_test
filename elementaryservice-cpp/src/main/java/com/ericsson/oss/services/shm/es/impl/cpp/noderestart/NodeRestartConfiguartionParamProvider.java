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
package com.ericsson.oss.services.shm.es.impl.cpp.noderestart;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

@ApplicationScoped
public class NodeRestartConfiguartionParamProvider {
    private final Logger LOGGER = LoggerFactory.getLogger(NodeRestartConfiguartionParamProvider.class);

    @Inject
    @Configured(propertyName = "RESTART_RANK")
    private String restartRank;

    @Inject
    @Configured(propertyName = "RESTART_REASON")
    private String restartReason;

    public void listenForRestartRank(@Observes @ConfigurationChangeNotification(propertyName = "RESTART_RANK") final String restartRank) {
        this.restartRank = restartRank;
        LOGGER.info("Changed value for RestartRank is  : {} ", this.restartRank);
    }

    public void listenForRestartReason(@Observes @ConfigurationChangeNotification(propertyName = "RESTART_REASON") final String restartReason) {
        this.restartReason = restartReason;
        LOGGER.info("Changed value for RestartReason is  : {} ", this.restartReason);
    }

    public String getRestartRankConfigParameter(final String parameterName) {
        return restartRank;
    }

    public String getRestartReasonConfigParameter(final String parameterName) {
        return restartReason;
    }

}
