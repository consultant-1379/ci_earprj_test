/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_END_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.PRECHECK_START_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityCallback;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

/**
 * This class facilitates the installation of upgrade package of CPP based node by invoking the UpgradePackage MO action(depending on the action type) that initializes the install activity.
 * 
 * @author xcharoh
 */
@EServiceQualifier("CPP.UPGRADE.install")
@ActivityInfo(activityName = "install", jobType = JobTypeEnum.UPGRADE, platform = PlatformTypeEnum.CPP)
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class InstallService implements Activity, ActivityCallback, AsynchronousActivity {

    /**
     * PRODUCT_NUMBER and PRODUCT_REVISION can be read from the Job Properties inside the InstallServiceLocal, so passing as null.
     */
    private static final String PRODUCT_NUMBER = null;
    private static final String PRODUCT_REVISION = null;
    /**
     * No need to log the package type for Upgrade Jobs.
     */
    private static final String PACKAGE_TYPE = null;

    private static final String REASON_FOR_FAILURE = "upgrade Package is not ready for installation.";

    @Inject
    private InstallActivityHandler localInstallationService;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallService.class);

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.ActivityCallback#processNotification(com.ericsson.oss.services.shm.notifications.api.Notification)
     */
    @Override
    public void processNotification(final Notification message) {
        LOGGER.debug("Entered cpp upgrade install activity - processNotification with event type : {} ", message.getNotificationEventType());
        if (!NotificationEventTypeEnum.AVC.equals(message.getNotificationEventType())) {
            LOGGER.debug("cpp upgrade install activity - Discarding non-AVC notification.");
            return;
        }
        final NotificationSubject notificationSubject = message.getNotificationSubject();
        final long activityJobId = activityUtils.getActivityJobId(notificationSubject);
        final JobActivityInfo activityInfo = activityUtils.getActivityInfo(activityJobId, InstallService.class);
        final Map<String, Object> processNotificationResult = localInstallationService.processNotification(message, activityInfo);
        final boolean isActivityCompleted = (boolean) processNotificationResult.get(ActivityConstants.ACTIVITY_STATUS);
        // since InstallationService.processNotification will be called for Restore job also, we will persist stepDuration here.
        if (isActivityCompleted) {
            final long activityStartTime = ((Date) activityUtils.getActivityJobAttributes(activityJobId).get(ShmConstants.ACTIVITY_START_DATE)).getTime();
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
        }
    }

    /**
     * This method validates the upgrade package to decide if verify activity can be started or not and sends back the activity result to Work Flow Service.
     * 
     * @param activityJobId
     * @return ActivityStepResult
     * 
     */
    @Override
    public ActivityStepResult precheck(final long activityJobId) {
        LOGGER.debug("Inside InstallService Precheck() with activityJobId {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        ActivityStepResult activityStepResult = new ActivityStepResult();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);

            activityStepResult = precheck(activityJobId, neJobStaticData, jobLogList);
        } catch (final Exception e) {
            activityStepResult.setActivityResultEnum(activityStepResultEnum);
            final String errorMsg = "An exception occured while processing precheck for Install activity with activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            String errorMessage = "";
            String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.INSTALL);
            errorMessage = exceptionMessage.isEmpty() ? e.getMessage() : exceptionMessage;
            jobLogMessage += String.format(JobLogConstants.FAILURE_REASON, errorMessage);
            activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        return activityStepResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#execute(long)
     */
    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
            localInstallationService.execute(PRODUCT_NUMBER, PRODUCT_REVISION, PACKAGE_TYPE, activityUtils.getActivityInfo(activityJobId, InstallService.class));
            final long activityStartTime = neJobStaticData.getActivityStartTime();
            // since InstallationService.execute will be called for Restore job also (that too in repeat), we will persist stepDuration here. Even if moAction was not triggered or failed. 
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
        } catch (final JobDataNotFoundException ex) {
            LOGGER.error("InstallService.execute- Unable to trigger action. Reason: ", ex.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            activityUtils.failActivity(activityJobId, jobLogList, null, ActivityConstants.INSTALL);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#handleTimeout(long)
     */
    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        return localInstallationService.handleTimeout(activityUtils.getActivityInfo(activityJobId, InstallService.class));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancel(long)
     */
    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        return localInstallationService.cancel(activityUtils.getActivityInfo(activityJobId, InstallService.class));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#cancelTimeout(long, boolean)
     */
    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean finalizeResult) {
        return localInstallationService.cancelTimeout(activityJobId, finalizeResult);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncPrecheck(long)
     */
    @Override
    @Asynchronous
    public void asyncPrecheck(final long activityJobId) {
        LOGGER.debug("Inside InstallService asyncPrecheck() with activityJobId {}", activityJobId);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        ActivityStepResult activityStepResult = new ActivityStepResult();
        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.INSTALL);
            if (!isUserAuthorized) {
                activityStepResult.setActivityResultEnum(activityStepResultEnum);
                activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.INSTALL, activityStepResult.getActivityResultEnum());
                return;
            }
            activityStepResult = precheck(activityJobId, neJobStaticData, jobLogList);
        } catch (final Exception e) {
            activityStepResult.setActivityResultEnum(activityStepResultEnum);
            final String errorMsg = "An exception occured while processing precheck for Install activity with activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            String errorMessage = "";
            String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, ActivityConstants.INSTALL);
            if (!(exceptionMessage.isEmpty())) {
                errorMessage = exceptionMessage;
            } else {
                errorMessage = e.getMessage();
            }
            jobLogMessage += String.format(JobLogConstants.FAILURE_REASON, errorMessage);
            activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        activityUtils.buildProcessVariablesForPrecheckAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.INSTALL, activityStepResult.getActivityResultEnum());
    }

    private ActivityStepResult precheck(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList) {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.ACTIVITY_INITIATED, ActivityConstants.INSTALL), JobLogLevel.INFO.toString()));
        jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PROCESSING_PRECHECK, ActivityConstants.INSTALL), JobLogLevel.INFO.toString()));
        final long neJobId = neJobStaticData.getNeJobId();
        activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, jobLogList, PRECHECK_START_PROGRESS_PERCENTAGE);
        activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        activityStepResult = localInstallationService.precheck(activityJobId, PRODUCT_NUMBER, PRODUCT_REVISION, activityUtils.getActivityInfo(activityJobId, InstallService.class).getJobType().name(),
                neJobStaticData);
        if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRE_CHECK_SUCCESS, ActivityConstants.INSTALL), JobLogLevel.INFO.toString()));
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
            activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PRECHECK);
        } else if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION) {
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRE_CHECK_ACTIVITY_SKIP, ActivityConstants.INSTALL), JobLogLevel.INFO.toString()));
            activityAndNEJobProgressPercentageCalculator.updateActivityJobProgressPercentage(activityJobId, null, null, PRECHECK_END_PROGRESS_PERCENTAGE);
            activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        } else if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION) {
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRE_CHECK_FAILURE, ActivityConstants.INSTALL, REASON_FOR_FAILURE), JobLogLevel.ERROR.toString()));
        }
        return activityStepResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.Activity#precheckHandleTimeout(long)
     */
    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        localInstallationService.precheckHandleTimeout(activityJobId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#asyncHandleTimeout(long)
     */
    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        NEJobStaticData neJobStaticData = null;
        final ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
        ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        long activityStartTime = 0L;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            activityStartTime = neJobStaticData.getActivityStartTime();
            activityStepResult = localInstallationService.handleTimeout(activityUtils.getActivityInfo(activityJobId, InstallService.class));
        } catch (final Exception e) {
            activityStepResult.setActivityResultEnum(activityStepResultEnum);
            final String errorMsg = "An exception occured in async handleTimeout for Install activity with activityJobId :" + activityJobId + ". Exception is: ";
            LOGGER.error(errorMsg, e);
            String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            exceptionMessage = exceptionMessage.isEmpty() ? e.getMessage() : exceptionMessage;
            activityUtils.handleExceptionForHandleTimeoutScenarios(activityJobId, ActivityConstants.INSTALL, exceptionMessage);
        }
        if (activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_FAIL || activityStepResult.getActivityResultEnum() == ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS) {
            activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.HANDLE_TIMEOUT);
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        activityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(activityJobId, neJobStaticData, ActivityConstants.INSTALL, activityStepResult.getActivityResultEnum());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.AsynchronousActivity#timeoutForAsyncHandleTimeout(long)
     */
    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        LOGGER.info("Entering into InstallService.timeoutForAsyncHandleTimeout for the activityJobId: {}", activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, ActivityConstants.INSTALL);
    }

}
