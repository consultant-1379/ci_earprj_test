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
package com.ericsson.oss.services.shm.es.impl.axe.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

public class AxeUpgradeCancelNotificationHandler extends AxeAbstractNotificationHandler {

    public void processNotification(final Map<String, Object> opsResponseAttributes, final NEJobStaticData neJobStaticData, final long activityJobId, final String activityName,
            final int activityOrder) throws MoNotFoundException {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        OPSScriptExecStatus status = null;
        if (opsResponseAttributes.get(AxeUpgradeActivityConstants.STATUS) != null  && !opsResponseAttributes.get(AxeUpgradeActivityConstants.STATUS).toString().isEmpty()) {
            status = OPSScriptExecStatus.valueOf(opsResponseAttributes.get(AxeUpgradeActivityConstants.STATUS).toString());
            logger.debug("processActivityStateAndResultForCancelResponse-->status {}", status);
            final Double progressPercentage = persistProgressPercentage(opsResponseAttributes);
            final boolean islatestResponse = verifyResponseTimeStampAttribute(opsResponseAttributes, activityJobId);
            switch (status) {

            case FAILED:
                prepareJobLogAndPropertyForFailedState(opsResponseAttributes, activityName, jobPropertyList, jobLogList);
                updateActivityAndNeJobProgressPercentage(jobPropertyList, jobLogList, progressPercentage, activityJobId, neJobStaticData);
                activityUtils.sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activityName, null, JobVariables.CANCEL_ACTION_DONE);
                break;
            case FINISHED:
                processSuccessCase(neJobStaticData, activityJobId, activityName, activityOrder, jobPropertyList, jobLogList, progressPercentage);
                break;
            case INTERRUPTED:
                processInterruptedState(islatestResponse, neJobStaticData, opsResponseAttributes, activityName, progressPercentage, status, JobVariables.CANCEL_IN_PROGRESS);
                break;
            case NOT_STARTED:
                processNotStartedState(islatestResponse, neJobStaticData, activityJobId, activityName, progressPercentage, status, JobVariables.CANCEL_IN_PROGRESS);
                break;
            case RUNNING:
                processRunningState(islatestResponse, neJobStaticData, activityJobId, activityName, progressPercentage, status, JobVariables.CANCEL_IN_PROGRESS);
                break;
            case STOPPED:
                if (progressPercentage == JobLogConstants.PROGRESS_PERCENTAGE_AS_MINUS_ONE) {
                    prepareJobLogAndPropertyForCancelledState(activityName, jobPropertyList, jobLogList);
                    updateActivityAndNeJobProgressPercentage(jobPropertyList, jobLogList, progressPercentage, activityJobId, neJobStaticData);
                    activityUtils.sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activityName, null, JobVariables.CANCEL_ACTION_DONE);
                } else {
                    persistLogIfImproperProgressPercentageReceived(activityJobId);
                }
                break;
            case WAITING_FOR_INPUT:
                processWaitForInputState(opsResponseAttributes, neJobStaticData, activityJobId, activityName, progressPercentage, status, JobVariables.CANCEL_IN_PROGRESS);
                break;
            default:
                persistLogIfInvalidStatusReceived(activityJobId, status, opsResponseAttributes);
            }
        } else {
            persistLogIfStatusNotReceived(activityJobId, opsResponseAttributes);
        }
    }

    @Override
    protected void processSuccessCase(final NEJobStaticData neJobStaticData, final long activityJobId, final String activityName, final int activityOrder,
            final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList, final Double progressPercentage) throws MoNotFoundException {
        prepareJobLogAndPropertyForFinishedState(activityName, jobPropertyList, jobLogList);
        updateActivityAndNeJobProgressPercentage(jobPropertyList, jobLogList, progressPercentage, activityJobId, neJobStaticData);
        activityUtils.sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activityName, null, JobVariables.CANCEL_ACTION_DONE);
        checkForSyncActivityToNotify(activityJobId, neJobStaticData, activityOrder);
    }

    /**
     * @param activityName
     * @param jobPropertyList
     * @param jobLogList
     */
    private void prepareJobLogAndPropertyForCancelledState(final String activityName, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList) {
        logger.debug("prepareJobLogAndPropertyForCancelledState-->activityName {} ", activityName);
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.CANCELLED.getJobResult());
        activityUtils.addJobLog(String.format(JobLogConstants.ACTIVITY_CANCELLED_SUCCESSFULLY, activityName), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());

    }

}
