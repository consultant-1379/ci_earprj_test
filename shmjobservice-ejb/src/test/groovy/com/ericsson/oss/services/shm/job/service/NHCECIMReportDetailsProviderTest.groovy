/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.job.service.ecim.impl.NHCECIMReportDetailsProvider
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.modelentities.*
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.job.activity.JobType

class NHCECIMReportDetailsProviderTest extends CdiSpecification {

    @ObjectUnderTest
    NHCECIMReportDetailsProvider objectUnderTest;

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.addProxyController(new EjbProxyController(true))
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }

    def "get jobConfiguration parameter Details"(){
        given:"Configuration Data"
        JobConfiguration jobConfigurationParamDetails=buildJobConfigurationDetails();

        when :"Retreiving data from rest point"
        JobConfigurationDetails response=objectUnderTest.getJobConfigParamDetails(jobConfigurationParamDetails, "Radio");

        then:"Getting configuration parameters details properly"
        assert (response.getNeType()=="Radio");
        assert (response.getActivityInfoList().get(0).getActivityName()=="enmhealthcheck");
        assert (response.getActivityInfoList().get(0).getScheduledTime() == null);
        assert (response.getActivityInfoList().get(0).getOrder()==1);
        assert(response.getActivityInfoList().get(0).getJobProperties().get(0).getValue() == "enmhealthcheck")
        assert(response.getActivityInfoList().get(0).getJobProperties().get(0).getKey() == "ENM_HEALTH_CHECK_RULES")
        assert (response.getActivityInfoList().get(1).getActivityName()=="nodehealthcheck");
        assert (response.getActivityInfoList().get(1).getOrder()==1);
        assert(response.getActivityInfoList().get(1).getJobProperties().get(0).getValue() == "nodehealthcheck")
        assert(response.getActivityInfoList().get(1).getJobProperties().get(0).getKey() == "NODE_HEALTH_CHECK_TEMPLATE")
        assert (response.getJobProperties()==jobConfigurationParamDetails.getJobProperties());

    }

    def "get jobConfiguration parameter Details by giving empty inputs"(){
        given:"Configuration Data"
        JobConfiguration jobConfigurationParamDetails=new  JobConfiguration();
        List<JobProperty> jobProperties=new ArrayList<>();
        List<Activity> activities = new ArrayList<Activity>()
        jobConfigurationParamDetails.setActivities(activities);
        jobConfigurationParamDetails.setJobProperties(jobProperties);

        when :"Retreiving data from rest point"
        JobConfigurationDetails response=objectUnderTest.getJobConfigParamDetails(jobConfigurationParamDetails, "");

        then:"Getting configuration parameters details properly"
        assert (response.getNeType()=="");
        assert (response.getActivityInfoList().isEmpty());
        assert (response.getJobProperties().isEmpty());
    }

    def "get jobConfiguration parameter Details by giving more data"(){
        given:"Configuration Data"
        JobConfiguration jobConfigurationParamDetails=buildJobConfigurationDetails();
        List<JobProperty> jobProperties=new ArrayList<>();
        List<JobProperty> neExtraProperties=new ArrayList<>();
        NeTypeJobProperty neTypeJobProperty = new NeTypeJobProperty();
        List<NeTypeJobProperty> neTypeJobProperties = new ArrayList<>();
        JobProperty neJobProp = new JobProperty("HEALTH_CHECK_RULES","extrahealthcheck");
        neExtraProperties.add(neJobProp)
        neTypeJobProperty.setNeType("Radio")
        neTypeJobProperty.setJobProperties(neExtraProperties);
        neTypeJobProperties.add(neTypeJobProperty)
        jobProperties.add(neJobProp)
        Activity activitydata = new Activity();
        activitydata.setName("MoreCheckToTest")
        activitydata.setNeType("JUNIPER")
        activitydata.setPlatform(PlatformTypeEnum.JUNOS)
        activitydata.setOrder(1)
        jobConfigurationParamDetails.getActivities().addAll(activitydata);
        jobConfigurationParamDetails.getNeTypeJobProperties().addAll(neTypeJobProperties);
        jobConfigurationParamDetails.getJobProperties().addAll(jobProperties);

        when :"Retreiving data from rest point"
        JobConfigurationDetails response=objectUnderTest.getJobConfigParamDetails(jobConfigurationParamDetails, "Radio");

        then:"Getting configuration parameters details properly"
        assert (response.getNeType()=="Radio");
        assert (response.getActivityInfoList().get(0).getActivityName()=="enmhealthcheck");
        assert(response.getActivityInfoList().get(0).getJobProperties().get(0).getValue() == "enmhealthcheck")
        assert(response.getActivityInfoList().get(0).getJobProperties().size()==1)
        assert(response.getActivityInfoList().get(1).getJobProperties().get(0).getValue() == "nodehealthcheck")
        assert(response.getActivityInfoList().get(1).getJobProperties().size()==1)
        assert (response.getActivityInfoList().get(1).getActivityName()=="nodehealthcheck");
        assert (response.getActivityInfoList().size()==2);
        assert (response.getJobProperties().size()!=jobConfigurationParamDetails.getJobProperties().size());

    }

    def "get jobConfiguration details"(){
        given:"Configuration Data"
        Map<String, Object> jobConfigurationDetails= new HashMap<>();;

        when :"Retreiving data from rest point"
        List<Map<String, String>> response=new LinkedList<>();
        response=objectUnderTest.getJobConfigurationDetails(jobConfigurationDetails, PlatformTypeEnum.ECIM, "Radio", "LTE06dg2ERBS00001")

        then:"Returns empty list"
        assert (response.isEmpty());
    }

    def buildJobConfigurationDetails() {
        JobConfiguration jobConfigurationParamDetails=new  JobConfiguration();
        List<JobProperty> jobProperties=new ArrayList<JobProperty>();
        JobProperty jobPropEnm = new JobProperty("ENM_HEALTH_CHECK_RULES","enmhealthcheck");
        JobProperty jobPropNhc = new JobProperty("NODE_HEALTH_CHECK_TEMPLATE","nodehealthcheck");
        NeTypeJobProperty neTypeJobProperty = new NeTypeJobProperty();
        List<NeTypeJobProperty> neTypeJobProperties = new ArrayList<>();
        jobProperties.add(jobPropEnm);
        jobProperties.add(jobPropNhc);
        List<Activity> activities = new ArrayList<Activity>()
        Activity activityNhc=new Activity();
        activityNhc.setName("nodehealthcheck");
        activityNhc.setPlatform(PlatformTypeEnum.ECIM)
        activityNhc.setOrder(1);
        activityNhc.setNeType("Radio")
        Activity activityEnm=new Activity();
        activityEnm.setName("enmhealthcheck");
        activityEnm.setPlatform(PlatformTypeEnum.ECIM)
        activityEnm.setOrder(1);
        activityEnm.setNeType("Radio")
        activities.add(activityEnm);
        activities.add(activityNhc);
        ExecMode execMode = ExecMode.SCHEDULED;
        Schedule schedule = new Schedule();
        schedule.setExecMode(execMode);
        activityNhc.setSchedule(schedule);
        activityEnm.setSchedule(schedule)
        neTypeJobProperty.setNeType("Radio");
        neTypeJobProperty.setJobProperties(jobProperties);
        neTypeJobProperties.add(neTypeJobProperty)
        jobConfigurationParamDetails.setActivities(activities);
        jobConfigurationParamDetails.setNeTypeJobProperties(neTypeJobProperties)
        jobConfigurationParamDetails.setJobProperties(jobProperties);
        return jobConfigurationParamDetails;
    }

}
