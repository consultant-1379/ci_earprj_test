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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
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
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class HandleTimeoutTaskTest {

    @InjectMocks
    private HandleTimeoutTask handleTimeoutTask;

    @Mock
    private TasksBase tasksBase;

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
    private Map<String, Object> mainJobProperties;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Mock
    private OnboardPackageContextBuilder onboardPackageContextBuilder;

    static final long activityJobId = 12345;

    @Test
    public void testHandleTimeoutForRepeat() {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobLogsAttributes = new HashMap<String, Object>();

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobContext);
        when(onboardSoftwarePackageContextForNfvo.areAllPackagesFailedToOnboard()).thenReturn(false);
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        when(onboardSoftwarePackageContextForNfvo.isComplete()).thenReturn(true);
        handleTimeoutTask.handleTimeout(activityJobId, jobActivityInformation);
        Assert.assertEquals(false, handleTimeoutTask.handleTimeout(activityJobId, jobActivityInformation));
        jobLogs.add(jobLogsAttributes);
        verify(jobAttributesPersistenceProvider, times(2)).persistJobPropertiesAndLogs(activityJobId, null, jobLogs);
        verify(tasksBase, times(2)).sendNfvoVnfPackageSyncRequest(activityJobId, onboardSoftwarePackageContextForNfvo);
    }

    @Test
    public void testHandleTimeoutForNotToRepeat() {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobLogsAttributes = new HashMap<String, Object>();
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobContext);
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        when(onboardSoftwarePackageContextForNfvo.isComplete()).thenReturn(false);
        handleTimeoutTask.handleTimeout(activityJobId, jobActivityInformation);
        Assert.assertEquals(true, handleTimeoutTask.handleTimeout(activityJobId, jobActivityInformation));
        jobLogs.add(jobLogsAttributes);
        verify(jobAttributesPersistenceProvider, times(2)).persistJobPropertiesAndLogs(activityJobId, null, jobLogs);

    }

    @Test
    public void testHandleTimeoutForRepeat_isFailedForAllPackages() {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobLogsAttributes = new HashMap<String, Object>();

        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobContext);
        when(onboardSoftwarePackageContextForNfvo.areAllPackagesFailedToOnboard()).thenReturn(true);
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        when(onboardSoftwarePackageContextForNfvo.isComplete()).thenReturn(true);
        handleTimeoutTask.handleTimeout(activityJobId, jobActivityInformation);
        Assert.assertEquals(false, handleTimeoutTask.handleTimeout(activityJobId, jobActivityInformation));
        jobLogs.add(jobLogsAttributes);
        verify(jobAttributesPersistenceProvider, times(2)).persistJobPropertiesAndLogs(activityJobId, null, jobLogs);
        verify(tasksBase, times(2)).sendNfvoVnfPackageSyncRequest(activityJobId, onboardSoftwarePackageContextForNfvo);
    }

}
