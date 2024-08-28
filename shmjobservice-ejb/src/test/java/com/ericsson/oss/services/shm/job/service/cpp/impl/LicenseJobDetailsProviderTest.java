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
package com.ericsson.oss.services.shm.job.service.cpp.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;

@RunWith(MockitoJUnitRunner.class)
public class LicenseJobDetailsProviderTest {

    @Mock
    JobPropertyUtils jobPropertyUtils;

    @Mock
    private ActivityParamMapper activityParamMapper;

    @InjectMocks
    LicenseJobDetailsProvider licenseJobDetailsProvider;

    @Test
    public void testGetLicenseKeyFileName() {
        final Map<String, Object> jobConfiguration = new HashMap<String, Object>();
        final String licenseFilePath = "/home/smrs/smrsroot/licence/LTE02ERBS00001_fp/LTE02ERBS00001_fp_151008_060505.xml";
        final String expectedLicenseFileName = "LTE02ERBS00001_fp_151008_060505.xml";
        Map<String, String> licenseJobDetailsMap = setup(licenseFilePath);
        final PlatformTypeEnum platformType = PlatformTypeEnum.CPP;
        final String neName = "LTE02ERBS00001";
        final String neType = "ERBS";
        when(jobPropertyUtils.getPropertyValue(Matchers.anyList(), Matchers.anyMap(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(licenseJobDetailsMap);
        final List<Map<String, String>> deleteBackupValueList = licenseJobDetailsProvider.getJobConfigurationDetails(jobConfiguration, platformType, neType, neName);
        for (Map<String, String> deleteBackupValueMap : deleteBackupValueList) {
            assertNotNull(deleteBackupValueMap);
            assertEquals(1, deleteBackupValueMap.size());
            assertEquals(true, deleteBackupValueMap.containsValue(expectedLicenseFileName));
        }

    }

    private Map<String, String> setup(final String licenseKeyFilePath) {
        Map<String, String> deleteBackupJobDetailsMap = new HashMap<String, String>();
        deleteBackupJobDetailsMap.put(JobPropertyConstants.PROP_LICENSE_KEYFILE_PATH, licenseKeyFilePath);
        return deleteBackupJobDetailsMap;
    }
}
