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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.remote.api.NeTypePropertiesProvider;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.job.remote.api.license.InstallLicenseJobData;
import com.ericsson.oss.services.shm.job.remote.api.license.LicenseJobData;
import com.ericsson.oss.services.shm.job.remote.impl.NeTypePropertiesHelper;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.CPP, jobType = com.ericsson.oss.services.shm.job.activity.JobType.LICENSE)
public class CppLicenseJobNeTypePropertiesProvider implements NeTypePropertiesProvider {

    @Inject
    private NeTypePropertiesHelper neTypePropertiesHelper;

    @Override
    public List<Map<String, Object>> getNeTypeProperties(final List<String> activityProperties, final ShmRemoteJobData shmRemoteJobData) {
        final InstallLicenseJobData installLicenseJobData = (InstallLicenseJobData) shmRemoteJobData;
        final List<Map<String, Object>> neProperties = new ArrayList<>();
        final Map<String, LicenseJobData> licenseJobDataMap = installLicenseJobData.getlicenseJobData();
        if (licenseJobDataMap.isEmpty() || licenseJobDataMap == null) {
            for (String neName : installLicenseJobData.getNeNames()) {
                licenseJobDataMap.put(neName, new LicenseJobData());
            }
        }

        for (final Entry<String, LicenseJobData> licenseDataOfNode : licenseJobDataMap.entrySet()) {
            final Map<String, Object> nePropertiesOfNode = new HashMap<>();
            nePropertiesOfNode.put(ShmConstants.NENAMES, licenseDataOfNode.getKey());
            nePropertiesOfNode.put(ShmConstants.PROPERTIES, buildProperties(licenseDataOfNode.getValue()));
            neProperties.add(nePropertiesOfNode);

        }

        return neProperties;
    }

    private List<Map<String, Object>> buildProperties(final LicenseJobData licenseJobDataMap) {
        final List<Map<String, Object>> properties = new ArrayList<>();
        if (isValidData(licenseJobDataMap.getLicenseKeyFilePath())) {
            properties.add(neTypePropertiesHelper.createPropertyMap(JobPropertyConstants.PROP_LICENSE_KEYFILE_PATH, licenseJobDataMap.getLicenseKeyFilePath()));
        }
        if (isValidData(licenseJobDataMap.getFingerPrint())) {
            properties.add(neTypePropertiesHelper.createPropertyMap(JobPropertyConstants.PROP_FINGERPRINT, licenseJobDataMap.getFingerPrint()));
        }
        if (isValidData(licenseJobDataMap.getSequenceNumber())) {
            properties.add(neTypePropertiesHelper.createPropertyMap(JobPropertyConstants.PROP_SEQUENCENUMBER, licenseJobDataMap.getSequenceNumber()));
        }
        return properties;
    }

    private boolean isValidData(final String value) {

        return value != null && !value.isEmpty();

    }

}
