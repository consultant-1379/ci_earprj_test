/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.axe.upgrade;

import java.util.Map;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.ModeledEvent;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.es.axe.api.AxeNotificationProcessor;

/**
 * Listens on ClusteredOpsShmResponseQueue for the OPS script execution responses
 * 
 * @author tcsvnag
 */
@ApplicationScoped
@Profiled
@Traceable
public class AxeUpgradeResponseObserver {

    @EJB
    private AxeNotificationProcessor axeNotificationProcessor;

    private static final Logger LOGGER = LoggerFactory.getLogger(AxeUpgradeResponseObserver.class);
    private static final String AXE_NODE_UPGRADE_RESPONSE_EVENT_URN = "//global/AXENodeUpgradeResponse/1.0.0";

    @SuppressWarnings("unchecked")
    public void axeNodeUpgradeResponseListener(@Observes @Modeled(eventUrn = AXE_NODE_UPGRADE_RESPONSE_EVENT_URN) final ModeledEvent axeNodeUpgradeResponse) {
        if (axeNodeUpgradeResponse != null) {
            final Map<String, Object> attributes = axeNodeUpgradeResponse.getAttributes();
            LOGGER.info("Received AXE Node Upgrade Response {}", attributes);
            axeNotificationProcessor.processNotification(attributes);
        }
    }
}