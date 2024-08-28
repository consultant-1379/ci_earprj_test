/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.axe.synchronous;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ejb.AccessTimeout;
import javax.ejb.EJBException;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.activity.axe.cache.AxeSynchronousActivityData;
import com.ericsson.oss.services.shm.activity.axe.cache.AxeUpgradeSynchronousActivityProvider;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.common.wfs.WfsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

/**
 * Class contains implementation to trigger synchronous Activity on all Nodes,if the notified activity completed on all nodes
 * 
 * @author ztamsra
 *
 */
@Singleton
public class AxeSynchronousActivityProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AxeSynchronousActivityProcessor.class);

    @Inject
    private AxeUpgradeSynchronousActivityProvider upgradeSyncActivityProvider;

    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Inject
    private AxeUpgradeSynchronousActivityService axeUpgradeSyncActivityService;

    @Inject
    private WfsRetryConfigurationParamProvider wfsRetryConfigurationParamProvider;

    @Inject
    private SynchronousNeJobFailHandler synchronousNeJobFailHandler;

    @Lock(LockType.WRITE)
    @AccessTimeout(value = 120000)
    public void checkAndNotifySynchronousActivities(final NEJobStaticData neJobStaticData, final int activityOrder, final String neType) {
        final long mainJobId = neJobStaticData.getMainJobId();
        final boolean isNeTypeSyncCompleted = upgradeSyncActivityProvider.getSyncCompletedStatusFromCache(mainJobId, neType);
        final String cacheKey = mainJobId + "_" + neType;
        if (!isNeTypeSyncCompleted) {
            LOGGER.debug("isNeTypeSyncCompleted not found in cache ,so fetch it from dps");
            final List<Map<String, String>> mainJobProperties = axeUpgradeSyncActivityService.getMainJobProperties(mainJobId);
            if (mainJobProperties != null && !mainJobProperties.isEmpty() && !isKeyExistsInJobProperties(mainJobProperties, neType + "_SyncCompleted")) {
                final Map<String, List<AxeSynchronousActivityData>> cacheData = upgradeSyncActivityProvider.getSynchronousAxeActivities(neJobStaticData.getMainJobId(), neType);
                final List<AxeSynchronousActivityData> axeSyncActivitiesList = cacheData.get(cacheKey);
                LOGGER.debug("checkAndNotifySynchronousActivities dataFromCache {} for key {}", axeSyncActivitiesList, cacheKey);
                notifySynchronousActivities(neJobStaticData, activityOrder, neType, cacheKey, axeSyncActivitiesList, mainJobProperties);
            } else if (mainJobProperties != null && !mainJobProperties.isEmpty() && isKeyExistsInJobProperties(mainJobProperties, cacheKey)) {
                upgradeSyncActivityProvider.updateSyncCompletedStatusInCache(cacheKey + "_SyncCompleted");
            }
        }

    }

    private void notifySynchronousActivities(final NEJobStaticData neJobStaticData, final int activityOrder, final String neType, final String cacheKey,
            final List<AxeSynchronousActivityData> axeSyncActivitiesList, final List<Map<String, String>> mainJobProperties) {
        if (axeSyncActivitiesList != null && !axeSyncActivitiesList.isEmpty()) {
            Collections.sort(axeSyncActivitiesList);
            resetCacheBasedOnNotifiedActivity(activityOrder, cacheKey, axeSyncActivitiesList);
            if (!axeSyncActivitiesList.isEmpty()) {
                if (axeSyncActivitiesList.get(0).getOrder() == 1) {
                    LOGGER.debug("For 1st Activity not required to check for synchronous");
                    axeSyncActivitiesList.remove(0);
                }
                checkIfPreviousActivityFinished(neJobStaticData, activityOrder, neType, cacheKey, axeSyncActivitiesList, mainJobProperties);
            }
        }
    }

    public boolean isKeyExistsInJobProperties(final List<Map<String, String>> mainJobProperties, final String key) {
        LOGGER.info("Search for key {} in {}", key, mainJobProperties);
        for (final Map<String, String> jobProperty : mainJobProperties) {
            if (key.equals(jobProperty.get(ShmConstants.KEY))) {
                LOGGER.info("Found found key");
                return Boolean.valueOf(jobProperty.get(ShmConstants.VALUE));
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void checkIfPreviousActivityFinished(final NEJobStaticData neJobStaticData, final int activityOrder, final String neType, final String cacheKey,
            final List<AxeSynchronousActivityData> axeSyncActivitiesList, final List<Map<String, String>> mainJobProperties) {
        final int cacheOrderId = axeSyncActivitiesList.get(0).getOrder();
        final String activitySyncStatus = neType + "_" + cacheOrderId;
        if (!axeSyncActivitiesList.isEmpty() && activityOrder == cacheOrderId - 1 && !isKeyExistsInJobProperties(mainJobProperties, activitySyncStatus)) {
            LOGGER.debug("Received notification is cache previos activity ,So check for activity status on other nejobs");
            final Map<String, Object> neJobsActivityStatus = axeUpgradeSyncActivityService.checkForActivityStatusOnNeJobs(neJobStaticData.getNeJobId(), neJobStaticData.getMainJobId(), neType,
                    activityOrder);
            LOGGER.debug("cache Activity {} ,status on other jobs:{}", activityOrder, neJobsActivityStatus);
            if (neJobsActivityStatus.containsKey("Completed")) {
                notifyActivityAndClearEntryInCache(neJobStaticData.getMainJobId(), (List<String>) neJobsActivityStatus.get(ShmConstants.NENAMES), axeSyncActivitiesList, cacheKey, neType,
                        cacheOrderId);
            } else if (neJobsActivityStatus.containsKey("Failed")) {
                failNeJobsExplicitlyAndClearCache((List<String>) neJobsActivityStatus.get(ShmConstants.NENAMES), neJobStaticData.getMainJobId(), neType);
                upgradeSyncActivityProvider.clear(neJobStaticData.getMainJobId(), neType);
            }
        }
    }

    private void resetCacheBasedOnNotifiedActivity(final int activityOrder, final String cacheKey, final List<AxeSynchronousActivityData> axeSyncActivitiesList) {
        final List<AxeSynchronousActivityData> axeSyncActivitiesUpdatedList = axeSyncActivitiesList;
        for (final AxeSynchronousActivityData axeActivityStaticData : axeSyncActivitiesList) {
            LOGGER.debug("Current Activity {} Synchronous Activity {}", activityOrder, axeActivityStaticData.getOrder());
            if (activityOrder >= axeActivityStaticData.getOrder()) {
                axeSyncActivitiesUpdatedList.remove(0);
                upgradeSyncActivityProvider.updateAxeSynchronousActivites(cacheKey, axeSyncActivitiesUpdatedList);
            }
        }
    }

    private void notifyActivityAndClearEntryInCache(final long mainJobId, final List<String> nodesList, final List<AxeSynchronousActivityData> axeSyncActivitiesList, final String cacheKey,
            final String neType, final int orderId) {
        try {
            int notifiedNeCount = 0;
            for (final String nodeName : nodesList) {
                if (notifyEachNodeSyncActivity(mainJobId, nodeName)) {
                    notifiedNeCount++;
                }
            }
            if (notifiedNeCount == nodesList.size()) {
                LOGGER.debug("Synchronous Activity triggered on Nodes{}, Update the cache", nodesList);
                axeSyncActivitiesList.remove(0);
                upgradeSyncActivityProvider.updateSyncCompleted(mainJobId, neType, orderId);
                upgradeSyncActivityProvider.updateAxeSynchronousActivites(cacheKey, axeSyncActivitiesList);
                if (upgradeSyncActivityProvider.get(mainJobId, neType) == null || upgradeSyncActivityProvider.get(mainJobId, neType).isEmpty()) {
                    LOGGER.debug("Cache completed for cache key :{}", cacheKey);
                    upgradeSyncActivityProvider.updateSyncCompleted(mainJobId, neType);
                }
            } else {
                LOGGER.debug("Unable to notify synchronous Activty on all nejobs for mainJobId {} ,NeType:{}", mainJobId, neType);
            }
        } catch (final WorkflowServiceInvocationException e) {
            LOGGER.error("Failed to send message to WorkFlow Service due to:", e);
        }
    }

    private boolean notifyEachNodeSyncActivity(final long mainJobId, final String nodeName) {
        final String businessKey = mainJobId + "@" + nodeName;
        boolean isNotified = false;
        final int retryCount = wfsRetryConfigurationParamProvider.getWfsRetryCount();
        final int retryTimeOut = wfsRetryConfigurationParamProvider.getWfsWaitIntervalInMS();
        LOGGER.info("Notifying the workflow for NeJob ");
        for (int i = 1; i <= retryCount; i++) {
            try {
                workflowInstanceNotifier.notifySyncActivity(businessKey, null);
                isNotified = true;
                break;
            } catch (final WorkflowServiceInvocationException | EJBException ex) {
                LOGGER.error("Job Initiation is failed  due to :{} sleep for {} and retry", ex.getMessage(), retryTimeOut);
                sleep(retryTimeOut);
            }
        }
        return isNotified;
    }

    private void sleep(final int retryTimeOut) {
        try {
            Thread.sleep(retryTimeOut);
        } catch (final InterruptedException ex) {
            LOGGER.error("Sleep interrupted due to : ", ex);
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    @Lock(LockType.WRITE)
    @AccessTimeout(value = 120000)
    public void failOtherNeJobsIfActvitityisSync(final long mainJobId, final long neJobId, final String neType, final int activityOrder) {
        LOGGER.info("Enter cancelOtherNeJobsIfActvitityisSync");
        final boolean isNeTypeSyncCompleted = upgradeSyncActivityProvider.getSyncCompletedStatusFromCache(mainJobId, neType);
        final String cacheKey = mainJobId + "_" + neType;
        if (!isNeTypeSyncCompleted) {
            final List<Map<String, String>> mainJobProperties = axeUpgradeSyncActivityService.getMainJobProperties(mainJobId);
            if (mainJobProperties != null && !mainJobProperties.isEmpty() && !isKeyExistsInJobProperties(mainJobProperties, neType + "_SyncCompleted")) {
                final Map<String, List<AxeSynchronousActivityData>> cacheData = upgradeSyncActivityProvider.getSynchronousAxeActivities(mainJobId, neType);
                final List<AxeSynchronousActivityData> axeSyncActivitiesList = cacheData.get(cacheKey);
                LOGGER.info("data from Cache :{}", axeSyncActivitiesList);
                    if (!axeSyncActivitiesList.isEmpty() && cacheData.containsKey(cacheKey) && activityOrder == axeSyncActivitiesList.get(0).getOrder() - 1) {
                        final Map<String, Object> neJobsActivityStatus = axeUpgradeSyncActivityService.checkForActivityStatusOnNeJobs(neJobId, mainJobId, neType, activityOrder);
                        if (neJobsActivityStatus.containsKey("Failed")) {
                            failNeJobsExplicitlyAndClearCache((List<String>) neJobsActivityStatus.get(ShmConstants.NENAMES), mainJobId, neType);
                        }
                    }
            } else if (mainJobProperties != null && !mainJobProperties.isEmpty() && isKeyExistsInJobProperties(mainJobProperties, cacheKey)) {
                upgradeSyncActivityProvider.updateSyncCompletedStatusInCache(cacheKey + "_SyncCompleted");
            }
        }
    }

    private void failNeJobsExplicitlyAndClearCache(final List<String> neJobList, final long mainJobId, final String neType) {
        LOGGER.debug("Cancelling list of ne Jobs {}", neJobList);
        int failedNeCount = 0;
        if (neJobList != null) {
            for (final String nodeName : neJobList) {
                final String businessKey = mainJobId + "@" + nodeName;
                synchronousNeJobFailHandler.failSynchronousNeJobExplicitly(businessKey);
                failedNeCount++;
            }
            if (failedNeCount == neJobList.size()) {
                upgradeSyncActivityProvider.clear(mainJobId, neType);
                upgradeSyncActivityProvider.updateSyncCompletedStatusInCache(mainJobId + "_" + neType);
            }
        }

    }
}