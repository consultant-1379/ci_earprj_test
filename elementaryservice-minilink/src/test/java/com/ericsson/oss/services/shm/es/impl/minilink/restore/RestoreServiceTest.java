package com.ericsson.oss.services.shm.es.impl.minilink.restore;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.CM_FUNCTION;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_ACCEPT;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_LOAD_OBJECTS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_STATUS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_NE_RESTART_COMMANDS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_NE_RESTART_OBJECTS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_DOWN_LOAD_OK;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_UP_LOADING;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_UP_LOAD_OK;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.ERROR_FILE_STORAGE;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfNERestartCommands.COLD_RESTART;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfNERestartCommands.WARM_RESTART;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.NETWORKELEMENT;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.OSSMODELIDENTITY;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_DOWN_LOADING;
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
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;

public class RestoreServiceTest extends BackupRestoreTestBase {

    @ObjectUnderTest
    private RestoreService restoreService;

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
    protected final ManagedObject xfNeRestartObjects = dpsStub.createManagedObjectMock(XF_NE_RESTART_OBJECTS);

    protected void sendConfigStatusChangeNotification() {
        restoreService.processNotification(getAvcNotificationMessage());
    }

    @Test
    public void testPrecheckSucceedsInValidState() {
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(NETWORKELEMENT+ NODE_NAME)).thenReturn(managedObject);
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_DOWN_LOAD_OK.getStatusValue());
        ActivityStepResult precheckResult = restoreService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        assertLog("Precheck for \"restore\" is successful.");
    }

    @Test
    public void testPrecheckSucceedsInValidStateFTP() {
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(NETWORKELEMENT+ NODE_NAME)).thenReturn(managedObject);
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn(UnsecureFTPModelIdentity.TN11B.getOssModelIdentity());
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_DOWN_LOAD_OK.getStatusValue());
        ActivityStepResult precheckResult = restoreService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        assertLog("Precheck for \"restore\" is successful.");
    }

    @Test
    public void testPrecheckFailsInInvalidStateFTP() {
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(NETWORKELEMENT+ NODE_NAME)).thenReturn(managedObject);
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn(UnsecureFTPModelIdentity.TN11B.getOssModelIdentity());
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_DOWN_LOADING.getStatusValue());
        ActivityStepResult precheckResult = restoreService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        assertLog("Unable to proceed \"restore\" activity because \"configDownLoadOK is not set on node\".");
    }

    @Test
    public void testPrecheckFailsInInvalidState() {
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(NETWORKELEMENT+ NODE_NAME)).thenReturn(managedObject);
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_DOWN_LOADING.getStatusValue());
        ActivityStepResult precheckResult = restoreService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        assertLog("Unable to proceed \"restore\" activity because \"configDownLoadOK is not set on node\".");
    }

    @Test
    public void testPrecheckExceptionFTP() {
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(NETWORKELEMENT+ NODE_NAME)).thenReturn(managedObject);
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn(UnsecureFTPModelIdentity.TN11B.getOssModelIdentity());
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, 12);
        ActivityStepResult precheckResult = restoreService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        assertLog("Unable to proceed \"restore\" activity because \"java.lang.Integer cannot be cast to java.lang.String\".");
    }

    @Test
    public void testPrecheckException() {
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(NETWORKELEMENT + NODE_NAME)).thenReturn(managedObject);
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, 12);
        ActivityStepResult precheckResult = restoreService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, precheckResult.getActivityResultEnum());
        assertLog("Unable to proceed \"restore\" activity because \"java.lang.Integer cannot be cast to java.lang.String\".");
    }

    @Test
    public void testExecuteSetsUpNodeProperly() {
        RetryPolicy policy = RetryPolicy.builder().attempts(3).waitInterval(10, TimeUnit.SECONDS)
                .exponentialBackoff(1.0).retryOn(EJBException.class).build();
        when(dpsPolicies.getDpsMoActionRetryPolicy()).thenReturn(policy);
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_OK.getStatusValue());
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_ACCEPT, MiniLinkConstants.xfConfigAccept.acceptRMM.toString());
        xfNeRestartObjects.setAttribute(XF_NE_RESTART_COMMANDS, COLD_RESTART.getStatusValue());
        restoreService.execute(ACTIVITY_JOB_ID);
        assertEquals("xfConfigAccept not set properly", MiniLinkConstants.xfConfigAccept.acceptFTP.toString(), xfConfigLoadObjectsMo.getAttribute(XF_CONFIG_ACCEPT));
        assertEquals("xfNERestartCommands not set properly", WARM_RESTART.getStatusValue(), xfNeRestartObjects.getAttribute(XF_NE_RESTART_COMMANDS));
        assertEquals("xfConfigStatus not set properly", CONFIG_UP_LOADING.getStatusValue(), xfConfigLoadObjectsMo.getAttribute(XF_CONFIG_STATUS));
        assertSubscribedToXfConfigLoadObjects();
        assertLog("Executing \"restore\" activity on backup file = \"backup.cfg\".");
        assertLog("Node is restarting.");
    }

    @Test
    public void testNonAvcAttributeChangeNotificationIgnored() {
        restoreService.processNotification(new Notification() {
            @Override public DpsDataChangedEvent getDpsDataChangedEvent() {
                return null;
            }

            @Override public NotificationSubject getNotificationSubject() {
                return null;
            }

            @Override public NotificationEventTypeEnum getNotificationEventType() {
                return null;
            }
        });

        assertTrue(jobUpdateServiceStub.getJobProperties().isEmpty());
    }

    @Test
    public void testSuccessfulRestore() {
        ((ActivityUtilsStub)activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_DOWN_LOAD_OK.getStatusValue());
        sendConfigStatusChangeNotification();
        assertActivitySuccess();
        assertLog("Node restarted successfully.");
    }

    @Test
    public void testErrorNotificationChangeHandledProperly() {
        ((ActivityUtilsStub)activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, ERROR_FILE_STORAGE.getStatusValue());
        sendConfigStatusChangeNotification();
        assertActivitySuccess();
    }

    @Test
    public void testFalseAttributeChangeNotificationIgnored() {
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOADING.getStatusValue());
        sendConfigStatusChangeNotification();
        assertTrue(jobUpdateServiceStub.getJobProperties().isEmpty());
    }

    @Test
    public void testProcessNotificationExceptionHandling() {
        makeSmrsFail = true;
        ((ActivityUtilsStub)activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        sendConfigStatusChangeNotification();
        assertActivityFailure();
    }

    @Test
    public void testHandleTimeout() {
        ((ActivityUtilsStub)activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        final ActivityStepResult result = restoreService.handleTimeout(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, result.getActivityResultEnum());
        assertActivityFailure();
    }

    @Test
    public void testCancel() {
        ((ActivityUtilsStub)activityUtils).subscribeToMoNotifications(XF_CONFIG_LOAD_OBJECTS);
        final ActivityStepResult result = restoreService.cancel(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, result.getActivityResultEnum());
        assertUnsubscribedFromXfConfigLoadObjects();
    }

    @Test
    public void testCancelTimeout() {
        restoreService.cancelTimeout(ACTIVITY_JOB_ID, true);
    }


}
