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
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

public class AxeUpgradeNotificationHandler extends AxeAbstractNotificationHandler {

    public void processNotification(final Map<String, Object> opsResponseAttributes, final NEJobStaticData neJobStaticData, final long activityJobId, final String activityName,
            final int activityOrder) throws MoNotFoundException {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        Double progressPercentage = persistProgressPercentage(opsResponseAttributes);
        OPSScriptExecStatus status = null;
        if (opsResponseAttributes.get(AxeUpgradeActivityConstants.STATUS) != null && !opsResponseAttributes.get(AxeUpgradeActivityConstants.STATUS).toString().isEmpty()) {
            status = OPSScriptExecStatus.getStatusName(opsResponseAttributes.get(AxeUpgradeActivityConstants.STATUS).toString());
            final boolean islatestResponse = verifyResponseTimeStampAttribute(opsResponseAttributes, activityJobId);
            switch (status) {

            case FAILED:
                prepareJobLogAndPropertyForFailedState(opsResponseAttributes, activityName, jobPropertyList, jobLogList);
                updateLogsWithProgressAndNotifyWfs(neJobStaticData, activityJobId, activityName, activityOrder, jobPropertyList, jobLogList, progressPercentage);
                break;

            case FINISHED:
                processSuccessCase(neJobStaticData, activityJobId, activityName, activityOrder, jobPropertyList, jobLogList, progressPercentage);
                break;

            case INTERRUPTED:
                processInterruptedState(islatestResponse, neJobStaticData, opsResponseAttributes, activityName, progressPercentage, status, JobVariables.RESET_HANDLE_TIMEOUT);
                break;
            case NOT_STARTED:
                persistSessionAndClusterId(opsResponseAttributes, activityJobId, jobPropertyList, jobLogList);
                processNotStartedState(islatestResponse, neJobStaticData, activityJobId, activityName, progressPercentage, status, JobVariables.RESET_HANDLE_TIMEOUT);
                break;

            case RUNNING:
                processRunningState(islatestResponse, neJobStaticData, activityJobId, activityName, progressPercentage, status, JobVariables.RESET_HANDLE_TIMEOUT);
                break;

            case STOPPED:
                activityUtils.addJobLog(String.format(JobLogConstants.AXE_SCRIPT_EXECUTION_STOPPED, activityName, progressPercentage), JobLogType.NE.toString(), jobLogList,
                        JobLogLevel.INFO.toString());
                if (progressPercentage >= 0.0) {
                    processSuccessCase(neJobStaticData, activityJobId, activityName, activityOrder, jobPropertyList, jobLogList, progressPercentage);
                } else {
                    progressPercentage = 0.0;
                    prepareJobLogAndPropertyForFailedState(opsResponseAttributes, activityName, jobPropertyList, jobLogList);
                    updateLogsWithProgressAndNotifyWfs(neJobStaticData, activityJobId, activityName, activityOrder, jobPropertyList, jobLogList, progressPercentage);
                }
                break;
            case WAITING_FOR_INPUT:
                processWaitForInputState(opsResponseAttributes, neJobStaticData, activityJobId, activityName, progressPercentage, status, JobVariables.RESET_HANDLE_TIMEOUT);
                break;

            default:
                persistLogIfInvalidStatusReceived(activityJobId, status, opsResponseAttributes);
            }
        } else {
            persistLogIfStatusNotReceived(activityJobId, opsResponseAttributes);
        }
    }

    /**
     * @param neJobStaticData
     * @param activityJobId
     * @param activityName
     * @param activityOrder
     * @param jobPropertyList
     * @param jobLogList
     * @param progressPercentage
     * @throws MoNotFoundException
     */
    private void updateLogsWithProgressAndNotifyWfs(final NEJobStaticData neJobStaticData, final long activityJobId, final String activityName, final int activityOrder,
            final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList, final Double progressPercentage) throws MoNotFoundException {
        boolean isPersisted;
        isPersisted = updateActivityAndNeJobProgressPercentage(jobPropertyList, jobLogList, progressPercentage, activityJobId, neJobStaticData);
        if (isPersisted) {
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityName, null);
            failOtherNeJobsIfActvitityisSync(activityJobId, neJobStaticData, activityOrder);
        }
    }

}
