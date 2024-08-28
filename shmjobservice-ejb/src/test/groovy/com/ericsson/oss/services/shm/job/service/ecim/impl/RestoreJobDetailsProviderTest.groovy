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
package com.ericsson.oss.services.shm.job.service.ecim.impl

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

class RestoreJobDetailsProviderTest extends CdiSpecification{

    @ObjectUnderTest
    private RestoreJobDetailsProvider restoreJobDetailsProvider

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    private static final NENAME = "CORE25MTAS002"
    private static final NETYPE ="vMTAS"

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

    JobConfiguration buildJobConfigToPrepareJobSummary(final boolean encrypSupported,final JobConfiguration jobConfiguration){

        List<JobProperty> jobPropertyList = new ArrayList<>()
        if(encrypSupported){
            JobProperty passwordJobProperty = new JobProperty(JobPropertyConstants.SECURE_BACKUP_KEY,"123")
            jobPropertyList.add(passwordJobProperty)
        }
        NeTypeJobProperty neTypeJobProperty = new NeTypeJobProperty();
        List<NeTypeJobProperty> neTypeJobPropertyList = new ArrayList<>()
        neTypeJobPropertyList.add(neTypeJobProperty)
        neTypeJobProperty.setNeType(NETYPE)
        neTypeJobProperty.setJobProperties(jobPropertyList)

        Activity activity= new Activity();
        final Schedule schedule = new Schedule();
        schedule.setExecMode(ExecMode.IMMEDIATE);

        activity.setName(ShmConstants.RESTORE_BACKUP)
        activity.setNeType(NETYPE)
        activity.setSchedule(schedule)
        activity.setPlatform(PlatformTypeEnum.ECIM);

        List<Activity> activityList = new ArrayList<>()
        activityList.add(activity)
        jobConfiguration.setActivities(activityList)
        jobConfiguration.setNeTypeJobProperties(neTypeJobPropertyList)

        return jobConfiguration
    }
    def "Check for Securue backup for Restore Jobs having the password in the JobConfiguration"(encrypSupported,jsonResponseContainsSecureBakcup){
        given :"JobConfiguration"
        JobConfiguration jobConfiguration = new JobConfiguration();
        when:"When JobSummary Rest end Request for response"
        JobConfigurationDetails jobConfigurationDetails = restoreJobDetailsProvider.getJobConfigParamDetails(buildJobConfigToPrepareJobSummary(encrypSupported,jobConfiguration), NETYPE)
        then:"Check for the Secure backup"
        checkForSecureBakcup(jobConfigurationDetails)==jsonResponseContainsSecureBakcup
        where:
        encrypSupported  | jsonResponseContainsSecureBakcup
        true             |  "Yes"
        false            |  null
    }

    public String checkForSecureBakcup(JobConfigurationDetails jobConfigurationDetails){
        List<ActivityInfo>  activtyInfoList = jobConfigurationDetails.getActivityInfoList();
        final boolean passwordExists;
        final boolean userlabelExists;
        for(ActivityInfo activityInfo:activtyInfoList){
            if(activityInfo.getActivityName().equals(ShmConstants.RESTORE_BACKUP)){
                List<JobProperty> createBackupJobPropertyList = activityInfo.getJobProperties();
                for (JobProperty jobProperty : createBackupJobPropertyList){
                    if((!jobProperty.getKey().equals(JobPropertyConstants.SECURE_BACKUP_KEY))&&(jobProperty.getKey().equals(ShmConstants.SECURE_BACKUP))){
                        passwordExists =true;
                    }
                }
            }
        }
        if(passwordExists){
            return "Yes";
        }
    }
}
