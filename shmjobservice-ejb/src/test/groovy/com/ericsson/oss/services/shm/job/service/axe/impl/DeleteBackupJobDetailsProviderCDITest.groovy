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
package com.ericsson.oss.services.shm.job.service.axe.impl

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants

public class DeleteBackupJobDetailsProviderCDITest extends CdiSpecification {

    @ObjectUnderTest
    private DeleteBackupJobDetailsProvider deleteBackupJobDetailsProvider;

    def "get Job Configuration Details"(){
        given : "Node Details"
        final Map<String, Object> configurationDetails = getJobConfigurationDetails(backupNames)
        final String neType = "BSC";
        final String neName = "GSM02BSC01";
        final PlatformTypeEnum platformTypeEnum = PlatformTypeEnum.AXE;

        when : "get the job configuration details"
        final List<Map<String, Object>> jobConfigurationDetails =  deleteBackupJobDetailsProvider.getJobConfigurationDetails(configurationDetails, platformTypeEnum, neType, neName)

        then : "Backups should be loaded with proper location"
        noOfBackups == jobConfigurationDetails.size()
        if(noOfBackups>0) {
            jobConfigurationDetails.get(backUpIndex).get("backupName") == backUpName
            jobConfigurationDetails.get(backUpIndex).get("location") == location
        }

        where:
        backupNames           | noOfBackups  |backUpIndex |  backUpName | location
        "test|NODE"           | 1            | 0          | "test"      | "NODE"
        "test|NODE,test1|ENM" | 2            | 1          | "test1"     | "ENM"
        ""                    | 0            | 0          | ""          | ""
    }

    final Map<String, Object> getJobConfigurationDetails(String backUpNames) {
        final Map<String, Object> jobConfigurationDetails =new HashMap<>()

        final List<Map<String, String>> jobPropertyList = new ArrayList<>()
        Map<String, String> jobPropertiest = new HashMap<>()
        jobPropertiest.put(ShmConstants.KEY, "BACKUP_NAME")
        jobPropertiest.put(ShmConstants.VALUE, backUpNames)

        jobPropertyList.add(jobPropertiest)

        Map<String, Object> neJobProperties = new HashMap<>()
        neJobProperties.put(ShmConstants.JOBPROPERTIES, jobPropertyList)
        neJobProperties.put(ShmConstants.NE_NAME,"GSM02BSC01")
        List<Map<String, Object>> configurationDetailList= new ArrayList<>()
        configurationDetailList.add(neJobProperties);

        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, configurationDetailList)
        return  jobConfigurationDetails
    }
}
