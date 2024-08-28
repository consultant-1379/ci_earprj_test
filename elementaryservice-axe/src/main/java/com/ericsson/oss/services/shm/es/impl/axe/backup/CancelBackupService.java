/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.axe.backup;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

/**
 * 
 * This is common class for all cancel actions happening to backup job(create and upload backup), delete backup job.
 * 
 * @author xprapav
 * 
 */
public class CancelBackupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelBackupService.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private PollingActivityManager pollingActivityManager;

    /**
     * This method cancels current running action(i.e createBackup or uploadBackup or DeleteBackup) on Node.
     * 
     * @param activityJobId
     * @param activityName
     * @param defaultactivityTimeout
     */
    public ActivityStepResult cancel(final long activityJobId, final String activityName) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        String logMessage = null;
        String nodeName = null;
        LOGGER.debug("Cancel action on activityJobId {} and activityName {} ", activityJobId, activityName);
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            activityUtils.logCancelledByUser(jobLogList, neJobStaticData, activityName);
            LOGGER.debug("Cancel action going to be triggered on nodeName {}  ", nodeName);
            // TODO: Winfiol is not supporting Cancel in 19.01, So it has to align later
            logMessage = String.format(JobLogConstants.CANCELLATION_NOT_SUPPORTED, activityName);
            LOGGER.debug(logMessage);
            final List<Map<String, Object>> propertyList = new ArrayList<>();
            activityUtils.prepareJobPropertyList(propertyList, ActivityConstants.IS_CANCEL_TRIGGERED, ActivityConstants.CHECK_TRUE);
            updateJobLog(activityJobId, logMessage, jobLogList, propertyList);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);
        } catch (final Exception ex) {
            LOGGER.error("Exception is occured in {} cancel action, exception message : {}", activityName, ex);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            updateJobLog(activityJobId, String.format(JobLogConstants.ACTION_TRIGGER_FAILED, activityName) + String.format(JobLogConstants.FAILURE_REASON, ex.getMessage()), jobLogList, null);
            return activityStepResult;
        }
        LOGGER.debug("Status of Cancel is: {} for activityJobId {}", logMessage, activityJobId);
        return activityStepResult;

    }

    private void updateJobLog(final long activityJobId, final String logMessage, final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> propertyList) {
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, propertyList, jobLogList);
    }

    public ActivityStepResult cancelTimeout(final long activityJobId, final String activityName) {
        String nodeName = null;
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            LOGGER.warn("CreateBackup cancelTimeout for nodeName: {} activityJobId,{} and activityName {}", nodeName, activityJobId, activityName);
            final String logMessage = String.format(JobLogConstants.CANCELLATION_TIMEOUT, activityName);
            pollingActivityManager.unsubscribeByActivityJobId(activityJobId, activityName, nodeName);
            //TODO: Need to evaluate cancel Backup status when Cancel rest end point available 
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
            updateJobLog(activityJobId, logMessage, jobLogList, jobPropertyList);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        } catch (final Exception ex) {
            LOGGER.error("Exception occured in cancelTimeout for createBackup Reason : {} ", ex);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            updateJobLog(activityJobId, String.format(JobLogConstants.ACTIVITY_FAILED, activityName) + String.format(JobLogConstants.FAILURE_REASON, ex.getMessage()), jobLogList, jobPropertyList);
        }
        return activityStepResult;
    }
}
