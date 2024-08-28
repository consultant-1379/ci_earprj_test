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
package com.ericsson.oss.services.shm.job.resources

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.jobs.common.mapper.JobMapper
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate
import com.ericsson.oss.services.shm.jobs.common.restentities.RestJobConfiguration

/**
 * Class to verify tests on Job Configuration rest data
 *
 * @author xkalkil
 *
 */
public class RestDataMapperGroovyTest extends AbstractSHMJobServiceImplSpec{

    @ObjectUnderTest
    private RestDataMapper restDataMapper;

    @Inject
    JobMapper jobMapper

    def "Verify Job config rest data for backup job on AXE nodes"() {

        given : "Provide persisted JobTemplate id try to get summary details"
        long jobTemplateId = getJobTemplatePOId(NETYPE, ACTIVITIES, NE_COMPONENTS)
        Map<String, Object> jobTemplateAttributes = getJobTemplate(jobTemplateId)
        inventoryQueryConfigurationListener.getNeFdnBatchSize() >> 50
        JobTemplate jobTemplate = jobMapper.getJobTemplateDetails(jobTemplateAttributes, jobTemplateId)

        when: "call job config method to get summary details"
        RestJobConfiguration restJobConfiguration = restDataMapper.mapJobConfigToRestDataFormat(jobTemplate)

        then: "check number of component activities created"
        int componentsCount = restJobConfiguration.getSelectedNEs().getNeTypeComponentActivityDetails().get(0).getComponentActivities().size()
        int activitiesCount = restJobConfiguration.getSelectedNEs().getNeTypeComponentActivityDetails().get(0).getComponentActivities().get(0).getActivityNames().size()
        int componentActivtiesCount = (componentsCount * activitiesCount)
        ACTIVITIESCOUNT == componentActivtiesCount

        where: "Verify component activities when single activity selected and both create and upload activities selected"
        ACTIVITIES      | NE_COMPONENTS | ACTIVITIESCOUNT

        BOTH_ACTIVITIES | COMPONENTS    | COMPONENTS.size() * BOTH_ACTIVITIES.size()
        SINGLE_ACTIVITY | COMPONENTS    | COMPONENTS.size() * SINGLE_ACTIVITY.size()
    }
}
