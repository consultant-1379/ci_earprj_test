package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.nfvo;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.mediation.vran.model.response.NfvoSwPackageMediationResponse;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.persistence.OnboardJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.tasks.ExecuteTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.tasks.HandleTimeoutTask;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.tasks.ProcessNotificationTask;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobNotification;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.onboard.notification.NfvoSoftwarePackageJobNotificationWrapper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class OnboardSoftwarePackageServiceTest {

    @InjectMocks
    private OnboardSoftwarePackageService onboardSoftwarePackageService;

    @Mock
    private JobActivityInfo jobActivityInformation;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private ActivityStepResult activityStepResult;

    @Mock
    private JobEnvironment jobContext;

    @Mock
    private NfvoSoftwarePackageJobNotification onboardNotification;

    @Mock
    private Map<String, Object> activityJobProperties;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private NfvoSwPackageMediationResponse nfvoSwPackageMediationResponse;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    private OnboardJobPropertiesPersistenceProvider onboardJobPropertiesPersistenceProvider;

    @Mock
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    @Mock
    private HandleTimeoutTask handleTimeoutTask;

    @Mock
    private ExecuteTask executeTask;

    @Mock
    private ProcessNotificationTask processNotificationTask;

    @Mock
    private NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse;

    @Mock
    private NfvoSoftwarePackageJobNotificationWrapper message;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    public static final long ACTIVITY_JOB_ID = 345;
    public static final String BUSINES_KEY = "VNFM12345";

    @Test
    public void testPrecheck() {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperties = new HashMap<String, Object>();
        jobLogs.add(jobProperties);
        final Map<String, Object> jobProperties1 = new HashMap<String, Object>();
        jobLogs.add(jobProperties1);
        onboardSoftwarePackageService.precheck(ACTIVITY_JOB_ID);
        verify(jobAttributesPersistenceProvider).persistJobPropertiesAndLogs(ACTIVITY_JOB_ID, null, jobLogs);

    }

    @Test
    public void testExecute() {
        onboardSoftwarePackageService.execute(ACTIVITY_JOB_ID);

    }

    @Test
    public void testprocessNotification() {
        when(message.getNfvoSoftwarePackageJobNotification()).thenReturn(nfvoSoftwarePackageJobResponse);
        onboardSoftwarePackageService.processNotification(message);
        verify(activityUtils).additionalInfoForEvent(nfvoSoftwarePackageJobResponse.getActivityJobId(), nfvoSoftwarePackageJobResponse.getNodeAddress(), nfvoSoftwarePackageJobResponse.toString());
        verify(processNotificationTask).processNotification(nfvoSoftwarePackageJobResponse, null);

    }

    @Test
    public void testhandleTimeoutWithRepeatexecuteTrue() {
        when(handleTimeoutTask.handleTimeout(ACTIVITY_JOB_ID, jobActivityInformation)).thenReturn(true);
        onboardSoftwarePackageService.handleTimeout(ACTIVITY_JOB_ID);
    }

    @Test
    public void testhandleTimeoutWithRepeatexecuteFalse() {
        when(handleTimeoutTask.handleTimeout(ACTIVITY_JOB_ID, jobActivityInformation)).thenReturn(false);
        onboardSoftwarePackageService.handleTimeout(ACTIVITY_JOB_ID);

    }

}
