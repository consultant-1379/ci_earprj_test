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
package com.ericsson.oss.services.shm.es.impl.ecim.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
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
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants.ReportProgress;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;

@RunWith(MockitoJUnitRunner.class)
public class EcimCommonUtilsTest {

    @InjectMocks
    private EcimCommonUtils ecimCommonUtils;

    @Mock
    AsyncActionProgress progressReportMock;

    @Mock
    private ActivityUtils activityUtilsMock;

    @Mock
    private SystemRecorder systemRecorderMock;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxy;

    long activityJobId = 1;
    long mainJobId = 3;
    private static final String actionName = "installKeyFile";
    private static final String activityName = "installKeyFile";
    String nodeName = "nodeName";
    String brmBackupManagerMoFdn = "brmBackupManagerMoFdn";

    List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
    Map<String, Object> progressReportData = new HashMap<>();

    @Test
    public void testGetValidAsyncActionProgress() {
        final Map<String, Object> reportProgress = new HashMap<String, Object>();
        reportProgress.put(ReportProgress.REPORT_PROGRESS_ACTION_NAME, actionName);
        final Map<String, AttributeChangeData> keyFileMgmtAttrs = new HashMap<String, AttributeChangeData>();
        final AttributeChangeData reportProgressAttributes = new AttributeChangeData(EcimCommonConstants.ReportProgress.ASYNC_ACTION_PROGRESS, reportProgress, reportProgress, null, null);
        keyFileMgmtAttrs.put(EcimCommonConstants.LicenseMoConstants.KEYFILEMANAGEMENT_REPORT_PROGRESS, reportProgressAttributes);
        Assert.assertNotNull(ecimCommonUtils.getValidAsyncActionProgress(activityName, keyFileMgmtAttrs));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testhandleCancelProgressReportStateRunning() {
        when(progressReportMock.getState()).thenReturn(ActionStateType.RUNNING);
        final JobResult jobResult = ecimCommonUtils.handleCancelProgressReportState(jobLogList, activityJobId, progressReportMock, new Date(), EcimBackupConstants.CREATE_BACKUP);
        Mockito.doNothing().when(activityUtilsMock).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
        Assert.assertEquals(JobResult.FAILED.toString(), jobResult.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testhandleCancelProgressReportStateFinishedSuccess() {
        when(progressReportMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReportMock.getResult()).thenReturn(ActionResultType.SUCCESS);
        final JobResult jobResult = ecimCommonUtils.handleCancelProgressReportState(jobLogList, activityJobId, progressReportMock, new Date(), EcimBackupConstants.CREATE_BACKUP);
        Mockito.doNothing().when(activityUtilsMock).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorderMock).recordCommand(Matchers.anyString(), (CommandPhase) Matchers.any(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Assert.assertEquals(JobResult.CANCELLED.toString(), jobResult.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testhandleCancelProgressReportStateFinishedFailed() {
        when(progressReportMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReportMock.getResult()).thenReturn(ActionResultType.FAILURE);
        final JobResult jobResult = ecimCommonUtils.handleCancelProgressReportState(jobLogList, activityJobId, progressReportMock, new Date(), EcimBackupConstants.CREATE_BACKUP);
        Mockito.doNothing().when(activityUtilsMock).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
        Assert.assertEquals(JobResult.FAILED.toString(), jobResult.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testhandleMoActionProgressReportStateRunning() {
        when(progressReportMock.getState()).thenReturn(ActionStateType.RUNNING);
        final JobResult jobResult = ecimCommonUtils.handleMoActionProgressReportState(jobLogList, activityJobId, progressReportMock, new Date(), EcimBackupConstants.CREATE_BACKUP, "");
        Mockito.doNothing().when(activityUtilsMock).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
        Assert.assertNull(jobResult);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testhandleMoActionProgressReportStateCancelling() {
        when(progressReportMock.getState()).thenReturn(ActionStateType.CANCELLING);
        final JobResult jobResult = ecimCommonUtils.handleMoActionProgressReportState(jobLogList, activityJobId, progressReportMock, new Date(), EcimBackupConstants.CREATE_BACKUP, "");
        Mockito.doNothing().when(activityUtilsMock).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
        Assert.assertNull(jobResult);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testhandleMoActionProgressReportStateSuccess() {
        when(progressReportMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReportMock.getResult()).thenReturn(ActionResultType.SUCCESS);
        final JobResult jobResult = ecimCommonUtils.handleMoActionProgressReportState(jobLogList, activityJobId, progressReportMock, new Date(), EcimBackupConstants.CREATE_BACKUP, "");
        Mockito.doNothing().when(activityUtilsMock).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorderMock).recordCommand(Matchers.anyString(), (CommandPhase) Matchers.any(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Assert.assertEquals(JobResult.SUCCESS.toString(), jobResult.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testhandleMoActionProgressReportStateFailed() {
        when(progressReportMock.getState()).thenReturn(ActionStateType.FINISHED);
        when(progressReportMock.getResult()).thenReturn(ActionResultType.FAILURE);
        final JobResult jobResult = ecimCommonUtils.handleMoActionProgressReportState(jobLogList, activityJobId, progressReportMock, new Date(), EcimBackupConstants.CREATE_BACKUP, "");
        Mockito.doNothing().when(activityUtilsMock).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorderMock).recordCommand(Matchers.anyString(), (CommandPhase) Matchers.any(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Assert.assertEquals(JobResult.FAILED.toString(), jobResult.getJobResult());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testhandleMoActionProgressReportStateCancelled() {
        when(progressReportMock.getState()).thenReturn(ActionStateType.CANCELLED);
        final JobResult jobResult = ecimCommonUtils.handleMoActionProgressReportState(jobLogList, activityJobId, progressReportMock, new Date(), EcimBackupConstants.CREATE_BACKUP, "");
        Mockito.doNothing().when(activityUtilsMock).prepareJobLogAtrributesList(Matchers.anyList(), Matchers.anyString(), Matchers.any(Date.class), Matchers.anyString(), Matchers.anyString());
        Mockito.doNothing().when(systemRecorderMock).recordCommand(Matchers.anyString(), (CommandPhase) Matchers.any(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        Assert.assertEquals(JobResult.FAILED.toString(), jobResult.getJobResult());
    }

    @Test
    public void testCalculateActivityProgressPercentage() {
        final double expected = 16.66;
        final double delta = 0.1;
        final Map<String, Object> activityJobAttributes = new HashMap<>();
        when(jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(activityUtilsMock.getActivityJobAttributeValue(activityJobAttributes, BackupActivityConstants.MO_ACTIVITY_END_PROGRESS)).thenReturn("0");
        when(activityUtilsMock.getActivityJobAttributeValue(activityJobAttributes, EcimBackupConstants.TOTAL_BACKUPS)).thenReturn("6");
        when(progressReportMock.getProgressPercentage()).thenReturn(Integer.parseInt("100"));
        Assert.assertEquals(expected, ecimCommonUtils.calculateActivityProgressPercentage(activityJobId, progressReportMock), delta);
    }

    /**
     * Invalid Notification, as backup name(Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000) received in notification is not same as backup name(TEST) for which action was triggered.
     */
    @Test
    public void testValidateActionProgressReportAlongWithBackupName1() {
        final String additionalInfo = "[Action started, Name: Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000, Creating database backup]";
        progressReportData.put(ReportProgress.REPORT_PROGRESS_ADDITIONAL_INFO, additionalInfo);
        progressReportData.put(ReportProgress.REPORT_PROGRESS_ACTION_NAME, EcimBackupConstants.BACKUP_CREATE_ACTION);
        final AsyncActionProgress progressReport = new AsyncActionProgress(progressReportData);
        final boolean isValidNotification = ecimCommonUtils.validateActionProgressReportAlongWithBackupName(progressReport, EcimBackupConstants.BACKUP_CREATE_ACTION_BSP,
                EcimBackupConstants.BACKUP_CREATE_ACTION, "TEST");
        assertFalse(isValidNotification);
    }

    /**
     * Valid Notification, as backup name(Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000) received in notification is same as backup
     * name(Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000) for which action was triggered. Content of Additional Info is [Action started, Name:
     * Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000, Creating database backup]
     */
    @Test
    public void testValidateActionProgressReportAlongWithBackupName2() {
        final String additionalInfo = "[Action started, Name: Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000, Creating database backup]";
        progressReportData.put(ReportProgress.REPORT_PROGRESS_ADDITIONAL_INFO, additionalInfo);
        progressReportData.put(ReportProgress.REPORT_PROGRESS_ACTION_NAME, EcimBackupConstants.BACKUP_CREATE_ACTION);
        final AsyncActionProgress progressReport = new AsyncActionProgress(progressReportData);
        final boolean isValidNotification = ecimCommonUtils.validateActionProgressReportAlongWithBackupName(progressReport, EcimBackupConstants.BACKUP_CREATE_ACTION_BSP,
                EcimBackupConstants.BACKUP_CREATE_ACTION, "Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000");
        assertTrue(isValidNotification);
    }

    /**
     * Valid Notification, as backup name(Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000) received in notification is same as backup
     * name(Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000) for which action was triggered. Content of Additional Info is [Action started, Name:
     * Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000]
     */
    @Test
    public void testValidateActionProgressReportAlongWithBackupName3() {
        final String additionalInfo = "[Action started, Name: Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000]";
        progressReportData.put(ReportProgress.REPORT_PROGRESS_ADDITIONAL_INFO, additionalInfo);
        progressReportData.put(ReportProgress.REPORT_PROGRESS_ACTION_NAME, EcimBackupConstants.BACKUP_CREATE_ACTION);
        final AsyncActionProgress progressReport = new AsyncActionProgress(progressReportData);
        final boolean isValidNotification = ecimCommonUtils.validateActionProgressReportAlongWithBackupName(progressReport, EcimBackupConstants.BACKUP_CREATE_ACTION_BSP,
                EcimBackupConstants.BACKUP_CREATE_ACTION, "Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000");
        assertTrue(isValidNotification);
    }

    /**
     * Valid Notification, as backup name doesn't exist in additionalInfo.
     */
    @Test
    public void testValidateActionProgressReportAlongWithBackupName4() {
        final String additionalInfo = "[Action started, Creating database backup]";
        progressReportData.put(ReportProgress.REPORT_PROGRESS_ADDITIONAL_INFO, additionalInfo);
        progressReportData.put(ReportProgress.REPORT_PROGRESS_ACTION_NAME, EcimBackupConstants.BACKUP_CREATE_ACTION);
        final AsyncActionProgress progressReport = new AsyncActionProgress(progressReportData);
        final boolean isValidNotification = ecimCommonUtils.validateActionProgressReportAlongWithBackupName(progressReport, EcimBackupConstants.BACKUP_CREATE_ACTION_BSP,
                EcimBackupConstants.BACKUP_CREATE_ACTION, "Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000");
        assertTrue(isValidNotification);
    }

    /**
     * Valid Notification, as additionalInfo doesn't exist.
     */
    @Test
    public void testValidateActionProgressReportAlongWithBackupName5() {
        progressReportData.put(ReportProgress.REPORT_PROGRESS_ACTION_NAME, EcimBackupConstants.BACKUP_CREATE_ACTION);
        final AsyncActionProgress progressReport = new AsyncActionProgress(progressReportData);
        final boolean isValidNotification = ecimCommonUtils.validateActionProgressReportAlongWithBackupName(progressReport, EcimBackupConstants.BACKUP_CREATE_ACTION_BSP,
                EcimBackupConstants.BACKUP_CREATE_ACTION, "Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000");
        assertTrue(isValidNotification);
    }
    
    /**
     * Valid Notification, as additionalInfo has backup name as 'BackupName: <backupName>'
     */
    @Test
    public void testValidateActionProgressReportSpecificToBSPNodes() {
        final String additionalInfo = "[08:28:58 Creating backup 7, backupName: CXP9025735_R14B07_15052019102854]";
        progressReportData.put(ReportProgress.REPORT_PROGRESS_ADDITIONAL_INFO, additionalInfo);
        progressReportData.put(ReportProgress.REPORT_PROGRESS_ACTION_NAME, EcimBackupConstants.BACKUP_CREATE_ACTION);
        final AsyncActionProgress progressReport = new AsyncActionProgress(progressReportData);
        final boolean isValidNotification = ecimCommonUtils.validateActionProgressReportAlongWithBackupName(progressReport, EcimBackupConstants.BACKUP_CREATE_ACTION_BSP,
                EcimBackupConstants.BACKUP_CREATE_ACTION, "Rollback_backup_BASEBAND_CXP9024418/6_R62A39_20190220T002959+0000");
        assertTrue(isValidNotification);
    }
}
