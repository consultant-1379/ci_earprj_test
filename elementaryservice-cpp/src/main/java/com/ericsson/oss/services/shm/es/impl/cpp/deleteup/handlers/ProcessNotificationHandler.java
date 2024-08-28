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
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup.handlers;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.EXECUTE_REPEAT_PROGRESS_PERCENTAGE;
import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.MOACTION_END_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.DeleteUpgradePackageService;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.DeleteUpgradePackageUtil;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.persistjobdata.PersistJobData;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RequestScoped
@Traceable
@SuppressWarnings("PMD.TooManyFields")
public class ProcessNotificationHandler {

    private static final String FAILED_TO_DELETE_THE_UPGRADE_PACKAGE_ID = "Failed to delete the UpgradePackage Id:";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessNotificationHandler.class);

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private DeleteUpgradePackageUtil deleteUpgradePackageUtil;

    @Inject
    private PersistJobData persistJobData;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private JobConfigurationServiceRetryProxy jobReaderProxy;

    @Inject
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    private String nodeName = null;
    private long neJobId = 0;
    private String neType = null;
    private String recordingEvent = null;

    private NEJobStaticData neJobStaticData = null;
    private NetworkElementData networkElement = null;

    private final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
    private final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
    private final Map<String, Object> processVariables = new HashMap<String, Object>();

    public void processNotification(final long activityJobId, final JobActivityInfo jobActivityInfo, final Notification notification) {
        LOGGER.debug("processNotification of deleteup Activity with activityJobId {}", activityJobId);
        final String activityName = notification.getNotificationSubject().getActivityName();
        JobResult jobResult = JobResult.FAILED;
        final String upMoFdn = notification.getNotificationSubject().getKey();
        final NotificationEventTypeEnum notificationEventTypeEnum = notification.getNotificationEventType();
        try {
            initializeVariables(activityJobId);
            if (upMoFdn != null) {
                final String upId = DeleteUpgradePackageUtil.getRdnId(upMoFdn);
                if (notificationEventTypeEnum.equals(NotificationEventTypeEnum.DELETE)) {
                    jobResult = JobResult.SUCCESS;
                    buildLogs(activityJobId, "Successfully deleted the UpgradePackage Id:" + upId, upMoFdn, nodeName);
                    final Map<String, Object> activityJobAttributes = jobReaderProxy.getActivityJobAttributes(activityJobId);
                    final String totalUPs = activityUtils.getActivityJobAttributeValue(activityJobAttributes, UpgradeActivityConstants.TOTAL_UPS);
                    final int totalUPsCount = Integer.parseInt(totalUPs);
                    final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
                    final Double currentProgressPercentage = activityAndNEJobProgressPercentageCalculator.calculateActivityProgressPercentage(jobEnvironment, totalUPsCount,
                            EXECUTE_REPEAT_PROGRESS_PERCENTAGE);
                    persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobStaticData.getNeJobId(), null, null, currentProgressPercentage);
                } else {
                    jobResult = JobResult.FAILED;
                    buildLogs(activityJobId, FAILED_TO_DELETE_THE_UPGRADE_PACKAGE_ID + upId, upMoFdn, nodeName);
                    activityUtils.prepareJobPropertyList(jobProperties, UpgradeActivityConstants.INTERMEDIATE_FAILURE, jobResult.toString());
                }
                activityUtils.unSubscribeToMoNotifications(upMoFdn, activityJobId, activityUtils.getActivityInfo(activityJobId, DeleteUpgradePackageService.class));
                persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, 0.0);

                configureRepeatExecute(activityJobId, jobResult, upMoFdn);

            } else {
                LOGGER.error("For the activityJobId {} there is no UP with the provided deatils on the node", activityJobId);
            }
        } catch (final Exception ex) {
            LOGGER.error("For the activityJobId Exception occured while Processing notification for the {} activity, Exception is : {}", activityJobId, activityName, ex);
            buildJobLogsAndPropertiesIfExceptionOccurs(activityJobId, upMoFdn, ex);
            persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, 0.0);
            configureRepeatExecute(activityJobId, jobResult, upMoFdn);
        }
    }

    private void configureRepeatExecute(final long activityJobId, final JobResult jobResult, final String upMoFdn) {
        final Map<String, Object> repeatRequiredAndActivityResult = deleteUpgradePackageUtil.evaluateRepeatRequiredAndActivityResult(activityJobId, jobResult, neJobStaticData, neType);
        final boolean repeatRequired = (boolean) repeatRequiredAndActivityResult.get(JobVariables.ACTIVITY_REPEAT_EXECUTE);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
            final JobResult evaluatedActivityResult = (JobResult) repeatRequiredAndActivityResult.get(ActivityConstants.ACTIVITY_RESULT);
            if (repeatRequired) {
                processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, repeatRequired);
            } else {
                activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, evaluatedActivityResult.toString());
                activityUtils.persistStepDurations(activityJobId, neJobStaticData.getActivityStartTime(), ActivityStepsEnum.PROCESS_NOTIFICATION);
            }
            if (evaluatedActivityResult == JobResult.SUCCESS) {
                buildLogs(activityJobId, String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, ActivityConstants.DELETE_UP_DISPLAY_NAME), upMoFdn, nodeName);
                persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, MOACTION_END_PROGRESS_PERCENTAGE);
            }
            persistJobData.persistPropertiesLogsAndProgress(activityJobId, neJobId, jobProperties, jobLogs, 0.0);
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, "execute", processVariables);
        } catch (final JobDataNotFoundException ex) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            activityUtils.failActivity(activityJobId, jobLogs, null, ShmConstants.DELETEUPGRADEPKG_ACTIVITY);
            LOGGER.error("ProcessNotificationHandler.configureRepeatExecute- {} ", ex.getMessage());
        }
    }

    private void buildLogs(final long activityJobId, final String logMessage, final String upMoFdn, final String nodeName) {
        activityUtils.recordEvent(recordingEvent, nodeName, upMoFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, logMessage));
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
    }

    private void buildJobLogsAndPropertiesIfExceptionOccurs(final long activityJobId, final String currentUpFdn, final Exception ex) {
        final String upId = DeleteUpgradePackageUtil.getRdnId(currentUpFdn);
        final String errorMessage = activityUtils.prepareErrorMessage(ex);
        final String logMessage = FAILED_TO_DELETE_THE_UPGRADE_PACKAGE_ID + upId + " " + String.format(JobLogConstants.FAILURE_REASON, errorMessage);
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        systemRecorder.recordCommand(recordingEvent, CommandPhase.FINISHED_WITH_ERROR, nodeName, currentUpFdn, logMessage);
        activityUtils.recordEvent(recordingEvent, nodeName, currentUpFdn, activityUtils.additionalInfoForEvent(activityJobId, nodeName, errorMessage));
    }

    private void initializeVariables(final long activityJobId) {
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY);
            nodeName = neJobStaticData.getNodeName();
            networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
            neJobId = neJobStaticData.getNeJobId();
            recordingEvent = SHMEvents.CPP_DELETEUPGRADEPACKAGE_EXECUTE;
        } catch (final MoNotFoundException moNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, moNotFoundException);
        } catch (JobDataNotFoundException jobDataNotFoundException) {
            activityUtils.prepareJobPropertyList(jobProperties, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            LOGGER.error("Initialization of Variables failed for node {} due to: ", nodeName, jobDataNotFoundException);
        }
    }

}
