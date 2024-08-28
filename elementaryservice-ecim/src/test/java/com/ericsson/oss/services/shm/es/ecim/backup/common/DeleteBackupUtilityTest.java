/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.ecim.backup.common;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.ACTIVITY_END_PROGRESS_PERCENTAGE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.DeleteSmrsBackupUtil;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.ecim.backup.CancelBackupService;
import com.ericsson.oss.services.shm.es.impl.ecim.common.EcimCommonUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RunWith(MockitoJUnitRunner.class)
public class DeleteBackupUtilityTest {

    long activityJobId = 1;
    long neJobId = 2;
    long mainJobId = 3;

    String nodeName = "nodeName";
    String businessKey = "businessKey";
    String inputVersion = "inputVersion";
    final static String backupName = "backupname";
    final static String backupDomain = "backupDomain";
    final static String backupType = "backupType";

    String brmBackupManagerMoFdn = "brmBackupManagerMoFdn";

    Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
    Map<String, Object> neJobAttributes = new HashMap<String, Object>();
    Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
    Map<String, String> propertyMap = new HashMap<String, String>();

    List<Map<String, String>> activityJobProperties = new ArrayList<Map<String, String>>();
    final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
    final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
    String backupsToDelete = null;
    String backupsToDeleteOnSmrs = null;

    @Mock
    ActivityUtils activityUtilsMock;

    @Mock
    JobUpdateService jobUpdateServiceMock;

    @Mock
    SystemRecorder systemRecorderMock;

    @Mock
    Notification notificationMock;

    @Mock
    NotificationSubject notificationSubjectMock;

    @Mock
    EcimBackupUtils ecimBackupUtilsMock;

    @Mock
    BrmMoServiceRetryProxy brmMoServiceRetryProxyMock;

    @Mock
    Map<String, AttributeChangeData> modifiedAttributesMock;

    @Mock
    AsyncActionProgress progressReportMock;

    @InjectMocks
    DeleteBackupUtility objectUnderTest;

    @Mock
    CancelBackupService cancelBackupService;

    @Mock
    EcimBackupInfo ecimBackupInfoMock;

    @Mock
    JobActivityInfo jobActivityInfoMock;

    @Mock
    private FragmentVersionCheck fragmentVersionCheckMock;

    @Mock
    private DeleteSmrsBackupUtil deleteSmrsBackupServiceMock;

    @Mock
    private NetworkElement networkElementMock;

    @Mock
    private EcimCommonUtils ecimCommonUtilsMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelperMock;

    @Mock
    private Map<String, Object> processVariables;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private JobLogUtil jobLogUtilMock;

    @Mock
    private NEJobStaticData neJobStaticDataMock;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProviderMock;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxyMock;

    final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(backupName, backupDomain, backupType);

    private void mockJobEnvironment() throws JobDataNotFoundException {
        final List<Map<String, String>> jobProperties = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(neJobStaticDataProviderMock.getNeJobStaticData(eq(activityJobId), anyString())).thenReturn(neJobStaticDataMock);
        when(neJobStaticDataMock.getActivityStartTime()).thenReturn(((Date) activityJobAttributes.get(ShmConstants.ACTIVITY_START_DATE)).getTime());
        when(neJobStaticDataMock.getNodeName()).thenReturn(nodeName);
        when(neJobStaticDataMock.getMainJobId()).thenReturn(mainJobId);
        when(jobConfigServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
    }

    @Test
    public void testDeleteBackupFromNodeSuccess() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockJobEnvironment();
        setUpBackupAttributes();
        when(brmMoServiceRetryProxyMock.getNotifiableMoFdn(com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(
                brmBackupManagerMoFdn);
        when(brmMoServiceRetryProxyMock.executeMoAction(nodeName, ecimBackupInfoMock, brmBackupManagerMoFdn, com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants.DELETE_BACKUP))
                .thenReturn(0);
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        networkElementsList.add(networkElement);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(networkElementsList);
        activityJobAttributes = new HashMap<>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        jobActivityInfoMock = new JobActivityInfo(activityJobId, ActivityConstants.DELETE_BACKUP, JobTypeEnum.DELETEBACKUP, PlatformTypeEnum.ECIM);
        objectUnderTest.deleteBackupFromNode(activityJobId, neJobStaticDataMock, "backup1|domain1|type1", JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock);
        verify(systemRecorderMock).recordCommand(SHMEvents.DELETE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, brmBackupManagerMoFdn,
                activityUtilsMock.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.DELETEBACKUP));
    }

    @Test
    public void testDeleteBackupWhenDeleteBackupIsUnavailableOnNode() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockJobEnvironment();
        setUpBackupAttributes();
        when(brmMoServiceRetryProxyMock.getNotifiableMoFdn(com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(
                brmBackupManagerMoFdn);
        when(brmMoServiceRetryProxyMock.executeMoAction(nodeName, ecimBackupInfoMock, brmBackupManagerMoFdn, com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants.DELETE_BACKUP))
                .thenThrow(new RuntimeException(JobLogConstants.BACKUP_DOES_NOT_EXISTS));
        final List<NetworkElement> networkElementsList = new ArrayList<>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        networkElementsList.add(networkElement);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(networkElementsList);
        activityJobAttributes = new HashMap<>();
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<>();
        final Map<String, Object> processVariablesMap = new HashMap<>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        jobActivityInfoMock = new JobActivityInfo(activityJobId, ActivityConstants.DELETE_BACKUP, JobTypeEnum.DELETEBACKUP, PlatformTypeEnum.ECIM);
        objectUnderTest.deleteBackupFromNode(activityJobId, neJobStaticDataMock, "backup1|domain1|type1", JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock);
        verify(activityUtilsMock).sendNotificationToWFS(neJobStaticDataMock, activityJobId, EcimBackupConstants.DELETE_BACKUP, processVariablesMap);
    }

    private void setUpBackupAttributes() throws MoNotFoundException, UnsupportedFragmentException {
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        final List<String> backupNameList = new ArrayList<>();
        backupNameList.add("backup1");
        final List<String> validBackupDataList = new ArrayList<>();
        validBackupDataList.add("backup1|domain1|type1");
        when(brmMoServiceRetryProxyMock.getBackupDetails(backupNameList, nodeName, "domain1", "type1")).thenReturn(validBackupDataList);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(validBackupDataList)).thenReturn("backup1|domain1|type1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteBackupFromNodeFailure() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockJobEnvironment();
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        final List<String> backupNameList = new ArrayList<String>();
        backupNameList.add("backup1");
        final List<String> ValidbackupDataList = new ArrayList<String>();
        ValidbackupDataList.add("backup1|domain1|type1");
        when(brmMoServiceRetryProxyMock.getBackupDetails(backupNameList, nodeName, "domain1", "type1")).thenReturn(ValidbackupDataList);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(ValidbackupDataList)).thenReturn("backup1|domain1|type1");
        when(brmMoServiceRetryProxyMock.getNotifiableMoFdn(com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(
                brmBackupManagerMoFdn);
        when(brmMoServiceRetryProxyMock.executeMoAction(nodeName, ecimBackupInfoMock, brmBackupManagerMoFdn, com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants.DELETE_BACKUP))
                .thenThrow(Exception.class);
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        networkElementsList.add(networkElement);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(networkElementsList);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        objectUnderTest.deleteBackupFromNode(activityJobId, neJobStaticDataMock, "backup1|domain1|type1", JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock);
        verify(systemRecorderMock).recordCommand(SHMEvents.DELETE_BACKUP_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, nodeName, brmBackupManagerMoFdn,
                activityUtilsMock.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.DELETEBACKUP));
    }

    @Test
    public void testHandleNotificationSuccess() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockJobEnvironment();
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributesMock);
        when(brmMoServiceRetryProxyMock.getValidAsyncActionProgress(nodeName, EcimBackupConstants.DELETE_BACKUP, modifiedAttributesMock)).thenReturn(progressReportMock);
        when(progressReportMock.getActionName()).thenReturn(EcimBackupConstants.BACKUP_DELETE_ACTION);
        when(activityUtilsMock.getMoFdnFromNotificationSubject(notificationSubjectMock)).thenReturn(brmBackupManagerMoFdn);
        final Date notificationTime = new Date();
        when(activityUtilsMock.getNotificationTimeStamp(notificationSubjectMock)).thenReturn(notificationTime);
        when(ecimCommonUtilsMock.handleMoActionProgressReportState(jobLogList, activityJobId, progressReportMock, notificationTime, EcimBackupConstants.DELETE_BACKUP, "backup1")).thenReturn(
                JobResult.SUCCESS);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, ACTIVITY_END_PROGRESS_PERCENTAGE);
        when(progressReportMock.getProgressPercentage()).thenReturn((int) ACTIVITY_END_PROGRESS_PERCENTAGE);
        when(ecimCommonUtilsMock.getCountOfTotalBackups(activityJobAttributes)).thenReturn(1);
        when(activityUtilsMock.getActivityJobAttributeValue(activityJobAttributes, BackupActivityConstants.INTERMEDIATE_FAILURE)).thenReturn(null);
        when(activityUtilsMock.getActivityJobAttributeValue(activityJobAttributes, BackupActivityConstants.PROCESSED_BACKUPS)).thenReturn("1");
        objectUnderTest.handleNotification(ecimBackupInfoMock, notificationSubjectMock, modifiedAttributesMock, neJobStaticDataMock, JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock);
        verify(activityUtilsMock).prepareJobPropertyList(Collections.<Map<String, Object>> emptyList(), ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.getJobResult());
    }

    @Test
    public void testHandleNotificationFailure() throws MoNotFoundException, UnsupportedFragmentException {
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributesMock);
        when(brmMoServiceRetryProxyMock.getValidAsyncActionProgress(nodeName, com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants.DELETE_BACKUP, modifiedAttributesMock))
                .thenReturn(progressReportMock);
        when(progressReportMock.getActionName()).thenReturn(EcimBackupConstants.BACKUP_DELETE_ACTION);
        when(activityUtilsMock.getMoFdnFromNotificationSubject(notificationSubjectMock)).thenReturn(brmBackupManagerMoFdn);
        final Date notificationTime = new Date();
        when(activityUtilsMock.getNotificationTimeStamp(notificationSubjectMock)).thenReturn(notificationTime);
        when(ecimCommonUtilsMock.handleMoActionProgressReportState(jobLogList, activityJobId, progressReportMock, notificationTime, EcimBackupConstants.DELETE_BACKUP, "backup1")).thenReturn(
                JobResult.FAILED);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        objectUnderTest.handleNotification(ecimBackupInfoMock, notificationSubjectMock, modifiedAttributesMock, neJobStaticDataMock, JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock);
        verify(activityUtilsMock).prepareJobPropertyList(Collections.<Map<String, Object>> emptyList(), ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
    }

    @Test
    public void testHandleNotificationRunning() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockJobEnvironment();
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributesMock);
        when(brmMoServiceRetryProxyMock.getValidAsyncActionProgress(nodeName, com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants.DELETE_BACKUP, modifiedAttributesMock))
                .thenReturn(progressReportMock);
        when(progressReportMock.getActionName()).thenReturn(EcimBackupConstants.BACKUP_DELETE_ACTION);
        when(activityUtilsMock.getMoFdnFromNotificationSubject(notificationSubjectMock)).thenReturn(brmBackupManagerMoFdn);
        final Date notificationTime = new Date();
        when(activityUtilsMock.getNotificationTimeStamp(notificationSubjectMock)).thenReturn(notificationTime);
        when(ecimCommonUtilsMock.handleMoActionProgressReportState(jobLogList, activityJobId, progressReportMock, notificationTime, EcimBackupConstants.DELETE_BACKUP, "")).thenReturn(null);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, Object>> activityJobPropertyList = new ArrayList<Map<String, Object>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        objectUnderTest.handleNotification(ecimBackupInfoMock, notificationSubjectMock, modifiedAttributesMock, neJobStaticDataMock, JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock);
        verify(jobUpdateServiceMock).readAndUpdateRunningJobAttributes(activityJobId, activityJobPropertyList, jobLogList, null);
    }

    @Test
    public void testHandleNotificationCancel() throws MoNotFoundException, UnsupportedFragmentException {
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributesMock);
        when(brmMoServiceRetryProxyMock.getValidAsyncActionProgress(nodeName, com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants.DELETE_BACKUP, modifiedAttributesMock))
                .thenReturn(progressReportMock);
        when(progressReportMock.getActionName()).thenReturn(EcimBackupConstants.BACKUP_CANCEL_ACTION);
        when(activityUtilsMock.getMoFdnFromNotificationSubject(notificationSubjectMock)).thenReturn(brmBackupManagerMoFdn);
        final Date notificationTime = new Date();
        when(activityUtilsMock.getNotificationTimeStamp(notificationSubjectMock)).thenReturn(notificationTime);
        when(ecimCommonUtilsMock.handleCancelProgressReportState(jobLogList, activityJobId, progressReportMock, notificationTime, EcimBackupConstants.DELETE_BACKUP)).thenReturn(JobResult.SUCCESS);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        objectUnderTest.handleNotification(ecimBackupInfoMock, notificationSubjectMock, modifiedAttributesMock, neJobStaticDataMock, JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock);
        //        verify(activityUtilsMock).prepareJobPropertyList(Collections.<Map<String, Object>> emptyList(), BackupActivityConstants.INTERMEDIATE_FAILURE, JobResult.FAILED.getJobResult());
        verify(activityUtilsMock).prepareJobPropertyList(Collections.<Map<String, Object>> emptyList(), ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
    }

    @Test
    public void testEvaluateJobResultSucceess() throws MoNotFoundException, UnsupportedFragmentException {
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        when(brmMoServiceRetryProxyMock.getNotifiableMoFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(brmBackupManagerMoFdn);
        when(brmMoServiceRetryProxyMock.isBackupDeletionCompleted(Matchers.anyString(), Matchers.any(EcimBackupInfo.class))).thenReturn(true);

        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        networkElementsList.add(networkElement);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(networkElementsList);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        assertEquals(JobResult.SUCCESS.toString(), objectUnderTest.evaluateJobResult(ecimBackupInfoMock, neJobStaticDataMock, jobLogList, JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock)
                .getJobResult());
    }

    @Test
    public void testEvaluateJobResultFailure() throws MoNotFoundException, UnsupportedFragmentException {
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        when(brmMoServiceRetryProxyMock.getNotifiableMoFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(brmBackupManagerMoFdn);
        when(brmMoServiceRetryProxyMock.isBackupDeletionCompleted(Matchers.anyString(), Matchers.any(EcimBackupInfo.class))).thenReturn(false);

        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        networkElementsList.add(networkElement);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(networkElementsList);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        assertEquals(JobResult.FAILED.toString(), objectUnderTest.evaluateJobResult(ecimBackupInfoMock, neJobStaticDataMock, jobLogList, JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock)
                .getJobResult());
    }

    @Test
    public void testEvaluateRepeatRequiredAndActivityResultSuccess() {
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(BackupActivityConstants.INTERMEDIATE_FAILURE, JobResult.SUCCESS.toString());
        activityJobPropertyList.add(jobProperty);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> mapMock = new HashMap<String, Object>();
        mapMock.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        objectUnderTest.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList);
        verify(activityUtilsMock).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
    }

    @Test
    public void testEvaluateRepeatRequiredAndActivityResultFailed() {
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        final Map<String, String> jobProperty = new HashMap<String, String>();
        jobProperty.put(BackupActivityConstants.INTERMEDIATE_FAILURE, JobResult.FAILED.toString());
        activityJobPropertyList.add(jobProperty);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> mapMock = new HashMap<String, Object>();
        mapMock.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        objectUnderTest.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.FAILED, jobPropertyList);
        verify(activityUtilsMock).prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
    }

}
