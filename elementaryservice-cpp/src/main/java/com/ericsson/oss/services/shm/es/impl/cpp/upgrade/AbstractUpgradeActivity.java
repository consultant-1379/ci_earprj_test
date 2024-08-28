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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBTransactionRolledbackException;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.exception.MoActionAbortRetryException;
import com.ericsson.oss.services.shm.common.exception.MoActionRetryException;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.DpsWriterRetryProxy;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityCompleteTimer;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

/**
 * This class facilitates the common methods or implementations required for all activities of Upgrade Use Case.
 * 
 * @author tcsrohc
 * 
 */
@Profiled
@Traceable
@SuppressWarnings("PMD.TooManyFields")
public abstract class AbstractUpgradeActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUpgradeActivity.class);
    private static final int NUMBER_OF_DEFAULT_DPS_RETRIES = 5;
    @Inject
    protected JobUpdateService jobUpdateService;

    @Inject
    protected DpsWriterRetryProxy dpsWriterRetryProxy;

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected UpgradePackageService upgradePackageService;

    @Inject
    private RetryManager retryManager;

    @Inject
    protected JobConfigurationService jobConfigurationService;

    @Inject
    protected DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Inject
    protected SystemRecorder systemRecorder;

    @Inject
    protected ActivityCompleteTimer activityCompleteTimer;

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    protected NotificationRegistry notificationRegistry;

    @Inject
    protected JobPropertyUtils jobPropertyUtils;

    @Inject
    protected FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    protected ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private DpsRetryConfigurationParamProvider dpsConfigurationParamProvider;

    @Inject
    protected JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    protected ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    protected JobStaticDataProvider jobStaticDataProvider;

    private static final String[] UPMO_ACTIONRESULT = { UpgradePackageMoConstants.UP_ACTION_RESULT };

    protected void sendActivateToWFS(final long activityJobId, final String businessKey, final String activity) {
        try {
            workflowInstanceNotifier.sendActivate(businessKey, null);
        } catch (final WorkflowServiceInvocationException e) {
            LOGGER.error("Failed to notify WFS with activityJonId {} with exception", activityJobId, e);
            final List<Map<String, Object>> jobLogList = new ArrayList<>();
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.WORKFLOW_SERVICE_INVOCATION_FAILED, activity), JobLogLevel.ERROR.toString()));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
        }
    }

    protected void sendCancelMOActionDoneToWFS(final long activityJobId, final String businessKey, final String activity) {
        try {
            workflowInstanceNotifier.sendCancelMOActionDone(businessKey, null);
        } catch (final WorkflowServiceInvocationException e) {
            LOGGER.error("Failed to notify WFS with activityJonId {} with exception", activityJobId, e);
            final List<Map<String, Object>> jobLogList = new ArrayList<>();
            jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.WORKFLOW_SERVICE_INVOCATION_FAILED, activity), JobLogLevel.ERROR.toString()));
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
        }
    }

    /**
     * This method returns UP MO Details based on the attribute names provided. To handle the Optimistic lock issues, this Method uses Manual retries instead of Service framework retries.
     * 
     * @param activityJobId
     * @param attributeNames
     * @return
     */
    public Map<String, Object> getUpMoData(final long activityJobId, final String[] attributeNames) {
        final int configuredRetries = dpsConfigurationParamProvider.getdpsRetryCount();
        Map<String, Object> upMoData = new HashMap<>();
        final int noOfRetries = configuredRetries > NUMBER_OF_DEFAULT_DPS_RETRIES ? configuredRetries : NUMBER_OF_DEFAULT_DPS_RETRIES;
        for (int i = 1; i <= noOfRetries; i++) {
            try {
                upMoData = upgradePackageService.getUpMoData(activityJobId, attributeNames, null, null);
                break;
            } catch (final EJBTransactionRolledbackException ex) {
                if (i == noOfRetries) {
                    LOGGER.debug("Attributes{} reading failed because :", attributeNames, ex);
                } else {
                    LOGGER.debug("Attributes{} reading failed because :", attributeNames, ex.getMessage());
                    LOGGER.warn("Optimistic Lock issue occurred with retry attempt {}, waiting to retry", i);
                }
                try {
                    Thread.sleep(dpsConfigurationParamProvider.getDpsOptimisticLockWaitIntervalInMS());
                } catch (final InterruptedException e) {
                    LOGGER.error("Sleep interrupted due to :", ex);
                }
            }
        }
        return upMoData;

    }

    /**
     * @param upMoFdn
     * @return list of all action results stored on the Up Mo irrespective of actionId
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getActionResultList(final String upMoFdn) {

        List<Map<String, Object>> actionResultDataList = new ArrayList<>();

        final Map<String, Object> upMoAttr = getUpMoAttributesByFdn(upMoFdn, UPMO_ACTIONRESULT);

        if (upMoAttr.containsKey(UpgradePackageMoConstants.UP_ACTION_RESULT)) {
            actionResultDataList = (List<Map<String, Object>>) upMoAttr.get(UpgradePackageMoConstants.UP_ACTION_RESULT);
        }

        return actionResultDataList;

    }

    public Map<String, Object> getUpMoAttributesByFdn(final String upMoFdn, final String[] attributeNames) {
        return upgradePackageService.getUpMoAttributesByFdn(upMoFdn, attributeNames);
    }

    /**
     * This method will return UP MO Fdn from Ne Job Properties. If not found, it will query DPS and then return.
     * 
     * @param activityJobId
     * @return upMoFdn
     */
    public String getUpMoFdn(final long activityJobId, final long neJobId, final Map<String, Object> neJobAttributes) {
        return getUpMoFdn(activityJobId, null, null, neJobId, neJobAttributes);
    }

    /**
     * This method will return UP MO Fdn from Ne Job Properties. If not found, it will query DPS and then return.
     * 
     * @param activityJobId
     * @return upMoFdn
     */
    @SuppressWarnings("unchecked")
    public String getUpMoFdn(final long activityJobId, final String productNumber, final String productRevision, final long neJobId, final Map<String, Object> neJobAttributes) {
        String upgradePackageMoFdn = null;
        if (productNumber == null || productRevision == null) {
            // Get UP MO Fdn from Ne Job Properties
            final Map<String, Object> neJobAttributesMap = activityUtils.getNeJobAttributes(activityJobId);
            if (neJobAttributesMap.get(ActivityConstants.JOB_PROPERTIES) != null) {
                final List<Map<String, Object>> jobPropertiesList = (List<Map<String, Object>>) neJobAttributesMap.get(ActivityConstants.JOB_PROPERTIES);
                for (final Map<String, Object> jobProperty : jobPropertiesList) {
                    if (UpgradeActivityConstants.UP_FDN.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                        upgradePackageMoFdn = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                        LOGGER.debug("UpMoFdn for  activity {} is {}", activityJobId, upgradePackageMoFdn);
                    }
                }
            }
        }
        // Get UP MO Fdn from DPS
        if (upgradePackageMoFdn == null) {
            LOGGER.debug("Getting upMoFdn from DPS as in NE JobProps its found as null for activity {}", activityJobId);
            upgradePackageMoFdn = getUpMoFdnWithRetry(activityJobId, productNumber, productRevision);
            if (upgradePackageMoFdn != null) {
                LOGGER.debug("The upgradePackageMoFdn for activity {} is {}", activityJobId, upgradePackageMoFdn);
                persistNeJobProperty(activityJobId, upgradePackageMoFdn, neJobId, neJobAttributes);
            } else {
                LOGGER.warn("No MO found for activity {}", activityJobId);
            }
        }
        return upgradePackageMoFdn;
    }

    private String getUpMoFdnWithRetry(final long activityJobId, final String productNumber, final String productRevision) {

        return retryManager.executeCommand(dpsRetryPolicies.getReadAttributesRetryPolicy(), new ShmDpsRetriableCommand<String>() {
            @Override
            public String execute() {
                return upgradePackageService.getUpMoFdn(activityJobId, productNumber, productRevision);
            }
        });
    }

    /**
     * This method will create UP MO.
     * 
     * @param activityJobId
     * @return upMoFdn
     */
    public String createUpgradeMO(final long activityJobId) {
        final String computedValue = retryManager.executeCommand(dpsRetryPolicies.getDpsMoActionRetryPolicy(), new ShmDpsRetriableCommand<String>() {
            @Override
            public String execute() {
                return upgradePackageService.createUpgradeMO(activityJobId);
            }
        });
        return computedValue;

    }

    /**
     * This method will create UP MO for upgrade install activity
     * 
     * @param activityJobId
     * @return upMoFdn
     */
    public String createUpgradeMO(final long activityJobId, final String productNumber, final String productRevision) {
        final String computedValue = retryManager.executeCommand(dpsRetryPolicies.getDpsMoActionRetryPolicy(), new ShmDpsRetriableCommand<String>() {
            @Override
            public String execute() {
                try {
                    return upgradePackageService.createUpgradeMO(activityJobId, productNumber, productRevision);
                } catch (final Exception ex) {
                    processFailureReason(activityJobId, ex);
                }
                return null;
            }

            private void processFailureReason(final long activityJobId, final Exception ex) {
                final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
                if (exceptionMessage != null) {
                    if (exceptionMessage.contains(ShmJobConstants.ACTION_NOT_ALLOWED_EXCEPTION) || exceptionMessage.contains(ShmJobConstants.UPGRADE_CONTROL_FILE_EXCEPTION)
                            || exceptionMessage.contains(ShmJobConstants.UPGRADE_NOT_POSSIBLE_EXCEPTION) || exceptionMessage.contains(ShmJobConstants.GET_FILE_EXCEPTION)) {
                        throw new MoActionAbortRetryException(exceptionMessage);
                    } else {
                        throw new MoActionRetryException(exceptionMessage, ex);
                    }
                } else {
                    final String failureMessage = String.format(JobLogConstants.UPGRADE_PACKAGE_CREATION_FAILURE, activityJobId);
                    throw new MoActionRetryException(failureMessage, ex);
                }
            }
        });
        return computedValue;
    }

    /**
     * This method will return the state of UP MO.
     * 
     * @param upgradePackageMO
     * @return UpgradePackageState
     */
    public UpgradePackageState getUPState(final Map<String, Object> upgradePackageMo) {
        final String upStateValue = (String) upgradePackageMo.get(UpgradePackageMoConstants.UP_MO_STATE);
        final UpgradePackageState upState = UpgradePackageState.getState(upStateValue);
        return upState;
    }

    /**
     * This method will persist UP MO FDN in Ne Job.
     * 
     * @param activityJobId
     * @param upMoFdn
     */
    @SuppressWarnings("unchecked")
    protected void persistNeJobProperty(final long activityJobId, final String upMoFdn, final long neJobId, final Map<String, Object> neJobAttribtues) {

        List<Map<String, Object>> neJobPropertyList = new ArrayList<>();
        if (neJobAttribtues.get(ShmConstants.JOBPROPERTIES) != null) {
            LOGGER.debug("Going to update NE Job Properties for activity {}.", activityJobId);
            neJobPropertyList = (List<Map<String, Object>>) neJobAttribtues.get(ShmConstants.JOBPROPERTIES);
            for (final Map<String, Object> jobProperty : neJobPropertyList) {
                if (!upMoFdn.equals(jobProperty.get(ActivityConstants.JOB_PROP_VALUE)) && UpgradeActivityConstants.UP_FDN.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    jobProperty.put(ActivityConstants.JOB_PROP_VALUE, upMoFdn);
                    jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
                    LOGGER.debug("Job Property in NE Job properties: {} ", jobProperty);
                }
            }
        } else {
            LOGGER.debug("Going to create NE Job Properties for activity {}.", activityJobId);
            final Map<String, Object> jobProperty = new HashMap<>();
            jobProperty.put(ActivityConstants.JOB_PROP_VALUE, upMoFdn);
            jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UP_FDN);
            neJobPropertyList.add(jobProperty);
        }
        LOGGER.debug("Updating NE Job properties in Job {} : {}", activityJobId, neJobPropertyList);
        jobUpdateService.updateRunningJobAttributes(neJobId, neJobPropertyList, null);

    }

    /**
     * mainActionResult from the list of actionResults available on the node with the same actionId
     * 
     * @param jobEnvironment
     * @param actionResultDataList
     * @return mainActionResult
     */
    public Map<String, Object> getMainActionResult(final NEJobStaticData neJobStaticData, final List<Map<String, Object>> actionResultDataList, final boolean isCancel, final long activityJobId) {
        String actionResultAdditionalInfo = null;
        final Map<String, Object> mainActionResult = new HashMap<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final Map<String, Object> activityJobAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
        activityUtils.addJobLog(JobLogConstants.FETCH_ACTION_RESULT, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.INFO.toString());
        int persistedActionId = -1;
        if (isCancel) {
            persistedActionId = activityUtils.getPersistedActionId(activityJobAttributes, neJobStaticData, activityJobId, ActivityConstants.CANCEL_ACTION_ID);
        } else {
            persistedActionId = activityUtils.getPersistedActionId(activityJobAttributes, neJobStaticData, activityJobId, ActivityConstants.ACTION_ID);
        }
        if (actionResultDataList == null) {
            return mainActionResult;
        }

        for (final Map<String, Object> actionResultData : actionResultDataList) {

            final int moActionId = getInvokedActionId(actionResultData);

            if (isActionIdPresentOnNode(moActionId, persistedActionId)) {
                String jobLogMessage = null;
                final String infoValue = (String) actionResultData.get(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO);
                final ActionResultInformation actionResultInfo = ActionResultInformation.getActionResultInfo(infoValue);
                LOGGER.trace("Action Result Info for activity {} is {}", activityJobId, actionResultInfo);
                actionResultAdditionalInfo = (String) actionResultData.get(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO);
                LOGGER.trace("Action Result additionalInfo for activityJobId {} is {}", activityJobId, actionResultAdditionalInfo);

                if (isMainActionResult(actionResultInfo)) {
                    mainActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, actionResultInfo);
                    mainActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, actionResultAdditionalInfo);
                    jobLogMessage = String.format(JobLogConstants.MAIN_ACTION_RESULT, actionResultInfo.getInfoMessage());
                    if (actionResultAdditionalInfo != null) {
                        jobLogMessage += String.format(JobLogConstants.ADDTIONAL_INFO, actionResultAdditionalInfo);
                    }

                    jobLogList.add(activityUtils.createNewLogEntry(jobLogMessage, new Date(), JobLogLevel.INFO.toString()));
                } else {
                    jobLogMessage = String.format(JobLogConstants.EXTRA_ACTION_RESULT, actionResultInfo.getInfoMessage());
                    if (actionResultAdditionalInfo != null) {
                        jobLogMessage += String.format(JobLogConstants.ADDTIONAL_INFO, actionResultAdditionalInfo);
                    }
                    jobLogList.add(activityUtils.createNewLogEntry(jobLogMessage, new Date(), JobLogLevel.INFO.toString()));
                }
            }
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList);
        return mainActionResult;

    }

    /**
     * Correlates the action Ids retrieved from node and persisted in dps.
     * 
     * @param moActionId
     * @param persistedActionId
     * @return
     */
    protected boolean isActionIdPresentOnNode(final int moActionId, final int persistedActionId) {
        return moActionId == persistedActionId && moActionId != -1 && persistedActionId != -1;

    }

    /**
     * Checks if the given ActionResultInformation is a main action result
     * 
     * @param actionReslutInfo
     * @return
     */
    private boolean isMainActionResult(final ActionResultInformation actionReslutInfo) {

        switch (actionReslutInfo) {
        case EXECUTED:
        case EXECUTED_WITH_WARNINGS:
        case EXECUTION_FAILED: {
            return true;
        }
        default:
            return false;
        }
    }

    /**
     * Retrieves actionId from ActionResultData on node
     * 
     * @param actionResultData
     * @return actionId value stored in actionResultData
     */
    protected int getInvokedActionId(final Map<String, Object> actionResultData) {
        final Object moActionIdObject = actionResultData.get(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID);
        int moActionId = -1;
        if (moActionIdObject != null) {
            final String actionIdString = moActionIdObject.toString();
            try {
                moActionId = Integer.parseInt(actionIdString);
            } catch (final NumberFormatException nfe) {
                LOGGER.error(nfe.getMessage(), nfe);
                moActionId = -1;
            }
        }
        return moActionId;
    }

    protected UpgradePackageInvokedAction getInvokedAction(final Map<String, Object> actionResultData) {
        final String typeOfInvokedAction = (String) actionResultData.get(UpgradePackageMoConstants.UP_ACTION_RESULT_TYPE_OF_INVOKED_ACTION);
        return UpgradePackageInvokedAction.getInvokedAction(typeOfInvokedAction);
    }

    /**
     * This method will fetch progressCount from the Map of Notifiable Attributes.
     * 
     * @param modifiedAttr
     * @return
     */
    public long getCurrentProgressCount(final Map<String, AttributeChangeData> modifiedAttr) {
        final Map<String, Object> progressCountMap = activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_COUNT);
        final long progressCount = getLongValue(progressCountMap.get(ConfigurationVersionMoConstants.NOTIFIABLE_ATTRIBUTE));
        return progressCount;
    }

    /**
     * This method will fetch progressTotal from the Map of Notifiable Attributes.
     * 
     * @param modifiedAttr
     * @return
     */
    public long getCurrentProgressTotal(final Map<String, AttributeChangeData> modifiedAttr) {

        final Map<String, Object> progressTotalMap = activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_TOTAL);
        final long progressTotal = getLongValue(progressTotalMap.get(ConfigurationVersionMoConstants.NOTIFIABLE_ATTRIBUTE));
        return progressTotal;
    }

    /**
     * This is a generic method used for converting any object to long.
     * 
     * @param key
     * @return
     */
    private long getLongValue(final Object key) {
        long longValue;
        if (key == null) {
            longValue = -1;
        } else {
            try {
                longValue = Long.parseLong(key.toString());
            } catch (final NumberFormatException nfe) {
                longValue = -1;
            }
        }

        return longValue;
    }

    /**
     * This method will fetch progressHeader from the Map of Notifiable Attributes.
     * 
     * @param modifiedAttr
     * @return
     */
    public UpgradeProgressInformation getCurrentProgressHeader(final Map<String, AttributeChangeData> modifiedAttr) {
        final Map<String, Object> progressHeaderMap = activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_PROG_HEADER);
        if (progressHeaderMap.get(ConfigurationVersionMoConstants.NOTIFIABLE_ATTRIBUTE) == null) {
            return null;
        }
        final String progressHeaderValue = (String) progressHeaderMap.get(ConfigurationVersionMoConstants.NOTIFIABLE_ATTRIBUTE);
        final UpgradeProgressInformation progressHeader = UpgradeProgressInformation.getHeader(progressHeaderValue);
        return progressHeader;
    }

    /**
     * This method will fetch state from the Map of Notifiable Attributes.
     * 
     * @param modifiedAttr
     * @return
     */
    public UpgradePackageState getCurrentUpState(final Map<String, AttributeChangeData> modifiedAttr) {
        final Map<String, Object> currentUpStateMap = activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE);
        if (currentUpStateMap.get(ConfigurationVersionMoConstants.NOTIFIABLE_ATTRIBUTE) == null) {
            return null;
        }
        final String currentUpStateValue = (String) currentUpStateMap.get(ConfigurationVersionMoConstants.NOTIFIABLE_ATTRIBUTE);
        final UpgradePackageState currentUpState = UpgradePackageState.getState(currentUpStateValue);
        return currentUpState;
    }

    /**
     * This method will fetch previousState from the Map of Notifiable Attributes.
     * 
     * @param modifiedAttr
     * @return
     */
    public UpgradePackageState getPreviousUpState(final Map<String, AttributeChangeData> modifiedAttr) {
        final Map<String, Object> currentUpStateMap = activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_MO_STATE);
        if (currentUpStateMap.get(ConfigurationVersionMoConstants.PREVIOUS_NOTIFIABLE_ATTRIBUTE) == null) {
            return null;
        }
        final String previousCurrentUpStateValue = (String) currentUpStateMap.get(ConfigurationVersionMoConstants.PREVIOUS_NOTIFIABLE_ATTRIBUTE);
        final UpgradePackageState previousCurrentUpState = UpgradePackageState.getState(previousCurrentUpStateValue);
        return previousCurrentUpState;
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getActionResult(final Map<String, AttributeChangeData> modifiedAttr) {
        final Map<String, Object> actionResultNotifiableAttribute = activityUtils.getNotifiableAttribute(modifiedAttr, UpgradePackageMoConstants.UP_ACTION_RESULT);
        if (actionResultNotifiableAttribute.get(ConfigurationVersionMoConstants.NOTIFIABLE_ATTRIBUTE) == null) {
            return new ArrayList<>();
        }

        return (List<Map<String, Object>>) actionResultNotifiableAttribute.get(ConfigurationVersionMoConstants.NOTIFIABLE_ATTRIBUTE);

    }

    /**
     * This method reports current UpgradePackageState and UpgradeProgressInformation based on the notification received
     * 
     * @param jobLogList
     * @param progressHeader
     * @param currentUpState
     */
    protected void reportUPProgress(final List<Map<String, Object>> jobLogList, final UpgradeProgressInformation progressHeader, final UpgradePackageState currentUpState) {

        if (currentUpState != null) {
            activityUtils.addJobLog(String.format(JobLogConstants.UP_MO_STATE, currentUpState.toString()), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
        }

        if (progressHeader != null) {
            activityUtils.addJobLog(String.format(JobLogConstants.UP_MO_PROGRESS_HEADER, progressHeader.getProgressMessage()), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
        }
    }

    /**
     * Reports current job state to Work Flow Service by sending an activate message
     * 
     * @param jobEnvironment
     * @param jobResult
     * @param activityName
     */

    protected void reportJobStateToWFS(final NEJobStaticData neJobStaticData, final JobResult jobResult, final String activityName, final long activityJobId) {
        LOGGER.debug("JobResult is {} for activityJobId {}", jobResult.getJobResult(), activityJobId);

        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();

        final String activityStatusMessage = jobResult == JobResult.SUCCESS ? JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY : JobLogConstants.ACTIVITY_FAILED;
        final String logMessage = String.format(activityStatusMessage, activityName);
        final String logLevel = jobResult == JobResult.SUCCESS ? JobLogLevel.INFO.toString() : JobLogLevel.ERROR.toString();
        activityUtils.addJobLog(logMessage, JobLogType.SYSTEM.toString(), jobLogList, logLevel);
        activityUtils.addJobProperty(ActivityConstants.ACTIVITY_RESULT, jobResult.getJobResult(), jobPropertyList);

        final boolean isJobResultPropertyPersisted = jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList);
        if (isJobResultPropertyPersisted) {
            final String businessKey = neJobStaticData.getNeJobBusinessKey();
            LOGGER.info("Sending message to wfs activate activityJobId:{}, businessKey:{}, activity:{}", activityJobId, businessKey, activityName);
            activityUtils.sendNotificationToWFS(neJobStaticData, activityJobId, activityName, null);
        } else {
            LOGGER.error(
                    "ActivityJob attributes[jobProperties={},jobLogList={}] are not updated in Database for {} activity[activityjob poId ={}]. Skipped to notify WFS. It will wait untill till timeout occurs to finish the job.",
                    jobPropertyList, jobLogList, activityName, activityJobId);
        }
    }

    protected String appendMediationFailureReasonInJobLog(final String activityLogMessage, final String exceptionMessage) {
        String logEntry = null;
        if (!exceptionMessage.isEmpty()) {
            logEntry = activityLogMessage + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
        } else {
            logEntry = activityLogMessage;
        }
        return logEntry;
    }

}
