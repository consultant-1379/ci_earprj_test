package com.ericsson.oss.services.shm.es.impl.minilink.restore;

import static com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants.ACTIVITY_RESULT;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.NETWORKELEMENT;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.OSSMODELIDENTITY;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_STATUS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_UP_LOAD_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.impl.minilink.backup.UnsecureFTPModelIdentity;
import com.ericsson.oss.services.shm.es.impl.minilink.common.BackupRestoreDpsStub;
import com.ericsson.oss.services.shm.es.impl.minilink.common.BackupRestoreTestBase;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;

public class VerifyServiceTest extends BackupRestoreTestBase {

    @ObjectUnderTest
    private VerifyService verifyService;

    @MockedImplementation
    private DataBucket liveBucket;

    @MockedImplementation
    private ManagedObject managedObject;

    protected BackupRestoreDpsStub dpsStub = new BackupRestoreDpsStub();
    @Test
    public void testPrecheckSucceedsInValidState() {
        ActivityStepResult precheckResult = verifyService.precheck(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, precheckResult.getActivityResultEnum());
        assertLog("Precheck for \"verify\" is successful.");
    }

    @Test
    public void testExecute() {
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_OK.getStatusValue());
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(NETWORKELEMENT + NODE_NAME)).thenReturn(managedObject);
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn("M11B");
        verifyService.execute(ACTIVITY_JOB_ID);
        assertTrue(jobUpdateServiceStub.getJobProperties().containsKey(ACTIVITY_RESULT));
    }

    @Test
    public void testExecuteFTP() {
        xfConfigLoadObjectsMo.setAttribute(XF_CONFIG_STATUS, CONFIG_UP_LOAD_OK.getStatusValue());
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(NETWORKELEMENT + NODE_NAME)).thenReturn(managedObject);
        when(managedObject.getAttribute(OSSMODELIDENTITY)).thenReturn(UnsecureFTPModelIdentity.TN11B.getOssModelIdentity());
        verifyService.execute(ACTIVITY_JOB_ID);
        assertTrue(jobUpdateServiceStub.getJobProperties().containsKey(ACTIVITY_RESULT));
    }

    @Test
    public void testNonAvcAttributeChangeNotificationIgnored() {
        verifyService.processNotification(new Notification() {
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
    public void testHandleTimeout() {
        final ActivityStepResult result = verifyService.handleTimeout(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, result.getActivityResultEnum());
    }

    @Test
    public void testCancel() {
        final ActivityStepResult result = verifyService.cancel(ACTIVITY_JOB_ID);
        assertEquals(ActivityStepResultEnum.EXECUTION_FAILED, result.getActivityResultEnum());
    }

    @Test
    public void testCancelTimeout() {
        verifyService.cancelTimeout(ACTIVITY_JOB_ID, true);
    }


}
