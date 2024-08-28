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
package com.ericsson.oss.services.shm.notifications.impl

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.es.impl.JobFailureAlarmParameterChangeListener
import com.ericsson.oss.services.shm.internal.alarm.ShmInternalAlarmGenerator
import com.ericsson.oss.services.shm.job.api.JobConfigurationService
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

public class JobsInternalAlarmServiceTest extends CdiSpecification {

    @MockedImplementation
    private JobConfigurationService jobConfigurationService;

    @MockedImplementation
    private JobFailureAlarmParameterChangeListener jobFailureAlarmParameterChangeListener;

    @MockedImplementation
    private ShmInternalAlarmGenerator shmInternalAlarmGenerator;

    @ObjectUnderTest
    private JobsInternalAlarmService jobsInternalAlarmService;

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.service")
    }

    long mainJobId=1;

    def loadAlarmConfigurations() {
        jobFailureAlarmParameterChangeListener.isAlarmNeededOnShmJobFailure() >> true;
        jobFailureAlarmParameterChangeListener.isAlarmNeededOnBackupJobFailure() >> true;
        jobFailureAlarmParameterChangeListener.isAlarmNeededOnUpgradeJobFailure() >> true;
        jobFailureAlarmParameterChangeListener.isAlarmNeededOnDeleteUpgradepackageJobFailure() >> true;
        jobFailureAlarmParameterChangeListener.isAlarmNeededOnLicenseJobFailure() >> true;
        jobFailureAlarmParameterChangeListener.isAlarmNeededOnRestoreJobFailure() >> true;
        jobFailureAlarmParameterChangeListener.isAlarmNeededOnDeleteBackupJobFailure() >> true;
        jobFailureAlarmParameterChangeListener.isAlarmNeededOnBackupHousekeepingJobFailure() >> true;
        jobFailureAlarmParameterChangeListener.isAlarmNeededOnDeleteSoftwarePackageJobFailure() >> true;
        jobFailureAlarmParameterChangeListener.isAlarmNeededOnOboardJobFailure() >> true;
    }
    def 'Check the alarms raised for each job Type' () {

        given: "job details and alarm configuration"
        loadAlarmConfigurations();
        Map<String,Object> jobDetails=new HashMap();
        jobDetails.put(ShmConstants.JOB_TYPE,jobType);
        jobDetails.put(ShmConstants.OWNER,"adminstrator")
        jobDetails.put(ShmConstants.NAME,"test")
        jobDetails.put(ShmConstants.CREATION_TIME,"")
        jobDetails.put(ShmConstants.TOTAL_NES,"1")
        jobDetails.put(ShmConstants.FAILED_NES,"0")
        jobDetails.put(ShmConstants.SKIPPED_NES,"0")
        jobConfigurationService.getJobDetailsToRaiseAlarm(mainJobId) >> jobDetails;

        when: "raising alarms"
        jobsInternalAlarmService.checkIfAlarmHasToBeRaised(mainJobId,jobType)
        assert(jobDetails.get(ShmConstants.EVENT_TYPE)== eventType);
        assert(jobDetails.get(ShmConstants.PERCEIVED_SEVERITY) == severity);

        then: "Check if the internal alarm raised"
        count*shmInternalAlarmGenerator.raiseInternalAlarm(jobDetails)

        where: ""
        jobType                                            |      count        | eventType        | severity
        JobTypeEnum.NODE_HEALTH_CHECK.toString()           |      0            | null             | null
        JobTypeEnum.UPGRADE.toString()                     |      1            | "SHM_ERROR"      | "CRITICAL"
        JobTypeEnum.BACKUP.toString()                      |      1            | "SHM_ERROR"      | "MAJOR"
        JobTypeEnum.RESTORE.toString()                     |      1            | "SHM_ERROR"      | "CRITICAL"
        JobTypeEnum.LICENSE.toString()                     |      1            | "SHM_ERROR"      | "CRITICAL"
        JobTypeEnum.DELETEBACKUP.toString()                |      1            | "SHM_ERROR"      | "MAJOR"
        JobTypeEnum.BACKUP_HOUSEKEEPING.toString()         |      1            | "SHM_ERROR"      | "MAJOR"
        JobTypeEnum.DELETE_SOFTWAREPACKAGE.toString()      |      1            | "Processing Error Alarm"      | "CRITICAL"
        JobTypeEnum.DELETE_UPGRADEPACKAGE.toString()       |      1            | "SHM_ERROR"      | "MAJOR"
        JobTypeEnum.ONBOARD.toString()                     |      1            | "Processing Error Alarm"      | "CRITICAL"
        JobTypeEnum.NODERESTART.toString()                 |      1            | "SHM_ERROR"      | "CRITICAL"
        JobTypeEnum.SYSTEM.toString()                      |      1            | "SHM_ERROR"      | "CRITICAL"
    }
}
