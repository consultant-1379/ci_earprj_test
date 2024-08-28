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
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

public class AxeUpgradeTimeOutNotificationHandler extends AxeAbstractNotificationHandler {

    public void processNotification(final Map<String, Object> opsResponseAttributes, final NEJobStaticData neJobStaticData, final long activityJobId, final String activityName,
            final int activityOrder) throws MoNotFoundException {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final Double progressPercentage = persistProgressPercentage(opsResponseAttributes);
        OPSScriptExecStatus status = null;
        if (opsResponseAttributes.get(AxeUpgradeActivityConstants.STATUS) != null  && !opsResponseAttributes.get(AxeUpgradeActivityConstants.STATUS).toString().isEmpty()) {
            status = OPSScriptExecStatus.getStatusName(opsResponseAttributes.get(AxeUpgradeActivityConstants.STATUS).toString());
            final boolean islatestResponse = verifyResponseTimeStampAttribute(opsResponseAttributes, activityJobId);
            boolean isPersisted = false;
            switch (status) {
            case FAILED:
                prepareJobLogAndPropertyForFailedState(opsResponseAttributes, activityName, jobPropertyList, jobLogList);
                isPersisted = updateActivityAndNeJobProgressPercentage(jobPropertyList, jobLogList, progressPercentage, activityJobId, neJobStaticData);
                if (isPersisted) {
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                    activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, activityName, activityStepResult.getActivityResultEnum());
                    failOtherNeJobsIfActvitityisSync(activityJobId, neJobStaticData, activityOrder);
                }
                break;
            case FINISHED:
                prepareJobLogAndPropertyForFinishedState(activityName, jobPropertyList, jobLogList);
                isPersisted = updateActivityAndNeJobProgressPercentage(jobPropertyList, jobLogList, progressPercentage, activityJobId, neJobStaticData);
                if (isPersisted) {
                    activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
                    activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, activityName, activityStepResult.getActivityResultEnum());
                    checkForSyncActivityToNotify(activityJobId, neJobStaticData, activityOrder);
                }
                break;

            case INTERRUPTED:
                processInterruptedState(islatestResponse, neJobStaticData, opsResponseAttributes, activityName, progressPercentage, status, JobVariables.REPEAT_HANDLE_TIMEOUT);
                break;

            case NOT_STARTED:
                persistSessionAndClusterId(opsResponseAttributes, activityJobId, jobPropertyList, jobLogList);
                processNotStartedState(islatestResponse, neJobStaticData, activityJobId, activityName, progressPercentage, status, JobVariables.REPEAT_HANDLE_TIMEOUT);
                break;

            case RUNNING:
                processRunningState(islatestResponse, neJobStaticData, activityJobId, activityName, progressPercentage, status, JobVariables.REPEAT_HANDLE_TIMEOUT);
                break;

                
            case STOPPED:
                processStoppedState(opsResponseAttributes, neJobStaticData, activityJobId, activityName, activityOrder, activityStepResult, jobPropertyList);
                break;
            case WAITING_FOR_INPUT:
                processWaitForInputState(opsResponseAttributes, neJobStaticData, activityJobId, activityName, progressPercentage, status, JobVariables.REPEAT_HANDLE_TIMEOUT);
                break;

            default:
                persistLogIfInvalidStatusReceived(activityJobId, status, opsResponseAttributes);
            }
        } else {
            persistLogIfStatusNotReceived(activityJobId, opsResponseAttributes);
        }
    }

    /**
     * @param opsResponseAttributes
     * @param neJobStaticData
     * @param activityJobId
     * @param activityName
     * @param activityOrder
     * @param activityStepResult
     * @param jobPropertyList
     * @throws MoNotFoundException
     */
    private void processStoppedState(final Map<String, Object> opsResponseAttributes, final NEJobStaticData neJobStaticData, final long activityJobId, final String activityName,
            final int activityOrder, final ActivityStepResult activityStepResult, final List<Map<String, Object>> jobPropertyList) throws MoNotFoundException {
        boolean isPersisted;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        Double progressPercentage = persistProgressPercentage(opsResponseAttributes);
        if (progressPercentage >= 0.0) {
            activityUtils.addJobLog(String.format(JobLogConstants.AXE_SCRIPT_EXECUTION_STOPPED, activityName, progressPercentage), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
            prepareJobLogAndPropertyForFinishedState(activityName, jobPropertyList, jobLogList);
            isPersisted = updateActivityAndNeJobProgressPercentage(jobPropertyList, jobLogList, progressPercentage, activityJobId, neJobStaticData);
            if (isPersisted) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
                activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, activityName, activityStepResult.getActivityResultEnum());
                checkForSyncActivityToNotify(activityJobId, neJobStaticData, activityOrder);
            }
        } else {
            activityUtils.addJobLog(String.format(JobLogConstants.AXE_SCRIPT_EXECUTION_STOPPED, activityName, progressPercentage), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
            progressPercentage =0.0;
            prepareJobLogAndPropertyForFailedState(opsResponseAttributes, activityName, jobPropertyList, jobLogList);
            isPersisted = updateActivityAndNeJobProgressPercentage(jobPropertyList, jobLogList, progressPercentage, activityJobId, neJobStaticData);
            if (isPersisted) {
                activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
                activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, activityName, activityStepResult.getActivityResultEnum());
                failOtherNeJobsIfActvitityisSync(activityJobId, neJobStaticData, activityOrder);
            }
        }
    }

    @Override
    protected void persistSessionAndClusterId(final Map<String, Object> opsResponseAttributes, final long activityJobId, final List<Map<String, Object>> jobPropertyList,
            final List<Map<String, Object>> jobLogList) {
        activityUtils.prepareJobPropertyList(jobPropertyList, AxeUpgradeActivityConstants.IS_AXE_HANDLETIMEOUT_TRIGGERED, "false");
        super.persistSessionAndClusterId(opsResponseAttributes, activityJobId, jobPropertyList, jobLogList);
    }

    @Override
    protected void addJobLogForStateAndPercentage(final String activityName, final String status, final Double progressPercentage, final List<Map<String, Object>> jobLogList) {
        activityUtils.addJobLog(String.format(JobLogConstants.AXE_ACTIVITY_TIMEOUT_SCRIPT_MESSAGE, activityName, status, progressPercentage), JobLogType.NE.toString(), jobLogList,
                JobLogLevel.INFO.toString());
    }

}
