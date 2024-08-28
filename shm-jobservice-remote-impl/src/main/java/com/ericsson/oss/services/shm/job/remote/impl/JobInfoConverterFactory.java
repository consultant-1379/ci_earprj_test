/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.remote.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.job.service.api.JobInfoConverter;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

/**
 * This class is used to select a Job Type specific implementation based on JobTypeAnnotation. There is a memory leakage issue in using InstanceProvider. So, we changed the implementation to Factory
 * Design as a quick fix. However, using Factory Design leads to poor code maintainability, so we need to analyze and find out a better solution which improves code maintainability and doesn't leak
 * any memory. JIRA : https://jira-nam.lmera.ericsson.se/browse/TORF-180324
 * 
 * @author tcskaki
 * 
 */
@ApplicationScoped
public class JobInfoConverterFactory {

    private static final Logger logger = LoggerFactory.getLogger(JobInfoConverterFactory.class);

    @Inject
    @JobTypeAnnotation(jobType = JobType.BACKUP)
    private JobInfoConverter backupJobInfoConverter;

    @Inject
    @JobTypeAnnotation(jobType = JobType.NODERESTART)
    private JobInfoConverter nodeRestartJobInfoConverter;

    @Inject
    @JobTypeAnnotation(jobType = JobType.DELETE_SOFTWAREPACKAGE)
    private JobInfoConverter deleteSoftwarePackageJobInfoConverter;

    @Inject
    @JobTypeAnnotation(jobType = JobType.DELETE_UPGRADEPACKAGE)
    private JobInfoConverter deleteUpgradePkgJobInfoConvertor;

    @Inject
    @JobTypeAnnotation(jobType = JobType.LICENSE)
    private JobInfoConverter installLicenseJobInfoConverter;

    @Inject
    @JobTypeAnnotation(jobType = JobType.LICENSE_REFRESH)
    private JobInfoConverter licenseRefreshJobInfoConverter;

    public JobInfoConverter getJobInfoConverter(final JobType jobType) {

        JobInfoConverter jobInfoConverterInstance = null;
        switch (jobType) {

        case BACKUP:
            jobInfoConverterInstance = backupJobInfoConverter;
            break;

        case NODERESTART:
            jobInfoConverterInstance = nodeRestartJobInfoConverter;
            break;

        case DELETE_SOFTWAREPACKAGE:
            jobInfoConverterInstance = deleteSoftwarePackageJobInfoConverter;
            break;

        case DELETE_UPGRADEPACKAGE:
            jobInfoConverterInstance = deleteUpgradePkgJobInfoConvertor;
            break;

        case LICENSE:
            jobInfoConverterInstance = installLicenseJobInfoConverter;
            break;

        case LICENSE_REFRESH:
            jobInfoConverterInstance = licenseRefreshJobInfoConverter;
            break;

        default:
            logger.warn("Unspecified Qualifier: {}", jobType);
        }
        return jobInfoConverterInstance;
    }

}
