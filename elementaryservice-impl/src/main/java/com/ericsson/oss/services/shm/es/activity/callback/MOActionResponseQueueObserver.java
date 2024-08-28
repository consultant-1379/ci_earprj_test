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
package com.ericsson.oss.services.shm.es.activity.callback;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.MOActionCallBack;
import com.ericsson.oss.services.shm.es.moaction.MoActionMTRManager;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.event.based.mediation.MOActionResponse;

/**
 * This class fetches data from Queue and delegates data to corresponding elementary services.
 * 
 * @author zdonkri
 * 
 */

public class MOActionResponseQueueObserver implements EMessageListener<MOActionResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOActionResponseQueueObserver.class);

    @Inject
    private MOActionCallBackReslover moActionCallBackResolver;

    @Inject
    private MoActionMTRManager actionMTRManager;

    @Override
    public void onMessage(final MOActionResponse moActionResponseEvent) {
    	
    long activityJobId = 0;
        try {
            LOGGER.debug("Entered into MOActionResponseQueueObserver.onMessage with {}", moActionResponseEvent);
            if (moActionResponseEvent == null) {
                LOGGER.error("Null event received from MOAction.");
                return;
            }
            final JobTypeEnum jobType = JobTypeEnum.getJobType((String) moActionResponseEvent.getAdditionalInformation().get(ShmConstants.JOB_TYPE));
            final PlatformTypeEnum platformType = PlatformTypeEnum.getPlatform((String) moActionResponseEvent.getAdditionalInformation().get(ShmConstants.PLATFORM));
            final String activityName = (String) moActionResponseEvent.getAdditionalInformation().get(ShmConstants.ACTIVITYNAME);
            activityJobId =  moActionResponseEvent.getActivityJobId();
            final MOActionCallBack moActionCallBackImpl = getMOActionCallBackService(activityName, jobType, platformType);
            if (moActionCallBackImpl != null) {
                final Map<String, Object> actionResponseAttributes = new HashMap<>();
                actionResponseAttributes.put(ShmConstants.FDN, moActionResponseEvent.getMoFdn());
                actionResponseAttributes.put(ShmConstants.ACTION_RESPONSE, moActionResponseEvent.getActionResponse());
                actionResponseAttributes.put(PollingActivityConstants.ERROR_MESSAGE, moActionResponseEvent.getErrorMessage());
                actionResponseAttributes.put(PollingActivityConstants.IS_ACTION_ALREADY_RUNNING,
                        moActionResponseEvent.getAdditionalInformation().get(PollingActivityConstants.IS_ACTION_ALREADY_RUNNING));
                actionResponseAttributes.put(PollingActivityConstants.MO_NAME, moActionResponseEvent.getAdditionalInformation().get(PollingActivityConstants.MO_NAME));
                moActionCallBackImpl.processMoActionResponse(activityJobId, actionResponseAttributes);
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while processing MO Action response attributes: {} for activityJobId: {} Exception is: {}", moActionResponseEvent,activityJobId, ex);
            actionMTRManager.removeMoActionMTRFromCache(activityJobId);
        }
    }

    private MOActionCallBack getMOActionCallBackService(final String activityName, final JobTypeEnum jobType, final PlatformTypeEnum platformType) {
        return moActionCallBackResolver.getMOActionCallBackService(platformType, jobType, activityName);
    }

}
