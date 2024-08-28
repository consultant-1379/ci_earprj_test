/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.noderestart;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.es.api.ActivityCompleteCallBack;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.NodeRestartJobActivityInfo;
import com.ericsson.oss.services.shm.es.api.NodeRestartValidator;
import com.ericsson.oss.services.shm.es.impl.ActivityCompleteCallBackProvider;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

/**
 * Once after receiving final notification, this service will call registered elementary service's "onActionComplete" method. This class is used to trigger the schedule timer on regular intervals to
 * check whether the node is reachable or not.
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class NodeRestartActivityTimer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeRestartActivityTimer.class);

    @Resource
    TimerService timerService;

    @Inject
    private ActivityCompleteCallBackProvider activityCompleteCallBackProvider;

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    private NodeRestartPlatformFactory nodeRestartPlatformFactory;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private SystemRecorder systemRecorder;

    /**
     * This method is used for start the scheduled timer.
     * 
     * @param nodeRestartJobActivityInfo
     * 
     */

    public void startTimer(final NodeRestartJobActivityInfo nodeRestartJobActivityInfo) {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerConfig.setInfo(nodeRestartJobActivityInfo);
        timerService.createIntervalTimer(nodeRestartJobActivityInfo.getCppNodeRestartSleepTime(), nodeRestartJobActivityInfo.getWaitIntervalForEachRetry(), timerConfig);
    }

    /**
     * This method is called by the container after the timer has expired. This is private because elementary services are not supposed to call this method directly.
     * 
     * @param timer
     * @throws JobDataNotFoundException
     * 
     */
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @Timeout
    private void checkIfNodeIsReachable(final Timer timer) {
        final NodeRestartJobActivityInfo nodeRestartJobActivityInfo = (NodeRestartJobActivityInfo) timer.getInfo();
        boolean isNodeUpAndRunning = false;
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(nodeRestartJobActivityInfo.getActivityJobId());
        final String nodeName = jobEnvironment.getNodeName();
        LOGGER.debug("checkIfNodeIsReachable : {} and time :{}", nodeName, new Date());
        final NodeRestartValidator nodeRestartPlatformValidator = nodeRestartPlatformFactory.getNodeRestartValidator(nodeRestartJobActivityInfo.getPlatform());
        LOGGER.debug("Node Restart PlatformValidator is {}", nodeRestartPlatformValidator);
        if (nodeRestartPlatformValidator != null) {
            isNodeUpAndRunning = nodeRestartPlatformValidator.isNodeReachable(nodeName);
            if (!isNodeUpAndRunning) {
                if (nodeRestartJobActivityInfo.getTimeElapsedForCppNodeRestart() <= nodeRestartJobActivityInfo.getMaxTimeForCppNodeRestart()) {
                    nodeRestartJobActivityInfo.setTimeElapsedForCppNodeRestart(nodeRestartJobActivityInfo.getTimeElapsedForCppNodeRestart() + nodeRestartJobActivityInfo.getWaitIntervalForEachRetry());
                } else {
                    LOGGER.error("Node: {} not started till max timeout respective to activity: {}", nodeName, nodeRestartJobActivityInfo.getActivityJobId());
                    actionComplete(timer);
                }
            } else {
                systemRecorder.recordEvent(activityUtils.getJobExecutionUser(jobEnvironment.getMainJobId()), SHMEvents.CPP_NODE_RESTART_ACTION_COMPLETED, EventLevel.COARSE, nodeName,
                        ActivityConstants.EMPTY, nodeName);
                if (nodeRestartJobActivityInfo.getTimeElapsedForCppNodeRestart() == 0) {
                    final List<Map<String, Object>> jobLogList = new ArrayList<>();
                    jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.NODE_IS_REACHABLE), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                    jobUpdateService.readAndUpdateRunningJobAttributes(nodeRestartJobActivityInfo.getActivityJobId(), null, jobLogList, null);
                }
                nodeRestartJobActivityInfo.setTimeElapsedForCppNodeRestart(nodeRestartJobActivityInfo.getTimeElapsedForCppNodeRestart() + nodeRestartJobActivityInfo.getWaitIntervalForEachRetry());
                actionComplete(timer);
            }
        } else {
            LOGGER.error("Implementation class not found for interface NodeRestartValidator for platform = {} ", nodeRestartJobActivityInfo.getPlatform());
        }
    }

    /**
     * This method is used for callback onActionComplete.
     * 
     * @param timer
     * 
     */
    private void actionComplete(final Timer timer) {
        final NodeRestartJobActivityInfo nodeRestartJobActivityInfo = (NodeRestartJobActivityInfo) timer.getInfo();
        final ActivityCompleteCallBack activityImpl = activityCompleteCallBackProvider.onActionCompleteHandler(nodeRestartJobActivityInfo.getPlatform(), nodeRestartJobActivityInfo.getJobType(),
                nodeRestartJobActivityInfo.getActivityName());
        activityImpl.onActionComplete(nodeRestartJobActivityInfo.getActivityJobId());
    }

    public void cancelTimer(final long activityJobId) {
        for (final Timer timer : timerService.getTimers()) {
            final NodeRestartJobActivityInfo nodeRestartJobActivityInfo = (NodeRestartJobActivityInfo) timer.getInfo();
            if (nodeRestartJobActivityInfo.getActivityJobId() == activityJobId) {
                timer.cancel();
                LOGGER.debug("Cancelled Node Restart Timer");
                break;
            }
        }
    }

    public boolean isWaitTimeElapsed(final long activityJobId) {
        for (final Timer timer : timerService.getTimers()) {
            final NodeRestartJobActivityInfo nodeRestartJobActivityInfo = (NodeRestartJobActivityInfo) timer.getInfo();
            if ((nodeRestartJobActivityInfo.getActivityJobId() == activityJobId)
                    && (nodeRestartJobActivityInfo.getTimeElapsedForCppNodeRestart() >= nodeRestartJobActivityInfo.getMaxTimeForCppNodeRestart())) {
                return true;
            }
        }
        return false;
    }
}
