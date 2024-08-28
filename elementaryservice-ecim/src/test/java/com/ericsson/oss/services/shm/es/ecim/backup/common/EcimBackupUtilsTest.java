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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroup;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroupPreparator;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.ecim.common.EcimCommonConstants;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.UpMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActiveSoftwareProvider;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.AutoGenerateNameValidator;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.inventory.backup.api.BackupInventoryConstants;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmHandler;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmVersionHandlersProviderFactory;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.EcimBackupItem;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackup;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackupManager;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.NeInfoQuery;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RunWith(MockitoJUnitRunner.class)
public class EcimBackupUtilsTest {

    @Mock
    Map<String, Object> mapMock;

    @Mock
    FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Mock
    JobPropertyUtils jobPropertyUtilsMock;

    @Mock
    Map<String, String> mapMock2;

    @Mock
    JobEnvironment jobEnvironment;

    @Mock
    NetworkElement networkElementMock;

    @Mock
    private FdnServiceBean fdnServiceBeanMock;

    @Mock
    private NetworkElementGroupPreparator neBatchPreparatorMock;

    @Mock
    private NetworkElementGroup networkElementGroupMock;

    @Mock
    private NeInfoQuery neInfoQueryMock;

    @Mock
    private BrmVersionHandlersProviderFactory handlersProviderFactoryMock;

    @Mock
    private OssModelInfo ossModelInfoMock;

    @Mock
    private BrmHandler brmHandlerMock;

    @Mock
    private Map<String, List<EcimBackupItem>> nodesBackupActivityItems;

    @Mock
    private Entry<String, List<EcimBackupItem>> nodesActivityItems;

    @Mock
    private EcimBackupItem ecimBackupItemMock;

    @InjectMocks
    EcimBackupUtils objectUnderTest;

    @Mock
    private NetworkElementData networkElementInfo;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private JobConfigurationServiceRetryProxy configServiceRetryProxy;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private NetworkElementData networkElementData;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private EAccessControl accessControl;

    @Mock
    private ActiveSoftwareProvider activeSoftwareProvider;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private UpMoServiceRetryProxy upMoServiceRetryProxyMock;

    @Mock
    private JobLogUtil jobLogUtilMock;

    @Mock
    private AutoGenerateNameValidator autoGenerateNameValidator;

    @Mock
    private BrmMoService brmMoServiceMock;

    Map<String, Object> templateJobAttr;
    Map<String, Object> mainJobAttr;
    Map<String, Object> neJobAttributes;
    List<Map<String, Object>> jobLogList;

    long activityJobId = 123L;
    long neJobId = 123L;
    long mainJobId = 123L;
    long templateJobId = 123L;
    String neName = "NetworkElement01";
    String cvMoFdn = "Some Cv Mo Fdn";
    String configurationVersionName = "Some CV Name";
    String identity = "Some Identity";
    String type = "Standard";
    String operatorName = "Some Operator Name";
    String comment = "Some Comment";
    String jobName = "Some Job Name";
    String currentUpgradePackage = "ManagedElement=1,SwManagement=1,UpgradePackage=CXP102051_1_R4D21";

    int actionId = 5;

    @Test
    public void testGetBackupDataToBeDeleted() {
        final String nodeName = "NetworkElement01";
        final List<String> neNamesList = new ArrayList<String>();
        neNamesList.add(nodeName);

        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN-MME");
        networkElement.setPlatformType(PlatformTypeEnum.ECIM);
        networkElementsList.add(networkElement);

        final Map<String, String> backupDataMap = new HashMap<String, String>();
        backupDataMap.put(EcimBackupConstants.BRM_BACKUP_NAME, "bkup1|domain1|type1, bkup2|domain1|type1, bkup3|domain2|type2");

        final List<String> keyList = new ArrayList<String>();
        keyList.add(EcimBackupConstants.BRM_BACKUP_NAME);

        final Map<String, Object> jobConfigurationMap = new HashMap<String, Object>();
        when(mapMock.get(ActivityConstants.JOB_CONFIGURATION_DETAILS)).thenReturn(jobConfigurationMap);
        when(fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neNamesList, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(networkElementsList);
        when(jobPropertyUtilsMock.getPropertyValue(keyList, jobConfigurationMap, nodeName, networkElement.getNeType(), networkElement.getPlatformType().name())).thenReturn(backupDataMap);

        final List<String> backupList = objectUnderTest.getBackupDataToBeDeleted(mapMock, nodeName);
        assertEquals(backupList.size(), 3);
    }

    @Test
    public void testGroupBackupNamesByDomainAndType() {
        final List<String> backupDataList = new ArrayList<String>();
        backupDataList.add("bkup1|domain1|type1|Location1");
        backupDataList.add("bkup2||type1");
        backupDataList.add("bkup3|Location1");
        backupDataList.add("bkup4|domain2|Location1");

        final Map<String, List<String>> backupMap = objectUnderTest.groupBackupNamesByDomainTypeAndLoc(backupDataList);
        assertEquals(backupMap.size(), 4);
        assertEquals(backupMap.get("domain1|type1|Location1"), Arrays.asList("bkup1"));
        assertEquals(backupMap.get("domain2||Location1"), Arrays.asList("bkup4"));
        assertEquals(backupMap.get("||type1"), Arrays.asList("bkup2"));
        assertEquals(backupMap.get("||Location1"), Arrays.asList("bkup3"));
    }

    @Test
    public void testGetEcimBackupInfo() {
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getEcimBackupInfo("bkup1|domain1|type1");
        assertEquals("domain1", ecimBackupInfo.getDomainName());
        assertEquals("bkup1", ecimBackupInfo.getBackupName());
        assertEquals("type1", ecimBackupInfo.getBackupType());
        final EcimBackupInfo ecimBackupInfo1 = objectUnderTest.getEcimBackupInfo("bkup1||type1");
        assertEquals("", ecimBackupInfo1.getDomainName());
        assertEquals("bkup1", ecimBackupInfo1.getBackupName());
        assertEquals("type1", ecimBackupInfo1.getBackupType());
        final EcimBackupInfo ecimBackupInfo2 = objectUnderTest.getEcimBackupInfo("bkup1||");
        assertEquals("", ecimBackupInfo2.getDomainName());
        assertEquals("bkup1", ecimBackupInfo2.getBackupName());
        assertEquals("", ecimBackupInfo2.getBackupType());
        final EcimBackupInfo ecimBackupInfo3 = objectUnderTest.getEcimBackupInfo("bkup1|domain1|");
        assertEquals("domain1", ecimBackupInfo3.getDomainName());
        assertEquals("bkup1", ecimBackupInfo3.getBackupName());
        assertEquals("", ecimBackupInfo3.getBackupType());
    }

    @Test
    public void testGetEcimBackupInfoForUploadWhenBackupNameIsProvided() {
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getEcimBackupInfoForUpload("bkup1/domain1/type1");
        assertEquals("domain1", ecimBackupInfo.getDomainName());
        assertEquals("bkup1", ecimBackupInfo.getBackupName());
        assertEquals("type1", ecimBackupInfo.getBackupType());
        final EcimBackupInfo ecimBackupInfo1 = objectUnderTest.getEcimBackupInfoForUpload("bkup/Name1/domain1/type1");
        assertEquals("domain1", ecimBackupInfo1.getDomainName());
        assertEquals("bkup/Name1", ecimBackupInfo1.getBackupName());
        assertEquals("type1", ecimBackupInfo1.getBackupType());
    }

    @Test
    public void testGetEcimBackupInfoForUploadWhenBackupDomainIsProvided() {
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getEcimBackupInfoForUpload("bkup1/domain1/type1");
        assertEquals("domain1", ecimBackupInfo.getDomainName());
        assertEquals("bkup1", ecimBackupInfo.getBackupName());
        assertEquals("type1", ecimBackupInfo.getBackupType());
        final EcimBackupInfo ecimBackupInfo1 = objectUnderTest.getEcimBackupInfoForUpload("bkup/Name1//type1");
        assertEquals("", ecimBackupInfo1.getDomainName());
        assertEquals("bkup/Name1", ecimBackupInfo1.getBackupName());
        assertEquals("type1", ecimBackupInfo1.getBackupType());
        final EcimBackupInfo ecimBackupInfo2 = objectUnderTest.getEcimBackupInfoForUpload("bk/up1//");
        assertEquals("", ecimBackupInfo2.getDomainName());
        assertEquals("bk/up1", ecimBackupInfo2.getBackupName());
        assertEquals("", ecimBackupInfo2.getBackupType());
        final EcimBackupInfo ecimBackupInfo3 = objectUnderTest.getEcimBackupInfoForUpload("bkup1//type1");
        assertEquals("", ecimBackupInfo3.getDomainName());
        assertEquals("bkup1", ecimBackupInfo3.getBackupName());
        assertEquals("type1", ecimBackupInfo3.getBackupType());

    }

    @Test
    public void testGetEcimBackupInfoForUploadWhenBackupTypeIsProvided() {
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getEcimBackupInfoForUpload("bkup1/domain1/type1");
        assertEquals("domain1", ecimBackupInfo.getDomainName());
        assertEquals("bkup1", ecimBackupInfo.getBackupName());
        assertEquals("type1", ecimBackupInfo.getBackupType());
        final EcimBackupInfo ecimBackupInfo1 = objectUnderTest.getEcimBackupInfoForUpload("bkup/Name1/domain1/");
        assertEquals("domain1", ecimBackupInfo1.getDomainName());
        assertEquals("bkup/Name1", ecimBackupInfo1.getBackupName());
        assertEquals("", ecimBackupInfo1.getBackupType());
        final EcimBackupInfo ecimBackupInfo2 = objectUnderTest.getEcimBackupInfoForUpload("bkup/Name1/domain1/type1");
        assertEquals("domain1", ecimBackupInfo2.getDomainName());
        assertEquals("bkup/Name1", ecimBackupInfo2.getBackupName());
        assertEquals("type1", ecimBackupInfo2.getBackupType());
    }

    @Test
    public void testGetBackupDomainAndType() {
        final String backupKey1 = "domain|type|Location";
        final String backupKey2 = "Location";
        final String backupKey3 = "domain|Location";

        final Map<String, String> backup1Data = objectUnderTest.getBackupDomainTypeAndLocation(backupKey1);
        assertEquals(backup1Data.get("DOMAIN"), "domain");
        assertEquals(backup1Data.get("TYPE"), "type");
        assertEquals(backup1Data.get("LOCATION"), "Location");

        final Map<String, String> backup2Data = objectUnderTest.getBackupDomainTypeAndLocation(backupKey2);
        assertEquals(backup2Data.get("DOMAIN"), "");
        assertEquals(backup2Data.get("TYPE"), "");
        assertEquals(backup1Data.get("LOCATION"), "Location");

        final Map<String, String> backup4Data = objectUnderTest.getBackupDomainTypeAndLocation(backupKey3);
        assertEquals(backup4Data.get("DOMAIN"), "domain");
        assertEquals(backup4Data.get("TYPE"), "");
        assertEquals(backup1Data.get("LOCATION"), "Location");
    }

    @Test
    public void testGetBackupWithbackupManagerId() throws MoNotFoundException {
        setJobEnvironment();
        mainJobAttr = setMainJobAttributes();
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttr);
        final String nodeName = "NetworkElement01";
        final List<String> neNamesList = new ArrayList<String>();
        neNamesList.add(nodeName);
        final List<NetworkElement> networkElementsList = new ArrayList<NetworkElement>();
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN-MME");
        networkElement.setPlatformType(PlatformTypeEnum.ECIM);
        networkElementsList.add(networkElement);
        final Map<String, String> backupDataMap = new HashMap<String, String>();
        backupDataMap.put(EcimBackupConstants.BRM_BACKUP_NAME, "bkup1|domain1|type1, bkup2|domain1|type1, bkup3|domain2|type2");
        backupDataMap.put(EcimBackupConstants.BRM_BACKUP_DOMAIN, "bkup1|domain1|type1, bkup2|domain1|type1, bkup3|domain2|type2");
        backupDataMap.put(EcimBackupConstants.BRM_BACKUP_MANAGER_ID, "bkup1/domain1");
        when(fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neNamesList, SHMCapabilities.BACKUP_JOB_CAPABILITY)).thenReturn(networkElementsList);
        when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(backupDataMap);
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getBackup(jobEnvironment);
        assertTrue(ecimBackupInfo != null);
    }

    @Test
    public void testGetBackupInfoForRestore() throws MoNotFoundException, UnsupportedFragmentException {
        setJobEnvironment();
        mainJobAttr = setMainJobAttributes();
        when(configServiceRetryProxy.getMainJobAttributes(Matchers.anyLong())).thenReturn(mainJobAttr);
        neJobAttributes = setNeJobPo();
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, null);
        when(configServiceRetryProxy.getNeJobAttributes(Matchers.anyLong())).thenReturn(neJobAttributes);
        when(brmMoServiceMock.extractAndAddBackupNameInNeProperties(anyString(), eq(neName), eq(neJobId), eq(networkElementInfo))).thenReturn(new EcimBackupInfo("domain1", "backupName", "SYSTEM"));
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, neName, "businessKey", PlatformTypeEnum.ECIM.getName(), new Date().getTime(), null);
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getBackupInfoForRestore(neJobStaticData, networkElementInfo);
        assertTrue(ecimBackupInfo != null);
    }

    @Test
    public void testGetBackupInfoForRestoreFromENM() throws MoNotFoundException, UnsupportedFragmentException {
        setJobEnvironment();
        mainJobAttr = setMainJobAttributesWithBackupFromENM();
        when(configServiceRetryProxy.getMainJobAttributes(Matchers.anyLong())).thenReturn(mainJobAttr);
        neJobAttributes = setNeJobPo();
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, null);
        when(configServiceRetryProxy.getNeJobAttributes(Matchers.anyLong())).thenReturn(neJobAttributes);
        when(brmMoServiceMock.extractAndAddBackupNameInNeProperties(anyString(), eq(neName), eq(neJobId), eq(networkElementInfo))).thenReturn(new EcimBackupInfo("domain1", "backupName", "SYSTEM"));
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, neName, "businessKey", PlatformTypeEnum.ECIM.getName(), new Date().getTime(), null);
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getBackupInfoForRestore(neJobStaticData, networkElementInfo);
        assertTrue(ecimBackupInfo != null);
    }

    @Test
    public void testgetBackupNameList() {
        final List<String> backupDataList = new ArrayList<String>();
        backupDataList.add("bkup1|domain1|type1|Location1");
        backupDataList.add("bkup2||type1");
        backupDataList.add("bkup3|Location1");
        backupDataList.add("bkup4|domain2|Location1");
        final List<String> backupList = objectUnderTest.getBackupNameList(backupDataList);
        assertEquals(backupList.size(), 4);
    }

    @Test
    public void testgetCommaSeparatedBackupData() {
        final List<String> backupDataList = new ArrayList<String>();
        backupDataList.add("bkup1|domain1|type1|Location1");
        backupDataList.add("bkup2||type1");
        backupDataList.add("bkup3|Location1");
        backupDataList.add("bkup4|domain2|Location1");
        final String backup = objectUnderTest.getCommaSeparatedBackupData(backupDataList);
        assertTrue(backup.length() != 0);
    }

    @Test
    public void testgetRemainingBackupsToDelete() {

        final String commaSeparatedBackupString = "bkup4,domain2,Location1";
        final String index = objectUnderTest.getRemainingBackupsToDelete(commaSeparatedBackupString);
        assertTrue(index.length() != 0);
    }

    private void setJobEnvironment() {
        when(jobEnvironment.getActivityJobId()).thenReturn(activityJobId);
        when(jobEnvironment.getNeJobId()).thenReturn(neJobId);
        when(jobEnvironment.getMainJobId()).thenReturn(mainJobId);
        when(jobEnvironment.getNodeName()).thenReturn(neName);
    }

    private Map<String, Object> setMainJobAttributes() {
        mainJobAttr = new HashMap<String, Object>();
        mainJobAttr.put(ShmConstants.JOBTEMPLATEID, templateJobId);
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
        final List<Map<String, String>> mainJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobPropertyCvIdentity = new HashMap<String, String>();
        mainJobPropertyCvIdentity.put(ShmConstants.KEY, BackupActivityConstants.CV_IDENTITY);
        mainJobPropertyCvIdentity.put(ShmConstants.VALUE, identity);
        mainJobPropertyList.add(mainJobPropertyCvIdentity);
        final Map<String, String> mainJobPropertyCvType = new HashMap<String, String>();
        mainJobPropertyCvType.put(ShmConstants.KEY, BackupActivityConstants.CV_TYPE);
        mainJobPropertyCvType.put(ShmConstants.VALUE, type);
        mainJobPropertyList.add(mainJobPropertyCvType);
        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, mainJobPropertyList);
        final Map<String, Object> mainSchedule = new HashMap<String, Object>();
        final List<Map<String, Object>> schedulePropertiesList = new ArrayList<Map<String, Object>>();
        mainSchedule.put(ShmConstants.SCHEDULINGPROPERTIES, schedulePropertiesList);
        jobConfigurationDetails.put(ShmConstants.MAIN_SCHEDULE, mainSchedule);
        mainJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> jobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobPropertyCvName = new HashMap<String, String>();
        mainJobPropertyCvName.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobPropertyCvName.put(ShmConstants.VALUE, configurationVersionName);
        jobProperties.add(mainJobPropertyCvName);
        mainJobAttr.put(ActivityConstants.JOB_PROPERTIES, jobProperties);
        mainJobAttr.put(ShmConstants.EXECUTIONINDEX, 1);
        return mainJobAttr;
    }

    private Map<String, Object> setMainJobAttributesWithBackupFromENM() {
        mainJobAttr = new HashMap<String, Object>();
        mainJobAttr.put(ShmConstants.JOBTEMPLATEID, templateJobId);
        final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();

        final List<Map<String, String>> neJobPropertyList = new ArrayList<Map<String, String>>();

        final Map<String, String> mainJobPropertyBackupLocation = new HashMap<String, String>();
        mainJobPropertyBackupLocation.put(ShmConstants.KEY, EcimBackupConstants.BACKUP_FILE_LOCATION);
        mainJobPropertyBackupLocation.put(ShmConstants.VALUE, ShmCommonConstants.LOCATION_ENM);
        neJobPropertyList.add(mainJobPropertyBackupLocation);
        final Map<String, String> mainJobPropertyAbsoluteBackupName = new HashMap<String, String>();
        mainJobPropertyAbsoluteBackupName.put(ShmConstants.KEY, EcimBackupConstants.ABSOLUTE_BACKUP_FILE_NAME);
        mainJobPropertyAbsoluteBackupName.put(ShmConstants.VALUE, "ABSOLUTE_BACKUP_NAME");
        neJobPropertyList.add(mainJobPropertyAbsoluteBackupName);

        final Map<String, Object> neJobProperty = new HashMap<>();
        neJobProperty.put(ShmJobConstants.NE_NAME, neName);
        final List<Map<String, Object>> neJobPropertyListObject = new ArrayList<>();
        neJobPropertyListObject.add(neJobProperty);
        neJobProperty.put(ShmJobConstants.JOBPROPERTIES, neJobPropertyList);
        jobConfigurationDetails.put(ShmJobConstants.NEJOB_PROPERTIES, neJobPropertyListObject);

        final Map<String, Object> mainSchedule = new HashMap<String, Object>();
        final List<Map<String, Object>> schedulePropertiesList = new ArrayList<Map<String, Object>>();
        mainSchedule.put(ShmConstants.SCHEDULINGPROPERTIES, schedulePropertiesList);
        jobConfigurationDetails.put(ShmConstants.MAIN_SCHEDULE, mainSchedule);
        mainJobAttr.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        final List<Map<String, String>> jobProperties = new ArrayList<Map<String, String>>();
        final Map<String, String> mainJobPropertyCvName = new HashMap<String, String>();
        mainJobPropertyCvName.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        mainJobPropertyCvName.put(ShmConstants.VALUE, configurationVersionName);
        jobProperties.add(mainJobPropertyCvName);

        mainJobAttr.put(ActivityConstants.JOB_PROPERTIES, jobProperties);
        mainJobAttr.put(ShmConstants.EXECUTIONINDEX, 1);
        return mainJobAttr;
    }

    private Map<String, Object> setNeJobPo() {
        final Map<String, Object> neJobAttr = new HashMap<String, Object>();
        neJobAttr.put(ShmConstants.NE_NAME, neName);
        neJobAttr.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        final List<Map<String, String>> neJobPropertyList = new ArrayList<Map<String, String>>();
        final Map<String, String> neJobProperty = new HashMap<String, String>();
        neJobProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        neJobProperty.put(ShmConstants.VALUE, configurationVersionName);
        neJobPropertyList.add(neJobProperty);
        neJobAttr.put(ActivityConstants.JOB_PROPERTIES, neJobPropertyList);
        return neJobAttr;
    }

    @Test
    public void testSortBrmBackUpsByCreationTimeAsending() {

        final Map<String, Object> brmBackupManagerAttributes = new HashMap<String, Object>();
        brmBackupManagerAttributes.put(BackupInventoryConstants.BACKUP_DOMAIN, "Domain");
        brmBackupManagerAttributes.put(BackupInventoryConstants.BACKUP_TYPE, "Type");

        final Map<String, Object> brmBackupMoAttributes1 = new HashMap<String, Object>();
        brmBackupMoAttributes1.put(BackupInventoryConstants.BACKUP_NAME, "BackupName1");
        brmBackupMoAttributes1.put(BackupInventoryConstants.CREATION_TIME, "2016-08-04T18:35:05");

        final Map<String, Object> brmBackupMoAttributes2 = new HashMap<String, Object>();
        brmBackupMoAttributes2.put(BackupInventoryConstants.BACKUP_NAME, "BackupName2");
        brmBackupMoAttributes2.put(BackupInventoryConstants.CREATION_TIME, "2017-07-04T12:08:56");

        final Map<String, Object> brmBackupMoAttributes3 = new HashMap<String, Object>();
        brmBackupMoAttributes3.put(BackupInventoryConstants.BACKUP_NAME, "BackupName2");
        brmBackupMoAttributes3.put(BackupInventoryConstants.CREATION_TIME, "2014-07-04T12:08:56");
        final BrmBackupManager brmBackupManager = new BrmBackupManager("brmBackupManagerMoFdn", brmBackupManagerAttributes);
        final BrmBackup backup1 = new BrmBackup("brmBackupMoFdn1", brmBackupMoAttributes1, brmBackupManager);
        final BrmBackup backup2 = new BrmBackup("brmBackupMoFdn2", brmBackupMoAttributes2, brmBackupManager);
        final BrmBackup backup3 = new BrmBackup("brmBackupMoFdn2", brmBackupMoAttributes3, brmBackupManager);

        final List<BrmBackup> listOfBrms = new ArrayList<BrmBackup>();
        listOfBrms.add(backup2);
        listOfBrms.add(backup1);
        listOfBrms.add(backup3);
        objectUnderTest.sortBrmBackUpsByCreationTime(listOfBrms, true);
        assertEquals("2014-07-04T12:08:56", listOfBrms.get(0).getCreationTime().getDateTime());
        assertEquals("2017-07-04T12:08:56", listOfBrms.get(2).getCreationTime().getDateTime());
    }

    @Test
    public void testSortBrmBackUpsByCreationTimeDesending() {

        final Map<String, Object> brmBackupManagerAttributes = new HashMap<String, Object>();
        brmBackupManagerAttributes.put(BackupInventoryConstants.BACKUP_DOMAIN, "Domain");
        brmBackupManagerAttributes.put(BackupInventoryConstants.BACKUP_TYPE, "Type");

        final Map<String, Object> brmBackupMoAttributes1 = new HashMap<String, Object>();
        brmBackupMoAttributes1.put(BackupInventoryConstants.BACKUP_NAME, "BackupName1");
        brmBackupMoAttributes1.put(BackupInventoryConstants.CREATION_TIME, "2014-07-04T12:08:56Z");

        final Map<String, Object> brmBackupMoAttributes2 = new HashMap<String, Object>();
        brmBackupMoAttributes2.put(BackupInventoryConstants.BACKUP_NAME, "BackupName2");
        brmBackupMoAttributes2.put(BackupInventoryConstants.CREATION_TIME, "2017-07-04T12:08:56Z");

        final Map<String, Object> brmBackupMoAttributes3 = new HashMap<String, Object>();
        brmBackupMoAttributes3.put(BackupInventoryConstants.BACKUP_NAME, "BackupName2");
        brmBackupMoAttributes3.put(BackupInventoryConstants.CREATION_TIME, "2016-07-04T12:08:56Z");

        final BrmBackupManager brmBackupManager = new BrmBackupManager("brmBackupManagerMoFdn", brmBackupManagerAttributes);

        final BrmBackup backup1 = new BrmBackup("brmBackupMoFdn1", brmBackupMoAttributes1, brmBackupManager);
        final BrmBackup backup2 = new BrmBackup("brmBackupMoFdn2", brmBackupMoAttributes2, brmBackupManager);
        final BrmBackup backup3 = new BrmBackup("brmBackupMoFdn2", brmBackupMoAttributes3, brmBackupManager);

        final List<BrmBackup> listOfBrms = new ArrayList<BrmBackup>();
        listOfBrms.add(backup2);
        listOfBrms.add(backup1);
        listOfBrms.add(backup3);

        objectUnderTest.sortBrmBackUpsByCreationTime(listOfBrms, false);

        assertEquals("2017-07-04T12:08:56Z", listOfBrms.get(0).getCreationTime().getDateTime());
        assertEquals("2014-07-04T12:08:56Z", listOfBrms.get(2).getCreationTime().getDateTime());

    }

    @Test
    public void testSortBrmBackUpsByCreationTimeWithoutDate() {

        final Map<String, Object> brmBackupManagerAttributes = new HashMap<String, Object>();
        brmBackupManagerAttributes.put(BackupInventoryConstants.BACKUP_DOMAIN, "Domain");
        brmBackupManagerAttributes.put(BackupInventoryConstants.BACKUP_TYPE, "Type");

        final Map<String, Object> brmBackupMoAttributes1 = new HashMap<String, Object>();
        brmBackupMoAttributes1.put(BackupInventoryConstants.BACKUP_NAME, "BackupName1");
        brmBackupMoAttributes1.put(BackupInventoryConstants.CREATION_TIME, "2016-08-04T18:35:05");

        final Map<String, Object> brmBackupMoAttributes2 = new HashMap<String, Object>();
        brmBackupMoAttributes2.put(BackupInventoryConstants.BACKUP_NAME, "BackupName2");
        brmBackupMoAttributes2.put(BackupInventoryConstants.CREATION_TIME, "2017-07-04T12:08:56");

        final Map<String, Object> brmBackupMoAttributes3 = new HashMap<String, Object>();
        brmBackupMoAttributes3.put(BackupInventoryConstants.BACKUP_NAME, "BackupName3");
        brmBackupMoAttributes3.put(BackupInventoryConstants.CREATION_TIME, "");

        final Map<String, Object> brmBackupMoAttributes4 = new HashMap<String, Object>();
        brmBackupMoAttributes4.put(BackupInventoryConstants.BACKUP_NAME, "BackupName4");
        brmBackupMoAttributes4.put(BackupInventoryConstants.CREATION_TIME, "2014-07-04T12:08:56");

        final BrmBackupManager brmBackupManager = new BrmBackupManager("brmBackupManagerMoFdn", brmBackupManagerAttributes);
        final BrmBackup backup1 = new BrmBackup("brmBackupMoFdn1", brmBackupMoAttributes1, brmBackupManager);
        final BrmBackup backup2 = new BrmBackup("brmBackupMoFdn2", brmBackupMoAttributes2, brmBackupManager);
        final BrmBackup backup3 = new BrmBackup("brmBackupMoFdn3", brmBackupMoAttributes3, brmBackupManager);
        final BrmBackup backup4 = new BrmBackup("brmBackupMoFdn4", brmBackupMoAttributes4, brmBackupManager);

        final List<BrmBackup> listOfBrms = new ArrayList<BrmBackup>();
        listOfBrms.add(backup2);
        listOfBrms.add(backup1);
        listOfBrms.add(backup3);
        listOfBrms.add(backup4);
        objectUnderTest.sortBrmBackUpsByCreationTime(listOfBrms, true);
        assertEquals(backup3, listOfBrms.get(0));
        assertEquals(backup2, listOfBrms.get(3));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetNodeBackupActivityItemsWithEmptyInput() throws UnsupportedFragmentException {
        when(fdnServiceBeanMock.getNetworkElements(anyList(), anyString())).thenReturn(Arrays.asList(networkElementMock));
        when(neBatchPreparatorMock.groupNetworkElementsByModelidentity(eq(Arrays.asList(networkElementMock)), eq(FragmentType.ECIM_BRM_TYPE.getFragmentName()))).thenReturn(networkElementGroupMock);
        when(networkElementGroupMock.getNetworkElementMap()).thenReturn(Collections.EMPTY_MAP);

        final Map<String, List<EcimBackupItem>> allNodesBackupItems = objectUnderTest.getNodeBackupActivityItems(neInfoQueryMock);
        Assert.assertNotNull(allNodesBackupItems);
        verify(fdnServiceBeanMock, times(1)).getNetworkElements(anyList(), anyString());
        verify(neBatchPreparatorMock, times(1)).groupNetworkElementsByModelidentity(anyList(), anyString());
        verify(handlersProviderFactoryMock, never()).getBrmHandler(anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUpdatedJobActivities_calledBrmHandlerButnotSetNodeParams() throws UnsupportedFragmentException {
        when(fdnServiceBeanMock.getNetworkElements(anyList(), anyString())).thenReturn(Arrays.asList(networkElementMock));
        when(neBatchPreparatorMock.groupNetworkElementsByModelidentity(eq(Arrays.asList(networkElementMock)), eq(FragmentType.ECIM_BRM_TYPE.getFragmentName()))).thenReturn(networkElementGroupMock);
        final Map<OssModelInfo, List<NetworkElement>> mapMock = new HashMap<OssModelInfo, List<NetworkElement>>();
        mapMock.put(ossModelInfoMock, Arrays.asList(networkElementMock));
        when(networkElementGroupMock.getNetworkElementMap()).thenReturn(mapMock);
        when(handlersProviderFactoryMock.getBrmHandler(null)).thenReturn(brmHandlerMock);
        when(brmHandlerMock.getBackupActivityItems(Arrays.asList(networkElementMock), null)).thenReturn(nodesBackupActivityItems);

        final Map<String, List<EcimBackupItem>> allNodesBackupItems = objectUnderTest.getNodeBackupActivityItems(neInfoQueryMock);
        Assert.assertNotNull(allNodesBackupItems);

        verify(fdnServiceBeanMock, times(1)).getNetworkElements(anyList(), anyString());
        verify(neBatchPreparatorMock, times(1)).groupNetworkElementsByModelidentity(anyList(), anyString());
        verify(handlersProviderFactoryMock, times(1)).getBrmHandler(null);
        verify(brmHandlerMock, times(1)).getBackupActivityItems(Arrays.asList(networkElementMock), null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUpdatedJobActivities_noNEGroups_BrmHandlerNotCalled() throws UnsupportedFragmentException {
        when(fdnServiceBeanMock.getNetworkElements(anyList(), anyString())).thenReturn(Arrays.asList(networkElementMock));
        when(neBatchPreparatorMock.groupNetworkElementsByModelidentity(eq(Arrays.asList(networkElementMock)), eq(FragmentType.ECIM_BRM_TYPE.getFragmentName()))).thenReturn(networkElementGroupMock);
        when(networkElementGroupMock.getNetworkElementMap()).thenReturn(Collections.EMPTY_MAP);

        final Map<String, List<EcimBackupItem>> allNodesBackupItems = objectUnderTest.getNodeBackupActivityItems(neInfoQueryMock);
        Assert.assertNotNull(allNodesBackupItems);
        Assert.assertTrue(allNodesBackupItems.isEmpty());
        verify(fdnServiceBeanMock, times(1)).getNetworkElements(anyList(), anyString());
        verify(neBatchPreparatorMock, times(1)).groupNetworkElementsByModelidentity(anyList(), anyString());
        verify(handlersProviderFactoryMock, never()).getBrmHandler(anyString());
    }

    @Test
    public void testGetCommonBackupActivityItems() {
        final Map<String, List<EcimBackupItem>> allNodesBackupItems = new HashMap<String, List<EcimBackupItem>>();
        allNodesBackupItems.put("SGS-01", Arrays.asList(ecimBackupItemMock));
        when(nodesActivityItems.getValue()).thenReturn(Arrays.asList(ecimBackupItemMock));
        when(ecimBackupItemMock.getDomain()).thenReturn("sysDomain");
        when(ecimBackupItemMock.getType()).thenReturn("sysType");
        final Set<String> domainType = objectUnderTest.getCommonBackupActivityItems(allNodesBackupItems);
        Assert.assertNotNull(domainType);
        Assert.assertTrue(domainType.size() == 1);
    }

    @Test
    public void testGetBackupWithAutoGenerateTrue() throws MoNotFoundException, JobDataNotFoundException, UnsupportedFragmentException {
        setJobEnvironment();
        mainJobAttr = setMainJobAttributes();
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttr);
        final String nodeName = "NetworkElement01";
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN-MME");
        networkElement.setPlatformType(PlatformTypeEnum.ECIM);
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "businessKey", PlatformTypeEnum.ECIM.getName(), new Date().getTime(), null);
        final Map<String, String> backupDataMap = new HashMap<>();
        backupDataMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, "true");
        final Map<String, String> activeSoftwareMap = new HashMap<>();
        activeSoftwareMap.put(nodeName, "CXP9010021_1||R34S108");
        backupDataMap.put(EcimBackupConstants.BRM_BACKUP_MANAGER_ID, "bkup1/domain1");
        when(networkElementRetrivalBean.getNetworkElementData(neJobStaticData.getNodeName())).thenReturn(networkElementData);
        when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(backupDataMap);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(jobStaticData.getOwner()).thenReturn("administrator");
        when(activeSoftwareProvider.getActiveSoftwareDetails(Matchers.anyList())).thenReturn(activeSoftwareMap);
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getBackup(neJobStaticData);
        assertTrue(ecimBackupInfo != null);
        assertTrue(ecimBackupInfo.getBackupFileName() != null);
    }

    @Test
    public void testGetBackupWithAutoGenerate_NoActiveSoftware() throws MoNotFoundException, JobDataNotFoundException {
        setJobEnvironment();
        mainJobAttr = setMainJobAttributes();
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttr);
        final String nodeName = "NetworkElement01";
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN-MME");
        networkElement.setPlatformType(PlatformTypeEnum.ECIM);
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "businessKey", PlatformTypeEnum.ECIM.getName(), new Date().getTime(), null);
        final Map<String, String> backupDataMap = new HashMap<>();
        backupDataMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, ActivityConstants.CHECK_TRUE);
        final Map<String, String> activeSoftwareMap = new HashMap<>();
        backupDataMap.put(EcimBackupConstants.BRM_BACKUP_MANAGER_ID, "bkup1/domain1");
        when(networkElementRetrivalBean.getNetworkElementData(neJobStaticData.getNodeName())).thenReturn(networkElementData);
        when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(backupDataMap);
        when(activityUtils.getActivityJobAttributeValue(Matchers.anyMap(), Matchers.anyString())).thenReturn("");
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(jobStaticData.getOwner()).thenReturn("administrator");
        when(activeSoftwareProvider.getActiveSoftwareDetails(Matchers.anyList())).thenReturn(activeSoftwareMap);
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getBackup(neJobStaticData);
        assertTrue(ecimBackupInfo != null);
    }

    @Test
    public void testGetBackupWithAutoGenerateFalse() throws MoNotFoundException, JobDataNotFoundException {
        setJobEnvironment();
        mainJobAttr = setMainJobAttributes();
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttr);
        final String nodeName = "NetworkElement01";
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN-MME");
        networkElement.setPlatformType(PlatformTypeEnum.ECIM);
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "businessKey", PlatformTypeEnum.ECIM.getName(), new Date().getTime(), null);
        final Map<String, String> backupDataMap = new HashMap<>();
        backupDataMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, ActivityConstants.CHECK_FALSE);
        final Map<String, String> activeSoftwareMap = new HashMap<>();
        activeSoftwareMap.put(nodeName, "CXP9010021_1||R34S108");
        backupDataMap.put(EcimBackupConstants.BRM_BACKUP_MANAGER_ID, "bkup1/domain1");
        backupDataMap.put(EcimBackupConstants.BACKUP_NAME, "backup");
        when(networkElementRetrivalBean.getNetworkElementData(neJobStaticData.getNodeName())).thenReturn(networkElementData);
        when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(backupDataMap);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(jobStaticData.getOwner()).thenReturn("administrator");
        when(activeSoftwareProvider.getActiveSoftwareDetails(Matchers.anyList())).thenReturn(activeSoftwareMap);
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getBackup(neJobStaticData);
        assertTrue(ecimBackupInfo != null);
    }

    @Test
    public void testGetBackupWithAutoGenerateEnabled() throws MoNotFoundException, JobDataNotFoundException, UnsupportedFragmentException {
        jobLogList = new ArrayList<>();
        setJobEnvironment();
        mainJobAttr = setMainJobAttributes();
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttr);
        final String nodeName = "NetworkElement01";
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN-MME");
        networkElement.setPlatformType(PlatformTypeEnum.ECIM);
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "businessKey", PlatformTypeEnum.ECIM.getName(), new Date().getTime(), null);
        final Map<String, String> backupDataMap = new HashMap<>();
        backupDataMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, ActivityConstants.CHECK_TRUE);
        final Map<String, String> activeSoftwareMap = new HashMap<>();
        activeSoftwareMap.put(EcimCommonConstants.ProductData.PRODUCT_NUMBER, "CXP9010021_1");
        activeSoftwareMap.put(EcimCommonConstants.ProductData.PRODUCT_REVISION, "R34S108");
        backupDataMap.put(EcimBackupConstants.BRM_BACKUP_MANAGER_ID, "bkup1/domain1");
        backupDataMap.put(JobPropertyConstants.BRM_BACKUP_NAME, "$productnumber_$productrevision_$timestamp");
        when(networkElementRetrivalBean.getNetworkElementData(neJobStaticData.getNodeName())).thenReturn(networkElementData);
        when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(backupDataMap);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(jobStaticData.getOwner()).thenReturn("administrator");
        when(upMoServiceRetryProxyMock.getActiveSoftwareDetailsFromNode(nodeName)).thenReturn(activeSoftwareMap);
        when(autoGenerateNameValidator.getValidatedAutoGenerateBackupName(Matchers.anyString())).thenReturn("OP_CXP00_RX123");
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getBackupWithAutoGeneratedName(neJobStaticData, jobLogList);
        assertTrue(ecimBackupInfo != null);
    }

    @Test
    public void testGetBackupWithAutoGenerateDisbled() throws MoNotFoundException, JobDataNotFoundException, UnsupportedFragmentException {
        jobLogList = new ArrayList<>();
        setJobEnvironment();
        mainJobAttr = setMainJobAttributes();
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttr);
        final String nodeName = "NetworkElement01";
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN-MME");
        networkElement.setPlatformType(PlatformTypeEnum.ECIM);
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "businessKey", PlatformTypeEnum.ECIM.getName(), new Date().getTime(), null);
        final Map<String, String> backupDataMap = new HashMap<>();
        backupDataMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, ActivityConstants.CHECK_FALSE);
        backupDataMap.put(JobPropertyConstants.BRM_BACKUP_NAME, "backupName");
        final Map<String, String> activeSoftwareMap = new HashMap<>();
        activeSoftwareMap.put(nodeName, "CXP9010021_1||R34S108");
        backupDataMap.put(EcimBackupConstants.BRM_BACKUP_MANAGER_ID, "bkup1/domain1");
        when(networkElementRetrivalBean.getNetworkElementData(neJobStaticData.getNodeName())).thenReturn(networkElementData);
        when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(backupDataMap);
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(jobStaticData.getOwner()).thenReturn("administrator");
        when(upMoServiceRetryProxyMock.getActiveSoftwareDetailsFromNode(nodeName)).thenReturn(activeSoftwareMap);
        when(autoGenerateNameValidator.getValidatedAutoGenerateBackupName(Matchers.anyString())).thenReturn("backupName");
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getBackupWithAutoGeneratedName(neJobStaticData, jobLogList);
        assertTrue(ecimBackupInfo != null);
        Assert.assertEquals("backupName", ecimBackupInfo.getBackupName());
    }

    @Test
    public void testGetBackupWithAutoGenerate_NoActiveSoftwareFromNode() throws MoNotFoundException, JobDataNotFoundException, UnsupportedFragmentException {
        jobLogList = new ArrayList<>();
        setJobEnvironment();
        mainJobAttr = setMainJobAttributes();
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttr);
        final String nodeName = "NetworkElement01";
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN-MME");
        networkElement.setPlatformType(PlatformTypeEnum.ECIM);
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "businessKey", PlatformTypeEnum.ECIM.getName(), new Date().getTime(), null);
        final Map<String, String> backupDataMap = new HashMap<>();
        backupDataMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, ActivityConstants.CHECK_TRUE);
        final Map<String, String> activeSoftwareMap = new HashMap<>();
        backupDataMap.put(EcimBackupConstants.BRM_BACKUP_MANAGER_ID, "bkup1/domain1");
        backupDataMap.put(JobPropertyConstants.BRM_BACKUP_NAME, "$productnumber_$productrevision");
        when(networkElementRetrivalBean.getNetworkElementData(neJobStaticData.getNodeName())).thenReturn(networkElementData);
        when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(backupDataMap);
        when(activityUtils.getActivityJobAttributeValue(Matchers.anyMap(), Matchers.anyString())).thenReturn("");
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(jobStaticData.getOwner()).thenReturn("administrator");
        when(activeSoftwareProvider.getActiveSoftwareDetails(Matchers.anyList())).thenReturn(activeSoftwareMap);
        when(autoGenerateNameValidator.getValidatedAutoGenerateBackupName(Matchers.anyString())).thenReturn("");
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getBackupWithAutoGeneratedName(neJobStaticData, jobLogList);
        assertTrue(ecimBackupInfo != null);
        final String backupName = ecimBackupInfo.getBackupName();
        assertTrue(backupName.startsWith(EcimBackupConstants.DEFAULT_BACKUP_NAME));
    }

    @Test
    public void testGetBackupWithAutoGenerate_NullActiveSoftwareDetailsFromNode() throws MoNotFoundException, JobDataNotFoundException, UnsupportedFragmentException {
        jobLogList = new ArrayList<>();
        setJobEnvironment();
        mainJobAttr = setMainJobAttributes();
        when(jobEnvironment.getMainJobAttributes()).thenReturn(mainJobAttr);
        final String nodeName = "NetworkElement01";
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setNeType("SGSN-MME");
        networkElement.setPlatformType(PlatformTypeEnum.ECIM);
        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "businessKey", PlatformTypeEnum.ECIM.getName(), new Date().getTime(), null);
        final Map<String, String> backupDataMap = new HashMap<>();
        backupDataMap.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, ActivityConstants.CHECK_TRUE);
        backupDataMap.put(EcimBackupConstants.BRM_BACKUP_MANAGER_ID, "bkup1/domain1");
        final Map<String, String> activeSoftwareMap = new HashMap<>();
        activeSoftwareMap.put(nodeName, null);
        when(networkElementRetrivalBean.getNetworkElementData(neJobStaticData.getNodeName())).thenReturn(networkElementData);
        when(jobPropertyUtilsMock.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(backupDataMap);
        when(activityUtils.getActivityJobAttributeValue(Matchers.anyMap(), Matchers.anyString())).thenReturn("");
        when(jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId())).thenReturn(jobStaticData);
        when(jobStaticData.getOwner()).thenReturn("administrator");
        when(activeSoftwareProvider.getActiveSoftwareDetails(Matchers.anyList())).thenReturn(activeSoftwareMap);
        final EcimBackupInfo ecimBackupInfo = objectUnderTest.getBackupWithAutoGeneratedName(neJobStaticData, jobLogList);
        assertTrue(ecimBackupInfo != null);
        final String backupName = ecimBackupInfo.getBackupName();
        assertTrue(backupName.startsWith(EcimBackupConstants.DEFAULT_BACKUP_NAME));
    }

}
