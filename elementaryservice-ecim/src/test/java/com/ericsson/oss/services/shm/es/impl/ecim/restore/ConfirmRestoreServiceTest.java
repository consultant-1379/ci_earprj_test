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
package com.ericsson.oss.services.shm.es.impl.ecim.restore;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupUtils;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.ecim.common.BackupMOInformationProvider;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(MockitoJUnitRunner.class)
public class ConfirmRestoreServiceTest {

    long activityJobId = 1;
    String businessKey = "2";
    String nodeName = "NetworkElement01";
    String brmRollbackAtRestoreMoFdn = "3";
    String inputVersion = "inputVersion";

    @Mock
    ActivityUtils activityUtilsMock;

    @Mock
    BrmMoServiceRetryProxy brmMoServiceMock;

    @Mock
    EcimBackupUtils ecimBackupUtilsMock;

    @Mock
    JobUpdateService jobUpdateServiceMock;

    @Mock
    Map<String, Object> mapMock;

    @Mock
    private FragmentVersionCheck fragmentVersionCheckMock;

    @InjectMocks
    ConfirmRestoreService classUnderTest;

    @Mock
    private NetworkElementData networkElementInfo;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private JobConfigurationServiceRetryProxy configurationServiceRetryProxy;

    @Mock
    private EcimBackupInfo ecimBackupInfo;

    @Mock
    JobLogUtil jobLogUtilMock;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private JobStaticData jobStaticDataMock;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private BackupMOInformationProvider backupMOInformationProvider;

    long neJobId = 2;
    long mainJobId = 3;

    private final List<Map<String, Object>> listofMap = new ArrayList<Map<String, Object>>();
    private Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
    private Map<String, Object> mainJobAttributes = new HashMap<String, Object>();

    private final Map<String, Object> emptyMap = Collections.emptyMap();

    @Test
    public void testPrecheck_success_withBrmRollbackAtRestoreAndLabelStore() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        prepareTestData();
        when(brmMoServiceMock.isConfirmRequired(nodeName)).thenReturn(true);
        when(ecimBackupUtilsMock.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn("backupName");
        when(brmMoServiceMock.isSpecifiedBackupRestored(nodeName, ecimBackupInfo)).thenReturn(true);
        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, ActivityConstants.CONFIRM_BACKUP)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, classUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @Test
    public void testPrecheck_failure_brmRollbackAtRestoreMO_doesnotExist() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        prepareTestData();
        when(ecimBackupUtilsMock.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn("backupName");
        when(brmMoServiceMock.isConfirmRequired(nodeName)).thenReturn(false);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, ActivityConstants.CONFIRM_BACKUP)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION, classUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testPrecheck_success_lastRestoredBackupNotSet() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        prepareTestData();
        //final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo("domain", "backup", "type");
        when(brmMoServiceMock.isConfirmRequired(nodeName)).thenReturn(true);
        when(ecimBackupUtilsMock.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(ecimBackupInfo.getBackupName()).thenReturn("backupName");
        when(brmMoServiceMock.isSpecifiedBackupRestored(nodeName, ecimBackupInfo)).thenReturn(false);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, ActivityConstants.CONFIRM_BACKUP)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, classUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @Test
    public void testPrecheck_failure_withMoNotFoundException() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        prepareTestData();
        final MoNotFoundException moNotFoundException = new MoNotFoundException("MoNotFound");
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo("domain", "backup", "type");
        when(ecimBackupUtilsMock.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceMock.isConfirmRequired(nodeName)).thenThrow(moNotFoundException);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, classUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testPrecheck_failure_withUnsupportedFragmentException() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        prepareTestData();
        final UnsupportedFragmentException unsupportedFragmentException = new UnsupportedFragmentException("unsupportedFragmentException");
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo("domain", "backup", "type");
        when(brmMoServiceMock.isConfirmRequired(nodeName)).thenReturn(true);
        when(ecimBackupUtilsMock.getBackupInfoForRestore(Matchers.any(NEJobStaticData.class), Matchers.any(NetworkElementData.class))).thenReturn(ecimBackupInfo);
        when(brmMoServiceMock.isSpecifiedBackupRestored(nodeName, ecimBackupInfo)).thenThrow(unsupportedFragmentException);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticDataMock, ActivityConstants.CONFIRM_BACKUP)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, classUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @Test
    public void testExecute_success() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        prepareTestData();
        List<Map<String, String>> swVersions = new ArrayList<>();
        Map<String, String> swVersionMap = new HashMap<>();
        swVersionMap.put("productRevision", "RA1234");
        swVersionMap.put("productNumber", "CXP2010055/1");
        swVersions.add(swVersionMap);
        when(brmMoServiceMock.getNotifiableMoFdn(ActivityConstants.CONFIRM_BACKUP, nodeName, null)).thenReturn(brmRollbackAtRestoreMoFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, null, brmRollbackAtRestoreMoFdn, ActivityConstants.CONFIRM_BACKUP)).thenReturn(0);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(networkElementRetrievalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        when(ecimBackupUtilsMock.getBackupInfoForRestore(neJobStaticData, networkElementInfo)).thenReturn(ecimBackupInfo);
        when(backupMOInformationProvider.getswVersionsListFromBrmBackupMOsList(networkElementInfo, ecimBackupInfo)).thenReturn(swVersions);
        classUnderTest.execute(activityJobId);
        Mockito.verify(activityUtilsMock, Mockito.times(2)).recordEvent(Matchers.eq(SHMEvents.CONFIRM_RESTORE_EXECUTE), Matchers.eq(nodeName), Matchers.eq(brmRollbackAtRestoreMoFdn),
                Matchers.anyString());
        Mockito.verify(activityUtilsMock).prepareJobPropertyList(listofMap, ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString());
        Mockito.verify(activityUtilsMock).sendActivateToWFS(businessKey, emptyMap);
        Mockito.verify(jobUpdateServiceMock).readAndUpdateRunningJobAttributes(activityJobId, listofMap, listofMap);
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.EXECUTE));
    }

    @Test
    public void testExecute_failure() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        prepareTestData();
        List<Map<String, String>> swVersions = new ArrayList<>();
        Map<String, String> swVersionMap = new HashMap<>();
        swVersionMap.put("productRevision", "RA1234");
        swVersionMap.put("productNumber", "CXP2010055/1");
        swVersions.add(swVersionMap);
        when(brmMoServiceMock.getNotifiableMoFdn(ActivityConstants.CONFIRM_BACKUP, nodeName, null)).thenReturn(brmRollbackAtRestoreMoFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, null, brmRollbackAtRestoreMoFdn, ActivityConstants.CONFIRM_BACKUP)).thenReturn(1);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(networkElementRetrievalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        when(ecimBackupUtilsMock.getBackupInfoForRestore(neJobStaticData, networkElementInfo)).thenReturn(ecimBackupInfo);
        when(backupMOInformationProvider.getswVersionsListFromBrmBackupMOsList(networkElementInfo, ecimBackupInfo)).thenReturn(swVersions);
        classUnderTest.execute(activityJobId);
        Mockito.verify(activityUtilsMock, Mockito.times(2)).recordEvent(Matchers.eq(SHMEvents.CONFIRM_RESTORE_EXECUTE), Matchers.eq(nodeName), Matchers.eq(brmRollbackAtRestoreMoFdn),
                Matchers.anyString());
        Mockito.verify(activityUtilsMock).prepareJobPropertyList(listofMap, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        Mockito.verify(activityUtilsMock).sendActivateToWFS(businessKey, emptyMap);
        Mockito.verify(jobUpdateServiceMock).readAndUpdateRunningJobAttributes(activityJobId, listofMap, listofMap);
    }

    @Test
    public void testExecute_withMoNotFoundException() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        prepareTestData();
        final MoNotFoundException moNotFoundException = new MoNotFoundException("MoNotFound");
        when(brmMoServiceMock.getNotifiableMoFdn(ActivityConstants.CONFIRM_BACKUP, nodeName, null)).thenThrow(moNotFoundException);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        classUnderTest.execute(activityJobId);
        Mockito.verify(activityUtilsMock).prepareJobPropertyList(listofMap, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        Mockito.verify(activityUtilsMock).sendActivateToWFS(businessKey, emptyMap);
        Mockito.verify(jobUpdateServiceMock).readAndUpdateRunningJobAttributes(activityJobId, listofMap, listofMap);
    }

    @Test
    public void testExecute_withMediationServiceException() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        prepareTestData();
        final Throwable cause = new Throwable("MediationServiceException");
        final Exception exception = new RuntimeException("MediationServiceExceptionMessage", cause);
        when(brmMoServiceMock.getNotifiableMoFdn(ActivityConstants.CONFIRM_BACKUP, nodeName, null)).thenReturn(brmRollbackAtRestoreMoFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, null, brmRollbackAtRestoreMoFdn, ActivityConstants.CONFIRM_BACKUP)).thenThrow(exception);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        classUnderTest.execute(activityJobId);
    }

    @Test
    public void testExecute_withUnsupportedFragmentException() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        prepareTestData();
        final UnsupportedFragmentException unsupportedFragmentException = new UnsupportedFragmentException("unsupportedFragmentException");
        when(brmMoServiceMock.getNotifiableMoFdn(ActivityConstants.CONFIRM_BACKUP, nodeName, null)).thenThrow(unsupportedFragmentException);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        classUnderTest.execute(activityJobId);
        Mockito.verify(activityUtilsMock).prepareJobPropertyList(listofMap, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        Mockito.verify(activityUtilsMock).sendActivateToWFS(businessKey, emptyMap);
        Mockito.verify(jobUpdateServiceMock).readAndUpdateRunningJobAttributes(activityJobId, listofMap, listofMap);
    }

    @Test
    public void testExecute_withArgumentBuilderException() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        prepareTestData();
        final ArgumentBuilderException argumentBuilderException = new ArgumentBuilderException("argumentBuilderException");
        when(brmMoServiceMock.getNotifiableMoFdn(ActivityConstants.CONFIRM_BACKUP, nodeName, null)).thenReturn(brmRollbackAtRestoreMoFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, null, brmRollbackAtRestoreMoFdn, ActivityConstants.CONFIRM_BACKUP)).thenThrow(argumentBuilderException);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        classUnderTest.execute(activityJobId);
        Mockito.verify(activityUtilsMock).prepareJobPropertyList(listofMap, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString());
        Mockito.verify(activityUtilsMock).sendActivateToWFS(businessKey, emptyMap);
        Mockito.verify(jobUpdateServiceMock).readAndUpdateRunningJobAttributes(activityJobId, listofMap, listofMap);
    }

    @Test
    public void testHandleTimeout_success() throws JobDataNotFoundException, MoNotFoundException {
        prepareTestData();
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(activityUtilsMock.getActivityJobAttributeValue(Matchers.anyMap(), Matchers.anyString())).thenReturn(JobResult.SUCCESS.toString());
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, classUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtilsMock).addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.SUCCESS.toString(), listofMap);
    }

    @Test
    public void testHandleTimeout_failure() throws JobDataNotFoundException, MoNotFoundException {
        prepareTestData();
        when(activityUtilsMock.getActivityJobAttributeValue(mapMock, ActivityConstants.ACTIVITY_RESULT)).thenReturn(JobResult.FAILED.toString());
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, classUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtilsMock).addJobProperty(ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.toString(), listofMap);
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    private void prepareTestData() throws JobDataNotFoundException, MoNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.RESTORE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(neJobStaticData.getNeJobId()).thenReturn(neJobId);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(neJobStaticData.getNeJobBusinessKey()).thenReturn(businessKey);
        when(neJobStaticData.getActivityStartTime()).thenReturn((new Date()).getTime());
        when(configurationServiceRetryProxy.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(configurationServiceRetryProxy.getNeJobAttributes(neJobId)).thenReturn(mainJobAttributes);
        when(configurationServiceRetryProxy.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(networkElementRetrievalBean.getNetworkElementData(Matchers.anyString())).thenReturn(networkElementInfo);
    }
}
