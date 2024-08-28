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
package com.ericsson.oss.services.shm.job.service.axe.impl

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants

class UpgradeJobDetailsProviderSpec extends CdiSpecification {

    @ObjectUnderTest
    private UpgradeJobDetailsProvider upgradeJobDetailsProvider;

    def "get BSCNode JobConfigurationDetails"(){
        given :
        final Map<String, Object> configurationDetails = bscNodegetConfigurationDetails()
        final String neType = "BSC";
        final String neName = "GSM02BSC01";
        final PlatformTypeEnum platformTypeEnum = PlatformTypeEnum.AXE;

        when :
        final List<Map<String, Object>> jobConfigurationDetails =  upgradeJobDetailsProvider.getJobConfigurationDetails(configurationDetails, platformTypeEnum, neType, neName)

        then :
        jobConfigurationDetails.get(0).get("_MM_CONFIG_FILE") == "bsc08a.cfg"
        jobConfigurationDetails.get(0).get("REF") == "RELFSW99"
        jobConfigurationDetails.get(0).get("_EXCHANGE_ID_NEWAS") == "B27I06K0150_D WO"
        jobConfigurationDetails.get(0).get("_SEA") == "false"
        jobConfigurationDetails.get(0).get("_MM_CONFIG_FILE") == "bsc08a.cfg"
        jobConfigurationDetails.get(0).get("REF") != "RLFS99"
        jobConfigurationDetails.get(0).get("_EXCHANGE_ID_NEWAS") != "B7I06K0150_D WO"
        jobConfigurationDetails.get(0).get("_SEA") != "true"
    }

    final Map<String, Object> bscNodegetConfigurationDetails() {
        final Map<String, Object> jobConfigurationDetails =new HashMap<>()

        final List<Map<String, String>> jobPropertyList = new ArrayList<>()
        Map<String, String> jobPropertiest = new HashMap<>()
        jobPropertiest.put(ShmConstants.KEY, "_MM_CONFIG_FILE")
        jobPropertiest.put(ShmConstants.VALUE, "bsc08a.cfg")

        Map<String, String> jobPropertiest1 = new HashMap<>()
        jobPropertiest1.put(ShmConstants.KEY, "REF")
        jobPropertiest1.put(ShmConstants.VALUE, "RELFSW99")

        Map<String, String> jobPropertiest2 = new HashMap<>()
        jobPropertiest2.put(ShmConstants.KEY, "_EXCHANGE_ID_NEWAS")
        jobPropertiest2.put(ShmConstants.VALUE, "B27I06K0150_D WO")

        Map<String, String> jobPropertiest3 = new HashMap<>()
        jobPropertiest3.put(ShmConstants.KEY, "_SEA")
        jobPropertiest3.put(ShmConstants.VALUE, "false")

        jobPropertyList.add(jobPropertiest)
        jobPropertyList.add(jobPropertiest1)
        jobPropertyList.add(jobPropertiest2)
        jobPropertyList.add(jobPropertiest3)

        Map<String, Object> neJobProperties = new HashMap<>()
        neJobProperties.put(ShmConstants.JOBPROPERTIES, jobPropertyList)
        neJobProperties.put(ShmConstants.NE_NAME,"GSM02BSC01")
        List<Map<String, Object>> configurationDetailList= new ArrayList<>()
        configurationDetailList.add(neJobProperties);

        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, configurationDetailList)
        return  jobConfigurationDetails
    }

    def "get MSC JobConfigurationDetails"(){
        given :
        final Map<String, Object> configurationDetails = mscNodegetConfigurationDetails()
        final String neType = "MSC-BC-IS";
        final String neName = "MSC-BC-IS-18A-V202-AXE_CLUSTER";
        final PlatformTypeEnum platformTypeEnum = PlatformTypeEnum.AXE;
        when :
        final List<Map<String, Object>> jobConfigurationDetails =  upgradeJobDetailsProvider.getJobConfigurationDetails(configurationDetails, platformTypeEnum, neType, neName)
        then :
        jobConfigurationDetails.get(0).get("_SMO") == "Y"
        jobConfigurationDetails.get(0).get("_AXS_NE_DUAL") == ""
        jobConfigurationDetails.get(0).get("_XML_SMO_EXCHANGE_HEADER") == ""
        jobConfigurationDetails.get(0).get("_SMO") != "DD"
        jobConfigurationDetails.get(0).get("_AXS_NE_DUAL") != "YYY"
        jobConfigurationDetails.get(0).get("_XML_SMO_EXCHANGE_HEADER") != "RR"
    }
    final Map<String, Object> mscNodegetConfigurationDetails() {
        Map<String, Object> jobConfigurationDetails =new HashMap<>()
        final List<Map<String, String>> jobPropertyList = new ArrayList<>()

        Map<String, String> jobPropertiest = new HashMap<>()
        jobPropertiest.put(ShmConstants.KEY, "_SMO")
        jobPropertiest.put(ShmConstants.VALUE, "Y")

        Map<String, String> jobPropertiest1 = new HashMap<>()
        jobPropertiest1.put(ShmConstants.KEY, "_AXS_NE_DUAL")
        jobPropertiest1.put(ShmConstants.VALUE, "")

        Map<String, String> jobPropertiest2 = new HashMap<>()
        jobPropertiest2.put(ShmConstants.KEY, "_XML_SMO_EXCHANGE_HEADER")
        jobPropertiest2.put(ShmConstants.VALUE, "")

        jobPropertyList.add(jobPropertiest)
        jobPropertyList.add(jobPropertiest1)
        jobPropertyList.add(jobPropertiest2)

        Map<String, Object> neJobProperties = new HashMap<>()
        neJobProperties.put(ShmConstants.JOBPROPERTIES, jobPropertyList)
        neJobProperties.put(ShmConstants.NE_NAME,"MSC-BC-IS-18A-V202-AXE_CLUSTER")
        List<Map<String, Object>> configurationDetailList= new ArrayList<>()
        configurationDetailList.add(neJobProperties);
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, configurationDetailList)
        return  jobConfigurationDetails
    }
}
