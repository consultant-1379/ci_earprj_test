/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.remote.api.errorcodes.JobCreationResponseCode;
import com.ericsson.oss.services.shm.job.remote.api.licenserefresh.LicenseRefreshJobData;
import com.ericsson.oss.services.shm.job.remote.impl.JobInfoConverterImpl;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;

@RunWith(MockitoJUnitRunner.class)
public class LicenseRefreshJobInfoConverterTest {

    @InjectMocks
    private LicenseRefreshJobInfoConverter licenseRefreshJobInfoConverterTest;

    @Mock
    private JobInfoConverterImpl jobInfoConverterImpl;

    @Mock
    private JobCapabilityProvider jobCapabilityProvider;

    @Test
    public void testPrepareJobInfoData() throws NoMeFDNSProvidedException {
        final LicenseRefreshJobData licenseRefreshJobData = new LicenseRefreshJobData();
        licenseRefreshJobData.setJobName("LicenseRefresh_job");
        licenseRefreshJobData.setNeNames(prepareNeNames());
        licenseRefreshJobData.setJobType(JobType.LICENSE_REFRESH);
        licenseRefreshJobData.setExecMode(ExecMode.IMMEDIATE.toString());
        licenseRefreshJobData.setConfigurations(new ArrayList<Map<String, Object>>());
        licenseRefreshJobData.setActivitySchedules(new ArrayList<Map<String, Object>>());
        licenseRefreshJobData.setMainSchedule(new HashMap<String, Object>());
        when(jobCapabilityProvider.getCapability(JobTypeEnum.LICENSE_REFRESH)).thenReturn("LicenseRefrsh_workflow_prefix");
        final JobInfo jobInfo = licenseRefreshJobInfoConverterTest.prepareJobInfoData(licenseRefreshJobData);
        verify(jobCapabilityProvider).getCapability(JobTypeEnum.LICENSE_REFRESH);
        assertNotNull(jobInfo);
        assertEquals(JobTypeEnum.LICENSE_REFRESH, jobInfo.getJobType());
        assertNotNull(jobInfo.getActivitySchedules());
        assertNotNull(jobInfo.getConfigurations());
        assertNotNull(jobInfo.getMainSchedule());
        assertEquals(3, jobInfo.getNeNames().size());
    }

    @Test
    public void testIsValidData_emptyJobName() {
        final LicenseRefreshJobData licenseRefreshJobData = new LicenseRefreshJobData();
        licenseRefreshJobData.setJobName("");
        licenseRefreshJobData.setNeNames(prepareNeNames());
        licenseRefreshJobData.setJobType(JobType.LICENSE_REFRESH);
        licenseRefreshJobData.setExecMode(ExecMode.IMMEDIATE.toString());
        licenseRefreshJobData.setConfigurations(new ArrayList<Map<String, Object>>());
        licenseRefreshJobData.setActivitySchedules(new ArrayList<Map<String, Object>>());
        licenseRefreshJobData.setMainSchedule(new HashMap<String, Object>());
        final JobCreationResponseCode jobCreationResponseCode = licenseRefreshJobInfoConverterTest.isValidData(licenseRefreshJobData);
        verifyAsserts(jobCreationResponseCode);
    }

    private Set<String> prepareNeNames() {
        Set<String> neNames = new HashSet<>();
        neNames.add("LTE01dg2ERBS00001");
        neNames.add("LTE555GRadioNode00001");
        neNames.add("LTE52vPP00001");
        return neNames;
    }

    private void verifyAsserts(final JobCreationResponseCode jobCreationResponseCode) {
        assertNotNull(jobCreationResponseCode);
        assertEquals(13304, jobCreationResponseCode.getErrorCode());
        assertEquals("Invalid job name", jobCreationResponseCode.getErrorMessage());
    }

}
