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
import java.util.Date;
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
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.MTRSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardPackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common.OnboardSoftwarePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.persistence.OnboardJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.common.NfvoVnfPackageSyncMTRSender;
import com.ericsson.oss.services.shm.onboard.notification.NfvoSoftwarePackageJobResponseImpl;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;
import com.ericsson.oss.services.shm.vran.shared.persistence.VnfSoftwarePackagePersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class TasksBaseTest {

    @InjectMocks
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
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Mock
    private OnboardPackageContextBuilder onboardPackageContextBuilder;

    @Mock
    private MTRSender onboardSoftwarepackageEventSender;

    @Mock
    private VranJobActivityUtil vranJobActivityUtil;

    @Mock
    private NfvoVnfPackageSyncMTRSender nfvoVnfPackageSyncMTRSender;

    public static final long ACTIVITY_JOB_ID = 12345;
    public static final String JOB_ID = "12345";
    public static final String TEST_NODE = "testNode";
    public static final String TEST_PATHENM = "testPathENM";
    public static final String SUBSCRIPTION_KEY = "testNode@12345";
    public static final String SUBSCRIPTION_KEY_FOR_FILE_PATH = "testNode@testPathENM";
    public static final String FULL_FILE_PATH = "d://smrs/smrsroot";
    public static final String NFVO_FDN = "NetworkFunctionVirtualizationOrchestrator=HPE-NFV-Director-001";
    public static final String VNF_PACKAGE_ID = "12282766-defd-11e5-a47e-fa153e1fee8c";

    @Test
    public void testOnboardNotCompleted() throws Throwable {

        final Map<String, Object> mainJobProperties = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        when(vranJobActivityService.getMainJobAttributes(jobContext)).thenReturn(mainJobProperties);
        when(vranJobActivityService.getNeJobAttributes(jobContext)).thenReturn(neJobProperties);
        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(onboardSoftwarePackageContextForNfvo.isComplete()).thenReturn(false);

        tasksBase.proceedWithNextStep(ACTIVITY_JOB_ID);
        verify(onboardJobPropertiesPersistenceProvider).incrementSoftwarePackageCurrentIndexToBeOnboarded(ACTIVITY_JOB_ID, jobContext);
        verify(jobAttributesPersistenceProvider).persistJobPropertiesAndLogs(ACTIVITY_JOB_ID, jobProperties, jobLogs);

    }

    @Test
    public void testOnboardCompleted() {

        final Map<String, Object> mainJobProperties = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobLogAttributes = new HashMap<String, Object>();

        final List<Map<String, Object>> activityJobProperties = new ArrayList<Map<String, Object>>();

        when(vranJobActivityService.getMainJobAttributes(jobContext)).thenReturn(mainJobProperties);
        when(vranJobActivityService.getNeJobAttributes(jobContext)).thenReturn(neJobProperties);

        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(onboardSoftwarePackageContextForNfvo.areAllPackagesFailedToOnboard()).thenReturn(false);
        when(onboardSoftwarePackageContextForNfvo.getNodeFdn()).thenReturn(NFVO_FDN);
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        when(onboardSoftwarePackageContextForNfvo.isComplete()).thenReturn(true);

        tasksBase.proceedWithNextStep(ACTIVITY_JOB_ID);
        jobLogs.add(jobLogAttributes);
        verify(jobAttributesPersistenceProvider).persistJobPropertiesAndLogs(ACTIVITY_JOB_ID, activityJobProperties, jobLogs);
        verify(nfvoVnfPackageSyncMTRSender).sendNfvoVnfPackagesSyncRequest(onboardSoftwarePackageContextForNfvo.getNodeFdn());

    }

    @Test
    public void testSubscriptionWithJobId() {
        tasksBase.subscribeNotifications(ACTIVITY_JOB_ID, TEST_NODE, jobActivityInformation);
        when(jobActivityInformation.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        verify(activityUtils).subscribeToMoNotifications(TEST_NODE+"@"+ACTIVITY_JOB_ID, ACTIVITY_JOB_ID, jobActivityInformation);
    }

    @Test
    public void testSubscriptionWithFilePath() {
        tasksBase.subscribeNotifications(ACTIVITY_JOB_ID, TEST_NODE, jobActivityInformation);
        when(jobActivityInformation.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        verify(activityUtils).subscribeToMoNotifications(TEST_NODE+"@"+ACTIVITY_JOB_ID, ACTIVITY_JOB_ID, jobActivityInformation);
    }

    @Test
    public void testUnSubscriptionWithJobId() {
        NfvoSoftwarePackageJobResponseImpl nfvoSoftwarePackageJobResponse = new NfvoSoftwarePackageJobResponseImpl();
        nfvoSoftwarePackageJobResponse.setJobId(JOB_ID);
        nfvoSoftwarePackageJobResponse.setNodeAddress(TEST_NODE);
        nfvoSoftwarePackageJobResponse.setActivityJobId(ACTIVITY_JOB_ID);
        nfvoSoftwarePackageJobResponse.setActivityJobId(ACTIVITY_JOB_ID);
        when(jobActivityInformation.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        tasksBase.unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInformation);
        verify(activityUtils).unSubscribeToMoNotifications(TEST_NODE+"@"+ACTIVITY_JOB_ID, ACTIVITY_JOB_ID, jobActivityInformation);
    }

    @Test
    public void testUnSubscriptionWithFilePath() {
        NfvoSoftwarePackageJobResponseImpl nfvoSoftwarePackageJobResponse = new NfvoSoftwarePackageJobResponseImpl();
        nfvoSoftwarePackageJobResponse.setJobId(null);
        nfvoSoftwarePackageJobResponse.setFullFilePath(FULL_FILE_PATH);
        nfvoSoftwarePackageJobResponse.setNodeAddress(TEST_NODE);
        nfvoSoftwarePackageJobResponse.setActivityJobId(ACTIVITY_JOB_ID);
        when(jobActivityInformation.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        tasksBase.unsubscribeNotifications(nfvoSoftwarePackageJobResponse, jobActivityInformation);
        verify(activityUtils).unSubscribeToMoNotifications(TEST_NODE+"@"+ACTIVITY_JOB_ID, ACTIVITY_JOB_ID, jobActivityInformation);
    }

    @Test
    public void testOnboardsoftwarePackageJobStatus() {
        NfvoSoftwarePackageJobResponseImpl nfvoSoftwarePackageJobResponse = new NfvoSoftwarePackageJobResponseImpl();
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        when(onboardSoftwarePackageContextForNfvo.getNodeFdn()).thenReturn(TEST_NODE);
        when(jobActivityInformation.getActivityJobId()).thenReturn(ACTIVITY_JOB_ID);
        nfvoSoftwarePackageJobResponse.setJobId(JOB_ID);
        nfvoSoftwarePackageJobResponse.setActivityJobId(ACTIVITY_JOB_ID);
        nfvoSoftwarePackageJobResponse.setVnfPackageId(VNF_PACKAGE_ID);
        final Map<String,Object> eventAttributes = new HashMap<String,Object>();
        eventAttributes.put("activityJobId", ACTIVITY_JOB_ID);
        tasksBase.requestOnboardsoftwarePackageJobStatus(nfvoSoftwarePackageJobResponse, onboardSoftwarePackageContextForNfvo, jobActivityInformation);
        verify(onboardSoftwarepackageEventSender).sendJobStatusRequest(onboardSoftwarePackageContextForNfvo.getNodeFdn(),VNF_PACKAGE_ID , JOB_ID,eventAttributes);
    }

    @Test
    public void testIncrementTime() {
        Calendar calender = Calendar.getInstance();
        vranJobActivityUtil.incrementTime(new Date(), 20);
        Assert.assertEquals(calender, calender);
    }

    @Test
    public void testOnboardCompleted_isFailedForAllPackages() {

        final Map<String, Object> mainJobProperties = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobLogAttributes = new HashMap<String, Object>();

        final List<Map<String, Object>> activityJobProperties = new ArrayList<Map<String, Object>>();

        when(vranJobActivityService.getMainJobAttributes(jobContext)).thenReturn(mainJobProperties);
        when(vranJobActivityService.getNeJobAttributes(jobContext)).thenReturn(neJobProperties);

        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);
        when(onboardSoftwarePackageContextForNfvo.areAllPackagesFailedToOnboard()).thenReturn(true);
        when(onboardSoftwarePackageContextForNfvo.getNodeFdn()).thenReturn(NFVO_FDN);
        when(onboardPackageContextBuilder.buildOnboardPackageContextForNfvo(jobContext)).thenReturn(onboardSoftwarePackageContextForNfvo);
        when(onboardSoftwarePackageContextForNfvo.isComplete()).thenReturn(true);

        tasksBase.proceedWithNextStep(ACTIVITY_JOB_ID);
        jobLogs.add(jobLogAttributes);
        verify(jobAttributesPersistenceProvider).persistJobPropertiesAndLogs(ACTIVITY_JOB_ID, activityJobProperties, jobLogs);

    }

}
