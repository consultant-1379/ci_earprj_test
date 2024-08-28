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
package com.ericsson.oss.services.shm.es.impl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.common.modelservice.api.ProductData;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.RemoteActivityCallBack;
import com.ericsson.oss.services.shm.es.api.RemoteActivityInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.model.NotificationType;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

/**
 * This is a utility class used by all the elementary services for their common implementations.
 * 
 * @author tcsrohc
 * 
 */
@SuppressWarnings({ "PMD.ExcessivePublicCount", "ExcessiveClassLength" })
@ApplicationScoped
@Traceable
public class ActivityUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityUtils.class);

    private static final String COMMA_SEPARATOR_FOR_MO_FDN = ",";

    private static final String NO_CAPABILITY = null;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationRegistry notificationRegistry;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Inject
    private FdnServiceBean fdnServiceBean;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private OssModelInfoProvider ossModelInfoProvider;

    @Inject
    private DpsRetryConfigurationParamProvider dpsConfigurationParamProvider;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Inject
    private JobLogUtil jobLogUtil;

    private static final int NUMBER_OF_DEFAULT_DPS_RETRIES = 5;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    private static final DecimalFormat decimalFormatter = new DecimalFormat("#0.00");

    /**
     * This method fetches modified attributes from the DpsDataChangedEvent.
     * 
     * @param e
     * @return
     */
    public Map<String, AttributeChangeData> getModifiedAttributes(final DpsDataChangedEvent e) {
        final Map<String, AttributeChangeData> values = new HashMap<String, AttributeChangeData>();
        final DpsAttributeChangedEvent avcEvent = (DpsAttributeChangedEvent) e;
        for (final AttributeChangeData avc : avcEvent.getChangedAttributes()) {
            values.put(avc.getName(), avc);
        }
        return values;
    }

    /**
     * This method retrieves the activity job id from the notification subject.
     * 
     * @param subject
     * @return
     */
    public long getActivityJobId(final NotificationSubject subject) {
        if (subject instanceof FdnNotificationSubject) {
            final long activityJobId = Long.parseLong(((FdnNotificationSubject) subject).getObserverHandle().toString());
            return activityJobId;
        }
        return -1;
    }

    /**
     * This method retrieves activity job properties.
     * 
     * @param activityJobId
     * @return
     */
    public Map<String, Object> getActivityJobAttributes(final long activityJobId) {
        return getPoAttributes(activityJobId);
    }

    /**
     * This method retrieves Ne job properties.
     * 
     * @param activityJobId
     * @return
     */
    public Map<String, Object> getNeJobAttributes(final long activityJobId) {
        final Map<String, Object> activityJobAttr = getPoAttributes(activityJobId);
        final long neJobId = (long) activityJobAttr.get(ShmConstants.ACTIVITY_NE_JOB_ID);
        final Map<String, Object> neJobAttr = getPoAttributes(neJobId);
        return neJobAttr;

    }

    /**
     * This method retrieves main job properties.
     * 
     * @param activityJobId
     * @return
     */
    public Map<String, Object> getMainJobAttributes(final long activityJobId) {
        final Map<String, Object> activityJobAttr = getPoAttributes(activityJobId);
        final long neJobId = (long) activityJobAttr.get(ShmConstants.ACTIVITY_NE_JOB_ID);
        final Map<String, Object> neJobAttr = getPoAttributes(neJobId);
        final long mainJobId = (long) neJobAttr.get(ShmConstants.MAIN_JOB_ID);

        final Map<String, Object> mainJobAttr = getPoAttributes(mainJobId);

        return mainJobAttr;

    }

    /**
     * This method takes neJobId as input and retrieves main job attributes.
     * 
     * @param neJobId
     * @return
     */
    public Map<String, Object> getMainJobAttributesByNeJobId(final long neJobId) {
        final Map<String, Object> neJobAttr = getPoAttributes(neJobId);
        final long mainJobId = (long) neJobAttr.get(ShmConstants.MAIN_JOB_ID);

        final Map<String, Object> mainJobAttr = getPoAttributes(mainJobId);

        return mainJobAttr;

    }

    /**
     * Reads the JobConfiguration Details form the JobTemplate using activityJobId in the sequence of
     * 
     * ActivityJob --> NeJob --> MainJob --> JobTemplate --> JobConfigurationDetails
     * 
     * @param activityJobId
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getJobConfigurationDetails(final long activityJobId) {

        final Map<String, Object> activityJobAttributes = jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId);
        final long neJobId = (long) activityJobAttributes.get(ShmConstants.NE_JOB_ID);
        LOGGER.debug("NeJobId[{}] retrieved from ActivityJobId[{}]", neJobId, activityJobId);

        final Map<String, Object> neJobAttributes = jobConfigurationServiceRetryProxy.getNeJobAttributes(neJobId);
        final long mainJobId = (long) neJobAttributes.get(ShmConstants.MAIN_JOB_ID);
        LOGGER.debug("MainJobId[{}] retrieved from NeJobId[{}]", mainJobId, neJobId);

        final Map<String, Object> templateJobAttributes = jobConfigurationServiceRetryProxy.getMainJobAttributes(mainJobId);
        final long templateJobId = (long) templateJobAttributes.get(ShmConstants.JOBTEMPLATEID);
        LOGGER.debug("JobTemplateId[{}] retrieved from MainJobId[{}]", templateJobId, mainJobId);

        final Map<String, Object> jobTemplate = getPoAttributes(templateJobId);
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) jobTemplate.get(ShmConstants.JOBCONFIGURATIONDETAILS);
        LOGGER.debug("JobConfiguration Details from TemplateID[{}] retrieved and returning back.", templateJobId);
        return jobConfigurationDetails;
    }

    /**
     * This method retrieves node name for activity job.
     * 
     * @param activityJobId
     * @return
     */
    public String getNodeName(final long activityJobId) {
        String nodeName = null;
        final Map<String, Object> neAttributesMap = getNeJobAttributes(activityJobId);
        nodeName = (String) neAttributesMap.get(ShmConstants.NE_NAME);
        return nodeName;
    }

    /**
     * This method records events.
     *
     * @param jobExecutionUser
     * @param eventType
     * @param source
     * @param resource
     * @param additionalInformation
     */
    public void recordEvent(final String jobExecutionUser, final String eventType, final String source, final String resource, final String additionalInformation) {
        systemRecorder.recordEvent(jobExecutionUser, eventType, EventLevel.COARSE, source, resource, additionalInformation);
    }

    /**
     * This method records events.
     * 
     * @param eventType
     * @param source
     * @param resource
     * @param additionalInformation
     */
    public void recordEvent(final String eventType, final String source, final String resource, final String additionalInformation) {
        systemRecorder.recordEvent(eventType, EventLevel.COARSE, source, resource, additionalInformation);
    }

    /**
     * This method retrieves the required attribute from the map of notifiable attributes.
     * 
     * @param modifiedAttr
     * @param key
     * @return
     */
    public Map<String, Object> getNotifiableAttribute(final Map<String, AttributeChangeData> modifiedAttr, final String key) {

        Object notifiableAttributeValue = null;
        Object previousNotifiableAttributeValue = null;

        AttributeChangeData modifiedNotifiableAttribute = null;
        if (modifiedAttr.get(key) != null) {
            modifiedNotifiableAttribute = modifiedAttr.get(key);

            notifiableAttributeValue = modifiedNotifiableAttribute.getNewValue();

            previousNotifiableAttributeValue = modifiedNotifiableAttribute.getOldValue();
        }

        LOGGER.debug("ModifiedNotifiableAttribute:{}, NotifiableAttributeValue : {} ,PreviousNotifiableAttributeValue : {} for Key is {}", modifiedNotifiableAttribute, notifiableAttributeValue,
                previousNotifiableAttributeValue, key);

        final Map<String, Object> notifiableAttributeMap = new HashMap<String, Object>();
        notifiableAttributeMap.put(ShmConstants.NOTIFIABLE_ATTRIBUTE_VALUE, notifiableAttributeValue);
        notifiableAttributeMap.put("previousNotifiableAttributeValue", previousNotifiableAttributeValue);

        return notifiableAttributeMap;
    }

    /**
     * This method retrieves main job property list for activity job.
     * 
     * @param activityJobId
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMainJobPropertyList(final long activityJobId) {
        final Map<String, Object> mainAttributesMap = getMainJobAttributes(activityJobId);
        final Map<String, Object> jobConfiguration = (Map<String, Object>) mainAttributesMap.get(ShmConstants.JOBCONFIGURATIONDETAILS);
        LOGGER.debug("JobConfiguration for activity {} is {} ", activityJobId, jobConfiguration);
        final List<Map<String, Object>> jobPropertyList = (List<Map<String, Object>>) jobConfiguration.get(ShmConstants.JOBPROPERTIES);
        LOGGER.debug("MainJobPropertyList for activity {} is {} ", activityJobId, jobPropertyList);
        return jobPropertyList;
    }

    /**
     * This method will retrieve timestamp of the notification received.
     * 
     * @param subject
     * @return
     */
    public Date getNotificationTimeStamp(final NotificationSubject subject) {
        final Date notificationTime = subject.getTimeStamp();
        return notificationTime;
    }

    /**
     * This method will format the additional Information for Event Recoding.
     * 
     * @param activityJobId
     * @param nodeName
     * @param logMessage
     * @return
     */
    public String additionalInfoForEvent(final long activityJobId, final String nodeName, final String logMessage) {
        return String.format(SHMEvents.EVENT_ADDITIONAL_INFO, activityJobId, nodeName, logMessage);
    }

    /**
     * This method retrieves the Persistence Object attribute.
     * 
     * @param poId
     * @return poAttributes
     */
    public Map<String, Object> getPoAttributes(final long poId) {
        LOGGER.debug("Inside ActivityUtils getJobAttributes with poId {}", poId);

        final Map<String, Object> computedValue = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
            @Override
            public Map<String, Object> execute() {
                final Map<String, Object> poAttributes = jobUpdateService.retrieveJobWithRetry(poId);
                LOGGER.debug("PO Attributes: {}", poAttributes);
                return poAttributes;
            }
        });
        return computedValue;
    }

    /**
     * This method prepares the job log list to be persisted in Database.
     * 
     * This method has been Deprecated, use {@link JobLogUtil.prepareJobLogAtrributesList() method instead.
     * 
     * @param jobLogList
     * @param activityLogMessage
     * @param entryTime
     * @param logType
     * @return void
     */
    @Deprecated
    public void prepareJobLogAtrributesList(final List<Map<String, Object>> jobLogList, final String activityLogMessage, final Date entryTime, final String logType, final String logLevel) {
        final Map<String, Object> activityAttributes = new HashMap<String, Object>();
        activityAttributes.put(ActivityConstants.JOB_LOG_MESSAGE, activityLogMessage);
        LOGGER.debug("Log Message is {}", activityLogMessage);
        activityAttributes.put(ActivityConstants.JOB_LOG_ENTRY_TIME, entryTime);
        activityAttributes.put(ActivityConstants.JOB_LOG_TYPE, logType);
        activityAttributes.put(ActivityConstants.JOB_LOG_LEVEL, logLevel);
        jobLogList.add(activityAttributes);
        LOGGER.debug("jobLogList {}", jobLogList);
    }

    public String additionalInfoForCommand(final long activityJobId, final long mainJobId, final JobTypeEnum jobType) {
        return String.format(SHMEvents.MO_ACTION_ADDITIONAL_INFO, activityJobId, mainJobId, jobType);
    }

    /**
     * @param message
     * @param jobLogType
     * @param jobLogList
     * 
     */
    public void addJobLog(final String message, final String jobLogType, final List<Map<String, Object>> jobLogList, final String logLevel) {
        final Map<String, Object> jobLog = new HashMap<String, Object>();
        jobLog.put(ActivityConstants.JOB_LOG_MESSAGE, message);
        jobLog.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        jobLog.put(ActivityConstants.JOB_LOG_TYPE, jobLogType);
        jobLog.put(ActivityConstants.JOB_LOG_LEVEL, logLevel);
        jobLogList.add(jobLog);
    }

    public ActivityStepResult getActivityStepResult(final ActivityStepResultEnum stepResultEnum) {
        final ActivityStepResult stepResult = new ActivityStepResult();
        stepResult.setActivityResultEnum(stepResultEnum);
        return stepResult;

    }

    public void addJobProperty(final String keyName, final Object value, final List<Map<String, Object>> propertyList) {
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, keyName);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, value);
        propertyList.add(jobProperty);
    }

    /**
     * 
     * @param activityJobId
     * @return {@link JobEnvironment}
     */
    public JobEnvironment getJobEnvironment(final long activityJobId) {
        return new JobEnvironment(activityJobId, this);
    }

    /**
     * subscribe to notifications for the given MO fdn
     * 
     * @param moFdn
     * @param activityJobId
     * @return {@link FdnNotificationSubject}
     */
    public FdnNotificationSubject subscribeToMoNotifications(final String moFdn, final long activityJobId, final JobActivityInfo jobActivityInfo) {
        final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(moFdn, activityJobId, jobActivityInfo);
        notificationRegistry.register(fdnNotificationSubject);
        return fdnNotificationSubject;
    }

    /**
     * unSubscribe to notifications for the given MO fdn
     * 
     * @param moFdn
     * @param activityJobId
     * @param jobActivityInfo
     * @return boolean, true if it is removed
     */
    public boolean unSubscribeToMoNotifications(final String moFdn, final long activityJobId, final JobActivityInfo jobActivityInfo) {
        LOGGER.debug("Unsubscribing MO Notifications for FDN : {} and activityJobId : {}", moFdn, activityJobId);
        final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(moFdn, activityJobId, jobActivityInfo);
        return notificationRegistry.removeSubject(fdnNotificationSubject);
    }

    /**
     * This method retrieves activityJobProperty value from given activityJobAttributes
     * 
     * @param activityJobAttributes
     * @param attributeName
     * @return value of the specified attributeName. {@code null} if this map contains no mapping for the key
     */
    @SuppressWarnings("unchecked")
    public String getActivityJobAttributeValue(final Map<String, Object> activityJobAttributes, final String attributeName) {
        String attrValue = "";
        final List<Map<String, Object>> activityJobPropertyList = (List<Map<String, Object>>) activityJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        if (activityJobPropertyList != null) {
            for (final Map<String, Object> activityJobProperty : activityJobPropertyList) {
                if (attributeName != null && attributeName.equals(activityJobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    attrValue = (String) activityJobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                    LOGGER.debug("Attribute Name={}, Value= {}", attributeName, attrValue);
                    break;
                }
            }
        }
        return attrValue;
    }

    /**
     * Creates a New Log entry of SYSTEM type, with current date
     * 
     * @param logMessage
     * @return
     */
    public Map<String, Object> createNewLogEntry(final String logMessage, final String logLevel) {
        return createNewLogEntry(logMessage, new Date(), logLevel);
    }

    /**
     * Creates a New Log entry of SYSTEM type, with given entry date.
     * 
     * @param logMessage
     * @param notificationTime
     * @return
     */
    public Map<String, Object> createNewLogEntry(final String logMessage, final Date notificationTime, final String logLevel) {
        final Map<String, Object> logEntry = new HashMap<String, Object>();
        logEntry.put(ActivityConstants.JOB_LOG_MESSAGE, logMessage);
        logEntry.put(ActivityConstants.JOB_LOG_ENTRY_TIME, notificationTime);
        logEntry.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        logEntry.put(ActivityConstants.JOB_LOG_LEVEL, logLevel);
        return logEntry;
    }

    /**
     * 
     * @param activityJobAttributes
     * @param neJobStaticData
     * @param activityJobId
     * @param keyToBeSearch
     * @return InvokedActionID
     */
    public int getPersistedActionId(final Map<String, Object> activityJobAttributes, final NEJobStaticData neJobStaticData, final long activityJobId, final String keyToBeSearch) {
        final String invokedActionId = getActivityJobAttributeValue(activityJobAttributes, keyToBeSearch);
        try {
            return Integer.parseInt(invokedActionId);
        } catch (final NumberFormatException e) {
            LOGGER.error("ActionID does not exist in activityjob properties for NodeName={},activityJobId={}", neJobStaticData.getNodeName(), activityJobId);
            return -1;

        }
    }

    /**
     * 
     * @param jobEnv
     * @return InvokedActionID
     */
    @Deprecated
    public int getPersistedActionId(final JobEnvironment jobEnv) {
        final String invokedActionId = getActivityJobAttributeValue(jobEnv.getActivityJobAttributes(), ActivityConstants.ACTION_ID);
        try {
            return Integer.parseInt(invokedActionId);
        } catch (final NumberFormatException e) {
            LOGGER.error("ActionID does not exist in activityjob properties for NodeName={},activityJobId={}", jobEnv.getNodeName(), jobEnv.getActivityJobId());
            return -1;
        }
    }

    public int getPersistedActionId(final long activityJobId, final String nodeName, final Map<String, Object> activityJobAttributes) {
        final String invokedActionId = getActivityJobAttributeValue(activityJobAttributes, ActivityConstants.ACTION_ID);
        try {
            return Integer.parseInt(invokedActionId);
        } catch (final NumberFormatException e) {
            LOGGER.error("ActionID does not exist in activityjob properties for NodeName={},activityJobId={}", nodeName, activityJobId);
            return -1;
        }
    }

    public int getPersistedCancelActionId(final JobEnvironment jobEnv) {
        final String invokedActionId = getActivityJobAttributeValue(jobEnv.getActivityJobAttributes(), ActivityConstants.CANCEL_ACTION_ID);
        try {
            return Integer.parseInt(invokedActionId);
        } catch (final NumberFormatException e) {
            LOGGER.error("cancelActionId does not exist in activityjob properties for NodeName={},activityJobId={}", jobEnv.getNodeName(), jobEnv.getActivityJobId());
            return -1;
        }
    }

    public String getProductId(final String productNumber, final String productRevision) {
        String productId = null;
        if (productNumber != null && productRevision != null) {
            productId = productNumber + ShmConstants.DELIMITER_COLON + productRevision;
        }
        return productId;
    }

    public List<Map<String, Object>> prepareJobPropertyList(final List<Map<String, Object>> jobPropertyList, final String propertyName, final String propertyValue) {
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, propertyName);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, propertyValue);
        jobPropertyList.add(jobProperty);
        LOGGER.debug("jobPropertyList {}", jobPropertyList);
        return jobPropertyList;

    }

    public List<Map<String, Object>> prepareJobPropertyObjectList(final List<Map<String, Object>> jobPropertyList, final String propertyName, final Object propertyValue) {
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, propertyName);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, propertyValue);
        jobPropertyList.add(jobProperty);
        LOGGER.debug("jobPropertyList {}", jobPropertyList);
        return jobPropertyList;

    }

    /**
     * To notify workflow from Elementary Service, please use sendNotificationToWFS() with parameters JobEnvironment, long activityJobId, String activity and Map processVariables. This method will
     * check and notify the relevant workflow(MOACtion or Cancel SubProcess)
     * 
     */
    @Deprecated
    public boolean sendActivateToWFS(final String businessKey, final Map<String, Object> processVariables) {
        try {
            return workflowInstanceNotifier.sendActivate(businessKey, processVariables);
        } catch (final WorkflowServiceInvocationException e) {
            LOGGER.error(e.getMessage());
            return false;
        }
    }

    /**
     * To notify workflow from Elementary Service, please use sendNotificationToWFS() with parameters JobEnvironment, long activityJobId, String activity and Map processVariables. This method will
     * check and notify the relevant workflow(MOACtion or Cancel SubProcess)
     * 
     * Use this only in case, Elementary Service is sure that it has to notify the cancel Workflow. Another limitation with this method is if co-relation fails, there won't be any job log to convey
     * the same.(This limitation is also handled/covered in the suggested method to use.)
     * 
     * 
     */
    @Deprecated
    public void sendCancelMOActionDoneToWFS(final String businessKey) {
        try {
            workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, businessKey);
        } catch (final WorkflowServiceInvocationException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * This method retrieves ActivityInfo annotation's data from the class specified in the parameters and prepares JobActivityInfo
     * 
     * @param activityJobId
     * @param clazz
     * @return JobActivityInfo
     */
    public JobActivityInfo getActivityInfo(final long activityJobId, final Class<?> clazz) {
        final ActivityInfo activityInfoAnnotation = clazz.getAnnotation(ActivityInfo.class);
        final JobActivityInfo jobActivityInfo = new JobActivityInfo(activityJobId, activityInfoAnnotation.activityName(), activityInfoAnnotation.jobType(), activityInfoAnnotation.platform());
        return jobActivityInfo;
    }

    /**
     * This method retrieves ActivityInfo annotation's data from the class specified in the parameters and prepares JobActivityInfo
     * 
     * @param activityJobId
     * @param clazz
     * @return JobActivityInfo
     */
    public JobActivityInfo getRemoteActivityInfo(final long activityJobId, final Class<? extends RemoteActivityCallBack> clazz) {
        final RemoteActivityInfo activityInfoAnnotation = clazz.getAnnotation(RemoteActivityInfo.class);
        final JobActivityInfo jobActivityInfo = new JobActivityInfo(activityJobId, activityInfoAnnotation.activityName(), activityInfoAnnotation.jobType(), activityInfoAnnotation.platform());
        return jobActivityInfo;
    }

    public String getMoFdnFromNotificationSubject(final NotificationSubject subject) {
        if (subject instanceof FdnNotificationSubject) {
            return ((FdnNotificationSubject) subject).getFdn();
        }
        return null;
    }

    public String getNotifiedFDN(final Notification notification) {
        return notification.getDpsDataChangedEvent().getFdn();
    }

    public void failActivity(final long activityJobId, final List<Map<String, Object>> jobLogList, final String businessKey, final String activityName) {
        if (businessKey == null || businessKey.isEmpty()) {
            LOGGER.error("Unable to get NE Job business key. So cannot fail the activity {} having activityJobId : {}", activityName, activityJobId);
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
            return;
        }
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        // Persist Result as Failed in case of unable to trigger action.
        prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        final boolean wfsActivated = sendActivateToWFS(businessKey, new HashMap<String, Object>());
        if (!wfsActivated) {
            final String logMessage = String.format(JobLogConstants.WORKFLOW_SERVICE_INVOCATION_FAILED, activityName);
            systemRecorder.recordEvent(logMessage, EventLevel.COARSE, activityName, businessKey, "For ActivityJobId : " + activityJobId);

            LOGGER.error(logMessage);
        }
        jobLogList.clear();
    }

    @Deprecated
    public void skipActivity(final long activityJobId, final JobEnvironment jobEnvironment, final List<Map<String, Object>> jobLogList, final String businessKey, final String activityName) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        // Persist Result as Skipped in case of unable to trigger action.
        prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SKIPPED.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        sendNotificationToWFS(jobEnvironment, activityJobId, activityName, new HashMap<String, Object>());
    }

    public void skipActivity(final long activityJobId, final NEJobStaticData neJobStaticData, final List<Map<String, Object>> jobLogList, final String businessKey, final String activityName) {
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        // Persist Result as Skipped in case of unable to trigger action.
        prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SKIPPED.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        sendNotificationToWFS(neJobStaticData, activityJobId, activityName, new HashMap<String, Object>());
    }

    public String isTreatAs(final String nodeName) {
        return obtainTreatAsSupportInfo(nodeName, null, NO_CAPABILITY);
    }

    public String isTreatAs(final String nodeName, final FragmentType fragmentType) {
        return isTreatAs(nodeName, fragmentType, NO_CAPABILITY);
    }

    public String isTreatAs(final String nodeName, final FragmentType fragmentType, final String capability) {
        return obtainTreatAsSupportInfo(nodeName, fragmentType, capability);
    }

    private String obtainTreatAsSupportInfo(final String nodeName, final FragmentType fragmentType, final String capability) {
        String treatAsInfo = null;
        final List<NetworkElement> networkElement = fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList(nodeName), capability);
        if (networkElement != null && networkElement.size() > 0) {
            final NetworkElement ne = networkElement.get(0);
            final String ossModelIdentity = ne.getOssModelIdentity();
            final String nodeModelIdentity = ne.getNodeModelIdentity();
            final String neType = ne.getNeType();

            final List<ProductData> productInfos = ossModelInfoProvider.getProductInfo(ossModelIdentity, neType);
            final List<ProductData> listOfProductData = ne.getNeProductVersion();

            if (!ossModelIdentity.equals(nodeModelIdentity)) {
                OssModelInfo ossModelInfo = null;
                final Set<String> releaseVersion = ossModelInfoProvider.getReleaseVersion(ossModelIdentity, neType);

                if (fragmentType != null) {
                    ossModelInfo = ossModelInfoProvider.getOssModelInfo(neType, ossModelIdentity, fragmentType.getFragmentName());
                } else {
                    final List<OssModelInfo> ossModelInfos = ossModelInfoProvider.getOssModelInfo(neType, ossModelIdentity);
                    if (ossModelInfos != null && !ossModelInfos.isEmpty()) {
                        ossModelInfo = ossModelInfos.get(0);
                    }
                }
                final String mimVersion = ossModelInfo != null ? ossModelInfo.getVersion() : "";
                treatAsInfo = String.format(JobLogConstants.NODE_IS_IN_TREAT_AS_SUPPORT, mimVersion, releaseVersion, productInfos, listOfProductData);
            }
        }
        return treatAsInfo;
    }

    public NetworkElement getNetworkElement(final String nodeName) throws MoNotFoundException {
        return getNetworkElement(nodeName, null);
    }

    public NetworkElement getNetworkElement(final String nodeName, final String capability) throws MoNotFoundException {
        final List<String> neNames = new ArrayList<String>();
        neNames.add(nodeName);
        final List<NetworkElement> networkElements = fdnServiceBean.getNetworkElementsByNeNames(neNames, capability);
        if (networkElements.isEmpty()) {
            throw new MoNotFoundException("Network element is not found for the node name=" + nodeName);
        }
        return networkElements.get(0);
    }

    /**
     * This method is deprecated to remove dependency on JobEnvironment. Use NEJobStaticData in place of JobEnvironment
     * 
     * @param jobLogList
     * @param jobEnvironment
     * @param activityName
     */
    @Deprecated
    public void logCancelledByUser(final List<Map<String, Object>> jobLogList, final JobEnvironment jobEnvironment, final String activityName) {
        final String cancelledByUser = getCancelledByUser(jobEnvironment);
        prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_INVOKED, activityName, cancelledByUser), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.WARN.toString());
    }

    public void logCancelledByUser(final List<Map<String, Object>> jobLogList, final NEJobStaticData neJobStaticData, final String activityName) {
        final String cancelledByUser = getCancelledByUser(neJobStaticData);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_INVOKED, activityName, cancelledByUser), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.WARN.toString());
    }

    /**
     * This method is deprecated. To use this method make use of neJobStaticData in place of JobEnvironment
     */
    @Deprecated
    private String getCancelledByUser(final JobEnvironment jobEnvironment) {
        String cancelledByUser = fetchCancelledByUserFromNEJob(jobEnvironment.getNeJobAttributes());
        if (cancelledByNotFoundInNEJob(cancelledByUser)) {
            cancelledByUser = fetchCancelledByUserFromMainJob(jobEnvironment.getMainJobAttributes());
        }
        return cancelledByUser;
    }

    public void logCancelledByUser(final List<Map<String, Object>> jobLogList, final Map<String, Object> mainJobAttributes, final Map<String, Object> neJobAttributes, final String activityName) {
        final String cancelledByUser = getCancelledByUser(mainJobAttributes, neJobAttributes);
        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.CANCEL_INVOKED, activityName, cancelledByUser), new Date(), JobLogType.SYSTEM.toString(),
                JobLogLevel.WARN.toString());
    }

    private String getCancelledByUser(final Map<String, Object> mainJobAttributes, final Map<String, Object> neJobAttributes) {
        String cancelledByUser = fetchCancelledByUserFromNEJob(neJobAttributes);
        if (cancelledByNotFoundInNEJob(cancelledByUser)) {
            cancelledByUser = fetchCancelledByUserFromMainJob(mainJobAttributes);
        }
        return cancelledByUser;
    }

    private String getCancelledByUser(final NEJobStaticData neJobStaticData) {
        String cancelledByUser = fetchCancelledByUserFromNEJob(jobConfigurationServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId()));
        if (cancelledByNotFoundInNEJob(cancelledByUser)) {
            cancelledByUser = fetchCancelledByUserFromMainJob(jobConfigurationServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId()));
        }
        return cancelledByUser;
    }

    private String fetchCancelledByUserFromNEJob(final Map<String, Object> neJobAttributes) {
        return fetchCancelledByFromJobProperties(neJobAttributes);
    }

    private boolean cancelledByNotFoundInNEJob(final String cancelledBy) {
        return cancelledBy == null || cancelledBy.length() == 0;
    }

    private String fetchCancelledByUserFromMainJob(final Map<String, Object> mainJobAttributes) {
        return fetchCancelledByFromJobProperties(mainJobAttributes);
    }

    @SuppressWarnings("unchecked")
    private String fetchCancelledByFromJobProperties(final Map<String, Object> jobAttributes) {
        final List<Map<String, String>> jobPropertyList = (List<Map<String, String>>) jobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        if (jobPropertyList != null) {
            for (final Map<String, String> jobProperty : jobPropertyList) {
                if (ShmConstants.CANCELLEDBY.equals(jobProperty.get(ShmConstants.KEY))) {
                    return jobProperty.get(ShmConstants.VALUE);
                }
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public boolean cancelTriggered(final long activityJobId) {
        final Map<String, Object> activityJobAttributes = jobUpdateService.retrieveJobWithRetry(activityJobId);
        final List<Map<String, String>> activityJobProperties = (List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES);
        if (activityJobProperties != null) {
            for (final Map<String, String> activityJobProperty : activityJobProperties) {
                if (ActivityConstants.IS_CANCEL_TRIGGERED.equals(activityJobProperty.get(ShmConstants.KEY))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Utility method to retrieve parent FDN for the input child MO FDN.
     * 
     * @param childFdn
     * @return parentFdn
     */
    public String getParentFdn(final String childFdn) {
        String parentFdn = null;
        final int lastIndexOfCommaToDetermineParent = childFdn.lastIndexOf(COMMA_SEPARATOR_FOR_MO_FDN);
        if (lastIndexOfCommaToDetermineParent != -1) {
            parentFdn = childFdn.substring(0, lastIndexOfCommaToDetermineParent);
        }
        return parentFdn;
    }

    private void logCorrelationFailure(final long activityJobId, final String activity, final String nodeName, final WorkflowServiceInvocationException e) {
        LOGGER.error("{}", e.getMessage());
        systemRecorder.recordEvent(SHMEvents.WORKFLOW_SERVICE_CORRELATION, EventLevel.COARSE, activity, nodeName, "Failure Reason : " + e.getMessage());
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        jobLogList.add(createNewLogEntry(String.format(JobLogConstants.WORKFLOW_SERVICE_INVOCATION_FAILED, activity), JobLogLevel.ERROR.toString()));
        jobUpdateService.addOrUpdateOrRemoveJobProperties(activityJobId, null, jobLogList);
    }

    /**
     * @deprecated use {@link sendNotificationToWFS(NEJobStaticData, long, String, Map<String, Object>)} instead.
     * 
     * @param jobEnvironment
     * @param activityJobId
     * @param activity
     * @param processVariables
     */
    @Deprecated
    public void sendNotificationToWFS(final JobEnvironment jobEnvironment, final long activityJobId, final String activity, final Map<String, Object> processVariables) {
        final String businessKey = (String) jobEnvironment.getNeJobAttributes().get(ShmConstants.BUSINESS_KEY);
        try {
            if (cancelTriggered(activityJobId)) {
                LOGGER.info("Sending notification to cancelWorkflow with activityJobId {} businessKey {} ", activityJobId, businessKey);
                workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, businessKey);
            } else {
                LOGGER.info("Sending activate to wfs with activityJobId {} businessKey {} ", activityJobId, businessKey);
                workflowInstanceNotifier.sendActivate(businessKey, processVariables);
            }
        } catch (final WorkflowServiceInvocationException workflowServiceInvocationException) {
            logCorrelationFailure(activityJobId, activity, jobEnvironment.getNodeName(), workflowServiceInvocationException);
        }

    }

    public void sendNotificationToWFS(final NEJobStaticData neJobStaticData, final long activityJobId, final String activity, final Map<String, Object> processVariables) {
        if (neJobStaticData == null || neJobStaticData.getNeJobBusinessKey() == null || neJobStaticData.getNeJobBusinessKey().isEmpty()) {
            LOGGER.error("Unable to get NE Job business key. So, cannot notify WFS for {} activity having activityJobId : {}", activity, activityJobId);
            return;
        }
        final String businessKey = neJobStaticData.getNeJobBusinessKey();
        try {
            if (cancelTriggered(activityJobId)) {
                LOGGER.info("Sending notification to cancelWorkflow with activityJobId {} businessKey {} ", activityJobId, businessKey);
                workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, businessKey);
            } else {
                LOGGER.info("Sending activate to wfs with activityJobId {} businessKey {} ", activityJobId, businessKey);
                workflowInstanceNotifier.sendActivate(businessKey, processVariables);
            }
        } catch (final WorkflowServiceInvocationException workflowServiceInvocationException) {
            logCorrelationFailure(activityJobId, activity, neJobStaticData.getNodeName(), workflowServiceInvocationException);
        }
    }

    /**
     * This method retrieves the attribute values for the specified conditions. Also retries if DB is unavailable or when Optimistic lock issue occurs.
     * 
     * @param namespace
     * @param type
     * @param restrictions
     * @param reqdAttributes
     * @return
     */
    public List<Map<String, Object>> getProjectedAttributes(final String namespace, final String type, final Map<Object, Object> restrictions, final List<String> reqdAttributes) {
        final int noOfRetries = getNoOfRetries();
        List<Map<String, Object>> projetctedAttributes = new ArrayList<Map<String, Object>>();
        for (int i = 1; i <= noOfRetries; i++) {
            try {
                projetctedAttributes = jobConfigurationService.getProjectedAttributes(namespace, type, restrictions, reqdAttributes);
                if (!projetctedAttributes.isEmpty()) {
                    break;
                }
            } catch (final Exception ex) {
                LOGGER.error("Job projection Query failed because :{}", ex);
                sleep(ex);
            }
        }
        return projetctedAttributes;
    }

    private int getNoOfRetries() {
        final int configuredRetries = dpsConfigurationParamProvider.getdpsRetryCount();
        return configuredRetries > NUMBER_OF_DEFAULT_DPS_RETRIES ? configuredRetries : NUMBER_OF_DEFAULT_DPS_RETRIES;
    }

    /**
     * Method to wait the control to wait for defined time.
     * 
     * wait interval may vary depending on DPS unavailable , Optimistic Lock issue.
     * 
     */
    private void sleep(final Exception exception) {
        try {
            if (exception instanceof EJBException && dpsAvailabilityInfoProvider.isDatabaseDown()) {
                LOGGER.warn("Database is down, waiting to retry");
                Thread.sleep(dpsConfigurationParamProvider.getdpsWaitIntervalInMS());
            } else if (exception instanceof EJBTransactionRolledbackException) {
                LOGGER.warn("Optimistic Lock issue occurred, waiting to retry");
                Thread.sleep(dpsConfigurationParamProvider.getDpsOptimisticLockWaitIntervalInMS());
            }
        } catch (final InterruptedException ie) {
            LOGGER.error("Thread sleep failed due to :: {}", ie.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * This method is no longer exists. Use activityJobId and NeJobStaticData in place of JobEnvironment
     * 
     * @param jobEnvironment
     * @param activity
     * @param processVariables
     * @param message
     */
    @Deprecated
    private void sendPrecheckOrTimeoutMessageToWfs(final JobEnvironment jobEnvironment, final String activity, final Map<String, Object> processVariables, final String message) {
        final String businessKey = (String) jobEnvironment.getNeJobAttributes().get(ShmConstants.BUSINESS_KEY);
        final long activityJobId = jobEnvironment.getActivityJobId();
        try {
            LOGGER.info("Sending message {} to wfs with activityJobId {}, businessKey {}, processVariables {} ", message, activityJobId, businessKey, processVariables);
            workflowInstanceNotifier.sendPrecheckOrTimeoutMsgToWfsInstance(businessKey, processVariables, message);
        } catch (final WorkflowServiceInvocationException workflowServiceInvocationException) {
            logCorrelationFailure(activityJobId, activity, jobEnvironment.getNodeName(), workflowServiceInvocationException);
        }
    }

    public void sendPrecheckOrTimeoutMessageToWfs(final long activityJobId, final NEJobStaticData neJobStaticData, final String activity, final Map<String, Object> processVariables,
            final String message) {
        String nodeName = "";
        try {
            nodeName = neJobStaticData.getNodeName();
            final String businessKey = neJobStaticData.getNeJobBusinessKey();
            LOGGER.info("Sending message {} to wfs with activityJobId {}, businessKey {}, processVariables {} ", message, activityJobId, businessKey, processVariables);
            workflowInstanceNotifier.sendPrecheckOrTimeoutMsgToWfsInstance(businessKey, processVariables, message);
        } catch (final WorkflowServiceInvocationException workflowServiceInvocationException) {
            logCorrelationFailure(activityJobId, activity, nodeName, workflowServiceInvocationException);
        }
    }

    /**
     * This method is no longer exists. Use activityJobId and NeJobStaticData in place of JobEnvironment
     * 
     */
    @Deprecated
    public void buildProcessVariablesForPrecheckAndNotifyWfs(final JobEnvironment jobEnvironment, final String activity, final ActivityStepResultEnum activityStepResult) {
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        LOGGER.info("buildProcessVariableForPrecheck : activity result status {} ", activityStepResult);
        if (activityStepResult == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            processVariables.put(JobVariables.ACTIVITY_SKIP_EXECUTION, false);
            processVariables.put(JobVariables.ACTIVITY_PRECHECK_COMPLETED, true);
            sendPrecheckOrTimeoutMessageToWfs(jobEnvironment, activity, processVariables, JobVariables.ACTIVITY_PRECHECK_COMPLETED);
        } else if ((activityStepResult == ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION)) {
            processVariables.put(JobVariables.ACTIVITY_SKIP_EXECUTION, true);
            sendPrecheckOrTimeoutMessageToWfs(jobEnvironment, activity, processVariables, JobVariables.ACTIVITY_PRECHECK_COMPLETED);
        } else {
            processVariables.put(JobVariables.ACTIVITY_SKIP_EXECUTION, true);
            sendPrecheckOrTimeoutMessageToWfs(jobEnvironment, activity, processVariables, WorkFlowConstants.PRECHECK_FAILED_WFMESSAGE);
        }
    }

    public void buildProcessVariablesForPrecheckAndNotifyWfs(final long activityJobId, final NEJobStaticData neJobStaticData, final String activity, final ActivityStepResultEnum activityStepResult) {
        if (neJobStaticData == null || neJobStaticData.getNeJobBusinessKey() == null || neJobStaticData.getNeJobBusinessKey().isEmpty()) {
            LOGGER.error("Unable to get NE Job business key. So cannot notify to workflow for activity {} having activityJobId : {}", activity, activityJobId);
            return;
        }
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        LOGGER.info("buildProcessVariableForPrecheck : activity result status {} ", activityStepResult);
        if (activityStepResult == ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION) {
            processVariables.put(JobVariables.ACTIVITY_SKIP_EXECUTION, false);
            processVariables.put(JobVariables.ACTIVITY_PRECHECK_COMPLETED, true);
            sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activity, processVariables, JobVariables.ACTIVITY_PRECHECK_COMPLETED);
        } else if ((activityStepResult == ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION)) {
            processVariables.put(JobVariables.ACTIVITY_SKIP_EXECUTION, true);
            sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activity, processVariables, JobVariables.ACTIVITY_PRECHECK_COMPLETED);
        } else {
            processVariables.put(JobVariables.ACTIVITY_SKIP_EXECUTION, true);
            sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activity, processVariables, WorkFlowConstants.PRECHECK_FAILED_WFMESSAGE);
        }
    }

    /**
     * This method has been Deprecated, use {@link ActivityUtils.buildProcessVariablesForTimeoutAndNotifyWfs(long, NEJobStaticData, String, ActivityStepResultEnum) method instead. of this.
     * 
     * @param jobEnvironment
     * @param activity
     * @param activityStepResultEnum
     * 
     */
    @Deprecated
    public void buildProcessVariablesForTimeoutAndNotifyWfs(final JobEnvironment jobEnvironment, final String activity, final ActivityStepResultEnum activityStepResultEnum) {
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        LOGGER.info("buildProcessVariableForPrecheck : activity result status {} ", activityStepResultEnum);
        if (activityStepResultEnum == ActivityStepResultEnum.REPEAT_EXECUTE) {
            processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
        } else if (activityStepResultEnum == ActivityStepResultEnum.TIMEOUT_REPEAT_EXECUTE_MANUAL) {
            processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
            processVariables.put(JobVariables.ACTIVITY_EXECUTE_MANUALLY, true);
        }
        sendPrecheckOrTimeoutMessageToWfs(jobEnvironment, activity, processVariables, JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
    }

    public void buildProcessVariablesForTimeoutAndNotifyWfs(final long activityJobId, final NEJobStaticData neJobStaticData, final String activity,
            final ActivityStepResultEnum activityStepResultEnum) {
        if (neJobStaticData == null || neJobStaticData.getNeJobBusinessKey() == null || neJobStaticData.getNeJobBusinessKey().isEmpty()) {
            LOGGER.error("Unable to get NE Job business key. So cannot notify to workflow for activity {} having activityJobId : {}", activity, activityJobId);
            return;
        }
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        LOGGER.info("buildProcessVariableForPrecheck : activity result status {} ", activityStepResultEnum);
        if (activityStepResultEnum == ActivityStepResultEnum.REPEAT_EXECUTE) {
            processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
        } else if (activityStepResultEnum == ActivityStepResultEnum.TIMEOUT_REPEAT_EXECUTE_MANUAL) {
            processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
            processVariables.put(JobVariables.ACTIVITY_EXECUTE_MANUALLY, true);
        }
        sendPrecheckOrTimeoutMessageToWfs(activityJobId, neJobStaticData, activity, processVariables, JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
    }

    public void failActivityForPrecheckTimeoutExpiry(final long activityJobId, final String activityName) {
        final Integer precheckTimeout = activityTimeoutsService.getPrecheckTimeoutAsInteger();
        failActivityForPrecheckOrHandleTimeoutExpiry(activityJobId, String.format(JobLogConstants.PRECHECK_TIMEOUT, activityName, precheckTimeout));
    }

    public void failActivityForHandleTimeoutExpiry(final long activityJobId, final String activityName) {
        final Integer timeoutForHandleTimeout = activityTimeoutsService.getTimeoutForHandleTimeoutAsInteger();
        failActivityForPrecheckOrHandleTimeoutExpiry(activityJobId, String.format(JobLogConstants.TIMEOUT_FOR_HANDLE_TIMEOUT, activityName, timeoutForHandleTimeout));
    }

    private void failActivityForPrecheckOrHandleTimeoutExpiry(final long activityJobId, final String logMessage) {
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        jobLogList.add(createNewLogEntry(logMessage, JobLogLevel.ERROR.toString()));
        prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
        LOGGER.info("{} for the activityJobId {}", logMessage, activityJobId);
    }

    public String getActivityCompletionEvent(final PlatformTypeEnum platform, final JobTypeEnum jobType, final String activity) {
        return String.format(SHMEvents.ACTIVITY_COMPLETION, platform.name().toUpperCase(), jobType.name().toUpperCase(), activity.toUpperCase(), "COMPLETED");
    }

    /**
     * Calculate each activity step durations and persist it to {@link ShmConstants.STEP_DURATIONS} identifier.
     * 
     * @param activityJobId
     *            - Activity job ID for the current activity.
     * @param activityStartTime
     *            - Start time of the activity as long.
     * @param activityStepsEnum
     *            - {@link ActivityStepsEnum} identifier for the step from where it is called.
     */
    public void persistStepDurations(final long activityJobId, final long activityStartTime, final ActivityStepsEnum activityStepsEnum) {
        if (activityStartTime <= 0) {
            LOGGER.warn("Activity Start Time is {} which is probably wrong.", activityStartTime);
            return;
        }
        try {
            final double timeConsumptionInSeconds = (System.currentTimeMillis() - activityStartTime) / 1000.0;
            final String stepAndDuration = activityStepsEnum.getStep() + "=" + decimalFormatter.format(timeConsumptionInSeconds);
            final boolean isDataPersisted = jobUpdateService.readAndUpdateStepDurations(activityJobId, stepAndDuration, activityStepsEnum.getStep());
            if (!isDataPersisted) {
                LOGGER.warn("Persisting step duration failed for activity ID : {}, step : {} and the value failed to persist : {}", activityJobId, activityStepsEnum.getStep(), stepAndDuration);
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while persisting {} step duration for activityJobId : {}. Exception is : ", activityStepsEnum, activityJobId, ex);
        }
    }

    public String getCapabilityByJobType(final String jobType) {
        String capability = null;
        switch (jobType) {
        case "BACKUP":
            capability = SHMCapabilities.BACKUP_JOB_CAPABILITY;
            break;

        case "UPGRADE":
            capability = SHMCapabilities.UPGRADE_JOB_CAPABILITY;
            break;

        case "LICENSE":
            capability = SHMCapabilities.LICENSE_JOB_CAPABILITY;
            break;

        case "RESTORE":
            capability = SHMCapabilities.RESTORE_JOB_CAPABILITY;
            break;

        case "NODERESTART":
            capability = SHMCapabilities.NODE_RESTART_JOB_CAPABILITY;
            break;

        case "DELETEBACKUP":
            capability = SHMCapabilities.DELETE_BACKUP_JOB_CAPABILITY;
            break;

        case "BACKUP_HOUSEKEEPING":
            capability = SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY;
            break;

        case "ONBOARD":
            capability = SHMCapabilities.ONBOARD_JOB_CAPABILITY;
            break;

        case "DELETE_UPGRADEPACKAGE":
            capability = SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY;
            break;

        default:
            break;
        }
        return capability;
    }

    public void handleExceptionForHandleTimeoutScenarios(final long activityJobId, final String activityName, final String exceptionMessage) {
        final String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, activityName, exceptionMessage);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        jobLogList.add(createNewLogEntry(jobLogMessage, JobLogLevel.ERROR.toString()));
        prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, null);
    }

    public void handleExceptionForPrecheckScenarios(final long activityJobId, final String activityName, final String exceptionMessage) {
        final String jobLogMessage = String.format(JobLogConstants.ACTIVITY_FAILED_WITH_REASON, activityName, exceptionMessage);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        jobLogList.add(createNewLogEntry(jobLogMessage, JobLogLevel.ERROR.toString()));
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, null);
    }

    /**
     * This method reads and returns the attribute {@link ActivityConstants.IS_PRECHECK_DONE} from jobPropertyList. If the entry is not present, it will return false.
     * 
     * @param jobPropertyList
     * @return
     */
    public boolean isPrecheckDone(final List<Map<String, String>> jobPropertyList) {
        if (jobPropertyList != null) {
            for (final Map<String, String> eachJobProperty : jobPropertyList) {
                if (ActivityConstants.IS_PRECHECK_DONE.equals(eachJobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    return (Boolean.parseBoolean(eachJobProperty.get(ActivityConstants.JOB_PROP_VALUE)));
                }
            }
        }
        return false;
    }

    public boolean isRepeatRequiredOnPrecheck(final long activityJobId, final List<Map<String, Object>> jobPropertyList, final int maxRetryAttempts, final Map<String, Integer> attemptsMap,
            final Map<String, Object> activityJobAttributes) {
        boolean repeatRequiredOnPrecheck = true;
        int attemptsForRepeatPrecheck = 1;
        final String attemptsForRepeatPrecheckPersistedinDB = getActivityJobAttributeValue(activityJobAttributes, ActivityConstants.ATTEMPTS_FOR_REPEAT_PRECHECK);
        if (attemptsForRepeatPrecheckPersistedinDB == null || attemptsForRepeatPrecheckPersistedinDB.isEmpty()) {
            prepareJobPropertyList(jobPropertyList, ActivityConstants.ATTEMPTS_FOR_REPEAT_PRECHECK, Integer.toString(attemptsForRepeatPrecheck));
            attemptsMap.put(ActivityConstants.ATTEMPTS, attemptsForRepeatPrecheck);
        } else {
            attemptsForRepeatPrecheck = Integer.parseInt(attemptsForRepeatPrecheckPersistedinDB);
            if (attemptsForRepeatPrecheck < maxRetryAttempts) {
                prepareJobPropertyList(jobPropertyList, ActivityConstants.ATTEMPTS_FOR_REPEAT_PRECHECK, Integer.toString(attemptsForRepeatPrecheck + 1));
                attemptsMap.put(ActivityConstants.ATTEMPTS, attemptsForRepeatPrecheck + 1);
            } else {
                repeatRequiredOnPrecheck = false;
                attemptsMap.put(ActivityConstants.ATTEMPTS, 0);
            }
        }
        return repeatRequiredOnPrecheck;
    }

    public String prepareErrorMessage(final Exception ex) {
        final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
        String errorMessage = "";
        if (!(exceptionMessage.isEmpty())) {
            errorMessage = exceptionMessage;
        } else {
            errorMessage = ex.getMessage();
        }
        return errorMessage;
    }

    public String getJobExecutionUser(final long mainJobId) {
        String jobExecutionUser = null;
        try {
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(mainJobId);
            jobExecutionUser = jobStaticData.getJobExecutionUser();
        } catch (JobDataNotFoundException e) {
            LOGGER.error("Failed to get Job static data either from cache or from DPS for jobPoId", e);
        }
        return jobExecutionUser;
    }

}
