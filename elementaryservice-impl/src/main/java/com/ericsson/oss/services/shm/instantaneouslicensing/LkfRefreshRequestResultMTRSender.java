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
package com.ericsson.oss.services.shm.instantaneouslicensing;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.services.shm.model.event.based.mediation.LkfRefreshRequestResultMediationTaskRequest;

@Stateless
public class LkfRefreshRequestResultMTRSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LkfRefreshRequestResultMTRSender.class);

    @Inject
    @Modeled
    private EventSender<LkfRefreshRequestResultMediationTaskRequest> eventSender;

    public void sendMTR(final Map<String, Object> lkfRefreshRequestResultProperties) {
        LOGGER.debug("LicenseRefreshJob:LkfRefreshRequestResultMTRSender lkfRefreshRequestResultProperties details : {} ", lkfRefreshRequestResultProperties);
        eventSender.send(prepareMTR(lkfRefreshRequestResultProperties));
    }

    private LkfRefreshRequestResultMediationTaskRequest prepareMTR(final Map<String, Object> lkfRefreshRequestResultProperties) {
        final LkfRefreshRequestResultMediationTaskRequest lkfRefreshRequestResultMediationTaskRequest = new LkfRefreshRequestResultMediationTaskRequest();
        lkfRefreshRequestResultMediationTaskRequest.setNodeAddress((String) lkfRefreshRequestResultProperties.get("NODE_NAME"));
        final Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put("requestType", lkfRefreshRequestResultProperties.get("requestType"));
        eventAttributes.put("requestId", lkfRefreshRequestResultProperties.get("requestId"));
        eventAttributes.put("resultCode", lkfRefreshRequestResultProperties.get("resultCode"));
        eventAttributes.put("requestInfo", lkfRefreshRequestResultProperties.get("requestInfo"));
        lkfRefreshRequestResultMediationTaskRequest.setEventAttributes(eventAttributes);
        LOGGER.info("LicenseRefreshJob:LkfRefreshRequestResultMTRSender lkfRefreshRequestResultMediationTaskRequest details : {} and eventAttributes : {}", lkfRefreshRequestResultMediationTaskRequest,
                eventAttributes);
        return lkfRefreshRequestResultMediationTaskRequest;
    }
}
