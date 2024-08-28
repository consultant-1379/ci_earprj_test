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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJBException;
import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy.RetryPolicyBuilder;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.WorkflowServiceInvocationException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.common.modelservice.api.ProductData;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
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
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RetryPolicy.class)
public class ActivityUtilsTest {

    @Mock
    @Inject
    DpsReader dpsReader;

    @Mock
    @Inject
    SystemRecorder systemRecorder;

    @Mock
    @Inject
    JobConfigurationService jobConfigurationService;

    @Mock
    @Inject
    JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Mock
    NotificationSubject notificationSubject;

    @Mock
    JobEnvironment jobEnv;

    @InjectMocks
    public ActivityUtils objectUnderTest;

    @Mock
    public DpsRetryPolicies dpsRetryPolicies;

    @Mock
    NotificationRegistry notificationRegistry;

    @Mock
    public JobUpdateService jobUpdateService;

    @Mock
    JobPropertyUtils jobPropertyUtils;

    @Mock
    JobActivityInfo jobActivityInfoMock;

    @Mock
    FdnServiceBean fdnServiceBean;

    @Mock
    NetworkElement networkElement;

    @Mock
    ProductData neProductVersion;

    @Mock
    ProductData neProductVersion1;

    @Mock
    WorkflowInstanceNotifier workflowInstanceNotifier;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    long activityJobId = 1;
    long neJobId = 2;
    long mainJobId = 3;
    public long poId = 4;

    @Mock
    Map<String, Object> mainJobAttr;

    @Mock
    Map<String, Object> neJobAttr;

    @Mock
    Map<String, Object> activityJobAttr;

    @Mock
    public Map<String, Object> poAttr;

    @Mock
    List<Map<String, Object>> jobPropertyList;
    List<Map<String, Object>> jobLogList;

    String swPkgName = "Some Sw Pkg Name";
    String neName = "ne name";
    String businessKey = "businessKey";

    @Mock
    RetryPolicyBuilder retryPolicyBuilderMock;

    @Mock
    DpsRetryConfigurationParamProvider dpsConfigMock;

    @Mock
    public RetryPolicy retryPolicyMock;

    @Mock
    OssModelInfoProvider ossModelInfoProvider;

    @Mock
    OssModelInfo ossModelInfo;

    @Mock
    public RetryManager retryManagerMock;

    @Mock
    private ActivityTimeoutsService activityTimeoutsService;

    private final Class<? extends Exception>[] exceptionsArray = new Class[] { IllegalStateException.class, EJBException.class };

    private static final String JOB_EXEC_USER = "xprapav";
    private static final String OWNER = "Administrator";

    @Test
    public void testGetModifiedAttributes() {
        final List<AttributeChangeData> attributeChangeData = new ArrayList<AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData("name", "oldValue", "newValue", "deltaRemoved", "deltaAdded");
        attributeChangeData.add(avc);
        final DpsDataChangedEvent dpsDataChangedEvent = new DpsAttributeChangedEvent("namespace", "type", "version", -1L, "fdn", "bucketName", attributeChangeData);

        final Map<String, AttributeChangeData> values = new HashMap<String, AttributeChangeData>();
        values.put(avc.getName(), avc);
        assertEquals(values, objectUnderTest.getModifiedAttributes(dpsDataChangedEvent));
    }

    @Test
    public void testGetActivityJobId() {
        final NotificationSubject subject = new FdnNotificationSubject("fdn", Long.toString(activityJobId));
        assertEquals(activityJobId, objectUnderTest.getActivityJobId(subject));
    }

    @Test
    public void testGetActivityJobIdWhenSubjectIsNotInstanceOfFdnNotificationSubject() {
        assertEquals(-1, objectUnderTest.getActivityJobId(notificationSubject));
    }

    @Test
    public void testGetActivityJobAttributes() {
        setRetryPolicies();
        setActivityJobPo();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(activityJobAttr);
        assertEquals(activityJobAttr, objectUnderTest.getActivityJobAttributes(activityJobId));
    }

    @Test
    public void testGetNeJobAttributes() {
        setRetryPolicies();
        setActivityJobPo();
        setNeJobPo();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(activityJobAttr, neJobAttr);
        assertEquals(neJobAttr, objectUnderTest.getNeJobAttributes(activityJobId));
    }

    @Test
    public void testGetMainJobAttributes() {
        setRetryPolicies();
        setActivityJobPo();
        setNeJobPo();
        setMainJobPo();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(activityJobAttr, neJobAttr, mainJobAttr);
        assertEquals(mainJobAttr, objectUnderTest.getMainJobAttributes(activityJobId));
    }

    @Test
    public void testGetNodeName() {
        setRetryPolicies();
        setActivityJobPo();
        setNeJobPo();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(activityJobAttr, neJobAttr);
    }

    @Test
    public void testRecordEvent() {
        Mockito.doNothing().when(systemRecorder).recordEvent("eventType", EventLevel.COARSE, "source", "resource", "additionalInformation");
        objectUnderTest.recordEvent("eventType", "source", "resource", "additionalInformation");
    }

    @Test
    public void testGetNotifiableAttribute() {
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData("key", "oldValue", "newValue", "deltaRemoved", "deltaAdded");
        modifiedAttr.put(avc.getName(), avc);

        final Map<String, Object> notifiableAttributeMap = new HashMap<String, Object>();
        notifiableAttributeMap.put("notifiableAttributeValue", "newValue");
        notifiableAttributeMap.put("previousNotifiableAttributeValue", "oldValue");
        assertEquals(notifiableAttributeMap, objectUnderTest.getNotifiableAttribute(modifiedAttr, "key"));
    }

    @Test
    public void testGetNotifiableAttributeWithNoData() {
        final Map<String, AttributeChangeData> modifiedAttr = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData avc = new AttributeChangeData("key", "oldValue", "newValue", "deltaRemoved", "deltaAdded");
        modifiedAttr.put(avc.getName(), avc);

        final Map<String, Object> notifiableAttributeMap = new HashMap<String, Object>();
        notifiableAttributeMap.put("notifiableAttributeValue", null);
        notifiableAttributeMap.put("previousNotifiableAttributeValue", null);
        assertEquals(notifiableAttributeMap, objectUnderTest.getNotifiableAttribute(modifiedAttr, "InvalidKey"));
    }

    //@Test
    public void testGetMainJobPropertyList() {
        setRetryPolicies();
        setActivityJobPo();
        setNeJobPo();
        setMainJobPo();
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(activityJobAttr, neJobAttr, mainJobAttr);
        assertEquals(jobPropertyList, objectUnderTest.getMainJobPropertyList(activityJobId));
    }

    @Test
    public void testGetNotificationTimeStamp() {
        final Date date = new Date();
        when(notificationSubject.getTimeStamp()).thenReturn(date);
        assertEquals(date, objectUnderTest.getNotificationTimeStamp(notificationSubject));
    }

    private void setActivityJobPo() {

        activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ShmConstants.ACTIVITY_NE_JOB_ID, neJobId);
    }

    private void setNeJobPo() {

        neJobAttr = new HashMap<String, Object>();
        neJobAttr.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neJobAttr.put(ShmConstants.NE_NAME, neName);
    }

    private void setMainJobPo() {

        mainJobAttr = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();
        jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.SWP_NAME);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, swPkgName);
        jobPropertyList.add(jobProperty);
        jobConfiguration.put(ShmConstants.NETYPEJOBPROPERTIES, jobPropertyList);
        mainJobAttr.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfiguration);
        when(jobUpdateService.retrieveJobWithRetry(mainJobId)).thenReturn(mainJobAttr);
    }

    private void setMainJobPoWithEmptyPropertyList() {

        mainJobAttr = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();
        jobPropertyList = new ArrayList<Map<String, Object>>();
        jobConfiguration.put(ShmConstants.JOBPROPERTIES, jobPropertyList);
        mainJobAttr.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfiguration);
        when(jobUpdateService.retrieveJobWithRetry(mainJobId)).thenReturn(mainJobAttr);
    }

    @Test
    public void testGetPoAttributes() {
        getPoAttributes();

        assertEquals(poAttr, objectUnderTest.getPoAttributes(poId));

    }

    @SuppressWarnings("unchecked")
    private void getPoAttributes() {
        poAttr = new HashMap<String, Object>();
        poAttr.put(ShmConstants.PO_ID, poId);
        when(jobUpdateService.retrieveJobWithRetry(poId)).thenReturn(poAttr);

        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(poAttr);
    }

    @Test
    public void TestCreateNewLogEntry() {
        final String logMessage = "log message";
        Map<String, Object> logEntry = new HashMap<String, Object>();
        logEntry = objectUnderTest.createNewLogEntry(logMessage, JobLogLevel.INFO.toString());
        assertNotNull(logEntry);
        assertEquals(4, logEntry.size());
        assertEquals("log message", logEntry.get(ActivityConstants.JOB_LOG_MESSAGE));
        assertEquals("SYSTEM", logEntry.get(ActivityConstants.JOB_LOG_TYPE));

    }

    @Test
    public void TestAddJobLog() {
        final List<Map<String, Object>> jobList = new ArrayList<Map<String, Object>>();
        objectUnderTest.addJobLog("message", "Job type", jobList, "logLevel");
        assertNotNull(jobList);
        assertEquals(1, jobList.size());
        assertEquals("message", jobList.get(0).get(ActivityConstants.JOB_LOG_MESSAGE));
        assertEquals("Job type", jobList.get(0).get(ActivityConstants.JOB_LOG_TYPE));
        assertEquals("logLevel", jobList.get(0).get(ActivityConstants.JOB_LOG_LEVEL));

    }

    @Test
    public void TestPrepareJobLogAtrributesList() {
        final List<Map<String, Object>> jobList = new ArrayList<Map<String, Object>>();
        objectUnderTest.prepareJobLogAtrributesList(jobList, "activity log message", new Date(), "log type", JobLogLevel.INFO.toString());
        assertNotNull(jobList);
        assertEquals(1, jobList.size());
        assertEquals("activity log message", jobList.get(0).get(ActivityConstants.JOB_LOG_MESSAGE));
        assertEquals("log type", jobList.get(0).get(ActivityConstants.JOB_LOG_TYPE));
    }

    @Test
    public void TestAdditionalInfoForEvent() {
        final String expected = "ActivityJobId: 1, NodeName: node name, Message: log message";
        final String actual = objectUnderTest.additionalInfoForEvent(activityJobId, "node name", "log message");
        assertEquals(expected, actual);

    }

    @Test
    public void TestAdditionalInfoForCommand() {
        final String expected = "ActivityJobId: 1, MainJobId: 3, JobType: RESTORE";
        final String actual = objectUnderTest.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.RESTORE);
        assertEquals(expected, actual);

    }

    @Test
    public void TestGetInvokedActionId() {

        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "1");
        activityJobPropertyList.add(jobProperty);
        activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        neJobAttr = new HashMap<String, Object>();
        final JobEnvironment jobEnvir = new JobEnvironment(this.activityJobId, objectUnderTest);
        when(jobUpdateService.retrieveJobWithRetry(this.activityJobId)).thenReturn(activityJobAttr);

        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(activityJobAttr);

        final int Result = objectUnderTest.getPersistedActionId(jobEnvir);
        assertNotNull(Result);
        assertEquals(1, Result);
    }

    @Test
    public void TestUnSubscribToMoNotifications() {
        final String mofdn = "mofdn";
        mockJobActivityInfo();
        objectUnderTest.unSubscribeToMoNotifications(mofdn, activityJobId, jobActivityInfoMock);
        verify(notificationRegistry, times(1)).removeSubject(any(FdnNotificationSubject.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void TestGetJobEnvironment() {
        poAttr = new HashMap<String, Object>();
        poAttr.put(ShmConstants.ACTIVITY_JOB, activityJobId);
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(poAttr);
        when(jobUpdateService.retrieveJobWithRetry(mainJobId)).thenReturn(poAttr);
        when(jobUpdateService.retrieveJobWithRetry(neJobId)).thenReturn(poAttr);
        poAttr.put(ShmConstants.NE_JOB_ID, neJobId);
        poAttr.put(ShmConstants.NE_NAME, neName);
        poAttr.put(ShmConstants.MAIN_JOB_ID, mainJobId);

        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(poAttr);

        final JobEnvironment jobEn = objectUnderTest.getJobEnvironment(activityJobId);
        assertNotNull(jobEn);
        assertEquals(neName, jobEn.getNodeName());
        assertEquals(neJobId, jobEn.getNeJobId());
        assertEquals(mainJobId, jobEn.getMainJobId());
        assertEquals(poAttr, jobEn.getActivityJobAttributes());
        assertEquals(poAttr, jobEn.getMainJobAttributes());
        assertEquals(poAttr, jobEn.getNeJobAttributes());

    }

    @Test
    public void TestGetInvokedActionIdwithNumberFormatException() {

        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ACTION_ID);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "1");
        activityJobPropertyList.add(jobProperty);
        activityJobAttr = new HashMap<String, Object>();
        activityJobAttr.put(ActivityConstants.JOB_PROPERTIES, activityJobPropertyList);
        neJobAttr = new HashMap<String, Object>();
        final JobEnvironment jobEnvir = new JobEnvironment(this.activityJobId, objectUnderTest);
        when(jobUpdateService.retrieveJobWithRetry(this.activityJobId)).thenReturn(activityJobAttr);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(activityJobAttr);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(poAttr);

        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(activityJobAttr);

        final int Result = objectUnderTest.getPersistedActionId(jobEnvir);
        assertNotNull(Result);
        assertEquals(1, Result);

    }

    private void setMainJob() {

        mainJobAttr = new HashMap<String, Object>();
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();
        jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.SWP_NAME);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, swPkgName);
        jobPropertyList.add(jobProperty);

        final Map<String, Object> nejobProperty1 = new HashMap<String, Object>();
        final List<Map<String, Object>> neTypeJobPropertyList = new ArrayList<Map<String, Object>>();
        nejobProperty1.put(ShmConstants.NETYPE, "ERBS");
        nejobProperty1.put(ShmConstants.JOBPROPERTIES, jobPropertyList);
        neTypeJobPropertyList.add(nejobProperty1);

        jobConfiguration.put(ShmConstants.NETYPEJOBPROPERTIES, neTypeJobPropertyList);

        mainJobAttr.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfiguration);
        when(jobUpdateService.retrieveJobWithRetry(mainJobId)).thenReturn(mainJobAttr);
        final List<String> keyList = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.SWP_NAME);
        final Map<String, String> swpPkg = new HashMap<String, String>();
        swpPkg.put(UpgradeActivityConstants.SWP_NAME, "Some Sw Pkg Name");
        Mockito.when(jobPropertyUtils.getPropertyValue(keyList, jobConfiguration, neName, "ERBS", "CPP")).thenReturn(swpPkg);

    }

    @Test
    public void testGetActivityStepResult() {
        final ActivityStepResultEnum stepResultEnum = ActivityStepResultEnum.EXECUTION_FAILED;
        final ActivityStepResult result = objectUnderTest.getActivityStepResult(stepResultEnum);
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, result.getActivityResultEnum());

    }

    @Test
    public void TestSubscribToMoNotifications() {
        mockJobActivityInfo();
        final FdnNotificationSubject fdnNotificationSubject = objectUnderTest.subscribeToMoNotifications("MoFdn", activityJobId, jobActivityInfoMock);
        assertNotNull(fdnNotificationSubject);
    }

    @Test
    public void TestAddJobProperty() {
        jobPropertyList = new ArrayList<Map<String, Object>>();
        final String keyName = "key Name";
        final Object object = new Object();
        objectUnderTest.addJobProperty(keyName, object, jobPropertyList);
        assertNotNull(jobPropertyList);
    }

    @Test
    public void testGetProductId() {
        final String productNumber = "CXP123";
        final String productRevision = "RD12";
        final String actualProductId = objectUnderTest.getProductId(productNumber, productRevision);
        assertEquals(productNumber + ShmConstants.DELIMITER_COLON + productRevision, actualProductId);
    }

    @Test
    public void testGetProductIdForNull() {
        final String productNumber = null;
        final String productRevision = null;
        final String actualProductId = objectUnderTest.getProductId(productNumber, productRevision);
        assertNull(actualProductId);
    }

    private void setRetryPolicies() {
        PowerMockito.mockStatic(RetryPolicy.class);
        when(RetryPolicy.builder()).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.attempts(anyInt())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.waitInterval(anyInt(), eq(TimeUnit.SECONDS))).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.exponentialBackoff(anyDouble())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.retryOn(exceptionsArray)).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.build()).thenReturn(retryPolicyMock);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
    }

    private void mockJobActivityInfo() {
        Mockito.when(jobActivityInfoMock.getJobType()).thenReturn(JobTypeEnum.BACKUP);
        Mockito.when(jobActivityInfoMock.getPlatform()).thenReturn(PlatformTypeEnum.CPP);
    }

    @Test
    public void testIsTreatAs_SupportedForCpp() {
        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        final List<ProductData> neProductVersionList = new ArrayList<ProductData>();
        networkElementList.add(networkElement);
        neProductVersionList.add(neProductVersion);
        when(fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList("nodeName"))).thenReturn(networkElementList);
        when(networkElementList.get(0).getOssModelIdentity()).thenReturn("4322-520-420");
        when(networkElementList.get(0).getNodeModelIdentity()).thenReturn("4322-520-420");
        when(networkElementList.get(0).getNeProductVersion()).thenReturn(neProductVersionList);
        when(neProductVersionList.get(0).getIdentity()).thenReturn("CXP101012");
        when(neProductVersionList.get(0).getRevision()).thenReturn("G1232");
        assertEquals(null, objectUnderTest.isTreatAs("nodeName"));
    }

    @Test
    public void testIsTreatAs_SupportedForEcim() {
        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        final List<ProductData> neProductVersionList = new ArrayList<ProductData>();
        networkElementList.add(networkElement);
        neProductVersionList.add(neProductVersion);
        when(fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList("nodeName"))).thenReturn(networkElementList);
        when(networkElementList.get(0).getOssModelIdentity()).thenReturn("4322-520-420");
        when(networkElementList.get(0).getNodeModelIdentity()).thenReturn("4322-520-420");
        when(networkElementList.get(0).getNeProductVersion()).thenReturn(neProductVersionList);
        when(neProductVersionList.get(0).getIdentity()).thenReturn("CXP101012");
        when(neProductVersionList.get(0).getRevision()).thenReturn("G1232");
        assertEquals(null, objectUnderTest.isTreatAs("nodeName", FragmentType.ECIM_BRM_TYPE, SHMCapabilities.UPGRADE_JOB_CAPABILITY));
    }

    @Test
    public void testIsTreatAs_unSupported() {
        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        final List<ProductData> neProductVersionList = new ArrayList<ProductData>();
        networkElementList.add(networkElement);
        neProductVersionList.add(neProductVersion);
        neProductVersionList.add(neProductVersion1);
        final Set<String> releaseVersion = new HashSet<String>();
        final ProductData productMap = new ProductData();
        final ProductData productMap1 = new ProductData();
        final List<ProductData> productInfos = new ArrayList<ProductData>();
        productInfos.add(productMap);
        productInfos.add(productMap1);
        when(fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList("nodeName"), null)).thenReturn(networkElementList);
        when(networkElementList.get(0).getOssModelIdentity()).thenReturn("4322-520-420");
        when(networkElementList.get(0).getNodeModelIdentity()).thenReturn("4322-520-425");
        when(networkElementList.get(0).getNeProductVersion()).thenReturn(neProductVersionList);
        final OssModelInfo ossModelInfo = new OssModelInfo("nameSpace", "version", "referenceName", "referenceMim");
        final List<OssModelInfo> info = new ArrayList<OssModelInfo>();
        info.add(ossModelInfo);
        when(ossModelInfoProvider.getReleaseVersion("4322-520-420", "ERBS")).thenReturn(releaseVersion);
        when(ossModelInfoProvider.getProductInfo(Matchers.anyString(), Matchers.anyString())).thenReturn(productInfos);
        when(ossModelInfoProvider.getOssModelInfo(Matchers.anyString(), Matchers.anyString())).thenReturn(info);
        final String mimVersion = ossModelInfo != null ? ossModelInfo.getVersion() : "";
        final String treatAsInfo = String.format(JobLogConstants.NODE_IS_IN_TREAT_AS_SUPPORT, mimVersion, releaseVersion, productInfos, neProductVersionList);
        assertEquals(treatAsInfo, objectUnderTest.isTreatAs("nodeName"));
    }

    @Test
    public void testCancelTriggeredReturningTrue() {
        activityJobAttr = new HashMap<String, Object>();
        jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.KEY, ActivityConstants.IS_CANCEL_TRIGGERED);
        jobProperty.put(ShmConstants.VALUE, "true");
        jobPropertyList.add(jobProperty);
        activityJobAttr.put(ShmConstants.JOBPROPERTIES, jobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(activityJobAttr);

        assertTrue(objectUnderTest.cancelTriggered(activityJobId));
    }

    @Test
    public void testCancelTriggeredReturningFalse() {
        activityJobAttr = new HashMap<String, Object>();
        when(jobEnv.getActivityJobAttributes()).thenReturn(activityJobAttr);

        assertFalse(objectUnderTest.cancelTriggered(activityJobId));
    }

    @Test
    public void testGetParentFdn() {
        final String childFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1,BrmBackup=test";
        final String parentFdn = "NetworkElement=NE01,MeContext=NE01,Brm=1,BrmBackupManager=1";
        assertEquals(objectUnderTest.getParentFdn(childFdn), parentFdn);
    }

    @Test
    public void testgetJobConfigurationDetails() {

        final Map<String, Object> jobConfigDetails = new HashMap<>();

        activityJobAttr = new HashMap<>();
        activityJobAttr.put(ShmConstants.NE_JOB_ID, neJobId);
        neJobAttr = new HashMap<>();
        neJobAttr.put(ShmConstants.MAINJOBID, mainJobId);
        mainJobAttr = new HashMap<>();
        mainJobAttr.put(ShmConstants.JOBTEMPLATEID, poId);
        poAttr = new HashMap<>();
        poAttr.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfigDetails);

        when(jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttr);
        when(jobConfigurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(neJobAttr);
        when(jobConfigurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttr);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
        when(retryManagerMock.executeCommand(eq(retryPolicyMock), any(RetriableCommand.class))).thenReturn(poAttr);

        final Map<String, Object> result = objectUnderTest.getJobConfigurationDetails(activityJobId);
        assertNotNull(result);
        assertEquals(jobConfigDetails, result);
    }

    @Test
    public void testSendNotificationToWFSWhenNotifyingCancelWorkflow() {
        final String activity = "execute";
        final String businessKey = "business";
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        neJobAttr = new HashMap<String, Object>();
        neJobAttr.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(jobEnv.getNeJobAttributes()).thenReturn(neJobAttr);
        activityJobAttr = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(ShmConstants.KEY, ActivityConstants.IS_CANCEL_TRIGGERED);
        jobProperty.put(ShmConstants.VALUE, "true");
        activityJobProperties.add(jobProperty);
        activityJobAttr.put(ShmConstants.JOBPROPERTIES, activityJobProperties);
        when(jobUpdateService.retrieveJobWithRetry(activityJobId)).thenReturn(activityJobAttr);

        objectUnderTest.sendNotificationToWFS(jobEnv, activityJobId, activity, processVariables);

        verify(workflowInstanceNotifier, times(1)).sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, businessKey);
    }

    @Test
    public void testSendNotificationToWFSWhenNotifyingNormalWorkflow() {
        final String activity = "execute";
        final String businessKey = "business";
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        neJobAttr = new HashMap<String, Object>();
        neJobAttr.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(jobEnv.getNeJobAttributes()).thenReturn(neJobAttr);
        activityJobAttr = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobProperties = new ArrayList<Map<String, String>>();
        activityJobAttr.put(ShmConstants.JOBPROPERTIES, activityJobProperties);
        when(jobEnv.getActivityJobAttributes()).thenReturn(activityJobAttr);

        objectUnderTest.sendNotificationToWFS(jobEnv, activityJobId, activity, processVariables);

        verify(workflowInstanceNotifier, times(1)).sendActivate(businessKey, processVariables);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSendNotificationToWFSThrowsWorkflowServiceInvocationException() {
        final String activity = "execute";
        final String businessKey = "business";
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        neJobAttr = new HashMap<String, Object>();
        neJobAttr.put(ShmConstants.BUSINESS_KEY, businessKey);
        when(jobEnv.getNeJobAttributes()).thenReturn(neJobAttr);
        activityJobAttr = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobProperties = new ArrayList<Map<String, String>>();
        activityJobAttr.put(ShmConstants.JOBPROPERTIES, activityJobProperties);
        when(jobEnv.getActivityJobAttributes()).thenReturn(activityJobAttr);
        when(jobEnv.getNodeName()).thenReturn(neName);
        when(workflowInstanceNotifier.sendActivate(businessKey, processVariables)).thenThrow(new WorkflowServiceInvocationException("Correlation Failed. Some Dummy Reason"));
        objectUnderTest.sendNotificationToWFS(jobEnv, activityJobId, activity, processVariables);
        verify(systemRecorder).recordEvent(SHMEvents.WORKFLOW_SERVICE_CORRELATION, EventLevel.COARSE, activity, neName, "Failure Reason : Correlation Failed. Some Dummy Reason");
        verify(jobUpdateService, times(1)).addOrUpdateOrRemoveJobProperties(Matchers.anyLong(), Matchers.anyMap(), Matchers.anyList());
    }

    @Test
    public void testBuildProcessVariablesForTimeoutAndNotifyWfsForTimeoutSuccess() {
        when(jobEnv.getNeJobAttributes()).thenReturn(neJobAttr);
        when(neJobAttr.get(ShmConstants.BUSINESS_KEY)).thenReturn(businessKey);
        when(jobEnv.getActivityJobId()).thenReturn(activityJobId);
        objectUnderTest.buildProcessVariablesForTimeoutAndNotifyWfs(jobEnv, ActivityConstants.INSTALL_LICENSE, ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        verify(workflowInstanceNotifier).sendPrecheckOrTimeoutMsgToWfsInstance(businessKey, processVariables, JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
    }

    @Test
    public void testBuildProcessVariablesForTimeoutAndNotifyWfsForTimeoutRepeatExecute() {
        when(jobEnv.getNeJobAttributes()).thenReturn(neJobAttr);
        when(neJobAttr.get(ShmConstants.BUSINESS_KEY)).thenReturn(businessKey);
        when(jobEnv.getActivityJobId()).thenReturn(activityJobId);
        objectUnderTest.buildProcessVariablesForTimeoutAndNotifyWfs(jobEnv, ActivityConstants.INSTALL_LICENSE, ActivityStepResultEnum.REPEAT_EXECUTE);
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
        verify(workflowInstanceNotifier).sendPrecheckOrTimeoutMsgToWfsInstance(businessKey, processVariables, JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
    }

    @Test
    public void testBuildProcessVariablesForTimeoutAndNotifyWfsForTimeoutRepeatExecuteManual() {
        when(jobEnv.getNeJobAttributes()).thenReturn(neJobAttr);
        when(neJobAttr.get(ShmConstants.BUSINESS_KEY)).thenReturn(businessKey);
        when(jobEnv.getActivityJobId()).thenReturn(activityJobId);
        objectUnderTest.buildProcessVariablesForTimeoutAndNotifyWfs(jobEnv, ActivityConstants.INSTALL_LICENSE, ActivityStepResultEnum.TIMEOUT_REPEAT_EXECUTE_MANUAL);
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
        processVariables.put(JobVariables.ACTIVITY_EXECUTE_MANUALLY, true);
        verify(workflowInstanceNotifier).sendPrecheckOrTimeoutMsgToWfsInstance(businessKey, processVariables, JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
    }

    @Test
    public void testBuildProcessVariablesForTimeoutAndNotifyWfsWhereNotifyToWFSThrownERROR() {
        when(jobEnv.getNeJobAttributes()).thenReturn(neJobAttr);
        when(neJobAttr.get(ShmConstants.BUSINESS_KEY)).thenReturn(businessKey);
        when(jobEnv.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnv.getNodeName()).thenReturn(neName);
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        final String exceptionMessage = "Exception";
        doThrow(new WorkflowServiceInvocationException(exceptionMessage)).when(workflowInstanceNotifier).sendPrecheckOrTimeoutMsgToWfsInstance(businessKey, processVariables,
                JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);

        objectUnderTest.buildProcessVariablesForTimeoutAndNotifyWfs(jobEnv, ActivityConstants.INSTALL_LICENSE, ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFailActivityForPrecheckTimeoutExpiry() {
        final Integer precheckTimeout = 5;
        final String activityName = ActivityConstants.INSTALL_LICENSE;
        when(activityTimeoutsService.getPrecheckTimeoutAsInteger()).thenReturn(precheckTimeout);
        objectUnderTest.failActivityForPrecheckTimeoutExpiry(activityJobId, activityName);
        verify(jobUpdateService).readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFailActivityForHandleTimeoutExpiry() {
        final Integer timeoutForHandleTimeout = 5;
        final String activityName = ActivityConstants.INSTALL_LICENSE;
        when(activityTimeoutsService.getTimeoutForHandleTimeoutAsInteger()).thenReturn(timeoutForHandleTimeout);
        objectUnderTest.failActivityForHandleTimeoutExpiry(activityJobId, activityName);
        verify(jobUpdateService).readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble());
    }

    @Test
    public void testGetActivityCompletionEvent() {
        assertEquals("SHM.CPP.UPGRADE.UPGRADE_COMPLETED", objectUnderTest.getActivityCompletionEvent(PlatformTypeEnum.CPP, JobTypeEnum.UPGRADE, "upgrade"));
    }

    @Test
    public void testPersistStepDurations() {
        final String stepName = ActivityStepsEnum.EXECUTE.getStep();
        objectUnderTest.persistStepDurations(activityJobId, new Date().getTime(), ActivityStepsEnum.EXECUTE);
        verify(jobUpdateService).readAndUpdateStepDurations(eq(activityJobId), Matchers.anyString(), eq(stepName));
    }

    @Test
    public void testIsRepeatRequiredOnPrecheckWhenPropertyDoesNotExist() {
        final String neType = "RadioNode";
        final String platform = "ECIM";
        final String jobType = "BACKUP";
        final String activityName = "uploadbackup";
        final Map<String, String> activityInformation = new HashMap<String, String>();
        activityInformation.put(ShmCommonConstants.NETYPE, neType);
        activityInformation.put(ShmCommonConstants.PLATFORM, platform);
        activityInformation.put(ShmCommonConstants.JOB_TYPE, jobType);
        activityInformation.put(ShmJobConstants.ACTIVITYNAME, activityName);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();

        getPoAttributes();

        getActivityJobAttributeValue(null);

        final int attemptsForRepeatPrecheckAsInteger = 1;
        final Map<String, Integer> retryAttempts = new HashMap<String, Integer>();
        retryAttempts.put("attempts", attemptsForRepeatPrecheckAsInteger);
        when(activityTimeoutsService.getRepeatPrecheckRetryAttempt(neType, platform, jobType, activityName)).thenReturn(attemptsForRepeatPrecheckAsInteger);

        assertTrue(objectUnderTest.isRepeatRequiredOnPrecheck(activityJobId, jobPropertyList, attemptsForRepeatPrecheckAsInteger, retryAttempts, activityJobAttributes));

        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ATTEMPTS_FOR_REPEAT_PRECHECK);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(attemptsForRepeatPrecheckAsInteger));
        verify(jobPropertyList).add(jobProperty);
    }

    @Test
    public void testIsRepeatRequiredOnPrecheckWhenPropertyExistAndAttemptsDidNotExhausted() {
        final String neType = "RadioNode";
        final String platform = "ECIM";
        final String jobType = "BACKUP";
        final String activityName = "uploadbackup";
        final Map<String, String> activityInformation = new HashMap<String, String>();
        activityInformation.put(ShmCommonConstants.NETYPE, neType);
        activityInformation.put(ShmCommonConstants.PLATFORM, platform);
        activityInformation.put(ShmCommonConstants.JOB_TYPE, jobType);
        activityInformation.put(ShmJobConstants.ACTIVITYNAME, activityName);

        getPoAttributes();

        final String attributeName = ActivityConstants.ATTEMPTS_FOR_REPEAT_PRECHECK;
        final String attemptsForRepeatPrecheckAsString = "2";
        final Map<String, Integer> retryAttempts = new HashMap<String, Integer>();
        retryAttempts.put("attempts", Integer.parseInt(attemptsForRepeatPrecheckAsString));
        final Map<String, Object> activityJobProperty = new HashMap<String, Object>();
        activityJobProperty.put(attributeName, attemptsForRepeatPrecheckAsString);
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        activityJobProperty.put(ActivityConstants.JOB_PROP_KEY, attributeName);
        activityJobProperty.put(ActivityConstants.JOB_PROP_VALUE, attemptsForRepeatPrecheckAsString);
        activityJobPropertyList.add(activityJobProperty);
        when(activityJobAttr.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
        final int attemptsForRepeatPrecheckAsInteger = 3;

        assertTrue(objectUnderTest.isRepeatRequiredOnPrecheck(activityJobId, jobPropertyList, attemptsForRepeatPrecheckAsInteger, retryAttempts, activityJobAttr));

        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, ActivityConstants.ATTEMPTS_FOR_REPEAT_PRECHECK);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, Integer.toString(Integer.parseInt(attemptsForRepeatPrecheckAsString) + 1));
        verify(jobPropertyList).add(jobProperty);
    }

    @Test
    public void testIsRepeatRequiredOnPrecheckWhenPropertyExistAndAttemptsExhausted() {
        final String neType = "RadioNode";
        final String platform = "ECIM";
        final String jobType = "BACKUP";
        final String activityName = "uploadbackup";
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();

        final Map<String, String> activityInformation = new HashMap<String, String>();
        activityInformation.put(ShmCommonConstants.NETYPE, neType);
        activityInformation.put(ShmCommonConstants.PLATFORM, platform);
        activityInformation.put(ShmCommonConstants.JOB_TYPE, jobType);
        activityInformation.put(ShmJobConstants.ACTIVITYNAME, activityName);
        final String attributeName = ActivityConstants.ATTEMPTS_FOR_REPEAT_PRECHECK;
        final String attemptsForRepeatPrecheckAsString = "1";
        final int maxRetryAttempts = 2;
        final Map<String, Integer> retryAttempts = new HashMap<String, Integer>();
        retryAttempts.put("attempts", Integer.parseInt(attemptsForRepeatPrecheckAsString));
        final Map<String, Object> activityJobProperty = new HashMap<String, Object>();
        activityJobProperty.put(ActivityConstants.JOB_PROP_KEY, attributeName);
        activityJobProperty.put(ActivityConstants.JOB_PROP_VALUE, attemptsForRepeatPrecheckAsString + 1);
        activityJobPropertyList.add(activityJobProperty);

        getPoAttributes();
        poAttr.put(ShmConstants.PO_ID, poId);
        poAttr.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        when(jobUpdateService.retrieveJobWithRetry(poId)).thenReturn(poAttr);
        when(activityJobAttr.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);

        assertFalse(objectUnderTest.isRepeatRequiredOnPrecheck(activityJobId, jobPropertyList, maxRetryAttempts, retryAttempts, activityJobAttr));
        verify(jobPropertyList, times(0)).add(Matchers.anyMapOf(String.class, Object.class));
    }

    @Test
    public void testGetActivityJobAttributeValue() {
        final String attributeName = ActivityConstants.ATTEMPTS_FOR_REPEAT_PRECHECK;
        final String attributeValue = "1";
        final Map<String, Object> activityJobProperty = new HashMap<String, Object>();
        activityJobProperty.put(ActivityConstants.JOB_PROP_KEY, attributeName);
        activityJobProperty.put(ActivityConstants.JOB_PROP_VALUE, attributeValue);
        getActivityJobAttributeValue(activityJobProperty);
        assertEquals(attributeValue, objectUnderTest.getActivityJobAttributeValue(activityJobAttr, attributeName));
    }

    private void getActivityJobAttributeValue(final Map<String, Object> activityJobProperty) {
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        activityJobPropertyList.add(activityJobProperty);
        when(activityJobAttr.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(activityJobPropertyList);
    }

    @Test
    public void testGetActivityJobAttributeValueWhenPropertyDoesNotExist() {
        final String attributeName = ActivityConstants.ATTEMPTS_FOR_REPEAT_PRECHECK;
        final Map<String, Object> activityJobProperty = new HashMap<String, Object>();
        activityJobProperty.put(ActivityConstants.JOB_PROP_KEY, null);
        activityJobProperty.put(ActivityConstants.JOB_PROP_VALUE, attributeName);
        getActivityJobAttributeValue(activityJobProperty);

        assertEquals("", objectUnderTest.getActivityJobAttributeValue(activityJobAttr, attributeName));
    }

    @Test
    public void testRecordEventWithExecutionUser() {
        Mockito.doNothing().when(systemRecorder).recordEvent("JOB_EXEC_USER", "eventType", EventLevel.COARSE, "source", "resource", "additionalInformation");
        objectUnderTest.recordEvent("JOB_EXEC_USER", "eventType", "source", "resource", "additionalInformation");
    }

    @Test
    public void testGetShmJobExecUserFromJobCacheOrDps() throws JobDataNotFoundException {
        final JobStaticData jobStaticData = new JobStaticData(OWNER, new HashMap<>(), ExecMode.MANUAL.getMode(), JobType.UPGRADE, JOB_EXEC_USER);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticData);
        final String jobExecUser = objectUnderTest.getJobExecutionUser(mainJobId);
        assertNotNull(jobExecUser);
        Assert.assertEquals(JOB_EXEC_USER, jobExecUser);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetShmJobExecUserWhenJobCacheOrDpsWithNull() throws JobDataNotFoundException {
        final JobStaticData jobStaticData = new JobStaticData(OWNER, new HashMap<>(), ExecMode.MANUAL.getMode(), JobType.UPGRADE, JOB_EXEC_USER);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticData);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenThrow(JobDataNotFoundException.class);
        objectUnderTest.getJobExecutionUser(mainJobId);

    }
}
