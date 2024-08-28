package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.tasks;
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

import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardPackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardSoftwarePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.persistence.OnboardJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class ProcessNotificationTaskTest {

    @InjectMocks
    private ProcessNotificationTask processNotificationTask;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    private JobEnvironment jobContext;

    @Mock
    private OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo;

    @Mock
    private OnboardJobPropertiesPersistenceProvider onboardJobPropertiesPersistenceProvider;

    @Mock
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    @Mock
    private JobActivityInfo jobActivityInformation;

    @Mock
    private NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse;

    @Mock
    private SuccessfulJobCreationNotificationProcessor successfulJobCreationNotificationProcessor;

    @Mock
    private JobStatusNotificationProcessor jobStatusNotificationProcessor;

    @Mock
    private OnboardPackageContextBuilder onboardPackageContextBuilder;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Mock
    private TasksBase tasksBase;

    static final long ACTIVITY_JOB_ID = 12345;

    @Test
    public void testProcessingSuccessCreateNotification() {
        when(nfvoSoftwarePackageJobResponse.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        when(nfvoSoftwarePackageJobResponse.getResponseType()).thenReturn(VranJobConstants.CREATE_SW_PACKAGE_ONBOARD_JOB);
        when(nfvoSoftwarePackageJobResponse.getResult()).thenReturn(ShmConstants.SUCCESS);
        processNotificationTask.processNotification(nfvoSoftwarePackageJobResponse, jobActivityInformation);
    }

    @Test
    public void testProcessingFailureCreateNotification() {
        when(nfvoSoftwarePackageJobResponse.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        when(nfvoSoftwarePackageJobResponse.getResponseType()).thenReturn(VranJobConstants.CREATE_SW_PACKAGE_ONBOARD_JOB);
        when(nfvoSoftwarePackageJobResponse.getResult()).thenReturn(ShmConstants.FALSE);
        processNotificationTask.processNotification(nfvoSoftwarePackageJobResponse, jobActivityInformation);
    }

    @Test
    public void testJobPollNotificationwithProcessing() {
        when(nfvoSoftwarePackageJobResponse.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        when(nfvoSoftwarePackageJobResponse.getResponseType()).thenReturn(VranJobConstants.SW_PACKAGE_POLL_JOB);
        when(nfvoSoftwarePackageJobResponse.getStatus()).thenReturn(VranJobConstants.PROCESSING_STATUS);
        processNotificationTask.processNotification(nfvoSoftwarePackageJobResponse, jobActivityInformation);
    }

    @Test
    public void testJobPollNotificationWithSuccess() {
        when(nfvoSoftwarePackageJobResponse.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        when(nfvoSoftwarePackageJobResponse.getResponseType()).thenReturn(VranJobConstants.SW_PACKAGE_POLL_JOB);
        when(nfvoSoftwarePackageJobResponse.getStatus()).thenReturn(ShmConstants.SUCCESS);
        processNotificationTask.processNotification(nfvoSoftwarePackageJobResponse, jobActivityInformation);
    }

    @Test
    public void testProcessActivityFailureResponse() {
        when(nfvoSoftwarePackageJobResponse.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        processNotificationTask.processNotification(nfvoSoftwarePackageJobResponse, jobActivityInformation);
    }

}
