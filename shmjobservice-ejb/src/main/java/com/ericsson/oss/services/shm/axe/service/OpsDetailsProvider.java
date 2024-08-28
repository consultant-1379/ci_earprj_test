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
package com.ericsson.oss.services.shm.axe.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.job.service.BatchParameterChangeListener;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobservice.axe.OpsSessionAndClusterIdInfo;
import com.ericsson.oss.services.shm.jobservice.common.NEJobInfo;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * This class is used to fetch ops SessionId and Cluster Id for AXE nodes,These ids will be used to open ops GUI in shm GUI
 * 
 * @author zviskar
 *
 */
public class OpsDetailsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpsDetailsProvider.class);

    @Inject
    private ShmAxeServiceRetryProxy shmAxeServiceRetryProxy;

    @Inject
    private BatchParameterChangeListener batchParameterChangeListener;

    /**
     * method to fetch ops session id and cluster id
     * 
     * @param supportedNeTypes
     * @param neTypesToNeJobsMap
     * @return
     */
    public Map<String, List<OpsSessionAndClusterIdInfo>> getSessionIdAndClusterId(final Set<String> supportedNeTypes, final Map<String, List<NEJobInfo>> neTypesToNeJobsMap) {
        final Map<String, List<OpsSessionAndClusterIdInfo>> opsSessionAndClusterIdResults = new HashMap<>();
        if (supportedNeTypes.isEmpty()) {
            return opsSessionAndClusterIdResults;
        }

        final List<OpsSessionAndClusterIdInfo> failureNodes = new ArrayList<>();
        final List<OpsSessionAndClusterIdInfo> successNodes = new ArrayList<>();

        neTypesToNeJobsMap.keySet().retainAll(supportedNeTypes);
        if (!neTypesToNeJobsMap.isEmpty()) {
            final Map<Long, String> neJobIdToNodeNameMap = new HashMap<>();
            for (final String supportedNetype : supportedNeTypes) {
                final List<NEJobInfo> listOfNeJobs = neTypesToNeJobsMap.get(supportedNetype);
                for (final NEJobInfo eachNeJob : listOfNeJobs) {
                    neJobIdToNodeNameMap.put(eachNeJob.getNeJobId(), eachNeJob.getNodeName());
                }
            }
            LOGGER.debug("NeJobId To NodeName: {}", neJobIdToNodeNameMap);

            final List<List<Long>> batchedNeJobIdsList = ListUtils.partition(new ArrayList<>(neJobIdToNodeNameMap.keySet()), batchParameterChangeListener.getJobDetailsQueryBatchSize());
            LOGGER.debug("NeJonIds batch size: {}", batchedNeJobIdsList.size());
            for (final List<Long> eachBatchOfNeJobIds : batchedNeJobIdsList) {
                final Map<Long, OpsSessionAndClusterIdInfo> neJobIdToOpsSessionAndClusterIdInfo = shmAxeServiceRetryProxy.getSessionIdAndClusterId(eachBatchOfNeJobIds);
                final Map<String, List<OpsSessionAndClusterIdInfo>> eachBatchOfOpsGuiDetailsResults = getEachBatchOpsSessionAndClusterIdInfoResult(neJobIdToOpsSessionAndClusterIdInfo,
                        neJobIdToNodeNameMap, eachBatchOfNeJobIds);
                successNodes.addAll(eachBatchOfOpsGuiDetailsResults.get(ShmConstants.SUCCESS_NODES));
                failureNodes.addAll(eachBatchOfOpsGuiDetailsResults.get(ShmConstants.FAILURED_NODES));
            }
            opsSessionAndClusterIdResults.put(ShmConstants.SUCCESS_NODES, successNodes);
            opsSessionAndClusterIdResults.put(ShmConstants.FAILURED_NODES, failureNodes);
        }
        return opsSessionAndClusterIdResults;
    }

    /**
     * method to evaluate ops session id and cluster id successfully fetched nodes and failure nodes
     * 
     * @param eachBatchOpsSessionAndClusterIdInfo
     * @param neJobIdToNodeNameMap
     * @param eachBatchOfNeJobIds
     * @return
     */
    private Map<String, List<OpsSessionAndClusterIdInfo>> getEachBatchOpsSessionAndClusterIdInfoResult(final Map<Long, OpsSessionAndClusterIdInfo> neJobIdToOpsSessionAndClusterIdInfo,
            final Map<Long, String> neJobIdToNodeNameMap, final List<Long> eachBatchOfNeJobIds) {
        final Map<String, List<OpsSessionAndClusterIdInfo>> opsSessionAndClusterIdsData = new HashMap<>();
        final List<OpsSessionAndClusterIdInfo> failureNodesInfo = new ArrayList<>();
        final List<OpsSessionAndClusterIdInfo> successNodesInfo = new ArrayList<>();
        if (!neJobIdToOpsSessionAndClusterIdInfo.isEmpty()) {
            for (final Long neJobId : eachBatchOfNeJobIds) {
                if (!neJobIdToOpsSessionAndClusterIdInfo.containsKey(neJobId)) {
                    failureNodesInfo.add(getFailureReson(neJobIdToNodeNameMap.get(neJobId), ActivityConstants.OPS_SESSIONID_CLUSTERID_NOT_FOUND));
                    continue;
                }
                final OpsSessionAndClusterIdInfo opsSessionAndClusterIdInfo = neJobIdToOpsSessionAndClusterIdInfo.get(neJobId);
                opsSessionAndClusterIdInfo.setNodeName(neJobIdToNodeNameMap.get(neJobId));
                if (null == opsSessionAndClusterIdInfo.getClusterID() || null == opsSessionAndClusterIdInfo.getSessionID()) {
                    opsSessionAndClusterIdInfo.setFailureReason(ActivityConstants.OPS_SESSIONID_CLUSTERID_NOT_FOUND);
                    failureNodesInfo.add(opsSessionAndClusterIdInfo);
                } else {
                    successNodesInfo.add(opsSessionAndClusterIdInfo);
                }
            }
        } else {
            failureNodesInfo.addAll(getfailuredNodesDeatails(neJobIdToNodeNameMap, eachBatchOfNeJobIds, ActivityConstants.OPS_SESSIONID_CLUSTERID_NOT_FOUND));
        }
        opsSessionAndClusterIdsData.put(ShmConstants.SUCCESS_NODES, successNodesInfo);
        opsSessionAndClusterIdsData.put(ShmConstants.FAILURED_NODES, failureNodesInfo);
        return opsSessionAndClusterIdsData;
    }

    /**
     * 
     * @param neJobIdToNeNameMap
     * @param eachBatchOfNeJobIds
     * @param reason
     * @return
     */
    private List<OpsSessionAndClusterIdInfo> getfailuredNodesDeatails(final Map<Long, String> neJobIdToNeNameMap, final List<Long> eachBatchOfNeJobIds, final String reason) {
        final List<OpsSessionAndClusterIdInfo> failureOpsGuiDetailsList = new ArrayList<>();
        for (final Long eachNeJobId : eachBatchOfNeJobIds) {
            failureOpsGuiDetailsList.add(getFailureReson(neJobIdToNeNameMap.get(eachNeJobId), reason));
        }
        return failureOpsGuiDetailsList;
    }

    /**
     * 
     * @param nodeName
     * @param reason
     * @return
     */
    private OpsSessionAndClusterIdInfo getFailureReson(final String nodeName, final String reason) {
        final OpsSessionAndClusterIdInfo opsSessionAndClusterIdInfo = new OpsSessionAndClusterIdInfo();
        opsSessionAndClusterIdInfo.setNodeName(nodeName);
        opsSessionAndClusterIdInfo.setFailureReason(reason);
        return opsSessionAndClusterIdInfo;
    }
}
