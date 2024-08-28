package com.ericsson.oss.services.shm.job.impl;

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
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.filestore.swpackage.api.ActivityDetails;
import com.ericsson.oss.services.shm.filestore.swpackage.api.SoftwarePackage;
import com.ericsson.oss.services.shm.filestore.swpackage.api.SoftwarePackageParameter;
import com.ericsson.oss.services.shm.job.activity.*;
import com.ericsson.oss.services.shm.job.cpp.activity.CppUpgradeActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.api.*;
import com.ericsson.oss.services.shm.jobs.common.constants.JobConfigurationConstants;
import com.ericsson.oss.services.shm.swpackage.query.api.SoftwarePackageQueryService;

@RunWith(MockitoJUnitRunner.class)
public class CppUpgradeJobActivitiesResponseModifierTest {

    @InjectMocks
    private CppUpgradeJobActivitiesResponseModifier objectUnderTest;

    @Mock
    private JobActivitiesResponse jobActivitiesResponseMock;

    @Mock
    private JobActivitiesQuery jobActivitiesQueryMock;

    @Mock
    private NeInfoQuery neInfoQueryMock;

    @Mock
    private NeActivityInformation neActivityInformationMock;

    @Mock
    private Activity activityMock;

    @Mock
    private ActivityParams activityParamsMock;

    @Mock
    private SoftwarePackageQueryService softwarePackageQueryServiceMock;

    @Mock
    private Map<String, SoftwarePackage> softwarePackageDataMock;

    @Mock
    private NeParams neParamsMock;

    @Mock
    private SoftwarePackage softwarePackageMock;

    @Mock
    private ActivityDetails softwarePackageActivityDetailsMock;

    @Mock
    private SoftwarePackageParameter softwarePackageParamMock;

    @Mock
    private SoftwarePackageParameter softwarePackageParamMock2;

    @Mock
    private ParamType paramTypeMock;

    @Mock
    private ParamType paramTypeMock1;

    @Test
    public void testgetUpdatedJobActivities_SelectType_InstallActivity() throws UnsupportedFragmentException {

        setUp();
        when(softwarePackageParamMock2.getName()).thenReturn(CppUpgradeActivityConstants.SMO_INSTALL_SELECT_TYPE);
        when(jobActivitiesResponseMock.getNeActivityInformation()).thenReturn(Arrays.asList(neActivityInformationMock));
        when(neActivityInformationMock.getActivity()).thenReturn(Arrays.asList(activityMock));
        when(neActivityInformationMock.getNeType()).thenReturn("ERBS");
        when(activityMock.getActivityParams()).thenReturn(Arrays.asList(activityParamsMock));
        when(activityParamsMock.getParam()).thenReturn(Arrays.asList(paramTypeMock));
        when(paramTypeMock.getName()).thenReturn(UpgradeActivityConstants.FORCEINSTALL);
        when(paramTypeMock.getName()).thenReturn(UpgradeActivityConstants.SELECTIVEINSTALL);
        when(paramTypeMock.getDefaultValue()).thenReturn(UpgradeActivityConstants.ACTION_SELECTIVE_INSTALL);
        when(activityMock.getName()).thenReturn(CppUpgradeActivityConstants.INSTALL);
        when(softwarePackageActivityDetailsMock.getName()).thenReturn(CppUpgradeActivityConstants.INSTALLATION);
        when(softwarePackageActivityDetailsMock.getActivityParams()).thenReturn(Arrays.asList(softwarePackageParamMock2));
        when(activityMock.getActivityParams().get(0).getParam()).thenReturn(Arrays.asList(paramTypeMock));
        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getNeType());
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getName());
        Assert.assertEquals(response.getNeActivityInformation().get(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getName(), UpgradeActivityConstants.SELECTIVEINSTALL);
        Assert.assertNotEquals(response.getNeActivityInformation().get(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getName(), UpgradeActivityConstants.FORCEINSTALL);
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getDefaultValue());
        Assert.assertEquals(jobActivitiesResponseMock, response);

    }

    @Test
    public void testgetUpdatedJobActivities_TransferType_InstallActivity() throws UnsupportedFragmentException {
        setUp();
        when(softwarePackageParamMock2.getName()).thenReturn(CppUpgradeActivityConstants.SMO_INSTALL_TRANSFER_TYPE);
        when(jobActivitiesResponseMock.getNeActivityInformation()).thenReturn(Arrays.asList(neActivityInformationMock));
        when(neActivityInformationMock.getActivity()).thenReturn(Arrays.asList(activityMock));
        when(neActivityInformationMock.getNeType()).thenReturn("ERBS");
        when(activityMock.getActivityParams()).thenReturn(Arrays.asList(activityParamsMock));
        when(activityParamsMock.getParam()).thenReturn(Arrays.asList(paramTypeMock));
        when(paramTypeMock.getName()).thenReturn(UpgradeActivityConstants.SELECTIVEINSTALL);
        when(paramTypeMock.getName()).thenReturn(UpgradeActivityConstants.FORCEINSTALL);
        when(paramTypeMock.getDefaultValue()).thenReturn(UpgradeActivityConstants.ACTION_FORCED_INSTALL);
        when(activityMock.getName()).thenReturn(CppUpgradeActivityConstants.INSTALL);
        when(softwarePackageActivityDetailsMock.getName()).thenReturn(CppUpgradeActivityConstants.INSTALLATION);
        when(softwarePackageActivityDetailsMock.getActivityParams()).thenReturn(Arrays.asList(softwarePackageParamMock2));
        when(activityMock.getActivityParams().get(0).getParam()).thenReturn(Arrays.asList(paramTypeMock));
        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getNeType());
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getName());
        Assert.assertEquals(response.getNeActivityInformation().get(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getName(), UpgradeActivityConstants.FORCEINSTALL);
        Assert.assertNotEquals(response.getNeActivityInformation().get(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getName(), UpgradeActivityConstants.SELECTIVEINSTALL);
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getDefaultValue());
        Assert.assertEquals(jobActivitiesResponseMock, response);
    }

    @Test
    public void testgetUpdatedJobActivities_Ucf_InstallActivity() throws UnsupportedFragmentException {

        setUp();
        when(softwarePackageParamMock2.getName()).thenReturn(CppUpgradeActivityConstants.SMO_UPGRADE_CONTROL_FILE);
        when(jobActivitiesResponseMock.getNeActivityInformation()).thenReturn(Arrays.asList(neActivityInformationMock));
        when(neActivityInformationMock.getActivity()).thenReturn(Arrays.asList(activityMock));
        when(neActivityInformationMock.getNeType()).thenReturn("ERBS");
        when(activityMock.getActivityParams()).thenReturn(Arrays.asList(activityParamsMock));
        when(activityParamsMock.getParam()).thenReturn(Arrays.asList(paramTypeMock));
        when(paramTypeMock.getName()).thenReturn(CppUpgradeActivityConstants.UCF);
        when(activityMock.getName()).thenReturn(CppUpgradeActivityConstants.INSTALL);
        when(softwarePackageActivityDetailsMock.getName()).thenReturn(CppUpgradeActivityConstants.INSTALLATION);
        when(softwarePackageActivityDetailsMock.getActivityParams()).thenReturn(Arrays.asList(softwarePackageParamMock));
        when(activityMock.getActivityParams().get(0).getParam()).thenReturn(Arrays.asList(paramTypeMock));
        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getNeType());
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getName());
        Assert.assertEquals(response.getNeActivityInformation().get(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getName(), CppUpgradeActivityConstants.UCF);
        Assert.assertEquals(jobActivitiesResponseMock, response);
    }

    @Test
    public void testgetUpdatedJobActivities_InstallActivity_AllParams() throws UnsupportedFragmentException {

        setUp();
        when(softwarePackageParamMock2.getName()).thenReturn(CppUpgradeActivityConstants.SMO_INSTALL_SELECT_TYPE);
        when(softwarePackageParamMock2.getName()).thenReturn(CppUpgradeActivityConstants.SMO_INSTALL_TRANSFER_TYPE);
        when(jobActivitiesResponseMock.getNeActivityInformation()).thenReturn(Arrays.asList(neActivityInformationMock));
        when(neActivityInformationMock.getActivity()).thenReturn(Arrays.asList(activityMock));
        when(neActivityInformationMock.getNeType()).thenReturn("ERBS");
        when(activityMock.getActivityParams()).thenReturn(Arrays.asList(activityParamsMock));
        when(activityParamsMock.getParam()).thenReturn(Arrays.asList(paramTypeMock));
        when(paramTypeMock.getName()).thenReturn(UpgradeActivityConstants.SELECTIVEINSTALL);
        when(paramTypeMock1.getName()).thenReturn(UpgradeActivityConstants.FORCEINSTALL);
        when(paramTypeMock1.getDefaultValue()).thenReturn(UpgradeActivityConstants.ACTION_SELECTIVE_INSTALL);
        when(activityMock.getName()).thenReturn(CppUpgradeActivityConstants.INSTALL);
        when(softwarePackageActivityDetailsMock.getName()).thenReturn(CppUpgradeActivityConstants.INSTALLATION);
        when(softwarePackageActivityDetailsMock.getActivityParams()).thenReturn(Arrays.asList(softwarePackageParamMock2));
        when(activityMock.getActivityParams().get(0).getParam()).thenReturn(Arrays.asList(paramTypeMock));
        when(activityMock.getActivityParams().get(0).getParam()).thenReturn(Arrays.asList(paramTypeMock1));
        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getNeType());
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getName());
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getName());
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getDefaultValue());
        Assert.assertEquals(jobActivitiesResponseMock, response);

    }

    @Test
    public void testgetUpdatedJobActivities_UpgradeActivity() throws UnsupportedFragmentException {
        final Map<String, SoftwarePackage> softwarePackageDataMap = new HashMap<String, SoftwarePackage>();
        softwarePackageDataMap.put("softwarePackage", softwarePackageMock);
        when(neInfoQueryMock.getParams()).thenReturn(Arrays.asList(neParamsMock));
        when(neInfoQueryMock.getNeType()).thenReturn("ERBS");
        when(neParamsMock.getName()).thenReturn(JobConfigurationConstants.SOFTWAREPKG_NAME);
        when(neParamsMock.getValue()).thenReturn("softwarePackage");
        when(softwarePackageQueryServiceMock.getSoftwarePackagesBasedOnPackageName(Matchers.anyMap())).thenReturn(softwarePackageDataMap);
        when(softwarePackageMock.getActivities()).thenReturn(Arrays.asList(softwarePackageActivityDetailsMock));
        when(jobActivitiesResponseMock.getNeActivityInformation()).thenReturn(Arrays.asList(neActivityInformationMock));
        when(neActivityInformationMock.getActivity()).thenReturn(Arrays.asList(activityMock));
        when(neActivityInformationMock.getNeType()).thenReturn("ERBS");
        when(activityMock.getActivityParams()).thenReturn(Arrays.asList(activityParamsMock));
        when(activityParamsMock.getParam()).thenReturn(Arrays.asList(paramTypeMock));
        when(paramTypeMock.getName()).thenReturn(UpgradeActivityConstants.REBOOTNODEUPGRADE);
        when(paramTypeMock.getDefaultValue()).thenReturn("True");
        when(activityMock.getName()).thenReturn(CppUpgradeActivityConstants.UPGRADE);
        when(softwarePackageActivityDetailsMock.getName()).thenReturn(CppUpgradeActivityConstants.UPGRADE_SMO);
        when(softwarePackageActivityDetailsMock.getActivityParams()).thenReturn(Arrays.asList(softwarePackageParamMock));
        when(softwarePackageParamMock.getName()).thenReturn("SMO_UPGRADE_REBOOT");
        when(softwarePackageParamMock.getValue()).thenReturn("False");
        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getNeType());
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getName());
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getName());
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getDefaultValue());
        Assert.assertEquals(jobActivitiesResponseMock, response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testgetUpdatedJobActivities_UpdateActivity() throws UnsupportedFragmentException {
        final Map<String, SoftwarePackage> softwarePackageDataMap = new HashMap<String, SoftwarePackage>();
        softwarePackageDataMap.put("softwarePackage", softwarePackageMock);
        when(neInfoQueryMock.getParams()).thenReturn(Arrays.asList(neParamsMock));
        when(neInfoQueryMock.getNeType()).thenReturn("ERBS");
        when(neParamsMock.getName()).thenReturn(JobConfigurationConstants.SOFTWAREPKG_NAME);
        when(neParamsMock.getValue()).thenReturn("softwarePackage");
        when(softwarePackageQueryServiceMock.getSoftwarePackagesBasedOnPackageName(Matchers.anyMap())).thenReturn(softwarePackageDataMap);
        when(softwarePackageMock.getActivities()).thenReturn(Arrays.asList(softwarePackageActivityDetailsMock));
        when(jobActivitiesResponseMock.getNeActivityInformation()).thenReturn(Arrays.asList(neActivityInformationMock));
        when(neActivityInformationMock.getActivity()).thenReturn(Arrays.asList(activityMock));
        when(neActivityInformationMock.getNeType()).thenReturn("ERBS");
        when(activityMock.getActivityParams()).thenReturn(Arrays.asList(activityParamsMock));
        when(activityParamsMock.getParam()).thenReturn(Arrays.asList(paramTypeMock));
        when(paramTypeMock.getName()).thenReturn(UpgradeActivityConstants.REBOOTNODEUPGRADE);
        when(paramTypeMock.getDefaultValue()).thenReturn("True");
        when(activityMock.getName()).thenReturn(CppUpgradeActivityConstants.UPGRADE);
        when(softwarePackageActivityDetailsMock.getName()).thenReturn(CppUpgradeActivityConstants.UPDATE);
        when(softwarePackageActivityDetailsMock.getActivityParams()).thenReturn(Arrays.asList(softwarePackageParamMock));
        when(softwarePackageParamMock.getName()).thenReturn(CppUpgradeActivityConstants.SMO_UPGRADE_REBOOT);
        when(softwarePackageParamMock.getValue()).thenReturn("False");
        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getNeType());
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getName());
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getName());
        Assert.assertNotNull(response.getNeActivityInformation().get(0).getActivity().get(0).getActivityParams().get(0).getParam().get(0).getDefaultValue());
        Assert.assertNotNull(response);
        Assert.assertEquals(jobActivitiesResponseMock, response);
    }

    @Test
    public void testgetUpdatedJobActivities_WhenReturnsEmptySMO() throws UnsupportedFragmentException {
        final Map<String, SoftwarePackage> softwarePackageDataMap = new HashMap<String, SoftwarePackage>();
        softwarePackageDataMap.put("softwarePackage", softwarePackageMock);
        when(neInfoQueryMock.getParams()).thenReturn(Arrays.asList(neParamsMock));
        when(neInfoQueryMock.getNeType()).thenReturn("ERBS");
        when(neParamsMock.getName()).thenReturn(JobConfigurationConstants.SOFTWAREPKG_NAME);
        when(neParamsMock.getValue()).thenReturn("softwarePackage");
        when(softwarePackageQueryServiceMock.getSoftwarePackagesBasedOnPackageName(Matchers.anyMap())).thenReturn(softwarePackageDataMap);

        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
        Assert.assertEquals(jobActivitiesResponseMock, response);
    }

    @Test
    public void testgetUpdatedJobActivities_WhenNoNeTypeMatches() throws UnsupportedFragmentException {
        final Map<String, SoftwarePackage> softwarePackageDataMap = new HashMap<String, SoftwarePackage>();
        when(neInfoQueryMock.getParams()).thenReturn(Arrays.asList(neParamsMock));
        when(neInfoQueryMock.getNeType()).thenReturn("ERBS");
        when(neParamsMock.getName()).thenReturn(JobConfigurationConstants.SOFTWAREPKG_NAME);
        when(neParamsMock.getValue()).thenReturn("softwarePackage");
        when(softwarePackageQueryServiceMock.getSoftwarePackagesBasedOnPackageName(Matchers.anyMap())).thenReturn(softwarePackageDataMap);

        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
        Assert.assertEquals(jobActivitiesResponseMock, response);
    }

    @Test
    public void testgetUpdatedJobActivities_WhenInvalidInput() throws UnsupportedFragmentException {
        final Map<String, SoftwarePackage> softwarePackageDataMap = new HashMap<String, SoftwarePackage>();
        softwarePackageDataMap.put("softwarePackage", softwarePackageMock);
        when(neInfoQueryMock.getParams()).thenReturn(Arrays.asList(neParamsMock));
        when(neInfoQueryMock.getNeType()).thenReturn("ERBS");
        final JobActivitiesResponse response = objectUnderTest.getUpdatedJobActivities(neInfoQueryMock, jobActivitiesResponseMock);
        Assert.assertNotNull(response);
        Assert.assertEquals(jobActivitiesResponseMock, response);
    }

    private void setUp() {
        final Map<String, SoftwarePackage> softwarePackageDataMap = new HashMap<String, SoftwarePackage>();
        softwarePackageDataMap.put("softwarePackage", softwarePackageMock);
        when(neInfoQueryMock.getParams()).thenReturn(Arrays.asList(neParamsMock));
        when(neInfoQueryMock.getNeType()).thenReturn("ERBS");
        when(neParamsMock.getName()).thenReturn(JobConfigurationConstants.SOFTWAREPKG_NAME);
        when(neParamsMock.getValue()).thenReturn("softwarePackage");
        when(softwarePackageQueryServiceMock.getSoftwarePackagesBasedOnPackageName(Matchers.anyMap())).thenReturn(softwarePackageDataMap);
        when(softwarePackageMock.getActivities()).thenReturn(Arrays.asList(softwarePackageActivityDetailsMock));
        when(softwarePackageMock.getJobParameters()).thenReturn(Arrays.asList(softwarePackageParamMock2));
    }
}
