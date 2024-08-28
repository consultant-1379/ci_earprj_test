package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.licensing;

import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_CORRUPT_SIGNATURE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_NO_SPACE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_NO_STORAGE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_SEQUENCE_NUMBER;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_SYSTEM_ERROR;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_TRANSFER_FAILED;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ERROR_XML_SYNTAX;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LICENSE_FILEPATH;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.LKF_CONFIGURING;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.NOOP;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.UNKNOWN_FINGERPRINT;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.UNKNOWN_FINGERPRINT_METHOD;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.UNKNOWN_SIGNATURE_TYPE;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.UNKNOWN_VERSION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.MiniLinkOutdoorJobUtil;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants;
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.DPSUtils;
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(MockitoJUnitRunner.class)
public class InstallLicenseKeyFileServiceTest {

    public static final long ACTIVITY_JOB_ID = 123;
    public static final String NODE_NAME = "ML-TN";
    public static final String LICENCE_NAME_TEST = "licence";

    @InjectMocks
    private InstallLicenseKeyFileService installLicenseKeyFileService;

    @Mock
    private MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private EventSender<MediationTaskRequest> licenceJobTaskRequest;

    @Mock
    private SHMCommonCallBackNotificationJobProgressBean notification;

    @Mock
    private SHMCommonCallbackNotification commonNotification;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private FdnNotificationSubject fdnNotificationSubject;

    @Mock
    private ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private DPSUtils dpsUtils;

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallLicenseKeyFileServiceTest.class);

    private static final String EXCEPTION = "Exception caught :: {}";
    private static final Long NE_JOB_ID = 123L;
    private static final String LICENCE = "LICENCE";
    private static final String TEST_SUBSCRIPTION = "testSubscription";

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckFailedSkipExecution() {
        try {
            when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvironment);
            when(jobEnvironment.getNodeName()).thenReturn(NODE_NAME);
            when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble()))
                    .thenReturn(true);
            when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
            when(neJobStaticData.getNeJobId()).thenReturn(NE_JOB_ID);
            when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
            when(activityJobTBACValidator.validateTBAC(ACTIVITY_JOB_ID, neJobStaticData, jobStaticData, ActivityConstants.INSTALL_LICENSE_ACTIVITY))
                    .thenReturn(false);
            doNothing().when(activityAndNEJobProgressCalculator).updateNEJobProgressPercentage(NE_JOB_ID);
            final ActivityStepResult precheckResult = installLicenseKeyFileService.precheck(ACTIVITY_JOB_ID);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        } catch (JobDataNotFoundException | MoNotFoundException e) {
            LOGGER.info(EXCEPTION, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckInventorySupervisionDisabled() {
        try {
            when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvironment);
            when(jobEnvironment.getNodeName()).thenReturn(NODE_NAME);
            when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble()))
                    .thenReturn(true);
            when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
            when(neJobStaticData.getNeJobId()).thenReturn(NE_JOB_ID);
            when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
            when(activityJobTBACValidator.validateTBAC(ACTIVITY_JOB_ID, neJobStaticData, jobStaticData, ActivityConstants.INSTALL_LICENSE_ACTIVITY))
                    .thenReturn(true);
            when(dpsUtils.isInventorySupervisionEnabled(NODE_NAME)).thenReturn(false);
            doNothing().when(activityAndNEJobProgressCalculator).updateNEJobProgressPercentage(NE_JOB_ID);
            final ActivityStepResult precheckResult = installLicenseKeyFileService.precheck(ACTIVITY_JOB_ID);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        } catch (JobDataNotFoundException | MoNotFoundException e) {
            LOGGER.info(EXCEPTION, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckSucceedsInValidState() {
        try {
            when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvironment);
            when(jobEnvironment.getNodeName()).thenReturn(NODE_NAME);
            when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble()))
                    .thenReturn(true);
            when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
            when(neJobStaticData.getNeJobId()).thenReturn(NE_JOB_ID);
            when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
            when(activityJobTBACValidator.validateTBAC(ACTIVITY_JOB_ID, neJobStaticData, jobStaticData, ActivityConstants.INSTALL_LICENSE_ACTIVITY))
                    .thenReturn(true);
            when(dpsUtils.isInventorySupervisionEnabled(NODE_NAME)).thenReturn(true);
            doNothing().when(activityAndNEJobProgressCalculator).updateNEJobProgressPercentage(NE_JOB_ID);
            final ActivityStepResult precheckResult = installLicenseKeyFileService.precheck(ACTIVITY_JOB_ID);
            assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        } catch (JobDataNotFoundException | MoNotFoundException e) {
            LOGGER.info(EXCEPTION, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute() {
        Map<String, Object> poAttributesMap = new HashMap<>();
        poAttributesMap.put(ShmConstants.NE_JOB_ID, NE_JOB_ID);
        Map<String, String> licenseFile = new HashMap<>();
        licenseFile.put(LICENSE_FILEPATH, "licenseFileName");
        when(activityUtils.getPoAttributes(ACTIVITY_JOB_ID)).thenReturn(poAttributesMap);
        when(activityUtils.createNewLogEntry(String.format(JobLogConstants.EXECUTING, ActivityConstants.INSTALL_LICENSE), JobLogLevel.INFO.getLogLevel())).thenReturn(
                poAttributesMap);
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble()))
                .thenReturn(true);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(NODE_NAME);
        when(miniLinkOutdoorJobUtil.getSubscriptionKey(NODE_NAME, LICENCE)).thenReturn(TEST_SUBSCRIPTION);
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, InstallLicenseKeyFileService.class)).thenReturn(jobActivityInfo);
        when(activityUtils.subscribeToMoNotifications(TEST_SUBSCRIPTION, ACTIVITY_JOB_ID, jobActivityInfo)).thenReturn(fdnNotificationSubject);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString())).thenReturn(licenseFile);
        installLicenseKeyFileService.execute(ACTIVITY_JOB_ID);
    }

    @Test
    public void testProcessNotificationLkfConfiguring() throws JobDataNotFoundException {
        when(notification.getCommonNotification()).thenReturn(commonNotification);
        when(commonNotification.getFdn()).thenReturn("fdn");
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, ACTIVITY_JOB_ID);
        additionalAttributes.put(Constants.ACTIVITY_NAME, LICENCE_NAME_TEST);
        when(commonNotification.getAdditionalAttributes()).thenReturn(additionalAttributes);
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, InstallLicenseKeyFileService.class)).thenReturn(jobActivityInfo);
        when(commonNotification.getState()).thenReturn(LKF_CONFIGURING);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(NODE_NAME);
        when(miniLinkOutdoorJobUtil.getSubscriptionKey(NODE_NAME, LICENCE_NAME_TEST)).thenReturn(TEST_SUBSCRIPTION);
        when(jobActivityInfo.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        installLicenseKeyFileService.processNotification(notification);
    }

    @Test
    public void testProcessNotificationAllRemainingOps() throws JobDataNotFoundException {
        when(notification.getCommonNotification()).thenReturn(commonNotification);
        when(commonNotification.getFdn()).thenReturn("fdn");
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, ACTIVITY_JOB_ID);
        additionalAttributes.put(Constants.ACTIVITY_NAME, LICENCE_NAME_TEST);
        when(commonNotification.getAdditionalAttributes()).thenReturn(additionalAttributes);
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, InstallLicenseKeyFileService.class)).thenReturn(jobActivityInfo);
        when(commonNotification.getState()).thenReturn(NOOP);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(NODE_NAME);
        when(miniLinkOutdoorJobUtil.getSubscriptionKey(NODE_NAME, LICENCE_NAME_TEST)).thenReturn(TEST_SUBSCRIPTION);
        when(jobActivityInfo.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        installLicenseKeyFileService.processNotification(notification);
        when(commonNotification.getState()).thenReturn(ERROR_NO_STORAGE);
        installLicenseKeyFileService.processNotification(notification);
        when(commonNotification.getState()).thenReturn(ERROR_NO_SPACE);
        installLicenseKeyFileService.processNotification(notification);
        when(commonNotification.getState()).thenReturn(UNKNOWN_VERSION);
        installLicenseKeyFileService.processNotification(notification);
        when(commonNotification.getState()).thenReturn(UNKNOWN_SIGNATURE_TYPE);
        installLicenseKeyFileService.processNotification(notification);
        when(commonNotification.getState()).thenReturn(UNKNOWN_FINGERPRINT_METHOD);
        installLicenseKeyFileService.processNotification(notification);
        when(commonNotification.getState()).thenReturn(UNKNOWN_FINGERPRINT);
        installLicenseKeyFileService.processNotification(notification);
        when(commonNotification.getState()).thenReturn(ERROR_CORRUPT_SIGNATURE);
        installLicenseKeyFileService.processNotification(notification);
        when(commonNotification.getState()).thenReturn(ERROR_TRANSFER_FAILED);
        installLicenseKeyFileService.processNotification(notification);
        when(commonNotification.getState()).thenReturn(ERROR_SEQUENCE_NUMBER);
        installLicenseKeyFileService.processNotification(notification);
        when(commonNotification.getState()).thenReturn(ERROR_XML_SYNTAX);
        installLicenseKeyFileService.processNotification(notification);
        when(commonNotification.getState()).thenReturn(ERROR_SYSTEM_ERROR);
        installLicenseKeyFileService.processNotification(notification);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleTimeout() throws JobDataNotFoundException {
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(NODE_NAME);
        when(miniLinkOutdoorJobUtil.getSubscriptionKey(NODE_NAME, LICENCE)).thenReturn(TEST_SUBSCRIPTION);
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, InstallLicenseKeyFileService.class)).thenReturn(jobActivityInfo);
        when(activityUtils.subscribeToMoNotifications(TEST_SUBSCRIPTION, ACTIVITY_JOB_ID, jobActivityInfo)).thenReturn(fdnNotificationSubject);
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble()))
                .thenReturn(true);
        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNeJobId()).thenReturn(NE_JOB_ID);
        doNothing().when(activityAndNEJobProgressCalculator).updateNEJobProgressPercentage(NE_JOB_ID);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        when(activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)).thenReturn(activityStepResult);
        final ActivityStepResult result = installLicenseKeyFileService.handleTimeout(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, result.getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancel() throws JobDataNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(ACTIVITY_JOB_ID, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble()))
                .thenReturn(true);
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, InstallLicenseKeyFileService.class)).thenReturn(jobActivityInfo);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobEnvironment);
        when(jobEnvironment.getNodeName()).thenReturn(NODE_NAME);
        when(miniLinkOutdoorJobUtil.getSubscriptionKey(NODE_NAME, LICENCE)).thenReturn(TEST_SUBSCRIPTION);
        when(activityUtils.unSubscribeToMoNotifications(TEST_SUBSCRIPTION, ACTIVITY_JOB_ID, jobActivityInfo)).thenReturn(true);
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.EXECUTION_FAILED);
        final ActivityStepResult result = installLicenseKeyFileService.cancel(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, result.getActivityResultEnum());
    }

}