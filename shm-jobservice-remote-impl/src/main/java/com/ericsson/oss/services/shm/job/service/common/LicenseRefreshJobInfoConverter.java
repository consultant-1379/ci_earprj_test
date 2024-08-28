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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.job.remote.api.errorcodes.JobCreationResponseCode;
import com.ericsson.oss.services.shm.job.remote.api.licenserefresh.LicenseRefreshJobData;
import com.ericsson.oss.services.shm.job.remote.impl.JobInfoConverterImpl;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;

@JobTypeAnnotation(jobType = JobType.LICENSE_REFRESH)
public class LicenseRefreshJobInfoConverter extends JobInfoConverterImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseRefreshJobInfoConverter.class);

    @Override
    public JobInfo prepareJobInfoData(final ShmRemoteJobData shmRemoteJobData) throws NoMeFDNSProvidedException {
        final JobInfo jobInfo = new JobInfo();
        final LicenseRefreshJobData licenseRefreshJobData = (LicenseRefreshJobData) shmRemoteJobData;
        setNeNamesFromSearchScope(shmRemoteJobData);

        jobInfo.setJobType(JobTypeEnum.LICENSE_REFRESH);

        setCommonAttributes(licenseRefreshJobData, jobInfo);

        jobInfo.setConfigurations(licenseRefreshJobData.getConfigurations());
        jobInfo.setActivitySchedules(licenseRefreshJobData.getActivitySchedules());
        jobInfo.setMainSchedule(licenseRefreshJobData.getMainSchedule());
        jobInfo.setJobProperties(licenseRefreshJobData.getJobProperties());
        jobInfo.setJobCategory(JobCategory.REMOTE);

        LOGGER.info("Prepared licenseRefreshJob info: {}", jobInfo);

        return jobInfo;
    }

    @Override
    public JobCreationResponseCode isValidData(final ShmRemoteJobData shmRemoteJobData) {
        final LicenseRefreshJobData licenseRefreshJobData = (LicenseRefreshJobData) shmRemoteJobData;
        if (licenseRefreshJobData.getJobName() == null || licenseRefreshJobData.getJobName().isEmpty()) {
            return JobCreationResponseCode.INVALID_JOB_NAME;
        }
        return null;
    }
}
