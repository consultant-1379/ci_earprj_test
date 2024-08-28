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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;

/**
 * Class used to fetch the list of neJobs for given mainJob and NeType Implementation to fetch notified Activity state and result
 * 
 * @author ztamsra
 *
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class AxeUpgradeSynchronousActivityService {

    @Inject
    private AxeSynchronousActivityRetryProxy axeSynchronousActivityRetryProxy;

    @Inject
    private ActivityUtils activityUtils;

    private static final Logger LOGGER = LoggerFactory.getLogger(AxeUpgradeSynchronousActivityService.class);

    public Map<String, Object> checkForActivityStatusOnNeJobs(final long neJobId, final long mainJobId, final String neType, final int activityOrder) {
        final int totalNeCountByNeType = getTotalNeCountByNeType(neJobId);
        final Map<Long, Map<String, String>> neJobDetailsMap = axeSynchronousActivityRetryProxy.retryForTotalNeJobCreation(mainJobId, neType, totalNeCountByNeType);
        final Map<String, Object> activityStatusDetails = new HashMap<>();
        if (neJobDetailsMap != null && !neJobDetailsMap.isEmpty()) {
            checkForNeJobResultAndFailOrNotify(neJobId, activityOrder, neJobDetailsMap, activityStatusDetails);
        } else {
            LOGGER.debug("Al NeJobs are not yet created so can't notify synchronous activity");
        }
        return activityStatusDetails;
    }

    private void checkForNeJobResultAndFailOrNotify(final long neJobId, final int activityOrder, final Map<Long, Map<String, String>> neJobDetailsMap,
            final Map<String, Object> activityStatusDetails) {
        final Map<Long, String> neJobIdAndNodeNameMap = new HashMap<>();
        final Map<Long, String> neJobIdAndJobResult = new HashMap<>();

        for (final Entry<Long, Map<String, String>> neJobDetailEntry : neJobDetailsMap.entrySet()) {
            final Map<String, String> nameAndResult = neJobDetailEntry.getValue();
            for (final Entry<String, String> nameAndResultEntry : nameAndResult.entrySet()) {
                neJobIdAndNodeNameMap.put(neJobDetailEntry.getKey(), nameAndResultEntry.getKey());
                neJobIdAndJobResult.put(neJobDetailEntry.getKey(), nameAndResultEntry.getValue());
            }
        }
        final List<String> nodeNameListToBeNotified = new ArrayList<>(neJobIdAndNodeNameMap.values());
        if (neJobIdAndJobResult.containsValue(JobResult.FAILED.toString())) {
            LOGGER.debug("NeJob Failed with out Activites are :{}", neJobId);
            failOtherNeJobs(activityOrder, activityStatusDetails, neJobIdAndNodeNameMap, neJobIdAndJobResult);
        } else {
            if (neJobDetailsMap.size() == 1 && neJobDetailsMap.containsKey(neJobId)) {
                activityStatusDetails.put("Completed", true);
                activityStatusDetails.put(ShmConstants.NENAMES, nodeNameListToBeNotified);
                LOGGER.debug("As it is single Nejob trigger synchronous activity with out any other checks");
            } else {
                checkForActivityStateAndResult(activityOrder, activityStatusDetails, neJobIdAndNodeNameMap, nodeNameListToBeNotified);
            }
        }
    }

    private void checkForActivityStateAndResult(final int activityOrder, final Map<String, Object> activityStatusDetails, final Map<Long, String> neJobIdAndNodeNameMap,
            final List<String> nodeNameListToBeNotified) {
        final Map<Long, String> activityCompletedNes = axeSynchronousActivityRetryProxy.checkforActivityCompletion(neJobIdAndNodeNameMap, activityOrder);
        if (activityCompletedNes != null && !activityCompletedNes.isEmpty()) {
            LOGGER.debug("Current Activity {} completed on all nes", activityOrder);
            checkActivityOnOtherNeJobResult(neJobIdAndNodeNameMap, nodeNameListToBeNotified, activityStatusDetails, activityCompletedNes);
        } else {
            LOGGER.debug("After retries activites are not yet completed,So exit");
        }
    }

    private void failOtherNeJobs(final int activityOrder, final Map<String, Object> activityStatusDetails, final Map<Long, String> neJobIdAndNodeNameMap, final Map<Long, String> neJobIdAndJobResult) {
        final List<String> nodeListToFail = fetchRunningNodesToFailExplicitly(neJobIdAndNodeNameMap, neJobIdAndJobResult);
        final Map<Long, String> jobIdAndNodeNamesToFail = new HashMap<>();
        for (final Entry<Long, String> neJobIdAndNodeName : neJobIdAndNodeNameMap.entrySet()) {
            if (nodeListToFail.contains(neJobIdAndNodeName.getValue())) {
                jobIdAndNodeNamesToFail.put(neJobIdAndNodeName.getKey(), neJobIdAndNodeName.getValue());
            }
        }
        final Map<Long, String> activityCompletedNes = axeSynchronousActivityRetryProxy.checkforActivityCompletion(jobIdAndNodeNamesToFail, activityOrder);
        if (activityCompletedNes != null && !activityCompletedNes.isEmpty()) {
            LOGGER.info("Current Activity completed on running nejobs");
            setActivityStatusAsFailed(nodeListToFail, activityStatusDetails);
        }
    }

    private void setActivityStatusAsFailed(final List<String> nodeListToFail, final Map<String, Object> activityStatusDetails) {
        activityStatusDetails.put("Failed", true);
        activityStatusDetails.put(ShmConstants.NENAMES, nodeListToFail);

    }

    @SuppressWarnings("unchecked")
    private int getTotalNeCountByNeType(final long neJobId) {
        final StringBuilder nodeNames = new StringBuilder();
        try {
            final Map<String, Object> neJobAttributes = activityUtils.getPoAttributes(neJobId);
            final List<Map<String, String>> neJobProperties = (List<Map<String, String>>) neJobAttributes.get(ShmConstants.JOBPROPERTIES);
            if (neJobProperties != null) {
                for (final Map<String, String> neJobProperty : neJobProperties) {
                    if (ShmConstants.AXE_NES.equals(neJobProperty.get(ShmConstants.KEY)) || ShmConstants.AXE_UNSUPPORTED_NES.equals(neJobProperty.get(ShmConstants.KEY))) {
                        nodeNames.append(neJobProperty.get(ShmConstants.VALUE));
                        nodeNames.append(",");
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception occurred while retrieving NE Job attributes {} ", neJobId, e);
        }
        return nodeNames.toString().split(",").length;
    }

    private void checkActivityOnOtherNeJobResult(final Map<Long, String> neJobDetails, final List<String> nodeList, final Map<String, Object> activityStatusDetails,
            final Map<Long, String> completedNes) {
        if (completedNes.containsValue(JobResult.FAILED.getJobResult()) || completedNes.containsValue(JobResult.CANCELLED.getJobResult())) {
            final List<String> failedNeNames = fetchRunningNodesToFailExplicitly(neJobDetails, completedNes);
            LOGGER.debug("Failed NE Jobs List {}", failedNeNames);
            if (!failedNeNames.isEmpty()) {
                activityStatusDetails.put(ShmConstants.NENAMES, failedNeNames);
            }
            activityStatusDetails.put("Failed", true);
        } else {
            activityStatusDetails.put(ShmConstants.NENAMES, nodeList);
            activityStatusDetails.put("Completed", true);
        }
    }

    private List<String> fetchRunningNodesToFailExplicitly(final Map<Long, String> neJobDetails, final Map<Long, String> completedNes) {
        final List<Long> neJobIds = new ArrayList<>();
        final List<String> neNames = new ArrayList<>();
        for (final Entry<Long, String> completedNe : completedNes.entrySet()) {
            if (!JobResult.FAILED.getJobResult().equals(completedNe.getValue()) && !JobResult.CANCELLED.getJobResult().equals(completedNe.getValue())) {
                neJobIds.add(completedNe.getKey());
            }
        }
        for (final Long neId : neJobIds) {
            if (neJobDetails.containsKey(neId)) {
                neNames.add(neJobDetails.get(neId));
            }
        }
        return neNames;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getMainJobProperties(final long mainJobId) {
        final Map<String, Object> neJobAttributes = activityUtils.getPoAttributes(mainJobId);
        return (List<Map<String, String>>) neJobAttributes.get(ShmConstants.JOBPROPERTIES);

    }

}
