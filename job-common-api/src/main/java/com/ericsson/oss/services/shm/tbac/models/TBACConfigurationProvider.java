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
package com.ericsson.oss.services.shm.tbac.models;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

@ApplicationScoped
public class TBACConfigurationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TBACConfigurationProvider.class);

    @Inject
    @Configured(propertyName = "IS_TBAC_AT_JOB_LEVEL")
    private boolean isTBACAtJobLevel;

    /**
     * @return the isTBACAtJobLevel
     */
    public boolean isTBACAtJobLevel() {
        return isTBACAtJobLevel;
    }

    public void listenForIsTBACAtJobLevel(@Observes @ConfigurationChangeNotification(propertyName = "IS_TBAC_AT_JOB_LEVEL") final boolean value) {
        this.isTBACAtJobLevel = value;
        LOGGER.info("Changed TBAC value for isTBACAtJobLevel : {}", isTBACAtJobLevel);
    }
}
