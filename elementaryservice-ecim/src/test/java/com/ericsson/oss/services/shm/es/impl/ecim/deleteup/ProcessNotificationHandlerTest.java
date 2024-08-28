package com.ericsson.oss.services.shm.es.impl.ecim.deleteup;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectDeletedEvent;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.ecim.deleteup.ProcessNotificationHandler;
import com.ericsson.oss.services.shm.es.impl.ecim.common.EcimCommonUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackup;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackupCreationType;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackupManager;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ProcessNotificationHandlerTest {

    @InjectMocks
    private ProcessNotificationHandler processNotificationHandler;
    @Mock
    private Notification notification;
    @Mock
    private NotificationSubject notificationSubject;
    @Mock
    private ActivityUtils activityUtils;
    @Mock
    private AsyncActionProgress progressReport;
    @Mock
    private DeleteUpgradePackageJobDataCollectorRetryProxy deleteUpDataCollectRetryProxy;
    @Mock
    private JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;
    @Mock
    private DeleteUpgradePackageDataCollector deleteUpJobDataCollector;
    @Mock
    private SystemRecorder systemRecorder;
    @Mock
    private JobLogUtil jobLogUtil;
    @Mock
    private JobUpdateService jobUpdateService;
    @Mock
    private EcimCommonUtils ecimCommonUtils;
    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;
    @Mock
    private NEJobStaticData neJobStaticData;
    @Mock
    private NetworkElementRetrievalBean networkElementRetrivalBean;
    @Mock
    private JobStaticDataProvider jobStaticDataProvider;
    @Mock
    private JobStaticData jobStaticData;
    @Mock
    private NetworkElementData networkElementData;
    @Mock
    private JobEnvironment jobEnvironment;
    @Mock
    private AttributeChangeData attributeChangeData;
    @Mock
    private DpsObjectDeletedEvent dpsObjectDeletedEvent;
    @Mock
    private JobActivityInfo jobActivityInfo;
    private long activityId = 123;
    private String platformType = "ECIM";
    private String nodeName = "LTE01dg2";
    private String nodeType = "RadioNode";
    private String neFdn = "NeFDN";
    private Map<String, AttributeChangeData> modifiedAttributes = new HashMap<>();
    private Map<String, Object> activityJobAttributes = new HashMap<>();
    private Map<String, Set<String>> upsWithSyscrBkps = new HashMap<>();
    String backupFdn = "BrmBackup=1";
    String backupData = "BackupName" + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + "ManagerFdn" + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR
            + BrmBackupCreationType.SYSTEM_CREATED.name() + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + backupFdn;
    Set<String> upDataSet;
    Set<String> nodeUpData;
    List<BrmBackup> brmBackupsList;

    @Test
    public void testProcessNotification_DeleteAllInActiveUPs_forDeleteEvent() throws JobDataNotFoundException, MoNotFoundException {
        initializeVariables();
        initializeData();
        commonStubs();

        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.DELETE);
        when(notification.getDpsDataChangedEvent()).thenReturn(dpsObjectDeletedEvent);

        when(activityUtils.getModifiedAttributes(Matchers.any(DpsDataChangedEvent.class))).thenReturn(modifiedAttributes);
        when(activityUtils.getActivityJobAttributeValue(Matchers.anyMap(), Matchers.anyString())).thenReturn("");
        when(deleteUpJobDataCollector.getDeletedSysCreatedBackupInfo(Matchers.anyMap(), Matchers.anyString())).thenReturn(backupData);
        doNothing().when(deleteUpJobDataCollector).unSubscribeToBrmBackupMosNotifications(Matchers.anyMap(), Matchers.anyLong());
        processNotificationHandler.processNotification(notification);
        verify(deleteUpJobDataCollector,times(1)).getDeletedSysCreatedBackupInfo(Matchers.anyMap(),Matchers.anyString());
        verify(deleteUpJobDataCollector,times(1)).unSubscribeToBrmBackupMosNotifications(Matchers.anyMap(),Matchers.anyLong());
        verify(activityUtils,times(1)).sendNotificationToWFS((NEJobStaticData) Matchers.anyObject(),Matchers.anyLong(),Matchers.anyString(),Matchers.anyMap());
    }

    @Test
    public void testProcessNotification_DeleteAllInActiveUPs_forAvcEvent() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        initializeVariables();
        initializeData();
        commonStubs();
        Map<String, Object> progressAttributes = new HashMap<String, Object>();
        progressAttributes.put("actionName", EcimBackupConstants.BACKUP_DELETE_ACTION);
        progressReport = new AsyncActionProgress(progressAttributes);

        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);

        when(activityUtils.getModifiedAttributes(Matchers.any(DpsDataChangedEvent.class))).thenReturn(modifiedAttributes);
        when(activityUtils.getActivityJobAttributeValue(Matchers.anyMap(), Matchers.anyString())).thenReturn("");
        when(deleteUpJobDataCollector.checkForDeletedSysCreatedBackupInfo(Matchers.anyMap(), Matchers.anyString())).thenReturn(backupData);
        when(deleteUpDataCollectRetryProxy.getValidAsyncActionProgress(Matchers.anyString(), Matchers.anyMap(), Matchers.anyString(),
                Matchers.anyString())).thenReturn(progressReport);
        doNothing().when(deleteUpJobDataCollector).unSubscribeToBrmBackupMosNotifications(Matchers.anyMap(), Matchers.anyLong());

        when(ecimCommonUtils.handleMoActionProgressReportState(Matchers.anyList(), Matchers.anyLong(), Matchers.any(AsyncActionProgress.class),
                Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString())).thenReturn(JobResult.SUCCESS);

        processNotificationHandler.processNotification(notification);

        verify(deleteUpJobDataCollector,times(1)).checkForDeletedSysCreatedBackupInfo(Matchers.anyMap(),Matchers.anyString());
        verify(deleteUpJobDataCollector,times(1)).unSubscribeToBrmBackupMosNotifications(Matchers.anyMap(),Matchers.anyLong());
        verify(activityUtils,times(1)).sendNotificationToWFS((NEJobStaticData) Matchers.anyObject(),Matchers.anyLong(),Matchers.anyString(),Matchers.anyMap());
    }

    @Test
    public void testProcessNotification_DeleteAllInActiveUPs_forAvcEvent_BackupEmpty() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        initializeVariables();
        initializeData();
        commonStubs();
        Map<String, Object> progressAttributes = new HashMap<String, Object>();
        progressAttributes.put("actionName", EcimBackupConstants.BACKUP_DELETE_ACTION);
        progressReport = new AsyncActionProgress(progressAttributes);
        when(notification.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(deleteUpDataCollectRetryProxy.getValidAsyncActionProgress(Matchers.anyString(), Matchers.anyMap(), Matchers.anyString(),
                Matchers.anyString())).thenReturn(progressReport);
        when(activityUtils.getModifiedAttributes(Matchers.any(DpsDataChangedEvent.class))).thenReturn(modifiedAttributes);
        when(activityUtils.getActivityJobAttributeValue(Matchers.anyMap(), Matchers.anyString())).thenReturn("");
        when(deleteUpJobDataCollector.checkForDeletedSysCreatedBackupInfo(Matchers.anyMap(), Matchers.anyString())).thenReturn("");

        doNothing().when(deleteUpJobDataCollector).unSubscribeToBrmBackupMosNotifications(Matchers.anyMap(), Matchers.anyLong());

        when(ecimCommonUtils.handleMoActionProgressReportState(Matchers.anyList(), Matchers.anyLong(), Matchers.any(AsyncActionProgress.class),
                Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString())).thenReturn(JobResult.SUCCESS);

        processNotificationHandler.processNotification(notification);
        verify(deleteUpJobDataCollector,times(1)).checkForDeletedSysCreatedBackupInfo(Matchers.anyMap(),Matchers.anyString());
        verify(deleteUpJobDataCollector,times(1)).unSubscribeToBrmBackupMosNotifications(Matchers.anyMap(),Matchers.anyLong());
    }

    private void commonStubs(){
        when(notification.getNotificationSubject()).thenReturn(notificationSubject);
        when(activityUtils.getActivityJobId(Matchers.any(NotificationSubject.class))).thenReturn(activityId);
        when(activityUtils.getNotificationTimeStamp(Matchers.any(NotificationSubject.class))).thenReturn(new Date());
        when(jobConfigurationServiceProxy.getActivityJobAttributes(Matchers.anyLong())).thenReturn(activityJobAttributes);

        when(deleteUpJobDataCollector.converToMap(Matchers.anyString())).thenReturn(upsWithSyscrBkps);

        doNothing().when(systemRecorder).recordCommand(Matchers.anyString(), Matchers.any(CommandPhase.class), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        doNothing().when(jobLogUtil).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble())).thenReturn(true);

        when(activityUtils.unSubscribeToMoNotifications(Matchers.anyString(), Matchers.anyLong(), Matchers.any(JobActivityInfo.class))).thenReturn(true);

        when(deleteUpJobDataCollector.setBackupDataForNextItration(Matchers.anyLong(), Matchers.anyString())).thenReturn(true);
        doNothing().when(activityUtils).sendNotificationToWFS(Matchers.any(NEJobStaticData.class), Matchers.anyLong(), Matchers.anyString(), Matchers.anyMap());
    }

    /**
     * @throws JobDataNotFoundException
     * @throws MoNotFoundException
     */
    private void initializeVariables() throws JobDataNotFoundException, MoNotFoundException {
        modifiedAttributes.put("modifiedAttribute", attributeChangeData);
        when(neJobStaticDataProvider.getNeJobStaticData(Matchers.anyLong(), Matchers.anyString())).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(12345l);
        when(jobStaticDataProvider.getJobStaticData(Matchers.anyLong())).thenReturn(jobStaticData);
        when(neJobStaticData.getPlatformType()).thenReturn(platformType);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn(nodeType);
        when(networkElementData.getNeFdn()).thenReturn(neFdn);
        when(activityUtils.getJobEnvironment(Matchers.anyLong())).thenReturn(jobEnvironment);
    }

    private void initializeData() {
        String updata = "ProdNum" + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + "ProdRev" + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + "MoFdn=1";
        upDataSet = new HashSet<>();
        nodeUpData = new HashSet<>();
        upDataSet.add(updata);
        nodeUpData.add(updata);
        String brmBackupMoFdn = "brmBackupMoFdn=1";
        String brmBackupManagerMoFdn = "brmBackupManagerMoFdn=1";
        Map<String, Object> brmBackupManagerAttributes = new HashMap<>();
        brmBackupManagerAttributes.put("backupDomain", "System");
        brmBackupManagerAttributes.put("backupType", "Data");
        brmBackupManagerAttributes.put("brmBackupManagerId", "1");
        brmBackupManagerAttributes.put("progressReport", "report");

        BrmBackupManager brmBackupManager = new BrmBackupManager(brmBackupManagerMoFdn, brmBackupManagerAttributes);
        Map<String, Object> brmBackupMoAttributes = new HashMap<>();
        brmBackupMoAttributes.put("backupName", "BackupName");
        brmBackupMoAttributes.put("creationType", "SYSTEM");
        brmBackupMoAttributes.put("progressReport", "");
        brmBackupMoAttributes.put("status", "COMPLETED");

        BrmBackup brmBackup = new BrmBackup(brmBackupMoFdn, brmBackupMoAttributes, brmBackupManager);
        brmBackupsList = new ArrayList<>();
        brmBackupsList.add(brmBackup);

        Set<String> backups = new HashSet<>();
        backups.add(backupData);
        upsWithSyscrBkps.put(updata, backups);
    }
}
