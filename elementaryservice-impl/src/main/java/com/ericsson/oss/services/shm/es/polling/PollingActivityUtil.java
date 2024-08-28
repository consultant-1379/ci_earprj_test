/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.polling;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FdnUtils;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.common.networkelement.NetworkElementAttributes;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.polling.api.PollCycleStatus;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityAttributes;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants;
import com.ericsson.oss.services.shm.es.polling.api.PollingData;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.ShmPollingActivityData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

/**
 * This Util class is used to prepare the Polling Activity PO attributes
 * 
 * @author xsrabop
 */
public class PollingActivityUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingActivityUtil.class);

    @Inject
    private OssModelInfoProvider ossModelInfoProvider;

    @Inject
    private PollingActivityConfigurationImpl pollingActivityConfiguration;

    @Inject
    private NeJobStaticDataProvider neJobsStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    public Map<String, Object> preparePoAttributes(final JobActivityInfo jobActivityInfo, final NetworkElementData networkElementData, final PollingData pollingData, final String callbackQueue,
            final String pollingType) {
        final OssModelInfo ossModelInfo = getOssModelInfo(networkElementData, pollingData.getFragmentType());
        final String ossPrefix = ((NetworkElementAttributes) networkElementData).getOssPrefix();
        final Map<String, Object> pollingEntry = new HashMap<String, Object>();
        pollingEntry.put(PollingActivityConstants.ACTIVITY_JOB_ID, jobActivityInfo.getActivityJobId());
        pollingEntry.put(PollingActivityConstants.MO_FDN, pollingData.getMoFdn());
        pollingEntry.put(PollingActivityConstants.MO_ATTRIBUTES, pollingData.getMoAttributes());
        if (ossModelInfo != null) {
            pollingEntry.put(PollingActivityConstants.MIM_VERSION, ossModelInfo.getVersion());
            pollingEntry.put(PollingActivityConstants.NAMESPACE, ossModelInfo.getNamespace());
        }
        if (pollingType != null) {
            pollingEntry.put(PollingActivityConstants.POLLING_TYPE, pollingType);
        }
        pollingEntry.put(PollingActivityConstants.ADDITIONAL_INFORMATION, prepareAdditionalInformation(jobActivityInfo, callbackQueue, ossPrefix, pollingData.getAdditionalInformation()));
        pollingEntry.put(PollingActivityConstants.POLL_CYCLE_STATUS, PollCycleStatus.READY.name());
        return pollingEntry;
    }

    private OssModelInfo getOssModelInfo(final NetworkElementData networkElementData, final String fragmentType) {
        OssModelInfo ossModelInfo = null;
        if (fragmentType != null) {
            ossModelInfo = ossModelInfoProvider.getOssModelInfo(networkElementData.getNeType(), networkElementData.getOssModelIdentity(), fragmentType);
        } else {
            final List<OssModelInfo> ossModelInfos = ossModelInfoProvider.getOssModelInfo(networkElementData.getNeType(), networkElementData.getOssModelIdentity());
            if (ossModelInfos != null && !ossModelInfos.isEmpty()) {
                ossModelInfo = ossModelInfos.get(0);
            }
        }
        return ossModelInfo;
    }

    public Map<String, String> prepareAdditionalInformation(final JobActivityInfo jobActivityInfo, final String callbackQueue, final String ossPrefix, final Map<String, String> additionalInfo) {
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(ShmConstants.JOB_TYPE, jobActivityInfo.getJobType().name());
        additionalInformation.put(ShmConstants.PLATFORM, jobActivityInfo.getPlatform().name());
        additionalInformation.put(ShmConstants.ACTIVITYNAME, jobActivityInfo.getActivityName());
        additionalInformation.put(PollingActivityConstants.OPERATION_TYPE, ShmConstants.READ);
        if (ossPrefix != null) {
            additionalInformation.put(PollingActivityConstants.NE_OSS_PREFIX, ossPrefix);
        }
        if (callbackQueue != null) {
            additionalInformation.put(PollingActivityConstants.POLLING_CALLBACK_QUEUE, callbackQueue);
        }
        if (additionalInfo != null) {
            additionalInformation.putAll(additionalInfo);
        }
        return additionalInformation;
    }

    /**
     * This method add the current time to the operation time out and gives the max wait time to read
     * 
     * @param operationTimeOut
     */
    public long getMaxWaitTimeToRead(final long operationTimeOut) {
        return System.currentTimeMillis() + operationTimeOut;
    }

    public Map<String, Object> preparePoAttributes(final ShmPollingActivityData shmPollingActivityData) {
        final Map<String, Object> pollingEntry = new HashMap<>();
        pollingEntry.put(PollingActivityConstants.ACTIVITY_JOB_ID, shmPollingActivityData.getActivityJobId());
        pollingEntry.put(PollingActivityConstants.MO_FDN, shmPollingActivityData.getMoFdn());
        pollingEntry.put(PollingActivityConstants.MO_ATTRIBUTES, shmPollingActivityData.moAttributes());
        pollingEntry.put(PollingActivityConstants.MIM_VERSION, shmPollingActivityData.getNamespace());
        pollingEntry.put(PollingActivityConstants.NAMESPACE, shmPollingActivityData.getMimVersion());
        pollingEntry.put(PollingActivityConstants.ADDITIONAL_INFORMATION, shmPollingActivityData.getAdditionalInformation());
        pollingEntry.put(PollingActivityConstants.POLL_CYCLE_STATUS, PollCycleStatus.READY.name());
        return pollingEntry;
    }

    /**
     * @param attributes
     * @param maxWaitTimeToRead
     */
    public void populateAttributesTobeUpdated(final Map<String, Object> attributes, final long maxWaitTimeToRead) {
        attributes.put(PollingActivityConstants.MAX_WAIT_TIME_TO_READ, maxWaitTimeToRead);
        attributes.put(PollingActivityConstants.POLL_CYCLE_STATUS, PollCycleStatus.IN_PROGRESS.toString());
        attributes.put(PollingActivityConstants.POLL_INITIATED_TIME, System.currentTimeMillis());
    }

    /**
     * This method retrieves the operationTimeout based on the Platform
     * 
     * @param Platform
     */
    public long getOperationTimeOutBasedOnPlatformType(final String platform) {
        long operationTimeOut = 0;
        if (platform != null && platform.equalsIgnoreCase(PlatformTypeEnum.CPP.toString())) {
            operationTimeOut = pollingActivityConfiguration.getOperationTimeOutForCppBasedNodes();
        } else if (platform != null && platform.equalsIgnoreCase(PlatformTypeEnum.ECIM.toString())) {
            operationTimeOut = pollingActivityConfiguration.getOperationTimeOutForEcimBasedNodes();
        }
        return operationTimeOut;

    }

    /**
     * @param pollingEntry
     */
    public ShmPollingActivityData getPollingActivityData(final long activityJobId, final Map<String, Object> pollingEntry) {
        final String moFdn = (String) pollingEntry.get(PollingActivityConstants.MO_FDN);
        final String namespace = (String) pollingEntry.get(PollingActivityConstants.NAMESPACE);
        final String mimVersion = (String) pollingEntry.get(PollingActivityConstants.MIM_VERSION);
        final String pollCycleStatus = (String) pollingEntry.get(PollingActivityConstants.POLL_CYCLE_STATUS);
        final List<String> moReadAttributes = (List<String>) pollingEntry.get(PollingActivityConstants.MO_ATTRIBUTES);
        final Map<String, String> additionalInformation = (Map<String, String>) pollingEntry.get(PollingActivityConstants.ADDITIONAL_INFORMATION);

        return new PollingActivityAttributes(activityJobId, moFdn, namespace, mimVersion, moReadAttributes, additionalInformation, pollCycleStatus);
    }

    /**
     * 
     * This method verifies if the activity timeout is elapsed
     * 
     * @param attributes
     * @return
     * 
     */
    public boolean isActivityTimeoutElapsed(final Map<String, Object> pollingAttributes) {
        LOGGER.debug("Inside isActivityTimeoutElapsed method with Polling PO attributes: {}", pollingAttributes);
        long activityJobId = 0L;
        try {
            activityJobId = (long) pollingAttributes.get(PollingActivityConstants.ACTIVITY_JOB_ID);
            final Map<String, Object> additionalInfo = (Map<String, Object>) pollingAttributes.get(PollingActivityConstants.ADDITIONAL_INFORMATION);
            final String jobType = (String) additionalInfo.get(ShmConstants.JOB_TYPE);
            final String platform = (String) additionalInfo.get(ShmConstants.PLATFORM);
            final String activityName = (String) additionalInfo.get(ShmConstants.ACTIVITYNAME);
            final String moFdn = (String) pollingAttributes.get(PollingActivityConstants.MO_FDN);
            final String nodeName = FdnUtils.getNodeName(moFdn);
            final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
            final long activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(networkElementData.getNeType(), platform, jobType, activityName);
            final long currentTimeInMillis = System.currentTimeMillis();
            final long activityStartTime = neJobsStaticDataProvider.getActivityStartTime(activityJobId);
            if ((currentTimeInMillis - activityStartTime) > (activityTimeout * 60000)) {
                return true;
            }
        } catch (final MoNotFoundException ex) {
            LOGGER.error("ActivityJob PO with Id:{} not exists in DPS then un-subscribe for polling without waiting for maxtime elapsed.", activityJobId);
            return true;
        } catch (final Exception ex) {
            LOGGER.error("Caught Exception while verifying activity elapsed time for activityJobId {} as {}", activityJobId, ex);
        }
        return false;
    }

}
