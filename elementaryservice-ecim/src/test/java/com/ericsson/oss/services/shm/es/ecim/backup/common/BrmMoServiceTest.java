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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReader;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmDataDescriptorParser;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmHandler;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmVersionHandlersProviderFactory;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackup;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.BrmBackupManager;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo;
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimCommonConstants;
import com.ericsson.oss.services.shm.inventory.backup.ecim.impl.EcimBrmDataParsersProviderFactory;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

@RunWith(MockitoJUnitRunner.class)
public class BrmMoServiceTest {

    public static final String nodeName = "nodeName";

    public static final String backupName = "backupName";

    public static final String domainName = "domainName";

    public static final String backupType = "backupType";

    public static final String brmBackupManagerMoFdn = "brmBackupManagerMoFdn";

    public static final String referenceMIMVersion = "3.3";

    public static final String backupFileName = "backupFileName";

    long neJobId = 1818L;

    long activityJobId = 123L;

    @Mock
    JobPropertyUtils jobPropertyUtils;

    @Mock
    FdnServiceBean fdnServiceBean;

    @Mock
    OssModelInfoProvider ossModelInfoProvider;

    @Mock
    BrmVersionHandlersProviderFactory handlersProviderFactory;

    @Mock
    BrmHandler brmHandler;

    @InjectMocks
    BrmMoService brmMoService;

    @Mock
    AsyncActionProgress asyncActionProgress;

    @Mock
    DataPersistenceService dataPersistenceService;

    @Mock
    DataBucket liveBucket;

    @Mock
    ManagedObject brmBackupMO;

    @Mock
    EcimBrmDataParsersProviderFactory parsersProviderFactory;

    @Mock
    BrmDataDescriptorParser brmDataDescriptorParser;

    @Mock
    @Inject
    EcimOssBackupItemsReader smrsDataReader;

    @Mock
    @Inject
    JobUpdateService jobUpdateService;

    @Mock
    NetworkElement networkElementMock;

    @Mock
    @Inject
    ActivityUtils activityUtilsMock;

    @Mock
    private NodeAttributesReader nodeAttributesReaderMock;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Mock
    private NetworkElementData networkElementInfo;

    @Test
    public void testGetBrmBackup() throws MoNotFoundException, UnsupportedFragmentException {
        getBrmHandler();
        final Map<String, Object> brmBackupManagerAttributes = new HashMap<String, Object>();
        final Map<String, Object> brmBackupMoAttributes = new HashMap<String, Object>();
        final BrmBackupManager brmBackupManager = new BrmBackupManager(brmBackupManagerMoFdn, brmBackupManagerAttributes);
        final BrmBackup brmBackup = new BrmBackup(brmBackupManagerMoFdn, brmBackupMoAttributes, brmBackupManager);
        when(brmHandler.getBrmBackup(networkElementInfo, backupName, nodeName, domainName, backupType)).thenReturn(brmBackup);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        final BrmBackup brmBackupResponse = brmMoService.getBrmBackup(backupName, nodeName, domainName, backupType);
        assertNotNull(brmBackupResponse);
    }

    @Test
    public void testExecuteMoAction() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        getBrmHandler();
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmHandler.executeMoAction(networkElementInfo, nodeName, ecimBackupInfo, brmBackupManagerMoFdn, "createbackup")).thenReturn(1);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        int executeResult = brmMoService.executeMoAction(nodeName, ecimBackupInfo, brmBackupManagerMoFdn, "createbackup");
        assertEquals(executeResult, 1);
    }

    @Test
    public void testGetAsyncActionProgress() throws MoNotFoundException, UnsupportedFragmentException {
        getBrmHandler();
        final Map<String, AttributeChangeData> modifiedAttributes = new HashMap<String, AttributeChangeData>();
        final Map<String, Object> reportProgressAttributes = new HashMap<String, Object>();
        final AsyncActionProgress actionProgress = new AsyncActionProgress(reportProgressAttributes);
        when(brmHandler.getValidAsyncActionProgress("createbackup", modifiedAttributes)).thenReturn(actionProgress);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        AsyncActionProgress progress = brmMoService.getValidAsyncActionProgress(nodeName, "createbackup", modifiedAttributes);
        assertEquals(progress, actionProgress);
    }

    public void getBrmHandler() throws UnsupportedFragmentException {
        when(networkElementInfo.getNeType()).thenReturn("SGSN-MME");
        when(networkElementInfo.getOssModelIdentity()).thenReturn("6607-651-025");

        final OssModelInfo ossModelInfo = new OssModelInfo("namespace", "version", "ECIM_BrM", referenceMIMVersion);
        final List<OssModelInfo> ossModelInfoList = new ArrayList<OssModelInfo>();
        ossModelInfoList.add(ossModelInfo);
        when(ossModelInfoProvider.getOssModelInfo(networkElementInfo.getNeType(), networkElementInfo.getOssModelIdentity())).thenReturn(ossModelInfoList);
        when(handlersProviderFactory.getBrmHandler(referenceMIMVersion)).thenReturn(brmHandler);
    }

    @Test
    public void getAsyncActionProgressFromBrmBackupManagerForSpecificActivityTest() throws UnsupportedFragmentException, MoNotFoundException {
        getBrmHandler();
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        final Map<String, Object> brmBackupManagerAttributes = new HashMap<String, Object>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("actionName", "createBakcup");
        brmBackupManagerAttributes.put(EcimCommonConstants.ReportProgress.ASYNC_ACTION_PROGRESS, map);
        final BrmBackupManager brmBackupManager = new BrmBackupManager(brmBackupManagerMoFdn, brmBackupManagerAttributes);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        when(brmHandler.getBrmBackupManager((NetworkElementData) Matchers.anyObject(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(brmBackupManager);
        AsyncActionProgress actionProgress = brmMoService.getProgressFromBrmBackupManagerMO(nodeName, ecimBackupInfo);
        assertNotNull(actionProgress);
        assertEquals(actionProgress.getActionName(), "createBakcup");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getBackupDetailsTest() throws UnsupportedFragmentException, MoNotFoundException {
        getBrmHandler();
        final List<String> getBackupDetails = new ArrayList<String>();
        when(brmHandler.getBackupDetails((NetworkElementData) Matchers.anyObject(), Matchers.anyList(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(getBackupDetails);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        List<String> backupDetails = brmMoService.getBackupDetails(getBackupDetails, nodeName, domainName, backupType);
        assertNotNull(backupDetails);
    }

    @Test
    public void getNotifiableMoFdnTest() throws MoNotFoundException, UnsupportedFragmentException {
        getBrmHandler();
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmHandler.getNotifiableMoFdn(networkElementInfo, "Uploadbackup", nodeName, ecimBackupInfo)).thenReturn(brmBackupManagerMoFdn);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        final String moFdn = brmMoService.getNotifiableMoFdn("Uploadbackup", nodeName, ecimBackupInfo);
        assertEquals(brmBackupManagerMoFdn, moFdn);
    }

    @Test
    public void isBackupExistTest() throws MoNotFoundException, UnsupportedFragmentException {
        getBrmHandler();
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmHandler.isBackupCreationCompleted(networkElementInfo, nodeName, ecimBackupInfo)).thenReturn(true);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        assertTrue(brmMoService.isBackupExist(nodeName, ecimBackupInfo));
    }

    @Test
    public void isBackupDeletionCompletedTest() throws MoNotFoundException, UnsupportedFragmentException {
        getBrmHandler();
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmHandler.isBackupDeletionCompleted(networkElementInfo, nodeName, ecimBackupInfo)).thenReturn(true);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        assertTrue(brmMoService.isBackupDeletionCompleted(nodeName, ecimBackupInfo));
    }

    @Test
    public void getAsyncActionProgressFromBrmBackupForSpecificActivityTest() throws MoNotFoundException, UnsupportedFragmentException {
        getBrmHandler();
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmHandler.getAsyncActionProgressFromBrmBackup(networkElementInfo, nodeName, "Restore", ecimBackupInfo)).thenReturn(asyncActionProgress);

        final AsyncActionProgress actionProgress = brmMoService.getAsyncActionProgressFromBrmBackupForSpecificActivity(nodeName, "Restore", ecimBackupInfo);

        assertEquals(asyncActionProgress, actionProgress);
    }

    @Test
    public void isConfirmRequired() throws UnsupportedFragmentException, MoNotFoundException {
        getBrmHandler();
        when(brmHandler.isConfirmRequired(networkElementInfo, nodeName)).thenReturn(true);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        assertTrue(brmMoService.isConfirmRequired(nodeName));
    }

    @Test
    public void isSpecifiedBackupRestored() throws UnsupportedFragmentException, MoNotFoundException {
        getBrmHandler();
        final EcimBackupInfo ecimBackupInfo = new EcimBackupInfo(domainName, backupName, backupType);
        when(brmHandler.isSpecifiedBackupRestored(networkElementInfo, nodeName, ecimBackupInfo)).thenReturn(true);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        assertTrue(brmMoService.isSpecifiedBackupRestored(nodeName, ecimBackupInfo));
    }

    @Test
    public void testGetBackupNameFromBrmBackupMOFdn() {
        final String brmBackupFdn = "NetworkElement=SGSN-01,BrmBackup=1";
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(brmBackupFdn)).thenReturn(brmBackupMO);
        brmMoService.getBackupNameFromBrmBackupMOFdn(brmBackupFdn);
        final String[] attributes = { "backupName" };
        Mockito.verify(nodeAttributesReaderMock).readAttributes(brmBackupMO, attributes);
    }

    @Test
    public void extractAndAddBackupNameInNePropertiesTest() throws UnsupportedFragmentException {

        final OssModelInfo ossModelInfo = new OssModelInfo("namespace", "version", "ECIM_BrM", referenceMIMVersion);
        final List<OssModelInfo> ossModelInfoList = new ArrayList<OssModelInfo>();
        ossModelInfoList.add(ossModelInfo);
        when(ossModelInfoProvider.getOssModelInfo(Matchers.anyString(), Matchers.anyString())).thenReturn(ossModelInfoList);
        when(parsersProviderFactory.getBrmParserHandler(referenceMIMVersion)).thenReturn(brmDataDescriptorParser);
        final Map<String, Object> backupDetail = new HashMap<String, Object>();
        backupDetail.put(EcimBackupConstants.BACKUP_NAME, backupName);
        backupDetail.put(EcimBackupConstants.BRM_BKP_MNGR_BACKUP_DOMAIN, domainName);
        backupDetail.put(EcimBackupConstants.BRM_BKP_MNGR_BACKUP_TYPE, backupType);
        when(smrsDataReader.getBackupItems(Matchers.any(NetworkElementData.class), Matchers.any(BrmDataDescriptorParser.class), Matchers.anyString(), Matchers.anyString())).thenReturn(backupDetail);
        final List<Map<String, Object>> jobLogList = new ArrayList<Map<String, Object>>();
        when((jobUpdateService).updateRunningJobAttributes(activityJobId, null, jobLogList)).thenReturn(true);
        Mockito.doNothing().when(activityUtilsMock).addJobProperty(Matchers.anyString(), Matchers.anyObject(), Matchers.anyList());
        final EcimBackupInfo ecimBackupInfo = brmMoService.extractAndAddBackupNameInNeProperties(backupFileName, nodeName, neJobId, networkElementInfo);
        assertTrue(ecimBackupInfo != null);
        assertEquals("backupName", ecimBackupInfo.getBackupName());
        assertEquals("domainName", ecimBackupInfo.getDomainName());
        assertEquals("backupType", ecimBackupInfo.getBackupType());
    }

    @Test(expected = UnsupportedFragmentException.class)
    public void test_extractAndAddBackupNameInNeProperties_when_invalid_mimversion() throws UnsupportedFragmentException {

        Mockito.when(parsersProviderFactory.getBrmParserHandler("")).thenThrow(UnsupportedFragmentException.class);
        final EcimBackupInfo ecimBackupInfo = brmMoService.extractAndAddBackupNameInNeProperties(backupFileName, nodeName, neJobId, networkElementInfo);

    }

    @Test
    public void testexecuteCancelAction() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        getBrmHandler();
        final Map<String, Object> actionArguments = new HashMap<String, Object>();
        when(brmHandler.executeCancelAction(brmBackupManagerMoFdn, "actionName", actionArguments)).thenReturn(1);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementInfo);
        int result = brmMoService.executeCancelAction(nodeName, brmBackupManagerMoFdn, "actionName", actionArguments);
        assertTrue(result != 0);

    }

}
