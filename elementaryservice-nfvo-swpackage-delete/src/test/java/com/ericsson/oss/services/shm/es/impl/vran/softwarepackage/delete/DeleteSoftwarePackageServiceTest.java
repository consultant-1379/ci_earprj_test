/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForEnm;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteSoftwarePackagePersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.tasks.ExecuteTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.tasks.HandleTimeoutTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.tasks.ProcessNotificationTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.tasks.TasksBase;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.onboard.notification.NfvoSoftwarePackageJobNotificationWrapper;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class DeleteSoftwarePackageServiceTest extends TasksBase {

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private DeleteJobPropertiesPersistenceProvider deleteJobPropertiesPersistenceProvider;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Mock
    private ProcessNotificationTask processNotificationTask;

    @Mock
    private HandleTimeoutTask handleTimeoutTask;

    @Mock
    private ExecuteTask executeTask;

    @Mock
    private TasksBase tasksBase;

    @InjectMocks
    private DeleteSoftwarePackageService deleteSoftwarePackageService;

    @Mock
    private DeletePackageContextBuilder deletePackageContextBuilder;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse;

    private static final long ACTIVITY_JOB_ID = 12345;

    @Mock
    private Notification notification;

    @Mock
    private NfvoSoftwarePackageJobNotificationWrapper nfvoSoftwarePackageJobNotificationWrapper;

    @Mock
    private JobEnvironment jobContext;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private ActivityStepResult activityStepResult;

    @Mock
    private DeletePackageContextForEnm deletePackageContextForEnm;

    @Mock
    private DeletePackageContextForNfvo deletePackageContextForNfvo;

    @Mock
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    @Mock
    private PersistenceObject vnfSoftwarePackageEntity;

    @Mock
    private DeleteSoftwarePackagePersistenceProvider deleteSoftwarePackagePersistenceProvider;

    @Before
    public void setUp() throws Exception {
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, DeleteSoftwarePackageService.class)).thenReturn(jobActivityInfo);
        when(deletePackageContextBuilder.buildDeletePackageContextForEnm(jobContext)).thenReturn(deletePackageContextForEnm);
        when(deletePackageContextBuilder.buildDeletePackageContextForNfvo(jobContext)).thenReturn(deletePackageContextForNfvo);
    }

    @Test
    public void testPreCheck() {
        deleteSoftwarePackageService.precheck(ACTIVITY_JOB_ID);
        verify(deleteJobPropertiesPersistenceProvider).initializeActivityVariables(ACTIVITY_JOB_ID);
    }

    @Test
    public void testExecute() {
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity(null)).thenReturn(vnfSoftwarePackageEntity);
        when(deletePackageContextForEnm.isComplete()).thenReturn(false);
        deleteSoftwarePackageService.execute(ACTIVITY_JOB_ID);
        verify(tasksBase).proceedWithNextStep(ACTIVITY_JOB_ID);

        when(deletePackageContextForEnm.isComplete()).thenReturn(true);
        deleteSoftwarePackageService.execute(ACTIVITY_JOB_ID);
    }

    @Test
    public void testExecuteForDeletePackageAvailableOnlyInEnm() {
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity(null)).thenReturn(vnfSoftwarePackageEntity);
        when(vnfSoftwarePackageEntity.getAttribute(VranJobConstants.SW_PACKAGE_LOCATION)).thenReturn(VranJobConstants.SMRS);
        when(deletePackageContextForEnm.isComplete()).thenReturn(false);
        deleteSoftwarePackageService.execute(ACTIVITY_JOB_ID);
        verify(tasksBase).proceedWithNextStep(ACTIVITY_JOB_ID);

        when(deletePackageContextForEnm.isComplete()).thenReturn(true);
        deleteSoftwarePackageService.execute(ACTIVITY_JOB_ID);
    }

    @Test
    public void testExecuteForDeletePackageAvailableInBothEnmAndNfvo() {
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity(null)).thenReturn(vnfSoftwarePackageEntity);
        when(vnfSoftwarePackageEntity.getAttribute(VranJobConstants.SW_PACKAGE_LOCATION)).thenReturn(VranJobConstants.SMRS_NFVO);
        when(deletePackageContextForEnm.isComplete()).thenReturn(false);
        when(tasksBase.isPackageAvailableBothInEnmAndNfvo(VranJobConstants.SMRS_NFVO)).thenReturn(true);
        deleteSoftwarePackageService.execute(ACTIVITY_JOB_ID);
        verify(tasksBase).proceedWithNextStep(ACTIVITY_JOB_ID);

        when(deletePackageContextForEnm.isComplete()).thenReturn(true);
        deleteSoftwarePackageService.execute(ACTIVITY_JOB_ID);
    }

    @Test
    public void testExecuteForDeletePackageWhichIsInUse() {
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity(null)).thenReturn(vnfSoftwarePackageEntity);
        when(vnfSoftwarePackageEntity.getAttribute(VranJobConstants.SW_PACKAGE_LOCATION)).thenReturn(VranJobConstants.SMRS_NFVO);
        when(deletePackageContextForEnm.isComplete()).thenReturn(false);
        when(deleteSoftwarePackagePersistenceProvider.isSoftwarePackageInUse(null)).thenReturn(true);
        deleteSoftwarePackageService.execute(ACTIVITY_JOB_ID);
        verify(tasksBase).proceedWithNextStep(ACTIVITY_JOB_ID);
        verify(deletePackageContextBuilder).buildDeletePackageContextForEnm(jobContext);
        verify(deletePackageContextBuilder).buildDeletePackageContextForNfvo(jobContext);
    }

    @Test
    public void testExecuteForDeletePackageWhichIsNotInDB() {
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity(null)).thenReturn(null);
        when(deletePackageContextForEnm.isComplete()).thenReturn(false);
        deleteSoftwarePackageService.execute(ACTIVITY_JOB_ID);
        verify(tasksBase).proceedWithNextStep(ACTIVITY_JOB_ID);
        verify(deletePackageContextBuilder).buildDeletePackageContextForEnm(jobContext);
        verify(deletePackageContextBuilder).buildDeletePackageContextForNfvo(jobContext);
    }

    @Test
    public void testExecuteForNfvo() {
        when(vnfSoftwarePackagePersistenceProvider.getVnfSoftwarePackageEntity(null)).thenReturn(vnfSoftwarePackageEntity);
        when(deletePackageContextForNfvo.isComplete()).thenReturn(false);
        deleteSoftwarePackageService.execute(ACTIVITY_JOB_ID);
        verify(deletePackageContextBuilder).buildDeletePackageContextForEnm(jobContext);
        verify(deletePackageContextBuilder).buildDeletePackageContextForNfvo(jobContext);
    }

    @Test
    public void testProcessNotification() {
        when(nfvoSoftwarePackageJobNotificationWrapper.getNfvoSoftwarePackageJobNotification()).thenReturn(nfvoSoftwarePackageJobResponse);
        when(nfvoSoftwarePackageJobResponse.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(nfvoSoftwarePackageJobResponse.getNodeAddress()).thenReturn("VrcNode");
        when(nfvoSoftwarePackageJobResponse.getVnfPackageId()).thenReturn("Pkg01");
        deleteSoftwarePackageService.processNotification(nfvoSoftwarePackageJobNotificationWrapper);
        verify(activityUtils).getActivityInfo(nfvoSoftwarePackageJobResponse.getActivityJobId(), DeleteSoftwarePackageService.class);
        verify(processNotificationTask).processNotification(nfvoSoftwarePackageJobResponse, jobActivityInfo);
    }

    @Test
    public void testHandleTimeout() {
        deleteSoftwarePackageService.handleTimeout(ACTIVITY_JOB_ID);
        verify(handleTimeoutTask).handleTimeout(ACTIVITY_JOB_ID, jobActivityInfo);
    }

    @Test
    public void testHandleTimeoutSuccess() {
        when(handleTimeoutTask.handleTimeout(ACTIVITY_JOB_ID, jobActivityInfo)).thenReturn(true);
        deleteSoftwarePackageService.handleTimeout(ACTIVITY_JOB_ID);
        verify(handleTimeoutTask).handleTimeout(ACTIVITY_JOB_ID, jobActivityInfo);
    }

    @Test
    public void testCancel() {
        deleteSoftwarePackageService.cancel(ACTIVITY_JOB_ID);
    }

    @Test
    public void testcancelTimeout() {
        deleteSoftwarePackageService.cancelTimeout(ACTIVITY_JOB_ID, true);
    }
}
