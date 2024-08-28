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
package com.ericsson.oss.services.shm.loadcontrol.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.api.SHMActivityRequest;
import com.ericsson.oss.services.shm.es.api.SHMStagedActivityRequest;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.loadcontrol.local.api.SHMLoadControllerLocalService;
import com.ericsson.oss.services.shm.loadcontrol.local.api.StagedActivityRequestBean;

/**
 * This method will check activity allowance of staged activities and will notify to WFS.
 */

public class ActivityLoadControlManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private SHMLoadControllerLocalService shmLoadControllerLocalService;

    @Inject
    private LoadControlWorkflowNotifier loadControlWorkflowNotifier;

    @Inject
    private LoadControllerPersistenceManager loadControllerPersistenceManager;

    @Inject
    private ConfigurationParamProvider maxCountProvider;

    /**
     * This method will check activity allowance of staged activities which are listen from Queue. if activity allowed, will notify to wfs, else update the state attribute back to READY.
     */
    public void checkActivityAllowance(final StagedActivityRequestBean stagedActivityRequestMessage) {
        final long startTime = System.currentTimeMillis();
        int waitingRequestsCounter = 0;
        final List<SHMStagedActivityRequest> shmStagedActivityRequestList = stagedActivityRequestMessage.getShmStagedActivityRequest();
        logger.info("In checkActivityAllowance - StagedActivityRequestList size which are listened from Queue {}", shmStagedActivityRequestList.size());
        for (final SHMStagedActivityRequest shmStagedActivityRequest : shmStagedActivityRequestList) {
            try {
                waitingRequestsCounter++;
                final SHMActivityRequest tokenRequest = new SHMActivityRequest();
                tokenRequest.setActivityName(shmStagedActivityRequest.getActivityName());
                tokenRequest.setJobType(shmStagedActivityRequest.getJobType());
                tokenRequest.setPlatformType(shmStagedActivityRequest.getPlatformType());
                tokenRequest.setActivityJobId(shmStagedActivityRequest.getActivityJobId());

                final boolean allow = shmLoadControllerLocalService.incrementCounter(tokenRequest);
                logger.debug("In checkActivityAllowance - Activity execution allowance is {} for businessKey {}  and activityRequest {}", allow, shmStagedActivityRequest.getBusinessKey(),
                        tokenRequest);

                if (!allow) {
                    logger.debug("Unable to get permit for businessKey {}, and StagedActivityRequest {} ", shmStagedActivityRequest.getBusinessKey(), tokenRequest);
                    createPOInDB(shmStagedActivityRequest, 0);
                    continue;
                }

                final boolean isMsgCorrelated = loadControlWorkflowNotifier.correlate(shmStagedActivityRequest.getBusinessKey(), shmStagedActivityRequest.getWorkflowInstanceId());
                logger.debug("Workflow correlation status is {} for request[jobType::{}, activty:{}, businessKey::{}].", isMsgCorrelated, shmStagedActivityRequest.getJobType(),
                        shmStagedActivityRequest.getActivityName(), shmStagedActivityRequest.getBusinessKey());

                if (!isMsgCorrelated) {

                    if (shmStagedActivityRequest != null && shmStagedActivityRequest.getRetryCount() < maxCountProvider.getLoadControllerCorrelationFailureRetryCount()) {
                        createPOInDB(shmStagedActivityRequest, shmStagedActivityRequest.getRetryCount() + 1);
                        logger.warn("workflow correlation failed, so creating a PO for popped ActivityRequest {}", shmStagedActivityRequest);
                    } else {
                        logger.warn("workflow correlation failed, for SHMActivityRequest {} ", shmStagedActivityRequest);
                    }
                    shmLoadControllerLocalService.decrementCounter(tokenRequest);
                }
            } catch (final Exception e) {
                logger.error("Exception occured while processing ShmStagedActivityQueue messages {}", e);
            }
        }
        final long endTime = System.currentTimeMillis();
        logger.info("Took {} seconds for processing {} Staged Activity Requests from queue", (endTime - startTime), waitingRequestsCounter);
    }

    private void createPOInDB(final SHMStagedActivityRequest shmStagedActivityRequest, final int retryCount) {
        final Map<String, Object> poAttributes = new HashMap<>();
        poAttributes.put(ShmConstants.PLATFORM, shmStagedActivityRequest.getPlatformType());
        poAttributes.put(ShmConstants.JOB_TYPE, shmStagedActivityRequest.getJobType());
        poAttributes.put(ShmConstants.ACTIVITYNAME, shmStagedActivityRequest.getActivityName());
        poAttributes.put(ShmConstants.BUSINESS_KEY, shmStagedActivityRequest.getBusinessKey());
        poAttributes.put(ShmConstants.LC_COUNTER_KEY, shmStagedActivityRequest.getPlatformType() + shmStagedActivityRequest.getJobType() + shmStagedActivityRequest.getActivityName());
        poAttributes.put(ShmConstants.STAGED_ACTIVITY_WAIT_TIME, new Date());
        poAttributes.put(ShmConstants.WORKFLOW_INSTANCE_ID, shmStagedActivityRequest.getWorkflowInstanceId());
        poAttributes.put(ShmConstants.RETRY_COUNT, retryCount);
        poAttributes.put(ShmConstants.ACTIVITY_JOB_ID, shmStagedActivityRequest.getActivityJobId());
        loadControllerPersistenceManager.createPO(poAttributes);
        logger.trace("create a PO again In DB for {}", shmStagedActivityRequest);
    }
}
