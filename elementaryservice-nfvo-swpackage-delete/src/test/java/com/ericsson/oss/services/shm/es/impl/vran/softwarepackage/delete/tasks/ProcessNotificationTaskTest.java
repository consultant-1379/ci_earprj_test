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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.tasks;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForEnm;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteSoftwarePackagePersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class ProcessNotificationTaskTest {

    @InjectMocks
    private ProcessNotificationTask processNotificationTask;

    @Mock
    private SuccessfulJobCreationNotificationProcessor successfulJobCreationNotificationProcessor;

    @Mock
    private JobStatusNotificationProcessor jobStatusNotificationProcessor;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private DeleteSoftwarePackagePersistenceProvider deleteSoftwarePackagePersistenceProvider;

    @Mock
    private DeleteJobPropertiesPersistenceProvider deleteJobPropertiesPersistenceProvider;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    private TasksBase tasksBase;

    @Mock
    private DeletePackageContextBuilder deletePackageContextBuilder;

    @Mock
    private NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse;

    @Mock
    private JobActivityInfo jobActivityInfo;

    @Mock
    private JobEnvironment jobContext;

    @Mock
    private DeletePackageContextForEnm deletePackageContextForEnm;

    @Mock
    private DeletePackageContextForNfvo deletePackageContextForNfvo;

    private static final long ACTIVITY_JOB_ID = 12345;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Before
    public void setUp() throws Exception {
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(deletePackageContextBuilder.buildDeletePackageContextForEnm(jobContext)).thenReturn(deletePackageContextForEnm);
        when(deletePackageContextBuilder.buildDeletePackageContextForNfvo(jobContext)).thenReturn(deletePackageContextForNfvo);
        when(nfvoSoftwarePackageJobResponse.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(nfvoSoftwarePackageJobResponse.getNodeAddress()).thenReturn("TestNode");
        when(deletePackageContextForNfvo.getContext()).thenReturn(jobContext);
        when(deletePackageContextForNfvo.getCurrentPackage()).thenReturn("TestVPPPackage");
    }

    @Test
    public void testInvalidResponseForCreateJob() {
        when(nfvoSoftwarePackageJobResponse.getResponseType()).thenReturn(VranJobConstants.DELETE_VNF_PACKAGES_FROM_NFVO);
        processNotificationTask.processNotification(nfvoSoftwarePackageJobResponse, jobActivityInfo);
        verifySteps();
    }

    @Test
    public void testResponseReceivedForCreateJobSuccessful() {
        when(nfvoSoftwarePackageJobResponse.getResponseType()).thenReturn(VranJobConstants.CREATE_SW_PACKAGE_DELETE_JOB);
        when(nfvoSoftwarePackageJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        processNotificationTask.processNotification(nfvoSoftwarePackageJobResponse, jobActivityInfo);
        verify(jobStatusNotificationProcessor).processJobSuccessResponse(nfvoSoftwarePackageJobResponse, deletePackageContextForNfvo, jobActivityInfo);
        verify(tasksBase).notifyWorkFlowService(ACTIVITY_JOB_ID, false, jobContext);
    }

    @Test
    public void testResponseReceivedForCreateJobFailed() {
        when(nfvoSoftwarePackageJobResponse.getResponseType()).thenReturn(VranJobConstants.CREATE_SW_PACKAGE_DELETE_JOB);
        when(nfvoSoftwarePackageJobResponse.getResult()).thenReturn(ShmConstants.FAILEDJOB);
        processNotificationTask.processNotification(nfvoSoftwarePackageJobResponse, jobActivityInfo);
        verifySteps();
    }

    public void verifySteps() {
        verify(deleteJobPropertiesPersistenceProvider).incrementFailedSoftwarePackageCountInNfvo(ACTIVITY_JOB_ID, jobContext);
        verify(deleteJobPropertiesPersistenceProvider).updateFailedSoftwarePackagesInNfvo(ACTIVITY_JOB_ID, deletePackageContextForNfvo.getCurrentPackage(), jobContext);
        verify(tasksBase).unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInfo);
    }
}
