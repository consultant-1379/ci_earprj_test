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
package com.ericsson.oss.services.shm.job.service.cpp.license;

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
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.job.remote.api.license.InstallLicenseJobData;
import com.ericsson.oss.services.shm.job.remote.api.license.LicenseJobData;
import com.ericsson.oss.services.shm.job.remote.impl.NeTypePropertiesHelper;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

@RunWith(MockitoJUnitRunner.class)
public class CppLicenseJobNeTypePropertiesProviderTest {

    @InjectMocks
    private CppLicenseJobNeTypePropertiesProvider cppLicenseJobNeTypePropertiesProvider;

    @Mock
    private NeTypePropertiesHelper neTypePropertiesHelper;

    @Test
    public void testNeTypePropertyBuilding() {

        final List<String> activityProperties = new ArrayList<>();
        final InstallLicenseJobData jobCreationRemoteRequest = new InstallLicenseJobData();
        final LicenseJobData licenseJobDataFromCLI = new LicenseJobData();
        licenseJobDataFromCLI.setLicenseKeyFilePath("/dummy/file/path/");
        licenseJobDataFromCLI.setFingerPrint("fingerprint");
        licenseJobDataFromCLI.setNodeName("LTEERBS00002");
        licenseJobDataFromCLI.setNodetype("ERBS");
        jobCreationRemoteRequest.setJobCategory(JobCategory.REMOTE);
        Map<String, LicenseJobData> licenseJobData = new HashMap<>();
        licenseJobData.put(licenseJobDataFromCLI.getNodeName(), licenseJobDataFromCLI);
        jobCreationRemoteRequest.setlicenseJobData(licenseJobData);
        jobCreationRemoteRequest.setJobCategory(JobCategory.REMOTE);
        jobCreationRemoteRequest.setJobName("InstallLicenseJob");
        jobCreationRemoteRequest.setExecMode(ExecMode.IMMEDIATE.toString());
        jobCreationRemoteRequest.setJobType(JobType.LICENSE);
        jobCreationRemoteRequest.setActivity(ShmConstants.INSTALL_ACTIVITY);
        final List<Map<String, Object>> nePropertiesList = cppLicenseJobNeTypePropertiesProvider.getNeTypeProperties(activityProperties, jobCreationRemoteRequest);
        Map<String, Object> neProperties = nePropertiesList.get(0);
        Assert.assertEquals(1, nePropertiesList.size());
        Assert.assertEquals(2, neProperties.size());
        Assert.assertEquals("LTEERBS00002", neProperties.get(ShmConstants.NENAMES));
    }

    @Test
    public void testNeTypePropertyBuildingWithoutNeproperties() {

        final List<String> activityProperties = new ArrayList<>();
        final List<Map<String, Object>> properties = new ArrayList<>();
        final InstallLicenseJobData jobCreationRemoteRequest = new InstallLicenseJobData();
        jobCreationRemoteRequest.setJobCategory(JobCategory.REMOTE);
        Map<String, LicenseJobData> licenseJobData = new HashMap<>();
        final Set<String> neNames = new HashSet<>();
        neNames.add("LTEERBS00001");
        jobCreationRemoteRequest.setNeNames(neNames);
        jobCreationRemoteRequest.setlicenseJobData(licenseJobData);
        jobCreationRemoteRequest.setJobCategory(JobCategory.REMOTE);
        jobCreationRemoteRequest.setJobName("InstallLicenseJob");
        jobCreationRemoteRequest.setExecMode(ExecMode.IMMEDIATE.toString());
        jobCreationRemoteRequest.setJobType(JobType.LICENSE);
        jobCreationRemoteRequest.setActivity(ShmConstants.INSTALL_ACTIVITY);
        final List<Map<String, Object>> nePropertiesList = cppLicenseJobNeTypePropertiesProvider.getNeTypeProperties(activityProperties, jobCreationRemoteRequest);
        Map<String, Object> neProperties = nePropertiesList.get(0);
        Assert.assertEquals(1, nePropertiesList.size());
        Assert.assertEquals(2, neProperties.size());
        Assert.assertEquals("LTEERBS00001", neProperties.get(ShmConstants.NENAMES));
        Assert.assertEquals(properties, neProperties.get(ShmConstants.PROPERTIES));
    }
}
