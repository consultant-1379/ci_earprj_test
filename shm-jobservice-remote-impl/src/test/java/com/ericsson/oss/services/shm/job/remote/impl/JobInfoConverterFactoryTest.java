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
package com.ericsson.oss.services.shm.job.remote.impl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.job.service.api.JobInfoConverter;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

@RunWith(MockitoJUnitRunner.class)
public class JobInfoConverterFactoryTest {

    @InjectMocks
    private JobInfoConverterFactory jobInfoConverterFactory;

    @Mock
    private JobInfoConverter backupJobInfoConverter;

    @Mock
    private JobInfoConverter nodeRestartJobInfoConverter;

    @Mock
    private JobInfoConverter deleteSoftwarePackageJobInfoConverter;

    @Mock
    private JobInfoConverter deleteUpgradePkgJobInfoConvertor;

    @Mock
    private JobInfoConverter installLicenseJobInfoConverter;

    @Mock
    private JobInfoConverter licenseRefreshJobInfoConverter;

    @Test
    public void testGetJobInfoConverter() {
        JobInfoConverter jobInfoConverterInstance;

        jobInfoConverterInstance = jobInfoConverterFactory.getJobInfoConverter(JobType.BACKUP);
        Assert.assertNotNull(jobInfoConverterInstance);
        jobInfoConverterInstance = jobInfoConverterFactory.getJobInfoConverter(JobType.NODERESTART);
        Assert.assertNotNull(jobInfoConverterInstance);
        jobInfoConverterInstance = jobInfoConverterFactory.getJobInfoConverter(JobType.DELETE_SOFTWAREPACKAGE);
        Assert.assertNotNull(jobInfoConverterInstance);
        jobInfoConverterInstance = jobInfoConverterFactory.getJobInfoConverter(JobType.DELETE_UPGRADEPACKAGE);
        Assert.assertNotNull(jobInfoConverterInstance);
        jobInfoConverterInstance = jobInfoConverterFactory.getJobInfoConverter(JobType.LICENSE);
        Assert.assertNotNull(jobInfoConverterInstance);
        jobInfoConverterInstance = jobInfoConverterFactory.getJobInfoConverter(JobType.LICENSE_REFRESH);
        Assert.assertNotNull(jobInfoConverterInstance);
        jobInfoConverterInstance = jobInfoConverterFactory.getJobInfoConverter(JobType.SYSTEM);
        Assert.assertNull(jobInfoConverterInstance);
    }

}
