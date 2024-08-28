/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service.minilinkindoor.impl

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails

class LicenseJobDetailsProviderSpec  extends CdiSpecification{
    @MockedImplementation
    private JobPropertyUtils jobPropertyUtils;

    @MockedImplementation
    private ActivityParamMapper activityParamMapper;

    @ObjectUnderTest
    LicenseJobDetailsProvider licenseJobDetailsProvider


    final Map<String, Object> jobConfigurationDetails = new HashMap<String, Object>();
    private static final String nodeName = "nodeName";
    private static final String neType = "MINI-LINK-Indoor";
    final List<String> keyList = new ArrayList<String>();
    final Map<String, String> licenseValueMap = new HashMap<>();
    Map<String, List<String>> acitvityParameters = new HashMap<String, List<String>>();
    JobConfiguration jobConfiguration = new JobConfiguration();
    JobConfigurationDetails jobConfigurationDetail = new JobConfigurationDetails();

    def setup() {
        keyList.add(JobPropertyConstants.PROP_LICENSE_KEYFILE_PATH);
        licenseValueMap.put(JobPropertyConstants.PROP_LICENSE_KEYFILE_PATH, "licenseKeyFilePath/file.xml")
    }

    def "TestgetJobConfigurationDetails"() {
        given:"initialize"
        jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, nodeName, neType, PlatformTypeEnum.MINI_LINK_INDOOR.toString()) >> licenseValueMap
        when: "invoke getJobConfigurationDetails"
        List<Map<String, String>> upgradeJobDetailsList = licenseJobDetailsProvider.getJobConfigurationDetails(jobConfigurationDetails, PlatformTypeEnum.MINI_LINK_INDOOR, neType, nodeName)
        then : "upgradeJobDetailsList should contain values"
        for (Map<String, String> result : upgradeJobDetailsList) {
            assertNotNull(result);
        }
    }

    def "TestgetJobConfigParamDetails"() {
        given: "init"
        activityParamMapper.getJobConfigurationDetails(jobConfiguration, neType, PlatformTypeEnum.MINI_LINK_INDOOR.name(), acitvityParameters) >> jobConfigurationDetail
        when: "execute"
        JobConfigurationDetails result = licenseJobDetailsProvider.getJobConfigParamDetails(jobConfiguration, neType);
        then:"results should match jobConfigurationDetail"
        assertEquals(result, jobConfigurationDetail);
    }
}
