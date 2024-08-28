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
package com.ericsson.oss.services.shm.fa;

import java.io.Serializable;
import java.util.*;

import javax.ejb.Asynchronous;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.fa.api.FaBuildingBlockResponseProvider;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * This class sends a job status to building blocks upon completion of requested operation(job creation or job execution).
 * 
 * @author xswagud
 * 
 */
public class FaBuildingBlocksResponseSender implements FaBuildingBlockResponseProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FaBuildingBlocksResponseSender.class);

    @Inject
    private ChannelLocator channelLocator;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    protected JobConfigurationServiceRetryProxy jobConfigServiceRetryProxy;

    private static final String SHM_CHANNEL_URI = "jms:/queue/ShmBBNotificationQueue";
    private static final String NHC_CHANNEL_URI = "jms:/queue/NhcBBNotificationQueue";

    public static final String IS_INTERMITTENT_RESPONSE = "isIntermittentResponse";

    /**
     * This method push the job status messages to ShmBBNotificationQueue queue.
     * 
     * @param response
     */
    @Override
    @Asynchronous
    public void send(final Map<String, Object> response, final JobCategory jobCategory) {
        LOGGER.info("In FaBuildingBlocksResponseSender.send: {} and jobCategory: {}", response, jobCategory);
        if (response != null) {
            String channelUrl = null;
            if (JobCategory.FA == jobCategory) {
                channelUrl = SHM_CHANNEL_URI;
            } else if (JobCategory.NHC_FA == jobCategory) {
                channelUrl = NHC_CHANNEL_URI;
            }
            LOGGER.debug("Sending FA response : {} through channel Url: {} ", response, channelUrl);
            final Channel channel = channelLocator.lookupChannel(channelUrl);
            channel.send((Serializable) response);
            LOGGER.info("Successfully sent a response: {} to builing block", response);
        } else {
            LOGGER.warn("Received invalid response message: {} to send to building blocks. So ignoring the message", response);
            LOGGER.info("Not able to send response to FA building blocks");
        }

    }

    /**
     * This method sends the updated activity timeout to ShmBBNotificationQueue queue based on activities selected from FA.
     * 
     * @param activityJobId
     * @param neJobStaticData
     * @param jobType
     * @param activityName
     * @param numberOfActivities
     * 
     */
    @Asynchronous
    @Override
    public void sendUpdatedActivityTimeout(final long activityJobId, final NEJobStaticData neJobStaticData, final Map<String, Object> activityJobPoAttributes, final String activityName,
            final int updatedActivityTimeout) {
        final Map<String, Object> response = new HashMap<>();
        LOGGER.debug("In FaBuildingBlocksResponseSender.sendUpdatedActivityTimeOut for activityName : {}. Total timeout: {}", activityName, updatedActivityTimeout);
        try {
            final String jobCategory = jobConfigurationService.getJobCategory(neJobStaticData.getMainJobId());

            if (activityJobPoAttributes.get(ActivityConstants.JOB_PROPERTIES) != null) {
                final List<Map<String, Object>> activityJobProperties = (List<Map<String, Object>>) activityJobPoAttributes.get(ActivityConstants.JOB_PROPERTIES);
                if (JobCategory.FA == JobCategory.getJobCategory(jobCategory)) {
                    LOGGER.debug("ActivityJobProperties: {} from FABuildingBlock sender", activityJobProperties);
                    for (final Map<String, Object> property : activityJobProperties) {
                        if (((String) property.get(ShmJobConstants.KEY)).equalsIgnoreCase(ShmJobConstants.FaCommonConstants.FA_REQUEST_ID)) {
                            response.put(ShmJobConstants.FaCommonConstants.FA_REQUEST_ID, property.get(ShmJobConstants.VALUE));
                        } else if (((String) property.get(ShmJobConstants.KEY)).equalsIgnoreCase(ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME)) {
                            response.put(ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME, property.get(ShmJobConstants.VALUE));
                        } else if (((String) property.get(ShmJobConstants.KEY)).equalsIgnoreCase(ShmJobConstants.USERNAME)) {
                            response.put(ShmJobConstants.USERNAME, property.get(ShmJobConstants.VALUE));
                        } else if (((String) property.get(ShmJobConstants.KEY)).equalsIgnoreCase(ShmJobConstants.FaCommonConstants.FBB_TYPE)) {
                            response.put(ShmJobConstants.FaCommonConstants.FBB_TYPE, property.get(ShmJobConstants.VALUE));
                        }
                    }
                    response.put(ActivityConstants.ACTIVITY_JOB_ID, activityJobId);
                    response.put(ShmConstants.MAIN_JOB_ID, neJobStaticData.getMainJobId());
                    response.put(ShmConstants.NE_JOB_ID, neJobStaticData.getNeJobId());
                    response.put(ShmConstants.ACTIVITY_NAME, activityName);
                    response.put(ShmJobConstants.ACTIVITY_TIME_OUT, updatedActivityTimeout);
                    response.put(ShmConstants.NE_NAME, neJobStaticData.getNodeName());
                    response.put(IS_INTERMITTENT_RESPONSE, true);
                    LOGGER.info("Sending Intermittent Response to FA with updated activity timeout : {}", response);
                    send(response, JobCategory.getJobCategory(jobCategory));
                }
            } else {
                LOGGER.warn("No Activity Job found with the PO Id: {}", activityJobId);
            }
        } catch (Exception ex) {
            LOGGER.error("Exception occurred while sending updated activityTimeout to FA for ActivityJobId:{} due to {}:", activityJobId, ex);
        }
    }
}
