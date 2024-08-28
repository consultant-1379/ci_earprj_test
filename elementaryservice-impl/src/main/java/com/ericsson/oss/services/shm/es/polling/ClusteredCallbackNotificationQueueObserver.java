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
package com.ericsson.oss.services.shm.es.polling;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.classic.EMessageListener;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.PollingCallBack;
import com.ericsson.oss.services.shm.es.polling.api.PollCycleStatus;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.event.based.mediation.MOReadResponse;

/**
 * This class fetches the data from the Queue and delegates the data to the corresponding elementary service.
 * 
 * @author xsrabop
 * 
 */
public class ClusteredCallbackNotificationQueueObserver implements EMessageListener<MOReadResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusteredCallbackNotificationQueueObserver.class);

    @Inject
    private PollingCallBackResolver pollingCallBackResolver;

    @Inject
    private PollingActivityCallbackManager pollingActivityCallbackManager;

    @Inject
    private PollingActivityManager pollingActivityManager;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Override
    public void onMessage(final MOReadResponse moReadResponseEvent) {
        try {
            final Map<String, Object> pollingActivityAttributes = new HashMap<>();
            LOGGER.debug("Entered into ClusteredCallbackNotificationQueueObserver.onMessage with {}", moReadResponseEvent);
            final long activityJobId = moReadResponseEvent.getActivityJobId();
            final JobTypeEnum jobType = JobTypeEnum.getJobType((String) moReadResponseEvent.getAdditionalInformation().get(ShmConstants.JOB_TYPE));
            final PlatformTypeEnum platformType = PlatformTypeEnum.getPlatform((String) moReadResponseEvent.getAdditionalInformation().get(ShmConstants.PLATFORM));
            final String activityName = (String) moReadResponseEvent.getAdditionalInformation().get(ShmConstants.ACTIVITYNAME);
            if (moReadResponseEvent.getErrorMessage() != null && !moReadResponseEvent.getErrorMessage().isEmpty()) {
                LOGGER.error("Received a error message:{} for MO read call for MO FDN:{}", moReadResponseEvent.getErrorMessage(), moReadResponseEvent.getMoFdn());
                pollingActivityAttributes.put(PollingActivityConstants.POLL_CYCLE_STATUS, PollCycleStatus.COMPLETED.toString());
                pollingActivityCallbackManager.updatePollingAttributesByActivityJobId(moReadResponseEvent.getActivityJobId(), pollingActivityAttributes);
                final boolean isActivityCompleted = jobConfigurationService.isJobResultEvaluated(activityJobId);
                if (isActivityCompleted) {
                    LOGGER.debug("Found {} activity result already persisted in ActivityJob PO, Assuming activity completed for {} having activityJobId : {}.", activityName,
                            moReadResponseEvent.getMoFdn(), activityJobId);
                    pollingActivityManager.unsubscribeByActivityJobId(activityJobId, activityName, moReadResponseEvent.getMoFdn());
                }
                return;
            }

            final PollingCallBack pollingCallBackImpl = getPollingCallBackService(activityName, jobType, platformType);
            pollingActivityAttributes.put(PollingActivityConstants.POLL_CYCLE_STATUS, PollCycleStatus.COMPLETED.toString());
            pollingActivityCallbackManager.updatePollingAttributesByActivityJobId(activityJobId, pollingActivityAttributes);
            if (pollingCallBackImpl != null) {
                final Map<String, Object> responseAttributes = new HashMap<>();
                responseAttributes.put(ShmConstants.FDN, moReadResponseEvent.getMoFdn());
                responseAttributes.put(ShmConstants.MO_ATTRIBUTES, moReadResponseEvent.getMoAttributes());
                pollingCallBackImpl.processPollingResponse(moReadResponseEvent.getActivityJobId(), responseAttributes);
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while processing response attributes for activityJobId {}. Exception is : ", moReadResponseEvent.getActivityJobId(), ex);
        }
    }

    protected PollingCallBack getPollingCallBackService(final String activityName, final JobTypeEnum jobType, final PlatformTypeEnum platform) {
        return pollingCallBackResolver.getPollingCallBackService(platform, jobType, activityName);
    }

}
