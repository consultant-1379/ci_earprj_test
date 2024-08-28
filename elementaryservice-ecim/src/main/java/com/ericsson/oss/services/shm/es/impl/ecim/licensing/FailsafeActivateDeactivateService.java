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

package com.ericsson.oss.services.shm.es.impl.ecim.licensing;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.EcimLicensingInfo;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.EcimLmUtils;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

/**
 * Specific to ECIM License Job. This class facilitates the Failsafe Backup with Activate, which creates temporary backup & Deactivate which deletes the temporary backup.
 * 
 * @author xprapav
 * 
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class FailsafeActivateDeactivateService {

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private EcimLmUtils ecimLmUtils;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    private static final String FAILSAFE_ACTIVATE_SUCCESS = "true";
    private static final String FAILSAFE_DEACTIVATE_SUCCESS = "true";
    private static final String ACTIVITY_NAME = "install";
    public static final String ASYNC_ACTION_TRIGGERED = "Failsafe \"%s\" step is triggered (timeout = %s minutes).";
    public static final String ACTION_TRIGGER_FAILED = "Unable to trigger Failsafe \"%s\" step on the node.";
    public static final String FAILSAFE_STEP_FAILED = "Failsafe \"%s\" step has failed.";

    public static final String FAILSAFE_TIMEOUT = "Notifications not received for the \"%s\" step. Verifying directly.";

    private static final Logger LOGGER = LoggerFactory.getLogger(FailsafeActivateDeactivateService.class);

    /**
     * This method is the activity execute step called when pre-check stage is passed. It registers for notifications and initiates the MO action.
     * 
     * @param activityJobId
     * @param nodeName
     * @param actionName
     * @param ecimLicensingInfo
     * @param jobActivityInfo
     */

    public void triggerBrmFailsafeActivateDeActivate(final long activityJobId, final NEJobStaticData neJobStaticData, final String actionName, final JobActivityInfo jobActivityInfo) {

        final String nodeName = neJobStaticData.getNodeName();
        LOGGER.debug("Inside BrmFailsafeActivateDeActivate() with activityJobId {},nodeName {},actionName {}", activityJobId, nodeName, actionName);
        String logMessage = null;
        String brmFailsafeBackupMoFdn = null;
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        int activateActionId = -1;
        try {
            final ManagedObject brmFailsafeBackupMo = brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName);
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            if (brmFailsafeBackupMo != null) {
                brmFailsafeBackupMoFdn = brmFailsafeBackupMo.getFdn();
                LOGGER.debug("brmFailsafeBackupMoFdn in triggerActivate{}", brmFailsafeBackupMoFdn);
                activityUtils.subscribeToMoNotifications(brmFailsafeBackupMoFdn, activityJobId, jobActivityInfo);
                activityUtils.addJobProperty(EcimCommonConstants.LicenseMoConstants.LAST_ACTION_TRIGGERED, actionName, propertyList);
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, null, null);
                if (EcimCommonConstants.LicenseMoConstants.FAILSAFE_ACTIVATE.equalsIgnoreCase(actionName)) {
                    activateActionId = brmMoServiceRetryProxy.performBrmFailSafeActivate(nodeName, brmFailsafeBackupMoFdn);
                    final long activityStartTime = neJobStaticData.getActivityStartTime();
                    activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.EXECUTE);
                } else {
                    activateActionId = brmMoServiceRetryProxy.performBrmFailSafeDeActivate(nodeName, brmFailsafeBackupMoFdn);
                }
                final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
                LOGGER.info("FailSafeBackup {} is triggered on BrmFailSafeBackup MO {} ,activityJobId {} returned actionId is {}", actionName, brmFailsafeBackupMoFdn, activityJobId, activateActionId);
                activityUtils.recordEvent(EcimCommonConstants.LicenseMoConstants.FAILSAFE, nodeName, brmFailsafeBackupMoFdn, actionName + ": Triggered for" + activityJobId + ":" + activateActionId);
                final Integer activateActivityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger(networkElement.getNeType(), PlatformTypeEnum.ECIM.name(), JobTypeEnum.LICENSE.name(),
                        ACTIVITY_NAME);
                activityUtils.prepareJobLogAtrributesList(jobLogList, String.format(ASYNC_ACTION_TRIGGERED, actionName, activateActivityTimeout), new Date(), JobLogType.SYSTEM.toString(),
                        JobLogLevel.INFO.toString());
                jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);

            } else {
                LOGGER.error("brmFailsafeBackupMo : {} for nodeName {}", brmFailsafeBackupMo, nodeName);
                failActivityWhileTriggeringAction(neJobStaticData, brmFailsafeBackupMoFdn, logMessage, actionName, jobActivityInfo, activityJobId);
            }

        } catch (final MoNotFoundException ex) {
            LOGGER.error("MoNotFoundException occurred triggerActivate(),Reason : {}", ex);
            logMessage = ex.getMessage();
            failActivityWhileTriggeringAction(neJobStaticData, brmFailsafeBackupMoFdn, logMessage, actionName, jobActivityInfo, activityJobId);
        } catch (final UnsupportedFragmentException ex) {
            LOGGER.error("UnsupportedFragmentException occurred in triggerActivate() ,Reason : {}", ex);
            logMessage = String.format(JobLogConstants.FRAGMENT_NOT_SUPPORTED, nodeName);
            failActivityWhileTriggeringAction(neJobStaticData, brmFailsafeBackupMoFdn, logMessage, actionName, jobActivityInfo, activityJobId);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred in triggerActivate() ,Reason : {}", ex);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
            logMessage = String.format(JobLogConstants.ACTIVITY_FAILED, actionName);
            if (!exceptionMessage.isEmpty()) {
                logMessage += String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            }
            failActivityWhileTriggeringAction(neJobStaticData, brmFailsafeBackupMoFdn, logMessage, actionName, jobActivityInfo, activityJobId);
        }

        LOGGER.debug("BrmFailSafeBackup Action {} triggered for {} and actionId {}", actionName, nodeName, activateActionId);

    }

    /**
     * This method persists the activity details and notifies WFS in case of failure or any exception has occurred.
     * 
     * @param ecimLicensingInfo
     * @param moFdn
     * @param jobLogMessage
     * @param actionName
     * @param jobActivityInfo
     */

    private void failActivityWhileTriggeringAction(final NEJobStaticData neJobStaticData, final String moFdn, final String jobLogMessage, final String actionName,
            final JobActivityInfo jobActivityInfo, final long activityJobId) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> neJobAttributes = jobConfigurationServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId());
        final String businessKey = (String) neJobAttributes.get(ShmConstants.BUSINESS_KEY);
        LOGGER.debug("failActivityWhileTriggeringAction {} for nodename {} - businessKey {} activityJobId {}", neJobStaticData.getNodeName(), actionName, businessKey, activityJobId);
        activityUtils.unSubscribeToMoNotifications(moFdn, activityJobId, jobActivityInfo);

        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);

        systemRecorder.recordCommand(EcimCommonConstants.LicenseMoConstants.FAILSAFE, CommandPhase.FINISHED_WITH_ERROR, neJobStaticData.getNodeName(), moFdn,
                activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE));

        activityUtils.failActivity(activityJobId, jobLogList, businessKey, ACTIVITY_NAME);

    }

    /**
     * This method validates the ActionProgressReport.
     * 
     * @param progressReport
     */

    public boolean validateActionProgressReport(final AsyncActionProgress progressReport) {
        boolean progressReportFlag = false;
        if (progressReport == null) {
            progressReportFlag = true;
        } else if (!(isActivateActionTriggered(progressReport) || isDeActivateActionTriggered(progressReport))) {
            progressReportFlag = true;
        }
        return progressReportFlag;
    }

    /**
     * This method checks the state of job, verifies result and sends notification to WFS.
     * 
     * @param jobLogList
     * @param jobPropertyList
     * @param nodeName
     * @param jobLogMessage
     * @param activityJobId
     * @param jobEnvironment
     * @param progressReport
     * @param brmBackupManagerMoFdn
     * @param state
     * @param result
     * @param mainJobId
     * @param notificationTime
     * @return
     */

    public void handleProgressReportState(final String nodeName, final EcimLicensingInfo ecimLicensingInfo, final long activityJobId, final NEJobStaticData neJobStaticData,
            final AsyncActionProgress progressReport, final String brmFailsafeBackupMoFdn, final JobActivityInfo jobActivityInfo) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        String jobLogMessage = null;
        final ActionStateType state = progressReport.getState();
        final ActionResultType result = progressReport.getResult();
        JobResult jobResult;
        LOGGER.info("Failsafe {} state: {}, result: {} for node {} in process notifications for activity jobId: {}", progressReport.getActionName(), state, result, nodeName, activityJobId);
        switch (state) {
        case RUNNING:
            jobLogMessage = progressReport.toString();
            activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            break;
        case FINISHED:
            JobLogLevel jobLogLevel;
            activityUtils.unSubscribeToMoNotifications(brmFailsafeBackupMoFdn, activityJobId, jobActivityInfo);
            final Map<String, Object> neJobAttributes = jobConfigurationServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId());
            final String businessKey = (String) neJobAttributes.get(ShmConstants.BUSINESS_KEY);
            if (ActionResultType.SUCCESS == progressReport.getResult()) {
                jobLogLevel = JobLogLevel.INFO;
                jobResult = JobResult.SUCCESS;
                activityUtils.prepareJobLogAtrributesList(jobLogList, progressReport.toString(), new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());
                jobLogMessage = String.format(JobLogConstants.STEP_COMPLETED_SUCCESSFULLY, mapActionNametoActivity(progressReport));
                if (isActivateActionTriggered(progressReport)) {
                    activityUtils.addJobProperty(EcimCommonConstants.LicenseMoConstants.FAILSAFE_ACTIVATE_TRIGGERED, FAILSAFE_ACTIVATE_SUCCESS, jobPropertyList);
                } else if (isDeActivateActionTriggered(progressReport)) {
                    activityUtils.addJobProperty(EcimCommonConstants.LicenseMoConstants.FAILSAFE_DEACTIVATE_TRIGGERED, FAILSAFE_DEACTIVATE_SUCCESS, jobPropertyList);
                }
            } else {
                jobLogLevel = JobLogLevel.ERROR;
                jobResult = JobResult.FAILED;
                activityUtils.prepareJobLogAtrributesList(jobLogList, progressReport.toString(), new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());
                jobLogMessage = String.format(FAILSAFE_STEP_FAILED, progressReport.getActionName()) + progressReport.getResultInfo();
                LOGGER.error("Failsafe Failed Reason {}", jobLogMessage);
            }
            activityUtils.recordEvent(EcimCommonConstants.LicenseMoConstants.FAILSAFE, nodeName, brmFailsafeBackupMoFdn,
                    "SHM:" + activityJobId + ":" + progressReport.getActionName() + ":" + jobResult.getJobResult() + " ResultInfo :" + progressReport.getResultInfo());
            activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());
            final Map<String, Object> processVariables = new HashMap<String, Object>();
            LOGGER.debug("LicenseInstall Status {}", ecimLicensingInfo.getInstallStatus());
            if (jobResult == JobResult.SUCCESS && (EcimCommonConstants.LicenseMoConstants.FAILSAFE_ACTIVATE.equalsIgnoreCase(progressReport.getActionName())
                    || EcimCommonConstants.LicenseMoConstants.FAILSAFE_CREATE.equalsIgnoreCase(progressReport.getActionName())
                    || EcimCommonConstants.LicenseMoConstants.FAILSAFE.equalsIgnoreCase(progressReport.getActionName()))) {
                LOGGER.debug("Activate-ACTIVITY_REPEAT_EXECUTE {}", jobResult.getJobResult());
                processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
            } else if (jobResult == JobResult.SUCCESS
                    && (EcimCommonConstants.LicenseMoConstants.FAILSAFE_DEACTIVATE.equalsIgnoreCase(progressReport.getActionName())
                            || EcimCommonConstants.LicenseMoConstants.FAILSAFE_DELETE.equalsIgnoreCase(progressReport.getActionName())
                            || EcimCommonConstants.LicenseMoConstants.FAILSAFEDEACTIVATE.equalsIgnoreCase(progressReport.getActionName()))
                    && JobResult.SUCCESS.toString().equalsIgnoreCase(ecimLicensingInfo.getInstallStatus())) {
                LOGGER.debug("Deactivate-ACTIVITY_RESULT {}", jobResult.getJobResult());

                final Map<String, Object> restrictionAttributes = ecimLmUtils.getRestrictionAttributes(ecimLicensingInfo);
                ecimLmUtils.deleteLicenseKeyFile(neJobStaticData, restrictionAttributes);
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult());
                final long activityStartTime = neJobStaticData.getActivityStartTime();
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
            } else {
                LOGGER.debug("Activity Result {} for action {} and License Install status {}  ", jobResult.getJobResult(), progressReport.getActionName(), ecimLicensingInfo.getInstallStatus());
                activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
                final long activityStartTime = neJobStaticData.getActivityStartTime();
                activityUtils.persistStepDurations(activityJobId, activityStartTime, ActivityStepsEnum.PROCESS_NOTIFICATION);
            }
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
            activityUtils.sendActivateToWFS(businessKey, processVariables);
            break;
        default:
            LOGGER.warn("Unsupported Action State Type {} for activityJobId {}", state, activityJobId);
        }

    }

    /**
     * This method handles timeout scenario for Activate & Deactivate steps and checks the actionResult on node to see if it is failed or success
     * 
     * @param ecimLicensingInfo
     * @param activityJobId
     * @param nodeName
     * @return
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     */
    public JobResult handleTimeoutForActivateDeactivateActivity(final EcimLicensingInfo ecimLicensingInfo, final long activityJobId, final String nodeName, final AsyncActionProgress progressReport,
            final JobActivityInfo jobActivityInfo) throws MoNotFoundException, UnsupportedFragmentException {
        JobResult jobResult = JobResult.FAILED;
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> propertyList = new ArrayList<Map<String, Object>>();
        JobLogLevel jobLogLevel = JobLogLevel.INFO;

        final ManagedObject brmFailsafeBackupMo = brmMoServiceRetryProxy.getBrmFailsafeBackupMo(nodeName);
        activityUtils.unSubscribeToMoNotifications(brmFailsafeBackupMo.getFdn(), activityJobId, jobActivityInfo);
        final String actionName = mapActionNametoActivity(progressReport);
        String jobLogMessage = String.format(FAILSAFE_TIMEOUT, actionName);
        activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        final Map<String, Object> actionResult = ecimLmUtils.getActionStatus(progressReport, actionName);
        jobLogLevel = (JobLogLevel) actionResult.get(ActivityConstants.JOB_LOG_LEVEL);
        jobLogMessage = (String) actionResult.get(ActivityConstants.JOB_LOG_MESSAGE);
        jobResult = (JobResult) actionResult.get(ActivityConstants.JOB_RESULT);
        if (EcimCommonConstants.LicenseMoConstants.FAILSAFE_ACTIVATE.equalsIgnoreCase(actionName)) {
            activityUtils.addJobProperty(EcimCommonConstants.LicenseMoConstants.FAILSAFE_ACTIVATE_TRIGGERED, FAILSAFE_ACTIVATE_SUCCESS, propertyList);
        } else if (EcimCommonConstants.LicenseMoConstants.FAILSAFE_DEACTIVATE.equalsIgnoreCase(actionName)) {
            activityUtils.addJobProperty(EcimCommonConstants.LicenseMoConstants.FAILSAFE_DEACTIVATE_TRIGGERED, FAILSAFE_DEACTIVATE_SUCCESS, propertyList);
        }
        LOGGER.info("Failsafe {}  result:  {} for node {} in HandleTimeout for activity jobId:{}", progressReport.getActionName(), jobResult.getJobResult().toString(), nodeName, activityJobId);
        activityUtils.recordEvent(EcimCommonConstants.LicenseMoConstants.FAILSAFE, nodeName, brmFailsafeBackupMo.getFdn(), actionName + " " + activityJobId + " : " + jobLogMessage);
        activityUtils.prepareJobLogAtrributesList(jobLogList, jobLogMessage, new Date(), JobLogType.SYSTEM.toString(), jobLogLevel.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, propertyList, jobLogList);
        return jobResult;

    }

    private String mapActionNametoActivity(final AsyncActionProgress progressReport) {
        String returnedActionName = "";
        if (isActivateActionTriggered(progressReport)) {
            returnedActionName = EcimCommonConstants.LicenseMoConstants.FAILSAFE_ACTIVATE_BACKUP;
        } else if (isDeActivateActionTriggered(progressReport)) {
            returnedActionName = EcimCommonConstants.LicenseMoConstants.FAILSAFE_DEACTIVATE_BACKUP;
        }
        return returnedActionName;
    }

    private boolean isActivateActionTriggered(final AsyncActionProgress progressReport) {
        boolean isActivateTriggered = false;
        if (EcimCommonConstants.LicenseMoConstants.FAILSAFE_ACTIVATE.equalsIgnoreCase(progressReport.getActionName())
                || EcimCommonConstants.LicenseMoConstants.FAILSAFE_CREATE.equalsIgnoreCase(progressReport.getActionName())
                || EcimCommonConstants.LicenseMoConstants.FAILSAFE.equalsIgnoreCase(progressReport.getActionName())) {
            isActivateTriggered = true;
        }
        return isActivateTriggered;
    }

    private boolean isDeActivateActionTriggered(final AsyncActionProgress progressReport) {
        boolean isDeActivateTriggered = false;
        if (EcimCommonConstants.LicenseMoConstants.FAILSAFE_DEACTIVATE.equalsIgnoreCase(progressReport.getActionName())
                || EcimCommonConstants.LicenseMoConstants.FAILSAFE_DELETE.equalsIgnoreCase(progressReport.getActionName())
                || EcimCommonConstants.LicenseMoConstants.FAILSAFEDEACTIVATE.equalsIgnoreCase(progressReport.getActionName())) {
            isDeActivateTriggered = true;
        }
        return isDeActivateTriggered;
    }

}
