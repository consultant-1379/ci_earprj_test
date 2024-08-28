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
package com.ericsson.oss.services.shm.es.impl

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean
import com.ericsson.oss.services.shm.es.api.JobConsolidationService
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType

class JobStatusServiceImplGroovyTest extends CdiSpecification{

    @ObjectUnderTest
    JobStatusServiceImpl jobStatusServiceImpl

    @MockedImplementation
    private JobStaticDataProvider jobStaticDataProvider;

    @MockedImplementation
    private JobStaticData jobStaticData;

    @MockedImplementation
    private ServiceFinderBean serviceFinderBeanMock;

    @MockedImplementation
    private JobConsolidationService jobConsolidationService;

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.itpf.sdk.core.classic")

        injectionProperties.addProxyController(new EjbProxyController(true))
    }


    def 'test_updateNHCJobAsCancelled_withisActivityJobUpdateAsFalseAndActivityResultAsFailed'(){
        given: "to call update ne job attributes provide mock data"

        jobStaticDataProvider.getJobStaticData(1L)>>jobStaticData;
        jobStaticData.getJobType() >>JobType.NODE_HEALTH_CHECK;

        Map<String, Object> jobAttributesMapMock = new HashMap<>();
        jobAttributesMapMock.put(ShmConstants.MAIN_JOB_ID, 1L);
        final Map<String, Object> neJobAttributes = new HashMap<>();

        serviceFinderBeanMock.find(JobConsolidationService.class, JobType.NODE_HEALTH_CHECK.name())>> jobConsolidationService
        jobConsolidationService.consolidateNeJobData(2L) >>  new HashMap<>();

        when: "call update ne job attributes while cancelling job"
        jobStatusServiceImpl.updateNeJobAttributeForNHCJobs(jobAttributesMapMock, 2L, neJobAttributes);

        then: "verify the method call"
        1* jobStatusServiceImpl.updateNeJobAttributeForNHCJobs(jobAttributesMapMock, 2L, neJobAttributes);
    }
}
