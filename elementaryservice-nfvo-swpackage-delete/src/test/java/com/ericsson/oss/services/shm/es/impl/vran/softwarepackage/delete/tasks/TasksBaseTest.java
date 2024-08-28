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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextBuilder;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForEnm;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.DeletePackageContextForNfvo;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common.MTRSender;
import com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence.DeleteJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.vran.common.NfvoVnfPackageSyncMTRSender;
import com.ericsson.oss.services.shm.es.vran.onboard.api.notifications.NfvoSoftwarePackageJobResponse;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.onboard.notification.NfvoSoftwarePackageJobResponseImpl;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

@RunWith(MockitoJUnitRunner.class)
public class TasksBaseTest {

    @Mock
    private ActivityUtils activityUtils;

    @InjectMocks
    private TasksBase tasksBase;

    @Mock
    private JobEnvironment jobContext;

    @Mock
    private DeleteJobPropertiesPersistenceProvider deleteJobPropertiesPersistenceProvider;

    @Mock
    private DeletePackageContextForEnm deletePackageContextForEnm;

    @Mock
    private DeletePackageContextForNfvo deletePackageContextForNfvo;

    @Mock
    private VranJobActivityServiceHelper vranJobActivityService;

    @Mock
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Mock
    private MTRSender deleteSoftwarePackageEventSender;

    @Mock
    private DeletePackageContextBuilder deletePackageContextBuilder;

    @Mock
    private NfvoVnfPackageSyncMTRSender nfvoVnfPackageSyncMTRSender;

    private static final long ACTIVITY_JOB_ID = 1234;

    private static final String VNF_PACKAGE_ID = "37a3dbbe-d347-11e6-ba32-fa163e9da88f";

    private static final String JOB_ID = "06e5e396-d347-11e6-94ea-fa163e9da88f";

    public static final String NFVO_FDN = "NetworkFunctionVirtualizationOrchestrator=HPE-NFV-Director-001";

    @Before
    public void setUp() throws Exception {

        final Map<String, Object> mainJobProperties = new HashMap<String, Object>();
        final List<Map<String, Object>> neJobProperties = new ArrayList<Map<String, Object>>();

        when(vranJobActivityService.getMainJobAttributes(jobContext)).thenReturn(mainJobProperties);
        when(vranJobActivityService.getNeJobAttributes(jobContext)).thenReturn(neJobProperties);

        when(activityUtils.getJobEnvironment(ACTIVITY_JOB_ID)).thenReturn(jobContext);

        when(deletePackageContextBuilder.buildDeletePackageContextForEnm(jobContext)).thenReturn(deletePackageContextForEnm);
        when(deletePackageContextBuilder.buildDeletePackageContextForNfvo(jobContext)).thenReturn(deletePackageContextForNfvo);

        //PowerMockito.whenNew(DeletePackageContextForEnm.class).withArguments(jobContext).thenReturn(deletePackageContextForEnm);
        //PowerMockito.whenNew(DeletePackageContextForNfvo.class).withArguments(jobContext).thenReturn(deletePackageContextForNfvo);
    }

    @Test
    public void proceedWithNextStepTest_ForEnmInComplete() {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();
        when(deletePackageContextForEnm.isComplete()).thenReturn(false);

        tasksBase.proceedWithNextStep(ACTIVITY_JOB_ID);

        verify(deleteJobPropertiesPersistenceProvider).incrementSoftwarePackageCurrentIndexInEnm(ACTIVITY_JOB_ID, jobContext);
        verify(jobAttributesPersistenceProvider).persistJobPropertiesAndLogs(ACTIVITY_JOB_ID, jobProperties, jobLogs);

        when(deletePackageContextForEnm.isComplete()).thenReturn(true);
        when(deletePackageContextForNfvo.areThereAnyPackagesToBeDeleted()).thenReturn(true);
        tasksBase.proceedWithNextStep(ACTIVITY_JOB_ID);

    }

    @Test
    public void proceedWithNextStepTest_ForDeletionComplete() {

        when(deletePackageContextForEnm.isComplete()).thenReturn(true);
        when(deletePackageContextForEnm.isComplete() && deletePackageContextForNfvo.areThereAnyPackagesToBeDeleted()).thenReturn(false);

        tasksBase.proceedWithNextStep(ACTIVITY_JOB_ID);

        final String jobLogMessage = tasksBase.buildConsolidatedJobLogMessageForEnm(deletePackageContextForEnm.getTotalCount(), deletePackageContextForEnm.getSuccessCount(),
                deletePackageContextForEnm.getNoOfFailures(), deletePackageContextForEnm.getFailedPackages());
        verify(vranJobActivityService).buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString());

    }

    @Test
    public void proceedWithNextStepForNfvoTest_ForNfvoComplete() {

        when(deletePackageContextForNfvo.isComplete()).thenReturn(true);
        when(deletePackageContextForNfvo.areAllPackagesFailedToDelete()).thenReturn(false);
        when(deletePackageContextForNfvo.getNodeFdn()).thenReturn(NFVO_FDN);

        tasksBase.proceedWithNextStepForNfvo(1234);

        final String jobLogMessage = tasksBase.buildConsolidatedJobLogMessageForNfvo(deletePackageContextForNfvo.getTotalCount(), deletePackageContextForNfvo.getSuccessCount(),
                deletePackageContextForNfvo.getNoOfFailures(), deletePackageContextForNfvo.getFailedPackages());
        verify(vranJobActivityService).buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString());

        when(deletePackageContextForEnm.getTotalCount()).thenReturn(1);
        when(deletePackageContextForEnm.getTotalCount() > 0 && deletePackageContextForEnm.isComplete()).thenReturn(true);
        tasksBase.proceedWithNextStepForNfvo(1234);

        verify(nfvoVnfPackageSyncMTRSender, times(2)).sendNfvoVnfPackagesSyncRequest(deletePackageContextForNfvo.getNodeFdn());

    }

    @Test
    public void proceedWithNextStepForNfvoTest_ForNfvoInComplete() {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();

        when(deletePackageContextForNfvo.isComplete()).thenReturn(false);

        tasksBase.proceedWithNextStepForNfvo(ACTIVITY_JOB_ID);

        verify(deleteJobPropertiesPersistenceProvider).incrementSoftwarePackageCurrentIndexInNfvo(ACTIVITY_JOB_ID, jobContext);
        verify(jobAttributesPersistenceProvider).persistJobPropertiesAndLogs(ACTIVITY_JOB_ID, jobProperties, jobLogs);
    }

    @Test
    public void proceedWithNfvoAndPersistJobPropertiesTest_ForNfvoComplete() {

        when(deletePackageContextForNfvo.isComplete()).thenReturn(true);
        when(deletePackageContextForNfvo.areAllPackagesFailedToDelete()).thenReturn(false);
        when(deletePackageContextForNfvo.getNodeFdn()).thenReturn(NFVO_FDN);

        tasksBase.proceedWithNextStepForNfvo(1234);

        final String jobLogMessage = tasksBase.buildConsolidatedJobLogMessageForNfvo(deletePackageContextForNfvo.getTotalCount(), deletePackageContextForNfvo.getSuccessCount(),
                deletePackageContextForNfvo.getNoOfFailures(), deletePackageContextForNfvo.getFailedPackages());
        verify(vranJobActivityService).buildJobLog(jobLogMessage, JobLogLevel.ERROR.toString());

        when(deletePackageContextForEnm.getTotalCount()).thenReturn(1);
        when(deletePackageContextForEnm.getTotalCount() > 0 && deletePackageContextForEnm.isComplete()).thenReturn(true);
        tasksBase.proceedWithNextStepForNfvo(1234);

        verify(nfvoVnfPackageSyncMTRSender, times(2)).sendNfvoVnfPackagesSyncRequest(deletePackageContextForNfvo.getNodeFdn());

        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);

        verify(activityUtils, times(0)).sendNotificationToWFS(jobContext, ACTIVITY_JOB_ID, VranJobConstants.DEL_SW_ACTIVITY, processVariables);
    }

    @Test
    public void proceedWithNfvoAndPersistJobProperties_ForNfvoInComplete() {
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();

        when(deletePackageContextForNfvo.isComplete()).thenReturn(false);

        tasksBase.proceedWithNextStepForNfvo(ACTIVITY_JOB_ID);

        verify(deleteJobPropertiesPersistenceProvider).incrementSoftwarePackageCurrentIndexInNfvo(ACTIVITY_JOB_ID, jobContext);
        verify(jobAttributesPersistenceProvider).persistJobPropertiesAndLogs(ACTIVITY_JOB_ID, jobProperties, jobLogs);
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);

        verify(activityUtils, times(0)).sendNotificationToWFS(jobContext, ACTIVITY_JOB_ID, VranJobConstants.DEL_SW_ACTIVITY, processVariables);
    }

    @Test
    public void handleDeletePackageFailureFromNfvoTest() {
        final String softwarePackageName = "";
        final String errorMessage = "";
        final List<Map<String, Object>> jobLogs = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobProperties = null;

        tasksBase.handleDeletePackageFailureFromNfvo(ACTIVITY_JOB_ID, deletePackageContextForNfvo, softwarePackageName, errorMessage);

        final String logMessage = String.format(VranJobLogMessageTemplate.ACTIVITY_FAILED_FOR_PACKAGE_WITH_REASON, VranJobConstants.DEL_SW_ACTIVITY, softwarePackageName, errorMessage);

        verify(vranJobActivityService).buildJobLog(logMessage, JobLogLevel.ERROR.toString());
        jobLogs.add(new HashMap<String, Object>());
        verify(jobAttributesPersistenceProvider).persistJobPropertiesAndLogs(ACTIVITY_JOB_ID, jobProperties, jobLogs);
        verify(deleteJobPropertiesPersistenceProvider).incrementFailedSoftwarePackageCountInNfvo(ACTIVITY_JOB_ID, deletePackageContextForNfvo.getContext());
        verify(deleteJobPropertiesPersistenceProvider).updateFailedSoftwarePackagesInNfvo(ACTIVITY_JOB_ID, softwarePackageName, deletePackageContextForNfvo.getContext());
    }

    @Test
    public void notifyWorkFlowServiceTest() {
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);

        tasksBase.notifyWorkFlowService(ACTIVITY_JOB_ID, false, jobContext);

        verify(activityUtils).sendNotificationToWFS(jobContext, ACTIVITY_JOB_ID, VranJobConstants.DEL_SW_ACTIVITY, processVariables);
    }

    @Test
    public void markSoftwarePackageDeleteActivityResultTest() {

        final List<Map<String, Object>> jobProperties = new ArrayList<Map<String, Object>>();

        tasksBase.markSoftwarePackageDeleteActivityResult(jobProperties, 0);
        verify(activityUtils).prepareJobPropertyList(jobProperties, ShmConstants.RESULT, JobResult.SUCCESS.toString());

        tasksBase.markSoftwarePackageDeleteActivityResult(jobProperties, 1);
        verify(activityUtils).prepareJobPropertyList(jobProperties, ShmConstants.RESULT, JobResult.FAILED.toString());

        tasksBase.markSoftwarePackageDeleteActivityResult(jobProperties, 0, 0);
        verify(activityUtils, times(2)).prepareJobPropertyList(jobProperties, ShmConstants.RESULT, JobResult.SUCCESS.toString());

        tasksBase.markSoftwarePackageDeleteActivityResult(jobProperties, 1, 0);
        verify(activityUtils, times(2)).prepareJobPropertyList(jobProperties, ShmConstants.RESULT, JobResult.FAILED.toString());

        tasksBase.markSoftwarePackageDeleteActivityResult(jobProperties, 0, 1);
        verify(activityUtils, times(3)).prepareJobPropertyList(jobProperties, ShmConstants.RESULT, JobResult.FAILED.toString());
    }

    @Test
    public void buildConsolidatedJobLogMessageForEnmTest() {

        String logMessage = String.format(VranJobLogMessageTemplate.DELETE_PACKAGES_RESULT_FROM_ENM, 0, 0, 0, null);
        String jobLogMessageForEnm = tasksBase.buildConsolidatedJobLogMessageForEnm(0, 0, 0, null);
        assertEquals(logMessage, jobLogMessageForEnm);

        logMessage = String.format(VranJobLogMessageTemplate.DELETE_SWPACKAGENAMES_FROM_ENM_LOCATION, 0, 0, 1, "");
        jobLogMessageForEnm = tasksBase.buildConsolidatedJobLogMessageForEnm(0, 0, 1, null);
        assertEquals(logMessage, jobLogMessageForEnm);
    }

    @Test
    public void buildConsolidatedJobLogMessageForNfvoTest() {

        String logMessage = String.format(VranJobLogMessageTemplate.DELETE_PACKAGES_RESULT_FROM_NFVO, 0, 0, 0, null);
        String jobLogMessageForNfvo = tasksBase.buildConsolidatedJobLogMessageForNfvo(0, 0, 0, null);
        assertEquals(logMessage, jobLogMessageForNfvo);

        logMessage = String.format(VranJobLogMessageTemplate.DELETE_SWPACKAGENAMES_FROM_NFVO_LOCATION, 0, 0, 1, "");
        jobLogMessageForNfvo = tasksBase.buildConsolidatedJobLogMessageForNfvo(0, 0, 1, null);
        assertEquals(logMessage, jobLogMessageForNfvo);
    }

    @Test
    public void requestDeleteSoftwarePackageJobStatusTest() {

        tasksBase.requestDeleteSoftwarePackageJobStatus(1234, "", deletePackageContextForNfvo, null);

        verify(deleteSoftwarePackageEventSender).sendJobStatusRequest(deletePackageContextForNfvo.getNodeFdn(), deletePackageContextForNfvo.getCurrentPackage(), "");
    }

    @Test
    public void subscribeNotificationsTest() {

        tasksBase.subscribeNotifications(1234, null, VranJobConstants.NFVO, VNF_PACKAGE_ID, null);
        verify(activityUtils).subscribeToMoNotifications(VranJobConstants.NFVO + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + VNF_PACKAGE_ID, 1234, null);

        tasksBase.subscribeNotifications(1234, JOB_ID, VranJobConstants.NFVO, VNF_PACKAGE_ID, null);
        verify(activityUtils).subscribeToMoNotifications(VranJobConstants.NFVO + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + JOB_ID, 1234, null);
    }

    @Test
    public void unsubscribeNotificationsTest() {
        NfvoSoftwarePackageJobResponse nfvoSoftwarePackageJobResponse = new NfvoSoftwarePackageJobResponseImpl();
        nfvoSoftwarePackageJobResponse.setJobId(JOB_ID);
        nfvoSoftwarePackageJobResponse.setNodeAddress(VranJobConstants.NFVO);
        nfvoSoftwarePackageJobResponse.setVnfPackageId(VNF_PACKAGE_ID);

        tasksBase.unsubscribeNotifications(nfvoSoftwarePackageJobResponse, null);
        verify(activityUtils).unSubscribeToMoNotifications(VranJobConstants.NFVO + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + JOB_ID, nfvoSoftwarePackageJobResponse.getActivityJobId(), null);

        nfvoSoftwarePackageJobResponse.setJobId(null);
        tasksBase.unsubscribeNotifications(nfvoSoftwarePackageJobResponse, null);
        verify(activityUtils).unSubscribeToMoNotifications(VranJobConstants.NFVO + VranJobConstants.SUBSCRIPTION_KEY_DELIMETER + VNF_PACKAGE_ID, nfvoSoftwarePackageJobResponse.getActivityJobId(),
                null);

    }
}
