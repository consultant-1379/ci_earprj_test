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
package com.ericsson.oss.services.shm.es.impl.ecim.deletebackup;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.ACTIVITY_END_PROGRESS_PERCENTAGE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyList;
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
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.ecim.backup.common.DeleteBackupUtility;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants;
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupUtils;
import com.ericsson.oss.services.shm.es.impl.ActivityStepsEnum;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.DeleteSmrsBackupUtil;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.ecim.backup.CancelBackupService;
import com.ericsson.oss.services.shm.es.impl.ecim.common.EcimCommonUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.timer.NEJobProgressPercentageCache;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables;
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
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(MockitoJUnitRunner.class)
public class DeleteBackupServiceTest {

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
    private static final String BACKUPS_TO_DELETE = "BACKUPS_TO_DELETE";
    private static final String BACKUPS_TO_DELETE_SMRS = "BACKUPS_TO_DELETE_SMRS";
    private static final String DOMAIN = "DOMAIN";
    private static final String TYPE = "TYPE";
    private static final String LOCATION = "LOCATION";
    private static final String BACKUP_NAME = "BACKUP_NAME";

    List<Map<String, String>> activityJobProperties = new ArrayList<Map<String, String>>();
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
    EcimCommonUtils ecimCommonUtilsMock;

    @Mock
    BrmMoServiceRetryProxy brmMoServiceMock;

    @Mock
    Map<String, AttributeChangeData> modifiedAttributesMock;

    @Mock
    AsyncActionProgress progressReportMock;

    @InjectMocks
    DeleteBackupService objectUnderTest;

    @Mock
    CancelBackupService cancelBackupService;

    @Mock
    EcimBackupInfo ecimBackupInfoMock;

    @Mock
    private FragmentVersionCheck fragmentVersionCheckMock;

    @Mock
    private DeleteSmrsBackupUtil deleteSmrsBackupServiceMock;

    @Mock
    private NetworkElement networkElementMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelperMock;

    @Mock
    private Map<String, Object> processVariables;

    @Mock
    private ActivityTimeoutsService activityTimeoutsServiceMock;

    @Mock
    private DeleteBackupUtility deleteBackupUtilityMock;

    @Mock
    JobActivityInfo jobActivityInfoMock;

    @Mock
    private NEJobProgressPercentageCache jobProgressPercentageCache;

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

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckSucess_ValidBackupOnNode() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        final Map<String, List<String>> inputMap = new HashMap<String, List<String>>();
        mockNeJobStaticData();
        final List<String> backupList = new ArrayList<String>();
        backupList.add("backup1");
        inputMap.put("domain1|type1|NODE", backupList);
        when(ecimBackupUtilsMock.getBackupDataToBeDeleted(mainJobAttributes, nodeName)).thenReturn(backupList);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(inputMap);
        final Map<String, List<String>> backupNamesGroupedByDomainAndType = new HashMap<String, List<String>>();
        final List<String> bkupNameListByDomainAndType = new ArrayList<String>();
        bkupNameListByDomainAndType.add("backup1");
        backupNamesGroupedByDomainAndType.put("domain1|type1|NODE", bkupNameListByDomainAndType);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(backupNamesGroupedByDomainAndType);
        final Map<String, String> domainAndTypeMap = new HashMap<String, String>();
        domainAndTypeMap.put("DOMAIN", "domain1");
        domainAndTypeMap.put("TYPE", "type1");
        domainAndTypeMap.put("LOCATION", "NODE");
        when(ecimBackupUtilsMock.getBackupDomainTypeAndLocation("domain1|type1|NODE")).thenReturn(domainAndTypeMap);
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType, nodeName, "domain1", "type1")).thenReturn(backupList);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(backupList)).thenReturn("backup1|domain1|type1|NODE");
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(anyList())).thenReturn("backup1");
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticDataMock, EcimBackupConstants.DELETE_BACKUP)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckSucess_ValidBackupOnEnm() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        final Map<String, List<String>> inputMap = new HashMap<String, List<String>>();
        mockNeJobStaticData();
        final List<String> backupList = new ArrayList<String>();
        backupList.add("backup3");
        inputMap.put("domain1|type1|ENM", backupList);
        when(ecimBackupUtilsMock.getBackupDataToBeDeleted(mainJobAttributes, nodeName)).thenReturn(backupList);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(inputMap);
        final Map<String, List<String>> backupNamesGroupedByDomainAndType = new HashMap<String, List<String>>();
        final List<String> bkupNameListByDomainAndType = new ArrayList<String>();
        bkupNameListByDomainAndType.add("backup3");
        backupNamesGroupedByDomainAndType.put("domain1|type1|ENM", bkupNameListByDomainAndType);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(backupNamesGroupedByDomainAndType);
        final Map<String, String> domainAndTypeMap = new HashMap<String, String>();
        domainAndTypeMap.put("DOMAIN", "domain1");
        domainAndTypeMap.put("TYPE", "type1");
        domainAndTypeMap.put("LOCATION", "ENM");
        when(ecimBackupUtilsMock.getBackupDomainTypeAndLocation("domain1|type1|ENM")).thenReturn(domainAndTypeMap);
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType, nodeName, "domain1", "type1")).thenReturn(backupList);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(anyList())).thenReturn("backup3");
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticDataMock, EcimBackupConstants.DELETE_BACKUP)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @Test
    public void testPrecheckFailureWithSingleInput_InValidBackup() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, List<String>> inputMap = new HashMap<String, List<String>>();
        final List<String> backupList = new ArrayList<String>();
        backupList.add("backup1");
        inputMap.put("domain1|type1|NODE", backupList);
        when(ecimBackupUtilsMock.getBackupDataToBeDeleted(mainJobAttributes, nodeName)).thenReturn(backupList);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(inputMap);
        final Map<String, List<String>> backupNamesGroupedByDomainAndType = new HashMap<String, List<String>>();
        final List<String> bkupNameListByDomainAndType = new ArrayList<String>();
        bkupNameListByDomainAndType.add("backup1");
        backupNamesGroupedByDomainAndType.put("domain1|type1|NODE", bkupNameListByDomainAndType);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(backupNamesGroupedByDomainAndType);
        final Map<String, String> domainAndTypeMap = new HashMap<String, String>();
        domainAndTypeMap.put("DOMAIN", "domain1");
        domainAndTypeMap.put("TYPE", "type1");
        domainAndTypeMap.put("LOCATION", "NODE");
        when(ecimBackupUtilsMock.getBackupDomainTypeAndLocation("domain1|type1|NODE")).thenReturn(domainAndTypeMap);
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType, nodeName, "domain1", "type1")).thenReturn(new ArrayList<String>());
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckFailure_ThrowsException() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockNeJobStaticData();
        final List<String> bkupNameListByDomainAndType = setUpPrecheckAttributes();
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType, nodeName, "domain1", "type1")).thenThrow(Exception.class);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckSuccessWhenNoBackupIsAvailable() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockNeJobStaticData();
        final List<String> bkupNameListByDomainAndType = setUpPrecheckAttributes();
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType, nodeName, "domain1", "type1")).thenThrow(MoNotFoundException.class);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(anyList())).thenReturn("backup1");
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticDataMock, EcimBackupConstants.DELETE_BACKUP)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckSuccessWithTwoInputs() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, List<String>> inputMap = new HashMap<String, List<String>>();
        final List<String> backupList = new ArrayList<String>();
        backupList.add("backup1");
        backupList.add("backup2");
        inputMap.put("domain1|type1|NODE", backupList);
        when(ecimBackupUtilsMock.getBackupDataToBeDeleted(mainJobAttributes, nodeName)).thenReturn(backupList);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(inputMap);
        final Map<String, List<String>> backupNamesGroupedByDomainAndType = new HashMap<String, List<String>>();
        final List<String> bkupNameListByDomainAndType = new ArrayList<String>();
        bkupNameListByDomainAndType.add("backup1");
        bkupNameListByDomainAndType.add("backup2");
        backupNamesGroupedByDomainAndType.put("domain1|type1|NODE", bkupNameListByDomainAndType);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(backupNamesGroupedByDomainAndType);
        final Map<String, String> domainAndTypeMap = new HashMap<String, String>();
        domainAndTypeMap.put("DOMAIN", "domain1");
        domainAndTypeMap.put("TYPE", "type1");
        domainAndTypeMap.put("LOCATION", "NODE");
        when(ecimBackupUtilsMock.getBackupDomainTypeAndLocation("domain1|type1|NODE")).thenReturn(domainAndTypeMap);
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType, nodeName, "domain1", "type1")).thenReturn(backupList);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(anyList())).thenReturn("backup1,backup2");
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticDataMock, EcimBackupConstants.DELETE_BACKUP)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPrecheckSuccess_WithDifferentBackupManagers() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, List<String>> inputMap = new HashMap<String, List<String>>();
        final List<String> backupList = new ArrayList<String>();
        backupList.add("backup1");
        backupList.add("backup2");
        inputMap.put("domain1|type1|NODE", backupList);
        when(ecimBackupUtilsMock.getBackupDataToBeDeleted(mainJobAttributes, nodeName)).thenReturn(backupList);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(inputMap);
        final Map<String, List<String>> backupNamesGroupedByDomainAndType = new HashMap<String, List<String>>();
        final List<String> bkupNameListByDomainAndType = new ArrayList<String>();
        final List<String> bkupNameListByDomainAndType1 = new ArrayList<String>();
        bkupNameListByDomainAndType.add("backup1");
        bkupNameListByDomainAndType1.add("backup2");
        backupNamesGroupedByDomainAndType.put("domain1|type1|NODE", bkupNameListByDomainAndType);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(backupNamesGroupedByDomainAndType);
        final Map<String, String> domainAndTypeMap = new HashMap<String, String>();
        domainAndTypeMap.put("DOMAIN", "domain1");
        domainAndTypeMap.put("TYPE", "type1");
        domainAndTypeMap.put("LOCATION", "NODE");
        when(ecimBackupUtilsMock.getBackupDomainTypeAndLocation("domain1|type1|NODE")).thenReturn(domainAndTypeMap);
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType, nodeName, "domain1", "type1")).thenReturn(backupList);
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType1, nodeName, "domain1", "type1")).thenThrow(MoNotFoundException.class);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(anyList())).thenReturn("backup1,backup2");

        when(fragmentVersionCheckMock.checkFragmentVersion(FragmentType.ECIM_BRM_TYPE, inputVersion)).thenReturn(null);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticDataMock, EcimBackupConstants.DELETE_BACKUP)).thenReturn(true);
        assertEquals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PRECHECK));
    }

    @Test
    public void testExecuteSuccessForDeleteBackupFromENM() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockNeJobStaticData();
        // Mocks for precheck/prevalidate
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = setUpExecuteAttributes();
        // Tests for Execute
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, true);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        when(deleteSmrsBackupServiceMock.getNetworkElement(nodeName)).thenReturn(networkElement);
        activityJobAttributes = new HashMap<>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1");
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        activityJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, ACTIVITY_END_PROGRESS_PERCENTAGE);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(nodeName, "backup1", "SGSN_MME")).thenReturn(true);
        when(deleteSmrsBackupServiceMock.deleteBackupOnSmrs(nodeName, "backup1", "SGSN_MME")).thenReturn(true);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        objectUnderTest.execute(activityJobId);
        verify(systemRecorderMock).recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, "backup1", Long.toString(activityJobId));
    }

    @Test
    public void testExecuteWhenBackupIsUnavailableOnENM() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockNeJobStaticData();
        // Mocks for precheck/prevalidate
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = setUpExecuteAttributes();
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        when(deleteSmrsBackupServiceMock.getNetworkElement(nodeName)).thenReturn(networkElement);
        activityJobAttributes = new HashMap<>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1");
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        activityJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, ACTIVITY_END_PROGRESS_PERCENTAGE);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        // Tests for Execute
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, true);
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(nodeName, "backup1", "SGSN_MME")).thenReturn(false);
        when(deleteSmrsBackupServiceMock.deleteBackupOnSmrs(nodeName, "backup1", "SGSN_MME")).thenReturn(true);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        objectUnderTest.execute(activityJobId);
        verify(systemRecorderMock).recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.ONGOING, nodeName, "backup1", Long.toString(activityJobId));
    }

    private Map<String, String> setUpExecuteAttributes() throws MoNotFoundException, UnsupportedFragmentException {
        final Map<String, List<String>> inputMap = new HashMap<>();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<String> backupList = new ArrayList<String>();
        backupList.add("backup1|domain1|type1|NODE");
        inputMap.put("domain1|type1|ENM", backupList);
        when(ecimBackupUtilsMock.getBackupDataToBeDeleted(mainJobAttributes, nodeName)).thenReturn(backupList);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(inputMap);
        final Map<String, List<String>> backupNamesGroupedByDomainAndType = new HashMap<String, List<String>>();
        final List<String> bkupNameListByDomainAndType = new ArrayList<String>();
        bkupNameListByDomainAndType.add("backup1");
        backupNamesGroupedByDomainAndType.put("domain1|type1|ENM", bkupNameListByDomainAndType);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(backupNamesGroupedByDomainAndType);
        final Map<String, String> domainAndTypeMap = new HashMap<String, String>();
        domainAndTypeMap.put("DOMAIN", "domain1");
        domainAndTypeMap.put("TYPE", "type1");
        domainAndTypeMap.put("LOCATION", "ENM");
        when(ecimBackupUtilsMock.getBackupDomainTypeAndLocation("domain1|type1|ENM")).thenReturn(domainAndTypeMap);
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType, nodeName, "domain1", "type1")).thenReturn(backupList);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(anyList())).thenReturn("backup1");
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "ENM");
        return backupsGroupedByDomainTypeAndLoc;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteFailureForDeleteBackupFromENM() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException, JobDataNotFoundException {
        mockNeJobStaticData();
        // Mocks for precheck/prevalidate
        final Map<String, List<String>> inputMap = new HashMap<String, List<String>>();
        mockNeJobStaticData();
        final List<String> backupList = new ArrayList<String>();
        backupList.add("backup1|domain1|type1|NODE");
        inputMap.put("domain1|type1|ENM", backupList);
        when(ecimBackupUtilsMock.getBackupDataToBeDeleted(mainJobAttributes, nodeName)).thenReturn(backupList);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(inputMap);
        final Map<String, List<String>> backupNamesGroupedByDomainAndType = new HashMap<String, List<String>>();
        final List<String> bkupNameListByDomainAndType = new ArrayList<String>();
        bkupNameListByDomainAndType.add("backup1");
        backupNamesGroupedByDomainAndType.put("domain1|type1|ENM", bkupNameListByDomainAndType);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(backupNamesGroupedByDomainAndType);
        final Map<String, String> domainAndTypeMap = new HashMap<String, String>();
        domainAndTypeMap.put("DOMAIN", "domain1");
        domainAndTypeMap.put("TYPE", "type1");
        domainAndTypeMap.put("LOCATION", "ENM");
        when(ecimBackupUtilsMock.getBackupDomainTypeAndLocation("domain1|type1|ENM")).thenReturn(domainAndTypeMap);
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType, nodeName, "domain1", "type1")).thenReturn(backupList);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(anyList())).thenReturn("backup1");

        // Execute
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, true);
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "ENM");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        when(deleteSmrsBackupServiceMock.getNetworkElement(nodeName)).thenThrow(MoNotFoundException.class);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1");
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(deleteSmrsBackupServiceMock.deleteBackupOnSmrs(nodeName, "backup1", "SGSN_MME")).thenReturn(true);
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(nodeName, "backup1", "")).thenReturn(true);
        activityJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, ACTIVITY_END_PROGRESS_PERCENTAGE);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.FAILED, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        objectUnderTest.execute(activityJobId);
        verify(systemRecorderMock).recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_ERROR, nodeName, "backup1", Long.toString(activityJobId));
    }

    @Test
    public void testExecuteSuccessForDeleteBackupFromNode() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockNeJobStaticData();
        // Mocks for preValidate/precheck
        final Map<String, List<String>> inputMap = new HashMap<String, List<String>>();
        final List<String> backupList = new ArrayList<String>();
        backupList.add("backup1|domain1|type1|NODE");
        inputMap.put("domain1|type1|NODE", backupList);
        when(ecimBackupUtilsMock.getBackupDataToBeDeleted(mainJobAttributes, nodeName)).thenReturn(backupList);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(inputMap);
        final Map<String, List<String>> backupNamesGroupedByDomainAndType = new HashMap<String, List<String>>();
        final List<String> bkupNameListByDomainAndType = new ArrayList<String>();
        bkupNameListByDomainAndType.add("backup1");
        backupNamesGroupedByDomainAndType.put("domain1|type1|NODE", bkupNameListByDomainAndType);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(backupNamesGroupedByDomainAndType);
        final Map<String, String> domainAndTypeMap = new HashMap<String, String>();
        domainAndTypeMap.put("DOMAIN", "domain1");
        domainAndTypeMap.put("TYPE", "type1");
        domainAndTypeMap.put("LOCATION", "NODE");
        when(ecimBackupUtilsMock.getBackupDomainTypeAndLocation("domain1|type1|NODE")).thenReturn(domainAndTypeMap);
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType, nodeName, "domain1", "type1")).thenReturn(backupList);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(backupList)).thenReturn("backup1|domain1|type1|NODE");
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(anyList())).thenReturn("backup1");

        //Execute
        when(ecimBackupUtilsMock.getBackupDataToBeDeleted(mainJobAttributes, nodeName)).thenReturn(backupList);
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "NODE");
        ecimBackupInfoMock.setBackupName("backup1");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        final List<String> backupNameList = new ArrayList<String>();
        backupNameList.add("backup1");
        final List<String> ValidbackupDataList = new ArrayList<String>();
        ValidbackupDataList.add("backup1|domain1|type1");
        when(brmMoServiceMock.getBackupDetails(backupNameList, nodeName, "domain1", "type1")).thenReturn(ValidbackupDataList);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(ValidbackupDataList)).thenReturn("backup1|domain1|type1");
        when(brmMoServiceMock.getNotifiableMoFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(brmBackupManagerMoFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfoMock, brmBackupManagerMoFdn, EcimBackupConstants.DELETE_BACKUP)).thenReturn(0);
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        networkElementsList.add(networkElement);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(networkElementsList);
        final Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        final Map<String, String> activityJobPropertyAttributes = new HashMap<String, String>();
        activityJobPropertyAttributes.put(ActivityConstants.IS_PRECHECK_DONE, ActivityConstants.CHECK_TRUE);
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobPropertyList.add(activityJobPropertyAttributes);
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, ActivityStepsEnum.PRECHECK);
        activityJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, ACTIVITY_END_PROGRESS_PERCENTAGE);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(activityUtilsMock.getActivityJobAttributeValue(activityJobAttributes, ActivityConstants.IS_PRECHECK_DONE)).thenReturn(ActivityConstants.CHECK_TRUE);
        objectUnderTest.execute(activityJobId);
        verify(deleteBackupUtilityMock).deleteBackupFromNode(activityJobId, neJobStaticDataMock, "backup1|domain1|type1", JobTypeEnum.DELETEBACKUP, jobActivityInfoMock);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteFailureForDeleteBackupFromNode() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockNeJobStaticData();
        // Mocks for preValidate/precheck
        final Map<String, List<String>> inputMap = new HashMap<String, List<String>>();
        final List<String> backupList = new ArrayList<String>();
        backupList.add("backup1|domain1|type1|NODE");
        inputMap.put("domain1|type1|NODE", backupList);
        when(ecimBackupUtilsMock.getBackupDataToBeDeleted(mainJobAttributes, nodeName)).thenReturn(backupList);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(inputMap);
        final Map<String, List<String>> backupNamesGroupedByDomainAndType = new HashMap<String, List<String>>();
        final List<String> bkupNameListByDomainAndType = new ArrayList<String>();
        bkupNameListByDomainAndType.add("backup1");
        backupNamesGroupedByDomainAndType.put("domain1|type1|NODE", bkupNameListByDomainAndType);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(backupNamesGroupedByDomainAndType);
        final Map<String, String> domainAndTypeMap = new HashMap<String, String>();
        domainAndTypeMap.put("DOMAIN", "domain1");
        domainAndTypeMap.put("TYPE", "type1");
        domainAndTypeMap.put("LOCATION", "NODE");
        when(ecimBackupUtilsMock.getBackupDomainTypeAndLocation("domain1|type1|NODE")).thenReturn(domainAndTypeMap);
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType, nodeName, "domain1", "type1")).thenReturn(backupList);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(backupList)).thenReturn("backup1|domain1|type1|NODE");
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(anyList())).thenReturn("backup1");

        // Execute
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "NODE");
        ecimBackupInfoMock.setBackupName("backup1");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        final List<String> backupNameList = new ArrayList<String>();
        backupNameList.add("backup1");
        final List<String> ValidbackupDataList = new ArrayList<String>();
        ValidbackupDataList.add("backup1|domain1|type1");
        when(brmMoServiceMock.getBackupDetails(backupNameList, nodeName, "domain1", "type1")).thenReturn(ValidbackupDataList);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(ValidbackupDataList)).thenReturn("backup1|domain1|type1");
        when(brmMoServiceMock.getNotifiableMoFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(brmBackupManagerMoFdn);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfoMock, brmBackupManagerMoFdn, EcimBackupConstants.DELETE_BACKUP)).thenThrow(Exception.class);
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
        activityJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, ACTIVITY_END_PROGRESS_PERCENTAGE);
        objectUnderTest.execute(activityJobId);
        verify(deleteBackupUtilityMock).deleteBackupFromNode(activityJobId, neJobStaticDataMock, "backup1|domain1|type1", JobTypeEnum.DELETEBACKUP, jobActivityInfoMock);
    }

    @Test
    public void testExecuteWithMediationServiceException() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockNeJobStaticData();

        // Mocks for preValidate/precheck
        final Map<String, List<String>> inputMap = new HashMap<String, List<String>>();
        final List<String> backupList = new ArrayList<String>();
        backupList.add("backup1|domain1|type1|NODE");
        inputMap.put("domain1|type1|NODE", backupList);
        when(ecimBackupUtilsMock.getBackupDataToBeDeleted(mainJobAttributes, nodeName)).thenReturn(backupList);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(inputMap);
        final Map<String, List<String>> backupNamesGroupedByDomainAndType = new HashMap<String, List<String>>();
        final List<String> bkupNameListByDomainAndType = new ArrayList<String>();
        bkupNameListByDomainAndType.add("backup1");
        backupNamesGroupedByDomainAndType.put("domain1|type1|NODE", bkupNameListByDomainAndType);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(backupNamesGroupedByDomainAndType);
        final Map<String, String> domainAndTypeMap = new HashMap<String, String>();
        domainAndTypeMap.put("DOMAIN", "domain1");
        domainAndTypeMap.put("TYPE", "type1");
        domainAndTypeMap.put("LOCATION", "NODE");
        when(ecimBackupUtilsMock.getBackupDomainTypeAndLocation("domain1|type1|NODE")).thenReturn(domainAndTypeMap);
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType, nodeName, "domain1", "type1")).thenReturn(backupList);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(backupList)).thenReturn("backup1|domain1|type1|NODE");
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(anyList())).thenReturn("backup1");

        // Execute
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "NODE");
        ecimBackupInfoMock.setBackupName("backup1");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        final List<String> backupNameList = new ArrayList<String>();
        backupNameList.add("backup1");
        final List<String> ValidbackupDataList = new ArrayList<String>();
        ValidbackupDataList.add("backup1|domain1|type1");
        when(brmMoServiceMock.getBackupDetails(backupNameList, nodeName, "domain1", "type1")).thenReturn(ValidbackupDataList);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(ValidbackupDataList)).thenReturn("backup1|domain1|type1");
        when(brmMoServiceMock.getNotifiableMoFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(brmBackupManagerMoFdn);
        final Throwable cause = new Throwable("MediationServiceException");
        final Exception exception = new RuntimeException("MediationServiceExceptionMessage", cause);
        when(brmMoServiceMock.executeMoAction(nodeName, ecimBackupInfoMock, brmBackupManagerMoFdn, EcimBackupConstants.DELETE_BACKUP)).thenThrow(exception);
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        networkElementsList.add(networkElement);
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1");
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(networkElementsList);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        activityJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, ACTIVITY_END_PROGRESS_PERCENTAGE);
        objectUnderTest.execute(activityJobId);
        verify(deleteBackupUtilityMock).deleteBackupFromNode(activityJobId, neJobStaticDataMock, "backup1|domain1|type1", JobTypeEnum.DELETEBACKUP, jobActivityInfoMock);
    }

    @Test
    public void testHandleTimeoutForDeleteBackupFromENM() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, true);
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "ENM");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        when(deleteSmrsBackupServiceMock.getNetworkElement(nodeName)).thenReturn(networkElement);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(false);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        objectUnderTest.handleTimeout(activityJobId);
        verify(systemRecorderMock).recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, "backup1", Long.toString(activityJobId));
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testHandleTimeoutForDeleteBackupFromENMwithRepeatExecute() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, true);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, false);
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "ENM");
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1|domain1|type1");
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        when(deleteSmrsBackupServiceMock.getNetworkElement(nodeName)).thenReturn(networkElement);
        activityJobAttributes = new HashMap<String, Object>();
        propertyMap.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.PROCESSED_BACKUPS);
        propertyMap.put(ActivityConstants.JOB_PROP_VALUE, "1");
        propertyMap.put(ActivityConstants.JOB_PROP_KEY, EcimBackupConstants.TOTAL_BACKUPS);
        propertyMap.put(ActivityConstants.JOB_PROP_VALUE, "2");
        activityJobProperties.add(propertyMap);
        activityJobAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobProperties);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(false);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        assertEquals(ActivityStepResultEnum.REPEAT_EXECUTE, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testHandleTimeoutForDeleteBackupFromNode() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, true);
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "NODE");
        ecimBackupInfoMock.setBackupName("backup1");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        when(brmMoServiceMock.getNotifiableMoFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(brmBackupManagerMoFdn);
        when(brmMoServiceMock.isBackupDeletionCompleted(Matchers.anyString(), Matchers.any(EcimBackupInfo.class))).thenReturn(true);
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1");
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        networkElementsList.add(networkElement);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(networkElementsList);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
    }

    @Test
    public void testHandleTimeoutForDeleteBackupFromNodeFailed() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockNeJobStaticData();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, false);
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "NODE");
        ecimBackupInfoMock.setBackupName("backup1");
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1")).thenReturn(ecimBackupInfoMock);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        when(brmMoServiceMock.getNotifiableMoFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(brmBackupManagerMoFdn);
        when(brmMoServiceMock.isBackupDeletionCompleted(Matchers.anyString(), Matchers.any(EcimBackupInfo.class))).thenReturn(false);

        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        networkElementsList.add(networkElement);
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(networkElementsList);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1|domain1|type1");
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.FAILED, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        assertEquals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL, objectUnderTest.handleTimeout(activityJobId).getActivityResultEnum());
        verify(deleteBackupUtilityMock).evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.FAILED, jobPropertyList);
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testProcessNotificationWithFinishedSuccessState() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, true);
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "NODE");
        ecimBackupInfoMock.setBackupName("backup1");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1")).thenReturn(ecimBackupInfoMock);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributesMock);
        when(brmMoServiceMock.getValidAsyncActionProgress(nodeName, EcimBackupConstants.DELETE_BACKUP, modifiedAttributesMock)).thenReturn(progressReportMock);
        when(progressReportMock.getActionName()).thenReturn(EcimBackupConstants.BACKUP_DELETE_ACTION);
        when(activityUtilsMock.getMoFdnFromNotificationSubject(notificationSubjectMock)).thenReturn(brmBackupManagerMoFdn);
        final Date notificationTime = new Date();
        when(activityUtilsMock.getNotificationTimeStamp(notificationSubjectMock)).thenReturn(notificationTime);
        when(ecimCommonUtilsMock.handleMoActionProgressReportState(jobLogList, activityJobId, progressReportMock, notificationTime, EcimBackupConstants.DELETE_BACKUP, "backup1")).thenReturn(
                JobResult.SUCCESS);
        activityJobAttributes = new HashMap<String, Object>();
        when(deleteBackupUtilityMock.getCountOfTotalBackups(activityJobAttributes)).thenReturn(1);
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.PROGRESSPERCENTAGE, ACTIVITY_END_PROGRESS_PERCENTAGE);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1");
        when(progressReportMock.getProgressPercentage()).thenReturn((int) ACTIVITY_END_PROGRESS_PERCENTAGE);
        objectUnderTest.processNotification(notificationMock);
        verify(systemRecorderMock).recordCommand(SHMEvents.UPLOAD_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, brmBackupManagerMoFdn,
                activityUtilsMock.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.BACKUP));
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PROCESS_NOTIFICATION));
    }

    @Test
    public void testProcessNotificationWithFinishedFailedState() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, false);
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "NODE");
        ecimBackupInfoMock.setBackupName("backup1");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1")).thenReturn(ecimBackupInfoMock);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributesMock);
        when(brmMoServiceMock.getValidAsyncActionProgress(nodeName, EcimBackupConstants.DELETE_BACKUP, modifiedAttributesMock)).thenReturn(progressReportMock);
        when(progressReportMock.getActionName()).thenReturn(EcimBackupConstants.BACKUP_DELETE_ACTION);
        when(activityUtilsMock.getMoFdnFromNotificationSubject(notificationSubjectMock)).thenReturn(brmBackupManagerMoFdn);
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1");
        final Date notificationTime = new Date();
        when(activityUtilsMock.getNotificationTimeStamp(notificationSubjectMock)).thenReturn(notificationTime);
        when(ecimCommonUtilsMock.handleMoActionProgressReportState(jobLogList, activityJobId, progressReportMock, notificationTime, EcimBackupConstants.DELETE_BACKUP, "backup1")).thenReturn(
                JobResult.FAILED);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.FAILED, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1");
        objectUnderTest.processNotification(notificationMock);
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PROCESS_NOTIFICATION));
    }

    @Test
    public void testProcessNotificationWithRunningState() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "NODE");
        ecimBackupInfoMock.setBackupName("backup1");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1")).thenReturn(ecimBackupInfoMock);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributesMock);
        when(brmMoServiceMock.getValidAsyncActionProgress(nodeName, EcimBackupConstants.DELETE_BACKUP, modifiedAttributesMock)).thenReturn(progressReportMock);
        when(progressReportMock.getActionName()).thenReturn(EcimBackupConstants.BACKUP_DELETE_ACTION);
        when(activityUtilsMock.getMoFdnFromNotificationSubject(notificationSubjectMock)).thenReturn(brmBackupManagerMoFdn);
        final Date notificationTime = new Date();
        when(activityUtilsMock.getNotificationTimeStamp(notificationSubjectMock)).thenReturn(notificationTime);
        when(ecimCommonUtilsMock.handleMoActionProgressReportState(jobLogList, activityJobId, progressReportMock, notificationTime, EcimBackupConstants.DELETE_BACKUP, "backup1")).thenReturn(null);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        when(deleteBackupUtilityMock.getCountOfTotalBackups(activityJobAttributes)).thenReturn(1);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(progressReportMock.getProgressPercentage()).thenReturn((int) ACTIVITY_END_PROGRESS_PERCENTAGE);
        objectUnderTest.processNotification(notificationMock);
        verify(jobUpdateServiceMock).readAndUpdateRunningJobAttributes(activityJobId, null, jobLogList, 0.0);
    }

    @Test
    public void testProcessNotificationWithCancelCuurentActionFinishedSuccessState() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, true);
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "NODE");
        ecimBackupInfoMock.setBackupName("backup1");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1")).thenReturn(ecimBackupInfoMock);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributesMock);
        when(brmMoServiceMock.getValidAsyncActionProgress(nodeName, EcimBackupConstants.DELETE_BACKUP, modifiedAttributesMock)).thenReturn(progressReportMock);
        when(progressReportMock.getActionName()).thenReturn(EcimBackupConstants.BACKUP_CANCEL_ACTION);
        when(activityUtilsMock.getMoFdnFromNotificationSubject(notificationSubjectMock)).thenReturn(brmBackupManagerMoFdn);
        final Date notificationTime = new Date();
        when(activityUtilsMock.getNotificationTimeStamp(notificationSubjectMock)).thenReturn(notificationTime);
        when(ecimCommonUtilsMock.handleCancelProgressReportState(jobLogList, activityJobId, progressReportMock, notificationTime, EcimBackupConstants.DELETE_BACKUP)).thenReturn(JobResult.SUCCESS);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1");
        objectUnderTest.processNotification(notificationMock);
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PROCESS_NOTIFICATION));
    }

    @Test
    public void testProcessNotificationWithCancelCuurentActionFinishedFailedState() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, true);
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "NODE");
        ecimBackupInfoMock.setBackupName("backup1");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1")).thenReturn(ecimBackupInfoMock);
        when(notificationMock.getNotificationSubject()).thenReturn(notificationSubjectMock);
        when(activityUtilsMock.getActivityJobId(notificationSubjectMock)).thenReturn(activityJobId);
        when(activityUtilsMock.getModifiedAttributes(notificationMock.getDpsDataChangedEvent())).thenReturn(modifiedAttributesMock);
        when(brmMoServiceMock.getValidAsyncActionProgress(nodeName, EcimBackupConstants.DELETE_BACKUP, modifiedAttributesMock)).thenReturn(progressReportMock);
        when(progressReportMock.getActionName()).thenReturn(EcimBackupConstants.BACKUP_CANCEL_ACTION);
        when(activityUtilsMock.getMoFdnFromNotificationSubject(notificationSubjectMock)).thenReturn(brmBackupManagerMoFdn);
        final Date notificationTime = new Date();
        when(activityUtilsMock.getNotificationTimeStamp(notificationSubjectMock)).thenReturn(notificationTime);
        when(ecimCommonUtilsMock.handleCancelProgressReportState(jobLogList, activityJobId, progressReportMock, notificationTime, EcimBackupConstants.DELETE_BACKUP)).thenReturn(JobResult.FAILED);
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1");
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.FAILED, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        objectUnderTest.processNotification(notificationMock);
        verify(ecimCommonUtilsMock).handleCancelProgressReportState(jobLogList, activityJobId, progressReportMock, notificationTime, EcimBackupConstants.DELETE_BACKUP);
        verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.PROCESS_NOTIFICATION));
        //verify(activityUtilsMock).prepareJobPropertyList(jobPropertyList, BackupActivityConstants.INTERMEDIATE_FAILURE, JobResult.FAILED.toString());
    }

    @Test
    public void testProcessNotificationWithNonAVCEvent() {
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.CREATE);
        objectUnderTest.processNotification(notificationMock);
        verify(activityUtilsMock, times(0)).getModifiedAttributes(notificationMock.getDpsDataChangedEvent());
    }

    @Test
    public void testCancelTimeoutForDeleteBackupFromENM() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, true);
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "ENM");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN_MME");
        networkElement.setName(nodeName);
        when(deleteSmrsBackupServiceMock.getNetworkElement(nodeName)).thenReturn(networkElement);
        activityJobAttributes = new HashMap<String, Object>();
        final List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, activityJobPropertyList);
        activityJobAttributes.put(ShmConstants.ACTIVITY_START_DATE, new Date());
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(false);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        objectUnderTest.handleTimeout(activityJobId);
        verify(systemRecorderMock).recordCommand(SHMEvents.DELETE_BACKUP_SERVICE, CommandPhase.FINISHED_WITH_SUCCESS, nodeName, "backup1", Long.toString(activityJobId));
        Mockito.verify(activityUtilsMock).persistStepDurations(eq(activityJobId), anyLong(), eq(ActivityStepsEnum.HANDLE_TIMEOUT));
    }

    @Test
    public void testCancelTimeoutForDeleteBackupFromNode() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, true);
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "NODE");
        ecimBackupInfoMock.setBackupName("backup1");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        when(brmMoServiceMock.getNotifiableMoFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(brmBackupManagerMoFdn);
        when(brmMoServiceMock.isBackupDeletionCompleted(Matchers.anyString(), Matchers.any(EcimBackupInfo.class))).thenReturn(true);

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
        when(deleteSmrsBackupServiceMock.isBackupExistsOnSmrs(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(false);
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.SUCCESS, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
    }

    @Test
    public void testCancelTimeoutForDeleteBackupFromNodeFailed() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException, JobDataNotFoundException {
        mockNeJobStaticData();
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> repeatRequiredAndActivityResult = new HashMap<String, Object>();
        repeatRequiredAndActivityResult.put(JobVariables.ACTIVITY_REPEAT_EXECUTE, false);
        repeatRequiredAndActivityResult.put(ActivityConstants.ACTIVITY_RESULT, false);
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "NODE");
        ecimBackupInfoMock.setBackupName("backup1");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        when(brmMoServiceMock.getNotifiableMoFdn(EcimBackupConstants.DELETE_BACKUP, nodeName, ecimBackupInfoMock)).thenReturn(brmBackupManagerMoFdn);
        when(brmMoServiceMock.isBackupDeletionCompleted(Matchers.anyString(), Matchers.any(EcimBackupInfo.class))).thenReturn(false);
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
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1");
        when(deleteBackupUtilityMock.evaluateRepeatRequiredAndActivityResult(activityJobId, JobResult.FAILED, jobPropertyList)).thenReturn(repeatRequiredAndActivityResult);
        objectUnderTest.cancelTimeout(activityJobId, false);
    }

    @Test
    public void testCancelForDeleteBackupOnNode() throws JobDataNotFoundException {
        mockNeJobStaticData();
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(Arrays.asList(networkElementMock));
        when(networkElementMock.getNeType()).thenReturn("SGSN");
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1|domain1|type1");
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1|domain1|type1")).thenReturn(ecimBackupInfoMock);
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "NODE");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        objectUnderTest.cancel(activityJobId);
        verify(cancelBackupService).cancel(Matchers.anyLong(), Matchers.anyString(), Matchers.any(EcimBackupInfo.class));
    }

    @Test
    public void testCancelForDeleteBackupOnENM() throws JobDataNotFoundException {
        mockNeJobStaticData();
        when(fdnServiceBeanRetryHelperMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(Arrays.asList(networkElementMock));
        when(networkElementMock.getNeType()).thenReturn("SGSN");
        when(deleteBackupUtilityMock.getBackupUnderDeletion(activityJobId)).thenReturn("backup1|domain1|type1");
        when(ecimBackupUtilsMock.getEcimBackupInfo("backup1")).thenReturn(ecimBackupInfoMock);
        final Map<String, String> backupsGroupedByDomainTypeAndLoc = new HashMap<String, String>();
        backupsGroupedByDomainTypeAndLoc.put(BACKUP_NAME, "backup1");
        backupsGroupedByDomainTypeAndLoc.put(DOMAIN, "domain1");
        backupsGroupedByDomainTypeAndLoc.put(TYPE, "type1");
        backupsGroupedByDomainTypeAndLoc.put(LOCATION, "ENM");
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(Matchers.anyString(), Matchers.anyString())).thenReturn(backupsGroupedByDomainTypeAndLoc);
        objectUnderTest.cancel(activityJobId);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testPrecheckForException() throws MoNotFoundException, UnsupportedFragmentException, JobDataNotFoundException {
        final Map<String, List<String>> inputMap = new HashMap<String, List<String>>();
        mockNeJobStaticData();
        final List<String> backupList = new ArrayList<String>();
        backupList.add("backup1");
        inputMap.put("domain1|type1|NODE", backupList);
        when(ecimBackupUtilsMock.getBackupDataToBeDeleted(mainJobAttributes, nodeName)).thenReturn(backupList);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(inputMap);
        final Map<String, List<String>> backupNamesGroupedByDomainAndType = new HashMap<String, List<String>>();
        final List<String> bkupNameListByDomainAndType = new ArrayList<String>();
        bkupNameListByDomainAndType.add("backup1");
        backupNamesGroupedByDomainAndType.put("domain1|type1|NODE", bkupNameListByDomainAndType);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(backupNamesGroupedByDomainAndType);
        final Map<String, String> domainAndTypeMap = new HashMap<String, String>();
        domainAndTypeMap.put("DOMAIN", "domain1");
        domainAndTypeMap.put("TYPE", "type1");
        domainAndTypeMap.put("LOCATION", "NODE");
        when(ecimBackupUtilsMock.getBackupDomainTypeAndLocation("domain1|type1|NODE")).thenReturn(domainAndTypeMap);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(backupList)).thenReturn("backup1|domain1|type1|NODE");
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(anyList())).thenReturn("backup1");
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticDataMock);
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticDataMock, jobStaticDataMock, EcimBackupConstants.DELETE_BACKUP)).thenReturn(true);
        when(brmMoServiceMock.getBackupDetails(bkupNameListByDomainAndType, nodeName, "domain1", "type1")).thenThrow(Exception.class);
        assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, objectUnderTest.precheck(activityJobId).getActivityResultEnum());
    }

    
    @SuppressWarnings("unchecked")
    @Test
    public void testAsyncHandleTimeoutForException() throws Exception {
        when(neJobStaticDataProviderMock.getNeJobStaticData(eq(activityJobId), anyString())).thenThrow(Exception.class);
        objectUnderTest.asyncHandleTimeout(activityJobId);
        verify(activityUtilsMock, times(1)).handleExceptionForHandleTimeoutScenarios(anyLong(), anyString(), anyString());
    }

    private void mockNeJobStaticData() throws JobDataNotFoundException {
        final List<Map<String, String>> jobProperties = new ArrayList<Map<String, String>>();
        activityJobAttributes.put(ShmConstants.JOBPROPERTIES, jobProperties);
        activityJobAttributes.put(ShmConstants.NE_JOB_ID, neJobId);
        activityJobAttributes.put(ShmConstants.STEP_DURATIONS, "PRECHECK");
        when(neJobStaticDataProviderMock.getNeJobStaticData(eq(activityJobId), anyString())).thenReturn(neJobStaticDataMock);
        when(neJobStaticDataMock.getActivityStartTime()).thenReturn(new Date().getTime());
        when(neJobStaticDataMock.getNodeName()).thenReturn(nodeName);
        when(neJobStaticDataMock.getMainJobId()).thenReturn(mainJobId);
        when(jobConfigServiceRetryProxyMock.getMainJobAttributes(mainJobId)).thenReturn(mainJobAttributes);
        when(jobConfigServiceRetryProxyMock.getActivityJobAttributes(activityJobId)).thenReturn(activityJobAttributes);
        when(notificationMock.getNotificationEventType()).thenReturn(NotificationEventTypeEnum.AVC);
        when(activityUtilsMock.getActivityInfo(activityJobId, DeleteBackupService.class)).thenReturn(jobActivityInfoMock);
    }

    private List<String> setUpPrecheckAttributes() {
        final Map<String, List<String>> inputMap = new HashMap<>();
        final List<String> backupList = new ArrayList<>();
        backupList.add("backup1");
        inputMap.put("domain1|type1|NODE", backupList);
        when(ecimBackupUtilsMock.getBackupDataToBeDeleted(mainJobAttributes, nodeName)).thenReturn(backupList);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(inputMap);
        final Map<String, List<String>> backupNamesGroupedByDomainAndType = new HashMap<>();
        final List<String> bkupNameListByDomainAndType = new ArrayList<>();
        bkupNameListByDomainAndType.add("backup1");
        backupNamesGroupedByDomainAndType.put("domain1|type1|NODE", bkupNameListByDomainAndType);
        when(ecimBackupUtilsMock.groupBackupNamesByDomainTypeAndLoc(backupList)).thenReturn(backupNamesGroupedByDomainAndType);
        final Map<String, String> domainAndTypeMap = new HashMap<>();
        domainAndTypeMap.put("DOMAIN", "domain1");
        domainAndTypeMap.put("TYPE", "type1");
        domainAndTypeMap.put("LOCATION", "NODE");
        when(ecimBackupUtilsMock.getBackupDomainTypeAndLocation("domain1|type1|NODE")).thenReturn(domainAndTypeMap);
        when(ecimBackupUtilsMock.getCommaSeparatedBackupData(backupList)).thenReturn("backup1|domain1|type1|NODE");
        return bkupNameListByDomainAndType;
    }

}
