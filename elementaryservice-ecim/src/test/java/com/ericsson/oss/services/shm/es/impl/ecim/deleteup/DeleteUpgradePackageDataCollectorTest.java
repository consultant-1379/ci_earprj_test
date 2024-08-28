/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.deleteup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoServiceRetryProxy;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackup;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackupCreationType;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.ProductData;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RunWith(MockitoJUnitRunner.class)
public class DeleteUpgradePackageDataCollectorTest {

    @InjectMocks
    DeleteUpgradePackageDataCollector objUnderTest;
    @Mock
    private ActivityUtils activityUtils;
    @Mock
    private JobUpdateService jobUpdateService;
    @Mock
    private BrmMoServiceRetryProxy brmMoServiceRetryProxy;
    @Mock
    private JobActivityInfo jobActivityInfo;
    @Mock
    private JobLogUtil jobLogUtil;
    @Mock
    ProductData productData;
    private static final String PROD_NUMBER = "CXP101";

    private static final String PROD_REVISION = "R9B01";
    Map<String, Set<String>> upMoData;
    String nodeName = "NodeName";
    String backupFdn = "BrmBackup=1";
    String backupData = "BackupName" + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + "ManagerFdn" + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR
            + BrmBackupCreationType.SYSTEM_CREATED.name() + DeleteUpgradePackageConstants.PRODUCTDATA_SEPERATOR + backupFdn;
    Set<String> upDataSet;
    Set<String> nodeUpData;
    List<BrmBackup> brmBackupsList;

    @Test
    public void testFetchValidUPDataOverInputDataWithNullNodeData() {
        final Set<String> inputProductDataSet = new HashSet<>();
        inputProductDataSet.add(PROD_NUMBER + "===" + PROD_REVISION);
        final Set<String> nodeData = null;
        final Set<String> inputData = objUnderTest.fetchValidUPDataOverInputData(nodeData, inputProductDataSet);
        Assert.assertNotNull(inputData);
        assertTrue(inputData.isEmpty());
    }

    @Test
    public void testFetchValidUPDataOverInputDataWithNodeData() {
        final Set<String> inputProductDataSet = new HashSet<>();
        inputProductDataSet.add(PROD_NUMBER + "===" + PROD_REVISION);
        inputProductDataSet.add("CXP101" + "===" + "R99I99");

        final Set<String> nodeData = new HashSet<>();
        nodeData.add(PROD_NUMBER + "===" + PROD_REVISION);

        final Set<String> inputData = objUnderTest.fetchValidUPDataOverInputData(nodeData, inputProductDataSet);
        Assert.assertNotNull(inputData);
        assertEquals(1, inputData.size());
        assertTrue(inputData.contains(PROD_NUMBER + "===" + PROD_REVISION));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPersistUPsWithSyscrBkpsDataInActivityJobProperties() {
        when(activityUtils.prepareJobPropertyList(Matchers.anyList(), Matchers.anyString(), Matchers.anyString())).thenReturn(new ArrayList<>());
        when(jobUpdateService.readAndUpdateRunningJobAttributes(Matchers.anyLong(), Matchers.anyList(), Matchers.anyList(), Matchers.anyDouble())).thenReturn(true);
        Map<String, Set<String>> upMoData = new HashMap<>();
        Set<String> backupsData = new HashSet<>();
        backupsData.add("Backup1");
        backupsData.add("Backup2");
        upMoData.put("UP1", backupsData);
        objUnderTest.persistUPsWithSyscrBkpsDataInActivityJobProperties(upMoData, 12345);
        verify(jobUpdateService,times(1)).readAndUpdateRunningJobAttributes(Matchers.anyLong(),Matchers.any(), Matchers.anyList(), Matchers.anyDouble());
    }

    @Test
    public void testgetDeletedSysCreatedBackupInfoFromAvailableUpMosData() {
        setUpDataMap();
        assertEquals(objUnderTest.getDeletedSysCreatedBackupInfo(upMoData, backupFdn), backupData);
    }

    @Test
    public void testgetDeletedSysCreatedBackupInfoIfUpsDataEmpty() {
        setUpDataMap();
        assertEquals("", objUnderTest.getDeletedSysCreatedBackupInfo(new HashMap<String, Set<String>>(), backupFdn));
    }

    @Test
    public void testcheckForDeletedSysCreatedBackupInfoReturnsBackupData() throws MoNotFoundException, UnsupportedFragmentException {
        setUpDataMap();
        when(brmMoServiceRetryProxy.isBackupDeletionCompleted(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(true);
        assertEquals(objUnderTest.checkForDeletedSysCreatedBackupInfo(upMoData, backupFdn), backupData);
    }

    @Test
    public void testcheckForDeletedSysCreatedBackupInfoReturnsEmptyIfBackupNotDeleted() throws MoNotFoundException, UnsupportedFragmentException {
        setUpDataMap();
        when(brmMoServiceRetryProxy.isBackupDeletionCompleted(Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(false);
        assertEquals("", objUnderTest.checkForDeletedSysCreatedBackupInfo(upMoData, backupFdn));
    }

    @Test
    public void testcheckForDeletedSysCreatedBackupInfo_throwsMoNotFoundException() throws MoNotFoundException, UnsupportedFragmentException {
        setUpDataMap();
        doThrow(MoNotFoundException.class).when(brmMoServiceRetryProxy).isBackupDeletionCompleted(Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        objUnderTest.checkForDeletedSysCreatedBackupInfo(upMoData, backupFdn);
        verify(brmMoServiceRetryProxy,times(1)).isBackupDeletionCompleted(Matchers.anyString(),Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testcheckForDeletedSysCreatedBackupInfo_throwsUnsupportedFragmentException() throws MoNotFoundException, UnsupportedFragmentException {
        setUpDataMap();
        doThrow(UnsupportedFragmentException.class).when(brmMoServiceRetryProxy).isBackupDeletionCompleted(Matchers.anyString(), Matchers.anyString(), Matchers.anyString());
        objUnderTest.checkForDeletedSysCreatedBackupInfo(upMoData, backupFdn);
        verify(brmMoServiceRetryProxy,times(1)).isBackupDeletionCompleted(Matchers.anyString(),Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testUnSubscribeToBrmBackupMosNotifications() {
        setUpDataMap();
        when(activityUtils.getActivityInfo(Matchers.anyLong(), Matchers.any())).thenReturn(jobActivityInfo);
        when(activityUtils.unSubscribeToMoNotifications(Matchers.anyString(), Matchers.anyLong(), Matchers.any(JobActivityInfo.class))).thenReturn(true);
        objUnderTest.unSubscribeToBrmBackupMosNotifications(upMoData, 12345);
        verify(activityUtils,times(2)).unSubscribeToMoNotifications(Matchers.anyString(),Matchers.anyLong(), Matchers.any());
    }

    private void setUpDataMap() {
        upMoData = new HashMap<>();
        Set<String> backupsData = new HashSet<>();
        backupsData.add(backupData);
        upMoData.put("UP1", backupsData);
    }

}
