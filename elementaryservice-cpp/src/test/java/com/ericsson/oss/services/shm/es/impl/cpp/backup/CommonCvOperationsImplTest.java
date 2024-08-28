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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

import static com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY;
import static com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.NodeModelNameSpaceProvider;
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReader;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;

@RunWith(MockitoJUnitRunner.class)
public class CommonCvOperationsImplTest {

    private static final String CV_NAME = "CV:ABCDE//XYZ-";
    private static final String NODE_NAME = "ERBS101";
    private static final String ACTION_TYPE_EXECUTE = "Execute";
    private static final String CV_MO_FDN = "NetworkElement=ERBS101,ConfigurationVersion=10";
    private static final int ACTION_ID = 1;
    private static final String ERBS_NODE_MODEL_NAMESPACE = "ERBS_NODE_MODEL";

    @Mock
    DpsReader dpsReaderMock;

    @Mock
    DpsWriter dpsWriterMock;

    @Mock
    ManagedObject mo;

    @Mock
    List<ManagedObject> moList;

    @InjectMocks
    CommonCvOperationsImpl commonCvOperationsImplMock = new CommonCvOperationsImpl();

    @Mock
    private NodeModelNameSpaceProvider nodeModelNameSpaceProviderMock;

    @Mock
    private NodeAttributesReader nodeAttributesReaderMock;

    @Mock
    ManagedObject managedObject;

    Map<String, Object> cvMo = new HashMap<String, Object>();
    Map<String, Object> actionParameters = new HashMap<String, Object>();

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteActionOnMoSuccess() {
        when(dpsWriterMock.performAction(eq(CV_MO_FDN), eq(ACTION_TYPE_EXECUTE), anyMap())).thenReturn(ACTION_ID);
        commonCvOperationsImplMock.executeActionOnMo(ACTION_TYPE_EXECUTE, CV_MO_FDN, null);
        assertEquals(ACTION_ID, commonCvOperationsImplMock.executeActionOnMo(ACTION_TYPE_EXECUTE, CV_MO_FDN, null));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteActionOnMoFailure() {
        when(dpsWriterMock.performAction(eq(CV_MO_FDN), eq(ACTION_TYPE_EXECUTE), anyMap())).thenReturn(ACTION_ID);
        commonCvOperationsImplMock.executeActionOnMo(ACTION_TYPE_EXECUTE, CV_MO_FDN, new HashMap<String, Object>());
        assertEquals(ACTION_ID, commonCvOperationsImplMock.executeActionOnMo(ACTION_TYPE_EXECUTE, CV_MO_FDN, null));
    }

    @Test
    public void testPrecheckForSetStartCVSetFistInRolbackSuccess() {
        final List<Map<String, Object>> storedCVList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> storedCv = new HashMap<String, Object>();
        storedCv.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, CV_NAME);
        storedCv.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_STATUS, "OK");
        storedCVList.add(storedCv);
        cvMo.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedCVList);
        cvMo.put(CV_MO_FDN, CV_MO_FDN);
        actionParameters.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedCVList);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, CV_NAME);
        assertTrue(commonCvOperationsImplMock.precheckForSetStartCVSetFistInRolback(cvMo, CV_NAME));
    }

    @Test
    public void testPrecheckForSetStartCVSetFistInRolbackFailure() {
        final List<Map<String, Object>> storedCVList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> storedCv = new HashMap<String, Object>();
        storedCv.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, CV_NAME);
        storedCv.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_STATUS, "NOK");
        storedCVList.add(storedCv);
        cvMo.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedCVList);
        cvMo.put(CV_MO_FDN, CV_MO_FDN);
        actionParameters.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedCVList);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, CV_NAME);
        assertFalse(commonCvOperationsImplMock.precheckForSetStartCVSetFistInRolback(cvMo, CV_NAME));
    }

    @Test
    public void testGetCVMO() {
        final Map<String, Object> restrictions = new HashMap<String, Object>();
        when(moList.get(0)).thenReturn(mo);
        when(nodeModelNameSpaceProviderMock.getNamespaceByNodeName(NODE_NAME)).thenReturn(ERBS_NODE_MODEL_NAMESPACE);
        when(dpsReaderMock.getManagedObjects(eq(ERBS_NODE_MODEL_NAMESPACE), eq(BackupActivityConstants.CV_MO_TYPE), eq(restrictions), eq(NODE_NAME))).thenReturn(moList);
        assertTrue(commonCvOperationsImplMock.getCVMo(NODE_NAME) != null);
    }

    @Test
    public void testGetCVMOAsNull() {
        final Map<String, Object> restrictions = new HashMap<String, Object>();
        restrictions.put(ConfigurationVersionMoConstants.CONFIGURATION_VERSION_ID, "1");
        final List<ManagedObject> managedObjectList = new ArrayList<ManagedObject>();
        when(dpsReaderMock.getManagedObjects(eq(ERBS_NODE_MODEL_NAMESPACE), eq(BackupActivityConstants.CV_MO_TYPE), eq(restrictions), eq(NODE_NAME))).thenReturn(managedObjectList);
        assertFalse(commonCvOperationsImplMock.getCVMo(NODE_NAME) != null);
    }

    @Test
    public void testGetUPMO() {
        final Map<String, Object> restrictions = new HashMap<String, Object>();
        restrictions.put(UpgradeActivityConstants.UPGRADE_PACKAGE_ID, "1");
        when(moList.get(0)).thenReturn(mo);
        when(nodeModelNameSpaceProviderMock.getNamespaceByNodeName(NODE_NAME)).thenReturn(ERBS_NODE_MODEL_NAMESPACE);
        when(dpsReaderMock.getManagedObjects(eq(ERBS_NODE_MODEL_NAMESPACE), eq(UpgradeActivityConstants.UP_MO_TYPE), eq(restrictions), eq(NODE_NAME))).thenReturn(moList);
        assertTrue(commonCvOperationsImplMock.getUPMo(NODE_NAME, "1") != null);
    }

    @Test
    public void testGetUPMOAsNull() {
        final Map<String, Object> restrictions = new HashMap<String, Object>();
        restrictions.put(UpgradeActivityConstants.UPGRADE_PACKAGE_ID, "1");
        final List<ManagedObject> managedObjectList = new ArrayList<ManagedObject>();
        when(dpsReaderMock.getManagedObjects(eq(ERBS_NODE_MODEL_NAMESPACE), eq(UpgradeActivityConstants.UP_MO_TYPE), eq(restrictions), eq(NODE_NAME))).thenReturn(managedObjectList);
        assertFalse(commonCvOperationsImplMock.getUPMo(NODE_NAME, "1") != null);
    }

    @Test
    public void testPrecheckForUploadCVActionSuccess() {
        cvMo.put(ConfigurationVersionMoConstants.CURRENT_MAIN_ACTIVITY, "IDLE");
        cvMo.put(ConfigurationVersionMoConstants.CURRENT_DETAILED_ACTIVITY, "IDLE");
        assertTrue(commonCvOperationsImplMock.precheckForUploadCVAction(cvMo, actionParameters));
    }

    @Test
    public void testPrecheckForUploadCVActionFailure() {
        assertFalse(commonCvOperationsImplMock.precheckForUploadCVAction(cvMo, actionParameters));
    }

    @Test
    public void testGetCVMoAttributesFromNode() {
        final String[] requiredCvMoAttributes = {};
        final Map<String, Object> cvMoData = new HashMap<String, Object>();
        cvMoData.put(CURRENT_MAIN_ACTIVITY, CVCurrentMainActivity.RESTORING_DOWNLOADED_BACKUP_CV);
        cvMoData.put(CURRENT_DETAILED_ACTIVITY, CVCurrentDetailedActivity.AWAITING_RESTORE_CONFIRMATION);
        when(dpsReaderMock.findMoByFdn(CV_MO_FDN)).thenReturn(mo);
        when(nodeAttributesReaderMock.readAttributes(mo, requiredCvMoAttributes)).thenReturn(cvMoData);
        final Map<String, Object> fetchedCvMoAttributes = commonCvOperationsImplMock.getCVMoAttributesFromNode(CV_MO_FDN, requiredCvMoAttributes);
        assertTrue(fetchedCvMoAttributes != null);
        assertFalse(fetchedCvMoAttributes.isEmpty());
    }
}
