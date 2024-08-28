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
package com.ericsson.oss.services.shm.es.impl.ecim.backuphousekeeping;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.ACTIVITY_END_PROGRESS_PERCENTAGE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.backuphousekeeping.NodeBackupHousekeepingConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.DeleteBackupUtility;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupUtils;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.ecim.backup.CancelBackupService;
import com.ericsson.oss.services.shm.es.impl.ecim.common.EcimCommonUtils;
import com.ericsson.oss.services.shm.fa.api.FaBuildingBlockResponseProvider;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackup;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackupManager;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.DateTime;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(MockitoJUnitRunner.class)
public class CleanBackupServiceTest {

    long activityJobId = 1;
    long neJobId = 2;
    long mainJobId = 3;

    String nodeName = "nodeName";
    String businessKey = "businessKey";
    String inputVersion = "inputVersion";

    String brmBackupManagerMoFdn = "brmBackupManagerMoFdn";

    Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
    Map<String, Object> neJobAttributes = new HashMap<String, Object>();
    Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
    Map<String, String> propertyMap = new HashMap<String, String>();

    List<Map<String, String>> activityJobProperties = new ArrayList<Map<String, String>>();

    @InjectMocks
    private CleanBackupService objectUnderTest;

    @Mock
    EcimBackupInfo ecimBackupInfoMock;

    @Mock
    private DeleteBackupUtility deleteBackupUtilityMock;

    @Mock
    private ActivityUtils activityUtilsMock;

    @Mock
    private BrmBackup brmBackupMock;

    @Mock
    protected FdnServiceBeanRetryHelper fdnServiceBeanRetryHelperMock;

    @Mock
    private NetworkElement networkElementMock;

    @Mock
    private JobPropertyUtils jobPropertyUtilsMock;

    @Mock
    private JobUpdateService jobUpdateServiceMock;

    @Mock
    private BrmBackupManager brmBackupManagerMock;

    @Mock
    private Notification notificationMock;

    @Mock
    private NotificationSubject notificationSubjectMock;

    @Mock
    private BrmMoServiceRetryProxy brmMoServiceMock;

    @Mock
    AsyncActionProgress progressReportMock;

    @Mock
    private CancelBackupService cancelBackupServiceMock;

    @Mock
    private EcimCommonUtils ecimCommonUtilsMock;

    @Mock
    private SystemRecorder systemRecorderMock;

    @Mock
    private ActivityTimeoutsService activityTimeoutsService;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private EcimBackupUtils ecimBackupUtilsMock;

    @Mock
    Map<String, AttributeChangeData> modifiedAttributesMock;

    @Mock
    JobActivityInfo jobActivityInfoMock;

    @Mock
    private JobLogUtil jobLogUtilMock;

    @Mock
    private NEJobStaticData neJobStaticDataMock;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProviderMock;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxyMock;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private JobStaticData jobStaticDataMock;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private NetworkElementData networkElementData;

    @Mock
    private FaBuildingBlockResponseProvider buildingBlockResponseProvider;

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckSucess_ValidBackupOnNode() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockJobEnvironment();
        final List<BrmBackup> brmBackups = new ArrayList<BrmBackup>();
        brmBackups.add(brmBackupMock);
        brmBackups.add(brmBackupMock);
        brmBackups.add(brmBackupMock);
        int timeout = 10;
        int updatedTimeout = timeout * brmBackups.size();
        final List<String> neFdns = new ArrayList<String>();
        neFdns.add(nodeName);
        final List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        networkElements.add(networkElementMock);
        final Map<String, String> keyValueMap = new HashMap<String, String>();
        keyValueMap.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "0");

        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        when(neJobStaticDataMock.getNodeName()).thenReturn(nodeName);
        when(brmMoServiceMock.getBrmBackups(nodeName)).thenReturn(brmBackups);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neFdns, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY)).thenReturn(networkElements);
        when(networkElementRetrievalBean.getNetworkElementData(neJobStaticDataMock.getNodeName())).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn("RadioNode");
        when(networkElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM);
        when(activityTimeoutsService.getActivityTimeoutAsInteger(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(timeout);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(brmBackupMock.getCreationTime()).thenReturn(new DateTime("123456"));
        when(brmBackupMock.getBrmBackupManager()).thenReturn(brmBackupManagerMock);
        when(brmBackupManagerMock.getBackupDomain()).thenReturn("Domain");
        when(brmBackupManagerMock.getBackupType()).thenReturn("BackupType");
        when(brmBackupMock.getCreationTime()).thenReturn(new DateTime("Thu Jun 21 17:32:05 2012"), new DateTime("Thu Jun 23 17:32:05 2013"));
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(keyValueMap);
        when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble())).thenReturn(true);

        final List<String> keyList = new ArrayList<String>();
        keyList.add(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE);

        when(jobPropertyUtils.getPropertyValue(keyList, null, nodeName, "SGSN", PlatformTypeEnum.ECIM.name())).thenReturn(keyValueMap);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticDataMock, EcimBackupConstants.DELETE_BACKUP)).thenReturn(true);

        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
        verify(buildingBlockResponseProvider, times(1)).sendUpdatedActivityTimeout(activityJobId, neJobStaticDataMock, activityJobAttributes, EcimBackupConstants.DELETE_BACKUP, updatedTimeout);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckFailure_ValidBackupOnNode() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockJobEnvironment();
        final List<BrmBackup> brmBackups = new ArrayList<BrmBackup>();
        brmBackups.add(brmBackupMock);

        final List<String> neFdns = new ArrayList<String>();
        neFdns.add(nodeName);
        final List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        networkElements.add(networkElementMock);
        final Map<String, String> keyValueMap = new HashMap<String, String>();
        keyValueMap.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "2");

        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        when(neJobStaticDataMock.getNodeName()).thenReturn(nodeName);
        when(brmMoServiceMock.getBrmBackups(nodeName)).thenReturn(brmBackups);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neFdns, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY)).thenReturn(networkElements);
        when(networkElementMock.getNeType()).thenReturn("SGSN");
        when(networkElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM);
        when(brmBackupMock.getCreationTime()).thenReturn(new DateTime("123456"));
        when(brmBackupMock.getBrmBackupManager()).thenReturn(brmBackupManagerMock);
        when(brmBackupManagerMock.getBackupDomain()).thenReturn("Domain");
        when(brmBackupManagerMock.getBackupType()).thenReturn("BackupType");
        final List<String> keyList = new ArrayList<String>();
        keyList.add(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(keyValueMap);
        when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble())).thenReturn(true);

        when(jobPropertyUtils.getPropertyValue(keyList, null, nodeName, "SGSN", PlatformTypeEnum.ECIM.name())).thenReturn(keyValueMap);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticDataMock, EcimBackupConstants.DELETE_BACKUP)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckFailure() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        final List<BrmBackup> brmBackups = new ArrayList<BrmBackup>();
        brmBackups.add(brmBackupMock);
        brmBackups.add(brmBackupMock);
        final List<String> neFdns = new ArrayList<String>();
        neFdns.add(nodeName);
        final List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        networkElements.add(networkElementMock);
        final Map<String, String> keyValueMap = new HashMap<String, String>();
        keyValueMap.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "2");
        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        when(neJobStaticDataMock.getNodeName()).thenReturn(nodeName);
        when(brmMoServiceMock.getBrmBackups(nodeName)).thenReturn(brmBackups);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neFdns)).thenReturn(networkElements);
        when(networkElementMock.getNeType()).thenReturn("SGSN");
        when(networkElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM);
        // when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(keyValueMap);
        when(brmBackupMock.getCreationTime()).thenReturn(new DateTime("123456"));
        when(brmBackupMock.getBrmBackupManager()).thenReturn(brmBackupManagerMock);
        when(brmBackupManagerMock.getBackupDomain()).thenReturn("Domain");
        when(brmBackupManagerMock.getBackupType()).thenReturn("BackupType");
        final List<String> keyList = new ArrayList<String>();
        keyList.add(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString())).thenReturn(keyValueMap);
        when(jobPropertyUtils.getPropertyValue(keyList, null, nodeName, "SGSN", PlatformTypeEnum.ECIM.name())).thenReturn(keyValueMap);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testExecute() throws UnsupportedFragmentException, MoNotFoundException, JobDataNotFoundException {
        mockJobEnvironment();

        // mocking for getPrecheckResponse()
        final List<BrmBackup> brmBackups = new ArrayList<BrmBackup>();
        brmBackups.add(brmBackupMock);
        brmBackups.add(brmBackupMock);
        brmBackups.add(brmBackupMock);
        final List<String> neFdns = new ArrayList<String>();
        neFdns.add(nodeName);
        final List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        networkElements.add(networkElementMock);
        final Map<String, String> keyValueMap = new HashMap<String, String>();
        keyValueMap.put(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE, "0");
        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        when(neJobStaticDataMock.getNodeName()).thenReturn(nodeName);
        when(brmMoServiceMock.getBrmBackups(nodeName)).thenReturn(brmBackups);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(neFdns, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY)).thenReturn(networkElements);
        when(networkElementMock.getNeType()).thenReturn("SGSN");
        when(networkElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM);
        when(brmBackupMock.getCreationTime()).thenReturn(new DateTime("123456"));
        when(brmBackupMock.getBrmBackupManager()).thenReturn(brmBackupManagerMock);
        when(brmBackupManagerMock.getBackupDomain()).thenReturn("Domain");
        when(brmBackupManagerMock.getBackupType()).thenReturn("BackupType");
        when(brmBackupMock.getCreationTime()).thenReturn(new DateTime("Thu Jun 21 17:32:05 2012"), new DateTime("Thu Jun 23 17:32:05 2013"));
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString())).thenReturn(keyValueMap);
        when(jobUpdateServiceMock.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble())).thenReturn(true);

        final List<String> keyList = new ArrayList<String>();
        keyList.add(NodeBackupHousekeepingConstants.MAX_BACKUPS_TO_KEEP_ON_NODE);
        when(jobPropertyUtils.getPropertyValue(keyList, null, nodeName, "SGSN", PlatformTypeEnum.ECIM.name())).thenReturn(keyValueMap);

        // Execute
        when(activityUtilsMock.getActivityJobAttributeValue(activityJobAttributes, NodeBackupHousekeepingConstants.BACKUPS_TO_BE_PROCESSED_FOR_DELETION))
                .thenReturn("backup1|domain1|type1,backup2|domain2|type2,backup3|domain3|type3");
        final List<String> backupList = new ArrayList<String>();
        backupList.add("backup1|domain1|type1");
        backupList.add("backup2|domain2|type2");
        backupList.add("backup3|domain3|type3");
        when(ecimBackupUtilsMock.prepareBackupDataList("backup1|domain1|type1,backup2|domain2|type2,backup3|domain3|type3")).thenReturn(backupList);
        when(deleteBackupUtilityMock.getBackupNameToBeProcessed(backupList, activityJobAttributes, activityJobId)).thenReturn("backup1|domain1|type1");
        activityJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, ACTIVITY_END_PROGRESS_PERCENTAGE);
        when(activityUtilsMock.getActivityInfo(activityJobId, CleanBackupService.class)).thenReturn(jobActivityInfoMock);
        objectUnderTest.execute(activityJobId);
        verify(deleteBackupUtilityMock).deleteBackupFromNode(activityJobId, neJobStaticDataMock, "backup1|domain1|type1", JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock);
        //verify(activityUtilsMock, times(1)).persistStepDurations(eq(activityJobId), anyLong(), Matchers.any(ActivityStepsEnum.class));

    }

    @Test
    public void testProcessNotification() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockJobEnvironment();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1|domain1|type1");
        when(activityUtilsMock.getActivityJobAttributeValue(activityJobAttributes, EcimBackupConstants.CURRENT_BACKUP)).thenReturn("backup1|domain1|type1");
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributesMock);
        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        when(brmMoServiceMock.getValidAsyncActionProgress(nodeName, EcimBackupConstants.DELETE_BACKUP, modifiedAttributesMock)).thenReturn(progressReportMock);
        when(progressReportMock.getActionName()).thenReturn(EcimBackupConstants.BACKUP_CANCEL_ACTION);
        when(activityUtilsMock.getMoFdnFromNotificationSubject(notificationSubjectMock)).thenReturn(brmBackupManagerMoFdn);
        final Date notificationTime = new Date();
        when(activityUtilsMock.getNotificationTimeStamp(notificationSubjectMock)).thenReturn(notificationTime);
        when(ecimCommonUtilsMock.handleCancelProgressReportState(jobLogList, activityJobId, progressReportMock, notificationTime, EcimBackupConstants.DELETE_BACKUP)).thenReturn(JobResult.FAILED);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(activityUtilsMock.getActivityInfo(activityJobId, CleanBackupService.class)).thenReturn(jobActivityInfoMock);
        objectUnderTest.processNotification(notificationMock);
        verify(deleteBackupUtilityMock).handleNotification(ecimBackupInfoMock, notificationSubjectMock, modifiedAttributesMock, neJobStaticDataMock, JobTypeEnum.BACKUP_HOUSEKEEPING,
                jobActivityInfoMock);
    }

    @Test
    public void testHandleTimeoutSuccess() throws JobDataNotFoundException {
        mockJobEnvironment();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, true);
        when(activityUtilsMock.getActivityJobAttributeValue(activityJobAttributes, EcimBackupConstants.CURRENT_BACKUP)).thenReturn("backup1|domain1|type1");
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1|domain1|type1");
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        when(activityUtilsMock.getActivityInfo(activityJobId, CleanBackupService.class)).thenReturn(jobActivityInfoMock);
        when(deleteBackupUtilityMock.evaluateJobResult(ecimBackupInfoMock, neJobStaticDataMock, jobLogList, JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock)).thenReturn(JobResult.SUCCESS);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testHandleTimeoutFailure() throws JobDataNotFoundException {
        mockJobEnvironment();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        jobLogList.clear();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, false);
        when(activityUtilsMock.getActivityJobAttributeValue(activityJobAttributes, EcimBackupConstants.CURRENT_BACKUP)).thenReturn("backup1|domain1|type1");
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1|domain1|type1");
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        when(activityUtilsMock.getActivityInfo(activityJobId, CleanBackupService.class)).thenReturn(jobActivityInfoMock);
        when(deleteBackupUtilityMock.evaluateJobResult(ecimBackupInfoMock, neJobStaticDataMock, jobLogList, JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock)).thenReturn(JobResult.FAILED);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.FAILED, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testHandleTimeoutRepeatRequired() throws JobDataNotFoundException {
        mockJobEnvironment();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, true);
        when(activityUtilsMock.getActivityJobAttributeValue(activityJobAttributes, EcimBackupConstants.CURRENT_BACKUP)).thenReturn("backup1|domain1|type1");
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1|domain1|type1");
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        when(activityUtilsMock.getActivityInfo(activityJobId, CleanBackupService.class)).thenReturn(jobActivityInfoMock);
        when(deleteBackupUtilityMock.evaluateJobResult(ecimBackupInfoMock, neJobStaticDataMock, jobLogList, JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock)).thenReturn(JobResult.SUCCESS);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        assertEquals(ActivityStepResultEnum.REPEAT_EXECUTE, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAsyncHandleTimeoutForException() throws Exception {
        when(neJobStaticDataProviderMock.getNeJobStaticData(eq(activityJobId), anyString())).thenThrow(Exception.class);
        objectUnderTest.asyncHandleTimeout(activityJobId);
        verify(activityUtilsMock, times(1)).handleExceptionForHandleTimeoutScenarios(anyLong(), anyString(), anyString());
    }

    @Test
    public void testCancelSuccess() throws JobDataNotFoundException {
        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        when(neJobStaticDataMock.getNodeName()).thenReturn(nodeName);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(Arrays.asList(networkElementMock));
        when(networkElementMock.getNeType()).thenReturn("SGSN");
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1|domain1|type1");
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        objectUnderTest.cancel(activityJobId);
        verify(cancelBackupServiceMock).cancel(Matchers.anyLong(), Matchers.anyString(), Matchers.any(EcimBackupInfo.class));
    }

    @Test
    public void testCancelTimeoutSuccess() throws JobDataNotFoundException {
        mockJobEnvironment();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, true);
        when(activityUtilsMock.getActivityJobAttributeValue(activityJobAttributes, EcimBackupConstants.CURRENT_BACKUP)).thenReturn("backup1|domain1|type1");
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1|domain1|type1");
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        when(activityUtilsMock.getActivityInfo(activityJobId, CleanBackupService.class)).thenReturn(jobActivityInfoMock);
        when(deleteBackupUtilityMock.evaluateJobResult(ecimBackupInfoMock, neJobStaticDataMock, jobLogList, JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock)).thenReturn(JobResult.SUCCESS);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, objectUnderTest.cancelTimeout(activityJobId, false).getActivityResultEnum());
    }

    @Test
    public void testCancelTimeoutFailure() throws JobDataNotFoundException {
        mockJobEnvironment();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, false);
        when(activityUtilsMock.getActivityJobAttributeValue(activityJobAttributes, EcimBackupConstants.CURRENT_BACKUP)).thenReturn("backup1|domain1|type1");
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1|domain1|type1");
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        when(ecimBackupInfoMock.getBackupName()).thenReturn("backup1");
        when(ecimBackupInfoMock.getDomainName()).thenReturn("domain1");
        when(ecimBackupInfoMock.getBackupType()).thenReturn("type1");
        when(activityUtilsMock.getActivityInfo(activityJobId, CleanBackupService.class)).thenReturn(jobActivityInfoMock);
        when(deleteBackupUtilityMock.evaluateJobResult(ecimBackupInfoMock, neJobStaticDataMock, jobLogList, JobTypeEnum.BACKUP_HOUSEKEEPING, jobActivityInfoMock)).thenReturn(JobResult.FAILED);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.FAILED, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.cancelTimeout(activityJobId, false).getActivityResultEnum());
    }

    @Test
    public void testProcessNotificationWithNonAVCEvent() {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        objectUnderTest.processNotification(notificationMock);
        verify(activityUtilsMock, times(0)).getModifiedAttributes(notificationMock.getDpsDataChangedEvent());
    }

    private void mockJobEnvironment() throws JobDataNotFoundException {
        final List<Map<String, String>> jobProperties = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(neJobStaticDataProviderMock.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_HOUSEKEEPING_JOB_CAPABILITY)).thenReturn(neJobStaticDataMock);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);

        when(neJobStaticDataMock.getActivityStartTime()).thenReturn(((Date) activityJobAttributes.get(ShmConstants.ACTIVITY_START_DATE)).getTime());
        when(neJobStaticDataMock.getNodeName()).thenReturn(nodeName);
        when(neJobStaticDataMock.getMainJobId()).thenReturn(mainJobId);
        when(jobConfigServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);

    }

}
