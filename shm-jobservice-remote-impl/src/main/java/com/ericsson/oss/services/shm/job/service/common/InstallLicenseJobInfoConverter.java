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
package com.ericsson.oss.services.shm.job.service.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.job.remote.api.errorcodes.JobCreationResponseCode;
import com.ericsson.oss.services.shm.job.remote.api.license.InstallLicenseJobData;
import com.ericsson.oss.services.shm.job.remote.impl.JobInfoConverterImpl;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;

@JobTypeAnnotation(jobType = JobType.LICENSE)
public class InstallLicenseJobInfoConverter extends JobInfoConverterImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallLicenseJobInfoConverter.class);

    @Inject
    private LicenseJobActivitySchedulesUtil licenseJobActivitySchedulesUtil;

    @Override
    public JobInfo prepareJobInfoData(final ShmRemoteJobData shmRemoteJobData) throws NoMeFDNSProvidedException {
        LOGGER.debug("License installation remote job input: {}", shmRemoteJobData);
        final JobInfo jobInfo = new JobInfo();
        final InstallLicenseJobData installLicenseJobData = (InstallLicenseJobData) shmRemoteJobData;
        setNeNamesFromSearchScope(shmRemoteJobData);

        final Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes = platformAndNeTypeFinder.findSupportedPlatformAndNodeTypes(SHMCapabilities.LICENSE_JOB_CAPABILITY,
                installLicenseJobData);

        LOGGER.debug("Platform:Node map: {}", supportedPlatformTypeAndNodeTypes);
        jobInfo.setJobType(JobTypeEnum.LICENSE);

        setCommonAttributes(installLicenseJobData, jobInfo);

        jobInfo.setConfigurations(getConfigurations(supportedPlatformTypeAndNodeTypes, installLicenseJobData));
        jobInfo.setActivitySchedules(getActivitySchedules(supportedPlatformTypeAndNodeTypes, installLicenseJobData.getActivity()));

        LOGGER.info("Prepare JobInfo: {}", jobInfo);

        return jobInfo;
    }

    private List<Map<String, Object>> getConfigurations(final Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes, final InstallLicenseJobData installLicenseJobData) {

        final List<Map<String, Object>> configurations = new ArrayList<>();

        for (final Entry<PlatformTypeEnum, List<String>> platformTypeAndNodeType : supportedPlatformTypeAndNodeTypes.entrySet()) {
            final Map<String, Object> platformType = new HashMap<>();

            for (final String nodeType : platformTypeAndNodeType.getValue()) {

                final Map<String, Object> neConfiguration = new HashMap<>();
                neConfiguration.put(ShmConstants.NETYPE, nodeType);
                neConfiguration.put(ShmConstants.NE_PROPERTIES,
                        neTypePropertiesUtil.prepareNeTypeProperties(platformTypeAndNodeType.getKey(), nodeType, JobType.LICENSE.getJobTypeName(), installLicenseJobData));

                configurations.add(neConfiguration);
            }

            configurations.add(platformType);
        }
        LOGGER.debug("Prepare job configuration: {}", configurations);
        return configurations;
    }

    private List<Map<String, Object>> getActivitySchedules(final Map<PlatformTypeEnum, List<String>> supportedPlatformTypeAndNodeTypes, final String activity) {
        final List<Map<String, Object>> activitySchedules = new ArrayList<>();
        for (final Entry<PlatformTypeEnum, List<String>> platformTypeAndNodeType : supportedPlatformTypeAndNodeTypes.entrySet()) {
            final Map<String, Object> platformTypeDetails = new HashMap<>();
            final List<Map<String, Object>> neTypeDetails = new ArrayList<>();
            for (final String nodeType : platformTypeAndNodeType.getValue()) {
                final Map<String, Object> neTypeDetail = new HashMap<>();
                neTypeDetail.put(ShmConstants.NETYPE, nodeType);
                neTypeDetail.put(ShmConstants.VALUE, licenseJobActivitySchedulesUtil.prepareActivitySchedules(platformTypeAndNodeType.getKey(), nodeType, JobType.LICENSE.getJobTypeName(), activity));
                neTypeDetails.add(neTypeDetail);
            }
            platformTypeDetails.put(ShmConstants.PLATFORMTYPE, platformTypeAndNodeType.getKey().name());
            platformTypeDetails.put(ShmConstants.VALUE, neTypeDetails);

            activitySchedules.add(platformTypeDetails);
        }

        LOGGER.debug("Prepared activity schedule: {}", activitySchedules);
        return activitySchedules;
    }

    @Override
    public JobCreationResponseCode isValidData(final ShmRemoteJobData shmRemoteJobData) {
        return null;
    }

}
