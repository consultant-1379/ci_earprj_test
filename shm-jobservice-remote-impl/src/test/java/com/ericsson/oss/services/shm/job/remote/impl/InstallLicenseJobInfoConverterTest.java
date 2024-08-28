/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.remote.impl;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.remote.api.errorcodes.JobCreationResponseCode;
import com.ericsson.oss.services.shm.job.remote.api.license.InstallLicenseJobData;
import com.ericsson.oss.services.shm.job.remote.api.license.LicenseJobData;
import com.ericsson.oss.services.shm.job.service.common.InstallLicenseJobInfoConverter;
import com.ericsson.oss.services.shm.job.service.common.LicenseJobActivitySchedulesUtil;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;

@RunWith(MockitoJUnitRunner.class)
public class InstallLicenseJobInfoConverterTest {

    @InjectMocks
    private InstallLicenseJobInfoConverter installLicenseJobInfoConverter;

    @Mock
    private SupportedPlatformAndNeTypeFinder platformAndNeTypeFinderMock;

    @Mock
    private LicenseJobActivitySchedulesUtil licenseJobActivitySchedulesUtilMock;

    @Mock
    FdnServiceBean fdnServiceBeanMock;

    @Mock
    private NeTypePropertiesUtil neTypePropertiesUtilMock;

    @Mock
    private JobCapabilityProvider jobCapabilityProviderMock;

    @Test
    public void test() throws NoMeFDNSProvidedException {

        final Set<String> fdns = new HashSet<>();
        fdns.add("NetworkElement=LTEERBS00001");

        final Set<String> neNames = new HashSet<>();
        neNames.add("LTE5GRadio00001");

        final List<String> nes = new ArrayList<>();
        nes.add("LTE5GRadio00001");
        final Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypesMock = new EnumMap<>(PlatformTypeEnum.class);
        supportedPlatformTypeAndNodeTypesMock.put(PlatformTypeEnum.ECIM, nes);

        final List<Map<String, Object>> nePropetyList = new ArrayList<>();
        final Map<String, Object> neProperties = new HashMap<>();
        neProperties.put(ShmConstants.NENAMES, "LTE5GRadio00001");
        neProperties.put(ShmConstants.PROPERTIES, new ArrayList<>());
        nePropetyList.add(neProperties);

        final NetworkElement networkElement = new NetworkElement();
        networkElement.setName("LTE5GRadio00001");
        networkElement.setNeType("5GRadioNode");
        networkElement.setPlatformType(PlatformTypeEnum.ECIM);
        final List<NetworkElement> networkElements = new ArrayList<>();
        networkElements.add(networkElement);

        final InstallLicenseJobData jobCreationRemoteRequest = new InstallLicenseJobData();
        final LicenseJobData licenseJobDataFromCLI = new LicenseJobData();
        licenseJobDataFromCLI.setLicenseKeyFilePath("/dummy/file/path/");
        licenseJobDataFromCLI.setFingerPrint("fingerprint");
        licenseJobDataFromCLI.setNodeName("LTE5GRadio00001");
        licenseJobDataFromCLI.setNodetype("5GRadioNode");
        jobCreationRemoteRequest.setJobCategory(JobCategory.REMOTE);
        Map<String, LicenseJobData> licenseJobData = new HashMap<String, LicenseJobData>();
        licenseJobData.put(licenseJobDataFromCLI.getNodeName(), licenseJobDataFromCLI);
        jobCreationRemoteRequest.setlicenseJobData(licenseJobData);
        jobCreationRemoteRequest.setJobName("InstallLicenseJob");
        jobCreationRemoteRequest.setExecMode(ExecMode.IMMEDIATE.toString());
        jobCreationRemoteRequest.setJobType(JobType.LICENSE);

        jobCreationRemoteRequest.setNeNames(neNames);
        jobCreationRemoteRequest.setFdns(fdns);

        jobCreationRemoteRequest.setActivity(ShmConstants.INSTALL_ACTIVITY);

        Mockito.when(platformAndNeTypeFinderMock.findSupportedPlatformAndNodeTypes(SHMCapabilities.LICENSE_JOB_CAPABILITY, jobCreationRemoteRequest)).thenReturn(supportedPlatformTypeAndNodeTypesMock);
        Mockito.when(neTypePropertiesUtilMock.prepareNeTypeProperties(PlatformTypeEnum.ECIM, "5GRadioNode", JobType.LICENSE.getJobTypeName(), jobCreationRemoteRequest)).thenReturn(nePropetyList);
        Mockito.when(jobCapabilityProviderMock.getCapability(JobTypeEnum.LICENSE)).thenReturn(SHMCapabilities.LICENSE_JOB_CAPABILITY);
        Mockito.when(fdnServiceBeanMock.getNetworkElements(new ArrayList<>(fdns), SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(networkElements);

        final JobInfo jobInfo = installLicenseJobInfoConverter.prepareJobInfoData(jobCreationRemoteRequest);

        Assert.assertNotNull(jobInfo);

    }

    @Test
    public void validateInputRequest() {

        final InstallLicenseJobData jobCreationRemoteRequest = new InstallLicenseJobData();
        final LicenseJobData licenseJobDataFromCLI = new LicenseJobData();
        jobCreationRemoteRequest.setJobCategory(JobCategory.REMOTE);
        jobCreationRemoteRequest.setJobName("InstallLicenseJob");
        jobCreationRemoteRequest.setExecMode(ExecMode.IMMEDIATE.toString());
        jobCreationRemoteRequest.setJobType(JobType.LICENSE);

        licenseJobDataFromCLI.setLicenseKeyFilePath("/dummy/file/path/");
        licenseJobDataFromCLI.setFingerPrint("fingerprint");
        licenseJobDataFromCLI.setNodeName("LTE5GRadio00001");
        licenseJobDataFromCLI.setNodetype("5GRadioNode");
        jobCreationRemoteRequest.setJobCategory(JobCategory.REMOTE);
        Map<String, LicenseJobData> licenseJobData = new HashMap<String, LicenseJobData>();
        licenseJobData.put(licenseJobDataFromCLI.getNodeName(), licenseJobDataFromCLI);
        jobCreationRemoteRequest.setActivity(ShmConstants.INSTALL_ACTIVITY);
        JobCreationResponseCode validationResult = installLicenseJobInfoConverter.isValidData(jobCreationRemoteRequest);

        Assert.assertNull(validationResult);
    }

}
