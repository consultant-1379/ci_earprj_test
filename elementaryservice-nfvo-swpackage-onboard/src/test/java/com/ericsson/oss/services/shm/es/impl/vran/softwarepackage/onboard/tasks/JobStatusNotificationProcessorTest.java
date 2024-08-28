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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardSoftwarePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.persistence.OnboardJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class JobStatusNotificationProcessorTest {

    @InjectMocks
    private JobStatusNotificationProcessor jobStatusNotificationProcessor;

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
    private TasksBase tasksBase;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Mock
    private TasksBase taskBase;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private VranJobActivityUtil vranJobActivityUtil;

    public static final long ACTIVITY_JOB_ID = 12345;

    @Test
    public void testProcessJobInprogress() {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        jobLogs.add(jobAttributes);
        jobStatusNotificationProcessor.processJobInprogressResponse(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, jobActivityInformation);
        verify(jobAttributesPersistenceProvider).persistJobLogs(nfvoSoftwarePackageJobResponse.getActivityJobId(), jobLogs);

    }

    @Test
    public void testProcessJobSuccessResponse() {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        final Map<String, Object> jobAttributes1 = new HashMap<String, Object>();
        jobLogs.add(jobAttributes);
        jobLogs.add(jobAttributes1);
        when(nfvoSoftwarePackageJobResponse.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(onboardSoftwarePackageContextForNfvo.getContext()).thenReturn(jobContext);
        when(vranJobActivityUtil.incrementTime(nfvoSoftwarePackageJobResponse.getNotificationTimeStamp(), 10)).thenReturn(Calendar.getInstance());

        jobStatusNotificationProcessor.processJobSuccessResponse(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, jobActivityInformation);
        verify(jobAttributesPersistenceProvider).persistJobLogs(nfvoSoftwarePackageJobResponse.getActivityJobId(), jobLogs);
    }
}
