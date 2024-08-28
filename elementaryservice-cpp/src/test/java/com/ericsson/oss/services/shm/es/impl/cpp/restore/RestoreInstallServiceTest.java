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
package com.ericsson.oss.services.shm.es.impl.cpp.restore;

import static com.ericsson.oss.services.shm.es.impl.cpp.restore.RestoreServiceConstants.CORRUPTED_PKGS;
import static com.ericsson.oss.services.shm.es.impl.cpp.restore.RestoreServiceConstants.MISSING_PKGS;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.testng.annotations.BeforeTest;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionService;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.InstallActivityHandler;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class RestoreInstallServiceTest {

    private static final String FDN = "abcds=sdgd,UP=null";

    private static final String PRODUCT_NUMBER = "null";
    private static final String PRODUCT_REVISION = "null";

    private static final long ACTIVITY_JOB_ID = 1234l;
    private final long neJobId = 789546l;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private JobConfigurationService jobConfigurationServiceMock;

    @InjectMocks
    private RestoreInstallService objectUnderTest;

    @Mock
    private InstallActivityHandler localInstallationService;

    @Mock
    private Map<String, Object> mapMock;

    @Mock
    private UpgradePackageService upgradePackageService;

    @Mock
    private ActivityStepResult activityStepResult;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private ConfigurationVersionService cvService;

    @Mock
    private Notification notificationMock;

    @Mock
    private FdnNotificationSubject notificationSubjectMock;

    @Mock
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Mock
    private JobUpdateService jobUpdateServiceMock;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @Mock
    private JobActivityInfo jobActivityInfoMock;

    @Mock
    private RestorePrecheckHandler restorePrecheckHandlerMock;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Mock
    private Map<String, Object> processNotificationResult;

    private Map<String, Object> activityJobAttributes = new HashMap<String, Object>();

    @Test
    public void testPrecheck() {
        when(restorePrecheckHandlerMock.getRestorePrecheckResult(ACTIVITY_JOB_ID, ActivityConstants.RESTORE_INSTALL_CV, ActivityConstants.INSTALL)).thenReturn(activityStepResult);
        final ActivityStepResult precheckResult = objectUnderTest.precheck(ACTIVITY_JOB_ID);
        Assert.assertEquals(activityStepResult, precheckResult);
    }

    @Test
    public void testExecute_noJobProprtiesFound_failed() {
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, new ArrayList<Map<String, Object>>());
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);

        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(activityJobAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(Collections.EMPTY_LIST);
        objectUnderTest.execute(ACTIVITY_JOB_ID);
        verify(localInstallationService, never()).execute(anyString(), anyString(), anyString(), anyString(), any(JobActivityInfo.class));

    }

    @Test
    public void testExecute_noJobProprtiesFound() {
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobProperties.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobProperties);
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(activityJobAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(Arrays.asList(mapMock));
        objectUnderTest.execute(ACTIVITY_JOB_ID);
        verify(localInstallationService, never()).execute(anyString(), anyString(), anyString(), anyString(), any(JobActivityInfo.class));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testExecute_emptyMissingPkgsFound_failed() {
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.KEY, "missing");
        jobProperty.put(ShmConstants.VALUE, "");
        activityJobProperties.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobProperties);
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);

        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(activityJobAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(MISSING_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn("");
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(new ArrayList());
        objectUnderTest.execute(ACTIVITY_JOB_ID);
        verify(localInstallationService, never()).execute(anyString(), anyString(), anyString(), anyString(), any(JobActivityInfo.class));
    }

    @Test
    public void testExecute_missingPkgsFound() {
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.KEY, "missing");
        jobProperty.put(ShmConstants.VALUE, "number:version");
        activityJobProperties.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobProperties);
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);

        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(activityJobAttributes);

        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(MISSING_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn("number:version");
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(Arrays.asList(mapMock));
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, ActivityStepsEnum.PRECHECK + "=9.9");
        when(activityUtils.getActivityJobAttributes(ACTIVITY_JOB_ID)).thenReturn(activityJobAttributes);

        objectUnderTest.execute(ACTIVITY_JOB_ID);

        verify(localInstallationService, times(1)).execute(anyString(), anyString(), anyString(), anyString(), any(JobActivityInfo.class));
        verify(activityUtils, times(1)).getActivityInfo(anyLong(), eq(RestoreInstallService.class));
        Mockito.verify(activityUtils, times(1)).persistStepDurations(eq(ACTIVITY_JOB_ID), anyLong(), eq(ActivityStepsEnum.EXECUTE));

    }

    @Test
    public void testExecute_emptyCorruptedPkgsFound_failed() {
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.KEY, "corrupted");
        jobProperty.put(ShmConstants.VALUE, "");
        activityJobProperties.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobProperties);
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);

        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(activityJobAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(CORRUPTED_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn("");
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(Arrays.asList(mapMock));
        objectUnderTest.execute(ACTIVITY_JOB_ID);
        verify(localInstallationService, never()).execute(anyString(), anyString(), anyString(), anyString(), any(JobActivityInfo.class));
    }

    @Test
    public void testExecute_corruptedFound() {
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobProperties = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ShmConstants.KEY, "corrupted");
        jobProperty.put(ShmConstants.VALUE, "number:version");
        activityJobProperties.add(jobProperty);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobProperties);
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);

        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(activityJobAttributes);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(CORRUPTED_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn("number:version");
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(Arrays.asList(mapMock));
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, ActivityStepsEnum.PRECHECK + "=9.9");
        when(activityUtils.getActivityJobAttributes(ACTIVITY_JOB_ID)).thenReturn(activityJobAttributes);

        objectUnderTest.execute(ACTIVITY_JOB_ID);
        verify(localInstallationService, times(1)).execute(anyString(), anyString(), anyString(), anyString(), any(JobActivityInfo.class));
        verify(activityUtils, times(1)).getActivityInfo(anyLong(), eq(RestoreInstallService.class));
        Mockito.verify(activityUtils, times(1)).persistStepDurations(eq(ACTIVITY_JOB_ID), anyLong(), eq(ActivityStepsEnum.EXECUTE));

    }

    @Test
    public void testHandleTimeout_noJobPropertiesFound() {
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(Collections.EMPTY_LIST);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        Assert.assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(ACTIVITY_JOB_ID).getActivityResultEnum());
        verify(jobUpdateServiceMock, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        Mockito.verify(activityUtils).persistStepDurations(eq(ACTIVITY_JOB_ID), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testHandleTimeout_noProductsTobeProcessed() {
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(Arrays.asList(mapMock));
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        Assert.assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(ACTIVITY_JOB_ID).getActivityResultEnum());
        verify(jobUpdateServiceMock, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        Mockito.verify(activityUtils).persistStepDurations(eq(ACTIVITY_JOB_ID), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));

    }

    @Test
    public void testHandleTimeout_emptyMissingPkgsFound_failed() {
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(MISSING_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn("");
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(Arrays.asList(mapMock));
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);
        Assert.assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(ACTIVITY_JOB_ID).getActivityResultEnum());
        verify(jobUpdateServiceMock, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        Mockito.verify(activityUtils).persistStepDurations(eq(ACTIVITY_JOB_ID), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));

    }

    @Test
    public void testHandleTimeout_missingPkgsFound_butFailed() {
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(MISSING_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(PRODUCT_NUMBER + ":" + PRODUCT_REVISION);
        final List<Map<String, Object>> jobProps = new ArrayList<Map<String, Object>>();
        jobProps.add(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(jobProps);
        when(mapMock.remove(anyMap())).thenReturn(true);
        when(activityStepResult.getActivityResultEnum()).thenReturn(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        when(localInstallationService.handleTimeout(any(JobActivityInfo.class))).thenReturn(activityStepResult);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        Assert.assertEquals(activityStepResult, objectUnderTest.handleTimeout(ACTIVITY_JOB_ID));
        verify(jobUpdateServiceMock, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(activityStepResult, never()).setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
        verify(activityUtils, times(1)).getActivityInfo(anyLong(), eq(RestoreInstallService.class));
        Mockito.verify(activityUtils).persistStepDurations(eq(ACTIVITY_JOB_ID), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testHandleTimeout_missingPkgsFound() {
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(MISSING_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(PRODUCT_NUMBER + ":" + PRODUCT_REVISION);
        final List<Map<String, Object>> jobProps = new ArrayList<Map<String, Object>>();
        jobProps.add(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(jobProps);
        when(mapMock.remove(anyMap())).thenReturn(true);
        when(localInstallationService.handleTimeout(any(JobActivityInfo.class))).thenReturn(activityStepResult);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        Assert.assertEquals(activityStepResult, objectUnderTest.handleTimeout(ACTIVITY_JOB_ID));
        verify(jobUpdateServiceMock, times(1)).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(activityStepResult, never()).setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
    }

    @Test
    public void testHandleTimeout_missingPkgsFound_morePkgsLeft() {
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(MISSING_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(PRODUCT_NUMBER + ":" + PRODUCT_REVISION + "|" + PRODUCT_NUMBER + ":" + PRODUCT_REVISION);
        final List<Map<String, Object>> jobProps = new ArrayList<Map<String, Object>>();
        jobProps.add(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(jobProps);
        when(mapMock.remove(anyMap())).thenReturn(true);
        when(localInstallationService.handleTimeout(any(JobActivityInfo.class))).thenReturn(activityStepResult);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        Assert.assertEquals(activityStepResult, objectUnderTest.handleTimeout(ACTIVITY_JOB_ID));
        verify(jobUpdateServiceMock, times(1)).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(activityStepResult, times(1)).setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
        verify(activityUtils, times(1)).getActivityInfo(anyLong(), eq(RestoreInstallService.class));
    }

    @Test
    public void testHandleTimeout_emptyCorruptedPkgsFound_failed() {
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(CORRUPTED_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn("");
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(Arrays.asList(mapMock));
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        Assert.assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(ACTIVITY_JOB_ID).getActivityResultEnum());
        verify(jobUpdateServiceMock, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(activityStepResult, never()).setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
        Mockito.verify(activityUtils).persistStepDurations(eq(ACTIVITY_JOB_ID), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));

    }

    @Test
    public void testHandleTimeout_corruptedPkgsFound_butFailed() {
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(CORRUPTED_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(PRODUCT_NUMBER + ":" + PRODUCT_REVISION);
        final List<Map<String, Object>> jobProps = new ArrayList<Map<String, Object>>();
        jobProps.add(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(jobProps);
        when(mapMock.remove(anyMap())).thenReturn(true);
        when(localInstallationService.handleTimeout(any(JobActivityInfo.class))).thenReturn(activityStepResult);
        when(activityStepResult.getActivityResultEnum()).thenReturn(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        Assert.assertEquals(activityStepResult, objectUnderTest.handleTimeout(ACTIVITY_JOB_ID));
        verify(jobUpdateServiceMock, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(activityStepResult, never()).setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
        verify(activityUtils, times(1)).getActivityInfo(anyLong(), eq(RestoreInstallService.class));
        Mockito.verify(activityUtils).persistStepDurations(eq(ACTIVITY_JOB_ID), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testHandleTimeout_corruptedPkgsFound() {
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(CORRUPTED_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(PRODUCT_NUMBER + ":" + PRODUCT_REVISION);
        final List<Map<String, Object>> jobProps = new ArrayList<Map<String, Object>>();
        jobProps.add(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(jobProps);
        when(mapMock.remove(anyMap())).thenReturn(true);
        when(localInstallationService.handleTimeout(any(JobActivityInfo.class))).thenReturn(activityStepResult);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        Assert.assertEquals(activityStepResult, objectUnderTest.handleTimeout(ACTIVITY_JOB_ID));
        verify(jobUpdateServiceMock, times(1)).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(activityStepResult, never()).setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
        verify(activityUtils, times(1)).getActivityInfo(anyLong(), eq(RestoreInstallService.class));
    }

    @Test
    public void testHandleTimeout_corruptedPkgsFound_morePkgsLeft() {
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(CORRUPTED_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(PRODUCT_NUMBER + ":" + PRODUCT_REVISION + "|" + PRODUCT_NUMBER + ":" + PRODUCT_REVISION);
        final List<Map<String, Object>> jobProps = new ArrayList<Map<String, Object>>();
        jobProps.add(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(jobProps);
        when(mapMock.remove(anyMap())).thenReturn(true);
        when(localInstallationService.handleTimeout(any(JobActivityInfo.class))).thenReturn(activityStepResult);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        Assert.assertEquals(activityStepResult, objectUnderTest.handleTimeout(ACTIVITY_JOB_ID));
        verify(jobUpdateServiceMock, times(1)).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(activityStepResult, times(1)).setActivityResultEnum(ActivityStepResultEnum.REPEAT_EXECUTE);
    }

    @Test
    public void testProcessNotification_noRepeatation() {
        final List<Map<String, Object>> jobProps = new ArrayList<Map<String, Object>>();
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtils.getActivityJobId(notificationSubjectMock)).thenReturn(ACTIVITY_JOB_ID);
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(CORRUPTED_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(PRODUCT_NUMBER + "|" + PRODUCT_NUMBER);
        jobProps.add(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(jobProps);
        when(notificationSubjectMock.getFdn()).thenReturn(FDN);
        when(localInstallationService.processNotification(eq(notificationMock), any(JobActivityInfo.class))).thenReturn(processNotificationResult);
        when(activityUtils.unSubscribeToMoNotifications(Matchers.anyString(), Matchers.anyLong(), (JobActivityInfo) Matchers.any(JobActivityInfo.class))).thenReturn(true);
        when(processNotificationResult.get(ActivityConstants.ACTIVITY_RESULT)).thenReturn(JobResult.SUCCESS);
        when(processNotificationResult.get(ActivityConstants.ACTIVITY_STATUS)).thenReturn(true);
        when(jobUpdateServiceMock.addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList())).thenReturn(true);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobEnvironment.getActivityJobAttributes()).thenReturn(activityJobAttributes);

        objectUnderTest.processNotification(notificationMock);
        verify(localInstallationService, times(1)).processNotification(eq(notificationMock), any(JobActivityInfo.class));
        verify(jobUpdateServiceMock, times(1)).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(workflowInstanceNotifier, times(1)).sendActivate(anyString(), anyMap());
        verify(activityUtils, times(2)).getActivityInfo(anyLong(), eq(RestoreInstallService.class));
        Mockito.verify(activityAndNEJobProgressCalculator).updateActivityJobProgressPercentage(anyLong(), anyList(), anyList(), anyDouble());
    }

    @Test
    public void testProcessNotification_noUpdatesToProp() {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtils.getActivityJobId(notificationSubjectMock)).thenReturn(ACTIVITY_JOB_ID);
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(CORRUPTED_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(PRODUCT_NUMBER + "|" + PRODUCT_NUMBER);
        final List<Map<String, Object>> jobProps = new ArrayList<Map<String, Object>>();
        jobProps.add(mapMock);

        final Map<String, Object> jobState = new HashMap<>();
        jobState.put(ActivityConstants.ACTIVITY_STATUS, false);

        when(localInstallationService.processNotification(eq(notificationMock), any(JobActivityInfo.class))).thenReturn(jobState);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(jobProps);
        when(notificationSubjectMock.getFdn()).thenReturn(FDN);

        objectUnderTest.processNotification(notificationMock);
        verify(localInstallationService, times(1)).processNotification(eq(notificationMock), any(JobActivityInfo.class));
        verify(jobUpdateServiceMock, never()).addOrUpdateOrRemoveJobProperties(anyLong(), anyMap(), anyList());
        verify(workflowInstanceNotifier, never()).sendActivate(anyString(), anyMap());
        verify(activityUtils, times(1)).getActivityInfo(anyLong(), eq(RestoreInstallService.class));

    }

    @Test
    public void testProcessNotification() {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(notificationSubjectMock.getFdn()).thenReturn(FDN);
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA };
        when(upgradePackageService.getUpMoAttributesByFdn(FDN, attributeNames)).thenReturn(mapMock);
        when(mapMock.get(UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA)).thenReturn(mapMock);
        when(activityUtils.getProductId(anyString(), anyString())).thenReturn("");

        when(activityUtils.getActivityJobId(notificationSubjectMock)).thenReturn(ACTIVITY_JOB_ID);
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(CORRUPTED_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(PRODUCT_NUMBER + "|" + PRODUCT_NUMBER);
        final List<Map<String, Object>> jobProps = new ArrayList<Map<String, Object>>();
        jobProps.add(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(jobProps);

        final Map<String, Object> jobState = new HashMap<>();
        jobState.put(ActivityConstants.ACTIVITY_STATUS, false);

        when(localInstallationService.processNotification(notificationMock, eq(any(JobActivityInfo.class)))).thenReturn(jobState);

        objectUnderTest.processNotification(notificationMock);
        verify(localInstallationService, times(1)).processNotification(eq(notificationMock), any(JobActivityInfo.class));
        verify(workflowInstanceNotifier, times(0)).sendActivate(anyString(), anyMap());
        verify(activityUtils, times(1)).getActivityInfo(anyLong(), eq(RestoreInstallService.class));
    }

    @Test
    public void testProcessNotificationJobFailed() {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtils.getActivityJobId(notificationSubjectMock)).thenReturn(ACTIVITY_JOB_ID);
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(CORRUPTED_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(PRODUCT_NUMBER + "|" + PRODUCT_NUMBER);

        final List<Map<String, Object>> jobProps = new ArrayList<Map<String, Object>>();
        jobProps.add(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(jobProps);

        final Map<String, Object> jobState = new HashMap<>();
        jobState.put(ActivityConstants.ACTIVITY_STATUS, true);

        jobState.put(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED);

        when(localInstallationService.processNotification(notificationMock, eq(any(JobActivityInfo.class)))).thenReturn(jobState);
        when(activityUtils.unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject())).thenReturn(true);
        when(notificationSubjectMock.getFdn()).thenReturn(FDN);
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA };
        when(upgradePackageService.getUpMoAttributesByFdn(FDN, attributeNames)).thenReturn(mapMock);
        when(mapMock.get(UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA)).thenReturn(mapMock);
        when(activityUtils.getProductId(anyString(), anyString())).thenReturn("");
        when(jobUpdateServiceMock.addOrUpdateOrRemoveJobProperties(Matchers.anyLong(), Matchers.anyMap(), Matchers.anyList())).thenReturn(true);

        objectUnderTest.processNotification(notificationMock);
        verify(localInstallationService, times(1)).processNotification(eq(notificationMock), any(JobActivityInfo.class));
        verify(jobUpdateServiceMock, never()).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList(), anyDouble());
        verify(workflowInstanceNotifier, times(1)).sendActivate(anyString(), anyMap());
    }

    @Test
    public void testProcessNotificationWithDuplicateNotification() {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtils.getActivityJobId(notificationSubjectMock)).thenReturn(ACTIVITY_JOB_ID);
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROP_KEY)).thenReturn(CORRUPTED_PKGS);
        when(mapMock.get(ActivityConstants.JOB_PROP_VALUE)).thenReturn(PRODUCT_NUMBER + "|" + PRODUCT_NUMBER);

        final List<Map<String, Object>> jobProps = new ArrayList<Map<String, Object>>();
        jobProps.add(mapMock);
        when(mapMock.get(ActivityConstants.JOB_PROPERTIES)).thenReturn(jobProps);

        final Map<String, Object> jobState = new HashMap<>();
        jobState.put(ActivityConstants.ACTIVITY_STATUS, true);

        jobState.put(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED);

        when(localInstallationService.processNotification(notificationMock, eq(any(JobActivityInfo.class)))).thenReturn(jobState);
        when(activityUtils.unSubscribeToMoNotifications(anyString(), anyLong(), (JobActivityInfo) anyObject())).thenReturn(false);

        objectUnderTest.processNotification(notificationMock);
        verify(localInstallationService, times(1)).processNotification(eq(notificationMock), any(JobActivityInfo.class));
        verify(workflowInstanceNotifier, times(0)).sendActivate(anyString(), anyMap());
    }

    @Test
    public void testProcessNotificationWithNonAVCEvent() {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        objectUnderTest.processNotification(notificationMock);
        verify(activityUtils, never()).getModifiedAttributes(notificationMock.getDpsDataChangedEvent());
    }

    @Before
    public void mockJobEnvironment() {
        when(activityUtils.getJobEnvironment(anyLong())).thenReturn(jobEnvironment);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getNodeName()).thenReturn("ERBS01_Node");
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
    }

    @BeforeTest
    public void mockJobActivityInfo() {
        when(jobActivityInfoMock.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
    }
}
