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
package com.ericsson.oss.services.shm.es.impl.license;

import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_DATA_PO;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_DATA_PO_NAMESPACE;
import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.LICENSE_MO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.NodeModelNameSpaceProvider;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.job.utils.NodeAttributesReader;
import com.ericsson.oss.services.shm.common.license.InstallLicenseService;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@RunWith(MockitoJUnitRunner.class)
public class LicensingServiceTest {

    @Mock
    @Inject
    DpsReader dpsReader;

    @Mock
    @Inject
    DpsWriter dpsWriter;

    @Mock
    @Inject
    ActivityUtils activityUtils;

    @InjectMocks
    LicensingService objectUnderTest;

    @Mock
    PersistenceObject licensePo;

    @Mock
    ManagedObject licenseMo;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private NodeModelNameSpaceProvider nodeModelNameSpaceProviderMock;

    @Mock
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxyMock;

    @Mock
    private NodeAttributesReader nodeAttributesReaderMock;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private InstallLicenseService installLicenseService;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    PersistenceObject neJobPO;
    @Mock
    @Inject
    DpsReader dpsReaderMock;
    @Mock
    private JobConfigurationService jobConfigurationService;
    final String fdn = "Some FDN";
    final Map<String, Object> licenseMoAttributes = new HashMap<String, Object>();
    final Map<String, Object> licenseMoWithFDN = new HashMap<String, Object>();
    private static final String ERBS_NODE_MODEL_NAMESPACE = "ERBS_NODE_MODEL";
    public static final String LICENSE_FILE_PATH = "LICENSE_FILEPATH";
    public static final String ACTUAL_LKF_PATH = "/home/smrs/path";
    final long activityJobId = 281474976740953L;
    final long neJobId = 281475014606261L;
    final long mainJobId = 281475014606250L;
    Map<String, Object> neAttributes;
    @Test
    public void testGetAttributesListOfLicensePOs() {
        final Map<String, Object> restrictions = new HashMap<String, Object>();

        final List<PersistenceObject> licensePoList = new ArrayList<PersistenceObject>();
        final Map<String, Object> licensePoAttributes = new HashMap<String, Object>();
        when(licensePo.getAllAttributes()).thenReturn(licensePoAttributes);
        licensePoList.add(licensePo);
        when(dpsReader.findPOs(LICENSE_DATA_PO_NAMESPACE, LICENSE_DATA_PO, restrictions)).thenReturn(licensePoList);

        final List<Map<String, Object>> licensePoAttributeList = new ArrayList<Map<String, Object>>();
        licensePoAttributeList.add(licensePoAttributes);
        assertEquals(licensePoAttributeList, objectUnderTest.getAttributesListOfLicensePOs(restrictions));
    }

    @Test
    public void testUpdateLicenseInstalledTime() {
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();

        final List<PersistenceObject> licensePoList = new ArrayList<PersistenceObject>();
        final Map<String, Object> licensePoAttributes = new HashMap<String, Object>();
        when(licensePo.getAllAttributes()).thenReturn(licensePoAttributes);
        licensePoList.add(licensePo);
        when(dpsReader.findPOs(LICENSE_DATA_PO_NAMESPACE, LICENSE_DATA_PO, restrictionAttributes)).thenReturn(licensePoList);

        final long poId = 123;
        final Map<String, Object> changedAttributes = new HashMap<String, Object>();
        Mockito.doNothing().when(dpsWriter).update(poId, changedAttributes);

        assertTrue(objectUnderTest.updateLicenseInstalledTime(restrictionAttributes));
    }

    @Test
    public void testUpdateLicenseInstalledTimeHavingNoLicensePo() {
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();

        final List<PersistenceObject> licensePoList = new ArrayList<PersistenceObject>();
        when(dpsReader.findPOs(LICENSE_DATA_PO_NAMESPACE, LICENSE_DATA_PO, restrictionAttributes)).thenReturn(licensePoList);

        assertFalse(objectUnderTest.updateLicenseInstalledTime(restrictionAttributes));
    }

    @Test
    public void testGetLicenseMoFdn() {
        final long activityJobId = 123;
        getLicenseMo(activityJobId);
        assertEquals(fdn, objectUnderTest.getLicenseMoFdn(activityJobId));
    }

    @Test
    public void testGetLicenseMoFdnWithNoLicenseMo() {
        final long activityJobId = 123;
        getLicenseMoAsNull(activityJobId);
        assertNull(objectUnderTest.getLicenseMoFdn(activityJobId));
    }

    @Test
    public void testGetLicenseMoAttributes() {
        final long activityJobId = 123;
        getLicenseMo(activityJobId);
        assertEquals(licenseMoWithFDN, objectUnderTest.getLicenseMoAttributes(activityJobId));
    }

    @Test
    public void testGetLicenseMoAttributesWithNoLicenseMo() {
        final long activityJobId = 123;
        getLicenseMoAsNull(activityJobId);
        assertNull(objectUnderTest.getLicenseMoAttributes(activityJobId));
    }

    @Test
    public void testGetLicenseKeyFilePathFromNeJob() {
        final Map<String, Object> neJobProperties = new HashMap<String, Object>();
        neJobProperties.put(ShmJobConstants.JOBPROPERTIES, getNeJobPO(neJobId, mainJobId));
        when(jobConfigurationService.retrieveJob(Matchers.anyLong())).thenReturn(neJobProperties);
        Assert.assertEquals(ACTUAL_LKF_PATH, objectUnderTest.getLicenseKeyFilePathFromNeJob(neJobId));
    }

    private List<Map<String, Object>> getNeJobPO(final long neJobId, final long mainJobId) {
        neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.NE_NAME, "ERBS00006");
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        final Map<String, Object> nePoAttrMap = new HashMap<String, Object>();
        final List<Map<String, Object>> nePropertyList = new ArrayList<Map<String, Object>>();

        final Map<String, Object> lkfJobProperty = new HashMap<String, Object>();
        lkfJobProperty.put(ShmConstants.KEY, LicensingActivityConstants.LICENSE_FILE_PATH);
        lkfJobProperty.put(ShmConstants.VALUE, ACTUAL_LKF_PATH);
        nePropertyList.add(lkfJobProperty);
        nePoAttrMap.put(ShmConstants.JOBPROPERTIES, nePropertyList);
        neAttributes.put(ShmConstants.JOBPROPERTIES, nePropertyList);
        nePropertyList.add(neAttributes);
        when(neJobPO.getAllAttributes()).thenReturn(neAttributes);
        when(dpsReaderMock.findPOByPoId(neJobId)).thenReturn(neJobPO);
        return nePropertyList;
    }

    private void getLicenseMo(final long activityJobId) {
        final String nodeName = "Some Node Name";
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        when(nodeModelNameSpaceProviderMock.getNamespaceByNodeName(nodeName)).thenReturn(ERBS_NODE_MODEL_NAMESPACE);

        final Map<String, Object> restrictions = new HashMap<String, Object>();
        final List<ManagedObject> managedObjectsList = new ArrayList<ManagedObject>();
        when(licenseMo.getFdn()).thenReturn(fdn);

        when(licenseMo.getAllAttributes()).thenReturn(licenseMoAttributes);

        licenseMoWithFDN.put(ShmConstants.MO_ATTRIBUTES, licenseMoAttributes);
        licenseMoWithFDN.put(ShmConstants.FDN, fdn);
        managedObjectsList.add(licenseMo);
        when(dpsReader.getManagedObjects(ERBS_NODE_MODEL_NAMESPACE, LICENSE_MO, restrictions, nodeName)).thenReturn(managedObjectsList);
    }

    private void getLicenseMoAsNull(final long activityJobId) {
        final String nodeName = "Some Node Name";
        when(activityUtils.getNodeName(activityJobId)).thenReturn(nodeName);
        when(nodeModelNameSpaceProviderMock.getNamespaceByNodeName(nodeName)).thenReturn(ERBS_NODE_MODEL_NAMESPACE);

        final Map<String, Object> restrictions = new HashMap<String, Object>();
        final List<ManagedObject> managedObjectsList = new ArrayList<ManagedObject>();
        when(dpsReader.getManagedObjects(ERBS_NODE_MODEL_NAMESPACE, LICENSE_MO, restrictions, nodeName)).thenReturn(managedObjectsList);
    }

    @Test
    public void testGetRestrictedParameters() {
        final long mainJobId = 281475014606250L;
        final String platform = "CPP";
        final String neName = "ERBS00006";
        final String fingerprint = "fingerprint";
        final String licensefilepath = "somelicensepath";
        final String neType = "ERBS";

        Map<String, Object> mainJobAttributes = new HashMap<>();
        Map<String, Object> jobConfigurationDetails = new HashMap<>();
        final List<Map<String, Object>> nePropertyList = new ArrayList<>();

        Map<String, Object> neJobAttributes = new HashMap<>();
        neJobAttributes.put(ShmConstants.NE_NAME, "ERBS00006");
        neJobAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        List<Map<String, String>> jobPropertyList = new ArrayList<>();
        neJobAttributes.put(ShmConstants.JOBPROPERTIES, jobPropertyList);
        nePropertyList.add(neJobAttributes);

        jobConfigurationDetails.put(ActivityConstants.NE_JOB_PROPERTIES, nePropertyList);
        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);

        when(neJobStaticData.getNodeName()).thenReturn(neName);
        when(neJobStaticData.getPlatformType()).thenReturn(platform);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        List<String> keyList = new ArrayList<String>();
        Map<String, String> keyValueMap = new HashMap<>();
        keyValueMap.put(LicensingActivityConstants.LICENSE_FILE_PATH, null);
        when(jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platform)).thenReturn(keyValueMap);

        when(installLicenseService.generateLicenseKeyFilePath(fingerprint)).thenReturn(licensefilepath);
        Map<String, Object> jobConfigurationDetailsMap = new HashMap<>();
        jobConfigurationDetailsMap.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails);
        doNothing().when(jobUpdateService).updateJobAttributes(Matchers.anyLong(), Matchers.any());
        final String licensePathRetrieved = (String) objectUnderTest.getRestrictedParameters(neJobStaticData, neType, fingerprint, mainJobAttributes)
                .get(LicensingActivityConstants.LICENSE_DATA_LICENSE_KEYFILE_PATH);
        assertEquals(licensefilepath, licensePathRetrieved);
    }

}
