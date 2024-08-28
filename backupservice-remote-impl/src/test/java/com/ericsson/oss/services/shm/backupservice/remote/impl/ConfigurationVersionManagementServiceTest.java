/*
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 **/

package com.ericsson.oss.services.shm.backupservice.remote.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.exception.general.DpsIllegalStateException;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.NotDefinedInModelException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.ESecuritySubject;
import com.ericsson.oss.services.shm.backupservice.remote.api.CVOperationRemoteException;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.ResourceOperations;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CommonRemoteCvManagementService;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;

@RunWith(PowerMockRunner.class)
public class ConfigurationVersionManagementServiceTest {

    private static final String NODE_NAME = "nodeName";
    private static final String CV_NAME = "cvName";
    private static final String IDENTITY = "identity";
    private static final String COMMENT = "comment";
    private static final String OPERATOR_NAME = "operatorName";
    private static final String CONFIGURATION_VERSION_FDN = "MeContext=shmDummyContext,ManagedElement=1,SoftwareManagement=1,ConfigurationVersion=1";
    private static final String NETYPE = "ERBS";
    private static final String ACCOUNT_TYPE = SmrsServiceConstants.BACKUP_ACCOUNT;

    @Mock
    CVManagementServiceFactory cvManagementServiceFactoryMock;

    @Mock
    CommonRemoteCvManagementService commonRemoteCvManagementServiceMock;

    @Mock
    ActivityUtils activityUtilsMock;

    @Mock
    SmrsFileStoreService smrsServiceUtilMock;

    @Mock
    ResourceOperations resourceOperations;

    @Mock
    FdnServiceBean fdnServiceBean;

    @Mock
    NetworkElement networkElement;

    @Mock
    EAccessControl accessControl;

    @Mock
    ESecuritySubject securitySubject;

    private ManagedObject cvManagedObject = null;
    private ManagedObject upManagedObject = null;
    private ConfigurationVersionMO configMoMock;
    private UpgradePackageMO upMoMock;

    @InjectMocks
    ConfigurationVersionManagementService configurationVersionManagementService;

    @Before
    public void setup() {
        createCVMOAttributes();
        configMoMock = new ConfigurationVersionMO(cvManagedObject.getFdn(), cvManagedObject.getAllAttributes());
        upMoMock = new UpgradePackageMO(upManagedObject.getFdn(), upManagedObject.getAllAttributes());

    }

    /*
     * CREATE CV UNIT TESTS
     */

    /**
     * CV Name = Not null, Node name = Not null, Identity = Not null, Comment = Not null, Author: ekatyas
     */
    @Test
    public void testCreateCV_supplyAllNotNullParams_returnsSuccess() throws CVOperationRemoteException {

        when(accessControl.getAuthUserSubject()).thenReturn(securitySubject);
        when(securitySubject.getSubjectId()).thenReturn(OPERATOR_NAME);

        final Map<String, Object> actionParameters = new HashMap<String, Object>();
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, CV_NAME);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_IDENTITY, IDENTITY);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_TYPE, "OTHER");
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_OPERATOR_NAME, OPERATOR_NAME);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_COMMENT, COMMENT);

        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_CREATE_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        when(commonRemoteCvManagementServiceMock.getCVMo(NODE_NAME)).thenReturn(configMoMock);
        doNothing().when(activityUtilsMock).recordEvent(SHMEvents.CREATE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding creation of CV by service request");
        when(commonRemoteCvManagementServiceMock.precheckOnMo(configMoMock.getAllAttributes(), actionParameters)).thenReturn(true);
        when(commonRemoteCvManagementServiceMock.executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters)).thenReturn(1);
        assertTrue(configurationVersionManagementService.createCV(NODE_NAME, CV_NAME, IDENTITY, COMMENT));

        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_CREATE_CV);
        verify(commonRemoteCvManagementServiceMock).getCVMo(NODE_NAME);
        verify(activityUtilsMock).recordEvent(SHMEvents.CREATE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding creation of CV by service request with cvName: " + CV_NAME + " identity: " + IDENTITY + " comment: " + COMMENT + " operatorName: " + OPERATOR_NAME);
        verify(commonRemoteCvManagementServiceMock).precheckOnMo(configMoMock.getAllAttributes(), actionParameters);
        verify(commonRemoteCvManagementServiceMock).executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters);
    }

    /**
     * CV Name = Not null, Node name = null, Identity = null, Comment = null, Author ekatyas
     */
    @Test
    public void testCreateCV_nodeNameCVNameIdentityNull_returnsFailure() throws CVOperationRemoteException {
        when(cvManagementServiceFactoryMock.getCvManagementService(null, BackupActivityConstants.ACTION_CREATE_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        assertFalse(configurationVersionManagementService.createCV(null, CV_NAME, null, null));
        verify(cvManagementServiceFactoryMock).getCvManagementService(null, BackupActivityConstants.ACTION_CREATE_CV);
    }

    /**
     * CV Name = null, CV MO = Not null, UP MO = Not null, currentUpgradePackage = Not null, precheckOnMo = false, Author ekatyas
     */
    @Test
    public void testCreateCV_cvNameNullUPMONull_returnsFailure() throws CVOperationRemoteException {
        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_CREATE_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        when(commonRemoteCvManagementServiceMock.getCVMo(NODE_NAME)).thenReturn(configMoMock);
        when(commonRemoteCvManagementServiceMock.getUPMo(NODE_NAME, "ABC")).thenReturn(null);
        assertFalse(configurationVersionManagementService.createCV(NODE_NAME, null, IDENTITY, COMMENT));
        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_CREATE_CV);
        verify(commonRemoteCvManagementServiceMock).getCVMo(NODE_NAME);
        verify(commonRemoteCvManagementServiceMock).getUPMo(NODE_NAME, "ABC");
    }

    /**
     * CV Name = null, CV MO = Not null, UP MO = Not null, currentUpgradePackage = Not null, precheckOnMo = false, Author ekatyas
     */
    @Test
    public void testCreateCV_cvNameNullUPMONotNull_returnsFailure() throws CVOperationRemoteException {
        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_CREATE_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        when(commonRemoteCvManagementServiceMock.getCVMo(NODE_NAME)).thenReturn(configMoMock);
        when(commonRemoteCvManagementServiceMock.getUPMo(NODE_NAME, "ABC")).thenReturn(upMoMock);
        assertFalse(configurationVersionManagementService.createCV(NODE_NAME, null, IDENTITY, COMMENT));
        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_CREATE_CV);
        verify(commonRemoteCvManagementServiceMock).getCVMo(NODE_NAME);
        verify(commonRemoteCvManagementServiceMock).getUPMo(NODE_NAME, "ABC");
    }

    /**
     * CV Name = Not null, CV MO = null, UP MO = null, currentUpgradePackage = null Author ekatyas
     */
    @Test(expected = CVOperationRemoteException.class)
    public void testCreateCV_cvMONull_ThrowsException() throws CVOperationRemoteException {
        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_CREATE_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        configurationVersionManagementService.createCV(NODE_NAME, CV_NAME, IDENTITY, COMMENT);
        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_CREATE_CV);
    }

    /**
     * CV Name = Not null, CV MO = Not null, UP MO = Not null, currentUpgradePackage = Not null, precheckOnMo = true, Author ekatyas
     */
    @SuppressWarnings("unchecked")
    @Test(expected = CVOperationRemoteException.class)
    public void testCreateCV_unknownExceptionWhileExecuteAction_throwsException() throws CVOperationRemoteException {
        when(accessControl.getAuthUserSubject()).thenReturn(securitySubject);
        when(securitySubject.getSubjectId()).thenReturn(OPERATOR_NAME);

        final Map<String, Object> actionParameters = new HashMap<String, Object>();
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, CV_NAME);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_IDENTITY, IDENTITY);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_TYPE, "OTHER");
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_OPERATOR_NAME, OPERATOR_NAME);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_COMMENT, COMMENT);

        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_CREATE_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        when(commonRemoteCvManagementServiceMock.getCVMo(NODE_NAME)).thenReturn(configMoMock);
        doNothing().when(activityUtilsMock).recordEvent(SHMEvents.CREATE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding creation of CV by service request");
        when(commonRemoteCvManagementServiceMock.precheckOnMo(configMoMock.getAllAttributes(), actionParameters)).thenReturn(true);
        when(commonRemoteCvManagementServiceMock.executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters)).thenThrow(CVOperationRemoteException.class);
        configurationVersionManagementService.createCV(NODE_NAME, CV_NAME, IDENTITY, COMMENT);

        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_CREATE_CV);
        verify(commonRemoteCvManagementServiceMock).getCVMo(NODE_NAME);
        verify(activityUtilsMock).recordEvent(SHMEvents.CREATE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding creation of CV by service request with cvName: " + CV_NAME + " identity: " + IDENTITY + " comment: " + COMMENT + " operatorName: " + OPERATOR_NAME);
        verify(commonRemoteCvManagementServiceMock).precheckOnMo(configMoMock.getAllAttributes(), actionParameters);
        verify(commonRemoteCvManagementServiceMock).executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters);
    }

    @Test
    public void testCreateCV_supplyAllNotNullParams_returnsSuccess_WithDefaultOperatorName() throws CVOperationRemoteException {

        when(accessControl.getAuthUserSubject()).thenReturn(null);

        final Map<String, Object> actionParameters = new HashMap<String, Object>();
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, CV_NAME);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_IDENTITY, IDENTITY);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_TYPE, "OTHER");
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_OPERATOR_NAME, "");
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_COMMENT, COMMENT);

        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_CREATE_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        when(commonRemoteCvManagementServiceMock.getCVMo(NODE_NAME)).thenReturn(configMoMock);
        doNothing().when(activityUtilsMock).recordEvent(SHMEvents.CREATE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding creation of CV by service request");
        when(commonRemoteCvManagementServiceMock.precheckOnMo(configMoMock.getAllAttributes(), actionParameters)).thenReturn(true);
        when(commonRemoteCvManagementServiceMock.executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters)).thenReturn(1);
        assertTrue(configurationVersionManagementService.createCV(NODE_NAME, CV_NAME, IDENTITY, COMMENT));

        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_CREATE_CV);
        verify(commonRemoteCvManagementServiceMock).getCVMo(NODE_NAME);
        verify(activityUtilsMock).recordEvent(SHMEvents.CREATE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding creation of CV by service request with cvName: " + CV_NAME + " identity: " + IDENTITY + " comment: " + COMMENT + " operatorName: " + "");
        verify(commonRemoteCvManagementServiceMock).precheckOnMo(configMoMock.getAllAttributes(), actionParameters);
        verify(commonRemoteCvManagementServiceMock).executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters);
    }

    /*
     * UPLOAD CV UNIT TESTS
     */

    /**
     * CV Name = Not null, Node name = Not null, Identity = Not null, Comment = Not null, Execute Action returns success, Author ekatyas
     */
    @Test
    public void testUploadCVSupplyAllNotNullParamsReturnsSuccess() throws CVOperationRemoteException {
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();

        smrsDetails.setPathOnServer("pathOnFtpServer");
        smrsDetails.setServerIpAddress("ftpServerIpAddress");
        smrsDetails.setUser("user");
        smrsDetails.setPassword("password".toCharArray());
        smrsDetails.setRelativePathToSmrsRoot("relativePathFromNetworkType");
        final List<NetworkElement> networkElementList = new ArrayList<>();
        networkElementList.add(networkElement);
        final Map<String, Object> actionParameters = new HashMap<>();
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_NAME, CV_NAME);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_PATH_ON_FTP_SERVER, "relativePathFromNetworkType/" + NODE_NAME);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_BACKUP_NAME_ON_FTP_SERVER, CV_NAME);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_IP_ADDRESS, "ftpServerIpAddress");
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_USER_ID, "user");
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_PASSWORD, "password");
        doNothing().when(resourceOperations).createDirectory("pathOnFtpServer", NODE_NAME);
        when(resourceOperations.isDirectoryExistsWithWritePermissions("pathOnFtpServer", NODE_NAME)).thenReturn(true);

        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_UPLOAD_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        when(commonRemoteCvManagementServiceMock.getCVMo(NODE_NAME)).thenReturn(configMoMock);
        doNothing().when(activityUtilsMock).recordEvent(SHMEvents.UPLOAD_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding with upload of CV by service request");
        when(fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList("nodeName"))).thenReturn(networkElementList);
        when(networkElementList.get(0).getNeType()).thenReturn(NETYPE);
        when(smrsServiceUtilMock.getSmrsDetails(ACCOUNT_TYPE, NETYPE, NODE_NAME)).thenReturn(smrsDetails);
        when(commonRemoteCvManagementServiceMock.precheckOnMo(configMoMock.getAllAttributes(), actionParameters)).thenReturn(true);
        when(commonRemoteCvManagementServiceMock.executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters)).thenReturn(1);
        assertTrue(configurationVersionManagementService.uploadCV(NODE_NAME, CV_NAME));

        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_UPLOAD_CV);
        verify(commonRemoteCvManagementServiceMock).getCVMo(NODE_NAME);
        verify(activityUtilsMock)
                .recordEvent(SHMEvents.UPLOAD_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(), "SHM:" + NODE_NAME + ":Proceeding with upload of CV by service request");
        verify(smrsServiceUtilMock).getSmrsDetails(ACCOUNT_TYPE, NETYPE, NODE_NAME);
        verify(commonRemoteCvManagementServiceMock).precheckOnMo(configMoMock.getAllAttributes(), actionParameters);
        verify(commonRemoteCvManagementServiceMock).executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters);
    }

    /**
     * CV Name = Not null, Node name = Not null, Identity = Not null, Comment = Not null, Execute Action returns failure, Author ekatyas
     */
    @Test
    public void testUploadCVSupplyAllNotNullParamsReturnsFailure() throws CVOperationRemoteException {
        final String user = "userName";
        final char[] password = { 'p', 'a', 's', 's', 'w', '0', 'r', 'd' };
        final String ipAddress = "ipAddress";
        final String pathOnServer = "/smrsroot/";
        final String relativePath = "/rootsmrs/software/erbs/";
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(pathOnServer);
        smrsDetails.setServerIpAddress(ipAddress);
        smrsDetails.setUser(user);
        smrsDetails.setPassword(password);
        smrsDetails.setRelativePathToSmrsRoot(relativePath);
        final List<NetworkElement> networkElementList = new ArrayList<>();
        networkElementList.add(networkElement);
        final Map<String, Object> actionParameters = new HashMap<>();
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, CV_NAME);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_PATH_ON_FTP_SERVER, "pathOnFtpServer");
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CV_BACKUP_NAME_ON_FTP_SERVER, BackupActivityConstants.EMPTY_STRING);
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_IP_ADDRESS, "ftpServerIpAddress");
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_USER_ID, "user");
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_FTP_SERVER_PASSWORD, "password");

        doNothing().when(resourceOperations).createDirectory("pathOnFtpServer", NODE_NAME);
        when(resourceOperations.isDirectoryExistsWithWritePermissions("pathOnFtpServer", NODE_NAME)).thenReturn(true);
        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_UPLOAD_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        when(commonRemoteCvManagementServiceMock.getCVMo(NODE_NAME)).thenReturn(configMoMock);
        doNothing().when(activityUtilsMock).recordEvent(SHMEvents.UPLOAD_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding with upload of CV by service request");

        when(fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList(NODE_NAME))).thenReturn(networkElementList);
        when(networkElementList.get(0).getNeType()).thenReturn(NETYPE);

        when(smrsServiceUtilMock.getSmrsDetails(ACCOUNT_TYPE, NETYPE, NODE_NAME)).thenReturn(smrsDetails);
        when(commonRemoteCvManagementServiceMock.precheckOnMo(configMoMock.getAllAttributes(), actionParameters)).thenReturn(true);
        when(commonRemoteCvManagementServiceMock.executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters)).thenReturn(1);
        assertFalse(configurationVersionManagementService.uploadCV(NODE_NAME, CV_NAME));

        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_UPLOAD_CV);
        verify(commonRemoteCvManagementServiceMock).getCVMo(NODE_NAME);
        verify(activityUtilsMock)
                .recordEvent(SHMEvents.UPLOAD_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(), "SHM:" + NODE_NAME + ":Proceeding with upload of CV by service request");
        verify(smrsServiceUtilMock).getSmrsDetails(ACCOUNT_TYPE, NETYPE, NODE_NAME);
    }

    /**
     * CV Name = null, NE name = null, Author ekatyas
     */
    @Test
    public void testUploadCV_cvNameNodeNameEmpty_returnsFailure() throws CVOperationRemoteException {
        when(cvManagementServiceFactoryMock.getCvManagementService(null, BackupActivityConstants.ACTION_UPLOAD_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        assertFalse(configurationVersionManagementService.uploadCV(null, null));
        verify(cvManagementServiceFactoryMock).getCvManagementService(null, BackupActivityConstants.ACTION_UPLOAD_CV);
    }

    /**
     * CV Name = Not null, NE name = Not null, CV MO = Not null, precheckOnMo false, Author ekatyas
     */
    @Test
    public void testUploadCV_precheckOnMOFailure_returnsFailure() throws CVOperationRemoteException {
        final String user = "userName";
        final char[] password = { 'p', 'a', 's', 's', 'w', '0', 'r', 'd' };
        final String ipAddress = "ipAddress";
        final String pathOnServer = "/smrsroot/";
        final String relativePath = "/rootsmrs/software/erbs/";
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setPathOnServer(pathOnServer);
        smrsDetails.setServerIpAddress(ipAddress);
        smrsDetails.setUser(user);
        smrsDetails.setPassword(password);
        smrsDetails.setRelativePathToSmrsRoot(relativePath);
        final List<NetworkElement> networkElementList = new ArrayList<NetworkElement>();
        networkElementList.add(networkElement);
        when(fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList(NODE_NAME))).thenReturn(networkElementList);
        when(networkElementList.get(0).getNeType()).thenReturn(NETYPE);
        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_UPLOAD_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        when(commonRemoteCvManagementServiceMock.getCVMo(NODE_NAME)).thenReturn(configMoMock);
        when(smrsServiceUtilMock.getSmrsDetails(ACCOUNT_TYPE, NETYPE, NODE_NAME)).thenReturn(smrsDetails);
        assertFalse(configurationVersionManagementService.uploadCV(NODE_NAME, CV_NAME));
        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_UPLOAD_CV);
        verify(commonRemoteCvManagementServiceMock).getCVMo(NODE_NAME);
    }

    /*
     * SET STARTABLE CV UNIT TESTS
     */

    /**
     * CV name = Not null, Node Name = Not null, CV MO = Not null, precheck on MO and execute action true, Author ekatyas
     * 
     * @throws CVOperationRemoteException
     */
    @Test
    public void testSetStartableCV_supplyAllNotNullParameters_returnsSuccess() throws CVOperationRemoteException {
        final Map<String, Object> actionParameters = new HashMap<String, Object>();
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, CV_NAME);
        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_SET_STARTABLE_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        when(commonRemoteCvManagementServiceMock.getCVMo(NODE_NAME)).thenReturn(configMoMock);
        doNothing().when(activityUtilsMock).recordEvent(SHMEvents.SETSTARTABLE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding with setStartable of CV by service request");
        when(commonRemoteCvManagementServiceMock.precheckOnMo(configMoMock.getAllAttributes(), actionParameters)).thenReturn(true);
        when(commonRemoteCvManagementServiceMock.executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters)).thenReturn(1);
        assertTrue(configurationVersionManagementService.setStartableCV(NODE_NAME, CV_NAME));

        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_SET_STARTABLE_CV);
        verify(commonRemoteCvManagementServiceMock).getCVMo(NODE_NAME);
        verify(activityUtilsMock).recordEvent(SHMEvents.SETSTARTABLE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding with setStartable of CV by service request");
        verify(commonRemoteCvManagementServiceMock).executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters);
    }

    /**
     * CV Name = null, Node name = null, Author ekatyas
     * 
     * @throws CVOperationRemoteException
     */
    @Test
    public void testSetStartableCV_CVNameNodeNameNull_returnsFailure() throws CVOperationRemoteException {
        when(cvManagementServiceFactoryMock.getCvManagementService(null, BackupActivityConstants.ACTION_SET_STARTABLE_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        assertFalse(configurationVersionManagementService.setStartableCV(null, null));
        verify(cvManagementServiceFactoryMock).getCvManagementService(null, BackupActivityConstants.ACTION_SET_STARTABLE_CV);
    }

    /**
     * CV Name = Not null, Node name = Not null, CV MO = null, Author ekatyas
     * 
     * @throws CVOperationRemoteException
     */
    @Test(expected = CVOperationRemoteException.class)
    public void testSetStartableCV_CVMONull_throwsException() throws CVOperationRemoteException {
        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_SET_STARTABLE_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        configurationVersionManagementService.setStartableCV(NODE_NAME, CV_NAME);
        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_SET_STARTABLE_CV);
    }

    /**
     * CV name = Not null, Node Name = Not null, CV MO = Not null, precheck on MO and execute action true, Author ekatyas
     * 
     * @throws CVOperationRemoteException
     */
    @SuppressWarnings("unchecked")
    @Test(expected = CVOperationRemoteException.class)
    public void testSetStartableCV_executeActionReturnsException_throwsException() throws CVOperationRemoteException {
        final Map<String, Object> actionParameters = new HashMap<String, Object>();
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, CV_NAME);
        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_SET_STARTABLE_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        when(commonRemoteCvManagementServiceMock.getCVMo(NODE_NAME)).thenReturn(configMoMock);
        doNothing().when(activityUtilsMock).recordEvent(SHMEvents.SETSTARTABLE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding with setStartable of CV by service request");
        when(commonRemoteCvManagementServiceMock.precheckOnMo(configMoMock.getAllAttributes(), actionParameters)).thenReturn(true);
        when(commonRemoteCvManagementServiceMock.executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters)).thenThrow(CVOperationRemoteException.class);
        assertTrue(configurationVersionManagementService.setStartableCV(NODE_NAME, CV_NAME));
        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_SET_STARTABLE_CV);
        verify(commonRemoteCvManagementServiceMock).getCVMo(NODE_NAME);
        verify(activityUtilsMock).recordEvent(SHMEvents.SETSTARTABLE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding with setStartable of CV by service request");
        verify(commonRemoteCvManagementServiceMock).precheckOnMo(configMoMock.getAllAttributes(), actionParameters);
        verify(commonRemoteCvManagementServiceMock).executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters);
    }

    /*
     * SET SV FIRST IN ROLL BACK LIST UNIT TESTS
     */

    /**
     * CV Name = Not null, Node name = Not null, CV MO = Not null, precheckOnMo = success, Author ekatyas
     * 
     * @throws CVOperationRemoteException
     */
    @Test
    public void testSetCVFirstInRollBackList_supplyAllNotNullParams_returnsSuccess() throws CVOperationRemoteException {
        final Map<String, Object> actionParameters = new HashMap<String, Object>();
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, CV_NAME);
        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        when(commonRemoteCvManagementServiceMock.getCVMo(NODE_NAME)).thenReturn(configMoMock);
        doNothing().when(activityUtilsMock).recordEvent(SHMEvents.SETSTARTABLE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding with setCVFirstInRollBackList of CV by service request");
        when(commonRemoteCvManagementServiceMock.precheckOnMo(configMoMock.getAllAttributes(), actionParameters)).thenReturn(true);
        when(commonRemoteCvManagementServiceMock.executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters)).thenReturn(1);
        assertTrue(configurationVersionManagementService.setCVFirstInRollBackList(NODE_NAME, CV_NAME));
        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV);
        verify(commonRemoteCvManagementServiceMock).getCVMo(NODE_NAME);
        verify(activityUtilsMock).recordEvent(SHMEvents.SETSTARTABLE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding with setCVFirstInRollBackList of CV by service request");
        verify(commonRemoteCvManagementServiceMock).executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters);
    }

    /**
     * CV Name = null, NE name = Not null, Author ekatyas
     * 
     * @throws CVOperationRemoteException
     */
    @Test
    public void testSetCVFirstInRollBackList_emptyCVNameNodeName_returnsFailure() throws CVOperationRemoteException {
        when(cvManagementServiceFactoryMock.getCvManagementService(null, BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        assertFalse(configurationVersionManagementService.setCVFirstInRollBackList(null, null));
        verify(cvManagementServiceFactoryMock).getCvManagementService(null, BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV);
    }

    /**
     * CV Name = not null, NE name = not null, CV MO = null, Author ekatyas
     * 
     * @throws CVOperationRemoteException
     */
    @Test(expected = CVOperationRemoteException.class)
    public void testSetCVFirstInRollBackList_cvMONull_throwsException() throws CVOperationRemoteException {
        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        configurationVersionManagementService.setCVFirstInRollBackList(NODE_NAME, CV_NAME);
        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV);
    }

    /**
     * CV Name = Not null, Node name = Not null, CV MO = Not null, precheckOnMo = success, Author ekatyas
     * 
     * @throws CVOperationRemoteException
     */
    @SuppressWarnings("unchecked")
    @Test(expected = CVOperationRemoteException.class)
    public void testSetCVFirstInRollBackList_executeActionReturnsException_throwsException() throws CVOperationRemoteException {
        final Map<String, Object> actionParameters = new HashMap<String, Object>();
        actionParameters.put(ConfigurationVersionMoConstants.ACTION_ARG_CONFIGURATION_VERSION_NAME, CV_NAME);
        when(cvManagementServiceFactoryMock.getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV)).thenReturn(commonRemoteCvManagementServiceMock);
        when(commonRemoteCvManagementServiceMock.getCVMo(NODE_NAME)).thenReturn(configMoMock);
        doNothing().when(activityUtilsMock).recordEvent(SHMEvents.SETSTARTABLE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding with setCVFirstInRollBackList of CV by service request");
        when(commonRemoteCvManagementServiceMock.precheckOnMo(configMoMock.getAllAttributes(), actionParameters)).thenReturn(true);
        when(commonRemoteCvManagementServiceMock.executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters)).thenThrow(CVOperationRemoteException.class);
        assertTrue(configurationVersionManagementService.setCVFirstInRollBackList(NODE_NAME, CV_NAME));
        verify(cvManagementServiceFactoryMock).getCvManagementService(NODE_NAME, BackupActivityConstants.ACTION_SET_FIRST_IN_ROLLBACK_CV);
        verify(commonRemoteCvManagementServiceMock).getCVMo(NODE_NAME);
        verify(activityUtilsMock).recordEvent(SHMEvents.SETSTARTABLE_BACKUP_SERVICE_REQUEST_ACTION, NODE_NAME, configMoMock.getFdn(),
                "SHM:" + NODE_NAME + ":Proceeding with setCVFirstInRollBackList of CV by service request");
        verify(commonRemoteCvManagementServiceMock).precheckOnMo(configMoMock.getAllAttributes(), actionParameters);
        verify(commonRemoteCvManagementServiceMock).executeAction(configMoMock.getFdn(), NODE_NAME, actionParameters);
    }

    private void createCVMOAttributes() {
        final Map<String, Object> cvMOAttributesMap = new HashMap<String, Object>();
        cvMOAttributesMap.put(ConfigurationVersionMoConstants.CURRENT_UPGRADE_PACKAGE, "CXC1090070=100=200=ABC");
        final Map<String, String> cvMOProductInfoMap = new HashMap<String, String>();
        cvMOProductInfoMap.put(UpgradeActivityConstants.PRODUCT_NUMBER, "AIHPCCXC132471/44");
        cvMOProductInfoMap.put(UpgradeActivityConstants.PRODUCT_REVISION, "Drev1/23");
        cvMOAttributesMap.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, cvMOProductInfoMap);
        cvManagedObject = new AbstractManagedObject(cvMOAttributesMap, CONFIGURATION_VERSION_FDN) {
            @Override
            public PersistenceObject getTarget() {
                return null;
            }

            @Override
            public void setTarget(final PersistenceObject arg0) {

            }

            @Override
            public String getFdn() {
                return fdn;
            }

            @Override
            public Map<String, Object> getAllAttributes() {
                return attributes;
            }

            @Override
            public boolean isMibRoot() {
                return false;
            }

            @Override
            public int getAssociatedObjectCount(final String arg0) throws NotDefinedInModelException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public Map<String, Object> readAttributesFromDelegate(final String... arg0) throws DpsIllegalStateException {
                // TODO Auto-generated method stub
                return null;
            }
        };

        upManagedObject = new AbstractManagedObject(null, null) {

            @Override
            public String getFdn() {
                return null;
            }

            @Override
            public Map<String, Object> getAllAttributes() {
                return null;
            }

            @Override
            public boolean isMibRoot() {
                return false;
            }

            @Override
            public PersistenceObject getTarget() {
                return null;
            }

            @Override
            public void setTarget(final PersistenceObject arg0) {

            }

            @Override
            public int getAssociatedObjectCount(final String arg0) throws NotDefinedInModelException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public Map<String, Object> readAttributesFromDelegate(final String... arg0) throws DpsIllegalStateException {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }
}