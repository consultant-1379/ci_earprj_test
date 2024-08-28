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
package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.xml.sax.SAXException;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.NodeModelNameSpaceProviderRetryProxy;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReaderRetryProxy;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.filestore.swpackage.remote.api.RemoteSoftwarePackageService;
import com.ericsson.oss.services.shm.job.cpp.activity.CppUpgradeActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class UpgradePackageServiceTest {

    @Mock
    @Inject
    ActivityUtils activityUtils;

    @Mock
    @Inject
    DpsReader dpsReader;

    @Mock
    @Inject
    DpsWriter dpsWriter;

    @Mock
    UpgradeControlFileParser upgradeControlFileParser;

    @Mock
    SmrsFileStoreService smrsServiceUtil;

    @Mock
    @Inject
    RemoteSoftwarePackageService remoteSoftwarePackageService;

    @InjectMocks
    UpgradePackageService objectUnderTest;

    @Mock
    ManagedObject upgradePackageParentMo;

    @Mock
    ManagedObject upMo;

    @Mock
    PersistenceObject upPo;

    @Mock
    Map<String, Object> mapMock;

    @Mock
    Map<String, Object> neJobAttributes;

    @Mock
    @Inject
    JobPropertyUtils jobPropertyUtils;

    @Mock
    private List<NetworkElement> neElementListMock;

    @Mock
    private NetworkElement neElementMock;

    @Mock
    private FdnServiceBeanRetryHelper fdnServiceBeanMock;

    @Mock
    private NodeModelNameSpaceProviderRetryProxy nodeModelNameSpaceProviderRetryProxyMock;

    @Mock
    private NodeAttributesReaderRetryProxy nodeAttributesReaderRetryProxyMock;

    private static final String ERBS_NODE_MODEL_NAMESPACE = "ERBS_NODE_MODEL";
    private static final String ipAddress = "ipAddress";
    private static final char[] password = { 'p', 'a', 's', 's', 'w', '0', 'r', 'd' };
    private static final String neType = "ERBS";
    private static final String nodeName = "Some node name";
    private static final String relativePath = "/rootsmrs/software/erbs/";
    private static final String smrsRootPath = "/home/smrs";
    private static final String ucfName = "CXP1020511_R4D73.xml";
    private static final String user = "userName";
    private static final String filePath = "src/test/";
    private static final String upPoHash = "resources";
    private static final String SMO_UPGRADE_CONTROL_FILE = "SMO_UPGRADE_CONTROL_FILE";
    private static final String PATH_ON_FTP_SERVER = filePath + "//" + upPoHash + "//" + ucfName;

    private void mockUpPOList(final String swPkgName) {
        final Map<String, Object> restrictionsOnPo = new HashMap<String, Object>();
        restrictionsOnPo.put(UpgradeActivityConstants.UP_PO_PACKAGE_NAME, swPkgName);
        final List<PersistenceObject> upPoList = new ArrayList<PersistenceObject>();
        final Map<String, Object> upPoMap = new HashMap<String, Object>();
        upPoMap.put(UpgradeActivityConstants.UP_PO_FILE_PATH, filePath);
        upPoMap.put(UpgradeActivityConstants.UP_PO_HASH, upPoHash);
        when(upPo.getAllAttributes()).thenReturn(upPoMap);
        upPoList.add(upPo);
        when(dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, UpgradeActivityConstants.UPPKG_TYPE_CPP, restrictionsOnPo)).thenReturn(upPoList);
    }

    private void mockSmrsDetails() {
        final SmrsAccountInfo smrsDetails = new SmrsAccountInfo();
        smrsDetails.setServerIpAddress(ipAddress);
        smrsDetails.setUser(user);
        smrsDetails.setPassword(password);
        smrsDetails.setRelativePathToSmrsRoot(relativePath);
        smrsDetails.setSmrsRootDirectory(smrsRootPath);
        when(smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.SOFTWARE_ACCOUNT, neType, nodeName)).thenReturn(smrsDetails);
    }

    private void mockJobParams() {
        final List<Map<String, Object>> jobParameters = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobParam = new HashMap<String, Object>();
        jobParam.put(UpgradeActivityConstants.UP_PO_PARAM_NAME,SMO_UPGRADE_CONTROL_FILE);
        jobParam.put(UpgradeActivityConstants.UP_PO_VALUE, ucfName);
        jobParameters.add(jobParam);
        when(upPo.getAttribute(UpgradeActivityConstants.UP_PO_JOBPARAMS)).thenReturn(jobParameters);
    }

    private void mockJobProperties() {
        final List<Map<String, Object>> jobPropertiesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UCF);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, ucfName);
        jobPropertiesList.add(jobProperty);
    }

    private String getProductNumber() {
        final String productNumber = "CXP102051/1";
        when(upgradeControlFileParser.getProductNumber()).thenReturn(productNumber);
        return productNumber;
    }

    private String getProductRevision() {
        final String productRevision = "R4D73";
        when(upgradeControlFileParser.getProductRevision()).thenReturn(productRevision);
        return productRevision;
    }

    private void setDataForCreateUpMo() throws IOException, SAXException {
        final long activityJobId = 1;
        final String swPkgName = "Some Sw Pkg Name";
        final String upParentMoFdn = "Parent MO Fdn";
        final String upParentMoVersion = "Parent MO Version";

        final List<String> availableUcfFiles = new ArrayList<>();
        final Map<String, String> installServiceMap = new HashMap<>();
        final Map<String, Object> jobConfigurationDetails = new HashMap<>();
        final List<String> keyList = new ArrayList<>();
        final List<String> keyList1 = new ArrayList<>();
        final List<ManagedObject> managedObjectList = new ArrayList<>();
        final Map<String, Object> restrictions = new HashMap<>();

        when(nodeModelNameSpaceProviderRetryProxyMock.getNamespaceByNodeName(nodeName)).thenReturn(ERBS_NODE_MODEL_NAMESPACE);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(jobConfigurationDetails);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        managedObjectList.add(upgradePackageParentMo);
        when(dpsReader.getManagedObjects(ERBS_NODE_MODEL_NAMESPACE, ShmConstants.UP_PARENT_MO_TYPE, restrictions, nodeName)).thenReturn(managedObjectList);
        when(upgradePackageParentMo.getFdn()).thenReturn(upParentMoFdn);
        when(upgradePackageParentMo.getVersion()).thenReturn(upParentMoVersion);

        keyList.add(UpgradeActivityConstants.SWP_NAME);

        installServiceMap.put(UpgradeActivityConstants.SWP_NAME, swPkgName);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(installServiceMap);

        mockJobProperties();

        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);

        keyList.add(UpgradeActivityConstants.UCF);
        final Map<String, String> installServiceMap1 = new HashMap<>();
        installServiceMap.put(UpgradeActivityConstants.UCF, ucfName);
        when(jobPropertyUtils.getPropertyValue(keyList1, mapMock, nodeName, neType, "CPP")).thenReturn(installServiceMap1);

        mockUpPOList(swPkgName);

        availableUcfFiles.add(PATH_ON_FTP_SERVER);
        when(remoteSoftwarePackageService.getSoftwarePackageDetails(swPkgName)).thenReturn(availableUcfFiles);

        Mockito.doNothing().when(upgradeControlFileParser).parse(PATH_ON_FTP_SERVER);

        getProductNumber();
        getProductRevision();
        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(neJobAttributes);
        when(neJobAttributes.get(ShmConstants.NETYPE)).thenReturn(neType);

        mockSmrsDetails();
    }

    private Map<String, Object> getUpMoAttributes() {
        final Map<String, Object> upgradePkgMOAttributes = new HashMap<String, Object>();

        upgradePkgMOAttributes.put(UpgradePackageMoConstants.UP_MO_UPGRADE_PACKAGE_ID, "CXP102051/1_R4D73");
        upgradePkgMOAttributes.put(UpgradePackageMoConstants.UP_MO_USER_LABEL, "SHM_1498652404149");
        upgradePkgMOAttributes.put(UpgradePackageMoConstants.UP_MO_UP_FILEPATH_ON_FTP_SERVER, "");
        upgradePkgMOAttributes.put(UpgradePackageMoConstants.UP_MO_FTP_SERVER_IP_ADDRESS, "ipAddress");
        upgradePkgMOAttributes.put(UpgradePackageMoConstants.UP_MO_USER, "userName");
        upgradePkgMOAttributes.put(UpgradePackageMoConstants.UP_MO_PASSWORD, "passw0rd");
        return upgradePkgMOAttributes;
    }

    @Test
    public void test_While_Creating_UPMO_ProductNumber_As_replaced_Slash_With_Underscore() throws IOException, SAXException {
        final long activityJobId = 1;
        final String upgradePackageId = "CXP102051/1_R4D73";
        final String upMoFdn = "Some Fdn";
        setDataForCreateUpMo();

        final Map<String, Object> upgradePkgMOAttributes = getUpMoAttributes();
        upgradePkgMOAttributes.put(UpgradePackageMoConstants.UP_MO_UPGRADE_PACKAGE_ID, "CXP102051_1_R4D73");
        when(dpsWriter.createManagedObject(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(upMo);
        when(upMo.getFdn()).thenReturn(upMoFdn);
        when(upMo.getAttribute(UpgradePackageMoConstants.UP_MO_UPGRADE_PACKAGE_ID)).thenReturn(upgradePkgMOAttributes.get("UpgradePackageId"));
        objectUnderTest.createUpgradeMO(activityJobId);
        assertNotEquals(upgradePackageId, upMo.getAttribute(UpgradePackageMoConstants.UP_MO_UPGRADE_PACKAGE_ID));
    }

    @Test
    public void test_While_Creating_UPMO_ProductNumber_Not_replaced_Slash_With_Underscore() throws IOException, SAXException {
        final long activityJobId = 1;
        final String upgradePackageId = "CXP102051/1_R4D73";
        final String upMoFdn = "Some Fdn";
        setDataForCreateUpMo();

        final Map<String, Object> upgradePkgMOAttributes = getUpMoAttributes();
        when(dpsWriter.createManagedObject(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(upMo);
        when(upMo.getFdn()).thenReturn(upMoFdn);
        when(upMo.getAttribute(UpgradePackageMoConstants.UP_MO_UPGRADE_PACKAGE_ID)).thenReturn(upgradePkgMOAttributes.get("UpgradePackageId"));
        objectUnderTest.createUpgradeMO(activityJobId);
        assertTrue(upgradePackageId.equals(upMo.getAttribute(UpgradePackageMoConstants.UP_MO_UPGRADE_PACKAGE_ID)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateUpgradeMO() throws IOException, SAXException {
        final long activityJobId = 1;
        setDataForCreateUpMo();
        getUpMoAttributes();
        when(dpsWriter.createManagedObject(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(upMo);
        final String upMoFdn = "Some Fdn";
        when(upMo.getFdn()).thenReturn(upMoFdn);

        assertEquals(upMoFdn, objectUnderTest.createUpgradeMO(activityJobId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateUpgradeMOWithUcfNameAsNull() throws IOException, SAXException {
        final long activityJobId = 1;

        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);

        final Map<String, Object> restrictions = new HashMap<String, Object>();
        final List<ManagedObject> managedObjectList = new ArrayList<ManagedObject>();
        managedObjectList.add(upgradePackageParentMo);
        when(dpsReader.getManagedObjects(ERBS_NODE_MODEL_NAMESPACE, ShmConstants.UP_PARENT_MO_TYPE, restrictions, nodeName)).thenReturn(managedObjectList);

        final String upParentMoFdn = "Parent MO Fdn";
        when(upgradePackageParentMo.getFdn()).thenReturn(upParentMoFdn);

        final String upParentMoVersion = "Parent MO Version";
        when(upgradePackageParentMo.getVersion()).thenReturn(upParentMoVersion);

        final String swPkgName = "Some Sw Pkg Name";
        final List<String> keyList = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.SWP_NAME);
        final Map<String, String> installServiceMap = new HashMap<String, String>();
        installServiceMap.put(UpgradeActivityConstants.SWP_NAME, swPkgName);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(installServiceMap);

        final List<Map<String, Object>> jobPropertiesList = new ArrayList<Map<String, Object>>();
        when(activityUtils.getMainJobPropertyList(activityJobId)).thenReturn(jobPropertiesList);

        mockUpPOList(swPkgName);

        Mockito.doNothing().when(upgradeControlFileParser).parse("src/test/resources/CXP1020511_R4D73.xml");

        boolean exceptionCame = false;
        try {
            objectUnderTest.createUpgradeMO(activityJobId);

        } catch (final Exception ex) {
            exceptionCame = true;
        }

        assertTrue(exceptionCame);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateUpgradeMOWithNoUpPo() throws IOException, SAXException {
        final long activityJobId = 1;

        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);

        final Map<String, Object> restrictions = new HashMap<String, Object>();
        final List<ManagedObject> managedObjectList = new ArrayList<ManagedObject>();
        managedObjectList.add(upgradePackageParentMo);
        when(dpsReader.getManagedObjects(ERBS_NODE_MODEL_NAMESPACE, ShmConstants.UP_PARENT_MO_TYPE, restrictions, nodeName)).thenReturn(managedObjectList);

        final String upParentMoFdn = "Parent MO Fdn";
        when(upgradePackageParentMo.getFdn()).thenReturn(upParentMoFdn);

        final String upParentMoVersion = "Parent MO Version";
        when(upgradePackageParentMo.getVersion()).thenReturn(upParentMoVersion);

        final String swPkgName = "Some Sw Pkg Name";
        final List<String> keyList = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.SWP_NAME);
        final Map<String, String> installServiceMap = new HashMap<String, String>();
        installServiceMap.put(UpgradeActivityConstants.SWP_NAME, swPkgName);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(installServiceMap);

        final List<Map<String, Object>> jobPropertiesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobProperty = new HashMap<String, Object>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, UpgradeActivityConstants.UCF);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "CXP1020511_R4D73.xml");
        jobPropertiesList.add(jobProperty);
        when(activityUtils.getMainJobPropertyList(activityJobId)).thenReturn(jobPropertiesList);

        final Map<String, Object> restrictionsOnPo = new HashMap<String, Object>();
        restrictionsOnPo.put(UpgradeActivityConstants.UP_PO_PACKAGE_NAME, swPkgName);
        final List<PersistenceObject> upPoList = new ArrayList<PersistenceObject>();
        when(dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, UpgradeActivityConstants.UPPKG_TYPE_CPP, restrictionsOnPo)).thenReturn(upPoList);

        Mockito.doNothing().when(upgradeControlFileParser).parse("src/test/resources/CXP1020511_R4D73.xml");

        boolean exceptionCame = false;
        try {
            objectUnderTest.createUpgradeMO(activityJobId);

        } catch (final Exception ex) {
            exceptionCame = true;
        }

        assertTrue(exceptionCame);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUpMoData() throws IOException, SAXException, NodeAttributesReaderException {
        final long activityJobId = 1;
        final String swPkgName = "Some Sw Pkg Name";

        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        final List<String> keyList = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.SWP_NAME);
        final Map<String, String> installServiceMap = new HashMap<String, String>();
        installServiceMap.put(UpgradeActivityConstants.SWP_NAME, swPkgName);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(installServiceMap);

        mockJobProperties();

        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        final List<String> keyList1 = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.UCF);
        final Map<String, String> installServiceMap1 = new HashMap<String, String>();
        installServiceMap.put(UpgradeActivityConstants.UCF, ucfName);
        when(jobPropertyUtils.getPropertyValue(keyList1, mapMock, nodeName, "ERBS", "CPP")).thenReturn(installServiceMap1);

        mockUpPOList(swPkgName);

        final List<String> availableUcfFiles = new ArrayList<String>();
        availableUcfFiles.add(PATH_ON_FTP_SERVER);
        when(remoteSoftwarePackageService.getSoftwarePackageDetails(swPkgName)).thenReturn(availableUcfFiles);

        Mockito.doNothing().when(upgradeControlFileParser).parse(PATH_ON_FTP_SERVER);

        final String productNumber = getProductNumber();
        final String productRevision = getProductRevision();

        final Map<String, Object> neJobAttributesMap = new HashMap<String, Object>();
        neJobAttributesMap.put(ShmConstants.NE_NAME, "Some Ne Name");
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neJobAttributesMap);

        final Map<String, Object> restrictions = new HashMap<String, Object>();
        restrictions.put(UpgradePackageMoConstants.UP_MO_UPGRADE_PACKAGE_ID, "CXP102051/1_R4D73");
        final List<ManagedObject> managedObjectList = new ArrayList<ManagedObject>();
        final Map<String, Object> productData = new HashMap<String, Object>();
        productData.put(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_NUMBER, productNumber);
        productData.put(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_REVISION, productRevision);
        final Map<String, Object> upMoAttributes = new HashMap<String, Object>();
        upMoAttributes.put(UpgradePackageMoConstants.UP_MO_STATE, "NOT_INSTALLED");
        upMoAttributes.put(UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA, productData);

        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };
        when(nodeAttributesReaderRetryProxyMock.readAttributesWithRetry(upMo, attributeNames)).thenReturn(upMoAttributes);

        final Map<String, Object> administrativeData = new HashMap<String, Object>();
        administrativeData.put(UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA, productData);

        final String[] attributeNames1 = { UpgradePackageMoConstants.UP_MO_STATE, UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA };
        when(nodeAttributesReaderRetryProxyMock.readAttributesWithRetry(upMo, attributeNames1)).thenReturn(upMoAttributes);
        managedObjectList.add(upMo);
        when(dpsReader.getManagedObjects(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyString())).thenReturn(managedObjectList);
        when(upMo.getFdn()).thenReturn("MoFdn");
        assertEquals(upMoAttributes, objectUnderTest.getUpMoData(activityJobId, attributeNames, null, null));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUpMoDataAsEmpty() throws IOException, SAXException {
        final long activityJobId = 1;
        final String[] attributeNames = {};
        final String swPkgName = "Some Sw Pkg Name";

        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        final List<String> keyList = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.SWP_NAME);
        final Map<String, String> installServiceMap = new HashMap<String, String>();
        installServiceMap.put(UpgradeActivityConstants.SWP_NAME, swPkgName);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(installServiceMap);

        mockJobProperties();

        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        final List<String> keyList1 = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.UCF);
        final Map<String, String> installServiceMap1 = new HashMap<String, String>();
        installServiceMap.put(UpgradeActivityConstants.UCF, ucfName);
        when(jobPropertyUtils.getPropertyValue(keyList1, mapMock, nodeName, "ERBS", "CPP")).thenReturn(installServiceMap1);

        mockUpPOList(swPkgName);

        final List<String> availableUcfFiles = new ArrayList<String>();
        availableUcfFiles.add(PATH_ON_FTP_SERVER);
        when(remoteSoftwarePackageService.getSoftwarePackageDetails(swPkgName)).thenReturn(availableUcfFiles);

        Mockito.doNothing().when(upgradeControlFileParser).parse(PATH_ON_FTP_SERVER);

        getProductNumber();
        getProductRevision();

        final Map<String, Object> neJobAttributesMap = new HashMap<String, Object>();
        neJobAttributesMap.put(ShmConstants.NE_NAME, "Some Ne Name");
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neJobAttributesMap);

        final Map<String, Object> restrictions = new HashMap<String, Object>();
        restrictions.put(UpgradePackageMoConstants.UP_MO_UPGRADE_PACKAGE_ID, "CXP102051/1_R4D73");
        final List<ManagedObject> managedObjectList = new ArrayList<ManagedObject>();
        when(dpsReader.getManagedObjects(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyString())).thenReturn(managedObjectList);

        assertEquals(new HashMap<String, Object>(), objectUnderTest.getUpMoData(activityJobId, attributeNames, null, null));
        assertEquals(0, (objectUnderTest.getUpMoData(activityJobId, attributeNames, null, null)).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUpMoFdn() throws IOException, SAXException, NodeAttributesReaderException {
        final long activityJobId = 1;

        final String swPkgName = "Some Sw Pkg Name";
        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        final List<String> keyList = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.SWP_NAME);
        final Map<String, String> installServiceMap = new HashMap<String, String>();
        installServiceMap.put(UpgradeActivityConstants.SWP_NAME, swPkgName);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(installServiceMap);

        mockJobProperties();

        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        final List<String> keyList1 = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.UCF);
        final Map<String, String> installServiceMap1 = new HashMap<String, String>();
        installServiceMap.put(UpgradeActivityConstants.UCF, ucfName);
        when(jobPropertyUtils.getPropertyValue(keyList1, mapMock, nodeName, "ERBS", "CPP")).thenReturn(installServiceMap1);

        mockUpPOList(swPkgName);
        final List<String> availableUcfFiles = new ArrayList<String>();
        availableUcfFiles.add(PATH_ON_FTP_SERVER);
        when(remoteSoftwarePackageService.getSoftwarePackageDetails(swPkgName)).thenReturn(availableUcfFiles);

        Mockito.doNothing().when(upgradeControlFileParser).parse(PATH_ON_FTP_SERVER);

        final String productNumber = getProductNumber();
        final String productRevision = getProductRevision();

        final Map<String, Object> neJobAttributesMap = new HashMap<String, Object>();
        neJobAttributesMap.put(ShmConstants.NE_NAME, "Some Ne Name");
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neJobAttributesMap);

        final List<ManagedObject> managedObjectList = new ArrayList<ManagedObject>();
        final String upMoFdn = "Some FDN";
        when(upMo.getFdn()).thenReturn(upMoFdn);
        final Map<String, Object> administrativeData = new HashMap<String, Object>();
        final Map<String, Object> productData = new HashMap<String, Object>();
        productData.put(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_NUMBER, productNumber);
        productData.put(UpgradePackageMoConstants.UP_MO_ADMIN_DATA_PRODUCT_REVISION, productRevision);
        administrativeData.put(UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA, productData);
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_ADMINISTRATIVE_DATA };
        when(nodeAttributesReaderRetryProxyMock.readAttributesWithRetry(upMo, attributeNames)).thenReturn(administrativeData);
        managedObjectList.add(upMo);
        when(dpsReader.getManagedObjects(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyString())).thenReturn(managedObjectList);

        assertEquals(upMoFdn, objectUnderTest.getUpMoFdn(activityJobId, null, null));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUpMoFdnAsNull() throws IOException, SAXException {
        final long activityJobId = 1;
        final String swPkgName = "Some Sw Pkg Name";

        final List<String> availableUcfFiles = new ArrayList<>();
        final List<ManagedObject> managedObjectList = new ArrayList<>();
        final Map<String, Object> neJobAttributesMap = new HashMap<>();
        final Map<String, Object> restrictions = new HashMap<>();

        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        final Map<String, String> installServiceMap = new HashMap<>();
        installServiceMap.put(UpgradeActivityConstants.SWP_NAME, swPkgName);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(installServiceMap);

        mockJobProperties();

        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        final List<String> keyList1 = new ArrayList<>();
        final Map<String, String> installServiceMap1 = new HashMap<String, String>();
        installServiceMap.put(UpgradeActivityConstants.UCF, ucfName);
        when(jobPropertyUtils.getPropertyValue(keyList1, mapMock, nodeName, "ERBS", "CPP")).thenReturn(installServiceMap1);

        mockUpPOList(swPkgName);

        availableUcfFiles.add(PATH_ON_FTP_SERVER);
        when(remoteSoftwarePackageService.getSoftwarePackageDetails(swPkgName)).thenReturn(availableUcfFiles);

        Mockito.doNothing().when(upgradeControlFileParser).parse(PATH_ON_FTP_SERVER);

        getProductNumber();
        getProductRevision();

        neJobAttributesMap.put(ShmConstants.NE_NAME, "Some Ne Name");
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neJobAttributesMap);
        when(nodeModelNameSpaceProviderRetryProxyMock.getNamespaceByNodeName(nodeName)).thenReturn(ERBS_NODE_MODEL_NAMESPACE);

        restrictions.put(UpgradePackageMoConstants.UP_MO_UPGRADE_PACKAGE_ID, "CXP102051/1_R4D73");
        when(dpsReader.getManagedObjects(Matchers.anyString(), Matchers.anyString(), Matchers.anyMap(), Matchers.anyString())).thenReturn(managedObjectList);

        assertNull(objectUnderTest.getUpMoFdn(activityJobId, null, null));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUpPoData() {
        final long activityJobId = 1;
        final String swPkgName = "Some Sw Pkg Name";

        final List<String> keyList = new ArrayList<String>();
        final Map<String, Object> restrictionsOnPo = new HashMap<String, Object>();
        final List<PersistenceObject> upPoList = new ArrayList<PersistenceObject>();
        final Map<String, Object> upPoMap = new HashMap<String, Object>();

        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);

        keyList.add(UpgradeActivityConstants.SWP_NAME);
        final Map<String, String> installServiceMap = new HashMap<String, String>();
        installServiceMap.put(UpgradeActivityConstants.SWP_NAME, swPkgName);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(installServiceMap);

        restrictionsOnPo.put(UpgradeActivityConstants.UP_PO_PACKAGE_NAME, swPkgName);

        upPoMap.put(UpgradeActivityConstants.UP_PO_FILE_PATH, "src/test/");
        upPoMap.put(UpgradeActivityConstants.UP_PO_HASH, "resources");
        when(upPo.getAllAttributes()).thenReturn(upPoMap);
        upPoList.add(upPo);
        when(dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, UpgradeActivityConstants.UPPKG_TYPE_CPP, restrictionsOnPo)).thenReturn(upPoList);

        assertEquals(upPoMap, objectUnderTest.getUpPoData(activityJobId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateUpgradeMOForRestoreInstall() throws IOException, SAXException {
        final long activityJobId = 1;
        final String upMoFdn = "SomeFdn";

        final Map<String, Object> restrictions = new HashMap<>();
        final List<ManagedObject> managedObjectList = new ArrayList<>();
        final List<NetworkElement> networkElementList = new ArrayList<>();
        final String packageName = "CXP102051_1_R4D73";
        final String productNumber = getProductNumber();
        final String productRevision = getProductRevision();
        final List<PersistenceObject> upgradePackagePoList = new ArrayList<>();
        final String upParentMoFdn = "Parent MO Fdn";
        final String upParentMoVersion = "Parent MO Version";

        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        when(nodeModelNameSpaceProviderRetryProxyMock.getNamespaceByNodeName(nodeName)).thenReturn(ERBS_NODE_MODEL_NAMESPACE);

        networkElementList.add(neElementMock);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(networkElementList);
        when(neElementMock.getNeType()).thenReturn(neType);

        managedObjectList.add(upgradePackageParentMo);
        when(dpsReader.getManagedObjects(ERBS_NODE_MODEL_NAMESPACE, ShmConstants.UP_PARENT_MO_TYPE, restrictions, nodeName)).thenReturn(managedObjectList);

        when(upgradePackageParentMo.getFdn()).thenReturn(upParentMoFdn);

        when(upgradePackageParentMo.getVersion()).thenReturn(upParentMoVersion);
        Mockito.doNothing().when(upgradeControlFileParser).parse(PATH_ON_FTP_SERVER);

        upgradePackagePoList.add(upPo);
        when(dpsReader.findPOs(UpgradeActivityConstants.UPPKG_NAMESPACE, UpgradeActivityConstants.UPPKG_TYPE_CPP, restrictions)).thenReturn(upgradePackagePoList);

        when(upPo.getAttribute(UpgradeActivityConstants.UP_PO_PACKAGE_NAME)).thenReturn(packageName);

        mockJobParams();

        final List<String> availableUcfFiles = new ArrayList<>();
        availableUcfFiles.add("src/test/resources/CXP1020511_R4D73.xml");
        when(remoteSoftwarePackageService.getSoftwarePackageDetails(packageName)).thenReturn(availableUcfFiles);

        when(dpsWriter.createManagedObject(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(upMo);
        when(upMo.getFdn()).thenReturn(upMoFdn);

        when(activityUtils.getPoAttributes(activityJobId)).thenReturn(neJobAttributes);
        when(neJobAttributes.get(ShmConstants.NETYPE)).thenReturn(neType);

        mockSmrsDetails();

        assertEquals(upMoFdn, objectUnderTest.createUpgradeMO(activityJobId, productNumber, productRevision));
    }

    @Test(expected = RetriableCommandException.class)
    public void testGetUpMoDataWithNameSpaceNotFoundException() throws IOException, SAXException {
        final long activityJobId = 1;
        final String swPkgName = "Some Sw Pkg Name";

        when(activityUtils.getMainJobAttributes(activityJobId)).thenReturn(mapMock);
        when(mapMock.get(ShmConstants.JOBCONFIGURATIONDETAILS)).thenReturn(mapMock);
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Matchers.anyList())).thenReturn(neElementListMock);
        when(neElementListMock.get(0)).thenReturn(neElementMock);
        when(neElementMock.getNeType()).thenReturn("ERBS");
        when(neElementMock.getPlatformType()).thenReturn(PlatformTypeEnum.CPP);
        final List<String> keyList = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.SWP_NAME);
        final Map<String, String> installServiceMap = new HashMap<String, String>();
        installServiceMap.put(UpgradeActivityConstants.SWP_NAME, swPkgName);
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(installServiceMap);

        mockJobProperties();

        final List<String> keyList1 = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.UCF);
        final Map<String, String> installServiceMap1 = new HashMap<String, String>();
        installServiceMap.put(UpgradeActivityConstants.UCF, ucfName);
        when(jobPropertyUtils.getPropertyValue(keyList1, mapMock, nodeName, "ERBS", "CPP")).thenReturn(installServiceMap1);

        final List<String> availableUcfFiles = new ArrayList<String>();
        availableUcfFiles.add(PATH_ON_FTP_SERVER);
        when(remoteSoftwarePackageService.getSoftwarePackageDetails(swPkgName)).thenReturn(availableUcfFiles);

        Mockito.doNothing().when(upgradeControlFileParser).parse(PATH_ON_FTP_SERVER);
        final String productNumber = getProductNumber();
        final String productRevision = getProductRevision();
        final Map<String, Object> neJobAttributesMap = new HashMap<String, Object>();
        neJobAttributesMap.put(ShmConstants.NE_NAME, "Some Ne Name");
        when(activityUtils.getNeJobAttributes(activityJobId)).thenReturn(neJobAttributesMap);
        final String[] attributeNames = { UpgradePackageMoConstants.UP_MO_STATE };
        doThrow(RetriableCommandException.class).when(nodeModelNameSpaceProviderRetryProxyMock).getNamespaceByNodeName(Matchers.any());
        objectUnderTest.getUpMoData(activityJobId, attributeNames, null, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateUpgradeMOForRestoreInstallThrowsException() throws IOException, SAXException {
        final long activityJobId = 1;
        String exceptionMessage = null;
        boolean exceptionCame = false;
        final String filePath = "src/test/resources";
        final String upMoFdn = "SomeFdn";
        final String upParentMoFdn = "Parent MO Fdn";

        final List<ManagedObject> managedObjectList = new ArrayList<>();
        final List<NetworkElement> networkElementList = new ArrayList<>();
        final String productNumber = getProductNumber();
        final String productRevision = getProductRevision();
        final Map<String, Object> restrictions = new HashMap<>();
        final String upParentMoVersion = "Parent MO Version";

        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        when(nodeModelNameSpaceProviderRetryProxyMock.getNamespaceByNodeName(nodeName)).thenReturn(ERBS_NODE_MODEL_NAMESPACE);

        networkElementList.add(neElementMock);
        when(fdnServiceBeanMock.getNetworkElementsByNeNames(Arrays.asList(nodeName))).thenReturn(networkElementList);
        when(neElementMock.getNeType()).thenReturn(neType);

        managedObjectList.add(upgradePackageParentMo);
        when(dpsReader.getManagedObjects(ERBS_NODE_MODEL_NAMESPACE, ShmConstants.UP_PARENT_MO_TYPE, restrictions, nodeName)).thenReturn(managedObjectList);

        when(upgradePackageParentMo.getFdn()).thenReturn(upParentMoFdn);

        when(upgradePackageParentMo.getVersion()).thenReturn(upParentMoVersion);
        Mockito.doNothing().when(upgradeControlFileParser).parse(PATH_ON_FTP_SERVER);

        when(upPo.getAttribute(UpgradeActivityConstants.UP_PO_FILE_PATH)).thenReturn(filePath);

        mockJobParams();

        when(dpsWriter.createManagedObject(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyMap())).thenReturn(upMo);
        when(upMo.getFdn()).thenReturn(upMoFdn);

        try {
            objectUnderTest.createUpgradeMO(activityJobId, productNumber, productRevision);
        } catch (final IllegalArgumentException exception) {
            exceptionCame = true;
            exceptionMessage = exception.getMessage();
        }
        assertTrue(exceptionCame);
        assertEquals("Respective Upgrade Package does not exist for productNumber " + productNumber + " and productRevision " + productRevision, exceptionMessage);
    }
}
