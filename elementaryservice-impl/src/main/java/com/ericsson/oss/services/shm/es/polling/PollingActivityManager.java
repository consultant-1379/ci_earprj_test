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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousPollingActivity;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.dps.events.DpsStatusInfoProvider;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants;
import com.ericsson.oss.services.shm.es.polling.api.PollingData;
import com.ericsson.oss.services.shm.es.polling.cache.PollingActivityCacheManager;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.ShmPollingActivityData;
import com.ericsson.oss.services.shm.model.event.based.mediation.MOReadRequest;
import com.ericsson.oss.services.shm.model.event.based.mediation.PollCycleStatus;
import com.ericsson.oss.services.shm.model.event.based.mediation.ShmCppReadMOMediationTaskRequest;
import com.ericsson.oss.services.shm.model.event.based.mediation.ShmEcimReadMOMediationTaskRequest;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * This class is used to manage the subscription,un-subscription and retrieval of polling entries from the database.
 * 
 * @author xsrabop
 */
public class PollingActivityManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(PollingActivityManager.class);

    private static final List<String> requiredAttributes = Arrays.asList(PollingActivityConstants.MAX_WAIT_TIME_TO_READ);

    @Inject
    private PollingActivityOperationsRetryProxy pollingActivityOperationsRetryProxy;

    @Inject
    @Modeled
    private EventSender<ShmEcimReadMOMediationTaskRequest> ecimEventSender;

    @Inject
    @Modeled
    private EventSender<ShmCppReadMOMediationTaskRequest> cppEventSender;

    @Inject
    private PollingActivityUtil pollingActivityUtil;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private PollingActivityCacheManager pollingActivityCacheManager;

    @Inject
    private DpsStatusInfoProvider dpsStatusInfoProvider;

    @Inject
    private PollingActivityOperations pollingActivityOperations;

    /**
     * This method is used to subscribe for polling. <br>
     * <b>SHM should not send callbackQueue in additionAttributes field, since mediation sends the response through modeled event bus on default modeled channel.
     * 
     */
    public void subscribe(final JobActivityInfo jobActivityInfo, final NetworkElementData networkElementData, final String fragmentType, final String moFdn, final List<String> moAttributes) {
        final PollingData pollingData = new PollingData(fragmentType, moFdn, moAttributes, new HashMap<>());
        final Map<String, Object> pollingEntry = pollingActivityUtil.preparePoAttributes(jobActivityInfo, networkElementData, pollingData, null, null);
        createPollingPO(jobActivityInfo, moFdn, pollingEntry);
    }

    /**
     * This method is used to subscribe for NHC Polling. <br>
     * <b>callbackQueue is needed only for other than SHM listeners since mediation sends the response on given callbackQueue explicitly
     * 
     */
    public void subscribe(final JobActivityInfo jobActivityInfo, final NetworkElementData networkElementData, final PollingData pollingData, final String callbackQueue, final String pollingType) {
        final Map<String, Object> pollingEntry = pollingActivityUtil.preparePoAttributes(jobActivityInfo, networkElementData, pollingData, callbackQueue, pollingType);
        createPollingPO(jobActivityInfo, pollingData.getMoFdn(), pollingEntry);
    }

    private void createPollingPO(final JobActivityInfo jobActivityInfo, final String moFdn, final Map<String, Object> pollingEntry) {
        try {
            if (!dpsStatusInfoProvider.isDatabaseAvailable()) {
                pollingActivityCacheManager.addPollingActivityDataInCache(pollingEntry);
            } else if (!pollingEntry.isEmpty()) {
                pollingActivityOperationsRetryProxy.createPO(pollingEntry);
                activityUtils.recordEvent(SHMEvents.POLLING_SUBSCRIPTION_SUCCESS, jobActivityInfo.getActivityName(), jobActivityInfo.getJobType().name(), "SHM:" + jobActivityInfo.getActivityJobId());
            } else {
                LOGGER.info("Ignoring polling subscription as input is not valid to persist in PollingActivity PO for MO: {}", moFdn);
            }
        } catch (final RuntimeException e) {
            if (!dpsStatusInfoProvider.isDatabaseAvailable()) {
                pollingActivityCacheManager.addPollingActivityDataInCache(pollingEntry);
            }
            LOGGER.error("Checking Database Availabilty in subscribe PollingActivityManager, runtime exception ", e);
        }
    }

    /**
     * This method un-subscribes from polling with the given poId.
     * 
     * @param poId
     */
    public void unsubscribeByPOId(final long poId) {
        try {
            pollingActivityOperationsRetryProxy.deletePOByPOId(poId);
            activityUtils.recordEvent(SHMEvents.POLLING_UNSUBSCRIPTION_SUCCESS, "PoId is " + poId, "", "");

        } catch (final RetriableCommandException ex) {
            LOGGER.error("Unable to un-subscribe for polling for poId : {}. Exception is : {}", poId, ex);
        }
    }

    /**
     * This method fetches the poId of the polling entry for the given activityJobId and un-subscribe from polling. l
     * 
     * @param activityJobId
     */
    public void unsubscribeByActivityJobId(final long activityJobId, final String activityName, final String nodeName) {
        try {
            pollingActivityOperationsRetryProxy.deletePOByActivityJobId(activityJobId);
            activityUtils.recordEvent(SHMEvents.POLLING_UNSUBSCRIPTION_SUCCESS, activityName, nodeName, "ActivityJobId:" + activityJobId);
        } catch (final RetriableCommandException ex) {
            LOGGER.error("Unable to un-subscribe for polling for acivityJobId : {}. Exception is : {}", activityJobId, ex);
        }
    }

    /**
     * This method fetches the polling entry POID which contains the given activityJobId.
     * 
     * @param activityJobId
     */
    public long getPoIdByActivityJobId(final long activityJobId) {
        LOGGER.debug("Inside PollingActivityManager.getPoIdByActivityJobId() for activityJobId: {}", activityJobId);
        return pollingActivityOperationsRetryProxy.getPoIdByActivityJobId(activityJobId);
    }

    /**
     * This method updates the given attributes for the given polling entry poId.
     * 
     * @param poId
     * @param attributes
     */
    public void updatePollingAttributesByPoId(final long poId, final Map<String, Object> attributes) {
        LOGGER.debug("Inside PollingActivityManager.updatePollingAttributesByPoId with poId {} , attributes : {}", poId, attributes);
        pollingActivityOperationsRetryProxy.updatePollingAttributesByPoId(poId, attributes);
    }

    /**
     * This method updates the given attributes in Polling Activity PO for the given activityJobId.
     * 
     * @param activityJobId
     * @param attributes
     */
    public void updatePollingAttributesByActivityJobId(final long activityJobId, final Map<String, Object> attributes) {
        LOGGER.debug("Inside PollingActivityManager.updatePollingAttributesByActivityJobId with activityJobId {} , attributes : {}", activityJobId, attributes);
        pollingActivityOperationsRetryProxy.updatePollingAttributesByActivityJobId(activityJobId, attributes);
    }

    /**
     * This method gets all the Polling Entries from DPS
     * 
     */
    public List<Map<String,Object>> getPollingActivityPOs() {
        List<Map<String,Object>> pollingActivityPos = new ArrayList<Map<String,Object>>();
        LOGGER.debug("Inside PollingActivityManager.getPollingActivityPOs");
        try {
            pollingActivityPos = pollingActivityOperationsRetryProxy.getPollingActivityPOs();
        } catch (final RetriableCommandException ex) {
            LOGGER.error("Exception occured while getting the polling activity details from DPS:", ex);
        }
        return pollingActivityPos;

    }

    public void processMOReadRequest(final MOReadRequest requestEvent) {
        String moFdn = "";
        long activityJobId = 0;
        try {
            moFdn = requestEvent.getMoFdn();
            final String pollCycleStatus = requestEvent.getPollCycleStatus().toString();
            final Map<String, String> additionalInformation = requestEvent.getAdditionalInformation();

            final String platform = additionalInformation.get(ShmConstants.PLATFORM);
            activityJobId = requestEvent.getActivityJobId();
            final String pollingActivityPoIdAsString = additionalInformation.get(ShmConstants.PO_ID);
            final long pollingActivityPoId = Long.parseLong(pollingActivityPoIdAsString);
            final long operationTimeOut = pollingActivityUtil.getOperationTimeOutBasedOnPlatformType(platform);
            final long maxWaitTimeToRead = pollingActivityUtil.getMaxWaitTimeToRead(operationTimeOut);
            final Map<String, Object> attributes = new HashMap<>();

            if (pollCycleStatus.equalsIgnoreCase(PollCycleStatus.COMPLETED.toString()) || pollCycleStatus.equalsIgnoreCase(PollCycleStatus.READY.toString())) {
                prepareAndSendMTR(requestEvent, moFdn, activityJobId);
                pollingActivityUtil.populateAttributesTobeUpdated(attributes, maxWaitTimeToRead);
                updatePollingAttributesByPoId(pollingActivityPoId, attributes);
            } else if (pollCycleStatus.equalsIgnoreCase(PollCycleStatus.IN_PROGRESS.toString()) && isOperationTimedOut(pollingActivityPoId, activityJobId, moFdn)) {
                attributes.put(PollingActivityConstants.MAX_WAIT_TIME_TO_READ, maxWaitTimeToRead);
                updatePollingAttributesByPoId(pollingActivityPoId, attributes);
                prepareAndSendMTR(requestEvent, moFdn, activityJobId);
            }
            final String logMessage = String.format(PollingActivityConstants.LOG_MESSAGE, moFdn, requestEvent.getMoAttributes(), requestEvent.getActivityJobId(), requestEvent.getNamespace(),
                    requestEvent.getMimVersion(), requestEvent.getAdditionalInformation());
            LOGGER.debug(logMessage);

        } catch (final Exception ex) {
            LOGGER.error("Failed to process the request received from poll entries queue for the node: {} and its activityJobId is: {}. Reason: ", moFdn, activityJobId, ex);
        }
    }

    /**
     * This method compares the OperationTime with the Max Wait Time to Read
     */
    private boolean isOperationTimedOut(final long pollingActivityPoId, final long activityJobId, final String moFdn) {
        boolean isOperationTimedOut = false;
        final long currentTime = System.currentTimeMillis();
        final Map<String, Object> attributes = pollingActivityOperations.getPollingActivityAttributes(pollingActivityPoId, requiredAttributes);
        if (attributes != null && !attributes.isEmpty()) {
            final long maxWaitTimeToRead = (long) attributes.get(PollingActivityConstants.MAX_WAIT_TIME_TO_READ);
            if (currentTime >= maxWaitTimeToRead) {
                final String message = " Retrying for polling requests after wait time for activity having activityJob as: \"%s\".";
                activityUtils.recordEvent(SHMEvents.POLLING_WAIT_PERIOD_TIMEDOUT, moFdn, "ActivityJobId: " + activityJobId, String.format(message, activityJobId));
                isOperationTimedOut = true;
            } else {
                final String message = "Skipping current polling iteration for activity with Id: \"%s\" as previous polling request is still in progress and not Timedout.";
                activityUtils.recordEvent(SHMEvents.POLLING_CYCLE_STATUS_IN_PROGRESS, moFdn, ActivityConstants.EMPTY, String.format(message, activityJobId));
            }
        } else {
            LOGGER.warn("For activityJobId: {} activity is already completed and PollingActivity PO might be deleted for MO: {}. So ignore polling for this entry.", activityJobId, moFdn);
        }
        return isOperationTimedOut;
    }

    /**
     * This method prepares the Mediation Task Request and sent it to the Mediation Queue
     * 
     * @param requestEvent
     * @param moFdn
     * @param activityJobId
     */
    public void prepareAndSendMTR(final MOReadRequest requestEvent, final String moFdn, final long activityJobId) {
        final Map<String, String> additionalInfoFromRequestQueue = requestEvent.getAdditionalInformation();
        final PlatformTypeEnum platformType = PlatformTypeEnum.getPlatform(additionalInfoFromRequestQueue.get(ShmConstants.PLATFORM));
        final int lastIndex = moFdn.lastIndexOf(ActivityConstants.COMMA);
        final String parentMOFdn = moFdn.substring(0, lastIndex);
        final Map<String, Object> additionalInformation = new HashMap<>();
        additionalInformation.putAll(additionalInfoFromRequestQueue);

        switch (platformType) {
        case CPP:
            prepareAndSendMTRForCppNodes(requestEvent, moFdn, parentMOFdn, additionalInformation, activityJobId);
            break;
        case ECIM:
            prepareAndSendMTRForEcimNodes(requestEvent, moFdn, parentMOFdn, additionalInformation, activityJobId);
            break;
        default:
            LOGGER.warn("Unsupported platform type {} recieved for activityJobId : {}", platformType, activityJobId);
        }

    }

    /**
     * @param requestEvent
     * @param moFdn
     * @param activityJobId
     */
    private void prepareAndSendMTRForEcimNodes(final MOReadRequest requestEvent, final String moFdn, final String parentMOFdn, final Map<String, Object> additionalInformation,
            final long activityJobId) {
        final ShmEcimReadMOMediationTaskRequest shmECIMReadMTR = new ShmEcimReadMOMediationTaskRequest();
        shmECIMReadMTR.setMoFdn(moFdn);
        shmECIMReadMTR.setMoAttributes(requestEvent.getMoAttributes());
        shmECIMReadMTR.setActivityJobId(activityJobId);
        shmECIMReadMTR.setNamespace(requestEvent.getNamespace());
        shmECIMReadMTR.setMimVersion(requestEvent.getMimVersion());
        shmECIMReadMTR.setAdditionalInformation(additionalInformation);
        shmECIMReadMTR.setNodeAddress(parentMOFdn);
        ecimEventSender.send(shmECIMReadMTR);
    }

    /**
     * @param requestEvent
     * @param moFdn
     * @param activityJobId
     */
    private void prepareAndSendMTRForCppNodes(final MOReadRequest requestEvent, final String moFdn, final String parentMOFdn, final Map<String, Object> additionalInformation,
            final long activityJobId) {
        final ShmCppReadMOMediationTaskRequest shmCppReadMTR = new ShmCppReadMOMediationTaskRequest();
        shmCppReadMTR.setMoFdn(moFdn);
        shmCppReadMTR.setMoAttributes(requestEvent.getMoAttributes());
        shmCppReadMTR.setActivityJobId(activityJobId);
        shmCppReadMTR.setNamespace(requestEvent.getNamespace());
        shmCppReadMTR.setVersion(requestEvent.getMimVersion());
        shmCppReadMTR.setAdditionalInformation(additionalInformation);
        shmCppReadMTR.setNodeAddress(parentMOFdn);
        cppEventSender.send(shmCppReadMTR);
    }

    public void processCachePollingEntries() {
        final List<ShmPollingActivityData> shmPollingActivityEntries = pollingActivityCacheManager.getPollingEntriesFromCache();

        for (final ShmPollingActivityData shmPollingActivityData : shmPollingActivityEntries) {
            try {
                final String moFdn = shmPollingActivityData.getMoFdn();
                if (moFdn != null && !ActivityConstants.EMPTY.equals(moFdn)) {
                    final Map<String, Object> pollingEntry = pollingActivityUtil.preparePoAttributes(shmPollingActivityData);
                    pollingActivityOperationsRetryProxy.createPO(pollingEntry);
                    pollingActivityCacheManager.removePollingEntriesFromCache((long) pollingEntry.get(PollingActivityConstants.ACTIVITY_JOB_ID));
                    final Map<String, String> addditionalInformation = (Map<String, String>) pollingEntry.get(PollingActivityConstants.ADDITIONAL_INFORMATION);
                    activityUtils.recordEvent(SHMEvents.POLLING_RE_SUBSCRIPTION_SUCCESS, addditionalInformation.get(ShmConstants.ACTIVITY_NAME), addditionalInformation.get(ShmConstants.JOB_TYPE),
                            "SHM:" + pollingEntry.get(PollingActivityConstants.ACTIVITY_JOB_ID));
                } else {
                    final JobTypeEnum jobType = JobTypeEnum.getJobType(shmPollingActivityData.getAdditionalInformation().get(ShmConstants.JOB_TYPE));
                    final PlatformTypeEnum platformType = PlatformTypeEnum.getPlatform(shmPollingActivityData.getAdditionalInformation().get(ShmConstants.PLATFORM));
                    final String activityName = shmPollingActivityData.getAdditionalInformation().get(ShmConstants.ACTIVITYNAME);

                    final String qualifier = platformType + JobVariables.VAR_NAME_DELIMITER + jobType + JobVariables.VAR_NAME_DELIMITER + activityName;
                    final ServiceFinderBean sfb = new ServiceFinderBean();
                    final AsynchronousPollingActivity pollingCallBackImpl = sfb.find(AsynchronousPollingActivity.class, qualifier);

                    if (pollingCallBackImpl != null) {
                        LOGGER.info("Resubscription for polling has started with activityjobId:{}", shmPollingActivityData.getActivityJobId());
                        pollingCallBackImpl.subscribeForPolling(shmPollingActivityData.getActivityJobId());
                        pollingActivityCacheManager.removePollingEntriesFromCache(shmPollingActivityData.getActivityJobId());
                    }
                }
            } catch (final RuntimeException ex) {
                LOGGER.error("Exception occured while getting the pollingEntries from Cache and Preparing the PollingActivity PO: ", ex);
                dpsStatusInfoProvider.checkDatabaseAvailability(ex);
            }
        }

    }

    /**
     * @param activityJobId
     * @param jobActivityInfo
     */
    public void prepareAndAddPollingActivityDataToCache(final long activityJobId, final JobActivityInfo jobActivityInfo) {
        final Map<String, String> pollingActivityData = pollingActivityUtil.prepareAdditionalInformation(jobActivityInfo, null, null, null);
        addToCache(activityJobId, pollingActivityData);
    }

    /**
     * callbackQueue is needed only for other than SHM listeners since mediation sends the response on given callbackQueue explicitly
     * 
     * @param activityJobId
     * @param jobActivityInfo
     * @param callbackQueue
     */
    public void prepareAndAddPollingActivityDataToCache(final long activityJobId, final JobActivityInfo jobActivityInfo, final String callbackQueue) {
        final Map<String, String> pollingActivityData = pollingActivityUtil.prepareAdditionalInformation(jobActivityInfo, callbackQueue, null, null);
        addToCache(activityJobId, pollingActivityData);
    }

    private void addToCache(final long activityJobId, final Map<String, String> pollingActivityData) {
        final Map<String, Object> activityMap = new HashMap<>();
        activityMap.put(PollingActivityConstants.ACTIVITY_JOB_ID, activityJobId);
        activityMap.put(PollingActivityConstants.ADDITIONAL_INFORMATION, pollingActivityData);
        pollingActivityCacheManager.addPollingActivityDataInCache(activityMap);
        LOGGER.debug("Placed ShmPollingActivityData in Cache for the activityjob id: {}", activityMap.get(PollingActivityConstants.ACTIVITY_JOB_ID));
    }

}
