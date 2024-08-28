package com.ericsson.oss.services.shm.es.impl.minilink.backup;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.*;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigLoadCommand.CONFIG_UPLOAD;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJBException;

import org.junit.Test;
import org.mockito.Mock;

import com.ericsson.cds.cdi.support.rule.*;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.impl.minilink.common.*;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.*;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;

public class BackupServiceTest extends BackupRestoreTestBase {

    long activityJobId = 123;
    long neJobId = 456;
    long mainJobId = 789;

    String nodeName = "nodeName";
    String businessKey = "businessKey";
    String domainName = "domainName";
    String backupName = "backupName";
    String backupType = "backupType";
    String parentNodeName = "parentNodeName";
    String platformType = "MINI-LINK-Indoor";
    String valueValue = "true";
    String keyValue = "value";
    String keyKey = "key";

    @ObjectUnderTest
    private BackupService backupService;

    @MockedImplementation
    private DataBucket liveBucket;

    @MockedImplementation
    private ManagedObject managedObject;

    @Mock
    private RetryManager retryManager;

    @MockedImplementation
    private DpsRetryPolicies dpsPolicies;

    @Mock
    private RetryPolicy retryPolicy;

    @ImplementationInstance
    private final JobConfigurationServiceRetryProxy jobConfigServiceRetryProxy = new JobConfigurationServiceRetryProxy() {
        @Override
        public Map<String, Object> getActivityJobAttributes(final long activityJobId) {
            return new HashMap<>();
        }

        @Override
        public Map<String, Object> getNeJobAttributes(final long neJobId) {
            return new HashMap<>();
        }

        @Override
        public List<Long> getJobPoIdsFromParentJobId(final long neJobPoId, final String typeOfJob, final String restrictionAttribute) {
            return new ArrayList();
        }

        @Override
        public Map<String, Object> getMainJobAttributes(final long mainJobId) {
            final Map<String, String> jobPropertiesMap = new HashMap<>();
            final List<Map<String, String>> jobProperties = new ArrayList<>();
            final Map<String, Object> neTypeJobPropertiesMap = new HashMap<>();
            final List<Map<String, Object>> neTypeJobProperties = new ArrayList<>();
            final List<Map<String, Object>> schedulePropertiesList = new ArrayList<>();
            final Map<String, Object> mainJobAttributes = new HashMap<>();
            final Map<String, Object> jobConfigurationDetails = new HashMap<>();
            final Map<String, Object> mainSchedule = new HashMap<>();
            jobPropertiesMap.put(keyValue, valueValue);
            jobPropertiesMap.put(keyKey, GENERATE_BACKUP_NAME);
            jobProperties.add(jobPropertiesMap);
            neTypeJobPropertiesMap.put("jobProperties", jobProperties);
            neTypeJobProperties.add(neTypeJobPropertiesMap);
            mainSchedule.put("scheduleAttributes", schedulePropertiesList);
            jobConfigurationDetails.put("neTypeJobProperties", neTypeJobProperties);
            jobConfigurationDetails.put("mainSchedule", mainSchedule);
            mainJobAttributes.put("jobConfigurationDetails", jobConfigurationDetails);
            return mainJobAttributes;
        }

        @Override
        public List<Map<String, Object>> getProjectedAttributes(final String namespace, final String type, final Map<Object, Object> restrictions, final List<String> reqdAttributes) {
            return new ArrayList<>();
        }

        @Override
        public Map<String, Object> getPOAttributes(final long poId) {
            return new HashMap<>();
        }

        @Override
        public List<Map<String, Object>> getActivityJobAttributesByNeJobId(long neJobId, Map<String, Object> restrictions) {
            // TODO Auto-generated method stub
            return new ArrayList<>();
        }
    };

    @ImplementationInstance
    private final NeJobStaticDataProvider neJobStaticDataProvider = new NeJobStaticDataProvider() {

        @Override
        public NEJobStaticData getNeJobStaticData(final long activityJobId, final String capability) {
            return new NEJobStaticData(neJobId, mainJobId, nodeName, businessKey, platformType, new Date().getTime(), parentNodeName);
        }

        @Override
        public void updateNeJobStaticDataCache(final long activityJobId, final String platformCapbility, final long activityStartTime) throws JobDataNotFoundException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear(final long activityJobId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void put(final long activityJobId, final NEJobStaticData neJobStaticData) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getActivityStartTime(final long activityJobId) {
            return 0;
        }
    };

    protected void sendConfigStatusChangeNotification() {
        backupService.processNotification(getAvcNotificationMessage());
    }

    private void getOssModelIdentity() {
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(NETWORKELEMENT + NODE_NAME)).thenReturn(managedObject);
    }

    @Test
    public void testPrecheckSucceedsInValidState() {
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_OK.getStatusValue());
        final ActivityStepResult precheckResult = backupService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
    }

    @Test
    public void testPrecheckFailsInInvalidState() {
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOADING.getStatusValue());
        final ActivityStepResult precheckResult = backupService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
    }

    @Test
    public void testExecuteSetsUpNodeProperly() {
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_OK.getStatusValue());
        RetryPolicy policy = RetryPolicy.builder().attempts(3).waitInterval(10, TimeUnit.SECONDS).exponentialBackoff(1.0).retryOn(EJBException.class).build();
        getOssModelIdentity();
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        when(dpsPolicies.getDpsMoActionRetryPolicy()).thenReturn(policy);
        backupService.execute(ACTIVITY_JOB_ID);
        assertTrue("Backup directory does not exist", existingResources.contains(PATH_ON_SERVER + NODE_NAME));
        assertEquals("xfConfigFileName not set properly", NODE_NAME + SLASH + EXPECTED_BACKUP_FILE_NAME, xfConfigLoadObjectsMo.getAttribute(XF_CONFIG_FILE_NAME));
        assertEquals("xfConfigLoadActiveFTP not set properly", new Integer(FTP_TABLE_ENTRY_INDEX), xfDcnFtpMo.getAttribute(XF_CONFIG_LOAD_ACTIVE_FTP));
        assertEquals("xfConfigStatus not set properly", CONFIG_UP_LOADING.getStatusValue(), xfConfigLoadObjectsMo.getAttribute(XF_CONFIG_STATUS));
        assertEquals("xfConfigLoadCommand not set properly", CONFIG_UPLOAD.getStatusValue(), xfConfigLoadObjectsMo.getAttribute(XF_CONFIG_LOAD_COMMAND));
        assertSubscribedToXfConfigLoadObjects();
        assertLog("Executing \"backup\" activity on backup file = \"backup.cfg\".");
    }

    @Test
    public void testSuccessfulBackupUpload() {
        getOssModelIdentity();
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_OK.getStatusValue());
        existingResources.add(BACKUP_FILE_PATH_PREFIX + EXPECTED_BACKUP_FILE_NAME);
        sendConfigStatusChangeNotification();
        assertActivitySuccess();
    }

    @Test
    public void testErrorNotificationChangeHandledProperly() {
        getOssModelIdentity();
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, ERROR_FILE_STORAGE.getStatusValue());
        sendConfigStatusChangeNotification();
        assertActivityFailure();
    }

    @Test
    public void testSuccessNotificationWithMissingBackupFileFails() {
        getOssModelIdentity();
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_OK.getStatusValue());
        sendConfigStatusChangeNotification();
        assertActivityFailure();
    }

    @Test
    public void testFalseAttributeChangeNotificationIgnored() {
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOADING.getStatusValue());
        getOssModelIdentity();
        sendConfigStatusChangeNotification();
        assertTrue(jobUpdateServiceStub.getJobProperties().isEmpty());
    }

    @Test
    public void testNonAvcAttributeChangeNotificationIgnored() {
        backupService.processNotification(new Notification() {
            @Override
            public DpsDataChangedEvent getDpsDataChangedEvent() {
                return null;
            }

            @Override
            public NotificationSubject getNotificationSubject() {
                return null;
            }

            @Override
            public NotificationEventTypeEnum getNotificationEventType() {
                return null;
            }
        });

        assertTrue(jobUpdateServiceStub.getJobProperties().isEmpty());
    }

    @Test
    public void testSuccessfulTimeout() {
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        existingResources.add(BACKUP_FILE_PATH_PREFIX + EXPECTED_BACKUP_FILE_NAME);
        final ActivityStepResult result = backupService.handleTimeout(ACTIVITY_JOB_ID);
        assertEquals(result.getActivityResultEnum(), ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS);
        assertActivitySuccess();
    }

    @Test
    public void testUnsuccessfulTimeout() {
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        final ActivityStepResult result = backupService.handleTimeout(ACTIVITY_JOB_ID);
        assertEquals(result.getActivityResultEnum(), ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        assertActivityFailure();
    }

    @Test
    public void testPrecheckExceptionHandling() {
        dpsStub.setDpsFail(true);
        getOssModelIdentity();
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        final ActivityStepResult result = backupService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, result.getActivityResultEnum());
    }

    @Test
    public void testExecuteExceptionHandlingFTP() {
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_OK.getStatusValue());
        getOssModelIdentity();
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn(UnsecureFTPModelIdentity.TN11B.getOssModelIdentity());
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        makeSmrsFail = true;
        backupService.execute(ACTIVITY_JOB_ID);
        assertActivityFailure();
    }

    @Test
    public void testExecuteExceptionHandling() {
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_OK.getStatusValue());
        getOssModelIdentity();
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        makeSmrsFail = true;
        backupService.execute(ACTIVITY_JOB_ID);
        assertActivityFailure();
    }

    @Test
    public void testProcessNotificationExceptionHandling() throws FTPRetryException {
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_FAILED.getStatusValue());
        getOssModelIdentity();
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        makeSmrsFail = true;
        sendConfigStatusChangeNotification();
        assertActivityFailure();
    }

    @Test
    public void testHandleTimeout() {
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        final ActivityStepResult result = backupService.handleTimeout(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, result.getActivityResultEnum());
        assertActivityFailure();
    }

    @Test
    public void testCancel() {
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        final ActivityStepResult result = backupService.cancel(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, result.getActivityResultEnum());
        assertUnsubscribedFromXfConfigLoadObjects();
    }

    @Test
    public void testCancelTimeout() {
        backupService.cancelTimeout(ACTIVITY_JOB_ID, true);
    }

}