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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.eventbus.EventConfigurationBuilder;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.Activity;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.AsynchronousActivity;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.axe.api.AxeNotificationProcessor;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.axe.upgrade.cache.AxeNodeTopologyDataNotFoundException;
import com.ericsson.oss.services.shm.es.impl.axe.upgrade.cache.NeJobsStaticDataPerNeTypeProviderImpl;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.axe.cache.AxeNeUpgradeCacheData;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.event.axe.AXENodeUpgradeRequest;
import com.ericsson.oss.services.shm.model.event.axe.AXEUpgradeRequestType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

/**
 * Generic implementation for each activity of AXE nodes 1. To prepare the required parameters and place the request on the OPS queue to execute the script of particular activity. 2. To process the
 * response received from OPS.
 * 
 * @author xnagvar
 */
@EServiceQualifier("AXE.UPGRADE.axeActivity")
@Traceable
@Profiled
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@SuppressWarnings("PMD.TooManyFields")
public class AxeUpgradeActivitiesService implements Activity, AsynchronousActivity, AxeNotificationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AxeUpgradeActivitiesService.class);

    @Inject
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private AxeUpgradeServiceHelper axeUpgradeServiceHelper;

    @Inject
    @Modeled
    private EventSender<AXENodeUpgradeRequest> eventSender;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private NeJobsStaticDataPerNeTypeProviderImpl neJobsStaticDataPerNeTypeProvider;

    @Inject
    private AxeUpgradeNotificationHandler axeUpgradeNotificationHandler;

    @Inject
    private AxeUpgradeTimeOutNotificationHandler axeUpgradeTimeOutNotificationHandler;

    @Inject
    private AxeUpgradeCancelNotificationHandler axeUpgradeCancelNotificationHandler;

    @Override
    @Asynchronous
    public void asyncHandleTimeout(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final Map<Object, Object> restrictions = getRestrictions(activityJobId);
        String activityName = null;
        try {
            final List<Map<String, Object>> activityDetails = jobConfigurationServiceProxy.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, restrictions,
                    Arrays.asList(ShmConstants.ACTIVITY_NAME));
            activityName = (String) activityDetails.get(0).get(ShmConstants.ACTIVITY_NAME);
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final Integer activityTimeout = activityTimeoutsService.getActivityTimeoutAsInteger("", PlatformTypeEnum.AXE.name(), JobTypeEnum.UPGRADE.name(), activityName);
            final String formattedLog = String.format(String.format(JobLogConstants.AXE_HANDLE_TIMEOUT, activityTimeout, activityName));
            activityUtils.addJobLog(formattedLog, JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, AxeUpgradeActivityConstants.IS_AXE_HANDLETIMEOUT_TRIGGERED, "true");
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
            final AXENodeUpgradeRequest axeNodeUpgradeRequest = prepareOnDemandRequest(activityJobId);
            final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
            eventConfigurationBuilder.priority(9);
            eventSender.send(axeNodeUpgradeRequest, eventConfigurationBuilder.build());
            LOGGER.info("On demand request {} is placed for the activity  {}", axeNodeUpgradeRequest, activityName);
            setSystemRecordEventData(activityJobId, neJobStaticData, activityName, AXEUpgradeRequestType.UPGRADE_STATUS.toString());
        } catch (Exception e) {
            LOGGER.error("Error while placing the request on queue in handle timeout activity id: {} : activityName: {} with reason : {}", activityJobId, activityName, e);
        }
    }

    @Override
    public void asyncPrecheck(final long activityJobId) {
        //Only asyncHandleTimeout is needed ,asyncPrecheck implementation is not needed
    }

    @Override
    public ActivityStepResult cancel(final long activityJobId) {
        LOGGER.info("Inside AXE activity cancel() with activityJobId:{}", activityJobId);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        try {
            final Map<Object, Object> restrictions = getRestrictions(activityJobId);
            final List<Map<String, Object>> activityDetails = jobConfigurationServiceProxy.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, restrictions,
                    Arrays.asList(ShmConstants.ACTIVITY_NAME));
            final String activityName = (String) activityDetails.get(0).get(ShmConstants.ACTIVITY_NAME);
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final long neJobId = neJobStaticData.getNeJobId();
            final long mainJobId = neJobStaticData.getMainJobId();
            final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
            final List<Map<String, Object>> jobLogList = new ArrayList<>();
            activityUtils.logCancelledByUser(jobLogList, jobConfigurationServiceProxy.getMainJobAttributes(mainJobId), jobConfigurationServiceProxy.getNeJobAttributes(neJobId), activityName);

            final AXENodeUpgradeRequest axeNodeRequest = prepareCancelRequest(activityJobId, neJobStaticData.getNodeName());
            eventSender.send(axeNodeRequest);

            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_CANCEL_TRIGGERED, Boolean.TRUE.toString());
            jobUpdateService.readAndUpdateJobAttributesForCancel(activityJobId, jobPropertyList, jobLogList);
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_SUCESS);
        } catch (final Exception e) {
            activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
            LOGGER.error("Exception occured after cancel triggered for activityJobId {} Reason :{}", activityJobId, e);
        }

        return activityStepResult;
    }

    @Override
    public ActivityStepResult cancelTimeout(final long activityJobId, final boolean isCancelTimeoutExhausted) {
        LOGGER.info("cancelTimeout for activityId {} and isCancelTimeoutExhausted {} ", activityJobId, isCancelTimeoutExhausted);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
        activityUtils.addJobLog(String.format(JobLogConstants.STILL_EXECUTING, ActivityConstants.CANCEL_UPGRADE), JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        return activityStepResult;
    }

    @Override
    @Asynchronous
    public void execute(final long activityJobId) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        NEJobStaticData neJobStaticData = null;
        String activityName = null;
        try {
            activityName = getActivityName(activityJobId);
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            final String shmJobExecUser = getShmJobExecUser(neJobStaticData.getNeJobId(), jobStaticData);
            LOGGER.info("Entered into execute for activity : {} ", activityName);
            final String neType = getNeType(neJobStaticData);
            final Map<String, Map<String, AxeNeUpgradeCacheData>> neJobStaticDataPerNetype = neJobsStaticDataPerNeTypeProvider.getNeJobsPerNeTypeData(neJobStaticData.getMainJobId(), neType,
                    neJobStaticData.getNeJobId());
            final Map<String, Object> jobConfigurationDetails = activityUtils.getJobConfigurationDetails(activityJobId);
            final Map<String, String> scriptParameters = axeUpgradeServiceHelper.getJobScriptParameters(jobConfigurationDetails, neJobStaticData.getNodeName(), activityName, neType);
            final String softwarePackageName = getSoftwarePackageName(scriptParameters);
            final String softwarePackagePath = axeUpgradeServiceHelper.getSoftwarPackagePath(softwarePackageName);
            final String jobOwner = getJobOwner(jobStaticData, shmJobExecUser);
            final Map<String, String> additionalParameters = axeUpgradeServiceHelper.getAdditionalParameters(jobOwner, neJobStaticData, activityName, scriptParameters, softwarePackagePath,
                    neJobStaticDataPerNetype, neType);
            final AXENodeUpgradeRequest axeNodeUpgradeRequest = prepareAxeNodeUpgradeRequest(activityJobId, neJobStaticData, softwarePackagePath, scriptParameters, jobOwner, additionalParameters);
            eventSender.send(axeNodeUpgradeRequest);
            setSystemRecordEventData(activityJobId, neJobStaticData, activityName, AXEUpgradeRequestType.UPGRADE.toString());
            activityUtils.addJobLog(String.format(JobLogConstants.REQUEST_FOR_SCRIPT_EXECUTION_SUBMITTED, activityName, scriptParameters.get(AxeUpgradeActivityConstants.SCRIPT), jobOwner),
                    JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.IS_ACTIVITY_TRIGGERED, ActivityConstants.CHECK_TRUE);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);

        } catch (final AxeNodeTopologyDataNotFoundException ex) {
            LOGGER.error("An excption occured while preparing cache for activity : {}, with activityJobId : {}. Exception is: {}", activityName, activityJobId, ex);
            activityUtils.addJobLog(String.format(JobLogConstants.AXE_ACTIVITY_EXECUTION_REQUEST_FAILED, activityName), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
            activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), activityName);
        } catch (Exception e) {
            LOGGER.error("Error occurred while placing the request in OPS queue for activity : {} id : {} with reason  ", activityName, activityJobId, e);
            activityUtils.addJobLog(String.format(JobLogConstants.AXE_ACTIVITY_EXECUTION_REQUEST_FAILED, activityName), JobLogType.NE.toString(), jobLogList, JobLogLevel.INFO.toString());
            if (neJobStaticData != null) {
                activityUtils.failActivity(activityJobId, jobLogList, neJobStaticData.getNeJobBusinessKey(), activityName);
            }
        }

    }

    @Override
    public ActivityStepResult handleTimeout(final long activityJobId) {
        return null;
    }

    @Override
    public ActivityStepResult precheck(final long activityJobId) {

        NEJobStaticData neJobStaticData = null;
        JobStaticData jobStaticData = null;
        ActivityStepResultEnum activityStepResultEnum = ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        String activityName = null;
        try {
            LOGGER.info("Precheck started with activityJobId {} ", activityJobId);
            activityName = getActivityName(activityJobId);
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            jobStaticData = jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId());
            //TBAC Validation
            final boolean isUserAuthorized = activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, activityName);
            final String neType = getNeType(neJobStaticData);
            //preparing Cache
            final Map<String, Map<String, AxeNeUpgradeCacheData>> cacheData = neJobsStaticDataPerNeTypeProvider.getNeJobsPerNeTypeData(neJobStaticData.getMainJobId(), neType,
                    neJobStaticData.getNeJobId());
            final boolean isNodeDataExist = neJobsStaticDataPerNeTypeProvider.isNodeDataExistInCacheData(cacheData, neJobStaticData.getNodeName(), neType);
            if (!isNodeDataExist) {
                handleIfCacheDataNotFound(activityJobId, activityName);
            }
            if (isUserAuthorized && isNodeDataExist) {
                activityStepResultEnum = ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
                jobLogList.add(activityUtils.createNewLogEntry(String.format(JobLogConstants.PRE_CHECK_SUCCESS, activityName), JobLogLevel.INFO.toString()));
            }

        } catch (final AxeNodeTopologyDataNotFoundException ex) {
            LOGGER.error("An excption occured while preparing cache for activity : {}, with activityJobId : {}. Exception is: {}", activityName, activityJobId, ex);
            handleIfCacheDataNotFound(activityJobId, activityName);
        } catch (final Exception e) {
            final String errorMsg = "An exception occured during TBAC validation or Preparing AxeNeUpgradeCacheData for activity : " + activityName + " with activityJobId :" + activityJobId
                    + ". Exception is: ";
            LOGGER.error(errorMsg, e);
            final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(e);
            String errorMessage = "";
            String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED, activityName);
            if (!(exceptionMessage.isEmpty())) {
                errorMessage = exceptionMessage;
            } else {
                errorMessage = e.getMessage();
            }
            jobLogMessage += String.format(JobLogConstants.FAILURE_REASON, errorMessage);
            activityUtils.addJobLog(jobLogMessage, JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
        }
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(activityStepResultEnum);
        return activityStepResult;
    }

    @Override
    public void precheckHandleTimeout(final long activityJobId) {
        //Only asyncHandleTimeout is needed ,asyncPrecheck implementation is not needed
    }

    @Override
    public void timeoutForAsyncHandleTimeout(final long activityJobId) {
        final Map<Object, Object> restrictions = getRestrictions(activityJobId);
        final List<Map<String, Object>> activityDetails = jobConfigurationServiceProxy.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, restrictions,
                Arrays.asList(ShmConstants.ACTIVITY_NAME));
        final String activityName = (String) activityDetails.get(0).get(ShmConstants.ACTIVITY_NAME);
        LOGGER.info("Entering into timeoutForAsyncHandleTimeout for  activity: '{}' with the activityJobId: {}", activityName, activityJobId);
        activityUtils.failActivityForHandleTimeoutExpiry(activityJobId, activityName);
    }

    /**
     * @param restrictions
     * @return
     */
    private String getActivityName(final long activityJobId) {
        String activityName;
        final Map<Object, Object> restrictions = getRestrictions(activityJobId);
        final List<Map<String, Object>> activityDetails = jobConfigurationServiceProxy.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, restrictions,
                Arrays.asList(ShmConstants.ACTIVITY_NAME));
        activityName = (String) activityDetails.get(0).get(ShmConstants.ACTIVITY_NAME);
        return activityName;
    }

    /**
     * @param neJobStaticData
     * @return
     */
    private String getJobOwner(final JobStaticData jobStaticData, final String shmJobExecUser) {
        String jobOwner = null;
        if (shmJobExecUser == null || shmJobExecUser.isEmpty()) {
            jobOwner = jobStaticData.getOwner();
        } else {
            jobOwner = shmJobExecUser;
        }
        return jobOwner;
    }

    /**
     * @param neJobStaticData
     * @return
     * @throws MoNotFoundException
     */
    private String getNeType(final NEJobStaticData neJobStaticData) throws MoNotFoundException {
        String neType = null;
        if (neJobStaticData.getParentNodeName() != null) {
            neType = networkElementRetrivalBean.getNeType(neJobStaticData.getParentNodeName());
        } else {
            neType = networkElementRetrivalBean.getNeType(neJobStaticData.getNodeName());
        }
        return neType;
    }

    private Map<Object, Object> getRestrictions(final long poId) {
        final Map<Object, Object> restrictions = new HashMap<>();
        restrictions.put(ObjectField.PO_ID, poId);
        return restrictions;
    }

    /**
     * @return
     */
    private Map<String, String> getSessionIdAndClusterIdOfActivity(final long activityJobId) {
        final Map<String, Object> activityJobPoAttributes = jobConfigurationServiceProxy.getActivityJobAttributes(activityJobId);
        final Map<String, String> opsSessionAndClusterIdInfo = new HashMap();
        final List<Map<String, Object>> jobPropertyAttributes = (List<Map<String, Object>>) activityJobPoAttributes.get(ActivityConstants.JOB_PROPERTIES);
        for (final Map<String, Object> jobProperty : jobPropertyAttributes) {
            if (ActivityConstants.OPS_CLUSTER_ID.equals(jobProperty.get(ShmConstants.KEY))) {
                opsSessionAndClusterIdInfo.put(ActivityConstants.OPS_CLUSTER_ID, (String) jobProperty.get(ShmConstants.VALUE));
            }
            if (ActivityConstants.OPS_SESSION_ID.equals(jobProperty.get(ShmConstants.KEY))) {
                opsSessionAndClusterIdInfo.put(ActivityConstants.OPS_SESSION_ID, (String) jobProperty.get(ShmConstants.VALUE));
            }
        }
        return opsSessionAndClusterIdInfo;
    }

    /**
     * @param activityDetails
     * @param shmJobExecUser
     * @param jobStaticData
     * @return
     */
    private String getShmJobExecUser(final long neJobId, final JobStaticData jobStaticData) {
        String shmJobExecUser = null;
        if (ExecMode.MANUAL.getMode().equals(jobStaticData.getExecutionMode())) {
            final Map<Object, Object> restrictions = getRestrictions(neJobId);
            final List<Map<String, Object>> neDetails = jobConfigurationServiceProxy.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, restrictions,
                    Arrays.asList(ShmConstants.SHM_JOB_EXEC_USER));
            shmJobExecUser = (String) neDetails.get(0).get(ShmConstants.SHM_JOB_EXEC_USER);
        }
        return shmJobExecUser;
    }

    /**
     * @param activityJobId
     * @return
     */
    private String getSoftwarePackageName(final Map<String, String> scriptParameters) {
        final String softwarePackageName = scriptParameters.get(UpgradeActivityConstants.SWP_NAME);
        LOGGER.info("Software package used for job is : {} ", softwarePackageName);
        return softwarePackageName;
    }

    @Override
    public void processNotification(final Map<String, Object> opsResponseAttributes) {
        final long activityJobId = (long) opsResponseAttributes.get(AxeUpgradeActivityConstants.ACTIVITY_ID);
        NEJobStaticData neJobStaticData = null;
        String activityName = null;
        LOGGER.info("activity ID {} , status {} , session id {} , cluster ID {}", (long) opsResponseAttributes.get("activityId"), opsResponseAttributes.get("status"),
                opsResponseAttributes.get(ActivityConstants.OPS_SESSION_ID), opsResponseAttributes.get(ActivityConstants.OPS_CLUSTER_ID));
        try {
            if (!jobConfigurationService.isJobResultEvaluated(activityJobId)) {
                final List<Map<String, Object>> activityDetails = getActivityNameAndOrder(activityJobId);
                activityName = (String) activityDetails.get(0).get(ShmConstants.ACTIVITY_NAME);
                final int activityOrder = (int) activityDetails.get(0).get(ShmConstants.ACTIVITY_ORDER);
                neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);

                if (activityUtils.cancelTriggered(activityJobId)) {
                    LOGGER.debug("Cancel is triggered  ");
                    axeUpgradeCancelNotificationHandler.processNotification(opsResponseAttributes, neJobStaticData, activityJobId, activityName, activityOrder);
                } else {
                    processActivityNotifications(opsResponseAttributes, neJobStaticData, activityJobId, activityName, activityOrder);
                }
            }

        } catch (JobDataNotFoundException e) {
            LOGGER.error("Job Data Not Found while processing axe upgrade job notification for activity id: {} : activityName: {} with reason : {}", activityJobId, activityName, e);
        } catch (Exception e) {
            LOGGER.error("Error while processing axe upgrade job notifications activity id: {} : activityName: {} with reason : {}", activityJobId, activityName, e);
        }
    }

    /**
     * @param activityJobId
     * @param neJobStaticData
     * @param softwarePackagePath
     * @param scriptParameters
     * @param jobOwner
     * @param additionalParameters
     */
    private AXENodeUpgradeRequest prepareAxeNodeUpgradeRequest(final long activityJobId, final NEJobStaticData neJobStaticData, final String softwarePackagePath,
            final Map<String, String> scriptParameters, final String jobOwner, final Map<String, String> additionalParameters) {
        final AXENodeUpgradeRequest axeNodeUpgradeRequest = new AXENodeUpgradeRequest();
        axeNodeUpgradeRequest.setActivityId(activityJobId);
        if (neJobStaticData.getNodeName().contains(AxeUpgradeActivityConstants.CLUSTER)) {
            axeNodeUpgradeRequest.setNodeName(neJobStaticData.getParentNodeName());
        } else {
            axeNodeUpgradeRequest.setNodeName(neJobStaticData.getNodeName());
        }
        axeNodeUpgradeRequest.setComponentName(null);
        axeNodeUpgradeRequest.setLogFilePath(softwarePackagePath + File.separator + AxeUpgradeActivityConstants.LOG);
        axeNodeUpgradeRequest.setScriptLocation(softwarePackagePath + File.separator + scriptParameters.get(AxeUpgradeActivityConstants.SCRIPT));
        axeNodeUpgradeRequest.setUserName(jobOwner);
        axeNodeUpgradeRequest.setRequestType(AXEUpgradeRequestType.UPGRADE.toString());
        axeNodeUpgradeRequest.setScriptParameters(scriptParameters);
        axeNodeUpgradeRequest.setAdditionalParameters(additionalParameters);
        LOGGER.info("axeNodeUpgradeRequest == activityJobId : {}  ,NodeName : {},  LogFilePath : {}, ScriptLocation : {}, UserName : {} , ScriptParameters : {} , additionalParameters : {}",
                axeNodeUpgradeRequest.getActivityId(), axeNodeUpgradeRequest.getNodeName(), axeNodeUpgradeRequest.getLogFilePath(), axeNodeUpgradeRequest.getScriptLocation(),
                axeNodeUpgradeRequest.getUserName(), axeNodeUpgradeRequest.getScriptParameters(), axeNodeUpgradeRequest.getAdditionalParameters());
        return axeNodeUpgradeRequest;
    }

    /**
     * @param activityJobId
     * @param nodeName
     * @return axeNodeRequest
     */
    private AXENodeUpgradeRequest prepareCancelRequest(final long activityJobId, final String nodeName) {
        final AXENodeUpgradeRequest axeNodeRequest = new AXENodeUpgradeRequest();
        axeNodeRequest.setRequestType(AXEUpgradeRequestType.CANCEL_UPGRADE.toString());
        final Map<String, String> sessionIdAndClusterId = getSessionIdAndClusterIdOfActivity(activityJobId);
        axeNodeRequest.setSessionId(sessionIdAndClusterId.get(ActivityConstants.OPS_SESSION_ID));
        axeNodeRequest.setClusterId(sessionIdAndClusterId.get(ActivityConstants.OPS_CLUSTER_ID));
        axeNodeRequest.setActivityId(activityJobId);
        axeNodeRequest.setNodeName(nodeName);
        LOGGER.info("sessionId {} And ClusterId {} and Request type {} ", axeNodeRequest.getSessionId(), axeNodeRequest.getClusterId(), axeNodeRequest.getRequestType());
        final Map<String, String> additionalParameters = new HashMap<>();
        additionalParameters.put(AxeUpgradeActivityConstants.SMO_SOFTSTOP, AxeUpgradeActivityConstants.STOP_MODE);
        axeNodeRequest.setAdditionalParameters(additionalParameters);
        return axeNodeRequest;
    }

    /**
     * @param activityJobId
     * @return
     */
    private AXENodeUpgradeRequest prepareOnDemandRequest(final long activityJobId) {
        final AXENodeUpgradeRequest axeNodeUpgradeRequest = new AXENodeUpgradeRequest();
        axeNodeUpgradeRequest.setRequestType(AXEUpgradeRequestType.UPGRADE_STATUS.toString());
        final Map<String, String> sessionIdAndClusterId = getSessionIdAndClusterIdOfActivity(activityJobId);
        axeNodeUpgradeRequest.setSessionId(sessionIdAndClusterId.get(ActivityConstants.OPS_SESSION_ID));
        axeNodeUpgradeRequest.setClusterId(sessionIdAndClusterId.get(ActivityConstants.OPS_CLUSTER_ID));
        axeNodeUpgradeRequest.setActivityId(activityJobId);
        return axeNodeUpgradeRequest;
    }

    /**
     * @param opsResponseAttributes
     * @param neJobStaticData
     * @param activityJobId
     * @param activityName
     * @throws MoNotFoundException
     */
    private void processActivityNotifications(final Map<String, Object> opsResponseAttributes, final NEJobStaticData neJobStaticData, final long activityJobId, final String activityName,
            final int activityOrder) throws MoNotFoundException {
        if (axeUpgradeServiceHelper.isAxeHandleTimeoutTriggered(activityJobId)) {
            axeUpgradeTimeOutNotificationHandler.processNotification(opsResponseAttributes, neJobStaticData, activityJobId, activityName, activityOrder);
        } else {
            axeUpgradeNotificationHandler.processNotification(opsResponseAttributes, neJobStaticData, activityJobId, activityName, activityOrder);
        }
    }

    /**
     * @param activityJobId
     * @param neJobStaticData
     * @param activityName
     */
    private void setSystemRecordEventData(final long activityJobId, final NEJobStaticData neJobStaticData, final String activityName, final String requestType) {
        final Map<String, Object> recordEventData = new HashMap<>();
        recordEventData.put("activityName", activityName);
        recordEventData.put("nodeName", neJobStaticData.getNodeName());
        recordEventData.put("platformType", neJobStaticData.getPlatformType());
        recordEventData.put("requestType", requestType);
        recordEventData.put("mainJobId", neJobStaticData.getMainJobId());
        recordEventData.put("activityJobId", activityJobId);
        systemRecorder.recordEventData(SHMEvents.AXE_ACTIVITY, recordEventData);
    }

    private List<Map<String, Object>> getActivityNameAndOrder(final long activityJobId) {
        final Map<Object, Object> restrictions = getRestrictions(activityJobId);
        return jobConfigurationServiceProxy.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.ACTIVITY_JOB, restrictions,
                Arrays.asList(ShmConstants.ACTIVITY_NAME, ShmConstants.ACTIVITY_ORDER));
    }

    public void handleIfCacheDataNotFound(final long activityJobId, final String activityName) {
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final StringBuilder jobLogMessasge = new StringBuilder();
        jobLogMessasge.append(String.format(JobLogConstants.ACTIVITY_FAILED, activityName)).append(String.format(JobLogConstants.FAILURE_REASON, JobLogConstants.AXE_TOPOLOGY_APG_INFO_NOT_FOUND));
        activityUtils.addJobLog(jobLogMessasge.toString(), JobLogType.SYSTEM.toString(), jobLogList, JobLogLevel.ERROR.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
    }

}
