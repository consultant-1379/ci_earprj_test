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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.MTRSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardPackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardSoftwarePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.persistence.OnboardJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.jobs.vran.constants.VranConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class ExecuteTaskTest {

    @InjectMocks
    private ExecuteTask executeTask;

    @Mock
    private TasksBase tasksBase;

    @Mock
    private MTRSender onboardSoftwarepackageEventSender;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    private JobEnvironment jobContext;

    @Mock
    private OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo;

    @Mock
    private OnboardJobPropertiesPersistenceProvider jobDetailsPersistenceService;

    @Mock
    private VnfSoftwarePackagePersistenceProvider vnfSoftwarePackagePersistenceProvider;

    @Mock
    private JobActivityInfo jobActivityInformation;

    @Mock
    private NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse;

    @Mock
    private OnboardPackageContextBuilder onboardPackageContextBuilder;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    public static final long ACTIVITY_JOB_ID = 12345;
    public static final String NODE_FDN = "testFdn";
    public static final String PACKAGE_NAME = "testPackage";
    public static final String FILE_PATH = "D://test/";

    @Test
    public void testExecute() {
        final Map<String, Object> mainJobProperties = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        when(vranJobActivityService.getMainJobAttributes(jobContext)).thenReturn(mainJobProperties);
        when(vranJobActivityService.getNeJobAttributes(jobContext)).thenReturn(neJobProperties);
        when(activityUtils.getActivityInfo(ACTIVITY_JOB_ID, this.getClass())).thenReturn(jobActivityInformation);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(onboardSoftwarePackageContextForNfvo.getNodeFdn()).thenReturn(NODE_FDN);
        when(onboardSoftwarePackageContextForNfvo.getCurrentPackage()).thenReturn(PACKAGE_NAME);
        when(vnfSoftwarePackagePersistenceProvider.getVnfPackageSMRSPath(PACKAGE_NAME)).thenReturn(FILE_PATH);
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        executeTask.execute(ACTIVITY_JOB_ID);
    }

    @Test
    public void testExecuteException() {
        final Map<String, Object> mainJobProperties = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        when(vranJobActivityService.getMainJobAttributes(jobContext)).thenReturn(mainJobProperties);
        when(vranJobActivityService.getNeJobAttributes(jobContext)).thenReturn(neJobProperties);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        executeTask.execute(ACTIVITY_JOB_ID);
    }

    @Test
    public void testPerformOnboardAction() {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobLogAttributes = new HashMap<String, Object>();
        jobLogs.add(jobLogAttributes);
        final Map<String,Object> eventAttributes = new HashMap<String,Object>();
        eventAttributes.put("activityJobId", ACTIVITY_JOB_ID);
        when(vnfSoftwarePackagePersistenceProvider.getVnfPackageSMRSPath(PACKAGE_NAME)).thenReturn(FILE_PATH);
        executeTask.performOnboardAction(ACTIVITY_JOB_ID, PACKAGE_NAME, NODE_FDN, jobActivityInformation);

        verify(jobAttributesPersistenceProvider).persistJobPropertiesAndLogs(ACTIVITY_JOB_ID, null, jobLogs);
        verify(onboardSoftwarepackageEventSender).sendOnboardSoftwarePackageRequest(NODE_FDN, FILE_PATH,eventAttributes);
    }

}
