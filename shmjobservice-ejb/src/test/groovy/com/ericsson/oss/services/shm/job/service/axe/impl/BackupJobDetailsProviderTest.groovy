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

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.modelentities.Activity
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobProperty
import com.ericsson.oss.services.shm.jobs.common.modelentities.NeTypeJobProperty
import com.ericsson.oss.services.shm.jobs.common.modelentities.Schedule
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails

class BackupJobDetailsProviderTest extends CdiSpecification {

    @ObjectUnderTest
    private BackupJobDetailsProvider backupJobDetailsProvider

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    private static final NENAME = "GSM02BSC01"
    private static final NETYPE ="MSC-BC-IS"
    private static final String INPUT_BACKUP_NAMES = "backupName";

    final Map<String, Object> getJobConfigurationDetails(final String key, final String backUpNames) {
        final Map<String, Object> jobConfigurationDetails =new HashMap<>()

        final List<Map<String, String>> jobPropertyList = new ArrayList<>()
        Map<String, String> jobPropertiest = new HashMap<>()
        jobPropertiest.put(ShmConstants.KEY, key)
        jobPropertiest.put(ShmConstants.VALUE, backUpNames)

        jobPropertyList.add(jobPropertiest)

        Map<String, Object> neJobProperties = new HashMap<>()
        neJobProperties.put(ShmConstants.JOBPROPERTIES, jobPropertyList)
        neJobProperties.put(ShmConstants.NE_NAME, NENAME)
        List<Map<String, Object>> configurationDetailList= new ArrayList<>()
        configurationDetailList.add(neJobProperties);

        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, configurationDetailList)
        return  jobConfigurationDetails
    }



    def "Configuration details of Node activities for manage Backup and create backup job for AXE platform nodes"(){
        given : "Prepare Job config data with back names for AXE platform nodes"
        final Map<String, Object> jobConfigurationDetails = getJobConfigurationDetails(key, backupNames)

        when: "call getJobConfigurationDetails"
        final List<Map<String, String>> response = backupJobDetailsProvider.getJobConfigurationDetails(jobConfigurationDetails,  PlatformTypeEnum.AXE, NETYPE, NENAME)

        then: "get list of backup names"
        assert(noOfBackups == response.size())
        assert(response.toString() == responseBackupName)

        where:
        key                                        |  backupNames           | noOfBackups  | responseBackupName
        INPUT_BACKUP_NAMES                         |  "Backup1"             | 1            | "[[backupName:Backup1]]"
        JobPropertyConstants.UPLOAD_BACKUP_DETAILS |  "Backup1, Backup2"    | 2            | "[[backupName:Backup1], [backupName: Backup2]]"
    }

    void buildJobConfigToPrepareJobSummary(final boolean encrypSupported,final JobConfiguration jobConfiguration){
        List<JobProperty> jobPropertyList = new ArrayList<>()

        if(encrypSupported){
            JobProperty passwordJobProperty = new JobProperty(JobPropertyConstants.SECURE_BACKUP_KEY,"123")
            JobProperty userlabelJobProperty = new JobProperty(JobPropertyConstants.USER_LABEL,"sample text")
            jobPropertyList.add(passwordJobProperty)
            jobPropertyList.add(userlabelJobProperty)
        }
        JobProperty rotateJobProperty = new JobProperty(JobPropertyConstants.ROTATE,"true")

        jobPropertyList.add(rotateJobProperty)
        NeTypeJobProperty neTypeJobProperty = new NeTypeJobProperty();
        List<NeTypeJobProperty> neTypeJobPropertyList = new ArrayList<>()
        neTypeJobPropertyList.add(neTypeJobProperty)
        println neTypeJobPropertyList
        neTypeJobProperty.setNeType("BSC")
        neTypeJobProperty.setJobProperties(jobPropertyList)
        Activity activity= new Activity();

        final Schedule schedule = new Schedule();
        schedule.setExecMode(ExecMode.IMMEDIATE);

        activity.setName(ShmConstants.CREATE_BACKUP)
        activity.setNeType("BSC")
        activity.setSchedule(schedule)
        activity.setPlatform(PlatformTypeEnum.AXE);


        List<Activity> activityList = new ArrayList<>()
        activityList.add(activity)
        jobConfiguration.setActivities(activityList)
        jobConfiguration.setNeTypeJobProperties(neTypeJobPropertyList)
    }
    def "Check for Securue APG backup for backup Jobs having the password and userlabel in the JobConfiguration"(encrypSupported,jsonResponseContainsSecureBakcup){
        given :"JobConfiguration"
        JobConfiguration jobConfiguration = new JobConfiguration();
        buildJobConfigToPrepareJobSummary(encrypSupported,jobConfiguration)
        println jobConfiguration.getNeTypeJobProperties()
        when:"When JobSummary Rest end Request for response"
        JobConfigurationDetails jobConfigurationDetails = backupJobDetailsProvider.getJobConfigParamDetails(jobConfiguration, "BSC")
        then:"Checck for the Secure APG backup"
        checkForSecureBakcup(jobConfigurationDetails)== jsonResponseContainsSecureBakcup
        where:
        encrypSupported  | jsonResponseContainsSecureBakcup
        true             |  true
        false            |  null
    }

    public Boolean checkForSecureBakcup(JobConfigurationDetails jobConfigurationDetails){
        List<ActivityInfo>  activtyInfoList = jobConfigurationDetails.getActivityInfoList();
        final boolean passwordExists;
        final boolean userlabelExists;
        for(ActivityInfo activityInfo:activtyInfoList){
            if(activityInfo.getActivityName().equals(ShmConstants.CREATE_BACKUP)){
                List<JobProperty> createBackupJobPropertyList = activityInfo.getJobProperties();
                for (JobProperty jobProperty : createBackupJobPropertyList){
                    if(!jobProperty.getKey().equals(JobPropertyConstants.SECURE_BACKUP_KEY) && jobProperty.getKey().equals("Secure APG backup")){
                        passwordExists =true;
                    }
                    if (!jobProperty.getKey().equals(JobPropertyConstants.USER_LABEL) && jobProperty.getKey().equals("User Label")){
                        userlabelExists=true;
                    }
                }
            }
        }
        if(passwordExists && userlabelExists){
            return true;
        }else{
            return false;
        }
    }
}
