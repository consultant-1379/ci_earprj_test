/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.loadcontrol.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.services.shm.loadcontrol.impl.ActivityLoadControlManager;
import com.ericsson.oss.services.shm.loadcontrol.local.api.StagedActivityRequestBean;

public class ShmStagedActivityQueueObserver implements EMessageListener<StagedActivityRequestBean> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ActivityLoadControlManager activityLoadControlManager;

    public ShmStagedActivityQueueObserver(final ActivityLoadControlManager activityLoadControlManager) {
        this.activityLoadControlManager = activityLoadControlManager;
    }

    @Override
    public void onMessage(final StagedActivityRequestBean stagedActivityRequestMessage) {
        logger.trace("Inside ShmStagedActivityQueueListener - onMessage {}", stagedActivityRequestMessage);
        activityLoadControlManager.checkActivityAllowance(stagedActivityRequestMessage);
    }

}
