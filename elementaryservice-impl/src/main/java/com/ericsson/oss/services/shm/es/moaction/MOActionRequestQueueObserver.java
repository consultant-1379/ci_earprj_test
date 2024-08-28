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
package com.ericsson.oss.services.shm.es.moaction;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.activity.callback.MOActionCallBackReslover;
import com.ericsson.oss.services.shm.es.api.MOActionCallBack;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.event.based.mediation.MOActionRequest;

/**
 * This class observes un-processed action requests from ShmPollEntriesRequestQueue and delegates data to corresponding elementary services.
 * 
 * @author tcssdas
 * 
 */
public class MOActionRequestQueueObserver implements EMessageListener<MOActionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOActionRequestQueueObserver.class);

    @Inject
    private MOActionCallBackReslover moActionCallBackResolver;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private MoActionMTRManager moActionMTRManager;

    @Override
    public void onMessage(final MOActionRequest requestEvent) {
        try {

            if (requestEvent == null) {
                LOGGER.warn("Discarding the event as MOAction Request is null");
            } else {
                LOGGER.debug("Entered into MOActionRequestQueueObserver.onMessage:{}", requestEvent.getActivityJobId());

                final JobTypeEnum jobType = JobTypeEnum.getJobType((String) requestEvent.getAdditionalInformation().get(ShmConstants.JOB_TYPE));
                final PlatformTypeEnum platformType = PlatformTypeEnum.getPlatform((String) requestEvent.getAdditionalInformation().get(ShmConstants.PLATFORM));
                final String activityName = (String) requestEvent.getAdditionalInformation().get(ShmConstants.ACTIVITYNAME);
                final long activityJobId = requestEvent.getActivityJobId();
                final boolean isActivityCompleted = jobConfigurationService.isJobResultEvaluated(activityJobId);
                if (isActivityCompleted) {
                    LOGGER.warn("Found {} activity result already persisted in ActivityJob PO. Removing MO action MTR from cache for activityJobId : {}.", activityName,                             activityJobId);
                    moActionMTRManager.removeMoActionMTRFromCache(activityJobId);
                    return;
                }
                final MOActionCallBack moActionCallBackImpl = getMOActionCallBackService(activityName, jobType, platformType);
                if (moActionCallBackImpl != null) {
                    moActionCallBackImpl.repeatExecute(activityJobId);
                } else {
                    LOGGER.info("Unable to resolve MO action callback service for jobType : {} ,platform : {} , activityName : {} ", jobType, platformType, activityName);
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while sending attributes for activityJobId {}. Exception is : ", requestEvent, ex);
        }
    }

    private MOActionCallBack getMOActionCallBackService(final String activityName, final JobTypeEnum jobType, final PlatformTypeEnum platformType) {
        return moActionCallBackResolver.getMOActionCallBackService(platformType, jobType, activityName);
    }
}
