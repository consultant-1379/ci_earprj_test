package com.ericsson.oss.services.shm.es.impl.minilink.restore;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.CM_FUNCTION;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.NETWORKELEMENT;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.OSSMODELIDENTITY;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.SLASH;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.SYNC_STATUS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_FILE_NAME;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_LOAD_ACTIVE_FTP;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_LOAD_COMMAND;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_LOAD_OBJECTS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_STATUS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.SyncStatus.SYNCHRONIZED;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.SyncStatus.UNSYNCHRONIZED;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigLoadCommand.CONFIG_DOWNLOAD;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_DOWN_LOADING;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_DOWN_LOAD_OK;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_UP_LOAD_FAILED;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_UP_LOAD_OK;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.ERROR_FILE_STORAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import javax.ejb.EJBException;

import org.junit.Test;
import org.mockito.Mock;

import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.impl.minilink.backup.UnsecureFTPModelIdentity;
import com.ericsson.oss.services.shm.es.impl.minilink.common.ActivityUtilsStub;
import com.ericsson.oss.services.shm.es.impl.minilink.common.BackupRestoreTestBase;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;

public class DownloadBackupServiceTest extends BackupRestoreTestBase {

    @ObjectUnderTest
    private DownloadBackupService downloadBackupService;

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

    protected final ManagedObject cmFunctionMo = dpsStub.createManagedObjectMock(CM_FUNCTION);

    protected void sendConfigStatusChangeNotification() {
        downloadBackupService.processNotification(getAvcNotificationMessage());
    }

    private void getOssModelIdentity() {
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(NETWORKELEMENT + NODE_NAME)).thenReturn(managedObject);
    }

    @Test
    public void testPrecheckSucceedsInValidState() {
        cmFunctionMo.setAttribute(SYNC_STATUS, SYNCHRONIZED.getStatusValue());
        ActivityStepResult precheckResult = downloadBackupService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        assertLog("Precheck for \"download\" is successful.");
    }

    @Test
    public void testPrecheckFailsInInvalidState() {
        cmFunctionMo.setAttribute(SYNC_STATUS, UNSYNCHRONIZED.getStatusValue());
        ActivityStepResult precheckResult = downloadBackupService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        assertLog("Unable to proceed \"download\" activity because \"Node is not synchronized!\".");
    }

    @Test
    public void testPrecheckException() {
        cmFunctionMo.setAttribute(SYNC_STATUS, 12);
        ActivityStepResult precheckResult = downloadBackupService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        assertLog("Unable to proceed \"download\" activity because \"java.lang.Integer cannot be cast to java.lang.String\".");
    }

    @Test
    public void testExecuteSetsUpNodeProperly() {
        RetryPolicy policy = RetryPolicy.builder().attempts(3).waitInterval(10, TimeUnit.SECONDS)
                .exponentialBackoff(1.0).retryOn(EJBException.class).build();
        when(dpsPolicies.getDpsMoActionRetryPolicy()).thenReturn(policy);
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_OK.getStatusValue());
        getOssModelIdentity();
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_OK.getStatusValue());
        downloadBackupService.execute(ACTIVITY_JOB_ID);
        assertEquals("xfConfigFileName not set properly", NODE_NAME + SLASH + EXPECTED_BACKUP_FILE_NAME,
                xfConfigLoadObjectsMo.getAttribute(XF_CONFIG_FILE_NAME));
        assertEquals("xfConfigLoadActiveFTP not set properly", new Integer(FTP_TABLE_ENTRY_INDEX), xfDcnFtpMo.getAttribute(XF_CONFIG_LOAD_ACTIVE_FTP));
        assertEquals("xfConfigLoadCommand not set properly", CONFIG_DOWNLOAD.getStatusValue(),
                xfConfigLoadObjectsMo.getAttribute(XF_CONFIG_LOAD_COMMAND));
        assertEquals("xfConfigStatus not set properly", CONFIG_DOWN_LOADING.getStatusValue(), xfConfigLoadObjectsMo.getAttribute(XF_CONFIG_STATUS));
        assertSubscribedToXfConfigLoadObjects();
        assertLog("Executing \"download\" activity on backup file = \"backup.cfg\".");
    }

    @Test
    public void testExecuteSetsUpNodeProperlyFTP() {
        RetryPolicy policy = RetryPolicy.builder().attempts(3).waitInterval(10, TimeUnit.SECONDS)
                .exponentialBackoff(1.0).retryOn(EJBException.class).build();
        when(dpsPolicies.getDpsMoActionRetryPolicy()).thenReturn(policy);
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_OK.getStatusValue());
        getOssModelIdentity();
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn(UnsecureFTPModelIdentity.TN11B.getOssModelIdentity());
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_OK.getStatusValue());
        downloadBackupService.execute(ACTIVITY_JOB_ID);
        assertEquals("xfConfigFileName not set properly", NODE_NAME + SLASH + EXPECTED_BACKUP_FILE_NAME,
                xfConfigLoadObjectsMo.getAttribute(XF_CONFIG_FILE_NAME));
        assertEquals("xfConfigLoadCommand not set properly", CONFIG_DOWNLOAD.getStatusValue(),
                xfConfigLoadObjectsMo.getAttribute(XF_CONFIG_LOAD_COMMAND));
        assertEquals("xfConfigStatus not set properly", CONFIG_DOWN_LOADING.getStatusValue(), xfConfigLoadObjectsMo.getAttribute(XF_CONFIG_STATUS));
        assertSubscribedToXfConfigLoadObjects();
        assertLog("Executing \"download\" activity on backup file = \"backup.cfg\".");
    }

    @Test
    public void testNonAvcAttributeChangeNotificationIgnored() {
        downloadBackupService.processNotification(new Notification() {
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
    public void testSuccessfulDownloadBackup() {
        getOssModelIdentity();
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_DOWN_LOAD_OK.getStatusValue());
        sendConfigStatusChangeNotification();
        assertActivitySuccess();
        assertLog("Configuration is downloaded.");
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
    public void testFalseAttributeChangeNotificationIgnored() {
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_DOWN_LOADING.getStatusValue());
        getOssModelIdentity();
        sendConfigStatusChangeNotification();
        assertTrue(jobUpdateServiceStub.getJobProperties().isEmpty());
    }

    @Test
    public void testProcessNotificationExceptionHandling() {
        makeSmrsFail = true;
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_FAILED.getStatusValue());
        getOssModelIdentity();
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        sendConfigStatusChangeNotification();
        assertActivityFailure();
    }

    @Test
    public void testProcessNotificationExceptionHandlingFTP() {
        makeSmrsFail = true;
        getOssModelIdentity();
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_FAILED.getStatusValue());
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn(UnsecureFTPModelIdentity.TN11B.getOssModelIdentity());
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        sendConfigStatusChangeNotification();
        assertActivityFailure();
    }

    @Test
    public void testHandleTimeout() {
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        final ActivityStepResult result = downloadBackupService.handleTimeout(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, result.getActivityResultEnum());
        assertActivityFailure();
    }

    @Test
    public void testCancel() {
        ((ActivityUtilsStub) activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        final ActivityStepResult result = downloadBackupService.cancel(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, result.getActivityResultEnum());
        assertUnsubscribedFromXfConfigLoadObjects();
    }

    @Test
    public void testCancelTimeout() {
        downloadBackupService.cancelTimeout(ACTIVITY_JOB_ID, true);
    }

}
