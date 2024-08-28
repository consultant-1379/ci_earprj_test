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

import java.util.Calendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardSoftwarePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class SuccessfulJobCreationNotificationProcessorTest {

    @InjectMocks
    private SuccessfulJobCreationNotificationProcessor successfulJobCreationNotificationProcessor;

    @Mock
    private OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo;

    @Mock
    private JobActivityInfo jobActivityInformation;

    @Mock
    private NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse;

    @Mock
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    @Mock
    private TasksBase tasksBase;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Mock
    private OnboardSoftwarePackageContextForNfvo onboardPackageContextForNfvo;

    public static final long ACTIVITY_JOB_ID = 0;
    public static final String JOB_ID = "12345";

    @Mock
    private VranJobActivityUtil vranJobActivityUtil;

    @Test
    public void testProcessCreateJobResponse() {

        when(vranJobActivityUtil.incrementTime(nfvoSoftwarePackageJobResponse.getNotificationTimeStamp(), 5)).thenReturn(Calendar.getInstance());
        successfulJobCreationNotificationProcessor.processCreateJobResponse(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, jobActivityInformation, 12345L);
        verify(tasksBase).unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInformation);
        nfvoSoftwarePackageJobResponse.setActivityJobId(ACTIVITY_JOB_ID);
        verify(tasksBase).requestOnboardsoftwarePackageJobStatus(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, jobActivityInformation);

    }
}
